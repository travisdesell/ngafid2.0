package org.ngafid.common.airports;

import ch.randelshofer.fastdoubleparser.JavaDoubleParser;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public final class GeoHash {
    private static final int HASH_DECIMALS = 2;
    private static final DecimalFormat DECIMAL_FORMAT;

    private GeoHash() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated.");
    }

    static {
        StringBuilder decimalStr = new StringBuilder();
        decimalStr.append("#".repeat(HASH_DECIMALS));

        DECIMAL_FORMAT = new DecimalFormat("###." + decimalStr);
        DECIMAL_FORMAT.setRoundingMode(RoundingMode.HALF_DOWN);
        DECIMAL_FORMAT.setPositivePrefix("+");
    }

    public static String getGeoHash(double latitude, double longitude) {
        return DECIMAL_FORMAT.format(latitude) + DECIMAL_FORMAT.format(longitude);
    }


    public static String[] getNearbyGeoHashes(double latitude, double longitude) {
        String latHash = DECIMAL_FORMAT.format(latitude);
        String lonHash = DECIMAL_FORMAT.format(longitude);

        double trimmedLat = JavaDoubleParser.parseDouble(latHash);
        double trimmedLon = JavaDoubleParser.parseDouble(lonHash);
        double modifier = Math.pow(10, -HASH_DECIMALS);

        String[] hashes = new String[9];

        //NW tile
        hashes[0] = DECIMAL_FORMAT.format(trimmedLat + modifier) + DECIMAL_FORMAT.format(trimmedLon - modifier);

        //N tile
        hashes[1] = DECIMAL_FORMAT.format(trimmedLat + modifier) + lonHash;

        //NE tile
        hashes[2] = DECIMAL_FORMAT.format(trimmedLat + modifier) + DECIMAL_FORMAT.format(trimmedLon + modifier);

        //W tile
        hashes[3] = latHash + DECIMAL_FORMAT.format(trimmedLon - modifier);

        //center tile
        hashes[4] = latHash + lonHash;

        //E tile
        hashes[5] = latHash + DECIMAL_FORMAT.format(trimmedLon + modifier);

        //SW tile
        hashes[6] = DECIMAL_FORMAT.format(trimmedLat - modifier) + DECIMAL_FORMAT.format(trimmedLon - modifier);

        //S tile
        hashes[7] = DECIMAL_FORMAT.format(trimmedLat - modifier) + lonHash;

        //SE tile
        hashes[8] = DECIMAL_FORMAT.format(trimmedLat - modifier) + DECIMAL_FORMAT.format(trimmedLon + modifier);

        /*
        System.out.println("Neighboring GeoTags for " + latitude + ", " + longitude);
        for (int i = 0; i < 9; i++) {
            System.out.println("\t" + hashes[i]);
        }
        */

        return hashes;
    }

}
