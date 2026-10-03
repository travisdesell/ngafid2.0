package org.ngafid.core.accounts;

/**
 * Checked exception for account- and fleet-related failures, carrying a short display title alongside the message.
 *
 * <p>The title provides a user-facing category (e.g. for rendering in the web UI) distinct from the detail message.
 */
public class AccountException extends Exception {
    private String title;

    public String getTitle() {
        return title;
    }

    /**
     * Constructs an account exception with a short title (for display) and a detail message.
     *
     * @param title a short title categorizing the error
     * @param message the detail message
     */
    public AccountException(String title, String message) {
        super(message);
        this.title = title;
    }

    /**
     * Constructs an account exception with a short title, a detail message, and an underlying cause.
     *
     * @param title a short title categorizing the error
     * @param message the detail message
     * @param cause the underlying cause
     */
    public AccountException(String title, String message, Throwable cause) {
        super(message, cause);
        this.title = title;
    }
}
