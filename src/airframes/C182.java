package src.airframes;

import java.util.ArrayList;

import src.EventTracker;

import src.events.Event;

public class C182 extends Airframe {
    public C182(String flightFilename) {
        super(flightFilename);
    }

    public ArrayList<Event> getEvents() {
        EventTracker c182EventTracker = new EventTracker(new String[]{
                "src.events.PitchEvent",
                "src.events.RollEvent",
                "src.events.VerticalAccelerationEvent",
                "src.events.LateralAccelerationEvent"//,
                // "src.events.C182IndicatedAirspeedEvent",
                // "src.events.C182HighCHTEvent",
                // "src.events.C182HighAltitudeEvent",
                // "src.events.C182LowFuelEvent",
                // "src.events.C182LowOilPressureEvent",
                // "src.events.C182LowAirspeedOnApproachEvent",
                // "src.events.C182LowAirspeedOnClimboutEvent"
            });

        ArrayList<Event> events = c182EventTracker.getEvents(csvValues);

        return events;
    }
}
