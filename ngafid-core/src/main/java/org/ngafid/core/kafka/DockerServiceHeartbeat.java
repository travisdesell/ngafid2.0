package org.ngafid.core.kafka;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

public class DockerServiceHeartbeat {

    /**
     * Create and send periodic heartbeat messages
     * containing the Docker service's name to the
     * 'ngafid.heartbeat' Kafka topic.
     */

    public static final boolean USING_DOCKER =
            Files.exists(Path.of("/.dockerenv"))
        ||  Files.exists(Path.of("/run/.containerenv"))
    ;

    
    private static final Logger LOG = Logger.getLogger(DockerServiceHeartbeat.class.getName());


    private static final String TOPIC = Topic.STATUS_HEARTBEAT.toString();
    private static final ScheduledExecutorService SCHED = Executors.newSingleThreadScheduledExecutor();


    private static final String SERVICE_NAME_UNKNOWN = "unknown-service";


    private static void start(KafkaProducer<String,String> producer, String serviceName, String instanceId, long periodMs) {

        final Runnable beat = () -> {
            String payload = "%d|%s|%s".formatted(System.currentTimeMillis(), serviceName, instanceId);
            producer.send(new ProducerRecord<>(TOPIC, serviceName, payload));
        };

        SCHED.scheduleAtFixedRate(beat, 0, periodMs, TimeUnit.MILLISECONDS);

    }

    public static void autostart() throws UnknownHostException {

        /*
         * Helper function to automatically start the heartbeat
         * when called from the main method of a consumer.
         */

        //Get service name
        final String service = System.getenv().getOrDefault("SERVICE_NAME", SERVICE_NAME_UNKNOWN);

        //Got unknown service name, do not start heartbeat
        if (service.equals(SERVICE_NAME_UNKNOWN)) {
            LOG.warning("No SERVICE_NAME environment variable set, not starting heartbeat");
            return;
        }

        //Not running in Docker, do not start heartbeat
        if (!USING_DOCKER) {
            LOG.log(Level.WARNING, "Detected running outside of Docker, heartbeat not started for service: {0}", service);
            return;
        }

        //Get properties for the heartbeat producer
        final Properties heartbeatProps = new Properties();
        String bootstrap = System.getenv("KAFKA_BOOTSTRAP");
        if (bootstrap == null || bootstrap.isEmpty()) {
            throw new RuntimeException("KAFKA_BOOTSTRAP environment variable must be set!");
        }
        heartbeatProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        heartbeatProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        heartbeatProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        //Create the heartbeat producer
        final KafkaProducer<String, String> heartbeatProducer = new KafkaProducer<>(heartbeatProps);

        //Get instance ID and heartbeat interval
        final String instance = InetAddress.getLocalHost().getHostName();
        final long heartbeatIntervalMS = Long.parseLong(System.getenv().getOrDefault("HEARTBEAT_INTERVAL_MS", "10_000"));

        //Start the heartbeat
        DockerServiceHeartbeat.start(heartbeatProducer, service, instance, heartbeatIntervalMS);
        
    }

}