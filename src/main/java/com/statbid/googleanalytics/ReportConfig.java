package com.statbid.googleanalytics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.statbid.googleanalytics.ga4.GA4ReportConfig;
import com.statbid.googleanalytics.ua.UAReportConfig;

import java.io.File;
import java.util.List;

/**
 * This class represents the deserialized version of the Campaign Report config.
 */
@JsonIgnoreProperties  // ignore everything not explicitly annotated
public class ReportConfig {
    @JsonProperty("reportOutputDir")
    public String reportOutputDir;

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = GA4ReportConfig.class, name = "GA4ReportConfig"),
            @JsonSubTypes.Type(value = UAReportConfig.class, name = "UAReportConfig"),
    })

    @JsonProperty("reports")
    public ReportBaseConfig[] reportConfigs;

    public File getReportOutputDir() {
        return new File(reportOutputDir);
    }

    public List<ReportBaseConfig> getReportConfigurations() {
        return List.of(reportConfigs);
    }


}



