package org.nomanspace.electricitymeters.data;

import org.nomanspace.electricitymeters.path.DatFileSelector;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class FileDataReader {

    public List<String> readDataFile() {
        List<String> allLines = new ArrayList<>();
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

                for (int i = 0; i < splitLines.length - 1; i++) {
                    allLines.add(splitLines[i]);
                }
                leftChunkEnd.append(splitLines[splitLines.length - 1]);
            }

            if (leftChunkEnd.length() > 0) {
                //тут логика для обработки последней строки, на которую я не зашел в for из-за условия i < длинна-1
                allLines.add(leftChunkEnd.toString());
                System.out.println("Processing final line: " + leftChunkEnd.toString());
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return allLines;
    }

}
