
package org.nomanspace.electricitymeters.data;

import java.util.List;

/**
 * Простой класс-контейнер (record) для передачи содержимого файла и его имени.
 * @param lines Список строк из файла.
 * @param sourceFileName Имя исходного файла.
 */
public record DatFileContent(List<String> lines, String sourceFileName) {
}
