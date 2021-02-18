package org.ngafid.common;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.zip.*;

public class Compression {

    private static Logger LOG = Logger.getLogger(Compression.class.getName());

    private static final int COMPRESSION_LEVEL = Deflater.DEFAULT_COMPRESSION;
    private static final boolean NOWRAP = false;

    private static byte[] inflate(byte[] data) throws IOException {
        for (int i = 0; i < 4; i++) {
            System.out.printf("0x%02x\n", data[i]);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Inflater inflater = new Inflater(NOWRAP);
        InflaterOutputStream outputStream = new InflaterOutputStream(baos, inflater);
        outputStream.write(data);
        outputStream.finish();

        byte[] out = baos.toByteArray();

        outputStream.close();

        return out;
    }

    public static byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Deflater deflater = new Deflater(Compression.COMPRESSION_LEVEL, NOWRAP);
        DeflaterOutputStream deflaterOutputStream =
                new DeflaterOutputStream(baos, deflater);
        deflaterOutputStream.write(data);
        deflaterOutputStream.finish();

        byte[] out = baos.toByteArray();

        deflaterOutputStream.close();


        return out;
    }

    public static double[] inflateDoubleArray(byte[] bytes, int size) throws IOException {
        byte[] inflated = inflate(bytes);
        double[] output = new double[size];
        ByteBuffer.wrap(inflated).asDoubleBuffer().get(output);

        return output;
    }

    public static double[] inflateDoubleArray(Blob blob, int size) throws SQLException, IOException {
        byte[] bytes = blob.getBytes(1, (int) blob.length());
        return inflateDoubleArray(bytes, size);
    }

    public static byte[] compressDoubleArray(double[] data) throws IOException {
        ByteBuffer bytes = ByteBuffer.allocate(data.length * Double.BYTES);
        bytes.asDoubleBuffer().put(data);
        return compress(bytes.array());
    }

    public static Object inflateObject(byte[] bytes) throws IOException, ClassNotFoundException {
        byte[] inflated = inflate(bytes);

        // Deserialize
        ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(inflated));
        Object o = inputStream.readObject();
        inputStream.close();

        return o;
    }

    public static byte[] compressObject(Object o) throws SQLException, IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        final ObjectOutputStream oos = new ObjectOutputStream(bout);
        oos.writeObject(o);
        oos.close();

        byte[] bytes = bout.toByteArray();
        bout.close();

        return compress(bytes);
    }
}
