package org.ngafid.flights.datcon.DatConRecs.Created4V1;

import DatConRecs.Payload;
import DatConRecs.Record;
import Files.ConvertDat;
import Files.ConvertDat.lineType;
import Files.DatConLog;
import Files.Signal;
import Files.Units;

// 15 HZ
public class RecTabletLoc179_43 extends Record {

    public static RecTabletLoc179_43 current = null;

    public double longRad = 0.0;

    public double latRad = 0.0;

    public double longitudeTablet = 0.0;

    public double latitudeTablet = 0.0;

    public boolean valid = false;

    public RecTabletLoc179_43(ConvertDat convertDat) {
        super(convertDat, 43, 179);
    }

    public void process(Payload _payload) {
        super.process(_payload);
        try {
            latRad = payloadBB.getDouble(155);
            longRad = payloadBB.getDouble(163);
            longitudeTablet = Math.toDegrees(longRad);
            latitudeTablet = Math.toDegrees(latRad);
            if (!valid) {
                if (longitudeTablet != 0.0 && latitudeTablet != 0.0) {
                    valid = true;
                }
            }
        } catch (Exception e) {
            RecordException(e);
        }
    }

    public final static Signal tabletGPSSig = Signal.SeriesDouble("Tablet",
            "Tablet", null, Units.degrees180);

    @Override
    public void printCols(lineType lineT) {
        try {
            printCsvValue(longitudeTablet, tabletGPSSig, "Longitude", lineT,
                    valid);
            printCsvValue(latitudeTablet, tabletGPSSig, "Latitude", lineT,
                    valid);
        } catch (Exception e) {
            DatConLog.Exception(e);
        }
    }
}
