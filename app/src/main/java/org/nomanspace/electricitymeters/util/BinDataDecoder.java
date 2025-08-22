package org.nomanspace.electricitymeters.util;

import org.nomanspace.electricitymeters.model.Meter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.nomanspace.electricitymeters.util.LogUtil;

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

    private static String bytesToHexLE(byte[] bytes, int offset, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02X", bytes[offset + i]));
        }
        return sb.toString();
    }

    private int smartBcdByteToInt(byte b) {
        int highNibble = (b >> 4) & 0x0F;
        int lowNibble = b & 0x0F;

        if (highNibble > 9 || lowNibble > 9) {
            return b & 0xFF;
        } else {
            return highNibble * 10 + lowNibble;
        }
    }

    // Старый вход без контекста оставляем для совместимости
    public List<Meter> decode(String binData) {
        return decode(binData, null, null);
    }

    // Новый метод с контекстом HOST/ADDR для расширенного логирования
    public List<Meter> decode(String binData, String host, String addr) {
        List<Meter> decodedMeters = new ArrayList<>();

        if (binData == null || binData.isEmpty()) {
            return decodedMeters;
        }

        byte[] payload = ParsingUtils.hexStringToByteArray(binData);

        for (int i = 3; i + 11 <= payload.length; i += 11) {
            Meter recordMeter = new Meter();
            byte recordType = payload[i];
            String key = (host != null && addr != null) ? (host + ":" + addr) : "-";

            switch (recordType) {
                case (byte) 0x40, (byte) 0x41, (byte) 0x42, (byte) 0x43, (byte) 0x4F:
                    // Согласно сверке с эталоном: 3 байта = целая часть в формате BIN (LE), 1 байт = сотые в BIN (0..99)
                    long integerPart = littleEndianBytesToLong(payload, i + 1, 3);
                    int fractionalPart = payload[i + 4] & 0xFF;
                    double finalEnergy = integerPart + (fractionalPart / 100.0);

                    // Логирование сырых байтов и результата для диагностики
                    String intHex = bytesToHexLE(payload, i + 1, 3);
                    String fracHex = bytesToHexLE(payload, i + 4, 1);
                    LogUtil.debug(String.format("[ENERGY] key=%s:%s type=0x4F intBIN3LE=%02X%02X%02X frac8bit=%02X -> int=%d frac=%d -> value=%d,%02d",
                            host, addr, payload[i+1], payload[i+2], payload[i+3], payload[i+4],
                            (int)integerPart, fractionalPart, (int)integerPart, fractionalPart));

                    if (recordType == (byte) 0x40) recordMeter.setEnergyT1(finalEnergy);
                    else if (recordType == (byte) 0x41) recordMeter.setEnergyT2(finalEnergy);
                    else if (recordType == (byte) 0x42) recordMeter.setEnergyT3(finalEnergy);
                    else if (recordType == (byte) 0x43) recordMeter.setEnergyT4(finalEnergy);
                    else if (recordType == (byte) 0x4F) recordMeter.setEnergyTotal(finalEnergy);
                    break;

                case (byte) 0x00, (byte) 0x10, (byte) 0x01, (byte) 0x11,
                     (byte) 0x02, (byte) 0x12, (byte) 0x03, (byte) 0x13,
                     (byte) 0x0F, (byte) 0x1F: {
                    long raw = littleEndianBytesToLong(payload, i + 1, 4);
                    LogUtil.debug(String.format("[SKIP-LE] key=%s type=0x%02X rawBytes=%s raw=%d — согласно документации BASE+INC, используем BCD-записи 0x4x при наличии",
                            key, recordType, bytesToHexLE(payload, i + 1, 4), raw));
                    // Временно игнорируем запись энергии из LE-группы, чтобы не заносить аномалии.
                    break;
                }

                case (byte) 0x48:
                    long serial = littleEndianBytesToLong(payload, i + 1, 4);
                    recordMeter.setSerialNumber(String.valueOf(serial));
                    LogUtil.debug(String.format("[SERIAL] key=%s type=0x48 bytesLE=%s serial=%d",
                            key, bytesToHexLE(payload, i + 1, 4), serial));
                    break;

                default:
                    LogUtil.debug("Warning: Unknown record type found: key=" + key + " type=0x" + String.format("%02X", recordType));
                    break;
            }

            int signalLevel = payload[i + 5] & 0xFF;
            recordMeter.setSignalLevel(signalLevel);

            byte[] timestampSlice = java.util.Arrays.copyOfRange(payload, i + 6, i + 11);
            LocalDateTime timestamp = ParsingUtils.parseTimestampRawPlusOne(timestampSlice);
            recordMeter.setLastMeasurementTimestamp(timestamp);

            decodedMeters.add(recordMeter);
        }

        return decodedMeters;
    }
}