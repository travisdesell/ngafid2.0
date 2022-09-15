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

import java.io.*;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import java.util.HashMap;
import java.util.List;
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


    public long startOfRecord = 0;

    public long lowestTickNo = -1;

    public long highestTickNo = -1;

    public long firstMotorStartTick = 0;

    public long lastMotorStopTick = -1;

    public long flightStartTick = -1;

    public long gpsLockTick = -1;

    protected int numBatteryCells = 0;

    public DATHeader.DroneModel droneModel = DATHeader.DroneModel.UNKNOWN;

    protected long lastRecordTickNo = 0;

    public static final DecimalFormat timeFormat = new DecimalFormat("###.000", new DecimalFormatSymbols(Locale.US));

    private static DATDJIFile DATDJIFile;

    double clockRate = 600;

    private DATHeader datHeader;

    private HashMap<Integer, RecSpec> recsInDat = new HashMap<>();

    public long tickNo = 0;

    public int type = 0;
    private int numErrorCRC = 0;
    private int numErrorOther = 0;
    private List<RecordDef> recordDefs;

    public int payloadLength = 0;

    public long start;

    static long datRecTickNo = -1;

    long presentOffset = 0;

    long prevOffset = 0;

    long lastActualTickNo = 0;

    long upperTickLim = Long.parseLong("4292717296");

    long tickNoBoundary = Long.parseLong("4294967296");

    boolean inRollover = false;

    int numRolloverRecs = 0;

    public int lengthOfRecord = 0;

    long xxxx = Long.parseLong("14156619143"); // TODO: Determine use
    public AnalyzeResultsDAT results;

    static int[] crc = new int[]{0x0000, 0x1189, 0x2312, 0x329b, 0x4624,
            0x57ad, 0x6536, 0x74bf, 0x8c48, 0x9dc1, 0xaf5a, 0xbed3, 0xca6c,
            0xdbe5, 0xe97e, 0xf8f7, 0x1081, 0x0108, 0x3393, 0x221a, 0x56a5,
            0x472c, 0x75b7, 0x643e, 0x9cc9, 0x8d40, 0xbfdb, 0xae52, 0xdaed,
            0xcb64, 0xf9ff, 0xe876, 0x2102, 0x308b, 0x0210, 0x1399, 0x6726,
            0x76af, 0x4434, 0x55bd, 0xad4a, 0xbcc3, 0x8e58, 0x9fd1, 0xeb6e,
            0xfae7, 0xc87c, 0xd9f5, 0x3183, 0x200a, 0x1291, 0x0318, 0x77a7,
            0x662e, 0x54b5, 0x453c, 0xbdcb, 0xac42, 0x9ed9, 0x8f50, 0xfbef,
            0xea66, 0xd8fd, 0xc974, 0x4204, 0x538d, 0x6116, 0x709f, 0x0420,
            0x15a9, 0x2732, 0x36bb, 0xce4c, 0xdfc5, 0xed5e, 0xfcd7, 0x8868,
            0x99e1, 0xab7a, 0xbaf3, 0x5285, 0x430c, 0x7197, 0x601e, 0x14a1,
            0x0528, 0x37b3, 0x263a, 0xdecd, 0xcf44, 0xfddf, 0xec56, 0x98e9,
            0x8960, 0xbbfb, 0xaa72, 0x6306, 0x728f, 0x4014, 0x519d, 0x2522,
            0x34ab, 0x0630, 0x17b9, 0xef4e, 0xfec7, 0xcc5c, 0xddd5, 0xa96a,
            0xb8e3, 0x8a78, 0x9bf1, 0x7387, 0x620e, 0x5095, 0x411c, 0x35a3,
            0x242a, 0x16b1, 0x0738, 0xffcf, 0xee46, 0xdcdd, 0xcd54, 0xb9eb,
            0xa862, 0x9af9, 0x8b70, 0x8408, 0x9581, 0xa71a, 0xb693, 0xc22c,
            0xd3a5, 0xe13e, 0xf0b7, 0x0840, 0x19c9, 0x2b52, 0x3adb, 0x4e64,
            0x5fed, 0x6d76, 0x7cff, 0x9489, 0x8500, 0xb79b, 0xa612, 0xd2ad,
            0xc324, 0xf1bf, 0xe036, 0x18c1, 0x0948, 0x3bd3, 0x2a5a, 0x5ee5,
            0x4f6c, 0x7df7, 0x6c7e, 0xa50a, 0xb483, 0x8618, 0x9791, 0xe32e,
            0xf2a7, 0xc03c, 0xd1b5, 0x2942, 0x38cb, 0x0a50, 0x1bd9, 0x6f66,
            0x7eef, 0x4c74, 0x5dfd, 0xb58b, 0xa402, 0x9699, 0x8710, 0xf3af,
            0xe226, 0xd0bd, 0xc134, 0x39c3, 0x284a, 0x1ad1, 0x0b58, 0x7fe7,
            0x6e6e, 0x5cf5, 0x4d7c, 0xc60c, 0xd785, 0xe51e, 0xf497, 0x8028,
            0x91a1, 0xa33a, 0xb2b3, 0x4a44, 0x5bcd, 0x6956, 0x78df, 0x0c60,
            0x1de9, 0x2f72, 0x3efb, 0xd68d, 0xc704, 0xf59f, 0xe416, 0x90a9,
            0x8120, 0xb3bb, 0xa232, 0x5ac5, 0x4b4c, 0x79d7, 0x685e, 0x1ce1,
            0x0d68, 0x3ff3, 0x2e7a, 0xe70e, 0xf687, 0xc41c, 0xd595, 0xa12a,
            0xb0a3, 0x8238, 0x93b1, 0x6b46, 0x7acf, 0x4854, 0x59dd, 0x2d62,
            0x3ceb, 0x0e70, 0x1ff9, 0xf78f, 0xe606, 0xd49d, 0xc514, 0xb1ab,
            0xa022, 0x92b9, 0x8330, 0x7bc7, 0x6a4e, 0x58d5, 0x495c, 0x3de3,
            0x2c6a, 0x1ef1, 0x0f78};


    public enum errorType {
        CRC, Other
    }

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

    public DATDJIFile(String fileName) throws IOException {
        this(new File(fileName));
    }

    public static DATDJIFile createDatFile(String datFileName) throws NotDATException, IOException {
        byte[] arra = new byte[256];
        //if (true )return (new DatFileV3(datFileName));
//        DatConLog.Log(" ");
//        DatConLog.Log("createDatFile " + datFileName);
        FileInputStream bfr = new FileInputStream(datFileName);
        bfr.read(arra, 0, 256); // TODO: Might remove
        bfr.close();
        String headerString = new String(arra, 0, 21);
        if (!(headerString.startsWith("BUILD", 16))) {
            if (DATPersist.invalidStructOK) {
//                DatConLog.Log("createDatFile invalid header - proceeding");
//                DATDJIFile = new DatFileV3(datFileName);
                DATDJIFile.setStartOfRecords(256);
                return DATDJIFile;
            }
            if (headerString.startsWith("LOGH")) {
                throw new NotDATException("Probably an encrypted .DAT");
            }

            throw new NotDATException("");
        }
        if ((new String(arra, 242, 10).equals("DJI_LOG_V3"))) {
            DATDJIFile = new DATDJIFile(datFileName); // Was V3
            DATDJIFile.setStartOfRecords(256);
        } else {
            DATDJIFile = new DATDJIFile(datFileName); // Was V1
            DATDJIFile.setStartOfRecords(128);
        }
        return DATDJIFile;
    }

    public static boolean isDatFile(String datFileName) {
        byte[] arra = new byte[256];
        try {
            // TODO: Try With resources
            FileInputStream bfr = new FileInputStream(datFileName);
            bfr.read(arra, 0, 256);
            bfr.close();
            if ((new String(arra, 16, 5).equals("BUILD"))) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    public DATDJIFile(File file) throws FileNotFoundException {
        this.datHeader = new DATHeader(this);
        this.file = file;
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
        droneModelStr = droneModel.toString();
    }

    public DATDJIFile() {
    }

    public DATConvert createConvertDat() {
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

    public void reset() throws IOException {
        numCorrupted = 0;
        if (inputStream == null) {
            inputStream = new FileInputStream(file);
            channel = inputStream.getChannel();
            memory = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileLength);
            memory.order(ByteOrder.LITTLE_ENDIAN);
        }
        startOfRecord = startOfRecords;
        setPosition(startOfRecord);
        DATRecord.totalNumRecExceptions = 0;
    }

    public void skipOver(int num) throws IOException {
        filePos = filePos + num;
        if (filePos > fileLength) throw (new IOException());
        channel.position(filePos);
    }

    public String toString() {
        return file.getName();
    }

    public void setPosition(final long pos) throws IOException {
        filePos = pos;
        if (filePos > fileLength) throw new EOFException();
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

    public int getByte(long fp) throws EOFException {
        if (fp >= fileLength) throw (new EOFException());
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

    public int getUnsignedShort() {
        return (0xff & memory.get((int) filePos)) + 256 * (0xff & memory.get((int) (filePos + 1)));
    }

    protected int getUnsignedShort(long fp) throws EOFException {
        if (fp > fileLength - 2) throw (new EOFException());
        return (0xff & memory.get((int) fp)) + 256 * (0xff & memory.get((int) (fp + 1)));
    }

    public int getInt() {
        return memory.getInt((int) filePos);
    }

    public long getUnsignedInt() throws EOFException {
        return getUnsignedInt(filePos);
    }

    public long getUnsignedInt(long fp) throws EOFException {
        if (fp > fileLength - 4) throw (new EOFException());
        return (long) (0xff & memory.get((int) fp)) + (256 * (long) (0xff & memory.get((int) (fp + 1)))) + (65536 * (long) (0xff & memory.get((int) (fp + 2)))) + (65536 * 256 * (long) (0xff & memory.get((int) (fp + 3))));
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
            case I1:
            case I2:
            case M200:
            case M100:
            case M600:
                this.numBatteryCells = 6;
                break;

            case MavicAir:
            case SPARK:
                this.numBatteryCells = 3;
                break;

            default:
                this.numBatteryCells = 4;
                break;

        }
    }

    public int getNumBatteryCells() {
        return this.numBatteryCells;
    }

//    public String getFirmwareDate() {
//        return datHeader.getFirmwareDate();
//    }

    public String getDroneModelStr() {
        return droneModelStr;
    }

    public RecSpec getRecId(int _type) {
        RecSpec retv = null;
        for (RecSpec tst : recsInDat.values()) {
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
        for (RecSpec tst : recsInDat.values()) {
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

    public double getErrorRatio(errorType type) {
        if (type == errorType.CRC) {
            return (double) numErrorCRC / numRecs;
        }

        return (double) numErrorOther / numRecs;
    }

    public List<RecordDef> getRecordDefs() {
        return recordDefs;
    }

    public boolean getNextDatRec(boolean filter, boolean translate, boolean sequence, boolean eofProcessing) throws EOFException, CorruptedException {
        boolean done = false;
        long nextStartOfRecord = 0;
        long actualTickNo = 0;
        long offset = 0;
        while (!done) {
            try {
                setPosition(startOfRecord);
                // if positioned at a 0x00 then try to skip over the 0x00s , this from Spark .DAT
                if (getByte(startOfRecord) == 0x00) {
                    while (getByte(startOfRecord) == 0x00) {
                        System.out.println(startOfRecord);
                        startOfRecord++;
                        if (startOfRecord > fileLength) break;
                    }
                }
                // if not positioned at next 0x55, then its corrupted
                if (getByte(startOfRecord) != 0x55) {
                    throw (new CorruptedException(actualTickNo, startOfRecord));
                }
                lengthOfRecord = (0xFF & getByte(startOfRecord + 1));
                byte alwaysZero = (byte) getByte(startOfRecord + 2);
                nextStartOfRecord = startOfRecord + lengthOfRecord;
                if (nextStartOfRecord > fileLength) break;
                int type = getUnsignedShort(startOfRecord + 4);
                long thisRecordTickNo = getUnsignedInt(startOfRecord + 6);
                int calcChksum = calcChecksum(memory, startOfRecord, (short) (lengthOfRecord - 2));
                int chksum = getUnsignedShort(startOfRecord + lengthOfRecord - 2);
                System.out.println("Calculated Checksum: " + calcChksum + " Checksum: " + chksum);
                if (calcChksum != chksum) {
                    handleCorruptedChecksum(alwaysZero, thisRecordTickNo, offset, actualTickNo, eofProcessing, nextStartOfRecord, sequence);
                }


            } catch (CorruptedException c) {
                if (getPos() > fileLength - 600) {
                    break;
                }
                numCorrupted++;
                if ((numRecs > 1000) && ((float) numCorrupted / (float) numRecs) > 0.02) {
                    throw (new CorruptedException(actualTickNo, startOfRecord));
                }
                try {
                    setPosition(c.filePos);
                    byte fiftyFiveByte = readByte();
                    while (fiftyFiveByte != 0X55) {
                        if (getPos() > fileLength - 1000) {
                            break;
                        }
                        fiftyFiveByte = readByte();
                    }
                } catch (EOFException f) {
                    throw (f);
                } catch (IOException e) {
                    throw (new CorruptedException(actualTickNo, nextStartOfRecord));
                }
                // set position right before the next 0x55
                startOfRecord = getPos() - 1;
            } catch (EOFException f) {
                throw (f);
            } catch (Exception e) {
                throw (new CorruptedException(actualTickNo, startOfRecord));
            }
        }
        return false;
    }

    private boolean handleCorruptedChecksum(int alwaysZero, long thisRecordTickNo, long offset, long actualTickNo, boolean eofProcessing, long nextStartOfRecord, boolean sequence) throws CorruptedException, EOFException {
        numRecs++;
        if (alwaysZero != 0) {
            throw (new CorruptedException(thisRecordTickNo, startOfRecord + 1));
        }

        if (!inRollover && lastRecordTickNo > upperTickLim && thisRecordTickNo < 2225000) {
            prevOffset = presentOffset;
            presentOffset += tickNoBoundary;
            inRollover = true;
            numRolloverRecs = 0;
        }

        offset = presentOffset;
        if (inRollover) {
            numRolloverRecs++;
            if (thisRecordTickNo > upperTickLim) {
                offset = prevOffset;
            }
            if (numRolloverRecs > 100) {
                inRollover = false;
                numRolloverRecs = 0;
            }
        }
        actualTickNo = thisRecordTickNo + offset;
        lastRecordTickNo = thisRecordTickNo;

        // look for large delta in tickNo
        if (Math.abs(lastActualTickNo - actualTickNo) > 22000000) {
            if (eofProcessing && !isTablet() && (fileLength - nextStartOfRecord < 40000)) { // the end of the file is corrupted
//                break;
            }
            // just this record is corrupted
            lastActualTickNo = actualTickNo;
            throw (new CorruptedException(thisRecordTickNo, startOfRecord + 1));
        }

        if (lengthOfRecord == 0) {
            throw (new CorruptedException(actualTickNo, startOfRecord + 1));
        }

        // if nextStartOfRecord not positioned at next 0x55, then this
        // is corrupted, but if it's 0x00 let it be handled by the
        // processing of the next record
        if (getByte(nextStartOfRecord) != 0x55 && getByte(nextStartOfRecord) != 0x00) {
            throw (new CorruptedException(actualTickNo, nextStartOfRecord));
        }

        System.out.println("Seq: " + sequence + " tickNo: " + actualTickNo + " lastTickNo:" + lastActualTickNo);
        if (!sequence || (actualTickNo > lastActualTickNo)) {
            lastActualTickNo = actualTickNo;
            this.type = type;
            payloadLength = lengthOfRecord - headerLength - chksumLength;
            tickNo = actualTickNo;
            start = startOfRecord + headerLength;
            startOfRecord = nextStartOfRecord;
            return true;
        }

        startOfRecord = nextStartOfRecord;

    }

    protected int calcChecksum(MappedByteBuffer memory, long start, short packetLen) {
        int checksum = 0x3692; //  # P3
        for (int i = 0; i < packetLen; i++) {
            int checksumShift = checksum >> 8;
            checksum = checksumShift ^ crc[((memory.get((int) (start + i)) ^ checksum) & 0xFF)];
        }
        return checksum;
    }

    public AnalyzeResultsDAT getResults() {
        return results;
    }


}
