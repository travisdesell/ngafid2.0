package org.ngafid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class CreateEventTracker {

    public static void replaceAll(StringBuilder sb, Pattern pattern, String replacement) {
        Matcher m = pattern.matcher(sb);
        int start = 0;
        while (m.find(start)) {
            sb.replace(m.start(), m.end(), replacement);
            start = m.start() + replacement.length();
        }
    }


    public static void generateTrackerCode(String eventName, String[] requiredColumns, String[] conditions, String[] rules, String outputFilename) throws Exception {
        //write the java class out to a file
        File templateFile = new File("./src/main/java/org/ngafid/generated/ProcessFile.template");
        BufferedReader bufferedReader = new BufferedReader(new FileReader(templateFile));
        StringBuilder template = new StringBuilder();

        String line = bufferedReader.readLine();
        while (line != null) {
            template.append(line);
            template.append("\n");
            line = bufferedReader.readLine();
        }
        bufferedReader.close();

        String createSeriesCode = "DoubleTimeSeries pitchSeries = flight.getDoubleTimeSeries";
        //String conditionsCode = "current < minValue || current > maxValue";
        String minValues = "-10.0";
        String maxValues = "10.0";

        if (conditions.length - 1 != rules.length) {
            System.err.println("Error: tried to create tracker code for '" + eventName + "' but the number of conditions (" + conditions.length + " - 1) was != the number of rules (" + rules.length + ")");

            System.err.println("conditions:");
            for (int i = 0; i < conditions.length; i++) {
                System.err.println("\t" + conditions[i]);
            }

            System.err.println("rules:");
            for (int i = 0; i < rules.length; i++) {
                System.err.println("\t" + rules[i]);
            }
            System.exit(1);
        }

        String conditionsCode = conditions[0];
        for (int i = 1; i < conditions.length; i++) {
            conditionsCode += " " + rules[i-1] + " " + conditions[i];
        }

        replaceAll(template, Pattern.compile("MIN_VALUE"), minValues);
        replaceAll(template, Pattern.compile("MAX_VALUE"), maxValues);
        replaceAll(template, Pattern.compile("DOUBLE_TIME_SERIES_CODE"), createSeriesCode + "(" +"\""+ eventName + "\""+");");
        //replaceAll(template, Pattern.compile("CONDITIONS_CODE"), conditionsCode);
        replaceAll(template, Pattern.compile("CONDITIONS_CODE"), conditionsCode);
        replaceAll(template, Pattern.compile("CLASS_NAME"), "Track" + eventName + "Events");

        System.out.println(template);

        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputFilename));
        bufferedWriter.append(template);
        bufferedWriter.close();
    }

    public static void main(String[] arguments) throws Exception {
        String eventName = "Pitch";
        String[] requiredColumns = new String[]{"Pitch"};
        //String minValues = "-10.0";
        //String maxValues = "10.0";
        //String rule = "||"; // choose eaither (or: "||") or (and:&&) 

        //String or;
        //String rule;
        //String logic;
        //String or="||";
        //String and = " && ";
        //if (logic == or)
        //    rule="||";
        //else
        //    rule="&&";

        //String conditions = "currentExceedance <" + minValue +" "+ rule + " currentExceedance > " + maxValue;
        String[] conditions = new String[]{"currentExceedance <  minValue", "currentExceedance >  + maxValue"};
        String[] rules = new String[]{"||"};

        String outputFilename = "./src/main/java/org/ngafid/generated/TrackPitchEvents.java";

        generateTrackerCode(eventName, requiredColumns, conditions, rules, outputFilename);
        /* 

           eventName = "Low Fuel";
           requiredColumns = new String[]{"FQtyL", "FQtyR"};
           conditions = "FQtyL + FQtyR < 8";
           outputFilename = "TrackLowFuelEvents.java";

           generateTrackerCode(eventName, requiredColumns, conditions, outputFilename);


           eventName = "Low Airspeed on Climbout";
           requiredColumns = new String[]{"IAS", "VSpd", "AltAGL"};
           conditions = "IAS > 52 AND VSpd > 0 AND AltAGL >= 100 AND AltAGL <= 500";
           outputFilename = "TrackLowFuelEvents.java";

           generateTrackerCode(eventName, requiredColumns, conditions, outputFilename);
           */
    }
}
