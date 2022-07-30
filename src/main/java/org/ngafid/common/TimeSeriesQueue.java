package org.ngafid.common;

public class TimeSeriesQueue<ValueType> {
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

    public ValueType dequeue() {
        ValueType popped = this.front.getValue();

        this.front = this.front.getNext();

        if (--size == 0) {
            this.front = null;
            this.back = null;
        }

        return popped;
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
