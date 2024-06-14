package com.statbid.googleanalytics;

import java.io.File;
import java.text.MessageFormat;

public class ReportBaseConfigValidator implements ConfigValidator {

    public void validateConfig(final ReportBaseConfig config) throws GoogleAnalyticsDownloaderConfigException {
        // Confirm that the download folder exists; otherwise, exit.
        File dir = new File(config.getReportFileName()).getParentFile();
        if (!dir.exists()) {
            throw new GoogleAnalyticsDownloaderConfigException("*** ERROR: The output folder does not exist. Ensure that the folder exists and try again.");
        }

        // Confirm that the account file exists; otherwise, exit.
        File acctFile = new File(config.getAccountFileName());
        if (!acctFile.exists()) {
            throw new GoogleAnalyticsDownloaderConfigException("*** ERROR: The accounts file does not exist. Ensure that the file exists and try again.");
        }
    }
}
