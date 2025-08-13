
package org.nomanspace.electricitymeters.util;

import java.time.LocalDateTime;

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
        int min = bcdByteToInt(timestampBytes[0]);
        int hour = bcdByteToInt(timestampBytes[1]);
        int day = bcdByteToInt(timestampBytes[2]);
        int month = bcdByteToInt(timestampBytes[3]);
        int year = bcdByteToInt(timestampBytes[4]) + 2000;

        if (year < 2000 || year > 2099 || month < 1 || month > 12 || day < 1 || day > 31 || hour > 23 || min > 59) {
            return null;
        }

        return LocalDateTime.of(year, month, day, hour, min, 0);
    }

    // Этот метод используется только в parseTimestamp, поэтому он может остаться здесь как вспомогательный
    private static int bcdByteToInt(Byte hexPair) {
        int leftPairChunk = (hexPair >> 4) & 0x0F;
        int rightPairChunk = hexPair & 0x0F;
        return leftPairChunk * 10 + rightPairChunk;
    }
}
