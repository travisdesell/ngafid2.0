package org.ngafid.flights.DJIBinary;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DAT2CSVWriter extends FileWriter {
    File file = null;

    public DAT2CSVWriter(String name) throws IOException {
        this(new File(name));
    }

    public DAT2CSVWriter(File file) throws IOException {
        super(file);
        this.file = file;
    }

    public void print(String string) throws IOException {
        write(string, 0, string.length());
    }

    public void println(String string) throws IOException {
        print(string + "\n");
    }

}
