package org.ngafid.flights.dji;

import java.nio.ByteBuffer;

public class svnInfo65534 extends DATRecord {

    ByteBuffer payload = null;

    protected String payloadString;

    public svnInfo65534(DATConvert datConvert) {
        super(datConvert, 65534, -1);
    }

    @Override
    public void process(Payload payload) {
        super.process(payload);
        try {
            this.payload = payload.getByteBuffer();
            payloadString = payload.getString();
            if (convertDat.cloPS != null && payloadString.length() > 0) {
                convertDat.cloPS.println(payload.getCleanString());
            }
        } catch (Exception e) {
            RecordException(e);
        }
    }
}
