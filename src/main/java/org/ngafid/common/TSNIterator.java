package org.ngafid.common;

import java.util.Iterator;

public class TSNIterator<ValueType> implements Iterator<TimeSeriesNode<ValueType>> {
    TimeSeriesNode<ValueType> node;

    public TSNIterator(TimeSeriesNode<ValueType> node) {
        this.node = node;
    }

    @Override
    public boolean hasNext() {
        return node != null;
    }

    @Override
    public TimeSeriesNode<ValueType> next() {
        TimeSeriesNode<ValueType> nextNode = node;
        node = node.getNext();

        return nextNode;
    }
}
