package org.ngafid.core.util;

import java.io.*;
import java.nio.ByteBuffer;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

/**
 * Utility for deflate-compressing and inflating byte arrays and SQL {@link java.sql.Blob}s.
 *
 * <p>Used to store and retrieve compressed flight time-series data; all members are static and the class is not
 * instantiable.
 */
public final class Compression {
    private Compression() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated.");
    }

    private static Logger LOG = Logger.getLogger(Compression.class.getName());

    private static final int COMPRESSION_LEVEL = Deflater.DEFAULT_COMPRESSION;
    private static final boolean NOWRAP = false;

    private static byte[] inflate(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Inflater inflater = new Inflater(NOWRAP);
        InflaterOutputStream outputStream = new InflaterOutputStream(baos, inflater);
        outputStream.write(data);
        outputStream.finish();

        byte[] out = baos.toByteArray();

        outputStream.close();

        return out;
    }

    /**
     * Compresses a byte array using raw (nowrap) DEFLATE at the configured compression level.
     *
     * @param data the bytes to compress
     * @return the compressed bytes
     * @throws IOException if compression fails
     */
    public static byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Deflater deflater = new Deflater(Compression.COMPRESSION_LEVEL, NOWRAP);
        DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(baos, deflater);
        deflaterOutputStream.write(data);
        deflaterOutputStream.finish();

        byte[] out = baos.toByteArray();

        deflaterOutputStream.close();

        return out;
    }

    /**
     * Decompresses the given bytes and interprets them as a {@code double[]} of the given length.
     *
     * @param bytes the compressed bytes
     * @param size the expected number of doubles
     * @return the decompressed double array
     * @throws IOException if decompression fails
     */
    public static double[] inflateDoubleArray(byte[] bytes, int size) throws IOException {
        byte[] inflated = inflate(bytes);
        double[] output = new double[size];
        ByteBuffer.wrap(inflated).asDoubleBuffer().get(output);

        return output;
    }

    /**
     * Reads a SQL {@link Blob}'s bytes and decompresses them into a {@code double[]} of the given length.
     *
     * @param blob the blob holding the compressed doubles
     * @param size the expected number of doubles
     * @return the decompressed double array
     * @throws SQLException if reading the blob fails
     * @throws IOException if decompression fails
     */
    public static double[] inflateDoubleArray(Blob blob, int size) throws SQLException, IOException {
        byte[] bytes = blob.getBytes(1, (int) blob.length());
        return inflateDoubleArray(bytes, size);
    }

    /**
     * Serializes a {@code double[]} to its raw little-/big-endian byte form and compresses it (used to store a
     * double time series as a blob).
     *
     * @param data the doubles to compress
     * @return the compressed bytes
     * @throws IOException if compression fails
     */
    public static byte[] compressDoubleArray(double[] data) throws IOException {
        ByteBuffer bytes = ByteBuffer.allocate(data.length * Double.BYTES);
        bytes.asDoubleBuffer().put(data);
        return compress(bytes.array());
    }

    /**
     * Decompresses and Java-deserializes an object that was stored by an older turn-to-final code path.
     *
     * @param bytes the compressed, serialized object bytes
     * @return the deserialized object
     * @throws IOException if decompression or deserialization fails
     * @throws ClassNotFoundException if the serialized class cannot be found
     */
    public static Object inflateTTFObject(byte[] bytes) throws IOException, ClassNotFoundException {
        byte[] inflated = inflate(bytes);

        // Deserialize
        // Use the custom TTFFixObjectInputStream which will properly recognize
        // the outdated reference to the turn to final class
        // new ObjectInputStream(new ByteArrayInputStream(inflated));
        ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(inflated));
        Object o = inputStream.readObject();
        inputStream.close();

        return o;
    }

    /**
     * Decompresses and Java-deserializes an object (used to read a blob-stored object such as a string time series).
     *
     * @param bytes the compressed, serialized object bytes
     * @return the deserialized object
     * @throws IOException if decompression or deserialization fails
     * @throws ClassNotFoundException if the serialized class cannot be found
     */
    public static Object inflateObject(byte[] bytes) throws IOException, ClassNotFoundException {
        byte[] inflated = inflate(bytes);

        // Deserialize
        ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(inflated));
        Object o = inputStream.readObject();
        inputStream.close();

        return o;
    }

    /**
     * Java-serializes an object and compresses the result (used to store an object such as a string time series as a
     * blob).
     *
     * @param o the object to serialize and compress
     * @return the compressed, serialized bytes
     * @throws SQLException if preparing the data for storage fails
     * @throws IOException if serialization or compression fails
     */
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
