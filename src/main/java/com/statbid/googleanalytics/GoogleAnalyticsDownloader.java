package com.statbid.googleanalytics;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * This code follows the same general pattern as used for
 * the Bing downloader
 **/
public class GoogleAnalyticsDownloader {
    public static final String SERVICE_NAME = "GoogleAnalyticsDownloader";

    private static final String CONFIG_FILE_OPTION = "c";

    public static void main(String[] args) throws IOException {
        // Build the command line options definitions and parse the args
        final Options options = buildCommandLineOptions();

        String configFileName = null;
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);
            configFileName = cmd.getOptionValue(CONFIG_FILE_OPTION);
        }
        catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("GoogleAnalyticsDownloader", options);
            StatbidMessageLogger.logFatal(
                    SERVICE_NAME,
                    "Command line parsing",
                    "ERROR: One or more command line options was incorrect");
            System.exit(1);
        }

        // Read the configuration file
        GoogleAnalyticsReportConfig config = null;
        try {
            config = readConfiguration(configFileName);
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            StatbidMessageLogger.logFatal(
                    SERVICE_NAME,
                    "Configuration file parsing",
                    String.format("ERROR: Unable to read configuration file: %s", e.getMessage()));
            System.exit(1);
        }

        // Invoke each reporting module to validate its configuration and execute.
        String reportingClassName = "unknown";
        try {
            final List<ReportBaseConfig> reportConfigs = config.getReportConfig().getReportConfigurations();
            for (ReportBaseConfig rc : reportConfigs) {
                // Figure out which class uses this config to do reporting
                reportingClassName = rc.getReportingClassName();
                final Class reportingClass = Class.forName(reportingClassName);

                System.out.println("Now generating report: " + reportingClass.getSimpleName());

                // Get the constructor and create an instance of the class
                final Constructor constructor =
                    reportingClass.getDeclaredConstructor(GoogleApiConfig.class, ReportBaseConfig.class, File.class);
                final GoogleAnalyticsReportingModule reporter =
                        (GoogleAnalyticsReportingModule) constructor.newInstance(
                                config.getGoogleApiConfig(),
                                rc,
                                config.getReportConfig().getReportOutputDir()
                        );
                // Invoke the execute() method to run the report.
                final Method execMethod =
                        reportingClass.getDeclaredMethod(GoogleAnalyticsReportingModule.DEFAULT_EXECUTION_METHOD_NAME);
                execMethod.invoke(reporter);
            }
        }
        catch (Throwable e) {
            final String msg = (e instanceof InvocationTargetException)  ? e.getCause().getMessage() : e.getMessage();

            System.err.println(msg);
            StatbidMessageLogger.logFatal(
                    SERVICE_NAME,
                    reportingClassName,
                    e,
                    String.format("ERROR: Error while running report: %s", msg));
            System.exit(1);
        }
        finally {
            final String msg = "Google Analytics 4 reporting complete";
            StatbidMessageLogger.logInfo(
                    SERVICE_NAME,
                    "Google Analytics 4 Reporting",
                    msg);
            System.out.println(msg);
        }
    }


    private static Options buildCommandLineOptions() {
        Options options = new Options();
        options.addOption(
            Option.builder(CONFIG_FILE_OPTION)
                .argName("Configuration file name")
                .longOpt("configFile")
                .desc("Full path to configuration file")
                .hasArg()
                .required()
                .build()
        );

        return options;
    }

    /**
     *
     */
    private static GoogleAnalyticsReportConfig readConfiguration(final String configFileName)
            throws GoogleAnalyticsDownloaderConfigException {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(new URL("file:" + configFileName), GoogleAnalyticsReportConfig.class);
        } catch (IOException e) {
            throw new GoogleAnalyticsDownloaderConfigException(
                String.format("Error reading configuration file: %s: %s", configFileName, e.getMessage()),
                e
            );
        }
    }

}
