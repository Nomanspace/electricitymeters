

package org.nomanspace.electricitymeters.text;

import org.nomanspace.electricitymeters.model.Concentrator;
import org.nomanspace.electricitymeters.model.Meter;
import org.nomanspace.electricitymeters.util.BinDataDecoder;
import org.nomanspace.electricitymeters.util.ParsingUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatFileParseHandler implements TextPatternHandler {

    private List<Concentrator> concentrators;
    private Concentrator currentConcentrator;
    private Meter lastSeenMeterContext = null;
    private final BinDataDecoder binDataDecoder = new BinDataDecoder();

    @Override
    public List<Concentrator> process(List<String> input) {
        concentrators = new ArrayList<>();
        String pendingBindata = null; // Для "склеивания" BINDATA

        for (String inputLine : input) {
            if (inputLine.trim().isEmpty()) {
                continue;
            }

            // Логика "склеивания" BINDATA
            if (pendingBindata != null) {
                inputLine = pendingBindata + inputLine.trim();
                pendingBindata = null; // Сбрасываем после использования
            }
            if (inputLine.contains("BINDATA=") && !inputLine.endsWith(";")) {
                // Если строка выглядит как начало BINDATA, но не закончена
                pendingBindata = inputLine;
                continue; // Переходим к следующей строке для склеивания
            }

            int identTabs = countIdentTabs(inputLine);
            switch (identTabs) {
                case 1:
                    handleRootLvlOne(inputLine);
                    break;
                case 2:
                    handleRootLvlTwo(inputLine);
                    break;
            }
        }
        return concentrators;
    }

    private void handleRootLvlOne(String inputLine) {
        if (inputLine.contains("TYPE=PLC_I_CONCENTRATOR")) {
            currentConcentrator = new Concentrator();
            currentConcentrator.setConcentratorName(getMapFromLine(inputLine).get("ADDR"));
            concentrators.add(currentConcentrator);
            lastSeenMeterContext = null; // Сбрасываем контекст при смене концентратора
        }
    }

    private void handleRootLvlTwo(String inputLine) {
        Map<String, String> lineMap = getMapFromLine(inputLine);

        // 1. Если это строка с контекстом, сохраняем его
        if (lineMap.containsKey("TYPE") && lineMap.get("TYPE").equals("PLC_I_METER")) {
            lastSeenMeterContext = new Meter();
            lastSeenMeterContext.setAddress(lineMap.get("ADDR"));
            lastSeenMeterContext.setHost(lineMap.get("HOST"));
            lastSeenMeterContext.setRoom(lineMap.get("Помещение"));
            lastSeenMeterContext.setBuilding(lineMap.get("Здание"));

            // 2. Парсим TIMEDATE
            if (lineMap.containsKey("TIMEDATE")) {
                byte[] timestampBytes = ParsingUtils.hexStringToByteArray(lineMap.get("TIMEDATE"));
                LocalDateTime logTimestamp = ParsingUtils.parseTimestamp(timestampBytes);
                lastSeenMeterContext.setLogTimestamp(logTimestamp);
            }
        }

        // 3. Если это строка с BINDATA и у нас есть контекст
        if (lineMap.containsKey("BINDATA") && lastSeenMeterContext != null) {
            String bindata = lineMap.get("BINDATA");
            List<Meter> decodedMeters = binDataDecoder.decode(bindata);

            // 4. Обогащаем каждую запись контекстом
            for (Meter decodedMeter : decodedMeters) {
                decodedMeter.setAddress(lastSeenMeterContext.getAddress());
                decodedMeter.setHost(lastSeenMeterContext.getHost());
                decodedMeter.setRoom(lastSeenMeterContext.getRoom());
                decodedMeter.setBuilding(lastSeenMeterContext.getBuilding());
                decodedMeter.setLogTimestamp(lastSeenMeterContext.getLogTimestamp());

                if (currentConcentrator != null && currentConcentrator.getConcentratorName().equals(decodedMeter.getHost())) {
                    currentConcentrator.addMeter(decodedMeter);
                }
            }
            // Сбрасываем контекст после использования, чтобы он не применился к следующей BINDATA без своего контекста
            lastSeenMeterContext = null;
        }
    }

    private Map<String, String> getMapFromLine(String inputLine) {
        Map<String, String> lineEntityMap = new HashMap<>();
        String[] subStringsLine = inputLine.trim().split(";");
        for (String subString : subStringsLine) {
            if (subString.contains("=")) {
                int separatorPosition = subString.indexOf('=');
                String key = subString.substring(0, separatorPosition).trim();
                String value = subString.substring(separatorPosition + 1).trim();
                lineEntityMap.put(key, value);
            }
        }
        return lineEntityMap;
    }

    private int countIdentTabs(String splitLine) {
        int count = 0;
        for (char c : splitLine.toCharArray()) {
            if (c == '\t') { // Считаем реальные табы
                count++;
            } else {
                break; // Прекращаем, как только встретили не-таб символ
            }
        }
        return count;
    }
}
