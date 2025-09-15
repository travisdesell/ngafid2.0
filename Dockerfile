FROM eclipse-temurin:24

# Copy configuration files directly (no environment variables needed)
COPY ngafid-core/src/main/resources/ngafid.properties /app/ngafid.properties
COPY ngafid-db-docker.conf /etc/ngafid-db.conf
COPY email-docker.conf /etc/email.conf
COPY resources/log.properties /etc/log.properties

# Copy data files
COPY resources/airports.csv /etc/airports.csv
COPY resources/runways.csv /etc/runways.csv

# Set log configuration
ENV LOG_CONFIG=/etc/log.properties