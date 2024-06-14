package com.statbid.googleanalytics.ga4;

import com.statbid.googleanalytics.ConfigValidator;
import com.statbid.googleanalytics.GoogleAnalyticsDownloaderConfigException;
import com.statbid.googleanalytics.ReportBaseConfig;
import com.statbid.googleanalytics.ReportBaseConfigValidator;

public class GA4ReportConfigValidator implements ConfigValidator {
    public void validateConfig(final ReportBaseConfig baseConfig) throws GoogleAnalyticsDownloaderConfigException {
        if (!(baseConfig instanceof GA4ReportConfig)) {
            throw new GoogleAnalyticsDownloaderConfigException("*** ERROR: The configuration provided to GA4ReportDownloader is not correct.");
        }

        GA4ReportConfig config = (GA4ReportConfig) baseConfig;

        // Validate the superclass
        ConfigValidator validator = new ReportBaseConfigValidator();
        validator.validateConfig(config);
    }
}
