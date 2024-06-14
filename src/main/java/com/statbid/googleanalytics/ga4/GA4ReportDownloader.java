package com.statbid.googleanalytics.ga4;

import com.google.analytics.data.v1beta.*;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.PermissionDeniedException;
import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.OAuth2Credentials;

import com.statbid.googleanalytics.*;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * This code was derived in part from the "GetCampaignsByLabel.java" example at
 * https://github.com/googleads/google-ads-java and follows a pattern also found in the
 * BingDownloader
 **/
public class GA4ReportDownloader extends GoogleAnalyticsReportingModule {

    private GA4ReportConfig config;

    private static final long PAGE_SIZE = 10000L;


    public GA4ReportDownloader(final GoogleApiConfig apiConfig, final ReportBaseConfig reportConfig,
                               final File reportOutputDir)
            throws GoogleAnalyticsDownloaderConfigException {
        super(apiConfig, reportOutputDir);
        this.config = validateConfiguration(reportConfig, reportOutputDir);
    }

    public GA4ReportConfig validateConfiguration(final ReportBaseConfig baseConfig, final File reportOutputDir)
            throws GoogleAnalyticsDownloaderConfigException {
        final ConfigValidator validator = baseConfig.getValidator();
        validator.validateConfig(baseConfig);

        if (!reportOutputDir.exists()) {
            throw new GoogleAnalyticsDownloaderConfigException("*** ERROR: The specified report output directory does not exist.");
        }

        return (GA4ReportConfig) baseConfig;
    }



    public void execute() throws IOException, GoogleAnalyticsDownloaderException  {
        // Get the requested set of account IDs from the CSV.
        final List<AccountIdFileRecord> accountIds =
                getAccountDetails(config.getAccountFileName());

        final File reportFile = new File(config.getReportFileName());
        if (reportFile.exists()) {
            System.out.println(
                    String.format("*** WARNING: Report output file %s already exists - it will be overwritten",
                            config.getReportFileName())
            );
            reportFile.delete();
        }

        final G4CSVAccountReportWriter writer = new G4CSVAccountReportWriter();
        writer.initialize(config.getReportFileName());

        final List<String> dimensionNames =
                GA4Constants.SELECTED_GA4_DIMENSIONS.stream()
                                .map(d -> d.getName())
                                .collect(Collectors.toList());
        final List<String> metricNames =
                GA4Constants.SELECTED_GA4_METRICS.stream()
                        .map(m -> m.getName())
                        .collect(Collectors.toList());
        final List<String> allNames = new ArrayList<>(dimensionNames.size() + metricNames.size());
        allNames.addAll(GA4Constants.BASE_COLUMN_NAMES);
        allNames.addAll(dimensionNames);
        allNames.addAll(metricNames);

        writer.setHeaders(allNames.toArray(new String[0]));
        writer.setValueCollector(
                (account, ga4Row) -> {
                    final List<String> values = new ArrayList<>();
                    // BASE_COLUMN_NAMES
                    values.add(account.getProperty(AccountIdFileColumnHeader.GA4_ACCOUNT_ID));
                    values.add(Instant.now().toString());

                    // For both dimensions and metrics we assume that the values in the list are in the same order
                    // as they are in the headers.  This seems reasonable given the docs at
                    // https://cloud.google.com/java/docs/reference/google-analytics-data/latest/com.google.analytics.data.v1beta.Row
                    final List<DimensionValue> dimensionValues = ga4Row.getDimensionValuesList();
                    for (DimensionValue value : dimensionValues) {
                        values.add(value.getValue());
                    }

                    final List<MetricValue> metricValues = ga4Row.getMetricValuesList();
                    for (MetricValue value : metricValues) {
                        values.add(value.getValue());
                    }

                    return values;
                }
        );

        executeReportForAccounts(accountIds, writer, config);

        writer.close();
    }

    private void executeReportForAccounts(final List<AccountIdFileRecord> accountIds, G4CSVAccountReportWriter writer,
                                            GA4ReportConfig config) throws GoogleAnalyticsDownloaderException  {

        BetaAnalyticsDataSettings betaAnalyticsDataSettings = null;
        try {
            AccessToken thisToken = refreshAccessToken();
            Credentials credentials = OAuth2Credentials.newBuilder().setAccessToken(thisToken).build();
            betaAnalyticsDataSettings =
                    BetaAnalyticsDataSettings.newBuilder()
                            .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                            .build();
        } catch (IOException e) {
            throw new GoogleAnalyticsDownloaderException(
                    String.format(
                            "ERROR: An error occurred while generating Google API credentials: %s",
                            e.getMessage()
                    )
            );
        }

        final DateRange dateRange = DateRange.newBuilder()
                .setStartDate(config.getReportStartDate().toString())
                .setEndDate(config.getReportEndDate().toString())
                .build();

        String gaAccountId = null;
        for (AccountIdFileRecord account : accountIds) {
            gaAccountId = account.getProperty(AccountIdFileColumnHeader.GA4_ACCOUNT_ID);
            // Some of the accounts do not have a UA account ID
            if ((gaAccountId == null) || (gaAccountId.length() == 0)) {
                continue;
            }

            long currentOffset = 0;

            try(
                    BetaAnalyticsDataClient betaAnalyticsDataClient = BetaAnalyticsDataClient.create(betaAnalyticsDataSettings)
            ) {
                RunReportResponse response = null;
                do {
                    RunReportRequest request =
                            RunReportRequest.newBuilder()
                                    .setProperty(String.format("properties/%s", gaAccountId))
                                    .addAllDimensions(GA4Constants.SELECTED_GA4_DIMENSIONS)
                                    .addAllMetrics(GA4Constants.SELECTED_GA4_METRICS)
                                    .addDateRanges(dateRange)
                                    //                                    .setDimensionFilter(FilterExpression.newBuilder().build())
                                    //                                    .setMetricFilter(FilterExpression.newBuilder().build())
                                    .setOffset(currentOffset)
                                    .setLimit(PAGE_SIZE)
                                    //                                    .addAllMetricAggregations(new ArrayList<MetricAggregation>())
                                    //                                    .addAllOrderBys(new ArrayList<OrderBy>())
                                    .setCurrencyCode("USD")
                                    //                                    .setCohortSpec(CohortSpec.newBuilder().build())
                                    .setKeepEmptyRows(true)
                                    .setReturnPropertyQuota(true)
                                    .build();
                    response = betaAnalyticsDataClient.runReport(request);

                    //                    int nTerminationLoops = 0;
                    //                    while (!betaAnalyticsDataClient.isTerminated() && (nTerminationLoops < TERMINATION_WAIT_LOOPS)) {
                    //                        betaAnalyticsDataClient.awaitTermination(TERMINATION_WAIT_TIME_IN_SECONDS, TimeUnit.SECONDS);
                    //                        nTerminationLoops++;
                    //                    }

                    /* NOTE: response.getRowsCount() is the count of rows in THIS response.  Compare to
                       response.getRowCount() (singular word "row") which gives you the total rows across all responses. */
//                    System.out.println(String.format("Account ID %s: %d rows retrieved out of %d total",
//                            gaAccountId, response.getRowsCount(), response.getRowCount()));
//                    System.out.println(String.format("Tokens remaining: remaining per property/day %s, remaining per property/hour %s, remaining per project/property/hour %s",
//                            response.getPropertyQuota().getTokensPerDay(),
//                            response.getPropertyQuota().getTokensPerHour(),
//                            response.getPropertyQuota().getTokensPerProjectPerHour()));
//                    System.out.println(String.format("Has tokens per day: %s, has tokens per hour: %s, has tokens per property/hour: %s",
//                            response.getPropertyQuota().hasTokensPerDay(),
//                            response.getPropertyQuota().hasTokensPerHour(),
//                            response.getPropertyQuota().hasTokensPerProjectPerHour()));
                    for (Row ga4Row : response.getRowsList()) {
                        writer.writeLine(account, ga4Row);
                    }
                    System.out.println(String.format("Finished printing rows for account ID %s. More rows: %s",
                            gaAccountId, (response.getRowsCount() < PAGE_SIZE ? "false" : "true")));

                    currentOffset += PAGE_SIZE;
                } while (response.getRowsCount() >= PAGE_SIZE);
            } catch (Exception e) {
                if (e instanceof PermissionDeniedException ||
                        (e instanceof GoogleJsonResponseException &&
                                ((GoogleJsonResponseException)e).getStatusCode() == 403)
                ) {
                    StatbidMessageLogger.logInfo(
                            GoogleAnalyticsDownloader.SERVICE_NAME,
                            config.getReportingClassName(),
                            String.format("User does not have permission for GA4 account %s.", gaAccountId)
                    );
                    continue;
                }
                else {
                    throw new GoogleAnalyticsDownloaderException(
                            String.format(
                                    "ERROR: An error occurred while writing output for account %s to CSV file: %s",
                                    gaAccountId, e.getMessage()
                            )
                    );
                }
            }
        }
    }
}