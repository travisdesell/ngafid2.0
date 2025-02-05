package org.ngafid.uploads.process.steps;

import org.ngafid.bin.CalculateExceedences;
import org.ngafid.common.Database;
import org.ngafid.events.EventDefinition;
import org.ngafid.flights.Parameters;
import org.ngafid.uploads.process.FatalFlightFileException;
import org.ngafid.uploads.process.MalformedFlightFileException;
import org.ngafid.uploads.process.format.FlightBuilder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

public class ComputeEvent extends ComputeStep {
    private static final Logger LOG = Logger.getLogger(ComputeEvent.class.getName());
    private static final List<EventDefinition> ALL_EVENT_DEFS = new ArrayList<>();
    private static final Map<Integer, List<EventDefinition>> FLEET_EVENT_DEFS = new HashMap<>();
    private static final List<EventDefinition> ALL_FLEET_EVENT_DEFS = new ArrayList<>();

    static {
        try (Connection connection = Database.getConnection()) {
            ALL_EVENT_DEFS.addAll(EventDefinition.getAll(connection).stream().filter(e -> e.getId() > 0).toList());

            for (EventDefinition defn : ALL_EVENT_DEFS) {
                if (defn.getFleetId() == 0) {
                    ALL_FLEET_EVENT_DEFS.add(defn);
                } else {
                    FLEET_EVENT_DEFS.computeIfAbsent(defn.getFleetId(), k -> new ArrayList<>()).add(defn);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public static List<ComputeEvent> getAllApplicable(Connection connection, FlightBuilder fb) {
        // We will mark these event definitions as having been computed (or attempted) in the
        ALL_EVENT_DEFS.forEach(fb::addComputedEvent);
        return ALL_EVENT_DEFS.stream()
                .filter(def -> def.getFleetId() == 0 || def.getFleetId() == fb.meta.fleetId)
                .filter(def -> def.getAirframeNameId() == 0 || def.getAirframeNameId() == fb.meta.airframe.getId())
                .map(def -> new ComputeEvent(connection, fb, def))
                .toList();
    }

    private final EventDefinition definition;

    public ComputeEvent(Connection connection, FlightBuilder fb, EventDefinition def) {
        super(connection, fb);
        definition = def;
    }

    @Override
    public Set<String> getRequiredDoubleColumns() {
        var cols = new HashSet<>(definition.getColumnNames());
        return cols;
    }

    private static final Set<String> REQUIRED_STRING_COLUMNS = Set.of(Parameters.LCL_DATE, Parameters.LCL_TIME, Parameters.UTC_OFFSET);

    @Override
    public Set<String> getRequiredStringColumns() {
        return REQUIRED_STRING_COLUMNS;
    }

    @Override
    public Set<String> getRequiredColumns() {
        Set<String> doubleCols = new HashSet<>(getRequiredDoubleColumns());
        doubleCols.addAll(getRequiredStringColumns());
        return doubleCols;
    }

    @Override
    public Set<String> getOutputColumns() {
        return Set.of();
    }

    @Override
    public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {
        LOG.info("COMPUTING EVENT: " + definition.getName() + " / " + definition.getId());
        builder.emitEvents(new CalculateExceedences(definition).searchForEvent(builder.getDoubleTimeSeriesMap(), builder.getStringTimeSeries(Parameters.LCL_DATE), builder.getStringTimeSeries(Parameters.LCL_TIME)));
        builder.addComputedEvent(definition);
    }
}
