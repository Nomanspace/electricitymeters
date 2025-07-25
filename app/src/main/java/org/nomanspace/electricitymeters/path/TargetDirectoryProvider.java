package org.nomanspace.electricitymeters.path;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class TargetDirectoryProvider implements PathProvider {

    @Override
    public Path providePath() {
        Path configPropPath = new ConfigLoader().providePath();
        Properties properties = new Properties();
        String propPath = null;

        try (BufferedReader reader = Files.newBufferedReader(configPropPath, StandardCharsets.UTF_8)) {
            // List<String> configPropLines = Files.readAllLines(configPropPath);
            /*
             * if (configPropLines.isEmpty()) { String errorMsg =
             * "File config.properties is empty: " + configPropPath;
             * System.err.println(errorMsg); }
             */
            // String firstLine = configPropLines.get(0);
            // System.out.println("Success! The first line is: " + firstLine);
            properties.load(reader);
            propPath = properties.getProperty("targetDir");
            // properties.load(Files.readString(configPropPath).transform());

        } catch (IOException e) {
            e.printStackTrace();
            // тут можно вернуть значение по умолчанию или создать кастомный эксепшен
        }

        if (propPath == null || propPath.trim().isEmpty()) {
            String errorMsg = "Property 'targetDir' not found or is empty in config.properties " + propPath;
            System.err.println(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        return Paths.get(propPath);

    }

}
