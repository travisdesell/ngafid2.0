package org.ngafid.core.accounts;

import java.io.Serializable;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * A user's per-type email opt-in settings, mapping each {@link EmailType} key to whether the user receives it.
 *
 * <p>Snapshots the current set of known email-type keys (refreshed from the database) at construction and answers
 * whether the user has opted in to a given type, defaulting to disabled for types absent from the map.
 */
public class UserEmailPreferences implements Serializable {

    private static final Logger LOG = Logger.getLogger(UserEmailPreferences.class.getName());
    private final int userId;
    private final HashMap<String, Boolean> emailTypesUser;
    private final String[] emailTypesKeys;

    /**
     * Constructs a user's email preferences from their per-type opt-in map, and snapshots the current set of email-type
     * keys (forcing a refresh from the database via {@link EmailType#getEmailTypeKeysRecent}).
     *
     * @param userId the id of the user these preferences belong to
     * @param emailTypesUser a map from email-type key to whether the user has that type enabled
     */
    public UserEmailPreferences(int userId, HashMap<String, Boolean> emailTypesUser) {
        this.userId = userId;
        this.emailTypesUser = emailTypesUser;

        String[] keysRecent = EmailType.getEmailTypeKeysRecent(true);

        this.emailTypesKeys = keysRecent;
    }

    public HashMap<String, Boolean> getEmailTypesUser() {
        return emailTypesUser;
    }

    /**
     * Returns whether the user has opted in to a given email type, defaulting to false when the type is not present in
     * the preference map.
     *
     * @param emailType the email type to check
     * @return true if the user has this email type enabled, false otherwise
     */
    public boolean getPreference(EmailType emailType) {
        return emailTypesUser.getOrDefault(emailType.getType(), false);
    }
}
