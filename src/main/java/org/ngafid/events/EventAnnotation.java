package org.ngafid.events;

import java.io.File;
import java.io.PrintWriter;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.ngafid.*;
import org.ngafid.accounts.User;
import org.ngafid.flights.Flight;
import org.ngafid.flights.GeneratedCSVWriter;

import static org.ngafid.flights.CalculationParameters.*;

/**
 * This class is used for creating event annotations in the NGAFID
 *
 * @author <a href=mailto:apl1341@cs.rit.edu>Aidan LaBella @ RIT CS</a>
 */
public class EventAnnotation extends Annotation {
    private int eventId, classId;
    private String className, notes;

    private static Connection connection = Database.getConnection();
    private static final Logger LOG = Logger.getLogger(EventAnnotation.class.getName());

    private static final String DEFAULT_COLUMNS = "user_id, fleet_id, timestamp, event_id, class_id, notes";
    private static final String CSV = "csv", TXT = "txt";

    public EventAnnotation(int eventId, String className, User user, LocalDateTime timestamp, String notes) throws SQLException {
        super(user, timestamp);

        this.eventId = eventId;
        this.className = className;
        this.classId = this.getClassId(className);
        this.notes = notes;
    }

    /**
     * Instantiates this object in the order:
     * user_id, fleet_id, timestamp, event_id, class_id
     *
     * @param resultSet is the ResultSet that is used to make this instance
     */
    public EventAnnotation(ResultSet resultSet) throws SQLException {
        super(User.get(connection, resultSet.getInt(1), resultSet.getInt(2)), resultSet.getTimestamp(3).toLocalDateTime());

        this.eventId = resultSet.getInt(4);
        this.classId = resultSet.getInt(5);
        this.notes = resultSet.getString(6);
    }

    /**
     * Checks this annotations primary key to determine if it is in the db.
     *
     * @return a boolean representing the state of this annotation in the db.
     */
    public boolean alreadyExists() throws SQLException {
        String queryString = "SELECT EXISTS (SELECT * FROM event_annotations WHERE fleet_id = ? AND user_id = ? AND event_id = ?)";

        PreparedStatement query = connection.prepareStatement(queryString);

        query.setInt(1, this.getFleetId());
        query.setInt(2, this.getUserId());
        query.setInt(3, this.getEventId());

        ResultSet resultSet = query.executeQuery();

        if (resultSet.next()) {
            return resultSet.getBoolean(1);
        }

        return false;
    }

    public int getEventId() {
        return this.eventId;
    }

    private int getClassId(String name) throws SQLException {
        String queryString = "SELECT id FROM loci_event_classes WHERE name = ? AND fleet_id = ?";

        PreparedStatement query = connection.prepareStatement(queryString);

        query.setString(1, name);
        query.setInt(2, this.getFleetId());

        ResultSet resultSet = query.executeQuery();

        if (resultSet.next()) {
            return resultSet.getInt(1);
        }

        // this shouldnt happen!
        return -1;
    }

    public boolean changeAnnotation(LocalDateTime timestamp) throws SQLException {
        String queryString = "UPDATE event_annotations SET class_id = ?, timestamp = ? WHERE user_id = ? AND fleet_id = ? AND event_id = ?";
        PreparedStatement query = connection.prepareStatement(queryString);

        query.setInt(1, this.classId);
        query.setTimestamp(2, Timestamp.valueOf(super.getTimestamp()));
        query.setInt(3, this.getUserId());
        query.setInt(4, this.getFleetId());
        query.setInt(5, this.getEventId());

        return query.executeUpdate() == 1;
    }

    public boolean updateDatabase() throws SQLException {
        String queryString = "INSERT INTO event_annotations(fleet_id, user_id, event_id, class_id, timestamp) VALUES (?,?,?,?,?)";
        PreparedStatement query = connection.prepareStatement(queryString);

        query.setInt(1, this.getFleetId());
        query.setInt(2, this.getUserId());
        query.setInt(3, this.getEventId());
        query.setInt(4, this.classId);
        query.setTimestamp(5, Timestamp.valueOf(super.getTimestamp()));

        return query.executeUpdate() == 1;
    }

    public boolean updateNotes(String notes) throws SQLException {
        String queryString = "UPDATE event_annotations SET notes = ? WHERE user_id = ? AND fleet_id = ? AND event_id = ?";
        PreparedStatement query = connection.prepareStatement(queryString);

        query.setString(1, notes);

        query.setInt(2, this.getUserId());
        query.setInt(3, this.getFleetId());
        query.setInt(4, this.getEventId());

        return query.executeUpdate() == 1;
    }

    public static List<EventAnnotation> getAllEventAnnotationsInSameGroup(User user) throws SQLException {
        return getAllEventAnnotationsByGroup(user.getGroup(connection));
    }

    public static List<EventAnnotation> getAllEventAnnotationsByGroup(int groupId) throws SQLException {
        String sql = "SELECT " + DEFAULT_COLUMNS + " FROM event_annotations WHERE user_id IN (SELECT user_id FROM user_groups WHERE group_id = ?)";
        PreparedStatement query = connection.prepareStatement(sql);

        query.setInt(1, groupId);
        ResultSet resultSet = query.executeQuery();

        List<EventAnnotation> annotations = new LinkedList<>();

        while (resultSet.next()) {
            annotations.add(new EventAnnotation(resultSet));
        }

        return annotations;
    }

    public static List<EventAnnotation> getDisplayedGroupAnnotations(User user) throws SQLException {
        String sql = "SELECT event_annotations.fleet_id, event_id, timestamp, notes, name FROM" +
                " event_annotations JOIN user_groups ug on event_annotations.user_id = ug.user_id JOIN" +
                " loci_event_classes lec on event_annotations.class_id = lec.id WHERE group_id = ?";
        int groupId = user.getGroup(connection);
        PreparedStatement query = connection.prepareStatement(sql);

        query.setInt(1, groupId);
        ResultSet resultSet = query.executeQuery();

        List<EventAnnotation> annotations = new LinkedList<>();
        while (resultSet.next()) {
            annotations.add(new EventAnnotation(resultSet.getInt(2), resultSet.getString(5), user, resultSet.getTimestamp(3).toLocalDateTime(), resultSet.getString(4)));
        }

        return annotations;
    }

    public static List<EventAnnotation> getAllEventAnnotations() throws SQLException {
        String sql = "SELECT " + DEFAULT_COLUMNS + " FROM event_annotations";
        PreparedStatement query = connection.prepareStatement(sql);

        ResultSet resultSet = query.executeQuery();

        List<EventAnnotation> annotations = new LinkedList<>();

        while (resultSet.next()) {
            annotations.add(new EventAnnotation(resultSet));
        }

        return annotations;
    }

    /**
     * Gets an EventAnnotation from its primary key
     */
    public static EventAnnotation getEventAnnotation(User user, int eventId) throws SQLException {
        String queryString = "SELECT " + DEFAULT_COLUMNS + " FROM event_annotations WHERE event_id = ? AND user_id = ? AND fleet_id = ?";

        PreparedStatement query = connection.prepareStatement(queryString);
        query.setInt(1, eventId);
        query.setInt(2, user.getId());
        query.setInt(3, user.getFleetId());

        ResultSet resultSet = query.executeQuery();

        EventAnnotation eventAnnotation = null;

        if (resultSet.next()) {
            eventAnnotation = new EventAnnotation(resultSet);
        }

        return eventAnnotation;
    }

    /**
     * Gets a list of the current event annotations associated with an event
     *
     * @param eventId the event id of the annotated event
     * @param currentUserId the id of the user retrieving information about the event
     *
     * @return a list of annotations
     */
    public static List<Annotation> getAnnotationsByEvent(int eventId, int currentUserId) throws SQLException {
        List<Annotation> annotations = new ArrayList<>();

        String queryString = "SELECT " + DEFAULT_COLUMNS + " FROM event_annotations WHERE event_id = ?";

        PreparedStatement query = connection.prepareStatement(queryString);
        query.setInt(1, eventId);

        ResultSet resultSet = query.executeQuery();

        while (resultSet.next()) {
            EventAnnotation ea = new EventAnnotation(resultSet);
            Annotation annotation = (Annotation) ea;

            //Mask the class the user chose if not the requesting user
            if (currentUserId != annotation.getUserId()) {
                ea.classId = -1;
            }

            annotations.add(annotation);
        }

        return annotations;
    }

    public static void displayUsage() {
        System.err.println("Usage: extract_loci_events [directory] [fleet_id] [user_id (of annotator)] [percent test events (0.d)]");
        System.err.println("Directory should have subdirectory with structure: <dir>/log");
    }

    public static int getEventLabelId(Connection connection, int eventId) throws SQLException {
        String sql = "SELECT lcs.id FROM event_annotations INNER JOIN loci_event_classes AS lcs ON class_id = lcs.id WHERE event_id = ?";
        PreparedStatement query = connection.prepareStatement(sql);

        query.setInt(1, eventId);

        ResultSet resultSet = query.executeQuery();

        int id = -1;

        if (resultSet.next()) {
            id = resultSet.getInt(1);
        }

        return id;
    }

    public static String getEventLabel(Connection connection, int eventId) throws SQLException {
        String sql = "SELECT lcs.name FROM event_annotations INNER JOIN loci_event_classes AS lcs ON class_id = lcs.id WHERE event_id = ?";
        PreparedStatement query = connection.prepareStatement(sql);

        query.setInt(1, eventId);

        ResultSet resultSet = query.executeQuery();

        String label = null;

        if (resultSet.next()) {
            label = resultSet.getString(1);
        }

        return label;
    }

    public static List<Event> getLabeledEvents(int fleetId, int userId) throws SQLException {
        String userClause = "";

        if (userId != -1) {
            User user = User.get(connection, userId, fleetId);
            LOG.info("Selecting events annotated by " + user.getFullName());

            userClause = "user_id = " + userId + " AND";
        } else {
            LOG.info("Selecting distinct events from all users...");
        }

        String sql = "SELECT DISTINCT event_id FROM event_annotations WHERE " + userClause + " fleet_id = ?";
        PreparedStatement query = connection.prepareStatement(sql);

        query.setInt(1, fleetId);

        List<Event> events = new LinkedList<>();

        ResultSet resultSet = query.executeQuery();

        while (resultSet.next()) {
            int eventId = resultSet.getInt(1);
            events.add(Event.getEvent(connection, eventId));
        }

        return events;
    }

    public static boolean isTestFile(double percent) {
        return Math.random() <= percent;
    }

    public static void main(String [] args) {
        Options options = new Options();

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        Option fleetIdArg = new Option("f", "fleet_id", true, "the fleet id to extract for");
        fleetIdArg.setRequired(true);
        fleetIdArg.setArgs(1);
        options.addOption(fleetIdArg);

        Option outDirArg = new Option("d", "output_directory", true, "the root output directory to write the csv and log files to");
        outDirArg.setRequired(true);
        outDirArg.setArgs(1);
        options.addOption(outDirArg);

        Option userIdArg = new Option("u", "user_id", true, "the user id for the annotator whose events should be pulled");
        userIdArg.setRequired(true);
        userIdArg.setArgs(1);
        options.addOption(userIdArg);

        Option tsArg = new Option("t", "num_timesteps", true, "the number of timesteps that the events should have");
        tsArg.setRequired(true);
        tsArg.setArgs(1);
        options.addOption(tsArg);

        Option logArg = new Option("l", "log_file_type", true, "how the exports should be logged; \"csv\" for a csv with each event csv file mapped to its class, \"txt\" for a human-readable logfile");
        logArg.setRequired(true);
        logArg.setArgs(1);
        options.addOption(logArg);

        Option aggArg = new Option("n", "numeric_classes", true, "use numeric classes");
        aggArg.setRequired(false);
        aggArg.setArgs(1);
        options.addOption(aggArg);


        try {
            cmd = parser.parse(options, args);

            String directoryRoot = cmd.getOptionValue("d");
            int fleetId = Integer.parseInt(cmd.getOptionValue("f"));
            int userId = Integer.parseInt(cmd.getOptionValue("u"));
            int nTimeSteps = Integer.parseInt(cmd.getOptionValue("t"));
            String logFileType = cmd.getOptionValue("l");
            boolean isAggregate = Boolean.parseBoolean(cmd.getOptionValue("a"));
            System.out.println(logFileType);

            boolean logToCSV = false;
            if (!(logToCSV = logFileType.equalsIgnoreCase(CSV)) && !logFileType.equalsIgnoreCase(TXT)) {
                System.err.println("The argument for log_file_type should be \"csv\" or \"txt\"");
                System.exit(1);
            } 
            
            LocalDateTime now = LocalDateTime.now();

            if (!new File(directoryRoot).exists()) {
                System.err.println("ERROR: directory does not exist!");
                System.exit(1);
            }
                
            File logDirectory = new File(directoryRoot + "/log/"); 

            if (!logDirectory.exists()) {
                logDirectory.mkdirs();
            }

            LOG.info("Arguments OK. Now extracting events...");

            File logFile = new File(logDirectory.getPath() + "/log_" + now.getYear() + now.getMonthValue() + now.getDayOfMonth() + now.getHour() + now.getMinute() + now.getSecond() + (logToCSV ? ".csv" : ".log"));

            if (!logFile.exists()) {
                logFile.createNewFile();
            }

            PrintWriter pw = new PrintWriter(logFile);

            if (logToCSV) {
                pw.println("# CSV Log file for generated events at time: " + now.toString());
                pw.println("# EventLabel,EventCSVFile");
                pw.println("EventLabel,EventCSVFile");
            } else {
                pw.println("Log file for generated events at time: " + now.toString());
            }

            List<Event> events = getLabeledEvents(fleetId, userId);

            for (Event event : events) {
                Flight flight = Flight.getFlight(connection, event.getFlightId());
                String label = getEventLabel(connection, event.getId());

                label = label.replaceAll("\\s", "_");

                String localCSVFilename = "event_" + event.getId() + "." + label + ".csv";
                String outputCSVFileName = directoryRoot + "/" + localCSVFilename;
                File outputCSVFile = new File(outputCSVFileName);

                GeneratedCSVWriter csvWriter = new GeneratedCSVWriter(flight, EVENT_RECOGNITION_COLUMNS, Optional.of(outputCSVFile));

                List<String> missingColumns = flight.checkCalculationParameters(EVENT_RECOGNITION_COLUMNS);

                if (!missingColumns.isEmpty() && !logToCSV) {
                    pw.println("NOT GENERATED: Event id#" + event.getId() + ": " + label + " missing columns: " + missingColumns.toString());
                } else {
                    csvWriter.writeToFile(event, nTimeSteps);
                    if (logToCSV) {
                        pw.println(label + "," + localCSVFilename);
                    } else {
                        pw.println(outputCSVFileName + ": Event id#" + event.getId() + ": " + label);
                    }
                }
            }
            
            pw.close();

            LOG.info("Done!");
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }
}
