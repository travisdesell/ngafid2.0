package org.ngafid.airframes;

import java.util.ArrayList;

import org.ngafid.EventTracker;

import org.ngafid.events.Event;

public class PA28 extends Airframe {
    public PA28 (String flightFilename) {
        super(flightFilename);
    }

    public ArrayList<Event> getEvents() {
        EventTracker pa28EventTracker = new EventTracker(new String[]{
            "org.ngafid.events.PitchEvent",
                "org.ngafid.events.RollEvent",
                "org.ngafid.events.VerticalAccelerationEvent",
                "org.ngafid.events.LateralAccelerationEvent",
                "org.ngafid.events.LongitudinalAccelerationEvent",
                "org.ngafid.events.VsiEvent",
                "org.ngafid.events.pa28.PA28IndicatedAirspeedEvent",
                "org.ngafid.events.pa28.PA28HighCHTEvent",
                "org.ngafid.events.pa28.PA28HighAltitudeEvent",
                "org.ngafid.events.pa28.PA28LowFuelEvent",
                "org.ngafid.events.pa28.PA28LowOilPressureEvent",
                "org.ngafid.events.pa28.PA28LowAirspeedOnApproachEvent",
                "org.ngafid.events.pa28.PA28LowAirspeedOnClimboutEvent"

        });

        ArrayList<Event> events = pa28EventTracker.getEvents(csvValues);

        return events;
    }
}
