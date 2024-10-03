package org.ngafid.flights.process;

import org.ngafid.flights.Airframes;

/**
 * Utility class used by FlightBuilder to call the Flight constructor.
 **/
public final class FlightMeta {

    public int fleetId = -1,
            uploaderId = -1,
            uploadId = -1,
            processingStatus = 0;

    public String startDateTime,
            endDateTime,
            md5Hash,
            systemId,
            filename,
            calculated,
            suggestedTailNumber;

    public Airframes.Airframe airframe = null;
    public Airframes.AirframeType airframeType = null;

    // Default constructor
    public FlightMeta() {
    }

    // Copy constructor
    public FlightMeta(FlightMeta other) {
        this.fleetId = other.fleetId;
        this.uploaderId = other.uploaderId;
        this.uploadId = other.uploadId;
        this.processingStatus = other.processingStatus;
        this.startDateTime = other.startDateTime != null ? new String(other.startDateTime) : null;
        this.endDateTime = other.endDateTime != null ? new String(other.endDateTime) : null;
        this.md5Hash = other.md5Hash != null ? new String(other.md5Hash) : null;
        this.systemId = other.systemId != null ? new String(other.systemId) : null;
        this.filename = other.filename != null ? new String(other.filename) : null;
        this.calculated = other.calculated != null ? new String(other.calculated) : null;
        this.suggestedTailNumber = other.suggestedTailNumber != null ? new String(other.suggestedTailNumber) : null;

        // Deep copy of Airframe if not null
        this.airframe = other.airframe != null ? new Airframes.Airframe(other.airframe.getName()) : null;

        // Deep copy of AirframeType if not null
        this.airframeType = other.airframeType != null ? new Airframes.AirframeType(other.airframeType.getName()) : null;
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

    public Airframes.AirframeType getAirframeType() {
        return airframeType;
    }

    public void setAirframeType(String airframeType) {
        this.airframeType = new Airframes.AirframeType(airframeType);
    }

    public void setAirframeType(Airframes.AirframeType airframeType) {
        this.airframeType = airframeType;
    }

    public Airframes.Airframe getAirframe() {
        return airframe;
    }

    public void setAirframe(String airframe) {
        this.airframe = new Airframes.Airframe(airframe);
    }

    public void setAirframe(Airframes.Airframe airframe) {
        this.airframe = airframe;
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
