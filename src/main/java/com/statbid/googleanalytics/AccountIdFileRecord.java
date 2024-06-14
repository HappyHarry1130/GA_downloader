package com.statbid.googleanalytics;

import java.util.Map;

public class AccountIdFileRecord {
    private Map<AccountIdFileColumnHeader, String> properties;

    public AccountIdFileRecord(final Map<AccountIdFileColumnHeader, String> props) {
        this.properties = props;
    }

    public String getProperty(final AccountIdFileColumnHeader name) {
        return properties.get(name);
    }
}
