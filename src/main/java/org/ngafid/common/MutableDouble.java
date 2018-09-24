package org.ngafid.common;


public class MutableDouble {
    double value;

    public MutableDouble() {
        value = Double.NaN;
    }

    public MutableDouble(double value) {
        this.value = value;
    }


    public void set(double value) {
        this.value = value;
    }

    public double get() {
        return value;
    }
}
