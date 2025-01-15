package org.ngafid.flights.process;

import org.ngafid.flights.Airframes;

/**
 * Utility class used by FlightBuilder to call the Flight constructor.
 **/
public final class FlightMeta {
    //CHECKSTYLE:OFF
    public int fleetId = -1;
    public int uploaderId = -1;
    public int uploadId = -1;
    public int processingStatus = 0;

    public String startDateTime;
    public String endDateTime;
    public String md5Hash;
    public String systemId;
    public String filename;
    public String calculated;
    public String suggestedTailNumber;

    public Airframes.Airframe airframe = null;
    public Airframes.AirframeType airframeType = null;
    //CHECKSTYLE:ON

    // Default constructor
    public FlightMeta() {
    }

    // Copy constructor
    public FlightMeta(FlightMeta other) {
        this.fleetId = other.fleetId;
        this.uploaderId = other.uploaderId;
        this.uploadId = other.uploadId;
        this.processingStatus = other.processingStatus;
        this.startDateTime = other.startDateTime;
        this.endDateTime = other.endDateTime;
        this.md5Hash = other.md5Hash;
        this.systemId = other.systemId;
        this.filename = other.filename;
        this.calculated = other.calculated;
        this.suggestedTailNumber = other.suggestedTailNumber;

        // Deep copy of Airframe, AirframeType  if not null
        this.airframe = other.airframe;
        this.airframeType = other.airframeType;
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
