package org.ngafid.core.bin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngafid.core.Database;
import org.ngafid.core.flights.Flight;
import org.ngafid.core.flights.TurnToFinal;

/**
 * Backfills the turn_to_final cache by deleting outdated entries and recomputing TTF data.
 * Run after deployments that change TurnToFinal.serialVersionUID.
 * <p>
 * Run from repo root: ./run/backfill/backfill_ttf [--batch N] [--limit N] [--dry-run]
 * Or: ./run/backfill/backfill_ttf --update-version-only  (fast: just UPDATE version, no recompute)
 */
public final class BackfillTTFCache {
    private static final Logger LOG = Logger.getLogger(BackfillTTFCache.class.getName());

    private BackfillTTFCache() {}

    public static void main(String[] args) {
        int batchSize = 100;
        Integer limit = null;
        boolean dryRun = false;
        boolean updateVersionOnly = false;
        for (int i = 0; i < args.length; i++) {
            if ("--batch".equals(args[i]) && i + 1 < args.length) {
                batchSize = Integer.parseInt(args[i + 1]);
                i++;
            } else if ("--limit".equals(args[i]) && i + 1 < args.length) {
                limit = Integer.parseInt(args[i + 1]);
                i++;
            } else if ("--dry-run".equals(args[i])) {
                dryRun = true;
            } else if ("--update-version-only".equals(args[i])) {
                updateVersionOnly = true;
            }
        }

        System.out.println("TurnToFinal.serialVersionUID = " + TurnToFinal.serialVersionUID);
        Logger.getLogger("org.ngafid.core.flights.TurnToFinal").setLevel(Level.WARNING);
        try (Connection connection = Database.getConnection()) {
            if (updateVersionOnly) {
                int updated = updateVersionOnly(connection);
                System.out.println("Done. Updated version for " + updated + " rows.");
            } else {
                System.out.println("Backfilling TTF cache (delete outdated, recompute)...");
                if (dryRun) {
                    System.out.println("DRY RUN - no changes will be made");
                }
                BackfillResult result = backfill(connection, batchSize, limit, dryRun);
                System.out.println("Done. recomputed=" + result.recomputed
                        + " skipped=" + result.skipped
                        + " errors=" + result.errors);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Fast path: UPDATE version column only. Use when only serialVersionUID changed, not the class structure.
     * If deserialization fails on TTF page after this, run full backfill.
     */
    private static int updateVersionOnly(Connection connection) throws SQLException {
        long currentVersion = TurnToFinal.serialVersionUID;
        System.out.println("Updating version to " + currentVersion + " for all rows...");
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE turn_to_final SET version = ? WHERE version != ?")) {
            ps.setLong(1, currentVersion);
            ps.setLong(2, currentVersion);
            int updated = ps.executeUpdate();
            return updated;
        }
    }

    static final class BackfillResult {
        int recomputed;
        int skipped;
        int errors;
    }

    public static BackfillResult backfill(Connection connection, int batchSize, Integer limit, boolean dryRun)
            throws SQLException, IOException, ClassNotFoundException {
        BackfillResult result = new BackfillResult();

        // 1. Get flight_ids to process. Prefer turn_to_final (outdated cache); fallback to itinerary (empty table).
        List<Integer> flightIds = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT flight_id FROM turn_to_final")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    flightIds.add(rs.getInt("flight_id"));
                    if (limit != null && flightIds.size() >= limit) {
                        break;
                    }
                }
            }
        }
        if (flightIds.isEmpty()) {
            System.out.println("turn_to_final is empty. Using flights from itinerary (flights with approach data).");
            String sql = "SELECT DISTINCT flight_id FROM itinerary ORDER BY flight_id";
            if (limit != null) {
                sql += " LIMIT " + limit;
            }
            try (PreparedStatement ps = connection.prepareStatement(sql);
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    flightIds.add(rs.getInt("flight_id"));
                }
            }
        }
        if (flightIds.isEmpty()) {
            System.out.println("No flights to process.");
            return result;
        }
        System.out.println("Found " + flightIds.size() + " flights to process.");

        if (!dryRun) {
            // 2. Delete from turn_to_final. Only TRUNCATE on full run (no limit).
            int toDelete = limit != null ? Math.min(flightIds.size(), limit) : flightIds.size();
            if (limit == null) {
                try (PreparedStatement ps = connection.prepareStatement("TRUNCATE TABLE turn_to_final")) {
                    ps.executeUpdate();
                    System.out.println("Truncated turn_to_final");
                }
            } else {
                for (int i = 0; i < toDelete; i++) {
                    try (PreparedStatement ps = connection.prepareStatement("DELETE FROM turn_to_final WHERE flight_id = ?")) {
                        ps.setInt(1, flightIds.get(i));
                        ps.executeUpdate();
                    }
                }
                System.out.println("Deleted " + toDelete + " rows from turn_to_final");
            }
        }

        // 3. Recompute and cache for each flight
        long start = System.currentTimeMillis();
        for (int i = 0; i < flightIds.size(); i++) {
            int flightId = flightIds.get(i);
            try {
                Flight flight = Flight.getFlight(connection, flightId);
                if (flight == null) {
                    result.skipped++;
                    continue;
                }
                if (dryRun) {
                    result.recomputed++;
                } else {
                    var ttfs = TurnToFinal.getTurnToFinal(connection, flight, null);
                    if (ttfs != null && !ttfs.isEmpty()) {
                        result.recomputed++;
                    } else {
                        result.skipped++;
                    }
                }
            } catch (Exception e) {
                LOG.warning("Failed flight " + flightId + ": " + e.getMessage());
                result.errors++;
            }
            int n = i + 1;
            boolean shouldPrint = n <= 10 || n % 100 == 0;
            if (shouldPrint) {
                long elapsed = (System.currentTimeMillis() - start) / 1000;
                System.out.println("flight_id=" + flightId + " " + n + "/" + flightIds.size()
                        + " recomputed=" + result.recomputed + " skipped=" + result.skipped
                        + " errors=" + result.errors + " (" + elapsed + "s elapsed)");
            }
        }
        return result;
    }
}
