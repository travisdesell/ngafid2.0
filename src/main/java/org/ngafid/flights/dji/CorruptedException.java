package org.ngafid.flights.dji;

public class CorruptedException extends Exception {
    private static final long serialVersionUID = 1L;

    public enum Type {
        CRC, Other
    }

    Type type;

    static int numCRC = 0;

    static int numOther = 0;

    public static void reset() {
        numCRC = 0;
        numOther = 0;
    }

    long tickNo = 0;

    public long filePos = 0;

    public CorruptedException(long _tickNo, long _filePos) {
        tickNo = _tickNo;
        filePos = _filePos;
        type = Type.Other;
        numOther++;
    }

    public CorruptedException(long _tickNo, long _filePos, Type _type) {
        tickNo = _tickNo;
        filePos = _filePos;
        type = _type;
        if (type == Type.CRC) {
            numCRC++;
        } else {
            numOther++;
        }
    }

    public static int getNum(Type _type) {
        switch (_type) {
        case CRC:
            return numCRC;
        case Other:
            return numOther;
        default:
            break;
        }
        return 0;
    }

    public CorruptedException() {

    }

    public String toString() {
        return "Partial or missing record at or near tickNo " + tickNo
                + ", file Position " + filePos;
    }

    public long getFilePos() {
        return filePos;
    }
}
