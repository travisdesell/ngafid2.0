/* ConvertDat class

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that redistribution of source code include
the following disclaimer in the documentation and/or other materials provided
with the distribution.

THIS SOFTWARE IS PROVIDED BY ITS CREATOR "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE CREATOR OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.ngafid.flights.dji;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.logging.Logger;

public class DATConvert {
    public static final Logger LOG = Logger.getLogger(DATConvert.class.getName());

    public enum KmlType {
        NONE, GROUNDTRACK, PROFILE
    }

    public static TSAGeoMag geoMag = new TSAGeoMag();

    public DATDJIFile datFile = null;

    public long tickNo = 0;

    public long tickRangeLower = 0;

    public long tickRangeUpper = Long.MAX_VALUE;

    public float sampleRate = (float) 600.0;

    public long timeOffset = 0;

    public List<DATRecord> DATRecords = new ArrayList<DATRecord>();


    public KmlType kmlType = KmlType.NONE;

    public File kmlFile;

    public String kmlFileName;

    public double homePointElevation = Double.NaN;

    public boolean csvEventLogOutput = false;

    public PrintStream eloPS = null;

    public PrintStream cloPS = null;

    public PrintStream recDefsPS = null;

    public PrintStream kmlPS = null;

    public DAT2CSVWriter csvWriter = null;

    public PrintStream tloPS = null;

    protected boolean printVersion;

    public float absoluteHeight = 0.0f;

    public boolean absoluteHeightValid = false;

    private double takeOffAlt = Double.NaN;

    private double relativeHeight = 0.0;

    private boolean relativeHeightOK = false;
    List<AttrValuePair> attrValuePairs = new LinkedList<>();

    public DATConvert(DATDJIFile datFile) {
        this.datFile = datFile;
    }

    public DATConvert() {
    }

    public static enum lineType {
        HEADER, LINE, XML
    }

    double lastLatRad = 0.0;

    double lastLongRad = 0.0;

    long lastTickNo = 0;

    public boolean gpsCoordsOK = false;

    private double longitudeHPDegrees;


    private double latitudeHPDegrees;


    private boolean validHP = false;


    private double geoDeclination = 0.0;


    private double geoInclination = 0.0;


    private double geoIntensity = 0.0;

    private float heightHP = 0.0f;


    private double longitudeHP = 0.0;


    private double latitudeHP = 0.0;


    public int getNumMotors() {
        if (datFile.getDroneModel() == DATHeader.DroneModel.M600 || datFile.getDroneModel() == DATHeader.DroneModel.S900) {
            return 6;
        }

        return 4;
    }

    public AnalyzeDatResults analyze(boolean printVersion) throws IOException {
        insertFWDateStr();
        boolean processedPayload = false;
        this.printVersion = printVersion;
        final int sampleSize = (int) (datFile.getClockRate() / sampleRate);

        try {
            datFile.reset();
            // If there is a .csv being produced go ahead and output
            // the first row containing the column headings
            if (csvWriter != null) {
                csvWriter.print("Tick#,offsetTime");
                printCSVLine(lineType.HEADER);
            }
            long lastTickNoPrinted = -sampleSize;

            while (datFile.getNextDatRec(true, true, true, false)) {
                int payloadType = datFile.type;
                long payloadStart = datFile.start;
                int payloadLength = datFile.payloadLength;
                tickNo = datFile.tickNo;
                if (tickNo > tickRangeUpper) {
                    throw new EOFException();
                }
                for (int i = 0; i < records.size(); i++) {
                    if (records.get(i).isId(payloadType)) {
                        Payload payload = new Payload(datFile, payloadStart, payloadLength, payloadType, tickNo);
                        try {
                            ((DATRecord) records.get(i)).process(payload);
                            processedPayload = true;
                        } catch (Exception e) {
                            String errMsg = "Can't process record " + ((DATRecord) records.get(i)) + " tickNo=" + tickNo + " filePos=" + datFile.getPos();
                            if (DATPersist.EXPERIMENTAL_DEV) {
                                System.out.println(errMsg);
                                e.printStackTrace();
                            } else {
                                LOG.warning(errMsg);
                            }
                            throw new RuntimeException(errMsg);
                        }
                    }
                }
                if (tickRangeLower <= tickNo && (csvWriter != null) && processedPayload && tickNo >= lastTickNoPrinted + sampleSize) {
                    csvWriter.print(tickNo + "," + datFile.timeString(tickNo, timeOffset));
                    printCSVLine(lineType.LINE);
                    lastTickNoPrinted = tickNo;
                    processedPayload = true;
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            datFile.close();
            LOG.info("CRC Error Ratio " + datFile.getErrorRatio(DATDJIFile.errorType.CRC));
            LOG.info("Other Error Ratio " + datFile.getErrorRatio(DATDJIFile.errorType.Other));
            LOG.info("TotalNumRecExceptions = " + DATRecord.totalNumRecExceptions);
        }
        return datFile.getResults();
    }

    private void printCsvValue(String header, String value, lineType lineT, boolean valid) throws IOException {
        if (lineT == lineType.HEADER) {
            csvWriter.print("," + header);
        } else {
            csvWriter.print(",");
            if (valid) csvWriter.print("" + value.trim());
        }
    }


    public void addAttrValuePair(String attr, String value) {
        attrValuePairs.add(new AttrValuePair(attr, value));
    }

    protected void processAttrValuesPairs() throws IOException {
        csvWriter.print(",");
        if (attrValuePairs.size() > 0) {
            AttrValuePair avp = attrValuePairs.remove(0);
            csvWriter.print(avp.getAttr() + "|" + avp.getValue());
        }
    }

    protected void printAttrValuePair(String attr, String value) throws IOException {
        csvWriter.print("," + attr + "," + value);
    }


    public void createRecordParsers() {
        goTxt5012.current = null; // TODO Figure out GoTxt50_12
        List<DATRecord> recs = new ArrayList<>();
        try {
            int numNoRecParsers = 0;
            int numCreatedParsers = 0;
            @SuppressWarnings("unchecked") HashMap<Integer, RecSpec> recsInDat = (HashMap<Integer, RecSpec>) datFile.getRecsInDat().clone();

            for (RecSpec recInDat : recsInDat.values()) {
                List<DATRecord> datRecordInstLst = getRecordInst(recInDat);
                if (datRecordInstLst != null && datRecordInstLst.size() > 0) {
                    for (DATRecord DATRecordInst : datRecordInstLst) {
                        int recInstLength = DATRecordInst.getLength();
                        if (recInstLength <= recInDat.getLength()) { // recInstLength == -1 means it's a RecType.STRING
                            recs.add(DATRecordInst);
                            numCreatedParsers++;
                            LOG.info("Add RecParser #" + numCreatedParsers + " " + DATRecordInst.getClassDescription());
                        } else {
                            LOG.info(" Wrong length RecParser #" + numNoRecParsers + " RecInDat Id/Length =" + recInDat.getId() + "/" + recInDat.getLength() + " RecInst/length =" + DATRecordInst.getName() + "//" + recInstLength);
                        }
                    }
                } else {
                    numNoRecParsers++;
                    LOG.info("No RecParser #" + numNoRecParsers + " RecId " + recInDat + "/" + recInDat.getLength());
                }
            }
            LOG.info("Num of created parsers " + numCreatedParsers + " Num of NoRecParsers " + numNoRecParsers);
            //now sort the records
            for (Integer integer : DATDictionary.defaultOrder) {
                int recId = integer;
                DATRecord foundDATRecord = null;
                for (DATRecord rec : recs) {
                    if (rec.getId() == recId && !(rec instanceof RecordDef)) {
                        DATRecords.add(rec);
                        foundDATRecord = rec;
                    }
                }
                if (foundDATRecord != null) {
                    recs.remove(foundDATRecord);
                }
            }

            DATRecords.addAll(recs);

        } catch (Exception e) {
            LOG.warning(e.toString());
        }
    }

    protected List<DATRecord> getRecordInst(RecSpec recInDat) {
        List<DATRecord> retv = new ArrayList<>();
        DATRecord rec;
        rec = DATDictionary.getRecordInst(DATDictionary.entries, recInDat, this, true);
        if (rec != null) {
            retv.add(rec);
            return retv;
        }
        switch (DATPersist.parsingMode) {
            case DAT_THEN_ENGINEERED:
                rec = getRecordInstFromDat(recInDat);
                if (rec != null) {
                    retv.add(rec);
                } else {
                    rec = getRecordInstEngineered(recInDat);
                    if (rec != null) {
                        retv.add(rec);
                    }
                }
                break;
            case ENGINEERED_THEN_DAT:
                rec = getRecordInstEngineered(recInDat);
                if (rec != null) {
                    retv.add(rec);
                } else {
                    rec = getRecordInstFromDat(recInDat);
                    if (rec != null) {
                        retv.add(rec);
                    }
                }
                break;
            case JUST_DAT:
                rec = getRecordInstFromDat(recInDat);
                if (rec != null) {
                    retv.add(rec);
                }
                break;
            case JUST_ENGINEERED:
                rec = getRecordInstEngineered(recInDat);
                if (rec != null) {
                    retv.add(rec);
                }
                break;
            case ENGINEERED_AND_DAT:
                rec = getRecordInstEngineered(recInDat);
                if (rec != null) {
                    retv.add(rec);
                }
                rec = getRecordInstFromDat(recInDat);
                if (rec != null) {
                    retv.add(rec);
                }
                break;
            default:
                return retv;
        }

        return null;
    }

    private DATRecord getRecordInstEngineered(RecSpec recInDat) {
        DATRecord retv;
        retv = DATDictionary.getRecordInst(DATDictionary.entries, recInDat, this, true);
        if (retv != null) {
            return retv;
        }
        retv = DATDictionary.getRecordInst(DATDictionary.entries, recInDat, this, true);
        return retv;
    }

    private DATRecord getRecordInstFromDat(RecSpec recInDat) {
        List<RecordDef> recordDefs = datFile.getRecordDefs();
        if (recordDefs != null) {
            for (RecordDef recDef : recordDefs) {
                if (recDef.getId() == recInDat.getId() && recDef.getLength() <= recInDat.getLength()) {
                    recDef.init(this);
                    return recDef;
                }
            }
        }
        DATRecord retv = null;
        if (null != (retv = DATDictionary.getRecordInst(DATDictionary.entries, recInDat, this, false))) {
            return retv;
        }

        return DATDictionary.getRecordInst(DATDictionary.entries, recInDat, this, false);
    }

    public DATDJIFile getDatFile() {
        return datFile;
    }

    protected void printCSVLine(lineType lineT) throws Exception {
        try {
            for (DATRecord datRecord : DATRecords) {
                datRecord.printCols(lineT);
            }

            if (lineT == lineType.HEADER) {
                csvWriter.print(",Attribute|Value");
                if (printVersion) {
                    printCsvValue(this.getClass().getSimpleName(), "", lineT, false);
                    // TODO: Determine what this is for
                    if (datFile.isTablet()) {
                        printCsvValue("Tablet", "", lineT, false);
                    } else {
                        printCsvValue("", "", lineT, false);
                    }
                }
            } else {
                processAttrValuesPairs();
            }
            csvWriter.print("\n");
        } catch (Exception e) {
            LOG.warning("ConvertDat error in printCsvLine");
            throw e;
        }
    }

    public void processCoords(double longitudeDegrees, double latitudeDegrees, float relativeHeight) {
        if (!gpsCoordsOK) {
            gpsCoordsOK = (longitudeDegrees != 0.0 && latitudeDegrees != 0.0 && relativeHeight != 0.0f);
        } else {
            this.relativeHeight = relativeHeight;
            this.relativeHeightOK = true;
        }
        if (kmlType != KmlType.NONE && tickRangeLower <= tickNo) {
            float alt = relativeHeight;
            if (kmlType == KmlType.PROFILE) {
                alt += homePointElevation;
                absoluteHeight = alt;
                absoluteHeightValid = true;
            }
            kmlPS.println("              " + longitudeDegrees + "," + latitudeDegrees + "," + alt);
        }
    }

    public void processCoordsNoGoTxt(double longitudeDegrees, double latitudeDegrees, float baroRaw) {
        if (!gpsCoordsOK) {
            gpsCoordsOK = (longitudeDegrees != 0.0 && latitudeDegrees != 0.0);
        }
        if (!Double.isNaN(takeOffAlt)) {
            relativeHeight = baroRaw - takeOffAlt;
            relativeHeightOK = true;
        }
        if (kmlType != KmlType.NONE && tickRangeLower <= tickNo) {
            if (gpsCoordsOK) {
                if (relativeHeightOK) {
                    float alt = (float) relativeHeight;
                    if (kmlType == KmlType.PROFILE) {
                        alt += homePointElevation;
                        absoluteHeight = alt;
                        absoluteHeightValid = true;
                    }
                    kmlPS.println("              " + longitudeDegrees + "," + latitudeDegrees + "," + alt);
                } else {
                    kmlPS.println("              " + longitudeDegrees + "," + latitudeDegrees);
                }
            }
        }
    }


    public void processHomePoint(/*Record255_1 record255_1,*/
            double latitudeHPDegrees, double longitudeHPDegrees, float height) {
        this.latitudeHPDegrees = latitudeHPDegrees;
        this.longitudeHPDegrees = longitudeHPDegrees;
        heightHP = height;
        longitudeHP = Math.toRadians(longitudeHPDegrees);
        latitudeHP = Math.toRadians(latitudeHPDegrees);
        validHP = true;
        if (geoDeclination == 0.0) {
            geoDeclination = geoMag.getDeclination(latitudeHPDegrees, longitudeHPDegrees);
            addAttrValuePair("geoDeclination", String.format("%1$2.2f degrees", geoDeclination));
            geoInclination = geoMag.getDipAngle(latitudeHPDegrees, longitudeHPDegrees);
            addAttrValuePair("geoInclination", String.format("%1$2.2f degrees", geoInclination));
            geoIntensity = geoMag.getIntensity(latitudeHPDegrees, longitudeHPDegrees);
            addAttrValuePair("geoIntensity", String.format("%1$2.2f nanoTesla", geoIntensity));
        }
    }

    public void setTakeOffAlt(double takeOffAlt) {
        this.takeOffAlt = takeOffAlt;
    }

    public double getTakeOffAlt() {
        return takeOffAlt;
    }

    public boolean isRelativeHeightOK() {
        return relativeHeightOK;
    }

    public double getRelativeHeight() {
        return relativeHeight;
    }

    public float getAbsoluteHeight() {
        return absoluteHeight;
    }

    public void createXMLFile() throws IOException {
        createXMLHeader();
        createXMLGuts();
        createXMLFinal();
    }

    public void createXMLFinal() throws IOException {
        csvWriter.println("</signals>");
        csvWriter.close();
    }

    public void createXMLHeader() throws IOException {
        csvWriter.println("<?xml version=\"1.0\"?>");
        csvWriter.println("<signals>");
        csvWriter.println("<series>");
        csvWriter.println("<sigName>Tick#</sigName>");
        csvWriter.println("<colName>Tick#</colName>");
        csvWriter.println("<numType>int</numType>");
        csvWriter.println("<experimental>true</experimental>");
        csvWriter.println("</series>");
        csvWriter.println("<timeAxis>");
        csvWriter.println("<sigName>offsetTime</sigName>");
        csvWriter.println("<colName>offsetTime</colName>");
        csvWriter.println("<numType>float</numType>");
        csvWriter.println("</timeAxis>");
    }

    public HashSet<Axis> axes = new HashSet<>();

    public void createXMLGuts() throws IOException {
        axes.clear();
        for (DATRecord datRecord : DATRecords) {
            datRecord.printCols(lineType.XML);
        }

        for (Axis axis : axes) {
            csvWriter.println("  <axis>");
            csvWriter.println("   <name>" + axis.getName() + "</name>");
            csvWriter.println("   <label>" + axis.getLabel() + "</label>");
            if (axis.getUnits() != null) {
                csvWriter.println("   <units>" + axis.getUnits() + "</units>");
            }
            csvWriter.println("  </axis>");
        }
    }

    public void setCsvWriter(DAT2CSVWriter writer) {
        csvWriter = writer;
        for (DATRecord datRecord : DATRecords) {
            datRecord.setCSVWriter(writer);
        }
    }

    public void setExperimentalFields(boolean experimental) {
        DATPersist.EXPERIMENTAL_FIELDS = experimental;
    }

    public double getTime() {
        return datFile.time(tickNo, 0);
    }

    public double getHPLongDeg() {
        return longitudeHPDegrees;
    }

    public double getHPLatDeg() {
        return latitudeHPDegrees;
    }

    public boolean isHpValid() {
        return validHP;
    }

    public double getGeoDeclination() {
        return geoDeclination;
    }

    public double getGeoInclination() {
        return geoInclination;
    }

    public float getHPHeight() {
        return heightHP;
    }

    public double getHPLatRad() {
        return latitudeHP;
    }

    public double getHPLongRad() {
        return longitudeHP;
    }

    public void setRecords(List<DATRecord> recs) {
        DATRecords = recs;
    }

    public void setSampleRate(float sampleRate) {
        this.sampleRate = sampleRate;
    }

}
