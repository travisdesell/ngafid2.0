# NGAFID — code conventions

## Every file change is reviewed and approved by the user (required)

**All** changes to files in this repository -- source (Java, Python,
JavaScript), tests, `README.md`, `CLAUDE.md`, `pom.xml`, `pyproject.toml`,
configuration, anything tracked or untracked -- must be shown to the user as a
diff and approved by them **before** they are applied. This holds for every
single edit, including edits that implement a plan the user has already
approved: approving a plan approves the _direction_, not the individual diffs,
and each diff still needs its own review.

- Make every repo file change with the **Edit** or **Write** tool, whose approval
  prompt shows the user the diff. Break large rewrites into several Edits rather
  than going around the prompt.
- **Never** make arbitrary (logic) file changes through Bash: no `sed -i`/`perl
-i`, no Python or heredoc scripts that rewrite files, no `>`/`>>` redirects or
  `tee` into repo paths, no `cp`/`mv`/`rm` of repo files, and no file-changing
  `git` commands (`checkout`, `restore`, `apply`, `reset`, `stash`, ...) without
  asking first.
- **Exception — formatter/linter autofixers may run without per-diff review.**
  Tools that make only formatting/lint fixes (no logic changes) --
  `mvn spotless:apply`, `ruff --fix`, `ruff format`, `eslint --fix`,
  `ktlint -F`, `black`, `isort`, `prettier --write` -- may be run directly, but
  **mention when you run them** and show the resulting diff afterward so the user
  can review the change set before committing. Anything requiring a manual code
  edit (renames, logic, bug fixes) still goes through Edit/Write.
- Scratch output belongs in the session scratchpad (or `/tmp`), never the repo.
  Verification scripts that would otherwise be piped to `python` via a heredoc
  must be written to the scratchpad and run from there.
- If a change cannot go through Edit/Write, stop and ask the user, explaining
  exactly what will change.

This is enforced in `.claude/settings.local.json` (an `Edit(/**)` / `Write(/**)`
ask rule, a `PreToolUse` Bash hook `.claude/hooks/bash_repo_write_gate.py` that
denies scripted edits and asks before other file operations, and an auto-mode
`hard_deny` rule), but the rule applies even where that enforcement misses a
case: do not look for ways around it.

## Type hints and docstrings (required)

Any code you **add or modify** must be fully type-hinted and documented. When
you touch a function, method, or class, bring it up to this standard even if
the surrounding legacy code predates it.

### Type hints

For python code:

- Annotate **every** parameter and the **return type** of every function and
  method — including `-> None` when nothing is returned, and nested/inner
  functions and locally-defined classes.
- Use modern typing: `X | None` (not `Optional`-in-prose or a bare `= None`
  without the `| None`), `list[...]`, `dict[str, Any]`, `tuple[...]`,
  `Iterator[...]`, `Callable[..., ...]`. Import names from `typing` /
  `collections.abc` as needed; `from __future__ import annotations` is already
  used, so annotations are lazy strings.
- Never use the builtin `any`/`list`/`dict` **as a type** in place of `Any` /
  `list[...]` / `dict[...]` (e.g. `dict[str, Any]`, never `dict(str, any)`).

### Docstrings

Java code should use **Javadoc-style** docstrings (matching the existing codebase)
with all of the applicable sections:

- A one-line summary sentence.
- `param:` — one entry per parameter (omit `self`/`cls`), describing each.
- `return:` — what is returned; state explicitly when the function returns
- `throws:` — every exception the function raises directly, with the condition.
- Keep docstrings **accurate**: if a method sets `self.x` rather than returning
  a value, document that; don't leave stale or placeholder argument lines.

Python code should use **Google-style** docstrings (matching the existing codebase)
with all of the applicable sections:

- A one-line summary sentence.
- `Args:` — one entry per parameter (omit `self`/`cls`), describing each.
- `Returns:` — what is returned; state explicitly when the function returns
  `None` and instead mutates state (say what it sets).
- `Raises:` — every exception the function raises directly, with the condition.
- Keep docstrings **accurate**: if a method sets `self.x` rather than returning
  a value, document that; don't leave stale or placeholder argument lines.

**Be behavior-focused, not signature-restating.** The summary and body should
explain _what the method does and how_ — the meaningful work, algorithm or
approach, side effects (what state it mutates, what it writes to the DB, files,
or caches), important edge cases, and any non-obvious behavior or assumptions —
rather than paraphrasing the method name or parameter types. A reader who cannot
see the body should understand the method's contract from the docstring.

- Prefer "Splits the flight into phases by scanning AltAGL for touch-and-go
  transitions, returning one section per detected phase" over "Processes the
  flight." Avoid empty restatements like "Gets the name" for `getName`, or
  "@param connection the connection" — say what the connection is used _for_.
- Each `@param`/`Args` entry should add information beyond the parameter's name
  and type (its role, units, valid range, null handling), not echo it.
- Genuinely trivial members — plain getters/setters, constructors that only
  store their arguments — may keep a short one-line description; there is no
  value in inventing detail that isn't there.

Document public and private methods alike, plus nested functions and classes
that carry real logic. Trivial one-line lambdas/closures whose behavior is
fully covered by their enclosing function's docstring may be left undocumented.

## Linting (required)

All generated code must pass the linter used by this codebase. The repository
lints in CI via [GitHub Super-Linter](.github/workflows/java-lint.yaml), with
rule configs under `.github/linters/`. Any code you add or modify must be clean
under that linter — do not introduce new lint errors or warnings. If a lint
rule genuinely must be suppressed, do so narrowly (a scoped inline suppression)
and explain why in a comment, rather than disabling the rule broadly.

## Tests (required)

Any code you **add or modify** must come with unit tests that verify its
correctness, in the same change set. Tests are part of the change, not a
follow-up: a new function, a bug fix, or a behavioral change without a test that
exercises it is incomplete.

- **Java / Kotlin** — JUnit 5 tests under the module's `src/test`. Database-backed
  code uses the shared Testcontainers MySQL (see `ngafid-core`'s `TestDatabase`).
- **Python** — `pytest` tests under `ngafid-pydata/tests` (or the relevant
  package's tests).
- **JS / TS** — Vitest tests (`*.test.ts` / `*.test.tsx` beside the code under
  `ngafid-frontend/src`), with React Testing Library for components. See the
  example tests `ngafid-frontend/src/map_utils.test.ts` (pure utility) and
  `ngafid-frontend/src/info_hint.test.tsx` (component).
- Cover the meaningful behavior **and** the edge cases (boundaries, error and
  empty paths), not just the happy path; keep tests deterministic and fast.
- Everything must pass via `scripts/test.sh` (the same suites CI runs) before a
  PR. Tests that need external infrastructure are tagged and opt-in (see
  `CONTRIBUTING.md`); the default/CI suites must not depend on it.

## Logging (required)

Emit diagnostics through the logging framework, never through raw
`System.out`/`System.err` prints (or stray committed `console.log` in TS/JS) that
bypass the configured level.

- **Java** uses `java.util.logging` (JUL). The default level is **WARN**
  (`WARNING`) — configured in [`resources/log.properties`](resources/log.properties)
  — so only warnings and errors are logged by default, which keeps production logs
  from growing too large against the size of the NGAFID database. Log verbose
  debugging at `FINE` and genuinely noteworthy operational events at `INFO`; both
  are suppressed by the WARN default and can be enabled per package when needed
  (e.g. `org.ngafid.level=INFO`). Do not commit `System.out`/`System.err` debug
  prints.
- **Python** uses the `logging` module; `ngafid-pydata` (and the other Python
  tools) log at **INFO** by default. Use `logger.debug(...)` for verbose detail.
- Choose the level deliberately for scale: anything that would fire per-flight or
  per-row across the full dataset must be `FINE` (Java) / `debug` (Python), never
  `INFO`/`WARN`.

## Keep the README, CONTRIBUTING, and documentation in sync (required)

Whenever an edit changes how code behaves, update the documentation that
describes it in the same change set, and surface the impact to the user before
finalizing. Treat the docs as part of the code: a behavioral change that leaves
the docs describing the old behavior is incomplete.

- When you add, remove, rename, or change the meaning/default of a command-line
  argument, a config/property key, an output file or its columns, or an entry
  point, update every place that documents it (the relevant `README.md`, and any
  `--help`/usage text) to match.
- When you change how developers build, lint, test, or run the project — the
  `scripts/` helpers (`lint.sh`, `format.sh`, `test.sh`) and their targets/flags,
  the CI workflows, tooling or tooling versions, or the testing/linting
  frameworks and their configs — update [`CONTRIBUTING.md`](CONTRIBUTING.md) in
  the same change set so its instructions stay accurate. Treat `CONTRIBUTING.md`
  exactly like the `README.md`: a process change that leaves it describing the old
  workflow is incomplete.
- For the Python tooling under [`ngafid-pydata/`](ngafid-pydata), keep
  [`ngafid-pydata/README.md`](ngafid-pydata/README.md) in step with the
  `foundry_export` CLI: the connection defaults, the command-line arguments, and
  the exact set of exported files (`events.csv`, `flights.csv`, the
  `event_definitions`/`airframes`/`airframe_types`/`tails` tables, and their
  `.avsc` schemas). Adding or changing an export means a matching README change
  and a test update.
- Do not silently update docs to paper over a behavioral change, and do not leave
  docs describing an interface the code no longer supports -- name the
  divergence, state what changed, and propose the concrete doc edits so the user
  can decide how to reconcile it.

## Design for the production database's scale (required)

The production NGAFID database is **massive and continually growing** — on the
order of **2.5 million flights** and **3 million flight hours**, and climbing.
Code that touches it must be written for that scale: an approach that looks fine
against a handful of local/test rows can make a page unusable or overload the
database in production.

When writing webpages, endpoints, or queries, always weigh **query time, index
usage, and the amount of data transferred**:

- **Never load whole tables (or whole result sets) into memory or send them to the
  browser.** Paginate, stream, or aggregate on the database side, and return only
  the rows and columns actually needed — no `SELECT *` on the large/wide tables.
- **Keep queries index-friendly.** Filter and join on indexed columns, avoid
  wrapping indexed columns in functions inside `WHERE`, and check the query plan
  for full scans over the big tables (flights, events, and the per-flight
  time-series data) before shipping. When a new access pattern needs an index, add
  it and document it.
- **Avoid N+1 query patterns** — do not run one query per flight/row in a loop;
  use a single set-based query or a join.
- **Aggregate and filter server-side**, not in the browser: compute counts,
  summaries, and rollups in SQL and transfer the small result, rather than
  shipping raw rows for the client to reduce.
- **Bound every request.** Paginate list endpoints, cap date ranges and result
  sizes, and prefer incremental/lazy loading for large views (maps, plots, time
  series) so one page view cannot pull millions of rows.
- When a change adds or alters a query or view over the large tables, call it out
  in review and note its expected cost (what it scans, how much it returns).
