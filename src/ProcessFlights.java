package src;

import java.util.ArrayList;

import src.airframes.Airframe;
import src.airframes.C172;
import src.events.Event;

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


        Airframe airframe = new Airframe(arguments[0]);
        //C172 airframe = new C172(arguments[0]);
        //PA22 airframe = new PA22(arguments[0]);
        airframe.printInformation();
        airframe.printValues();

        ArrayList<Event> events = airframe.getEvents();

        System.out.println();
        System.out.println();
        System.out.println("ALL EVENTS:");
        for (int i = 0; i < events.size(); i++) {
            System.out.println( events.get(i).toString() );
        }

    }

}
