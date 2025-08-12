package org.nomanspace.electricitymeters.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Meter {
    //ADDR номер\адрес счетчика
    private String address;
    //номер\адрес хоста\маршрутизатора\концентратора
    private String host;
    //помещение
    private String room;
    //помещение
    private String building;
    //дата
    private String date;
    //bindata
    private String binDate;

    private String serialNumber;
    private Long energyT1;
    private Long energyT2;
    private Long energyT3;
    private Long energyT4;
    private Long energyTotal;
    private Integer signalLevel;
    private LocalDateTime lastMeasurementTimeStamp;


    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public Long getEnergyT1() {
        return energyT1;
    }

    public void setEnergyT1(Long energyT1) {
        this.energyT1 = energyT1;
    }

    public Long getEnergyT2() {
        return energyT2;
    }

    public void setEnergyT2(Long energyT2) {
        this.energyT2 = energyT2;
    }

    public Long getEnergyT3() {
        return energyT3;
    }

    public void setEnergyT3(Long energyT3) {
        this.energyT3 = energyT3;
    }

    public Long getEnergyT4() {
        return energyT4;
    }

    public void setEnergyT4(Long energyT4) {
        this.energyT4 = energyT4;
    }

    public Long getEnergyTotal() {
        return energyTotal;
    }

    public void setEnergyTotal(Long energyTotal) {
        this.energyTotal = energyTotal;
    }

    public Integer getSignalLevel() {
        return signalLevel;
    }

    public void setSignalLevel(Integer signalLevel) {
        this.signalLevel = signalLevel;
    }

    public LocalDateTime getLastMeasurementTimeStamp() {
        return lastMeasurementTimeStamp;
    }

    public void setLastMeasurementTimeStamp(LocalDateTime lastMeasurementTimeStamp) {
        this.lastMeasurementTimeStamp = lastMeasurementTimeStamp;
    }

    public Meter() {

    }

    public Meter(String address, String host, String room, String building, String date, String binDate) {
        this.address = address;
        this.host = host;
        this.room = room;
        this.building = building;
        this.date = date;
        this.binDate = binDate;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Meter meter = (Meter) o;
        return Objects.equals(address, meter.address)
                && Objects.equals(host, meter.host)
                && Objects.equals(room, meter.room)
                && Objects.equals(building, meter.building)
                && Objects.equals(date, meter.date)
                && Objects.equals(binDate, meter.binDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, host, room, building, date, binDate);
    }

    @Override
    public String toString() {
        return "Meter{" +
                "address=" + address +
                ", host=" + host +
                ", room='" + room + '\'' +
                ", building='" + building + '\'' +
                ", date='" + date + '\'' +
                ", binDate='" + binDate + '\'' +
                '}';
    }

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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getBinDate() {
        return binDate;
    }

    public void setBinDate(String binDate) {
        this.binDate = binDate;
    }
}
