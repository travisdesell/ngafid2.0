package org.ngafid.uploads.process;

import java.util.Collections;
import java.util.List;

/**
 * An aggregate exception that contains all of the FATAL exceptions that occurred during flight processing.
 * Namely SQLException, FatalFlightFileException, IOException, and FlightAlreadyExistsException.
 * <p>
 * If flight processing steps are done in parallel multiple exceptions could be thrown, which is
 * where this class comes in: it will contain all of the exceptions that occurred.
 **/
public class FlightProcessingException extends Exception {
    private static final long serialVersionUID = 1235003;
    private static final String DEFAULT_MESSAGE = "(exception message was empty / null)";

    private List<Exception> exceptions;

    public FlightProcessingException(Exception e) {
        exceptions = List.<Exception>of(e);
    }

    public FlightProcessingException(List<Exception> exceptions) {
        this.exceptions = Collections.<Exception>unmodifiableList(exceptions);
    }

    public String getMessage() {
        String message;

        if (exceptions.size() == 1) {

            message = exceptions.get(0).getMessage();
            if (message == null)
                return DEFAULT_MESSAGE;

        } else {
            message = "Encountered the following " + exceptions.size() + " errors when processing a flight:\n";
            for (var e : exceptions) {
                String eMessage = e.getMessage();
                if (eMessage == null)
                    eMessage = DEFAULT_MESSAGE;
                message += eMessage + "\n\n";
            }
        }

        return message;
    }
}
