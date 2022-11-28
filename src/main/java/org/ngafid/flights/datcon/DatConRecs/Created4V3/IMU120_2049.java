package org.ngafid.flights.datcon.DatConRecs.Created4V3;

import org.ngafid.flights.datcon.DatConRecs.Payload;
import org.ngafid.flights.datcon.DatConRecs.RecIMU;
import org.ngafid.flights.datcon.Files.ConvertDat;

public class IMU120_2049 extends RecIMU {
    public IMU120_2049(ConvertDat convertDat) {
        super(convertDat, 2049, 120, 1);
        current = this;
    }

    public void process(Payload _payload) {
        super.process(_payload);
    }
}
