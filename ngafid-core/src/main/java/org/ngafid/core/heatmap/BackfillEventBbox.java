package org.ngafid.core.heatmap;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.ngafid.core.Database;
import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.Parameters;

/**
 * Backfills min_latitude, max_latitude, min_longitude, max_longitude on the events table
 * for existing events that have NULL bbox. Uses each event's flight_id and start_line/end_line
 * to compute the bounding box from the flight's Latitude/Longitude time series.
 * <p>
 * Run from command line to backfill so older events show on the heatmap (proximity_events_in_box).
 */
public final class BackfillEventBbox {
    private static final Logger LOG = Logger.getLogger(BackfillEventBbox.class.getName());

    private static final String SELECT_EVENTS_NEEDING_BBOX =
            "SELECT id, flight_id, start_line, end_line FROM events "
                    + "WHERE min_latitude IS NULL AND start_line IS NOT NULL AND end_line IS NOT NULL "
                    + "ORDER BY id";

    private static final String UPDATE_EVENT_BBOX =
            "UPDATE events SET min_latitude = ?, max_latitude = ?, min_longitude = ?, max_longitude = ? WHERE id = ?";

    private BackfillEventBbox() {}

    public static void main(String[] args) {
        int batchSize = 500;
        Integer limit = null;
        for (int i = 0; i < args.length; i++) {
            if ("--batch".equals(args[i]) && i + 1 < args.length) {
                batchSize = Integer.parseInt(args[i + 1]);
                i++;
            } else if ("--limit".equals(args[i]) && i + 1 < args.length) {
                limit = Integer.parseInt(args[i + 1]);
                i++;
            }
        }

        System.out.println("Backfilling event bbox columns (min/max lat/lon) for events with NULL bbox...");
        try (Connection connection = Database.getConnection()) {
            BackfillResult result = backfill(connection, batchSize, limit);
            System.out.println("Done. Updated=" + result.updated
                    + " skipped_no_series=" + result.skippedNoSeries
                    + " skipped_no_valid_coords=" + result.skippedNoValidCoords
                    + " errors=" + result.errors);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static final class EventRow {
        final int id;
        final int flightId;
        final int startLine;
        final int endLine;

        EventRow(int id, int flightId, int startLine, int endLine) {
            this.id = id;
            this.flightId = flightId;
            this.startLine = startLine;
            this.endLine = endLine;
        }
    }

    static final class BackfillResult {
        int updated;
        int skippedNoSeries;
        int skippedNoValidCoords;
        int errors;
    }

    public static BackfillResult backfill(Connection connection, int batchSize, Integer limit) throws SQLException {
        BackfillResult result = new BackfillResult();
        List<EventRow> rows = new ArrayList<>();
        try (PreparedStatement select = connection.prepareStatement(SELECT_EVENTS_NEEDING_BBOX)) {
            try (ResultSet rs = select.executeQuery()) {
                while (rs.next()) {
                    rows.add(new EventRow(
                            rs.getInt("id"),
                            rs.getInt("flight_id"),
                            rs.getInt("start_line"),
                            rs.getInt("end_line")));
                    if (limit != null && rows.size() >= limit) {
                        break;
                    }
                }
            }
        }

        if (rows.isEmpty()) {
            System.out.println("No events with NULL bbox found.");
            return result;
        }
        System.out.println("Found " + rows.size() + " events to backfill.");

        boolean priorAutoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            // ignore if not supported
        }
        try (PreparedStatement update = connection.prepareStatement(UPDATE_EVENT_BBOX)) {
            for (EventRow row : rows) {
                BboxResult bboxResult = computeBbox(connection, row);
                if (bboxResult.skipReason == SkipReason.NO_SERIES) {
                    result.skippedNoSeries++;
                    continue;
                }
                if (bboxResult.skipReason == SkipReason.NO_VALID_COORDS || bboxResult.bbox == null) {
                    result.skippedNoValidCoords++;
                    continue;
                }
                double[] bbox = bboxResult.bbox;
                double minLat = bbox[0], maxLat = bbox[1], minLon = bbox[2], maxLon = bbox[3];
                try {
                    update.setDouble(1, minLat);
                    update.setDouble(2, maxLat);
                    update.setDouble(3, minLon);
                    update.setDouble(4, maxLon);
                    update.setInt(5, row.id);
                    int n = update.executeUpdate();
                    if (n > 0) {
                        result.updated++;
                    }
                } catch (SQLException e) {
                    LOG.warning("Failed to update event " + row.id + ": " + e.getMessage());
                    result.errors++;
                }
                if ((result.updated + result.skippedNoSeries + result.skippedNoValidCoords + result.errors) % batchSize == 0) {
                    connection.commit();
                    System.out.println("Updated " + result.updated + ", skipped " + (result.skippedNoSeries + result.skippedNoValidCoords) + ", errors " + result.errors + " so far.");
                }
            }
            connection.commit();
        } finally {
            try {
                connection.setAutoCommit(priorAutoCommit);
            } catch (SQLException e) {
                LOG.warning("Restore autoCommit: " + e.getMessage());
            }
        }
        return result;
    }

    enum SkipReason { NONE, NO_SERIES, NO_VALID_COORDS }

    static final class BboxResult {
        final double[] bbox;
        final SkipReason skipReason;

        BboxResult(double[] bbox) {
            this.bbox = bbox;
            this.skipReason = SkipReason.NONE;
        }

        BboxResult(SkipReason skipReason) {
            this.bbox = null;
            this.skipReason = skipReason;
        }
    }

    /**
     * Returns bbox { minLat, maxLat, minLon, maxLon } or a skip reason.
     */
    private static BboxResult computeBbox(Connection connection, EventRow row) throws SQLException {
        DoubleTimeSeries latSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, row.flightId, Parameters.LATITUDE);
        DoubleTimeSeries lonSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, row.flightId, Parameters.LONGITUDE);
        if (latSeries == null || lonSeries == null) {
            return new BboxResult(SkipReason.NO_SERIES);
        }
        int start = Math.max(0, row.startLine);
        int end = row.endLine;
        int latSize = latSeries.size();
        int lonSize = lonSeries.size();
        if (start > end || end >= latSize || end >= lonSize) {
            return new BboxResult(SkipReason.NO_VALID_COORDS);
        }

        double minLat = Double.POSITIVE_INFINITY;
        double maxLat = Double.NEGATIVE_INFINITY;
        double minLon = Double.POSITIVE_INFINITY;
        double maxLon = Double.NEGATIVE_INFINITY;
        for (int i = start; i <= end && i < latSize && i < lonSize; i++) {
            double lat = latSeries.get(i);
            double lon = lonSeries.get(i);
            if (lat != 0.0 && lon != 0.0 && !Double.isNaN(lat) && !Double.isNaN(lon)) {
                if (lat < minLat) minLat = lat;
                if (lat > maxLat) maxLat = lat;
                if (lon < minLon) minLon = lon;
                if (lon > maxLon) maxLon = lon;
            }
        }
        if (minLat == Double.POSITIVE_INFINITY || maxLat == Double.NEGATIVE_INFINITY
                || minLon == Double.POSITIVE_INFINITY || maxLon == Double.NEGATIVE_INFINITY) {
            return new BboxResult(SkipReason.NO_VALID_COORDS);
        }
        return new BboxResult(new double[] { minLat, maxLat, minLon, maxLon });
    }
}
