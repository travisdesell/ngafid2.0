package org.ngafid.core.bin;

import org.apache.commons.cli.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.ngafid.core.Database;
import org.ngafid.core.uploads.UploadDoesNotExistException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.ngafid.core.kafka.Configuration.getUploadProperties;

public enum UploadHelper {
    ;

    private static final Logger LOG = Logger.getLogger(UploadHelper.class.getName());

    private static KafkaProducer<String, Integer> getUploadProducer() {
        return new KafkaProducer<>(getUploadProperties());
    }

    private static Options buildCLIOptions() {
        Options options = new Options();

        Option input = new Option("f", "fleet", true, "Fleet ID for which all uploads should be added to the processing queue");
        input.setRequired(false);
        input.setArgs(Option.UNLIMITED_VALUES);
        input.setOptionalArg(true);
        options.addOption(input);

        Option upload = new Option("u", "upload", true, "Upload ID that should be added to the processing queue");
        upload.setRequired(false);
        upload.setArgs(Option.UNLIMITED_VALUES);
        upload.setOptionalArg(true);
        options.addOption(upload);

        Option file = new Option("F", "file", true, "File from which to read IDs");
        file.setRequired(false);
        options.addOption(file);

        return options;
    }

    public static void main(String[] arguments) throws SQLException, UploadDoesNotExistException, IOException {
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

        File file = cmd.hasOption("file") ? new File(cmd.getOptionValue("file")) : null;
        List<Integer> ids = new ArrayList<>();

        if (file != null) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] split = line.split(" ");
                    for (String chunk : split) {
                        chunk = chunk.trim();
                        if (chunk.isEmpty())
                            continue;
                        try {
                            int id = Integer.parseInt(chunk);
                            ids.add(id);
                        } catch (NumberFormatException e) {
                            System.out.println("Error parsing integer: '" + chunk + "'");
                        }
                    }
                }
            }
        }

        if (cmd.hasOption("fleet")) {
            if (ids.isEmpty())
                for (String v : cmd.getOptionValues("fleet"))
                    ids.add(Integer.parseInt(v));

            enqueueFleetUploads(ids);
        } else if (cmd.hasOption("upload")) {
            if (ids.isEmpty())
                for (String v : cmd.getOptionValues("upload"))
                    ids.add(Integer.parseInt(v));

            enqueueUploads(ids);
        } else {
            formatter.printHelp("ngafid-upload-utility", options);
        }
    }


    static private void enqueueUploads(List<Integer> uploadIds) {
        try (KafkaProducer<String, Integer> producer = getUploadProducer()) {
            for (Integer uploadId : uploadIds)
                producer.send(new ProducerRecord<>("upload", uploadId));
        }
    }

    static private void enqueueFleetUploads(List<Integer> fleetIds) throws SQLException {
        try (Connection connection = Database.getConnection();
             KafkaProducer<String, Integer> producer = getUploadProducer()) {
            for (Integer fleetId : fleetIds) {
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
}
