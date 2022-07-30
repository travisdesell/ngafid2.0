package org.ngafid.common;

import com.sun.jdi.Value;

import java.util.Iterator;

/**
 * Queue designed for Time Series Data
 *
 * @author Aaron Chan
 */

public class TimeSeriesQueue<ValueType> implements Iterable<TimeSeriesNode<ValueType>> {
    private TimeSeriesNode<ValueType> front;
    private TimeSeriesNode<ValueType> back;
    private int size;


    public TimeSeriesQueue() {
        this.front = null;
        this.back = null;
        this.size = 0;
    }

    public void enqueue(double time, ValueType value) {
        this.enqueue(new TimeSeriesNode<>(time, value));
    }

    public void enqueue(TimeSeriesNode<ValueType> node) {
        if (this.front == null) {
            this.front = node;
            this.back = node;
        } else {
            this.back.setNext(node);
            this.back = back.getNext();
        }

        size++;
    }

    public TimeSeriesNode<ValueType> dequeue() {
        TimeSeriesNode<ValueType> popped = this.front;

        this.front = this.front.getNext();

        if (--size == 0) {
            this.front = null;
            this.back = null;
        }

        return popped;
    }

    public void purge(double timeDiff) {
        double diff = this.back.getTime() - this.front.getTime();;

        while (!isEmpty() && diff > timeDiff) {
            this.dequeue();

            diff = this.back.getTime() - this.front.getTime();
        }
    }

    public void clear() {
        this.front = null;
        this.back = null;
        this.size = 0;
    }

    public boolean isEmpty() {
        return size == 0;
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

    @Override
    public String toString() {
        return "TimeSeriesQueue{" + this.front + "}";
    }

    @Override
    public Iterator<TimeSeriesNode<ValueType>> iterator() {
        return new TSNIterator<>(this.front);
    }
}