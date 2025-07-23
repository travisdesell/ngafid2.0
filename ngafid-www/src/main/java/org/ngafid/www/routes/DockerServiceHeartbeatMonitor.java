package org.ngafid.www.routes;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.ngafid.core.kafka.Topic;

public class DockerServiceHeartbeatMonitor implements Runnable {

    private static final Logger LOG = Logger.getLogger(DockerServiceHeartbeatMonitor.class.getName());

    final int TIMEOUT_DURATION_MS = 60_000;
    
    private final Map<String, Map<String, Long>> lastSeen = new ConcurrentHashMap<>();
    private final KafkaConsumer<String,String> consumer;

    public DockerServiceHeartbeatMonitor(Properties consumerProps) {

        consumer = new KafkaConsumer<>(consumerProps);
        Topic.STATUS_HEARTBEAT.subscribeWith(consumer);

    }

    public static DockerServiceHeartbeatMonitor initialize() { 

        DockerServiceHeartbeatMonitor monitorOut;

        //Create the consumer properties
        Properties consumerProps = new Properties();
        String bootstrap = System.getenv("KAFKA_BOOTSTRAP");
        if (bootstrap == null || bootstrap.isEmpty()) {
            throw new RuntimeException("KAFKA_BOOTSTRAP environment variable must be set!");
        }
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "ngafid-heartbeat-monitor");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"latest");

        //Create the monitor instance
        monitorOut = new DockerServiceHeartbeatMonitor(consumerProps);

        //Start the heartbeat monitor in a separate thread
        Thread heartbeatMonitorThread = new Thread(monitorOut, "heartbeat-monitor");
        heartbeatMonitorThread.setDaemon(true);
        heartbeatMonitorThread.start();

        return monitorOut;

    }

    @Override
    public void run() {

        while (true) {

            final int POLL_INTERVAL_S = 5;
            final int RECORD_PARTS_LENGTH_EXPECTED = 3;
            final int RECORD_PARTS_TIMESTAMP_INDEX = 0;
            final int RECORD_PARTS_SERVICE_NAME_INDEX = 1;
            final int RECORD_PARTS_INSTANCE_INDEX = 2;

            consumer.poll(Duration.ofSeconds(POLL_INTERVAL_S)).forEach((ConsumerRecord<String, String> record) -> {

                String[] parts = record.value().split("\\|");

                //Unexpected number of parts in the heartbeat record, skip
                if (parts.length != RECORD_PARTS_LENGTH_EXPECTED) {
                    LOG.log(Level.WARNING, "Received malformed heartbeat: {0}", record.value());
                    return;
                }

                //Read the timestamp and service name from the heartbeat record
                long timestamp = Long.parseLong(parts[RECORD_PARTS_TIMESTAMP_INDEX]);
                String serviceName = parts[RECORD_PARTS_SERVICE_NAME_INDEX];
                String instanceID = parts[RECORD_PARTS_INSTANCE_INDEX];

                //Update the last seen timestamp for this service, or create a new entry if it doesn't exist
                lastSeen
                    .computeIfAbsent(serviceName, key -> new ConcurrentHashMap<>())
                    .put(instanceID, timestamp)
                ;

                LOG.log(Level.INFO, "Received heartbeat from {0} at {1}", new Object[]{serviceName, timestamp});

            });

        }

    }

    public Map<String, StatusJavalinRoutes.ServiceStatus> instanceStatuses(String service) {

        Map<String, Long> instances = lastSeen.get(service);

        //No instances for this service -> empty map
        if (instances == null)
            return Map.of();

        //Calculate the cut-off time for service instances
        long cutOff = System.currentTimeMillis() - TIMEOUT_DURATION_MS;

        //Filter instances that have been seen recently
        return instances.entrySet().stream().collect(
            Collectors.toMap(
                Map.Entry::getKey,
                e -> (e.getValue() >= cutOff)
                    ? StatusJavalinRoutes.ServiceStatus.OK
                    : StatusJavalinRoutes.ServiceStatus.WARNING
            )
        );
        
    }

    public StatusJavalinRoutes.ServiceStatus status(String service) {

        Map<String, Long> serviceInstances = lastSeen.get(service);

        //No instances for this service -> ERROR
        if (serviceInstances==null || serviceInstances.isEmpty()) {
            LOG.log(Level.WARNING, "No heartbeat received for service: {0}", service);
            return StatusJavalinRoutes.ServiceStatus.ERROR;
        }

        //Calculate the cut-off time for service instances
        long cutOff = System.currentTimeMillis() - TIMEOUT_DURATION_MS;

        //Init flags for service status
        boolean anyAlive = false;
        boolean anyTimeout = false;

        //Check the last seen timestamps for each service instance
        for (long seen : serviceInstances.values()) {

            //Service instance has been seen recently -> anyAlive
            if (seen > cutOff)
                anyAlive = true;

            //Otherwise -> anyTimeout
            else
                anyTimeout = true;

        }
        
        //All (at least 1) service instances have been seen recently -> OK
        if (anyAlive && !anyTimeout)
            return StatusJavalinRoutes.ServiceStatus.OK;

        //Haven't seen this service in a while -> WARNING
        if (anyAlive)
            return StatusJavalinRoutes.ServiceStatus.WARNING;

        //No instances have been seen recently -> ERROR
        return StatusJavalinRoutes.ServiceStatus.ERROR;

    }


}