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

public class ReadFlightPreProcess {
    private String fileInformation;
    private String[] dataTypes;
    private String[] headers;

    private ArrayList<ArrayList<String>> csvValues;

    public ReadFlightPreProcess(String flightFilename) {
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

    public void getExceedences(ArrayList<Exceedence> exceedences, int exceedenceColumn, double exceedenceLimit, String exceedenceName) {
        int timeColumn = 1;
        int exceedenceBuffer = 10;

        Exceedence currentExceedence = null;

        for (int i = 0; i < csvValues.size(); i++) {
            ArrayList<String> current = csvValues.get(i);

            String time = current.get(timeColumn);
            double exceedenceValue = Double.parseDouble(current.get(exceedenceColumn));

            System.out.println(time + " : " + exceedenceValue);

            if (Math.abs(exceedenceValue) >= exceedenceLimit) {
                if (currentExceedence == null) {
                    currentExceedence = new Exceedence(time, time, i, i, exceedenceName);
                    System.out.println("CREATED NEW      " + currentExceedence);

                } else {
                    currentExceedence.updateEnd(time, i);
                    System.out.println("UPDATED END TIME " + currentExceedence);
                }

            } else {
                if (currentExceedence != null) {
                    if ((i - currentExceedence.getEndLine()) > exceedenceBuffer) {
                        //we're done with this exceedence
                        exceedences.add(currentExceedence);
                        System.out.println("FINISHED         " + currentExceedence);

                        currentExceedence = null;
                    }
                }
            }
        }

    }

    public ArrayList<Exceedence> getExceedences() {
        ArrayList<Exceedence> exceedences = new ArrayList<Exceedence>();

        int pitchColumn = 13;
        double maxPitch = 10.0;
        getExceedences(exceedences, pitchColumn, maxPitch, "PITCH");

        int rollColumn = 14;
        double maxRoll = 20.0;
        getExceedences(exceedences, rollColumn, maxRoll, "ROLL");

        //do this for the other exceedences on the webpage

        return exceedences;
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

        ReadFlightPreProcess readFlightPreProcess = new ReadFlightPreProcess(arguments[0]);
        readFlightPreProcess.printInformation();
        readFlightPreProcess.printValues();

        ArrayList<Exceedence> exceedences = readFlightPreProcess.getExceedences();

        //exceedences.get(0).updateEnd("24:23:02", 84382);
        for (int i = 0; i < exceedences.size(); i++) {
            exceedences.get(i).print();
        }

    }

}
