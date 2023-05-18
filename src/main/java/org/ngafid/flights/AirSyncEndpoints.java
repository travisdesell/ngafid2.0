package org.ngafid.flights;

public interface AirSyncEndpoints {
    // This will be the default page size when getting imports;
    // Set this higher to improve performance, lower to reduce memory usage and reliability
    // (although memory usage should be fairly small here anyways).
    public static final int PAGE_SIZE = 25; 

    //Aircraft info
    public static final String AIRCRAFT = "https://api.air-sync.com/partner_api/v1/aircraft/";

    //Authentication
    public static final String AUTH_DEV = "https://service-dev.air-sync.com/partner_api/v1/auth/";
    public static final String AUTH_PROD = "https://api.air-sync.com/partner_api/v1/auth/";

    // Logs with format arguments (aircraft_id, page_num, num_results)
    public static final String SINGLE_LOG = "https://api.air-sync.com/partner_api/v1/logs/%d";
    public static final String ALL_LOGS = "https://api.air-sync.com/partner_api/v1/aircraft/%d/logs?page=%d&number_of_results=%d";
    public static final String ALL_LOGS_BY_TIME = "https://api.air-sync.com/partner_api/v1/aircraft/%d/logs?page=%d&number_of_results=%d&timestamp_uploaded=%s,%s";
}
