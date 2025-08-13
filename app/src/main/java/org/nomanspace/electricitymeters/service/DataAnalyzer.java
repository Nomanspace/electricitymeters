
package org.nomanspace.electricitymeters.service;

import org.nomanspace.electricitymeters.model.Concentrator;
import org.nomanspace.electricitymeters.model.Meter;

import java.util.*;
import java.util.stream.Collectors;

public class DataAnalyzer {

    /**
     * Обрабатывает сырой список концентраторов и возвращает список,
     * содержащий только по одному, самому последнему показанию для каждого уникального счетчика.
     *
     * @param allData Список концентраторов с полным набором "сырых" показаний.
     * @return Очищенный список с последними показаниями для каждого счетчика.
     */
    public List<Meter> getLatestReadings(List<Concentrator> allData) {
        if (allData == null || allData.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. Собираем все показания из всех концентраторов в один плоский список
        List<Meter> allReadings = allData.stream()
                .flatMap(concentrator -> concentrator.getMeters().stream())
                .collect(Collectors.toList());

        // 2. Группируем показания по уникальному идентификатору счетчика
        Map<String, List<Meter>> readingsGroupedById = new HashMap<>();
        for (Meter reading : allReadings) {
            // Приоритет - серийный номер. Если его нет - используем адрес.
            String uniqueId = (reading.getSerialNumber() != null && !reading.getSerialNumber().isEmpty()) ?
                    reading.getSerialNumber() : reading.getAddress();

            if (uniqueId != null) {
                readingsGroupedById.computeIfAbsent(uniqueId, k -> new ArrayList<>()).add(reading);
            }
        }

        // 3. В каждой группе находим самое последнее по времени показание
        List<Meter> latestReadings = new ArrayList<>();
        for (List<Meter> group : readingsGroupedById.values()) {
            group.stream()
                    .max(Comparator.comparing(Meter::getLastMeasurementTimestamp, Comparator.nullsLast(Comparator.naturalOrder())))
                    .ifPresent(latestReadings::add);
        }

        return latestReadings;
    }
}
