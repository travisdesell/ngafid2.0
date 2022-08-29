package org.ngafid.flights.dji;

import java.util.LinkedList;
import java.util.List;

public class Axis {

    private final String label;

    private final String name;

    private final Units units;

    private static final LinkedList<Axis> axes = new LinkedList<>();

    public Axis(String name, String label) {
        this(name, label, null);
    }

    public Axis(String name, String label, Units units) {
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

    public static List<Axis> getAxes() {
        return axes;
    }
}
