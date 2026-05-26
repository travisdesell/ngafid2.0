package org.ngafid.processor.format;

import ch.randelshofer.fastdoubleparser.JavaDoubleParser;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.OptionalInt;
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
 * CSV flight recorder exports for rotorcraft. Tail and airframe come from {@code tail_airframe_registry}:
 * standard {@code tail_...} filenames, or USCG IGS metadata ({@code Aircraft Serial Number,<tail>}).
 * <p>
 * Standard filenames use the first basename segment as tail ({@code 344Y} in
 * {@code 344Y_190724180219_NGAFID_FLT.csv}); the next segment is system id when present.
 * <p>
 * Supported layouts: USCG IGS, Garmin {@code #airframe_info}, direct {@code Lcl Date} columns, Appareo,
 * Metro OTL/HAA, and RT_Flight ({@code Month,Day,Year,...} header plus units row).
 */
public final class RotorcraftCSVFileProcessor extends CSVFileProcessor {

    private static final String GARMIN_STYLE_FIRST_LINE_PREFIX = "#airframe_info";
    private static final String DIRECT_COLUMN_HEADER_PREFIX = "Lcl Date";
    private static final String APPAREO_HEADER_PREFIX = "Relative Time";
    /** Appareo exports use this column name; canonical series is {@link Parameters#UNIX_TIME_SECONDS}. */
    static final String APPAREO_UNIX_TIME_COLUMN = "UNIX Time";

    static final String CALENDAR_MONTH = "Month";
    static final String CALENDAR_DAY = "Day";
    static final String CALENDAR_YEAR = "Year";
    static final String CALENDAR_HOUR = "Hour";
    static final String CALENDAR_MINUTE = "Minute";
    static final String CALENDAR_SECOND = "Second";

    /** Metro OTL/HAA exports: quoted CSV with {@code index} as the first column. */
    static final String METRO_INDEX_COLUMN = "index";

    static final String METRO_ANALOG_TIME_COLUMN = "Analog-time";

    private static final Pattern SYSTEM_ID_DATE_TIME =
            Pattern.compile("(\\d{4})(\\d{2})(\\d{2})T(\\d{2})(\\d{2})(\\d{2})");

    private static final DateTimeFormatter METRO_ANALOG_TIME =
            DateTimeFormatter.ofPattern("H:mm:ss");

    private static final String USCG_IGS_FIRST_LINE_PREFIX = "IGS v,";
    private static final String USCG_AIRCRAFT_SERIAL_KEY = "Aircraft Serial Number";
    private static final String USCG_DATA_HEADER_PREFIX = "Rec #,";
    private static final String USCG_UTC_DAY = "UTC_Day";
    private static final String USCG_UTC_MONTH = "UTC_Month";
    private static final String USCG_UTC_YEAR = "UTC_Year";
    private static final String USCG_UTC_TIME = "UTC_Time";
    static final String USCG_PNAV_LAT = "PNAV_Lat";
    static final String USCG_PNAV_LONG = "PNAV_Long";

    private static final Pattern USCG_FILENAME_DATE_TIME =
            Pattern.compile("(\\d{8})-(\\d{6})");

    private static final DateTimeFormatter USCG_UTC_TIME_OF_DAY =
            DateTimeFormatter.ofPattern("H:mm:ss[.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S]");

    private final ParsedFilename parsed;

    /** Resolves tail/system id from USCG metadata or filename, then loads airframe from the registry. */
    public RotorcraftCSVFileProcessor(
            Connection connection, InputStream stream, String filename, Pipeline pipeline)
            throws IOException, FatalFlightFileException, SQLException {
        super(connection, stream, filename, pipeline);
        parsed = resolveParsedFilename(connection, filename, stream);
        applyRegistryIdentity(connection, meta, parsed);
    }

    /**
     * Returns true when this CSV should use {@link RotorcraftCSVFileProcessor}: USCG metadata serial in the
     * registry, or a {@code tail_...} filename in the registry. Rewinds {@code reader} before returning.
     */
    public static boolean isRotorcraftUpload(Connection connection, String filename, BufferedReader reader)
            throws SQLException, IOException, FatalFlightFileException {
        if (reader != null) {
            reader.mark(512 * 1024);
            try {
                String firstLine = reader.readLine();
                if (resolveUscgRotorcraftIdentity(connection, reader, firstLine, filename).isPresent()) {
                    return true;
                }
                if (isUscgFormat(firstLine)) {
                    throw fatalUscgRegistryMiss(peekUscgAircraftSerialAfterReset(reader));
                }
            } finally {
                reader.reset();
            }
        }
        return isRotorcraftFilenameInRegistry(connection, filename);
    }

    /** True when the first {@code _}-delimited filename segment is a rotorcraft tail in the registry. */
    public static boolean isRotorcraftFilenameInRegistry(Connection connection, String filename) throws SQLException {
        Optional<ParsedFilename> parsed = parseFilename(filename);
        return parsed.isPresent()
                && RotorcraftTailAirframeRegistry.findRotorcraft(connection, parsed.get().tail()).isPresent();
    }

    /** True when the first line is a USCG IGS export ({@code IGS v,...}). */
    static boolean isUscgFormat(String firstLine) {
        String normalized = normalizeLine(firstLine);
        return normalized.startsWith(USCG_IGS_FIRST_LINE_PREFIX);
    }

    /** Trims a line and strips a leading UTF-8 BOM when present. */
    static String normalizeLine(String line) {
        if (line == null) {
            return "";
        }
        String trimmed = line.trim();
        if (trimmed.startsWith("\uFEFF")) {
            trimmed = trimmed.substring(1).trim();
        }
        return trimmed;
    }

    /**
     * Scans the IGS preamble for {@code Aircraft Serial Number,<tail>} and returns the substring after the comma.
     */
    static Optional<String> peekUscgAircraftSerial(BufferedReader reader, String firstLine) throws IOException {
        String prefix = USCG_AIRCRAFT_SERIAL_KEY + ",";
        String line = firstLine;
        while (line != null && !isUscgIgsSeparatorLine(line)) {
            String normalized = normalizeLine(line);
            if (normalized.regionMatches(true, 0, prefix, 0, prefix.length())) {
                String tail = normalized.substring(prefix.length());
                if (!tail.isEmpty()) {
                    return Optional.of(tail);
                }
            }
            line = reader.readLine();
        }
        return Optional.empty();
    }

    /**
     * For USCG files: reads metadata serial, looks up registry tail/airframe, and builds system id from the filename.
     */
    static Optional<ParsedFilename> resolveUscgRotorcraftIdentity(
            Connection connection, BufferedReader reader, String firstLine, String filename)
            throws IOException, SQLException {
        if (!isUscgFormat(firstLine)) {
            return Optional.empty();
        }
        Optional<String> serial = peekUscgAircraftSerial(reader, firstLine);
        if (serial.isEmpty()) {
            return Optional.empty();
        }
        Optional<RotorcraftTailAirframeRegistry.Entry> entry =
                RotorcraftTailAirframeRegistry.findRotorcraft(connection, serial.get());
        if (entry.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ParsedFilename(entry.get().tail(), parseUscgSystemIdFromFilename(filename)));
    }

    /**
     * Resolves tail and system id from USCG metadata or {@code tail_...} filename; fails if layout is unknown or
     * unregistered.
     */
    static ParsedFilename resolveParsedFilename(Connection connection, String filename, InputStream stream)
            throws FatalFlightFileException, SQLException, IOException {
        stream.mark(512 * 1024);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            reader.mark(512 * 1024);
            String firstLine = reader.readLine();
            Optional<ParsedFilename> fromContent =
                    resolveUscgRotorcraftIdentity(connection, reader, firstLine, filename);
            if (fromContent.isPresent()) {
                return fromContent.get();
            }
            if (isUscgFormat(firstLine)) {
                throw fatalUscgRegistryMiss(peekUscgAircraftSerialAfterReset(reader));
            }
        } finally {
            stream.reset();
        }
        Optional<ParsedFilename> fromFilename = parseFilename(filename);
        if (fromFilename.isPresent()
                && RotorcraftTailAirframeRegistry.findRotorcraft(connection, fromFilename.get().tail()).isPresent()) {
            return fromFilename.get();
        }
        throw new FatalFlightFileException("Invalid rotorcraft filename or unrecognized layout: " + filename);
    }

    /** Rewinds the reader and re-reads the IGS preamble to obtain the metadata serial for error messages. */
    private static Optional<String> peekUscgAircraftSerialAfterReset(BufferedReader reader) throws IOException {
        reader.reset();
        reader.mark(512 * 1024);
        return peekUscgAircraftSerial(reader, reader.readLine());
    }

    /** Builds an error for a USCG file with a missing or unregistered {@code Aircraft Serial Number}. */
    private static FatalFlightFileException fatalUscgRegistryMiss(Optional<String> serial) {
        if (serial.isEmpty()) {
            return new FatalFlightFileException("USCG IGS file is missing 'Aircraft Serial Number' metadata.");
        }
        return new FatalFlightFileException("USCG aircraft serial '" + serial.get()
                + "' is not in tail_airframe_registry as Rotorcraft. Apply database migrations or add"
                + " the tail to the registry.");
    }

    /** True for the dashed line between IGS metadata and the {@code Rec #,...} data header. */
    static boolean isUscgIgsSeparatorLine(String line) {
        if (line == null) {
            return false;
        }
        String trimmed = line.trim();
        return trimmed.length() >= 10 && trimmed.chars().allMatch(ch -> ch == '-');
    }

    /** Returns the USCG datetime prefix from the basename (e.g. {@code 20240125-134952C} from {@code 20240125-134952C_...}). */
    static String parseUscgSystemIdFromFilename(String path) {
        String base = basenameWithoutExtension(path);
        int underscore = base.indexOf('_');
        if (underscore <= 0) {
            return "";
        }
        String prefix = base.substring(0, underscore);
        if (USCG_FILENAME_DATE_TIME.matcher(prefix).find()) {
            return prefix;
        }
        return "";
    }

    /** True when the file starts with Garmin {@code #airframe_info}. */
    static boolean isGarminStyleRotorcraftHeader(String firstLine) {
        String normalized = normalizeLine(firstLine);
        return normalized.regionMatches(
                true, 0, GARMIN_STYLE_FIRST_LINE_PREFIX, 0, GARMIN_STYLE_FIRST_LINE_PREFIX.length());
    }

    /** True when the first row is a Garmin column header beginning with {@code Lcl Date}. */
    static boolean isDirectGarminColumnHeader(String firstLine) {
        return normalizeLine(firstLine).startsWith(DIRECT_COLUMN_HEADER_PREFIX);
    }

    /** True for RT_Flight / Pfizer layouts ({@code Month,Day,Year,...} header row). */
    static boolean isRtFlightHeader(String firstLine) {
        return normalizeLine(firstLine).startsWith("Month,Day,Year");
    }

    /** True for Appareo {@code #} comment lines in the preamble. */
    static boolean isAppareoCommentLine(String line) {
        if (line == null) {
            return false;
        }
        String trimmed = line.trim();
        return trimmed.startsWith("#")
                && (trimmed.toLowerCase().contains("appareo") || trimmed.startsWith("# Converted Flight Data"));
    }

    /** True when a row is the Appareo data header ({@code Relative Time,...}). */
    static boolean isAppareoDataHeader(String line) {
        return line != null && line.trim().startsWith(APPAREO_HEADER_PREFIX);
    }

    /** True for Metro OTL/HAA quoted CSV whose first column is {@code index}. */
    static boolean isMetroOtlHeader(String firstLine) {
        if (firstLine == null) {
            return false;
        }
        try {
            List<String> fields = parseCsvFields(firstLine);
            return !fields.isEmpty() && METRO_INDEX_COLUMN.equalsIgnoreCase(fields.get(0));
        } catch (IOException | CsvException e) {
            return false;
        }
    }

    /**
     * Parses {@code tail} and optional {@code systemId} from the basename ({@code 344Y_190724180219_...}).
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

    /** Returns the filename without path or extension. */
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

    /** Sets {@link FlightMeta} tail, system id, and airframe from a registry lookup. */
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
        meta.setSuggestedTailNumber(entry.get().tail());
        meta.setAirframe(new Airframes.Airframe(
                connection, entry.get().airframe(), new Airframes.Type(entry.get().airframeType())));
    }

    /**
     * Parses rows, builds canonical time/position columns for the detected layout, and promotes aliases for storage.
     */
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
        addCanonicalTimeFromCalendarColumns(doubleTimeSeries, stringTimeSeries);
        addCanonicalTimeFromMetroAnalogTime(doubleTimeSeries, stringTimeSeries, parsed.systemId());
        addCanonicalTimeFromUscgUtcColumns(doubleTimeSeries, stringTimeSeries);
        RotorcraftFlightBuilder.promoteForPersistence(doubleTimeSeries, stringTimeSeries);

        return Stream.of(makeFlightBuilder(meta, doubleTimeSeries, stringTimeSeries));
    }

    /** Uses {@link RotorcraftFlightBuilder} so recorder column aliases apply during ingest. */
    @Override
    FlightBuilder makeFlightBuilder(
            FlightMeta metaParam,
            Map<String, DoubleTimeSeries> doubleSeries,
            Map<String, StringTimeSeries> stringSeries) {
        return new RotorcraftFlightBuilder(metaParam, doubleSeries, stringSeries);
    }

    /** Fills canonical unix/UTC series from Appareo {@value #APPAREO_UNIX_TIME_COLUMN} (epoch seconds). */
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

    /** Builds canonical time from {@code Month/Day/Year/Hour/Minute/Second} columns (interpreted as UTC). */
    static void addCanonicalTimeFromCalendarColumns(
            Map<String, DoubleTimeSeries> doubleTimeSeries, Map<String, StringTimeSeries> stringTimeSeries) {
        if (doubleTimeSeries.containsKey(Parameters.UNIX_TIME_SECONDS)
                && stringTimeSeries.containsKey(Parameters.UTC_DATE_TIME)) {
            return;
        }

        DoubleTimeSeries month = doubleTimeSeries.get(CALENDAR_MONTH);
        DoubleTimeSeries day = doubleTimeSeries.get(CALENDAR_DAY);
        DoubleTimeSeries year = doubleTimeSeries.get(CALENDAR_YEAR);
        DoubleTimeSeries hour = doubleTimeSeries.get(CALENDAR_HOUR);
        DoubleTimeSeries minute = doubleTimeSeries.get(CALENDAR_MINUTE);
        DoubleTimeSeries second = doubleTimeSeries.get(CALENDAR_SECOND);
        if (month == null || day == null || year == null || hour == null || minute == null || second == null) {
            return;
        }

        DoubleTimeSeries unixCanonical =
                new DoubleTimeSeries(Parameters.UNIX_TIME_SECONDS, Parameters.Unit.SECONDS.toString());
        StringTimeSeries utcCanonical = new StringTimeSeries(
                Parameters.UTC_DATE_TIME, Parameters.Unit.UTC_DATE_TIME.toString());

        for (int i = 0; i < month.size(); i++) {
            if (!hasCalendarSample(month, day, year, hour, minute, second, i)) {
                unixCanonical.add(Double.NaN);
                utcCanonical.add("");
                continue;
            }

            LocalDateTime local = LocalDateTime.of(
                    calendarComponent(year, i),
                    calendarComponent(month, i),
                    calendarComponent(day, i),
                    calendarComponent(hour, i),
                    calendarComponent(minute, i),
                    calendarComponent(second, i));
            OffsetDateTime utc = OffsetDateTime.of(local, ZoneOffset.UTC);
            unixCanonical.add(utc.toEpochSecond());
            utcCanonical.add(utc.format(TimeUtils.ISO_8601_FORMAT));
        }

        doubleTimeSeries.put(Parameters.UNIX_TIME_SECONDS, unixCanonical);
        stringTimeSeries.put(Parameters.UTC_DATE_TIME, utcCanonical);
    }

    /** Combines flight date from system id with each {@code Analog-time} sample (Metro OTL/HAA). */
    static void addCanonicalTimeFromMetroAnalogTime(
            Map<String, DoubleTimeSeries> doubleTimeSeries,
            Map<String, StringTimeSeries> stringTimeSeries,
            String systemId) {
        if (doubleTimeSeries.containsKey(Parameters.UNIX_TIME_SECONDS)
                && stringTimeSeries.containsKey(Parameters.UTC_DATE_TIME)) {
            return;
        }

        StringTimeSeries analogTime = stringTimeSeries.get(METRO_ANALOG_TIME_COLUMN);
        if (analogTime == null) {
            return;
        }

        Optional<LocalDate> flightDate = parseDateFromSystemId(systemId);
        if (flightDate.isEmpty()) {
            return;
        }

        DoubleTimeSeries unixCanonical =
                new DoubleTimeSeries(Parameters.UNIX_TIME_SECONDS, Parameters.Unit.SECONDS.toString());
        StringTimeSeries utcCanonical = new StringTimeSeries(
                Parameters.UTC_DATE_TIME, Parameters.Unit.UTC_DATE_TIME.toString());

        for (int i = 0; i < analogTime.size(); i++) {
            String sample = analogTime.get(i);
            if (sample == null || sample.isBlank()) {
                unixCanonical.add(Double.NaN);
                utcCanonical.add("");
                continue;
            }

            try {
                LocalTime time = LocalTime.parse(sample.trim(), METRO_ANALOG_TIME);
                LocalDateTime local = LocalDateTime.of(flightDate.get(), time);
                OffsetDateTime utc = OffsetDateTime.of(local, ZoneOffset.UTC);
                unixCanonical.add(utc.toEpochSecond());
                utcCanonical.add(utc.format(TimeUtils.ISO_8601_FORMAT));
            } catch (DateTimeParseException e) {
                unixCanonical.add(Double.NaN);
                utcCanonical.add("");
            }
        }

        doubleTimeSeries.put(Parameters.UNIX_TIME_SECONDS, unixCanonical);
        stringTimeSeries.put(Parameters.UTC_DATE_TIME, utcCanonical);
    }

    /** Builds canonical time from USCG {@code UTC_Day/Month/Year} and {@code UTC_Time} columns. */
    static void addCanonicalTimeFromUscgUtcColumns(
            Map<String, DoubleTimeSeries> doubleTimeSeries, Map<String, StringTimeSeries> stringTimeSeries) {
        if (doubleTimeSeries.containsKey(Parameters.UNIX_TIME_SECONDS)
                && stringTimeSeries.containsKey(Parameters.UTC_DATE_TIME)) {
            return;
        }

        int size = uscgUtcSeriesSize(doubleTimeSeries, stringTimeSeries);
        if (size <= 0) {
            return;
        }

        DoubleTimeSeries unixCanonical =
                new DoubleTimeSeries(Parameters.UNIX_TIME_SECONDS, Parameters.Unit.SECONDS.toString());
        StringTimeSeries utcCanonical = new StringTimeSeries(
                Parameters.UTC_DATE_TIME, Parameters.Unit.UTC_DATE_TIME.toString());

        for (int i = 0; i < size; i++) {
            OptionalInt day = uscgCalendarComponent(doubleTimeSeries, stringTimeSeries, USCG_UTC_DAY, i);
            OptionalInt month = uscgCalendarComponent(doubleTimeSeries, stringTimeSeries, USCG_UTC_MONTH, i);
            OptionalInt year = uscgCalendarComponent(doubleTimeSeries, stringTimeSeries, USCG_UTC_YEAR, i);
            if (day.isEmpty() || month.isEmpty() || year.isEmpty()) {
                unixCanonical.add(Double.NaN);
                utcCanonical.add("");
                continue;
            }
            String sample = uscgTimeOfDaySample(doubleTimeSeries, stringTimeSeries, i);
            if (sample == null || sample.isBlank()) {
                unixCanonical.add(Double.NaN);
                utcCanonical.add("");
                continue;
            }
            try {
                LocalTime time = LocalTime.parse(sample.trim(), USCG_UTC_TIME_OF_DAY);
                LocalDateTime local = LocalDateTime.of(
                        year.getAsInt(), month.getAsInt(), day.getAsInt(),
                        time.getHour(), time.getMinute(), time.getSecond(), time.getNano());
                OffsetDateTime utc = OffsetDateTime.of(local, ZoneOffset.UTC);
                unixCanonical.add(utc.toEpochSecond());
                utcCanonical.add(utc.format(TimeUtils.ISO_8601_FORMAT));
            } catch (DateTimeParseException e) {
                unixCanonical.add(Double.NaN);
                utcCanonical.add("");
            }
        }

        doubleTimeSeries.put(Parameters.UNIX_TIME_SECONDS, unixCanonical);
        stringTimeSeries.put(Parameters.UTC_DATE_TIME, utcCanonical);
    }

    /** Row count from the first present USCG UTC column (double or string series). */
    private static int uscgUtcSeriesSize(
            Map<String, DoubleTimeSeries> doubleTimeSeries, Map<String, StringTimeSeries> stringTimeSeries) {
        for (String name : List.of(USCG_UTC_DAY, USCG_UTC_MONTH, USCG_UTC_YEAR, USCG_UTC_TIME)) {
            DoubleTimeSeries d = doubleTimeSeries.get(name);
            if (d != null) {
                return d.size();
            }
            StringTimeSeries s = stringTimeSeries.get(name);
            if (s != null) {
                return s.size();
            }
        }
        return 0;
    }

    private static OptionalInt uscgCalendarComponent(
            Map<String, DoubleTimeSeries> doubles,
            Map<String, StringTimeSeries> strings,
            String column,
            int index) {
        DoubleTimeSeries numeric = doubles.get(column);
        if (numeric != null) {
            double value = numeric.get(index);
            if (!Double.isNaN(value)) {
                return OptionalInt.of((int) Math.round(value));
            }
        }
        StringTimeSeries text = strings.get(column);
        if (text != null) {
            String sample = text.get(index);
            if (sample != null && !sample.isBlank()) {
                try {
                    return OptionalInt.of((int) Math.round(Double.parseDouble(sample.trim())));
                } catch (NumberFormatException ignored) {
                    return OptionalInt.empty();
                }
            }
        }
        return OptionalInt.empty();
    }

    /** Returns {@code UTC_Time} as a string at {@code index}, formatting numeric seconds-of-day when needed. */
    private static String uscgTimeOfDaySample(
            Map<String, DoubleTimeSeries> doubles, Map<String, StringTimeSeries> strings, int index) {
        StringTimeSeries text = strings.get(USCG_UTC_TIME);
        if (text != null) {
            return text.get(index);
        }
        DoubleTimeSeries numeric = doubles.get(USCG_UTC_TIME);
        if (numeric == null) {
            return null;
        }
        double seconds = numeric.get(index);
        if (Double.isNaN(seconds)) {
            return null;
        }
        return LocalTime.ofSecondOfDay((int) seconds % 86400).format(USCG_UTC_TIME_OF_DAY);
    }

    /** Converts USCG {@code PNAV_Lat}/{@code PNAV_Long} DMS strings to decimal {@code Latitude}/{@code Longitude}. */
    static void addCanonicalPositionFromPnavDms(
            Map<String, DoubleTimeSeries> doubleTimeSeries, Map<String, StringTimeSeries> stringTimeSeries) {
        if (doubleTimeSeries.containsKey(Parameters.LATITUDE)) {
            return;
        }

        StringTimeSeries latSource = resolveStringSeries(stringTimeSeries, USCG_PNAV_LAT);
        StringTimeSeries lonSource = resolveStringSeries(stringTimeSeries, USCG_PNAV_LONG);
        if (latSource == null || lonSource == null || latSource.size() != lonSource.size()) {
            return;
        }

        DoubleTimeSeries latitude =
                new DoubleTimeSeries(Parameters.LATITUDE, Parameters.Unit.DEGREES.toString());
        DoubleTimeSeries longitude =
                new DoubleTimeSeries(Parameters.LONGITUDE, Parameters.Unit.DEGREES.toString());
        int validPoints = 0;

        for (int i = 0; i < latSource.size(); i++) {
            double lat = parseDmsCoordinate(latSource.get(i));
            double lon = parseDmsCoordinate(lonSource.get(i));
            latitude.add(lat);
            longitude.add(lon);
            if (!Double.isNaN(lat) && !Double.isNaN(lon) && lat != 0.0 && lon != 0.0) {
                validPoints++;
            }
        }

        if (validPoints < 1) {
            return;
        }

        doubleTimeSeries.put(Parameters.LATITUDE, latitude);
        doubleTimeSeries.put(Parameters.LONGITUDE, longitude);
    }

    /** Looks up a string series by exact or case-insensitive trimmed column name. */
    private static StringTimeSeries resolveStringSeries(Map<String, StringTimeSeries> stringTimeSeries, String name) {
        StringTimeSeries direct = stringTimeSeries.get(name);
        if (direct != null) {
            return direct;
        }
        for (Map.Entry<String, StringTimeSeries> entry : stringTimeSeries.entrySet()) {
            if (entry.getKey() != null && entry.getKey().trim().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /** Parses {@code deg:min:sec[.frac]} (optional leading minus) to decimal degrees; NaN on failure. */
    static double parseDmsCoordinate(String value) {
        if (value == null || value.isBlank()) {
            return Double.NaN;
        }
        String trimmed = value.trim();
        boolean negative = trimmed.startsWith("-");
        if (negative) {
            trimmed = trimmed.substring(1).trim();
        }
        String[] parts = trimmed.split(":");
        if (parts.length == 0 || parts[0].isBlank()) {
            return Double.NaN;
        }
        try {
            double degrees = Double.parseDouble(parts[0].trim());
            double minutes = parts.length > 1 ? Double.parseDouble(parts[1].trim()) : 0.0;
            double seconds = parts.length > 2 ? Double.parseDouble(parts[2].trim()) : 0.0;
            double decimal = degrees + (minutes / 60.0) + (seconds / 3600.0);
            return negative ? -decimal : decimal;
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    /** Skips IGS metadata, then reads {@code Rec #,...} header and units rows into {@code headers}/{@code dataTypes}. */
    private void readUscgIgsHeadersAndUnits(BufferedReader reader, String firstLine)
            throws FatalFlightFileException, IOException, CsvException {
        String line = firstLine;
        while (line != null && !isUscgIgsSeparatorLine(line)) {
            line = reader.readLine();
        }
        if (line == null) {
            throw new FatalFlightFileException("USCG IGS CSV missing metadata separator.");
        }
        String headerLine = reader.readLine();
        if (headerLine == null || !headerLine.trim().startsWith(USCG_DATA_HEADER_PREFIX)) {
            throw new FatalFlightFileException("USCG IGS CSV missing Rec # header row.");
        }
        headers = splitCommaSeparated(headerLine);
        String unitsLine = reader.readLine();
        if (unitsLine == null) {
            throw new FatalFlightFileException("USCG IGS CSV missing units row.");
        }
        dataTypes = splitCommaSeparated(unitsLine);
        dedupeColumnNames(headers);
    }

    /** Parses {@code yyyyMMdd} from a system id containing {@code yyyyMMddTHHmmss}. */
    static Optional<LocalDate> parseDateFromSystemId(String systemId) {
        if (systemId == null || systemId.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = SYSTEM_ID_DATE_TIME.matcher(systemId);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(LocalDate.of(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3))));
    }

    /** True when all six RT_Flight calendar components are non-NaN at {@code index}. */
    private static boolean hasCalendarSample(
            DoubleTimeSeries month,
            DoubleTimeSeries day,
            DoubleTimeSeries year,
            DoubleTimeSeries hour,
            DoubleTimeSeries minute,
            DoubleTimeSeries second,
            int index) {
        return !Double.isNaN(month.get(index))
                && !Double.isNaN(day.get(index))
                && !Double.isNaN(year.get(index))
                && !Double.isNaN(hour.get(index))
                && !Double.isNaN(minute.get(index))
                && !Double.isNaN(second.get(index));
    }

    /** Rounds a calendar column value to an integer component. */
    private static int calendarComponent(DoubleTimeSeries series, int index) {
        return (int) Math.round(series.get(index));
    }

    /**
     * Classifies each column as numeric or string by scanning for the first parseable cell (not middle-row only).
     */
    @Override
    void readTimeSeries(
            List<String[]> rows,
            Map<String, DoubleTimeSeries> doubleTimeSeries,
            Map<String, StringTimeSeries> stringTimeSeries)
            throws FlightProcessingException {
        ArrayList<ArrayList<String>> columns = new ArrayList<>();
        for (int j = 0; j < headers.size(); j++) {
            columns.add(new ArrayList<>());
        }

        int validRows = 0;
        for (String[] row : rows) {
            if (row.length != headers.size()) {
                break;
            }
            for (int i = 0; i < row.length; i++) {
                columns.get(i).add(row[i]);
            }
            validRows += 1;
        }

        if (validRows <= Math.max(rows.size() - 2, 0)) {
            throw new FlightProcessingException(
                    new FatalFlightFileException("Flight has 0 rows, or consecutive malformed rows -- "
                            + "there is a serious problem with the file format."));
        }

        for (int j = 0; j < columns.size(); j++) {
            var columnData = columns.get(j);
            var name = headers.get(j);
            var dataType = dataTypes.get(j);
            if (columnLooksNumeric(columnData)) {
                try {
                    doubleTimeSeries.put(name, new DoubleTimeSeries(name, dataType, columnData));
                } catch (NumberFormatException e) {
                    stringTimeSeries.put(name, new StringTimeSeries(name, dataType, columnData));
                }
            } else {
                stringTimeSeries.put(name, new StringTimeSeries(name, dataType, columnData));
            }
        }
    }

    /** True if any non-empty cell in the column parses as a double. */
    static boolean columnLooksNumeric(List<String> columnData) {
        for (String cell : columnData) {
            if (cell == null || cell.trim().isEmpty()) {
                continue;
            }
            try {
                JavaDoubleParser.parseDouble(cell.trim());
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    /**
     * Detects rotorcraft CSV layout from the first line, sets {@code headers}/{@code dataTypes}, and reads data rows.
     */
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
            } else if (isRtFlightHeader(firstLine)) {
                headers = splitCommaSeparated(firstLine);
                String unitsLine = bufferedReader.readLine();
                if (unitsLine == null) {
                    throw new FlightProcessingException(
                            new FatalFlightFileException("Rotorcraft CSV missing units row."));
                }
                dataTypes = splitCommaSeparated(unitsLine);
                dedupeColumnNames(headers);
            } else if (isMetroOtlHeader(firstLine)) {
                headers = parseCsvFields(firstLine);
                dataTypes = emptyUnits(headers.size());
            } else if (isUscgFormat(firstLine)) {
                readUscgIgsHeadersAndUnits(bufferedReader, firstLine);
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

    /** Skips Appareo preamble, then reads column header and units rows. */
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

    /** Advances past Appareo {@code #} comments and blank lines to the column header row. */
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

    /** Returns one empty unit string per column when the export has no units row. */
    private static List<String> emptyUnits(int columnCount) {
        return new ArrayList<>(Collections.nCopies(columnCount, ""));
    }

    /** Appends {@code [n]} to repeated header names so column keys stay unique. */
    static void dedupeColumnNames(List<String> headers) {
        Map<String, Integer> seen = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String name = headers.get(i);
            int count = seen.merge(name, 1, Integer::sum);
            if (count > 1) {
                headers.set(i, name + " [" + count + "]");
            }
        }
    }

    /** Splits a simple comma-separated line and strips each field (no quoted-field handling). */
    private static List<String> splitCommaSeparated(String line) {
        return Arrays.stream(line.split(",", -1)).map(String::strip).collect(Collectors.toList());
    }

    /** Parses one CSV record with OpenCSV (handles quoted fields for Metro headers). */
    private static List<String> parseCsvFields(String line) throws IOException, CsvException {
        try (CSVReader reader = new CSVReader(new StringReader(line))) {
            String[] fields = reader.readNext();
            if (fields == null) {
                throw new IOException("CSV row was empty.");
            }
            return Arrays.stream(fields).map(String::strip).collect(Collectors.toList());
        }
    }

    /** Tail and optional system id resolved from filename or USCG metadata. */
    record ParsedFilename(String tail, String systemId) {}
}
