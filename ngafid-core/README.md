# NGAFID Core

This core module contains the core components used by the webserver and data processor. More or less, this module acts
as an object relational mapping (ORM) for the database. There are also a few useful utilities and binary scripts.

## Running the tests

The tests run against a real MySQL database that is started automatically with
[Testcontainers](https://testcontainers.com/) (see `src/test/java/org/ngafid/core/TestDatabase.java`). The production
Liquibase changelog is applied to that throwaway container, so the test schema matches production exactly. **A running
Docker engine is therefore required** to run `mvn test`.

On a standard Docker install (Linux, or CI) this works with no extra configuration.

### Potential Docker Desktop issue (local macOS)

Recent Docker Desktop releases expose a proxied socket and ship a daemon whose minimum API version is newer than the
one Testcontainers' bundled Docker client negotiates by default. On some setups this makes `mvn test` fail before any
test runs, with an error such as `Could not find a valid Docker environment` or
`client version 1.xx is too old. Minimum supported API version is 1.40`.

If you hit this, create a `~/.testcontainers.properties` file that points Testcontainers at the Docker Desktop socket:

```properties
docker.host=unix:///Users/<you>/.docker/run/docker.sock
ryuk.disabled=true
```

- Use `unix:///Users/<you>/Library/Containers/com.docker.docker/Data/docker.raw.sock` if the `~/.docker/run` socket is
  not present.
- `ryuk.disabled=true` skips the Ryuk reaper container, whose socket bind-mount can fail under Docker Desktop's proxied
  socket; test containers are still stopped when the JVM exits.

If the `client version ... is too old` error persists after that, also pin the Docker API version to your daemon's when
running the tests:

```bash
mvn -pl ngafid-core test -DargLine="-Dapi.version=$(docker version --format '{{.Server.APIVersion}}')"
```
