package org.ngafid.core.flights.maintenance;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.logging.Logger;

public class MaintenanceRecord implements Comparable<MaintenanceRecord> {
    private static final Logger LOG = Logger.getLogger(MaintenanceRecord.class.getName());

    private final int workorderNumber;
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

    public LocalDate getActionDate() {
        return actionDate;
    }

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yy");

    private final ArrayList<MaintenanceRecord> combinedRecords = new ArrayList<MaintenanceRecord>();


    public MaintenanceRecord(String line) {
        String[] parts = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
        if (parts.length != 13) {
            throw new IllegalArgumentException("Maintenance CSV line must have exactly 13 columns "
                    + "(workorder,open,close,tail,airframe,ata,label,label_id,cleaned_problem,original_problem,date,ata,original_action); got "
                    + parts.length + ". If a field contains commas, quote it (e.g. \"text, with comma\").");
        }

        workorderNumber = Integer.parseInt(parts[0].trim());
        openDate = LocalDate.parse(parts[1].trim(), formatter);
        closeDate = LocalDate.parse(parts[2].trim(), formatter);
        tailNumber = parts[3].trim();
        airframe = parts[4].trim();
        problemATACode = Integer.parseInt(parts[5].trim());
        label = parts[6].trim();
        labelId = parts[7].trim();
        cleanedProblem = parts[8].trim();
        originalProblem = parts[9].trim();
        actionDate = LocalDate.parse(parts[10].trim(), formatter);
        actionATACode = Integer.parseInt(parts[11].trim());
        originalAction = parts[12].trim();
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

    public String toJSON() {
        return "{\n"
                + "\t\"workorderNumber\" : \"" + workorderNumber + "\",\n"
                + "\t\"openDate\" : \"" + openDate + "\",\n"
                + "\t\"closeDate\" : \"" + closeDate + "\",\n"
                + "\t\"tailNumber\" : \"" + tailNumber + "\",\n"
                + "\t\"airframe\" : \"" + airframe + "\",\n"
                + "\t\"problemATACode\" : \"" + problemATACode + "\",\n"
                + "\t\"label\" : \"" + label + "\",\n"
                + "\t\"labelId\" : \"" + labelId + "\",\n"
                + "\t\"cleanedProblem\" : \"" + cleanedProblem + "\",\n"
                + "\t\"originalProblem\" : \"" + originalProblem + "\",\n"
                + "\t\"actionDate\" : \"" + actionDate + "\",\n"
                + "\t\"actionATACode\" : \"" + actionATACode + "\",\n"
                + "\t\"originalAction\" : \"" + originalAction + "\"\n"
                + "}";
    }

    public String toString() {
        return "[Maintenance Record - WO#: '" + workorderNumber
                + "', openDate: '" + openDate
                + "', closeDate: '" + closeDate
                + "', tailNumber: '" + tailNumber
                + "', airframe: '" + airframe
                + "', problemATACode: '" + problemATACode
                + "', label: '" + label
                + "', labelId: '" + labelId
                + "', cleanedProblem: '" + cleanedProblem
                + "', originalProblem: '" + originalProblem
                + "', actionDate: '" + actionDate
                + "', actionATACode: '" + actionATACode
                + "', originalAction: '" + originalAction
                + "']";
    }
}

