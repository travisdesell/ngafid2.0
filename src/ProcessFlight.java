// Java Program to illustrate reading from FileReader
// using BufferedReader
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import java.lang.Math;

import java.util.Arrays;
import java.util.ArrayList;

import events.Event;

public class ProcessFlight {
    private String fileInformation;
    private String[] dataTypes;
    private String[] headers;

    private ArrayList<ArrayList<String>> csvValues;

    public ProcessFlight(String flightFilename) {
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
            //file information -- this was the first line
            fileInformation = bufferedReader.readLine();
            dataTypes = bufferedReader.readLine().split("\\,", -1);;
            headers = bufferedReader.readLine().split("\\,", -1);;

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
        for (int i = 0; i < headers.length; i++) {
            System.out.println("\theaders[" + i + "]: '" + headers[i].trim() + "' (" + dataTypes[i].trim() + ")");
        }
        System.out.println();

        System.out.println("Data Types:");
        for (int i = 0; i < dataTypes.length; i++) {
            System.out.println("\tdataTypes[" + i + "]: '" + dataTypes[i].trim() + "' (" + dataTypes[i].trim() + ")");
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

    public ArrayList<Event> getEvents() {
        EventTracker eventTracker = new EventTracker();

        ArrayList<Event> events = eventTracker.getEvents(csvValues);

        //getLanding(events, "Landing");
        //getTouchAndGo(events, "Touch And Go");
        //getGoAround(events, "Go Around");


        // appraoching runaway
        /*
        // first i will need to know where the aircraft is
        getAirplanePoint <- geoPoint
        //Secon i need to identify the airport
        getAirPortPoint <- detectAirport
        //I need to know how far the arcraft is above the airport
        getAGL <- AircraftAltitiude - airportAltitueAboveSeaLevel
        if aircraftGeoPoint < 1 mile || AGL  < 500 ft
        getDistance ,- updateAirplanePoint
        else if AGL > 200 ft || AGL < 500 ft
        NewAGL <- updateAGL
        esle
        go-around
        else updateDistance
        */

        return events;
    }


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

        ProcessFlight readFlight = new ProcessFlight(arguments[0]);
        readFlight.printInformation();
        readFlight.printValues();

        ArrayList<Event> events = readFlight.getEvents();

        System.out.println();
        System.out.println();
        System.out.println("ALL EVENTS:");
        for (int i = 0; i < events.size(); i++) {
            System.out.println( events.get(i).toString() );
        }

    }

}
