package org.ngafid.core.airports;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GeoHashTest {
    /**
     * Verifies {@link GeoHash#getGeoHash} produces a non-empty, sign-prefixed, decimal-formatted hash string.
     */
    @Test
    @DisplayName("Should format a geohash with sign prefix and decimals")
    public void testGetGeoHashFormat() {
        String hash = GeoHash.getGeoHash(10.1234, 20.5678);
        assertTrue(hash.startsWith("+"));
        assertTrue(hash.contains("."));
        assertTrue(hash.length() > 0);
    }

    /**
     * Verifies {@link GeoHash#getNearbyGeoHashes} returns nine distinct tiles with the query point's own hash at the
     * center index.
     */
    @Test
    @DisplayName("Should return nine unique nearby geohashes centered on the query point")
    public void testNearbyGeoHashesCountAndUniqueness() {
        String[] hashes = GeoHash.getNearbyGeoHashes(10.1234, 20.5678);
        assertEquals(9, hashes.length);
        assertTrue(hashes[4].equals(GeoHash.getGeoHash(10.1234, 20.5678)));
        assertEquals(9, java.util.Arrays.stream(hashes).distinct().count());
    }

    /**
     * Verifies {@link GeoHash#getNearbyGeoHashes} still returns nine tiles near the coordinate-system edges.
     */
    @Test
    @DisplayName("Should return nine nearby geohashes at coordinate edges")
    public void testNearbyGeoHashesEdgeCase() {
        String[] hashes = GeoHash.getNearbyGeoHashes(-89.9999, 179.9999);
        assertEquals(9, hashes.length);
    }

    /**
     * Verifies the {@link GeoHash} utility constructor is private and throws {@link UnsupportedOperationException} when
     * invoked reflectively, preventing instantiation.
     *
     * @throws Exception if reflective access to the constructor fails
     */
    @Test
    @DisplayName("Should prevent instantiation via its private constructor")
    public void testPrivateConstructorThrowsException() throws Exception {
        var constructor = GeoHash.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        InvocationTargetException exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
        Throwable cause = exception.getCause();
        assertTrue(cause instanceof UnsupportedOperationException);
        assertEquals("Utility class cannot be instantiated.", cause.getMessage());
    }
}
