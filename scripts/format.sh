#!/usr/bin/env bash
#
# Unified auto-formatter for the NGAFID monorepo.
#
# Runs every auto-formatter this project uses, applying fixes in place. It is the
# companion to scripts/lint.sh: `format.sh` *fixes* what `lint.sh` *checks*. Run
# it before committing to bring your changes up to the project's formatting
# standard; then run `scripts/lint.sh` to confirm nothing remains (lint also
# covers checks that have no auto-fix, e.g. Checkstyle Javadoc and shellcheck).
#
# Usage:
#   scripts/format.sh [all|java|kotlin|python|js|css|web|html|bash|yaml|markdown]
#
#   target   Which formatters to run (default: all):
#              java | kotlin -> Spotless apply (Palantir Java Format + ktlint)
#              python        -> ruff format + ruff check --fix   (ngafid-pydata)
#              js            -> Prettier + ESLint --fix          (ngafid-frontend)
#              css           -> Prettier (ngafid-frontend styles)
#              web           -> js + css together
#              html          -> djLint --reformat                (ngafid-static templates)
#              bash          -> shfmt -w                         (shell scripts)
#              yaml          -> Prettier                         (workflows/configs)
#              markdown      -> Prettier                         (*.md)
#
# A formatter whose toolchain is missing is SKIPPED with a note, so the script is
# useful on a machine that has only some toolchains installed.

set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT" || exit 1

TARGET="all"
for arg in "$@"; do
    case "$arg" in
        all | java | kotlin | python | js | css | web | html | bash | yaml | markdown) TARGET="$arg" ;;
        *)
            echo "Unknown argument: $arg" >&2
            echo "Usage: scripts/format.sh [all|java|kotlin|python|js|css|web|html|bash|yaml|markdown]" >&2
            exit 2
            ;;
    esac
done

# Shell scripts that are part of this repo (kept in sync with lint.sh).
SHELL_FILES=(
    scripts/lint.sh
    scripts/format.sh
    resources/services/link-dropin-configs.sh
)

# Prettier is installed as a dev dependency of ngafid-frontend; invoke that copy.
PRETTIER="$ROOT/ngafid-frontend/node_modules/.bin/prettier"

note() { echo "[skip] $1"; }

# Resolve a JDK 25 home for Spotless (its Palantir formatter crashes on JDK 27).
JDK25_HOME=""
if [[ -n "${JAVA25_HOME:-}" && -x "${JAVA25_HOME}/bin/java" ]]; then
    JDK25_HOME="$JAVA25_HOME"
elif [[ -x /usr/libexec/java_home ]]; then
    JDK25_HOME="$(/usr/libexec/java_home -v 25 2>/dev/null || true)"
fi

format_spotless() {
    echo
    echo "=== Java/Kotlin (Spotless apply) ==="
    if ! command -v mvn >/dev/null 2>&1; then
        note "mvn not found; skipping Spotless"
        return
    fi
    local mvn_env=()
    if [[ -n "$JDK25_HOME" ]]; then
        mvn_env=(env "JAVA_HOME=$JDK25_HOME")
        echo "(using JDK 25 launcher: $JDK25_HOME)"
    fi
    "${mvn_env[@]}" mvn -B -ntp spotless:apply
}

format_python() {
    echo
    echo "=== Python (ruff format + ruff check --fix) ==="
    local ruff=""
    if command -v ruff >/dev/null 2>&1; then
        ruff="ruff"
    elif python3 -m ruff --version >/dev/null 2>&1; then
        ruff="python3 -m ruff"
    else
        note "ruff not found (pip install ruff); skipping Python"
        return
    fi
    $ruff format .
    $ruff check --fix .
}

format_js() {
    echo
    echo "=== JS/TS (Prettier + ESLint --fix) ==="
    if [[ -x "$PRETTIER" ]]; then
        "$PRETTIER" --write "ngafid-frontend/src/**/*.{js,jsx,mjs,ts,tsx}"
    else
        note "prettier not found (cd ngafid-frontend && npm ci); skipping JS/TS Prettier"
    fi
    if [[ -d "$ROOT/ngafid-frontend/node_modules" ]]; then
        (cd "$ROOT/ngafid-frontend" && npx --no-install eslint . --fix) || true
    else
        note "frontend deps missing; skipping ESLint --fix"
    fi
}

format_css() {
    echo
    echo "=== CSS (Prettier) ==="
    if [[ -x "$PRETTIER" ]]; then
        "$PRETTIER" --write "ngafid-frontend/src/**/*.css"
    else
        note "prettier not found; skipping CSS"
    fi
}

format_html() {
    echo
    echo "=== HTML templates (djLint --reformat) ==="
    local djlint=""
    if command -v djlint >/dev/null 2>&1; then
        djlint="djlint"
    elif python3 -m djlint --version >/dev/null 2>&1; then
        djlint="python3 -m djlint"
    else
        note "djlint not found (pip install djlint); skipping HTML templates"
        return
    fi
    $djlint ngafid-static/templates --reformat || true
}

format_bash() {
    echo
    echo "=== Bash (shfmt -w) ==="
    if ! command -v shfmt >/dev/null 2>&1; then
        note "shfmt not found (brew install shfmt); skipping Bash"
        return
    fi
    shfmt -i 4 -ci -w "${SHELL_FILES[@]}"
}

format_yaml() {
    echo
    echo "=== YAML (Prettier) ==="
    if [[ ! -x "$PRETTIER" ]]; then
        note "prettier not found; skipping YAML"
        return
    fi
    # All tracked YAML (workflows under .github plus the root docker-compose files).
    local files
    files=$(git ls-files | grep -iE '\.ya?ml$' || true)
    if [[ -n "$files" ]]; then
        # shellcheck disable=SC2086  # intentional word-splitting of the file list
        "$PRETTIER" --write $files
    fi
}

format_markdown() {
    echo
    echo "=== Markdown (Prettier) ==="
    if [[ ! -x "$PRETTIER" ]]; then
        note "prettier not found; skipping Markdown"
        return
    fi
    # Tracked Markdown only (git ls-files naturally excludes node_modules).
    local files
    files=$(git ls-files | grep -iE '\.md$' | grep -v node_modules || true)
    if [[ -n "$files" ]]; then
        # shellcheck disable=SC2086  # intentional word-splitting of the file list
        "$PRETTIER" --write $files
    fi
}

case "$TARGET" in
    all)
        format_spotless
        format_python
        format_js
        format_css
        format_html
        format_bash
        format_yaml
        format_markdown
        ;;
    java | kotlin) format_spotless ;;
    python) format_python ;;
    js) format_js ;;
    css) format_css ;;
    web)
        format_js
        format_css
        ;;
    html) format_html ;;
    bash) format_bash ;;
    yaml) format_yaml ;;
    markdown) format_markdown ;;
esac

echo
echo "Formatting complete. Run 'scripts/lint.sh' to verify (it also covers checks with no auto-fix)."
