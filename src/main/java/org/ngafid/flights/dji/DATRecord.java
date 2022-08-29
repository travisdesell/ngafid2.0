package org.ngafid.flights.dji;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Objects;


public abstract class DATRecord extends RecSpec {

    protected ByteBuffer payloadBB = null;

    protected DAT2CSVWriter csvWriter = null;

    protected DATConvert convertDat = null;

    protected DATDJIFile datFile;


    protected int numRecExceptions = 0;

    public static int totalNumRecExceptions = 0;

    static DecimalFormat df = new DecimalFormat("000.#############", new DecimalFormatSymbols(Locale.US));

    public DATRecord() {
        super();
    }

    public String getDescription() {
        return this.getClass().toString();
    }

    public DATRecord(DATConvert convertDat, int id, int length) {
        super(id, length);
        this.convertDat = convertDat;
        datFile = this.convertDat.getDatFile();
        this.csvWriter = convertDat.csvWriter;
    }

    public DATRecord(DATDJIFile datFile) { // TODO: Originally DatFileV3. Look into it
        this.datFile = datFile;
    }

    public DATRecord(String name, int id, RecType recType) {
        super(name, id, recType);
    }

    public void process(Payload record) {
        payloadBB = record.getBB();
    }

    public void printCols(lineType lineT) {
        throw new RuntimeException("printCols called in Record");
    }

    public void printCSVValue(Number value, DATSignal signal, String suffix, lineType lineT, boolean valid) throws IOException {
        if (lineT == lineType.XML) {
            printXmlSig(signal.getName(), suffix, signal);
            return;
        }
        if (Persist.EXPERIMENTAL_FIELDS || !signal.isExperimental()) {
            if (lineT == lineType.HEADER) {
                csvWriter.print("," + signal.getName());
                if (suffix != null && !suffix.isEmpty()) {
                    csvWriter.print(":" + suffix);
                }
                if (Persist.showUnits && signal.hasUnits()) {
                    csvWriter.print("[" + signal.getUnitsNoComma() + "]");
                }
            } else if (lineT == lineType.LINE) {
                csvWriter.print(",");
                if (valid) csvWriter.print("" + value);
            }
        }
    }

    protected void printCSVValue(String value, DATSignal signal, String suffix, lineType lineT, boolean valid) throws IOException {
        if (lineT == lineType.XML) {
            printXmlSig(signal.getName(), suffix, signal);
            return;
        }
        if (Persist.EXPERIMENTAL_FIELDS || !signal.isExperimental()) {
            if (lineT == lineType.HEADER) {
                csvWriter.print("," + signal.getName());
                if (!Objects.equals(suffix, "")) {
                    csvWriter.print(":" + suffix);
                }
                if (Persist.showUnits && signal.hasUnits()) {
                    csvWriter.print("[" + signal.getUnitsNoComma() + "]");
                }
            } else if (lineT == lineType.LINE) {
                csvWriter.print(",");
                if (valid) csvWriter.print("" + value);
            }
        }
    }

    protected void RecordException(Exception e) {
        if (numRecExceptions == 0) {
            String errMsg = "RecException filePos()=" + datFile.getPos() + " tickNo " + datFile.tickNo + " type =" + datFile.type;
            if (Persist.EXPERIMENTAL_DEV) {
                System.out.println(errMsg);
                e.printStackTrace();
            } else {
                DatConLog.Exception(e, errMsg);
            }
        }
        numRecExceptions++;
        totalNumRecExceptions++;
    }

    protected void printCSVValue(float value, String header, lineType lineT, boolean valid) throws IOException {
        if (lineT == lineType.XML) return;
        if (lineT == lineType.HEADER) {
            csvWriter.print("," + header);
        } else {
            csvWriter.print(",");
            if (valid) csvWriter.print("" + value);
        }
    }

    protected void printCSVValue(String value, String header, lineType lineT, boolean valid) throws IOException {
        if (lineT == lineType.HEADER) {
            csvWriter.print("," + header);
        } else {
            csvWriter.print(",");
            if (valid) csvWriter.print("" + value);
        }
    }

    private void printXmlSig(String name, String suffix, DATSignal signal) throws IOException {
        String colName = name;
        String description;
        if (suffix != null && !suffix.equalsIgnoreCase("")) {
            colName = name + ":" + suffix;
        }
        switch (signal.getType()) {
            case SERIES:
                csvWriter.println("<series>");
                csvWriter.println("  <sigName>" + colName + "</sigName>");
                csvWriter.println("  <colName>" + colName + "</colName>");
                Axis axis = signal.getAxis();
                if (axis != null) {
                    csvWriter.println("  <axis>" + axis.getName() + "</axis>");
                    convertDat.axes.add(axis);
                }
                switch (signal.getNumType()) {
                    case DOUBLE:
                        csvWriter.println("  <numType>double</numType>");
                        break;
                    case FLOAT4:
                        csvWriter.println("  <numType>float</numType>");
                        break;
                    case INT:
                        csvWriter.println("  <numType>int</numType>");
                        break;
                    case UNDEFINED:
                        break;
                    default:
                        break;
                }
                description = signal.getDescription();
                if (description != null) {
                    csvWriter.println("  <description>" + description + "</description>");
                }
                if (signal.isExperimental()) {
                    csvWriter.println("  <experimental>true</experimental>");
                }
                if (signal.hasUnits()) {
                    csvWriter.println("  <units>" + signal.getUnits() + "</units>");
                }
                csvWriter.println("</series>");
                break;
            case STATE:
                csvWriter.println("<state>");
                csvWriter.println("  <sigName>" + colName + "</sigName>");
                csvWriter.println("  <colName>" + colName + "</colName>");
                csvWriter.println("  <inverse></inverse>");
                description = signal.getDescription();
                if (description != null) {
                    csvWriter.println("  <description>" + description + "</description>");
                }
                csvWriter.println("  <stateSpec>");
                csvWriter.println("     <stateName>" + signal.getDefaultState() + "</stateName>");
                csvWriter.println("     <color>white</color>");
                csvWriter.println("  </stateSpec>");
                csvWriter.println("</state>");
                break;
            case TIMEAXIS:
                break;
            case UNDEFINED:
                break;
            default:
                break;
        }
    }

    public void setCSVWriter(DAT2CSVWriter writer) {
        this.csvWriter = writer;
    }

    public String getClassDescription() {
        return getClass().toString() + " /" + getLength();
    }

    public void setDatFile(DATDJIFile datFile) {
        this.datFile = datFile;
    }

}
