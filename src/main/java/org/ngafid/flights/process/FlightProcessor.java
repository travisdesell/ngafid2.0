package org.ngafid.flights.process;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.ngafid.flights.Flight;
import org.ngafid.flights.MalformedFlightFileException;
import org.ngafid.flights.FatalFlightFileException;

/**
 * This class applies a sequence of processing steps to a flight that has already been parse (that is, a flight which
 * has already had its columns parsed). The processing done here falls largely into a few categories:
 *
 *    - Creation of new synthetic columns that aren't in the original files
 *    - Conversion of existing columns to different units
 *
 * Currently, the basic idea is that a list of process steps is created and then sequentially applied.
 * Some of these steps are going to be mandatory, and some will only be applied to specific aircraft and/or
 * aircraft that have the appropriate columns.
 *
 **/
public class FlightProcessor {
 
    private static final Logger LOG = Logger.getLogger(FlightProcessor.class.getName());

    Flight flight;

    public FlightProcessor(Flight flight) {
        this.flight = flight;
    }

    private static List<ProcessStep.Factory> requiredSteps = List.of();
    private static List<ProcessStep.Factory> optionalSteps = List.of();

    protected ArrayList<ProcessStep> gatherProcessSteps() throws FatalFlightFileException {
        ArrayList<ProcessStep> steps = new ArrayList<>();
        
        for (ProcessStep.Factory factory : requiredSteps) {
            ProcessStep step = factory.create(flight);

            if (!step.applicable())
                throw new FatalFlightFileException("Cannot apply required step " + step.toString() + " to flight " + flight.getId());
            
            steps.add(step);
        }

        for (ProcessStep.Factory factory : optionalSteps) {
            ProcessStep step = factory.create(flight);
            
            if (!step.applicable())
                LOG.info("Cannot apply optional step " + step.toString() + " to flight " + flight.getId());
            
            steps.add(step);
        }

        return steps;
    }

    final private void process() {
    }
}
