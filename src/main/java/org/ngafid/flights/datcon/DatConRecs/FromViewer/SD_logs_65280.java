package org.ngafid.flights.datcon.DatConRecs.FromViewer;

import DatConRecs.Payload;
import DatConRecs.Record;
import Files.ConvertDat;
import Files.ConvertDat.lineType;
import Files.DatConLog;
public class SD_logs_65280 extends Record {
 String text = "";

 public SD_logs_65280(ConvertDat convertDat) {
  super(convertDat, 65280,-1);
}
@Override
  public void process(Payload _payload) {
      super.process(_payload);
        try {
} catch (Exception e) {RecordException(e);}}


   public void printCols(lineType lineT) {
try {

 } catch (Exception e) {
DatConLog.Exception(e);
}
}

   }
