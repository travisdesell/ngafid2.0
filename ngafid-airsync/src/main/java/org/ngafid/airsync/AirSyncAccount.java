package org.ngafid.airsync;

import static org.ngafid.airsync.Utility.OBJECT_MAPPER;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;

public class AirSyncAccount {

    private final String name;
    private final String accountToken;

    /**
     * Constructs an AirSync account, as deserialized from the AirSync API JSON.
     *
     * @param name the account name
     * @param accountToken the AirSync account token
     */
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

    /**
     * Fetches all AirSync accounts associated with the given fleet from the AirSync API.
     *
     * @param fleet the fleet whose accounts to fetch, used for authentication
     * @return the list of AirSync accounts for the fleet
     * @throws IOException if the AirSync request fails
     */
    public static List<AirSyncAccount> getAirSyncAccounts(AirSyncFleet fleet) throws IOException {
        byte[] respRaw = getBytes(fleet);
        return OBJECT_MAPPER.readValue(respRaw, new TypeReference<>() {});
    }

    private static byte[] getBytes(AirSyncFleet fleet) throws IOException {
        AirSyncAuth authentication = fleet.getAuth();
        HttpsURLConnection netConnection = (HttpsURLConnection)
                new URL(AirSyncEndpoints.AIRSYNC_ROOT + "/aircraft" + "/accounts").openConnection();
        netConnection.setRequestMethod("GET");
        netConnection.setRequestProperty("Authorization", authentication.getBearerString());

        byte[] respRaw;
        try (InputStream is = netConnection.getInputStream()) {
            respRaw = is.readAllBytes();
        }
        return respRaw;
    }
}
