
module org.nomanspace.electricitymeters {
    requires java.base;
    requires java.desktop;

    // Добавляем зависимости для работы с Excel
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires org.apache.commons.math3;

    exports org.nomanspace.electricitymeters;
    exports org.nomanspace.electricitymeters.path;
    exports org.nomanspace.electricitymeters.model;
    exports org.nomanspace.electricitymeters.service;
}
