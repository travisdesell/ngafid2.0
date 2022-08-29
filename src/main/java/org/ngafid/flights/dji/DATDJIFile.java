/* DatFile class

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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

public class DATDJIFile {

    protected final static int headerLength = 10;

    protected final static int chksumLength = 2;

    protected int startOfRecords = 128;

    public void setStartOfRecords(int startOfRecords) {
        this.startOfRecords = startOfRecords;
    }

    protected MappedByteBuffer memory = null;

    private String droneModelStr;

    protected long filePos = 0;

    protected File file = null;

    protected FileInputStream inputStream = null;

    protected FileChannel channel = null;

    protected long fileLength = 0;

    public static String firmwareDate;

    protected int numCorrupted = 0;

    protected long numRecs = 0;

    protected AnalyzeDatResults results = null;

    public long startOfRecord = 0;

    public long lowestTickNo = -1;

    public long highestTickNo = -1;

    public long firstMotorStartTick = 0;

    public long lastMotorStopTick = -1;

    public long flightStartTick = -1;

    public long gpsLockTick = -1;

    protected int numBattCells = 0;

    public DATHeader.DroneModel droneModel = DATHeader.DroneModel.UNKNOWN;

    protected long lastRecordTickNo = 0;

    public static final DecimalFormat timeFormat = new DecimalFormat("###.000", new DecimalFormatSymbols(Locale.US));

    private static DATDJIFile DATDJIFile;

    double clockRate = 600;

    private DATHeader datHeader;

    private HashMap<Integer, RecSpec> recsInDat = new HashMap<Integer, RecSpec>();

    public long _tickNo = 0;

    public int _type = 0;

    public HashMap<Integer, RecSpec> getRecsInDat() {
        return recsInDat;
    }

    public void addRecInDat(int type, int length) {
        Integer key = type;
        if (!recsInDat.containsKey(key)) {
            recsInDat.put(key, new RecSpec(type, length));
        }
    }

    protected void clearRecsInDat() {
        recsInDat.clear();
    }

    public DATDJIFile(String fileName) throws IOException, NotDatFile {
        this(new File(fileName));
    }

    public static DATDJIFile createDatFile(String datFileName)
            throws NotDatFile, IOException {
        byte arra[] = new byte[256];
        //if (true )return (new DatFileV3(datFileName));
//        DatConLog.Log(" ");
//        DatConLog.Log("createDatFile " + datFileName);
        FileInputStream bfr = new FileInputStream(new File(datFileName));
        bfr.read(arra, 0, 256);
        bfr.close();
        String headerString = new String(arra, 0, 21);
        if (!(headerString.substring(16, 21).equals("BUILD"))) {
            if (Persist.invalidStructOK) {
//                DatConLog.Log("createDatFile invalid header - proceeding");
                DATDJIFile = new DatFileV3(datFileName);
                DATDJIFile.setStartOfRecords(256);
                return DATDJIFile;
            }
            if (headerString.substring(0, 4).equals("LOGH")) {
//                throw new NotDatFile("Probably an encrypted .DAT");
            }
//            throw new NotDatFile();
        }
        if ((new String(arra, 242, 10).equals("DJI_LOG_V3"))) {
            DATDJIFile = new DatFileV3(datFileName);
            DATDJIFile.setStartOfRecords(256);
        } else {
            DATDJIFile = new DatFileV1(datFileName);
            DATDJIFile.setStartOfRecords(128);
        }
        return DATDJIFile;
    }

    public static boolean isDatFile(String datFileName) {
        byte arra[] = new byte[256];
        try {
            // TODO: Try With resources
            FileInputStream bfr = new FileInputStream(new File(datFileName));
            bfr.read(arra, 0, 256);
            bfr.close();
            if ((new String(arra, 16, 5).equals("BUILD"))) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    public static DATDJIFile createDatFile(String datFileName, final DatCon datCon) throws IOException {
        if (DJIAssistantFile.isDJIDat(new File(datFileName))) {
            if (Persist.autoTransDJIAFiles) {
                int lastSlash = datFileName.lastIndexOf("\\");
                try {
                    return new DatFileV3(result.getFile());
                }
            }
        }
        return createDatFile(datFileName);
    }

    public DATDJIFile(File file) throws FileNotFoundException {
        this.datHeader = new DATHeader(this);
        this.file = file;
        this.results = new AnalyzeDatResults();
        this.fileLength = file.length();
        this.inputStream = new FileInputStream(file);
        this.channel = inputStream.getChannel();

        try {
            memory = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileLength);
        } catch (IOException e) {
            e.printStackTrace();
        }
        memory.order(ByteOrder.LITTLE_ENDIAN);
        droneModel = datHeader.getDroneModel();
        //acTypeName = DatHeader.toString(acType);
    }

    public DATDJIFile() {
    }

    public DATConvert createConVertDat() {
        return (new DATConvert(this));
    }

    public void close() {
        if (inputStream != null) {
            try {
                inputStream.close();
                if (inputStream.getChannel() != null) {
                    inputStream.getChannel().close();
                    inputStream = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        memory = null;
        System.gc();
    }

    public void reset() throws IOException, FileEnd {
        // tickGroups[0].reset();
        // tickGroups[1].reset();
        // tgIndex = 1;
        numCorrupted = 0;
        results = new AnalyzeDatResults();
        if (inputStream == null) {
            inputStream = new FileInputStream(file);
            channel = inputStream.getChannel();
            memory = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileLength);
            memory.order(ByteOrder.LITTLE_ENDIAN);
        }
        startOfRecord = startOfRecords;
        setPosition(startOfRecord);
        Record.totalNumRecExceptions = 0;
    }

    public void skipOver(int num) throws IOException {
        filePos = filePos + num;
        if (filePos > fileLength)
            throw (new IOException());
        channel.position(filePos);
    }

    public String toString() {
        return file.getName();
    }

    public void setPosition(final long pos) throws FileEnd, IOException {
        filePos = pos;
        if (filePos > fileLength)
            throw (new FileEnd());
        channel.position(pos);
    }

    public long getPos() {
        return filePos;
    }

    public long getLength() {
        return fileLength;
    }

    public byte getByte() {
        return memory.get((int) filePos);
    }

    public int getByte(long fp) throws FileEnd {
        if (fp >= fileLength)
            throw (new FileEnd());
        return memory.get((int) fp);
    }

    public byte readByte() throws IOException {
        byte rv = getByte();
        skipOver(1);
        return rv;
    }

    protected short getShort() {
        return memory.getShort((int) filePos);
    }

    protected short getShort(long fp) {
        return memory.getShort((int) fp);
    }

    //    protected short getShort(long fp) throws FileEnd {
    //        if (fp > fileLength - 2)
    //            throw (new FileEnd());
    //        return (int) (0xff & memory.get((int) fp))
    //                + 256 * (int) (0xff & memory.get((int) (fp + 1)));
    //    }

    public int getUnsignedShort() {
        return (0xff & memory.get((int) filePos))
                + 256 * (0xff & memory.get((int) (filePos + 1)));
    }

    protected int getUnsignedShort(long fp) throws FileEnd {
        if (fp > fileLength - 2)
            throw (new FileEnd());
        return (0xff & memory.get((int) fp))
                + 256 * (int) (0xff & memory.get((int) (fp + 1)));
    }

    public int getInt() {
        return memory.getInt((int) filePos);
    }

    public long getUnsignedInt() throws FileEnd {
        return getUnsignedInt(filePos);
    }

    public long getUnsignedInt(long fp) throws FileEnd {
        if (fp > fileLength - 4)
            throw (new FileEnd());
        return (long) (0xff & memory.get((int) fp))
                + (256 * (long) (0xff & memory.get((int) (fp + 1))))
                + (65536 * (long) (0xff & memory.get((int) (fp + 2))))
                + (65536 * 256 * (long) (0xff & memory.get((int) (fp + 3))));
    }

    public long getLong() {
        return memory.getLong((int) filePos);
    }

    public float getFloat() {
        return memory.getFloat((int) filePos);
    }

    public double getDouble() {
        return memory.getDouble((int) filePos);
    }

    public AnalyzeDatResults getResults() {
        return results;
    }

    public File getFile() {
        return file;
    }

    public void setStartOfRecord(long sor) {
        startOfRecord = sor;
    }

    public String getFileName() {
        String retv = null;
        if (file != null) {
            retv = file.getName();
        }
        return retv;
    }

    public double getClockRate() {
        return clockRate;
    }

    public String timeString(long tickNo, long offset) {
        return timeFormat.format(time(tickNo, offset));
    }

    public float time(long tickNo, long offset) {
        return (float) (tickNo - offset) / (float) clockRate;
    }

    public void setClockRate(double rate) {
        clockRate = rate;
    }

    public long getTickFromTime(Number time, long offset) {
        return ((long) (clockRate * time.doubleValue())) + offset;
    }

    public void preAnalyze() {
        switch (droneModel) {
            case I1, M600, I2, M100, M200 -> numBattCells = 6;
            case MavicPro, MavicAir, SPARK -> numBattCells = 3;
            case P3AP, P3S, P4, P4A, P4P -> numBattCells = 4;

            default -> {
                numBattCells = 4;
//                DatConLog.Log("Assuming 4 cells per battery");
            }
        }
    }

    public int getNumBattCells() {
        return numBattCells;
    }

    public String getFirmwareDate() {
        return datHeader.getFirmwareDate();
    }

    public String getACTypeString() {
        if (droneModel != DATHeader.DroneModel.UNKNOWN) {
            return droneModelStr.toString();
        }

        return droneModelStr;
    }

    public RecSpec getRecId(int _type) {
        RecSpec retv = null;
        Iterator<RecSpec> iter = recsInDat.values().iterator();
        while (iter.hasNext()) {
            RecSpec tst = iter.next();
            if (tst.getId() == _type) {
                if (retv != null) {
                    return null;
                }
                retv = tst;
            }
        }
        return retv;
    }

    public void printTypes() {
        Iterator<RecSpec> iter = recsInDat.values().iterator();
        while (iter.hasNext()) {
            RecSpec tst = iter.next();
//            DatConLog.Log(tst.getDescription() + " Type " + tst.getId());
        }
    }

    public boolean isTablet() {
        return false;
    }

    public DATHeader.DroneModel getDroneModel() {
        return droneModel;
    }

    public double getPercentCorrupted() {
        return (((double) numCorrupted) / ((double) numRecs)) / 100.0;
    }

    public double getErrorRatio(Type type) {
        switch (type) {
        case CRC:
            return (double) Corrupted.getNum(Corrupted.Type.CRC)
                    / (double) numRecs;
        case Other:
            return (double) Corrupted.getNum(Corrupted.Type.Other)
                    / (double) numRecs;
        default:
            return 0.0;

        }
    }

}
