package org.ngafid.routes;

import org.ngafid.accounts.AccountException;

public class ErrorResponse {
    private String errorTitle;
    private String errorMessage;

    public ErrorResponse(String errorTitle, String errorMessage) {
        this.errorTitle = errorTitle;
        this.errorMessage = errorMessage;
    }

    public ErrorResponse(AccountException e) {
        this.errorTitle = e.getTitle();
        this.errorMessage = e.getMessage();
    }

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
