package org.ngafid.flights.datcon.DatConRecs.Created4V1;

importorg.ngafid.flights.datcon.DatConRecs.GoTxt50_12;
importorg.ngafid.flights.datcon.DatConRecs.Payload;
importorg.ngafid.flights.datcon.DatConRecs.RecIMU;
import org.ngafid.Files.datcon.DatConRecs.ConvertDat;
import org.ngafid.Files.datcon.DatConRecs.ConvertDat.lineType;
import org.ngafid.Files.datcon.DatConRecs.DatConLog;
import org.ngafid.Files.datcon.DatConRecs.Signal;
import org.ngafid.Files.datcon.DatConRecs.Units;

public class Record_1 extends RecIMU {
    public Record_1(ConvertDat convertDat) {
        super(convertDat, 1, 120, 0);
        current = this;
        heightSig = Signal.SeriesDouble("General", "Height", null,
                Units.meters);
    }

    public void process(Payload _payload) {
        super.process(_payload);
        imuTemp *= 100.0;
    }

    public void printCols(lineType lineT) {
        super.printCols(lineT);
        try {
            if (GoTxt50_12.current != null) {
                printCsvValue(GoTxt50_12.current.flightTime, flightTimeSig, "",
                        lineT, GoTxt50_12.current.valid);
            }

            if (GoTxt50_12.current != null) {
                printCsvValue(GoTxt50_12.current.gpsLevel, gpsHealthSig, "",
                        lineT, GoTxt50_12.current.valid);
            }

            if (GoTxt50_12.current != null) {
                printCsvValue(GoTxt50_12.current.vpsHeight, heightSig,
                        "vpsHeight", lineT,
                        GoTxt50_12.current.valid & GoTxt50_12.current.waveWork);
            }
            printCsvValue(convertDat.getRelativeHeight(), heightSig,
                    "relativeHeight", lineT, convertDat.isRelativeHeightOK());

            printCsvValue(convertDat.getAbsoluteHeight(), heightSig,
                    "absoluteHeight", lineT, convertDat.absoluteHeightValid);

        } catch (Exception e) {
            DatConLog.Exception(e);
        }
    }
}
