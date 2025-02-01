package org.ngafid.bin;

import org.apache.commons.cli.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.ngafid.common.Database;
import org.ngafid.uploads.UploadDoesNotExistException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import static org.ngafid.kafka.Configuration.getUploadProducer;

public enum UploadHelper {
    ;

    private static final Logger LOG = Logger.getLogger(UploadHelper.class.getName());

    private static Options buildCLIOptions() {
        Options options = new Options();

        Option input = new Option("f", "fleet", true, "Fleet ID for which all uploads should be added to the processing queue");
        input.setRequired(false);
        options.addOption(input);

        Option output = new Option("u", "upload", true, "Upload ID that should be added to the processing queue");
        output.setRequired(false);
        options.addOption(output);

        return options;
    }

    public static void main(String[] arguments) throws SQLException, UploadDoesNotExistException {
        Options options = buildCLIOptions();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, arguments);
        } catch (org.apache.commons.cli.ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("ngafid-upload-utility", options);
            System.exit(1);
        }

        if (cmd.hasOption("fleet")) {
            enqueueFleetUploads(Integer.parseInt(cmd.getOptionValue("fleet")));
        } else if (cmd.hasOption("upload")) {
            enqueueUpload(Integer.parseInt(cmd.getOptionValue("upload")));
        } else {
            formatter.printHelp("ngafid-upload-utility", options);
        }
    }


    static private void enqueueUpload(int uploadId) {
        try (KafkaProducer<String, Integer> producer = getUploadProducer()) {
            producer.send(new ProducerRecord<>("upload", uploadId));
        }
    }

    static private void enqueueFleetUploads(int fleetId) throws SQLException {
        try (Connection connection = Database.getConnection();
             KafkaProducer<String, Integer> producer = getUploadProducer()) {

            // Step through the fleets uploads in pages of 1K, adding them to the upload topic as we do.
            int idCursor = 0;
            while (true) {
                try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM uploads WHERE fleet_id = ?  AND id > ? LIMIT 1000")) {
                    statement.setInt(1, fleetId);
                    statement.setInt(2, idCursor);

                    try (ResultSet resultSet = statement.executeQuery()) {
                        int nRows = 0;
                        while (resultSet.next()) {
                            int uploadId = resultSet.getInt(1);
                            LOG.info("Added upload id = " + uploadId + " to `upload` topic.");
                            producer.send(new ProducerRecord<>("upload", uploadId));
                            idCursor = Math.max(uploadId, idCursor);
                            nRows += 1;
                        }
                        if (nRows == 0)
                            break;
                    }
                }
            }
        }
    }
}
