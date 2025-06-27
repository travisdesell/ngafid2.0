package proximity;

import org.junit.Test;
import org.ngafid.processor.events.proximity.FlightTimeLocation;

import java.sql.SQLException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProximityTest {

    private FlightTimeLocation createFlight(double minLat, double maxLat,
                                            double minLon, double maxLon) {
        return new FlightTimeLocation(minLat, maxLat, minLon, maxLon);
    }

    @Test
    public void shouldReturnFalse_whenNoOverlapWithoutBuffer() {
        FlightTimeLocation a = createFlight(10, 12, 10, 12);
        FlightTimeLocation b = createFlight(12.1, 13, 12.1, 13);

        assertFalse(a.hasRegionOverlap(b,0));
        assertFalse(b.hasRegionOverlap(a,0));
    }

    @Test
    public void shouldReturnTrue_whenOverlapWithoutBuffer()  {
        FlightTimeLocation a = createFlight(10, 12, 10, 12);
        FlightTimeLocation b = createFlight(11, 13, 11, 13);

        assertTrue(a.hasRegionOverlap(b,0));
        assertTrue(b.hasRegionOverlap(a,0));

    }

    @Test
    public void shouldReturnTrue_whenBufferTouchesEdges() throws SQLException {
        FlightTimeLocation a = createFlight(10, 12, 10, 12);
        FlightTimeLocation b = createFlight(12.5, 14, 12.5, 14);

        // With buffer, b should now overlap a
        assertTrue(a.hasRegionOverlap(b,0.5));
        assertTrue(b.hasRegionOverlap(a,0.5));
    }

    @Test
    public void shouldReturnTrue_whenEdgesTouchExactly()  {
        FlightTimeLocation a = createFlight(10, 12, 10, 12);
        FlightTimeLocation b = createFlight(12, 14, 12, 14);

        assertTrue(a.hasRegionOverlap(b, 0));
        assertTrue(b.hasRegionOverlap(a, 0));
    }

    @Test
    public void shouldReturnFalse_whenJustOutsideWithoutBuffer() {
        FlightTimeLocation a = createFlight(10, 12, 10, 12);
        FlightTimeLocation b = createFlight(12.01, 14, 12.01, 14);

        assertFalse(a.hasRegionOverlap(b, 0));
        assertFalse(b.hasRegionOverlap(a, 0));
    }

    @Test
    public void shouldReturnTrue_whenRegionIsFullyContainedWithinAnother() {
        FlightTimeLocation outer = createFlight(10, 20, 10, 20);
        FlightTimeLocation inner = createFlight(12, 18, 12, 18);

        assertTrue(outer.hasRegionOverlap(inner, 0));
        assertTrue(inner.hasRegionOverlap(outer, 0));
    }

    @Test
    public void shouldReturnTrue_whenRegionsAreIdentical() {
        FlightTimeLocation a = createFlight(10, 20, 10, 20);
        FlightTimeLocation b = createFlight(10, 20, 10, 20);

        assertTrue(a.hasRegionOverlap(b, 0));
        assertTrue(b.hasRegionOverlap(a, 0));
    }

    @Test
    public void testNegativeCoordinatesOverlap() {
        FlightTimeLocation a = createFlight(-5, -3, -5, -3);
        FlightTimeLocation b = createFlight(-4, -2, -4, -2);

        assertTrue(a.hasRegionOverlap(b,0));
    }

    @Test
    public void testNoOverlapEvenWithBuffer() {
        FlightTimeLocation a = createFlight(0, 1, 0, 1);
        FlightTimeLocation b = createFlight(5, 6, 5, 6);

        assertFalse(a.hasRegionOverlap(b,0));
    }

    @Test
    public void shouldReturnTrue_whenNegativeRegionsOverlap() {
        FlightTimeLocation a = createFlight(-5, -3, -5, -3);
        FlightTimeLocation b = createFlight(-4, -2, -4, -2);

        assertTrue(a.hasRegionOverlap(b, 0));
        assertTrue(b.hasRegionOverlap(a, 0));
    }

    @Test
    public void shouldReturnFalse_whenRegionsAreFarApartEvenWithBuffer() {
        FlightTimeLocation a = createFlight(0, 1, 0, 1);
        FlightTimeLocation b = createFlight(5, 6, 5, 6);

        assertFalse(a.hasRegionOverlap(b, 1.0));
        assertFalse(b.hasRegionOverlap(a, 1.0));
    }
}
