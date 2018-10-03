package org.ngafid;

import java.io.InputStream;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.ngafid.airframes.Airframe;
import org.ngafid.airframes.C172;
import org.ngafid.airframes.C182;
import org.ngafid.airframes.PA28;
import org.ngafid.airframes.PA44;
import org.ngafid.airframes.SR20;

import org.ngafid.events.Event;

import org.ngafid.flights.Flight;

public class ProcessFlights {
    public static void main(String[] arguments) throws Exception {
        // We need to provide file path as the parameter:
        // double backquote is to avoid compiler interpret words
        // like \test as \t (ie. as a escape sequence)

        System.out.println("Command Line Arguments:");
        for (int i = 0; i < arguments.length; i++) {
            System.out.println("arguments[" + i + "]: '" + arguments[i] + "'");
        }

        if (arguments.length == 0) {
            System.err.println("Incorrect arguments, should take a flight file.");
            System.exit(1);
        }

        //The first argument should be the directory we're going to parse
        //For each subdirectory:
        //  get the airframe type (C172, C182, PA28, etc.)
        //  for each N-Number in that directory:
        //      for each flight in that directory:
        //          create the appropriate airframe object (C172 for C172s)
        //          get events
        //          insert the events and flight information into the database

        ZipFile zipFile = new ZipFile(arguments[0]);

        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();

            if (entry.isDirectory()) {
                //System.err.println("SKIPPING: " + entry.getName());
                continue;
            }

            if (name.contains("__MACOSX")) {
                //System.err.println("SKIPPING: " + entry.getName());
                continue;
            }

            System.err.println("PROCESSING: " + name);
            if (true) continue;

            String[] parts = entry.getName().split("/");
            for (int i = 0; i < parts.length; i++) {
                System.err.println(parts[i]);
            }

            if (entry.getName().contains(".csv")) {
                InputStream stream = zipFile.getInputStream(entry);
                Flight flight = new Flight(entry.getName(), stream);
            }
        }

        String filename = arguments[0];

        Flight flight = new Flight(filename);
        flight.calculateAGL("AltAGL", "AltMSL", "Latitude", "Longitude");
        flight.calculateAirportProximity("Latitude", "Longitude");
        flight.calculateEvents();

        flight.printValues(new String[]{
            "Latitude",
            "Longitude",
            "AltAGL",
            "NearestAirport",
            "AirportDistance",
            "NearestRunway",
            "RunwayDistance"
        });

        ArrayList<Event> events;

        Airframe airframe = null;

        if (filename.contains("C172")) {
            System.out.println("Cessna 172 filetype detected!");
            System.out.println();
            System.out.println();

            airframe = new C172(filename);

        } else if (filename.contains("C182")) {
            System.out.println("C182 filetype detected!");
            System.out.println();
            System.out.println();

            airframe = new C182(filename);

        } else if (filename.contains("PA28")) {
            System.out.println("PA28 filetype detected!");
            System.out.println();
            System.out.println();

            airframe = new PA28(filename);

        } else if (filename.contains("PA44")) {
            System.out.println("PA44 filetype detected!");
            System.out.println();
            System.out.println();

            airframe = new PA44(filename);
        } else if (filename.contains("SR20")) {
            System.out.println("SR20 filetype detected!");
            System.out.println();
            System.out.println();

            airframe = new SR20(filename);

        } else {
            System.out.println("Generic filetype detected!");
            System.out.println();
            System.out.println();

            airframe = new Airframe(arguments[0]);
        }

        airframe.calculateAGL(/*altitude MSL column*/ 8, /*latitude column*/ 4, /*longitude column*/ 5);
        airframe.calculateAirportProximity(/*latitude column*/ 4, /*longitude column*/ 5);

        airframe.printInformation();
        airframe.printValues(new String[]{
            "Latitude",
            "Longitude",
            "Pitch",
            "Roll",
            "AltAGL",
            "NearestAirport",
            "AirportDistance",
            "NearestRunway",
            "RunwayDistance"
        });

        events = airframe.getEvents();

        System.out.println();
        System.out.println();
        System.out.println("ALL EVENTS:");
        for (int i = 0; i < events.size(); i++) {
            System.out.println( events.get(i).toString() );
        }

    }

}
