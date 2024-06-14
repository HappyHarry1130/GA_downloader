package com.statbid.googleanalytics.ga4;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.statbid.googleanalytics.ConfigValidator;
import com.statbid.googleanalytics.ReportBaseConfig;


/**
 * This class represents the deserialized version of the Campaign Report config.
 */
@JsonIgnoreProperties  // ignore everything not explicitly annotated
//@JsonTypeName("CampaignReport")
public class GA4ReportConfig extends ReportBaseConfig {
    private static final String REPORTING_CLASS_NAME = "com.statbid.googleanalytics.ga4.GA4ReportDownloader";

    public GA4ReportConfig() {
        super(REPORTING_CLASS_NAME);
    }

    @Override
    public ConfigValidator getValidator() {
        return new GA4ReportConfigValidator();
    }
}

