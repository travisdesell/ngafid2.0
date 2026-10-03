"""Startup check that Docker Compose bind-mounts and host wiring are in place.

Runs as the ``COMPOSE`` category of the NGAFID startup validator: confirms the
config files the container expects are mounted (``ngafid.properties``, the db and
optional email config, the repository root at ``/workspace``), that
``host.docker.internal`` resolves when running inside Docker, and that the
compose startup contract and workspace logging files are present.
"""

from __future__ import annotations

import re
import socket
from pathlib import Path
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from validator import Validator


def run_check(validator: Validator) -> None:
    """Verify the container's required bind-mounts, host alias, and log files.

    Checks that the ngafid properties, database config, and workspace-root mounts
    are readable; additionally requires the email config mount only when email is
    enabled in ``ngafid.properties``. When running inside Docker, verifies
    ``host.docker.internal`` resolves. Finally delegates to the compose
    startup-contract and workspace-logging checks. Every outcome is recorded
    through the validator's pass/fail helpers.

    Args:
        validator: The running startup validator; supplies the file-readable and
            pass/fail helpers plus the ``in_docker`` flag.
    """
    category = "COMPOSE"
    validator._check_file_readable(
        category,
        "ngafid properties mount",
        "/app/ngafid.properties",
        "mount ngafid.properties to /app/ngafid.properties",
    )
    validator._check_file_readable(
        category,
        "db config mount",
        "/etc/ngafid-db.conf",
        "mount liquibase.docker.properties to /etc/ngafid-db.conf",
    )
    validator._check_file_readable(
        category,
        "workspace root mount",
        "/workspace/docker-compose.yml",
        "mount repository root to /workspace for artifact checks",
    )

    email_enabled = validator._is_email_enabled_from_file("/app/ngafid.properties")
    if email_enabled:
        validator._check_file_readable(
            category,
            "email config mount",
            "/etc/ngafid-email.conf",
            "mount email_info.txt to /etc/ngafid-email.conf or disable email",
        )

    if validator.in_docker:
        try:
            socket.gethostbyname("host.docker.internal")
            validator._pass(category, "host alias", "host.docker.internal resolves")
        except socket.gaierror:
            validator._fail(
                category,
                "host alias",
                "host.docker.internal does not resolve",
                "add extra_hosts mapping for host.docker.internal if this deployment requires it",
            )

    _check_compose_startup_contract(validator, category)
    _check_workspace_logging_files(validator, category)


def _check_compose_startup_contract(validator, category):
    compose_path = Path("/workspace/docker-compose.yml")
    try:
        compose_text = compose_path.read_text(encoding="utf-8")
    except OSError as exc:
        validator._fail(
            category,
            "compose startup contract",
            f"unable to read {compose_path}: {exc}",
            "mount workspace and verify docker-compose.yml readability",
        )
        return

    if "ngafid-validate:" not in compose_text:
        validator._fail(
            category,
            "compose validator service",
            "ngafid-validate service is not defined in docker-compose.yml",
            "define a one-shot ngafid-validate service in docker-compose.yml",
        )
    else:
        validator._pass(category, "compose validator service", "ngafid-validate service is defined")

    common_depends_pattern = re.compile(
        r"x-ngafid-service-common:\s*&ngafid-service-common.*?depends_on:.*?ngafid-validate:\s*\n\s*condition:\s*service_completed_successfully",  # noqa: E501
        re.DOTALL,
    )
    if common_depends_pattern.search(compose_text):
        validator._pass(
            category,
            "compose validator gating",
            "shared service dependency includes ngafid-validate completion gate",
        )
    else:
        validator._fail(
            category,
            "compose validator gating",
            "shared service dependency does not gate startup on ngafid-validate completion",
            "add ngafid-validate: condition: service_completed_successfully to shared depends_on",
        )


def _check_workspace_logging_files(validator: Validator, category: str) -> None:
    """Require the Java logging config the services load at runtime.

    Only ``resources/log.properties`` is checked: it is the java.util.logging
    configuration the web server loads (and the Dockerfile copies to
    ``/etc/log.properties``). The former repo-root ``logging.properties`` was
    never loaded by anything and has been removed, so it is no longer required.

    Args:
        validator: The running startup validator, whose ``_check_file_readable``
            helper records the pass/fail outcome.
        category: The report category to file the result under (``COMPOSE``).
    """
    validator._check_file_readable(
        category,
        "workspace log.properties",
        "/workspace/resources/log.properties",
        "ensure resources/log.properties exists for runtime logging configuration",
    )
