package com.statbid.googleanalytics.ua;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.statbid.googleanalytics.ConfigValidator;
import com.statbid.googleanalytics.ReportBaseConfig;

/**
 * This class represents the deserialized version of the Campaign Report config.
 */
@JsonIgnoreProperties  // ignore everything not explicitly annotated
public class UAReportConfig extends ReportBaseConfig {
    private static final String REPORTING_CLASS_NAME = "com.statbid.googleanalytics.ua.UAReportDownloader";

    public UAReportConfig() {
        super(REPORTING_CLASS_NAME);
    }

    @Override
    public ConfigValidator getValidator() {
        return new UAReportConfigValidator();
    }
}


