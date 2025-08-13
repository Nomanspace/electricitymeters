package org.nomanspace.electricitymeters.util;

import org.nomanspace.electricitymeters.model.Meter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BinDataDecoder {

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

    /**
     * "Умный" декодер для дробной части. Если байт не является валидным BCD (содержит A-F),
     * он интерпретируется как обычное бинарное число. Иначе - как BCD.
     */
    private int smartBcdByteToInt(byte b) {
        int highNibble = (b >> 4) & 0x0F;
        int lowNibble = b & 0x0F;

        if (highNibble > 9 || lowNibble > 9) {
            // Невалидный BCD, читаем как простое число
            return b & 0xFF;
        } else {
            // Валидный BCD
            return highNibble * 10 + lowNibble;
        }
    }

    // --- Основной метод декодирования ---

    public List<Meter> decode(String binData) {
        List<Meter> decodedMeters = new ArrayList<>();

        if (binData == null || binData.isEmpty()) {
            return decodedMeters; // Возвращаем пустой список
        }

        byte[] payload = ParsingUtils.hexStringToByteArray(binData);

        for (int i = 3; i + 11 <= payload.length; i += 11) {
            Meter recordMeter = new Meter();
            byte recordType = payload[i];

            switch (recordType) {
                case (byte) 0x40, (byte) 0x41, (byte) 0x42, (byte) 0x43, (byte) 0x4F:
                    long integerPart = bcdBytesToLong(payload, i + 1, 3);
                    // Используем новый "умный" метод для дробной части
                    int fractionalPart = smartBcdByteToInt(payload[i + 4]);
                    double finalEnergy = integerPart + (fractionalPart / 100.0);

                    if (recordType == (byte) 0x40) recordMeter.setEnergyT1(finalEnergy);
                    else if (recordType == (byte) 0x41) recordMeter.setEnergyT2(finalEnergy);
                    else if (recordType == (byte) 0x42) recordMeter.setEnergyT3(finalEnergy);
                    else if (recordType == (byte) 0x43) recordMeter.setEnergyT4(finalEnergy);
                    else if (recordType == (byte) 0x4F) recordMeter.setEnergyTotal(finalEnergy);
                    break;

                case (byte) 0x00, (byte) 0x10: recordMeter.setEnergyT1((double) littleEndianBytesToLong(payload, i + 1, 4)); break;
                case (byte) 0x01, (byte) 0x11: recordMeter.setEnergyT2((double) littleEndianBytesToLong(payload, i + 1, 4)); break;
                case (byte) 0x02, (byte) 0x12: recordMeter.setEnergyT3((double) littleEndianBytesToLong(payload, i + 1, 4)); break;
                case (byte) 0x03, (byte) 0x13: recordMeter.setEnergyT4((double) littleEndianBytesToLong(payload, i + 1, 4)); break;
                case (byte) 0x0F, (byte) 0x1F: recordMeter.setEnergyTotal((double) littleEndianBytesToLong(payload, i + 1, 4)); break;

                case (byte) 0x48:
                    long serial = littleEndianBytesToLong(payload, i + 1, 4);
                    recordMeter.setSerialNumber(String.valueOf(serial));
                    break;

                default:
                    System.out.println("Warning: Unknown record type found: 0x" + String.format("%02X", recordType));
                    break;
            }

            int signalLevel = payload[i + 5] & 0xFF;
            recordMeter.setSignalLevel(signalLevel);

            byte[] timestampSlice = java.util.Arrays.copyOfRange(payload, i + 6, i + 11);
            LocalDateTime timestamp = ParsingUtils.parseTimestamp(timestampSlice);
            recordMeter.setLastMeasurementTimestamp(timestamp);

            decodedMeters.add(recordMeter);
        }

        return decodedMeters;
    }

    /*public static void main(String[] args) {
        BinDataDecoder decoder = new BinDataDecoder();
        String bindataWithInvalidBcd = "852F004F5800002B04060B0203194F460000340420151E0219";

        System.out.println("--- Starting decode with invalid BCD example ---");
        List<Meter> meters = decoder.decode(bindataWithInvalidBcd);
        System.out.println("--- Decode finished ---");

        System.out.println("Found " + meters.size() + " records.");
        for (int i = 0; i < meters.size(); i++) {
            System.out.println("--- Record " + (i + 1) + " ---");
            System.out.println(meters.get(i));
        }
    }*/
}