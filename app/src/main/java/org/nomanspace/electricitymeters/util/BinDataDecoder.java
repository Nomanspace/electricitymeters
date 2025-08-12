package org.nomanspace.electricitymeters.util;

import org.nomanspace.electricitymeters.model.Meter;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.ZoneOffset;


public class BinDataDecoder {

    private static byte[] hexStringToByteArray(String string) {
        byte[] binDataByte = new byte[string.length() / 2];
        int z = 0;
        for (int i = 0; i < string.length(); i = i + 2) {

            int oneByteUnit = Integer.parseInt(string.substring(i, i + 2), 16);
            binDataByte[z] = (byte) oneByteUnit;
            z++;
        }
        return binDataByte;
    }

    private static int bcdByteToInt(Byte hexPair) {
        int leftPairChunk = (hexPair >> 4) & 0x0F;
        int rightPairChunk = hexPair & 0x0F;
        return leftPairChunk * 10 + rightPairChunk;
    }

    private static long bcdBytesToLong(byte[] bcdBytes, int offset, int lengthForExecute) {
        long result = 0;
        for (int i = offset + lengthForExecute - 1; i >= offset; i--) {
            int currentChunk = bcdByteToInt(bcdBytes[i]);
            result = result * 100;
            result = result + currentChunk;
        }

        return result;
    }

    private static long littleEndianBytesToLong(byte[] hexByte, int offset, int lengthForExecute) {
        long result = 0L;
        for (int i = offset + lengthForExecute - 1; i >= offset; i--) {
            result = (result << 8);
            result = result | (hexByte[i] & 0xFF);
        }
        return result;
    }

    private LocalDateTime parseTimestamp(byte[] timestampBytes) {
        int min = timestampBytes[0];
        int hour = timestampBytes[1];
        int day = timestampBytes[2];
        int month = timestampBytes[3] + 1; // Добавляем 1, так как месяцы в логе 0-11
        int year = timestampBytes[4] + 2000; // Добавляем 2000, чтобы получить полный год
        // Секунды отсутствуют в этом формате, используем 0
        return LocalDateTime.of(year, month, day, hour, min, 0);
    }

    public Meter decode(String binData, Meter meter) {

        if (binData == null || binData.isEmpty()) {
            return meter;
        }

        byte[] payload = hexStringToByteArray(binData);

        if (payload.length < 14) {
            return meter;
        }

        for (int i = 3; i + 11 <= payload.length; i += 11) {
            Byte recordType = payload[i];

            switch (recordType) {
                case (byte) 0x40:
                    System.out.println("Found Tariff 1 record");
                    long tariffOne = bcdBytesToLong(payload, i + 1, 4);
                    meter.setEnergyT1(tariffOne);
                    break;

                case (byte) 0x41:
                    System.out.println("Found Tariff 2 record");
                    long tariffTwo = bcdBytesToLong(payload, i + 1, 4);
                    meter.setEnergyT2(tariffTwo);
                    break;

                case (byte) 0x42:
                    System.out.println("Found Tariff 3 record");
                    long tariffThree = bcdBytesToLong(payload, i + 1, 4);
                    meter.setEnergyT3(tariffThree);
                    break;

                case (byte) 0x43:
                    System.out.println("Found Tariff 4 record");
                    long tariffFour = bcdBytesToLong(payload, i + 1, 4);
                    meter.setEnergyT4(tariffFour);
                    break;

                case (byte) 0x4F:
                    System.out.println("Found summary all Tariff record");
                    long tariffSummary = bcdBytesToLong(payload, i + 1, 4);
                    meter.setEnergyTotal(tariffSummary);
                    break;

                case (byte) 0x48:
                    System.out.println("Found serialNumber record");
                    long serial = littleEndianBytesToLong(payload, i + 1, 4);
                    String serialNumber = String.valueOf(serial);
                    meter.setSerialNumber(serialNumber);
                    break;

                default:
                    break;
            }

            int signalLevel = payload[i + 5] & 0xFF;
            meter.setSignalLevel(signalLevel);

            /*long timeStampSecond = littleEndianBytesToLong(payload, i + 6, 5);
            LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(timeStampSecond), ZoneOffset.UTC);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDate = dateTime.format(formatter);
            System.out.println("current 11 byte slice timestamp is " + formattedDate + " raw value: " + timeStampSecond);*/

            byte[] timestampSlice = java.util.Arrays.copyOfRange(payload, i + 6, i + 11);
            LocalDateTime timestamp = parseTimestamp(timestampSlice); // Вызываем новый метод
            System.out.println("Timestamp from record: " + timestamp);

        }

        return meter;
    }

    public static void main(String[] args) {
        // 1. Создаем экземпляр нашего декодера
        BinDataDecoder decoder = new BinDataDecoder();
        // 2. Создаем пустой объект Meter для заполнения
        Meter meter = new Meter();
        // 3. СЮДА НУЖНО ВСТАВИТЬ РЕАЛЬНУЮ СТРОКУ BINDATA ИЗ ЛОГА
        String realBindata = "8543004F07000020041E030203194F05000059043A171E0219";

        // 4. Запускаем декодирование
        System.out.println("--- Starting decode ---");
        decoder.decode(realBindata, meter);
        System.out.println("--- Decode finished ---");
    }
}
