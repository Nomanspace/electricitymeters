package org.nomanspace.electricitymeters.text;

import org.nomanspace.electricitymeters.model.Concentrator;
import org.nomanspace.electricitymeters.model.Meter;
import org.nomanspace.electricitymeters.util.BinDataDecoder;
import org.nomanspace.electricitymeters.util.LogUtil;
import org.nomanspace.electricitymeters.util.ParsingUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class DatFileParseHandler implements TextPatternHandler {

    private static final String TIMEDATE = "TIMEDATE";

    private List<Concentrator> concentrators;
    private Concentrator currentConcentrator;
    private Meter lastSeenMeterContext = null;
    // Кэш серийных номеров по ключу HOST:ADDR
    private final Map<String, String> serialCache = new HashMap<>();
    private final BinDataDecoder binDataDecoder = new BinDataDecoder();

    @Override
    public List<Concentrator> process(List<String> input) {
        concentrators = new ArrayList<>();

        for (String inputLine : input) {
            if (inputLine.trim().isEmpty()) {
                continue;
            }

            int identTabs = countIdentTabs(inputLine);

            switch (identTabs) {
                case 1:
                    handleRootLvlOne(inputLine);
                    break;
                case 2:
                    handleRootLvlTwo(inputLine);
                    break;
                // Удалён вызов handleRootLvlThree, так как он не нужен на реальных данных
                default:
                    break;
            }
        }
        return concentrators;
    }

    private void handleRootLvlOne(String inputLine) {
        if (inputLine.contains("TYPE=PLC_I_CONCENTRATOR")) {
            currentConcentrator = new Concentrator();
            String concentratorName = getMapFromLine(inputLine).get("ADDR");
            currentConcentrator.setConcentratorName(concentratorName);
            concentrators.add(currentConcentrator);
            lastSeenMeterContext = null;
        }
    }

    private void handleRootLvlTwo(String inputLine) {
        Map<String, String> lineMap = getMapFromLine(inputLine);
        if (lineMap.isEmpty()) {
            return;
        }

        updateMeterContextWithLocation(lineMap);
        if (isMeterStart(lineMap)) {
            startNewMeter(lineMap);
        }
        if (lineMap.containsKey("BINDATA")) {
            processBindata(lineMap);
        }
    }

    // Вынесено: обновление контекста Meter по "Помещение"/"Здание"
    private void updateMeterContextWithLocation(Map<String, String> lineMap) {
        if (lastSeenMeterContext != null) {
            String roomVal = firstNonEmpty(lineMap, "Помещение", "Room", "ROOM", "Rm");
            if (roomVal != null && (lastSeenMeterContext.getRoom() == null || lastSeenMeterContext.getRoom().isEmpty())) {
                lastSeenMeterContext.setRoom(roomVal);
            }
            String bldVal = firstNonEmpty(lineMap, "Здание", "Building", "BUILDING", "Bld", "Bldg");
            if (bldVal != null && (lastSeenMeterContext.getBuilding() == null || lastSeenMeterContext.getBuilding().isEmpty())) {
                lastSeenMeterContext.setBuilding(bldVal);
            }
        }
    }

    // Вынесено: определение начала нового счетчика
    private boolean isMeterStart(Map<String, String> lineMap) {
        return lineMap.containsKey("TYPE") && "PLC_I_METER".equals(lineMap.get("TYPE"));
    }

    // Вынесено: инициализация нового Meter
    private void startNewMeter(Map<String, String> lineMap) {
        lastSeenMeterContext = new Meter();
        lastSeenMeterContext.setAddress(lineMap.get("ADDR"));
        lastSeenMeterContext.setHost(lineMap.get("HOST"));
        String roomValOnType = firstNonEmpty(lineMap, "Помещение", "Room", "ROOM", "Rm");
        if (roomValOnType != null) {
            lastSeenMeterContext.setRoom(roomValOnType);
        }
        String bldValOnType = firstNonEmpty(lineMap, "Здание", "Building", "BUILDING", "Bld", "Bldg");
        if (bldValOnType != null) {
            lastSeenMeterContext.setBuilding(bldValOnType);
        }
        if (lineMap.containsKey(TIMEDATE)) {
            byte[] timestampBytes = ParsingUtils.hexStringToByteArray(lineMap.get(TIMEDATE));
            // Используем новую логику: сырое значение байта + 1 для дня и месяца
            LocalDateTime logTimestamp = ParsingUtils.parseTimestampRawPlusOne(timestampBytes);
            lastSeenMeterContext.setLogTimestamp(logTimestamp);
            LogUtil.debug(String.format("[PARSE] PLC_I_METER TIMEDATE parsed (raw+1) -> %s", logTimestamp));
        }
        LogUtil.debug(String.format(
                "[PARSE] PLC_I_METER: ADDR=%s, HOST=%s, ROOM=%s, BUILDING=%s, TIMEDATE=%s",
                lastSeenMeterContext.getAddress(),
                lastSeenMeterContext.getHost(),
                lastSeenMeterContext.getRoom(),
                lastSeenMeterContext.getBuilding(),
                lineMap.getOrDefault(TIMEDATE, "-")
        ));
    }

    // Вынесено: обработка BINDATA
    private void processBindata(Map<String, String> lineMap) {
        if (lastSeenMeterContext == null) return;
        String bindata = lineMap.get("BINDATA");
        logBindataInfo(bindata);
        List<Meter> decodedMeters = decodeMeters(bindata);
        if (decodedMeters.isEmpty()) return;
        
        // Добавляем ВСЕ записи от декодера, а не только одну "лучшую"
        for (Meter decodedMeter : decodedMeters) {
            Meter combined = createCombinedMeter(lastSeenMeterContext, decodedMeter);
            applySerialNumberFromCacheIfNeeded(combined);
            updateSerialNumberCacheIfNeeded(combined);
            if (currentConcentrator != null) {
                currentConcentrator.addMeter(combined);
            }
        }
    }

    // Вынесено: логирование информации о BINDATA
    private void logBindataInfo(String bindata) {
        LogUtil.debug(String.format("[PARSE] BINDATA обнаружен для ADDR=%s, HOST=%s, длина=%d",
                lastSeenMeterContext.getAddress(),
                lastSeenMeterContext.getHost(),
                bindata != null ? bindata.length() : 0));
    }

    // Вынесено: декодирование списка Meter
    private List<Meter> decodeMeters(String bindata) {
        List<Meter> decodedMeters = binDataDecoder.decode(
                bindata,
                lastSeenMeterContext.getHost(),
                lastSeenMeterContext.getAddress()
        );
        LogUtil.debug(String.format("decodedMeters: %d", decodedMeters.size()));
        return decodedMeters;
    }

    // Вынесено: создание итогового Meter
    private Meter createCombinedMeter(Meter context, Meter chosen) {
        Meter combined = new Meter();
        combined.setAddress(context.getAddress());
        combined.setHost(context.getHost());
        combined.setRoom(context.getRoom());
        combined.setBuilding(context.getBuilding());
        combined.setLogTimestamp(context.getLogTimestamp());
        combined.setSerialNumber(chosen.getSerialNumber());
        combined.setEnergyTotal(chosen.getEnergyTotal());
        combined.setEnergyT1(chosen.getEnergyT1());
        combined.setEnergyT2(chosen.getEnergyT2());
        combined.setEnergyT3(chosen.getEnergyT3());
        combined.setEnergyT4(chosen.getEnergyT4());
        combined.setSignalLevel(chosen.getSignalLevel());
        combined.setLastMeasurementTimestamp(chosen.getLastMeasurementTimestamp());
        return combined;
    }

    // Вынесено: применение серийного номера из кэша
    private void applySerialNumberFromCacheIfNeeded(Meter meter) {
        if ((meter.getSerialNumber() == null || meter.getSerialNumber().isEmpty())
                && meter.getHost() != null && meter.getAddress() != null) {
            String key = meter.getHost() + ":" + meter.getAddress();
            if (serialCache.containsKey(key)) {
                meter.setSerialNumber(serialCache.get(key));
                LogUtil.debug(String.format("Применён кэш для %s -> %s", key, meter.getSerialNumber()));
            }
        }
    }

    // Вынесено: обновление кэша серийных номеров
    private void updateSerialNumberCacheIfNeeded(Meter meter) {
        if (meter.getSerialNumber() != null && !meter.getSerialNumber().isEmpty()
                && meter.getHost() != null && meter.getAddress() != null) {
            String key = meter.getHost() + ":" + meter.getAddress();
            serialCache.put(key, meter.getSerialNumber());
        }
    }

    // УДАЛЕНО: метод selectBestMeter больше не нужен

    // Вспомогательный метод для преобразования байтов в hex-строку
    private static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    private Map<String, String> getMapFromLine(String inputLine) {
        if (inputLine == null || inputLine.trim().isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> lineEntityMap = new HashMap<>();
        String trimmedLine = inputLine.trim();

        try {
            // Выводим hex-представление строки в кодировке CP1251
            StringBuilder hexDump = new StringBuilder("Hex представление (CP1251): ");
            for (byte b : trimmedLine.getBytes("CP1251")) {
                hexDump.append(String.format("%02X ", b));
            }
            LogUtil.debug(hexDump.toString());

            // Конвертируем строку из CP1251 в UTF-8
            String utf8Line = new String(trimmedLine.getBytes("CP1251"), StandardCharsets.UTF_8);
            LogUtil.debug("После конвертации в UTF-8: " + utf8Line);

            // Используем сконвертированную строку для дальнейшей обработки
            trimmedLine = utf8Line;
        } catch (UnsupportedEncodingException e) {
            LogUtil.error("Ошибка при конвертации из CP1251: " + e.getMessage());
            // Продолжаем с оригинальной строкой в случае ошибки
        }

        // Разбиваем строку на пары ключ-значение
        String[] subStringsLine = trimmedLine.split(";");

        for (String subString : subStringsLine) {
            subString = subString.trim();
            if (subString.contains("=")) {
                String[] parts = subString.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    if (!key.isEmpty()) {
                        // Выводим hex-представление ключа и значения
                        LogUtil.debug(String.format("  Ключ: '%s' (hex: %s) = '%s' (hex: %s)",
                                key,
                                bytesToHex(key.getBytes(StandardCharsets.UTF_8)),
                                value,
                                bytesToHex(value.getBytes(StandardCharsets.UTF_8))));
                        lineEntityMap.put(key, value);
                    }
                }
            }
        }

        return lineEntityMap;
    }

    public static int countIdentTabs(String splitLine) {
        int count = 0;
        for (char c : splitLine.toCharArray()) {
            if (c == '\t') {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    // Возвращает первое непустое значение по одному из возможных ключей (регистронезависимо)
    private static String firstNonEmpty(Map<String, String> map, String... keys) {
        if (map == null || map.isEmpty()) {
            LogUtil.debug("firstNonEmpty: пустые входные данные");
            return null;
        }
        LogUtil.debug("Доступные ключи в карте: " + map.keySet());
        LogUtil.debug("Ищем ключи: " + Arrays.toString(keys));
        String result = findExactKey(map, keys);
        if (result != null) return result;
        return findByPrefixKey(map, keys);
    }

    // Точное совпадение ключа
    private static String findExactKey(Map<String, String> map, String... keys) {
        for (String k : keys) {
            if (map.containsKey(k)) {
                String v = map.get(k);
                if (v != null && !v.trim().isEmpty()) {
                    LogUtil.debug(String.format("Найдено точное совпадение: '%s' = '%s'", k, v));
                    return v.trim();
                }
            }
        }
        return null;
    }

    // Совпадение по первым буквам (префикс)
    private static String findByPrefixKey(Map<String, String> map, String... keys) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getKey() == null) continue;
            String entryKey = entry.getKey().trim();
            if (isPrefixMatch(entryKey, keys)) {
                String v = entry.getValue();
                String result = getNonEmptyTrimmedValue(entryKey, v, keys);
                if (result != null) return result;
            }
        }
        LogUtil.debug("Ни один из искомых ключей не найден");
        return null;
    }

    // Вынесено: получение непустого значения с логированием
    private static String getNonEmptyTrimmedValue(String entryKey, String value, String... keys) {
        if (value != null && !value.trim().isEmpty()) {
            LogUtil.debug(String.format("Найдено совпадение по первым буквам: '%s' ~ один из %s = '%s'", entryKey, Arrays.toString(keys), value));
            return value.trim();
        }
        return null;
    }

    // Вынесено: проверка совпадения по префиксу
    private static boolean isPrefixMatch(String entryKey, String... keys) {
        for (String k : keys) {
            if (k == null) continue;
            if (entryKey.regionMatches(true, 0, k, 0, Math.min(3, Math.min(entryKey.length(), k.length())))) {
                return true;
            }
        }
        return false;
    }
}
