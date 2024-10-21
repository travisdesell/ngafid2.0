package org.ngafid.flights.process;

import java.util.Set;
import java.util.Map;
import static java.util.Map.entry;
import java.util.List;
import java.util.HashSet;
import java.util.Collections;
import java.sql.Connection;
import java.sql.SQLException;

import org.ngafid.flights.DoubleTimeSeries;
import static org.ngafid.flights.Parameters.*;
import static org.ngafid.flights.Airframes.*;
import org.ngafid.flights.FatalFlightFileException;
import org.ngafid.flights.MalformedFlightFileException;

public class ProcessDivergence extends ProcessStep {

    private record DivergenceConfig(List<String> parameters, String output) {
    }

    private static final Set<String> AIRFRAME_BLACKLIST = Set.of(AIRFRAME_SCAN_EAGLE, AIRFRAME_DJI);

    private static final List<DivergenceConfig> CESSNA_CONFIG = List.of(
            new DivergenceConfig(List.of("E1 CHT1", "E1 CHT2", "E1 CHT3", "E1 CHT4"), "E1 CHT Divergence"),
            new DivergenceConfig(List.of("E1 EGT1", "E1 EGT2", "E1 EGT3", "E1 EGT4"), "E1 EGT Divergence"));

    private static final List<DivergenceConfig> PA_28_CONFIG = List.of(
            new DivergenceConfig(List.of("E1 EGT1", "E1 EGT2", "E1 EGT3", "E1 EGT4"), "E1 EGT Divergence"));

    private static final List<DivergenceConfig> PA_44_CONFIG = List.of(
            new DivergenceConfig(List.of("E1 EGT1", "E1 EGT2", "E1 EGT3", "E1 EGT4"), "E1 EGT Divergence"),
            new DivergenceConfig(List.of("E2 EGT1", "E2 EGT2", "E2 EGT3", "E2 EGT4"), "E2 EGT Divergence"));

    private static final List<DivergenceConfig> SIX_CYLINDER_CIRRUS = List.of(
            new DivergenceConfig(List.of("E1 CHT1", "E1 CHT2", "E1 CHT3", "E1 CHT4", "E1 CHT5", "E1 CHT6"),
                    "E1 CHT Divergence"),
            new DivergenceConfig(List.of("E1 EGT1", "E1 EGT2", "E1 EGT3", "E1 EGT4", "E1 EGT5", "E1 EGT6"),
                    "E1 EGT Divergence"));

    private static final List<DivergenceConfig> DIAMOND_CONFIG = List.of(
            new DivergenceConfig(List.of("E1 CHT1", "E1 CHT2", "E1 CHT3", "E1 CHT4"), "E1 CHT Divergence"),
            new DivergenceConfig(List.of("E1 EGT1", "E1 EGT2", "E1 EGT3", "E1 EGT4"), "E1 EGT Divergence"));

    private static final Map<String, List<DivergenceConfig>> CONFIG_MAP = Map.ofEntries(
            entry(AIRFRAME_CESSNA_172R, CESSNA_CONFIG),
            entry(AIRFRAME_CESSNA_172S, CESSNA_CONFIG),
            entry(AIRFRAME_PA_28_181, PA_28_CONFIG),
            entry(AIRFRAME_PA_44_180, PA_44_CONFIG),
            entry(AIRFRAME_CIRRUS_SR20, SIX_CYLINDER_CIRRUS),
            entry(AIRFRAME_CESSNA_T182T, SIX_CYLINDER_CIRRUS),
            entry(AIRFRAME_CESSNA_182T, SIX_CYLINDER_CIRRUS),
            entry(AIRFRAME_BEECHCRAFT_A36_G36, SIX_CYLINDER_CIRRUS),
            entry(AIRFRAME_CIRRUS_SR22, SIX_CYLINDER_CIRRUS),
            entry(AIRFRAME_CESSNA_400, SIX_CYLINDER_CIRRUS),
            entry(AIRFRAME_DIAMOND_DA_40_F, DIAMOND_CONFIG),
            entry(AIRFRAME_DIAMOND_DA_40, DIAMOND_CONFIG),
            entry(AIRFRAME_DIAMOND_DA40, DIAMOND_CONFIG));

    public ProcessDivergence(Connection connection, FlightBuilder builder) {
        super(connection, builder);
    }

    private Set<String> requiredDoubleColumns = null;

    public Set<String> getRequiredDoubleColumns() {
        if (requiredDoubleColumns == null) {

            var configs = CONFIG_MAP.get(builder.meta.airframe.getName());
            if (configs != null) {

                requiredDoubleColumns = new HashSet<>(32);
                for (var config : configs)
                    requiredDoubleColumns.addAll(config.parameters);

            } else {
                requiredDoubleColumns = Collections.emptySet();
            }
        }

        return requiredDoubleColumns;
    }

    public Set<String> getRequiredStringColumns() {
        return Collections.<String>emptySet();
    }

    public Set<String> getRequiredColumns() {
        return getRequiredDoubleColumns();
    }

    private Set<String> outputColumns = null;

    public Set<String> getOutputColumns() {
        if (outputColumns == null) {

            var configs = CONFIG_MAP.get(builder.meta.airframe.getName());
            if (configs != null) {

                outputColumns = new HashSet<>();
                for (var config : configs)
                    outputColumns.add(config.output);

            } else {
                outputColumns = Collections.emptySet();
            }
        }

        return outputColumns;
    }

    public boolean airframeIsValid(String airframe) {
        for (String blacklisted : AIRFRAME_BLACKLIST)
            if (airframe.contains(blacklisted))
                return false;

        return true;
    }

    private void calculateDivergence(List<String> columnNames, String varianceColumnName)
            throws MalformedFlightFileException, SQLException {
        DoubleTimeSeries columns[] = new DoubleTimeSeries[columnNames.size()];
        for (int i = 0; i < columns.length; i++) {
            columns[i] = builder.getDoubleTimeSeries(columnNames.get(i));

            if (columns[i] == null) {
                throw new MalformedFlightFileException("Cannot calculate '" + varianceColumnName + "' as parameter '"
                        + columnNames.get(i) + "' was missing.");
            }
        }

        DoubleTimeSeries variance = new DoubleTimeSeries(varianceColumnName, Unit.DEGREES_F, columns[0].size());

        for (int i = 0; i < columns[0].size(); i++) {
            double max = -Double.MAX_VALUE;
            double min = Double.MAX_VALUE;

            for (int j = 0; j < columns.length; j++) {
                double current = columns[j].get(i);
                if (!Double.isNaN(current) && current > max)
                    max = columns[j].get(i);
                if (!Double.isNaN(current) && current < min)
                    min = columns[j].get(i);
            }

            double v = 0;
            if (max != -Double.MAX_VALUE && min != Double.MAX_VALUE) {
                v = max - min;
            }

            variance.add(v);
        }

        builder.addTimeSeries(variance);
    }

    public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {
        List<DivergenceConfig> configs = CONFIG_MAP.get(builder.meta.airframe.getName());

        if (configs == null)
            return;

        for (var config : configs)
            calculateDivergence(config.parameters, config.output);
    }
}
