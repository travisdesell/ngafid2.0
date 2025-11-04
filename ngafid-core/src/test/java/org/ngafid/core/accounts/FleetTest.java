package org.ngafid.core.accounts;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Fleet class.
 * Tests fleet creation, basic functionality, and equality.
 */
public class FleetTest {

    @Test
    @DisplayName("Should create Fleet with valid id and name")
    public void testFleetConstructor() {
        int id = 1;
        String name = "Test Fleet";
        
        Fleet fleet = new Fleet(id, name);
        
        assertNotNull(fleet);
        assertEquals(id, fleet.getId());
        assertEquals(name, fleet.getName());
    }

    @Test
    @DisplayName("Should create Fleet with negative id")
    public void testFleetConstructorWithNegativeId() {
        int id = -1;
        String name = "Test Fleet";
        
        Fleet fleet = new Fleet(id, name);
        
        assertNotNull(fleet);
        assertEquals(id, fleet.getId());
        assertEquals(name, fleet.getName());
    }

    @Test
    @DisplayName("Should create Fleet with zero id")
    public void testFleetConstructorWithZeroId() {
        int id = 0;
        String name = "Test Fleet";
        
        Fleet fleet = new Fleet(id, name);
        
        assertNotNull(fleet);
        assertEquals(id, fleet.getId());
        assertEquals(name, fleet.getName());
    }

    @Test
    @DisplayName("Should create Fleet with null name")
    public void testFleetConstructorWithNullName() {
        int id = 1;
        String name = null;
        
        Fleet fleet = new Fleet(id, name);
        
        assertNotNull(fleet);
        assertEquals(id, fleet.getId());
        assertNull(fleet.getName());
    }

    @Test
    @DisplayName("Should create Fleet with empty name")
    public void testFleetConstructorWithEmptyName() {
        int id = 1;
        String name = "";
        
        Fleet fleet = new Fleet(id, name);
        
        assertNotNull(fleet);
        assertEquals(id, fleet.getId());
        assertEquals(name, fleet.getName());
    }

    @Test
    @DisplayName("Should create Fleet with long name")
    public void testFleetConstructorWithLongName() {
        int id = 1;
        String name = "Very Long Fleet Name That Exceeds Normal Length And Contains Special Characters @#$%^&*()";
        
        Fleet fleet = new Fleet(id, name);
        
        assertNotNull(fleet);
        assertEquals(id, fleet.getId());
        assertEquals(name, fleet.getName());
    }


    @Test
    @DisplayName("Should get fleet id")
    public void testGetId() {
        int id = 123;
        Fleet fleet = new Fleet(id, "Test Fleet");
        
        assertEquals(id, fleet.getId());
    }

    @Test
    @DisplayName("Should get fleet name")
    public void testGetName() {
        String name = "Test Fleet";
        Fleet fleet = new Fleet(1, name);
        
        assertEquals(name, fleet.getName());
    }


    @Test
    @DisplayName("Should return string representation")
    public void testToString() {
        Fleet fleet = new Fleet(123, "Test Fleet");
        String toString = fleet.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("123"), "toString should contain fleet ID");
        assertTrue(toString.contains("Test Fleet"), "toString should contain fleet name");
        assertTrue(toString.contains("Fleet id:"), "toString should contain 'Fleet id:'");
        assertTrue(toString.contains("name:"), "toString should contain 'name:'");
    }



    @Test
    @DisplayName("Should test fleet equality")
    public void testFleetEquals() {
        Fleet fleet1 = new Fleet(1, "Test Fleet");
        Fleet fleet2 = new Fleet(1, "Test Fleet");
        Fleet fleet3 = new Fleet(2, "Test Fleet");
        Fleet fleet4 = new Fleet(1, "Different Fleet");
        
        // Same fleet
        assertTrue(fleet1.equals(fleet2), "Same fleet should be equal");
        assertTrue(fleet2.equals(fleet1), "Equality should be symmetric");
        
        // Different ID
        assertFalse(fleet1.equals(fleet3), "Different ID should not be equal");
        
        // Different name
        assertFalse(fleet1.equals(fleet4), "Different name should not be equal");
        
        // Same object
        assertTrue(fleet1.equals(fleet1), "Same object should be equal");
    }

    @Test
    @DisplayName("Should test fleet equality with null")
    public void testFleetEqualsWithNull() {
        Fleet fleet = new Fleet(1, "Test Fleet");
        
        assertFalse(fleet.equals(null), "Fleet should not equal null");
    }

    @Test
    @DisplayName("Should test fleet equality with different object type")
    public void testFleetEqualsWithDifferentType() {
        Fleet fleet = new Fleet(1, "Test Fleet");
        String notAFleet = "Not a Fleet";
        
        assertFalse(fleet.equals(notAFleet), "Fleet should not equal different type");
    }

    @Test
    @DisplayName("Should test fleet equality with different class")
    public void testFleetEqualsWithDifferentClass() {
        Fleet fleet = new Fleet(1, "Test Fleet");
        String notAFleet = "Not a Fleet";
        
        assertFalse(fleet.equals(notAFleet), "Fleet should not equal different class");
    }

    @Test
    @DisplayName("Should handle fleet with null name in equality")
    public void testFleetEqualsWithNullName() {
        // Note: This test documents that Fleet.equals() doesn't handle null names gracefully
        // The Fleet.equals() method will throw NullPointerException when comparing null names
        Fleet fleet1 = new Fleet(1, null);
        Fleet fleet2 = new Fleet(1, null);
        
        // This will throw NullPointerException due to the implementation
        assertThrows(NullPointerException.class, () -> {
            fleet1.equals(fleet2);
        });
    }

    @Test
    @DisplayName("Should handle fleet with empty name in equality")
    public void testFleetEqualsWithEmptyName() {
        Fleet fleet1 = new Fleet(1, "");
        Fleet fleet2 = new Fleet(1, "");
        Fleet fleet3 = new Fleet(1, "Test Fleet");
        
        assertTrue(fleet1.equals(fleet2), "Fleets with empty names should be equal if IDs match");
        assertFalse(fleet1.equals(fleet3), "Fleet with empty name should not equal fleet with name");
    }

    @Test
    @DisplayName("Should handle fleet with special characters in equality")
    public void testFleetEqualsWithSpecialCharacters() {
        String specialName = "Fleet @#$%^&*()";
        Fleet fleet1 = new Fleet(1, specialName);
        Fleet fleet2 = new Fleet(1, specialName);
        Fleet fleet3 = new Fleet(1, "Different Name");
        
        assertTrue(fleet1.equals(fleet2), "Fleets with same special characters should be equal");
        assertFalse(fleet1.equals(fleet3), "Fleet with special characters should not equal different name");
    }

    @Test
    @DisplayName("Should handle fleet with unicode characters in equality")
    public void testFleetEqualsWithUnicodeCharacters() {
        String unicodeName = "舰队";
        Fleet fleet1 = new Fleet(1, unicodeName);
        Fleet fleet2 = new Fleet(1, unicodeName);
        Fleet fleet3 = new Fleet(1, "English Name");
        
        assertTrue(fleet1.equals(fleet2), "Fleets with same unicode characters should be equal");
        assertFalse(fleet1.equals(fleet3), "Fleet with unicode characters should not equal different name");
    }

    @Test
    @DisplayName("Should handle fleet with very long name in equality")
    public void testFleetEqualsWithLongName() {
        String longName = "Very Long Fleet Name That Exceeds Normal Length And Contains Special Characters @#$%^&*()_+-=[]{}|;':\",./<>?";
        Fleet fleet1 = new Fleet(1, longName);
        Fleet fleet2 = new Fleet(1, longName);
        Fleet fleet3 = new Fleet(1, "Short Name");
        
        assertTrue(fleet1.equals(fleet2), "Fleets with same long names should be equal");
        assertFalse(fleet1.equals(fleet3), "Fleet with long name should not equal different name");
    }

    @Test
    @DisplayName("Should handle fleet with negative ID in equality")
    public void testFleetEqualsWithNegativeId() {
        Fleet fleet1 = new Fleet(-1, "Test Fleet");
        Fleet fleet2 = new Fleet(-1, "Test Fleet");
        Fleet fleet3 = new Fleet(1, "Test Fleet");
        
        assertTrue(fleet1.equals(fleet2), "Fleets with same negative IDs should be equal");
        assertFalse(fleet1.equals(fleet3), "Fleet with negative ID should not equal fleet with positive ID");
    }

    @Test
    @DisplayName("Should handle fleet with zero ID in equality")
    public void testFleetEqualsWithZeroId() {
        Fleet fleet1 = new Fleet(0, "Test Fleet");
        Fleet fleet2 = new Fleet(0, "Test Fleet");
        Fleet fleet3 = new Fleet(1, "Test Fleet");
        
        assertTrue(fleet1.equals(fleet2), "Fleets with same zero IDs should be equal");
        assertFalse(fleet1.equals(fleet3), "Fleet with zero ID should not equal fleet with positive ID");
    }
}