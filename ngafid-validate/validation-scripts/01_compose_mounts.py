# flake8: noqa: E501
import re
import socket
from pathlib import Path


def run_check(validator):
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
        r"x-ngafid-service-common:\s*&ngafid-service-common.*?depends_on:.*?ngafid-validate:\s*\n\s*condition:\s*service_completed_successfully",
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


def _check_workspace_logging_files(validator, category):
    validator._check_file_readable(
        category,
        "workspace logging.properties",
        "/workspace/logging.properties",
        "ensure logging.properties exists at repository root",
    )
    validator._check_file_readable(
        category,
        "workspace log.properties",
        "/workspace/resources/log.properties",
        "ensure resources/log.properties exists for runtime logging configuration",
    )
