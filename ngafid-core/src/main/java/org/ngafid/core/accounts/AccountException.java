package org.ngafid.core.accounts;


public class AccountException extends Exception {
    private String title;

    public String getTitle() {
        return title;
    }

    public AccountException(String title, String message) {
        super(message);
        this.title = title;
    }

    public AccountException(String title, String message, Throwable cause) {
        super(message, cause);
        this.title = title;
    }
}
