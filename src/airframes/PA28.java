package src.airframes;

import java.util.ArrayList;

import src.EventTracker;

import src.events.Event;

public class PA28 extends Airframe {
    public PA28 (String flightFilename) {
        super(flightFilename);
    }

    public ArrayList<Event> getEvents() {
        EventTracker pa28EventTracker = new EventTracker(new String[]{
            "src.events.PitchEvent",
                "src.events.RollEvent",
                "src.events.VerticalAccelerationEvent",
                "src.events.LateralAccelerationEvent",
                "src.events.LongitudinalAccelerationEvent",
                "src.events.IndicatedAirspeedEvent",
                "src.events.PA28IndicatedAirspeedEvent",
                "src.events.PA28HighCHTEvent",
                "src.events.PA28HighAltitudeEvent",
                "src.events.PA28LowFuelEvent",
                "src.events.PA28LowOilPressureEvent",
                "src.events.PA28LowAirspeedOnApproachEvent",
                "src.events.PA28LowAirspeedOnClimboutEvent"

        });

        ArrayList<Event> events = pa28EventTracker.getEvents(csvValues);

        return events;
    }
}
