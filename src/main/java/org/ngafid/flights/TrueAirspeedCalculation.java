package org.ngafid.flights;

import java.sql.SQLException;
import java.util.*;

import org.ngafid.Database;

import static org.ngafid.flights.CalculationParameters.*;

public class TrueAirspeedCalculation extends Calculation {
    public TrueAirspeedCalculation(Flight flight, Map<String, DoubleTimeSeries> parameters) {
        super(flight, tascParamStrings, parameters);
    }

    public void calculate(DoubleTimeSeries doubleSeries) {
        System.out.println("Calculating TAS with " + this.parameters.toString());

        DoubleTimeSeries gndSpd = this.parameters.get(GND_SPD);
        DoubleTimeSeries ias = this.parameters.get(IAS);
        DoubleTimeSeries wndSpd = this.parameters.get(WIND_SPEED);
        DoubleTimeSeries wndDir = this.parameters.get(WIND_DIRECTION);

        for (int i = 0; i < ias.size(); i++) {
            double wndDirY = wndDir.get(i);
            double angRad = Math.cos((wndDirY * 180) / Math.PI);
            double windY = wndSpd.get(i) * angRad;
            System.out.println(i + "\t" + (ias.get(i) - gndSpd.get(i)) + "\t" + windY + "@" + wndDirY);
        }

        System.out.println("calculation complete");
        
    }

    public String getCalculationName() {
        return TASC;
    }

    /**
     * For experimental purpoeses
     */
    public static void main(String [] args) {
        int flightId = Integer.parseInt(args[0]);

        System.err.println("Performing a TAS calculation on flight #" + flightId);

        Map<String, DoubleTimeSeries> params = new HashMap<>();

		try {
			Calculation tasc = new TrueAirspeedCalculation(Flight.getFlight(Database.getConnection(), flightId), params);
            CalculatedDoubleTimeSeries tas = new CalculatedDoubleTimeSeries(tasc, tascDeps);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        System.exit(0);

    }

}
