package org.ngafid.uploads.process;


import org.apache.avro.generic.GenericRecord;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.io.InputFile;
import org.ngafid.uploads.Upload;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.logging.Logger;


public class ParquetPipeline {
    private static final Logger LOG = Logger.getLogger(ParquetPipeline.class.getName());
    private final Path parquetFilePath;
    private final Connection connection;
    private final Upload upload;
    private static final String LOG_FILE = "parquet_pipeline_log.txt"; // File to log messages

    public ParquetPipeline(Connection connection, Upload upload, Path parquetFilePath) {
        this.connection = connection;
        this.upload = upload;
        this.parquetFilePath = parquetFilePath;

    }

    public void execute() {
        LOG.info("Processing Parquet file: " + parquetFilePath.toString());


        try {
            processParquetFile();

        } catch (IOException e) {
            LOG.severe("Failed to process Parquet file: " + e.getMessage());
        }
    }



    private void processParquetFile() throws IOException {
        LOG.info("Reading Parquet file: " + parquetFilePath.toString());
        try {

            InputFile inputFile = new NioInputFile(parquetFilePath);



            // Read the Parquet file
            try (ParquetReader<GenericRecord> reader = AvroParquetReader.<GenericRecord>builder(inputFile).build();
            ) {
                GenericRecord record;
                while ((record = reader.read()) != null) {

                    System.out.println("Printing parquet file!!");
                    System.out.println(record);
                    System.out.println("Printing parquet file!! Done");
                    break;
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
