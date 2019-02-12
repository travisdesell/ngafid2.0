
rm mvn compile exec:java -Dexec.mainClass="org.ngafid.events_db.CalculatePitchEvents"

mvn compile exec:java -Dexec.mainClass="org.ngafid.events_db.ConditionGenerator"
mvn compile exec:java -Dexec.mainClass="org.ngafid.events_db.CalculatePitchEvents"
