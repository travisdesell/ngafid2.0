package org.ngafid.airports;

import java.math.RoundingMode;
import java.text.DecimalFormat;


public class GeoHash {
    private static final int hashDecimals = 2;
    private static DecimalFormat decimalFormat;

    static {
        StringBuilder decimalStr = new StringBuilder();
        for (int i = 0; i < hashDecimals; i++) {
            decimalStr.append("#");
        }

        decimalFormat = new DecimalFormat("###." + decimalStr);
        decimalFormat.setRoundingMode(RoundingMode.HALF_DOWN);
        decimalFormat.setPositivePrefix("+");
    }

    public static String getGeoHash(double latitude, double longitude) {
        return decimalFormat.format(latitude) + decimalFormat.format(longitude);
    }
    

    public static String[] getNearbyGeoHashes(double latitude, double longitude) {
        String latHash = decimalFormat.format(latitude);
        String lonHash = decimalFormat.format(longitude);

        double trimmedLat = Double.parseDouble(latHash);
        double trimmedLon = Double.parseDouble(lonHash);
        double modifier = Math.pow(10, -hashDecimals);

        String[] hashes = new String[9];

        //NW tile
        hashes[0] = decimalFormat.format(trimmedLat + modifier) + decimalFormat.format(trimmedLon - modifier);

        //N tile
        hashes[1] = decimalFormat.format(trimmedLat + modifier) + lonHash;

        //NE tile
        hashes[2] = decimalFormat.format(trimmedLat + modifier) + decimalFormat.format(trimmedLon + modifier);

        //W tile
        hashes[3] = latHash + decimalFormat.format(trimmedLon - modifier);

        //center tile
        hashes[4] = latHash + lonHash;

        //E tile
        hashes[5] = latHash + decimalFormat.format(trimmedLon + modifier);

        //SW tile
        hashes[6] = decimalFormat.format(trimmedLat - modifier) + decimalFormat.format(trimmedLon - modifier);

        //S tile
        hashes[7] = decimalFormat.format(trimmedLat - modifier) + lonHash;

        //SE tile
        hashes[8] = decimalFormat.format(trimmedLat - modifier) + decimalFormat.format(trimmedLon + modifier);

        /*
        System.out.println("Neighboring GeoTags for " + latitude + ", " + longitude);
        for (int i = 0; i < 9; i++) {
            System.out.println("\t" + hashes[i]);
        }
        */

        return hashes;
    }

}
