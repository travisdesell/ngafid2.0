package org.ngafid.flights;

import static org.ngafid.flights.CalculationParameters.*;

import java.util.Map;

import org.ngafid.Database;

public class VSPDCalculation extends Calculation {
    private final int LAG = 1;
    
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
        DoubleTimeSeries mslLagged = msl.lag(LAG);
        this.parameters.put(mslLagged.getName(), mslLagged);

        DoubleTimeSeries vsiCalculated = new DoubleTimeSeries(VSPD_CALCULATED, "double");
        
        for (int i = 0; i < LAG; i++) vsiCalculated.add(Double.NaN);

        for (int i = LAG; i < msl.size(); i++) {
            double m = msl.get(i) - mslLagged.get(i);
            double vsi = m / LAG;

            vsi *= FPM_CONV;

            vsiCalculated.add(vsi);
        }

        this.parameters.put(VSPD_CALCULATED, vsiCalculated);
    }

    public void updateDatabase() {
        DoubleTimeSeries vsi = this.parameters.get(VSPD_CALCULATED);
        vsi.updateDatabase(Database.getConnection(), this.flight.getId());
    }
}
