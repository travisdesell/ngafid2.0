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

    public void getEvents(ArrayList<Event> events, int eventColumn, double eventLimit, String eventName) {
        int timeColumn = 1;

        Event currentEvent = null;

        for (int i = 0; i < csvValues.size(); i++) {
            ArrayList<String> currentLine = csvValues.get(i);

            String time = current.get(timeColumn);
            double eventValue = Double.parseDouble(current.get(eventColumn));

            System.out.println(time + " : " + eventValue);

            //if (Math.abs(eventValue) >= eventLimit) {
            if (event.isOccuring(currentLine)) {
                if (currentEvent == null) {
                    currentEvent = new Event(time, time, i, i, eventName);
                    System.out.println("CREATED NEW      " + currentEvent);

                } else {
                    currentEvent.updateEnd(time, i);
                    System.out.println("UPDATED END TIME " + currentEvent);
                }

            } else {
                if (currentEvent != null) {
                    if (currentEvent.isOutsideBuffer(i)) {
                        //we're done with this event
                        events.add(currentEvent);
                        System.out.println("FINISHED         " + currentEvent);

                        currentEvent = null;
                    }
                }
            }
        }
    }

    public ArrayList<Event> getEvents() {
        ArrayList<Event> events = new ArrayList<Event>();

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
        
        //getLanding(events, "Landing");
        //getTouchAndGo(events, "Touch And Go");
        //getGoAround(events, "Go Around");

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

        //events.get(0).updateEnd("24:23:02", 84382);
        for (int i = 0; i < events.size(); i++) {
            events.get(i).print();
        }

    }

}
