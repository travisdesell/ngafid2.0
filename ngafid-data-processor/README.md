# NGAFID Data Processor

This data processor package contains the code to read archives containing flight data and process the individual files
robustly. The primary entry point for this is in the `org.ngafid.processor.UploadConsumer` class, which contains code
for a Kafka consumer to read from `upload` and `upload-retry` topics. These topics contain upload IDs that are to be
processed -- this queue can be populated by the webserver when a user uploads a file (see `ngafid-www`) or by the
upload helper binary with which you can choose to add a single upload or group of uploads to the queue
(see `UploadHelper` in `ngafid-core`).

The primary entry point for the actual data processing is in `org.ngafid.processor.Pipeline` -- see it for further
design details.