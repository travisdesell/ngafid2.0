package org.ngafid.processor.steps;

import org.ngafid.core.Database;
import org.ngafid.core.event.EventDefinition;
import org.ngafid.core.flights.Airframes;
import org.ngafid.core.flights.FatalFlightFileException;
import org.ngafid.core.flights.MalformedFlightFileException;
import org.ngafid.core.flights.Parameters;
import org.ngafid.processor.events.AbstractEventScanner;
import org.ngafid.processor.events.EventScanner;
import org.ngafid.processor.events.LowEndingFuelScanner;
import org.ngafid.processor.events.SpinEventScanner;
import org.ngafid.processor.format.FlightBuilder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

/**
 * An instance of this class is used to scan a flight for an event, and is basically a wrapper on top of {@link EventScanner}.
 * <p>
 * When adding a custom event, you may have to modify the private factory methods contained within this class to properly
 * compute the event upon ingestion.
 */
public class ComputeEvent extends ComputeStep {
    private static final Logger LOG = Logger.getLogger(ComputeEvent.class.getName());

    /**
     * All event definitions contained within the database, populated within a static initialization block.
     */
    private static final List<EventDefinition> ALL_EVENT_DEFS = new ArrayList<>();

    /**
     * Maps fleet id to event definitions that are particular to that fleet.
     * Should not be written to after static initialization.
     */
    private static final Map<Integer, List<EventDefinition>> FLEET_EVENT_DEFS = new HashMap<>();

    /**
     * Event definitions for use over every fleet.
     */
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

    /**
     * Returns a list of ComputeEvent objects where the underlying event definitions are applicable to the supplied flight builder,
     * i.e. all the required columns are available.
     * <p>
     * Some additional logic is required specifically for custom events, as custom events use subclasses of ComputeEvent
     * to perform custom compuations.
     *
     * @param connection database connection
     * @param fb         flight builder
     * @return list of ComputeEvent objects that can be properly computed for the supplied flight builder.
     */
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

    /**
     * Creates ComputeEvent object for the supplied event definition, if possible. Some events cannot be computed before
     * the flight is inserted into the database (namely proximity events), and this function will return null in that case.
     *
     * @param connection
     * @param fb
     * @param def
     * @return a ComputeEvent object if successful, or null if the event cannot be computed during initial ingestion.
     */
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

    /**
     * Creates an event scanner (see {@link AbstractEventScanner} and {@link EventScanner})
     * to search for an event with the supplied event definition.
     * <p>
     * Normal events use a common event scanner, whereas custom events must use custom event scanners.
     *
     * @param builder
     * @param definition
     * @return
     */
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

    private final HashSet<String> requiredDoubleColumns = new HashSet<>();

    public ComputeEvent(Connection connection, FlightBuilder fb, EventDefinition def, AbstractEventScanner scanner) {
        super(connection, fb);
        definition = def;
        this.scanner = scanner;

        requiredDoubleColumns.addAll(definition.getColumnNames());
        requiredDoubleColumns.add(Parameters.UNIX_TIME_SECONDS);
    }

    @Override
    public Set<String> getRequiredDoubleColumns() {
        return requiredDoubleColumns;
    }

    private static final Set<String> REQUIRED_STRING_COLUMNS = Set.of(Parameters.UTC_DATE_TIME);

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

    /**
     * Simply applies the event scanner to the supplied time series.
     *
     * @throws SQLException
     * @throws MalformedFlightFileException
     * @throws FatalFlightFileException
     */
    @Override
    public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {
        builder.emitEvents(scanner.scan(builder.getDoubleTimeSeriesMap(), builder.getStringTimeSeriesMap()));
    }
}
