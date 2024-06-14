package com.statbid.googleanalytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.*;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;

import com.google.auth.oauth2.AccessToken;

import com.google.api.client.json.gson.GsonFactory;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class GoogleAnalyticsReportingModule {
    public static final String DEFAULT_EXECUTION_METHOD_NAME = "execute";
    protected static final String APPLICATION_NAME = "StatBid_GA4_Downloader";
    protected static final int REQUEST_TIMEOUT = 3 * 60000;

    private ObjectMapper mapper = new ObjectMapper();

    private String refreshToken;
    private GoogleApiConfig apiConfig;
    private File reportOutputDir;
    private HttpTransport httpTransport;
    private HttpRequestFactory httpRequestFactory;


    public abstract ReportBaseConfig validateConfiguration(final ReportBaseConfig baseConfig, final File reportOutputDir)
            throws GoogleAnalyticsDownloaderConfigException;

    public abstract void execute() throws IOException, GoogleAnalyticsDownloaderException;

    protected GoogleAnalyticsReportingModule(final GoogleApiConfig apiConfig, final File reportOutputDir)
            throws GoogleAnalyticsDownloaderConfigException {
        this.apiConfig = apiConfig;
        this.reportOutputDir = reportOutputDir;
        initializeApi();
    }



    /**
     * Initializes an Analytics Reporting API V4 service object.
     *
     * @return An authorized Analytics Reporting API V4 service object.
     * @throws IOException
     * @throws GeneralSecurityException
     */
    protected void initializeApi() throws GoogleAnalyticsDownloaderConfigException {
        try {
            if (!this.apiConfig.getCredentialsFile().exists()) {
                throw new GoogleAnalyticsDownloaderConfigException(
                        String.format("Client secret file file %s not found ",
                                apiConfig.getCredentialsFile().getAbsolutePath())
                );
            }


            this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            // We create a request factory with an HttpRequestInitializer so that we can disable read timeouts
            // (and connect timeouts, if needed)
            this.httpRequestFactory = this.httpTransport.createRequestFactory(
                    new HttpRequestInitializer() {
                        @Override public void initialize(HttpRequest request) {
                            request.setConnectTimeout(REQUEST_TIMEOUT);
                            request.setReadTimeout(REQUEST_TIMEOUT);
                        }
                    });
        } catch (GeneralSecurityException | IOException e) {
            throw new GoogleAnalyticsDownloaderConfigException(
                    String.format("Unable to create HTTP connection: %s", e.getMessage())
            );
        }

        this.refreshToken = readCachedRefreshTokenFromFile();
    }

    protected HttpTransport getHttpTransport() { return this.httpTransport; }
    protected HttpRequestFactory getHttpRequestFactory() { return this.httpRequestFactory; }

    protected HttpRequestInitializer setHttpTimeout(final HttpRequestInitializer requestInitializer) {
        return new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest httpRequest) throws IOException {
                requestInitializer.initialize(httpRequest);
                httpRequest.setConnectTimeout(REQUEST_TIMEOUT);  // 3 minutes connect timeout
                httpRequest.setReadTimeout(REQUEST_TIMEOUT);  // 3 minutes read timeout
            }
        };
    }


    /**
     * Get the cached refresh token
     */
    protected String readCachedRefreshTokenFromFile() throws GoogleAnalyticsDownloaderConfigException {
        if (!apiConfig.getRefreshTokenCache().exists()) {
            throw new GoogleAnalyticsDownloaderConfigException(
                    String.format("Cached refresh token file %s not found ",
                            apiConfig.getRefreshTokenCache().getAbsolutePath())
            );
        }

        try {
            final JsonNode treeRoot = mapper.readTree(apiConfig.getRefreshTokenCache());
            final JsonNode refreshTokenNode = treeRoot.get("refresh_token");
            return refreshTokenNode.asText();
        } catch (IOException e) {
            throw new GoogleAnalyticsDownloaderConfigException(
                    String.format("Unable to read cached refersh token: %s", e.getMessage())
            );
        }
    }

    /**
     * Get an access token
     */
    protected AccessToken refreshAccessToken() throws GoogleAnalyticsDownloaderException {
        try {
            final GsonFactory gsonFactory = GsonFactory.getDefaultInstance();
            final GoogleClientSecrets secret = GoogleClientSecrets.load(
                    gsonFactory,
                    new InputStreamReader(new FileInputStream(apiConfig.credentialsFile))
            );
            final GoogleClientSecrets.Details details = secret.getDetails();

            final HttpRequest request = this.httpRequestFactory.buildPostRequest(
                    new GenericUrl("https://oauth2.googleapis.com/token"),
                    ByteArrayContent.fromString(
                            "application/x-www-form-urlencoded",
                            String.format("grant_type=refresh_token&client_id=%s&client_secret=%s&refresh_token=%s",
                                    details.getClientId(), details.getClientSecret(), this.refreshToken)
                    )
            );
//            request.setContentType("application/www-form-urlencoded");

            final HttpResponse response = request.execute();

            if (!response.isSuccessStatusCode()) {
                throw new GoogleAnalyticsDownloaderException(
                        String.format("Error obtaining Google access token: status code %d, message %s",
                                response.getStatusCode(), response.getStatusMessage())
                );
            }
            final InputStream responseContentStream = response.getContent();
            String responseContent = new String(responseContentStream.readAllBytes(), StandardCharsets.UTF_8);
            final JsonNode treeRoot = mapper.readTree(responseContent);
            final JsonNode accessTokenNode = treeRoot.get("access_token");

            return AccessToken.newBuilder().setTokenValue(accessTokenNode.asText()).build();
        } catch (Exception e) {
            throw new GoogleAnalyticsDownloaderException(
                    String.format("Unable to obtain Google access token: %s", e.getMessage())
            );
        }
    }

    /**
     * Get the account IDs from the specified file.
     */
    protected List<AccountIdFileRecord> getAccountDetails(final String csvFileName)
            throws IOException
    {
        final List<String> accountIds = new ArrayList<>();
        final File csvFile = new File(csvFileName);
        final CSVFormat format = CSVFormat.Builder.create()
                .setDelimiter(',')
                .setQuote('"')
                .setRecordSeparator("\r\n")
                .setIgnoreEmptyLines(false)
                .setHeader()
                .build();

        final CSVParser parser =
                CSVParser.parse(csvFile,
                        Charset.forName("UTF-8"),
                        format);

        final List<AccountIdFileRecord> propsList = new ArrayList<>();
        for (CSVRecord csvRecord : parser) {
            System.out.println(csvRecord.toString());
            final Map<AccountIdFileColumnHeader, String> props = new HashMap<>();
            for (AccountIdFileColumnHeader hdr : AccountIdFileColumnHeader.values()) {
                props.put(hdr, csvRecord.get(hdr.getColumnName()));
            }
            propsList.add(new AccountIdFileRecord(props));
        }

        return propsList;
    }

}

