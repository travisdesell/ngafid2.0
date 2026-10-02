# NGAFID Core

This core module contains the core components used by the webserver and data processor. More or less, this module acts
as an object relational mapping (ORM) for the database. There are also a few useful utilities and binary scripts.

## Running the tests

The tests run against a real MySQL database that is started automatically with
[Testcontainers](https://testcontainers.com/) (see `src/test/java/org/ngafid/core/TestDatabase.java`). The production
Liquibase changelog is applied to that throwaway container, so the test schema matches production exactly. **A running
Docker engine is therefore required** to run `mvn test`.

On a standard Docker install (Linux, or CI) this works with no extra configuration.

### Docker Desktop quirk (local macOS/Windows)

Recent Docker Desktop releases expose a socket proxy and ship a daemon whose minimum API version is newer than the one
Testcontainers' bundled Docker client negotiates by default. When that happens, `mvn test` fails before any test runs
with an error such as:

- `Could not find a valid Docker environment`, or
- `client version 1.xx is too old. Minimum supported API version is 1.40`

If you hit this, point Testcontainers at the Docker Desktop socket and pin the Docker API version to your daemon's:

```bash
# Use your daemon's API version (find it with the command below):
API_VERSION=$(docker version --format '{{.Server.APIVersion}}')

DOCKER_HOST="unix://$HOME/.docker/run/docker.sock" \
TESTCONTAINERS_RYUK_DISABLED=true \
  mvn -pl ngafid-core test -DargLine="-Dapi.version=$API_VERSION"
```

Notes:

- `DOCKER_HOST` can also point at `unix://$HOME/Library/Containers/com.docker.docker/Data/docker.raw.sock` if the
  `~/.docker/run/docker.sock` path is not present.
- `TESTCONTAINERS_RYUK_DISABLED=true` skips the Ryuk reaper container, whose socket bind-mount can fail under Docker
  Desktop's custom socket; containers are still cleaned up when the JVM exits.
- To make this permanent for your machine, put the equivalent settings in `~/.testcontainers.properties`
  (`docker.host=...`, `ryuk.disabled=true`) rather than exporting them each time.
