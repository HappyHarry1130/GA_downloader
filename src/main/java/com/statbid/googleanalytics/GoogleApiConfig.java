package com.statbid.googleanalytics;

import java.io.File;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents the deserialized version of the Campaign Report config.
 */
@JsonIgnoreProperties  // ignore everything not explicitly annotated
public class GoogleApiConfig {
    @JsonProperty("credentialsFile")
    public String credentialsFile;
    @JsonProperty("refreshTokenCache")
    public String refreshTokenCache;

    public GoogleApiConfig() {

    }

    public File getCredentialsFile() {
        return new File(credentialsFile);
    }

    public File getRefreshTokenCache() {
        return new File(refreshTokenCache);
    }
}


