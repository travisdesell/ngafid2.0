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

    private static byte[] inflate(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InflaterOutputStream outputStream = new InflaterOutputStream(baos);
        outputStream.write(data);
        outputStream.finish();

        return baos.toByteArray();
    }

    public static byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DeflaterOutputStream deflaterOutputStream =
                new DeflaterOutputStream(baos, new Deflater(Compression.COMPRESSION_LEVEL));
        deflaterOutputStream.write(data);
        deflaterOutputStream.finish();

        return baos.toByteArray();
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

    public static byte[] compressDoubleArray(double[] data, int size) throws IOException {
        ByteBuffer bytes = ByteBuffer.allocate(data.length * Double.BYTES);
        bytes.asDoubleBuffer().put(data);
        return compress(bytes.array());
    }

    public static Object inflateObject(byte[] bytes, int size) throws IOException, ClassNotFoundException {
        byte[] inflated = inflate(bytes);

        // Deserialize
        ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(inflated));
        Object o = inputStream.readObject();
        inputStream.close();

        return o;
    }

    public static byte[] compressObject(Object o) throws SQLException, IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        final ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(bout);
            oos.writeObject(o);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        byte[] bytes = bout.toByteArray();
        bout.close();

        return compress(bytes);
    }
}
