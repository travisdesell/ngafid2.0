package org.ngafid;

import java.util.HashMap;
import java.util.ArrayList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.ngafid.events.Event;

public class EventTracker {

    String[] eventClassNames;

    HashMap<String, Constructor> eventConstructors = new HashMap<String, Constructor>();
    HashMap<String, Method> isOccuringMethods = new HashMap<String, Method>();
    HashMap<String, Event> currentEvents = new HashMap<String, Event>();


    public EventTracker(String[] eventClassNames) {
        this.eventClassNames = eventClassNames;

        for (int i = 0; i < eventClassNames.length; i++) {
            try {
                eventConstructors.put(eventClassNames[i], Class.forName(eventClassNames[i]).getConstructor(String.class, String.class, int.class, int.class));
                isOccuringMethods.put(eventClassNames[i], Class.forName(eventClassNames[i]).getMethod("isOccuring", ArrayList.class));
                currentEvents.put(eventClassNames[i], null);

            } catch (ClassNotFoundException cnfe) {
                cnfe.printStackTrace();
                System.err.println("Class '" + eventClassNames[i] + "' was not found!");
                System.exit(1);
            } catch (NoSuchMethodException nsme) {
                nsme.printStackTrace();
                System.err.println("Method 'isOccuring' was not found!");
                System.exit(1);
            }
        }
    }

    public ArrayList<Event> getEvents(ArrayList<ArrayList<String>> csvValues) {
        ArrayList<Event> events = new ArrayList<Event>();

        int timeColumn = 1;

        for (int line = 0; line < csvValues.size(); line++) {
            ArrayList<String> lineValues = csvValues.get(line);

            String time = lineValues.get(timeColumn);

            for (int i = 0; i < eventClassNames.length; i++) {
                Constructor eventConstructor = eventConstructors.get(eventClassNames[i]);
                Method eventMethod = isOccuringMethods.get(eventClassNames[i]);
                Event currentEvent = currentEvents.get(eventClassNames[i]);

                try {
                    Object result = eventMethod.invoke(null, lineValues);

                    //System.out.println("Result for checkingEventOccurences: on class '" + eventClassNames[i] + "': " + result);

                    if (((Boolean)result).booleanValue() == true) {
                        //this particular event occured on this line

                        if (currentEvent == null) {
                            currentEvent = (Event)eventConstructor.newInstance(time, time, line, line);
                            currentEvents.put(eventClassNames[i], currentEvent);
                            System.out.println("CREATED NEW      " + currentEvent);

                        } else {
                            currentEvent.updateEnd(time, line);
                            //System.out.println("UPDATED END TIME " + currentEvent);
                        }
                    } else {
                        if (currentEvent != null) {
                            if (currentEvent.isFinished(line, lineValues)) {
                                //we're done with this event
                                events.add(currentEvent);
                                System.out.println("FINISHED         " + currentEvent);

                                //pitchEvent = null;
                                currentEvents.put(eventClassNames[i], null);
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }

        return events;
    }

}
