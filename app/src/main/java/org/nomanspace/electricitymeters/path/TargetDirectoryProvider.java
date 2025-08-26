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
        String pathFromConfig = null;

        try (BufferedReader reader = Files.newBufferedReader(configPropPath, StandardCharsets.UTF_8)) {
            properties.load(reader);
            pathFromConfig = properties.getProperty("targetDirToDat");            
        } catch (IOException e) {
            e.printStackTrace();
            // В случае ошибки чтения конфига, выбрасываем исключение
            throw new RuntimeException("Could not read config.properties file.", e);
        }

        if (pathFromConfig == null || pathFromConfig.trim().isEmpty()) {
            String errorMsg = "Property 'targetDir' not found or is empty in config.properties";
            System.err.println(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        // Нормализуем путь: заменяем все виды слэшей на универсальный '/'
        String normalizedPath = pathFromConfig.replace('\\', '/');

        return Paths.get(normalizedPath);
    }

}
