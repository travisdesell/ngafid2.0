// ngafid-core/src/main/java/org/ngafid/core/accounts/PasswordPolicy.java
package org.ngafid.core.accounts;

public final class PasswordPolicy {
    public static final int MIN_LENGTH = 10;
    public static final String ALLOWED_SPECIAL_CHARACTERS = "@#$%^&*()_+!/\\.,";
    public static final String REQUIREMENTS_MESSAGE = "Must be at least " + MIN_LENGTH
            + " characters long and contain only letters, numbers, spaces, and these special characters: "
            + ALLOWED_SPECIAL_CHARACTERS;

    private PasswordPolicy() {
        // Utility class
    }

    public static String validationError(String password) {

        // Empty password
        if (password == null || password.length() == 0)
            return "Password is required.";

        // Password too short / contains invalid characters
        if (password.length() < MIN_LENGTH || !containsOnlyAllowedCharacters(password))
            return "Password is not valid. " + REQUIREMENTS_MESSAGE;

        return null;

    }

    public static boolean isValid(String password) {
        return validationError(password) == null;
    }

    private static boolean containsOnlyAllowedCharacters(String password) {

        for (int i = 0; i < password.length(); i++) {

            char value = password.charAt(i);

            // Got invalid character -> False
            if (!isAllowedCharacter(value))
                return false;

        }

        return true;

    }

    private static boolean isAllowedCharacter(char value) {

        /*
            Allowed characters are:
            
            - Lowercase letters (a-z)
            - Uppercase letters (A-Z)
            - Digits (0-9)
            - Space ( )
            - Special characters defined in ALLOWED_SPECIAL_CHARACTERS
        */

        return (value >= 'a' && value <= 'z')
                || (value >= 'A' && value <= 'Z')
                || (value >= '0' && value <= '9')
                || value == ' '
                || ALLOWED_SPECIAL_CHARACTERS.indexOf(value) >= 0;
    }
}
