package org.ngafid.flights;

public interface AirSyncEndpoints {
    // This will be the default page size when getting imports;
    // Set this higher to improve performance, lower to reduce memory usage and reliability
    // (although memory usage should be fairly small here anyways).
    public static final int PAGE_SIZE = 25; 


    //NOTE: DEV endpoints 
    //comment this block out and uncomment the below for prod endpoints

    // Use this to swap to sandbox / dev api
    public static final String AIRSYNC_ROOT = "https://api.air-sync.com/partner_api/v1";
    // public static final String AIRSYNC_ROOT = "https://service-dev.air-sync.com/partner_api/v1";

    //Authentication
    public static final String AUTH = 
        AIRSYNC_ROOT + "/auth/";
    // Logs with format arguments (aircraft_id, page_num, num_results)

    public static final String SINGLE_LOG =
        AIRSYNC_ROOT + "/logs/%d";
    public static final String ALL_LOGS = 
        AIRSYNC_ROOT + "/aircraft/%d/logs?page=%d&number_of_results=%d";
    public static final String ALL_LOGS_BY_TIME =
        AIRSYNC_ROOT + "/aircraft/%d/logs?page=%d&number_of_results=%d&timestamp_uploaded=%s,%s";

    //Aircraft info
    public static final String AIRCRAFT = 
        AIRSYNC_ROOT + "/aircraft/";
}
