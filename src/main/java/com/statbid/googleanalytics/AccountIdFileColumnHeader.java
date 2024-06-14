package com.statbid.googleanalytics;


public enum AccountIdFileColumnHeader {
    CLIENT_NAME("Client Name"),
    SITE_NAME("Site Name"),
    UA_ACCOUNT_ID("UA"),
    GA4_ACCOUNT_ID("GA4");

    private String columnName;
    AccountIdFileColumnHeader(final String name) {
        this.columnName = name;
    }

    public String getColumnName() { return columnName; }

}