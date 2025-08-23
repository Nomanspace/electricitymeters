
package org.nomanspace.electricitymeters.service;

import org.nomanspace.electricitymeters.model.Concentrator;
import org.nomanspace.electricitymeters.model.Meter;

import java.util.*;
import java.util.stream.Collectors;

import org.nomanspace.electricitymeters.model.*;
import org.nomanspace.electricitymeters.util.LogUtil;

import java.time.format.DateTimeFormatter;

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

        // 3. В каждой группе выбираем запись с самой последней датой.
        // Приоритет выбора:
        // 1. Самая свежая метка времени (lastMeasurementTimestamp).
        // 2. При равенстве времени - запись с наибольшей суммарной энергией (energyTotal).
        List<Meter> latestReadings = new ArrayList<>();
        for (Map.Entry<String, List<Meter>> entry : readingsGroupedById.entrySet()) {
            List<Meter> group = entry.getValue();
            String uniqueId = entry.getKey();

            // --- ВРЕМЕННОЕ ЛОГИРОВАНИЕ ДЛЯ ДИАГНОСТИКИ ---
            if ("2001:7".equals(uniqueId) || (group.stream().anyMatch(m -> "7".equals(m.getAddress()) && "2001".equals(m.getHost())))) {
                LogUtil.info("--- ДИАГНОСТИКА ДЛЯ СЧЕТЧИКА " + uniqueId + " ---");
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                for (int i = 0; i < group.size(); i++) {
                    Meter m = group.get(i);
                    String ts = m.getLastMeasurementTimestamp() != null ? m.getLastMeasurementTimestamp().format(dtf) : "null";
                    Double energy = m.getEnergyTotal();
                    LogUtil.info(String.format("  [%d] Дата: %s, Энергия: %s", i, ts, energy != null ? String.format("%.2f", energy) : "null"));
                }
                LogUtil.info("------------------------------------------");
            }
            // --- КОНЕЦ ВРЕМЕННОГО ЛОГИРОВАНИЯ ---
            
            Comparator<Meter> byTimeThenEnergy = Comparator
                    .comparing(Meter::getLastMeasurementTimestamp, Comparator.nullsFirst(Comparator.naturalOrder()))
                    .thenComparing(Meter::getEnergyTotal, Comparator.nullsFirst(Comparator.naturalOrder()));

            Meter chosen = group.stream()
                    .max(byTimeThenEnergy)
                    .orElse(null);

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
