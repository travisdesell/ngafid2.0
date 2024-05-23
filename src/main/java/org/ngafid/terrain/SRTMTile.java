package org.ngafid.terrain;

import java.io.IOException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.NoSuchFileException;
import java.util.logging.*;

public class SRTMTile {
    private static final Logger LOG = Logger.getLogger(SRTMTile.class.getName());
    
    public static final int srtmTileSize = 1201;
    public static final double srtmGridSize = 1.0/(srtmTileSize - 1.0);

    private String directory;
    private String filename;

    private int latitudeS;
    private int longitudeW;

    private byte[] bytes;

    private int[][] altitudesFt;

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
        int altitude_m;

        int offset = 0;

        altitudesFt = new int[srtmTileSize][srtmTileSize];

        int max = 0;
        int min = 5000;
        int altitude_ft;

        for (int y = 0; y < srtmTileSize; y++) {
            for (int x = 0; x < srtmTileSize; x++) {
                altitude_m = ((bytes[offset] & 0xff) << 8) | (bytes[offset + 1] & 0xff);

                altitude_ft = (int)((double)altitude_m * 3.2808399);
                //altitudesFt[x][y] = altitude_ft;
                //altitudesFt[1200 - x][y] = altitude_ft;
                //altitudesFt[x][1200 - y] = altitude_ft; //close-ish
                //altitudesFt[1200 - x][1200 - y] = altitude_ft;

                altitudesFt[y][x] = altitude_ft;
                //altitudesFt[1200 - y][x] = altitude_ft;
                //altitudesFt[y][1200 - x] = altitude_ft;
                //altitudesFt[1200 - y][1200 - x] = altitude_ft;

                if (altitude_ft > max) max = altitude_ft;
                if (altitude_ft < min) min = altitude_ft;

                offset += 2;
            }
        }

        // LOG.info("read " + bytes.length + " bytes.");
        // LOG.info("final offset: " + offset);

        // LOG.info("max: " + max);
        // LOG.info("min: " + min);
    }

    public double getAltitudeFt(double latitude, double longitude) {
        double lat_diff = Math.ceil(latitude) - latitude;
        double lon_diff = longitude - Math.floor(longitude);
        //cout << "latitude: " << latitude << ", lat_diff: " << lat_diff << endl;
        //cout << "longitude: " << longitude << ", lon_diff: " << lon_diff << endl;

        //tiles store the terrain height values starting in the NW corner
        //even though the file name is for the SW corner

        //int latIndex0 = (int)(lat_diff / (srtmGridSize - 1));
        int latIndex0 = (int)(lat_diff / srtmGridSize);
        int latIndex1 = latIndex0 + 1;
        //int lonIndex0 = (int)(lon_diff / (srtmGridSize - 1));
        int lonIndex0 = (int)(lon_diff / srtmGridSize );
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
        
        double x = lon_diff - (lonIndex0 * srtmGridSize);
        double y = lat_diff - (latIndex0 * srtmGridSize);

        double interpolated_altitude =
              (altitudesFt[latIndex0][lonIndex0] * (1 - x) * (1 - y))
            + (altitudesFt[latIndex1][lonIndex0] * x * (1 - y))
            + (altitudesFt[latIndex0][lonIndex1] * (1 - x) * y)
            + (altitudesFt[latIndex1][lonIndex1] * x * y);

        return interpolated_altitude;

        /*
        int y = (int)Math.floor((1.0 - (latitude % 1.0)) * srtmTileSize);
        int x = (int)Math.ceil((longitude % 1.0) * srtmTileSize);
        int offset = (y * srtmTileSize * 2) + (x * 2);

        double altitude_m = ((bytes[offset] & 0xff) << 8) | (bytes[offset + 1] & 0xff);
        System.out.println("altitude_m: " + altitude_m);
        int altitude_ft = (int)((double)altitude_m * 3.2808399);
        System.out.println("altitude_ft: " + altitude_ft);

        return altitude_ft;
        */
    }
}
