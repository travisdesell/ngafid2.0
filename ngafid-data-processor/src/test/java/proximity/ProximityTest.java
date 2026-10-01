package proximity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import org.junit.Test;
import org.ngafid.processor.events.proximity.FlightTimeLocation;

public class ProximityTest {

    private FlightTimeLocation createFlight(double minLat, double maxLat, double minLon, double maxLon) {
        return new FlightTimeLocation(minLat, maxLat, minLon, maxLon);
    }

    /**
     * Verifies that two disjoint bounding boxes report no region overlap when no buffer is applied, in both
     * comparison directions.
     */
    @Test
    public void shouldReturnFalseWhenNoOverlapWithoutBuffer() {
        FlightTimeLocation a = createFlight(10, 12, 10, 12);
        FlightTimeLocation b = createFlight(12.1, 13, 12.1, 13);

        assertFalse(a.hasRegionOverlap(b, 0));
        assertFalse(b.hasRegionOverlap(a, 0));
    }

    /**
     * Verifies that two overlapping bounding boxes report a region overlap with no buffer, in both comparison
     * directions.
     */
    @Test
    public void shouldReturnTrueWhenOverlapWithoutBuffer() {
        FlightTimeLocation a = createFlight(10, 12, 10, 12);
        FlightTimeLocation b = createFlight(11, 13, 11, 13);

        assertTrue(a.hasRegionOverlap(b, 0));
        assertTrue(b.hasRegionOverlap(a, 0));
    }

    /**
     * Verifies that two boxes that are separated by a small gap report an overlap once a buffer large enough to
     * bridge the gap is applied.
     */
    @Test
    public void shouldReturnTrueWhenBufferTouchesEdges() throws SQLException {
        FlightTimeLocation a = createFlight(10, 12, 10, 12);
        FlightTimeLocation b = createFlight(12.5, 14, 12.5, 14);

        // With buffer, b should now overlap a
        assertTrue(a.hasRegionOverlap(b, 0.5));
        assertTrue(b.hasRegionOverlap(a, 0.5));
    }

    /**
     * Verifies that two boxes sharing an exact edge coordinate are treated as overlapping even with no buffer.
     */
    @Test
    public void shouldReturnTrueWhenEdgesTouchExactly() {
        FlightTimeLocation a = createFlight(10, 12, 10, 12);
        FlightTimeLocation b = createFlight(12, 14, 12, 14);

        assertTrue(a.hasRegionOverlap(b, 0));
        assertTrue(b.hasRegionOverlap(a, 0));
    }

    /**
     * Verifies that two boxes separated by a tiny gap report no overlap when no buffer is applied, confirming the
     * boundary is exclusive.
     */
    @Test
    public void shouldReturnFalseWhenJustOutsideWithoutBuffer() {
        FlightTimeLocation a = createFlight(10, 12, 10, 12);
        FlightTimeLocation b = createFlight(12.01, 14, 12.01, 14);

        assertFalse(a.hasRegionOverlap(b, 0));
        assertFalse(b.hasRegionOverlap(a, 0));
    }

    /**
     * Verifies that a box fully contained within another reports an overlap in both comparison directions.
     */
    @Test
    public void shouldReturnTrueWhenRegionIsFullyContainedWithinAnother() {
        FlightTimeLocation outer = createFlight(10, 20, 10, 20);
        FlightTimeLocation inner = createFlight(12, 18, 12, 18);

        assertTrue(outer.hasRegionOverlap(inner, 0));
        assertTrue(inner.hasRegionOverlap(outer, 0));
    }

    /**
     * Verifies that two identical bounding boxes report an overlap in both comparison directions.
     */
    @Test
    public void shouldReturnTrueWhenRegionsAreIdentical() {
        FlightTimeLocation a = createFlight(10, 20, 10, 20);
        FlightTimeLocation b = createFlight(10, 20, 10, 20);

        assertTrue(a.hasRegionOverlap(b, 0));
        assertTrue(b.hasRegionOverlap(a, 0));
    }

    /**
     * Verifies that overlap detection works for boxes expressed in negative latitude/longitude coordinates.
     */
    @Test
    public void testNegativeCoordinatesOverlap() {
        FlightTimeLocation a = createFlight(-5, -3, -5, -3);
        FlightTimeLocation b = createFlight(-4, -2, -4, -2);

        assertTrue(a.hasRegionOverlap(b, 0));
    }

    /**
     * Verifies that two widely separated boxes report no overlap when no buffer is applied.
     */
    @Test
    public void testNoOverlapEvenWithBuffer() {
        FlightTimeLocation a = createFlight(0, 1, 0, 1);
        FlightTimeLocation b = createFlight(5, 6, 5, 6);

        assertFalse(a.hasRegionOverlap(b, 0));
    }

    /**
     * Verifies that overlapping boxes in negative coordinates report an overlap in both comparison directions.
     */
    @Test
    public void shouldReturnTrueWhenNegativeRegionsOverlap() {
        FlightTimeLocation a = createFlight(-5, -3, -5, -3);
        FlightTimeLocation b = createFlight(-4, -2, -4, -2);

        assertTrue(a.hasRegionOverlap(b, 0));
        assertTrue(b.hasRegionOverlap(a, 0));
    }

    /**
     * Verifies that two widely separated boxes still report no overlap when a buffer too small to bridge the gap
     * is applied.
     */
    @Test
    public void shouldReturnFalseWhenRegionsAreFarApartEvenWithBuffer() {
        FlightTimeLocation a = createFlight(0, 1, 0, 1);
        FlightTimeLocation b = createFlight(5, 6, 5, 6);

        assertFalse(a.hasRegionOverlap(b, 1.0));
        assertFalse(b.hasRegionOverlap(a, 1.0));
    }
}
