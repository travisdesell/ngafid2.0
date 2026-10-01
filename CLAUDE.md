# NGAFID — code conventions

## Every file change is reviewed and approved by the user (required)

**All** changes to files in this repository -- source (Java, Python,
JavaScript), tests, `README.md`, `CLAUDE.md`, `pom.xml`, `pyproject.toml`,
configuration, anything tracked or untracked -- must be shown to the user as a
diff and approved by them **before** they are applied. This holds for every
single edit, including edits that implement a plan the user has already
approved: approving a plan approves the *direction*, not the individual diffs,
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

## Keep the README and documentation in sync (required)

Whenever an edit changes how code behaves, update the documentation that
describes it in the same change set, and surface the impact to the user before
finalizing. Treat the docs as part of the code: a behavioral change that leaves
the docs describing the old behavior is incomplete.

- When you add, remove, rename, or change the meaning/default of a command-line
  argument, a config/property key, an output file or its columns, or an entry
  point, update every place that documents it (the relevant `README.md`, and any
  `--help`/usage text) to match.
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

