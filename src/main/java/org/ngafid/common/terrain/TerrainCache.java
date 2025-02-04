package org.ngafid.common.terrain;

import java.nio.file.NoSuchFileException;


public final class TerrainCache {

    private static final SRTMTile[][] TILES = new SRTMTile[180][360];
    public static final String TERRAIN_DIRECTORY;

    static {
        // TERRAIN_DIRECTORY = "/Users/fa3019/Data/terrain/";
        if (System.getenv("TERRAIN_DIRECTORY") == null) {
            System.err.println("ERROR: 'TERRAIN_DIRECTORY' environment variable not specified at runtime.");
            System.err.println("Please add the following to your ~/.bash_rc or ~/.profile file:");
            System.err.println("export TERRAIN_DIRECTORY=<path_to_terrain_data>");
            System.exit(1);
        }

        TERRAIN_DIRECTORY = System.getenv("TERRAIN_DIRECTORY");
        //TERRAIN_DIRECTORY = "/Users/fa3019/Data/terrain/";
    }

    private TerrainCache() {
        throw new UnsupportedOperationException("Utility class");
    }


    //each directory contains a 4 by 6 grid of files, 4 latitudes worth and 4 longitudes worth
    //the equator starts at A and goes north alphabetically, and at SA and goes south alphabetically (SA, SB, SC)...
    //numbers start at 01, which corresponds to W180 ... W175, 02 is W174 ... 169, etc
    public static String getDirectoryFromLatLon(int latitude, int longitude) {
        String directory = "";
        if (latitude < 0) {
            directory += 'S';
            latitude *= -1;
        }

        int ilatitude = latitude / 4;
        int ilongitude = longitude + 180;
        ilongitude /= 6;
        ilongitude += 1;

        // System.out.println("iLatitude: " + ilatitude + ", iLongitude: " + ilongitude);

        //note that ascii 65 == 'A'
        directory += Character.toString((char) (65 + ilatitude)) + ilongitude;

        return directory;
    }

    public static String getFilenameFromLatLon(int latitude, int longitude) {
        String ns = "N";
        if (latitude < 0) ns = "S";

        String ew = "E";
        if (longitude < 0) ew = "W";

        int ilatitude = Math.abs(latitude);
        int ilongitude = Math.abs(longitude);

        StringBuilder strLongitude = new StringBuilder(Integer.toString(ilongitude));
        while (strLongitude.length() < 3) strLongitude.insert(0, "0");

        return ns + ilatitude + ew + strLongitude + ".hgt";
    }

    public static int getAltitudeFt(double msl, double latitude, double longitude) throws NoSuchFileException, TerrainUnavailableException {
        //cout << "getting tile for latitude: " << latitude << " and longitude: " << longitude << endl;
        int latIndex = -((int) Math.ceil(latitude) - 91);
        int lonIndex = (int) Math.floor(longitude) + 180;

        //System.out.println("lat_index: " + latIndex + ", lon_index: " + lonIndex);

        if (latIndex < 0 || lonIndex < 0 || latIndex >= 180 || lonIndex >= 360) {
            System.err.println("ERROR: getting tile for latitude: " + latitude + " and longitude: " + longitude);
            System.err.println("tile[" + latIndex + "][" + lonIndex + "] does not exist!");
            System.err.println("latitude should be >= -90 and <= 90");
            System.err.println("longitude should be >= -180 and <= 180");
            throw new TerrainUnavailableException("There is no tile latitude: " + latitude + " and longitude: " + longitude);
        }

        SRTMTile tile = TILES[latIndex][lonIndex];

        if (tile == null) {
            // System.out.println("tiles[" + latIndex + "][" + lonIndex + "] not initialized, loading!");
            tile = new SRTMTile(90 - latIndex, lonIndex - 180);
            TILES[latIndex][lonIndex] = tile;
        }

        double altitudeFt = tile.getAltitudeFt(latitude, longitude);

        //System.out.println("msl: " + msl + ", terrain: " + altitudeFt + ", agl: " + Math.max(0.0, msl - altitudeFt));
        //return (int)altitudeFt;
        return (int) Math.max(0, msl - altitudeFt);

    }

    public static void main(String[] arguments) throws NoSuchFileException, TerrainUnavailableException {
        //albany airport - should be 267 ft
        double test = getAltitudeFt(0, 42.74871, -73.80550);
        System.out.println("albany airport altitude: " + test + ", should be 267 ft");
        test = getAltitudeFt(0, 42.74911111094814, -73.80197222206861);
        System.out.println("albany airport (2) altitude: " + test + ", should be 267 ft");

        //grand forks airport - should be 838 ft
        test = getAltitudeFt(0, 47.94286, -97.17658);
        System.out.println("grand forks airport altitude: " + test + ", should be 838 ft");
        test = getAltitudeFt(0, 47.94727777751542, -97.17377777794896);
        System.out.println("grand forks airport (2) altitude: " + test + ", should be 838 ft");

        //denver airport - should be 5373 ft
        test = getAltitudeFt(0, 39.85610, -104.67374);
        System.out.println("denver airport altitude: " + test + ", should be 5373 ft");
        test = getAltitudeFt(0, 39.86166666671349, -104.67316666709075);
        System.out.println("denver airport (2) altitude: " + test + ", should be 5373 ft");

        //rochester airport - should be 542 ft
        test = getAltitudeFt(0, 43.12252, -77.66657);
        System.out.println("rochester airport altitude: " + test + ", should be 542 ft");

        //phoenix airport - should be 1124 ft
        test = getAltitudeFt(0, 33.43727, -112.00779);
        System.out.println("phoenix airport altitude: " + test + ", should be 1124 ft");

        /*
        double endLat = 42.85;
        int latDim = 3000;

        double startLon = -73.9;
        double endLon = -73.7;
        int lonDim = 3000;
        */

        double startLat = 36.1;
        double endLat = 40.1;
        int latDim = 6000;

        double startLon = -80.0;
        double endLon = -76.0;
        int lonDim = 7500;

        /*
        double startLat = 36.75;
        double endLat = 38.25;
        int latDim = 1201;

        double startLon = -76.75;
        double endLon = -78.25;
        int lonDim = 1201;
        */

        /*
        BufferedImage image = new BufferedImage(lonDim, latDim, BufferedImage.TYPE_INT_RGB);

        int max = 0;
        for (int x = 0; x < lonDim; x++) {
            for (int y = 0; y < latDim; y++) {
                double lon = startLon + (((endLon - startLon) / lonDim) * x);
                double lat = startLat + (((endLat - startLat) / latDim) * y);
                int altitude = (int)getAltitudeFt(0, lat, lon);

                if (altitude > max) max = altitude;
                altitude /= 10;
                //altitude *= 2;

                try {
                    if (altitude > 255) altitude = 255;

                    image.setRGB(x, y, new Color(altitude, altitude, altitude).getRGB());
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("altitude: " + altitude);
                    System.exit(1);
                }
            }
        }
        System.out.println("max altitude: " + max);

        File imageFile = new File("output.png");
        try {
            ImageIO.write(image, "png", imageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        */

    }

}

