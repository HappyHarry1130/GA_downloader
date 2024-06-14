package com.statbid.googleanalytics.ua;

import com.statbid.googleanalytics.ConfigValidator;
import com.statbid.googleanalytics.GoogleAnalyticsDownloaderConfigException;
import com.statbid.googleanalytics.ReportBaseConfig;
import com.statbid.googleanalytics.ReportBaseConfigValidator;
import com.statbid.googleanalytics.ga4.GA4ReportConfig;

public class UAReportConfigValidator implements ConfigValidator {
    public void validateConfig(final ReportBaseConfig baseConfig) throws GoogleAnalyticsDownloaderConfigException {
        if (!(baseConfig instanceof UAReportConfig)) {
            throw new GoogleAnalyticsDownloaderConfigException("*** ERROR: The configuration provided to UAReportConfig is not correct.");
        }

        UAReportConfig config = (UAReportConfig) baseConfig;

        // Validate the superclass
        ConfigValidator validator = new ReportBaseConfigValidator();
        validator.validateConfig(config);
    }
}
