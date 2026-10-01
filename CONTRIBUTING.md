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

All code must pass linting (and be formatted) before you open a PR; CI enforces
this. The quickest way to verify everything at once is the helper script, which
runs every language's checks -- the same ones CI runs -- and simply skips any
whose toolchain you don't have installed:

```
scripts/lint.sh            # check everything; exits non-zero if anything fails
scripts/lint.sh python     # check one language: java | kotlin | python | js | format | checkstyle
scripts/lint.sh --report   # run everything without failing, and print counts
```

The individual checks, and how to run / auto-fix each directly:

| Scope | Tool | Check | Auto-fix |
| --- | --- | --- | --- |
| Java (style + Javadoc, max line 120) | Checkstyle | `scripts/lint.sh checkstyle` | mostly `mvn spotless:apply`; Javadoc/naming are manual |
| Java + Kotlin (formatting) | Spotless (Palantir Java Format + ktlint) | `mvn spotless:check` | `mvn spotless:apply` |
| Python | ruff | `ruff check .` | `ruff check --fix .` |
| JS / TS | ESLint | `cd ngafid-frontend && npm run check` | `npm run check -- --fix` |

Rule configs live where each tool expects them: `.github/linters/checkstyle.xml`
(Java), `ruff.toml` plus `ngafid-pydata/pyproject.toml` (Python), `.editorconfig`
(Kotlin/ktlint), and `ngafid-frontend/eslint.config.mjs` (JS/TS).

To see the current repo-wide backlog without installing every toolchain, run the
**Lint Inventory** workflow from the GitHub Actions tab: it runs the same
`scripts/lint.sh --report` and uploads the full per-language reports as an
artifact.

These concerns should be properly addressed before requesting a review.