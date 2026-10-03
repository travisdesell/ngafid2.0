package org.ngafid.processor.terrain;

/**
 * Thrown when terrain elevation data for a requested latitude/longitude is missing or out of range.
 *
 * <p>Raised when the corresponding SRTM tile file does not exist or the coordinate falls outside the valid tile
 * grid, signalling that an above-ground-level altitude cannot be computed for the point.
 */
public class TerrainUnavailableException extends Exception {
    TerrainUnavailableException(String message) {
        super(message);
    }
}
