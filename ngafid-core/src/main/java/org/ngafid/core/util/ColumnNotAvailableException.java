package org.ngafid.core.util;

/**
 * Checked exception thrown when a requested flight data column is not present in the data being processed.
 */
public class ColumnNotAvailableException extends Exception {
    /**
     * Constructs the exception with a message naming the column that was not available.
     *
     * @param s the detail message (typically the missing column name)
     */
    public ColumnNotAvailableException(String s) {
        super(s);
    }
}
