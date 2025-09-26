package org.ngafid.core.airports;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class GeoHashTest {
    @Test
    void testGetGeoHashFormat() {
        String hash = GeoHash.getGeoHash(10.1234, 20.5678);
        assertTrue(hash.startsWith("+"));
        assertTrue(hash.contains("."));
        assertTrue(hash.length() > 0);
    }

    @Test
    void testNearbyGeoHashesCountAndUniqueness() {
        String[] hashes = GeoHash.getNearbyGeoHashes(10.1234, 20.5678);
        assertEquals(9, hashes.length);
        assertTrue(hashes[4].equals(GeoHash.getGeoHash(10.1234, 20.5678)));
        assertEquals(9, java.util.Arrays.stream(hashes).distinct().count());
    }

    @Test
    void testNearbyGeoHashesEdgeCase() {
        String[] hashes = GeoHash.getNearbyGeoHashes(-89.9999, 179.9999);
        assertEquals(9, hashes.length);
    }

    @Test
    void testPrivateConstructorThrowsException() throws Exception {
        var constructor = GeoHash.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        InvocationTargetException exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
        Throwable cause = exception.getCause();
        assertTrue(cause instanceof UnsupportedOperationException);
        assertEquals("Utility class cannot be instantiated.", cause.getMessage());
    }
}
