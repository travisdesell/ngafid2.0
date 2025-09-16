package org.ngafid.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private static final Properties properties = new Properties();
    private static final String PROPERTIES_FILE = "ngafid.properties";
    private static final boolean IS_DOCKER_ENVIRONMENT;
    private static boolean environmentLogged = false;
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
        // Check Docker environment once and cache the result
        IS_DOCKER_ENVIRONMENT = isRunningInDocker();
        
        // Load properties file
        loadProperties();
        
        // Initialize configuration values from properties file
        PARALLELISM = getIntPropertyWithDefault("ngafid.parallelism", Runtime.getRuntime().availableProcessors());
        NGAFID_PORT = getIntPropertyWithDefault("ngafid.port", 8181);
        MAX_TERRAIN_CACHE_SIZE = getIntPropertyWithDefault("ngafid.max.terrain.cache.size", 384);

        NGAFID_USE_MARIA_DB = getBooleanPropertyWithDefault("ngafid.use.maria.db", false);
        NGAFID_EMAIL_ENABLED = getBooleanPropertyWithDefault("ngafid.email.enabled", false);
        DISABLE_PERSISTENT_SESSIONS = getBooleanPropertyWithDefault("ngafid.disable.persistent.sessions", false);

        AIRPORTS_FILE = getStringProperty("ngafid.airports.file");
        RUNWAYS_FILE = getStringProperty("ngafid.runways.file");
        NGAFID_UPLOAD_DIR = getStringProperty("ngafid.upload.dir");
        NGAFID_ARCHIVE_DIR = getStringProperty("ngafid.archive.dir");
        NGAFID_STATIC_DIR = getStringProperty("ngafid.static.dir");
        NGAFID_TERRAIN_DIR = getStringProperty("ngafid.terrain.dir");
        MUSTACHE_TEMPLATE_DIR = NGAFID_STATIC_DIR + "/templates";
        NGAFID_DB_INFO = getStringProperty("ngafid.db.info");
        KAFKA_CONFIG_FILE = getStringProperty("ngafid.kafka.config.file");
        EMAIL_INFO_FILE = getStringProperty("ngafid.email.info");
        NGAFID_ADMIN_EMAILS = getStringProperty("ngafid.admin.emails");
        LOG_PROPERTIES_FILE = getStringProperty("ngafid.log.properties.file");
    }
    
    private static void loadProperties() {
        // Check for custom properties file first
        String customPropertiesFile = System.getProperty("ngafid.config.file");
        if (customPropertiesFile != null) {
            try (InputStream input = Config.class.getClassLoader().getResourceAsStream(customPropertiesFile)) {
                if (input != null) {
                    properties.load(input);
                    System.out.println("Loaded configuration from " + customPropertiesFile);
                    return;
                }
            } catch (IOException e) {
                System.err.println("Error loading custom properties file " + customPropertiesFile + ": " + e.getMessage());
            }
        }
        
        // Load the unified properties file
        try (InputStream input = Config.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input != null) {
                properties.load(input);
                System.out.println("Loaded unified configuration from " + PROPERTIES_FILE);
                
                // Resolve variable substitutions
                resolveVariableSubstitutions();
            } else {
                System.err.println("Properties file " + PROPERTIES_FILE + " not found!");
                throw new RuntimeException("Configuration file not found: " + PROPERTIES_FILE);
            }
        } catch (IOException e) {
            System.err.println("Error loading properties file: " + e.getMessage());
            throw new RuntimeException("Failed to load configuration file: " + PROPERTIES_FILE, e);
        }
    }
    
    private static String getStringProperty(String propertyKey) {
        // First try system property (can be set via -D)
        String systemProperty = System.getProperty(propertyKey);
        if (systemProperty != null) {
            return systemProperty;
        }
        
        // Use cached Docker environment check
        boolean isDocker = IS_DOCKER_ENVIRONMENT;
        
        // Try environment-specific property first, then fallback to general property
        String propertyValue = null;
        if (isDocker) {
            String dockerPropertyKey = propertyKey.replace("ngafid.", "ngafid.docker.");
            propertyValue = properties.getProperty(dockerPropertyKey);
        }
        
        // If no Docker-specific property found, try the general property
        if (propertyValue == null) {
            propertyValue = properties.getProperty(propertyKey);
        }
        
        if (propertyValue != null) {
            return propertyValue;
        }
        
        // If no value found, throw exception
        System.err.println("ERROR: Configuration value not found for property '" + propertyKey + "'");
        System.err.println("Please either:");
        System.err.println("1. Set the property in ngafid.properties file");
        System.err.println("2. Set the system property: -D" + propertyKey + "=<value>");
        throw new RuntimeException("Configuration value not found for '" + propertyKey + "'");
    }
    

    // New methods for property-based configuration
    public static String getProperty(String key) {
        return getStringProperty(key);
    }
    
    public static int getIntProperty(String key) {
        String value = getStringProperty(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.err.println("Invalid integer value for " + key + ": " + value);
            throw new RuntimeException("Invalid integer value for '" + key + "': " + value);
        }
    }
    
    private static int getIntPropertyWithDefault(String key, int defaultValue) {
        try {
            String value = getStringProperty(key);
            return Integer.parseInt(value);
        } catch (RuntimeException e) {
            // Property not found, use default
            return defaultValue;
        }
    }
    
    public static boolean getBooleanProperty(String key) {
        String value = getStringProperty(key);
        return Boolean.parseBoolean(value);
    }
    
    private static boolean getBooleanPropertyWithDefault(String key, boolean defaultValue) {
        try {
            String value = getStringProperty(key);
            return Boolean.parseBoolean(value);
        } catch (RuntimeException e) {
            // Property not found, use default
            return defaultValue;
        }
    }
    
    /**
     * Resolves variable substitutions in properties (e.g., ${ngafid.repo.path})
     */
    private static void resolveVariableSubstitutions() {
        // First pass: resolve basic variables
        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            if (value != null && value.contains("${")) {
                String resolved = resolveVariables(value);
                properties.setProperty(key, resolved);
            }
        }
        
        // Second pass: resolve nested variables
        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            if (value != null && value.contains("${")) {
                String resolved = resolveVariables(value);
                properties.setProperty(key, resolved);
            }
        }
    }
    
    /**
     * Resolves variables in a string value
     */
    private static String resolveVariables(String value) {
        if (value == null || !value.contains("${")) {
            return value;
        }
        
        String result = value;
        int start = result.indexOf("${");
        while (start != -1) {
            int end = result.indexOf("}", start);
            if (end != -1) {
                String varName = result.substring(start + 2, end);
                String varValue = properties.getProperty(varName);
                if (varValue != null) {
                    result = result.substring(0, start) + varValue + result.substring(end + 1);
                } else {
                    // Variable not found, leave as is
                    start = result.indexOf("${", end);
                }
            } else {
                break;
            }
            start = result.indexOf("${");
        }
        
        return result;
    }
    
    /**
     * Public method to check if running in Docker environment
     */
    public static boolean isDockerEnvironment() {
        return IS_DOCKER_ENVIRONMENT;
    }
    
    /**
     * Detects if the application is running inside a Docker container
     * by checking for the presence of /.dockerenv file
     */
    private static boolean isRunningInDocker() {
        try {
            java.io.File dockerEnv = new java.io.File("/.dockerenv");
            boolean exists = dockerEnv.exists();
            
            // Only log once to avoid spam
            if (!environmentLogged) {
                System.out.println("Checking /.dockerenv: " + exists);
                if (exists) {
                    System.out.println("Docker environment detected - using Docker properties");
                } else {
                    System.out.println("Development environment detected - using development properties");
                }
                environmentLogged = true;
            }
            
            return exists;
        } catch (Exception e) {
            if (!environmentLogged) {
                System.out.println("Error checking /.dockerenv: " + e.getMessage());
                System.out.println("Development environment detected - using development properties");
                environmentLogged = true;
            }
            return false;
        }
    }
}
