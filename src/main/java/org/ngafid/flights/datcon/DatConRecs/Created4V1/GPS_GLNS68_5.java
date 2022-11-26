package org.ngafid.flights.datcon.DatConRecs.Created4V1;

import DatConRecs.GpsGroup;
import DatConRecs.Payload;
import Files.ConvertDat;

public class GPS_GLNS68_5 extends GpsGroup {

    public GPS_GLNS68_5(ConvertDat convertDat) {
        super(convertDat, 0, 5, 68);
    }

    @Override
    public void process(Payload _payload) {
        super.process(_payload);

    }

}
