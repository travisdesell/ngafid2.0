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
                "org.ngafid.events.VsiEvent",
                "org.ngafid.events.sr20.SR20IndicatedAirspeedEvent",
                "org.ngafid.events.sr20.SR20HighCHTEvent",
                "org.ngafid.events.sr20.SR20HighAltitudeEvent",
                "org.ngafid.events.sr20.SR20LowFuelEvent",
                "org.ngafid.events.sr20.SR20LowOilPressureEvent",
                "org.ngafid.events.sr20.SR20LowAirspeedOnApproachEvent",
                "org.ngafid.events.sr20.SR20LowAirspeedOnClimboutEvent"

        });

        ArrayList<Event> events = sr20EventTracker.getEvents(csvValues);

        return events;
    }
}
