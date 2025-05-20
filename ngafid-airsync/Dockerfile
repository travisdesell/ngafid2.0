FROM ngafid-base
RUN mkdir /opt/ngafid-airsync
COPY ngafid-airsync/target/ngafid-airsync-1.0-SNAPSHOT-jar-with-dependencies.jar /opt/ngafid-airsync/ngafid-airsync.jar
CMD ["java", "-XX:MaxRAMPercentage=25.0", "-cp", "/opt/ngafid-airsync/ngafid-airsync.jar", "org.ngafid.airsync.ImportService"]