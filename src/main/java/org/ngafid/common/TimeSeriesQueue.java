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

    /**
     * Adds a new node to the queue
     *
     * @param time
     * @param value
     */
    public void enqueue(double time, ValueType value) {
        this.enqueue(new TimeSeriesNode<>(time, value));
    }

    /**
     * Adds a new node to the queue
     *
     * @param node
     */
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

    /**
     * Removes the first element in the queue
     *
     * @return The first node in the queue
     */
    public TimeSeriesNode<ValueType> dequeue() {
        TimeSeriesNode<ValueType> popped = this.front;

        this.front = this.front.getNext();

        if (--size == 0) {
            this.front = null;
            this.back = null;
        }

        return popped;
    }

    /**
     * Removes elements in the queue based on time difference of the latest element
     * @param timeDiff
     */
    public void purge(double timeDiff) {
        double diff = this.back.getTime() - this.front.getTime();;

        while (!isEmpty() && diff > timeDiff) {
            this.dequeue();

            diff = this.back.getTime() - this.front.getTime();
        }
    }

    /**
     * Wipes the entire queue
     */
    public void clear() {
        this.front = null;
        this.back = null;
        this.size = 0;
    }

    /**
     * Check if the queue is empty
     *
     * @return Queue is empty
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Getter for front node
     *
     * @return First Element in Queue
     */
    public TimeSeriesNode<ValueType> getFront() {
        return front;
    }

    /**
     * Getter for last node
     *
     * @return Last Element In Queue
     */
    public TimeSeriesNode<ValueType> getBack() {
        return back;
    }

    /**
     * Getter for the size of the queue
     *
     * @return Size of the Queue
     */
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