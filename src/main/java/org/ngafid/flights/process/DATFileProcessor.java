package org.ngafid.flights.process;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.ngafid.flights.*;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import static org.ngafid.common.TimeUtils.addMilliseconds;

import Files.*;


import org.ngafid.flights.FatalFlightFileException;
import org.ngafid.flights.Flight;
import org.ngafid.flights.FlightAlreadyExistsException;
import org.ngafid.flights.MalformedFlightFileException;

/**
 * Parses DAT files from DJI flights after converting them to CSV
 *
 * @author Aaron Chan
 */

public class DATFileProcessor extends FlightFileProcessor {
    private static final Logger LOG = Logger.getLogger(DATFileProcessor.class.getName());

    private static final Set<String> STRING_COLS = new HashSet<>(List.of("flyCState", "flycCommand", "flightAction",
            "nonGPSCause", "connectedToRC", "Battery:lowVoltage", "RC:ModeSwitch", "gpsUsed", "visionUsed", "IMUEX(0):err"));

    private final ZipFile zipFile;
    private File processedCSVFile;

    public DATFileProcessor(Connection connection, InputStream stream, String filename, ZipFile file) {
        super(connection, stream, filename);
        this.zipFile = file;
    }

    @Override
    public Stream<FlightBuilder> parse() throws FlightProcessingException {
        try {
            convertAndInsert();
            InputStream csvFileStream = new FileInputStream(processedCSVFile);
            List<InputStream> inputStreams = duplicateInputStream(csvFileStream, 2);
            Map<Integer, String> indexedCols = new HashMap<>();
            Map<String, DoubleTimeSeries> doubleTimeSeriesMap = new HashMap<>();
            Map<String, StringTimeSeries> stringTimeSeriesMap = new HashMap<>();
            Map<String, String> attributeMap = getAttributeMap(inputStreams.remove(inputStreams.size() - 1));

            if (!attributeMap.containsKey("mcID(SN)")) {
                throw new FlightProcessingException(new FatalFlightFileException("No DJI serial number provided in binary."));
            }

            try (CSVReader reader = new CSVReader(new BufferedReader(new InputStreamReader(inputStreams.remove(inputStreams.size() - 1))))) {
                processCols(reader.readNext(), indexedCols, doubleTimeSeriesMap, stringTimeSeriesMap);

                readData(reader, doubleTimeSeriesMap, stringTimeSeriesMap, indexedCols);
                calculateLatLonGPS(doubleTimeSeriesMap);

                if (attributeMap.containsKey("dateTime")) {
                    calculateDateTime(doubleTimeSeriesMap, stringTimeSeriesMap, attributeMap.get("dateTime"));
                    String dateTimeStr = findStartDateTime(doubleTimeSeriesMap);

                    if (dateTimeStr != null) {
                        calculateDateTime(doubleTimeSeriesMap, stringTimeSeriesMap, dateTimeStr);
                    }
                }
            } catch (CsvValidationException | FatalFlightFileException | IOException e) {
                throw new FlightProcessingException(e);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            dropBlankCols(doubleTimeSeriesMap, stringTimeSeriesMap);

            FlightMeta meta = new FlightMeta();
            meta.setFilename(filename);
            meta.setAirframeType("UAS Rotorcraft");
            meta.setAirframeName("DJI " + attributeMap.get("ACType"));
            meta.setSystemId(attributeMap.get("mcID(SN)"));


            return Stream.of(new FlightBuilder[]{new DATFlightBuilder(meta, doubleTimeSeriesMap, stringTimeSeriesMap)});
        } catch (NotDatFile | FileEnd | IOException e) {
            throw new FlightProcessingException(e);
        }
    }

    // TODO: Validate the conversion works still. Also maybe figure out another way of doing this since var args forced into FFP

    /**
     * Converts the DAT file to CSV and inserts it into the zip file
     * @throws NotDatFile
     * @throws IOException
     * @throws FileEnd
     */
    private void convertAndInsert() throws NotDatFile, IOException, FileEnd {
        String zipName = filename.substring(filename.lastIndexOf("/"));
        String parentFolder = zipFile.getName().substring(0, zipFile.getName().lastIndexOf("/"));
        File tempExtractedFile = new File(parentFolder, zipName);

        System.out.println("Extracting to " + tempExtractedFile.getAbsolutePath());
        try (InputStream inputStream = zipFile.getInputStream(zipFile.getEntry(filename)); FileOutputStream fileOutputStream = new FileOutputStream(tempExtractedFile)) {
            int len;
            byte[] buffer = new byte[1024];

            while ((len = inputStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, len);
            }
        }

        convertDATFile(tempExtractedFile);
        processedCSVFile = new File(tempExtractedFile.getAbsolutePath() + ".csv");
        // placeInZip(processedCSVFile.getAbsolutePath(), zipFile.getName().substring(zipFile.getName().lastIndexOf("/") + 1));
    }

    /**
     * Places a file into the given zip file
     * @param file - File to place
     * @param zipFileName - Name of the zip file
     * @throws IOException
     */
    private static void placeInZip(String file, String zipFileName) throws IOException {
        LOG.info("Placing " + file + " in zip");

        Map<String, String> zipENV = new HashMap<>();
        zipENV.put("create", "true");

        Path csvFilePath = Paths.get(file);
        Path zipFilePath = Paths.get(csvFilePath.getParent() + "/" + zipFileName);

        URI zipURI = URI.create("jar:" + zipFilePath.toUri());
        try (FileSystem fileSystem = FileSystems.newFileSystem(zipURI, zipENV)) {
            Path zipFileSystemPath = fileSystem.getPath(file.substring(file.lastIndexOf("/") + 1));
            Files.write(zipFileSystemPath, Files.readAllBytes(csvFilePath), StandardOpenOption.CREATE);
        }
    }

    /**
     * Converts the DAT file to CSV
     * @param file - File to convert
     * @return - CSV converted file
     * @throws NotDatFile
     * @throws IOException
     * @throws FileEnd
     */
    private static File convertDATFile(File file) throws NotDatFile, IOException, FileEnd {
        LOG.info("Converting to CSV: " + file.getAbsolutePath());
        DatFile datFile = DatFile.createDatFile(file.getAbsolutePath());
        datFile.reset();
        datFile.preAnalyze();

        ConvertDat convertDat = datFile.createConVertDat();

        String csvFilename = file.getAbsolutePath() + ".csv";
        convertDat.csvWriter = new CsvWriter(csvFilename);
        convertDat.createRecordParsers();

        datFile.reset();
        AnalyzeDatResults results = convertDat.analyze(false);
        LOG.info(datFile.getFile().getAbsolutePath());

        return datFile.getFile();
    }

    /**
     * Reads the data from the converted CSV file
     * @param reader - CSV reader
     * @param doubleTimeSeriesMap - Map of double time series data
     * @param stringTimeSeriesMap - Map of string time series data
     * @param indexedCols - Map of indexed columns
     * @throws IOException
     * @throws CsvValidationException
     */
    private static void readData(CSVReader reader, Map<String, DoubleTimeSeries> doubleTimeSeriesMap,
                                 Map<String, StringTimeSeries> stringTimeSeriesMap, Map<Integer, String> indexedCols) throws IOException, CsvValidationException {
        String[] line;

        while ((line = reader.readNext()) != null) {
            for (int i = 0; i < line.length; i++) {

                String column = indexedCols.get(i);

                try {
                    if (doubleTimeSeriesMap.containsKey(column)) {
                        DoubleTimeSeries colTimeSeries = doubleTimeSeriesMap.get(column);
                        double value = !line[i].equals("") ? Double.parseDouble(line[i]) : Double.NaN;
                        colTimeSeries.add(value);
                    } else {
                        StringTimeSeries colTimeSeries = stringTimeSeriesMap.get(column);
                        colTimeSeries.add(line[i]);
                    }
                } catch (NullPointerException e) {
                    LOG.log(Level.WARNING, "Column {0} not found in time series map", column);
                } catch (NumberFormatException e) {
                    LOG.log(Level.WARNING, "Could not parse value {0} as double", line[i]);
                }
            }
        }
    }

    /**
     * Calculates GPS data from the given time series map
     * @param doubleTimeSeriesMap - Map of double time series data
     * @throws FatalFlightFileException
     */
    private static void calculateLatLonGPS(Map<String, DoubleTimeSeries> doubleTimeSeriesMap) throws FatalFlightFileException {
        DoubleTimeSeries lonRad = doubleTimeSeriesMap.get("GPS(0):Long");
        DoubleTimeSeries latRad = doubleTimeSeriesMap.get("GPS(0):Lat");
        DoubleTimeSeries altMSL = doubleTimeSeriesMap.get("GPS(0):heightMSL");

        if (lonRad == null || latRad == null) {
            LOG.log(Level.WARNING, "Could not find GPS(0):Long or GPS(0):Lat in time series map");
            throw new FatalFlightFileException("No GPS data found in binary.");
        }

        DoubleTimeSeries longDeg = new DoubleTimeSeries("Longitude", "degrees");
        DoubleTimeSeries latDeg = new DoubleTimeSeries("Latitude", "degrees");
        DoubleTimeSeries msl = new DoubleTimeSeries("AltMSL", "ft");

        for (int i = 0; i < lonRad.size(); i++) {
            longDeg.add(lonRad.get(i));
        }

        for (int i = 0; i < lonRad.size(); i++) {
            latDeg.add(latRad.get(i));
        }

        for (int i = 0; i < altMSL.size(); i++) {
            msl.add(altMSL.get(i));
        }

        doubleTimeSeriesMap.put("Longitude", longDeg);
        doubleTimeSeriesMap.put("Latitude", latDeg);
        doubleTimeSeriesMap.put("AltMSL", altMSL);
    }

    /**
     * Calculates the local date and time from the given time series map
     * @param doubleTimeSeriesMap - Map of double time series data
     * @param stringTimeSeriesMap - Map of string time series data
     * @param dateTimeStr - Format of the date and time
     * @throws ParseException
     */
    private static void calculateDateTime(Map<String, DoubleTimeSeries> doubleTimeSeriesMap, Map<String, StringTimeSeries> stringTimeSeriesMap, String dateTimeStr) throws ParseException {
        LOG.info("Calculating date time for DAT file");
        StringTimeSeries localDateSeries = new StringTimeSeries("Lcl Date", "yyyy-mm-dd");
        StringTimeSeries localTimeSeries = new StringTimeSeries("Lcl Time", "hh:mm:ss");
        StringTimeSeries utcOfstSeries = new StringTimeSeries("UTCOfst", "hh:mm"); // Always 0
        DoubleTimeSeries seconds = doubleTimeSeriesMap.get("offsetTime");

        SimpleDateFormat lclDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat lclTimeFormat = new SimpleDateFormat("HH:mm:ss");

        String[] dateTime = dateTimeStr.split(" ");
        String date = dateTime[0];

        if (date.split("-")[1].length() == 1) {
            date = date.substring(0, 5) + "0" + date.substring(5);
        }

        if (date.split("-")[2].length() == 1) {
            date = date.substring(0, 8) + "0" + date.substring(8);
        }

        String time = dateTime[1];

        Date parsedDate = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).parse(date + " " + time);
        for (int i = 0; i < seconds.size(); i++) {
            int millis = (int) (seconds.get(i) * 1000);
            Date newDate = addMilliseconds(parsedDate, millis);

            localDateSeries.add(lclDateFormat.format(newDate));
            localTimeSeries.add(lclTimeFormat.format(newDate));
            utcOfstSeries.add("+00:00");
        }

        stringTimeSeriesMap.put("Lcl Date", localDateSeries);
        stringTimeSeriesMap.put("Lcl Time", localTimeSeries);
        stringTimeSeriesMap.put("UTCOfst", utcOfstSeries);
    }


    /**
     * Determine the start date and time from the given time series map
     * @param doubleTimeSeriesMap - Map of double time series data
     * @return
     */
    private static String findStartDateTime(Map<String, DoubleTimeSeries> doubleTimeSeriesMap) {
        DoubleTimeSeries dateSeries = doubleTimeSeriesMap.get("GPS(0):Date");
        DoubleTimeSeries timeSeries = doubleTimeSeriesMap.get("GPS(0):Time");
        DoubleTimeSeries offsetTime = doubleTimeSeriesMap.get("offsetTime");

        if (dateSeries == null || timeSeries == null) {
            LOG.log(Level.WARNING, "Could not find GPS(0):Date or GPS(0):Time in time series map");
            return null;
        }

        int colCount = 0;
        while (colCount < dateSeries.size() && colCount < timeSeries.size()) {
            int date = (int) dateSeries.get(colCount); // Date is an integer in the format YYYYMMDD
            int time = (int) timeSeries.get(colCount);


            if (!Double.isNaN(date) && !Double.isNaN(time) && date != 0 && time != 0) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

                String year = String.valueOf(date).substring(0, 4);
                String month = String.valueOf(date).substring(4, 6);
                String day = String.valueOf(date).substring(6, 8);

                String hour = String.valueOf(time).substring(0, 2);
                String minute = String.valueOf(time).substring(2, 4);
                String second = String.valueOf(time).substring(4, 6);

                try {
                    Date parsedDate = dateFormat.parse(year + month + day + hour + minute + second);
                    int currentOffset = (int) (offsetTime.get(colCount) * 1000);
                    Date newDate = addMilliseconds(parsedDate, -currentOffset);

                    return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(newDate);
                } catch (ParseException e) {
                    LOG.log(Level.WARNING, "Could not parse date {0} and time {1} as date", new Object[]{date, time});
                    return null;
                }
            }

            colCount++;
        }

        return null;
    }

    /**
     * Duplicate an input stream a given number of times
     * @param inputStream - Input Stream to duplicate
     * @param copies - Number of copies to make
     * @return - List of input streams
     * @throws IOException
     */
    private static List<InputStream> duplicateInputStream(InputStream inputStream, int copies) throws IOException {
        List<InputStream> inputStreams = new ArrayList<>();
        List<OutputStream> outputStreams = new ArrayList<>();

        for (int i = 0; i < copies; i++) {
            outputStreams.add(new ByteArrayOutputStream());
        }

        byte[] buffer = new byte[1024];
        while (inputStream.read(buffer) > -1) {
            for (OutputStream outputStream : outputStreams) {
                outputStream.write(buffer);
            }
        }

        for (OutputStream outputStream : outputStreams) {
            outputStream.flush();
            inputStreams.add(new ByteArrayInputStream(((ByteArrayOutputStream) outputStream).toByteArray()));
        }

        return inputStreams;
    }

    /**
     * Gets the attributes of the flight
     * @param stream - Input stream of flight file
     * @return
     */
    private static Map<String, String> getAttributeMap(InputStream stream) {
        Map<String, String> attributeMap = new HashMap<>();
        try (CSVReader reader = new CSVReader(new BufferedReader(new InputStreamReader(stream)))) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                if (line[line.length - 1].contains("|")) {
                    String[] split = line[line.length - 1].split("\\|");
                    attributeMap.put(split[0], split[1]);
                }
            }
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }

        LOG.log(Level.INFO, "Attribute Map: {0}", attributeMap);

        return attributeMap;
    }

    /**
     * Drop all columns that have no data
     * @param doubleTimeSeriesMap - Map of double time series data
     * @param stringTimeSeriesMap - Map of string time series data
     */
    private static void dropBlankCols(Map<String, DoubleTimeSeries> doubleTimeSeriesMap, Map<String, StringTimeSeries> stringTimeSeriesMap) {
        for (String key : doubleTimeSeriesMap.keySet()) {
            if (doubleTimeSeriesMap.get(key).size() == 0) {
                doubleTimeSeriesMap.remove(key);
            }
        }

        for (String key : stringTimeSeriesMap.keySet()) {
            if (stringTimeSeriesMap.get(key).size() == 0) {
                stringTimeSeriesMap.remove(key);
            }
        }
    }

    /**
     * Initialize columns based on flight data
     * @param cols
     * @param indexedCols
     * @param doubleTimeSeriesMap
     * @param stringTimeSeriesMap
     */
    private static void processCols(String[] cols, Map<Integer, String> indexedCols, Map<String, DoubleTimeSeries> doubleTimeSeriesMap, Map<String, StringTimeSeries> stringTimeSeriesMap) {
        int i = 0;
        for (String col : cols) {
            indexedCols.put(i++, col);
            String[] splitCol = col.split(":");

            // Format of a column is roughly:
            // <CATEGORY>:<TYPE>:<SUBNAME>
            // where :<SUBNAME> is optional.
            String category = splitCol[0];

            if (category.contains("(")) {
                category = category.substring(0, category.indexOf("("));
            }

            switch (category) {
                case "IMU_ATTI":
                case "IMUEX":
                    handleIMUDataType(col, splitCol[1], doubleTimeSeriesMap, stringTimeSeriesMap);
                    break;
                case "GPS":
                    handleGPSDataType(col, doubleTimeSeriesMap, stringTimeSeriesMap);
                    break;

                case "Battery":
                case "SMART_BATT":
                    handleBatteryDataType(col, doubleTimeSeriesMap);
                    break;

                case "Motor":
                case "MotorCtrl":
                    handleMotorDataType(col, doubleTimeSeriesMap, stringTimeSeriesMap);
                    break;

                case "RC":
                    handleRCDataType(col, doubleTimeSeriesMap, stringTimeSeriesMap);
                    break;

                case "AirComp":
                    handleAirCompDataType(col, doubleTimeSeriesMap);
                    break;

                case "General":
                    doubleTimeSeriesMap.put(col, new DoubleTimeSeries(col, "ft"));
                    break;

                case "Controller":
                    doubleTimeSeriesMap.put(col, new DoubleTimeSeries(col, "level"));
                    break;

                default:
                    handleMiscDataType(col, doubleTimeSeriesMap, stringTimeSeriesMap);
            }

        }
    }

    static final Map<String, String> DATA_TYPE_MAP = Map.ofEntries(
        Map.entry("accel", "m/s^2"),
        Map.entry("gyro", "deg/s"),
        Map.entry("Gyro", "deg/s"),
        Map.entry("vel", "m/s"),
        Map.entry("Velocity", "m/s"),
        Map.entry("mag", "A/m"),
        Map.entry("Longitude", "degrees"),
        Map.entry("Latitude", "degrees"),
        Map.entry("roll", "degrees"),
        Map.entry("pitch", "degrees"),
        Map.entry("yaw", "degrees"),
        Map.entry("directionOfTravel", "degrees"),
        Map.entry("distance", "ft"),
        Map.entry("GPS-H", "ft"),
        Map.entry("Alti", "ft"),
        Map.entry("temperature", "Celsius"),
        Map.entry("barometer", "atm"),
        Map.entry("Long", "degrees"),
        Map.entry("Lat", "degrees"),
        Map.entry("height", "ft"),
        Map.entry("DOP", "DOP Value"),
        Map.entry("Date", "Date"),
        Map.entry("Time", "Time"),
        Map.entry("sAcc", "cm/s")
    );

    /**
     * Helper for initializing IMU data
     * @param colName - Name of column
     * @param doubleTimeSeriesMap - Map of double time series data
     * @param stringTimeSeriesMap - Map of string time series data
     */
    private static void handleIMUDataType(String col, String dataType, Map<String, DoubleTimeSeries> doubleTimeSeriesMap, Map<String, StringTimeSeries> stringTimeSeriesMap) {
        String unit = getBestUnitMatch(col);

        if (unit == null) {
            if (col.contains("err")) {
                stringTimeSeriesMap.put(col, new StringTimeSeries("IMUEX Error", "error"));
                return;
            }

            unit = "number";
            if (!col.contains("num")) {
                LOG.log(Level.WARNING, "IMU Unknown data type: {0}", col);
            }
        }

        doubleTimeSeriesMap.put(col, new DoubleTimeSeries(col, dataType));
    }

    private static String getBestUnitMatch(String col) {
        Set<String> units =
            DATA_TYPE_MAP.keySet().stream()
                .filter(key -> col.contains(key))
                .collect(Collectors.toSet());
        String unit = null;
        if (units.size() == 1) {
            for (String u : units)
                unit = u;
        } else if (units.size() > 1) {
            unit = units.stream().max(Comparator.comparingInt(String::length)).get();
        }

        return unit;
    }

    /**
     * Helper for initializing battery data
     * @param colName - Name of column
     * @param doubleTimeSeriesMap - Map of double time series data
     */
    private static void handleGPSDataType(String colName, Map<String, DoubleTimeSeries> doubleTimeSeriesMap, Map<String, StringTimeSeries> stringTimeSeriesMap) {

        if (colName.contains("dateTimeStamp")) {
            stringTimeSeriesMap.put(colName, new StringTimeSeries(colName, "yyyy-mm-ddThh:mm:ssZ"));
            return;
        }
       
        String unit = getBestUnitMatch(colName);

        if (unit == null) {
            unit = "number";
            if (!colName.contains("num")) {
                LOG.log(Level.WARNING, "GPS Unknown data type: {0}", colName);
            }
        }

        doubleTimeSeriesMap.put(colName, new DoubleTimeSeries(colName, unit));
    }

    /**
     * Helper for initializing battery data
     * @param colName - Name of column
     * @param doubleTimeSeriesMap - Map of double time series data
     */
    private static void handleBatteryDataType(String colName, Map<String, DoubleTimeSeries> doubleTimeSeriesMap) {
        String dataType = "number";
        String lowerColName = colName.toLowerCase();

        if (lowerColName.contains("volt")) {
            dataType = "Voltage";
        } else if (lowerColName.contains("watts")) {
            dataType = "Watts";
        } else if (lowerColName.contains("current")) {
            dataType = "Amps";
        } else if (lowerColName.contains("cap")) {
            dataType = "Capacity";
        } else if (lowerColName.contains("temp")) {
            dataType = "Celsius";
        } else if (lowerColName.contains("%")) {
            dataType = "Percentage";
        } else if (lowerColName.contains("time")) {
            dataType = "seconds";
        } else if (lowerColName.contains("status")) {
            dataType = "Battery Status";
        } else {
            LOG.log(Level.WARNING, "Battery Unknown data type: {0}", colName);
        }

        doubleTimeSeriesMap.put(colName, new DoubleTimeSeries(colName, dataType));
    }

    /**
     * Helper for initializing motor data
     * @param colName - Name of column
     * @param doubleTimeSeriesMap - Map of double time series data
     */
    private static void handleMotorDataType(String colName, Map<String, DoubleTimeSeries> doubleTimeSeriesMap, Map<String, StringTimeSeries> stringTimeSeriesMap) {
        if (colName.contains("lowVoltage")) {
            stringTimeSeriesMap.put(colName, new StringTimeSeries(colName, "Low Voltage"));
            return;
        } else if (colName.contains("status") || colName.contains("Status")) {
            stringTimeSeriesMap.put(colName, new StringTimeSeries(colName, "Motor Status"));
            return;
        }

        String dataType = "number";

        if (colName.contains("V_out") || colName.contains("Volts")) {
            dataType = "Voltage";
        } else if (colName.contains("Speed")) {
            dataType = "m/s";
        } else if (colName.contains("Current")) {
            dataType = "Amps";
        } else if (colName.contains("PPMrecv")) {
            dataType = "RC Stop Command";
        } else if (colName.contains("Temp")) {
            dataType = "Celsius";
        } else if (colName.contains("Status")) {
            dataType = "Status Number";
        } else if (colName.contains("Hz")) {
            dataType = "Hz";
        } else if (colName.contains("PWM")) {
            dataType = "PWM Reading";
        } else {
            LOG.log(Level.WARNING, "Battery Unknown data type: {0}", colName);
        }

        doubleTimeSeriesMap.put(colName, new DoubleTimeSeries(colName, dataType));
    }

    /**
     * Helper for initializing RC data
     * @param colName - Name of column
     * @param doubleTimeSeriesMap - Map of double time series data
     */
    private static void handleRCDataType(String colName, Map<String, DoubleTimeSeries> doubleTimeSeriesMap, Map<String, StringTimeSeries> stringTimeSeriesMap) {
        String dataType = "number";

        if (colName.contains("Aileron")) {
            dataType = "Aileron";
        } else if (colName.contains("Elevator")) {
            dataType = "Elevator";
        } else if (colName.contains("Rudder")) {
            dataType = "Rudder";
        } else if (colName.contains("Throttle")) {
            dataType = "Throttle";
        } else {
            if (colName.equals("RC:ModeSwitch")) {
                stringTimeSeriesMap.put(colName, new StringTimeSeries("RC Mode Switch", "Mode"));
                return;
            }

            LOG.log(Level.WARNING, "RC Unknown data type: {0}", colName);
        }

        doubleTimeSeriesMap.put(colName, new DoubleTimeSeries(colName, dataType));
    }

    /**
     * Helper for initializing air comp data
     * @param colName - Name of column
     * @param doubleTimeSeriesMap - Map of double time series data
     */
    private static void handleAirCompDataType(String colName, Map<String, DoubleTimeSeries> doubleTimeSeriesMap) {
        String dataType;

        if (colName.contains("AirSpeed")) {
            dataType = "knots";
        } else if (colName.contains("Alti")) {
            dataType = "ft";
        } else if (colName.contains("Vel")) {
            dataType = "k/h";
        } else {
            dataType = "number";
            LOG.log(Level.WARNING, "AirComp Unknown data type: {0}", colName);
        }

        doubleTimeSeriesMap.put(colName, new DoubleTimeSeries(colName, dataType));
    }

    /**
     * Helper for initializing other types of data
     * @param colName - Name of column
     * @param doubleTimeSeriesMap - Map of double time series data
     */
    private static void handleMiscDataType(String colName, Map<String, DoubleTimeSeries> doubleTimeSeriesMap, Map<String, StringTimeSeries> stringTimeSeriesMap) {
        String dataType;
        boolean isDouble = true;
        switch (colName) {
            case "Tick#":
                dataType = "tick";
                break;

            case "offsetTime":
            case "flightTime":
                dataType = "seconds";
                break;

            case "gpsHealth":
                dataType = "GPS Health";
                break;

            case "flyCState":
                dataType = "C State";
                isDouble = false;
                break;

            case "flycCommand":
                dataType = "Command";
                isDouble = false;
                break;

            case "flightAction":
                dataType = "Action";
                isDouble = false;
                break;

            case "nonGPSCause":
                dataType = "GPS Cause";
                isDouble = false;
                break;

            case "connectedToRC":
                dataType = "Connection";
                isDouble = false;
                break;

            case "gpsUsed":
            case "visionUsed":
                dataType = "boolean";
                isDouble = false;
                break;

            case "Attribute|Value":
                dataType = "Key-Value Pair";
                isDouble = false;
                break;

            default:
                dataType = "N/A";
                isDouble = false;
                LOG.log(Level.WARNING, "Misc Unknown data type: {0}", colName);
        }

        if (isDouble) {
            doubleTimeSeriesMap.put(colName, new DoubleTimeSeries(colName, dataType));
        } else {
            stringTimeSeriesMap.put(colName, new StringTimeSeries(colName, dataType));
        }
    }
}
