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
import java.sql.Connection;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    private static final Logger LOG = Logger.getLogger(RotorcraftCSVFileProcessor.class.getName());

    private static final String GARMIN_STYLE_FIRST_LINE_PREFIX = "#airframe_info";
    private static final String DIRECT_COLUMN_HEADER_PREFIX = "Lcl Date";
    private static final String APPAREO_HEADER_PREFIX = "Relative Time";
    /** Appareo exports use this column name; canonical series is {@link Parameters#UNIX_TIME_SECONDS}. */
    static final String APPAREO_UNIX_TIME_COLUMN = "UNIX Time";

    private static final String SKYTRAC_FDM_FIRST_LINE = "SkyTrac FDM Log";
    static final String SKYTRAC_DATE_COLUMN = "Date";
    static final String SKYTRAC_TIME_COLUMN = "Time";

    private static final List<DateTimeFormatter> SKYTRAC_DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("d/M/yyyy H:mm:ss"),
            DateTimeFormatter.ofPattern("d/M/yyyy H:m:s"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

    static final String CALENDAR_MONTH = "Month";
    static final String CALENDAR_DAY = "Day";
    static final String CALENDAR_YEAR = "Year";
    static final String CALENDAR_HOUR = "Hour";
    static final String CALENDAR_MINUTE = "Minute";
    static final String CALENDAR_SECOND = "Second";

    /** Metro OTL/HAA exports: quoted CSV with {@code index} as the first column. */
    static final String METRO_INDEX_COLUMN = "index";

    static final String METRO_ANALOG_TIME_COLUMN = "Analog-time";

    /** MPIO2 Metro OTL/HAA exports use {@code ANALOG.UTC} instead of {@code Analog-time}. */
    static final String METRO_MPIO2_UTC_TIME_COLUMN = "ANALOG.UTC";

    private static final List<String> METRO_TIME_COLUMNS =
            List.of(METRO_ANALOG_TIME_COLUMN, METRO_MPIO2_UTC_TIME_COLUMN);

    private static final Pattern SYSTEM_ID_DATE_TIME =
            Pattern.compile("(\\d{4})(\\d{2})(\\d{2})T(\\d{2})(\\d{2})(\\d{2})");

    private static final DateTimeFormatter METRO_ANALOG_TIME = DateTimeFormatter.ofPattern("H:mm:ss");

    /** ISO-friendly local date/time for Cesium ({@code JulianDate.fromIso8601}). */
    private static final DateTimeFormatter LCL_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter LCL_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final String USCG_IGS_FIRST_LINE_PREFIX = "IGS v,";
    private static final String USCG_AIRCRAFT_SERIAL_KEY = "Aircraft Serial Number";
    private static final String USCG_DDF_PATH_KEY = "DDF Path";
    private static final String USCG_DATA_HEADER_PREFIX = "Rec #,";
    private static final String USCG_UTC_DAY = "UTC_Day";
    private static final String USCG_UTC_MONTH = "UTC_Month";
    private static final String USCG_UTC_YEAR = "UTC_Year";
    private static final String USCG_UTC_TIME = "UTC_Time";
    static final String USCG_PNAV_LAT = "PNAV_Lat";
    static final String USCG_PNAV_LONG = "PNAV_Long";

    private static final Pattern USCG_FILENAME_DATE_TIME = Pattern.compile("(\\d{8})-(\\d{6})");

    private static final DateTimeFormatter USCG_UTC_TIME_OF_DAY =
            DateTimeFormatter.ofPattern("H:mm:ss[.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S]");

    private final ParsedFilename parsed;

    /**
     * Resolves tail/system id from USCG metadata or filename, then loads airframe from the registry.
     * @param connection the database connection
     * @param stream the input stream to inspect
     * @param filename the filename to inspect
     * @param pipeline the pipeline coordinating the upload
     * @throws IOException if an I/O error occurs
     * @throws FatalFlightFileException if the file format is invalid or the airframe cannot be resolved
     * @throws SQLException if a database error occurs
     */
    public RotorcraftCSVFileProcessor(
            Connection connection, InputStream stream, String filename, Pipeline pipeline)
            throws IOException, FatalFlightFileException, SQLException {
        super(connection, stream, filename, pipeline);
        Optional<String> garminAirframeName = peekGarminAirframeName(stream);
        parsed = resolveParsedFilename(connection, filename, stream);
        applyRegistryIdentity(connection, meta, parsed, garminAirframeName);
    }

    /**
     * Returns true when this CSV should use {@link RotorcraftCSVFileProcessor}: USCG metadata serial in the
     * registry, or a filename tail prefix present in {@code tail_airframe_registry}. A Garmin-style header alone
     * is not enough (fixed-wing logs such as {@code log_*.csv} also use {@code #airframe_info}). Rewinds
     * {@code reader} before returning.
     * @param connection the database connection
     * @param filename the filename to inspect
     * @param reader the reader to inspect
     * @return true if the upload should be handled as a rotorcraft CSV
     * @throws SQLException if a database error occurs
     * @throws IOException if an I/O error occurs
     * @throws FatalFlightFileException if the file format is invalid or the airframe cannot be resolved
     */
    public static boolean isRotorcraftUpload(Connection connection, String filename, BufferedReader reader)
            throws SQLException, IOException, FatalFlightFileException {
        Optional<ParsedFilename> parsed = parseFilename(filename);
        if (reader != null) {
            reader.mark(512 * 1024);
            try {
                String firstLine = reader.readLine();
                if (resolveUscgRotorcraftIdentity(connection, reader, firstLine, filename)
                        .isPresent()) {
                    return true;
                }
                if (isUscgFormat(firstLine)) {
                    throw fatalUscgRegistryMiss(peekUscgAircraftSerialAfterReset(reader));
                }
                if (parsed.isPresent()) {
                    String tail = parsed.get().tail();
                    if (RotorcraftTailAirframeRegistry.findRotorcraft(connection, tail)
                            .isPresent()) {
                        return true;
                    }
                }
            } finally {
                reader.reset();
            }
        }
        if (parsed.isPresent()
                && RotorcraftTailAirframeRegistry.findRotorcraft(
                                connection, parsed.get().tail())
                        .isPresent()) {
            return true;
        }
        if (parsed.isPresent()) {
            LOG.info(() -> "Filename tail prefix '" + parsed.get().tail() + "' from '" + filename
                    + "' is not in tail_airframe_registry and the file start does not match a known rotorcraft CSV"
                    + " layout; standard CSV processing will be used.");
        }
        return false;
    }

    /**
     * Describes a supported rotorcraft CSV layout from the first line, or empty when the format is not recognized.
     * @param firstLine the first line to inspect
     * @return the detected rotorcraft CSV layout, if recognized
     */
    static Optional<String> describeRotorcraftCsvLayout(String firstLine) {
        if (firstLine == null) {
            return Optional.empty();
        }
        if (isGarminStyleRotorcraftHeader(firstLine)) {
            return Optional.of("Garmin #airframe_info");
        }
        if (isAppareoRotorcraftCsv(firstLine)) {
            return Optional.of("Appareo converted CSV");
        }
        if (isDirectGarminColumnHeader(firstLine)) {
            return Optional.of("Garmin direct column header (Lcl Date)");
        }
        if (isRtFlightHeader(firstLine)) {
            return Optional.of("RT_Flight (Month,Day,Year,...)");
        }
        if (isMetroOtlHeader(firstLine)) {
            return Optional.of("Metro OTL/HAA");
        }
        if (isSkyTracFdmLog(firstLine)) {
            return Optional.of("SkyTrac FDM log");
        }
        return Optional.empty();
    }

    /**
     * Error when the filename tail is missing from {@code tail_airframe_registry} for a known rotorcraft CSV layout.
     * @param tail the tail value to describe
     * @param filename the filename to inspect
     * @param recorderAirframeName the recorder airframe name, if present
     * @param layoutDescription the detected layout description, if present
     * @return the fatal exception describing the missing tail registry entry
     */
    static FatalFlightFileException fatalTailRegistryMiss(
            String tail, String filename, Optional<String> recorderAirframeName, Optional<String> layoutDescription) {
        StringBuilder message =
                new StringBuilder("Filename tail prefix '").append(tail).append("'");
        if (filename != null && !filename.isBlank()) {
            message.append(" from '").append(filename).append("'");
        }
        message.append(" is not in tail_airframe_registry as a Rotorcraft aircraft.");
        layoutDescription.ifPresent(layout -> message.append(" Detected rotorcraft CSV layout: ")
                .append(layout)
                .append("."));
        recorderAirframeName.ifPresent(
                name -> message.append(" File airframe_name is '").append(name).append("'."));
        recorderAirframeName
                .flatMap(Airframes::resolveGarminRotorcraftAirframeCode)
                .ifPresent(code -> message.append(" Suggested: INSERT INTO tail_airframe_registry (tail, airframe_id)")
                        .append(" SELECT '")
                        .append(tail)
                        .append("', a.id FROM airframes a WHERE a.airframe = '")
                        .append(code)
                        .append("';"));
        if (recorderAirframeName.flatMap(Airframes::resolveGarminRotorcraftAirframeCode).isEmpty()) {
            message.append(" Add: INSERT INTO tail_airframe_registry (tail, airframe_id)")
                    .append(" SELECT '")
                    .append(tail)
                    .append("', a.id FROM airframes a WHERE a.airframe = '<airframe_code>';");
        }
        return new FatalFlightFileException(message.toString());
    }

    /**
     * True for Appareo converted exports ({@code # Converted Flight Data} / {@code Relative Time,...}).
     * @param firstLine the first line to inspect
     * @return true if the condition is met
     */
    static boolean isAppareoRotorcraftCsv(String firstLine) {
        return isAppareoCommentLine(firstLine) || isAppareoDataHeader(firstLine);
    }

    /**
     * Reads {@code airframe_name} from a Garmin {@code #airframe_info} first line.
     * @param firstLine the first line to inspect
     * @return the Garmin airframe name, if present
     */
    static Optional<String> peekGarminAirframeName(String firstLine) {
        if (!isGarminStyleRotorcraftHeader(firstLine)) {
            return Optional.empty();
        }
        String normalized = normalizeLine(firstLine).replace("\"", "");
        String[] parts = normalized.split(",");
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.isEmpty()) {
                continue;
            }
            String[] keyValue = part.split("=", 2);
            if (keyValue.length == 2 && "airframe_name".equalsIgnoreCase(keyValue[0].trim())) {
                String value = keyValue[1].trim();
                return value.isEmpty() ? Optional.empty() : Optional.of(value);
            }
        }
        return Optional.empty();
    }

    /**
     * Peeks the stream start for Garmin {@code airframe_name} when present.
     * @param stream the input stream to inspect
     * @return the Garmin airframe name, if present
     * @throws IOException if an I/O error occurs
     */
    private static Optional<String> peekGarminAirframeName(InputStream stream) throws IOException {
        stream.mark(512 * 1024);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return peekGarminAirframeName(reader.readLine());
        } finally {
            stream.reset();
        }
    }

    /**
     * True when the first {@code _}-delimited filename segment is a rotorcraft tail in the registry.
     * @param connection the database connection
     * @param filename the filename to inspect
     * @return true if the filename tail is present in the rotorcraft registry
     * @throws SQLException if a database error occurs
     */
    public static boolean isRotorcraftFilenameInRegistry(Connection connection, String filename) throws SQLException {
        Optional<ParsedFilename> parsed = parseFilename(filename);
        return parsed.isPresent()
                && RotorcraftTailAirframeRegistry.findRotorcraft(
                                connection, parsed.get().tail())
                        .isPresent();
    }

    /**
     * True when the first line is a USCG IGS export ({@code IGS v,...}).
     * @param firstLine the first line to inspect
     * @return true if the first line matches the USCG IGS format
     */
    static boolean isUscgFormat(String firstLine) {
        String normalized = normalizeLine(firstLine);
        return normalized.startsWith(USCG_IGS_FIRST_LINE_PREFIX);
    }

    /**
     * Trims a line and strips a leading UTF-8 BOM when present.
     * @param line the line to inspect
     * @return the normalized line
     */
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
     * Scans the IGS preamble for {@code key,<value>} and returns the substring after the first comma.
     * @param reader the reader to inspect
     * @param firstLine the first line to inspect
     * @param key the metadata key to look up
     * @return the matching metadata value, if present
     * @throws IOException if an I/O error occurs
     */
    static Optional<String> peekUscgMetadataValue(BufferedReader reader, String firstLine, String key)
            throws IOException {
        String prefix = key + ",";
        String line = firstLine;
        while (line != null && !isUscgIgsSeparatorLine(line)) {
            String normalized = normalizeLine(line);
            if (normalized.regionMatches(true, 0, prefix, 0, prefix.length())) {
                String value = normalized.substring(prefix.length()).trim();
                if (!value.isEmpty()) {
                    return Optional.of(value);
                }
            }
            line = reader.readLine();
        }
        return Optional.empty();
    }

    /**
     * Scans the IGS preamble for {@code Aircraft Serial Number,<tail>}.
     * @param reader the reader to inspect
     * @param firstLine the first line to inspect
     * @return the aircraft serial value, if present
     * @throws IOException if an I/O error occurs
     */
    static Optional<String> peekUscgAircraftSerial(BufferedReader reader, String firstLine) throws IOException {
        return peekUscgMetadataValue(reader, firstLine, USCG_AIRCRAFT_SERIAL_KEY);
    }

    /**
     * For USCG files: resolves bureau tail from metadata and filename, then loads registry airframe.
     * <p>
     * USCG exports are inconsistent: {@code Aircraft Serial Number} may be the bureau tail ({@code 6032}),
     * the airframe model ({@code MH60}), or another id; {@code DDF Path} and the upload basename often carry
     * the bureau tail when the serial field does not.
     * @param connection the database connection
     * @param reader the reader to inspect
     * @param firstLine the first line to inspect
     * @param filename the filename to inspect
     * @return the resolved rotorcraft identity, if present
     * @throws IOException if an I/O error occurs
     * @throws SQLException if a database error occurs
     */
    static Optional<ParsedFilename> resolveUscgRotorcraftIdentity(
            Connection connection, BufferedReader reader, String firstLine, String filename)
            throws IOException, SQLException {
        if (!isUscgFormat(firstLine)) {
            return Optional.empty();
        }
        reader.mark(512 * 1024);
        try {
            Optional<String> serial = peekUscgAircraftSerial(reader, firstLine);
            Optional<String> ddfPath = peekUscgMetadataValue(reader, firstLine, USCG_DDF_PATH_KEY);
            reader.reset();
            reader.mark(512 * 1024);

            List<String> candidates = new ArrayList<>();
            serial.ifPresent(value -> {
                if (!isLikelyUscgAirframeTypeCode(value)) {
                    candidates.addAll(expandUscgTailCandidates(value));
                }
            });
            ddfPath.ifPresent(path -> candidates.addAll(expandUscgTailCandidates(parseUscgDdfBasename(path))));
            parseUscgTailFromFilename(filename).ifPresent(tail -> candidates.addAll(expandUscgTailCandidates(tail)));

            for (String candidate : candidates) {
                Optional<ParsedFilename> resolved = lookupUscgRegistryTail(connection, candidate, filename);
                if (resolved.isPresent()) {
                    if (serial.isPresent() && !candidate.equals(serial.get().trim())) {
                        LOG.info(() -> "USCG tail '" + candidate + "' resolved from DDF path or filename"
                                + " (Aircraft Serial Number was '" + serial.get() + "').");
                    }
                    return resolved;
                }
            }
            return Optional.empty();
        } finally {
            reader.reset();
        }
    }

    /**
     * Basename of {@code DDF Path} value (e.g. {@code 6039_08_29_20241} from {@code 6039_08_29_20241.DDF}).
     * @param ddfPath the DDF path to inspect
     * @return the DDF basename without its extension
     */
    static String parseUscgDdfBasename(String ddfPath) {
        String trimmed = ddfPath.trim();
        int slash = Math.max(trimmed.lastIndexOf('/'), trimmed.lastIndexOf('\\'));
        if (slash >= 0) {
            trimmed = trimmed.substring(slash + 1);
        }
        int dot = trimmed.lastIndexOf('.');
        if (dot > 0) {
            trimmed = trimmed.substring(0, dot);
        }
        return trimmed;
    }

    /**
     * Possible bureau tails from a token ({@code 6039}, {@code 657503BAM} → also tries leading {@code 6575}).
     * @param raw the raw tail candidate to expand
     * @return the possible tail candidates derived from the raw value
     */
    static List<String> expandUscgTailCandidates(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String trimmed = raw.trim();
        List<String> candidates = new ArrayList<>();
        addUniqueTailCandidate(candidates, trimmed);
        int underscore = trimmed.indexOf('_');
        if (underscore > 0) {
            addUniqueTailCandidate(candidates, trimmed.substring(0, underscore));
        }
        if (trimmed.length() > 4 && Character.isDigit(trimmed.charAt(0))) {
            int digitEnd = 0;
            while (digitEnd < trimmed.length() && Character.isDigit(trimmed.charAt(digitEnd))) {
                digitEnd++;
            }
            if (digitEnd >= 4) {
                addUniqueTailCandidate(candidates, trimmed.substring(0, 4));
            }
        }
        return candidates;
    }

    private static void addUniqueTailCandidate(List<String> candidates, String tail) {
        if (tail == null || tail.isBlank()) {
            return;
        }
        String normalized = tail.trim();
        if (!candidates.contains(normalized)) {
            candidates.add(normalized);
        }
    }

    private static Optional<ParsedFilename> lookupUscgRegistryTail(Connection connection, String tail, String filename)
            throws SQLException {
        return RotorcraftTailAirframeRegistry.findRotorcraft(connection, tail)
                .map(entry -> new ParsedFilename(entry.tail(), parseUscgSystemIdFromFilename(filename)));
    }

    /**
     * Bureau tail from USCG export basenames: {@code yyyyMMdd-HHmmssC_<tail>_...} (e.g. {@code ..._6039_...}).
     * @param path the path to parse
     * @return the parsed USCG tail, if present
     */
    static Optional<String> parseUscgTailFromFilename(String path) {
        String base = basenameWithoutExtension(path);
        int firstUnderscore = base.indexOf('_');
        if (firstUnderscore <= 0 || firstUnderscore + 1 >= base.length()) {
            return Optional.empty();
        }
        String datetimePrefix = base.substring(0, firstUnderscore);
        if (!USCG_FILENAME_DATE_TIME.matcher(datetimePrefix).find()) {
            return Optional.empty();
        }
        int secondUnderscore = base.indexOf('_', firstUnderscore + 1);
        String tail = secondUnderscore < 0
                ? base.substring(firstUnderscore + 1)
                : base.substring(firstUnderscore + 1, secondUnderscore);
        return tail.isEmpty() ? Optional.empty() : Optional.of(tail);
    }

    /**
     * Resolves tail and system id from USCG metadata or {@code tail_...} filename; fails if layout is unknown or
     * unregistered.
     * @param connection the database connection
     * @param filename the filename to inspect
     * @param stream the input stream to inspect
     * @return the resolved parsed filename data
     * @throws FatalFlightFileException if the file format is invalid or the airframe cannot be resolved
     * @throws SQLException if a database error occurs
     * @throws IOException if an I/O error occurs
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
            Optional<ParsedFilename> fromFilename = parseFilename(filename);
            if (fromFilename.isPresent()) {
                String tail = fromFilename.get().tail();
                if (RotorcraftTailAirframeRegistry.findRotorcraft(connection, tail)
                        .isPresent()) {
                    return fromFilename.get();
                }
            }
        } finally {
            stream.reset();
        }
        throw new FatalFlightFileException("Invalid rotorcraft filename or unrecognized layout: " + filename);
    }

    /**
     * Rewinds the reader and re-reads the IGS preamble to obtain the metadata serial for error messages.
     * @param reader the reader to inspect
     * @return the resolved value, if present
     * @throws IOException if an I/O error occurs
     */
    private static Optional<String> peekUscgAircraftSerialAfterReset(BufferedReader reader) throws IOException {
        reader.reset();
        reader.mark(512 * 1024);
        return peekUscgAircraftSerial(reader, reader.readLine());
    }

    /**
     * Builds an error for a USCG file with a missing or unregistered {@code Aircraft Serial Number}.
     * @param serial the serial value to inspect
     * @return the fatal exception describing the registry lookup failure
     */
    private static FatalFlightFileException fatalUscgRegistryMiss(Optional<String> serial) {
        if (serial.isEmpty()) {
            return new FatalFlightFileException("USCG IGS file is missing 'Aircraft Serial Number' metadata.");
        }
        String value = serial.get();
        String hint = isLikelyUscgAirframeTypeCode(value)
                ? " Aircraft Serial Number is an airframe model (e.g. MH60), not a bureau tail;"
                        + " use DDF Path, filename, or serial 6039-style ids."
                : "";
        return new FatalFlightFileException("Could not resolve a USCG bureau tail in tail_airframe_registry."
                + " Aircraft Serial Number was '" + value + "'." + hint
                + " Also checked DDF Path and the upload filename. Add the tail to the registry if missing.");
    }

    /**
     * True when metadata serial matches a known rotorcraft airframe code rather than a bureau tail.
     * @param serial the serial value to inspect
     * @return true if the serial looks like an airframe type code
     */
    private static boolean isLikelyUscgAirframeTypeCode(String serial) {
        return "MH60".equalsIgnoreCase(serial) || "MH65".equalsIgnoreCase(serial);
    }

    /**
     * True for the dashed line between IGS metadata and the {@code Rec #,...} data header.
     * @param line the line to inspect
     * @return true if the line is the USCG metadata separator
     */
    static boolean isUscgIgsSeparatorLine(String line) {
        if (line == null) {
            return false;
        }
        String trimmed = line.trim();
        return trimmed.length() >= 10 && trimmed.chars().allMatch(ch -> ch == '-');
    }

    /**
     * Returns the USCG datetime prefix from the basename (for example,
     * {@code 20240125-134952C} from {@code 20240125-134952C_...}).
     * @param path the path to parse
     * @return the parsed USCG system id prefix
     */
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

    /**
     * True when the file starts with Garmin {@code #airframe_info}.
     * @param firstLine the first line to inspect
     * @return true if the file starts with a Garmin rotorcraft header
     */
    static boolean isGarminStyleRotorcraftHeader(String firstLine) {
        String normalized = normalizeLine(firstLine);
        return normalized.regionMatches(
                true, 0, GARMIN_STYLE_FIRST_LINE_PREFIX, 0, GARMIN_STYLE_FIRST_LINE_PREFIX.length());
    }

    /**
     * True when the first row is a Garmin column header beginning with {@code Lcl Date}.
     * @param firstLine the first line to inspect
     * @return true if the first row is a Garmin data header
     */
    static boolean isDirectGarminColumnHeader(String firstLine) {
        return normalizeLine(firstLine).startsWith(DIRECT_COLUMN_HEADER_PREFIX);
    }

    /**
     * True for RT_Flight / Pfizer layouts ({@code Month,Day,Year,...} header row).
     * @param firstLine the first line to inspect
     * @return true if the first row is an RT_Flight header
     */
    static boolean isRtFlightHeader(String firstLine) {
        return normalizeLine(firstLine).startsWith("Month,Day,Year");
    }

    /**
     * True for Appareo {@code #} comment lines in the preamble.
     * @param line the line to inspect
     * @return true if the line is an Appareo comment line
     */
    static boolean isAppareoCommentLine(String line) {
        if (line == null) {
            return false;
        }
        String trimmed = line.trim();
        return trimmed.startsWith("#")
                && (trimmed.toLowerCase().contains("appareo") || trimmed.startsWith("# Converted Flight Data"));
    }

    /**
     * True when a row is the Appareo data header ({@code Relative Time,...}).
     * @param line the line to inspect
     * @return true if the line is an Appareo data header
     */
    static boolean isAppareoDataHeader(String line) {
        return line != null && line.trim().startsWith(APPAREO_HEADER_PREFIX);
    }

    /**
     * True for Metro OTL/HAA quoted CSV whose first column is {@code index}.
     * @param firstLine the first line to inspect
     * @return true if the file starts with a Metro OTL header
     */
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
     * @param path the path to parse
     * @return the parsed filename data, if present
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

    /**
     * Returns the filename without path or extension.
     * @param path the path to parse
     * @return the filename without its path or extension
     */
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

    /**
     * Sets {@link FlightMeta} tail, stored system id, and airframe from a registry lookup.
     * @param connection the database connection
     * @param meta the flight metadata to update
     * @param parsed the parsed filename data
     * @param recorderAirframeName the recorder airframe name, if present
     * @throws FatalFlightFileException if the file format is invalid or the airframe cannot be resolved
     * @throws SQLException if a database error occurs
     */
    private static void applyRegistryIdentity(
            Connection connection, FlightMeta meta, ParsedFilename parsed, Optional<String> recorderAirframeName)
            throws FatalFlightFileException, SQLException {
        Optional<String> airframeFromFile =
                recorderAirframeName.flatMap(Airframes::resolveGarminRotorcraftAirframeCode);
        Optional<RotorcraftTailAirframeRegistry.Entry> entry =
                RotorcraftTailAirframeRegistry.findRotorcraft(connection, parsed.tail());
        if (entry.isEmpty()) {
            throw fatalTailRegistryMiss(parsed.tail(), meta.getFilename(), recorderAirframeName, Optional.empty());
        }
        airframeFromFile.ifPresent(csvCode -> {
            if (!csvCode.equals(entry.get().airframe())) {
                LOG.log(
                        Level.WARNING,
                        "Registry airframe ''{0}'' for tail ''{1}'' differs from file ''{2}'' (resolved to ''{3}'')",
                        new Object[] {entry.get().airframe(), parsed.tail(), recorderAirframeName.orElse(""), csvCode});
            }
        });
        // For rotorcraft uploads, we store the operator tail (registry tail) as the system id.
        // The filename-derived token (parsed.systemId) is still used inside {@link #parse()} for
        // layout-specific fallbacks like Metro OTL/HAA date parsing.
        String systemId = entry.get().tail();
        if (systemId.length() > 64) {
            systemId = systemId.substring(0, 64);
        }
        meta.setSystemId(systemId);
        meta.setSuggestedTailNumber(entry.get().tail());
        meta.setAirframe(new Airframes.Airframe(
                connection,
                entry.get().airframe(),
                new Airframes.Type(entry.get().airframeType())));
    }

    /**
     * Parses rows, builds canonical time/position columns for the detected layout, and promotes aliases for storage.
     * @return the parsed flight builders
     * @throws FlightProcessingException if the flight data cannot be processed
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
        addCanonicalTimeFromSkyTracDateTime(doubleTimeSeries, stringTimeSeries);
        RotorcraftFlightBuilder.promoteForPersistence(doubleTimeSeries, stringTimeSeries);

        return Stream.of(makeFlightBuilder(meta, doubleTimeSeries, stringTimeSeries));
    }

    /**
     * Uses {@link RotorcraftFlightBuilder} so recorder column aliases apply during ingest.
     * @param metaParam the flight metadata to use
     * @param doubleSeries the numeric time series keyed by column name
     * @param stringSeries the string time series keyed by column name
     * @return the make flight builder
     */
    @Override
    FlightBuilder makeFlightBuilder(
            FlightMeta metaParam,
            Map<String, DoubleTimeSeries> doubleSeries,
            Map<String, StringTimeSeries> stringSeries) {
        return new RotorcraftFlightBuilder(metaParam, doubleSeries, stringSeries);
    }

    /**
     * True when the file begins with {@value #SKYTRAC_FDM_FIRST_LINE}.
     * @param firstLine the first line to inspect
     * @return true if the file is a SkyTrac FDM log
     */
    static boolean isSkyTracFdmLog(String firstLine) {
        return firstLine != null && normalizeLine(firstLine).equalsIgnoreCase(SKYTRAC_FDM_FIRST_LINE);
    }

    /**
     * True for the SkyTrac column header row ({@code Date, Time, Latitude,...}).
     * @param line the line to inspect
     * @return true if the line is a SkyTrac data header
     */
    static boolean isSkyTracDataHeader(String line) {
        if (line == null) {
            return false;
        }
        String trimmed = normalizeLine(line);
        return trimmed.regionMatches(true, 0, SKYTRAC_DATE_COLUMN + ",", 0, (SKYTRAC_DATE_COLUMN + ",").length())
                && trimmed.toLowerCase(Locale.ROOT).contains("latitude");
    }

    /**
     * Builds canonical time from SkyTrac {@value #SKYTRAC_DATE_COLUMN} / {@value #SKYTRAC_TIME_COLUMN}
     * ({@code dd/mm/yyyy} + {@code hh:mm:ss}, interpreted as UTC).
     * @param doubleTimeSeries the numeric time series keyed by column name
     * @param stringTimeSeries the string time series keyed by column name
     */
    static void addCanonicalTimeFromSkyTracDateTime(
            Map<String, DoubleTimeSeries> doubleTimeSeries, Map<String, StringTimeSeries> stringTimeSeries) {
        if (doubleTimeSeries.containsKey(Parameters.UNIX_TIME_SECONDS)
                && stringTimeSeries.containsKey(Parameters.UTC_DATE_TIME)) {
            return;
        }

        StringTimeSeries dates = stringTimeSeries.get(SKYTRAC_DATE_COLUMN);
        StringTimeSeries times = stringTimeSeries.get(SKYTRAC_TIME_COLUMN);
        if (dates == null || times == null || dates.size() != times.size()) {
            return;
        }

        DoubleTimeSeries unixCanonical =
                new DoubleTimeSeries(Parameters.UNIX_TIME_SECONDS, Parameters.Unit.SECONDS.toString());
        StringTimeSeries utcCanonical =
                new StringTimeSeries(Parameters.UTC_DATE_TIME, Parameters.Unit.UTC_DATE_TIME.toString());

        for (int i = 0; i < dates.size(); i++) {
            String date = dates.get(i);
            String time = times.get(i);
            if (date == null
                    || time == null
                    || date.trim().isEmpty()
                    || time.trim().isEmpty()) {
                unixCanonical.add(Double.NaN);
                utcCanonical.add("");
                continue;
            }

            Optional<LocalDateTime> parsed = parseSkyTracLocalDateTime(date.trim(), time.trim());
            if (parsed.isEmpty()) {
                unixCanonical.add(Double.NaN);
                utcCanonical.add("");
                continue;
            }

            OffsetDateTime utc = OffsetDateTime.of(parsed.get(), ZoneOffset.UTC);
            unixCanonical.add(utc.toEpochSecond());
            utcCanonical.add(utc.format(TimeUtils.ISO_8601_FORMAT));
        }

        doubleTimeSeries.put(Parameters.UNIX_TIME_SECONDS, unixCanonical);
        stringTimeSeries.put(Parameters.UTC_DATE_TIME, utcCanonical);
    }

    static Optional<LocalDateTime> parseSkyTracLocalDateTime(String date, String time) {
        String dateTime = date + " " + time;
        for (DateTimeFormatter formatter : SKYTRAC_DATE_TIME_FORMATTERS) {
            try {
                return Optional.of(LocalDateTime.parse(dateTime, formatter));
            } catch (DateTimeParseException ignored) {
                // try next pattern
            }
        }
        return Optional.empty();
    }

    /**
     * Fills canonical unix/UTC series from Appareo {@value #APPAREO_UNIX_TIME_COLUMN} (epoch seconds).
     * @param doubleTimeSeries the numeric time series keyed by column name
     * @param stringTimeSeries the string time series keyed by column name
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
        StringTimeSeries utcCanonical =
                new StringTimeSeries(Parameters.UTC_DATE_TIME, Parameters.Unit.UTC_DATE_TIME.toString());

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

    /**
     * Builds canonical time from {@code Month/Day/Year/Hour/Minute/Second} columns (interpreted as UTC).
     * @param doubleTimeSeries the numeric time series keyed by column name
     * @param stringTimeSeries the string time series keyed by column name
     */
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
        StringTimeSeries utcCanonical =
                new StringTimeSeries(Parameters.UTC_DATE_TIME, Parameters.Unit.UTC_DATE_TIME.toString());

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

    /**
     * Combines flight date from system id with each Metro time sample (legacy
     * {@code Analog-time} or MPIO2 {@code ANALOG.UTC}).
     * @param doubleTimeSeries the numeric time series keyed by column name
     * @param stringTimeSeries the string time series keyed by column name
     * @param systemId the system id to inspect
     */
    static void addCanonicalTimeFromMetroAnalogTime(
            Map<String, DoubleTimeSeries> doubleTimeSeries,
            Map<String, StringTimeSeries> stringTimeSeries,
            String systemId) {
        if (doubleTimeSeries.containsKey(Parameters.UNIX_TIME_SECONDS)
                && stringTimeSeries.containsKey(Parameters.UTC_DATE_TIME)) {
            return;
        }

        StringTimeSeries analogTime = findMetroTimeColumn(stringTimeSeries);
        if (analogTime == null) {
            return;
        }

        Optional<LocalDate> flightDate = parseDateFromSystemId(systemId);
        if (flightDate.isEmpty()) {
            return;
        }

        DoubleTimeSeries unixCanonical =
                new DoubleTimeSeries(Parameters.UNIX_TIME_SECONDS, Parameters.Unit.SECONDS.toString());
        StringTimeSeries utcCanonical =
                new StringTimeSeries(Parameters.UTC_DATE_TIME, Parameters.Unit.UTC_DATE_TIME.toString());

        LocalDate currentDate = flightDate.get();
        LocalTime previousTime = null;

        for (int i = 0; i < analogTime.size(); i++) {
            String sample = analogTime.get(i);
            if (sample == null || sample.isBlank()) {
                unixCanonical.add(Double.NaN);
                utcCanonical.add("");
                continue;
            }

            try {
                LocalTime time = LocalTime.parse(sample.trim(), METRO_ANALOG_TIME);
                if (previousTime != null && time.isBefore(previousTime)) {
                    currentDate = currentDate.plusDays(1);
                }
                LocalDateTime local = LocalDateTime.of(currentDate, time);
                previousTime = time;
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

    static StringTimeSeries findMetroTimeColumn(Map<String, StringTimeSeries> stringTimeSeries) {
        for (String column : METRO_TIME_COLUMNS) {
            StringTimeSeries series = stringTimeSeries.get(column);
            if (series != null) {
                return series;
            }
        }
        return null;
    }

    /**
     * Builds canonical time from USCG {@code UTC_Day/Month/Year} and {@code UTC_Time} columns.
     * @param doubleTimeSeries the numeric time series keyed by column name
     * @param stringTimeSeries the string time series keyed by column name
     */
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
        StringTimeSeries utcCanonical =
                new StringTimeSeries(Parameters.UTC_DATE_TIME, Parameters.Unit.UTC_DATE_TIME.toString());

        for (int i = 0; i < size; i++) {
            OptionalInt day = uscgCalendarComponent(doubleTimeSeries, stringTimeSeries, USCG_UTC_DAY, i);
            OptionalInt month = uscgCalendarComponent(doubleTimeSeries, stringTimeSeries, USCG_UTC_MONTH, i);
            OptionalInt year = uscgCalendarComponent(doubleTimeSeries, stringTimeSeries, USCG_UTC_YEAR, i);
            if (day.isEmpty() || month.isEmpty() || year.isEmpty()) {
                unixCanonical.add(Double.NaN);
                utcCanonical.add("");
                continue;
            }
            int yearValue = year.getAsInt();
            int monthValue = month.getAsInt();
            int dayValue = day.getAsInt();
            if (!isValidUscgCalendarDate(yearValue, monthValue, dayValue)) {
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
                        yearValue,
                        monthValue,
                        dayValue,
                        time.getHour(),
                        time.getMinute(),
                        time.getSecond(),
                        time.getNano());
                OffsetDateTime utc = OffsetDateTime.of(local, ZoneOffset.UTC);
                unixCanonical.add(utc.toEpochSecond());
                utcCanonical.add(utc.format(TimeUtils.ISO_8601_FORMAT));
            } catch (DateTimeException e) {
                unixCanonical.add(Double.NaN);
                utcCanonical.add("");
            }
        }

        doubleTimeSeries.put(Parameters.UNIX_TIME_SECONDS, unixCanonical);
        stringTimeSeries.put(Parameters.UTC_DATE_TIME, utcCanonical);
    }

    /**
     * Fills {@link Parameters#LCL_DATE}, {@link Parameters#LCL_TIME}, and {@link Parameters#UTC_OFFSET} from
     * {@link Parameters#UTC_DATE_TIME} when Garmin local columns are absent (e.g. Appareo, Metro, USCG).
     *
     * @param stringTimeSeries string series keyed by column name
     */
    static void addCanonicalLocalDateTimeFromUtc(Map<String, StringTimeSeries> stringTimeSeries) {
        if (stringTimeSeries.containsKey(Parameters.LCL_DATE)) {
            return;
        }

        StringTimeSeries utc = stringTimeSeries.get(Parameters.UTC_DATE_TIME);
        if (utc == null) {
            return;
        }

        StringTimeSeries lclDate = new StringTimeSeries(Parameters.LCL_DATE, "");
        StringTimeSeries lclTime = new StringTimeSeries(Parameters.LCL_TIME, "");
        StringTimeSeries utcOffset = new StringTimeSeries(Parameters.UTC_OFFSET, "");

        for (int i = 0; i < utc.size(); i++) {
            String sample = utc.get(i);
            if (sample == null || sample.isBlank()) {
                lclDate.add("");
                lclTime.add("");
                utcOffset.add("");
                continue;
            }

            try {
                OffsetDateTime odt = TimeUtils.parseUTC(sample.trim());
                LocalDateTime local = odt.toLocalDateTime();
                lclDate.add(local.format(LCL_DATE_FORMAT));
                lclTime.add(local.format(LCL_TIME_FORMAT));
                utcOffset.add(formatUtcOffsetForGarmin(odt));
            } catch (DateTimeParseException e) {
                lclDate.add("");
                lclTime.add("");
                utcOffset.add("");
            }
        }

        stringTimeSeries.put(Parameters.LCL_DATE, lclDate);
        stringTimeSeries.put(Parameters.LCL_TIME, lclTime);
        stringTimeSeries.put(Parameters.UTC_OFFSET, utcOffset);
    }

    private static String formatUtcOffsetForGarmin(OffsetDateTime odt) {
        if (odt.getOffset().equals(ZoneOffset.UTC)) {
            return "+00:00";
        }
        return odt.getOffset().getId();
    }

    /**
     * Row count from the first present USCG UTC column (double or string series).
     * @param doubleTimeSeries the numeric time series keyed by column name
     * @param stringTimeSeries the string time series keyed by column name
     * @return the row count from the first present USCG UTC series
     */
    private static int uscgUtcSeriesSize(
            Map<String, DoubleTimeSeries> doubleTimeSeries, Map<String, StringTimeSeries> stringTimeSeries) {
        for (String name : List.of(USCG_UTC_DAY, USCG_UTC_MONTH, USCG_UTC_YEAR, USCG_UTC_TIME)) {
            DoubleTimeSeries d = resolveDoubleSeries(doubleTimeSeries, name);
            if (d != null) {
                return d.size();
            }
            StringTimeSeries s = resolveStringSeries(stringTimeSeries, name);
            if (s != null) {
                return s.size();
            }
        }
        return 0;
    }

    private static OptionalInt uscgCalendarComponent(
            Map<String, DoubleTimeSeries> doubles, Map<String, StringTimeSeries> strings, String column, int index) {
        DoubleTimeSeries numeric = resolveDoubleSeries(doubles, column);
        if (numeric != null) {
            double value = numeric.get(index);
            if (!Double.isNaN(value)) {
                return OptionalInt.of((int) Math.round(value));
            }
        }
        StringTimeSeries text = resolveStringSeries(strings, column);
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

    /**
     * Rejects corrupt USCG UTC calendar samples (recorder often leaves time-of-day but clears date).
     * @param year the year series
     * @param month the month series
     * @param day the day series
     * @return true if the USCG calendar date is valid
     */
    static boolean isValidUscgCalendarDate(int year, int month, int day) {
        return month >= 1 && month <= 12 && day >= 1 && day <= 31 && year >= 1970 && year <= 2100;
    }

    /**
     * Returns {@code UTC_Time} as a string at {@code index}, formatting numeric seconds-of-day when needed.
     * @param doubles the numeric time series keyed by column name
     * @param strings the string time series keyed by column name
     * @param index the row index
     * @return the UTC time-of-day sample at the requested index
     */
    private static String uscgTimeOfDaySample(
            Map<String, DoubleTimeSeries> doubles, Map<String, StringTimeSeries> strings, int index) {
        StringTimeSeries text = resolveStringSeries(strings, USCG_UTC_TIME);
        if (text != null) {
            return text.get(index);
        }
        DoubleTimeSeries numeric = resolveDoubleSeries(doubles, USCG_UTC_TIME);
        if (numeric == null) {
            return null;
        }
        double seconds = numeric.get(index);
        if (Double.isNaN(seconds)) {
            return null;
        }
        return LocalTime.ofSecondOfDay((int) seconds % 86400).format(USCG_UTC_TIME_OF_DAY);
    }

    /**
     * Converts USCG {@code PNAV_Lat}/{@code PNAV_Long} DMS strings to decimal {@code Latitude}/{@code Longitude}.
     * @param doubleTimeSeries the numeric time series keyed by column name
     * @param stringTimeSeries the string time series keyed by column name
     */
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

        DoubleTimeSeries latitude = new DoubleTimeSeries(Parameters.LATITUDE, Parameters.Unit.DEGREES.toString());
        DoubleTimeSeries longitude = new DoubleTimeSeries(Parameters.LONGITUDE, Parameters.Unit.DEGREES.toString());
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

    /**
     * True when two recorder column names match, ignoring case and spaces vs underscores.
     * @param actual the actual column name
     * @param expected the expected column name
     * @return true if the column names match
     */
    static boolean columnNamesMatch(String actual, String expected) {
        if (actual == null || expected == null) {
            return false;
        }
        return normalizeColumnName(actual).equals(normalizeColumnName(expected));
    }

    static String normalizeColumnName(String name) {
        return name.trim().replace(' ', '_').toLowerCase(Locale.ROOT);
    }

    /**
     * Looks up a string series by exact or normalized column name.
     * @param stringTimeSeries the string time series keyed by column name
     * @param name the series name to resolve
     * @return the matching string series, if present
     */
    private static StringTimeSeries resolveStringSeries(Map<String, StringTimeSeries> stringTimeSeries, String name) {
        StringTimeSeries direct = stringTimeSeries.get(name);
        if (direct != null) {
            return direct;
        }
        for (Map.Entry<String, StringTimeSeries> entry : stringTimeSeries.entrySet()) {
            if (columnNamesMatch(entry.getKey(), name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Looks up a double series by exact or normalized column name.
     * @param doubleTimeSeries the numeric time series keyed by column name
     * @param name the series name to resolve
     * @return the matching double series, if present
     */
    private static DoubleTimeSeries resolveDoubleSeries(Map<String, DoubleTimeSeries> doubleTimeSeries, String name) {
        DoubleTimeSeries direct = doubleTimeSeries.get(name);
        if (direct != null) {
            return direct;
        }
        for (Map.Entry<String, DoubleTimeSeries> entry : doubleTimeSeries.entrySet()) {
            if (columnNamesMatch(entry.getKey(), name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Parses {@code deg:min:sec[.frac]} (optional leading minus) to decimal degrees; NaN on failure.
     * @param value the coordinate value to parse
     * @return the parsed decimal-degree coordinate
     */
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

    /**
     * Skips IGS metadata, then reads {@code Rec #,...} header and units rows into {@code headers}/{@code dataTypes}.
     * @param reader the reader to inspect
     * @param firstLine the first line to inspect
     * @throws FatalFlightFileException if the file format is invalid or the airframe cannot be resolved
     * @throws IOException if an I/O error occurs
     * @throws CsvException if the CSV row cannot be parsed
     */
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

    /**
     * Parses {@code yyyyMMdd} from a system id containing {@code yyyyMMddTHHmmss}.
     * @param systemId the system id to inspect
     * @return the parsed flight date, if present
     */
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

    /**
     * True when all six RT_Flight calendar components are non-NaN at {@code index}.
     * @param month the month series
     * @param day the day series
     * @param year the year series
     * @param hour the hour series
     * @param minute the minute series
     * @param second the second series
     * @param index the row index
     * @return true if all calendar components are present at that index
     */
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

    /**
     * Rounds a calendar column value to an integer component.
     * @param series the time series to read from
     * @param index the row index
     * @return the rounded calendar component value
     */
    private static int calendarComponent(DoubleTimeSeries series, int index) {
        return (int) Math.round(series.get(index));
    }

    /**
     * Classifies each column as numeric or string by scanning for the first parseable cell (not middle-row only).
     * @param rows the parsed CSV rows
     * @param doubleTimeSeries the numeric time series keyed by column name
     * @param stringTimeSeries the string time series keyed by column name
     * @throws FlightProcessingException if the flight data cannot be processed
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

    /**
     * True if any non-empty cell in the column parses as a double.
     * @param columnData the column values to inspect
     * @return true if the column looks numeric
     */
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
     * @return the resulting values
     * @throws FlightProcessingException if the flight data cannot be processed
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
                skipGarminExtraHeaderRowIfPresent(bufferedReader);
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
            } else if (isSkyTracFdmLog(firstLine)) {
                readSkyTracHeadersAndUnits(bufferedReader, firstLine);
            } else {
                throw new FlightProcessingException(new FatalFlightFileException(
                        "Unrecognized rotorcraft CSV header for tail '" + parsed.tail() + "'."));
            }

            if (headers.size() != dataTypes.size()) {
                throw new FlightProcessingException(
                        new FatalFlightFileException("Rotorcraft CSV header column count does not match units row."));
            }

            CSVReader csvReader = new CSVReader(bufferedReader);
            return new ArrayList<>(csvReader.readAll());
        } catch (IOException | CsvException | FatalFlightFileException e) {
            throw new FlightProcessingException(e);
        }
    }

    /**
     * Skips SkyTrac metadata lines, then reads column header and units rows.
     * @param reader the reader to inspect
     * @param line the line to inspect
     * @throws FatalFlightFileException if the file format is invalid or the airframe cannot be resolved
     * @throws IOException if an I/O error occurs
     */
    private void readSkyTracHeadersAndUnits(BufferedReader reader, String line)
            throws FatalFlightFileException, IOException {
        String headerLine = advancePastSkyTracPreamble(reader, line);
        if (!isSkyTracDataHeader(headerLine)) {
            throw new FatalFlightFileException(
                    "Expected SkyTrac header row beginning with " + SKYTRAC_DATE_COLUMN + ", Time, Latitude,...");
        }
        headers = trimTrailingEmptyColumns(splitCommaSeparated(headerLine));
        String unitsLine = reader.readLine();
        if (unitsLine == null) {
            throw new FatalFlightFileException("SkyTrac FDM log missing units row.");
        }
        dataTypes = trimTrailingEmptyColumns(splitCommaSeparated(unitsLine));
    }

    /**
     * Advances past SkyTrac version/serial preamble to the {@code Date, Time,...} header row.
     * @param reader the reader to inspect
     * @param line the line to inspect
     * @return the first SkyTrac header line, or null if none is found
     * @throws IOException if an I/O error occurs
     */
    static String advancePastSkyTracPreamble(BufferedReader reader, String line) throws IOException {
        String current = line;
        while (current != null) {
            if (isSkyTracDataHeader(current)) {
                return current;
            }
            current = reader.readLine();
        }
        return null;
    }

    /**
     * Drops trailing empty header/unit cells produced by a trailing comma on the SkyTrac header line.
     * @param columns the columns to trim
     * @return the columns without trailing empty entries
     */
    static List<String> trimTrailingEmptyColumns(List<String> columns) {
        List<String> trimmed = new ArrayList<>(columns);
        while (!trimmed.isEmpty() && trimmed.get(trimmed.size() - 1).isBlank()) {
            trimmed.remove(trimmed.size() - 1);
        }
        return trimmed;
    }

    /**
     * Skips Appareo preamble, then reads column header and units rows.
     * @param reader the reader to inspect
     * @param line the line to inspect
     * @throws FatalFlightFileException if the file format is invalid or the airframe cannot be resolved
     * @throws IOException if an I/O error occurs
     */
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

    /**
     * Advances past Appareo {@code #} comments and blank lines to the column header row.
     * @param reader the reader to inspect
     * @param line the line to inspect
     * @return the first Appareo data header line, or null if none is found
     * @throws IOException if an I/O error occurs
     */
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

    /**
     * Returns one empty unit string per column when the export has no units row.
     * @param columnCount the number of columns
     * @return the empty units list
     */
    private static List<String> emptyUnits(int columnCount) {
        return new ArrayList<>(Collections.nCopies(columnCount, ""));
    }

    /**
     * Appends {@code [n]} to repeated header names so column keys stay unique.
     * @param headers the header names to deduplicate
     */
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

    /**
     * Splits a simple comma-separated line and strips each field (no quoted-field handling).
     * @param line the line to inspect
     * @return the stripped fields from the comma-separated line
     */
    private static List<String> splitCommaSeparated(String line) {
        return Arrays.stream(line.split(",", -1)).map(String::strip).collect(Collectors.toList());
    }

    /**
     * Parses one CSV record with OpenCSV (handles quoted fields for Metro headers).
     * @param line the line to inspect
     * @return the parsed CSV fields
     * @throws IOException if an I/O error occurs
     * @throws CsvException if the CSV row cannot be parsed
     */
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
