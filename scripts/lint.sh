#!/usr/bin/env bash
#
# Unified lint/format checker for the NGAFID monorepo.
#
# Runs every linter/formatter check this project uses so a developer can verify
# their code meets the project's linting requirements before opening a PR -- the
# same checks CI runs. The lint-inventory GitHub workflow runs this in --report
# mode to size the backlog.
#
# Usage:
#   scripts/lint.sh [--report] [all|java|kotlin|python|js|format|checkstyle]
#
#   --report   Never exit non-zero: run every check, print counts, and (in CI)
#              append a summary to $GITHUB_STEP_SUMMARY. Used for inventory.
#              Without it, the script exits non-zero if any check finds problems,
#              which makes it a usable pre-PR gate.
#   target     Which checks to run (default: all):
#                java       -> Spotless (format) + Checkstyle (style/Javadoc)
#                kotlin     -> Spotless (format + ktlint)
#                format     -> Spotless only (Java + Kotlin)
#                checkstyle -> Checkstyle only (Java)
#                python     -> ruff
#                js         -> ESLint (ngafid-frontend)
#
# A check whose toolchain is missing is SKIPPED with a note (not failed), so the
# script is useful on a machine that has only some of the toolchains installed.

set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

REPORT=0
TARGET="all"
for arg in "$@"; do
    case "$arg" in
        --report) REPORT=1 ;;
        all | java | kotlin | python | js | format | checkstyle) TARGET="$arg" ;;
        *)
            echo "Unknown argument: $arg" >&2
            echo "Usage: scripts/lint.sh [--report] [all|java|kotlin|python|js|format|checkstyle]" >&2
            exit 2
            ;;
    esac
done

REPORTS_DIR="$ROOT/lint-reports"
CACHE_DIR="$ROOT/.lint-cache"
mkdir -p "$REPORTS_DIR" "$CACHE_DIR"

FAILED=0

# Append a line to the GitHub Actions run summary when running in CI.
summary() {
    if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
        echo "$1" >>"$GITHUB_STEP_SUMMARY"
    fi
}

# Record a check result, updating FAILED and printing/append-summarizing it.
#   $1 = label, $2 = status (OK|PROBLEMS|SKIP), $3 = detail
record() {
    local label="$1" status="$2" detail="$3"
    echo "[$status] $label: $detail"
    summary "- **$label**: $status -- $detail"
    if [[ "$status" == "PROBLEMS" ]]; then
        FAILED=1
    fi
}

# Resolve a JDK 25 home for the Spotless step. The Palantir formatter Spotless
# runs crashes on JDK 27 (an internal javac API changed), so prefer a JDK 25
# launcher when one is available. Order: $JAVA25_HOME, then macOS java_home -v 25.
# Falls back to the current JAVA_HOME/PATH (CI installs JDK 25 directly, so no
# override is needed there). Maven's toolchain pins the compiler/tests to 25
# separately; this only fixes the launcher JVM that Spotless itself runs in.
JDK25_HOME=""
resolve_jdk25() {
    if [[ -n "${JAVA25_HOME:-}" && -x "${JAVA25_HOME}/bin/java" ]]; then
        JDK25_HOME="$JAVA25_HOME"
    elif [[ -x /usr/libexec/java_home ]]; then
        JDK25_HOME="$(/usr/libexec/java_home -v 25 2>/dev/null || true)"
    fi
}
resolve_jdk25

lint_python() {
    echo
    echo "=== Python (ruff) ==="
    if ! command -v ruff >/dev/null 2>&1; then
        record "Python (ruff)" "SKIP" "ruff not found (pip install ruff, or activate the project venv)"
        return
    fi
    local report="$REPORTS_DIR/python-ruff.txt"
    ruff check . --output-format=concise | tee "$report"
    local status=${PIPESTATUS[0]}
    local count
    count=$(grep -cE ':[0-9]+:[0-9]+: ' "$report" || true)
    if [[ "$status" -eq 0 ]]; then
        record "Python (ruff)" "OK" "no violations"
    else
        record "Python (ruff)" "PROBLEMS" "$count violation(s) -- auto-fix many with 'ruff check --fix .'"
    fi
}

lint_js() {
    echo
    echo "=== JS/TS (ESLint) ==="
    if ! command -v npm >/dev/null 2>&1; then
        record "JS/TS (ESLint)" "SKIP" "npm not found (install Node.js)"
        return
    fi
    if [[ ! -d "$ROOT/ngafid-frontend/node_modules" ]]; then
        record "JS/TS (ESLint)" "SKIP" "dependencies missing (run: cd ngafid-frontend && npm ci)"
        return
    fi
    local report="$REPORTS_DIR/js-eslint.txt"
    (cd "$ROOT/ngafid-frontend" && npx --no-install eslint .) | tee "$report"
    local status=${PIPESTATUS[0]}
    if [[ "$status" -eq 0 ]]; then
        record "JS/TS (ESLint)" "OK" "no errors"
    else
        local detail
        detail=$(grep -oE '[0-9]+ problems?.*' "$report" | tail -1 || true)
        record "JS/TS (ESLint)" "PROBLEMS" "${detail:-see $report} -- auto-fix many with 'npm run check -- --fix'"
    fi
}

lint_format() {
    echo
    echo "=== Java/Kotlin formatting (Spotless) ==="
    if ! command -v mvn >/dev/null 2>&1; then
        record "Formatting (Spotless)" "SKIP" "mvn not found (install Maven)"
        return
    fi
    local report="$REPORTS_DIR/spotless.txt"
    # Launch under JDK 25 when available (Spotless's Palantir formatter breaks on 27).
    local mvn_env=()
    if [[ -n "$JDK25_HOME" ]]; then
        mvn_env=(env "JAVA_HOME=$JDK25_HOME")
        echo "(using JDK 25 launcher: $JDK25_HOME)"
    fi
    "${mvn_env[@]}" mvn -B -ntp spotless:check | tee "$report"
    local status=${PIPESTATUS[0]}
    if [[ "$status" -eq 0 ]]; then
        record "Formatting (Spotless)" "OK" "all files formatted"
    else
        record "Formatting (Spotless)" "PROBLEMS" "formatting issues -- fix with 'mvn spotless:apply'"
    fi
}

# Resolve the Checkstyle all-in-one jar, downloading the latest release once
# into .lint-cache/ when it is not already present.
ensure_checkstyle_jar() {
    local jar="$CACHE_DIR/checkstyle.jar"
    if [[ -f "$jar" ]]; then
        echo "$jar"
        return 0
    fi
    local url
    url=$(curl -sSL https://api.github.com/repos/checkstyle/checkstyle/releases/latest |
        grep browser_download_url | grep -- '-all.jar' | head -1 | cut -d '"' -f 4)
    if [[ -z "$url" ]]; then
        return 1
    fi
    curl -sSL -o "$jar" "$url" && echo "$jar"
}

lint_checkstyle() {
    echo
    echo "=== Java (Checkstyle) ==="
    if ! command -v java >/dev/null 2>&1; then
        record "Java (Checkstyle)" "SKIP" "java not found (install a JDK >= 24)"
        return
    fi
    if ! command -v curl >/dev/null 2>&1; then
        record "Java (Checkstyle)" "SKIP" "curl not found (needed to fetch the Checkstyle jar)"
        return
    fi
    local jar
    jar=$(ensure_checkstyle_jar) || {
        record "Java (Checkstyle)" "SKIP" "could not download the Checkstyle jar"
        return
    }
    local report="$REPORTS_DIR/java-checkstyle.txt"
    # Lint the module source roots (keeps target/ build output out of scope).
    local dirs
    dirs=$(ls -d ./*/src/main/java ./*/src/test/java 2>/dev/null || true)
    if [[ -z "$dirs" ]]; then
        record "Java (Checkstyle)" "SKIP" "no Java source roots found"
        return
    fi
    # shellcheck disable=SC2086  # intentional word-splitting of the dir list
    java -Dorg.checkstyle.sun.suppressionfilter.config=.github/linters/checkstyle-suppressions.xml \
        -jar "$jar" -c .github/linters/checkstyle.xml $dirs >"$report" 2>&1
    local status=$?
    local count
    count=$(grep -cE '^\[(ERROR|WARN)\]' "$report" || true)
    if [[ "$status" -eq 0 ]]; then
        record "Java (Checkstyle)" "OK" "no violations"
    else
        record "Java (Checkstyle)" "PROBLEMS" "$count violation(s) -- see $report"
    fi
}

case "$TARGET" in
    all)
        lint_format
        lint_checkstyle
        lint_python
        lint_js
        ;;
    java)
        lint_format
        lint_checkstyle
        ;;
    kotlin | format) lint_format ;;
    checkstyle) lint_checkstyle ;;
    python) lint_python ;;
    js) lint_js ;;
esac

echo
if [[ "$REPORT" -eq 1 ]]; then
    echo "Inventory complete (report mode: not failing). Reports in $REPORTS_DIR/"
    exit 0
fi

if [[ "$FAILED" -ne 0 ]]; then
    echo "Linting found problems. Fix them (or run the suggested auto-fixers) before opening a PR."
    exit 1
fi

echo "All available linters passed."
exit 0
