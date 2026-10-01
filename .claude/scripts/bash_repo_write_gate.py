#!/usr/bin/env python3
"""PreToolUse hook: stop Bash commands from modifying ngafid2.0 repo files unreviewed.

Every change to a repo file must be reviewed and approved by the user, which
the Edit/Write tools guarantee (their approval prompt shows the diff). Bash can
write files without that prompt, so this hook inspects each Bash command:

* scripted edits (``sed -i``, ``perl -i``, Python stdin/heredoc scripts,
  ``python -c`` that opens/writes files, redirects or ``tee`` into anything
  other than a temp/scratch path) are DENIED, with a reason telling Claude to
  use Edit/Write instead;
* file operations that have no Edit equivalent (``cp``/``mv``/``rm``,
  file-changing ``git`` subcommands, ``black`` without ``--check``/``--diff``,
  ``patch``, ``truncate``) require the user's APPROVAL.

Reads the hook payload as JSON on stdin and prints a PreToolUse decision as
JSON on stdout (nothing when the command is allowed through unchanged).

This file is tracked so the whole team inherits the gate; it is wired up by the
tracked ``.claude/settings.json``.
"""

from __future__ import annotations

import json
import re
import sys

#: Redirect/tee targets under these prefixes are scratch space, not the repo.
SAFE_TARGET_PREFIXES: tuple[str, ...] = ("/dev/", "/tmp/", "/private/tmp/")

#: Commands that edit files in place from a script -- use Edit/Write instead.
DENY_PATTERNS: tuple[tuple[str, str], ...] = (
    (r"\bsed\b[^|;&]*\s-i", "sed -i"),
    (r"\bperl\b[^|;&]*\s-i", "perl -i"),
    (r"\bpython[0-9.]*\s+-\s*(<<|$|\|)", "a Python script read from stdin/heredoc"),
    (r"\|\s*python[0-9.]*\s+-?\s*($|[;&|])", "a Python script piped to python"),
    (r"\bpython[0-9.]*\s+-c\b.*(open\(|\.write|write_text|shutil|os\.(remove|rename|replace))", "python -c writing files"),
)

#: Commands that change files but have no Edit/Write equivalent -- ask first.
ASK_PATTERNS: tuple[tuple[str, str], ...] = (
    (r"(^|[;&|(]\s*|\s)(cp|mv|rm|rmdir|truncate|patch|ln|install)\s", "a file copy/move/delete"),
    (r"\bgit\s+(checkout|restore|apply|am|reset|stash|clean|rm|mv|merge|rebase|cherry-pick|revert|pull|commit)\b", "a file-changing git command"),
    (r"\bblack\b(?![^|;&]*--(check|diff))", "black reformatting files"),
    (r"\bisort\b(?![^|;&]*--(check|diff))", "isort rewriting files"),
    (r"\bruff\s+format\b(?![^|;&]*--(check|diff))", "ruff format rewriting files"),
    # In-place linters/fixers that rewrite files -- use their --check/--diff mode,
    # or apply fixes via Edit/Write so the diff is reviewed.
    (r"\bruff\b[^|;&]*--fix\b", "ruff applying fixes in place (--fix)"),
    (r"\beslint\b[^|;&]*--fix\b", "eslint applying fixes in place (--fix)"),
    (r"\bktlint\b[^|;&]*(--format|\s-F\b)", "ktlint reformatting files"),
    (r"\bprettier\b[^|;&]*--write\b", "prettier rewriting files"),
    (r"\bspotless:apply\b", "Spotless rewriting files (spotless:apply)"),
    (r"\bnpm\s+run\s+\S+\s+--\s+--fix\b", "an npm script applying --fix in place"),
)


def _read_token(command: str, start: int) -> tuple[str, int]:
    """Reads one shell token (handling quotes) beginning at ``start``.

    Args:
        command: The Bash command string.
        start: Index at which the token begins.

    Returns:
        A tuple of the unquoted token text and the index just past it.
    """

    index = start
    length = len(command)
    chars: list[str] = []
    quote: str | None = None
    while index < length:
        char = command[index]
        if quote:
            if char == quote:
                quote = None
            else:
                chars.append(char)
        elif char in ("'", '"'):
            quote = char
        elif char in " \t;&|()<>":
            break
        else:
            chars.append(char)
        index += 1
    return "".join(chars), index


def redirect_targets(command: str) -> list[str]:
    """Finds the files a command redirects or tees output into.

    Scans with quote awareness so a ``>`` inside a quoted argument -- an awk
    program like ``'$1 > 2'`` or an SQL string -- is not mistaken for a file
    redirect. Captures the target of each ``>``/``>>`` redirect that appears
    outside quotes, plus ``tee`` arguments, with surrounding quotes stripped.
    File-descriptor duplications such as ``2>&1`` and ``>&2`` are skipped.

    Args:
        command: The Bash command string.

    Returns:
        The redirect/tee targets found outside quotes.
    """

    targets: list[str] = []
    index = 0
    length = len(command)
    quote: str | None = None
    while index < length:
        char = command[index]
        if quote:
            if char == quote:
                quote = None
            index += 1
            continue
        if char in ("'", '"'):
            quote = char
            index += 1
            continue
        if char == ">":
            cursor = index + 1
            if cursor < length and command[cursor] == ">":  # '>>' append
                cursor += 1
            while cursor < length and command[cursor] in " \t":
                cursor += 1
            if cursor < length and command[cursor] == "&":  # '>&' fd duplication
                index = cursor + 1
                continue
            target, index = _read_token(command, cursor)
            if target:
                targets.append(target)
            continue
        index += 1

    for match in re.finditer(r"\btee\s+((?:-\S+\s+)*)([^\s;&|)]+)", command):
        targets.append(match.group(2).strip("'\""))
    return targets


def decide(command: str) -> tuple[str, str] | None:
    """Classifies a Bash command as allowed, needing approval, or denied.

    Args:
        command: The Bash command string.

    Returns:
        ``None`` to let the command through, or ``(decision, reason)`` where
        ``decision`` is ``"deny"`` or ``"ask"``.
    """

    for pattern, label in DENY_PATTERNS:
        if re.search(pattern, command, flags=re.MULTILINE):
            return (
                "deny",
                f"Blocked {label}: ngafid2.0 repo files may only be changed with the "
                "Edit/Write tools so the user reviews and approves every diff. "
                "Use Edit/Write (or write the script's output to the scratchpad).",
            )

    # Targets beginning with '$' use an unexpanded variable we cannot resolve
    # (commonly a temp/scratchpad path); don't flag what we can't evaluate.
    unsafe = [
        t
        for t in redirect_targets(command)
        if not t.startswith(SAFE_TARGET_PREFIXES) and not t.startswith("$")
    ]
    if unsafe:
        return (
            "deny",
            f"Blocked shell redirect/tee into {', '.join(unsafe)}: ngafid2.0 repo files may "
            "only be changed with the Edit/Write tools so the user reviews every diff. "
            "Write scratch output under /tmp or /private/tmp (the scratchpad) instead.",
        )

    for pattern, label in ASK_PATTERNS:
        if re.search(pattern, command, flags=re.MULTILINE):
            return (
                "ask",
                f"This Bash command looks like {label}, which may change ngafid2.0 files "
                "without a diff review. Approve only if intended.",
            )

    return None


def main() -> None:
    """Reads the hook payload and prints the PreToolUse decision, if any.

    Returns:
        None. Writes a JSON decision to stdout when the command is gated.
    """

    payload = json.load(sys.stdin)
    command = str(payload.get("tool_input", {}).get("command", ""))
    decision = decide(command)
    if decision is None:
        return
    print(
        json.dumps(
            {
                "hookSpecificOutput": {
                    "hookEventName": "PreToolUse",
                    "permissionDecision": decision[0],
                    "permissionDecisionReason": decision[1],
                }
            }
        )
    )


if __name__ == "__main__":
    main()
