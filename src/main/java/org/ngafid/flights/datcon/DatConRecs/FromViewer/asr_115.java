package org.ngafid.flights.datcon.DatConRecs.FromViewer;

import org.ngafid.flights.datcon.DatConRecs.Payload;
import org.ngafid.flights.datcon.DatConRecs.Record;
import org.ngafid.flights.datcon.Files.ConvertDat;
import org.ngafid.flights.datcon.Files.ConvertDat.lineType;
import org.ngafid.flights.datcon.Files.DatConLog;
import org.ngafid.flights.datcon.Files.Signal;
import org.ngafid.flights.datcon.Files.Units;


public class asr_115 extends Record {
protected boolean valid = false;

protected long lead = (long)0;

      public asr_115(ConvertDat convertDat) {
           super(convertDat, 115, 4);
       }

@Override
  public void process(Payload _payload) {
      super.process(_payload);
        try {
      valid = true;

 lead = _payload.getUnsignedInt(0);
} catch (Exception e) {RecordException(e);}}


    protected static Signal asrIntSig = Signal
.SeriesInt("asr", "", null, Units.noUnits);
    protected static Signal asrFloatSig = Signal
.SeriesFloat("asr", "", null, Units.noUnits);
    protected static Signal asrDoubleSig = Signal
.SeriesDouble("asr", "", null, Units.noUnits);

   public void printCols(lineType lineT) {
try {

 printCsvValue(lead, asrIntSig, "lead",lineT, valid);
 } catch (Exception e) {
DatConLog.Exception(e);
}
}

   }
