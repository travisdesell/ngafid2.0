package org.ngafid.core.accounts;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for UserPreferences class.
 * Tests user preference creation, updates, and string representation.
 */
public class UserPreferencesTest {

    @Test
    @DisplayName("Should create UserPreferences with List constructor")
    public void testConstructorWithList() {
        int userId = 1;
        int decimalPrecision = 2;
        List<String> flightMetrics = Arrays.asList("altitude", "airspeed", "vertical_speed");
        
        UserPreferences preferences = new UserPreferences(userId, decimalPrecision, flightMetrics);
        
        assertNotNull(preferences);
        assertEquals(decimalPrecision, preferences.getDecimalPrecision());
        assertEquals(flightMetrics, preferences.getFlightMetrics());
    }

    @Test
    @DisplayName("Should create UserPreferences with array constructor")
    public void testConstructorWithArray() {
        int userId = 2;
        int decimalPrecision = 3;
        String[] metrics = {"altitude", "airspeed", "vertical_speed"};
        
        UserPreferences preferences = new UserPreferences(userId, decimalPrecision, metrics);
        
        assertNotNull(preferences);
        assertEquals(decimalPrecision, preferences.getDecimalPrecision());
        assertEquals(Arrays.asList(metrics), preferences.getFlightMetrics());
    }

    @Test
    @DisplayName("Should create default preferences")
    public void testDefaultPreferences() {
        int userId = 3;
        
        UserPreferences preferences = UserPreferences.defaultPreferences(userId);
        
        assertNotNull(preferences);
        assertEquals(1, preferences.getDecimalPrecision());
        assertNotNull(preferences.getFlightMetrics());
        // DEFAULT_METRICS could be empty or non-empty, just check it's not null
        // The actual content depends on the DEFAULT_METRICS constant
    }

    @Test
    @DisplayName("Should get decimal precision")
    public void testGetDecimalPrecision() {
        UserPreferences preferences = new UserPreferences(1, 5, Arrays.asList("altitude"));
        
        assertEquals(5, preferences.getDecimalPrecision());
    }

    @Test
    @DisplayName("Should get flight metrics")
    public void testGetFlightMetrics() {
        List<String> metrics = Arrays.asList("altitude", "airspeed", "vertical_speed");
        UserPreferences preferences = new UserPreferences(1, 2, metrics);
        
        assertEquals(metrics, preferences.getFlightMetrics());
    }

    @Test
    @DisplayName("Should update decimal precision when different")
    public void testUpdateWithDifferentPrecision() {
        UserPreferences preferences = new UserPreferences(1, 2, Arrays.asList("altitude"));
        
        boolean wasUpdated = preferences.update(5);
        
        assertTrue(wasUpdated);
        assertEquals(5, preferences.getDecimalPrecision());
    }

    @Test
    @DisplayName("Should not update decimal precision when same")
    public void testUpdateWithSamePrecision() {
        UserPreferences preferences = new UserPreferences(1, 2, Arrays.asList("altitude"));
        
        boolean wasUpdated = preferences.update(2);
        
        assertFalse(wasUpdated);
        assertEquals(2, preferences.getDecimalPrecision());
    }

    @Test
    @DisplayName("Should update from 0 to positive precision")
    public void testUpdateFromZeroToPositive() {
        UserPreferences preferences = new UserPreferences(1, 0, Arrays.asList("altitude"));
        
        boolean wasUpdated = preferences.update(3);
        
        assertTrue(wasUpdated);
        assertEquals(3, preferences.getDecimalPrecision());
    }

    @Test
    @DisplayName("Should update from positive to zero precision")
    public void testUpdateFromPositiveToZero() {
        UserPreferences preferences = new UserPreferences(1, 3, Arrays.asList("altitude"));
        
        boolean wasUpdated = preferences.update(0);
        
        assertTrue(wasUpdated);
        assertEquals(0, preferences.getDecimalPrecision());
    }

    @Test
    @DisplayName("Should update from negative to positive precision")
    public void testUpdateFromNegativeToPositive() {
        UserPreferences preferences = new UserPreferences(1, -1, Arrays.asList("altitude"));
        
        boolean wasUpdated = preferences.update(2);
        
        assertTrue(wasUpdated);
        assertEquals(2, preferences.getDecimalPrecision());
    }

    @Test
    @DisplayName("Should have correct toString representation")
    public void testToString() {
        int userId = 123;
        int decimalPrecision = 4;
        List<String> flightMetrics = Arrays.asList("altitude", "airspeed", "vertical_speed");
        UserPreferences preferences = new UserPreferences(userId, decimalPrecision, flightMetrics);
        
        String result = preferences.toString();
        
        assertNotNull(result);
        assertTrue(result.contains("user_id : 123"));
        assertTrue(result.contains("precision: 4"));
        assertTrue(result.contains("metrics [altitude, airspeed, vertical_speed]"));
    }

    @Test
    @DisplayName("Should have correct toString with empty metrics")
    public void testToStringWithEmptyMetrics() {
        int userId = 456;
        int decimalPrecision = 0;
        List<String> emptyMetrics = Arrays.asList();
        UserPreferences preferences = new UserPreferences(userId, decimalPrecision, emptyMetrics);
        
        String result = preferences.toString();
        
        assertNotNull(result);
        assertTrue(result.contains("user_id : 456"));
        assertTrue(result.contains("precision: 0"));
        assertTrue(result.contains("metrics []"));
    }

    @Test
    @DisplayName("Should have correct toString with single metric")
    public void testToStringWithSingleMetric() {
        int userId = 789;
        int decimalPrecision = 1;
        List<String> singleMetric = Arrays.asList("altitude");
        UserPreferences preferences = new UserPreferences(userId, decimalPrecision, singleMetric);
        
        String result = preferences.toString();
        
        assertNotNull(result);
        assertTrue(result.contains("user_id : 789"));
        assertTrue(result.contains("precision: 1"));
        assertTrue(result.contains("metrics [altitude]"));
    }

    @Test
    @DisplayName("Should handle toString with null metrics gracefully")
    public void testToStringWithNullMetrics() {
        int userId = 999;
        int decimalPrecision = 2;
        // Create preferences with null metrics (edge case)
        UserPreferences preferences = new UserPreferences(userId, decimalPrecision, (List<String>) null);
        
        String result = preferences.toString();
        
        assertNotNull(result);
        assertTrue(result.contains("user_id : 999"));
        assertTrue(result.contains("precision: 2"));
        assertTrue(result.contains("metrics null"));
    }

    @Test
    @DisplayName("Should handle multiple updates correctly")
    public void testMultipleUpdates() {
        UserPreferences preferences = new UserPreferences(1, 1, Arrays.asList("altitude"));
        
        // First update
        boolean firstUpdate = preferences.update(3);
        assertTrue(firstUpdate);
        assertEquals(3, preferences.getDecimalPrecision());
        
        // Second update with same value
        boolean secondUpdate = preferences.update(3);
        assertFalse(secondUpdate);
        assertEquals(3, preferences.getDecimalPrecision());
        
        // Third update with different value
        boolean thirdUpdate = preferences.update(0);
        assertTrue(thirdUpdate);
        assertEquals(0, preferences.getDecimalPrecision());
    }

    @Test
    @DisplayName("Should handle edge case with maximum precision")
    public void testMaximumPrecision() {
        UserPreferences preferences = new UserPreferences(1, Integer.MAX_VALUE, Arrays.asList("altitude"));
        
        assertEquals(Integer.MAX_VALUE, preferences.getDecimalPrecision());
        
        boolean wasUpdated = preferences.update(Integer.MAX_VALUE - 1);
        assertTrue(wasUpdated);
        assertEquals(Integer.MAX_VALUE - 1, preferences.getDecimalPrecision());
    }

    @Test
    @DisplayName("Should handle edge case with minimum precision")
    public void testMinimumPrecision() {
        UserPreferences preferences = new UserPreferences(1, Integer.MIN_VALUE, Arrays.asList("altitude"));
        
        assertEquals(Integer.MIN_VALUE, preferences.getDecimalPrecision());
        
        boolean wasUpdated = preferences.update(Integer.MIN_VALUE + 1);
        assertTrue(wasUpdated);
        assertEquals(Integer.MIN_VALUE + 1, preferences.getDecimalPrecision());
    }
}
