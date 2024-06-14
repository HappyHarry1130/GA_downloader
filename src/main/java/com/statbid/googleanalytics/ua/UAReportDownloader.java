package com.statbid.googleanalytics.ua;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.gax.rpc.PermissionDeniedException;
import com.google.auth.oauth2.AccessToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;

import com.google.api.services.analyticsreporting.v4.AnalyticsReporting;

import com.google.api.services.analyticsreporting.v4.model.ColumnHeader;
import com.google.api.services.analyticsreporting.v4.model.DateRange;
import com.google.api.services.analyticsreporting.v4.model.DateRangeValues;
import com.google.api.services.analyticsreporting.v4.model.GetReportsRequest;
import com.google.api.services.analyticsreporting.v4.model.GetReportsResponse;
import com.google.api.services.analyticsreporting.v4.model.Metric;
import com.google.api.services.analyticsreporting.v4.model.Dimension;
import com.google.api.services.analyticsreporting.v4.model.MetricHeaderEntry;
import com.google.api.services.analyticsreporting.v4.model.Report;
import com.google.api.services.analyticsreporting.v4.model.ReportRequest;
import com.google.api.services.analyticsreporting.v4.model.ReportRow;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.statbid.googleanalytics.*;

public class UAReportDownloader extends GoogleAnalyticsReportingModule {
    private static final long PAGE_SIZE = 10000L;

    private UAReportConfig config;


    public UAReportDownloader(final GoogleApiConfig apiConfig, final ReportBaseConfig reportConfig,
                               final File reportOutputDir)
            throws GoogleAnalyticsDownloaderConfigException {
        super(apiConfig, reportOutputDir);
        this.config = validateConfiguration(reportConfig, reportOutputDir);
    }

    public UAReportConfig validateConfiguration(final ReportBaseConfig baseConfig, final File reportOutputDir)
            throws GoogleAnalyticsDownloaderConfigException {
        final ConfigValidator validator = baseConfig.getValidator();
        validator.validateConfig(baseConfig);

        if (!reportOutputDir.exists()) {
            throw new GoogleAnalyticsDownloaderConfigException("*** ERROR: The specified report output directory does not exist.");
        }

        return (UAReportConfig) baseConfig;
    }



    public void execute() throws IOException, GoogleAnalyticsDownloaderException  {
        // Get the requested set of account IDs from the CSV.
        final List<AccountIdFileRecord> accounts =
                getAccountDetails(config.getAccountFileName());

        final File reportFile = new File(config.getReportFileName());
        if (reportFile.exists()) {
            System.out.println(
                    String.format("*** WARNING: Report output file %s already exists - it will be overwritten",
                            config.getReportFileName())
            );
            reportFile.delete();
        }

        final UACSVAccountReportWriter writer = new UACSVAccountReportWriter();
        writer.initialize(config.getReportFileName());

        final List<String> dimensionNames =
                UAConstants.SELECTED_UA_DIMENSIONS.stream()
                        .map(d -> d.getName())
                        .collect(Collectors.toList());
        final List<String> metricNames =
                UAConstants.SELECTED_UA_METRICS.stream()
                        .map(m -> m.getAlias())
                        .collect(Collectors.toList());
        final List<String> allNames = new ArrayList<>(dimensionNames.size() + metricNames.size());
        allNames.addAll(UAConstants.BASE_COLUMN_NAMES);
        allNames.addAll(dimensionNames);
        allNames.addAll(metricNames);

        writer.setHeaders(allNames.toArray(new String[0]));
        writer.setValueCollector(
                (record, rowValues) -> {
                    final List<String> values = new ArrayList<>();
                    // BASE_COLUMN_NAMES
                    values.add(record.getProperty(AccountIdFileColumnHeader.GA4_ACCOUNT_ID));
                    values.add(record.getProperty(AccountIdFileColumnHeader.UA_ACCOUNT_ID));
                    values.add(Instant.now().toString());
                    // VALUES FROM UA
                    values.addAll(rowValues);

                    return values;
                }
        );

        executeReportForAccounts(accounts, writer, config);

        writer.close();
    }

    private void executeReportForAccounts(final List<AccountIdFileRecord> accountIds, UACSVAccountReportWriter writer,
                                          UAReportConfig config)
            throws IOException, GoogleAnalyticsDownloaderException  {
        final AccessToken accessToken = refreshAccessToken();
        final GoogleCredential credentials = new GoogleCredential().setAccessToken(accessToken.getTokenValue());

        // Construct the Analytics Reporting service object.
        final AnalyticsReporting service =
                new AnalyticsReporting.Builder(
                        getHttpTransport(),
                        GsonFactory.getDefaultInstance(),
                        setHttpTimeout(credentials)
                )
                .setApplicationName(APPLICATION_NAME).build();

        DateRange dateRange = new DateRange();
        dateRange.setStartDate(config.getReportStartDate().toString());
        dateRange.setEndDate(config.getReportEndDate().toString());
        final List<DateRange> allDateRanges = Arrays.asList(dateRange);

        String uaAccountId = null;
        for (AccountIdFileRecord account : accountIds) {
            try {
                uaAccountId = account.getProperty(AccountIdFileColumnHeader.UA_ACCOUNT_ID);
                // Some of the accounts do not have a UA account ID
                if ((uaAccountId == null) || (uaAccountId.length() == 0)) {
                    continue;
                }

                // Create the ReportRequest object.
                ReportRequest request = new ReportRequest()
                        .setViewId(uaAccountId)
                        .setDateRanges(allDateRanges)
                        .setMetrics(UAConstants.SELECTED_UA_METRICS)
                        .setDimensions(UAConstants.SELECTED_UA_DIMENSIONS);

                ArrayList<ReportRequest> requests = new ArrayList<ReportRequest>();
                requests.add(request);

                // Create the GetReportsRequest object.
                GetReportsRequest getReport = new GetReportsRequest()
                        .setReportRequests(requests);

                // Call the batchGet method.
                GetReportsResponse response = service.reports().batchGet(getReport).execute();

                for (Report report: response.getReports()) {
                    ColumnHeader header = report.getColumnHeader();
                    List<String> dimensionHeaders = header.getDimensions();
                    List<MetricHeaderEntry> metricHeaders = header.getMetricHeader().getMetricHeaderEntries();
                    List<ReportRow> rows = report.getData().getRows();

                    if (rows == null) {
                        StatbidMessageLogger.logInfo(
                                GoogleAnalyticsDownloader.SERVICE_NAME,
                                config.getReportingClassName(),
                                String.format("No data found for UA account %s", uaAccountId)
                        );
                        continue;
                    }

                    for (ReportRow row : rows) {
                        List<String> dimensions = row.getDimensions();
                        List<DateRangeValues> metrics = row.getMetrics();

                        for (int drvIndex = 0; drvIndex < metrics.size(); drvIndex++) {
                            final List<String> rowValues = new ArrayList<>();

                            final DateRangeValues drv = metrics.get(drvIndex);
                            rowValues.add(allDateRanges.get(drvIndex).getStartDate());
                            rowValues.add(allDateRanges.get(drvIndex).getEndDate());

                            for (Dimension d : UAConstants.SELECTED_UA_DIMENSIONS) {
                                final int idx = dimensionHeaders.indexOf(d.getName());
                                rowValues.add(dimensions.get(idx));
                            }

                            // Note that this code only assumes ONE date range (as set when the report was
                            // created), so the index of the list of values for the date range is always 0.
                            final List<String> values = drv.getValues();
                            for (Metric m : UAConstants.SELECTED_UA_METRICS) {
                                OptionalInt indexOpt = IntStream.range(0, metricHeaders.size())
                                        .filter(i -> m.getAlias().equals(metricHeaders.get(i).getName()))
                                        .findFirst();
                                if (indexOpt.isPresent()) {
                                    rowValues.add(values.get(indexOpt.getAsInt()));
                                } else {
                                    throw new GoogleAnalyticsDownloaderException(
                                            String.format("Unable to find data values for metric %s in results for UA account %s",
                                                    m.getAlias(), uaAccountId)
                                    );
                                }
                            }

                            writer.writeLine(account, rowValues);
                        }
                    }
                }
            } catch (Exception e) {
                if (e instanceof PermissionDeniedException ||
                        (e instanceof GoogleJsonResponseException &&
                                ((GoogleJsonResponseException)e).getStatusCode() == 403)
                ) {
                    StatbidMessageLogger.logInfo(
                            GoogleAnalyticsDownloader.SERVICE_NAME,
                            config.getReportingClassName(),
                            String.format("User does not have permission for UA account %s.", uaAccountId)
                    );
                    continue;
                }
                else {
                    throw new GoogleAnalyticsDownloaderException(
                            String.format(
                                    "ERROR: An error occurred while writing data for account %s the output CSV file: %s",
                                    uaAccountId, e.getMessage()
                            )
                    );
                }
            }
        }
    }

}
