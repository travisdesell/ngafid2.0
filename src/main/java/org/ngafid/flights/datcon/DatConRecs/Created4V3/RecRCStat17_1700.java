package org.ngafid.flights.datcon.DatConRecs.Created4V3;

import org.ngafid.flights.datcon.DatConRecs.Created4V3.RCStatus;
import org.ngafid.flights.datcon.DatConRecs.Payload;
import org.ngafid.flights.datcon.Files.ConvertDat;
import org.ngafid.flights.datcon.Files.ConvertDat.lineType;
import org.ngafid.flights.datcon.Files.DatConLog;

public class RecRCStat17_1700 extends RCStatus {
    protected boolean valid = false;

    public RecRCStat17_1700(ConvertDat convertDat) {
        super(convertDat, 1700, 17);
    }

    public RecType getRecType() {
        return RecType.BINARY;
    }

    @Override
    public void process(Payload _payload) {
        super.process(_payload);
        try {
            statusValid = true;
            //cur_cmd = _payload.getUnsignedShort(0);
            fail_safe = _payload.getUnsignedByte(2);
            //vedio_lost = _payload.getUnsignedByte(3);
            data_lost = ((payloadBB.get(4) == 1) ? "lost" : "");
            app_lost = ((payloadBB.get(5) == 1) ? "lost" : "");
            frame_lost = payloadBB.get(6);
            rec_cnt = ((long) payloadBB.getInt(7) & 0xffffffffL);
            connected = ((payloadBB.get(13) == 1) ? "Connected"
                    : "Disconnected");
            super.common();
        } catch (Exception e) {
            RecordException(e);
        }
    }

    @Override
    public void printCols(lineType lineT) {
        super.printCols(lineT);
        try {
            printCsvValue(connected, RCStateSig, "connected", lineT,
                    statusValid);
        } catch (Exception e) {
            DatConLog.Exception(e);
        }
    }

}