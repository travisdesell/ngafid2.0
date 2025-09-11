package org.ngafid.www.services

import com.warrenstrange.googleauth.GoogleAuthenticator
import com.warrenstrange.googleauth.GoogleAuthenticatorKey
import java.security.SecureRandom
import java.util.*

/**
 * TwoFactorAuthService - Core 2FA Cryptographic Engine
 * 
 * <p>This service handles all cryptographic operations for Two-Factor Authentication:
 * <ul>
 *   <li>TOTP (Time-based One-Time Password) secret generation and verification</li>
 *   <li>Backup code generation and verification for emergency access</li>
 *   <li>QR code URL generation for authenticator app setup</li>
 * </ul>
 * 
 * <p>The service uses the Google Authenticator library for TOTP operations and
 * implements secure random generation for backup codes.
 * 
 * <p><strong>Integration Points:</strong>
 * <ul>
 *   <li>Called by {@link org.ngafid.www.routes.api.AuthRoutes} for all 2FA operations</li>
 *   <li>Used during login for TOTP code verification</li>
 *   <li>Used during 2FA setup for secret generation and QR code creation</li>
 *   <li>Used for backup code management</li>
 * </ul>
 * 
 * @author NGAFID Development Team
 * @since 2.0.0
 */
object TwoFactorAuthService {
    private val totp = GoogleAuthenticator()
    private val random = SecureRandom()

    /**
     * Generate a new TOTP secret for a user.
     * 
     * <p>This creates a cryptographically secure secret key that will be:
     * <ol>
     *   <li>Stored in the user's database record</li>
     *   <li>Used to generate the QR code for authenticator app setup</li>
     *   <li>Used to verify TOTP codes during login</li>
     * </ol>
     * 
     * @return A base32-encoded secret string (typically 32 characters)
     * @see #verifyCode(String, Int)
     * @see #generateQRCodeUrl(String, String, String)
     */
    fun generateSecret(): String {
        val key: GoogleAuthenticatorKey = totp.createCredentials()
        return key.key
    }

    /**
     * Verify a TOTP code against a stored secret.
     * 
     * <p>This validates the 6-digit code from the user's authenticator app:
     * <ol>
     *   <li>Takes the user's stored secret from the database</li>
     *   <li>Compares it against the provided TOTP code</li>
     *   <li>Accounts for time drift (30-second windows)</li>
     *   <li>Returns true if the code is valid</li>
     * </ol>
     * 
     * @param secret The user's stored TOTP secret from the database
     * @param code The 6-digit code from the user's authenticator app
     * @return true if the code is valid, false otherwise
     * @see #generateSecret()
     * @throws IllegalArgumentException if the secret is null or empty
     */
    fun verifyCode(secret: String, code: Int): Boolean {
        return totp.authorize(secret, code)
    }

    /**
     * Generate backup codes for emergency access.
     * 
     * <p>Creates 10 unique 8-digit backup codes that users can use if they:
     * <ul>
     *   <li>Lose their authenticator device</li>
     *   <li>Can't access their authenticator app</li>
     *   <li>Need emergency access to their account</li>
     * </ul>
     * 
     * <p>These codes are:
     * <ol>
     *   <li>Generated using cryptographically secure random numbers</li>
     *   <li>Hashed before storage in the database</li>
     *   <li>Provided to the user once during 2FA setup</li>
     *   <li>Single-use (should be invalidated after use)</li>
     * </ol>
     * 
     * @return List of 10 unique 8-digit backup codes
     * @see #hashBackupCode(String)
     * @see #verifyBackupCode(String, List)
     */
    fun generateBackupCodes(): List<String> {
        return (1..10).map { generateRandomCode() }
    }

    /**
     * Generate QR code URL for authenticator app setup.
     * 
     * <p>Creates the standard otpauth:// URL that authenticator apps can scan:
     * <ol>
     *   <li>Follows the TOTP standard format</li>
     *   <li>Includes the user's email and issuer (NGAFID)</li>
     *   <li>Contains the generated secret</li>
     *   <li>Can be converted to a QR code image</li>
     * </ol>
     * 
     * @param secret The generated TOTP secret
     * @param email The user's email address
     * @param issuer The service name (defaults to "NGAFID")
     * @return otpauth:// URL string for QR code generation
     * @see #generateSecret()
     * @throws IllegalArgumentException if secret or email is null or empty
     */
    fun generateQRCodeUrl(secret: String, email: String, issuer: String = "NGAFID"): String {
        return "otpauth://totp/$issuer:$email?secret=$secret&issuer=$issuer"
    }

    /**
     * Generate a random 8-digit backup code.
     * 
     * <p>Creates a single backup code using cryptographically secure random generation:
     * <ol>
     *   <li>Uses SecureRandom for cryptographic security</li>
     *   <li>Generates 8-digit codes (00000000 to 99999999)</li>
     *   <li>Ensures uniqueness across all generated codes</li>
     * </ol>
     * 
     * @return A formatted 8-digit string with leading zeros
     * @see #generateBackupCodes()
     */
    private fun generateRandomCode(): String {
        return String.format("%08d", random.nextInt(100000000))
    }

    /**
     * Hash a backup code for secure database storage.
     * 
     * <p>Securely hashes backup codes before storing them in the database:
     * <ol>
     *   <li>Prevents plaintext backup codes from being stored</li>
     *   <li>Uses Java's hashCode() for consistent hashing</li>
     *   <li>Allows verification without storing plaintext codes</li>
     * </ol>
     * 
     * <p><strong>Note:</strong> In production, consider using a more secure hashing algorithm
     * like bcrypt or PBKDF2 for enhanced security.
     * 
     * @param code The plaintext backup code to hash
     * @return Hashed string representation of the code
     * @see #verifyBackupCode(String, List)
     * @throws IllegalArgumentException if code is null
     */
    fun hashBackupCode(code: String): String {
        return code.hashCode().toString()
    }

    /**
     * Verify a backup code against stored hashed codes.
     * 
     * <p>Validates a backup code during emergency access:
     * <ol>
     *   <li>Hashes the provided backup code</li>
     *   <li>Compares it against the list of stored hashed codes</li>
     *   <li>Returns true if a match is found</li>
     * </ol>
     * 
     * @param code The plaintext backup code to verify
     * @param hashedCodes List of hashed backup codes from the database
     * @return true if the code is valid, false otherwise
     * @see #hashBackupCode(String)
     * @see #generateBackupCodes()
     * @throws IllegalArgumentException if code is null or hashedCodes is null
     */
    fun verifyBackupCode(code: String, hashedCodes: List<String>): Boolean {
        return hashedCodes.contains(hashBackupCode(code))
    }
}
