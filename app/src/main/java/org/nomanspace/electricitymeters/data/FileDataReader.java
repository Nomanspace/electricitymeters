package org.nomanspace.electricitymeters.data;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

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
                String[] splitedLines = string.split("\\R", -1);
                chunk.clear();//почистили
                for (int i = 0; i < splitedLines.length - 1; i++) {
                    //тут мы делаем, что угодно?
                    // может тут вызывать метод для выборки нужных строк для составления таблиц?
                    if (splitedLines[i].trim().isEmpty()) {
                        continue;
                    }
                    int identTabs = 0;
                    identTabs = countIdentTabs(splitedLines[i]);

                    if (identTabs == 1 && splitedLines[i].contains("TYPE=PLC_I_CONCENTRATOR")) {
                        String[] subStringsLine = splitedLines[i].split(";");

                    }

                    System.out.println("Processing line: " + splitedLines[i]);
                }
                leftChunkEnd.append(splitedLines[splitedLines.length - 1]);
            }
            if (leftChunkEnd.length() > 0) {
                //тут логика для обработки последней строки, на которую я не зашел в for из-за условия i < длинна-1
                System.out.println("Processing final line: " + leftChunkEnd.toString());
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private int countIdentTabs(String splitedLine) {
        int count = 0;
        for (char c : splitedLine.toCharArray()) {
            if(c == '\t') {
                count++;
            }
        }
        return count;
    }
}
