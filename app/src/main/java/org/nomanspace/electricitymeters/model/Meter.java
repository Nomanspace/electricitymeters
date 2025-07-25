package org.nomanspace.electricitymeters.model;

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
