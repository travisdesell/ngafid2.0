package src.airframes;

import java.util.ArrayList;

import src.EventTracker;

import src.events.Event;

public class PA44 extends Airframe {
    public PA44(String flightFilename) {
        super(flightFilename);
    }

    public ArrayList<Event> getEvents() {
        EventTracker pa44EventTracker = new EventTracker(new String[]{
                "src.events.PitchEvent",
                "src.events.RollEvent",
                "src.events.VerticalAccelerationEvent",
                "src.events.LateralAccelerationEvent"//,
                // "src.events.PA44IndicatedAirspeedEvent",
                // "src.events.PA44HighCHTEvent",
                // "src.events.PA44HighAltitudeEvent",
                // "src.events.PA44LowFuelEvent",
                // "src.events.PA44LowOilPressureEvent",
                // "src.events.PA44LowAirspeedOnApproachEvent",
                // "src.events.PA44LowAirspeedOnClimboutEvent"
            });

        ArrayList<Event> events = pa44EventTracker.getEvents(csvValues);

        return events;
    }
}
