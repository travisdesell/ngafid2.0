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

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import java.util.Locale;

public class DatFile {

    protected final static int headerLength = 10;

    protected final static int chksumLength = 2;

    protected int startOfRecords = 128;

    public void setStartOfRecords(int startOfRecords) {
        this.startOfRecords = startOfRecords;
    }

    protected MappedByteBuffer memory = null;

    private String acTypeName = "";

    protected long filePos = 0;

    protected File file = null;

    protected FileInputStream inputStream = null;

    protected FileChannel _channel = null;

    protected long fileLength = 0;

    public static String firmwareDate = "";

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

    public static DecimalFormat timeFormat = new DecimalFormat("###.000",
            new DecimalFormatSymbols(Locale.US));

    private static DatFile datFile;

    double clockRate = 600;

    private DATHeader datHeader;

    private HashMap<Integer, RecSpec> recsInDat = new HashMap<Integer, RecSpec>();

    public long _tickNo = 0;

    public int _type = 0;

    public HashMap<Integer, RecSpec> getRecsInDat() {
        return recsInDat;
    }

    public void addRecInDat(int type, int length) {
        Integer key = Integer.valueOf(type);
        if (!recsInDat.containsKey(key)) {
            recsInDat.put(key, new RecSpec(type, length));
        }
    }

    protected void clearRecsInDat() {
        recsInDat.clear();
    }

    public DatFile(String fileName) throws IOException, NotDatFile {
        this(new File(fileName));
    }

    public static DatFile createDatFile(String datFileName)
            throws NotDatFile, IOException {
        byte arra[] = new byte[256];
        //if (true )return (new DatFileV3(datFileName));
        DatConLog.Log(" ");
        DatConLog.Log("createDatFile " + datFileName);
        FileInputStream bfr = new FileInputStream(new File(datFileName));
        bfr.read(arra, 0, 256);
        bfr.close();
        String headerString = new String(arra, 0, 21);
        if (!(headerString.substring(16, 21).equals("BUILD"))) {
            if (Persist.invalidStructOK) {
                DatConLog.Log("createDatFile invalid header - proceeding");
                datFile = new DatFileV3(datFileName);
                datFile.setStartOfRecords(256);
                return datFile;
            }
            if (headerString.substring(0, 4).equals("LOGH")) {
                throw new NotDatFile("Probably an encrypted .DAT");
            }
            throw new NotDatFile();
        }
        if ((new String(arra, 242, 10).equals("DJI_LOG_V3"))) {
            datFile = new DatFileV3(datFileName);
            datFile.setStartOfRecords(256);
        } else {
            datFile = new DatFileV1(datFileName);
            datFile.setStartOfRecords(128);
        }
        return datFile;
    }

    public static boolean isDatFile(String datFileName) {
        byte arra[] = new byte[256];
        try {
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

    public static DatFile createDatFile(String datFileName, final DatCon datCon)
            throws NotDatFile, IOException {
        if (DJIAssistantFile.isDJIDat(new File(datFileName))) {
            if (Persist.autoTransDJIAFiles) {
                int lastSlash = datFileName.lastIndexOf("\\");
                String tempDirName = datFileName.substring(0, lastSlash + 1);
                Color bgColor = datCon.goButton.getBackground();
                Color fgColor = datCon.goButton.getForeground();
                boolean enabled = datCon.goButton.isEnabled();
                String text = datCon.goButton.getText();
                datCon.goButton.setBackground(Color.BLUE);
                datCon.goButton.setForeground(Color.WHITE);
                datCon.goButton.setEnabled(false);
                datCon.goButton.setText("Extracting .DAT");
                try {
                    DatConLog.Log("DJIAssistantFile.extractFirst(" + datFileName
                            + ", " + tempDirName + ")");
                    DJIAssistantFile.ExtractResult result = DJIAssistantFile
                            .extractFirst(datFileName, tempDirName);
                    if (result.moreThanOne()) {
                        //                    if (true) {
                        DatConLog.Log(
                                "DJIAssistantFile.extractFirst:moreThanOne");
                        boolean moreThanOnePopup = DatConPopups
                                .moreThanOne(DatCon.frame);
                        if (moreThanOnePopup) {
                            return new DatFileV3(result.getFile());
                        } else {
                            return null;
                        }
                    } else if (result.none()) {
                        DatConLog.Log("DJIAssistantFile.extractFirst:none");
                        DatConPopups.none(DatCon.frame);
                        return null;
                    }
                    DatConLog.Log("DJIAssistantFile.extractFirst:one");
                    return new DatFileV3(result.getFile());
                } finally {
                    datCon.goButton.setBackground(bgColor);
                    datCon.goButton.setForeground(fgColor);
                    datCon.goButton.setEnabled(enabled);
                    datCon.goButton.setText(text);
                }
            }
        }
        return createDatFile(datFileName);
    }

    public DatFile(File _file) throws NotDatFile, FileNotFoundException {
        datHeader = new DATHeader(this);
        file = _file;
        results = new AnalyzeDatResults();
        fileLength = file.length();
        inputStream = new FileInputStream(file);
        _channel = inputStream.getChannel();
        try {
            memory = _channel.map(FileChannel.MapMode.READ_ONLY, 0, fileLength);
        } catch (IOException e) {
            e.printStackTrace();
        }
        memory.order(ByteOrder.LITTLE_ENDIAN);
        droneModel = datHeader.getAcType();
        //acTypeName = DatHeader.toString(acType);
    }

    public DatFile() {
    }

    public ConvertDat createConVertDat() {
        return (new ConvertDat(this));
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
        System.runFinalization();
    }

    public void reset() throws IOException, FileEnd {
        // tickGroups[0].reset();
        // tickGroups[1].reset();
        // tgIndex = 1;
        numCorrupted = 0;
        results = new AnalyzeDatResults();
        if (inputStream == null) {
            inputStream = new FileInputStream(file);
            _channel = inputStream.getChannel();
            memory = _channel.map(FileChannel.MapMode.READ_ONLY, 0, fileLength);
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
        _channel.position(filePos);
    }

    public String toString() {
        return file.getName();
    }

    public void setPosition(final long pos) throws FileEnd, IOException {
        filePos = pos;
        if (filePos > fileLength)
            throw (new FileEnd());
        _channel.position(pos);
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
        return (int) (0xff & memory.get((int) filePos))
                + 256 * (int) (0xff & memory.get((int) (filePos + 1)));
    }

    protected int getUnsignedShort(long fp) throws FileEnd {
        if (fp > fileLength - 2)
            throw (new FileEnd());
        return (int) (0xff & memory.get((int) fp))
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

    public void preAnalyze() throws NotDatFile {
        switch (droneModel) {
        case I1:
            case M600:
            case I2:
            case M100:
            case M200:
                numBattCells = 6;
            break;
            case MavicPro:
            case MavicAir:
            case SPARK:
                numBattCells = 3;
            break;
            case P3AP:
            case P3S:
            case P4:
            case P4A:
            case P4P:
                numBattCells = 4;
            break;
            case UNKNOWN:
            numBattCells = 4;
            DatConLog.Log("Assuming 4 cellls per battery");
            break;
        default:
            numBattCells = 4;
            DatConLog.Log("Assuming 4 cellls per battery");
            break;
        }
    }

    public int getNumBattCells() {
        return numBattCells;
    }

    public String getFirmwareDate() {
        return datHeader.getFWDate();
    }

    public String getACTypeString() {
        if (droneModel != DATHeader.DroneModel.UNKNOWN) {
            return droneModel.toString();
        }
        return acTypeName;
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
            DatConLog.Log(tst.getDescription() + " Type " + tst.getId());
        }
    }

    public boolean isTablet() {
        return false;
    }

    public AcType getACType() {
        return droneModel;
    }

    public double getPercentCorrupted() {
        return (((double) numCorrupted) / ((double) numRecs)) / 100.0;
    }

    public double getErrorRatio(Type _type) {
        switch (_type) {
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
