mvn compile exec:java -Dexec.mainClass="org.ngafid.kafka.UploadConsumer" -Dexec.jvmArguments="-XX:MaxRAMPercentage=95.0" -Dexec.args="$*"
