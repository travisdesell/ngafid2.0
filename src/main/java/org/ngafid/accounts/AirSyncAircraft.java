package org.ngafid.accounts;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AirSyncAircraft {
    private int id;
    private String tailNumber;

    public class AirSyncAircraftLog {
        int id, aircraftId;
        String origin, destination;
        LocalDateTime timeStart, timeEnd;
        String fileUrl;
    }

    public AirSyncAircraft(int id, String tailNumber) {
        this.id = id;
        this.tailNumber = tailNumber;
    }

    public List<AirSyncAircraftLog> getLogs() {
        List<AirSyncAircraftLog> logs = new ArrayList<>();
        return logs;
    }
}
