package org.ngafid.flights;

import static org.ngafid.flights.CalculationParameters.*;

import java.util.Map;

import org.ngafid.Database;

public class VSPDCalculation extends Calculation {
    private final int DIFF = 1;
    
    static final double FPM_CONV = 60.f;

    public VSPDCalculation(Flight flight, Map<String, DoubleTimeSeries> parameters) {
        super(flight, vsiParamStrings, parameters);
    }

    /**
     * This is a linear regression calculation to get a more instantaneous VSI
     */
    public void calculate() {
        System.out.println("Calcuating VSPD from MSL data");

        DoubleTimeSeries msl = this.parameters.get(ALT_MSL);
        DoubleTimeSeries mslLead = msl.lead(DIFF);
        this.parameters.put(mslLead.getName(), mslLead);

        DoubleTimeSeries vsiCalculated = new DoubleTimeSeries(VSPD_CALCULATED, "double");
        
        //for (int i = 0; i < DIFF; i++) vsiCalculated.add(Double.NaN);

        for (int i = DIFF; i < msl.size(); i++) {
            //double m = msl.get(i) - mslLead.get(i);
            double m = mslLead.get(i) - msl.get(i);
            double vsi = m / DIFF;

            vsi *= FPM_CONV;

            vsiCalculated.add(vsi);
        }

        vsiCalculated.add(Double.NaN);

        this.parameters.put(VSPD_CALCULATED, vsiCalculated);
    }

    public void updateDatabase() {
        DoubleTimeSeries vsi = this.parameters.get(VSPD_CALCULATED);
        vsi.updateDatabase(Database.getConnection(), this.flight.getId());
    }
}
