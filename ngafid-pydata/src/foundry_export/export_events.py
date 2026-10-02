"""Export NGAFID events and related reference tables to CSV files.

Connects to the NGAFID MySQL database (by default over a local SSH tunnel) and
writes a set of CSV files into an output directory:

* ``events.csv`` — every row from the ``events`` table whose ``start_time``
  falls within an inclusive ``[start_date, end_date]`` range.
* ``flights.csv`` — the ``id``, ``fleet_id``, ``system_id``, ``airframe_id``,
  ``start_time``, and ``end_time`` columns of the flights referenced by those
  events (via ``events.flight_id``), fetched with a single semi-join query so
  the flight-id set never has to be materialized on the client.
* ``event_definitions.csv`` — the ``id``, ``fleet_id``, ``airframe_id``,
  ``airframe_type_id``, and ``name`` columns from the ``event_definitions``
  table.
* ``airframes.csv`` — the ``id``, ``airframe``, and ``type_id`` columns from
  the ``airframes`` table.
* ``airframe_types.csv`` — the ``id`` and ``name`` columns from the
  ``airframe_types`` table.
* ``tails.csv`` — the ``system_id``, ``fleet_id``, ``tail``, and ``confirmed``
  columns from the ``tails`` table.

Alongside each ``*.csv`` file, a matching Avro schema (``*.avsc``) describing
its columns is written, so every CSV export is paired with a schema. Foreign-key
columns are annotated in the schema with a ``"references"`` attribute naming the
referenced table and column.

The output directory is created recursively if it does not already exist. All
connection details and the date range are supplied on the command line via
:mod:`argparse`.

Example:
    ngafid-export-events \\
        --start-date 2024-01-01 --end-date 2024-01-31 \\
        --output-dir export/2024-01
"""

from __future__ import annotations

import argparse
import csv
import getpass
import json
import re
import sys
from datetime import date, datetime, time
from pathlib import Path
from typing import Any

import mysql.connector
from mysql.connector import FieldType
from mysql.connector.abstracts import MySQLConnectionAbstract
from tqdm import tqdm

# Default connection parameters. The port matches the local SSH tunnel set up
# in ngafid-pydata/database_tunnel.sh (server 3306 forwarded to local 3306).
DEFAULT_HOST = "127.0.0.1"
DEFAULT_PORT = 3306
DEFAULT_USER = "ngafid_user"
DEFAULT_DATABASE = "ngafid"

# CSV filenames written into the output directory.
EVENTS_FILENAME = "events.csv"
FLIGHTS_FILENAME = "flights.csv"
EVENT_DEFINITIONS_FILENAME = "event_definitions.csv"
AIRFRAMES_FILENAME = "airframes.csv"
AIRFRAME_TYPES_FILENAME = "airframe_types.csv"
TAILS_FILENAME = "tails.csv"

# Number of rows fetched per round trip while streaming a result set to CSV.
# Streaming (rather than fetching everything at once) keeps memory bounded and
# lets the progress bar advance during large fetches.
FETCH_BATCH_SIZE = 1000

# Avro namespace used for the generated record schemas.
AVRO_NAMESPACE = "org.ngafid.foundry"

# MySQL field-type codes grouped by the Avro primitive they map to. Any type
# not listed here (dates, datetimes, strings, blobs, enums, ...) maps to an
# Avro "string", matching how it is serialized into the CSV.
_AVRO_INT_TYPES = frozenset({FieldType.TINY, FieldType.SHORT, FieldType.INT24, FieldType.LONG, FieldType.YEAR})
_AVRO_LONG_TYPES = frozenset({FieldType.LONGLONG, FieldType.BIT})
_AVRO_FLOAT_TYPES = frozenset({FieldType.FLOAT})
_AVRO_DOUBLE_TYPES = frozenset({FieldType.DOUBLE, FieldType.DECIMAL, FieldType.NEWDECIMAL})

# Logical foreign keys that are not declared as constraints in the database and
# so do not appear in information_schema, keyed by table name then column name
# and mapping to a (referenced_table, referenced_column) pair. These supplement
# the declared foreign keys; a declared constraint always takes precedence.
SUPPLEMENTAL_FOREIGN_KEYS: dict[str, dict[str, tuple[str, str]]] = {
    "event_definitions": {
        "fleet_id": ("fleet", "id"),
        "airframe_id": ("airframes", "id"),
    },
}


def valid_date(value: str) -> date:
    """Parse a ``YYYY-MM-DD`` command-line string into a :class:`date`.

    Args:
        value: The raw command-line argument, expected in ISO ``YYYY-MM-DD``
            format.

    Returns:
        The parsed calendar date.

    Raises:
        argparse.ArgumentTypeError: If ``value`` is not a valid ISO date, so
            that argparse reports a clean usage error.
    """
    try:
        return datetime.strptime(value, "%Y-%m-%d").date()
    except ValueError as exc:
        raise argparse.ArgumentTypeError(f"invalid date '{value}', expected YYYY-MM-DD") from exc


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    """Build the argument parser and parse the command-line arguments.

    Args:
        argv: Optional argument vector (excluding the program name); when
            ``None``, :data:`sys.argv` is used.

    Returns:
        The populated argparse namespace with connection, date-range, and
        output-directory options.
    """
    parser = argparse.ArgumentParser(
        description=(
            "Export NGAFID events within a date range, plus the "
            "event_definitions, airframes, and airframe_types reference "
            "tables, to CSV files in an output directory."
        )
    )

    parser.add_argument(
        "--host",
        default=DEFAULT_HOST,
        help=f"MySQL host (default: {DEFAULT_HOST}).",
    )
    parser.add_argument(
        "--port",
        type=int,
        default=DEFAULT_PORT,
        help=f"MySQL port (default: {DEFAULT_PORT}).",
    )
    parser.add_argument(
        "--user",
        default=DEFAULT_USER,
        help=f"MySQL user (default: {DEFAULT_USER}).",
    )
    parser.add_argument(
        "--database",
        default=DEFAULT_DATABASE,
        help=f"MySQL database (default: {DEFAULT_DATABASE}).",
    )
    parser.add_argument(
        "--password",
        default=None,
        help=(
            "MySQL password. If omitted, you are prompted interactively so the password is not stored in shell history."
        ),
    )

    parser.add_argument(
        "--start-date",
        required=True,
        type=valid_date,
        help="Inclusive start date (YYYY-MM-DD), matched against start_time.",
    )
    parser.add_argument(
        "--end-date",
        required=True,
        type=valid_date,
        help="Inclusive end date (YYYY-MM-DD), matched against start_time.",
    )

    parser.add_argument(
        "--output-dir",
        required=True,
        help=("Directory to write the CSV files into. Created recursively if it does not already exist."),
    )

    return parser.parse_args(argv)


def fetch_foreign_keys(
    connection: MySQLConnectionAbstract,
    table_name: str,
) -> dict[str, tuple[str, str]]:
    """Look up the foreign-key references declared on a table.

    Reads ``information_schema.KEY_COLUMN_USAGE`` for the current database to
    find every column of ``table_name`` that references another table, then
    merges in any logical foreign keys from :data:`SUPPLEMENTAL_FOREIGN_KEYS`
    that are not declared as database constraints. Declared constraints take
    precedence over supplemental entries.

    Args:
        connection: An open MySQL connection to the NGAFID database.
        table_name: The name of the table whose foreign keys are requested.

    Returns:
        A mapping from local column name to a ``(referenced_table,
        referenced_column)`` tuple. Empty if the table has neither declared nor
        supplemental foreign keys.
    """
    query = (
        "SELECT COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME "
        "FROM information_schema.KEY_COLUMN_USAGE "
        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = %s "
        "AND REFERENCED_TABLE_NAME IS NOT NULL "
        "ORDER BY CONSTRAINT_NAME, ORDINAL_POSITION"
    )

    cursor = connection.cursor()
    try:
        cursor.execute(query, (table_name,))
        rows = cursor.fetchall()
    finally:
        cursor.close()

    # A column can appear in more than one constraint (e.g. flights.fleet_id
    # references both fleet and, as part of a composite key, tails); keep the
    # first constraint per column so the result is deterministic.
    references: dict[str, tuple[str, str]] = {}
    for column_name, referenced_table, referenced_column in rows:
        references.setdefault(column_name, (referenced_table, referenced_column))

    # Fill in logical foreign keys that are not declared as DB constraints,
    # without overriding any that are.
    for column_name, target in SUPPLEMENTAL_FOREIGN_KEYS.get(table_name, {}).items():
        references.setdefault(column_name, target)

    return references


def avro_type_for(type_code: int) -> str:
    """Map a MySQL DB-API field-type code to an Avro primitive type name.

    Numeric MySQL types map to the corresponding Avro numeric primitive; every
    other type (dates, datetimes, strings, blobs, enums, ...) maps to
    ``"string"``, matching how the value is serialized into the CSV.

    Args:
        type_code: The DB-API ``type_code`` from ``cursor.description`` (a
            :class:`mysql.connector.FieldType` constant).

    Returns:
        The Avro primitive type name: ``"int"``, ``"long"``, ``"float"``,
        ``"double"``, or ``"string"``.
    """
    if type_code in _AVRO_INT_TYPES:
        return "int"
    if type_code in _AVRO_LONG_TYPES:
        return "long"
    if type_code in _AVRO_FLOAT_TYPES:
        return "float"
    if type_code in _AVRO_DOUBLE_TYPES:
        return "double"
    return "string"


def avro_schema(
    record_name: str,
    description: list[tuple[Any, ...]],
    foreign_keys: dict[str, tuple[str, str]] | None = None,
) -> dict[str, Any]:
    """Build an Avro record schema describing a query's result columns.

    Nullable columns (per the DB-API ``null_ok`` flag) are emitted as an Avro
    union ``["null", <type>]`` with a default of ``null``. Columns that are
    foreign keys carry a custom ``"references"`` attribute of the form
    ``{"table": <table>, "column": <column>}`` as field metadata.

    Args:
        record_name: Base name for the Avro record; sanitized to a valid Avro
            name (letters, digits, and underscores, not starting with a digit).
        description: The DB-API ``cursor.description`` for the query, one
            ``(name, type_code, ..., null_ok, ...)`` tuple per column.
        foreign_keys: Mapping from column name to a ``(referenced_table,
            referenced_column)`` tuple; columns present here are annotated with
            a ``"references"`` attribute. Defaults to no foreign keys.

    Returns:
        The Avro schema as a JSON-serializable dictionary.
    """
    foreign_keys = foreign_keys or {}

    safe_name = re.sub(r"\W", "_", record_name)
    if not safe_name or safe_name[0].isdigit():
        safe_name = f"_{safe_name}"

    fields: list[dict[str, Any]] = []
    for column in description:
        name = column[0]
        type_code = column[1]
        null_ok = bool(column[6])

        avro_type = avro_type_for(type_code)
        field: dict[str, Any] = {"name": name}
        if null_ok:
            field["type"] = ["null", avro_type]
            field["default"] = None
        else:
            field["type"] = avro_type

        if name in foreign_keys:
            referenced_table, referenced_column = foreign_keys[name]
            field["references"] = {
                "table": referenced_table,
                "column": referenced_column,
            }

        fields.append(field)

    return {
        "type": "record",
        "name": safe_name,
        "namespace": AVRO_NAMESPACE,
        "fields": fields,
    }


def export_query(
    connection: MySQLConnectionAbstract,
    table_name: str,
    query: str,
    params: tuple[object, ...],
    output_path: Path,
) -> int:
    """Run a query and write its results to a CSV file plus an Avro schema.

    Streams the result rows in batches of :data:`FETCH_BATCH_SIZE`, writing
    them to ``output_path`` as CSV while a :mod:`tqdm` progress bar (labeled
    with ``table_name``) advances so the query's progress is visible. An Avro
    schema describing the columns is written to a sibling file with the
    ``.avsc`` extension, so any export routed through this function always
    produces both files, with foreign-key references (looked up from
    ``table_name``) embedded as field metadata.

    Args:
        connection: An open MySQL connection to the NGAFID database.
        table_name: The source table, used to look up foreign-key references
            to embed in the Avro schema and to label the progress bar.
        query: The SQL query to execute, using ``%s`` placeholders.
        params: The positional parameters bound to the query placeholders.
        output_path: Destination path for the CSV file; overwritten if it
            already exists. The Avro schema is written alongside it with the
            ``.avsc`` extension.

    Returns:
        The number of data rows written (excluding the header row).
    """
    cursor = connection.cursor()
    try:
        cursor.execute(query, params)
        description: list[tuple[Any, ...]] = list(cursor.description)
        columns = [column[0] for column in description]

        row_count = 0
        with open(output_path, "w", newline="", encoding="utf-8") as csv_file:
            writer = csv.writer(csv_file)
            writer.writerow(columns)
            with tqdm(desc=table_name, unit="row", disable=None) as progress:
                while True:
                    batch = cursor.fetchmany(FETCH_BATCH_SIZE)
                    if not batch:
                        break
                    writer.writerows(batch)
                    row_count += len(batch)
                    progress.update(len(batch))
    finally:
        cursor.close()

    foreign_keys = fetch_foreign_keys(connection, table_name)
    schema = avro_schema(output_path.stem, description, foreign_keys)
    schema_path = output_path.with_suffix(".avsc")
    with open(schema_path, "w", encoding="utf-8") as schema_file:
        json.dump(schema, schema_file, indent=2)
        schema_file.write("\n")

    return row_count


def main(argv: list[str] | None = None) -> int:
    """Entry point: parse arguments, query the database, and write the CSVs.

    Writes ``events.csv`` (filtered by the date range) plus the
    ``event_definitions.csv``, ``airframes.csv``, and ``airframe_types.csv``
    reference tables into ``--output-dir``, which is created recursively if
    needed. Status lines are written to standard error.

    Args:
        argv: Optional argument vector (excluding the program name); when
            ``None``, :data:`sys.argv` is used.

    Returns:
        A process exit code: ``0`` on success, ``1`` if the start date is
        after the end date, and ``2`` if the output directory cannot be
        created or the database connection or a query fails.
    """
    args = parse_args(argv)

    if args.start_date > args.end_date:
        print(
            f"error: start date {args.start_date} is after end date {args.end_date}",
            file=sys.stderr,
        )
        return 1

    output_dir = Path(args.output_dir)
    try:
        output_dir.mkdir(parents=True, exist_ok=True)
    except OSError as exc:
        print(
            f"error: could not create output directory {output_dir}: {exc}",
            file=sys.stderr,
        )
        return 2

    # Expand the inclusive date range to cover the full end day.
    start_dt = datetime.combine(args.start_date, time.min)
    end_dt = datetime.combine(args.end_date, time.max)

    # Each export is a (filename, table, query, params) tuple. events.csv is
    # filtered by the date range; the reference tables are exported in full.
    # The table name drives the foreign-key lookup embedded in each schema.
    exports: list[tuple[str, str, str, tuple[object, ...]]] = [
        (
            EVENTS_FILENAME,
            "events",
            "SELECT * FROM events WHERE start_time >= %s AND start_time <= %s ORDER BY start_time",
            (start_dt, end_dt),
        ),
        (
            # Flights referenced by the queried events. The semi-join keeps the
            # flight-id set in the database rather than shipping it back to the
            # client, which matters when the events span many thousands of
            # unique flights.
            FLIGHTS_FILENAME,
            "flights",
            "SELECT id, fleet_id, system_id, airframe_id, start_time, end_time "
            "FROM flights "
            "WHERE id IN ("
            "SELECT flight_id FROM events "
            "WHERE start_time >= %s AND start_time <= %s"
            ") "
            "ORDER BY id",
            (start_dt, end_dt),
        ),
        (
            EVENT_DEFINITIONS_FILENAME,
            "event_definitions",
            "SELECT id, fleet_id, airframe_id, airframe_type_id, name FROM event_definitions ORDER BY id",
            (),
        ),
        (
            AIRFRAMES_FILENAME,
            "airframes",
            "SELECT id, airframe, type_id FROM airframes ORDER BY id",
            (),
        ),
        (
            AIRFRAME_TYPES_FILENAME,
            "airframe_types",
            "SELECT id, name FROM airframe_types ORDER BY id",
            (),
        ),
        (
            TAILS_FILENAME,
            "tails",
            "SELECT system_id, fleet_id, tail, confirmed FROM tails ORDER BY fleet_id, system_id",
            (),
        ),
    ]

    password = args.password
    if password is None:
        password = getpass.getpass(f"Password for {args.user}@{args.host}: ")

    try:
        connection = mysql.connector.connect(
            host=args.host,
            port=args.port,
            user=args.user,
            password=password,
            database=args.database,
        )
    except mysql.connector.Error as exc:
        print(f"error: could not connect to database: {exc}", file=sys.stderr)
        return 2

    try:
        for filename, table_name, query, params in exports:
            output_path = output_dir / filename
            count = export_query(connection, table_name, query, params, output_path)
            schema_name = output_path.with_suffix(".avsc").name
            print(
                f"wrote {count} row(s) to {output_path} (+ schema {schema_name})",
                file=sys.stderr,
            )
    except mysql.connector.Error as exc:
        print(f"error: query failed: {exc}", file=sys.stderr)
        return 2
    finally:
        connection.close()

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
