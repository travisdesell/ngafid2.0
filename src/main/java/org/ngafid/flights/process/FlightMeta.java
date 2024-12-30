package org.ngafid.flights.process;

import org.ngafid.flights.Airframes;

/**
 * Utility class used by FlightBuilder to call the Flight constructor.
 **/
public final class FlightMeta {

    private int fleetId = -1;
    private int uploaderId = -1;
    private int uploadId = -1;
    public int processingStatus = 0;

    private String startDateTime;
    private String endDateTime;
    private String md5Hash;
    private String systemId;
    private String filename;
    private String calculated;
    private String suggestedTailNumber;

    private Airframes.Airframe airframe = null;
    private Airframes.AirframeType airframeType = null;

    public int getFleetId() {
        return fleetId;
    }

    public void setFleetId(int newFleetId) {
        this.fleetId = newFleetId;
    }

    public int getUploaderId() {
        return uploaderId;
    }

    public void setUploaderId(int newUploaderId) {
        this.uploaderId = newUploaderId;
    }

    public int getUploadId() {
        return uploadId;
    }

    public void setUploadId(int newUploadId) {
        this.uploadId = newUploadId;
    }

    public int getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(int newProcessingStatus) {
        this.processingStatus = newProcessingStatus;
    }

    public String getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(String newStartDateTime) {
        this.startDateTime = newStartDateTime;
    }

    public String getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(String newEndDateTime) {
        this.endDateTime = newEndDateTime;
    }

    public String getMd5Hash() {
        return md5Hash;
    }

    public void setMd5Hash(String newMd5Hash) {
        this.md5Hash = newMd5Hash;
    }

    public Airframes.AirframeType getAirframeType() {
        return airframeType;
    }

    public void setAirframeType(String newAirframeType) {
        this.airframeType = new Airframes.AirframeType(newAirframeType);
    }

    public void setAirframeType(Airframes.AirframeType newAirframeType) {
        this.airframeType = newAirframeType;
    }

    public Airframes.Airframe getAirframe() {
        return airframe;
    }

    public void setAirframe(String newAirframe) {
        this.airframe = new Airframes.Airframe(newAirframe);
    }

    public void setAirframe(Airframes.Airframe newAirframe) {
        this.airframe = newAirframe;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String newSystemId) {
        this.systemId = newSystemId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String newFilename) {
        this.filename = newFilename;
    }

    public String getCalculated() {
        return calculated;
    }

    public void setCalculated(String newCalculated) {
        this.calculated = newCalculated;
    }

    public String getSuggestedTailNumber() {
        return suggestedTailNumber;
    }

    public void setSuggestedTailNumber(String newSuggestedTailNumber) {
        this.suggestedTailNumber = newSuggestedTailNumber;
    }
}
