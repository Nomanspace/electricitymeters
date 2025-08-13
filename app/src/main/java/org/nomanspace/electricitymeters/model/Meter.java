
package org.nomanspace.electricitymeters.model;

import java.time.LocalDateTime;

public class Meter {

    // Контекстные данные (из строки лога)
    private String address; // ADDR
    private String host; // HOST
    private String room; // Помещение
    private String building; // Здание
    private LocalDateTime logTimestamp; // TIMEDATE (время опроса концентратора)

    // Данные из BINDATA (конкретное измерение)
    private String serialNumber;
    private Double energyT1;
    private Double energyT2;
    private Double energyT3;
    private Double energyT4;
    private Double energyTotal;
    private Integer signalLevel;
    private LocalDateTime lastMeasurementTimestamp; // Внутренняя дата из 11-байтной записи

    // Пустой конструктор
    public Meter() {
    }

    // Геттеры и сеттеры

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public LocalDateTime getLogTimestamp() {
        return logTimestamp;
    }

    public void setLogTimestamp(LocalDateTime logTimestamp) {
        this.logTimestamp = logTimestamp;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public Double getEnergyT1() {
        return energyT1;
    }

    public void setEnergyT1(Double energyT1) {
        this.energyT1 = energyT1;
    }

    public Double getEnergyT2() {
        return energyT2;
    }

    public void setEnergyT2(Double energyT2) {
        this.energyT2 = energyT2;
    }

    public Double getEnergyT3() {
        return energyT3;
    }

    public void setEnergyT3(Double energyT3) {
        this.energyT3 = energyT3;
    }

    public Double getEnergyT4() {
        return energyT4;
    }

    public void setEnergyT4(Double energyT4) {
        this.energyT4 = energyT4;
    }

    public Double getEnergyTotal() {
        return energyTotal;
    }

    public void setEnergyTotal(Double energyTotal) {
        this.energyTotal = energyTotal;
    }

    public Integer getSignalLevel() {
        return signalLevel;
    }

    public void setSignalLevel(Integer signalLevel) {
        this.signalLevel = signalLevel;
    }

    public LocalDateTime getLastMeasurementTimestamp() {
        return lastMeasurementTimestamp;
    }

    public void setLastMeasurementTimestamp(LocalDateTime lastMeasurementTimestamp) {
        this.lastMeasurementTimestamp = lastMeasurementTimestamp;
    }

    @Override
    public String toString() {
        return "Meter{" +
                "address='" + address + '\'' +
                ", host='" + host + '\'' +
                ", room='" + room + '\'' +
                ", building='" + building + '\'' +
                ", logTimestamp=" + logTimestamp +
                ", serialNumber='" + serialNumber + '\'' +
                ", energyT1=" + energyT1 +
                ", energyT2=" + energyT2 +
                ", energyT3=" + energyT3 +
                ", energyT4=" + energyT4 +
                ", energyTotal=" + energyTotal +
                ", signalLevel=" + signalLevel +
                ", lastMeasurementTimestamp=" + lastMeasurementTimestamp +
                '}';
    }
}
