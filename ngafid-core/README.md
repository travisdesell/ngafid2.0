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

The most reliable way to apply these on a Docker Desktop machine is to export them in the shell you run the tests from,
rather than relying on the properties file alone — on some setups the file's `ryuk.disabled` is not honored and Ryuk
still tries (and fails) to start:

```bash
export DOCKER_HOST=unix:///Users/<you>/.docker/run/docker.sock
export TESTCONTAINERS_RYUK_DISABLED=true
mvn -pl ngafid-core test -DargLine="-Dapi.version=$(docker version --format '{{.Server.APIVersion}}')"
```

- The `-Dapi.version=...` argument pins the Docker API version to your daemon's, which resolves the
  `client version ... is too old` error. It uses command substitution, so the `docker` CLI must be on your `PATH`;
  Docker Desktop does not always add it to non-login shells. If `docker version` is not found, add Docker's bin
  directory (for example `~/.docker/bin` or `/Applications/Docker.app/Contents/Resources/bin`) to your `PATH` first.
- `TESTCONTAINERS_RYUK_DISABLED=true` is the environment-variable form of `ryuk.disabled`; prefer it when the
  `~/.testcontainers.properties` entry is not taking effect.
