package org.nomanspace.electricitymeters.path;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigLoader implements PathProvider {

    @Override
    public Path providePath() {
        //String currentDirectory = System.getProperty("user.dir");
        //System.out.println("Current directory: " + currentDirectory);
        //"../config.properties"
        //currentDirectory,
        //Path settingRootPath = Paths.get("..", "config.properties");
        Path settingRootPath = Paths.get(System.getProperty("user.dir"))  // Текущая директория
                .getParent()                            // Переход на уровень выше
                .resolve("config.properties");          // Добавляем файл

        if (Files.notExists(settingRootPath)) {
            //String errorMessage = "config.properties not found in dir: " + currentDirectory;
            //System.err.println(errorMessage);
            System.out.println("Current working directory: " + System.getProperty("user.dir"));
            System.out.println("Absolute path to config.properties: " + settingRootPath.toAbsolutePath());
            System.out.println("Searching for config.properties in: " + settingRootPath);
            throw new RuntimeException("config.properties not found in: " + settingRootPath.toAbsolutePath());
        }
        return settingRootPath;
    }

}
