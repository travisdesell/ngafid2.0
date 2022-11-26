package org.ngafid.flights.datcon.DatConRecs.FromViewer;

import DatConRecs.Payload;
import DatConRecs.Record;
import Files.ConvertDat;
import Files.ConvertDat.lineType;
import Files.DatConLog;
import Files.Signal;
import Files.Units;


public class rc_delay_52735 extends Record {
protected boolean valid = false;

protected long dly_ns = (long)0;

      public rc_delay_52735(ConvertDat convertDat) {
           super(convertDat, 52735, 4);
       }

@Override
  public void process(Payload _payload) {
      super.process(_payload);
        try {
      valid = true;

 dly_ns = _payload.getUnsignedInt(0);
} catch (Exception e) {RecordException(e);}}


    protected static Signal rc_delayIntSig = Signal
.SeriesInt("rc_delay", "", null, Units.noUnits);
    protected static Signal rc_delayFloatSig = Signal
.SeriesFloat("rc_delay", "", null, Units.noUnits);
    protected static Signal rc_delayDoubleSig = Signal
.SeriesDouble("rc_delay", "", null, Units.noUnits);

   public void printCols(lineType lineT) {
try {

 printCsvValue(dly_ns, rc_delayIntSig, "dly_ns",lineT, valid);
 } catch (Exception e) {
DatConLog.Exception(e);
}
}

   }
