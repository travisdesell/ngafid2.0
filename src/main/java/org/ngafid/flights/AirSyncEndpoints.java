package org.ngafid.flights;

public interface AirSyncEndpoints {
    // This will be the default page size when getting imports;
    // Set this higher to improve performance, lower to reduce memory usage and reliability
    // (although memory usage should be fairly small here anyways).
    public static final int PAGE_SIZE = 25; 


    //NOTE: DEV endpoints 
    //comment this block out and uncomment the below for prod endpoints

    //Authentication
    //public static final String AUTH = "https://service-dev.air-sync.com/partner_api/v1/auth/";

    // Logs with format arguments (aircraft_id, page_num, num_results)
    //public static final String SINGLE_LOG = "https://service-dev.air-sync.com/partner_api/v1/logs/%d";
    //public static final String ALL_LOGS = "https://service-dev.air-sync.com/partner_api/v1/aircraft/%d/logs?page=%d&number_of_results=%d";
    //public static final String ALL_LOGS_BY_TIME = "https://service-dev.air-sync.com/partner_api/v1/aircraft/%d/logs?page=%d&number_of_results=%d&timestamp_uploaded=%s,%s";
    //
    //Aircraft info
    //public static final String AIRCRAFT = "https://service-dev.air-sync.com/partner_api/v1/aircraft/";


    //NOTE: PROD endpoints (default)
    //comment this block out and uncomment the above for dev endpoints

    //Authentication
    public static final String AUTH = "https://api.air-sync.com/partner_api/v1/auth/";
    // Logs with format arguments (aircraft_id, page_num, num_results)
    public static final String SINGLE_LOG = "https://api.air-sync.com/partner_api/v1/logs/%d";
    public static final String ALL_LOGS = "https://api.air-sync.com/partner_api/v1/aircraft/%d/logs?page=%d&number_of_results=%d";
    public static final String ALL_LOGS_BY_TIME = "https://api.air-sync.com/partner_api/v1/aircraft/%d/logs?page=%d&number_of_results=%d&timestamp_uploaded=%s,%s";

    ////Aircraft info
    public static final String AIRCRAFT = "https://api.air-sync.com/partner_api/v1/aircraft/";
}
