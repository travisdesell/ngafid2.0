package org.ngafid.core.agl_converter;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.ngafid.core.Config;

/**
 * Lightweight MSL to AGL converter that works on-demand per point.
 * Uses SRTM terrain data files directly without caching to minimize memory usage.
 */
public class MSLtoAGLConverter {
    private static final Logger LOG = Logger.getLogger(MSLtoAGLConverter.class.getName());
    
    // SRTM tile configuration
    private static final int SRTM_TILE_SIZE = 1201;
    private static final double SRTM_GRID_SIZE = 1.0 / (SRTM_TILE_SIZE - 1.0);
    
    // Terrain data directory from configuration
    private static final String TERRAIN_DIR = Config.NGAFID_TERRAIN_DIR;
    
    // Simple cache for file existence checks to avoid repeated file system calls
    private static final ConcurrentHashMap<String, Boolean> FILE_EXISTS_CACHE = new ConcurrentHashMap<>();
    
    /**
     * Converts MSL altitude to AGL altitude using terrain data.
     * 
     * @param altitudeMSL MSL altitude in feet
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @return AGL altitude in feet, or NaN if conversion not possible
     */
    public static double convertMSLToAGL(double altitudeMSL, double latitude, double longitude) {
        LOG.info("MSLtoAGLConverter.convertMSLToAGL called with: MSL=" + altitudeMSL + ", lat=" + latitude + ", lon=" + longitude);
        
        if (Double.isNaN(altitudeMSL) || Double.isInfinite(altitudeMSL) ||
            Double.isNaN(latitude) || Double.isInfinite(latitude) ||
            Double.isNaN(longitude) || Double.isInfinite(longitude)) {
            LOG.warning("Invalid input parameters: MSL=" + altitudeMSL + ", lat=" + latitude + ", lon=" + longitude);
            return Double.NaN;
        }

        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            LOG.warning("Coordinates out of range: lat=" + latitude + ", lon=" + longitude);
            return Double.NaN;
        }

        // Check if terrain directory is configured
        if (TERRAIN_DIR == null || TERRAIN_DIR.trim().isEmpty()) {
            LOG.warning("Terrain directory not configured. Cannot convert MSL to AGL. TERRAIN_DIR=" + TERRAIN_DIR);
            return Double.NaN;
        }
        
        LOG.info("Terrain directory configured: " + TERRAIN_DIR);

        try {
            // Get terrain elevation at the given coordinates
            double terrainElevationFt = getTerrainElevation(latitude, longitude);
            LOG.info("Terrain elevation: " + terrainElevationFt + " ft");
            
            // AGL = MSL - terrain elevation
            double agl = altitudeMSL - terrainElevationFt;
            LOG.info("Calculated AGL: " + agl + " ft (MSL=" + altitudeMSL + " - terrain=" + terrainElevationFt + ")");
            
            // Ensure AGL is not negative (aircraft can't be below ground)
            double finalAgl = Math.max(0.0, agl);
            LOG.info("Final AGL: " + finalAgl + " ft");
            return finalAgl;
            
        } catch (Exception e) {
            LOG.warning("Failed to convert MSL to AGL for lat=" + latitude + 
                       ", lon=" + longitude + ", MSL=" + altitudeMSL + ": " + e.getMessage());
            e.printStackTrace();
            return Double.NaN;
        }
    }
    
    /**
     * Gets terrain elevation at the specified coordinates.
     * 
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @return Terrain elevation in feet
     * @throws IOException if terrain data file cannot be read
     */
    private static double getTerrainElevation(double latitude, double longitude) throws IOException {
        // Calculate tile coordinates
        int latIndex = -((int) Math.ceil(latitude) - 91);
        int lonIndex = (int) Math.floor(longitude) + 180;
        
        // Validate tile coordinates
        if (latIndex < 0 || latIndex >= 180 || lonIndex < 0 || lonIndex >= 360) {
            throw new IOException("Invalid coordinates: lat=" + latitude + ", lon=" + longitude);
        }
        
        // Convert to SRTM tile coordinates
        int tileLat = 90 - latIndex;
        int tileLon = lonIndex - 180;
        
        // Get directory and filename for the tile
        String directory = getDirectoryFromLatLon(tileLat, tileLon);
        String filename = getFilenameFromLatLon(tileLat, tileLon);
        
        Path tilePath = Paths.get(TERRAIN_DIR, directory, filename);
        String tileKey = tilePath.toString();
        
        // Check cache first, then file system
        Boolean exists = FILE_EXISTS_CACHE.get(tileKey);
        if (exists == null) {
            exists = Files.exists(tilePath);
            FILE_EXISTS_CACHE.put(tileKey, exists);
        }
        
        if (!exists) {
            throw new IOException("Terrain tile not found: " + tilePath);
        }
        
        // Read terrain elevation from the tile file
        return readTerrainElevation(tilePath, latitude, longitude);
    }
    
    /**
     * Reads terrain elevation from SRTM tile file using bilinear interpolation.
     * 
     * @param tilePath Path to the SRTM tile file
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @return Terrain elevation in feet
     * @throws IOException if file cannot be read
     */
    private static double readTerrainElevation(Path tilePath, double latitude, double longitude) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(tilePath.toFile(), "r")) {
            // Calculate position within the tile
            double latDiff = Math.ceil(latitude) - latitude;
            double lonDiff = longitude - Math.floor(longitude);
            
            int latIndex0 = (int) (latDiff / SRTM_GRID_SIZE);
            int latIndex1 = Math.min(latIndex0 + 1, SRTM_TILE_SIZE - 1);
            int lonIndex0 = (int) (lonDiff / SRTM_GRID_SIZE);
            int lonIndex1 = Math.min(lonIndex0 + 1, SRTM_TILE_SIZE - 1);
            
            // Read the four surrounding elevation points
            double alt00 = readElevationPoint(file, latIndex0, lonIndex0);
            double alt10 = readElevationPoint(file, latIndex1, lonIndex0);
            double alt01 = readElevationPoint(file, latIndex0, lonIndex1);
            double alt11 = readElevationPoint(file, latIndex1, lonIndex1);
            
            // Bilinear interpolation
            double x = lonDiff - (lonIndex0 * SRTM_GRID_SIZE);
            double y = latDiff - (latIndex0 * SRTM_GRID_SIZE);
            
            return (alt00 * (1 - x) * (1 - y)) +
                   (alt10 * x * (1 - y)) +
                   (alt01 * (1 - x) * y) +
                   (alt11 * x * y);
        }
    }
    
    /**
     * Reads a single elevation point from the SRTM tile file.
     * 
     * @param file RandomAccessFile for the tile
     * @param latIndex Latitude index within tile
     * @param lonIndex Longitude index within tile
     * @return Elevation in feet
     * @throws IOException if file cannot be read
     */
    private static double readElevationPoint(RandomAccessFile file, int latIndex, int lonIndex) throws IOException {
        // Calculate byte offset for the point (2 bytes per elevation value)
        long offset = (latIndex * SRTM_TILE_SIZE + lonIndex) * 2L;
        file.seek(offset);
        
        // Read 2 bytes and convert to elevation in feet
        int altitudeM = ((file.readByte() & 0xff) << 8) | (file.readByte() & 0xff);
        
        // Convert meters to feet
        return altitudeM * 3.2808399;
    }
    
    /**
     * Gets directory name from latitude and longitude for SRTM tile organization.
     * 
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @return Directory name
     */
    private static String getDirectoryFromLatLon(int latitude, int longitude) {
        String directory = "";
        if (latitude < 0) {
            directory += 'S';
            latitude *= -1;
        }

        int ilatitude = latitude / 4;
        int ilongitude = longitude + 180;
        ilongitude /= 6;
        ilongitude += 1;

        // ASCII 65 == 'A'
        directory += Character.toString((char) (65 + ilatitude)) + ilongitude;

        return directory;
    }
    
    /**
     * Gets filename from latitude and longitude for SRTM tile.
     * 
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @return Filename
     */
    private static String getFilenameFromLatLon(int latitude, int longitude) {
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
    
} 