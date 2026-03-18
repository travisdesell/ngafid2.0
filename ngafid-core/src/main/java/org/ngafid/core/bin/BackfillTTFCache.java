package org.ngafid.core.bin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.ngafid.core.Database;
import org.ngafid.core.flights.Flight;
import org.ngafid.core.flights.TurnToFinal;

/**
 * Backfills the turn_to_final cache by deleting outdated entries and recomputing TTF data.
 * Run after deployments that change TurnToFinal.serialVersionUID.
 * <p>
 * Run from repo root: ./run/backfill/backfill_ttf [--batch N] [--limit N] [--dry-run]
 */
public final class BackfillTTFCache {
    private static final Logger LOG = Logger.getLogger(BackfillTTFCache.class.getName());

    private BackfillTTFCache() {}

    public static void main(String[] args) {
        int batchSize = 100;
        Integer limit = null;
        boolean dryRun = false;
        for (int i = 0; i < args.length; i++) {
            if ("--batch".equals(args[i]) && i + 1 < args.length) {
                batchSize = Integer.parseInt(args[i + 1]);
                i++;
            } else if ("--limit".equals(args[i]) && i + 1 < args.length) {
                limit = Integer.parseInt(args[i + 1]);
                i++;
            } else if ("--dry-run".equals(args[i])) {
                dryRun = true;
            }
        }

        System.out.println("Backfilling TTF cache (delete outdated, recompute)...");
        if (dryRun) {
            System.out.println("DRY RUN - no changes will be made");
        }
        try (Connection connection = Database.getConnection()) {
            BackfillResult result = backfill(connection, batchSize, limit, dryRun);
            System.out.println("Done. recomputed=" + result.recomputed
                    + " skipped=" + result.skipped
                    + " errors=" + result.errors);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
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

        // 1. Get flight_ids from turn_to_final (these need repopulation)
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
            System.out.println("No TTF cache entries found.");
            return result;
        }
        System.out.println("Found " + flightIds.size() + " flights to repopulate.");

        if (!dryRun) {
            // 2. Delete all from turn_to_final (or only those we're repopulating)
            int toDelete = limit != null ? Math.min(flightIds.size(), limit) : flightIds.size();
            if (toDelete == flightIds.size()) {
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
            if ((i + 1) % batchSize == 0) {
                long elapsed = (System.currentTimeMillis() - start) / 1000;
                System.out.println("Progress: " + (i + 1) + "/" + flightIds.size()
                        + " recomputed=" + result.recomputed + " skipped=" + result.skipped
                        + " errors=" + result.errors + " (" + elapsed + "s elapsed)");
            }
        }
        return result;
    }
}
