package com.statbid.googleanalytics;

public interface ConfigValidator {
    public void validateConfig(final ReportBaseConfig config) throws GoogleAnalyticsDownloaderConfigException;
}
