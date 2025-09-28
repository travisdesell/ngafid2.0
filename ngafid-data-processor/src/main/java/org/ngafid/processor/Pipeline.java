package org.ngafid.processor;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.ngafid.core.Config;
import org.ngafid.core.flights.Airframes;
import org.ngafid.core.flights.FatalFlightFileException;
import org.ngafid.core.flights.Flight;
import org.ngafid.core.flights.FlightProcessingException;
import org.ngafid.core.uploads.Upload;
import org.ngafid.core.uploads.UploadException;
import org.ngafid.processor.format.*;
import org.ngafid.processor.steps.ComputeStep;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ClosedChannelException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Primary entry point for interacting with the org.ngafid.flights.process package.
 * This wraps up the process of (1) recognizing file types, (2) parsing the files, and (3) processing the data.
 * <p>
 * Files are recognized by their file extensions - the extension is used to create a `FlightFileProcessor` from the list
 * of factories in `this.factories`. We may need to read some data within the file to properly determine how it should be
 * processed, which factories may do -- see the factory function in {@link CSVFileProcessor} for an example of this.
 * <p>
 * {@link FlightFileProcessor} objects handle the task of parsing the data: obtaining metadata and the actual double and
 * string series. These are placed into a {@link FlightBuilder}, which is an intermediate representation of a partially
 * processed flight.
 * <p>
 * {@link FlightBuilder}s can be specialized and these specializations will specify a set of {@link ComputeStep}s
 * which will be applied to compute everything we need. ComputeSteps all have required input columns and output columns: these
 * requirements can be used to form a DAG, traversing it in the proper order will let us compute the steps in the proper
 * order. This process can also be parallelized, see {@link org.ngafid.uploads.process.DependencyGraph} for more on this.
 * <p>
 * This object may open resources, thus it must be created using a try-with statement to ensure they are closed properly.
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
        int parallelism = Config.PARALLELISM;

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
    private ZipArchiveOutputStream derivedArchive = null;
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
        if (derivedArchive != null) {
            derivedArchive.finish();
            derivedArchive.close();
        }
    }

    /**
     * Executes the flight processing pipeline. This chains together and parallelizes a few steps:
     * (1) Dispatch / create the appropriate FlightFileProcessor for each file.
     * (2) Parse the file, and filter out failures.
     * (3) Build the flights - apply all necessary processing steps (see FlightBuilder::build).
     * (4) Insert the created flights into the database.
     */
    public void execute() {
        if (Config.MEMORY_EFFICIENT_UPLOAD_PROCESSOR)
            executeMemoryEfficient();
        else
            executeFast();
    }

    /**
     * Throws all tasks into a threadpool; this should minimize core downtime but will eat more memory.
     */
    private void executeFast() {
        if (Pipeline.pool == null)
            initialize();

        LOG.info("Creating pipeline to process upload id " + upload.id + " / " + upload.filename);

        var zipEntries = getValidFilesStream().toList();

        for (ZipArchiveEntry entry : zipEntries) {
            try {
                FlightFileProcessor fileProcessor = create(entry);
                if (fileProcessor != null) {
                    pool.submit(fileProcessor);
                }
            } catch (SQLException | IOException | FatalFlightFileException e) {
                fail(entry.getName(), e);
            }
        }

        // FlightFileProcessor forks some tasks off without explicitly waiting for them to do async IO.
        // We need to wait until these tasks have completed.
        while (!pool.awaitQuiescence(10, TimeUnit.SECONDS)) {
            LOG.info("Waiting for pool quiescence...");
        }

        LOG.info("Done...");
    }

    /**
     * Only executes up to PARALLELISM tasks at a time, ensuring we only read up to that many files from the zip into
     * memory at any one time.
     */
    private void executeMemoryEfficient() {
        var taskQueue = new ArrayBlockingQueue<Object>(Config.PARALLELISM);
        var zipEntries = getValidFilesStream().toList();

        new Thread(() -> {
            for (var entry : zipEntries) {
                try {
                    FlightFileProcessor fileProcessor = create(entry);
                    if (fileProcessor != null) {
                        while (true) {
                            try {
                                taskQueue.put(fileProcessor);
                                break;
                            } catch (InterruptedException e) {
                                // Ignore...
                            }
                        }
                    }
                } catch (SQLException | FatalFlightFileException e) {
                    fail(entry.getName(), e);
                } catch (IOException e) {
                    // An IOException at this point in time probably means something is wrong with the zip file, it may
                    // be broken. Just give up.
                    e.printStackTrace();
                    fail(entry.getName(), e);
                    break;
                }
            }
            for (int i = 0; i < Config.PARALLELISM; i++) {
                while (true) {
                    try {
                        taskQueue.put(new Object());
                        break;
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
            }
        }).start();

        var handles = new ArrayList<Thread>();
        for (int i = 0; i < Config.PARALLELISM; i++) {
            var worker = new Thread(() -> {
                while (true) {
                    try {
                        var task = taskQueue.take();
                        if (task instanceof FlightFileProcessor proc) {
                            proc.call();
                        } else {
                            break;
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            worker.start();
            handles.add(i, worker);
        }

        for (var handle : handles) {
            while (true) {
                try {
                    handle.join();
                    break;
                } catch (InterruptedException e) {
                }
            }
        }
    }

    /**
     * Creates a stream of zip entires in the ZipFile that are in fact, files.
     *
     * @return stream of `ZipEntry`s
     */
    private Stream<ZipArchiveEntry> getValidFilesStream() {
        var entries = zipFile.getEntries();
        return StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(entries.asIterator(), Spliterator.ORDERED),
                        false)
                .filter(z -> !z.getName().contains("__MACOSX"))
                .filter(z -> !z.isDirectory());
    }

    // private Stream<FlightFileProcessor> getFlightFileProcessorStream() {
    //
    // }

    /**
     * Creates a FlightFileProcessor for the given entry if possible. The type of FlightFileProcessor is depdendent on
     * the file extension, and is dispatched using `this.factories`.
     *
     * @param entry The zip entry to create a FlightFileProcessor for.
     * @return A FlightFileProcessor if the file extension is supported, otherwise `null`.
     * @throws Exception TODO this should be a specific set of exceptions.
     */
    private FlightFileProcessor create(ZipArchiveEntry entry) throws IOException, SQLException, FatalFlightFileException {
        String filename = entry.getName();
        LOG.fine("Creating flight builder for file: '" + filename + "'");

        int index = filename.lastIndexOf('.');
        String extension = index >= 0 ? filename.substring(index + 1).toLowerCase() : "";
        FlightFileProcessor.Factory f = FACTORIES.get(extension);

        if (f != null) {
            try (InputStream is = zipFile.getInputStream(entry)) {
                return f.create(connection, is, filename, this);
            }
        } else {
            fail(filename,
                    new UploadException("Unknown file type '" + extension + "' contained in zip file.", filename));
        }

        return null;
    }

    /**
     * Calls the `parse` method on `processor`, returning the resulting stream of FlightBuilder objects. In the event of
     * an error, the error is logged using `this::fail` and an empty stream is returned.
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
            LOG.info("Building flight file '" + fb.meta.filename + "'");
            fb.meta.setFleetId(this.upload.fleetId);
            fb.meta.setUploaderId(this.upload.uploaderId);
            fb.meta.setUploadId(this.upload.id);
            fb.meta.airframe = new Airframes.Airframe(connection, fb.meta.airframe.getName(), fb.meta.airframe.getType());
            return fb.build(connection);
        } catch (FlightProcessingException | SQLException e) {
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
        LOG.info("Finalizing flight file '" + flight.getFilename() + "'");

        if (flight.getStatus() == Flight.FlightStatus.WARNING) {
            warningFlightsCount.incrementAndGet();
        } else {
            validFlightsCount.incrementAndGet();
        }

        flightInfo.put(flight.getFilename(),
                new ProcessUpload.FlightInfo(flight.getId(), flight.getNumberRows(), flight.getFilename(), flight.getExceptions()));

        return builder;
    }

    /**
     * Add an upload exception to the flightError map for the given filename.
     */
    public void fail(String filename, Exception e) {
        if (!(e instanceof ClosedChannelException)) {
            LOG.info("Encountered an irrecoverable issue processing flight file '" + filename + "'");
            e.printStackTrace();
        }
        this.flightErrors.put(filename, new UploadException(e.getMessage(), e, filename));
    }

    /**
     * Adds a file with the supplied filename containing the supplied data to the derived ZipFile. If the derived
     * ZipFile and upload have not been created yet, create them.
     * <p>
     * This method is synchronized so there is only a single thread mutating the derivedFileSystem at once.
     */
    public synchronized void addDerivedFile(File convertedOnDisk, String filename, byte[] data) throws IOException, SQLException {
        if (derivedArchive == null) {
            derivedUpload = Upload.createDerivedUpload(connection, upload);
            derivedArchive = derivedUpload.getArchiveOutputStream();
        }

        try (InputStream bis = new ByteArrayInputStream(data)) {
            var entry = new ZipArchiveEntry(convertedOnDisk, filename);
            entry.setMethod(ZipArchiveEntry.DEFLATED);
            derivedArchive.addRawArchiveEntry(entry, bis);
        }
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
