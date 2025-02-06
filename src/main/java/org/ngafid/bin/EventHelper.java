package org.ngafid.bin;

public class EventHelper {
    // TODO:
    // - Flight Status to Enum, maybe get rid of `has_coords`
    // - Ensure Flight Status is PROCESSING during building AND use flight locks
    // - Refactor compute events daemon to work with Kafka. Observer should search for completed flights that are missing event calculations.
    // - Ensure custom events are considered here as well, so we don't have to write a script every time a new one is added.
    // - Move proximity to Kafka
}
