package org.ngafid.events_db;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.swing.JOptionPane;


public class ConditionGenerator {

    public static void replaceAll(StringBuilder sb, Pattern pattern, String replacement) {
        Matcher m = pattern.matcher(sb);
        int start = 0;
        while (m.find(start)) {
            sb.replace(m.start(), m.end(), replacement);
            start = m.start() + replacement.length();
        }
    }

    public static void generateTrackerCode(String outputFilename) throws Exception {
        //write the java class out to a file
        File templateFile = new File("./src/main/java/org/ngafid/events_db/MainProcessFile.template");
        BufferedReader bufferedReader = new BufferedReader(new FileReader(templateFile));
        StringBuilder template = new StringBuilder();

        String line = bufferedReader.readLine();
        while (line != null) {
            template.append(line);
            template.append("\n");
            line = bufferedReader.readLine();
        }
        bufferedReader.close();

        String minValues;
        minValues = JOptionPane.showInputDialog("Min Value", "Please Enter Min Value");
        System.out.println("Min value Entered as: " + " [" + minValues + "] " +"\n");

        String maxValues;
        maxValues = JOptionPane.showInputDialog("Max Value", "Please Enter Max Value");
        System.out.println("Max value Entered as: " + " [" + maxValues + "] " +"\n");

        String bufferTime;
        bufferTime = JOptionPane.showInputDialog("Buffer Value", "Please Enter Buffer Value");
        System.out.println("Buffer value Entered as: " + " [" + bufferTime + "] " +"\n");

        String eventName;
        eventName = JOptionPane.showInputDialog("Event Name", "Please Enter Event Name");
        System.out.println("Event Name Entered as: " + " [" + eventName + "] " +"\n");

        String conditionsCode;
        conditionsCode = JOptionPane.showInputDialog("Event Condition: Please use as an example show", "current < minValue || current > maxValue");
        System.out.println("Condition Entered as: " + " [" + conditionsCode + "] " +"\n");

        // if (conditions.length - 1 != rules.length) {
        //     System.err.println("Error: tried to create tracker code for '" + eventName + "' but the number of conditions (" + conditions.length + " - 1) was != the number of rules (" + rules.length + ")");

        //     System.err.println("conditions:");
        //     for (int i = 0; i < conditions.length; i++) {
        //         System.err.println("\t" + conditions[i]);
        //     }

        //     System.err.println("rules:");
        //     for (int i = 0; i < rules.length; i++) {
        //         System.err.println("\t" + rules[i]);
        //     }
        //     System.exit(1);
        // }

        // String conditionsCode = conditions[0];
        // for (int i = 1; i < conditions.length; i++) {
        //     conditionsCode += " " + rules[i-1] + " " + conditions[i];
        // }

        replaceAll(template, Pattern.compile("MIN_VALUE"), minValues);
        replaceAll(template, Pattern.compile("MAX_VALUE"), maxValues);
        replaceAll(template, Pattern.compile("BUFFER_VALUE"), bufferTime);
        replaceAll(template, Pattern.compile("EVENT_NAME"), eventName);
        replaceAll(template, Pattern.compile("CONDITIONS_CODE"), conditionsCode);
        replaceAll(template, Pattern.compile("CLASS_NAME"), "Calculate" + eventName + "Events");

        System.out.println(template);

        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputFilename));
        bufferedWriter.append(template);
        bufferedWriter.close();
    }

    public static void main(String[] arguments) throws Exception {
       
        
        //String[] conditions = new String[]{"current < minValue", "current > + maxValue"};
        String outputFilename = "./src/main/java/org/ngafid/events_db/CalculatePitchEvents.java";

        generateTrackerCode(outputFilename);
        //System.exit(1);
    }
}
