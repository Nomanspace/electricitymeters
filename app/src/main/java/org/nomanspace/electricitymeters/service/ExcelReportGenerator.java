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
            Sheet sheet = workbook.createSheet("Последние показания счетчиков");

            // --- Стили ячеек ---
            // Стиль для заголовка
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Стиль для даты
            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd.MM.yyyy HH:mm:ss"));

            // --- Создание заголовка ---
            LogUtil.debug("Создание заголовков таблицы...");
            String[] headers = {
                    "Адрес", "Хост", "Здание", "Помещение", "Серийный номер",
                    "Суммарная энергия", "Энергия Т1", "Энергия Т2", "Энергия Т3", "Энергия Т4",
                    "Уровень сигнала", "Время снятия (из бин-данных)", "Время опроса концентратора"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            LogUtil.info("Формирование строк отчета...");
            int rowNum = 1;
            for (Meter meter : latestReadings) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(meter.getAddress());
                row.createCell(1).setCellValue(meter.getHost());
                row.createCell(2).setCellValue(meter.getBuilding());
                row.createCell(3).setCellValue(meter.getRoom());
                // Фолбэк для серийника: если пусто — используем HOST:ADDR
                String serial = meter.getSerialNumber();
                if (serial == null || serial.isEmpty()) {
                    if (meter.getHost() != null && meter.getAddress() != null) {
                        serial = meter.getHost() + ":" + meter.getAddress();
                    }
                }
                row.createCell(4).setCellValue(serial != null ? serial : "");

                if (meter.getEnergyTotal() != null) row.createCell(5).setCellValue(meter.getEnergyTotal());
                if (meter.getEnergyT1() != null) row.createCell(6).setCellValue(meter.getEnergyT1());
                if (meter.getEnergyT2() != null) row.createCell(7).setCellValue(meter.getEnergyT2());
                if (meter.getEnergyT3() != null) row.createCell(8).setCellValue(meter.getEnergyT3());
                if (meter.getEnergyT4() != null) row.createCell(9).setCellValue(meter.getEnergyT4());
                if (meter.getSignalLevel() != null) row.createCell(10).setCellValue(meter.getSignalLevel());

                if (meter.getLastMeasurementTimestamp() != null) {
                    Cell dateCell = row.createCell(11);
                    dateCell.setCellValue(toDate(meter.getLastMeasurementTimestamp()));
                    dateCell.setCellStyle(dateStyle);
                }
                if (meter.getLogTimestamp() != null) {
                    Cell dateCell = row.createCell(12);
                    dateCell.setCellValue(toDate(meter.getLogTimestamp()));
                    dateCell.setCellStyle(dateStyle);
                }
            }

            // --- Финальное оформление ---
            // Автоподбор ширины колонок
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Закрепление шапки
            sheet.createFreezePane(0, 1);

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
