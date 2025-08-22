package org.nomanspace.electricitymeters.util;

import java.time.LocalDateTime;
import java.time.YearMonth;

public final class ParsingUtils {

    // Приватный конструктор, чтобы нельзя было создать экземпляр этого класса
    private ParsingUtils() {}

    public static byte[] hexStringToByteArray(String string) {
        if (string == null || string.isEmpty() || string.length() % 2 != 0) {
            return new byte[0];
        }
        byte[] binDataByte = new byte[string.length() / 2];
        int z = 0;
        for (int i = 0; i < string.length(); i = i + 2) {
            int oneByteUnit = Integer.parseInt(string.substring(i, i + 2), 16);
            binDataByte[z] = (byte) oneByteUnit;
            z++;
        }
        return binDataByte;
    }

    public static LocalDateTime parseTimestamp(byte[] timestampBytes) {
        if (timestampBytes == null || timestampBytes.length < 5) {
            return null; // Возвращаем null, если данные некорректны
        }
        // По наблюдениям TIMEDATE кодирует минуты и часы как "сырые" байты, а не BCD
        // Пример: 0x17 -> 23 часа, 0x1B -> 27 минут
        int min = timestampBytes[0] & 0xFF;
        int hour = timestampBytes[1] & 0xFF;
        int day = smartBcdByteToInt(timestampBytes[2]);
        int month = smartBcdByteToInt(timestampBytes[3]);
        // Год — как «сырое» значение байта + 2000 (не BCD)
        int year = (timestampBytes[4] & 0xFF) + 2000;

        // В документации встречается кодирование дня и месяца с нуля -> инкрементируем
        day += 1;
        month += 1;

        if (year < 2000 || year > 2099 || month < 1 || month > 12 || day < 1 || day > 31 || hour > 23 || min > 59) {
            return null;
        }

        // Коррекция несуществующих дат (например, 30 февраля -> 28/29)
        int maxDayInMonth = YearMonth.of(year, month).lengthOfMonth();
        if (day > maxDayInMonth) {
            day = maxDayInMonth;
        }

        return LocalDateTime.of(year, month, day, hour, min, 0);
    }

    /**
     * Парсинг временной метки из BINDATA: день и месяц берутся как (байт + 1), без BCD-декодирования.
     */
    public static LocalDateTime parseTimestampRawPlusOne(byte[] timestampBytes) {
        if (timestampBytes == null || timestampBytes.length < 5) {
            LogUtil.debug("[BINDATA-TIME] Некорректная длина массива временной метки: " + (timestampBytes == null ? "null" : timestampBytes.length));
            return null;
        }
        StringBuilder hexDump = new StringBuilder();
        for (byte b : timestampBytes) {
            hexDump.append(String.format("%02X ", b));
        }
        LogUtil.debug("[BINDATA-TIME] Входные байты: " + hexDump.toString().trim());
        int min = timestampBytes[0] & 0xFF;
        int hour = timestampBytes[1] & 0xFF;
        int day = (timestampBytes[2] & 0xFF) + 1;
        int month = (timestampBytes[3] & 0xFF) + 1;
        int year = (timestampBytes[4] & 0xFF) + 2000;
        LogUtil.debug(String.format("[BINDATA-TIME] min=%d, hour=%d, day=%d, month=%d, year=%d", min, hour, day, month, year));
        if (year < 2000 || year > 2099 || month < 1 || month > 12 || day < 1 || day > 31 || hour > 23 || min > 59) {
            LogUtil.debug("[BINDATA-TIME] Некорректная дата: min=" + min + ", hour=" + hour + ", day=" + day + ", month=" + month + ", year=" + year);
            return null;
        }
        int maxDayInMonth = java.time.YearMonth.of(year, month).lengthOfMonth();
        if (day > maxDayInMonth) {
            LogUtil.debug("[BINDATA-TIME] Коррекция дня: " + day + " -> " + maxDayInMonth);
            day = maxDayInMonth;
        }
        LocalDateTime result = LocalDateTime.of(year, month, day, hour, min, 0);
        LogUtil.debug("[BINDATA-TIME] Итоговая дата: " + result);
        return result;
    }

    // Умный разбор: если полубайты валидные BCD (<=9), трактуем как BCD,
    // иначе возвращаем беззнаковое значение байта (0..255).
    private static int smartBcdByteToInt(byte b) {
        int highNibble = (b >> 4) & 0x0F;
        int lowNibble = b & 0x0F;
        if (highNibble <= 9 && lowNibble <= 9) {
            return highNibble * 10 + lowNibble;
        } else {
            return b & 0xFF;
        }
    }
}
