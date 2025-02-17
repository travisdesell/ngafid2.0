package org.ngafid.bin;

import org.apache.commons.cli.*;
import org.ngafid.common.Database;
import org.ngafid.uploads.UploadDoesNotExistException;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Things this program should allow you to do:
 * - Delete computed events, potentially restricting this to a particular flight, upload, or fleet.
 * A flag shall be made available to potentially mark the events as already having been computed therefore preventing the
 * EventObserver from queueing up their re-computation.
 * - Delete an event definition and all related entries.
 * -
 */
public class EventHelper {

    private static Options getOptions() {
        Options options = new Options();

        Option fleet = new Option("f", "fleet", true, "Restrict event modification to this fleet ID");
        fleet.setRequired(false);
        options.addOption(fleet);

        Option upload = new Option("u", "upload", true, "Restrict event modification to this upload ID");
        upload.setRequired(false);
        options.addOption(upload);

        Option flight = new Option("l", "flight", true, "Restrict event modification to this flight ID");
        flight.setRequired(false);
        options.addOption(flight);

        Option noRecompute = new Option("nr", "no-recompute", false, "Prevent deleted events from being recomputed by marking them as completed in the database.");
        noRecompute.setRequired(false);
        options.addOption(noRecompute);

        Option definition = new Option("d", "definition", true, "Event definition ID to delete all associated data for.");
        definition.setRequired(false);
        options.addOption(definition);

        Option deleteDef = new Option("x", "delete-definition", false, "Remove the event definition after deleting all associated data.");
        deleteDef.setRequired(false);
        options.addOption(deleteDef);

        return options;
    }

    public static void main(String[] arguments) throws SQLException, UploadDoesNotExistException, IOException {
        Options options = getOptions();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, arguments);
        } catch (org.apache.commons.cli.ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("ngafid-event-utility", options);
            return;
        }

        if (cmd.hasOption("definition")) {
            int defId = Integer.parseInt(cmd.getOptionValue("definition"));
            System.out.println("Read event definition ID " + defId + " -- clearing relevant data from the database (subject to constrains).");
            clearDefinition(defId, cmd);
        } else {
            System.out.println("Clearing events subject to constrains.");
            clearEvents(cmd);
        }
    }

    private static String getConstraints(CommandLine cmd, boolean eventsTable) {
        List<String> constraints = new ArrayList<>();
        if (cmd.hasOption("fleet")) {
            constraints.add("fleet_id = " + cmd.getOptionValue("fleet"));
        }

        if (cmd.hasOption("flight")) {
            if (eventsTable) {
                constraints.add("(flight_id = " + cmd.getOptionValue("flight") + " OR other_flight_id = " + cmd.getOptionValue("flight") + ")");
            } else {
                constraints.add("flight_id = " + cmd.getOptionValue("flight"));
            }
        }

        String c = constraints.stream().reduce((s1, s2) -> s1 + " AND " + s2).orElse("");

        if (cmd.hasOption("upload")) {
            c += " INNER JOIN flights ON flights.id = events.flight_id";
        }

        return c;
    }

    private static void clearDefinition(int defId, CommandLine cmd) throws SQLException {
        System.out.println("WARNING: Clearing a definition from the database while upload processing is running may lead to isolated bugs.");
        try (Connection connection = Database.getConnection()) {
            String constraints = getConstraints(cmd, false).trim();
            String eventconstraints = getConstraints(cmd, true).trim();
            try {
                connection.setAutoCommit(false);

                // Delete all events with event definition ID
                String eventQuery = "DELETE FROM events WHERE event_definition_id = " + defId;
                if (!eventconstraints.isEmpty())
                    eventQuery += " AND " + eventconstraints;

                try (PreparedStatement statement = connection.prepareStatement(eventQuery)) {
                    System.out.println("Executing query: \n" + statement.toString());
                    statement.executeUpdate();
                }

                // Delete event definition
                if (cmd.hasOption("delete-definition")) {
                    try (PreparedStatement statement = connection.prepareStatement("DELETE FROM event_definitions WHERE id = " + defId)) {
                        System.out.println("Executing query: \n" + statement.toString());
                        statement.executeUpdate();
                    }
                } else if (cmd.hasOption("nr")) {
                    String query = """
                            UPDATE flight_processed SET
                                count = 0,
                                sum_duration = DEFAULT,
                                sum_duration = DEFAULT,
                                min_duration = DEFAULT,
                                max_duration = DEFAULT,
                                sum_severity = DEFAULT,
                                min_severity = DEFAULT,
                                max_severity = DEFAULT,
                                had_error = DEFAULT
                            WHERE event_definition_id =\s""" + defId;
                    if (!constraints.isEmpty())
                        query += " AND " + constraints;
                    try (PreparedStatement statement = connection.prepareStatement(query)) {
                        System.out.println("Executing query: \n" + statement.toString());
                        statement.executeUpdate();
                    }
                } else {
                    // Delete all entries in flight_processed table for that event
                    String flightProcessedTableQuery = "DELETE FROM flight_processed WHERE event_definition_id = " + defId;
                    if (!constraints.isEmpty())
                        flightProcessedTableQuery += " AND " + constraints;

                    try (PreparedStatement statement = connection.prepareStatement(flightProcessedTableQuery)) {
                        System.out.println("Executing query: \n" + statement.toString());
                        statement.executeUpdate();
                    }
                }

                // TODO: In the future, we will have to delete event statistics stuff as well.

                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private static void clearEvents(CommandLine cmd) throws SQLException {

    }
}
