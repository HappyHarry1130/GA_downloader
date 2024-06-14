package com.statbid.googleanalytics;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.time.LocalDate;

public class ReportBaseConfig {
    private static final Integer DEFAULT_DAYS_TO_REPORT = 1;

    @JsonProperty("type")
    private String type;
    @JsonProperty("accountFileName")
    private String accountFileName;
    @JsonProperty("reportFileName")
    private String reportFileName;
    @JsonProperty("daysToReport")
    private Integer daysToReport;
    private LocalDate fromDate;

    private String reportingClassName;

    protected ReportBaseConfig(final String reportingClassName) {
        this.reportingClassName = reportingClassName;
        this.fromDate = LocalDate.now();
        this.daysToReport = DEFAULT_DAYS_TO_REPORT;
    }

    public String getType() { return type; }

    public String getAccountFileName() {
        return accountFileName;
    }

    public String getReportFileName() {
        return reportFileName;
    }

    public String getReportingClassName() { return reportingClassName; }

    @JsonSetter("fromDate")
    public void setFromDate(final String date)  {
        fromDate = LocalDate.parse(date);
    }

    public LocalDate getReportEndDate() {
        if (fromDate == null) {
            fromDate = LocalDate.now();
        }
        return fromDate;
    }

    public LocalDate getReportStartDate() {
        final LocalDate start = getReportEndDate();
        return start.minusDays(daysToReport);
    }

    public ConfigValidator getValidator() {
        return new ReportBaseConfigValidator();
    }


    @Override
    public String toString() {
        return String.format(
                "Account file name: %1$s, report file name: %2$s, days to report: %3$d, from date: %4$tm-%4$td-%4$tY",
                accountFileName, reportFileName, daysToReport, fromDate
        );
    }
}
