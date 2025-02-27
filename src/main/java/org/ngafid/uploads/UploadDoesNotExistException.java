package org.ngafid.uploads;

/**
 * Thrown when an upload that should be there isn't -- may happen when the kafka queue runs behind and an upload being
 * starts processing after it was deleted.
 */
public class UploadDoesNotExistException extends Exception {
    final int id;

    public UploadDoesNotExistException(int id) {
        this.id = id;
    }

    @Override
    public String getMessage() {
        return "Upload doesn't exist: " + id;
    }
}
