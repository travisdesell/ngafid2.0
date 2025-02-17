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

        double lateralDistance = Airports.calculateDistanceInFeet(flightLatitude, flightLongitude,
                otherFlightLatitude, otherFlightLongitude);
        double altDiffFt = Math.abs(flightAltitude - otherFlightAltitude);

        return Math.sqrt((lateralDistance * lateralDistance) + (altDiffFt * altDiffFt));
    }

    /*
     * static String timeSeriesName = "Lcl Time";
     * static String dateSeriesName = "Lcl Date";
     */

    public static double calculateLateralDistance(double flightLatitude, double flightLongitude,
                                                  double otherFlightLatitude, double otherFlightLongitude) {

        return Airports.calculateDistanceInFeet(flightLatitude, flightLongitude,
                otherFlightLatitude, otherFlightLongitude);

    }

    public static double calculateVerticalDistance(double flightAltitude, double otherFlightAltitude) {

        return Math.abs(flightAltitude - otherFlightAltitude);

    }

    public static double[] calculateRateOfClosure(FlightTimeLocation flightInfo, FlightTimeLocation otherInfo,
                                                  int startLine, int endLine, int otherStartLine, int otherEndLine) {

        int shift = 5;
        int newStart1 = Math.max((startLine - shift), 0);
        int newStart2 = Math.max((otherStartLine - shift), 0);
        int startShift1 = startLine - newStart1;
        int startShift2 = otherStartLine - newStart2;
        int startShift = Math.min(startShift1, startShift2);

        newStart1 = startLine - startShift;
        newStart2 = otherStartLine - startShift;

        int newEnd1 = Math.min((endLine + shift), flightInfo.epochTime.length);
        int newEnd2 = Math.min((otherEndLine + shift), otherInfo.epochTime.length);
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

        ArrayList<Double> rateOfClosure = new ArrayList<Double>();
        int i = (startLine + 1), j = (otherStartLine + 1);
        while (i < endLine && j < otherEndLine) {
            if (flightInfo.epochTime[i] == 0) {
                i++;
                continue;
            }

            if (otherInfo.epochTime[j] == 0) {
                j++;
                continue;
            }

            // Ensure both iterators are for the same time
            if (flightInfo.epochTime[i] < otherInfo.epochTime[j]) {
                i++;
                continue;
            }
            if (otherInfo.epochTime[j] < flightInfo.epochTime[i]) {
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

        // Leave in to verify how things work in these edge cases
        if (startShift < 5 || endShift < 5) System.exit(1);

        return roc;

    }

    public static boolean addProximityIfNotInList(ArrayList<Event> eventList, Event testEvent) {

        for (Event event : eventList) {

            boolean hasSameFlightIDs =
                    (event.getFlightId() == testEvent.getFlightId() && event.getOtherFlightId() == testEvent.getOtherFlightId());
            boolean hasSameTimestamps =
                    (event.getStartTime().equals(testEvent.getStartTime()) && event.getEndTime().equals(testEvent.getEndTime()));

            // Event already in the list, don't add it again
            if (hasSameFlightIDs && hasSameTimestamps) return false;

        }

        // Event not in the list, add it
        eventList.add(testEvent);
        return true;
    }
}
