package org.ngafid.airframes;

import java.util.ArrayList;

import org.ngafid.EventTracker;

import org.ngafid.events.Event;

public class SR20 extends Airframe {
    public SR20(String flightFilename) {
        super(flightFilename);
    }

    public ArrayList<Event> getEvents() {
        EventTracker sr20EventTracker = new EventTracker(new String[]{
            "org.ngafid.events.PitchEvent",
                "org.ngafid.events.RollEvent",
                "org.ngafid.events.VerticalAccelerationEvent",
                "org.ngafid.events.LateralAccelerationEvent",
                "org.ngafid.events.LongitudinalAccelerationEvent",
                "org.ngafid.events.IndicatedAirspeedEvent",
                "org.ngafid.events.SR20IndicatedAirspeedEvent",
                "org.ngafid.events.SR20HighCHTEvent",
                "org.ngafid.events.SR20HighAltitudeEvent",
                "org.ngafid.events.SR20LowFuelEvent",
                "org.ngafid.events.SR20LowOilPressureEvent",
                "org.ngafid.events.SR20LowAirspeedOnApproachEvent",
                "org.ngafid.events.SR20LowAirspeedOnClimboutEvent"

        });

        ArrayList<Event> events = sr20EventTracker.getEvents(csvValues);

        return events;
    }
}
