package org.ngafid.airframes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


import java.util.Arrays;
import java.util.ArrayList;

import org.ngafid.EventTracker;
import org.ngafid.events.Event;

import org.ngafid.terrain.TerrainCache;
import org.ngafid.airports.Airport;
import org.ngafid.airports.Airports;
import org.ngafid.airports.Runway;

import org.ngafid.common.MutableDouble;

public class Airframe {
    private String fileInformation;
    private ArrayList<String> dataTypes;
    private ArrayList<String> headers;

    protected ArrayList<ArrayList<String>> csvValues;

    public Airframe(String flightFilename) {
        File file = new File(flightFilename);

        BufferedReader bufferedReader = null;

        try {
            bufferedReader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException fileNotFoundException) {
            System.err.println("Could not find flight file: '" + flightFilename + "'");
            fileNotFoundException.printStackTrace();
            System.exit(1);
        }

        try {
            dataTypes = new ArrayList<String>();
            headers = new ArrayList<String>();


            //file information -- this was the first line
            fileInformation = bufferedReader.readLine();
            dataTypes.addAll( Arrays.asList( bufferedReader.readLine().split("\\,", -1) ) );
            headers.addAll( Arrays.asList( bufferedReader.readLine().split("\\,", -1) ) );
            dataTypes.replaceAll(String::trim);
            headers.replaceAll(String::trim);

            csvValues = new ArrayList<ArrayList<String>>();

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                //split up the values by commas into our array of strings
                String[] values = line.split("\\,", -1);

                //trim the excess whitespace
                for (int i = 0; i < values.length; i++) {
                    values[i] = values[i].trim();
                }

                //initialize a new ArrayList for this line of values
                ArrayList<String> currentValues = new ArrayList<String>();

                //add every String in values to this new ArrayList
                currentValues.addAll(Arrays.asList(values));

                //add the new ArrayList to our ArrayList of ArrayLists
                csvValues.add(currentValues);
            }
        } catch (IOException ioException) {
            System.err.println("IOException while reading file: '" + flightFilename + "'");
            ioException.printStackTrace();
            System.exit(1);
        }
    }

    public void printInformation() {
        System.out.println("File Information: ");
        System.out.println(fileInformation);
        System.out.println();

        System.out.println("Headers:");
        for (int i = 0; i < headers.size(); i++) {
            System.out.println("\theaders[" + i + "]: '" + headers.get(i).trim() + "' (" + dataTypes.get(i).trim() + ")");
        }
        System.out.println();

        System.out.println("Data Types:");
        for (int i = 0; i < dataTypes.size(); i++) {
            System.out.println("\tdataTypes[" + i + "]: '" + dataTypes.get(i).trim() + "' (" + dataTypes.get(i).trim() + ")");
        }
        System.out.println();
    }

    public void printValues() {
        System.out.println("Values:");

        for (int i = 0; i < csvValues.size(); i++) {
            ArrayList<String> current = csvValues.get(i);

            for (int j = 0; j < current.size(); j++) {
                String value = current.get(j);

                if (j > 0) System.out.print(",");
                System.out.print(value);
            }
            System.out.println();
        }
        System.out.println();
    }

    public void printValues(String[] requestedHeaders) {
        System.out.println("Values:");

        for (int i = 0; i < requestedHeaders.length; i++) {
            if (i > 0) System.out.print(",");
            System.out.print(requestedHeaders[i]);
        }
        System.out.println();


        for (int i = 0; i < csvValues.size(); i++) {
            ArrayList<String> current = csvValues.get(i);

            boolean first = true;
            for (int j = 0; j < current.size(); j++) {
                if (!Arrays.asList(requestedHeaders).contains(headers.get(j))) {
                    continue;
                }

                String value = current.get(j);

                if (!first) System.out.print(",");
                System.out.print(value);

                first = false;
            }
            System.out.println();
        }
        System.out.println();
    }

    public ArrayList<Event> getEvents() {
        EventTracker genericEventTracker = new EventTracker(new String[]{
            "org.ngafid.events.PitchEvent",
                "org.ngafid.events.RollEvent",
                "org.ngafid.events.VerticalAccelerationEvent",
                "org.ngafid.events.LateralAccelerationEvent",
                //"org.ngafid.events.IndicatedAirspeedEvent",
                "org.ngafid.events.LongitudinalAccelerationEvent",
                "org.ngafid.events.VsiEvent"
        });

        ArrayList<Event> events = genericEventTracker.getEvents(csvValues);

        return events;
    }

    public void calculateAGL(int mslColumn, int latitudeColumn, int longitudeColumn) {
        //calculates altitudeAGL (above ground level) from altitudeMSL (mean sea levl)
        headers.add("AltAGL");
        dataTypes.add("ft agl");

        for (int i = 0; i < csvValues.size(); i++) {
            double altitudeMSL = Double.parseDouble(csvValues.get(i).get(mslColumn));
            double latitude = Double.parseDouble(csvValues.get(i).get(latitudeColumn));
            double longitude = Double.parseDouble(csvValues.get(i).get(longitudeColumn));

            int altitudeAGL = TerrainCache.getAltitudeFt(altitudeMSL, latitude, longitude);

            csvValues.get(i).add(Integer.toString(altitudeAGL));

            //System.out.println("msl: " + altitudeMSL + ", agl: " + altitudeAGL);
        }
    }

    public void calculateAirportProximity(int latitudeColumn, int longitudeColumn) {
        //calculates if the aircraft is within maxAirportDistance from an airport
        headers.add("NearestAirport");
        dataTypes.add("IATA Code");

        headers.add("AirportDistance");
        dataTypes.add("ft");

        headers.add("NearestRunway");
        dataTypes.add("IATA Code");

        headers.add("RunwayDistance");
        dataTypes.add("ft");

        final double max_distance_ft = 5000;

        for (int i = 0; i < csvValues.size(); i++) {
            double latitude = Double.parseDouble(csvValues.get(i).get(latitudeColumn));
            double longitude = Double.parseDouble(csvValues.get(i).get(longitudeColumn));

            MutableDouble airportDistance = new MutableDouble();
            Airport airport = Airports.getNearestAirportWithin(latitude, longitude, max_distance_ft, airportDistance);
            if (airport == null) {
                csvValues.get(i).add(null);
                csvValues.get(i).add(null);
                csvValues.get(i).add(null);
                csvValues.get(i).add(null);

                //System.out.println(latitude + ", " + longitude + ", null, null, null, null");
            } else {
                csvValues.get(i).add(airport.iataCode);
                csvValues.get(i).add(Double.toString(airportDistance.get()));

                MutableDouble runwayDistance = new MutableDouble();
                Runway runway = airport.getNearestRunwayWithin(latitude, longitude, max_distance_ft, runwayDistance);
                if (runway == null) {
                    csvValues.get(i).add(null);
                    csvValues.get(i).add(null);
                    //System.out.println(latitude + ", " + longitude + ", " + airport.iataCode + ", " + airportDistance.get() + ", " + null + ", " + null);
                } else {
                    csvValues.get(i).add(runway.name);
                    csvValues.get(i).add(Double.toString(runwayDistance.get()));
                    //System.out.println(latitude + ", " + longitude + ", " + airport.iataCode + ", " + airportDistance.get() + ", " + runway.name + ", " + runwayDistance.get());
                }
            }

        }
    }

}
