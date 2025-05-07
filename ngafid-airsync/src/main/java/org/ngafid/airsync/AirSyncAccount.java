package org.ngafid.airsync;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import static org.ngafid.airsync.Utility.OBJECT_MAPPER;

public class AirSyncAccount {

    public final String name;
    public final String accountToken;

    @JsonCreator
    public AirSyncAccount(@JsonProperty("name") String name, @JsonProperty("account_token") String accountToken) {
        this.name = name;
        this.accountToken = accountToken;
    }

    public static List<AirSyncAccount> getAirSyncAccounts(AirSyncFleet fleet) throws IOException {
        byte[] respRaw = getBytes(fleet);
        return OBJECT_MAPPER.readValue(respRaw, new TypeReference<>() {
        });
    }

    private static byte[] getBytes(AirSyncFleet fleet) throws IOException {
        AirSyncAuth authentication = fleet.getAuth();
        HttpsURLConnection netConnection = (HttpsURLConnection) new URL(AirSyncEndpoints.AIRSYNC_ROOT + "/aircraft" +
                "/accounts").openConnection();
        netConnection.setRequestMethod("GET");
        netConnection.setDoOutput(true);
        netConnection.setRequestProperty("Authorization", authentication.bearerString());

        byte[] respRaw;
        try (InputStream is = netConnection.getInputStream()) {
            respRaw = is.readAllBytes();
        }
        return respRaw;
    }
}
