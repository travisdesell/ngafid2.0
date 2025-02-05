package org.ngafid.uploads.process;

import org.ngafid.common.Database;
import org.ngafid.events.Event;
import org.ngafid.events.calculations.TurnToFinal;
import org.ngafid.flights.Flight;
import org.ngafid.uploads.ProcessUpload;
import org.ngafid.uploads.Upload;
import org.ngafid.uploads.UploadException;
import org.ngafid.uploads.process.format.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Primary entry point for interacting with the org.ngafid.flights.process package.
 * This wraps up the process of (1) recognizing file types, (2) parsing the files, and (3) processing the data.
 * <p>
 * Files are recognized by their file extensions - the extension is used to create a `FlightFileProcessor` from the list
 * of factories in `this.factories`.
 * <p>
 * `FlightFlileProcessor` objects handle the task of parsing the data: obtaining meta data and the actual double and
 * string series. These are placed into a flight builder, which is a more general representation of an incomplete
 * flight.
 * <p>
 * `FlightBuilder`s can be specialized and these specializations will specify a set of `ProcessStep`s which will be
 * applied to compute everything we need. `ProcessStep`s all have required input columns and output columns: these
 * requirements can be used to form a DAG, traversing it in the proper order will let us compute the steps in the proper
 * order. This process can also be parallelized, see org.ngafid.flights.process.DepdendencyGraph for more on this.
 *
 * @author Joshua Karns (josh@karns.dev)
 */
public class Pipeline implements AutoCloseable {
    private static Logger LOG = Logger.getLogger(Pipeline.class.getName());

    private static ForkJoinPool pool = null;

    /**
     * Creates the thread pool used for flight file processing pipelines. By default, the number of threads created is
     * equal to the number of hardware threads that are available. This can be overridden with an environment variable.
     */
    private static void initialize() {
        int parallelism;

        try {
            parallelism = Integer.parseInt(System.getenv("PARALLELISM"));
        } catch (NullPointerException | NumberFormatException e) {
            parallelism = Runtime.getRuntime().availableProcessors();
        }

        if (parallelism <= 0)
            pool = new ForkJoinPool();
        else
            pool = new ForkJoinPool(parallelism);

        LOG.info("Created pool with " + parallelism + " threads");
    }

    private final Connection connection;

    // Whether processing was successfull. Processing is only to be considered unsuccessful from the perspective of this
    // pipeline if we encounter a SQL
    private boolean successfull = true;

    private final Upload upload;
    private final ZipFile zipFile;

    // ZipFileSystem
    private FileSystem derivedFileSystem = null;
    private Upload derivedUpload = null;

    // Maps lowercase file extension to the relevent FileProcessor. In the future, these may themselves have to do some
    // additional delegation if a single file extension may actually map to multiple significantly different schemas.
    private static final Map<String, FlightFileProcessor.Factory> FACTORIES = Map.of(
            "csv", CSVFileProcessor::factory,
            "dat", DATFileProcessor::new,
            "json", JSONFileProcessor::new,
            "gpx", GPXFileProcessor::new);

    // Used to count the number of valid flights. Atomic to enable concurrent mutation.
    private final AtomicInteger validFlightsCount = new AtomicInteger(0);

    // Same story as above but for warning flights.
    private final AtomicInteger warningFlightsCount = new AtomicInteger(0);

    // Maps filenames to the upload exception that caused processing of that flight to fail.
    private final ConcurrentHashMap<String, UploadException> flightErrors = new ConcurrentHashMap<>();

    // Maps filenames to a FlightInfo object.
    private final ConcurrentHashMap<String, ProcessUpload.FlightInfo> flightInfo = new ConcurrentHashMap<>();

    public Pipeline(Connection connection, Upload upload, ZipFile zipFile) {
        this.connection = connection;
        this.upload = upload;
        this.zipFile = zipFile;
    }

    /**
     * Closes the derivedFileSystem if there is one.
     */
    @Override
    public void close() throws IOException {
        if (derivedFileSystem != null)
            derivedFileSystem.close();
    }

    /**
     * Executes the flight processing pipeline. This chains together and parallelizes a few steps:
     * (1) Dispatch / create the appropriate FlightFileProcessor for each file.
     * (2) Parse the file, and filter out failures.
     * (3) Build the flights - apply all necessary processing steps (see FlightBuilder::build).
     * (4) Insert the created flights into the database.
     */
    public void execute() {
        if (Pipeline.pool == null)
            initialize();

        LOG.info("Creating pipeline to process upload id " + upload.id + " / " + upload.filename);

        var processHandle = pool.submit(() -> this.getValidFilesStream()
                .parallel()
                .forEach((ZipEntry ze) -> {
                    try (Connection connection = Database.getConnection()) {
                        List<FlightBuilder> fbuilders = this
                                .parse(this.create(ze))
                                .parallel()
                                .filter(Objects::nonNull)
                                .map(fbs -> this.build(connection, fbs))
                                .filter(Objects::nonNull)
                                .map(this::finalize)
                                .toList();

                        if (fbuilders.isEmpty())
                            return;

                        List<Flight> flights = fbuilders.stream().map(FlightBuilder::getFlight).toList();
                        Flight.batchUpdateDatabase(connection, upload, flights);
                        for (FlightBuilder builder : fbuilders) {
                            Event.batchInsertion(connection, builder.getFlight(), builder.getEvents());
                            TurnToFinal.cacheTurnToFinal(connection, builder.getFlight().getId(), builder.getTurnToFinals());
                        }
                    } catch (SQLException | IOException e) {
                        LOG.info("Encountered SQLException trying to get database connection...");
                        this.fail(ze.getName(), e);
                    }
                }));
        LOG.info("Executing...");
        processHandle.join();
        LOG.info("Joined.");
    }

    /**
     * Creates a stream of zip entires in the ZipFile that are in fact, files.
     *
     * @return stream of `ZipEntry`s
     */
    private Stream<? extends ZipEntry> getValidFilesStream() {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        return StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(entries.asIterator(), Spliterator.ORDERED),
                        false)
                .filter(z -> !z.getName().contains("__MACOSX"))
                .filter(z -> !z.isDirectory());
    }

    /**
     * Creates a FlightFileProcessor for the given entry if possible. The type of FlightFileProcessor is depdendent on
     * the file extension, and is dispatched using `this.factories`.
     *
     * @param entry The zip entry to create a FlightFileProcessor for.
     * @return A FlightFileProcessor if the file extension is supported, otherwise `null`.
     */
    private FlightFileProcessor create(ZipEntry entry) {
        String filename = entry.getName();

        int index = filename.lastIndexOf('.');
        String extension = index >= 0 ? filename.substring(index + 1).toLowerCase() : "";
        FlightFileProcessor.Factory f = FACTORIES.get(extension);

        if (f != null) {
            try (InputStream is = zipFile.getInputStream(entry)) {
                return f.create(connection, is, filename, this);
            } catch (Exception e) {
                fail(filename, new UploadException(e.getMessage(), e, filename));
            }
        } else {
            fail(filename,
                    new UploadException("Unknown file type '" + extension + "' contained in zip file.", filename));
        }

        return null;
    }

    /**
     * Calls the `parse` method on `processor`, returning the resulting stream of FlightBuilder objects. In the event of
     * an error, the error is logged using `this::fail` and and empty stream is returned.
     *
     * @returns A stream of flight builders on success, an empty stream if there is an error.
     */
    public Stream<FlightBuilder> parse(FlightFileProcessor processor) {
        try {
            if (processor == null)
                return Stream.of();
            return processor.parse();
        } catch (FlightProcessingException e) {
            fail(processor.filename, e);
            return Stream.of();
        }
    }

    /**
     * Calls `FlightBuilder::build` on the supplied flight builder and returns the resulting flight.
     *
     * @returns a flight object if there are no exceptions, otherwise returns `null`.
     */
    public FlightBuilder build(Connection connection, FlightBuilder fb) {
        try {
            fb.meta.setFleetId(this.upload.fleetId);
            return fb.build(connection);
        } catch (FlightProcessingException e) {
            LOG.info("Encountered an irrecoverable issue processing a flight");
            fail(fb.meta.filename, new UploadException(e.getMessage(), e, fb.meta.filename));
            return null;
        }
    }

    /**
     * Calls `this::build` on each `FlightBuilder` in the supplied stream.
     *
     * @returns a list of `Flight` objects, having filtered out any `null` values.
     */
    public List<FlightBuilder> build(Connection connection, Stream<FlightBuilder> fbs) {
        return fbs.map(fb -> this.build(connection, fb)).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * Tabulates various flight information.
     */
    public FlightBuilder finalize(FlightBuilder builder) {
        Flight flight = builder.getFlight();

        if (flight.getStatus().equals("WARNING")) {
            warningFlightsCount.incrementAndGet();
        } else {
            validFlightsCount.incrementAndGet();
        }

        LOG.info("FLIGHT STATUS = " + flight.getStatus());

        flightInfo.put(flight.getFilename(),
                new ProcessUpload.FlightInfo(flight.getId(), flight.getNumberRows(), flight.getFilename(), flight.getExceptions()));

        return builder;
    }

    /**
     * Add an upload exception to the flightError map for the given filename.
     */
    public void fail(String filename, Exception e) {
        LOG.info("Encountered an irrecoverable issue processing a flight");
        e.printStackTrace();
        this.flightErrors.put(filename, new UploadException(e.getMessage(), e, filename));
    }

    /**
     * Adds a file with the supplied filename containing the supplied data to the derived ZipFile. If the derived
     * ZipFile and upload have not been created yet, create them.
     * <p>
     * This method is synchronized so there is only a single thread mutating the derivedFileSystem at once.
     */
    public synchronized void addDerivedFile(String filename, byte[] data) throws IOException, SQLException {
        if (derivedFileSystem == null) {
            derivedUpload = Upload.createDerivedUpload(connection, upload);
            derivedFileSystem = derivedUpload.getZipFileSystem(Map.of("create", "true"));
        }

        Path zipFileSystemPath = derivedFileSystem.getPath(filename);
        Path parentPath = zipFileSystemPath.getParent();

        if (parentPath != null && !Files.exists(parentPath)) {
            Files.createDirectories(parentPath);
        }

        Files.write(zipFileSystemPath, data, StandardOpenOption.CREATE);
    }

    public Map<String, ProcessUpload.FlightInfo> getFlightInfo() {
        return flightInfo;
    }

    public Map<String, UploadException> getFlightErrors() {
        return Collections.unmodifiableMap(flightErrors);
    }

    public int getWarningFlightsCount() {
        return warningFlightsCount.get();
    }

    public int getValidFlightsCount() {
        return validFlightsCount.get();
    }

    public int getDerivedUploadId() {
        return derivedUpload.id;
    }

    public Upload getUpload() {
        return upload;
    }
}
