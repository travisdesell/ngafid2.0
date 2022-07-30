package org.ngafid.common;

public class TimeSeriesNode<valueType> {
    private final double time;
    private final valueType value;
    private TimeSeriesNode<valueType> next;

    public TimeSeriesNode(double time, valueType value) {
        this.time = time;
        this.value = value;
    }

    public double getTime() {
        return time;
    }

    public valueType getValue() {
        return value;
    }

    public TimeSeriesNode<valueType> getNext() {
        return next;
    }

    public void setNext(TimeSeriesNode<valueType> next) {
        this.next = next;
    }
}
