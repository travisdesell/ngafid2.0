/**
 * This interface defines the process of calcuating new {@link DoubleTimeSeries} that require more 
 * complex analysis, such as for Stall Probaility and Loss of Control Probability
 *
 * @author <a href = mailto:apl1341@cs.rit.edu>Aidan LaBella @ RIT Computer Science</a>
 */

package org.ngafid.flights;

public interface Calculation {
    /**
     * This method contains the logic of the calculation for any index
     *
     * @param index the index to calculate at
     */
    public double calculate(int index);
}

