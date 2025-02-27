package org.ngafid.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.ngafid.common.ColumnNotAvailableException;
import org.ngafid.common.Database;
import org.ngafid.events.*;
import org.ngafid.events.proximity.ProximityEventScanner;
import org.ngafid.flights.Flight;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class EventConsumer {

    private static final Logger LOG = Logger.getLogger(EventConsumer.class.getName());

    private static AbstractEventScanner getScanner(Flight flight, EventDefinition def) {
        if (def.getId() > 0) {
            return new EventScanner(def);
        } else {
            return switch (def.getId()) {
                case -6, -5, -4 -> new LowEndingFuelScanner(flight.getAirframe(), def);
                case -3, -2 -> new SpinEventScanner(def);
                case -1 -> new ProximityEventScanner(flight, def);
                default ->
                        throw new RuntimeException("Cannot create scanner for event with definition id " + def.getId() + ". Please manually update `org.ngafid.kafka.EventConsumer with the mapping to the scanner.");
            };
        }
    }

    private static void clearExistingEvents(Connection connection, Flight flight, EventDefinition def) throws SQLException {
        org.ngafid.events.Event.deleteEvents(connection, flight.getId(), def.getId());
    }

    private static Map<Integer, EventDefinition> getEventDefinitionMap() throws SQLException {
        try (Connection connection = Database.getConnection()) {
            List<EventDefinition> defs = EventDefinition.getAll(connection);
            HashMap<Integer, EventDefinition> map = new HashMap<>(defs.size() * 2);
            defs.forEach(def -> map.put(def.getId(), def));
            return map;
        }
    }

    public static void main(String[] args) {
        final ObjectMapper objectMapper = new ObjectMapper();
        try (KafkaConsumer<String, String> consumer = Events.createConsumer();
             KafkaProducer<String, Events.EventToCompute> producer = Events.createProducer()) {

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                LOG.info("Got " + records.count() + " records for consumer.");
                Map<Integer, EventDefinition> eventDefinitionMap = getEventDefinitionMap();
                for (ConsumerRecord<String, String> record : records) {
                    Events.EventToCompute etc = objectMapper.readValue(record.value(), Events.EventToCompute.class);

                    try (Connection connection = Database.getConnection()) {
                        Flight flight = Flight.getFlight(connection, etc.flightId());
                        if (flight == null) {
                            LOG.info("Cannot compute event with definition id " + etc.eventId() + " for flight " + etc.flightId() + " because the flight does not exist in the database. Assuming this was a stale request");
                            continue;
                        }

                        EventDefinition def = eventDefinitionMap.get(etc.eventId());
                        if (def == null) {
                            LOG.info("Cannot compute event with definition id " + etc.eventId() + " for flight " + etc.flightId() + " because there is no event with that definition in the database.");
                            continue;
                        }

                        try {
                            clearExistingEvents(connection, flight, eventDefinitionMap.get(etc.eventId()));
                            AbstractEventScanner scanner = getScanner(flight, eventDefinitionMap.get(etc.eventId()));
                            scanner.gatherRequiredColumns(connection, flight);

                            // Scanners may emit events of more than one type -- filter the other events out.
                            List<org.ngafid.events.Event> events = scanner
                                    .scan(flight.getDoubleTimeSeriesMap(), flight.getStringTimeSeriesMap())
                                    .stream()
                                    .filter(e -> e.getEventDefinitionId() == etc.eventId())
                                    .toList();

                            org.ngafid.events.Event.batchInsertion(connection, flight, events);
                        } catch (SQLException e) {
                            // If a sql exception happens, there is likely a bug that needs to addressed or the process should be rebooted. Crash process.
                            e.printStackTrace();
                            throw e;
                        } catch (ColumnNotAvailableException e) {
                            // Some other exception happened...
                            e.printStackTrace();
                            LOG.info("A required column was not available so the event could not be computed: " + e.getMessage());
                        } catch (Exception e) {
                            // Something else happened, retry?
                            e.printStackTrace();
                            if (record.topic().equals(Topic.EVENT.toString()))
                                producer.send(new ProducerRecord<>(Topic.EVENT_RETRY.toString(), etc));
                        }
                    }
                }
                consumer.commitSync();
            }
        } catch (JsonProcessingException | SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
