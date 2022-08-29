package org.ngafid.flights.dji;

import java.io.EOFException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Payload {

    byte xorArray[] = null;

    ByteBuffer byteBuffer = null;

    int length = 0;

    public long tickNo = 0;

    public long start = 0;

    int type = 0;

    public DATDJIFile datFile = null;

    public Payload(DATDJIFile datFile, long start, int length, int type, long tickNo) throws IOException {
        this.datFile = datFile;
        this.start = start;
        this.length = length;
        this.tickNo = tickNo;
        this.type = type;
        if (this.start + this.length - 1 >= this.datFile.getLength()) {
            throw new EOFException();
        }
        this.xorArray = new byte[this.length];
        this.byteBuffer = ByteBuffer.wrap(xorArray).order(ByteOrder.LITTLE_ENDIAN);

        byte xorKey = (byte) (this.tickNo % 256);
        for (int i = 0; i < this.length; i++) {
            xorArray[i] = (byte) (this.datFile.getByte(this.start + i) ^ xorKey);
        }
    }

    public long getStart() {
        return start;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public int getType() {
        return type;
    }

    public void printBB(PrintStream printStream) {
        printStream.print("Rec" + "_" + type + " ");
        printStream.print("FilePos = " + start + " ");
        if (tickNo >= 0) printStream.print("TickNo = " + tickNo);
        printStream.println("");
        for (int i = 0; i < length; i++) {
            printStream.print(i + ":" + String.format("%02X", (0xff & byteBuffer.get(i))) + ":" + String.format("%C", (0xff & byteBuffer.get(i))) + ":" + (0xff & byteBuffer.get(i)));
            if (i < length - 1) {
                printStream.print(":Shrt " + byteBuffer.getShort(i) + " :UShrt " + getUnsignedShort(i));
            }
            if (i < length - 3) {
                printStream.print(" :I " + byteBuffer.getInt(i) + " :UI " + getUnsignedInt(i) + " :F " + byteBuffer.getFloat(i));
            }
            if (i < length - 7) {
                printStream.print(" :L " + byteBuffer.getLong(i) + " :D " + byteBuffer.getDouble(i));
            }
            printStream.println("");
        }
    }

    public void printBBAsString(PrintStream printStream) {
        for (int i = 0; i < length; i++) {
            printStream.printf("%c", (0xff & byteBuffer.get(i)));
        }
        printStream.println("");
    }

    public void printBBAsBytes(PrintStream printStream) {
        for (int i = 0; i < length; i++) {
            printStream.print(": " + String.format("%02X", (0xff & byteBuffer.get(i))));
        }
        printStream.println("");
    }

    public void printBBAsShort(PrintStream printStream) {
        for (int i = 0; i < length; i++) {
            printStream.print(":" + (0xff & byteBuffer.get(i)));
        }
        printStream.println("");
    }

    public short getByte(int index) {
        return byteBuffer.get(index);
    }

    public short getUnsignedByte(int index) {
        return (short) (0xff & byteBuffer.get(index));
    }

    public long getUnsignedInt(int index) {
        return (long) (0xff & byteBuffer.get(index)) + (256 * (long) (0xff & byteBuffer.get(index + 1))) + (65536 * (long) (0xff & byteBuffer.get(index + 2))) + (65536 * 256 * (long) (0xff & byteBuffer.get(index + 3)));
    }

    public int getUnsignedShort(int index) {
        return (int) (0xff & byteBuffer.get(index)) + (256 * (int) (0xff & byteBuffer.get(index + 1)));
    }

    public float getFloat(int index) {
        return byteBuffer.getFloat(index);
    }

    public short getShort(int index) {
        return byteBuffer.getShort(index);
    }

    public int getInt(int index) {
        return byteBuffer.getInt(index);
    }

    public double getDouble(int index) {
        return byteBuffer.getDouble(index);
    }

    public long getTickNo() {
        return tickNo;
    }

    public void lookforQuat() {
        //        if (recType == 44 || recType == 207 || recType == 225 || recType == 187)
        //            return;
        for (int i = 0; i < length - 16; i++) {
            double X1 = byteBuffer.getFloat(i);
            double X2 = byteBuffer.getFloat(i + 4);
            double X3 = byteBuffer.getFloat(i + 8);
            double X4 = byteBuffer.getFloat(i + 12);
            if (X1 != 0.0 && X2 != 0.0 && X3 != 0.0 && X4 != 0.0) {
                double test = Math.sqrt(X1 * X1 + X2 * X2 + X3 * X3 + X4 * X4);
                if (Math.abs(test - 1.0) < 1.0E-2) {
                    System.out.print("Rec" + "_" + type);
                    System.out.print("TickNo = " + tickNo);
                    System.out.print(":offset " + i);
                    Quaternion q = new Quaternion(X1, X2, X3, X4);
                    System.out.println(" RPW " + (new RollPitchYaw(q)).toDegString());
                }
            }
        }
    }

    public String getString() {
        byte bytes[] = new byte[length];
        int l = 0;
        byte B;
        for (int i = 0; i < length; i++) {
            B = byteBuffer.get(i);
            if (B == 0x00) {
                l = i;
                break;
            }
            bytes[i] = B;
        }
        return new String(bytes, 0, l);
    }

    public String getAsString() {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = byteBuffer.get(i);
        }
        String retv = new String(bytes, 0, length - 1);
        return retv;
    }

    public String getCleanString() {
        byte[] bytes = new byte[length];
        int l = 0;
        byte B = 0x00;
        for (int i = 0; i < length; i++) {
            B = byteBuffer.get(i);
            if (B == 0x00) {
                l = i;
                break;
            } else if (B >= 0x20) {
                bytes[l++] = B;
            } else if (B == '\r' || B == '\n') {
                bytes[l++] = '|';
            } else if (B == '\t') {
                bytes[l++] = ' ';
            } else {
                bytes[l++] = '.';
            }
        }
        return new String(bytes, 0, l);
    }

    public String getStrings() {
        byte[] bytes = new byte[length];
        int l = 0;
        byte B = 0x00;
        for (int i = 0; i < length; i++) {
            B = byteBuffer.get(i);
            if (B >= 0x20 && B <= 0x7F) {
                bytes[l++] = B;
            } else if (B == 0x00 || B == '\r' || B == '\n') {
                bytes[l++] = '|';
            } else {
                bytes[l++] = '.';
            }
        }
        return new String(bytes, 0, l);
    }

    public void lookForString() {
        int runLength = 0;
        for (int i = 0; i < length; i++) {
            byte b = byteBuffer.get(i);
            if (0x20 <= b && b <= 0x7E) {
                runLength++;
            } else {
                if (runLength > 8) {
                    for (int j = i - runLength; j < i; j++) {
                        System.out.printf("%C", (0xff & byteBuffer.get(j)));
                    }
                    System.out.println();
                }
                runLength = 0;
            }
        }
    }

    public void lookForLatLong() {
        for (int i = 0; i < length - 16; i++) {
            double number1 = byteBuffer.getDouble(i);
            double number2 = byteBuffer.getDouble(i + 8);
            if ((((0.8 < number1 && number1 < .81) && (-1.3 < number2 && number2 < -1.2)) || ((0.8 < number2 && number2 < .81) && (-1.3 < number1 && number1 < -1.2)))) {
                System.out.print("Rec" + "_" + type + ":");
                System.out.print("Offset " + i + " ");
                System.out.print("FilePos = " + start + " ");
                if (tickNo >= 0) System.out.print("TickNo = " + tickNo);
                System.out.println(" " + number1 + " " + number2);
            }
        }
    }

    public void lookForLatLong(double latRad, double longRad) {
        for (int i = 0; i < length - 16; i++) {
            double number1 = byteBuffer.getDouble(i);
            double number2 = byteBuffer.getDouble(i + 8);
            if ((Math.abs(number2) > 0.01) && (((Math.abs(number1 - latRad) < 0.05) && (Math.abs(number2 - longRad) < 0.05)) || ((Math.abs(number1 - longRad) < 0.05) && (Math.abs(number2 - latRad) < 0.05)))) {
                System.out.print("Rec" + "_" + type + ":");
                System.out.print("Offset " + i + " ");
                System.out.print("FilePos = " + start + " ");
                if (tickNo >= 0) System.out.print("TickNo = " + tickNo);
                System.out.println(" " + number1 + " " + number2 + " lat " + latRad + " long " + longRad);
            }
        }
    }

    public void lookForLatLong(long latDeg, long longDeg) {
        for (int i = 0; i < length - 16; i++) {
            long number1 = byteBuffer.getInt(i);
            long number2 = byteBuffer.getInt(i + 4);

            if ((Math.abs(number1) > 10000000) && (Math.abs(number2) > 10000000) && (((Math.abs(number1 - latDeg) < 10000000) && (Math.abs(number2 - longDeg) < 10000000)) || ((Math.abs(number1 - longDeg) < 10000000) && (Math.abs(number2 - latDeg) < 10000000)))) {
                System.out.print("Rec" + "_" + type + ":");
                System.out.print("Offset " + i + " ");
                System.out.print("FilePos = " + start + " ");
                if (tickNo >= 0) System.out.print("TickNo = " + tickNo);
                System.out.println(" " + number1 + " " + number2 + " lat " + latDeg + " long " + longDeg);
            }
        }
    }

    public int getLength() {
        return length;
    }

}
