package org.ngafid.flights.datcon.DatConRecs.Created4V1;

import org.ngafid.flights.datcon.DatConRecs.GpsGroup;
import org.ngafid.flights.datcon.DatConRecs.Payload;
import org.ngafid.Files.datcon.DatConRecs.ConvertDat;
import org.ngafid.flights.datcon.DatConRecs.Payload;

public class GPS_GLNS68_5 extends GpsGroup {

    public GPS_GLNS68_5(ConvertDat convertDat) {
        super(convertDat, 0, 5, 68);
    }

    @Override
    public void process(Payload _payload) {
        super.process(_payload);

    }

}
