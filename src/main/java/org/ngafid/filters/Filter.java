package org.ngafid.filters;

import java.util.ArrayList;
import java.util.TreeSet;
import java.util.logging.Logger;

import java.time.LocalDateTime;
import java.time.OffsetTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;


public class Filter {
    private static final Logger LOG = Logger.getLogger(Filter.class.getName());

    protected String type = null;
    protected String condition = null;

    protected String text = null;

    protected ArrayList<String> inputs = null;
    protected ArrayList<Filter> filters = null;

    /**
     * Creates a RULE filter with the given inputs
     *
     * @param inputs a list of inputs for the rule, e.g., "Pitch" "&gt;" "15.33"
     */
    public Filter(ArrayList<String> inputs) {
        this.type = "RULE";

        this.inputs = inputs;
    }

    /**
     * Creates a GROUP filter with a particular condition ("AND" or "OR")
     *
     * @param condition the condition for the filter group
     */
    public Filter(String condition) {
        this.type = "GROUP";
        this.condition = condition;

        filters = new ArrayList<Filter>();
    }

    /**
     * Helper function for get column names. Appends non-duplicate column names to the input parameter.
     *
     * @param columnNames holds all the column names found
     * 
     */
    public void getColumnNamesHelper(Filter filter, TreeSet<String> columnNames) {
        LOG.info("getting column filter for " + filter.type);

        if (filter.type.equals("RULE")) {
            LOG.info(filter.inputs.toString());
            columnNames.add(filter.inputs.get(0));

        } else if (filter.type.equals("GROUP")) {
            for (int i = 0; i < filter.filters.size(); i++) {
                getColumnNamesHelper(filter.filters.get(i), columnNames);
            }
        } else {
            LOG.severe("Attempted to convert a filter to a String with an unknown type: '" + type + "'");
            System.exit(1);
        }
    }

    /**
     * Gets the first input of each rule without duplicates, for use by EventDefinitions.
     *
     * @return a hash set of strings for each column name, without duplicates
     * 
     */
    public TreeSet<String> getColumnNames() {
        TreeSet<String> columnNames = new TreeSet<String>();
        getColumnNamesHelper(this, columnNames);
        return columnNames;
    }

    /**
     * Adds a filter to a GROUP filter
     *
     * @param condition the condition for the filter group
     */
    public void addFilter(Filter filter) {
        if (filters != null) {
            filters.add(filter);
        } else {
            LOG.severe("Attempted to add a filter " + filter + " to a non-group filter");
            System.exit(1);
        }
    }

    /**
     * Check to see of the passed string is &lt;=, &lt;, =, &gt; or &gt;=
     * to make sure the formulated mysql query wont be hijacked
     *
     * @param operator the operator string passed from the query filter
     *
     * @return the passed string if valid, null otherwise
     */
    public String checkOperator(String op) {
        if (op.equals(">=") || op.equals(">") || op.equals("=") || op.equals("<") || op.equals ("<=")) {
            return op;
        }
        return null;
    }

    /**
     * Check to see of the passed string is 'min', 'avg', or 'max'
     * to make sure the formulated mysql query wont be hijacked
     *
     * @param operator the operator string passed from the query filter
     *
     * @return the passed string if valid, null otherwise
     */
    public String checkSeriesOp(String op) {
        if (op.equals("min") || op.equals("avg") || op.equals("max")) {
            return op;
        }
        return null;
    }

    /**
     * Pads an hour, minute or second value with a leading 0 if it does
     * not have one so it works for a mysql query.
     *
     * @param value the hour, minute or second value
     *
     * @return the 0 padded time value string
     */
    public String timePad(String value) {
        if (value.length() == 0) return "00";
        else if (value.length() == 1) return "0" + value;
        else return value;
    }


    /**
     * Converts a datetime and long form time zone offset (from the filter select) to
     * a time in GMT.
     *
     * @param the date time in yyyy-MM-dd hh:mm:ss
     * @param the time zone offset, e.g. '(GMT-05:00) Eastern Time (US & Canada)'
     *
     * @return the date time in yyyy-MM-dd hh:mm:ss converted to GMT
     */
    public static String getOffsetDateTime(String datetime, String longOffset) {
        String offset = longOffset.substring(4,10);
        System.out.println("datetime is: " + datetime);
        System.out.println("time zone offset is: " + offset);
        OffsetDateTime odt = LocalDateTime.parse(datetime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).atOffset(ZoneOffset.of(offset));
        String gmtTime = odt.withOffsetSameInstant(ZoneOffset.of("+00:00")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.println("gmt date time is: " + gmtTime);

        return gmtTime;
    }

    /**
     * Converts a time and long form time zone offset (from the filter select) to
     * a time in GMT.
     *
     * @param the time in hh:mm:ss
     * @param the time zone offset, e.g. '(GMT-05:00) Eastern Time (US & Canada)'
     *
     * @return the date time in hh:mm:ss converted to GMT
     */
    public static String getOffsetTime(String time, String longOffset) {
        String offset = longOffset.substring(4,10);
        System.out.println("time is: " + time);
        System.out.println("time  zone offset is: " + offset);
        OffsetTime ot = OffsetTime.parse(time + offset);
        String gmtTime = ot.withOffsetSameInstant(ZoneOffset.of("+00:00")).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("gmt time is: " + gmtTime);

        return gmtTime;
    }


    /**
     * Recursively returns a mysql query represented by this rule, updates the parameters
     * argument with parameters that need to be filled in by this query.
     *
     * @param fleetId the id of the fleet for this query
     * @param parameters will be updated with what inputs this rule needs
     *
     * @return A string mysql query of this rule 
     */
    public String getRuleQuery(int fleetId, ArrayList<Object> parameters) {
        String eventName;
        String condition;

        int seperatorIndex;

        switch (inputs.get(0)) {
            case "Airframe":
                System.out.println(inputs);
                parameters.add(fleetId);
                parameters.add(inputs.get(2));
                if (inputs.get(1).equals("is")) {
                    return "flights.airframe_id = (SELECT id FROM airframes WHERE fleet_id = ? AND airframe = ?)";
                } else {
                    return "flights.airframe_id = (SELECT id FROM airframes WHERE fleet_id = ? AND airframe != ?)";
                }

            case "System ID":
                parameters.add(fleetId);
                parameters.add(inputs.get(2));
                if (inputs.get(1).equals("is")) {
                    return "flights.fleet_id = ? AND flights.system_id = ?";
                } else {
                    return "flights.fleet_id = ? AND flights.system_id != ?";
                }


            case "Tail Number":
                parameters.add(fleetId);
                parameters.add(inputs.get(2));
                if (inputs.get(1).equals("is")) {
                    return "flights.system_id in (SELECT system_id FROM tails WHERE fleet_id = ? AND tail = ?)";
                } else {
                    return "flights.system_id in (SELECT system_id FROM tails WHERE fleet_id = ? AND tail != ?)";
                }

            case "Duration":
                parameters.add(timePad(inputs.get(2)) + ":" + timePad(inputs.get(3)) + ":" + timePad(inputs.get(4)));
                return "TIMEDIFF(flights.end_time, flights.start_time) " + checkOperator(inputs.get(1)) + " ?";

            case "Start Date and Time":
                parameters.add(getOffsetDateTime(inputs.get(2), inputs.get(3)));
                return "flights.start_time " + checkOperator(inputs.get(1)) + " ?";

            case "End Date and Time":
                parameters.add(getOffsetDateTime(inputs.get(2), inputs.get(3)));
                return "flights.end_time " + checkOperator(inputs.get(1)) + " ?";

            case "Start Date":
                parameters.add(inputs.get(2));
                return "DATE(flights.start_time) " + checkOperator(inputs.get(1)) + " ?";

            case "End Date":
                parameters.add(inputs.get(2));
                return "DATE(flights.end_time) " + checkOperator(inputs.get(1)) + " ?";

            case "Start Time":

            case "End Time":
                parameters.add(getOffsetTime(inputs.get(2), inputs.get(3)));
                return "TIME(flights.start_time) " + checkOperator(inputs.get(1)) + " ?";

            case "Parameter":
                parameters.add(inputs.get(2));
                parameters.add(inputs.get(4));
                return "EXISTS (SELECT id FROM double_series WHERE flights.id = double_series.flight_id AND double_series.name_id = (SELECT id FROM double_series_names WHERE name = ?) AND double_series." + checkSeriesOp(inputs.get(1)) + " " + checkOperator(inputs.get(3)) + " ?)";

            case "Airport":
                String iataCode1 = inputs.get(1).substring(0, 3);
                parameters.add(iataCode1);

                if (inputs.get(2).equals("visited")) {
                    return "EXISTS (SELECT id FROM itinerary WHERE flights.id = itinerary.flight_id AND itinerary.airport = ?)";
                } else {
                    return "NOT EXISTS (SELECT id FROM itinerary WHERE flights.id = itinerary.flight_id AND itinerary.airport = ?)";
                }

            case "Runway":
                String iataRunway = inputs.get(1);
                String iataCode2 = iataRunway.substring(0, 3);
                String runway = iataRunway.substring(6, iataRunway.length());
                parameters.add(iataCode2);
                parameters.add(runway);

                if (inputs.get(2).equals("visited")) {
                    return "EXISTS (SELECT id FROM itinerary WHERE flights.id = itinerary.flight_id AND itinerary.airport = ? AND itinerary.runway = ?)";
                } else {
                    return "NOT EXISTS (SELECT id FROM itinerary WHERE flights.id = itinerary.flight_id AND itinerary.airport = ? AND itinerary.runway = ?)";
                }

            case "Event Count":
                eventName = inputs.get(1);
                condition = checkOperator(inputs.get(2));

                seperatorIndex = eventName.indexOf(" - ");
                if (seperatorIndex < 0) {
                    parameters.add(eventName);
                    parameters.add(inputs.get(3));

                    return "EXISTS (SELECT flight_id FROM flight_processed WHERE flights.id = flight_processed.flight_id AND flight_processed.event_definition_id = (SELECT id FROM event_definitions WHERE event_definitions.name = ? AND event_definitions.airframe_id = 0) AND flight_processed.count " + condition + " ?)";
                } else {
                    String airframeName = eventName.substring(seperatorIndex + 3);
                    eventName = eventName.substring(0, seperatorIndex);

                    parameters.add(eventName);
                    parameters.add(airframeName);
                    parameters.add(inputs.get(3));

                    return "EXISTS (SELECT flight_id FROM flight_processed WHERE flights.id = flight_processed.flight_id AND flight_processed.event_definition_id = (SELECT id FROM event_definitions WHERE event_definitions.name = ? AND event_definitions.airframe_id = (SELECT id FROM airframes WHERE airframe = ?)) AND flight_processed.count " + condition + " ?)";
                }

            case "Event Severity":
                eventName = inputs.get(1);
                condition = checkOperator(inputs.get(2));

                seperatorIndex = eventName.indexOf(" - ");
                if (seperatorIndex < 0) {
                    parameters.add(eventName);
                    parameters.add(inputs.get(3));

                    return "EXISTS (SELECT id FROM events WHERE flights.id = events.flight_id AND events.event_definition_id = (SELECT id FROM event_definitions WHERE event_definitions.name = ? AND event_definitions.airframe_id = 0) AND events.severity " + condition + " ?)";
                } else {
                    String airframeName = eventName.substring(seperatorIndex + 3);
                    eventName = eventName.substring(0, seperatorIndex);

                    parameters.add(eventName);
                    parameters.add(airframeName);
                    parameters.add(inputs.get(3));

                    return "EXISTS (SELECT id FROM events WHERE flights.id = events.flight_id AND events.event_definition_id = (SELECT id FROM event_definitions WHERE event_definitions.name = ? AND event_definitions.airframe_id = (SELECT id FROM airframes WHERE airframe = ?)) AND events.severity " + condition + " ?)";
                }

            case "Event Duration":
                eventName = inputs.get(1);
                condition = checkOperator(inputs.get(2));

                seperatorIndex = eventName.indexOf(" - ");
                if (seperatorIndex < 0) {
                    parameters.add(eventName);
                    parameters.add(inputs.get(3));

                    return "EXISTS (SELECT id FROM events WHERE flights.id = events.flight_id AND events.event_definition_id = (SELECT id FROM event_definitions WHERE event_definitions.name = ? AND event_definitions.airframe_id = 0) AND ((events.end_line - events.start_line) + 1) " + condition + " ?)";
                } else {
                    String airframeName = eventName.substring(seperatorIndex + 3);
                    eventName = eventName.substring(0, seperatorIndex);

                    parameters.add(eventName);
                    parameters.add(airframeName);
                    parameters.add(inputs.get(3));

                    return "EXISTS (SELECT id FROM events WHERE flights.id = events.flight_id AND events.event_definition_id = (SELECT id FROM event_definitions WHERE event_definitions.name = ? AND event_definitions.airframe_id = (SELECT id FROM airframes WHERE airframe = ?)) AND ((events.end_line - events.start_line) + 1) " + condition + " ?)";
                }

            case "Tag":
                parameters.add(fleetId);
                parameters.add(inputs.get(1)); //we can ignore index 0, it is for the UI only
				String det = inputs.get(2);
				if(det.substring(0,6).equals("Is Not")){
					return "NOT EXISTS (SELECT flight_id FROM flight_tag_map WHERE tag_id = (SELECT id FROM flight_tags WHERE fleet_id = ? AND name = ?) AND flight_id = flights.id)";
				}else{
					return "EXISTS (SELECT flight_id FROM flight_tag_map WHERE tag_id = (SELECT id FROM flight_tags WHERE fleet_id = ? AND name = ?) AND flight_id = flights.id)";
				}

            default:
                return "";
        }
    }

    /**
     * Recursively returns a mysql query represented by this filter, updates the parameters
     * argument with parameters that need to be filled in by this query.
     *
     * @param fleetId the id of the fleet for this query
     * @param parameters will be updated with what inputs this query needs
     *
     * @return A string mysql query of this filter
     */
    public String toQueryString(int fleetId, ArrayList<Object> parameters) {
        if (type.equals("RULE")) {
            return "(" + getRuleQuery(fleetId, parameters) + ")";

        } else if (type.equals("GROUP")) {
            String string = "";
            for (int i = 0; i < filters.size(); i++) {
                if (i > 0) string += " " + condition + " ";
                string += filters.get(i).toQueryString(fleetId, parameters);
            }

            return "(" + string + ")";

        } else {
            LOG.severe("Attempted to convert a filter to a String with an unknown type: '" + type + "'");
            System.exit(1);
        }
        return "";
    }


    /**
     * Recursively returns a human-readable representation of this filter (for display on webpages)
     *
     * @return A string of the human-readable version of this filter
     */
    public String toHumanReadable() {
        // Catch custom event definition JSON, which uses
        // text instead of type for its value
        if(text != null) {
            return text;
        }

        if (type.equals("RULE")) {
            String string = "";
            for (int i = 0; i < inputs.size(); i++) {
                if (i > 0) string += " ";
                string +=  inputs.get(i);
            }

            return string;

        } else if (type.equals("GROUP")) {
            String string = "";
            for (int i = 0; i < filters.size(); i++) {
                if (i > 0) string += " " + condition + " ";
                string += filters.get(i).toHumanReadable();
            }

            return "(" + string + ")";

        } else {
            LOG.severe("Attempted to convert a filter to a String with an unknown type: '" + type + "'");
            System.exit(1);
        }
        return "";
    }

    /**
     * Recursively returns a string representation of this filter and all it's children
     *
     * @return A string represntation of this filter
     */
    public String toString() {
        if (type.equals("RULE")) {
            String string = "";
            for (int i = 0; i < inputs.size(); i++) {
                if (i > 0) string += " ";
                string += "'" + inputs.get(i) + "'";
            }

            return "(" + string + ")";

        } else if (type.equals("GROUP")) {
            String string = "";
            for (int i = 0; i < filters.size(); i++) {
                if (i > 0) string += " " + condition + " ";
                string += filters.get(i).toString();
            }

            return "(" + string + ")";

        } else {
            LOG.severe("Attempted to convert a filter to a String with an unknown type: '" + type + "'");
            System.exit(1);
        }
        return "";
    }

    /**
     * Used for comparing two filters for equality
     */
    @Override
    public boolean equals(Object e){
        if(e instanceof Filter){
            Filter t = (Filter)e;
            return t.hashCode() == super.hashCode();
        }else{
            return false; 
        }
    }
}
