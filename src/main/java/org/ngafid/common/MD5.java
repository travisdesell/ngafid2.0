package org.ngafid.common;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

public class MD5 {
    private static final Logger LOG = Logger.getLogger(MD5.class.getName());

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
