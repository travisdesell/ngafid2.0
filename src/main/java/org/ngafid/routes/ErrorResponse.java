package org.ngafid.routes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ngafid.accounts.AccountException;

public class ErrorResponse {
    @JsonProperty
    private String errorTitle;
    @JsonProperty
    private String errorMessage;

    @JsonCreator
    public ErrorResponse(@JsonProperty String errorTitle, @JsonProperty String errorMessage) {
        this.errorTitle = errorTitle;
        this.errorMessage = errorMessage;
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
