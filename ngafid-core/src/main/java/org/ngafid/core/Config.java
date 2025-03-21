package org.ngafid.core;

public class Config {
    public static final String NGAFID_UPLOAD_DIR;
    public static final String NGAFID_ARCHIVE_DIR;
    public static final String MUSTACHE_TEMPLATE_DIR;

    static {
        NGAFID_UPLOAD_DIR = getEnvironmentVariable("NGAFID_UPLOAD_DIR");
        NGAFID_ARCHIVE_DIR = getEnvironmentVariable("NGAFID_ARCHIVE_DIR");
        MUSTACHE_TEMPLATE_DIR = getEnvironmentVariable("MUSTACHE_TEMPLATE_DIR");
    }

    public static String getEnvironmentVariable(String key) {
        String value = System.getenv(key);
        if (value == null) {
            System.err.println("ERROR: '" + key + "' environment variable not specified at runtime.");
            System.err.println("Please add the following to your ~/.bash_rc or ~/.profile file:");
            System.err.println("export " + key + "=<value>");
            throw new RuntimeException("Environment variable '" + key + "' not set.");
        }

        return value;
    }
}
