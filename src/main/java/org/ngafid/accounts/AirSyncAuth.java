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

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class AirSyncAuth {
    private Fleet fleet;
    private String apiKey, apiSecret;
    private byte [] hash, bearerAuth;

    private static String AIR_SYNC_AUTH_ENDPOINT_DEV = "https://service-dev.air-sync.com/partner_api/v1/auth/";
    private static String AIR_SYNC_AUTH_ENDPOINT_PROD = "https://api.air-sync.com/partner_api/v1/auth/";

    private static final long BEARER_CERT_EXP_TIME = 3600;

    private LocalDateTime issueTime;
    private static final Gson gson = WebServer.gson;

    public AirSyncAuth(String apiKey, String apiSecret) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;

        byte [] srcWord = (apiKey + ":" + apiSecret).getBytes();
        this.hash = Base64.getEncoder().encode(srcWord);

        try {
            this.requestAuthorization();
        } catch (IOException ie) {
            ie.printStackTrace();
        }
    }

    public void requestAuthorization() throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(AIR_SYNC_AUTH_ENDPOINT_DEV).openConnection();

        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", "Basic " + new String(this.hash));     

        InputStream is = connection.getInputStream();
        byte [] respRaw = is.readAllBytes();

        String str = new String(respRaw);
        System.out.println("Got response: " + str);
    }

    public LocalDateTime getIssueTime() {
        return issueTime;
    }

    public boolean isOutdated() {
        Duration duration = Duration.between(this.issueTime, LocalDateTime.now());

        return (duration.getSeconds() > BEARER_CERT_EXP_TIME);
    }
}
