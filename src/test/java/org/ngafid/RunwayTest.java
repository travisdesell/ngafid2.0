package org.ngafid;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.ngafid.airports.Runway;

/**
 * Unit test for simple App.
 */
public class RunwayTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public RunwayTest( String testName ) {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite( RunwayTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testDistance() {
        Runway runway = new Runway("2304.9*", "9L/15R", 42.32, -97.23, 42.323, -97.239);

        double distanceFt = runway.getDistanceFt(42.29, -97.05);

        assertTrue( distanceFt == 1342.9 );
    }
}
