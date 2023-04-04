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
}
