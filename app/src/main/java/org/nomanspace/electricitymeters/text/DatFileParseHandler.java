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

            String currentLine = inputLine;

            int identTabs = countIdentTabs(currentLine);

            switch (identTabs) {
                case 1:
                    handleRootLvlOne(currentLine);
                    break;
                case 2:
                    handleRootLvlTwo(currentLine);
                    break;
                case 3:
                    handleRootLvlThree(currentLine);
                    break;
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

    // Атрибуты, которые могут следовать отдельными строками внутри одного PLC_I_METER блока (indent=3)
    private void handleRootLvlThree(String inputLine) {
        Map<String, String> lineMap = getMapFromLine(inputLine);
        if (lineMap.isEmpty() || lastSeenMeterContext == null) {
            return;
        }

        // Подхватываем метаданные, если встретились как дочерние записи
        String roomVal = firstNonEmpty(lineMap, "Помещение", "Room", "ROOM", "Rm");
        if (roomVal != null && (lastSeenMeterContext.getRoom() == null || lastSeenMeterContext.getRoom().isEmpty())) {
            lastSeenMeterContext.setRoom(roomVal);
        }
        String bldVal = firstNonEmpty(lineMap, "Здание", "Building", "BUILDING", "Bld", "Bldg");
        if (bldVal != null && (lastSeenMeterContext.getBuilding() == null || lastSeenMeterContext.getBuilding().isEmpty())) {
            lastSeenMeterContext.setBuilding(bldVal);
        }
    }

    private void handleRootLvlTwo(String inputLine) {
        Map<String, String> lineMap = getMapFromLine(inputLine);
        if (lineMap.isEmpty()) {
            return;
        }

        // Независимо от строки TYPE, если на этом уровне приходят поля "Здание"/"Помещение",
        // обновим текущий контекст счётчика (если он уже открыт).
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

        if (lineMap.containsKey("TYPE") && lineMap.get("TYPE").equals("PLC_I_METER")) {
            lastSeenMeterContext = new Meter();
            lastSeenMeterContext.setAddress(lineMap.get("ADDR"));
            lastSeenMeterContext.setHost(lineMap.get("HOST"));
            // Доп. контекст из лога
            String roomValOnType = firstNonEmpty(lineMap, "Помещение", "Room", "ROOM", "Rm");
            if (roomValOnType != null) {
                lastSeenMeterContext.setRoom(roomValOnType);
            }
            String bldValOnType = firstNonEmpty(lineMap, "Здание", "Building", "BUILDING", "Bld", "Bldg");
            if (bldValOnType != null) {
                lastSeenMeterContext.setBuilding(bldValOnType);
            }

            if (lineMap.containsKey("TIMEDATE")) {
                byte[] timestampBytes = ParsingUtils.hexStringToByteArray(lineMap.get("TIMEDATE"));
                LocalDateTime logTimestamp = ParsingUtils.parseTimestamp(timestampBytes);
                lastSeenMeterContext.setLogTimestamp(logTimestamp);
                LogUtil.debug(String.format("[PARSE] PLC_I_METER TIMEDATE parsed -> %s", String.valueOf(logTimestamp)));
            }

            // Логируем обнаружение строки счетчика
            LogUtil.debug(String.format(
                "[PARSE] PLC_I_METER: ADDR=%s, HOST=%s, ROOM=%s, BUILDING=%s, TIMEDATE=%s",
                lastSeenMeterContext.getAddress(),
                lastSeenMeterContext.getHost(),
                lastSeenMeterContext.getRoom(),
                lastSeenMeterContext.getBuilding(),
                lineMap.getOrDefault("TIMEDATE", "-")
            ));
        }

        if (lineMap.containsKey("BINDATA")) {
            if(lastSeenMeterContext != null) {
                String bindata = lineMap.get("BINDATA");
                LogUtil.debug(String.format("[PARSE] BINDATA обнаружен для ADDR=%s, HOST=%s, длина=%d",
                        lastSeenMeterContext.getAddress(),
                        lastSeenMeterContext.getHost(),
                        bindata != null ? bindata.length() : 0));
                List<Meter> decodedMeters = binDataDecoder.decode(
                        bindata,
                        lastSeenMeterContext.getHost(),
                        lastSeenMeterContext.getAddress()
                );
                LogUtil.debug(String.format("decodedMeters: %d", decodedMeters.size()));

                // Внутри одного BINDATA выбираем запись с МАКСИМАЛЬНОЙ суммарной энергией (0x4F),
                // при равенстве — с наиболее поздней меткой времени; если 0x4F нет, берём самую позднюю по времени.
                Comparator<Meter> byEnergyThenTime = Comparator
                        .comparing((Meter m) -> m.getEnergyTotal(), Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(Meter::getLastMeasurementTimestamp, Comparator.nullsLast(Comparator.naturalOrder()));

                Optional<Meter> bestByEnergy = decodedMeters.stream()
                        .filter(m -> m.getEnergyTotal() != null)
                        .max(byEnergyThenTime);

                Meter chosenInPacket = bestByEnergy.orElseGet(() ->
                        decodedMeters.stream()
                                .max(Comparator.comparing(Meter::getLastMeasurementTimestamp, Comparator.nullsLast(Comparator.naturalOrder())))
                                .orElse(null)
                );

                if (chosenInPacket == null) {
                    // Нет полезных данных — пропускаем этот пакет
                    return;
                }

                // Объединяем: переносим контекст PLC и поля из выбранной записи
                Meter combined = new Meter();
                combined.setAddress(lastSeenMeterContext.getAddress());
                combined.setHost(lastSeenMeterContext.getHost());
                combined.setRoom(lastSeenMeterContext.getRoom());
                combined.setBuilding(lastSeenMeterContext.getBuilding());
                combined.setLogTimestamp(lastSeenMeterContext.getLogTimestamp());

                // Поля из выбранной записи (энергии и метка времени — строго из неё)
                combined.setSerialNumber(chosenInPacket.getSerialNumber());
                combined.setEnergyTotal(chosenInPacket.getEnergyTotal());
                combined.setEnergyT1(chosenInPacket.getEnergyT1());
                combined.setEnergyT2(chosenInPacket.getEnergyT2());
                combined.setEnergyT3(chosenInPacket.getEnergyT3());
                combined.setEnergyT4(chosenInPacket.getEnergyT4());
                combined.setSignalLevel(chosenInPacket.getSignalLevel());
                combined.setLastMeasurementTimestamp(chosenInPacket.getLastMeasurementTimestamp());

                // Если серийник в этом пакете отсутствует — попробуем достать из кэша
                if ((combined.getSerialNumber() == null || combined.getSerialNumber().isEmpty())
                        && combined.getHost() != null && combined.getAddress() != null) {
                    String key = combined.getHost() + ":" + combined.getAddress();
                    if (serialCache.containsKey(key)) {
                        combined.setSerialNumber(serialCache.get(key));
                        LogUtil.debug(String.format("Применён кэш для %s -> %s", key, combined.getSerialNumber()));
                    }
                }

                // Обновим кэш, если в пакете появился серийный
                if (combined.getSerialNumber() != null && !combined.getSerialNumber().isEmpty()
                        && combined.getHost() != null && combined.getAddress() != null) {
                    String key = combined.getHost() + ":" + combined.getAddress();
                    serialCache.put(key, combined.getSerialNumber());
                }

                if (currentConcentrator != null) {
                    currentConcentrator.addMeter(combined);
                }
                // Не сбрасываем контекст: для одного PLC_I_METER может быть несколько BINDATA (например, отдельный пакет с серийником 0x48)
            }
        }
    }

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
        
        // Сначала проверяем точное совпадение
        for (String k : keys) {
            if (map.containsKey(k)) {
                String v = map.get(k);
                if (v != null && !v.trim().isEmpty()) {
                    LogUtil.debug(String.format("Найдено точное совпадение: '%s' = '%s'", k, v));
                    return v.trim();
                }
            }
        }
        
        // Если точного совпадения нет, ищем по первым буквам
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getKey() == null) continue;
            
            String entryKey = entry.getKey().trim();
            for (String k : keys) {
                if (k == null) continue;
                
                if (entryKey.regionMatches(true, 0, k, 0, Math.min(3, Math.min(entryKey.length(), k.length())))) {
                    String v = entry.getValue();
                    if (v != null && !v.trim().isEmpty()) {
                        LogUtil.debug(String.format("Найдено совпадение по первым буквам: '%s' ~ '%s' = '%s'", 
                            entryKey, k, v));
                        return v.trim();
                    }
                }
            }
        }
        
        LogUtil.debug("Ни один из искомых ключей не найден");
        return null;
    }
}
