FROM eclipse-temurin:24

# Copy configuration files 
COPY ngafid-core/src/main/resources/ngafid.properties /app/ngafid.properties
COPY resources/log.properties /etc/log.properties

# Copy data files
COPY resources/airports.csv /etc/airports.csv
COPY resources/runways.csv /etc/runways.csv

# Copy JAR files
RUN mkdir -p /opt/ngafid-core
COPY ngafid-core/target/ngafid-core-1.0-SNAPSHOT-jar-with-dependencies.jar /opt/ngafid-core/ngafid-core.jar

# Copy Kafka client config
COPY resources/connect-standalone-docker.properties /etc/connect-standalone-docker.properties

# Set log configuration
ENV LOG_CONFIG=/etc/log.properties

# Default command (can be overridden in docker-compose.yml)
CMD ["java", "-cp", "/opt/ngafid-core/ngafid-core.jar", "org.ngafid.www.NgafidWebServer"]