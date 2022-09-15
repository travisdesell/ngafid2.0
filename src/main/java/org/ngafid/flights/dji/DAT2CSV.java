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

        DATPersist.save();

        String filePath = "/home/aaron/Downloads/djiDatData/FLY001.DAT";
        System.out.println("Init Datfile");
        DATDJIFile datFile = DATDJIFile.createDatFile(filePath);
        datFile.reset();
        datFile.preAnalyze();


        DATConvert datConvert = datFile.createConvertDat();

        datFile.reset();

        AnalyzeResultsDAT results = datConvert.analyze(true);

        datFile = DATDJIFile.createDatFile(datFile.getAbsolutePath(), datConvert);
        if (datFile != null) {
            String datFileName = datFile.getFile().getAbsolutePath();
            DATPersist.save();
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
