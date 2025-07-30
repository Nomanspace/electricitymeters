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

            int identTabs = 0;
            identTabs = countIdentTabs(inputLine);

            //тут меню с выбором метода в зависимости от вложенности таба

            if (identTabs == 1 && inputLine.contains("TYPE=PLC_I_CONCENTRATOR")) {
                getCurrentConcentrator(inputLine);
            }


            if (identTabs == 2 && inputLine.contains("TYPE=PLC_I_METER")) {
                Map<String, String> meterMap = getMapFromLine(inputLine);
                if (currentConcentrator.getConcentratorName().equals(meterMap.get("HOST"))) {
                    currentConcentrator.addMeter(getMeter(meterMap));
                }
            }

            System.out.println("Processing line: " + inputLine);
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

    private void getCurrentConcentrator(String inputLine) {
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
            int separatorPosition = subString.indexOf('=');
            String key = subString.substring(0, separatorPosition).trim();
            String value = subString.substring(separatorPosition + 1).trim();
            lineEntityMap.put(key, value);
        }
        return lineEntityMap;
    }

    private int countIdentTabs(String splitLine) {
        int count = 0;
        for (char c : splitLine.toCharArray()) {
            if (c == '\t') {
                count++;
            }
        }
        return count;
    }
}
