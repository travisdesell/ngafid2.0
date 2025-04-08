package org.ngafid.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.jline.utils.Log;
import org.ngafid.common.ColumnNotAvailableException;
import org.ngafid.common.Database;
import org.ngafid.common.filters.Pair;
import org.ngafid.events.*;
import org.ngafid.events.proximity.ProximityEventScanner;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Flight;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

public class EventConsumer extends DisjointConsumer<String, String> {

    private static final Logger LOG = Logger.getLogger(EventConsumer.class.getName());

    public static void main(String[] args) {
        KafkaConsumer<String, String> consumer = Events.createConsumer();
        KafkaProducer<String, String> producer = Events.createProducer();

        new EventConsumer(Thread.currentThread(), consumer, producer).run();
    }

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Map<Integer, EventDefinition> eventDefinitionMap;

    protected EventConsumer(Thread mainThread, KafkaConsumer<String, String> consumer, KafkaProducer<String, String> producer) {
        super(mainThread, consumer, producer);
    }

    @Override
    protected void preProcess(ConsumerRecords<String, String> records) {
        try {
            eventDefinitionMap = getEventDefinitionMap();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static AbstractEventScanner getScanner(Flight flight, EventDefinition def) {

        if (def.getId() > 0) {
            LOG.info("GET Scanner GENERIC Event SCANNer.Airframe id: " + def.getId());
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


    @Override
    protected Pair<ConsumerRecord<String, String>, Boolean> process(ConsumerRecord<String, String> record) {
        Events.EventToCompute etc;

        try {
            etc = objectMapper.readValue(record.value(), Events.EventToCompute.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        try (Connection connection = Database.getConnection()) {
            Flight flight = Flight.getFlight(connection, etc.flightId());
            if (flight == null) {
                LOG.info("Cannot compute event with definition id " + etc.eventId() + " for flight " + etc.flightId() + " because the flight does not exist in the database. Assuming this was a stale request");
                return new Pair<>(record, false);
            }

// Logging block for debugging flight content
            Log.info("Event Consumer.");
            Log.info("Event Consumer.Preparing to compute events for flight: " + flight.getFilename());
            Log.info("Event Consumer. Flight ID: " + flight.getId());
            Log.info("Event Consumer. Number of rows: " + flight.getNumberRows());
            Log.info("Event Consumer. DoubleTimeSeries keys: " + flight.getDoubleTimeSeriesMap().keySet());
            Log.info("Event Consumer. StringTimeSeries keys: " + flight.getStringTimeSeriesMap().keySet());

            int middleIndex = flight.getNumberRows() / 2;
            Log.info("Event Consumer. Middle index for sample values: " + middleIndex);

// Log middle values from each DoubleTimeSeries
            for (Map.Entry<String, DoubleTimeSeries> entry : flight.getDoubleTimeSeriesMap().entrySet()) {
                String name = entry.getKey();
                DoubleTimeSeries series = entry.getValue();

                double valueAtMiddle = Double.NaN;
                if (series.size() > middleIndex) {
                    valueAtMiddle = series.get(middleIndex);
                }

                Log.info("DoubleTimeSeries [" + name + "] value at middle index (" + middleIndex + "): " + valueAtMiddle);
            }


            EventDefinition def = eventDefinitionMap.get(etc.eventId());
            if (def == null) {
                LOG.info("Cannot compute event with definition id " + etc.eventId() + " for flight " + etc.flightId() + " because there is no event with that definition in the database.");
                return new Pair<>(record, false);
            }

            try {
                clearExistingEvents(connection, flight, eventDefinitionMap.get(etc.eventId()));
                AbstractEventScanner scanner = getScanner(flight, eventDefinitionMap.get(etc.eventId()));
                Log.info("!!! Before gather required columns");
                scanner.gatherRequiredColumns(connection, flight);
                Log.info("!!! After gather required columns");



                // Scanners may emit events of more than one type -- filter the other events out.
                List<org.ngafid.events.Event> events = scanner
                        .scan(flight.getDoubleTimeSeriesMap(), flight.getStringTimeSeriesMap())
                        .stream()
                        .filter(e -> e.getEventDefinitionId() == etc.eventId())
                        .toList();

                org.ngafid.events.Event.batchInsertion(connection, flight, events);

                // Computed okay.
                return new Pair<>(record, true);
            } catch (ColumnNotAvailableException e) {
                // Some other exception happened...
                e.printStackTrace();
                LOG.info("A required column was not available so the event could not be computed: " + e.getMessage());
                return new Pair<>(record, false);
            } catch (Exception e) {
                e.printStackTrace();
                // Retry
                return new Pair<>(record, false);
            }
        } catch (SQLException e) {
            // If a sql exception happens, there is likely a bug that needs to addressed or the process should be rebooted. Crash process.
            e.printStackTrace();
            done.set(true);
            mainThread.interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String getTopicName() {
        return Topic.EVENT.toString();
    }

    @Override
    protected String getRetryTopicName() {
        return Topic.EVENT_RETRY.toString();
    }

    @Override
    protected String getDLTTopicName() {
        return Topic.EVENT_DLQ.toString();
    }

    @Override
    protected long getMaxPollIntervalMS() {
        return Events.MAX_POLL_INTERVAL_MS;
    }

}
