package src.airframes;

import java.util.ArrayList;

import src.EventTracker;

import src.events.Event;

public class C172 extends Airframe {
    public C172(String flightFilename) {
        super(flightFilename);
    }

    public ArrayList<Event> getEvents() {
        EventTracker c172EventTracker = new EventTracker(new String[]{
                "src.events.PitchEvent",
                "src.events.RollEvent",
                "src.events.VerticalAccelerationEvent",
                "src.events.LateralAccelerationEvent"//,
                //"src.events.C172IndicatedAirspeedEvent",
                //"src.events.C172HighCHTEvent",
                //"src.events.C172HighAltitudeEvent",
                //"src.events.C172LowFuelEvent",
               // "src.events.C172LowOilPressureEvent",
               // "src.events.C172LowAirspeedOnApproachEvent",
               // "src.events.C172LowAirspeedOnClimboutEvent"
            });

        ArrayList<Event> events = c172EventTracker.getEvents(csvValues);

        return events;
    }
}
