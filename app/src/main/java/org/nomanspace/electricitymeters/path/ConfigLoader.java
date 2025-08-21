package org.nomanspace.electricitymeters.path;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.nomanspace.electricitymeters.util.LogUtil;

public class ConfigLoader implements PathProvider {

    @Override
    public Path providePath() {
        // 1) Системное свойство -Dconfig.file=...
        String sysProp = System.getProperty("config.file");
        if (sysProp != null && !sysProp.isBlank()) {
            Path byProp = Paths.get(sysProp).toAbsolutePath().normalize();
            if (Files.exists(byProp)) {
                LogUtil.info("config.properties найден по системному свойству: " + byProp);
                return byProp;
            } else {
                LogUtil.info("Указанный -Dconfig.file не найден: " + byProp);
            }
        }

        // 2) Переменные окружения CONFIG_FILE/CONFIG_PATH
        String envFile = System.getenv("CONFIG_FILE");
        if ((envFile == null || envFile.isBlank())) {
            envFile = System.getenv("CONFIG_PATH");
        }
        if (envFile != null && !envFile.isBlank()) {
            Path byEnv = Paths.get(envFile).toAbsolutePath().normalize();
            if (Files.exists(byEnv)) {
                LogUtil.info("config.properties найден по ENV: " + byEnv);
                return byEnv;
            } else {
                LogUtil.info("Указанный в ENV путь не найден: " + byEnv);
            }
        }

        // 3) Поиск в текущей и родительских директориях
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        int steps = 0;
        while (current != null && steps < 5) { // ограничим до 5 уровней вверх
            Path candidate = current.resolve("config.properties");
            if (Files.exists(candidate)) {
                LogUtil.info("config.properties найден в: " + candidate.toAbsolutePath());
                return candidate;
            }
            current = current.getParent();
            steps++;
        }

        // 4) Не найден
        LogUtil.info("Current working directory: " + System.getProperty("user.dir"));
        throw new RuntimeException("config.properties not found (проверьте -Dconfig.file или ENV CONFIG_FILE/CONFIG_PATH)");
    }

}
