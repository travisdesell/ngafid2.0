package org.ngafid.common;

public class ConvertToHTML {
    /**
     * Gets the stack trace from an exception and converts it to a string
     *
     * @param error the exception with the stack trace to convert
     * @return Formatted string of the stack trace
     */
    public static String convertError(Throwable error) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : error.getStackTrace()) {
            sb.append(element.toString());
            sb.append("<br>");
        }

        return sb.toString();
    }

    public static String convertString(String string) {
        return string.replaceAll("\n", "<br>");
    }
}
