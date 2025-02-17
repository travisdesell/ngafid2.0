package org.ngafid.uploads.process.steps;

import org.ngafid.common.Database;
import org.ngafid.events.*;
import org.ngafid.flights.Airframes;
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
            ALL_EVENT_DEFS.addAll(EventDefinition.getAll(connection).stream().toList());

            for (EventDefinition def : ALL_EVENT_DEFS) {
                if (def.getFleetId() == 0) {
                    ALL_FLEET_EVENT_DEFS.add(def);
                } else {
                    FLEET_EVENT_DEFS.computeIfAbsent(def.getFleetId(), k -> new ArrayList<>()).add(def);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public static List<ComputeEvent> getAllApplicable(Connection connection, FlightBuilder fb) {
        // We will mark these event definitions as having been computed (or attempted) in the
        var applicableEvents = ALL_EVENT_DEFS.stream()
                .filter(def -> def.getFleetId() == 0 || def.getFleetId() == fb.meta.fleetId)
                .filter(def -> def.getAirframeNameId() == 0 || def.getAirframeNameId() == fb.meta.airframe.getId())
                .toList();
        return applicableEvents.stream()
                .map(def -> factory(connection, fb, def))
                .filter(Objects::nonNull)
                .toList();
    }

    private static ComputeEvent factory(Connection connection, FlightBuilder fb, EventDefinition def) {
        var scanner = scannerFactory(fb, def);
        if (scanner != null)
            fb.addComputedEvent(def);
        else
            return null;

        if (def.getId() > 0) {
            return new ComputeEvent(connection, fb, def, scanner);
        } else {
            return switch (def.getId()) {
                case -3, -2 -> new ComputeSpinEvents(connection, fb, def, scanner);
                default -> new ComputeEvent(connection, fb, def, scanner);
            };
        }
    }

    private static AbstractEventScanner scannerFactory(FlightBuilder builder, EventDefinition definition) {
        if (definition.getId() > 0) {
            return new EventScanner(definition);
        } else {
            return switch (definition.getId()) {
                case -6, -5, -4 -> new LowEndingFuelScanner(builder.meta.airframe, definition);
                case -3, -2 -> new SpinEventScanner(definition);
                // For events with either (1) no scanner or (2)
                default -> null;
            };
        }
    }

    protected final EventDefinition definition;
    private final AbstractEventScanner scanner;

    public ComputeEvent(Connection connection, FlightBuilder fb, EventDefinition def, AbstractEventScanner scanner) {
        super(connection, fb);
        definition = def;
        this.scanner = scanner;
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
    public boolean airframeIsValid(Airframes.Airframe airframe) {
        // While we could technically create events for non-fixed wing aircraft, we haven't yet!
        return builder.meta.airframe.getType().getName().equals("Fixed Wing");
    }

    @Override
    public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {
        builder.emitEvents(scanner.scan(builder.getDoubleTimeSeriesMap(), builder.getStringTimeSeriesMap()));
    }
}
