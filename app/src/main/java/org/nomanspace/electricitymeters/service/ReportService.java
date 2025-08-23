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
import java.util.Comparator;

import org.nomanspace.electricitymeters.util.LogUtil;

/**
 * Сервисный класс-оркестратор, который управляет всем процессом создания отчета.
 */
public class ReportService {

    /**
     * Запускает полный процесс: чтение .dat файла, парсинг, анализ и формирование Excel-отчета.
     */
    public void createReportFromLatestFile() {
        LogUtil.info("--- Процесс запущен ---");
        try {
            DatFileContent fileContent = readLatestDatFile();
            if (fileContent == null) return;

            List<Concentrator> rawData = parseDatFileContent(fileContent);
            if (rawData == null) return;

            List<Meter> latestReadings = analyzeData(rawData);
            if (latestReadings == null) return;

            // Сортируем список сначала по хосту, потом по адресу для соответствия эталону
            LogUtil.info("Сортировка результатов для отчета...");
            latestReadings.sort(Comparator.comparing(Meter::getHost, Comparator.nullsLast(String::compareTo))
                    .thenComparing(Meter::getAddress, Comparator.nullsLast(String::compareTo)));

            Path reportPath = prepareReportPath(fileContent.sourceFileName());
            if (reportPath == null) {
                LogUtil.error("Отчет не будет создан из-за ошибки создания директории.");
                return;
            }
            generateExcelReport(latestReadings, reportPath);

        } catch (Exception e) {
            LogUtil.error("Произошла непредвиденная ошибка: " + e.getMessage());
            e.printStackTrace();
        }
        LogUtil.info("--- Процесс завершен ---");
    }

    private DatFileContent readLatestDatFile() {
        LogUtil.info("[1/4] Чтение исходного .dat файла...");
        FileDataReader fileReader = new FileDataReader();
        DatFileContent fileContent = fileReader.readDataFile();
        if (fileContent.lines().isEmpty()) {
            LogUtil.error("Ошибка: Файл с данными пуст или не найден.");
            return null;
        }
        LogUtil.info("Прочитано строк из файла: " + fileContent.lines().size());
        return fileContent;
    }

    private List<Concentrator> parseDatFileContent(DatFileContent fileContent) {
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
        return rawData;
    }

    private List<Meter> analyzeData(List<Concentrator> rawData) {
        LogUtil.info("[3/4] Анализ данных и отбор последних показаний...");
        DataAnalyzer analyzer = new DataAnalyzer();
        List<Meter> latestReadings = analyzer.getLatestReadings(rawData);
        LogUtil.info("Анализ завершен. Найдено уникальных счетчиков: " + latestReadings.size());
        return latestReadings;
    }

    private Path prepareReportPath(String sourceFileName) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        String timestamp = LocalDateTime.now().format(formatter);
        String cleanFileName = sourceFileName.replace(".dat", "");
        String reportFileName = String.format("%s_Report_from_%s.xlsx", timestamp, cleanFileName);
        Path reportDir = Paths.get("..")
                .resolve("Отчеты").toAbsolutePath().normalize();
        try {
            Files.createDirectories(reportDir);
        } catch (IOException e) {
            LogUtil.error("Не удалось создать директорию для отчетов: " + e.getMessage());
            return null;
        }
        return reportDir.resolve(reportFileName);
    }

    private void generateExcelReport(List<Meter> latestReadings, Path reportPath) {
        LogUtil.info("[4/4] Создание Excel-отчета по пути: " + reportPath);
        ExcelReportGenerator reportGenerator = new ExcelReportGenerator();
        try {
            reportGenerator.createReport(latestReadings, reportPath.toString());
            LogUtil.info("Отчет успешно создан!");
        } catch (IOException e) {
            LogUtil.error("Ошибка при создании Excel-отчета: " + e.getMessage());
        }
    }
}