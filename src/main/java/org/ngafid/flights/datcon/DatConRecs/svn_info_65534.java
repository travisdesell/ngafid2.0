package org.ngafid.flights.datcon.DatConRecs;

import DatConRecs.Payload;
import DatConRecs.Record;
import Files.ConvertDat;
import Files.ConvertDat.lineType;
import Files.DatConLog;

import java.nio.ByteBuffer;

public class svn_info_65534 extends Record {

    ByteBuffer payload = null;

    protected String payloadString;

    public svn_info_65534(ConvertDat convertDat) {
        super(convertDat, 65534, -1);
    }

    @Override
    public void process(Payload _payload) {
        super.process(_payload);
        try {
            payload = _payload.getBB();
            payloadString = _payload.getString();
            if (convertDat.cloPS != null) {
                if (payloadString.length() > 0) {
                    convertDat.cloPS.println(_payload.getCleanString());
                }
            }
        } catch (Exception e) {
            RecordException(e);
        }
    }

    public void printCols(lineType lineT) {
        try {
        } catch (Exception e) {
            DatConLog.Exception(e);
        }
    }

}
