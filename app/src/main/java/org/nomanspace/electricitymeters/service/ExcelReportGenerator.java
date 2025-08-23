package org.nomanspace.electricitymeters.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.nomanspace.electricitymeters.model.Meter;
import org.nomanspace.electricitymeters.util.LogUtil;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

public class ExcelReportGenerator {

    public void createReport(List<Meter> latestReadings, String filePath) throws IOException {
        LogUtil.info("Создание отчета...");

        if (latestReadings == null || latestReadings.isEmpty()) {
            LogUtil.error("Нет данных для отчета");
            return;
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Показания счетчиков");

            // --- Стили ячеек ---
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            // Стиль для чисел с запятой
            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            // --- Создание заголовка ---
            LogUtil.debug("Создание заголовков таблицы...");
            String[] headers = {
                    "Конц.", "PLC", "Дата", "кВт*ч"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // --- Формирование строк ---
            LogUtil.info("Формирование строк отчета...");
            int rowNum = 1;
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM HH:mm");

            for (Meter meter : latestReadings) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(meter.getHost());
                row.createCell(1).setCellValue(meter.getAddress());

                // Формируем ячейку "Дата"
                String dateCellString = "";
                if (meter.getLastMeasurementTimestamp() != null) {
                    String formattedDate = meter.getLastMeasurementTimestamp().format(dateFormatter);
                    Integer signal = meter.getSignalLevel();
                    dateCellString = String.format("%s (%d)", formattedDate, signal != null ? signal : 0);
                }
                row.createCell(2).setCellValue(dateCellString);
                
                // Формируем ячейку "кВт*ч"
                if (meter.getEnergyTotal() != null) {
                    Cell energyCell = row.createCell(3);
                    energyCell.setCellValue(meter.getEnergyTotal());
                    energyCell.setCellStyle(numberStyle);
                }
            }

            // --- Финальное оформление ---
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            sheet.createFreezePane(0, 1);

            // --- Сохранение ---
            LogUtil.info("Сохранение отчета в файл: " + filePath);
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
                LogUtil.info("Отчет успешно сохранен: " + filePath);
            } catch (IOException e) {
                LogUtil.error("Ошибка при сохранении отчета: " + e.getMessage());
                e.printStackTrace();
            }
        }
        LogUtil.info("--- Готово ---");
    }

    private static Date toDate(LocalDateTime ldt) {
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }
}
