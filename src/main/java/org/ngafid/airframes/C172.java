package org.ngafid.airframes;

import java.util.ArrayList;

import org.ngafid.EventTracker;

import org.ngafid.events.Event;

//import org.ngafid.events.c172.C172IndicatedAirspeedEvent;
// import org.ngafid.events.c172.C172HighAltitudeEvent;
// import org.ngafid.events.c172.C172LowFuelEvent;
// import org.ngafid.events.c172.C172HighCHTEvent;
// import org.ngafid.events.c172.C172LowAirspeedOnApproachEvent;
// import org.ngafid.events.c172.C172LowAirspeedOnClimboutEvent;
// import org.ngafid.events.c172.C172LowOilPressureEvent;

public class C172 extends Airframe {
    public C172(String flightFilename) {
        super(flightFilename);
    }

    public ArrayList<Event> getEvents() {
        EventTracker c172EventTracker = new EventTracker(new String[]{
            "org.ngafid.events.PitchEvent",
                "org.ngafid.events.RollEvent",
                "org.ngafid.events.VerticalAccelerationEvent",
                "org.ngafid.events.LateralAccelerationEvent",
                "org.ngafid.events.LongitudinalAccelerationEvent",
                "org.ngafid.events.VsiEvent",
                "org.ngafid.events.c172.C172IndicatedAirspeedEvent",
                "org.ngafid.events.c172.C172HighCHTEvent",
                "org.ngafid.events.c172.C172HighAltitudeEvent",
                "org.ngafid.events.c172.C172LowFuelEvent",
                "org.ngafid.events.c172.C172LowOilPressureEvent",
                "org.ngafid.events.c172.C172LowAirspeedOnApproachEvent",
                "org.ngafid.events.c172.C172LowAirspeedOnClimboutEvent"

        });

        ArrayList<Event> events = c172EventTracker.getEvents(csvValues);

        return events;
    }
}
