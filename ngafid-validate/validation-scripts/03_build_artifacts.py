"""Startup check that the deployable Maven assembly jars have been packaged.

Verifies that each jar-with-dependencies artifact the Docker images copy out of
``target/`` exists under ``/workspace`` before ``docker compose up``. Runs as the
``BUILD`` category of the NGAFID startup validator.
"""

from __future__ import annotations

from pathlib import Path
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from validator import Validator


def run_check(validator: Validator) -> None:
    """Check that every expected build artifact jar is present under /workspace.

    Passes immediately (recording a skip) when ``--skip-build-artifacts`` was
    given. Otherwise, for each path in ``validator.jar_artifacts`` resolved under
    ``/workspace``, records a pass when the jar exists as a file and a fail (with
    the remediation to run ``run/package``) when it is missing.

    Args:
        validator: The running startup validator; supplies the artifact list,
            the CLI args, and the pass/fail recording helpers.
    """
    if validator.args.skip_build_artifacts:
        validator._pass("BUILD", "artifact checks", "skipped by flag")
        return

    category = "BUILD"
    for rel_path in validator.jar_artifacts:
        full_path = Path("/workspace") / rel_path
        if full_path.exists() and full_path.is_file():
            validator._pass(category, f"artifact {rel_path}", "present")
        else:
            validator._fail(
                category,
                f"artifact {rel_path}",
                "missing",
                "run run/package before docker compose up",
            )
