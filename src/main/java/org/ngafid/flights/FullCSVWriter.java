package org.ngafid.flights;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import org.ngafid.Database;
import java.util.logging.Logger;

/**
 * This class is designed to 
 */
public class FullCSVWriter {
    private File outFile;
    private List<DoubleTimeSeries> timeSeries;
    private int numberValidSeries;
    private int numberRows;

    static Connection connection = Database.getConnection();

    public FullCSVWriter(String fileName, int flightId) throws Exception {
        this.outFile = new File(fileName);

        this.timeSeries = DoubleTimeSeries.getAllDoubleTimeSeries(connection, flightId);
        this.numberValidSeries = this.timeSeries.size();

        //This asserts that all time series columns will have the same length (which should always be the case!)
        this.numberRows = this.timeSeries.get(0).size();
    }

    private String getHeader() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < numberValidSeries; i++) {
            DoubleTimeSeries dts = this.timeSeries.get(i);
            sb.append((i < numberValidSeries - 1) ? (dts.getName() + ",") : dts.getName());
        }

        return sb.toString();
    }

    public void writeFile() throws IOException {
        PrintWriter pw = new PrintWriter(this.outFile);

        pw.println(getHeader());

        for (int i = 0; i < this.numberRows; i++) {
            for (int j = 0; j < numberValidSeries; j++) {
                DoubleTimeSeries dts = this.timeSeries.get(j);
                pw.print((j < numberValidSeries -1) ? (dts.get(i) + ",") : (dts.get(i) + "\n"));
            }
        }

        pw.close();
    }

    public static void main(String [] args) {
        Logger LOG = Logger.getLogger(FullCSVWriter.class.getName());

        LOG.info("Usage: FullCSVWriter [flight id] [file name prefix (e.g. <prefix>_flight_id.csv)]");

        int flightId = Integer.parseInt(args[0]);
        String prefix = args[1];

        try {
            String fileName = prefix + "_flight_" + flightId + ".csv";
            LOG.info("Creating file: " + fileName);

            FullCSVWriter writer = new FullCSVWriter(fileName, flightId);
            LOG.info("Writing to file");
            writer.writeFile();
        } catch (Exception e) {
            LOG.severe(e.getMessage());
            System.exit(1);
        }

        LOG.info("Done!");
        System.exit(0);
    }
}
