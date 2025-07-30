package org.nomanspace.electricitymeters.text;

import org.nomanspace.electricitymeters.model.Concentrator;
import org.nomanspace.electricitymeters.model.Meter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatFileParseHandler implements TextPatternHandler {
    private List<Concentrator> concentrators;
    private Concentrator currentConcentrator;

    @Override
    public List<Concentrator> process(List<String> input) {

        concentrators = new ArrayList<>();

        for (String inputLine : input) {
            if (inputLine.trim().isEmpty()) {
                continue;
            }



            System.out.println("Processing line: " + inputLine);

            int identTabs;
            identTabs = countIdentTabs(inputLine);

            switch (identTabs) {
                case 1:
                    handleRootlvlOne(inputLine);
                    break;

                case 2:
                    handleRootlvlTwo(inputLine);
                    break;
            }

        }

        return concentrators;
    }

    private Meter getMeter(Map<String, String> meterMap) {
        return new Meter(meterMap.get("ADDR"),
                meterMap.get("HOST"),
                meterMap.get("Помещение"),
                meterMap.get("Здание"),
                meterMap.get("TIMEDATE"),
                meterMap.get("BINDATA"));
    }

    private void createAndAddConcentrator(String inputLine) {
        currentConcentrator = new Concentrator();
        currentConcentrator.setConcentratorName(getConcentratorADDR(inputLine));
        concentrators.add(currentConcentrator);
    }

    private String getConcentratorADDR(String inputLine) {
        Map<String, String> concentartotMap = getMapFromLine(inputLine);
        return concentartotMap.get("ADDR");
    }

    private Map<String, String> getMapFromLine(String inputLine) {
        Map<String, String> lineEntityMap = new HashMap<>();
        String[] subStringsLine = inputLine.split(";");
        for (String subString : subStringsLine) {
            if (!subString.contains("=")) {
                continue;
            }
            int separatorPosition = subString.indexOf('=');
            String key = subString.substring(0, separatorPosition).trim();
            String value = subString.substring(separatorPosition + 1).trim();
            lineEntityMap.put(key, value);
        }
        return lineEntityMap;
    }

    /**
     * Считает количество табов в начале строки для определения уровня вложенности
     */
    private int countIdentTabs(String splitLine) {
        int count = 0;
        for (char c : splitLine.toCharArray()) {
            if (c == '\t') {
                count++;
            }
        }
        return count;
    }

    private void handleRootlvlOne(String inputLine) {
        if (inputLine.contains("TYPE=PLC_I_CONCENTRATOR")) {
            createAndAddConcentrator(inputLine);
        }
    }

    private void handleRootlvlTwo(String inputLine) {
        if (inputLine.contains("TYPE=PLC_I_METER")) {
            Map<String, String> meterMap = getMapFromLine(inputLine);
            if (currentConcentrator.getConcentratorName().equals(meterMap.get("HOST"))) {
                currentConcentrator.addMeter(getMeter(meterMap));
            }
        }
    }
}
