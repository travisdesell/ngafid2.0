package org.ngafid.events;

import org.ngafid.flights.Flight;
import org.ngafid.flights.DoubleTimeSeries;
import static org.ngafid.flights.CalculationParameters.*;

import java.util.ArrayList;

/**
 * A Custom Exceedence for spin events.
 *
 * @author <a href = mailto:apl1341@cs.rit.edu>Aidan LaBella</a>
 */

public class SpinEvent extends Event {
    private SpinEvent(String startTime, String endTime, int startLine, int endLine, double severity) {
        super(startTime, endTime, startLine, endLine, severity);
        this.id = -2;
    }

    public static List<SpinEvent> find(Flight flight) {
        List<SpinEvent> spins = new ArrayList<>();

        DoubleTimeSeries ias = flight.getDoubleTimeSeries(IAS);
        DoubleTimeSeries vSpd = flight.getDoubleTimeSeries(VSPD);

        boolean airspeedIsLow = false;
        int lowAirspeedIndex = -1;

        for (int i = 0; i < flight.getNumberRows(); i++) {
            double instIAS = ias.get(i);
            double instVSI = vSpd.get(i);

            if (ias < 50) {
                airspeedIsLow = true;
                lowAirspeedIndex = i;
            }

            if (airspeedIsLow) {
                if (i - lowAirspeedIndex <= 3) {

                }
            }
        }

    }
} 
