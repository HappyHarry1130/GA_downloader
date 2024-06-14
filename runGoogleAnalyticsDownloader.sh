#!/bin/bash

# Navigate to the downloads folder
cd /downloads/ga_downloader

# Authenticate as the "feed-downloader" service account
gcloud auth activate-service-account --key-file=../feed-storage-94d94e70e271.json

# Get a current date/time stamp, and set file names for the various outputs.
DATETIME=$(date -Iseconds)
# Set a path in GCS for the output from this routine
GCS_PATH=gs://statbid/googleanalytics/ga4


# IMPORTANT: The GA4_STATS_FILE variable must match the output file names in the
# configuration.
GA4_STATS_FILE=GA4Report.csv
GA4_STATS_FILE_LOCAL_PATH=$PWD/reports/$GA4_STATS_FILE
GA4_STATS_FILE_REMOTE_PATH=$GCS_PATH/$GA4_STATS_FILE.$DATETIME

LOG_FILE=GoogleAnalyticsDownloader_$DATETIME.log
LOG_FILE_LOCAL_PATH=$PWD/reports/$LOG_FILE
LOG_FILE_REMOTE_PATH=$GCS_PATH/$LOG_FILE


# Get analytics from GA4 and stash them in Cloud Storage.
java -jar ./target/GoogleAnalyticsDownloader-0.1-SNAPSHOT.jar \
        -c ./config.json \
         1> $LOG_FILE_LOCAL_PATH 2>&1

ERROR_COUNT=$(cat $LOG_FILE_LOCAL_PATH | grep -ic error)
if [ $ERROR_COUNT -eq 0 ]; then
        gsutil cp $GA4_STATS_FILE_LOCAL_PATH $GA4_STATS_FILE_REMOTE_PATH >> $LOG_FILE_LOCAL_PATH 2>&1
fi

# Determine if any errors occurred during the GA4 data download process.
ERROR_COUNT=$(cat $LOG_FILE_LOCAL_PATH | grep -ic error)
if [ $ERROR_COUNT -eq 0 ]; then
        bq load --source_format=CSV --skip_leading_rows=1 --location=us-west2 --allow_jagged_rows=true \
                ga_analytics.ga4_analytics_stats $GA4_STATS_FILE_REMOTE_PATH >> $LOG_FILE_LOCAL_PATH 2>&1
fi

ERROR_COUNT=$(cat $LOG_FILE_LOCAL_PATH | grep -ic error)
if [ $ERROR_COUNT -ne 0 ]; then
        echo "One or more errors occurred during Google Analytics (GA4) download processing - check the $LOG_FILE_REMOTE_PATH file for details."
fi

gsutil cp $LOG_FILE_LOCAL_PATH $LOG_FILE_REMOTE_PATH

# delete the stats and log files.
rm $GA4_STATS_FILE_LOCAL_PATH
rm $LOG_FILE_LOCAL_PATH