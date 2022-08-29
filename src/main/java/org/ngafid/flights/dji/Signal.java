package org.ngafid.flights.dji;

import java.util.LinkedList;

public class Signal {
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

    private static final LinkedList<Signal> signals = new LinkedList<>();

    public static LinkedList<Signal> getSignals() {
        return signals;
    }

    public static Signal State(String name, String description, String defaultState) {
        Signal retv = new Signal();
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

    public static Signal StateExperimental(String name, String description, String defaultState) {
        Signal retv = State(name, description, defaultState);
        retv.experimental = true;
        return retv;
    }

    private Signal(String name, String description, Axis axis, SigType sigType, NumType numType, Units units) {
        this.name = name;
        this.description = description;
        this.axis = axis;
        this.sigType = sigType;
        this.numType = numType;
        this.experimental = false;
        this.units = units;
        signals.add(this);
    }

    public Signal() {
        signals.add(this);
    }

    public static Signal SeriesDouble(String name, String description, Axis axis, Units units) {
        return new Signal(name, description, axis, SigType.SERIES, NumType.DOUBLE, units);
    }

    public static Signal SeriesDouble(String name, int index, String description, Axis axis, Units units) {
        return new Signal(name + "(" + index + ")", description, axis, SigType.SERIES, NumType.DOUBLE, units);
    }

    public static Signal SeriesDoubleExperimental(String name, String description, Axis axis, Units units) {
        Signal retv = new Signal(name, description, axis, SigType.SERIES, NumType.DOUBLE, units);
        retv.experimental = true;
        return retv;
    }

    public static Signal SeriesDoubleExperimental(String name, int index, String description, Axis axis, Units units) {
        Signal retv = new Signal(name + "(" + index + ")", description, axis, SigType.SERIES, NumType.DOUBLE, units);
        retv.experimental = true;
        return retv;
    }

    public static Signal SeriesFloat(String name, String description, Axis axis, Units units) {
        Signal retv = new Signal(name, description, axis, SigType.SERIES, NumType.FLOAT4, units);
        return retv;
    }

    public static Signal SeriesFloat(String name, int index, String description, Axis axis, Units units) {
        Signal retv = new Signal(name + "(" + index + ")", description, axis, SigType.SERIES, NumType.FLOAT4, units);
        return retv;
    }

    public static Signal SeriesFloatExperimental(String name, String description, Axis axis, Units units) {
        Signal retv = new Signal(name, description, axis, SigType.SERIES, NumType.FLOAT4, units);
        retv.experimental = true;
        return retv;
    }

    public static Signal SeriesFloatExperimental(String name, int index, String description, Axis axis, Units units) {
        Signal retv = new Signal(name + "(" + index + ")", description, axis, SigType.SERIES, NumType.FLOAT4, units);
        retv.experimental = true;
        return retv;
    }

    public static Signal SeriesInt(String name, String description, Axis axis, Units units) {
        return new Signal(name, description, axis, SigType.SERIES, NumType.INT, units);
    }

    public static Signal SeriesInt(String name, int index, String description, Axis axis, Units units) {
        return new Signal(name + "(" + index + ")", description, axis, SigType.SERIES, NumType.INT, units);
    }

    public static Signal SeriesIntExperimental(String name, String description, Axis axis, Units units) {
        Signal retv = new Signal(name, description, axis, SigType.SERIES, NumType.INT, units);
        retv.experimental = true;
        return retv;
    }

    public static Signal SeriesIntExperimental(String name, int index, String description, Axis axis, Units units) {
        Signal retv = new Signal(name + "(" + index + ")", description, axis, SigType.SERIES, NumType.INT, units);
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
