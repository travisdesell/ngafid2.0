package org.ngafid.airframes;

import java.util.ArrayList;

import org.ngafid.EventTracker;

import org.ngafid.events.Event;

public class C182 extends Airframe {
    public C182(String flightFilename) {
        super(flightFilename);
    }

    public ArrayList<Event> getEvents() {
        EventTracker c182EventTracker = new EventTracker(new String[]{
            "org.ngafid.events.PitchEvent",
                "org.ngafid.events.RollEvent",
                "org.ngafid.events.VerticalAccelerationEvent",
                "org.ngafid.events.LateralAccelerationEvent",
                "org.ngafid.events.LongitudinalAccelerationEvent",
                "org.ngafid.events.VsiEvent",
                "org.ngafid.events.c182.C182IndicatedAirspeedEvent",
                "org.ngafid.events.c182.C182HighCHTEvent",
                "org.ngafid.events.c182.C182HighAltitudeEvent",
                "org.ngafid.events.c182.C182LowFuelEvent",
                "org.ngafid.events.c182.C182LowOilPressureEvent",
                "org.ngafid.events.c182.C182LowAirspeedOnApproachEvent",
                "org.ngafid.events.c182.C182LowAirspeedOnClimboutEvent"

        });

        ArrayList<Event> events = c182EventTracker.getEvents(csvValues);

        return events;
    }
}
