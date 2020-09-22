package org.ngafid.common;

import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.IOException;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.util.Enumeration;

import org.ngafid.flights.Flight;

import jdk.incubator.jpackage.internal.IOUtils;

public class CSVWriter{
    private File file;
    private String directoryRoot;
	private Flight flight;

	/**
	 * Constructor
	 * Finds the zip file with the filght requested
	 *
	 * @param directoryRoot the root directory of the zipped files
	 * @param uploadId the id of the upload
	 *
	 * @return a File pointing to the zip containing the entry
	 */
    public CSVWriter(String directoryRoot, Flight flight){
		File root = new File(directoryRoot);
		File[] dirs = root.listFiles();

		this.flight = flight;

		int uploadId = flight.getUploadId();

		System.out.println("target id: "+uploadId);
		for (File archive : dirs) {
			String archPath = archive.toString();
			String [] archDirs = archPath.split("/");
			String archName = archDirs[archDirs.length - 1];
			if(archName.contains(Integer.toString(uploadId))){
				 this.file = archive;
			}
		}
	}
		
	/**
	 * Writes data to a file output stream
	 *
	 * @param inputStream the input stream to copy data from
	 *
	 * @return a String with all the data for the CSV file
	 *
	 * @throws IOException if there is an IOException when parsing the inputStream
	 */
	private String writeToFile(InputStream inputStream) throws IOException {
		String strOut = new String(spark.utils.IOUtils.toByteArray(inputStream));

		inputStream.close();
		return strOut;
	}

	/**
	 * Writes to a file and gets the zip archive first
	 *
	 * @return a String with the file contents
	 *
	 * @throws IOException if there is a problem with file i/o
	 */
	public String write() throws IOException {
		ZipFile zipArchive = new ZipFile(this.file);
		String filename = flight.getFilename();
		System.out.println("filename: "+filename);
		Enumeration<? extends ZipEntry> entries = zipArchive.entries();
		String fileOut = "";
		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();

			if (entry.getName().equals(filename)) {
				fileOut = this.writeToFile(zipArchive.getInputStream(entry));
			} 
		} 
		zipArchive.close();
		return fileOut;
	}
}

