package org.ngafid.flights.DJIBinary;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;


public class DATDJIFile {

    public enum DroneModel {
        I1, I2,
        S900, SPARK,
        P3I1, P3AP, P3S,
        P4, P4A, P4P,
        M200, M600, M100,
        MavicPro, MavicAir, UNKNOWN
    }

    private final static int HEADER_LEN = 10;
    private final static int CRC_LEN = 2;
    private int recordStart = 128;
    private MappedByteBuffer memory = null;
    private String modelName = "";
    private File file = null;
    private FileInputStream inputStream = null;
    private FileChannel channel = null;
    private long fileLen = 0;
    private String firmwareData = "";
    private int numCorrupted = 0;
    private long numRecs = 0;
    private DroneModel model;
    private final FileChannel inputStreamChannel;


    public DATDJIFile(File file) throws FileNotFoundException {
        this.file = file;
        this.fileLen = file.length();
        this.inputStream = new FileInputStream(file);
        this.inputStreamChannel = inputStream.getChannel();

        try {
            this.memory = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileLen);
        } catch (IOException e) {}

        this.memory.order(ByteOrder.LITTLE_ENDIAN);
        // TODO: Figure out header situation and getting model type
    }


    public void setRecordStart(int recordStart) {
        this.recordStart = recordStart;
    }


    public DroneModel getModel() {
        return model;
    }
}
