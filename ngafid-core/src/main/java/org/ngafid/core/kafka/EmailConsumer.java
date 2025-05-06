package org.ngafid.core.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.IteratorUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.ngafid.core.Database;
import org.ngafid.core.util.SendEmail;

import java.sql.Connection;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * The email consumer monitors the email topic and sends batches of emails found in the topic. This consumer will attempt
 * to send each email at most twice, after which the emails are placed into a dead letter queue.
 */
public class EmailConsumer {
    private static final Logger LOG = Logger.getLogger(EmailConsumer.class.getName());
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static Properties getProperties() {
        Properties props = Configuration.getProperties();

        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        props.put("value.serializer", "org.ngafid.core.kafka.JsonSerializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("json.deserializer.type", SendEmail.Email.class.getName());

        return props;
    }

    public static KafkaConsumer<String, SendEmail.Email> getConsumer() {
        return new KafkaConsumer<>(getProperties());
    }

    public static KafkaProducer<String, SendEmail.Email> getProducer() {
        return new KafkaProducer<>(getProperties());
    }

    public static void main(String[] args) {
        Properties props = Configuration.getProperties();

        while (true) {
            try {
                emailConsumer(props);
            } catch (Exception e) {
                LOG.severe("Error in fleet consumer");
                LOG.severe(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void emailConsumer(Properties props) {
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
             KafkaProducer<String, SendEmail.Email> producer = new KafkaProducer<>(props)) {
            List<String> topics = List.of(Topic.EMAIL.toString(), Topic.EMAIL_RETRY.toString());
            consumer.subscribe(topics);

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(10000));
                for (String topic : topics) {
                    List<ConsumerRecord<String, String>> emails = IteratorUtils.<ConsumerRecord<String, String>>toList(records.records(topic).iterator());
                    List<SendEmail.Email> converted = emails.stream().map(ConsumerRecord::value).map(x -> {
                        try {
                            return objectMapper.readValue(x, SendEmail.Email.class);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }).toList();

                    try (Connection connection = Database.getConnection()) {
                        SendEmail.sendBatchEmail(converted, connection);
                    } catch (Exception e) {
                        LOG.info("Encountered error in upload consumer: " + e.getMessage());
                        e.printStackTrace();
                        String targetTopic = topic.equals(Topic.EMAIL.toString()) ? Topic.EMAIL_RETRY.toString() : Topic.EMAIL_DLQ.toString();
                        converted.forEach(email -> producer.send(new ProducerRecord<>(targetTopic, email)));
                    }
                }

                consumer.commitSync();
            }
        }
    }
}
