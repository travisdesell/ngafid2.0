#!/usr/bin/env bash
#
# Unified unit-test runner for the NGAFID monorepo.
#
# Runs every module's unit tests so a developer can verify the whole repo is
# green before opening a PR -- the same tests CI runs. It is the test counterpart
# to scripts/lint.sh: one command, per-language targets, a --verbose switch, and
# a --report mode for CI summaries.
#
# Usage:
#   scripts/test.sh [--report] [--verbose] [--e2e] [--terrain] [--security] \
#       [--log-level=LEVEL] [all|java|kotlin|python|js]
#
#   --report    Never exit non-zero: run every suite, print counts, and (in CI)
#               append a summary to $GITHUB_STEP_SUMMARY. Without it, the script
#               exits non-zero if any suite fails, so it works as a pre-PR gate.
#   --verbose   (-v) Stream each test runner's full output instead of only its
#               summary.
#   --e2e       Also run the Selenium end-to-end tests (JUnit tag "e2e"), which
#               are skipped by default. They need a running NGAFID server and a
#               browser; start the server first (see CONTRIBUTING.md). Point them
#               at a non-default server with NGAFID_BASE_URL / -Dngafid.baseUrl.
#   --terrain   Also run the TerrainCache altitude tests (JUnit tag "terrain"),
#               which are skipped by default. They need the (large, out-of-repo)
#               SRTM terrain data and a repo-root ngafid.properties whose
#               ngafid.terrain.dir points at it (see CONTRIBUTING.md).
#   --security  Also run the standalone Gradle SQL-injection security-test project
#               under ngafid-www/src/test/security-test. It targets a running
#               server configured via its own .env (see CONTRIBUTING.md).
#   --log-level=LEVEL
#               Logging level for the Java/Kotlin test JVMs (default WARN, the
#               production default from resources/log.properties). Sets both
#               java.util.logging (NGAFID code) and slf4j-simple (third-party
#               libraries). LEVEL is case-insensitive: OFF, ERROR, WARN, INFO,
#               DEBUG, TRACE, or the JUL names SEVERE, WARNING, CONFIG, FINE,
#               FINER, FINEST, ALL. E.g. --log-level=DEBUG to see LOG.fine output.
#   target     Which suites to run (default: all):
#                 java       -> mvn test (ngafid-core, ngafid-www, ngafid-data-processor, ...)
#                 kotlin     -> alias for java (Kotlin tests run under Maven too)
#                 python     -> pytest (ngafid-pydata)
#                 js         -> Vitest (ngafid-frontend; npm test)
#
# A suite whose toolchain is missing is SKIPPED with a note (not failed), so the
# script is useful on a machine that has only some toolchains installed.
#
# Docker: the ngafid-core tests start a throwaway MySQL via Testcontainers, so a
# running Docker engine is required for the java suite. On a standard Docker
# install (Linux/CI) this works with no extra setup; on Docker Desktop this
# script auto-applies the socket/API-version workaround (see ngafid-core/README).

set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT" || exit 1

REPORT=0
VERBOSE=0
RUN_E2E=0
RUN_TERRAIN=0
RUN_SECURITY=0
LOG_LEVEL=""
TARGET="all"
USAGE="Usage: scripts/test.sh [--report] [--verbose] [--e2e] [--terrain] [--security] [--log-level=LEVEL] [all|java|kotlin|python|js]"
for arg in "$@"; do
    case "$arg" in
        --report) REPORT=1 ;;
        -v | --verbose) VERBOSE=1 ;;
        --e2e) RUN_E2E=1 ;;
        --terrain) RUN_TERRAIN=1 ;;
        --security) RUN_SECURITY=1 ;;
        --log-level=*) LOG_LEVEL="${arg#--log-level=}" ;;
        all | java | kotlin | python | js)
            TARGET="$arg"
            ;;
        *)
            echo "Unknown argument: $arg" >&2
            echo "$USAGE" >&2
            exit 2
            ;;
    esac
done

# Map --log-level onto the java.util.logging level (JUL_LEVEL, for NGAFID code) and the
# slf4j-simple level (SLF4J_LEVEL, for third-party libraries). Accepts the log4j/SLF4J names
# and the JUL names, case-insensitively; slf4j has no CONFIG level, so it maps to info.
JUL_LEVEL=""
SLF4J_LEVEL=""
if [[ -n "$LOG_LEVEL" ]]; then
    case "$(echo "$LOG_LEVEL" | tr '[:lower:]' '[:upper:]')" in
        OFF) JUL_LEVEL=OFF SLF4J_LEVEL=off ;;
        ERROR | SEVERE) JUL_LEVEL=SEVERE SLF4J_LEVEL=error ;;
        WARN | WARNING) JUL_LEVEL=WARNING SLF4J_LEVEL=warn ;;
        INFO) JUL_LEVEL=INFO SLF4J_LEVEL=info ;;
        CONFIG) JUL_LEVEL=CONFIG SLF4J_LEVEL=info ;;
        DEBUG | FINE) JUL_LEVEL=FINE SLF4J_LEVEL=debug ;;
        TRACE | FINER) JUL_LEVEL=FINER SLF4J_LEVEL=trace ;;
        FINEST) JUL_LEVEL=FINEST SLF4J_LEVEL=trace ;;
        ALL) JUL_LEVEL=ALL SLF4J_LEVEL=trace ;;
        *)
            echo "Unknown --log-level: $LOG_LEVEL (expected OFF, ERROR, WARN, INFO, DEBUG, TRACE," \
                "or a JUL level: SEVERE, WARNING, CONFIG, FINE, FINER, FINEST, ALL)" >&2
            exit 2
            ;;
    esac
fi

REPORTS_DIR="$ROOT/test-reports"
mkdir -p "$REPORTS_DIR"

FAILED=0

# Append a line to the GitHub Actions run summary when running in CI.
summary() {
    if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
        echo "$1" >>"$GITHUB_STEP_SUMMARY"
    fi
}

# Record a suite result, updating FAILED and printing/append-summarizing it.
#   $1 = label, $2 = status (OK|PROBLEMS|SKIP), $3 = detail, $4 = report file (optional)
# On PROBLEMS, also prints the tail of the report (unless --verbose already streamed
# it), so the failure is visible in the CI log without the report file itself.
record() {
    local label="$1" status="$2" detail="$3" report="${4:-}"
    echo "[$status] $label: $detail"
    summary "- **$label**: $status -- $detail"
    if [[ "$status" == "PROBLEMS" ]]; then
        FAILED=1
        if [[ "$VERBOSE" -eq 0 && -n "$report" && -f "$report" ]]; then
            echo "----- last 40 lines of $report -----"
            tail -n 40 "$report"
            echo "-----"
        fi
    fi
}

# Run a command, streaming its output when --verbose and otherwise capturing it to
# a report file. Sets RUN_STATUS to the command's exit code.
#   $1 = report file, $2.. = command
RUN_STATUS=0
run_capture() {
    local report="$1"
    shift
    if [[ "$VERBOSE" -eq 1 ]]; then
        "$@" 2>&1 | tee "$report"
        RUN_STATUS=${PIPESTATUS[0]}
    else
        "$@" >"$report" 2>&1
        RUN_STATUS=$?
    fi
}

# Extra Maven arguments needed to talk to the Docker daemon; populated for Docker
# Desktop by prepare_docker_env, empty otherwise.
DOCKER_MVN_ARGS=()

# Prepare the environment so Testcontainers (used by the ngafid-core tests) can
# reach the Docker daemon. On a standard Docker install this is a no-op. On Docker
# Desktop the bundled docker-java client negotiates an API version the daemon
# rejects and the proxied socket/Ryuk reaper can fail, so we point Testcontainers
# at the Docker Desktop socket, disable Ryuk, and pin the API version to the
# daemon's (passed to the forked test JVM via argLine; that overrides JaCoCo's
# argLine, so coverage is skipped in this local path only -- CI keeps coverage).
# Pre-set env vars are respected and never overwritten.
prepare_docker_env() {
    if ! command -v docker >/dev/null 2>&1; then
        # Docker Desktop does not always add its CLI to non-login shells; try the
        # usual install locations so command substitution below can work.
        local d
        for d in "$HOME/.docker/bin" /Applications/Docker.app/Contents/Resources/bin \
            /usr/local/bin /opt/homebrew/bin; do
            if [[ -x "$d/docker" ]]; then
                PATH="$d:$PATH"
                export PATH
                break
            fi
        done
    fi
    if ! command -v docker >/dev/null 2>&1; then
        echo "WARNING: docker CLI not found; the ngafid-core tests need a running Docker engine." >&2
        return 0
    fi

    local platform
    platform="$(docker version --format '{{.Server.Platform.Name}}' 2>/dev/null || true)"
    if [[ "$platform" != *"Docker Desktop"* ]]; then
        return 0 # standard Docker (Linux/CI): no workaround needed.
    fi

    if [[ -z "${DOCKER_HOST:-}" && -S "$HOME/.docker/run/docker.sock" ]]; then
        export DOCKER_HOST="unix://$HOME/.docker/run/docker.sock"
    fi
    export TESTCONTAINERS_RYUK_DISABLED="${TESTCONTAINERS_RYUK_DISABLED:-true}"

    local api
    api="$(docker version --format '{{.Server.APIVersion}}' 2>/dev/null || true)"
    if [[ -n "$api" ]]; then
        DOCKER_MVN_ARGS=(-DargLine="-Dapi.version=$api" -Djacoco.skip=true)
    fi
    echo "(Docker Desktop detected: DOCKER_HOST=${DOCKER_HOST:-<unset>}, Ryuk disabled," \
        "api.version=${api:-<unknown>}; JaCoCo coverage skipped for this run)"
}

# Sum surefire's per-module totals ("Tests run: N, Failures: F, Errors: E,
# Skipped: S" with nothing after). Echoes "run failures errors skipped".
sum_surefire() {
    grep -oE 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$' "$1" |
        awk -F'[ ,]+' '{run+=$3; fail+=$5; err+=$7; skip+=$9}
            END {printf "%d %d %d %d", run, fail, err, skip}'
}

test_java() {
    echo
    echo "=== Java/Kotlin (mvn test) ==="
    if ! command -v mvn >/dev/null 2>&1; then
        record "Java/Kotlin (mvn test)" "SKIP" "mvn not found (install Maven)"
        return
    fi
    prepare_docker_env

    local mvn_args=(-B -ntp test)
    # Append Docker args only when present (expanding an empty array trips `set -u` on bash < 4.4).
    if [[ ${#DOCKER_MVN_ARGS[@]} -gt 0 ]]; then
        mvn_args+=("${DOCKER_MVN_ARGS[@]}")
    fi
    if [[ -n "$JUL_LEVEL" ]]; then
        # Override the test JVMs' logging (the poms default to resources/log.properties + slf4j
        # warn). JUL has no level system property, so write a copy of the production config with
        # only the root .level replaced (keeping its handler/format) and point the tests at it.
        local log_config="$REPORTS_DIR/java-logging.properties"
        {
            grep -v '^[[:space:]]*\.level[[:space:]]*=' "$ROOT/resources/log.properties"
            echo ".level=$JUL_LEVEL"
        } >"$log_config"
        mvn_args+=(-Dngafid.test.log.config="$log_config" -Dngafid.test.slf4j.level="$SLF4J_LEVEL")
        echo "(--log-level: test logging at $JUL_LEVEL (java.util.logging) / $SLF4J_LEVEL (slf4j))"
    fi
    if [[ "$RUN_E2E" -eq 1 ]]; then
        # Clear ngafid-www's default e2e exclusion so the Selenium tests run too.
        mvn_args+=(-Dngafid.www.test.excludedGroups=)
        echo "(--e2e: including Selenium end-to-end tests; a running server + browser is required)"
    fi
    if [[ "$RUN_TERRAIN" -eq 1 ]]; then
        # Clear ngafid-data-processor's default terrain exclusion so the altitude tests run too,
        # and point them at the real SRTM terrain data (from $NGAFID_TERRAIN_DIR, else resolved
        # from ngafid.terrain.dir in the repo-root ngafid.properties).
        mvn_args+=(-Dngafid.dp.test.excludedGroups=)
        local terrain_dir="${NGAFID_TERRAIN_DIR:-}"
        if [[ -z "$terrain_dir" && -f "$ROOT/ngafid.properties" ]]; then
            local raw_dir data_folder
            raw_dir="$(grep -E '^ngafid\.terrain\.dir=' "$ROOT/ngafid.properties" | head -1 | cut -d= -f2-)"
            data_folder="$(grep -E '^ngafid\.data\.folder=' "$ROOT/ngafid.properties" | head -1 | cut -d= -f2-)"
            terrain_dir="${raw_dir/\$\{ngafid.data.folder\}/$data_folder}"
        fi
        if [[ -n "$terrain_dir" ]]; then
            mvn_args+=(-Dngafid.test.terrain.dir="$terrain_dir")
            echo "(--terrain: including TerrainCache altitude tests against terrain data at $terrain_dir)"
        else
            echo "(--terrain: including TerrainCache altitude tests, but no terrain dir resolved --" \
                "set NGAFID_TERRAIN_DIR or ngafid.terrain.dir in ./ngafid.properties, or they will fail)"
        fi
    fi

    local report="$REPORTS_DIR/java.txt"
    run_capture "$report" mvn "${mvn_args[@]}"
    local status=$RUN_STATUS

    local totals run fail err skip
    totals="$(sum_surefire "$report")"
    read -r run fail err skip <<<"$totals"
    local detail="${run:-0} run, ${fail:-0} failed, ${err:-0} errored, ${skip:-0} skipped"
    if [[ "$status" -eq 0 ]]; then
        record "Java/Kotlin (mvn test)" "OK" "$detail"
    else
        record "Java/Kotlin (mvn test)" "PROBLEMS" "$detail -- see $report" "$report"
    fi
}

test_python() {
    echo
    echo "=== Python (pytest) ==="
    local py=""
    if command -v pytest >/dev/null 2>&1; then
        py="pytest"
    elif python3 -m pytest --version >/dev/null 2>&1; then
        py="python3 -m pytest"
    else
        record "Python (pytest)" "SKIP" "pytest not found (pip install -e 'ngafid-pydata[dev]')"
        return
    fi
    if [[ ! -d "$ROOT/ngafid-pydata/tests" ]]; then
        record "Python (pytest)" "SKIP" "no ngafid-pydata/tests directory found"
        return
    fi
    local report="$REPORTS_DIR/python.txt"
    local vflag=""
    [[ "$VERBOSE" -eq 1 ]] && vflag="-v"
    # shellcheck disable=SC2086  # $py may be "python3 -m pytest" and $vflag may be empty (intentional split)
    run_capture "$report" bash -c "cd '$ROOT/ngafid-pydata' && $py $vflag"
    local status=$RUN_STATUS
    local detail
    detail="$(grep -oE '[0-9]+ (passed|failed|errors?|skipped|xfailed|xpassed|warnings?)' "$report" |
        paste -sd ', ' - || true)"
    if [[ "$status" -eq 0 ]]; then
        record "Python (pytest)" "OK" "${detail:-all tests passed}"
    else
        record "Python (pytest)" "PROBLEMS" "${detail:-failures} -- see $report" "$report"
    fi
}

test_security() {
    echo
    echo "=== Security (Gradle SQL-injection) ==="
    local dir="$ROOT/ngafid-www/src/test/security-test"
    if [[ ! -x "$dir/gradlew" ]]; then
        record "Security (Gradle SQLi)" "SKIP" "no security-test project found"
        return
    fi
    echo "(--security: targets a running server configured via $dir/.env; see CONTRIBUTING.md)"
    local report="$REPORTS_DIR/security.txt"
    run_capture "$report" bash -c "cd '$dir' && ./gradlew test --console=plain"
    if [[ "$RUN_STATUS" -eq 0 ]]; then
        record "Security (Gradle SQLi)" "OK" "passed"
    else
        record "Security (Gradle SQLi)" "PROBLEMS" "see $report" "$report"
    fi
}

test_js() {
    echo
    echo "=== JS/TS (frontend, Vitest) ==="
    if ! command -v npm >/dev/null 2>&1; then
        record "JS/TS (Vitest)" "SKIP" "npm not found (install Node.js)"
        return
    fi
    if [[ ! -d "$ROOT/ngafid-frontend/node_modules" ]]; then
        record "JS/TS (Vitest)" "SKIP" "dependencies missing (run: cd ngafid-frontend && npm ci)"
        return
    fi
    # `npm test` runs `vitest run` (one-shot, non-watch) over src/**/*.{test,spec}.*
    local report="$REPORTS_DIR/js.txt"
    run_capture "$report" bash -c "cd '$ROOT/ngafid-frontend' && npm test --silent"
    local status=$RUN_STATUS
    # Vitest prints a summary line like "Tests  10 passed (10)"; surface it verbatim.
    local detail
    detail="$(grep -E '^[[:space:]]*Tests[[:space:]]' "$report" | tail -1 | sed -E 's/^[[:space:]]*//; s/[[:space:]]+/ /g' || true)"
    if [[ "$status" -eq 0 ]]; then
        record "JS/TS (Vitest)" "OK" "${detail:-all tests passed}"
    else
        record "JS/TS (Vitest)" "PROBLEMS" "${detail:-failures} -- see $report" "$report"
    fi
}

case "$TARGET" in
    all)
        test_java
        test_python
        test_js
        ;;
    java | kotlin) test_java ;;
    python) test_python ;;
    js) test_js ;;
esac

# The security-test project is opt-in regardless of the selected target.
if [[ "$RUN_SECURITY" -eq 1 ]]; then
    test_security
fi

echo
if [[ "$REPORT" -eq 1 ]]; then
    echo "Test inventory complete (report mode: not failing). Reports in $REPORTS_DIR/"
    exit 0
fi

if [[ "$FAILED" -ne 0 ]]; then
    echo "Tests failed. Fix them before opening a PR (see the reports in $REPORTS_DIR/)."
    exit 1
fi

echo "All available test suites passed."
exit 0
