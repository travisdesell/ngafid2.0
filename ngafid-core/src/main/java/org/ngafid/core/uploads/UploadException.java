package org.ngafid.core.uploads;

public class UploadException extends Exception {
    private String filename;

    public UploadException(String message, String filename) {
        super(message);
        this.filename = filename;
    }

    public UploadException(String message, Throwable cause, String filename) {
        super(message, cause);
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }
}
