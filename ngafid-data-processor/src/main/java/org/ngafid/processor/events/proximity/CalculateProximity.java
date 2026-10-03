package org.ngafid.processor.events.proximity;

import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Logger;
import org.ngafid.core.airports.Airports;
import org.ngafid.core.event.Event;

/**
 * Detects proximity (near-miss) events by comparing a flight's track against those of other flights.
 *
 * <p>Provides the geometry used for proximity detection -- 3D and lateral separation between aircraft positions --
 * and drives the search for other flights whose time span and spatial bounds overlap, recording proximity
 * {@link Event}s with negative identifiers so they are excluded from the ordinary event-calculation pipeline.
 */
public class CalculateProximity {

    private CalculateProximity() {
        // Utility class
    }

    // Proximity events (and potentially other complicated event calculations) will have negative IDs so they
    // can be excluded from the regular event calculation process
    private static final Logger LOG = Logger.getLogger(CalculateProximity.class.getName());

    /**
     * Computes the straight-line 3D distance in feet between two aircraft positions, combining the great-circle
     * lateral distance between the two latitude/longitude points with the absolute altitude difference
     * (distance = hypotenuse of the lateral distance and the altitude difference).
     *
     * @param flightLatitude the primary aircraft's latitude, in degrees
     * @param flightLongitude the primary aircraft's longitude, in degrees
     * @param flightAltitude the primary aircraft's altitude, in feet
     * @param otherFlightLatitude the other aircraft's latitude, in degrees
     * @param otherFlightLongitude the other aircraft's longitude, in degrees
     * @param otherFlightAltitude the other aircraft's altitude, in feet
     * @return the 3D separation between the two aircraft, in feet
     */
    public static double calculateDistance(
            double flightLatitude,
            double flightLongitude,
            double flightAltitude,
            double otherFlightLatitude,
            double otherFlightLongitude,
            double otherFlightAltitude) {

        double lateralDistance = Airports.calculateDistanceInFeet(
                flightLatitude, flightLongitude, otherFlightLatitude, otherFlightLongitude);
        double altDiffFt = Math.abs(flightAltitude - otherFlightAltitude);

        return Math.sqrt((lateralDistance * lateralDistance) + (altDiffFt * altDiffFt));
    }

    /**
     * Computes the lateral (horizontal) great-circle distance in feet between two latitude/longitude points,
     * ignoring altitude.
     *
     * @param flightLatitude the primary aircraft's latitude, in degrees
     * @param flightLongitude the primary aircraft's longitude, in degrees
     * @param otherFlightLatitude the other aircraft's latitude, in degrees
     * @param otherFlightLongitude the other aircraft's longitude, in degrees
     * @return the horizontal distance between the two points, in feet
     */
    public static double calculateLateralDistance(
            double flightLatitude, double flightLongitude, double otherFlightLatitude, double otherFlightLongitude) {

        return Airports.calculateDistanceInFeet(
                flightLatitude, flightLongitude, otherFlightLatitude, otherFlightLongitude);
    }

    /**
     * Computes the vertical separation in feet between two aircraft as the absolute difference of their altitudes.
     *
     * @param flightAltitude the primary aircraft's altitude, in feet
     * @param otherFlightAltitude the other aircraft's altitude, in feet
     * @return the absolute altitude difference, in feet
     */
    public static double calculateVerticalDistance(double flightAltitude, double otherFlightAltitude) {

        return Math.abs(flightAltitude - otherFlightAltitude);
    }

    /**
     * Computes the per-sample rate of closure between two aircraft across a proximity event, i.e. how quickly the
     * 3D distance between them changes over time. The analysis window is padded by a few samples on each side of
     * the event, then the two flights' time series are stepped through in epoch-time lockstep (skipping samples
     * with a zero/missing timestamp); for each aligned step the change in 3D distance from the previous step is
     * recorded. A positive value means the aircraft are closing, a negative value means they are separating.
     *
     * @param flightInfo the primary flight's loaded time/location series
     * @param otherInfo the other flight's loaded time/location series
     * @param startLine the primary flight's event start index
     * @param endLine the primary flight's event end index
     * @param otherStartLine the other flight's event start index
     * @param otherEndLine the other flight's event end index
     * @return the rate-of-closure values (feet per step) sampled across the padded event window
     */
    public static double[] calculateRateOfClosure(
            FlightTimeLocation flightInfo,
            FlightTimeLocation otherInfo,
            int startLine,
            int endLine,
            int otherStartLine,
            int otherEndLine) {

        int shift = 5;
        int newStart1 = Math.max((startLine - shift), 0);
        int newStart2 = Math.max((otherStartLine - shift), 0);
        int startShift1 = startLine - newStart1;
        int startShift2 = otherStartLine - newStart2;
        int startShift = Math.min(startShift1, startShift2);

        newStart1 = startLine - startShift;
        newStart2 = otherStartLine - startShift;

        int newEnd1 = Math.min((endLine + shift), flightInfo.epochTime.size());
        int newEnd2 = Math.min((otherEndLine + shift), otherInfo.epochTime.size());
        int endShift1 = newEnd1 - endLine;
        int endShift2 = newEnd2 - otherEndLine;
        int endShift = Math.min(endShift1, endShift2);

        newEnd1 = endLine + endShift;
        newEnd2 = otherEndLine + endShift;

        startLine = newStart1;
        otherStartLine = newStart2;
        endLine = newEnd1;
        otherEndLine = newEnd2;

        double previousDistance = calculateDistance(
                flightInfo.latitude[startLine],
                flightInfo.longitude[startLine],
                flightInfo.altitudeMSL[startLine],
                otherInfo.latitude[otherStartLine],
                otherInfo.longitude[otherStartLine],
                otherInfo.altitudeMSL[otherStartLine]);

        ArrayList<Double> rateOfClosure = new ArrayList<Double>();
        int i = (startLine + 1);
        int j = (otherStartLine + 1);
        while (i < endLine && j < otherEndLine) {
            if (flightInfo.epochTime.get(i) == 0) {
                i++;
                continue;
            }

            if (otherInfo.epochTime.get(j) == 0) {
                j++;
                continue;
            }

            // Ensure both iterators are for the same time
            if (flightInfo.epochTime.get(i) < otherInfo.epochTime.get(j)) {
                i++;
                continue;
            }
            if (otherInfo.epochTime.get(j) < flightInfo.epochTime.get(i)) {
                j++;
                continue;
            }

            double currentDistance = calculateDistance(
                    flightInfo.latitude[i],
                    flightInfo.longitude[i],
                    flightInfo.altitudeMSL[i],
                    otherInfo.latitude[j],
                    otherInfo.longitude[j],
                    otherInfo.altitudeMSL[j]);

            rateOfClosure.add(previousDistance - currentDistance);
            previousDistance = currentDistance;
            i++;
            j++;
        }

        // Convert the ArrayList to a primitive array
        double[] roc = new double[rateOfClosure.size()];

        for (int k = 0; k < roc.length; k++) {
            roc[k] = rateOfClosure.get(k);
        }

        // Handle edge cases where we don't have enough data points
        if (startShift < 5 || endShift < 5) {
            LOG.info(String.format(
                    "Insufficient data points for rate of closure calculation: startShift=%d, "
                            + "endShift=%d. Skipping rate of closure calculation.",
                    startShift, endShift));
            return new double[0];
        }

        return roc;
    }

    /**
     * Appends a proximity event to the list, guarding against bad or redundant entries: a null event is logged and
     * ignored, a self-referential event (whose other-flight id equals its own flight id) is skipped, and an event
     * that duplicates one already in the list (same flight id, same other-flight id, and same start and end times)
     * is skipped. Otherwise the event is added.
     *
     * @param eventList the list of accumulated proximity events to add to
     * @param testEvent the candidate event to add; may be null (ignored)
     */
    public static void addProximityIfNotInList(ArrayList<Event> eventList, Event testEvent) {
        // Validate input
        if (testEvent == null) {
            LOG.warning("Attempted to add null event to list");
            return;
        }

        // Skip self-referential events
        Integer otherFlightId = testEvent.getOtherFlightId();
        if (otherFlightId != null && otherFlightId.equals(testEvent.getFlightId())) {
            LOG.warning(String.format(
                    "Skipping self-referential event: flight_id=%d, other_flight_id=%d, start_time=%s, end_time=%s",
                    testEvent.getFlightId(),
                    testEvent.getOtherFlightId(),
                    testEvent.getStartTime(),
                    testEvent.getEndTime()));
            return;
        }

        // Check for duplicate events
        for (Event event : eventList) {
            Integer eventOtherFlightId = event.getOtherFlightId();
            Integer testEventOtherFlightId = testEvent.getOtherFlightId();
            boolean hasSameFlightIDs = (event.getFlightId() == testEvent.getFlightId()
                    && Objects.equals(eventOtherFlightId, testEventOtherFlightId));
            boolean hasSameTimestamps = (event.getStartTime().equals(testEvent.getStartTime())
                    && event.getEndTime().equals(testEvent.getEndTime()));

            if (hasSameFlightIDs && hasSameTimestamps) {
                LOG.info(String.format(
                        "Skipping duplicate event: flight_id=%d, other_flight_id=%d, start_time=%s, end_time=%s",
                        testEvent.getFlightId(),
                        testEvent.getOtherFlightId(),
                        testEvent.getStartTime(),
                        testEvent.getEndTime()));
                return;
            }
        }

        // Event not in the list, add it
        LOG.info(String.format(
                "Adding new event: flight_id=%d, other_flight_id=%d, start_time=%s, end_time=%s",
                testEvent.getFlightId(),
                testEvent.getOtherFlightId(),
                testEvent.getStartTime(),
                testEvent.getEndTime()));
        eventList.add(testEvent);
    }
}
