/**
 * Normalizes an upload archive name to characters allowed by the server
 * (letters, digits, dash, underscore, period). Spaces and other characters become underscores.
 */
export function sanitizeUploadFilename(filename) {
    if (!filename) {
        return "upload";
    }
    const sanitized = filename
        .replace(/ /g, "_")
        .replace(/[^a-zA-Z0-9_.-]/g, "_")
        .replace(/_+/g, "_");
    return sanitized.length > 0 ? sanitized : "upload";
}
