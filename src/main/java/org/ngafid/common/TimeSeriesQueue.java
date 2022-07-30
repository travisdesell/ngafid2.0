package org.ngafid.common;

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

public class TimeSeriesQueue<ValueType> {
    private TimeSeriesNode<ValueType> front;
    private TimeSeriesNode<ValueType> back;
    private int size;


    public TimeSeriesQueue() {
        this.front = null;
        this.back = null;
        this.size = 0;
    }

    public TimeSeriesNode<ValueType> getFront() {
        return front;
    }

    public TimeSeriesNode<ValueType> getBack() {
        return back;
    }

    public int getSize() {
        return size;
    }
}
