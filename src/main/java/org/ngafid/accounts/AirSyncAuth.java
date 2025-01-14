package org.ngafid.accounts;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import javax.net.ssl.HttpsURLConnection;
import org.ngafid.WebServer;
import org.ngafid.flights.AirSyncEndpoints;

/**
 * This class represents the authentication information for an AirSync-enabled fleet
 * The pertinent information should be stored in the database
 */
public class AirSyncAuth {
    /**
     * This represents the access token that is used to access AirSync
     *
     * @param accessToken the raw access token, in {@link String} form
     */
    class AccessToken {
        public String accessToken;
    }

    private byte [] hash;
    private AccessToken accessToken;

    private static final long BEARER_CERT_EXP_TIME = 3600;

    private LocalDateTime issueTime;
    private static final Gson gson = WebServer.gson;

    /**
     * Default constructor
     *
     * @param apiKey the api key string (probably from the database)
     * @param apiSecret the api secret string (probably from the database)
     */
    public AirSyncAuth(String apiKey, String apiSecret) {
        byte [] srcWord = (apiKey + ":" + apiSecret).getBytes();
        this.hash = Base64.getEncoder().encode(srcWord);

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

            InputStream is = connection.getInputStream();
            byte [] respRaw = is.readAllBytes();

            String resp = new String(respRaw).replaceAll("access_token", "accessToken");

            this.accessToken = gson.fromJson(resp, AccessToken.class);
            this.issueTime = LocalDateTime.now();
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
}
