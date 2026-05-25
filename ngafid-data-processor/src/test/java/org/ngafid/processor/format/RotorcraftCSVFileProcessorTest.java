package org.ngafid.processor.format;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.Parameters;
import org.ngafid.core.flights.StringTimeSeries;

class RotorcraftCSVFileProcessorTest {

    @Test
    void detectsGarminStyleRotorcraftHeader() {
        String line =
                "#airframe_info, log_version=\"1.00\", airframe_name=\"AW-119\", system_id=\"251511D1E\", mode=NORMAL,";
        assertTrue(RotorcraftCSVFileProcessor.isGarminStyleRotorcraftHeader(line));
    }

    @Test
    void detectsDirectGarminColumnHeader() {
        assertTrue(RotorcraftCSVFileProcessor.isDirectGarminColumnHeader("Lcl Date, Lcl Time, UTCOfst, AtvWpt"));
    }

    @Test
    void detectsTimeseriesHeader() {
        assertTrue(RotorcraftCSVFileProcessor.isTimeseriesHeader("Month,Day,Year,Hour,Minute,Second"));
    }

    @Test
    void detectsAppareoCommentLines() {
        assertTrue(RotorcraftCSVFileProcessor.isAppareoCommentLine("# Converted Flight Data"));
        assertTrue(RotorcraftCSVFileProcessor.isAppareoCommentLine("# Appareo Systems"));
    }

    @Test
    void detectsAppareoDataHeader() {
        assertTrue(RotorcraftCSVFileProcessor.isAppareoDataHeader(
                "Relative Time,UNIX Time,Latitude,Pressure Altitude,Longitude"));
    }

    @Test
    void skipsBlankLineBetweenAppareoCommentsAndHeader() throws IOException {
        String preamble =
                """
                # Converted Flight Data
                # Fused File: VIS-FG0D-20094
                # Appareo Systems
                # Converted to CSV on 31 July 2024

                Relative Time,UNIX Time,Latitude
                """;
        try (BufferedReader reader = new BufferedReader(new StringReader(preamble))) {
            String first = reader.readLine();
            String header = RotorcraftCSVFileProcessor.advancePastAppareoPreamble(reader, first);
            assertEquals("Relative Time,UNIX Time,Latitude", header);
        }
    }

    @Test
    void parsesAppareoStyleFilename() {
        var parsed = RotorcraftCSVFileProcessor.parseFilename("N11UQ_20240728T045025_TD.csv");
        assertEquals("N11UQ", parsed.get().tail());
        assertEquals("20240728T045025", parsed.get().systemId());
    }

    @Test
    void parsesTdStyleFilename() {
        var parsed = RotorcraftCSVFileProcessor.parseFilename("N351LL_20240429T193603_TD.csv");
        assertTrue(parsed.isPresent());
        assertEquals("N351LL", parsed.get().tail());
        assertEquals("20240429T193603", parsed.get().systemId());
    }

    @Test
    void parsesNgafidFltFilename() {
        var parsed = RotorcraftCSVFileProcessor.parseFilename("344Y_190724180219_NGAFID_FLT.csv");
        assertTrue(parsed.isPresent());
        assertEquals("344Y", parsed.get().tail());
        assertEquals("190724180219", parsed.get().systemId());
    }

    @Test
    void parsesFilenameWithPath() {
        var parsed = RotorcraftCSVFileProcessor.parseFilename(
                "/data/UND/709__344Y/344Y_190724180219_NGAFID_FLT.csv");
        assertEquals("344Y", parsed.get().tail());
        assertEquals("190724180219", parsed.get().systemId());
    }

    @Test
    void parsesTailOnlyFilename() {
        var parsed = RotorcraftCSVFileProcessor.parseFilename("344Y.csv");
        assertEquals("344Y", parsed.get().tail());
        assertEquals("", parsed.get().systemId());
    }

    @Test
    void buildsCanonicalUnixTimeFromAppareoColumn() {
        var unix = new DoubleTimeSeries(RotorcraftCSVFileProcessor.APPAREO_UNIX_TIME_COLUMN, "seconds");
        unix.add(1722142225.749);
        var doubles = new HashMap<String, DoubleTimeSeries>();
        doubles.put(RotorcraftCSVFileProcessor.APPAREO_UNIX_TIME_COLUMN, unix);
        var strings = new HashMap<String, StringTimeSeries>();

        RotorcraftCSVFileProcessor.addCanonicalUnixTimeSeries(doubles, strings);

        assertTrue(doubles.containsKey(Parameters.UNIX_TIME_SECONDS));
        assertTrue(strings.containsKey(Parameters.UTC_DATE_TIME));
        assertEquals(1722142225.749, doubles.get(Parameters.UNIX_TIME_SECONDS).get(0), 0.001);
        assertTrue(strings.get(Parameters.UTC_DATE_TIME).get(0).contains("2024-07-28"));
    }
}
