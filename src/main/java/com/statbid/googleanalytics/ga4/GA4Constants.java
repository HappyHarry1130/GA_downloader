package com.statbid.googleanalytics.ga4;

import com.google.analytics.data.v1beta.Metric;
import com.google.analytics.data.v1beta.Dimension;

import java.util.Arrays;
import java.util.List;

/**
 * See https://developers.google.com/analytics/devguides/reporting/data/v1/api-schema#dimensions for details.
 */
public class GA4Constants {

    // These columns should be included in every report
    public static final List<String> BASE_COLUMN_NAMES =  Arrays.asList( "accountId", "ingestionDateTime" );

    public static final List<Dimension> SELECTED_GA4_DIMENSIONS = Arrays.asList(
            Dimension.newBuilder().setName("date").build(),
            Dimension.newBuilder().setName("sessionSource").build(),
            Dimension.newBuilder().setName("sessionMedium").build(),
            Dimension.newBuilder().setName("landingPage").build()
    );

    public static final List<Metric> SELECTED_GA4_METRICS = Arrays.asList(
            Metric.newBuilder().setName("totalUsers").build(),
            Metric.newBuilder().setName("sessions").build(),
            Metric.newBuilder().setName("transactions").build(),
            Metric.newBuilder().setName("totalRevenue").build(),
            Metric.newBuilder().setName("bounceRate").build(),
            Metric.newBuilder().setName("averageSessionDuration").build(),
            Metric.newBuilder().setName("conversions").build(),
            Metric.newBuilder().setName("engagedSessions").build(),
            Metric.newBuilder().setName("eventCount").build()

    );

    public static final List<Dimension> SELECTED_UA_DIMENSIONS = Arrays.asList(
            Dimension.newBuilder().setName("ga:date").build(),
            Dimension.newBuilder().setName("ga:source").build(),
            Dimension.newBuilder().setName("ga:sourceMedium").build(),
            Dimension.newBuilder().setName("ga:landingPagePath").build()
    );

    public static final List<Metric> SELECTED_UA_METRICS = Arrays.asList(
//            Metric.newBuilder().setName("totalUsers").build(),
            Metric.newBuilder().setName("ga:sessions").build(),
            Metric.newBuilder().setName("ga:transactions").build(),
//            Metric.newBuilder().setName("totalRevenue").build(),
            Metric.newBuilder().setName("ga:bounceRate").build(),
            Metric.newBuilder().setName("ga:avgSessionDuration").build(),
//            Metric.newBuilder().setName("conversions").build(),
//            Metric.newBuilder().setName("engagedSessions").build(),
            Metric.newBuilder().setName("ga:totalEvents").build(),
            Metric.newBuilder().setName("ga:uniqueEvents").build()

    );
}
