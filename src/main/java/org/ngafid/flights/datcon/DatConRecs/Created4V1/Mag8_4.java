package org.ngafid.flights.datcon.DatConRecs.Created4V1;

import org.ngafid.flights.datcon.DatConRecs.Created4V3.MagRawGroup;
import org.ngafid.flights.datcon.DatConRecs.Payload;
import org.ngafid.flights.datcon.Files.ConvertDat;
import org.ngafid.flights.datcon.DatConRecs.Created4V3.MagRawGroup;
import org.ngafid.flights.datcon.DatConRecs.Payload;

public class Mag8_4 extends MagRawGroup {

    public Mag8_4(ConvertDat convertDat) {
        super(convertDat, 4, 8, 0);
    }

    public void process(Payload _payload) {
        super.process(_payload);
    }
}