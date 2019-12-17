package org.ngafid.flights;

import org.ngafid.airports.Airport;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class TurnToFinal {
    private static final Logger LOG = Logger.getLogger(TurnToFinal.class.getName());

    // List of (lat, long) coords (in that order) representing a turn to final
    private double[] latitude;
    private double[] longitude;

    private int nTimesteps;

    public TurnToFinal(double[] lat, double[] lon, int from, int to) {
        this.latitude = Arrays.copyOfRange(lat, from, to);
        this.longitude = Arrays.copyOfRange(lon, from, to);
    }

    public double[] getPosition(int timestep) {
        assert timestep < this.nTimesteps;
        return new double[] { latitude[timestep], longitude[timestep] };
    }

    public static ArrayList<TurnToFinal> getTurnToFinal(Connection connection, int flightId) throws SQLException {
        // For now just use the flight object to get lat and long series
        // In the future we could just get the lat and long series in isolation to speed things up
        Flight flight = Flight.getFlight(connection, flightId);
        DoubleTimeSeries latTimeSeries = flight.getDoubleTimeSeries("Latitude");
        DoubleTimeSeries lonTimeSeries = flight.getDoubleTimeSeries("Longitude");

        assert latTimeSeries != null;
        assert lonTimeSeries != null;

        double[] lat = new double[latTimeSeries.size()];
        double[] lon = new double[lonTimeSeries.size()];

        assert lat.length == lon.length;

        for (int i = 0; i < lat.length; i++) {
            lat[i] = latTimeSeries.get(i);
            lon[i] = lonTimeSeries.get(i);
        }

        ArrayList<Itinerary> itineraries = Itinerary.getItinerary(connection, flightId);

        // I don't think this should happen?
        if (itineraries.size() == 0) return new ArrayList<>();

        ArrayList<TurnToFinal> ttfs = new ArrayList<>(itineraries.size());

        for (Itinerary it : itineraries) {
            int     from = -1,
                    to   = -1;

            // Airport airport = new Airport();



            ttfs.add(new TurnToFinal(lat, lon, from, to));
        }
        return null;
    }
}
