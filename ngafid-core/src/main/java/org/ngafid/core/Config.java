package org.ngafid.core;

public class Config {
    public static final int NGAFID_PORT;
    public static final int MAX_TERRAIN_CACHE_SIZE;
    public static final int PARALLELISM;

    public static final boolean NGAFID_USE_MARIA_DB;
    public static final boolean NGAFID_EMAIL_ENABLED;
    public static final boolean DISABLE_PERSISTENT_SESSIONS;

    public static final String NGAFID_DB_INFO;
    public static final String NGAFID_UPLOAD_DIR;
    public static final String NGAFID_ARCHIVE_DIR;
    public static final String NGAFID_STATIC_DIR;
    public static final String NGAFID_TERRAIN_DIR;
    public static final String MUSTACHE_TEMPLATE_DIR;
    public static final String AIRPORTS_FILE;
    public static final String RUNWAYS_FILE;
    public static final String KAFKA_CONFIG_FILE;
    public static final String EMAIL_INFO_FILE;
    public static final String NGAFID_ADMIN_EMAILS;
    public static final String LOG_PROPERTIES_FILE;

    static {
        PARALLELISM = Integer.parseInt(getEnvironmentVariable("NGAFID_PARALLELISM", Runtime.getRuntime().availableProcessors() + ""));
        NGAFID_PORT = Integer.parseInt(getEnvironmentVariable("NGAFID_PORT", "8181"));
        MAX_TERRAIN_CACHE_SIZE = Integer.parseInt(getEnvironmentVariable("MAX_TERRAIN_CACHE_SIZE", "384"));

        NGAFID_USE_MARIA_DB = Boolean.parseBoolean(getEnvironmentVariable("NGAFID_USE_MARIA_DB", "false"));
        NGAFID_EMAIL_ENABLED = Boolean.parseBoolean(getEnvironmentVariable("NGAFID_EMAIL_ENABLED", "false"));
        DISABLE_PERSISTENT_SESSIONS = Boolean.parseBoolean(getEnvironmentVariable("DISABLE_PERSISTANT_SESSIONS", "false"));

        AIRPORTS_FILE = getEnvironmentVariable("AIRPORTS_FILE");
        RUNWAYS_FILE = getEnvironmentVariable("RUNWAYS_FILE");
        NGAFID_UPLOAD_DIR = getEnvironmentVariable("NGAFID_UPLOAD_DIR");
        NGAFID_ARCHIVE_DIR = getEnvironmentVariable("NGAFID_ARCHIVE_DIR");
        NGAFID_STATIC_DIR = getEnvironmentVariable("NGAFID_STATIC_DIR");
        NGAFID_TERRAIN_DIR = getEnvironmentVariable("NGAFID_TERRAIN_DIR");
        MUSTACHE_TEMPLATE_DIR = NGAFID_STATIC_DIR + "/templates";
        NGAFID_DB_INFO = getEnvironmentVariable("NGAFID_DB_INFO");
        KAFKA_CONFIG_FILE = getEnvironmentVariable("KAFKA_CONFIG_FILE");
        EMAIL_INFO_FILE = getEnvironmentVariable("EMAIL_INFO_FILE");
        NGAFID_ADMIN_EMAILS = getEnvironmentVariable("NGAFID_ADMIN_EMAILS", "");
        LOG_PROPERTIES_FILE = getEnvironmentVariable("LOG_PROPERTIES_FILE", "resources/log.properties");
    }

    public static String getEnvironmentVariable(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null ? defaultValue : value;
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
// Trigger workflow
