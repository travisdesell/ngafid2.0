
package org.ngafid.events_db;

import java.util.Scanner;
import java.util.ArrayList;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.ngafid.events_db.CalculateExceedanceNew;


class Condition {

    Condition(String conditionString) {
        //use scanner to process conditionString and set up 
        //this object
    }

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        System.out.println("--------------------------------------------------------------------------------------------------------------------------------------------------------------------\n");
        System.out.print("Please Enter The Logical Input That You want To Use Regarding Flight Exceedance [OR (Threshold < minValue || Threshold > maxValue), AND , BOTH , NONE or CUSTOME]: ");

        String operator = scanner.nextLine();
        System.out.println("---------------------\n");
        
        //double bufferTime = scanner.nextDouble();
        String eventName = scanner.nextLine();
        System.out.print("Enter your Event Name: ");
        double bufferTime = scanner.nextDouble();
        System.out.print("Enter your Buffer Time: ");
        double minValue = scanner.nextDouble();
        System.out.print("Enter your min Value: ");
        double maxValue = scanner.nextDouble(); 
        System.out.print("Enter your max Value: ");
          
            // String full_condition;
            // full_name = maxValue + " " + minValue;
            // System.out.println("eventName " + eventName);

            switch (operator) {
                case "OR":
                    System.out.print("User Generated condition is: " + " [" + " current < minValue || current > maxValue" + " ] " + "\n");
                    System.out.print("User deatiled condition is: " + " [" + " current" + " < " + minValue + " || " + "current" + " > " + maxValue + " ] " + "\n");
                    System.out.println("buffer time entered as: " + " [" + bufferTime + "] " +"\n");
                    System.out.println("-------------------------\n");
                    break;

                case "AND":
                    System.out.print("User Generated condition is: " + " [" + " current < minValue && current > maxValue" + " ] " + "\n");
                    System.out.print("User Generated condition is: " + " [" +  " current" + " < " + minValue + " && " + "current" + " > " + maxValue + " ] " + "\n");
                    System.out.println("buffer time entered as: " + " [" + bufferTime + "] " +"\n");
                    System.out.println("-------------------------\n");
                    break;

                case "BOTH":
                    System.out.print("User Generated condition is: " + " [" + " current < minValue && current > maxValue" + " ] " + "\n");
                    System.out.print("User Generated condition is: " + " [" +  " current" + " < " + minValue + " && " + "current" + " > " + maxValue + " ] " + "\n");
                    System.out.println("buffer time entered as: " + " [" + bufferTime + "] " +"\n");
                    System.out.println("-------------------------\n");
                    break;

                case "NONE":
                    System.out.print("User Generated condition is: " + " [" + " current > maxValue" + " ] " + "\n");
                    System.out.print("User Generated condition is: " + " [" +  " current" + " > " + maxValue + " ] " + "\n");
                    System.out.println("buffer time entered as: " + " [" + bufferTime + "] " +"\n");
                    System.out.println("-------------------------\n");
                    break;
                case "Custome":
                    System.out.print("Write your condition is: " + "\n");
                    System.out.println("-------------------------\n");
                    break;

                default:
                    System.out.println("Invalid operator!");
                    break;
            }

    scanner.close();

    }
}

