package org.ngafid.core.flights.maintenance;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.logging.Logger;

public class MaintenanceRecord implements Comparable<MaintenanceRecord> {
    private static final Logger LOG = Logger.getLogger(MaintenanceRecord.class.getName());

    private final int workorderNumber;
    private final LocalDateTime openDateTime;
    private final LocalDateTime closeDateTime;
    private final LocalDate openDate;
    private final LocalDate closeDate;
    private final String tailNumber;
    private final String airframe;
    private final int problemATACode;
    private final String label;
    private final String labelId;
    private final String cleanedProblem;
    private final String originalProblem;
    private final LocalDate actionDate;
    private final int actionATACode;
    private final String originalAction;
    /** Raw date strings from CSV (for debugging log). */
    private final String rawOpenDate;
    private final String rawCloseDate;

    public int getWorkorderNumber() {
        return workorderNumber;
    }

    public String getLabelId() {
        return labelId;
    }

    public String getTailNumber() {
        return tailNumber;
    }

    public String getAirframe() {
        return airframe;
    }

    public String getLabel() {
        return label;
    }

    public String getOriginalAction() {
        return originalAction;
    }

    public LocalDate getOpenDate() {
        return openDate;
    }

    public LocalDate getCloseDate() {
        return closeDate;
    }

    /** Open date-time (for before/during/after phase comparison). */
    public LocalDateTime getOpenDateTime() {
        return openDateTime;
    }

    /** Close date-time (for before/during/after phase comparison). */
    public LocalDateTime getCloseDateTime() {
        return closeDateTime;
    }

    public LocalDate getActionDate() {
        return actionDate;
    }

    /** Raw open date string as read from CSV (for debugging). */
    public String getRawOpenDate() {
        return rawOpenDate;
    }

    /** Raw close date string as read from CSV (for debugging). */
    public String getRawCloseDate() {
        return rawCloseDate;
    }

    /** yyyy-MM-dd HH:mm or yyyy-MM-dd HH:mm:ss — used in new maintenance CSV format. */
    private static final DateTimeFormatter FORMAT_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FORMAT_DATETIME_SEC = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** yyyy-MM-dd — for problem_date. */
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /** MM-dd-yyyy (e.g. 02-02-2017) — legacy format. */
    private static final DateTimeFormatter FORMAT_MM_DD_YYYY = DateTimeFormatter.ofPattern("MM-dd-yyyy");
    /** M/d/yy (e.g. 7/13/12) — legacy format. */
    private static final DateTimeFormatter FORMAT_M_D_YY = DateTimeFormatter.ofPattern("M/d/yy");

    private final ArrayList<MaintenanceRecord> combinedRecords = new ArrayList<MaintenanceRecord>();

    /**
     * Parses a datetime string: yyyy-MM-dd HH:mm, yyyy-MM-dd HH:mm:ss, or yyyy-MM-dd.
     */
    private static LocalDateTime parseDateTime(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Empty date/time string");
        }
        try {
            return LocalDateTime.parse(s, FORMAT_DATETIME_SEC);
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(s, FORMAT_DATETIME);
            } catch (DateTimeParseException e2) {
                try {
                    return LocalDate.parse(s, FORMAT_DATE).atStartOfDay();
                } catch (DateTimeParseException e3) {
                    throw new IllegalArgumentException("Cannot parse datetime '" + s + "'; expected yyyy-MM-dd HH:mm or yyyy-MM-dd", e3);
                }
            }
        }
    }

    /**
     * Parses a date string: yyyy-MM-dd, MM-dd-yyyy, or M/d/yy.
     */
    private static LocalDate parseDate(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Empty date string");
        }
        try {
            return LocalDate.parse(s, FORMAT_DATE);
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(s, FORMAT_MM_DD_YYYY);
            } catch (DateTimeParseException e2) {
                try {
                    return LocalDate.parse(s, FORMAT_M_D_YY);
                } catch (DateTimeParseException e3) {
                    throw new IllegalArgumentException("Cannot parse date '" + s + "'; expected yyyy-MM-dd, MM-dd-yyyy or M/d/yy", e3);
                }
            }
        }
    }

    /**
     * Parses a line from the new maintenance CSV format (11 columns):
     * workorder,date_time_opened,date_time_closed,registration,total_time,ata_code,problem,problem_date,action,cluster_id,cluster_name
     */
    public MaintenanceRecord(String line) {
        String[] parts = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
        if (parts.length != 11) {
            throw new IllegalArgumentException("Maintenance CSV line must have exactly 11 columns "
                    + "(workorder,date_time_opened,date_time_closed,registration,total_time,ata_code,problem,problem_date,action,cluster_id,cluster_name); got "
                    + parts.length + ". If a field contains commas, quote it (e.g. \"text, with comma\").");
        }

        workorderNumber = Integer.parseInt(parts[0].trim());
        rawOpenDate = parts[1].trim();
        rawCloseDate = parts[2].trim();
        openDateTime = parseDateTime(parts[1]);
        closeDateTime = parseDateTime(parts[2]);
        openDate = openDateTime.toLocalDate();
        closeDate = closeDateTime.toLocalDate();
        tailNumber = parts[3].trim();
        airframe = "";  // New format has no airframe column
        problemATACode = parseIntOrZero(parts[5]);
        label = parts[10].trim();
        labelId = parts[9].trim();
        cleanedProblem = unquote(parts[6]);
        originalProblem = cleanedProblem;
        actionDate = parseDate(parts[7]);
        actionATACode = problemATACode;
        originalAction = unquote(parts[8]);
    }

    private static String unquote(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1).replace("\"\"", "\"");
        }
        return s;
    }

    private static int parseIntOrZero(String s) {
        if (s == null || s.trim().isEmpty()) return 0;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void combine(MaintenanceRecord other) {
        ArrayList<String> mismatches = new ArrayList<String>();
        if (workorderNumber != other.workorderNumber) mismatches.add("workorderNumber");
        if (!openDate.equals(other.openDate)) mismatches.add("openDate");
        if (!closeDate.equals(other.closeDate)) mismatches.add("closeDate");
        if (!tailNumber.equals(other.tailNumber)) mismatches.add("tailNumber");
        if (!airframe.equals(other.airframe)) mismatches.add("airframe");
        if (problemATACode != other.problemATACode) mismatches.add("problemATACode");
        if (!label.equals(other.label)) mismatches.add("label");
        if (!cleanedProblem.equals(other.cleanedProblem)) mismatches.add("cleanedProblem");
        if (!originalProblem.equals(other.originalProblem)) mismatches.add("originalProblem");
        if (!actionDate.equals(other.actionDate)) mismatches.add("actionDate");
        if (actionATACode != other.actionATACode) mismatches.add("actionATACode");
        if (!originalAction.equals(other.originalAction)) mismatches.add("originalAction");
        if (!labelId.equals(other.labelId)) mismatches.add("labelId");

        if (mismatches.contains("workorderNumber")) {
            System.err.println("Cannot combine two records with different workorder numbers: "
                    + workorderNumber + " vs " + other.workorderNumber);
            System.exit(1);
        }

        combinedRecords.add(other);
    }

    public int compareTo(MaintenanceRecord other) {
        return openDate.compareTo(other.openDate);
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    public String toJSON() {
        return "{\n"
                + "\t\"workorderNumber\" : \"" + workorderNumber + "\",\n"
                + "\t\"openDate\" : \"" + openDate + "\",\n"
                + "\t\"closeDate\" : \"" + closeDate + "\",\n"
                + "\t\"openDateTime\" : \"" + openDateTime + "\",\n"
                + "\t\"closeDateTime\" : \"" + closeDateTime + "\",\n"
                + "\t\"tailNumber\" : \"" + escapeJson(tailNumber) + "\",\n"
                + "\t\"airframe\" : \"" + escapeJson(airframe) + "\",\n"
                + "\t\"problemATACode\" : \"" + problemATACode + "\",\n"
                + "\t\"label\" : \"" + escapeJson(label) + "\",\n"
                + "\t\"labelId\" : \"" + escapeJson(labelId) + "\",\n"
                + "\t\"cleanedProblem\" : \"" + escapeJson(cleanedProblem) + "\",\n"
                + "\t\"originalProblem\" : \"" + escapeJson(originalProblem) + "\",\n"
                + "\t\"actionDate\" : \"" + actionDate + "\",\n"
                + "\t\"actionATACode\" : \"" + actionATACode + "\",\n"
                + "\t\"originalAction\" : \"" + escapeJson(originalAction) + "\"\n"
                + "}";
    }

    public String toString() {
        return "[Maintenance Record - WO#: '" + workorderNumber
                + "', openDateTime: '" + openDateTime
                + "', closeDateTime: '" + closeDateTime
                + "', tailNumber: '" + tailNumber
                + "', airframe: '" + airframe
                + "', problemATACode: '" + problemATACode
                + "', label: '" + label
                + "', labelId: '" + labelId
                + "', cleanedProblem: '" + cleanedProblem
                + "', actionDate: '" + actionDate
                + "', originalAction: '" + originalAction
                + "']";
    }
}
