package org.ngafid.flights.dji;

public class RecBatt45_17 extends RecBatt {

    public RecBatt45_17(DATConvert datConvert) {
        super(datConvert, 17, 45, 0);
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


    public float ratedCapacity = (float) 0.0;

    public void process(Payload payload) {
        super.process(payload);
        if (numSamples == 0) { // first time
            init();
        }
        valid = true;
        numSamples++;
        fcc = payload.getUnsignedShort(0);
        ratedCapacity = payloadBB.getShort(2);
        remcap = payloadBB.getShort(4);
        totalVolts = (float) (((float) (payloadBB.getShort(6))) / 1000.0);
        current = -(float) (((float) (payload.getUnsignedShort(8) - 65536))
                / 1000.0);
        batteryPercent = payloadBB.get(11);
        temp = payloadBB.get(12);
        for (int i = 0; i < numCells; i++) {
            volt[i] = (float) (((float) (payloadBB.getShort(18 + (2 * i))))
                    / 1000.0);
        }
        float voltMax = maxVolt(volt);
        float voltMin = minVolt(volt);
        voltDiff = voltMax - voltMin;
        processComputedBatt();
    }
}
