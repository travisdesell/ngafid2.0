package org.ngafid.flights.datcon.DatConRecs.Created4V3;

import org.ngafid.flights.datcon.DatConRecs.AirCraftCondition;
import org.ngafid.flights.datcon.DatConRecs.Payload;
import org.ngafid.flights.datcon.Files.ConvertDat;

public class AirCraftCondition8_1001 extends AirCraftCondition {

    public AirCraftCondition8_1001(ConvertDat convertDat) {
        super(convertDat, 1001, 8);
    }

    @Override
    public void process(Payload _payload) {
        super.process(_payload);
        try {
            valid = true;
            intFlightState = _payload.getUnsignedByte(0);
            flightState = _payload.getUnsignedByte(1);
            lastFlightState = _payload.getUnsignedByte(2);
            nearGndState = _payload.getUnsignedByte(3);
            UP_state = _payload.getUnsignedByte(4);
            landState = _payload.getUnsignedByte(5);
            safe_fltr = _payload.getShort(6);
            nearGrnd = (nearGndState != 0) ? "True" : "False";
        } catch (Exception e) {
            RecordException(e);
        }
    }

}
