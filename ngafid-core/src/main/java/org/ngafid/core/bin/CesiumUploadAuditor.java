package org.ngafid.core.bin;

import static org.ngafid.core.kafka.Configuration.getUploadProperties;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.cli.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.ngafid.core.Config;
import org.ngafid.core.Database;
import org.ngafid.core.flights.CesiumFlightReadiness;
import org.ngafid.core.flights.Flight;
import org.ngafid.core.kafka.Topic;
import org.ngafid.core.uploads.UploadDoesNotExistException;

/**
 * Scans uploads by sampling flight data against Cesium readiness rules, then optionally
 * re-enqueues failed uploads through the normal {@link UploadHelper} Kafka pipeline.
 */
public final class CesiumUploadAuditor {

    private static final Logger LOG = Logger.getLogger(CesiumUploadAuditor.class.getName());

    private CesiumUploadAuditor() {}

    private static Options buildCLIOptions() {
        Options options = new Options();

        Option fleet = new Option("f", "fleet", true, "Fleet ID(s) to scan");
        fleet.setArgs(Option.UNLIMITED_VALUES);
        fleet.setOptionalArg(true);
        options.addOption(fleet);

        Option upload = new Option("u", "upload", true, "Upload ID(s) to scan");
        upload.setArgs(Option.UNLIMITED_VALUES);
        upload.setOptionalArg(true);
        options.addOption(upload);

        Option file = new Option("F", "file", true, "File containing upload IDs (one or more per line)");
        options.addOption(file);

        Option query = new Option("q", "query", true, "SQL WHERE clause against the uploads table");
        options.addOption(query);

        Option enqueue = new Option("e", "enqueue", false, "Re-enqueue uploads that fail the Cesium check");
        options.addOption(enqueue);

        Option checkAll = new Option("a", "check-all-flights", false, "Check every flight in each upload (default: first flight only)");
        options.addOption(checkAll);

        Option output = new Option("o", "output", true, "Write failing upload IDs to this file (one per line)");
        options.addOption(output);

        Option report = new Option(
                "r",
                "report",
                true,
                "Write a full audit/reprocess report to this file (default: run/cesium_reprocess_<timestamp>.log when -e is used)");
        options.addOption(report);

        Option limit = new Option("l", "limit", true, "Maximum number of uploads to scan");
        options.addOption(limit);

        return options;
    }

    public static void main(String[] arguments) throws SQLException, UploadDoesNotExistException, IOException {
        Options options = buildCLIOptions();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, arguments);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("ngafid-cesium-upload-auditor", options);
            System.exit(1);
            return;
        }

        List<Integer> uploadIds = resolveUploadIds(cmd);
        if (uploadIds.isEmpty()) {
            System.out.println("No upload IDs to scan.");
            formatter.printHelp("ngafid-cesium-upload-auditor", options);
            System.exit(1);
            return;
        }

        if (cmd.hasOption("limit")) {
            int max = Integer.parseInt(cmd.getOptionValue("limit"));
            if (uploadIds.size() > max) {
                uploadIds = uploadIds.subList(0, max);
            }
        }

        boolean checkAllFlights = cmd.hasOption("check-all-flights");
        boolean enqueue = cmd.hasOption("enqueue");
        List<Integer> failedUploadIds = new ArrayList<>();
        List<String> reportLines = new ArrayList<>();

        try (Connection connection = Database.getConnection()) {
            for (Integer uploadId : uploadIds) {
                AuditResult result = auditUpload(connection, uploadId, checkAllFlights);
                if (result.failed()) {
                    failedUploadIds.add(uploadId);
                    String line = "FAIL upload " + uploadId + " flight " + result.failedFlightId() + ": "
                            + result.errorMessage();
                    reportLines.add(line);
                    System.out.println(line);
                } else if (result.skipped()) {
                    String line = "SKIP upload " + uploadId + ": " + result.errorMessage();
                    reportLines.add(line);
                    System.out.println(line);
                } else {
                    String line = "OK   upload " + uploadId;
                    reportLines.add(line);
                    System.out.println(line);
                }
            }
        }

        System.out.println();
        String summary = "Scanned " + uploadIds.size() + " upload(s); " + failedUploadIds.size()
                + " failed Cesium check.";
        System.out.println(summary);

        if (cmd.hasOption("output") && !failedUploadIds.isEmpty()) {
            writeUploadIds(new File(cmd.getOptionValue("output")), failedUploadIds);
            System.out.println("Wrote failing upload IDs to " + cmd.getOptionValue("output"));
        }

        if (enqueue) {
            if (failedUploadIds.isEmpty()) {
                System.out.println("Nothing to enqueue.");
            } else {
                enqueueUploads(failedUploadIds);
                System.out.println("Enqueued " + failedUploadIds.size() + " upload(s) for reprocessing.");
            }
        } else if (!failedUploadIds.isEmpty()) {
            System.out.println("Dry run only. Re-run with -e / --enqueue to reprocess failed uploads.");
        }

        File reportFile = resolveReportFile(cmd, enqueue);
        if (reportFile != null) {
            writeReport(reportFile, summary, reportLines, failedUploadIds, enqueue);
            System.out.println("Wrote report to " + reportFile.getPath());
        }
    }

    private static File resolveReportFile(CommandLine cmd, boolean enqueue) {
        if (cmd.hasOption("report")) {
            return new File(cmd.getOptionValue("report"));
        }
        if (enqueue) {
            return defaultReportFile();
        }
        return null;
    }

    private static File defaultReportFile() {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
        File runDir = new File(Config.getProperty("ngafid.repo.path"), "run");
        if (!runDir.exists()) {
            runDir.mkdirs();
        }
        return new File(runDir, "cesium_reprocess_" + timestamp + ".log");
    }

    private static void writeReport(
            File file,
            String summary,
            List<String> reportLines,
            List<Integer> enqueuedUploadIds,
            boolean enqueued)
            throws IOException {
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }

        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println("# Cesium upload auditor report");
            writer.println("# " + LocalDateTime.now());
            writer.println("# " + summary);
            writer.println("# reprocess_enqueued=" + enqueued);
            writer.println();
            for (String line : reportLines) {
                writer.println(line);
            }
            writer.println();
            if (enqueued && !enqueuedUploadIds.isEmpty()) {
                writer.println("ENQUEUED_UPLOAD_IDS:");
                for (Integer uploadId : enqueuedUploadIds) {
                    writer.println(uploadId);
                }
            }
        }
    }

    private record AuditResult(boolean failed, boolean skipped, int failedFlightId, String errorMessage) {
        static AuditResult ok() {
            return new AuditResult(false, false, -1, null);
        }

        static AuditResult skip(int uploadId, String reason) {
            return new AuditResult(false, true, -1, reason);
        }

        static AuditResult fail(int flightId, String errorMessage) {
            return new AuditResult(true, false, flightId, errorMessage);
        }
    }

    private static AuditResult auditUpload(Connection connection, int uploadId, boolean checkAllFlights)
            throws SQLException {
        ArrayList<Flight> flights = Flight.getFlightsFromUpload(connection, uploadId);
        if (flights.isEmpty()) {
            return AuditResult.skip(uploadId, "no flights");
        }

        List<Flight> flightsToCheck = checkAllFlights ? flights : List.of(flights.get(0));
        for (Flight flight : flightsToCheck) {
            CesiumFlightReadiness.Result result = CesiumFlightReadiness.evaluate(connection, flight.getId());
            if (!result.ready()) {
                return AuditResult.fail(result.flightId(), result.errorMessage());
            }
        }
        return AuditResult.ok();
    }

    private static List<Integer> resolveUploadIds(CommandLine cmd) throws SQLException, IOException {
        List<Integer> ids = new ArrayList<>();

        if (cmd.hasOption("file")) {
            try (BufferedReader reader = new BufferedReader(new FileReader(cmd.getOptionValue("file")))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    for (String chunk : line.split("\\s+")) {
                        chunk = chunk.trim();
                        if (chunk.isEmpty()) {
                            continue;
                        }
                        ids.add(Integer.parseInt(chunk));
                    }
                }
            }
        } else if (cmd.hasOption("q")) {
            String where = cmd.getOptionValue("q");
            try (Connection connection = Database.getConnection();
                    PreparedStatement statement =
                            connection.prepareStatement("SELECT id FROM uploads WHERE " + where);
                    ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(resultSet.getInt(1));
                }
            }
        }

        if (cmd.hasOption("fleet")) {
            if (ids.isEmpty()) {
                for (String value : cmd.getOptionValues("fleet")) {
                    ids.addAll(uploadIdsForFleet(Integer.parseInt(value)));
                }
            }
        }

        if (cmd.hasOption("upload")) {
            if (ids.isEmpty()) {
                for (String value : cmd.getOptionValues("upload")) {
                    ids.add(Integer.parseInt(value));
                }
            }
        }

        return ids;
    }

    private static List<Integer> uploadIdsForFleet(int fleetId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (Connection connection = Database.getConnection()) {
            int idCursor = 0;
            while (true) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT id FROM uploads WHERE fleet_id = ? AND id > ? ORDER BY id LIMIT 1000")) {
                    statement.setInt(1, fleetId);
                    statement.setInt(2, idCursor);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        int nRows = 0;
                        while (resultSet.next()) {
                            int uploadId = resultSet.getInt(1);
                            ids.add(uploadId);
                            idCursor = uploadId;
                            nRows++;
                        }
                        if (nRows == 0) {
                            break;
                        }
                    }
                }
            }
        }
        return ids;
    }

    private static void writeUploadIds(File file, List<Integer> uploadIds) throws IOException {
        try (PrintWriter writer = new PrintWriter(file)) {
            for (Integer uploadId : uploadIds) {
                writer.println(uploadId);
            }
        }
    }

    private static void enqueueUploads(List<Integer> uploadIds) {
        try (KafkaProducer<String, Integer> producer = new KafkaProducer<>(getUploadProperties())) {
            for (Integer uploadId : uploadIds) {
                LOG.info("Added upload id = " + uploadId + " to `upload` topic.");
                producer.send(new ProducerRecord<>(Topic.UPLOAD.toString(), String.valueOf(uploadId), uploadId));
            }
        }
    }
}
