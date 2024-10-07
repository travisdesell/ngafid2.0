export JAVA_PROGRAM_ARGS=`echo "$@"`
mvn compile exec:java -Dexec.mainClass="org.ngafid.ProcessUpload" -Dexec.jvmArguments="-Xmx24G" -Dexec.args="$@"
