
package org.ngafid.flights.datcon.DatConRecs.Created4V3;

import org.ngafid.flights.datcon.DatConRecs.GpsGroup;
import org.ngafid.flights.datcon.DatConRecs.Payload;
import org.ngafid.flights.datcon.Files.ConvertDat;

public class GPS_2098 extends GpsGroup {

    public GPS_2098(ConvertDat convertDat) {
        super(convertDat, 2, 2098, 66);
    }

    //    public RecSpec.RecType getRecType() {
    //        return RecSpec.RecType.BINARY;
    //    }

    public void process(Payload _payload) {
        super.process(_payload);
    }
}
