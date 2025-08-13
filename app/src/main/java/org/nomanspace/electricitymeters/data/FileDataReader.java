package org.nomanspace.electricitymeters.data;

import org.nomanspace.electricitymeters.path.DatFileSelector;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class FileDataReader {

    public DatFileContent readDataFile() {
        List<String> allLines = new ArrayList<>();
        Path fileToRead = new DatFileSelector().providePath();

        if (fileToRead == null) {
            System.out.println("Warning: No .dat file found to read.");
            // Возвращаем пустой контейнер, если файл не найден
            return new DatFileContent(new ArrayList<>(), "");
        }

        // Получаем имя файла из пути
        String sourceFileName = fileToRead.getFileName().toString();

        try (FileChannel fileChannel = FileChannel.open(fileToRead, StandardOpenOption.READ)) {
            ByteBuffer chunk = ByteBuffer.allocate(8192);
            StringBuilder leftChunkEnd = new StringBuilder();
            while (fileChannel.read(chunk) != -1) {
                String string;
                chunk.flip();//прочитали
                string = leftChunkEnd + StandardCharsets.UTF_8.decode(chunk).toString();
                leftChunkEnd.setLength(0);
                String[] splitLines = string.split("\\R", -1);
                chunk.clear();//почистили

                for (int i = 0; i < splitLines.length - 1; i++) {
                    allLines.add(splitLines[i]);
                }
                leftChunkEnd.append(splitLines[splitLines.length - 1]);
            }

            if (leftChunkEnd.length() > 0) {
                allLines.add(leftChunkEnd.toString());
            }

        } catch (IOException e) {
            // Лучше обработать исключение более грациозно
            e.printStackTrace();
            return new DatFileContent(new ArrayList<>(), sourceFileName); // Возвращаем то, что успели, и имя файла
        }
        // Возвращаем новый контейнер с данными и именем файла
        return new DatFileContent(allLines, sourceFileName);
    }
}