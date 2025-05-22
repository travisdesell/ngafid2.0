FROM ngafid-base
RUN mkdir /opt/ngafid-data-processor
COPY ngafid-data-processor/target/ngafid-data-processor-1.0-SNAPSHOT-jar-with-dependencies.jar /opt/ngafid-data-processor/ngafid-data-processor.jar
CMD ["java", "-XX:MaxRAMPercentage=25.0", "-cp", "/opt/ngafid-data-processor/ngafid-data-processor.jar", "org.ngafid.processor.UploadConsumer"]