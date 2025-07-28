package org.nomanspace.electricitymeters.data;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.nomanspace.electricitymeters.model.Concentrator;
import org.nomanspace.electricitymeters.model.Meter;
import org.nomanspace.electricitymeters.path.DatFileSelector;

public class FileDataReader {

    public void readDataFile() {
        Path fileToRead = new DatFileSelector().providePath();
        try (FileChannel fileChannel = FileChannel.open(fileToRead, StandardOpenOption.READ)) {
            ByteBuffer chunk = ByteBuffer.allocate(8192);
            StringBuilder leftChunkEnd = new StringBuilder();
            while (fileChannel.read(chunk) != -1) {
                String string;
                chunk.flip();//прочитали
                string = leftChunkEnd.toString() + StandardCharsets.UTF_8.decode(chunk).toString();
                leftChunkEnd.setLength(0);
                String[] splitLines = string.split("\\R", -1);
                chunk.clear();//почистили
                List<Concentrator> concentrators = new ArrayList<>();
                for (int i = 0; i < splitLines.length - 1; i++) {
                    //тут мы делаем что угодно?
                    // может тут вызывать метод для выборки нужных строк для составления таблиц?
                    if (splitLines[i].trim().isEmpty()) {
                        continue;
                    }
                    int identTabs = 0;
                    identTabs = countIdentTabs(splitLines[i]);
                    Concentrator currentConcentrator = null;
                    if (identTabs == 1 && splitLines[i].contains("TYPE=PLC_I_CONCENTRATOR")) {
                        Map<String, String> concentartotMap = new HashMap<>();
                        currentConcentrator = new Concentrator();
                        String[] subStringsLine = splitLines[i].split(";");
                        for (String subString : subStringsLine) {
                            int separatorPosition = subString.indexOf('=');
                            String key = subString.substring(0, separatorPosition).trim();
                            String value = subString.substring(separatorPosition + 1).trim();
                            concentartotMap.put(key, value);
                        }
                        //что я по итогу буду возвращать когда вынесу код в другой класс?
                        currentConcentrator.setConcentratorName(concentartotMap.get("ADDR"));
                    }

                    Meter meter;
                    if (identTabs == 2 && splitLines[i].contains("TYPE=PLC_I_METER")) {
                        String[] subStringsLine = splitLines[i].split(";");
                        Map<String, String> meterMap = new HashMap<>();
                        for (String subString : subStringsLine) {
                            int separatorPosition = subString.indexOf('=');
                            String key = subString.substring(0, separatorPosition).trim();
                            String value = subString.substring(separatorPosition + 1).trim();
                            meterMap.put(key, value);
                        }
                        if (currentConcentrator.getConcentratorName().equals(meterMap.get("HOST"))) {
                            meter = new Meter(meterMap.get("ADDR"),meterMap.get("HOST"), meterMap.get("Помещение"), meterMap.get("Здание"), meterMap.get("TIMEDATE"), meterMap.get("BINDATA"));
                            currentConcentrator.addMeter(meter);
                        }
                    }

                    System.out.println("Processing line: " + splitLines[i]);
                }
                leftChunkEnd.append(splitLines[splitLines.length - 1]);
            }
            if (leftChunkEnd.length() > 0) {
                //тут логика для обработки последней строки, на которую я не зашел в for из-за условия i < длинна-1
                System.out.println("Processing final line: " + leftChunkEnd.toString());
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
