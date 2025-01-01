package org.ngafid.common;

/**
 * Conversion methods for converting objects into nicely formatted HTML
 */

public final class ConvertToHTML {
    private ConvertToHTML() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated.");
    }

    /**
     * Gets the stack trace from an exception and converts it to a string
     *
     * @param error the exception with the stack trace to convert
     * @return Formatted string of the stack trace
     */
    public static String convertError(Throwable error) {
        StringBuilder sb = new StringBuilder();
        sb.append("<font color=\"red\">");
        for (StackTraceElement element : error.getStackTrace()) {
            sb.append(element.toString());
            sb.append("<br>");
        }
        sb.append("</font>");

        return sb.toString();
    }

    /**
     * Converts newlines to html line breaks
     *
     * @param string the string to convert
     * @return HTML formatted string
     */
    public static String convertString(String string) {
        return string.replaceAll("\n", "<br>");
    }
}
