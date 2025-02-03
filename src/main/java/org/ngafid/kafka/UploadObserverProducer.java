package org.ngafid.kafka;

import java.util.logging.Logger;

/**
 * Kafka producer that watches the uploads table, looking for uploads that have completed processing.
 * <p>
 * When an upload with status 'UPLOADED' is detected, a message will be added to the `upload` topic for processing.
 */
public class UploadObserverProducer implements Runnable {
    private static final Logger LOG = Logger.getLogger(UploadObserverProducer.class.getName());

    @Override
    public void run() {
    }
}
