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
    _validate_path_relationships(validator, category)
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
        "ngafid.docker.data.folder",
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


def _validate_path_relationships(validator, category):
    data_folder = validator._effective_property("ngafid.data.folder")
    upload_dir = validator._effective_property("ngafid.upload.dir")
    archive_dir = validator._effective_property("ngafid.archive.dir")
    terrain_dir = validator._effective_property("ngafid.terrain.dir")

    if not data_folder or not upload_dir or not archive_dir or not terrain_dir:
        return

    normalized_data = data_folder.rstrip("/") + "/"
    outside = []
    for label, path in (("upload", upload_dir), ("archive", archive_dir), ("terrain", terrain_dir)):
        normalized_path = path.rstrip("/") + "/"
        if not normalized_path.startswith(normalized_data):
            outside.append(f"{label}={path}")

    if outside:
        validator._pass(
            category,
            "data path hierarchy",
            f"using split-path layout outside ngafid.data.folder ({data_folder}): {', '.join(outside)}",
        )
    else:
        validator._pass(
            category,
            "data path hierarchy",
            "upload/archive/terrain paths align with ngafid.data.folder",
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

    smtp_host = validator._effective_property("ngafid.smtp.host") or ""
    smtp_user = validator._effective_property("ngafid.smtp.username") or ""
    smtp_password = validator._effective_property("ngafid.smtp.password") or ""
    smtp_from = validator._effective_property("ngafid.smtp.from") or ""
    smtp_port_value = validator._effective_property("ngafid.smtp.port") or ""
    admin_emails = validator._effective_property("ngafid.admin.emails") or ""

    missing = []
    if not smtp_host.strip():
        missing.append("ngafid.smtp.host")
    if not smtp_user.strip():
        missing.append("ngafid.smtp.username")
    if not smtp_password.strip():
        missing.append("ngafid.smtp.password")
    if not smtp_from.strip():
        missing.append("ngafid.smtp.from")
    if not admin_emails.strip():
        missing.append("ngafid.admin.emails")

    if missing:
        validator._fail(
            category,
            "email required fields",
            f"email enabled but missing values: {', '.join(missing)}",
            "set required ngafid.smtp.* and ngafid.admin.emails fields",
        )
    else:
        validator._pass(category, "email required fields", "email configuration fields are present")

    smtp_port = _parse_int(smtp_port_value)
    if smtp_port is None or smtp_port < 1 or smtp_port > 65535:
        validator._fail(
            category,
            "smtp port bounds",
            f"invalid smtp port '{smtp_port_value}'",
            "set ngafid.smtp.port to an integer in range 1-65535",
        )
    else:
        validator._pass(category, "smtp port bounds", f"ngafid.smtp.port={smtp_port}")

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
