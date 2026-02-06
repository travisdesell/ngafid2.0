package org.ngafid.core.kafka;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DeleteTopicsOptions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.KafkaConsumer;

/**
 * Enumeration of the kafka topics and their corresponding names.
 * <p>
 * This enumeration also contains a main function which can be used to create these topics.
 */
public enum Topic {
    UPLOAD("upload"),
    UPLOAD_RETRY("upload-retry"),
    UPLOAD_DLQ("upload-dlq"),

    EMAIL("email"),
    EMAIL_RETRY("email-retry"),
    EMAIL_DLQ("email-dlq"),

    EVENT("event"),
    EVENT_RETRY("event-retry"),
    EVENT_DLQ("event-dlq"),

    STATUS_HEARTBEAT("docker-status-heartbeat")
    ;

    private final String name;

    Topic(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public <K, V> void subscribeWith(KafkaConsumer<K, V> consumer) {
        consumer.subscribe(List.of(this.toString()));
    }

    public static void main(String[] args) throws Exception {
        try (AdminClient adminClient = AdminClient.create(Configuration.getProperties())) {
            if (args.length != 0 && args[0].equals("drain")) {
                List<String> toDelete = new ArrayList<>();
                for (int i = 1; i < args.length; i++) {
                    System.out.println("Will delete topic '" + args[i] + "'");
                    toDelete.add(args[i]);
                }

                adminClient.deleteTopics(toDelete, new DeleteTopicsOptions());
                System.out.println("Jobs done");
            }

            // Always create the topics
            List<NewTopic> newTopics = Arrays.stream(Topic.values())
                    .map(topic -> new NewTopic(topic.toString(), 6, (short) 1))
                    .toList();
            adminClient.createTopics(newTopics);
        }
    }
}
