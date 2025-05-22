package org.ngafid.core.accounts;

import java.io.Serializable;
import java.util.HashMap;
import java.util.logging.Logger;

public class UserEmailPreferences implements Serializable {

    private static final Logger LOG = Logger.getLogger(UserEmailPreferences.class.getName());
    private final int userId;
    private final HashMap<String, Boolean> emailTypesUser;
    private final String[] emailTypesKeys;


    public UserEmailPreferences(int userId, HashMap<String, Boolean> emailTypesUser) {
        this.userId = userId;
        this.emailTypesUser = emailTypesUser;

        String[] keysRecent = EmailType.getEmailTypeKeysRecent(true);

        this.emailTypesKeys = keysRecent;
    }

    public HashMap<String, Boolean> getEmailTypesUser() {
        return emailTypesUser;
    }

    public boolean getPreference(EmailType emailType) {
        return emailTypesUser.getOrDefault(emailType.getType(), false);
    }
}
