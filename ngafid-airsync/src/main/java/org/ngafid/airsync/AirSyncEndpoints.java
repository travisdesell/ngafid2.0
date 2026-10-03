package org.ngafid.airsync;

/**
 * Collects the AirSync partner API base URL and the URL templates for its endpoints.
 *
 * <p>Defines constants for authentication, log retrieval and confirmation, and aircraft info, along with the
 * default page size used when paging through logs. The {@code AIRSYNC_ROOT} constant is switched between the
 * production and sandbox/dev hosts here.
 */
public interface AirSyncEndpoints {
    // This will be the default page size when getting imports;
    // Set this higher to improve performance, lower to reduce memory usage and reliability
    // (although memory usage should be fairly small here anyways).
    int PAGE_SIZE = 25;

    // NOTE: DEV endpoints
    // comment this block out and uncomment the below for prod endpoints

    // Use this to swap to sandbox / dev api
    String AIRSYNC_ROOT = "https://api.air-sync.com/partner_api/v1";
    // String AIRSYNC_ROOT = "https://service-dev.air-sync.com/partner_api/v1";

    // Authentication
    String AUTH = AIRSYNC_ROOT + "/auth/";
    // Logs with format arguments (aircraft_id, page_num, num_results)

    String SINGLE_LOG = AIRSYNC_ROOT + "/logs/%d";
    String CONFIRM_LOG = AIRSYNC_ROOT + "/logs/%d/confirm?partner_key=%s";
    String ALL_LOGS = AIRSYNC_ROOT + "/aircraft/%d/logs?page=%d&number_of_results=%d";
    String ALL_LOGS_BY_TIME = AIRSYNC_ROOT + "/aircraft/%d/logs?page=%d&number_of_results=%d&timestamp_uploaded=%s,%s";

    // Aircraft info
    String AIRCRAFT = AIRSYNC_ROOT + "/aircraft/";
}
