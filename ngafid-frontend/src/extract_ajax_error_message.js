// ngafid-frontend/src/extract_ajax_error_message.js

import { showErrorModal } from "./error_modal";

/**
 * Extracts a user-friendly error message from a jQuery AJAX error response.
 * The function attempts to find an error message in the following order:
 * 1. `responseJSON.errorMessage` field in the jqXHR object
 * 2. `responseJSON.message` field in the jqXHR object
 * 3. Non-empty `responseText` from the jqXHR object, optionally parsed as JSON for structured messages
 * 4. The `errorThrown` parameter if it's a non-generic string
 * 5. The `statusText` from the jqXHR object if it's non-empty
 * 6. A provided fallback message if all else fails
 *
 * @param {object} jqXHR - The jQuery XHR object from the AJAX error callback
 * @param {string} errorThrown - The error thrown by jQuery (e.g., "timeout", "error", etc.)
 * @param {string} fallbackMessage - A default message to return if no specific error message is found
 * @returns {string} A user-friendly error message extracted from the AJAX response
 */
export default function extractAjaxErrorMessage(jqXHR, errorThrown, fallbackMessage = "Request failed.") {
    
    const responseJson = jqXHR?.responseJSON;

    // Got a JSON response with an error message
    if (responseJson?.errorMessage)
        return responseJson.errorMessage;

    // Got a JSON response with a message (but no specific errorMessage field)
    if (responseJson?.message)
        return responseJson.message;

    const responseText = (typeof jqXHR?.responseText === "string")
        ? jqXHR.responseText.trim()
        : "";

    // Got a non-empty response text...
    if (responseText) {

        // Attempt to parse it as JSON in case it's a structured error message
        try {

            const parsed = JSON.parse(responseText);

            // Parsed an error message from the response text
            if (parsed?.errorMessage)
                return parsed.errorMessage;

            // Parsed a generic message from the response text
            if (parsed?.message)
                return parsed.message;

        // Parsing failed, return the raw response text
        } catch {
            return responseText;
        }

        return responseText;

    }

    // Attempt to use the errorThrown parameter
    const thrown = (typeof errorThrown === "string")
        ? errorThrown.trim()
        : "";

    if (thrown && thrown.toLowerCase() !== "error")
        return thrown;

    // Attempt to use the jqXHR status text
    const statusText = (typeof jqXHR?.statusText === "string")
        ? jqXHR.statusText.trim()
        : "";

    if (statusText)
        return statusText;

    return fallbackMessage;

}

/**
 * Shows a modal with a user-friendly error message extracted from a jQuery AJAX error response.
 * 
 * @param {object} jqXHR - The jQuery XHR object from the AJAX error callback
 * @param {string} errorThrown - The error thrown by jQuery (e.g., "timeout", "error", etc.)
 * @param {string} fallbackMessage - A default message to return if no specific error message is found
 */
export function showAjaxErrorModal(jqXHR, errorThrown, fallbackMessage) {

    const message = extractAjaxErrorMessage(jqXHR, errorThrown, fallbackMessage);
    showErrorModal("Error", message);

}