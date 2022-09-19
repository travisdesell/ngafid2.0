package org.ngafid.flights.dji;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class DAT2CSV {
    public static void main(String[] args) throws IOException, NotDATException {
        Scanner scanner = new Scanner(System.in);
//        System.out.println("Enter DAT file path");
//        String filePath = scanner.nextLine();

        // Might be unncessary. probably just stores configuration
//        DATPersist.save();

        String filePath = "/home/aaron/Downloads/djiDatData/FLY001.DAT";
        File file = new File(filePath);
        DATDJIFile datFile = DATDJIFile.createDatFile(filePath);
        DATConvert datConvert = datFile.createConvertDat();
        datConvert.setCsvWriter(new DAT2CSVWriter("/home/aaron/Downloads/djiDatData/FLY001.csv"));
        datConvert.createRecordParsers();
        datFile = DATDJIFile.createDatFile(file.getAbsolutePath(), datConvert);

        if (datFile != null) {
            String datFileName = datFile.getFile().getAbsolutePath();
            datFile.reset();
            datFile.preAnalyze();


//              setFromMarkers();
//                new Runnable() {
//                    @Override
//                    public void run() {
//                        reset();
//                        createFileNames();
//                        checkState();
//                        Persist.save();
//                    }
//                };
        }

        AnalyzeResultsDAT results = datConvert.analyze(true);
    }


}
