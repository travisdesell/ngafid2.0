/**
 * Generates/Copies CSV files for flights in the ngafid
 *
 * @author <a href = "mailto:apl1341@cs.rit.edu">Aidan LaBella</a>
 */

package org.ngafid.common;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;

import java.util.zip.*;

import java.util.Enumeration;

import spark.utils.IOUtils;

import org.ngafid.flights.Flight;

public class CSVWriter{
    private File file;
    private String directoryRoot;
	private Flight flight;
	private ZipEntry entry;
	private ZipFile zipArchive;

	/**
	 * Constructor
	 * Finds the zip file with the filght requested
	 *
	 * @param directoryRoot the root directory of the zipped files
	 * @param uploadId the id of the upload
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
				 try{
					 this.zipArchive = new ZipFile(this.file);
				 } catch (IOException e) {
					 e.printStackTrace();
				 }
			}
		}

		String filename = flight.getFilename();
		Enumeration<? extends ZipEntry> entries = zipArchive.entries();
		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();

			if (entry.getName().equals(filename)) {
				this.entry = entry;
			} 
		} 
	}

	/**
	 * Gets the CSV file as primitive (binary) data
	 *
	 * @return a primitive array of bytes 
	 */
	public byte[] toBinaryData() throws IOException {
		InputStream inputStream = zipArchive.getInputStream(this.entry);
		return IOUtils.toByteArray(inputStream);
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
	private String writeToFile() throws IOException {
		String strOut = new String(toBinaryData());
		return strOut;
	}

	/**
	 * Accessor method for the {@link ZipEntry} associated with this flight
	 *
	 * @return and instance of {@link ZipEntry}
	 */
	public ZipEntry getFlightEntry(){
		return this.entry;
	}

	/**
	 * Writes to a file and gets the zip archive first
	 *
	 * @return a String with the file contents
	 *
	 * @throws IOException if there is a problem with file i/o
	 */
	public String write() throws IOException {
		String fileOut = "";
		fileOut = this.writeToFile();
		zipArchive.close();
		return fileOut;
	}

	/**
	 * Writes to a file and gets the zip archive first
	 *
	 * @return a String with the file contents
	 *
	 * @throws IOException if there is a problem with file i/o
	 */
	public ZipEntry getZipEntry() throws IOException {
		ZipFile zipArchive = new ZipFile(this.file);
		String filename = flight.getFilename();
		Enumeration<? extends ZipEntry> entries = zipArchive.entries();
		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();

			if (entry.getName().equals(filename)) {
				zipArchive.close();
				return entry;
			} 
		} 
		zipArchive.close();
		return null;
	}

	/**
	 * Gets a string representation of a CSV Writer object
	 *
	 * @return a {@link String} with the zip file and archive paths
	 */
	@Override
	public String toString(){
		return this.zipArchive.toString()+" "+this.entry.toString();
	}
}
