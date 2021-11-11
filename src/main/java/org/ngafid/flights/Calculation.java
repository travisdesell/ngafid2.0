/**
 * This interface defines the process of calcuating new {@link DoubleTimeSeries} that require more 
 * complex analysis, such as for Stall and Loss of Control Indicies 
 *
 * @author <a href = mailto:apl1341@cs.rit.edu>Aidan LaBella @ RIT Computer Science</a>
 */

package org.ngafid.flights;

import java.io.IOException;
import java.sql.SQLException;

public interface Calculation {
    /**
     * This method contains the logic of the calculation for any index
     *
     * @param index the index to calculate at
     */
    public double calculate(int index) throws SQLException, IOException;
}

