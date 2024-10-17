package org.ngafid.common;

import org.apache.fury.*;
import org.apache.fury.config.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.zip.*;

import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.StringTimeSeries;
import org.ngafid.flights.calculations.TurnToFinal;

public class Compression {
    private static Logger LOG = Logger.getLogger(Compression.class.getName());

    private static int COMPRESSION_LEVEL = 2;
    private static final boolean NOWRAP = false;

    public static int USE_NEW_COMPRESSION = 1;

    static {
        String compressionStrategy = System.getenv("COMPRESSION_STRATEGY");
        if (compressionStrategy == null || compressionStrategy.toLowerCase().equals("new")) {
            USE_NEW_COMPRESSION = 1;
        } else if (compressionStrategy.toLowerCase().equals("old")) {
            USE_NEW_COMPRESSION = 0;
        }
        
        String compressionLevelString = System.getenv("COMPRESSION_LEVEL");
        if (compressionLevelString != null) {
            try {
                int x = Integer.parseInt(compressionLevelString);
                COMPRESSION_LEVEL = x;
            } catch (NumberFormatException e) {
                if (compressionLevelString.toLowerCase().equals("default")) {
                    COMPRESSION_LEVEL = Deflater.DEFAULT_COMPRESSION;
                }
            }
        }
    }

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

    private static record SerializedObject(byte[] data, byte[] compressed) {} 

    private static ThreadSafeFury FURY = new ThreadLocalFury(classLoader -> {
        Fury f = Fury.builder().withLanguage(Language.JAVA)
            .withClassLoader(classLoader)
            .withIntCompressed(true)    
            .build();
        f.register(SerializedObject.class);
        f.register(DoubleTimeSeries.class);
        f.register(StringTimeSeries.class);
        f.register(StringTimeSeries.DenseStringSeriesData.class);
        f.register(StringTimeSeries.SparseStringSeriesData.class);
        f.register(TurnToFinal.class);
        return f;
    });

    public static Object inflateObject(byte[] bytes) throws IOException, ClassNotFoundException {
        if (USE_NEW_COMPRESSION == 1) {
            SerializedObject o = (SerializedObject) FURY.deserialize(bytes);
            if (o.data != null) {
                return FURY.deserialize(o.data);
            } else {
                return FURY.deserialize(inflate(o.compressed));
            }
        } else {
            return inflateObjectOld(bytes);
        }
    }

    public static byte[] compressObject(Object o) throws IOException {
        if (USE_NEW_COMPRESSION == 1) {
            byte[] data = FURY.serialize(o);
            byte[] compressed = compress(data);

            if (data.length < compressed.length) {
                return FURY.serialize(new SerializedObject(data, null));
            } else {
                return FURY.serialize(new SerializedObject(null, compressed));
            }
        } else {
            return compressObjectOld(o);
        }
    }
    private static Object inflateObjectOld(byte[] bytes) throws IOException, ClassNotFoundException {
        byte[] inflated = inflate(bytes);

        // Deserialize
        ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(inflated));
        Object o = inputStream.readObject();
        inputStream.close();

        return o;
    }

    private static byte[] compressObjectOld(Object o) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        final ObjectOutputStream oos = new ObjectOutputStream(bout);
        oos.writeObject(o);
        oos.close();

        byte[] bytes = bout.toByteArray();
        bout.close();

        return compress(bytes);
    }
}
