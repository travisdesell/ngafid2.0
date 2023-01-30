package org.ngafid.flights;

public interface AirSyncEndpoints {
    //Aircraft ingo
    public static String AIRCRAFT = "https://service-dev.air-sync.com/partner_api/v1/aircraft/";

    //Authentication
    public static String AUTH_DEV = "https://service-dev.air-sync.com/partner_api/v1/auth/";
    public static String AUTH_PROD = "https://api.air-sync.com/partner_api/v1/auth/";

    // Logs with format arguments (aircraft_id, page_num, num_results)
    public static String ALL_LOGS = "https://service-dev.air-sync.com/partner_api/v1/aircraft/%d/logs?page=%d&number_of_results=%d";
    public static String ALL_LOGS_BY_TIME = "https://service-dev.air-sync.com/partner_api/v1/aircraft/%d/logs?page=%d&number_of_results=%d&timestamp_uploaded=%s,%s";
}
