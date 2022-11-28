package org.ngafid.flights.datcon.DatConRecs.FromViewer;

import org.ngafid.flights.datcon.DatConRecs.Payload;
import org.ngafid.flights.datcon.DatConRecs.Record;
import org.ngafid.flights.datcon.Files.ConvertDat;
import org.ngafid.flights.datcon.Files.ConvertDat.lineType;
import org.ngafid.flights.datcon.Files.DatConLog;
import org.ngafid.flights.datcon.Files.Signal;
import org.ngafid.flights.datcon.Files.Units;


public class PWM_OUTPUT_20 extends Record {
protected boolean valid = false;

protected int M1 = (int)0;
protected int M2 = (int)0;
protected int M3 = (int)0;
protected int M4 = (int)0;
protected int M5 = (int)0;
protected int M6 = (int)0;
protected int M7 = (int)0;
protected int M8 = (int)0;

      public PWM_OUTPUT_20(ConvertDat convertDat) {
           super(convertDat, 20, 16);
       }

@Override
  public void process(Payload _payload) {
      super.process(_payload);
        try {
      valid = true;

 M1 = _payload.getUnsignedShort(0);
 M2 = _payload.getUnsignedShort(2);
 M3 = _payload.getUnsignedShort(4);
 M4 = _payload.getUnsignedShort(6);
 M5 = _payload.getUnsignedShort(8);
 M6 = _payload.getUnsignedShort(10);
 M7 = _payload.getUnsignedShort(12);
 M8 = _payload.getUnsignedShort(14);
} catch (Exception e) {RecordException(e);}}


    protected static Signal PWM_OUTPUTIntSig = Signal
.SeriesInt("PWM_OUTPUT", "", null, Units.noUnits);
    protected static Signal PWM_OUTPUTFloatSig = Signal
.SeriesFloat("PWM_OUTPUT", "", null, Units.noUnits);
    protected static Signal PWM_OUTPUTDoubleSig = Signal
.SeriesDouble("PWM_OUTPUT", "", null, Units.noUnits);

   public void printCols(lineType lineT) {
try {

 printCsvValue(M1, PWM_OUTPUTIntSig, "M1",lineT, valid);
 printCsvValue(M2, PWM_OUTPUTIntSig, "M2",lineT, valid);
 printCsvValue(M3, PWM_OUTPUTIntSig, "M3",lineT, valid);
 printCsvValue(M4, PWM_OUTPUTIntSig, "M4",lineT, valid);
 printCsvValue(M5, PWM_OUTPUTIntSig, "M5",lineT, valid);
 printCsvValue(M6, PWM_OUTPUTIntSig, "M6",lineT, valid);
 printCsvValue(M7, PWM_OUTPUTIntSig, "M7",lineT, valid);
 printCsvValue(M8, PWM_OUTPUTIntSig, "M8",lineT, valid);
 } catch (Exception e) {
DatConLog.Exception(e);
}
}

   }
