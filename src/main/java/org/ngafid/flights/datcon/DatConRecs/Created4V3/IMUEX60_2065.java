package org.ngafid.flights.datcon.DatConRecs.Created4V3;

import DatConRecs.Created4V3.IMUEX60;
import DatConRecs.Payload;
import Files.ConvertDat;

public class IMUEX60_2065 extends IMUEX60 {

    public IMUEX60_2065(ConvertDat convertDat) {
        super(convertDat, 2065, 60, 1);
    }

    @Override
    public void process(Payload _payload) {
        super.process(_payload);
    }

}
