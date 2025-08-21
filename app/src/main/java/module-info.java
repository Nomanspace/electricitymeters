module org.nomanspace.electricitymeters {
    requires java.base;
    requires java.desktop;
    requires java.logging;

    // Библиотеки без module-info: используем automatic module names
    // Apache Commons Math 3 -> automatic module name (derived): commons.math3
    requires commons.math3;
    // Guava предоставляет automatic module name: com.google.common (при необходимости)
    requires com.google.common;

    // Модули Apache POI
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;

    // Модули Picocli
    requires info.picocli;

    // Экспортируем наши пакеты
    exports org.nomanspace.electricitymeters;
    exports org.nomanspace.electricitymeters.data;
    exports org.nomanspace.electricitymeters.model;
    exports org.nomanspace.electricitymeters.path;
    exports org.nomanspace.electricitymeters.service;
    exports org.nomanspace.electricitymeters.text;
    exports org.nomanspace.electricitymeters.util;
}