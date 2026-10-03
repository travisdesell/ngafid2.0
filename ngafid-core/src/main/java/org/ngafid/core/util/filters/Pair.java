package org.ngafid.core.util.filters;

/**
 * An immutable two-element tuple holding a pair of (possibly differently typed) values.
 *
 * <p>Used by the filter machinery to carry associated values such as min/max bounds together.
 *
 * @param <K> the type of the first element
 * @param <V> the type of the second element
 */
public class Pair<K, V> {

    private final K first;
    private final V second;

    /**
     * Factory method that constructs a pair from two values, inferring the type parameters from the arguments.
     *
     * @param first the first element
     * @param second the second element
     * @param <K> the type of the first element
     * @param <V> the type of the second element
     * @return a new pair holding the two values
     */
    public static <K, V> Pair<K, V> createPair(K first, V second) {
        return new Pair<K, V>(first, second);
    }

    /**
     * Constructs an immutable pair of two values.
     *
     * @param first the first element
     * @param second the second element
     */
    public Pair(K first, V second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Returns the first element of the pair.
     *
     * @return the first element
     */
    public K first() {
        return first;
    }

    /**
     * Returns the second element of the pair.
     *
     * @return the second element
     */
    public V second() {
        return second;
    }
}
