package org.ngafid.core.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.ngafid.core.Database;
import org.ngafid.core.util.SendEmail;
import org.ngafid.core.util.filters.Pair;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * The email consumer monitors the email topic and sends batches of emails found in the topic. This consumer will attempt
 * to send each email at most twice, after which the emails are placed into a dead letter queue.
 */
public class EmailConsumer extends DisjointConsumer<String, String> {
    private static final Logger LOG = Logger.getLogger(EmailConsumer.class.getName());
    private static final ObjectMapper objectMapper = new ObjectMapper();

    protected EmailConsumer(Thread mainThread, KafkaConsumer<String, String> consumer, KafkaProducer<String, String> producer) {
        super(mainThread, consumer, producer);
    }

    private static Properties getProperties() {
        Properties props = Configuration.getProperties();

        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        return props;
    }

    public static KafkaConsumer<String, String> getConsumer() {
        return new KafkaConsumer<>(getProperties());
    }

    public static KafkaProducer<String, String> getProducer() {
        return new KafkaProducer<>(getProperties());
    }

    public static void main(String[] args) {
        new EmailConsumer(Thread.currentThread(), getConsumer(), getProducer()).run();
    }

    @Override
    protected String getTopicName() {
        return Topic.EMAIL.toString();
    }

    @Override
    protected String getRetryTopicName() {
        return Topic.EMAIL_RETRY.toString();
    }

    @Override
    protected String getDLTTopicName() {
        return Topic.EMAIL_DLQ.toString();
    }

    @Override
    protected long getMaxPollIntervalMS() {
        return 10 * 60 * 1000;
    }

    @Override
    protected Pair<ConsumerRecord<String, String>, Boolean> process(ConsumerRecord<String, String> record) {
        SendEmail.Email email;

        try {
            email = objectMapper.readValue(record.value(), SendEmail.Email.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return new Pair<>(record, false);
        }

        try (Connection connection = Database.getConnection()) {
            SendEmail.sendBatchEmail(List.of(email), connection);
            return new Pair<>(record, true);
        } catch (SQLException e) {
            LOG.info("Encountered error in upload consumer: " + e.getMessage());
            e.printStackTrace();
            return new Pair<>(record, false);
        }
    }
}
