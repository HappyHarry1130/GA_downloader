package com.statbid.googleanalytics.ga4;

import com.google.analytics.data.v1beta.Row;
import com.statbid.googleanalytics.AccountIdFileRecord;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.function.BiFunction;

public class G4CSVAccountReportWriter {

    private CSVPrinter printer;
    private BiFunction<AccountIdFileRecord, Row, List<String>> valueCollector;


    public void initialize(final String reportFileName) throws IOException {
        printer = new CSVPrinter(new FileWriter(reportFileName), CSVFormat.EXCEL);
    }

    public void setHeaders(final String... columnNames) throws IOException {
        printer.printRecord((Object[]) columnNames);
    }

    public void setValueCollector(final BiFunction<AccountIdFileRecord, Row, List<String>> valueCollector) {
        this.valueCollector = valueCollector;
    }

    public void writeLine(final AccountIdFileRecord acct, final Row ga4Row) throws IOException {
        final List<String> values = valueCollector.apply(acct, ga4Row);
        printer.printRecord(values);
    }

    public void close() throws IOException {
        printer.close(true);
    }
}
