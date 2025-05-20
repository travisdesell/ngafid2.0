# NGAFID System Design

The NGAFID's design has moved towards that of a standard web application: several services with discrete roles which all
operate on a shared database, in addition to the javascript frontend. These services communicate with one another using
Kafka.

The role of each service will be described, then their interactions will be described.

## NGAFID Database

The core of the NGAFID is of course then the database located in `ngafid-db`. The schema is written in MySQL dialect
SQL,
though some syntax has been modified to be H2 compliant for testing purposes. There is nothing particularly remarkable
the schema, though it is worth mentioning the generous use of `ON DELETE CASCADE` which serves to alleviate code bloat.
Before employing this, there was a significant amount of code dedicated to deleting hierarchically laid out data in the
database which can of course be deleted in one fell swoop (from the programmers perspective).

Liquibase is the name of the tool used to manage and update the schema. Liquibase is quite powerful and only a fraction
of its capabilities are being used, but the key capabilities it provides us are:

- Version tracking of the database
- Automatic deployment of changes
- Schema replication during testing
- Ability to run analytical queries on-demand, used to compute statistics hourly and daily

The last bullet refers to what could be considered two discrete services: `ngafid-daily-materialized-views` and
`ngafid-hourly-materialized-views`.

It is worth reading about the general workflow of Liquibase before messing with anything.

## NGAFID AirSync Service

The AirSync service `ngafid-airsync` reads data from AirSync servers and bundles it into synthetic uploads. These
uploads are processed
by the upload daemon as per usual.

## NGAFID Upload Consumer

The upload consumer or data processor in `ngafid-data-processor` is where flight processing and analyses occur before
the resulting structured data is placed into the database. The upload consumer is designed to be highly
parallelizable --
archives can be processed in parallel at a per-file level as well as a per-processing step level (though the latter is
unlikely to see significant use and is turned off by default).

## NGAFID Webserver

The webserver located in `ngafid-www` is largely boilerplate code, simply fetching data requested of it, authentication,
etc. Most of the actual business logic is in `ngafid-core`. It serves static files, including the javascript generated
by the `ngafid-frontend`. All of these files are placed in `ngafid-static` -- note that `ngafid-frontend` places its
compiled javascript in the static directory.

## NGAFID Email Consumer

The email consumer does all the actual sending of email sending. The code is located in `ngafid-core`.

## NGAFID Event Consumer

Some event scans, namely proximity, can only be done after flight data has been inserted into the database -- this
service does that. It is located in `ngafid-data-processor`.

## NGAFID Event Observer

The database contains a table which should contain an entry for every processed flight and event definition id pair,
indicating that it has been computed (or queued for computation in the case of proximity). If this is ever found to not
be the case, this daemon will queue up the event for potential computation. Even if the event definition is not
applicable, an entry will be made in the table to indicate that an attempt was made.

This should also ease the task of re-computing an event if the definition has been modified. All one would have to do is
remove entries in the appropriate table and this daemon will automatically enqueue events for re-computation.

# Interactions

At its simplest, Kafka can be thought of as a message passing queue with some degree of persistence. The data is not
perfectly resilient, however there is no question that it is robust enough to hadnle the standard load of transient bugs
any webserver is sure to encounter.

Kafka maintains named queues referred to as *topics*. Consumers read from these queues, and producers add to the queues.
There are several topics with which the NGAFID sends messages:

- UPLOAD
- EMAIL
- EVENT

Each one of these queues also has an associated retry queue and so-called dead letter queue (DLQ). If processing a
message fails, it will be retried by placing it into the retry queue. If it fails again, it will be placed into the DLQ
indicating it may need manual review.

## Upload Topic

The webserver and airsync services both add to the upload topic. A user will upload data through the web interface, and
once the file has been completely received by the webserver a message to begin processing it will be added to the topic.
The upload consumer service will immediately begin processing this if idle, otherwise it will get to it eventually by
processing messages in FIFO order.

The process is a little different in the case of AirSync as the uploads are automatically generated. Once an AirSync zip
file has been finalized, the upload will be marked as uploaded and a message for processing will be added to the topic.

A key feature of Kafka topics that has not yet been mentioned is that of partitions. In the case of the NGAFID, it
simply
is used to split the upload topics into sub-topics. The details are quite technical, but the gist is this: only one
consumer can subscribe to a given partition at the same time, so in order to allow for the existence of multiple upload
consumers, 3 partitions are applied to the upload topic.

If only a single consumer is running, this is fine and the Kafka coordinator will provide messages from all three
partitions to the single consumer. If another consumer comes online randomly, the Kafka coordinator will rebalance
things.

## Email Topic

Most services act as producers for the email topic at some point, mostly to complain about errors. Of course the
webserver also will send important emails regarding account information, upload processing, etc.

The only consumer of this topic is the Email Consumer Service, which simply reads emails and sends them in batches. It
is
paramount that the email service remains active as much as possible, otherwise emails will simple pile up and be sent
en-masse upon
the resumption of the process. It may be a good idea to add an expiration date to send requests for this reason...

## Event Topic

The even topic is written to by the event observer and the upload consumer services, and is consumed only by the event
consumer service. These relationships are quite straightforward.