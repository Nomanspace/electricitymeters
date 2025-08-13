
package org.nomanspace.electricitymeters.service;

import org.nomanspace.electricitymeters.data.DatFileContent;
import org.nomanspace.electricitymeters.data.FileDataReader;
import org.nomanspace.electricitymeters.model.Concentrator;
import org.nomanspace.electricitymeters.model.Meter;
import org.nomanspace.electricitymeters.text.DatFileParseHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Сервисный класс-оркестратор, который управляет всем процессом создания отчета.
 */
public class ReportService {

    public void createReportFromLatestFile() {
        System.out.println("--- Процесс запущен ---");

        try {
            // --- Шаг 1: Чтение данных из файла ---
            System.out.println("[1/4] Чтение исходного .dat файла...");
            FileDataReader fileReader = new FileDataReader();
            DatFileContent fileContent = fileReader.readDataFile();

            if (fileContent.lines().isEmpty()) {
                System.out.println("Ошибка: Файл с данными пуст или не найден.");
                return;
            }
            System.out.println("Файл '" + fileContent.sourceFileName() + "' успешно прочитан.");

            // --- Шаг 2: Парсинг данных в "сырую" модель ---
            System.out.println("[2/4] Парсинг данных...");
            DatFileParseHandler parser = new DatFileParseHandler();
            List<Concentrator> rawData = parser.process(fileContent.lines());
            System.out.println("Данные успешно распарсены.");

            // --- Шаг 3: Анализ и отбор последних показаний ---
            System.out.println("[3/4] Анализ данных и отбор последних показаний...");
            DataAnalyzer analyzer = new DataAnalyzer();
            List<Meter> latestReadings = analyzer.getLatestReadings(rawData);
            System.out.println("Анализ завершен. Найдено уникальных счетчиков: " + latestReadings.size());

            // --- Шаг 4: Генерация Excel-отчета ---
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
            String timestamp = LocalDateTime.now().format(formatter);
            String sourceFileName = fileContent.sourceFileName().replace(".dat", "");
            String reportFileName = String.format("%s_Report_from_%s.xlsx", timestamp, sourceFileName);

            Path reportDir = Paths.get("..").resolve("Отчет").toAbsolutePath().normalize();
            Files.createDirectories(reportDir);
            Path reportPath = reportDir.resolve(reportFileName);

            System.out.println("[4/4] Создание Excel-отчета по пути: " + reportPath);
            ExcelReportGenerator reportGenerator = new ExcelReportGenerator();
            reportGenerator.createReport(latestReadings, reportPath.toString());
            System.out.println("Отчет успешно создан!");

        } catch (IOException e) {
            System.err.println("Произошла ошибка при работе с файлами: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Произошла непредвиденная ошибка: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("--- Процесс завершен ---");
    }
}
