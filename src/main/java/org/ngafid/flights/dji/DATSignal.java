package org.ngafid.flights.dji;

import java.util.LinkedList;

public class DATSignal {
    public enum NumType {
        FLOAT4, DOUBLE, INT, UNDEFINED
    }

    public enum SigType {
        SERIES, STATE, TIMEAXIS, UNDEFINED
    }

    String name = "";

    String description = "";

    SigType sigType = SigType.UNDEFINED;

    private Axis axis = null;

    private NumType numType = NumType.UNDEFINED;

    private String defaultState = null;

    private boolean experimental;

    private Units units = Units.noUnits;

    private static final LinkedList<DATSignal> signals = new LinkedList<>();

    public static LinkedList<DATSignal> getSignals() {
        return signals;
    }

    public static DATSignal State(String name, String description, String defaultState) {
        DATSignal retv = new DATSignal();
        retv.name = name;
        retv.description = description;
        retv.axis = null;
        retv.sigType = SigType.STATE;
        retv.numType = NumType.INT;
        retv.defaultState = defaultState;
        retv.experimental = false;
        retv.units = Units.noUnits;
        return retv;
    }

    public static DATSignal StateExperimental(String name, String description, String defaultState) {
        DATSignal retv = State(name, description, defaultState);
        retv.experimental = true;
        return retv;
    }

    private DATSignal(String name, String description, Axis axis, SigType sigType, NumType numType, Units units) {
        this.name = name;
        this.description = description;
        this.axis = axis;
        this.sigType = sigType;
        this.numType = numType;
        this.experimental = false;
        this.units = units;
        signals.add(this);
    }

    public DATSignal() {
        signals.add(this);
    }

    public static DATSignal SeriesDouble(String name, String description, Axis axis, Units units) {
        return new DATSignal(name, description, axis, SigType.SERIES, NumType.DOUBLE, units);
    }

    public static DATSignal SeriesDouble(String name, int index, String description, Axis axis, Units units) {
        return new DATSignal(name + "(" + index + ")", description, axis, SigType.SERIES, NumType.DOUBLE, units);
    }

    public static DATSignal SeriesDoubleExperimental(String name, String description, Axis axis, Units units) {
        DATSignal retv = new DATSignal(name, description, axis, SigType.SERIES, NumType.DOUBLE, units);
        retv.experimental = true;
        return retv;
    }

    public static DATSignal SeriesDoubleExperimental(String name, int index, String description, Axis axis, Units units) {
        DATSignal retv = new DATSignal(name + "(" + index + ")", description, axis, SigType.SERIES, NumType.DOUBLE, units);
        retv.experimental = true;
        return retv;
    }

    public static DATSignal SeriesFloat(String name, String description, Axis axis, Units units) {
        DATSignal retv = new DATSignal(name, description, axis, SigType.SERIES, NumType.FLOAT4, units);
        return retv;
    }

    public static DATSignal SeriesFloat(String name, int index, String description, Axis axis, Units units) {
        DATSignal retv = new DATSignal(name + "(" + index + ")", description, axis, SigType.SERIES, NumType.FLOAT4, units);
        return retv;
    }

    public static DATSignal SeriesFloatExperimental(String name, String description, Axis axis, Units units) {
        DATSignal retv = new DATSignal(name, description, axis, SigType.SERIES, NumType.FLOAT4, units);
        retv.experimental = true;
        return retv;
    }

    public static DATSignal SeriesFloatExperimental(String name, int index, String description, Axis axis, Units units) {
        DATSignal retv = new DATSignal(name + "(" + index + ")", description, axis, SigType.SERIES, NumType.FLOAT4, units);
        retv.experimental = true;
        return retv;
    }

    public static DATSignal SeriesInt(String name, String description, Axis axis, Units units) {
        return new DATSignal(name, description, axis, SigType.SERIES, NumType.INT, units);
    }

    public static DATSignal SeriesInt(String name, int index, String description, Axis axis, Units units) {
        return new DATSignal(name + "(" + index + ")", description, axis, SigType.SERIES, NumType.INT, units);
    }

    public static DATSignal SeriesIntExperimental(String name, String description, Axis axis, Units units) {
        DATSignal retv = new DATSignal(name, description, axis, SigType.SERIES, NumType.INT, units);
        retv.experimental = true;
        return retv;
    }

    public static DATSignal SeriesIntExperimental(String name, int index, String description, Axis axis, Units units) {
        DATSignal retv = new DATSignal(name + "(" + index + ")", description, axis, SigType.SERIES, NumType.INT, units);
        retv.experimental = true;
        return retv;
    }

    public String getDescription() {
        return description;
    }

    public NumType getNumType() {
        return numType;
    }

    public Axis getAxis() {
        return axis;
    }

    public String getName() {
        return name;
    }

    public SigType getType() {
        return sigType;
    }

    public String getDefaultState() {
        return defaultState;
    }

    public boolean isExperimental() {
        return experimental;
    }

    public Units getUnits() {
        return units;
    }

    public boolean hasUnits() {
        return (units != Units.noUnits);
    }

    public String getUnitsNoComma() {
        return units.toString().replaceAll(",", ";");
    }

}
