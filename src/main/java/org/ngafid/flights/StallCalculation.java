/**
 * Performs stall probability calculations for all GA aircraft in the NGAFID
 *
 * @author <a href = mailto:apl1341@cs.rit.edu>Aidan LaBella @ RIT Computer Science</a>
 */

package org.ngafid.flights;

import java.util.HashMap;
import java.util.Map;

import static org.ngafid.flights.CalculationParameters.*;

public class StallCalculation extends Calculation {

    public StallCalculation(Flight flight, Map<String, DoubleTimeSeries> parameters) {
        super(flight, spParamStrings, parameters);
        this.parameters.put(AOA_SIMPLE, new DoubleTimeSeries(AOA_SIMPLE, "double"));
        this.parameters.put(TAS_FTMIN, new DoubleTimeSeries(TAS_FTMIN, "double"));
    }


    /**
     * Calculates the simple angle of attack at a given index
     *
     * @param index the index of the {@link DoubleTimeSeries} to access
     *
     * @return a double with the calculated value at the given index
     */
    private double getAOASimple(int index) {
        DoubleTimeSeries pitch = this.parameters.get(PITCH);
        DoubleTimeSeries aoaSimp = this.parameters.get(AOA_SIMPLE);

        double value = pitch.get(index) - this.getFlightPathAngle(index);

        if (aoaSimp.size() == index) {
            aoaSimp.add(value);
        }

        return value;
    }

    /**
     * Calculates the flight path angle at a given index
     *
     * @param index the index of the {@link DoubleTimeSeries} to access
     *
     * @return a double with the calculated value at the given index
     */
    private double getFlightPathAngle(int index){
        double fltPthAngle = Math.asin(this.getVspdGeometric(index) / this.getTrueAirspeedFtMin(index));
        fltPthAngle = fltPthAngle * (180 / Math.PI);
        return fltPthAngle;
    }

    /**
     * Calculates the geometric vertical speed at a given index
     *
     * @param index the index of the {@link DoubleTimeSeries} to access
     *
     * @return a double with the calculated value at the given index
     */
    private double getVspdGeometric(int index){
        DoubleTimeSeries vSpd = this.parameters.get(VSPD_CALCULATED);
        //DoubleTimeSeries vSpd = this.parameters.get(VSPD);
        return vSpd.get(index) * Math.pow(this.getDensityRatio(index), -0.5);
    }

    /**
     * Calculates the true airspeed (in ft/min) at a given index
     *
     * @param index the index of the {@link DoubleTimeSeries} to access
     *
     * @return a double with the calculated value at the given index
     */
    private double getTrueAirspeedFtMin(int index){
        DoubleTimeSeries tasFtMin = this.parameters.get(TAS_FTMIN);

        double value = this.getTrueAirspeed(index) * ((double) 6076 / 60);

        if (tasFtMin.size() == index) {
            tasFtMin.add(value);
        }

        return value;
    }

    /**
     * Calculates the true airspeed at a given index
     *
     * @param index the index of the {@link DoubleTimeSeries} to access
     *
     * @return a double with the calculated value at the given index
     */
    private double getTrueAirspeed(int index){
        DoubleTimeSeries ias = this.parameters.get(IAS);
        return ias.get(index) * Math.pow(this.getDensityRatio(index), -0.5);
    }

    /**
     * Calculates the density ratio at a given index
     *
     * @param index the index of the {@link DoubleTimeSeries} to access
     *
     * @return a double with the calculated value at the given index
     */
    private double getDensityRatio(int index){
        return this.getPressureRatio(index) / this.getTempRatio(index);
    }

    /**
     * Calculates the temperature ratio at a given index
     *
     * @param index the index of the {@link DoubleTimeSeries} to access
     *
     * @return a double with the calculated value at the given index
     */
    private double getTempRatio(int index){
        DoubleTimeSeries oat = this.parameters.get(OAT);
        return (273 + oat.get(index)) / 288;
    }

    /**
     * Calculates the pressure ratio at a given index
     *
     * @param index the index of the {@link DoubleTimeSeries} to access
     *
     * @return a double with the calculated value at the given index
     */
    private double getPressureRatio(int index){
        DoubleTimeSeries baroA = this.parameters.get(BARO_A);
        return baroA.get(index) / STD_PRESS_INHG;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateDatabase() {
        this.parameters.get(STALL_PROB).updateDatabase(connection, super.flight.getId());
        this.parameters.get(AOA_SIMPLE).updateDatabase(connection, super.flight.getId());
        this.parameters.get(TAS_FTMIN).updateDatabase(connection, super.flight.getId());
    }

    /**
     * {@inheritDoc}
     */
    public String getCalculationName() {
        return STALL_PROB;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void calculate() {
        DoubleTimeSeries altAGL = this.parameters.get(ALT_AGL);
        DoubleTimeSeries stallProbability; 
        this.parameters.put(STALL_PROB, 
                (stallProbability = new DoubleTimeSeries(STALL_PROB, "double")));

        for (int i = 0; i < altAGL.size(); i++) {
            //invoke the equation "tree"
            double prob = Math.min(((Math.abs(this.getAOASimple(i) / AOA_CRIT)) * 100), 100);
            stallProbability.add(prob / 100);
        }
    }
}
