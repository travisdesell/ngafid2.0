package org.ngafid.common;


public class MutableDouble {
    private double value;

    public MutableDouble() {
        value = Double.NaN;
    }

    public MutableDouble(double value) {
        this.value = value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }
}
