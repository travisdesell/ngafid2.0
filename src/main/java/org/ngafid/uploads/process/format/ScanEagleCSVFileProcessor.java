package org.ngafid.uploads.process.format;

import org.ngafid.flights.Airframes;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.StringTimeSeries;
import org.ngafid.uploads.process.FatalFlightFileException;
import org.ngafid.uploads.process.FlightMeta;
import org.ngafid.uploads.process.Pipeline;
import org.ngafid.uploads.process.steps.ComputeStep;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ScanEagleCSVFileProcessor extends CSVFileProcessor {
    private static final Logger LOG = Logger.getLogger(ScanEagleCSVFileProcessor.class.getName());

    /**
     * Flight builder for G5FlightBuilder
     */
    public static class ScanEagleFlightBuilder extends FlightBuilder {

        public ScanEagleFlightBuilder(FlightMeta meta, Map<String, DoubleTimeSeries> doubleTimeSeries,
                                      Map<String, StringTimeSeries> stringTimeSeries) {
            super(meta, doubleTimeSeries, stringTimeSeries);
        }

        @Override
        protected List<ComputeStep> gatherSteps(Connection connection) {
            // As of now, none of our process steps apply to scan eagle data.
            return List.of();
        }
    }

    public ScanEagleCSVFileProcessor(Connection connection, InputStream stream, String filename, Pipeline pipeline) throws IOException {
        super(connection, stream, filename, pipeline);

        meta.airframe = new Airframes.Airframe("ScanEagle");
        meta.airframeType = new Airframes.AirframeType("UAS Fixed Wing");
    }

    /**
     * Parses for ScanEagle flight data
     */
    @Override
    void processMetaData(BufferedReader reader) throws FatalFlightFileException {
        // need a custom method to process ScanEagle data because the column
        // names are different and there is no header info
        try {
            var fileInformation = reader.readLine();
            scanEagleSetTailAndID();
            scanEagleHeaders(fileInformation);
        } catch (IOException e) {
            throw new FatalFlightFileException("Stream ended prematurely -- unable to read file", e);
        }
    }

    /**
     * Handles setting the tail number and system id for ScanEagle data
     */
    private void scanEagleSetTailAndID() {
        String[] filenameParts = filename.split("_");
        meta.startDateTime = filenameParts[0];
        meta.endDateTime = meta.startDateTime;
        LOG.log(Level.INFO, "start date: '{0}'", meta.startDateTime);
        LOG.log(Level.INFO, "end date: '{0}'", meta.startDateTime);

        // UND doesn't have the systemId for UAS anywhere in the filename or file (sigh)
        meta.suggestedTailNumber = "N" + filenameParts[1] + "ND";
        meta.systemId = meta.suggestedTailNumber;

        LOG.log(Level.INFO, "suggested tail number: '{0}'", meta.suggestedTailNumber);
        LOG.log(Level.INFO, "system id: '{0}'", meta.systemId);
    }

    // TODO: Figure out ScanEagle data
    void scanEagleHeaders(String fileInformation) {
        String headersLine = fileInformation;
        headers.addAll(Arrays.asList(headersLine.split(",", -1)));
        headers.replaceAll(String::trim);

        // scan eagle files have no data types, set all to ""
        // TODO: These columns certainly have data types. We should create a map of them ourselves?
        for (int i = 0; i < headers.size(); i++) {
            dataTypes.add("none");
        }
    }

    @Override
    List<String> processDataTypes(BufferedReader reader) throws FatalFlightFileException {
        return dataTypes;
    }

    @Override
    List<String> processHeaders(BufferedReader reader) throws FatalFlightFileException {
        return headers;
    }
}
