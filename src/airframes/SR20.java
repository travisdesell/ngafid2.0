package src.airframes;

import java.util.ArrayList;

import src.EventTracker;

import src.events.Event;

public class SR20 extends Airframe {
    public SR20(String flightFilename) {
        super(flightFilename);
    }

    public ArrayList<Event> getEvents() {
        EventTracker sr20EventTracker = new EventTracker(new String[]{
            "src.events.PitchEvent",
                "src.events.RollEvent",
                "src.events.VerticalAccelerationEvent",
                "src.events.LateralAccelerationEvent",
                "src.events.LongitudinalAccelerationEvent",
                "src.events.IndicatedAirspeedEvent",
                "src.events.SR20IndicatedAirspeedEvent",
                "src.events.SR20HighCHTEvent",
                "src.events.SR20HighAltitudeEvent",
                "src.events.SR20LowFuelEvent",
                "src.events.SR20LowOilPressureEvent",
                "src.events.SR20LowAirspeedOnApproachEvent",
                "src.events.SR20LowAirspeedOnClimboutEvent"

        });

        ArrayList<Event> events = sr20EventTracker.getEvents(csvValues);

        return events;
    }
}
