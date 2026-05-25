package org.ngafid.processor.format;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ngafid.core.flights.Airframes;
import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.FatalFlightFileException;
import org.ngafid.core.flights.FlightMeta;
import org.ngafid.core.flights.FlightProcessingException;
import org.ngafid.core.flights.Parameters;
import org.ngafid.core.flights.RotorcraftTailAirframeRegistry;
import org.ngafid.core.flights.StringTimeSeries;
import org.ngafid.core.util.MD5;
import org.ngafid.core.util.TimeUtils;
import org.ngafid.processor.Pipeline;

/**
 * CSV flight recorder exports for rotorcraft. Identity (tail, system id, airframe) comes from the
 * filename and {@code tail_airframe_registry}, not from embedded recorder metadata.
 * <p>
 * The tail is the first segment of the basename ({@code 344Y} in {@code 344Y_190724180219_NGAFID_FLT.csv}).
 * When present, the second segment is used as system id ({@code 190724180219}).
 */
public final class RotorcraftCSVFileProcessor extends CSVFileProcessor {

    private static final String GARMIN_STYLE_FIRST_LINE_PREFIX = "#airframe_info";
    private static final String DIRECT_COLUMN_HEADER_PREFIX = "Lcl Date";
    private static final String APPAREO_HEADER_PREFIX = "Relative Time";
    /** Appareo exports use this column name; canonical series is {@link Parameters#UNIX_TIME_SECONDS}. */
    static final String APPAREO_UNIX_TIME_COLUMN = "UNIX Time";

    private final ParsedFilename parsed;

    public RotorcraftCSVFileProcessor(
            Connection connection, InputStream stream, String filename, Pipeline pipeline)
            throws IOException, FatalFlightFileException, SQLException {
        super(connection, stream, filename, pipeline);
        parsed = parseFilename(filename)
                .orElseThrow(() -> new FatalFlightFileException("Invalid rotorcraft filename: " + filename));
        applyRegistryIdentity(connection, meta, parsed);
    }

    /**
     * True when the filename yields a tail that exists in {@code tail_airframe_registry} as Rotorcraft.
     */
    public static boolean isRotorcraftUpload(Connection connection, String filename) throws SQLException {
        Optional<ParsedFilename> parsed = parseFilename(filename);
        if (parsed.isEmpty()) {
            return false;
        }
        return RotorcraftTailAirframeRegistry.findRotorcraft(connection, parsed.get().tail()).isPresent();
    }

    static boolean isGarminStyleRotorcraftHeader(String firstLine) {
        if (firstLine == null) {
            return false;
        }
        return firstLine.trim().regionMatches(true, 0, GARMIN_STYLE_FIRST_LINE_PREFIX, 0, GARMIN_STYLE_FIRST_LINE_PREFIX.length());
    }

    static boolean isDirectGarminColumnHeader(String firstLine) {
        if (firstLine == null) {
            return false;
        }
        return firstLine.trim().startsWith(DIRECT_COLUMN_HEADER_PREFIX);
    }

    static boolean isTimeseriesHeader(String firstLine) {
        return firstLine != null && firstLine.startsWith("Month,Day,Year");
    }

    static boolean isAppareoCommentLine(String line) {
        if (line == null) {
            return false;
        }
        String trimmed = line.trim();
        return trimmed.startsWith("#")
                && (trimmed.toLowerCase().contains("appareo") || trimmed.startsWith("# Converted Flight Data"));
    }

    static boolean isAppareoDataHeader(String line) {
        return line != null && line.trim().startsWith(APPAREO_HEADER_PREFIX);
    }

    /**
     * Basename first {@code _}-delimited segment is the tail; second segment is system id when present.
     */
    static Optional<ParsedFilename> parseFilename(String path) {
        String base = basenameWithoutExtension(path);
        if (base.isEmpty()) {
            return Optional.empty();
        }
        int firstUnderscore = base.indexOf('_');
        String tail = firstUnderscore < 0 ? base : base.substring(0, firstUnderscore);
        if (tail.isEmpty()) {
            return Optional.empty();
        }
        String systemId = "";
        if (firstUnderscore >= 0 && firstUnderscore + 1 < base.length()) {
            int secondUnderscore = base.indexOf('_', firstUnderscore + 1);
            systemId = secondUnderscore < 0
                    ? base.substring(firstUnderscore + 1)
                    : base.substring(firstUnderscore + 1, secondUnderscore);
        }
        return Optional.of(new ParsedFilename(tail, systemId));
    }

    static String basenameWithoutExtension(String path) {
        String name = path;
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        return name;
    }

    private static void applyRegistryIdentity(Connection connection, FlightMeta meta, ParsedFilename parsed)
            throws FatalFlightFileException, SQLException {
        Optional<RotorcraftTailAirframeRegistry.Entry> entry =
                RotorcraftTailAirframeRegistry.findRotorcraft(connection, parsed.tail());
        if (entry.isEmpty()) {
            throw new FatalFlightFileException("Tail '" + parsed.tail()
                    + "' is not in tail_airframe_registry as a Rotorcraft aircraft.");
        }
        String systemId = parsed.systemId();
        if (systemId.length() > 64) {
            systemId = systemId.substring(0, 64);
        }
        meta.setSystemId(systemId);
        meta.setSuggestedTailNumber(parsed.tail());
        meta.setAirframe(new Airframes.Airframe(
                connection, entry.get().airframe(), new Airframes.Type(entry.get().airframeType())));
    }

    @Override
    public Stream<FlightBuilder> parse() throws FlightProcessingException {
        try {
            stream.reset();
            meta.setMd5Hash(MD5.computeHexHash(super.stream));
            stream.reset();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        Map<String, DoubleTimeSeries> doubleTimeSeries = new HashMap<>();
        Map<String, StringTimeSeries> stringTimeSeries = new HashMap<>();

        List<String[]> rows = extractFlightData();
        readTimeSeries(rows, doubleTimeSeries, stringTimeSeries);
        addCanonicalUnixTimeSeries(doubleTimeSeries, stringTimeSeries);

        return Stream.of(makeFlightBuilder(meta, doubleTimeSeries, stringTimeSeries));
    }

    @Override
    FlightBuilder makeFlightBuilder(
            FlightMeta metaParam,
            Map<String, DoubleTimeSeries> doubleSeries,
            Map<String, StringTimeSeries> stringSeries) {
        return new RotorcraftFlightBuilder(metaParam, doubleSeries, stringSeries);
    }

    /**
     * Builds canonical {@link Parameters#UNIX_TIME_SECONDS} and {@link Parameters#UTC_DATE_TIME} from Appareo
     * {@value #APPAREO_UNIX_TIME_COLUMN} (absolute epoch seconds; {@code Relative Time} is not used).
     */
    static void addCanonicalUnixTimeSeries(
            Map<String, DoubleTimeSeries> doubleTimeSeries, Map<String, StringTimeSeries> stringTimeSeries) {
        if (doubleTimeSeries.containsKey(Parameters.UNIX_TIME_SECONDS)
                && stringTimeSeries.containsKey(Parameters.UTC_DATE_TIME)) {
            return;
        }

        DoubleTimeSeries unixSource = doubleTimeSeries.get(APPAREO_UNIX_TIME_COLUMN);
        if (unixSource == null) {
            return;
        }

        DoubleTimeSeries unixCanonical =
                new DoubleTimeSeries(Parameters.UNIX_TIME_SECONDS, Parameters.Unit.SECONDS.toString());
        StringTimeSeries utcCanonical = new StringTimeSeries(
                Parameters.UTC_DATE_TIME, Parameters.Unit.UTC_DATE_TIME.toString());

        for (int i = 0; i < unixSource.size(); i++) {
            double raw = unixSource.get(i);
            if (Double.isNaN(raw)) {
                unixCanonical.add(Double.NaN);
                utcCanonical.add("");
                continue;
            }
            double seconds = raw > 32503680000L ? raw / 1000.0 : raw;
            unixCanonical.add(seconds);
            utcCanonical.add(TimeUtils.convertUnixTimeToUTCDateTime(seconds));
        }

        doubleTimeSeries.put(Parameters.UNIX_TIME_SECONDS, unixCanonical);
        stringTimeSeries.put(Parameters.UTC_DATE_TIME, utcCanonical);
    }

    @Override
    List<String[]> extractFlightData() throws FlightProcessingException {
        try (BufferedReader bufferedReader =
                new BufferedReader(new InputStreamReader(super.stream, StandardCharsets.UTF_8))) {
            String firstLine = bufferedReader.readLine();
            if (firstLine == null) {
                throw new FlightProcessingException(new FatalFlightFileException("Rotorcraft CSV was empty."));
            }

            if (isGarminStyleRotorcraftHeader(firstLine)) {
                dataTypes = processDataTypes(bufferedReader);
                headers = processHeaders(bufferedReader);
            } else if (isAppareoCommentLine(firstLine) || isAppareoDataHeader(firstLine)) {
                readAppareoHeadersAndUnits(bufferedReader, firstLine);
            } else if (isDirectGarminColumnHeader(firstLine)) {
                headers = splitCommaSeparated(firstLine);
                dataTypes = emptyUnits(headers.size());
            } else if (isTimeseriesHeader(firstLine)) {
                headers = splitCommaSeparated(firstLine);
                String unitsLine = bufferedReader.readLine();
                if (unitsLine == null) {
                    throw new FlightProcessingException(
                            new FatalFlightFileException("Rotorcraft CSV missing units row."));
                }
                dataTypes = splitCommaSeparated(unitsLine);
            } else {
                throw new FlightProcessingException(new FatalFlightFileException(
                        "Unrecognized rotorcraft CSV header for tail '" + parsed.tail() + "'."));
            }

            if (headers.size() != dataTypes.size()) {
                throw new FlightProcessingException(new FatalFlightFileException(
                        "Rotorcraft CSV header column count does not match units row."));
            }

            CSVReader csvReader = new CSVReader(bufferedReader);
            return new ArrayList<>(csvReader.readAll());
        } catch (IOException | CsvException | FatalFlightFileException e) {
            throw new FlightProcessingException(e);
        }
    }

    private void readAppareoHeadersAndUnits(BufferedReader reader, String line)
            throws FatalFlightFileException, IOException {
        String headerLine = advancePastAppareoPreamble(reader, line);
        if (!isAppareoDataHeader(headerLine)) {
            throw new FatalFlightFileException(
                    "Expected Appareo header row beginning with " + APPAREO_HEADER_PREFIX + ".");
        }
        headers = splitCommaSeparated(headerLine);
        String unitsLine = reader.readLine();
        if (unitsLine == null) {
            throw new FatalFlightFileException("Appareo CSV missing units row.");
        }
        dataTypes = splitCommaSeparated(unitsLine);
    }

    /** Skips {@code #} comment lines and blank lines before the column header row. */
    static String advancePastAppareoPreamble(BufferedReader reader, String line) throws IOException {
        String current = line;
        while (current != null) {
            String trimmed = current.trim();
            if (trimmed.startsWith("#") || trimmed.isEmpty()) {
                current = reader.readLine();
                continue;
            }
            return current;
        }
        return null;
    }

    private static List<String> emptyUnits(int columnCount) {
        return new ArrayList<>(Collections.nCopies(columnCount, ""));
    }

    private static List<String> splitCommaSeparated(String line) {
        return Arrays.stream(line.split(",", -1)).map(String::strip).collect(Collectors.toList());
    }

    record ParsedFilename(String tail, String systemId) {}
}
