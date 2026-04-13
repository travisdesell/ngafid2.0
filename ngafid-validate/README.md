# ngafid-validate

Comprehensive startup validation for NGAFID. This document defines a fail-loud preflight that runs during Docker startup and blocks service boot when required dependencies are not ready.

## Goal

When starting the stack with `docker compose up`, validation should:

- Run automatically before long-running NGAFID services start.
- Check configuration, filesystem, database, and Kafka readiness.
- Exit non-zero on any failed required check.
- Print actionable error output so operators can immediately fix the issue.

The same checks should also be runnable manually for local troubleshooting.

## Startup Model

Validation is intended to run as a one-shot preflight service in Compose.

Current implementation files in this folder:

- `ngafid-validate/validator.py`
- `ngafid-validate/Dockerfile`
- `ngafid-validate/requirements.txt`
- `ngafid-validate/validation-scripts/*.py`

This service is wired into `docker-compose.yml` as `ngafid-validate`.

### Check Script Auto-Discovery

The validator automatically discovers and runs all Python files in:

- `ngafid-validate/validation-scripts`

Contract for each check script:

- File must end in `.py` and not start with `_`.
- File must define `run_check(validator)`.
- No manual registration is required in `validator.py`.

Execution order is filename-sorted. Prefixes like `01_`, `02_`, etc. are recommended for stable ordering.

### Compose Pattern

1. Define a dedicated `ngafid-validate` service.
2. Have it depend on infrastructure services (`mysql`, `kafka`) and topic creation (`ngafid-kafka-topics`).
3. Gate application services (`ngafid-www`, consumers, observers) with:

```yaml
depends_on:
	ngafid-validate:
		condition: service_completed_successfully
```

This keeps validation centralized and ensures startup fails early when preconditions are not met.

### Commands

Automatic startup validation:

```bash
docker compose up
```

Manual validation only:

```bash
docker compose run --rm ngafid-validate
```

Manual validation with optional build artifact skip:

```bash
docker compose run --rm ngafid-validate --skip-build-artifacts
```

### Validation Result Logs

Every validator run writes a log file to:

- `ngafid-validate/validation-results/`

Naming format:

- `validationlog_YYYYMMDD_HHMMSS_PASS.log`
- `validationlog_YYYYMMDD_HHMMSS_FAIL.log`

You can override the output directory with:

- `--results-dir <path>`
- `VALIDATION_RESULTS_DIR=<path>`

## Validation Scope

Validation should be grouped into these categories.

### 1) Container and Compose Preconditions

- Confirm required Compose mounts exist and are readable:
  - `./ngafid.properties` -> `/app/ngafid.properties`
  - `./ngafid-db/src/liquibase.docker.properties` -> `/etc/ngafid-db.conf`
  - `./email_info.txt` -> `/etc/ngafid-email.conf`
  - Static assets mount (`./ngafid-static` in the default dev compose file)
- Confirm data mounts are present for uploads/archive/terrain and match the active deployment configuration.
  - In local dev, these are often repo-local paths (for example `./data/...`).
  - In non-dev environments, these are typically external host paths defined by the operator.
  - Validation should use configured mount sources from the active Compose/configuration, not hardcoded host paths.
- Confirm expected host aliases are available (`host.docker.internal` mapping in Compose).
- Confirm required environment assumptions for startup order are present.

Source of truth:

- `docker-compose.yml`

### 2) Build Artifact Preconditions

Before Compose startup, the Java artifacts needed by Docker images should exist.

Required artifacts:

- `ngafid-core/target/ngafid-core-1.0-SNAPSHOT-jar-with-dependencies.jar`
- `ngafid-www/target/ngafid-www-1.0-SNAPSHOT-jar-with-dependencies.jar`
- `ngafid-data-processor/target/ngafid-data-processor-1.0-SNAPSHOT-jar-with-dependencies.jar`

These are produced by:

- `run/package`

Source of truth:

- `run/package`
- module Dockerfiles and root `Dockerfile`

### 3) Properties and Configuration Validation

Validate required files exist and contain expected key settings.

Required files:

- `ngafid.properties`
- `ngafid-db/src/liquibase.docker.properties`
- `logging.properties`
- `email_info.txt` (required only when email is enabled)

Required `ngafid.properties` keys (minimum):

- `ngafid.repo.path`
- `ngafid.data.folder`
- `ngafid.upload.dir`
- `ngafid.archive.dir`
- `ngafid.terrain.dir`
- `ngafid.db.info` and Docker variant
- `ngafid.kafka.bootstrap.servers` and Docker variant
- `ngafid.port`

Fail conditions:

- Missing file.
- Missing required property key.
- Placeholder value left in required runtime fields where feature is enabled.

Source of truth:

- `ngafid.properties`

### 4) Filesystem and Data Validation

Validate data paths used by processing code are present and accessible.

Required checks:

- Effective upload path exists and is writable.
- Effective archive path exists and is writable.
- Effective terrain path exists and contains required tile subdirectories.
- Airports and runways CSV files exist at effective configured paths.

Path resolution rules:

- Resolve paths from active runtime configuration first (properties + Compose mounts).
- Treat host paths as user-defined deployment inputs.
- Do not assume a specific host root such as project-local `./data` or `/mnt/ngafid/...` unless explicitly configured by the user.

Fail conditions:

- Missing directory/file.
- Permission denied for expected write path.

Source of truth:

- `docker-compose.yml`
- `ngafid.properties` path settings

### 5) Database Validation

Validate MySQL connectivity and schema readiness.

Required checks:

- Parse DB credentials from `liquibase.docker.properties`.
- Connect to DB using configured URL/user/password.
- Confirm target database exists (`ngafid`).
- Confirm core schema tables exist (at minimum Liquibase tracking tables and a few application tables).

Fail conditions:

- Cannot connect to DB.
- Authentication failure.
- Database missing.
- Schema not initialized.

Source of truth:

- `ngafid-db/src/liquibase.docker.properties`
- `ngafid-db/src/db/changelog/`

### 6) Kafka Validation

Validate broker readiness and topic availability.

Required checks:

- Connect to Kafka bootstrap server in docker mode (`kafka:9093`).
- Confirm required topics exist:
  - `upload`, `upload-retry`, `upload-dlq`
  - `email`, `email-retry`, `email-dlq`
  - `event`, `event-retry`, `event-dlq`
- Confirm topic creation pre-step completed (`ngafid-kafka-topics`).

Fail conditions:

- Broker unreachable.
- Required topic missing.

Source of truth:

- `docker-compose.yml`
- `run/kafka/create_topics`
- `DESIGN.md`

### 7) Optional Subsystem Validation

Run only when feature is enabled.

Email enabled checks:

- `email_info.txt` is present and parseable.
- SMTP host/port config is present.

Chart service enabled checks:

- Chart endpoint config is set (`ngafid.chart.tile.base.url`).
- Chart service dependencies are installed in the target runtime.

Source of truth:

- `ngafid.properties`
- `ngafid-chart-processor/README.md`

## Fail-Loud Behavior

Validation must fail loudly and predictably.

Required behavior:

- Exit code `0` only when all required checks pass.
- Exit code non-zero if any required check fails.
- Emit a concise summary and per-failure diagnostics.

Recommended output format:

```
[PASS] CONFIG: ngafid.properties loaded
[PASS] DB: mysql connection established
[FAIL] FS: <resolved-terrain-path> missing required tile directories

Validation failed: 1 required check failed.
Action: verify terrain mount and ngafid.terrain.dir configuration.
```

## Manual Validation Mode

Manual mode must run the same checks as startup preflight.

Manual mode should be available to:

- Reproduce failures outside Compose.
- Validate fixes before restarting services.
- Support local development debugging.

Expected manual behavior:

- Same pass/fail rules as automatic mode.
- Same error messages and remediation hints.

## Suggested Check Order

Use this order so failures are reported as early as possible.

1. File and property presence checks.
2. Filesystem permissions and data assets.
3. Database connectivity and schema checks.
4. Kafka connectivity and topic checks.
5. Optional subsystem checks.

## Common Failure Cases

### Missing `ngafid.properties`

Symptoms:

- Startup preflight fails in configuration stage.

Fix:

1. Ensure `ngafid.properties` exists at repository root.
2. Fill required local/docker values.

### Wrong DB host in Docker config

Symptoms:

- DB connection refused during preflight.

Fix:

1. Verify `ngafid-db/src/liquibase.docker.properties` URL.
2. Use docker-reachable host value expected by deployment.

### Kafka broker reachable but topics missing

Symptoms:

- Kafka check fails on topic existence.

Fix:

1. Ensure topic creation step runs before service startup.
2. Re-run topic creation flow and restart preflight.

### Terrain/data mounts missing

Symptoms:

- Filesystem checks fail for uploads/archive/terrain.

Fix:

1. Create missing directories.
2. Verify active volume mappings in deployment configuration (for example Compose overrides or environment-specific manifests).
3. Confirm permissions for container user.
4. Confirm resolved runtime paths match user-configured destinations for uploads/archive/terrain.

### Non-dev external data root misconfigured

Symptoms:

- Validation fails even though the repository `data` folder exists.

Cause:

- Runtime is configured to use external host paths, but one or more configured paths are missing, inaccessible, or mismapped.

Fix:

1. Inspect active deployment config and identify effective host-to-container data mounts.
2. Ensure the user-defined host paths exist and contain expected content.
3. Re-run validation and confirm checks pass against resolved runtime paths.

## Implementation Notes

- This README is the validation contract for startup behavior.
- As validation code evolves, keep this document in sync with the implemented checks.
- Prefer adding new checks in a strict, categorized way and document each check with pass/fail criteria.
