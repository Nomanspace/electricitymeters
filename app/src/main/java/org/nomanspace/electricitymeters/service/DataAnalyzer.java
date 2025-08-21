
package org.nomanspace.electricitymeters.service;

import org.nomanspace.electricitymeters.model.Concentrator;
import org.nomanspace.electricitymeters.model.Meter;

import java.util.*;
import java.util.stream.Collectors;

import org.nomanspace.electricitymeters.model.*;
import org.nomanspace.electricitymeters.util.LogUtil;

public class DataAnalyzer {

    /**
     * Обрабатывает сырой список концентраторов и возвращает список,
     * содержащий только по одному, самому последнему показанию для каждого уникального счетчика.
     *
     * @param allData Список концентраторов с полным набором "сырых" показаний.
     * @return Очищенный список с последними показаниями для каждого счетчика.
     */
    public List<Meter> getLatestReadings(List<Concentrator> allData) {
        LogUtil.info("Анализ данных и отбор последних показаний...");
        
        if (allData == null || allData.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. Собираем все показания из всех концентраторов в один плоский список
        List<Meter> allReadings = allData.stream()
                .flatMap(concentrator -> concentrator.getMeters().stream())
                .collect(Collectors.toList());
        
        LogUtil.info("Всего записей счетчиков до обработки: " + allReadings.size());

        // 2. Группируем показания по уникальному идентификатору счетчика
        Map<String, List<Meter>> readingsGroupedById = new HashMap<>();
        for (Meter reading : allReadings) {
            // Приоритет - серийный номер. Если его нет - используем связку HOST:ADDR, чтобы не смешивать одинаковые адреса на разных концентраторах.
            String uniqueId;
            if (reading.getSerialNumber() != null && !reading.getSerialNumber().isEmpty()) {
                uniqueId = reading.getSerialNumber();
            } else if (reading.getHost() != null && reading.getAddress() != null) {
                uniqueId = reading.getHost() + ":" + reading.getAddress();
            } else {
                uniqueId = reading.getAddress();
            }

            if (uniqueId != null) {
                readingsGroupedById.computeIfAbsent(uniqueId, k -> new ArrayList<>()).add(reading);
            }
        }

        // 3. В каждой группе выбираем запись с МАКСИМАЛЬНОЙ суммарной энергией (0x4F),
        // при равенстве энергий — с наиболее поздней меткой времени из бин-данных.
        // Если суммарной энергии нет ни в одной записи группы, откатываемся к правилу "самая поздняя по времени".
        List<Meter> latestReadings = new ArrayList<>();
        for (List<Meter> group : readingsGroupedById.values()) {
            Comparator<Meter> byEnergyThenTime = Comparator
                    .comparing((Meter m) -> m.getEnergyTotal(), Comparator.nullsFirst(Comparator.naturalOrder()))
                    .thenComparing(Meter::getLastMeasurementTimestamp, Comparator.nullsLast(Comparator.naturalOrder()));

            // Найдем максимум по суммарной энергии (учитываем, что null < любое значение)
            Optional<Meter> maxByEnergy = group.stream()
                    .filter(m -> m.getEnergyTotal() != null)
                    .max(byEnergyThenTime);

            Meter chosen;
            if (maxByEnergy.isPresent()) {
                chosen = maxByEnergy.get();
            } else {
                // Фолбэк: если нет energyTotal, берем самую позднюю по времени
                chosen = group.stream()
                        .max(Comparator.comparing(Meter::getLastMeasurementTimestamp, Comparator.nullsLast(Comparator.naturalOrder())))
                        .orElse(null);
            }

            if (chosen != null) {
                if ((chosen.getSerialNumber() == null || chosen.getSerialNumber().isEmpty())) {
                    // Попробуем подтянуть серийный из любой записи группы
                    String serialFromGroup = group.stream()
                            .map(Meter::getSerialNumber)
                            .filter(s -> s != null && !s.isEmpty())
                            .findFirst()
                            .orElse(null);
                    if (serialFromGroup != null) {
                        chosen.setSerialNumber(serialFromGroup);
                    }
                }

                // Добиваем метаданные: если у выбранной записи нет "Здание"/"Помещение",
                // подставим из любой непустой записи группы (они происходят из одного PLC_I_METER блока)
                if (chosen.getBuilding() == null || chosen.getBuilding().isEmpty()) {
                    String buildingFromGroup = group.stream()
                            .map(Meter::getBuilding)
                            .filter(b -> b != null && !b.isEmpty())
                            .findFirst()
                            .orElse(null);
                    if (buildingFromGroup != null) {
                        chosen.setBuilding(buildingFromGroup);
                    }
                }
                if (chosen.getRoom() == null || chosen.getRoom().isEmpty()) {
                    String roomFromGroup = group.stream()
                            .map(Meter::getRoom)
                            .filter(r -> r != null && !r.isEmpty())
                            .findFirst()
                            .orElse(null);
                    if (roomFromGroup != null) {
                        chosen.setRoom(roomFromGroup);
                    }
                }
                latestReadings.add(chosen);
            }
        }

        return latestReadings;
    }
}
