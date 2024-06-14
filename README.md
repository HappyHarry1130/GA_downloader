# Google Analytics Downloader
This utility uses two Google Analytics APIs to download data and make it available in BigQuery for analysis:

- the [Google Analytics 4 Data API](https://cloud.google.com/java/docs/reference/google-analytics-data/latest/overview.html) to download Google Analytics 4 (GA4) data. 
- the [Google Universal Analytics Reporting API](https://developers.google.com/analytics/devguides/reporting/core/v4) to download Universal Analytics (UA) data.

At the time of this writing UA is no longer processing data and is in the process of being deprecated. Support for UA downloads was added primarily to allow archival of UA data prior to its removal from the Google system.
Special scripts and configurations were created to download the UA data (see `ua_backfill.sh` in the `/downloads/ga_downloader` directory on the VM) but UA reports will not be run under normal circumstances.

A configuration file is available that allows you to specify which data is to be downloaded. In both cases the downloaded data will be added to Google Cloud Storage for archival and to BigQuery for analysis.

## Preparing the GCP VM
Before using this tool, you must install Java, Git and the Maven build system on the target virtual machine.  These instructions assume that you know how to
create and configure a Debian virtual machine on GCP; if you need more information, refer to the [Google Cloud Compute Engine documentation](https://cloud.google.com/compute).

### Installing Java
At the time of this writing the most current LTS release of OpenJDK Java is [version 17](https://openjdk.org/projects/jdk/17/). The utility is compiled
for Java 14, so any release after this should work. Follow these steps to install:

- Connect to the VM via SSH.
- Download and install the JDK 17 runtime (JRE) using the command `sudo apt-get install openjdk-17-jre`. This will install the JRE and put it into your path.
- Confirm successful execution of the JRE using the command `java --version`.  You should see information about the runtime version and other build details.
- Download and install Maven using the command `sudo apt-get install maven`.  This will install Maven and put it in your path.
- Confirm successful Maven installation by checking the output of the `mvn --version` command. You should see info about the runtime version and other details.

Installing Git can be somewhat complex, so it's best to review the installation instructions for the [Git command line tool](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git).


## Building and installing the downloader
In the current process, the tool is built and executed on the target machine.

Before following the steps below, ensure that the VM's disk has at least 30 GB available.  All of the Google analytics downloader software will be installed in a directory called `/downloads`; if you do not already 
have a directory with that name on the VM, create on using the command `sudo mkdir /downloads`, then change to that directory using the `cd /downloads` command.

### Download the source code from Git
Navigate into the `/downloads` directory, then download the code from Github using the command `git clone git@github.com:statbid/ga_downloader.git`.     
The repository will be placed in the directory in which the command is executed.

### The build process
To build the system, execute the following command from the "ga_downloader" repository directory:

```shell
mvn clean package
```

This will build the system and produce a self-contained JAR file called `GoogleAnalyticsDownloader-0.1-SNAPSHOT.jar` which can be used to execute the downloader.
The output will be located in the "target" subdirectory.

NOTE: The build process also creates a JAR file called `original-GoogleAnalyticsDownloader-0.1-SNAPSHOT.jar`.  This file should NOT be used; it is an intermediate file created only to facilitate packaging of all system dependencies.

### Running the downloader
There are four files required to run the Google Analytics downloader:

- `GoogleAnalyticsDownloader-0.1-SNAPSHOT.jar` (the actual application JAR built in the previous step)
- `runGoogleAnalyticsDownloader.sh`, the 'bash' script used to run the various steps to download the data and move it to GCS and BigQuery
- `ga_accounts.csv`, which contains both GA4 and UA account IDs for which Google Analytics data are to be retrieved.
- A JSON configuration file (normally `config.json`).  A sample is available in the Github repository.  See [Creating a configuration file](#Creating-a-configuration-file).

The JAR file is created during the build process, and the `.sh` and sample config file are included in the repository you just downloaded.  A sample StatBid account ID is also included in the repository, but
you will likely want to replace/update this file with the most current info. 
Files can be copied to the VM using the "Upload file" feature of GCP's SSH client, the `gcloud` command line utility, or other methods; 
see [Transfer files to Linux VMs](https://cloud.google.com/compute/docs/instances/transfer-files) for details.

Steps for deploying the tool with the GCP browser-based SSH client are shown below.

#### Copy files to the VM (OPTIONAL)
If you want to copy a new `ga_accounts.csv` file to the VM, follow the steps below.

- From the menu in the upper right of the SSH browser window, select "Upload file".  
- Select the "Choose Files..." button. A dialog will open enabling you to specify the location of the `ga_accounts.csv` file on your computer.
- Click the "Upload" button.  The file will be transferred to your home directory on the virtual machine.  (If you're signed in as "analytics@statbid.com" the home directory will be `/home/analytics`.) 
- Copy the file from the home directory to the Google Analytics downloader directory using the `cp ~/ga_accounts.csv /downloads/ga_downloader` command.

#### Prepare the files for execution
- Enter the `cd /downloads` command in the SSH window.  This will take you to the directory where the various tools and utilities are located.
- (OPTIONAL - FIRST TIME INSTALL) Use the command `sudo chown -R downloader1:downloaders ga_downloader` to assign ownership of the `ga_downloader` directory to the `downloader1` user in the `downloaders` group.
- Use the command `cd ga_downloader` to navigate to the downloader's directory.

#### Creating a configuration file
The Google Analytics Downloader requires a configuration file that describes the reports to be run. Sample GA4 and UA report configuration are shown below, and are also included in the repository contents.

For GA4:

```json
{
  "googleApiConfig": {
    "credentialsFile": "./client_secret_18981351914-mrj5e1qcec2mop32bn6tg4pcol6ljta9.apps.googleusercontent.com.json",
    "refreshTokenCache": "./ga_refresh_token.txt"
  },
  "reportConfig": {
    "reportOutputDir": "./reports",
    "reports": [
      {
        "type": "GA4ReportConfig",
        "accountFileName": "./ga_accounts.csv",
        "reportFileName": "./reports/GA4Report.csv",
        "daysToReport": 1
      }
    ]
  }
}
```

For UA:

```json
{
  "googleApiConfig": {
    "credentialsFile": "./client_secret_18981351914-mrj5e1qcec2mop32bn6tg4pcol6ljta9.apps.googleusercontent.com.json",
    "refreshTokenCache": "./ga_refresh_token.txt"
  },
  "reportConfig": {
    "reportOutputDir": "./reports",
    "reports": [
      {
        "type": "UAReportConfig",
        "accountFileName": "./ga_accounts.csv",
        "reportFileName": "./reports/UAReport.csv",
        "daysToReport": 1
      }
    ]
  }
}
```

The "googleApiConfig" section has information that tells the downloader how to authenticate itself to the Google Analytics system.
- `credentialsFile` refers to the Google client secret file used to identify the Statbid downloaders to Google (For historical reasons this is also known as the "GSC Downloader 2" credentials file in Google Cloud IAM.)
- The `refreshTokenCache` parameter points to a file containing the refresh token required to run the downloader. See "Authorizing use of the API", below.

The "reportConfig" section has information that tells the downloader which reports to run and where to put the results.
- The `reportOutputDir` parameter points to the output directory into which the downloader will put its data.  
- The `reports` section details the reports to be run.  This section can have configurations for either GA4 or UA reports (or both). Each report will have the following parameters:
  - The `type` parameter tells the downloader code what sort of report to run.  For the GA4 report this parameter will be "GAReportConfig", and for the UA report this will be "UAReportConfig"
  - `accountFileName` is the name of the file containing the Google Analytics UA and GA4 account IDs to be used in reporting.  See the sample for the layout.
  - `reportFileName` is the full path to the output file.  If this file exists when the utility is run, it will be overwritten.
  - `daysToReport` and `fromDate` specify the interval for which reporting is desired.  The report will contain data starting `daysToReport` days BACKWARDS from `fromDate`.  (If you do not specify a `fromDate` parameter the current date will be used.)

Additional reports can be added in the future.

### Authorizing use of the API
The Google Analytics API requires user authorization in order to run. To grant this authorization, we need to generate a "refresh token" which the API can use to authenticate itself. This is a one-time step that cannot be done in the execution script.

To create a refresh token, use the [Google Developer's Console OAuth Playground](https://developers.google.com/oauthplayground):

- Go to the Google Cloud [APIs and Services "Credentials" page](https://console.cloud.google.com/apis/credentials?project=feed-storage).  Locate the credentials for "GSC Downloader 2"
  and select them to view the details page. Note the authorized redirect URI, the client ID and the client secret.
- Go to the [Google OAuth Playground](https://developers.google.com/oauthplayground/).  From the pane on the left, select "Google Analytics Data API v1beta", then select the
  "https://www.googleapis.com/auth/analytics.readonly" scope. DO NOT CLICK THE "Authorize APIs" BUTTON.
- In the upper right corner, click the gear icon to access the OAuth 2.0 settings. Complete the dialog shown as follows:
  -- OAuth Flow: Server side
  -- OAuth Endpoints: Google
  -- Authorization Endpoint: https://accounts.google.com/o/oauth2/v2/auth (should already be filled in)
  -- Token Endpoint: https://oauth2.googleapis.com/token (should already be filled in)
  -- Access Type: Offline
  -- Force Prompt: Consent screen
  -- Click the 'Use your own OAuth credentials' checkbox, then enter the client ID and client secret you got from the GCP "Credentials" page.
  -- Click Close.
- Click the "Authorize APIs" button, then follow the prompts to grant access.
- You should now see information about an authorization code.  Click the "Exchange authorization code for token" button to obtain an access token.
- Once the authorization code is exchanged for an access token you will see a filled-in box containing the refresh token.  Copy the value of the refresh token.
- Create a file called `ga_refresh_token.txt` and store the value of the refresh token into that file.  Make sure the report configurations point to this file.  DO NOT DELETE THIS FILE.

**IMPORTANT**: This refresh token is NOT saved in Github.  If for some reason you delete this refresh token file you must regenerate the refresh token.

#### Define the crontab entry to periodically execute the script
In order for the script to execute periodically, a crontab entry must be created.

- Use the `sudo su - downloader1` command to become the `downloader1` user.  This is required to enable customization of the `downloader1` crontab.
- Use the command `crontab -e` to enter edit mode on the crontab.  Make the necessary changes, then save and exit.  
- Return to your normal user context by entering the `exit` command.

A crontab entry defines two pieces of information: when a task is to run, and what task is to run.  A sample entry may look like this:

```shell
0 20 * * * /path/to/script >/dev/null 2>&1
```

NOTE: The `crontab -e` command invokes the 'nano' editor to allow you to change the crontab.  It's recommend that you become at least minimally familiar with 'nano' before editing the crontab.

The numbers at the beginning of the crontab entry define *when* the utility is to run. [Crontab Guru](https://crontab.guru/) is an excellent resource for more information on how these values are used.
The `/path/to/script` portion defines which script is to be run at the designated time.  In the case of the Google Analytics downloader this will be `/downloads/ga_downloader/runGoogleAnalyticsDownloader.sh`.
The last portion (` >/dev/null 2>&1`) should be added at the end.

#### Running the script manually
To run the script manually, follow these steps:

- Use the `sudo su - downloader1` command to become the `downloader1` user.  This is required to enable customization of the `downloader1` crontab.
- Use the command `cd /downloads/ga_downloader` to move into the downloader's directory.
- Use the command `./runGoogleAnalyticsDownloader.sh` to execute the downloader.  
- Return to your normal user context by entering the `exit` command.


## Updating the Google Analytics Account IDs File

[Process Documentation](https://docs.google.com/document/d/1lIKZ4VU0SbdSObO7a_ULOdRSLA7bTzG3DKrbzKfmvBo/edit)


