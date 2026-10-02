# Contributing to the NGAFID

Currently, there are two primary tracking branches in the ngafid2.0 repository: `stable` and `main`.
`stable` is, unsurprisingly, the stable version of the website that is in production. `main` is the in-development
branch and may contain bugs, as well as incomplete or broken functionality.

The `stable` branch is reserved for more mature changes and critical bug fixes.
Feature branches should generally be based off of `main`. Once merged and tested enough to be considered mature, the
changes can be pulled into stable (this should be done by merging with main up until a certain commit).
Some bugs may exist in both `stable` and `main` -- your patch may be applicable to both branches with a simple rebase,
but if not you may need to implement the fix twice.

# Workflow

The workflow is relatively simple:

1. Pick the parent branch your branch will be based upon, either `main` or `stable`.
2. Create a new branch in the following format: `<parent-branch>/<your-name>/<feature-identifier>`. So for example, if
   I (Josh) wanted to add a `cool-feature` to the `main` branch I would create a branched named `main/josh/cool-feature`
   and base it on `main`.
3. Implement and test your changes. Ensure your code is (1) linted and (2) formatted (see below).
4. Create a pull request. After review your changes will be merged.
5. At some point in time these changes will be considered "mature", at which point they may be merged into `stable` and
   eventually deployed. Deployment on the main server should generally require no additional steps outside of
   recompilation of the java
   source and react modules, or creation of new tables via liquibase.

## Linting and formatting

All code must pass linting and be formatted before you open a PR; CI enforces
this. Two companion helper scripts cover **every** language in the repo. They run
the same checks CI runs and simply skip any whose toolchain you don't have
installed:

- **`scripts/format.sh`** auto-fixes what can be fixed (formatting, import order,
  safe lint fixes).
- **`scripts/lint.sh`** checks everything and exits non-zero if any issue remains
  (including checks that have no auto-fix, e.g. Checkstyle Javadoc or shellcheck).

The usual loop before opening a PR:

```bash
scripts/format.sh          # auto-format the whole repo
scripts/lint.sh            # verify; exits non-zero if anything still fails
```

Both scripts take an optional target to run a single language, and `lint.sh`
takes a couple of flags:

```bash
scripts/lint.sh python         # check just one language
scripts/format.sh web          # auto-fix just JS + CSS
scripts/lint.sh --report       # run everything without failing, print counts
scripts/lint.sh --verbose js   # also list each file the check covers
```

Valid targets: `all` (default), `java`, `kotlin`, `python`, `js`, `css`, `web`
(js + css), `html`, `bash`, `yaml`, `markdown`, `dockerfile`; `lint.sh` also
accepts `format` (Spotless only) and `checkstyle` (Checkstyle only).

The individual checks and how to auto-fix each:

| Scope                                | Tool(s)                                  | Check                        | Auto-fix                                            |
| ------------------------------------ | ---------------------------------------- | ---------------------------- | --------------------------------------------------- |
| Java (style + Javadoc, max line 120) | Checkstyle                               | `scripts/lint.sh checkstyle` | `scripts/format.sh java`; Javadoc/naming are manual |
| Java + Kotlin (formatting)           | Spotless (Palantir Java Format + ktlint) | `scripts/lint.sh format`     | `scripts/format.sh java` (or `kotlin`)              |
| Python (lint + format)               | ruff                                     | `scripts/lint.sh python`     | `scripts/format.sh python`                          |
| JS / TS (code quality)               | ESLint                                   | `scripts/lint.sh js`         | `scripts/format.sh js`                              |
| JS / TS / CSS (formatting)           | Prettier                                 | `scripts/lint.sh js` / `css` | `scripts/format.sh web`                             |
| HTML templates                       | djLint                                   | `scripts/lint.sh html`       | `scripts/format.sh html`                            |
| Bash                                 | shfmt + shellcheck                       | `scripts/lint.sh bash`       | `scripts/format.sh bash` (shellcheck is manual)     |
| YAML                                 | yamllint + Prettier                      | `scripts/lint.sh yaml`       | `scripts/format.sh yaml`                            |
| Markdown                             | markdownlint + Prettier                  | `scripts/lint.sh markdown`   | `scripts/format.sh markdown`                        |
| Dockerfile                           | hadolint                                 | `scripts/lint.sh dockerfile` | manual (no auto-fixer)                              |

Rule configs (at the repo root unless noted):

- **Java** — `.github/linters/checkstyle.xml` (+ `checkstyle-suppressions.xml`); Spotless is configured in `pom.xml`.
- **Kotlin** — `.editorconfig` (ktlint).
- **Python** — `ruff.toml` (and `ngafid-pydata/pyproject.toml` for that package).
- **JS / TS** — `ngafid-frontend/eslint.config.mjs`.
- **Prettier** (JS/TS/CSS/YAML/Markdown) — `.prettierrc` and `.prettierignore`.
- **YAML** — `.yamllint.yml`.
- **Markdown** — `.markdownlint.yaml`.
- **HTML templates** — `.djlintrc`.

The scripts discover files from git, so a **newly added file is only checked once
it has been `git add`ed** (untracked files are skipped until then).

To see the current repo-wide backlog without installing every toolchain, run the
**Lint Inventory** workflow from the GitHub Actions tab: it runs the same
`scripts/lint.sh --report` and uploads the full per-language reports as an
artifact.

These concerns should be properly addressed before requesting a review.
