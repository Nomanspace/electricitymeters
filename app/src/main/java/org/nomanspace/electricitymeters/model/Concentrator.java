package org.nomanspace.electricitymeters.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Concentrator {
    //ADDR
    private String concentratorName;
    private List<Meter> meters;

    public Concentrator() {
        //this.concentratorName = concentratorName;
        this.concentratorName = null;
        this.meters = new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Concentrator that = (Concentrator) o;
        return Objects.equals(concentratorName, that.concentratorName) && Objects.equals(meters, that.meters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(concentratorName, meters);
    }

    @Override
    public String toString() {
        return "Concentrator{" +
                "concentratorName='" + concentratorName + '\'' +
                ", meters=" + meters +
                '}';
    }

    public String getConcentratorName() {
        return concentratorName;
    }

    public void setConcentratorName(String concentratorName) {
        this.concentratorName = concentratorName;
    }

    public List<Meter> getMeters() {
        return meters;
    }

    public void setMeters(List<Meter> meters) {
        this.meters = meters;
    }

    public void addMeter(Meter meter) {
        meters.add(meter);
    }
}
