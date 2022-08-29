package org.ngafid.flights.dji;

import java.util.logging.Logger;

public class RecBatt extends DATRecord {
    public static final Logger LOG = Logger.getLogger(RecBatt.class.getName());

    public float current = (float) 0.0;

    protected short batteryPercent = 0;

    protected float fcc = 0.0f;

    protected float remcap = 0.0f;

    public float volt[];

    protected int numCells = 0;

    public float temp = (float) 0.0;
    ;

    public float totalVolts = (float) 0.0;

    public float maxVolts = (float) 0.0;

    public float minVolts = (float) 0.0;

    public float sumOfVolts = (float) 0.0;

    public float avgVolts = (float) 0.0;

    protected long sumOfCurrents = 0;

    protected long numSamples = 0;

    public float voltDiff = (float) 0.0;

    public float maxCurrent = (float) 0.0;

    public float minCurrent = (float) 0.0;

    public float avgCurrent = (float) 0.0;

    public float watts = (float) 0.0;

    public float maxWatts = (float) 0.0;

    public float minWatts = (float) 0.0;

    protected float sumOfWatts = (float) 0.0;

    public float avgWatts = (float) 0.0;

    public boolean valid = false;

    protected DATSignal statusSig;

    protected DATSignal battPercent;

    protected DATSignal currentSig;

    protected DATSignal cellVoltSig;

    protected DATSignal batteryTempSig;

    protected DATSignal batteryFCC;

    protected DATSignal batteryRemCap;

    protected DATSignal voltsSig;

    protected DATSignal wattsSig;

    public RecBatt(DATConvert convertDat, int id, int length, int index) {
        super(convertDat, id, length);
        numCells = convertDat.getDatFile().getNumBatteryCells();
        volt = new float[numCells];
        for (int i = 0; i < numCells; i++) {
            volt[i] = 0.0f;
        }
        statusSig = DATSignal.SeriesIntExperimental("Battery", index, "Battery Status", null, Units.noUnits);
        battPercent = DATSignal.SeriesInt("Battery", index, "Battery Percentage", null, Units.percentage);

        currentSig = DATSignal.SeriesFloat("Battery", index, "Current", null, Units.amps);

        cellVoltSig = DATSignal.SeriesFloat("Battery", index, "Cell Volts", new Axis("cellVolts", "Cell Volts",
                Units.volts), Units.volts);

        batteryTempSig = DATSignal.SeriesFloat("Battery", index, "Battery Temp", null, Units.degreesC);

        batteryFCC = DATSignal.SeriesFloat("Battery", index, "Battery Full Charge Capacity", null, Units.mAh);

        batteryRemCap = DATSignal.SeriesFloat("Battery", index, "Battery Remaining Cap", null, Units.mAh);

        voltsSig = DATSignal.SeriesFloat("Battery", index, "Volts", null, Units.volts);

        wattsSig = DATSignal.SeriesFloat("Battery", index, "Watts", null, Units.watts);

    }

    protected void init() {
        maxVolts = (float) -1.0;
        minVolts = Float.MAX_VALUE;
        minCurrent = Float.MAX_VALUE;
        avgCurrent = (float) 0.0;
        maxWatts = (float) -1.0;
        minWatts = Float.MAX_VALUE;
    }

    protected float maxVolt(float... floatVolts) {
        float retv = -Float.MAX_VALUE;
        for (float volts : floatVolts) {
            if (volts > retv) {
                retv = volts;
            }
        }
        return retv;
    }

    protected float minVolt(float... floatVolts) {
        float retv = Float.MAX_VALUE;
        for (float volts : floatVolts) {
            if (volts < retv) {
                retv = volts;
            }
        }
        return retv;
    }

    protected float minVolts(float[] volts) {
        float min = Float.MAX_VALUE;
        for (int i = 0; i < volts.length; i++) {
            if (volts[i] < min) min = volts[i];
        }
        return min;
    }

    protected float maxVolts(float[] volts) {
        float max = Float.MIN_VALUE;
        for (int i = 0; i < volts.length; i++) {
            if (volts[i] > max) max = volts[i];
        }
        return max;
    }

    protected void processComputedBatt() {
        if (totalVolts > maxVolts) maxVolts = totalVolts;
        if (totalVolts < minVolts) minVolts = totalVolts;
        sumOfVolts += totalVolts;
        avgVolts = sumOfVolts / (float) numSamples;

        if (current > maxCurrent) maxCurrent = current;
        if (current < minCurrent) minCurrent = current;
        sumOfCurrents += current;
        avgCurrent = sumOfCurrents / (float) numSamples;

        watts = totalVolts * current;
        if (watts > maxWatts) maxWatts = watts;
        if (watts < minWatts) minWatts = watts;
        sumOfWatts += watts;
        avgWatts = sumOfWatts / (float) numSamples;
    }

    protected void printComputedBatteryCols(DATConvert.lineType lineT) throws Exception {
        printCSVValue(voltDiff, voltsSig, "voltSpread", lineT, valid);
        printCSVValue(watts, wattsSig, "watts", lineT, valid);
        printCSVValue(minCurrent, currentSig, "minCurrent", lineT, valid);
        printCSVValue(maxCurrent, currentSig, "maxCurrent", lineT, valid);
        printCSVValue(avgCurrent, currentSig, "avgCurrent", lineT, valid);

        printCSVValue(minVolts, voltsSig, "minVolts", lineT, valid);
        printCSVValue(maxVolts, voltsSig, "maxVolts", lineT, valid);
        printCSVValue(avgVolts, voltsSig, "avgVolts", lineT, valid);

        printCSVValue(minWatts, wattsSig, "minWatts", lineT, valid);
        printCSVValue(maxWatts, wattsSig, "maxWatts", lineT, valid);
        printCSVValue(avgWatts, wattsSig, "avgWatts", lineT, valid);
    }

    @Override
    public void printCols(DATConvert.lineType lineT) {
        try {
            for (int i = 1; i <= datFile.getNumBatteryCells(); i++) {
                printCSVValue(volt[i - 1], cellVoltSig, "cellVolts" + i, lineT, valid);
            }
            printCSVValue(current, currentSig, "current", lineT, valid);
            printCSVValue(totalVolts, voltsSig, "totalVolts", lineT, valid);
            printCSVValue(temp, batteryTempSig, "Temp", lineT, valid);
            printCSVValue(batteryPercent, battPercent, "battery%", lineT, valid);
            printCSVValue(fcc, batteryFCC, "FullChargeCap", lineT, valid);
            printCSVValue(remcap, batteryRemCap, "RemainingCap", lineT, valid);
            printComputedBatteryCols(lineT);

        } catch (Exception e) {
            LOG.warning(e.toString());
        }
    }

}
