package org.nomanspace.electricitymeters.text;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.nomanspace.electricitymeters.path.ReportDirCreate;
import org.nomanspace.electricitymeters.util.LogUtil;

public class ReportDirCreateTest {

    @BeforeAll
    static void init() {
        LogUtil.setLoggingEnabled(true);
    }

    @Test
    void testForUnderstandingHowReportDirCreateWork() {
        System.out.println("Тест начался"); // ← Проверить, доходит ли сюда
        ReportDirCreate reportDirCreate = new ReportDirCreate();
        System.out.println("Объект создан"); // ← Проверить, доходит ли сюда
        Path result = reportDirCreate.providePath();
        System.out.println("Метод вызван"); // ← Проверить, доходит ли сюда
        LogUtil.info("ВЫВОД: " + result.toString());
    }

}
