// ngafid-frontend/src/app/lib/password_validation.ts
export const PASSWORD_MIN_LENGTH = 10;
export const PASSWORD_ALLOWED_SPECIAL_CHARACTERS = "@#$%^&*()_+!/\\.,";
export const PASSWORD_REQUIREMENTS_MESSAGE =
    `Must be at least ${PASSWORD_MIN_LENGTH} characters long and contain only letters, numbers, spaces, and these special characters: ${PASSWORD_ALLOWED_SPECIAL_CHARACTERS}`;

const PASSWORD_ALLOWED_REGEX = /^[@#$%^&*()_+!/\\.,a-zA-Z0-9 ]*$/;

export interface PasswordValidationResult {
    valid: boolean;
    message: string;
}

function invalid(message: string): PasswordValidationResult {
    return { valid: false, message };
}

function valid(): PasswordValidationResult {
    return { valid: true, message: "" };
}

export function validateNewPassword(password: string, label = "Password"): PasswordValidationResult {
    if (password.length === 0)
        return invalid(`${label} is required.`);

    if (password.length < PASSWORD_MIN_LENGTH || !PASSWORD_ALLOWED_REGEX.test(password))
        return invalid(`${label} is not valid. ${PASSWORD_REQUIREMENTS_MESSAGE}`);

    return valid();
}

export function validateConfirmedPassword(
    password: string,
    confirmPassword: string,
    passwordLabel = "Password",
    confirmPasswordLabel = "Confirmation password",
): PasswordValidationResult {
    const passwordValidation = validateNewPassword(password, passwordLabel);
    if (!passwordValidation.valid)
        return passwordValidation;

    const confirmPasswordValidation = validateNewPassword(confirmPassword, confirmPasswordLabel);
    if (!confirmPasswordValidation.valid)
        return confirmPasswordValidation;

    if (password !== confirmPassword)
        return invalid(`${passwordLabel} and ${confirmPasswordLabel.toLowerCase()} must match.`);

    return valid();
}

export function validatePasswordChange(
    currentPassword: string,
    newPassword: string,
    confirmPassword: string,
): PasswordValidationResult {

    // Password is empty
    if (currentPassword.length === 0)
        return invalid("Current password is required.");

    const passwordValidation = validateConfirmedPassword(newPassword, confirmPassword, "New password", "Confirmation password");
    if (!passwordValidation.valid)
        return passwordValidation;

    if (currentPassword === newPassword)
        return invalid("New password cannot be the same as your current password.");

    return valid();
    
}
