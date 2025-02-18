package org.ngafid.uploads;

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
