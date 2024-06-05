for CLUSTER in {1..39}
do
    MKDIR_COMMAND="mkdir /mnt/md3200_ngafid/c_"$CLUSTER"_with_ids"
    echo $MKDIR_COMMAND
    $MKDIR_COMMAND

    CLUSTER_FILES=~/und_maintenance_clusters/c_*.csv
    #echo $CLUSTER_FILES

    EXTRACT_COMMAND='sh run_extract_maintenance_flights.sh /mnt/md3200_ngafid/c_'$CLUSTER'_with_ids c_'$CLUSTER' '$CLUSTER_FILES
    echo $EXTRACT_COMMAND
    $EXTRACT_COMMAND
done
