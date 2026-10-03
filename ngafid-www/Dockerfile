# ngafid-base is a locally-built image (see the root Dockerfile), not a registry
# image, so there is no published version tag to pin.
# hadolint ignore=DL3006
FROM ngafid-base
RUN mkdir /opt/ngafid-www
COPY ngafid-www/target/ngafid-www-1.0-SNAPSHOT-jar-with-dependencies.jar /opt/ngafid-www/ngafid-www.jar
CMD ["java", "-XX:MaxRAMPercentage=25.0", "-cp", "/opt/ngafid-www/ngafid-www.jar", "org.ngafid.www.WebServer"]