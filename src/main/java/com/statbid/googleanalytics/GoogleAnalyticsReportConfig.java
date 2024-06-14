package com.statbid.googleanalytics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents the deserialized version of the config.json file.
 */
@JsonIgnoreProperties  // ignore everything not explicitly annotated
public class GoogleAnalyticsReportConfig {

    @JsonProperty("googleApiConfig")
    public GoogleApiConfig googleApiConfig;
    @JsonProperty("reportConfig")
    public ReportConfig reportConfig;

    public GoogleApiConfig getGoogleApiConfig() {
        return googleApiConfig;
    }
    public ReportConfig getReportConfig() { return reportConfig; }
}
