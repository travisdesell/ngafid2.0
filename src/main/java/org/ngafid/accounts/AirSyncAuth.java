package org.ngafid.accounts;

import com.google.gson.Gson;
import org.ngafid.WebServer;
import org.ngafid.flights.AirSyncEndpoints;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * This class represents the authentication information for an AirSync-enabled fleet
 * The pertinent information should be stored in the database
 */
public class AirSyncAuth {
    private static final long BEARER_CERT_EXP_TIME = 3600;
    //CHECKSTYLE:ON
    private static final Gson GSON = WebServer.gson;
    private final byte[] hash;
    private AccessToken accessToken;
    private LocalDateTime issueTime;
    /**
     * Default constructor
     *
     * @param apiKey    the api key string (probably from the database)
     * @param apiSecret the api secret string (probably from the database)
     */
    public AirSyncAuth(String apiKey, String apiSecret) {
        byte[] srcWord = (apiKey + ":" + apiSecret).getBytes();
        this.hash = Base64.getEncoder().encode(srcWord);
        System.out.println("API Key = " + apiKey);
        System.out.println("API Secret = " + apiSecret);
        this.requestAuthorization();
    }

    /**
     * Formats a bearer string for requesting authorization
     *
     * @return a {@link String} with the bearer information
     */
    public String bearerString() {
        return "Bearer " + this.accessToken.accessToken;
    }

    /**
     * Requests authorization from the AirSync servers using the fleets information stored in the database.
     */
    public void requestAuthorization() {
        try {
            HttpsURLConnection connection = (HttpsURLConnection) new URL(AirSyncEndpoints.AUTH).openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", "Basic " + new String(this.hash));

            try (InputStream is = connection.getInputStream()) {
                byte[] respRaw = is.readAllBytes();

                is.close();

                String resp = new String(respRaw).replaceAll("access_token", "accessToken");

                this.accessToken = GSON.fromJson(resp, AccessToken.class);
                this.issueTime = LocalDateTime.now();
            }
        } catch (IOException ie) {
            ie.printStackTrace();
            System.err.println("FATAL: Unable to get a token from AirSync! Exiting due to fatal error.");

            System.exit(1);
        }
    }

    /**
     * Accessor method for the time this auth was last issued.
     *
     * @return the last issue time as a LocalDateTime object.
     */
    public LocalDateTime getIssueTime() {
        return issueTime;
    }

    /**
     * Determines if the auth credentials are outdated, speficied by the variable
     * BEARER_CERT_EXP_TIME.
     *
     * @return true if the credentials need to be renewed.
     */
    public boolean isOutdated() {
        Duration duration = Duration.between(this.issueTime, LocalDateTime.now());

        return (duration.getSeconds() > BEARER_CERT_EXP_TIME);
    }

    /**
     * This represents the access token that is used to access AirSync
     *
     * @param accessToken the raw access token, in {@link String} form
     */
    //CHECKSTYLE:OFF
    class AccessToken {
        public String accessToken;
    }
}
