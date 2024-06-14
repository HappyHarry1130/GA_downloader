package com.statbid.googleanalytics.ua;

import com.statbid.googleanalytics.AccountIdFileRecord;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.function.BiFunction;


public class UACSVAccountReportWriter {

    private CSVPrinter printer;
    private BiFunction<AccountIdFileRecord, List<String>, List<String>> valueCollector;


    public void initialize(final String reportFileName) throws IOException {
        printer = new CSVPrinter(new FileWriter(reportFileName), CSVFormat.EXCEL);
    }

    public void setHeaders(final String... columnNames) throws IOException {
        printer.printRecord((Object[]) columnNames);
    }

    public void setValueCollector(final BiFunction<AccountIdFileRecord, List<String>, List<String>> valueCollector) {
        this.valueCollector = valueCollector;
    }

    public void writeLine(final AccountIdFileRecord record, final List<String> uaRow) throws IOException {
        final List<String> values = valueCollector.apply(record, uaRow);
        printer.printRecord(values);
    }

    public void close() throws IOException {
        printer.close(true);
    }
}

