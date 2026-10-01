package org.ngafid.core.uploads;

public class UploadException extends Exception {
    private String filename;

    /**
     * Constructs an upload exception with a message and the name of the file being uploaded when it occurred.
     *
     * @param message the detail message
     * @param filename the name of the file whose upload failed
     */
    public UploadException(String message, String filename) {
        super(message);
        this.filename = filename;
    }

    /**
     * Constructs an upload exception with a message, an underlying cause, and the name of the file being uploaded.
     *
     * @param message the detail message
     * @param cause the underlying cause
     * @param filename the name of the file whose upload failed
     */
    public UploadException(String message, Throwable cause, String filename) {
        super(message, cause);
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }
}
