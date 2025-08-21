package org.nomanspace.electricitymeters.util;

public class LogUtil {
    private static boolean loggingEnabled = false;
    
    public static void setLoggingEnabled(boolean enabled) {
        loggingEnabled = enabled;
    }
    
    public static boolean isLoggingEnabled() {
        return loggingEnabled;
    }
    
    public static void info(String message) {
        if (loggingEnabled) {
            System.out.println(message);
        }
    }
    
    public static void error(String message) {
        if (loggingEnabled) {
            System.err.println(message);
        }
    }
    
    public static void debug(String message) {
        if (loggingEnabled) {
            System.out.println("[DEBUG] " + message);
        }
    }
}
