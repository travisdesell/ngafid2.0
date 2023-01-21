package org.ngafid.accounts;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;

import javax.net.ssl.HttpsURLConnection;

import org.ngafid.WebServer;
import org.ngafid.accounts.AirSyncAuth.AccessToken;
import org.ngafid.flights.AirSyncImport;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.lang.reflect.Type;
import java.util.List;
import java.util.logging.Logger;
import java.io.InputStream;

public class AirSyncAircraft {
    private int id;
    private String tailNumber;
    private Fleet fleet;
    private static final Gson gson = WebServer.gson;

    private static final Logger LOG = Logger.getLogger(AirSyncAircraft.class.getName());

    private AirSyncAircraft(int id, String tailNumber) {
        this.id = id;
        this.tailNumber = tailNumber;
    }

    public void initialize(Fleet fleet) {
        this.fleet = fleet;
    }

    public String getTailNumber() {
        return this.tailNumber;
    }

    private URL getAircraftLogURL() throws MalformedURLException {
        //TODO: get this to iterate through the pages
        String baseURL = "https://service-dev.air-sync.com/partner_api/v1/aircraft/%d/logs?page=0&number_of_results=25";

        return new URL(String.format(baseURL, id));
    }

    public List<AirSyncImport> getImports(Connection connection, AirSyncFleet fleet) {
        AirSyncAuth authentication = fleet.getAuth();
        List<AirSyncImport> imports = null;

        try {
            HttpsURLConnection netConnection = (HttpsURLConnection) getAircraftLogURL().openConnection();

            netConnection.setRequestMethod("GET");
            netConnection.setDoOutput(true);
            netConnection.setRequestProperty("Authorization", authentication.bearerString());     

            InputStream is = netConnection.getInputStream();
            byte [] respRaw = is.readAllBytes();

            String resp = new String(respRaw).replaceAll("aircraft_id", "aircraftId");
            resp = resp.replaceAll("tail_number", "tailNumber");
            resp = resp.replaceAll("time_start", "timeStart");
            resp = resp.replaceAll("time_end", "timeEnd");
            resp = resp.replaceAll("file_url", "fileUrl");

            Type target = new TypeToken<List<AirSyncImport>>(){}.getType();
            imports = gson.fromJson(resp, target);

            for (AirSyncImport i : imports) i.init(fleet, this);
        } catch (Exception e) {
            LOG.severe("Caught " + e.toString() + " when making AirSync request!");
        }
        
        return imports;
    }
}
