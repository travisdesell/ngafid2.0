package org.ngafid.flights.datcon.DatConRecs.FromViewer;

import org.ngafid.flights.datcon.DatConRecs.Payload;
import org.ngafid.flights.datcon.DatConRecs.Record;
import org.ngafid.flights.datcon.Files.ConvertDat;
import org.ngafid.flights.datcon.Files.ConvertDat.lineType;
import org.ngafid.flights.datcon.Files.DatConLog;
import org.ngafid.flights.datcon.Files.Persist;

public class console_52719 extends Record {
    String text = "";

    public console_52719(ConvertDat convertDat) {
        super(convertDat, 52719, -1);
    }

    @Override
    public void process(Payload _payload) {
        super.process(_payload);
        try {
            String payloadString = _payload.getString();
            if (Persist.EXPERIMENTAL_DEV) {
                System.out.println("console_52719 " + payloadString);
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
