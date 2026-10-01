package org.ngafid.www;

import org.ngafid.core.accounts.AccountException;

public class ErrorResponse {
    private String errorTitle;
    private String errorMessage;

    /**
     * Constructs an error response from an explicit title and message.
     *
     * @param errorTitle the short error title
     * @param errorMessage the detailed error message
     */
    public ErrorResponse(String errorTitle, String errorMessage) {
        this.errorTitle = errorTitle;
        this.errorMessage = errorMessage;
    }

    /**
     * Constructs an error response from an account exception, using its title and message.
     *
     * @param e the account exception to describe
     */
    public ErrorResponse(AccountException e) {
        this.errorTitle = e.getTitle();
        this.errorMessage = e.getMessage();
    }

    /**
     * Constructs an error response from a generic exception, using the exception's simple class name as the title
     * and its message as the detail.
     *
     * @param e the exception to describe
     */
    public ErrorResponse(Exception e) {
        errorTitle = e.getClass().getSimpleName();

        /*
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        /e.printStackTrace(pw);
        String sStackTrace = sw.toString(); // stack trace as a string

        errorMessage = e.getMessage() + "\n" + sStackTrace;
        */

        errorMessage = e.getMessage();
    }
}
