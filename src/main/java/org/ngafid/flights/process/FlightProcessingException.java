package org.ngafid.flights.process;

import java.sql.SQLException;
import java.util.List;
import java.util.Collections;

/**
 * An exception that contains all of the FATAL exceptions that occurred during flight processing.
 * This includes SQLException, FatalFlightFileException, IOException, and FlightAlreadyExistsException.
 *
 * If flight processing steps are done in parallel, multiple exceptions could be thrown,
 * which is where this class comes in: it will contain all of the exceptions that occurred.
 */
public class FlightProcessingException extends Exception {
    private static final long serialVersionUID = 1235003L;
    private static final String DEFAULT_MESSAGE = "(exception message was empty / null)";

    private List<Exception> exceptions;

    public FlightProcessingException(Exception e) {
        super(e);
        this.exceptions = List.of(e);
    }

    public FlightProcessingException(String message, Exception e) {
        super(message, e);
        this.exceptions = List.of(e);
    }

    public FlightProcessingException(List<Exception> exceptions) {
        super(exceptions.isEmpty() ? DEFAULT_MESSAGE : exceptions.get(0).getMessage());
        this.exceptions = Collections.unmodifiableList(exceptions);
    }

    @Override
    public String getMessage() {
        if (exceptions.size() == 1) {
            Exception e = exceptions.get(0);
            if (e instanceof SQLException) {
                return getSQLExceptionMessage((SQLException) e);
            } else {
                String message = e.getMessage();
                return message != null ? message : DEFAULT_MESSAGE;
            }
        }

        StringBuilder messageBuilder = new StringBuilder("Encountered the following ")
                .append(exceptions.size())
                .append(" errors when processing a flight:\n");

        for (Exception e : exceptions) {
            if (e instanceof SQLException) {
                messageBuilder.append(getSQLExceptionMessage((SQLException) e));
            } else {
                String eMessage = e.getMessage();
                messageBuilder.append(eMessage != null ? eMessage : DEFAULT_MESSAGE).append("\n\n");
            }
        }

        return messageBuilder.toString();
    }

    // Helper method to extract detailed information from SQLException
    private String getSQLExceptionMessage(SQLException sqlException) {
        StringBuilder sqlMessage = new StringBuilder("SQL Exception Details:\n");

        sqlMessage.append("Message: ").append(sqlException.getMessage()).append("\n");
        sqlMessage.append("SQLState: ").append(sqlException.getSQLState()).append("\n");
        sqlMessage.append("Error Code: ").append(sqlException.getErrorCode()).append("\n");
        SQLException nextException = sqlException.getNextException();
        while (nextException != null) {
            sqlMessage.append("\nNext SQLException in chain:\n")
                    .append("Message: ").append(nextException.getMessage()).append("\n")
                    .append("SQLState: ").append(nextException.getSQLState()).append("\n")
                    .append("Error Code: ").append(nextException.getErrorCode()).append("\n");

            nextException = nextException.getNextException();
        }

        return sqlMessage.toString();
    }
}
