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

public class ReadFlight {
    private String fileInformation;
    private String[] dataTypes;
    private String[] headers;

    private ArrayList<ArrayList<String>> csvValues;

    public ReadFlight(String flightFilename) {
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
        ArrayList<Event> events = new ArrayList<Event>();

        /*
        int pitchColumn = 13;
        double maxPitch = 10.0;
        getEvents(events, pitchColumn, maxPitch, "Pitch Exceedence");

        int rollColumn = 14;
        double maxRoll = 20.0;
        getEvents(events, rollColumn, maxRoll, "Roll Exceedence");

        //do this for the other events on the webpage
        int latAcColumn = 15;
        double maxLatAc = 0.04;
        getEvents(events, latAcColumn, maxLatAc, "Lateral Accelaration");

        int normAcColumn = 16;
        double maxNormAc = 0.05;
        getEvents(events, normAcColumn, maxNormAc, "Vertical (Normal) Exceedence");

        int longAcColumn = 5;
        double maxLongAc = 96.6199;
        getEvents(events, longAcColumn, maxLongAc, "Longitudinal Acceleration");

        int vsiColumn = 12;
        double maxVsi = 16;
        getEvents(events, vsiColumn, maxVsi, "VSI on Final");
        */

        //getLanding(events, "Landing");
        //getTouchAndGo(events, "Touch And Go");
        //getGoAround(events, "Go Around");

        int timeColumn = 1;

        PitchEvent pitchEvent = null;

        for (int i = 0; i < csvValues.size(); i++) {
            ArrayList<String> currentLine = csvValues.get(i);

            String time = currentLine.get(timeColumn);

            if (PitchEvent.isOccuring(currentLine)) {
                if (pitchEvent == null) {
                    pitchEvent = new PitchEvent(time, time, i, i);
                    System.out.println("CREATED NEW      " + pitchEvent);

                } else {
                    pitchEvent.updateEnd(time, i);
                    System.out.println("UPDATED END TIME " + pitchEvent);
                }

            } else {
                if (pitchEvent != null) {
                    if (pitchEvent.isOutsideBuffer(i)) {
                        //we're done with this event
                        events.add(pitchEvent);
                        System.out.println("FINISHED         " + pitchEvent);

                        pitchEvent = null;
                    }
                }
            }

        }

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

        ReadFlight readFlight = new ReadFlight(arguments[0]);
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
