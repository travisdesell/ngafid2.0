# NGAFID Wishlist

The following is a list of relatively large changes that could greatly improve the website.

## Rework Frontend to Server Webpages Directly

The way webpages are fetched right now is, quick simply, awful. There are numerous nearly identical templates that provide nearly no value beyond injecting javascript values.
These javascript values should be fetched asychronously anyways and be replaced with an API. There is no need for a templating engine in the backend as we can simply replace it
entirely with a javascript webapplication thing like Vite.

Then, `ngafid-www` would just contain API hits to fetch data. `ngafid-frontend` would contain an actual web application.

This has some implications for development flow. If the frontend is disjoint from the API, we can hit ngafid.org with api calls while developing the frontend locally. This would
help to alleviate the longstanding issue of chnages tested locally breaking on the actual website.

We would need to allow some sort of configuration file for these things to be changed at a whim (see: Replace Environment Variables with Configuration File). The web application (e.g. vite)
would have to read this config in order to know where to send API calls (e.g. locally or to ngafid.org/api).

In pursuing this there are also some other things that will naturally be dealt with, e.g. right now webpage size is absurd because we package everything into every javascript bundle.

This is also a good opportunity to normalize all rouates and parameters. API routes should not use url parameters, only webapplication routes.

In pursuing this `ngafid-www` will need to largely be rewritten, I suggest doing so in Kotlin as Javalin was developed with Kotlin in mind. It should save some significant number of lines of code.


## Replace Environment Variables with Configuration File

The NGAFID uses environment variables for all sorts of things. This ends up being a big pain in the ass, though, mainly because it can't be version controlled effectively.
A simple java `properties` file would suffice (this is also what is used by liquibase etc., better to not introduce too many formats).

## Extensive Unit Tests

The NGAFID has a pitiful number of unit tests. Introducing unit tests with a high degree of branch coverage would alleviate the all-too-common regressions that happen, particularly on the backend.

The difficulty of doing this before had to do with the database -- there was no database available during testing and many functions operate on a database connection. Using liquibase, we can clone
the database schema to a temporary in-memory H2 database and even insert fake data. This unlocks the door to writing tests for functions that oerate on the database.

IntelliJ has excellent tooling for computing test coverage, it would be a good place to start. Once a large number of tests have been introduced, a GitHub action to enforce test coverage could
be introduced.
