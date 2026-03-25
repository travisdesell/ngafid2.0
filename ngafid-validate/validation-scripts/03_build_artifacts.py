# flake8: noqa: E501
from pathlib import Path


def run_check(validator):
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
