package com.statbid.googleanalytics.ua;

import com.google.api.services.analyticsreporting.v4.model.Metric;
import com.google.api.services.analyticsreporting.v4.model.Dimension;

import java.util.Arrays;
import java.util.List;

/**
 * See https://developers.google.com/analytics/devguides/reporting/data/v1/api-schema#dimensions for details.
 */
public class UAConstants {

    // These columns should be included in every report
    public static final List<String> BASE_COLUMN_NAMES =
            Arrays.asList( "gaAccountId", "uaAccountId", "ingestionDateTime", "dateRangeStart", "dateRangeEnd" );

    public static final List<Dimension> SELECTED_UA_DIMENSIONS = Arrays.asList(
            new Dimension().setName("ga:date"),
            new Dimension().setName("ga:source"),
            new Dimension().setName("ga:sourceMedium"),
            new Dimension().setName("ga:landingPagePath")
    );

    public static final List<Metric> SELECTED_UA_METRICS = Arrays.asList(
            new Metric()
                    .setExpression("ga:sessions")
                    .setAlias("sessions"),
            new Metric()
                    .setExpression("ga:transactions")
                    .setAlias("transactions"),
            new Metric()
                    .setExpression("ga:bounceRate")
                    .setAlias("bounceRate"),
            new Metric()
                    .setExpression("ga:avgSessionDuration")
                    .setAlias("avgSessionDuration"),
            new Metric()
                    .setExpression("ga:totalEvents")
                    .setAlias("totalEvents"),
            new Metric()
                    .setExpression("ga:uniqueEvents")
                    .setAlias("uniqueEvents"),
            new Metric()
                    .setExpression("ga:users")
                    .setAlias("users"),
            new Metric()
                    .setExpression("ga:transactionRevenue")
                    .setAlias("transactionRevenue")
    );
}

