package org.ngafid.flights.datcon.DatConRecs.Created4V3;

import org.ngafid.flights.datcon.DatConRecs.Created4V3.IMUEX40;
import org.ngafid.flights.datcon.DatConRecs.Payload;
import org.ngafid.flights.datcon.Files.ConvertDat;

public class IMUEX40_3 extends IMUEX40 {

    public IMUEX40_3(ConvertDat convertDat) {
        super(convertDat, 3, 40, 0);
    }

    @Override
    public void process(Payload _payload) {
        super.process(_payload);
    }

}
