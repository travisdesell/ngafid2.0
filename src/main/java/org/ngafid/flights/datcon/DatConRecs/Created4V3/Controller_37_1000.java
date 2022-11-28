package org.ngafid.flights.datcon.DatConRecs.Created4V3;

import org.ngafid.flights.datcon.DatConRecs.Created4V3.RecController;
import org.ngafid.flights.datcon.DatConRecs.Payload;
import org.ngafid.flights.datcon.Files.ConvertDat;

public class Controller_37_1000 extends RecController {
    public Controller_37_1000(ConvertDat convertDat) {
        super(convertDat, 1000, 37);
    }

    public void process(Payload _payload) {
        super.process(_payload);
        ctrl_pitch = (short) payloadBB.getShort(4);
        ctrl_roll = (short) payloadBB.getShort(6);
        ctrl_yaw = (short) payloadBB.getShort(8);
        ctrl_thr = (short) payloadBB.getShort(10);
        sig_level = _payload.getUnsignedByte(15);
        ctrl_level = _payload.getUnsignedByte(16);
        valid = true;
    }
}
