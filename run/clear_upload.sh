export JAVA_PROGRAM_ARGS=`echo "$@"`
mvn compile exec:java -Dexec.mainClass="org.ngafid.ClearUpload" -Dexec.args="$*"
