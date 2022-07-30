package org.ngafid.common;

public class TimeSeriesNode<ValueType> {
    private final double time;
    private final ValueType value;
    private TimeSeriesNode<ValueType> next;

    public TimeSeriesNode(double time, ValueType value) {
        this.time = time;
        this.value = value;
    }

    public double getTime() {
        return time;
    }

    public ValueType getValue() {
        return value;
    }

    public TimeSeriesNode<ValueType> getNext() {
        return next;
    }

    public void setNext(TimeSeriesNode<ValueType> next) {
        this.next = next;
    }
}