package org.ngafid.flights;

public interface AirSyncEndpoints {
    // This will be the default page size when getting imports;
    // Set this higher to improve performance, lower to reduce memory usage and reliability
    // (although memory usage should be fairly small here anyways).
    int PAGE_SIZE = 25;


    //NOTE: DEV endpoints
    //comment this block out and uncomment the below for prod endpoints

    // Use this to swap to sandbox / dev api
    String AIRSYNC_ROOT = "https://api.air-sync.com/partner_api/v1";
    // String AIRSYNC_ROOT = "https://service-dev.air-sync.com/partner_api/v1";

    //Authentication
    String AUTH = AIRSYNC_ROOT + "/auth/";
    // Logs with format arguments (aircraft_id, page_num, num_results)

    String SINGLE_LOG = AIRSYNC_ROOT + "/logs/%d";
    String ALL_LOGS = AIRSYNC_ROOT + "/aircraft/%d/logs?page=%d&number_of_results=%d";
    String ALL_LOGS_BY_TIME = AIRSYNC_ROOT + "/aircraft/%d/logs?page=%d&number_of_results=%d&timestamp_uploaded=%s,%s";

    //Aircraft info
    String AIRCRAFT = AIRSYNC_ROOT + "/aircraft/";
}
