package org.ngafid.accounts;

import java.net.MalformedURLException;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

import org.ngafid.WebServer;
import org.ngafid.accounts.AirSyncAuth.AccessToken;
import org.ngafid.flights.AirSyncUpload;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.lang.reflect.Type;
import java.util.List;
import java.io.InputStream;

public class AirSyncAircraft {
    private int id;
    private String tailNumber;
    private static final Gson gson = WebServer.gson;


    public AirSyncAircraft(int id, String tailNumber) {
        this.id = id;
        this.tailNumber = tailNumber;
    }

    private URL getAircraftLogURL() throws MalformedURLException {
        String baseURL = "https://service-dev.air-sync.com/partner_api/v1/aircraft/%d/logs?page=0&number_of_results=25";

        return new URL(String.format(baseURL, id));
    }

    public List<AirSyncUpload> getUploads(AirSyncAuth authentication) throws Exception {
        HttpsURLConnection connection = (HttpsURLConnection) getAircraftLogURL().openConnection();

        connection.setRequestMethod("GET");
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", authentication.bearerString());     

        InputStream is = connection.getInputStream();
        byte [] respRaw = is.readAllBytes();

        String resp = new String(respRaw).replaceAll("aircraft_id", "aircraftId");
        resp = resp.replaceAll("time_start", "timeStart");
        resp = resp.replaceAll("time_end", "timeEnd");
        resp = resp.replaceAll("file_url", "fileUrl");

        System.out.println(resp);

        Type target = new TypeToken<List<AirSyncUpload>>(){}.getType();
        return gson.fromJson(resp, target);
    }
}
