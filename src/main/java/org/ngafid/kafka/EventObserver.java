package org.ngafid.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.ngafid.common.Database;
import org.ngafid.events.EventDefinition;
import org.ngafid.flights.Flight;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

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

    public static void main(String[] args) {
        try (KafkaProducer<String, Event.EventToCompute> producer = Event.createProducer()) {

            while (true) {
                try (Connection connection = Database.getConnection()) {
                    // Grab all the events each time we scan the database to detect new events.
                    List<EventDefinition> events = EventDefinition.getAll(connection);
                    for (EventDefinition event : events) {
                        List<Flight> flights = getApplicableFlightsWithoutEvent(connection, event);
                        for (Flight flight : flights) {
                            producer.send(new ProducerRecord<>(Topic.EVENT.toString(), new Event.EventToCompute(flight.getId(), event.getId())));
                            flight.insertComputedEvents(connection, List.of(event));
                        }
                    }
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
