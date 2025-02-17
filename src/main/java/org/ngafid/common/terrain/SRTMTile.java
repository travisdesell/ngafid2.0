package org.ngafid.common.terrain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class SRTMTile {
    private static final Logger LOG = Logger.getLogger(SRTMTile.class.getName());

    public static final int SRTM_TILE_SIZE = 1201;
    public static final double SRTM_GRID_SIZE = 1.0 / (SRTM_TILE_SIZE - 1.0);

    private final String directory;
    private final String filename;

    private final int latitudeS;
    private final int longitudeW;

    private byte[] bytes;

    private final int[][] altitudesFt;

    public SRTMTile(int latitudeS, int longitudeW) throws NoSuchFileException {
        this.latitudeS = latitudeS;
        this.longitudeW = longitudeW;

        directory = TerrainCache.getDirectoryFromLatLon(latitudeS, longitudeW);
        filename = TerrainCache.getFilenameFromLatLon(latitudeS, longitudeW);

        // LOG.info("loading terrain from: '" + directory + "/" + filename + "'");
        // LOG.info("lat and lon for SW corner -- latitude_s: " + latitudeS + ", longitude_w: " + longitudeW);

        Path path = Paths.get(TerrainCache.TERRAIN_DIRECTORY + "/" + directory + "/" + filename);
        //Path path = Paths.get(TerrainCache.getTerrainDirectory() + "/" + filename);

        bytes = null;
        try {
            bytes = Files.readAllBytes(path);
        } catch (NoSuchFileException e) {
            throw e;
        } catch (IOException e) {
            System.err.println("Error reading all bytes from file: '" + path + "'");
            e.printStackTrace();
            System.exit(1);
        }

        //file currents in the northwest corner
        int altitudeM;

        int offset = 0;

        altitudesFt = new int[SRTM_TILE_SIZE][SRTM_TILE_SIZE];

        int max = 0;
        int min = 5000;
        int altititudeFt;

        for (int y = 0; y < SRTM_TILE_SIZE; y++) {
            for (int x = 0; x < SRTM_TILE_SIZE; x++) {
                altitudeM = ((bytes[offset] & 0xff) << 8) | (bytes[offset + 1] & 0xff);

                altititudeFt = (int) ((double) altitudeM * 3.2808399);
                //altitudesFt[x][y] = altititudeFt;
                //altitudesFt[1200 - x][y] = altititudeFt;
                //altitudesFt[x][1200 - y] = altititudeFt; //close-ish
                //altitudesFt[1200 - x][1200 - y] = altititudeFt;

                altitudesFt[y][x] = altititudeFt;
                //altitudesFt[1200 - y][x] = altititudeFt;
                //altitudesFt[y][1200 - x] = altititudeFt;
                //altitudesFt[1200 - y][1200 - x] = altititudeFt;

                if (altititudeFt > max) max = altititudeFt;
                if (altititudeFt < min) min = altititudeFt;

                offset += 2;
            }
        }

        // LOG.info("read " + bytes.length + " bytes.");
        // LOG.info("final offset: " + offset);

        // LOG.info("max: " + max);
        // LOG.info("min: " + min);
    }

    public double getAltitudeFt(double latitude, double longitude) {
        double latDiff = Math.ceil(latitude) - latitude;
        double lonDiff = longitude - Math.floor(longitude);
        //cout << "latitude: " << latitude << ", latDiff: " << latDiff << endl;
        //cout << "longitude: " << longitude << ", lonDiff: " << lonDiff << endl;

        //tiles store the terrain height values starting in the NW corner
        //even though the file name is for the SW corner

        //int latIndex0 = (int)(latDiff / (srtmGridSize - 1));
        int latIndex0 = (int) (latDiff / SRTM_GRID_SIZE);
        int latIndex1 = latIndex0 + 1;
        //int lonIndex0 = (int)(lonDiff / (srtmGridSize - 1));
        int lonIndex0 = (int) (lonDiff / SRTM_GRID_SIZE);
        int lonIndex1 = lonIndex0 + 1;

        //cout << "srtmGridSize: " << srtmGridSize << endl;
        //cout << "latIndex0: " << latIndex0 << ", latIndex1: " << latIndex1 << endl;
        //cout << "lonIndex0: " << lonIndex0 << ", lonIndex1: " << lonIndex1 << endl;

        /*
        System.out.println("altitudesFt[" + latIndex0 + "][" + lonIndex0 + "]: " + altitudesFt[latIndex0][lonIndex0]);
        System.out.println("altitudesFt[" + latIndex1 + "][" + lonIndex0 + "]: " + altitudesFt[latIndex0][lonIndex0]);
        System.out.println("altitudesFt[" + latIndex0 + "][" + lonIndex1 + "]: " + altitudesFt[latIndex0][lonIndex0]);
        System.out.println("altitudesFt[" + latIndex1 + "][" + lonIndex1 + "]: " + altitudesFt[latIndex0][lonIndex0]);
        */

        double x = lonDiff - (lonIndex0 * SRTM_GRID_SIZE);
        double y = latDiff - (latIndex0 * SRTM_GRID_SIZE);

        return (altitudesFt[latIndex0][lonIndex0] * (1 - x) * (1 - y)) +
                (altitudesFt[latIndex1][lonIndex0] * x * (1 - y)) +
                (altitudesFt[latIndex0][lonIndex1] * (1 - x) * y) +
                (altitudesFt[latIndex1][lonIndex1] * x * y);

        /*
        int y = (int)Math.floor((1.0 - (latitude % 1.0)) * srtmTileSize);
        int x = (int)Math.ceil((longitude % 1.0) * srtmTileSize);
        int offset = (y * srtmTileSize * 2) + (x * 2);

        double altitudeM = ((bytes[offset] & 0xff) << 8) | (bytes[offset + 1] & 0xff);
        System.out.println("altitudeM: " + altitudeM);
        int altititudeFt = (int)((double)altitudeM * 3.2808399);
        System.out.println("altititudeFt: " + altititudeFt);

        return altititudeFt;
        */
    }
}
