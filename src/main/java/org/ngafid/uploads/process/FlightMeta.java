package org.ngafid.uploads.process;

import org.ngafid.flights.Airframes;

import java.time.OffsetDateTime;

/**
 * Utility class used by FlightBuilder to call the Flight constructor, contains all metadata required for a flight.
 */
public final class FlightMeta {
    public int fleetId = -1;
    public int uploaderId = -1;
    public int uploadId = -1;
    public int processingStatus = 0;

    public OffsetDateTime startDateTime;
    public OffsetDateTime endDateTime;
    public String md5Hash;
    public String systemId;
    public String filename;
    public String calculated;
    public String suggestedTailNumber;

    public Airframes.Airframe airframe = null;

    public FlightMeta() {
    }

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

    public OffsetDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(OffsetDateTime odt) {
        this.startDateTime = odt;
    }

    public OffsetDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(OffsetDateTime odt) {
        this.endDateTime = odt;
    }

    public String getMd5Hash() {
        return md5Hash;
    }

    public void setMd5Hash(String md5Hash) {
        this.md5Hash = md5Hash;
    }

    public Airframes.Type getAirframeType() {
        return airframe.getType();
    }

    public Airframes.Airframe getAirframe() {
        return airframe;
    }

    public void setAirframe(String airframe, String airframeType) {
        this.airframe = new Airframes.Airframe(airframe, new Airframes.Type(airframeType));
    }

    public void setAirframe(Airframes.Airframe airframe, Airframes.Type airframeType) {
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
