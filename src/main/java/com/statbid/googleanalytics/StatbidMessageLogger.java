package com.statbid.googleanalytics;

import com.google.cloud.MonitoredResource;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import com.google.cloud.logging.Payload.JsonPayload;
import com.google.cloud.logging.Severity;
import com.google.common.collect.ImmutableMap;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;

public class StatbidMessageLogger {
    private static final String LOG_NAME = "downloader-messages";

    public static void logInfo(final String service, final String reportName, final String message) {
        // Instantiates a client
        Map<String, String> payload = ImmutableMap.of(
                "message", message);
        logMessageToGoogleCloudLogging(service, reportName, payload, Severity.INFO);
    }

    public static void logWarning(final String service, final String reportName, final String message) {
        // Instantiates a client
        Map<String, String> payload = ImmutableMap.of(
                "message", message);
        logMessageToGoogleCloudLogging(service, reportName, payload, Severity.WARNING);
    }

    public static void logError(final String service, final String reportName, final String message) {
        // Instantiates a client
        Map<String, String> payload = ImmutableMap.of(
                "@type", "type.googleapis.com/google.devtools.clouderrorreporting.v1beta1.ReportedErrorEvent",
                "message", message);
        logMessageToGoogleCloudLogging(service, reportName, payload, Severity.ERROR);
    }

    public static void logFatal(final String service, final String reportName, final String message) {
        // Instantiates a client
        Map<String, String> payload = ImmutableMap.of(
                "@type", "type.googleapis.com/google.devtools.clouderrorreporting.v1beta1.ReportedErrorEvent",
                "message", message);
        logMessageToGoogleCloudLogging(service, reportName, payload, Severity.ERROR);
    }

    public static void logFatal(final String service, final String reportName, final Throwable throwable,
                                final String message) {
        final StringWriter stringWriter = new StringWriter();
        final PrintWriter stackTraceWriter = new PrintWriter(stringWriter, true);
        throwable.printStackTrace(stackTraceWriter);
        stackTraceWriter.close();

        // Instantiates a client
        Map<String, String> payload = ImmutableMap.of(
                "@type", "type.googleapis.com/google.devtools.clouderrorreporting.v1beta1.ReportedErrorEvent",
                "message", message,
                "stack_trace", stringWriter.toString());
        logMessageToGoogleCloudLogging(service, reportName, payload, Severity.ERROR);
    }

    private static Logging logSetup() {
        return LoggingOptions.getDefaultInstance().getService();
    }

    private static void logMessageToGoogleCloudLogging(final String service, final String reportName,
                                                       final Map<String, String> payload, final Severity severity) {
        try (Logging logging = StatbidMessageLogger.logSetup()) {
            LogEntry entry =
                    LogEntry.newBuilder(JsonPayload.of(payload))
                            .setLabels(ImmutableMap.of(
                                    "service", service, "reportName", reportName
                            ))
                            .setSeverity(severity)
                            .setLogName(LOG_NAME)
//                            .setResource(MonitoredResource.newBuilder("gce_instance").build())
                            .setResource(MonitoredResource.newBuilder("global").build())
                            .build();

            // Writes the log entry asynchronously
            logging.write(Collections.singleton(entry));

            // Optional - flush any pending log entries just before Logging is closed
            logging.flush();
            System.out.printf("Wrote to %s\n", LOG_NAME);
        }
        catch (Exception e) {
            System.out.println(String.format("Unable to log to Google Cloud Logging: %s", e.getMessage()));
        }
    }
}