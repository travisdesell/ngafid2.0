package org.ngafid.flights.datcon.Files;

import org.ngafid.flights.datcon.Files.Units;

import java.util.LinkedList;

public class Axis {
    String label = "";

    String name = "";

    org.ngafid.flights.datcon.Files.Units units = null;

    private static LinkedList<Axis> axes = new LinkedList<Axis>();

    public static LinkedList<Axis> getAxes() {
        return axes;
    }

    public Axis(String name, String label) {
        this(name, label, null);
    }

    public Axis(String name, String label, org.ngafid.flights.datcon.Files.Units units) {
        this.label = label;
        this.name = name;
        this.units = units;
        axes.add(this);
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public Units getUnits() {
        return units;
    }
}
