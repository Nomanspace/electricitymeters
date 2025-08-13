package org.nomanspace.electricitymeters;

import org.nomanspace.electricitymeters.service.ReportService;

public class App {

    public static void main(String[] args) {
        ReportService reportService = new ReportService();
        reportService.createReportFromLatestFile();
    }
}