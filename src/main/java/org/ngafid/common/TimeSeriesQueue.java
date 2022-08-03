package org.ngafid.common;

import com.sun.jdi.Value;

import java.sql.Time;
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

        return new TimeSeriesNode<>(popped.getTime(), popped.getValue());
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
        return new TimeSeriesNode<>(front.getTime(), front.getValue());
    }

    /**
     * Getter for last node
     *
     * @return Last Element In Queue
     */
    public TimeSeriesNode<ValueType> getBack() {
        return new TimeSeriesNode<>(back.getTime(), back.getValue());
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

    /**
     * Testing
     * @param args
     */
    public static void main(String[] args) {
        TimeSeriesQueue<Integer> queue = new TimeSeriesQueue<>();

        queue.enqueue(0, 1);
        queue.enqueue(.5, 2);
        queue.enqueue(1.0, 3);
        queue.enqueue(1.5, 4);
        queue.enqueue(2.0, 5);
        queue.enqueue(2.5, 6);
        queue.enqueue(3.0, 7);
        queue.enqueue(3.5, 8);
        queue.enqueue(4.0, 9);
        queue.enqueue(4.5, 10);

        System.out.println("QUEUE TEST: " + queue);
        System.out.println("FRONT/BACK TEST: " );
        System.out.println("\tEXPECTED: (0.0, 1) ACTUAL:" + queue.getFront());
        System.out.println("\tEXPECTED: (3.0, 7) ACTUAL:" + queue.getBack());

        System.out.println("DEQUEUE TEST: ");
        System.out.println("\tEXPECTED: (0.0, 1) ACTUAL:" + queue.dequeue());
        System.out.println("\tEXPECTED: (0.5, 2) ACTUAL:" + queue.dequeue());
        System.out.println("\tEXPECTED: (1.0, 3) ACTUAL:" + queue.dequeue());
        System.out.println("\tEXPECTED: (1.5, 4) ACTUAL:" + queue.dequeue());
        System.out.println("\tEXPECTED: (2.0, 5) ACTUAL:" + queue.dequeue());

        queue.purge(1.0);
        System.out.println("PURGE TEST: ");
        System.out.println("EXPECTED: (3.5, 8) ACTUAL: " + queue);






    }
}