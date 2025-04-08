package org.ngafid.events.proximity;

import org.ngafid.common.airports.Airports;
import org.ngafid.events.Event;

import java.util.ArrayList;
import java.util.logging.Logger;

public class CalculateProximity {

    // Proximity events (and potentially other complicated event calculations) will have negative IDs so they
    // can be excluded from the regular event calculation process
    private static final Logger LOG = Logger.getLogger(CalculateProximity.class.getName());

    public static double calculateDistance(double flightLatitude, double flightLongitude, double flightAltitude,
                                           double otherFlightLatitude, double otherFlightLongitude,
                                           double otherFlightAltitude) {
        LOG.info("Calculating distance between flights - Flight 1: (" + flightLatitude + ", " + flightLongitude + ", " + flightAltitude + 
                 "), Flight 2: (" + otherFlightLatitude + ", " + otherFlightLongitude + ", " + otherFlightAltitude + ")");

        double lateralDistance = Airports.calculateDistanceInFeet(flightLatitude, flightLongitude,
                otherFlightLatitude, otherFlightLongitude);
        double altDiffFt = Math.abs(flightAltitude - otherFlightAltitude);

        double totalDistance = Math.sqrt((lateralDistance * lateralDistance) + (altDiffFt * altDiffFt));
        LOG.info("Calculated total distance: " + totalDistance + "ft (lateral: " + lateralDistance + "ft, vertical: " + altDiffFt + "ft)");
        return totalDistance;
    }

    public static double calculateLateralDistance(double flightLatitude, double flightLongitude,
                                                  double otherFlightLatitude, double otherFlightLongitude) {
        LOG.info("Calculating lateral distance between flights - Flight 1: (" + flightLatitude + ", " + flightLongitude + 
                 "), Flight 2: (" + otherFlightLatitude + ", " + otherFlightLongitude + ")");

        double distance = Airports.calculateDistanceInFeet(flightLatitude, flightLongitude,
                otherFlightLatitude, otherFlightLongitude);
        LOG.info("Calculated lateral distance: " + distance + "ft");
        return distance;
    }

    public static double calculateVerticalDistance(double flightAltitude, double otherFlightAltitude) {
        LOG.info("Calculating vertical distance between flights - Flight 1: " + flightAltitude + "m, Flight 2: " + otherFlightAltitude + "m");

        double distance = Math.abs(flightAltitude - otherFlightAltitude);
        LOG.info("Calculated vertical distance: " + distance + "m");
        return distance;
    }

    public static double[] calculateRateOfClosure(FlightTimeLocation flightInfo, FlightTimeLocation otherInfo,
                                                  int startLine, int endLine, int otherStartLine, int otherEndLine) {
        LOG.info("Calculating rate of closure - Flight 1: " + flightInfo.flightId + " (lines " + startLine + "-" + endLine + 
                 "), Flight 2: " + otherInfo.flightId + " (lines " + otherStartLine + "-" + otherEndLine + ")");

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

        double previousDistance = calculateDistance(flightInfo.latitude[startLine], flightInfo.longitude[startLine],
                flightInfo.altitudeMSL[startLine], otherInfo.latitude[otherStartLine],
                otherInfo.longitude[otherStartLine], otherInfo.altitudeMSL[otherStartLine]);

        LOG.info("Initial distance: " + previousDistance + "ft");

        ArrayList<Double> rateOfClosure = new ArrayList<Double>();
        int i = (startLine + 1), j = (otherStartLine + 1);
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

            double currentDistance = calculateDistance(flightInfo.latitude[i], flightInfo.longitude[i],
                    flightInfo.altitudeMSL[i], otherInfo.latitude[j], otherInfo.longitude[j], otherInfo.altitudeMSL[j]);

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

        LOG.info("Calculated rate of closure array with " + roc.length + " values");

        // Leave in to verify how things work in these edge cases
        if (startShift < 5 || endShift < 5) System.exit(1);

        return roc;

    }

    public static void addProximityIfNotInList(ArrayList<Event> eventList, Event testEvent) {
        LOG.info("Checking if event exists in list - Flight IDs: " + testEvent.getFlightId() + " and " + testEvent.getOtherFlightId());

        for (Event event : eventList) {
            boolean hasSameFlightIDs =
                    (event.getFlightId() == testEvent.getFlightId() && event.getOtherFlightId() == testEvent.getOtherFlightId());
            boolean hasSameTimestamps =
                    (event.getStartTime().equals(testEvent.getStartTime()) && event.getEndTime().equals(testEvent.getEndTime()));

            if (hasSameFlightIDs && hasSameTimestamps) {
                LOG.info("Event already exists in list - skipping");
                return;
            }
        }

        LOG.info("Adding new event to list");
        eventList.add(testEvent);
    }
}
