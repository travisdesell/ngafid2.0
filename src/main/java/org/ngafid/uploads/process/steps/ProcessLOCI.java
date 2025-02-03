package org.ngafid.uploads.process.steps;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

import static org.ngafid.flights.Airframes.AIRFRAME_CESSNA_172S;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Flight;
import static org.ngafid.flights.Parameters.HDG;
import static org.ngafid.flights.Parameters.LOCI;
import static org.ngafid.flights.Parameters.LOCI_DEPENDENCIES;
import static org.ngafid.flights.Parameters.PRO_SPIN_FORCE;
import static org.ngafid.flights.Parameters.ROLL;
import static org.ngafid.flights.Parameters.STALL_PROB;
import static org.ngafid.flights.Parameters.TAS_FTMIN;
import org.ngafid.flights.Parameters.Unit;
import static org.ngafid.flights.Parameters.YAW_RATE_LAG;
import org.ngafid.uploads.process.FatalFlightFileException;
import org.ngafid.uploads.process.MalformedFlightFileException;
import org.ngafid.uploads.process.format.FlightBuilder;

public class ProcessLOCI extends ProcessStep {
	private static final Logger LOG = Logger.getLogger(ProcessLOCI.class.getName());

	private static final Set<String> REQUIRED_DOUBLE_COLUMNS = Set.of(LOCI_DEPENDENCIES);

	public ProcessLOCI(Connection connection, FlightBuilder builder) {
    	super(connection, builder);
	}

	public Set<String> getRequiredDoubleColumns() {
    	return REQUIRED_DOUBLE_COLUMNS;
	}

	public Set<String> getRequiredStringColumns() {
    	return Collections.emptySet();
	}

	public Set<String> getRequiredColumns() {
    	return REQUIRED_DOUBLE_COLUMNS;
	}

	public Set<String> getOutputColumns() {
    	return Collections.emptySet();
	}

	public boolean airframeIsValid(String airframe) {
    	return airframe.equals(AIRFRAME_CESSNA_172S);
	}

	public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {

    	DoubleTimeSeries hdg = builder.getDoubleTimeSeries(HDG);
    	DoubleTimeSeries hdgLagged = hdg.lag(YAW_RATE_LAG);
    	DoubleTimeSeries roll = builder.getDoubleTimeSeries(ROLL);
    	DoubleTimeSeries tas = builder.getDoubleTimeSeries(TAS_FTMIN);
    	DoubleTimeSeries stallIndex = builder.getDoubleTimeSeries(STALL_PROB);

    	int length = roll.size();

    	DoubleTimeSeries coordIndex;
    	coordIndex = DoubleTimeSeries.computed(PRO_SPIN_FORCE, Unit.INDEX, length, index -> {

        	double rollValue = roll.get(index);
        	double tasValue = tas.get(index);
        	double laggedHdgValue = hdgLagged.get(index);

        	if (Double.isNaN(rollValue) || Double.isNaN(tasValue) || Double.isNaN(laggedHdgValue))
            	return 0.0;

        	double valueOut = Flight.calculateLOCI(hdg, index, roll, tas, laggedHdgValue);
        	return valueOut;

    	});

    	DoubleTimeSeries loci = DoubleTimeSeries.computed(LOCI, "index", length, index -> {

            double prob = stallIndex.get(index) * coordIndex.get(index);
            if (Double.isNaN(prob))
                return 0.0;

            return (prob / 100);
            
        });

    	builder.addTimeSeries(coordIndex);
    	builder.addTimeSeries(loci);

	}
}