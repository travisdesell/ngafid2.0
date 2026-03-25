# flake8: noqa: E501
import importlib
from pathlib import Path


def run_check(validator):
    category = "DB"
    try:
        mysql_connector = importlib.import_module("mysql.connector")
    except ImportError as exc:
        validator._fail(
            category,
            "mysql driver",
            str(exc),
            "install validator dependencies in ngafid-validate/requirements.txt",
        )
        return

    db_props = validator._parse_properties(Path("/etc/ngafid-db.conf"))
    for key in ("url", "username", "password"):
        if key not in db_props or not db_props[key].strip():
            validator._fail(
                category,
                f"db config key {key}",
                "missing or empty",
                "set url/username/password in liquibase.docker.properties",
            )
            return

    parsed = validator._parse_jdbc_mysql(db_props["url"])
    if not parsed:
        validator._fail(
            category,
            "jdbc parse",
            f"unsupported JDBC URL: {db_props['url']}",
            "use jdbc:mysql://<host>:<port>/<database>",
        )
        return

    host, port, db_name = parsed
    try:
        conn = mysql_connector.connect(
            host=host,
            port=port,
            user=db_props["username"],
            password=db_props["password"],
            database=db_name,
            connection_timeout=validator.args.timeout,
        )
        validator._pass(
            category,
            "mysql connection",
            f"connected to {host}:{port}/{db_name}",
        )
    except Exception as exc:  # pylint: disable=broad-except
        validator._fail(
            category,
            "mysql connection",
            str(exc),
            "verify DB host/port/credentials and service readiness",
        )
        return

    try:
        cursor = conn.cursor()
        _check_mysql_server_info(validator, category, cursor)
        _check_table_sets(validator, category, cursor, db_name)
        _check_critical_columns(validator, category, cursor, db_name)
        _check_liquibase_state(validator, category, cursor, db_name)
        _check_seed_data(validator, category, cursor)
        _check_transaction_and_write_capability(validator, category, conn, cursor)
    except Exception as exc:  # pylint: disable=broad-except
        validator._fail(
            category,
            "schema tables",
            str(exc),
            "verify database permissions and schema",
        )
    finally:
        conn.close()


def _fetch_table_set(cursor, db_name, table_names):
    expected_lower = [t.lower() for t in table_names]
    placeholders = ", ".join(["%s"] * len(expected_lower))
    query = (
        "SELECT table_name FROM information_schema.tables "
        "WHERE table_schema = %s AND LOWER(table_name) IN ("
        + placeholders
        + ")"
    )
    cursor.execute(query, [db_name] + expected_lower)
    return {row[0].lower() for row in cursor.fetchall()}


def _check_mysql_server_info(validator, category, cursor):
    cursor.execute("SELECT VERSION()")
    version = str(cursor.fetchone()[0])
    validator._pass(category, "mysql version", version)

    version_parts = []
    for part in version.split(".")[:2]:
        digits = "".join(ch for ch in part if ch.isdigit())
        if digits:
            version_parts.append(int(digits))
    if len(version_parts) >= 2 and (version_parts[0] < 8):
        validator._fail(
            category,
            "mysql version floor",
            f"mysql version {version} is below expected major version 8",
            "use MySQL 8.0+ for NGAFID runtime compatibility",
        )
    else:
        validator._pass(category, "mysql version floor", "MySQL major version is compatible")

    cursor.execute("SELECT @@character_set_database")
    charset = str(cursor.fetchone()[0]).lower()
    if charset != "utf8mb4":
        validator._fail(
            category,
            "database charset",
            f"database charset is '{charset}', expected 'utf8mb4'",
            "configure database/default charset to utf8mb4",
        )
    else:
        validator._pass(category, "database charset", "utf8mb4")


def _check_table_sets(validator, category, cursor, db_name):
    found_core = _fetch_table_set(cursor, db_name, validator.schema_tables)
    missing_core = [t for t in validator.schema_tables if t.lower() not in found_core]
    if missing_core:
        validator._fail(
            category,
            "schema tables",
            f"missing tables: {', '.join(missing_core)}",
            "run liquibase update against the same DB config (e.g. mvn liquibase:update -Dliquibase.propertyFile=liquibase.docker.properties)",
        )
    else:
        validator._pass(category, "schema tables", "required core schema tables exist")

    found_critical = _fetch_table_set(cursor, db_name, validator.critical_db_tables)
    missing_critical = [t for t in validator.critical_db_tables if t.lower() not in found_critical]
    if missing_critical:
        validator._fail(
            category,
            "critical tables",
            f"missing critical tables: {', '.join(missing_critical)}",
            "apply full Liquibase changelog set and verify database initialization",
        )
    else:
        validator._pass(category, "critical tables", "critical runtime tables are present")

    found_mv = _fetch_table_set(cursor, db_name, validator.materialized_view_tables)
    missing_mv = [t for t in validator.materialized_view_tables if t.lower() not in found_mv]
    if missing_mv:
        validator._fail(
            category,
            "materialized view tables",
            f"missing materialized view tables: {', '.join(missing_mv)}",
            "run Liquibase migrations for 02-views changelog files",
        )
    else:
        validator._pass(category, "materialized view tables", "materialized view tables are present")


def _resolve_table_name(cursor, db_name, logical_name):
    cursor.execute(
        "SELECT table_name FROM information_schema.tables WHERE table_schema = %s AND LOWER(table_name) = %s LIMIT 1",
        (db_name, logical_name.lower()),
    )
    row = cursor.fetchone()
    return row[0] if row else None


def _check_liquibase_state(validator, category, cursor, db_name):
    changelog_table_name = _resolve_table_name(cursor, db_name, "databasechangelog")
    if not changelog_table_name:
        validator._fail(
            category,
            "liquibase changelog entries",
            "could not resolve databasechangelog table name from information_schema",
            "run Liquibase migrations and verify changelog tables exist",
        )
        return

    cursor.execute(f"SELECT COUNT(*) FROM `{changelog_table_name}`")
    changelog_count = int(cursor.fetchone()[0])
    if changelog_count < 20:
        validator._fail(
            category,
            "liquibase changelog entries",
            f"databasechangelog has suspiciously low row count: {changelog_count}",
            "run liquibase update and verify all changesets are applied",
        )
    else:
        validator._pass(
            category,
            "liquibase changelog entries",
            f"databasechangelog row count={changelog_count}",
        )


def _check_seed_data(validator, category, cursor):
    cursor.execute("SELECT COUNT(*) FROM event_definitions WHERE fleet_id = 0")
    default_event_defs = int(cursor.fetchone()[0])
    if default_event_defs < 20:
        validator._fail(
            category,
            "event definitions seed data",
            f"fleet_id=0 event definition count too low: {default_event_defs}",
            "ensure event definition seed inserts were applied",
        )
    else:
        validator._pass(
            category,
            "event definitions seed data",
            f"fleet_id=0 count={default_event_defs}",
        )

    cursor.execute("SELECT COUNT(*) FROM airframe_types")
    airframe_types_count = int(cursor.fetchone()[0])
    if airframe_types_count < 4:
        validator._fail(
            category,
            "airframe types seed data",
            f"airframe_types count too low: {airframe_types_count}",
            "ensure airframe_types seed inserts were applied",
        )
    else:
        validator._pass(
            category,
            "airframe types seed data",
            f"airframe_types count={airframe_types_count}",
        )

    cursor.execute("SELECT COUNT(*) FROM event_metadata_keys")
    metadata_keys_count = int(cursor.fetchone()[0])
    if metadata_keys_count < 2:
        validator._fail(
            category,
            "event metadata keys seed data",
            f"event_metadata_keys count too low: {metadata_keys_count}",
            "ensure event metadata key seed inserts were applied",
        )
    else:
        validator._pass(
            category,
            "event metadata keys seed data",
            f"event_metadata_keys count={metadata_keys_count}",
        )


def _check_critical_columns(validator, category, cursor, db_name):
    required_columns = {
        "user": ["id"],
        "fleet": ["id"],
        "uploads": ["id"],
        "flights": ["id"],
        "event_definitions": ["id", "fleet_id"],
    }

    missing = []
    for table_name, columns in required_columns.items():
        placeholders = ", ".join(["%s"] * len(columns))
        query = (
            "SELECT LOWER(column_name) FROM information_schema.columns "
            "WHERE table_schema = %s AND LOWER(table_name) = %s AND LOWER(column_name) IN ("
            + placeholders
            + ")"
        )
        cursor.execute(query, [db_name, table_name.lower()] + [column.lower() for column in columns])
        found_columns = {row[0] for row in cursor.fetchall()}
        for column in columns:
            if column.lower() not in found_columns:
                missing.append(f"{table_name}.{column}")

    if missing:
        validator._fail(
            category,
            "critical column structure",
            f"missing expected columns: {', '.join(missing)}",
            "apply latest schema migrations and verify table definitions",
        )
    else:
        validator._pass(category, "critical column structure", "required columns exist on core tables")


def _check_transaction_and_write_capability(validator, category, conn, cursor):
    temp_table = "validator_transaction_probe"

    try:
        cursor.execute(f"DROP TEMPORARY TABLE IF EXISTS {temp_table}")
        cursor.execute(f"CREATE TEMPORARY TABLE {temp_table} (id INT NOT NULL)")

        conn.rollback()
        conn.start_transaction()
        cursor.execute(f"INSERT INTO {temp_table} (id) VALUES (1)")
        conn.rollback()

        cursor.execute(f"SELECT COUNT(*) FROM {temp_table}")
        row_count = int(cursor.fetchone()[0])

        if row_count != 0:
            validator._fail(
                category,
                "transaction rollback",
                f"rollback probe left {row_count} row(s) in temporary table",
                "verify transactional engine settings and DB transaction behavior",
            )
        else:
            validator._pass(
                category,
                "transaction rollback",
                "write/rollback probe succeeded",
            )
    except Exception as exc:  # pylint: disable=broad-except
        validator._fail(
            category,
            "transaction rollback",
            str(exc),
            "ensure DB user can create temporary tables and execute transactions",
        )
    finally:
        try:
            cursor.execute(f"DROP TEMPORARY TABLE IF EXISTS {temp_table}")
        except Exception:  # pylint: disable=broad-except
            pass
