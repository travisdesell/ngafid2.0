export JAVA_PROGRAM_ARGS=`echo "$@"`
MAVEN_OPTS="-agentpath:/Users/josh/Downloads/async-profiler-3.0-macos/lib/libasyncProfiler.dylib=start,event=cpu,file=profile.html" mvn compile exec:java -Dexec.mainClass="org.ngafid.ProcessUpload" -Dexec.args="$*"
      

# $ java -agentpath:/path/to/libasyncProfiler.so=start,event=cpu,file=profile.html ...
