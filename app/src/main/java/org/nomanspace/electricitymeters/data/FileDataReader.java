package org.nomanspace.electricitymeters.data;

import org.nomanspace.electricitymeters.path.DatFileSelector;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.nomanspace.electricitymeters.util.LogUtil;

public class FileDataReader {

    public DatFileContent readDataFile() {
        Path fileToRead = new DatFileSelector().providePath();

        if (fileToRead == null) {
            LogUtil.error("Warning: No .dat file found to read.");
            return new DatFileContent(new ArrayList<>(), "");
        }

        String sourceFileName = fileToRead.getFileName().toString();

        try {
            List<String> allLines;
            try {
                allLines = Files.readAllLines(fileToRead, StandardCharsets.UTF_8);
                LogUtil.info("Файл '" + sourceFileName + "' успешно прочитан в UTF-8. Всего строк: " + allLines.size());
            } catch (java.nio.charset.MalformedInputException mie) {
                // Файлы .dat нередко в Windows-1251 — пробуем fallback
                Charset cp1251 = Charset.forName("windows-1251");
                allLines = Files.readAllLines(fileToRead, cp1251);
                LogUtil.info("Файл '" + sourceFileName + "' прочитан в Windows-1251 (fallback). Всего строк: " + allLines.size());
            }
            return new DatFileContent(allLines, sourceFileName);
        } catch (IOException e) {
            LogUtil.error("Ошибка чтения файла: " + sourceFileName);
            e.printStackTrace();
            return new DatFileContent(new ArrayList<>(), sourceFileName);
        }
    }
}
