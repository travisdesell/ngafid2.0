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
#   scripts/format.sh [--verbose] [all|java|kotlin|python|js|css|web|html|bash|yaml|markdown]
#
#   --verbose  (-v) List each file a formatter will touch, one per line.
#   target   Which formatters to run (default: all):
#              java | kotlin -> Spotless apply (Palantir Java Format + ktlint)
#              python        -> ruff format + ruff check --fix   (ngafid-pydata)
#              js            -> Prettier + ESLint --fix          (ngafid-frontend)
#              css           -> Prettier (ngafid-frontend + ngafid-static styles)
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

VERBOSE=0
TARGET="all"
for arg in "$@"; do
    case "$arg" in
        -v | --verbose) VERBOSE=1 ;;
        all | java | kotlin | python | js | css | web | html | bash | yaml | markdown) TARGET="$arg" ;;
        *)
            echo "Unknown argument: $arg" >&2
            echo "Usage: scripts/format.sh [--verbose] [all|java|kotlin|python|js|css|web|html|bash|yaml|markdown]" >&2
            exit 2
            ;;
    esac
done

# Prettier is installed as a dev dependency of ngafid-frontend; invoke that copy.
PRETTIER="$ROOT/ngafid-frontend/node_modules/.bin/prettier"

note() { echo "[skip] $1"; }

# List the repository's tracked files to format (mirrors scripts/lint.sh's
# repo_files). Discovered dynamically so new files are covered once added to git;
# untracked files are intentionally left until then. git tracking also keeps
# generated trees out. Falls back to a pruned find when not inside a git tree.
repo_files() {
    if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
        git ls-files
    else
        find . -type f \
            -not -path '*/node_modules/*' -not -path '*/.git/*' \
            -not -path '*/target/*' -not -path '*/build/*' -not -path '*/dist/*' \
            -not -path '*/cesium/Build/*' | sed 's|^\./||'
    fi
}

# With --verbose, print each file a formatter will touch (one per line, indented).
# $1 is a newline-separated file list. A no-op unless --verbose was passed.
vlist() {
    [[ "$VERBOSE" -eq 1 ]] || return 0
    while IFS= read -r _f; do
        [[ -n "$_f" ]] && echo "    - $_f"
    done <<<"$1"
}

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
    vlist "$(repo_files | grep -E '\.(java|kt|kts)$' || true)"
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
    vlist "$(repo_files | grep -E '\.py$' || true)"
    $ruff format .
    $ruff check --fix .
}

format_js() {
    echo
    echo "=== JS/TS (Prettier + ESLint --fix) ==="
    vlist "$(repo_files | grep -E '\.(js|jsx|mjs|ts|tsx)$' | grep -v cesium/Build || true)"
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
    # Covers both the frontend source and the static CSS; vendored/minified files
    # (cesium/Build, *.min.css) are skipped via .prettierignore.
    vlist "$(repo_files | grep -iE '\.css$' | grep -vE 'cesium/Build|\.min\.css$' || true)"
    if [[ -x "$PRETTIER" ]]; then
        "$PRETTIER" --write "ngafid-frontend/src/**/*.css" "ngafid-static/css/**/*.css"
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
    # Config is .djlintrc at the repo root (profile=handlebars).
    vlist "$(repo_files | grep -iE 'ngafid-static/templates/.*\.html$' || true)"
    $djlint ngafid-static/templates --extension html --reformat || true
}

format_bash() {
    echo
    echo "=== Bash (shfmt -w) ==="
    if ! command -v shfmt >/dev/null 2>&1; then
        note "shfmt not found (brew install shfmt); skipping Bash"
        return
    fi
    # Every shell script in the repo (see repo_files), discovered dynamically.
    local files
    files=$(repo_files | grep -E '\.sh$' || true)
    if [[ -n "$files" ]]; then
        vlist "$files"
        # shellcheck disable=SC2086  # intentional word-splitting of the file list
        shfmt -i 4 -ci -w $files
    fi
}

format_yaml() {
    echo
    echo "=== YAML (Prettier) ==="
    if [[ ! -x "$PRETTIER" ]]; then
        note "prettier not found; skipping YAML"
        return
    fi
    # All YAML (workflows under .github plus the root docker-compose files); see repo_files.
    local files
    files=$(repo_files | grep -iE '\.ya?ml$' || true)
    if [[ -n "$files" ]]; then
        vlist "$files"
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
    # All Markdown in the repo (see repo_files; node_modules is excluded).
    local files
    files=$(repo_files | grep -iE '\.md$' || true)
    if [[ -n "$files" ]]; then
        vlist "$files"
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
