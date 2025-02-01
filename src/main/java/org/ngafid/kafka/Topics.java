package org.ngafid.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;

import java.util.Arrays;
import java.util.List;

public class Topics {

    private enum Topic {
        UPLOAD("upload"),
        UPLOAD_RETRY("upload-retry"),
        UPLOAD_DLQ("upload-dlq"),
        EMAIL("email"),
        EMAIL_RETRY("email-retry"),
        EMAIL_DLQ("email-dlq");

        private final String name;

        Topic(String name) {
            this.name = name;
        }

        public String toString() {
            return this.name;
        }
    }

    public static void main(String[] args) throws Exception {
        try (AdminClient adminClient = AdminClient.create(Configuration.getProperties())) {
            List<NewTopic> newTopics = Arrays.stream(Topic.values())
                    .map(topic -> new NewTopic(topic.toString(), 1, (short) 1))
                    .toList();
            adminClient.createTopics(newTopics);
        }
    }
}
