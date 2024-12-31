package org.ngafid.maintenance;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.util.logging.Logger;

import java.util.ArrayList;

public class MaintenanceRecord implements Comparable<MaintenanceRecord> {
    private static final Logger LOG = Logger.getLogger(MaintenanceRecord.class.getName());

    private int workorderNumber;
    private LocalDate openDate;
    private LocalDate closeDate;
    private String tailNumber;
    private String airframe;
    private int problemATACode;
    private String label;
    private String cleanedProblem;
    private String originalProblem;
    private LocalDate actionDate;
    private int actionATACode;
    private String originalAction;

    public int getWorkorderNumber() {
        return workorderNumber;
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

    public LocalDate getOpenDate() {
        return openDate;
    }

    public LocalDate getCloseDate() {
        return closeDate;
    }

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yy");

    private ArrayList<MaintenanceRecord> combinedRecords = new ArrayList<MaintenanceRecord>();


    public MaintenanceRecord(String line) {
        String[] parts = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");

        workorderNumber = Integer.parseInt(parts[0]);
        openDate = LocalDate.parse(parts[1], formatter);
        closeDate = LocalDate.parse(parts[2], formatter);
        tailNumber = parts[3];
        airframe = parts[4];
        problemATACode = Integer.parseInt(parts[5]);
        label = parts[6];
        cleanedProblem = parts[7];
        originalProblem = parts[8];
        actionDate = LocalDate.parse(parts[9], formatter);
        actionATACode = Integer.parseInt(parts[10]);
        originalAction = parts[11];
    }

    public void combine(MaintenanceRecord other) {
        ArrayList<String> mismatches = new ArrayList<String>();
        if (workorderNumber != other.workorderNumber) mismatches.add("workorderNumber");
        if (!openDate.equals(other.openDate)) mismatches.add("openDate");
        if (!closeDate.equals(closeDate)) mismatches.add("closeDate");
        if (!tailNumber.equals(other.tailNumber)) mismatches.add("tailNumber");
        if (!airframe.equals(other.airframe)) mismatches.add("airframe");
        if (problemATACode != other.problemATACode) mismatches.add("problemATACode");
        if (!label.equals(other.label)) mismatches.add("label");
        if (!cleanedProblem.equals(other.cleanedProblem)) mismatches.add("cleanedProblem");
        if (!originalProblem.equals(other.originalProblem)) mismatches.add("originalProblem");
        if (!actionDate.equals(other.actionDate)) mismatches.add("actionDate");
        if (actionATACode != other.actionATACode) mismatches.add("actionATACode");
        if (!originalAction.equals(other.originalAction)) mismatches.add("originalAction");

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
            + "', cleanedProblem: '" + cleanedProblem
            + "', originalProblem: '" + originalProblem
            + "', actionDate: '" + actionDate
            + "', actionATACode: '" + actionATACode
            + "', originalAction: '" + originalAction
            + "']";
    }
}

