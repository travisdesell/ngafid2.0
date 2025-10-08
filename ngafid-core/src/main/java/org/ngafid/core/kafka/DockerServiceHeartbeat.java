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
import org.ngafid.core.Config;

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

        //Get service name - try to detect from context or use default
        String service = Config.getProperty("ngafid.service.name");
        
        // If still unknown, try to detect from main class or stack trace
        if (service.equals(SERVICE_NAME_UNKNOWN)) {
            service = detectServiceName();
        }

        //Got unknown service name, do not start heartbeat
        if (service.equals(SERVICE_NAME_UNKNOWN)) {
            LOG.warning("No service name configured, not starting heartbeat");
            return;
        }

        //Not running in Docker, do not start heartbeat
        if (!USING_DOCKER) {
            LOG.log(Level.WARNING, "Detected running outside of Docker, heartbeat not started for service: {0}", service);
            return;
        }

        //Get properties for the heartbeat producer
        final Properties heartbeatProps = new Properties();
        String bootstrap = Config.getProperty("ngafid.kafka.bootstrap.servers");
        heartbeatProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        heartbeatProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        heartbeatProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        //Create the heartbeat producer
        final KafkaProducer<String, String> heartbeatProducer = new KafkaProducer<>(heartbeatProps);

        //Get instance ID and heartbeat interval
        final String instance = InetAddress.getLocalHost().getHostName();
        final long heartbeatIntervalMS = Long.parseLong(Config.getProperty("ngafid.heartbeat.interval.ms"));

        //Start the heartbeat
        DockerServiceHeartbeat.start(heartbeatProducer, service, instance, heartbeatIntervalMS);
        
    }
    
    /**
     * Detects the service name from the calling context
     */
    private static String detectServiceName() {
        try {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stackTrace) {
                String className = element.getClassName();
                if (className.contains("EmailConsumer")) {
                    return "ngafid-email-consumer";
                } else if (className.contains("UploadConsumer")) {
                    return "ngafid-upload-consumer";
                } else if (className.contains("EventConsumer")) {
                    return "ngafid-event-consumer";
                } else if (className.contains("EventObserver")) {
                    return "ngafid-event-observer";
                } else if (className.contains("WebServer")) {
                    return "ngafid-www";
                }
            }
        } catch (Exception e) {
            // Ignore exceptions
        }
        return SERVICE_NAME_UNKNOWN;
    }

}