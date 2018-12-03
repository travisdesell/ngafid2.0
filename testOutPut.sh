rm /Users/fa3019/Code/ngafid2.0/src/main/java/org/ngafid/TrackPitchEvents.java
mvn compile exec:java -Dexec.mainClass="org.ngafid.CreateEventTracker"
mvn compile exec:java -Dexec.mainClass="org.ngafid.TrackPitchEvents"
