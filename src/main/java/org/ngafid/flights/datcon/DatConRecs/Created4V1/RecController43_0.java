
package org.ngafid.flights.datcon.DatConRecs.Created4V1;

import DatConRecs.Payload;
import DatConRecs.Record;
import Files.AxesAndSigs;
import Files.ConvertDat;
import Files.ConvertDat.lineType;
import Files.DatConLog;

public class RecController43_0 extends Record {
    
    public static RecController43_0 current = null;

    public short aileron = 0;

    public short elevator = 0;

    public short throttle = 0;

    public short rudder = 0;

    public boolean sticksValid = false;

    public RecController43_0(ConvertDat convertDat) {
        super(convertDat, 0, 43);
    }

    public void process(Payload _payload) {
        super.process(_payload);
        try {
            aileron = payloadBB.getShort(4);
            elevator = payloadBB.getShort(6);
            throttle = payloadBB.getShort(8);
            rudder = payloadBB.getShort(10);
            sticksValid = true;
        } catch (Exception e) {
            RecordException(e);
        }
    }

    @Override
    public void printCols(lineType lineT) {
        try {
            printCsvValue(aileron, AxesAndSigs.aileronSig, "", lineT,
                    sticksValid);
            printCsvValue(elevator, AxesAndSigs.elevatorSig, "", lineT,
                    sticksValid);
            printCsvValue(rudder, AxesAndSigs.rudderSig, "", lineT,
                    sticksValid);
            printCsvValue(throttle, AxesAndSigs.throttleSig, "", lineT,
                    sticksValid);
        } catch (Exception e) {
            DatConLog.Exception(e);
        }
    }
}
