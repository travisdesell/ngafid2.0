package org.ngafid.accounts;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Base64;

import javax.net.ssl.HttpsURLConnection;

import org.ngafid.WebServer;
import org.ngafid.flights.AirSyncEndpoints;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class AirSyncAuth {
    class AccessToken {
        public String accessToken;
    }

    private Fleet fleet;
    private String apiKey, apiSecret;
    private byte [] hash;
    private AccessToken accessToken;


    private static final long BEARER_CERT_EXP_TIME = 3600;

    private LocalDateTime issueTime;
    private static final Gson gson = WebServer.gson;

    public AirSyncAuth(String apiKey, String apiSecret) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;

        byte [] srcWord = (apiKey + ":" + apiSecret).getBytes();
        this.hash = Base64.getEncoder().encode(srcWord);

        this.requestAuthorization();
    }

    public String bearerString() {
        return "Bearer " + this.accessToken.accessToken;
    }

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

    public LocalDateTime getIssueTime() {
        return issueTime;
    }

    public boolean isOutdated() {
        Duration duration = Duration.between(this.issueTime, LocalDateTime.now());

        return (duration.getSeconds() > BEARER_CERT_EXP_TIME);
    }
}
