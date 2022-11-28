package org.ngafid.flights.datcon.DatConRecs.Created4V1;

importorg.ngafid.flights.datcon.DatConRecs.Created4V3.IMUEX40;
importorg.ngafid.flights.datcon.DatConRecs.Payload;
import org.ngafid.Files.datcon.DatConRecs.ConvertDat;

public class IMUEX42_3 extends IMUEX40 {

    public IMUEX42_3(ConvertDat convertDat) {
        super(convertDat, 3, 42, 0);
    }

    @Override
    public void process(Payload _payload) {
        super.process(_payload);
    }

}
