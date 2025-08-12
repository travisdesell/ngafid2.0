package org.ngafid.processor.terrain;

import java.nio.file.NoSuchFileException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jetbrains.annotations.NotNull;
import org.ngafid.core.Config;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public enum TerrainCache {
    ;
    private static final Logger LOG = Logger.getLogger(TerrainCache.class.getName());

    // private static final LoadingCache<TileCoordinate, SRTMTile> TILE_CACHE;
    private static final LoadingCache<TileKey, SRTMTile> TILE_CACHE;

    static {

        LOG.log(Level.INFO, "Initializing TerrainCache with max size: {0}", Config.MAX_TERRAIN_CACHE_SIZE);

        TILE_CACHE = CacheBuilder.newBuilder()
                .maximumSize(Config.MAX_TERRAIN_CACHE_SIZE)
                .build(
                        new CacheLoader<>() {
                            @NotNull
                            @Override
                            public SRTMTile load(@NotNull TileKey key) throws TerrainUnavailableException, NoSuchFileException {
                                return new SRTMTile(90 - key.latIndex, key.lonIndex - 180);
                            }
                        }
                );
    }

    // each directory contains a 4 by 6 grid of files, 4 latitudes worth and 4 longitudes worth
    // the equator starts at A and goes north alphabetically, and at SA and goes south alphabetically (SA, SB, SC)...
    // numbers start at 01, which corresponds to W180 ... W175, 02 is W174 ... 169, etc
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

        // LOG.info("iLatitude: " + ilatitude + ", iLongitude: " + ilongitude);

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

    public static int getAltitudeFt(double msl, double latitude, double longitude) throws TerrainUnavailableException {
        //cout << "getting tile for latitude: " << latitude << " and longitude: " << longitude << endl;
        TileCoordinate coordinate = TileCoordinate.fromLatLon(latitude, longitude);

        //LOG.info("lat_index: " + latIndex + ", lon_index: " + lonIndex);

        if (coordinate.latIndex < 0 || coordinate.lonIndex < 0 || coordinate.latIndex >= 180 || coordinate.lonIndex >= 360) {
            LOG.severe("ERROR: getting tile for latitude: " + latitude + " and longitude: " + longitude);
            LOG.severe("tile[" + coordinate.latIndex + "][" + coordinate.lonIndex + "] does not exist!");
            LOG.severe("latitude should be >= -90 and <= 90");
            LOG.severe("longitude should be >= -180 and <= 180");
            throw new TerrainUnavailableException("There is no tile latitude: " + latitude + " and longitude: " + longitude);
        }

        TileKey key = new TileKey(coordinate.latIndex, coordinate.lonIndex);

        SRTMTile tile = null;
        try {
            tile = TILE_CACHE.get(key);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof TerrainUnavailableException te) {
                throw te;
            }
        }

        double altitudeFt = tile.getAltitudeFt(latitude, longitude);

        return (int) Math.max(0, msl - altitudeFt);
    }

    private static final class TileKey {

        final int latIndex;
        final int lonIndex;
        TileKey(int latIndex, int lonIndex) {
            this.latIndex = latIndex;
            this.lonIndex = lonIndex;
        }

        @Override public int hashCode() { return Objects.hash(latIndex, lonIndex); }
        @Override public boolean equals(Object objectTarget) {

            //Target isn't a TileKey -> False
            if (!(objectTarget instanceof TileKey k))
                return false;

            return (k.latIndex == latIndex && k.lonIndex == lonIndex);
        }
        
    }

    private record TileCoordinate(double lat, double lon, int latIndex, int lonIndex) {
        static TileCoordinate fromLatLon(double lat, double lon) {
            int latIndex = -((int) Math.ceil(lat) - 91);
            int lonIndex = (int) Math.floor(lon) + 180;

            return new TileCoordinate(lat, lon, latIndex, lonIndex);
        }

        SRTMTile getTile() throws TerrainUnavailableException {
            try {
                return new SRTMTile(90 - latIndex, lonIndex - 180);
            } catch (NoSuchFileException e) {
                throw new TerrainUnavailableException("There is no tile for latitude: " + lat + " and longitude: " + lon);
            }
        }
    }

}
