package org.ngafid.uploads;

import java.util.ConcurrentModificationException;

public class UploadAlreadyLockedException extends ConcurrentModificationException {
    public UploadAlreadyLockedException(String message) {
        super(message);
    }
}
