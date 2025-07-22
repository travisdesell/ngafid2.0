package org.ngafid.processor;

import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.ngafid.core.Database;
import org.ngafid.core.event.EventDefinition;
import org.ngafid.core.flights.Flight;
import org.ngafid.core.kafka.DockerServiceHeartbeat;
import org.ngafid.core.kafka.Events;
import org.ngafid.core.kafka.Topic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Scans the database for flights with missing rows from the `flight_processed` table and adds the appropriate events
 * to the event topic.
 * <p>
 * The event definitions in this program are automatically refreshed periodically, so in the event that event definitions
 * are modified in the database this program does not need to be restarted to detect it.
 */
public class EventObserver {

    private static List<Flight> getApplicableFlightsWithoutEvent(Connection connection, EventDefinition event) throws SQLException {
        StringBuilder condition = new StringBuilder();
        if (event.getFleetId() != 0) {
            condition.append(" fleet_id = ").append(event.getFleetId()).append(" ");
        }

        if (event.getAirframeNameId() != 0) {
            if (!condition.isEmpty()) condition.append("AND ");
            condition.append(" airframe_id = ").append(event.getAirframeNameId()).append(" ");
        }

        if (!condition.isEmpty()) condition.append(" AND ");
        condition.append("""
                NOT EXISTS (
                    SELECT flight_id FROM flight_processed WHERE
                        flight_processed.event_definition_id =\s""").append(event.getId()).append(""" 
                        AND flight_processed.flight_id = flights.id)
                """);

        String extraCondition = condition.toString();

        System.out.println(extraCondition);

        return Flight.getFlights(connection, extraCondition);
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws UnknownHostException {

        /* Start Docker Service Heartbeat Producer */
        DockerServiceHeartbeat.autostart();
        
        try (KafkaProducer<String, String> producer = Events.createProducer()) {

            while (true) {
                try (Connection connection = Database.getConnection()) {
                    // Grab all the events each time we scan the database to detect new events.
                    List<EventDefinition> events = EventDefinition.getAll(connection);
                    for (EventDefinition event : events) {
                        List<Flight> flights = getApplicableFlightsWithoutEvent(connection, event);
                        for (Flight flight : flights) {
                            producer.send(new ProducerRecord<>(Topic.EVENT.toString(), objectMapper.writeValueAsString(new Events.EventToCompute(flight.getId(), event.getId()))));
                            flight.insertComputedEvents(connection, List.of(event));
                        }
                    }
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
