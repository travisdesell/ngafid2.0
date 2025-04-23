package org.ngafid.core.uploads;

import java.util.ConcurrentModificationException;

/**
 * Thrown when one attempts to acquire a lock on an upload but that lock is already in use.
 */
public class UploadAlreadyLockedException extends ConcurrentModificationException {
    public UploadAlreadyLockedException(String message) {
        super(message);
    }
}
