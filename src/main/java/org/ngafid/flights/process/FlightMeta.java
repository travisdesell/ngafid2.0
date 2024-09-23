package org.ngafid.flights.process;

/**
 * Utility class used by FlightBuilder to call the Flight constructor.
 **/
public final class FlightMeta {

    public int  fleetId = -1,
                uploaderId = -1,
                uploadId = -1,
                processingStatus = 0;

    public String startDateTime, 
                  endDateTime,
                  md5Hash,
                  airframeType,
                  systemId,
                  filename,
                  airframeName,
                  calculated,
                  suggestedTailNumber;


    // Copy constructor
    public FlightMeta(FlightMeta meta) {
        this.fleetId = meta.fleetId;
        this.uploaderId = meta.uploaderId;
        this.uploadId = meta.uploadId;
        this.processingStatus = meta.processingStatus;
        this.startDateTime = meta.startDateTime;
        this.endDateTime = meta.endDateTime;
        this.md5Hash = meta.md5Hash;
        this.airframeType = meta.airframeType;
        this.systemId = meta.systemId;
        this.filename = meta.filename;
        this.airframeName = meta.airframeName;
        this.calculated = meta.calculated;
        this.suggestedTailNumber = meta.suggestedTailNumber;
    }

    // Default constructor
    public FlightMeta() {
    }

    public int getFleetId() {
        return fleetId;
    }

    public void setFleetId(int fleetId) {
        this.fleetId = fleetId;
    }

    public int getUploaderId() {
        return uploaderId;
    }

    public void setUploaderId(int uploaderId) {
        this.uploaderId = uploaderId;
    }

    public int getUploadId() {
        return uploadId;
    }

    public void setUploadId(int uploadId) {
        this.uploadId = uploadId;
    }

    public int getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(int processingStatus) {
        this.processingStatus = processingStatus;
    }

    public String getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(String startDateTime) {
        this.startDateTime = startDateTime;
    }

    public String getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(String endDateTime) {
        this.endDateTime = endDateTime;
    }

    public String getMd5Hash() {
        return md5Hash;
    }

    public void setMd5Hash(String md5Hash) {
        this.md5Hash = md5Hash;
    }

    public String getAirframeType() {
        return airframeType;
    }

    public void setAirframeType(String airframeType) {
        this.airframeType = airframeType;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getAirframeName() {
        return airframeName;
    }

    public void setAirframeName(String airframeName) {
        this.airframeName = airframeName;
    }

    public String getCalculated() {
        return calculated;
    }

    public void setCalculated(String calculated) {
        this.calculated = calculated;
    }

    public String getSuggestedTailNumber() {
        return suggestedTailNumber;
    }

    public void setSuggestedTailNumber(String suggestedTailNumber) {
        this.suggestedTailNumber = suggestedTailNumber;
    }
}
