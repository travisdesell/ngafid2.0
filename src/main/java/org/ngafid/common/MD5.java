package org.ngafid.common;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

/**
 * Utility class for computing MD5 hashes.
 *
 * @author Joshua Karns
 */
public class MD5 {
    private static final Logger LOG = Logger.getLogger(MD5.class.getName());

    /**
     * Walks through an input stream and computes the hash over all the bytes. Some of the files we read are larger
     * than the max java array size so we have to read the stream byte by byte. It is unlikely that this will lead to
     * profound performance degradation.
     *
     * @param is
     * @return lowercase hex binary hash string
     * @throws IOException if an exception occurs while reading the stream
     */
    public static String computeHexHash(InputStream is) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestInputStream dis = new DigestInputStream(is, md);

            while (dis.read() != -1) { /* * */ }

            byte[] hash = md.digest();
            return DatatypeConverter.printHexBinary(hash).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            LOG.severe("Unable to find MD5 algorithm");
            e.printStackTrace();
            System.exit(1);
            // Unreachable
            return null;
        }
    }

    /**
     * Compute the MD5 hash for the supplied string.
     *
     * @param s string to hash
     * @return lowercase hex binary hash string
     */
    public static String computeHexHash(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(s.getBytes());
            byte[] hash = md.digest();
            return DatatypeConverter.printHexBinary(hash).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            LOG.severe("Unable to find MD5 algorithm");
            e.printStackTrace();
            System.exit(1);
            // Unreachable
            return null;
        }
    }
}
