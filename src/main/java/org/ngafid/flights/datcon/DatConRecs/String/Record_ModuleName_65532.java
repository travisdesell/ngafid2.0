
package org.ngafid.flights.datcon.DatConRecs.String;

import org.ngafid.flights.datcon.DatConRecs.Payload;
import org.ngafid.flights.datcon.DatConRecs.Record;
import org.ngafid.flights.datcon.Files.ConvertDat;
import org.ngafid.flights.datcon.Files.ConvertDat.lineType;
import org.ngafid.flights.datcon.Files.DatConLog;
import org.ngafid.flights.datcon.Files.Persist;

public class Record_ModuleName_65532 extends Record {

    public static Record_ModuleName_65532 current = null;

    String payloadString = "";

    public Record_ModuleName_65532(ConvertDat convertDat) {
        super(convertDat, 65532, -1);
    }

    @Override
    public void process(Payload _payload) {
        super.process(_payload);
        try{
        if (Persist.EXPERIMENTAL_DEV) {
            payloadString = _payload.getString();
            System.out.println("Record_ModuleName_65532 " + payloadString);
        }
        } catch (Exception e) {
            RecordException(e);
        }
    }

    @Override
    public void printCols(lineType lineT) {
        try {

        } catch (Exception e) {
            DatConLog.Exception(e);
        }
    }

}
