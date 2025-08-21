package org.nomanspace.electricitymeters;

import org.nomanspace.electricitymeters.service.ReportService;
import org.nomanspace.electricitymeters.util.LogUtil;
import org.nomanspace.electricitymeters.util.TeeOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class App {

    public static void main(String[] args) {
        // Enable logging if --log or -log flag is present
        if (args != null) {
            for (String a : args) {
                if ("-log".equalsIgnoreCase(a) || "--log".equalsIgnoreCase(a)) {
                    LogUtil.setLoggingEnabled(true);
                    enableFileLogging();
                    break;
                }
            }
        }
        
        ReportService reportService = new ReportService();
        reportService.createReportFromLatestFile();
    }

    private static void enableFileLogging() {
        // Only set up file logging if logging is enabled
        if (!LogUtil.isLoggingEnabled()) {
            return;
        }
        
        try {
            String fileName = "ElectricityMetersApp.log";
            PrintStream ps = new PrintStream(new FileOutputStream(fileName, true), true, StandardCharsets.UTF_8);

            // Banner with timestamp to separate runs
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            ps.println("===== START " + ts + " =====");

            // Redirect both stdout and stderr to the log file
            System.setOut(new TeeOutputStream(System.out, ps));
            System.setErr(new TeeOutputStream(System.err, ps));
            
            // Also echo where the log is
            System.out.println("[LOG] Logging enabled. Writing to " + new java.io.File(fileName).getAbsolutePath());
        } catch (Exception e) {
            // If enabling file logging fails, keep console output and report the issue.
            System.err.println("[LOG] Failed to enable file logging: " + e.getMessage());
        }
    }
}