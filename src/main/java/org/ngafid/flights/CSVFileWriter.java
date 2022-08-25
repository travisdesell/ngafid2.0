package org.ngafid.flights;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Writes a CSV file based on a flight NOT in the database
 *
 * @author Aaron Chan
 */

public class CSVFileWriter {
    private final String path;
    private final String filename;
    private File file;
    private boolean fileInitialized;
    private Map<String, String> fields;

    public CSVFileWriter(String path, String filename) {
        this.path = path;
        this.filename = filename;
        this.fileInitialized = false;
        this.file = null;
        this.fields = null;
    }

    public void initFile() throws IOException {
        File file = new File(path + "/" + this.filename);

        if (!file.createNewFile()) return;

        this.file = file;
        this.fileInitialized = true;
    }


    public String getFilename() {
        return filename;
    }

    public File getFile() {
        return file;
    }

    public boolean isFileInitialized() {
        return fileInitialized;
    }
}
