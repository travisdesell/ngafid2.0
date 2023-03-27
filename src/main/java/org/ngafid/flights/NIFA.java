package org.ngafid.flights;

import org.ngafid.airports.Airport;
import org.ngafid.airports.Airports;
import org.ngafid.airports.Runway;
import org.ngafid.events.EventDefinition;
import org.ngafid.events.Event;
import org.ngafid.Database;

import java.util.Set;
import java.util.stream.*;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.ngafid.flights.calculations.Parameters.*;

public class NIFA implements Serializable {

    // This utility class doesn't seem strictly necessary,
    // but may come in handy in the future.
    static class NIFAEventDefinition {
        public final int id;
        public final String name;
        public final String columnNames = "";
        public final int startBuffer = -1;
        public final int stopBuffer = -1;
        public final String conditionJson = "";
        public final String severityColumnNames = "";
        public final String severityType = "max";
        public final String color = null;

        public NIFAEventDefinition(int id, String name) {
            this.id = id;
            this.name = name;
        }

    }

    private static ArrayList<NIFAEventDefinition> nifaEventDefinitions = new ArrayList<>();
    private static HashMap<Integer, NIFAEventDefinition> nifaEventDefinitionMap = new HashMap<>();

    private static int _BASE = -100;
    private static final int DEBUG_ENTER_CROSSWIND = _BASE--;
    private static final int DEBUG_ENTER_FIRST_TURN = _BASE--;
    private static final int DEBUG_ENTER_SECOND_TURN = _BASE--;
    private static final int DEBUG_ENTER_THIRD_TURN = _BASE--;
    private static final int DEBUG_ENTER_FINAL_TURN = _BASE--;
    private static final int DEBUG_EXIT_CROSSWIND = _BASE--;
    private static final int DEBUG_EXIT_FIRST_TURN = _BASE--;
    private static final int DEBUG_EXIT_SECOND_TURN = _BASE--;
    private static final int DEBUG_EXIT_THIRD_TURN = _BASE--;
    private static final int DEBUG_EXIT_FINAL_TURN = _BASE--;
    private static final int DEBUG_NIFA_TRACKING_ERROR = _BASE--;

    static {
        nifaEventDefinitions.add(new NIFAEventDefinition(DEBUG_ENTER_CROSSWIND, "DEBUG_ENTER_CROSSWIND"));
        nifaEventDefinitions.add(new NIFAEventDefinition(DEBUG_ENTER_FIRST_TURN, "DEBUG_ENTER_FIRST_TURN"));
        nifaEventDefinitions.add(new NIFAEventDefinition(DEBUG_ENTER_SECOND_TURN, "DEBUG_ENTER_SECOND_TURN"));
        nifaEventDefinitions.add(new NIFAEventDefinition(DEBUG_ENTER_THIRD_TURN, "DEBUG_ENTER_THIRD_TURN"));
        nifaEventDefinitions.add(new NIFAEventDefinition(DEBUG_ENTER_FINAL_TURN, "DEBUG_ENTER_FINAL_TURN"));
        nifaEventDefinitions.add(new NIFAEventDefinition(DEBUG_EXIT_CROSSWIND, "DEBUG_EXIT_CROSSWIND"));
        nifaEventDefinitions.add(new NIFAEventDefinition(DEBUG_EXIT_FIRST_TURN, "DEBUG_EXIT_FIRST_TURN"));
        nifaEventDefinitions.add(new NIFAEventDefinition(DEBUG_EXIT_SECOND_TURN, "DEBUG_EXIT_SECOND_TURN"));
        nifaEventDefinitions.add(new NIFAEventDefinition(DEBUG_EXIT_THIRD_TURN, "DEBUG_EXIT_THIRD_TURN"));
        nifaEventDefinitions.add(new NIFAEventDefinition(DEBUG_EXIT_FINAL_TURN, "DEBUG_EXIT_FINAL_TURN"));
        nifaEventDefinitions.add(new NIFAEventDefinition(DEBUG_NIFA_TRACKING_ERROR, "DEBUG_NIFA_TRACKING_ERROR"));
   
        // ... etc
        // Create map
        for (NIFAEventDefinition def : nifaEventDefinitions)
            nifaEventDefinitionMap.put(def.id, def);
        
        try {
            init(Database.getConnection());
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Failed to initialized NIFA event definitions. This wont be a problem unless you plan on using thme");
        }
    }

    static boolean initialized = false;
    // Insert NIFA events
    static void init(Connection connection) throws SQLException {
        if (initialized)
            return;

        initialized = true;
        Set<Integer> eventIds = EventDefinition.getAll(connection)
            .stream()
            .map(e -> e.getId())
            .collect(Collectors.toSet());
        for (NIFAEventDefinition e : nifaEventDefinitions) {
            if (!eventIds.contains(e.id))
                EventDefinition.insert(connection, 
                    e.id, e.name, e.startBuffer, e.stopBuffer, 0);
        }
    }

    private static final Logger LOG = Logger.getLogger(NIFA.class.getName());

    private static final double FEET_PER_MILE = 5280;

    private StringTimeSeries dates, times;

    private double[] lat, lon, altitude, altMSL, velocity;

    private double runwayAltitude;

    public String airportIataCode;
    public String flightStartDate;

    private int nTimesteps, fleetId, flightId;
    private final Flight flight;

    public NIFA(Connection connection, Flight flight) throws SQLException {
        this.flight = flight;
        DoubleTimeSeries latTimeSeries = flight.getDoubleTimeSeries(LATITUDE);
        DoubleTimeSeries lonTimeSeries = flight.getDoubleTimeSeries(LONGITUDE);
        DoubleTimeSeries altTimeSeries = flight.getDoubleTimeSeries(ALT_AGL);
        DoubleTimeSeries altMSLTimeSeries = flight.getDoubleTimeSeries(ALT_MSL);
        DoubleTimeSeries velocityTimeSeries = flight.getDoubleTimeSeries(GND_SPD);
        StringTimeSeries timeSeries = flight.getStringTimeSeries(connection, "Lcl Time");
        StringTimeSeries dateSeries = flight.getStringTimeSeries(connection, "Lcl Date");

        LOG.info("TIME SERIES 0 = " + timeSeries.get(0));
        LOG.info("DATE SERIES 0 = " + dateSeries.get(0));

        int flightId = flight.getId();
        Object[] a =  {latTimeSeries, lonTimeSeries, altTimeSeries, altMSLTimeSeries, timeSeries, dateSeries, velocityTimeSeries};
        for (int i = 0; i < a.length; i++) {
            if (a[i] == null) {
                System.out.println("null at " + i);
            }
        }

        if (!Stream.of(latTimeSeries, lonTimeSeries, altTimeSeries, altMSLTimeSeries, timeSeries, dateSeries, velocityTimeSeries).anyMatch(Objects::isNull)) {

            lat = latTimeSeries.innerArray();
            lon = lonTimeSeries.innerArray();
            altitude = altTimeSeries.innerArray();
            velocity = velocityTimeSeries.innerArray();
            assert lat.length == lon.length;

            this.dates = dateSeries;
            this.times = timeSeries;

            this.fleetId = flight.getFleetId();
            this.flightId = flight.getId();

            this.nTimesteps = this.altitude.length;

            process(connection);
        } else {
            LOG.info("Could not process flight " + flight.getId());
        }
    }
   

    // Creates event of a given type between the given index range and inserts it into the database.
    void createEvent(Connection connection, int eventDefinitionId, int startIndex, int endIndex) {
        String startTimestamp = dates.get(startIndex)+ " " + times.get(startIndex);
        String endTimestamp = dates.get(endIndex) + " " + times.get(endIndex);
        
        Event e = new Event(0, fleetId, flightId, eventDefinitionId, startIndex, endIndex, startTimestamp, endTimestamp, 1.0, null);
        e.updateDatabase(connection, fleetId, flightId, eventDefinitionId);
    }

    private double[] getExtendedRunwayCenterLine(Runway runway) {
        final double LEN = 2.0;
        double dlat = runway.lat1 - runway.lat2;
        double dlon = runway.lon1 - runway.lon2;
        double lat1 = runway.lat1 + LEN * dlat;
        double lon1 = runway.lon1 + LEN * dlon;
        double lat2 = runway.lat2 - LEN * dlat;
        double lon2 = runway.lon2 - LEN * dlon;
        return new double[] {lat1, lon1, lat2, lon2};
    }

    // spooky equation taken from wikipedia - https://en.wikipedia.org/wiki/Distance_from_a_point_to_a_line
    public double getDistanceFromRunwayCenterline(Runway runway, double alat, double alon) {
        double rlatdiff = runway.lat1 - runway.lat2;
        double rlondiff = runway.lon1 - runway.lon2;
        double numerator = Math.abs(rlatdiff*(runway.lon2 - alon) - (runway.lat2 - alat)*rlondiff);
        double denominator = Math.sqrt(rlatdiff*rlatdiff + rlondiff*rlondiff);
        return numerator / denominator;
    }

    public double[] getPosition(int timestep) {
        assert timestep < this.nTimesteps;
        return new double[] { lat[timestep], lon[timestep] };
    }

    // Shamelessly taken from https://stackoverflow.com/questions/9457988/bearing-from-one-coordinate-to-another
    protected static double bearing(double lat1, double lon1, double lat2, double lon2) {
        double latitude1 = Math.toRadians(lat1);
        double latitude2 = Math.toRadians(lat2);
        double longDiff = Math.toRadians(lon2 - lon1);
        double y = Math.sin(longDiff) * Math.cos(latitude2);
        double x = Math.cos(latitude1) * Math.sin(latitude2) - Math.sin(latitude1) * Math.cos(latitude2) * Math.cos(longDiff);

        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }

    private void process(Connection connection) throws SQLException {
        ArrayList<Itinerary> itineraries = Itinerary.getItinerary(connection, flightId);
        LOG.info("Itinerary length: " + itineraries.size());

        // Getting normal distance from Runway heading (at least I think)
//        double normal = Runway.getDistanceFt(lat, lon);

        // Notes: one entry per second

        // For each flight itinerary, we need to trace the flight path
        for (Itinerary it : itineraries) {
            int to = it.minAltitudeIndex;
            if (!it.wasApproach())
                continue;

            System.out.println( "start approach: " + it.startOfApproach + ", end approach: " + it.endOfApproach + "\n" 
                              + "start takeoff:  " + it.startOfTakeoff + ", end takeoff  : " + it.endOfTakeoff + "\n"
                              + "final index:    " + it.finalIndex);

            // Need airport/runway
            Airport airport = Airports.getAirport(it.getAirport());
            Runway runway = airport.getRunway(it.getRunway());
            double runwayAltitude = altitude[0];
            double runwayBearing = bearing(runway.lat1, runway.lon1, runway.lat2, runway.lon2);

            // Finding takeoff: (given runway)
            // Need altitude to increase (maybe > 100ft)
            // Ideally, heading should remain +/-10 degrees

            // TODO MVP New process:
            //  loop through lat,long
            //  acquire bearing relative to runway per recorded position
            //  mark transition points in array for later
            //  keep track of state (which leg plane is on) when processing
            //  for each state, check additional exceedences with if statements
            boolean isUpwind = true, isDownwind = false, isCrosswind = false,
                    isFirstTurn = false, isFinalTurn = false, isSecondTurn = false, isThirdTurn = false;

            int from = it.startOfTakeoff;
            int until = it.endOfTakeoff + 1;
            for (int i = from + 1; i < until; i++) {
                double b = bearing(lat[i-1], lon[i-1], lat[i], lon[i]);
                double normDeg = (runwayBearing - b) % 360;
                // 0-180 degrees, difference between runway and aircraft bearing, used to find flight phase
                double bDiff = Math.min(360-normDeg, normDeg);

                if (bDiff < 20 && !isFirstTurn) {
                    // within 20 degrees of runway --> tracking upwind
                    if (!isUpwind) {
                        // exiting turn to final
                        isUpwind = true;
                        isFinalTurn = false;
                        // TODO mark 'DEBUG EXIT FINAL TURN' event here!
                        createEvent(connection, DEBUG_EXIT_FINAL_TURN, i, i + 1);
                        // TODO event: Low Turn to Final
                        //  trigger if altitude is below 200 feet
                        //  slideshow also mentions checking this anytime after aircraft climbs to 400 feet,
                        //  ^ will need to double check if that is necessary
                    }

                    // TODO event: Poor Tracking on Upwind
                    //  check (lat, long) of aircraft to make sure it fits corridor
                    //  if not, flip arbitrary boolean flag for as long as aircraft is outside corridor,
                    //  create event once it re-enters

                    // TODO: use getDistanceFromRunwayCenterline(lat[i],lon[i] maybe?

                    // FIXME ^^ this event will definitely blow up on a go-around

                } else if ((20 <= bDiff && bDiff < 70) || isFirstTurn || isFinalTurn) {
                    // turning from or into upwind
                    if (!isFirstTurn && !isFinalTurn) { // FIXME we can consolidate booleans if we want
                        if (isUpwind) {
                            // exiting upwind tracking
                            isFirstTurn = true;
                            isUpwind = false;
                            // TODO mark 'DEBUG FIRST TURN' event here!
                            createEvent(connection, DEBUG_ENTER_FIRST_TURN, i, i + 1);

                            // TODO event: Low Turn to Crosswind
                            //  trigger if altitude is below 400 feet
                        } else {
                            // entering turn to final
                            isFinalTurn = true;
                            isCrosswind = false;
                            // TODO mark 'DEBUG ENTER FINAL TURN' event here!
                            createEvent(connection, DEBUG_ENTER_FINAL_TURN, i, i + 1);
                            
                            // TODO: overshoot/undershoot final approach too complex for this implementation
                        }
                    }
                    // do nothing here I guess until we can add final approach event
                    // because other events do not require duration

                } else if (70 <= bDiff && bDiff < 110 && !isFinalTurn && !isSecondTurn) {
                    // tracking crosswind (both times for now...)
                    if (!isCrosswind) {
                        isCrosswind = true;
                        isFirstTurn = false;
                        isThirdTurn = false;
                        // TODO mark 'DEBUG ENTER CROSSWIND' event here!
                        createEvent(connection, DEBUG_ENTER_CROSSWIND, i, i + 1);
                    }

                    // TODO: Constant Turn events too complex for this implementation

                } else if ((110 <= bDiff && bDiff < 160) || isSecondTurn || isThirdTurn) {
                    // turning from or into downwind
                    if (!isSecondTurn && !isThirdTurn) { // FIXME we can consolidate booleans if we want
                        if (isDownwind) {
                            // exiting downwind tracking
                            isThirdTurn = true;
                            isDownwind = false;
                            // TODO mark 'DEBUG THIRD TURN' event here!
                            createEvent(connection, DEBUG_ENTER_THIRD_TURN, i, i + 1);
                        } else {
                            // entering turn to downwind
                            isSecondTurn = true;
                            isCrosswind = false;
                            // TODO mark 'DEBUG SECOND TURN' event here!
                            createEvent(connection, DEBUG_ENTER_SECOND_TURN, i, i + 1);
                        }
                    }
                } else if (bDiff <= 160 && !isThirdTurn){
                    // tracking downwind
                    if (!isDownwind) {
                        isDownwind = true;
                        isSecondTurn = false;
                        // TODO mark 'DEBUG EXIT SECOND TURN' event here!
                    }

                    // TODO event: Wide Downwind
                    //  count number of consecutive positions aircraft was out of bounds
                    //  at end, if counter > 5, trigger the event

                } else {
                    // TODO mark 'something went wrong' event maybe?

                }
            }


            /*
            // FIXME mark beginning index of each state only? seems hacky
            ArrayList<Integer> upwindTracking = new ArrayList<>();
            ArrayList<Integer> downwindTracking = new ArrayList<>();
            ArrayList<Integer> crossToDownwindTracking = new ArrayList<>();
            ArrayList<Integer> crossToUpwindTracking = new ArrayList<>();

            double trailingBearing = bearing(runway.lat1, runway.lon1, runway.lat2, runway.lon2);
            double runwayBearing = bearing(runway.lat1, runway.lon1, runway.lat2, runway.lon2);
            double[] bears = new double[60];
            double[] bearDiffs = new double[60];
            // ^^ will be cheeky ~ overwrite this index equal to i mod 60

            // Path tracing:
            // Precondition - assume index 0 is aircraft beginning takeoff
            // 1. derive heading of aircraft for every adjacent point
            // 2. store max change in heading between 60 seconds (60 points)
            // 3. classify the point as:
            //      Tracking: change is < 30 degrees
            //      Turn: otherwise
            // 4. assign state to trailing point of window
            double maxBearingChange = -1.0;
            double windowB;
            int start = 1, end = lat.length;
            for (int i = start; i < end; i++) {
                double normDeg = (trailingBearing - bearing(lat[i-1], lon[i-1], lat[i], lon[i])) % 360;
                double bDiff = Math.min(360-normDeg, normDeg);

//                normDeg = (runwayBearing - bearing(lat[i-1], lon[i-1], lat[i], lon[i])) % 360;
//                double b = Math.min(360-normDeg, normDeg);
                double b = bearing(lat[i-1], lon[i-1], lat[i], lon[i]);

                // 0, 1, 2, 4, 2, 0, 1, 5, 10, 20, 30, 40, 50, 60, 70, 80, 85, 88, 91, 92, 90


                // Store new bearing in current window
                if (i-1 < 60) {
                    bears[i-1] = b;
                    bearDiffs[i-1] = bDiff;
                    windowB = (bears[0] - b) % 360;
                    windowB = Math.min(360-windowB, windowB);
                } else {
                    bears[(i-1) % 60] = b;
                    bearDiffs[(i-1) % 60] = bDiff;
                    trailingBearing = bearDiffs[i % 60];
                    windowB = bDiff;
                }

                int turnBeginIndex;

                if (bearDiffs[i-1] >= 30) {
                    turnBeginIndex = i - 15;
                }


                // Get max bearing change within the window
                double maxB = -1;
                for (double x : bearDiffs) {
//                    maxB = Math.max(x, maxB);
                    if (x > maxB) {
                        maxB = x;
                    }
                }

                // if maxB > 30, I think we can assume the whole window is a turn
                // TODO this, but also double check above logic makes sense
                //  also make sure states are recorded correctly


                // path DFA:
                // upTrack -> turnCross1 -> crossTrack1 -> turnDown -> downTrack -> turnCross2 -> crossTrack2
                // -> turnFinal or turnUp -> upTrack

            }


             */
        }
    }
}
