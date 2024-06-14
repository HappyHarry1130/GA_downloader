WITH conversions AS (
    SELECT 
        user_pseudo_id, 
        (SELECT value.int_value FROM unnest(event_params) WHERE key = 'ga_session_id') AS ga_session_id,
        timestamp_micros(MIN(event_timestamp)) AS session_start_timestamp,
        SUM(ecommerce.purchase_revenue) AS revenue,
        MAX(ecommerce.transaction_id) AS transaction_id,
        DATE(timestamp_micros(MIN(event_timestamp))) AS conversion_date
    FROM `feed-storage.analytics_314664061.events_*`
    WHERE event_name = 'purchase'
    GROUP BY 1, 2
),

currentSessions AS (
    SELECT 
        user_pseudo_id, 
        ga_session_id, 
        IF(gclid IS NOT NULL, 'cpc', medium) AS medium, 
        MIN(session_start_timestamp) AS session_start_timestamp 
    FROM (
        SELECT 
            user_pseudo_id, 
            (SELECT value.int_value FROM unnest(event_params) WHERE key = 'ga_session_id') AS ga_session_id,
            collected_traffic_source.manual_medium AS medium,
            collected_traffic_source.gclid AS gclid,
            timestamp_micros(event_timestamp) AS session_start_timestamp
        FROM `feed-storage.analytics_314664061.events_*` 
        WHERE event_name = 'session_start'
    )
    GROUP BY 1, 2, 3
),

precedingSessions AS (
    SELECT 
        user_pseudo_id, 
        old_ga_session_id, 
        IF(gclid IS NOT NULL, 'cpc', old_medium) AS old_medium 
    FROM (
        SELECT 
            user_pseudo_id, 
            (SELECT value.int_value FROM unnest(event_params) WHERE key = 'ga_session_id') AS old_ga_session_id,  
            FIRST_VALUE(collected_traffic_source.manual_medium) OVER (PARTITION BY user_pseudo_id, (SELECT value.int_value FROM unnest(event_params) WHERE key = 'ga_session_id') ORDER BY event_timestamp) AS old_medium,
            FIRST_VALUE(collected_traffic_source.gclid) OVER (PARTITION BY user_pseudo_id, (SELECT value.int_value FROM unnest(event_params) WHERE key = 'ga_session_id') ORDER BY event_timestamp) AS gclid
        FROM `feed-storage.analytics_314664061.events_*` 
        WHERE event_name NOT IN ('session_start', 'first_visit')
    )
),

interactions AS (
    SELECT 
        user_pseudo_id, 
        ga_session_id, 
        IF(medium IS NULL, 
            (SELECT old_medium FROM UNNEST(old_sessions) WHERE old_medium IS NOT NULL ORDER BY old_ga_session_id DESC LIMIT 1), 
            medium) AS medium,
        session_start_timestamp 
    FROM (
        SELECT 
            user_pseudo_id, 
            ga_session_id, 
            medium, 
            session_start_timestamp, 
            ARRAY_AGG(STRUCT(old_ga_session_id, old_medium)) AS old_sessions 
        FROM currentSessions 
        LEFT OUTER JOIN precedingSessions USING(user_pseudo_id)
        WHERE old_ga_session_id <= ga_session_id
        GROUP BY 1, 2, 3, 4
    )
),

base AS (
    SELECT 
        interactions.user_pseudo_id, 
        interactions.ga_session_id, 
        interactions.medium, 
        interactions.session_start_timestamp,  
        conversions.session_start_timestamp AS conversion_timestamp, 
        conversions.revenue, 
        conversions.transaction_id, 
        conversions.conversion_date,
        COUNT(*) OVER (PARTITION BY conversions.transaction_id) AS totalInteractions,
        ROW_NUMBER() OVER (PARTITION BY conversions.transaction_id ORDER BY interactions.session_start_timestamp) AS interactionNumber,
        ROW_NUMBER() OVER (PARTITION BY conversions.transaction_id ORDER BY interactions.session_start_timestamp DESC) AS interactionNumber_DESC
    FROM conversions
    LEFT OUTER JOIN interactions USING (user_pseudo_id)
    WHERE interactions.session_start_timestamp <= conversions.session_start_timestamp 
    AND interactions.session_start_timestamp > TIMESTAMP_SUB(conversions.session_start_timestamp, INTERVAL 30 DAY)
),

firstTouchAttr AS (
    SELECT 
        medium, 
        conversion_date, 
        SUM(revenue) AS revenueFirstTouch, 
        COUNT(transaction_id) AS conversionsFirstTouch
    FROM base 
    WHERE interactionNumber = 1
    GROUP BY 1, 2
)

SELECT * 
FROM firstTouchAttr
ORDER BY conversion_date;
