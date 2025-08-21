package org.nomanspace.electricitymeters.service;

import org.nomanspace.electricitymeters.data.DatFileContent;
import org.nomanspace.electricitymeters.data.FileDataReader;
import org.nomanspace.electricitymeters.model.Concentrator;
import org.nomanspace.electricitymeters.model.Meter;
import org.nomanspace.electricitymeters.text.DatFileParseHandler;
import org.nomanspace.electricitymeters.service.ExcelReportGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.nomanspace.electricitymeters.util.LogUtil;

/**
 * Сервисный класс-оркестратор, который управляет всем процессом создания отчета.
 */
public class ReportService {

    public void createReportFromLatestFile() {
        LogUtil.info("--- Процесс запущен ---");

        try {
            LogUtil.info("[1/4] Чтение исходного .dat файла...");
            FileDataReader fileReader = new FileDataReader();
            DatFileContent fileContent = fileReader.readDataFile();

            if (fileContent.lines().isEmpty()) {
                LogUtil.error("Ошибка: Файл с данными пуст или не найден.");
                return;
            }
            LogUtil.info("Прочитано строк из файла: " + fileContent.lines().size());

            LogUtil.info("[2/4] Парсинг данных...");
            DatFileParseHandler parser = new DatFileParseHandler();
            List<Concentrator> rawData = parser.process(fileContent.lines());
            LogUtil.info("Данные успешно распарсены.");

            LogUtil.info("Найдено концентраторов: " + rawData.size());
            int totalMeters = rawData.stream().mapToInt(c -> c.getMeters().size()).sum();
            LogUtil.info("Суммарное количество записей счетчиков (после парсинга): " + totalMeters);
            if (LogUtil.isLoggingEnabled()) {
                AtomicInteger idx = new AtomicInteger(1);
                rawData.stream().limit(5).forEach(c -> {
                    LogUtil.info("  [Концентратор " + idx.getAndIncrement() + "] '" + c.getConcentratorName() + "' -> meters: " + c.getMeters().size());
                });
            }

            LogUtil.info("[3/4] Анализ данных и отбор последних показаний...");
            DataAnalyzer analyzer = new DataAnalyzer();
            List<Meter> latestReadings = analyzer.getLatestReadings(rawData);
            LogUtil.info("Анализ завершен. Найдено уникальных счетчиков: " + latestReadings.size());

            // --- Шаг 4: Генерация Excel-отчета ---
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
            String timestamp = LocalDateTime.now().format(formatter);
            String sourceFileName = fileContent.sourceFileName().replace(".dat", "");
            String reportFileName = String.format("%s_Report_from_%s.xlsx", timestamp, sourceFileName);

            Path reportDir = Paths.get("..").resolve("Отчеты").toAbsolutePath().normalize();
            Files.createDirectories(reportDir);
            Path reportPath = reportDir.resolve(reportFileName);

            LogUtil.info("[4/4] Создание Excel-отчета по пути: " + reportPath);
            ExcelReportGenerator reportGenerator = new ExcelReportGenerator();
            reportGenerator.createReport(latestReadings, reportPath.toString());
            // Always show success message
            System.out.println("Отчет успешно создан!");

        } catch (Exception e) {
            LogUtil.error("Произошла непредвиденная ошибка: " + e.getMessage());
            e.printStackTrace();
        }

        LogUtil.info("--- Процесс завершен ---");
    }
}