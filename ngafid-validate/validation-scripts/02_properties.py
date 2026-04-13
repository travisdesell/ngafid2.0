# flake8: noqa: E501
import re
import os
from pathlib import Path


def run_check(validator):
    category = "CONFIG"
    props = validator._parse_properties(Path("/app/ngafid.properties"))
    validator.properties = props

    if not props:
        validator._fail(
            category,
            "properties parse",
            "ngafid.properties is empty or unreadable",
            "fix or remount ngafid.properties",
        )
        return

    validator._pass(category, "properties parse", "ngafid.properties loaded")

    for key in validator.required_prop_keys:
        if key in props and props[key].strip() != "":
            validator._pass(category, f"required key {key}", "present")
        else:
            validator._fail(
                category,
                f"required key {key}",
                "missing or empty",
                f"set {key} in ngafid.properties",
            )

    placeholder_key = "ngafid.azure.maps.key"
    if placeholder_key in props and "YOUR_ACTUAL_AZURE_MAPS_KEY_HERE" in props[placeholder_key]:
        validator._fail(
            category,
            "azure maps key placeholder",
            "placeholder value still present",
            "set ngafid.azure.maps.key to a real key",
        )
    elif placeholder_key in props:
        validator._pass(
            category,
            "azure maps key placeholder",
            "value is not default placeholder",
        )

    _validate_numeric_bounds(validator, category, props)
    _validate_docker_paths(validator, category, props)
    _validate_runtime_contract_files(validator, category)
    _validate_bootstrap_server_format(validator, category)
    _validate_db_engine_flag(validator, category)
    _validate_email_config(validator, category)


def _parse_int(value):
    try:
        return int(str(value).strip())
    except (TypeError, ValueError):
        return None


def _validate_numeric_bounds(validator, category, props):
    port_value = props.get("ngafid.port", "")
    port = _parse_int(port_value)
    if port is None or port < 1 or port > 65535:
        validator._fail(
            category,
            "web port bounds",
            f"invalid ngafid.port='{port_value}'",
            "set ngafid.port to an integer in range 1-65535",
        )
    else:
        validator._pass(category, "web port bounds", f"ngafid.port={port}")

    parallelism_value = props.get("ngafid.parallelism", "")
    parallelism = _parse_int(parallelism_value)
    if parallelism is None or parallelism < 1:
        validator._fail(
            category,
            "parallelism bounds",
            f"invalid ngafid.parallelism='{parallelism_value}'",
            "set ngafid.parallelism to an integer >= 1",
        )
    else:
        validator._pass(
            category,
            "parallelism bounds",
            f"ngafid.parallelism={parallelism}",
        )

    cpu_count = os.cpu_count() or 1
    if parallelism is not None and parallelism > cpu_count:
        validator._fail(
            category,
            "parallelism cpu alignment",
            f"ngafid.parallelism={parallelism} exceeds detected CPU count={cpu_count}",
            "set ngafid.parallelism to a value <= available CPUs",
        )
    elif parallelism is not None:
        validator._pass(
            category,
            "parallelism cpu alignment",
            f"parallelism {parallelism} <= CPU count {cpu_count}",
        )

    terrain_cache_value = props.get("ngafid.max.terrain.cache.size", "")
    terrain_cache = _parse_int(terrain_cache_value)
    if terrain_cache is None or terrain_cache < 0:
        validator._fail(
            category,
            "terrain cache bounds",
            f"invalid ngafid.max.terrain.cache.size='{terrain_cache_value}'",
            "set ngafid.max.terrain.cache.size to an integer >= 0",
        )
    else:
        validator._pass(
            category,
            "terrain cache bounds",
            f"ngafid.max.terrain.cache.size={terrain_cache}",
        )

    pool_max = _parse_int(props.get("ngafid.db.maximum.pool.size", ""))
    pool_min = _parse_int(props.get("ngafid.db.minimum.idle", ""))
    max_lifetime = _parse_int(props.get("ngafid.db.max.lifetime", ""))
    if pool_max is not None and pool_min is not None:
        if pool_min < 0 or pool_max < 1 or pool_min > pool_max:
            validator._fail(
                category,
                "db pool bounds",
                f"invalid pool settings: min_idle={pool_min}, max_pool={pool_max}",
                "set ngafid.db.minimum.idle >= 0 and <= ngafid.db.maximum.pool.size",
            )
        else:
            validator._pass(
                category,
                "db pool bounds",
                f"min_idle={pool_min}, max_pool={pool_max}",
            )

    if max_lifetime is not None:
        if max_lifetime < 10000:
            validator._fail(
                category,
                "db max lifetime",
                f"ngafid.db.max.lifetime={max_lifetime} is too low",
                "set ngafid.db.max.lifetime to at least 10000ms",
            )
        else:
            validator._pass(
                category,
                "db max lifetime",
                f"ngafid.db.max.lifetime={max_lifetime}",
            )


def _validate_docker_paths(validator, category, props):
    if not validator.in_docker:
        return

    docker_path_keys = [
        "ngafid.docker.repo.path",
        "ngafid.docker.upload.dir",
        "ngafid.docker.archive.dir",
        "ngafid.docker.terrain.dir",
    ]

    invalid = []
    for key in docker_path_keys:
        value = props.get(key, "").strip()
        if not value.startswith("/"):
            invalid.append(f"{key}='{value}'")

    if invalid:
        validator._fail(
            category,
            "docker path consistency",
            f"non-absolute docker paths: {', '.join(invalid)}",
            "set ngafid.docker.* path properties to absolute container paths",
        )
    else:
        validator._pass(
            category,
            "docker path consistency",
            "all ngafid.docker.* path properties are absolute",
        )


def _validate_runtime_contract_files(validator, category):
    db_info_file = validator._effective_property("ngafid.db.info")
    if not db_info_file:
        validator._fail(
            category,
            "db info file",
            "could not resolve effective ngafid.db.info",
            "set ngafid.db.info/ngafid.docker.db.info to a valid path",
        )
    else:
        validator._check_file_readable(
            category,
            "db info file",
            db_info_file,
            "ensure DB config file exists and is mounted into runtime",
        )


def _validate_bootstrap_server_format(validator, category):
    bootstrap = validator._effective_property("ngafid.kafka.bootstrap.servers") or ""
    endpoints = [endpoint.strip() for endpoint in bootstrap.split(",") if endpoint.strip()]

    if not endpoints:
        validator._fail(
            category,
            "kafka bootstrap format",
            "no bootstrap endpoints configured",
            "set ngafid.kafka.bootstrap.servers/ngafid.docker.kafka.bootstrap.servers",
        )
        return

    invalid = []
    for endpoint in endpoints:
        if ":" not in endpoint:
            invalid.append(endpoint)
            continue
        host, _, port_str = endpoint.rpartition(":")
        if not host.strip():
            invalid.append(endpoint)
            continue
        port = _parse_int(port_str)
        if port is None or port < 1 or port > 65535:
            invalid.append(endpoint)

    if invalid:
        validator._fail(
            category,
            "kafka bootstrap format",
            f"invalid bootstrap endpoints: {', '.join(invalid)}",
            "set kafka bootstrap endpoints in host:port format",
        )
    else:
        validator._pass(
            category,
            "kafka bootstrap format",
            f"validated endpoints: {', '.join(endpoints)}",
        )


def _validate_db_engine_flag(validator, category):
    maria_flag = (validator._effective_property("ngafid.use.maria.db") or "false").strip().lower()
    if maria_flag in {"1", "true", "yes", "on"}:
        validator._fail(
            category,
            "database engine flag",
            "ngafid.use.maria.db=true while docker stack is configured for MySQL",
            "set ngafid.use.maria.db=false or provide a MariaDB-compatible deployment",
        )
    else:
        validator._pass(category, "database engine flag", "MySQL mode is active")


def _validate_email_config(validator, category):
    email_enabled = validator._is_email_enabled()
    if not email_enabled:
        return

    admin_emails = validator._effective_property("ngafid.admin.emails") or ""
    email_info_file = validator._effective_property("ngafid.email.info")

    if not admin_emails.strip():
        validator._fail(
            category,
            "admin email values",
            "email enabled but ngafid.admin.emails is empty",
            "set ngafid.admin.emails/ngafid.docker.admin.emails to at least one address",
        )
    else:
        validator._pass(category, "admin email values", "admin email values are present")

    if not email_info_file:
        validator._fail(
            category,
            "email info file property",
            "could not resolve effective ngafid.email.info",
            "set ngafid.email.info/ngafid.docker.email.info to a readable file path",
        )
    else:
        validator._check_file_readable(
            category,
            "email info file",
            email_info_file,
            "ensure email info file exists and is mounted into runtime",
        )

    email_pattern = re.compile(r"^[^@\s]+@[^@\s]+\.[^@\s]+$")
    invalid_admin = [email for email in [e.strip() for e in admin_emails.split(";")] if email and not email_pattern.match(email)]
    if invalid_admin:
        validator._fail(
            category,
            "admin email format",
            f"invalid admin emails: {', '.join(invalid_admin)}",
            "set ngafid.admin.emails to semicolon-separated valid email addresses",
        )
    else:
        validator._pass(category, "admin email format", "admin email values are well-formed")
