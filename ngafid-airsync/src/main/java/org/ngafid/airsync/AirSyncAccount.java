package org.ngafid.airsync;

import static org.ngafid.airsync.Utility.OBJECT_MAPPER;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class AirSyncAccount {

    private final String name;
    private final String accountToken;

    @JsonCreator
    public AirSyncAccount(@JsonProperty("name") String name, @JsonProperty("account_token") String accountToken) {
        this.name = name;
        this.accountToken = accountToken;
    }

    public String getName() {
        return name;
    }

    public String getAccountToken() {
        return accountToken;
    }

    public static List<AirSyncAccount> getAirSyncAccounts(AirSyncFleet fleet) throws IOException {
        byte[] respRaw = getBytes(fleet);
        return OBJECT_MAPPER.readValue(respRaw, new TypeReference<>() {});
    }

    private static byte[] getBytes(AirSyncFleet fleet) throws IOException, URISyntaxException {
        AirSyncAuth authentication = fleet.getAuth();

        URI uri = new URI(AirSyncEndpoints.AIRSYNC_ROOT + "/aircraft" + "/accounts");
        URL url = uri.toURL();
        HttpsURLConnection netConnection = (HttpsURLConnection) url.openConnection();

        netConnection.setRequestMethod("GET");
        netConnection.setRequestProperty("Authorization", authentication.getBearerString());

        byte[] respRaw;
        try (InputStream is = netConnection.getInputStream()) {
            respRaw = is.readAllBytes();
        }
        return respRaw;
    }
}
