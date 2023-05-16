mvn clean
mvn compile exec:java -Dexec.mainClass="org.ngafid.events.EventAnnotation" -Dexec.args="$*"
