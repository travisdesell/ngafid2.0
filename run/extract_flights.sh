#sh run_extract_flights.sh -f 10 -o ./test_flight

ARGUMENTS="$@"

echo "command line args are: '$ARGUMENTS'"

mvn compile exec:java -Dexec.mainClass="org.ngafid.bin.ExtractFlights" -Dexec.args="$ARGUMENTS"
