package org.ngafid.uploads;

import java.util.ConcurrentModificationException;

public class UploadAlreadyProcessingException extends ConcurrentModificationException {
    public UploadAlreadyProcessingException(String message) {
        super(message);
    }
}
