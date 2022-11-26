package org.ngafid.flights.datcon.DatConRecs.Created4V3;

import DatConRecs.MotorControl;
import DatConRecs.Payload;
import Files.ConvertDat;

public class MotorCtrl16_1307 extends MotorControl {

    public MotorCtrl16_1307(ConvertDat convertDat) {
        super(convertDat, 1307, 16);
    }

    @Override
    public void process(Payload _payload) {
        super.process(_payload);
        try {
            valid = true;
            pwm1 = ((float) _payload.getUnsignedShort(0)) / 100.0f;
            pwm2 = ((float) _payload.getUnsignedShort(2)) / 100.0f;
            pwm3 = ((float) _payload.getUnsignedShort(4)) / 100.0f;
            pwm4 = ((float) _payload.getUnsignedShort(6)) / 100.0f;
            pwm5 = ((float) _payload.getUnsignedShort(8)) / 100.0f;
            pwm6 = ((float) _payload.getUnsignedShort(10)) / 100.0f;
            pwm7 = ((float) _payload.getUnsignedShort(12)) / 100.0f;
            pwm8 = ((float) _payload.getUnsignedShort(14)) / 100.0f;
        } catch (Exception e) {
            RecordException(e);
        }
    }
}
