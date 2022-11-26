package org.ngafid.flights.datcon.DatConRecs.FromViewer;

import DatConRecs.Payload;
import DatConRecs.Record;
import Files.ConvertDat;
import Files.ConvertDat.lineType;
import Files.DatConLog;
import Files.Signal;
import Files.Units;


public class sweep_52722 extends Record {
protected boolean valid = false;

protected int ppm = (int)0;

      public sweep_52722(ConvertDat convertDat) {
           super(convertDat, 52722, 2);
       }

@Override
  public void process(Payload _payload) {
      super.process(_payload);
        try {
      valid = true;

 ppm = _payload.getUnsignedShort(0);
} catch (Exception e) {RecordException(e);}}


    protected static Signal sweepIntSig = Signal
.SeriesInt("sweep", "", null, Units.noUnits);
    protected static Signal sweepFloatSig = Signal
.SeriesFloat("sweep", "", null, Units.noUnits);
    protected static Signal sweepDoubleSig = Signal
.SeriesDouble("sweep", "", null, Units.noUnits);

   public void printCols(lineType lineT) {
try {

 printCsvValue(ppm, sweepIntSig, "ppm",lineT, valid);
 } catch (Exception e) {
DatConLog.Exception(e);
}
}

   }
