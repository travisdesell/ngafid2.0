package org.ngafid.flights.dji;

import java.io.IOException;
import java.util.Scanner;

public class DAT2CSV {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter DAT file path");
        String filePath = scanner.nextLine();

        System.out.println("Init Datfile");
        DATDJIFile datFile = new DATDJIFile(filePath);
        DATConvert datConvert = datFile.createConvertDat();

        datFile.reset();

        AnalyzeResultsDAT results = datConvert.analyze(true);
    }


}
