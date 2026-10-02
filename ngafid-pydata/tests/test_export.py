"""Unit tests for query execution and CSV/Avro file export."""

from __future__ import annotations

import csv
import json
from pathlib import Path

import avro.schema

from foundry_export import export_events
from tests.conftest import FakeConnection


def test_export_query_writes_csv_and_avsc(
    fake_connection: FakeConnection,
    tmp_path: Path,
) -> None:
    """export_query writes both the CSV and a valid Avro schema, and counts rows.

    Args:
        fake_connection: A healthy fake database connection.
        tmp_path: Temporary directory for the output files.
    """
    output_path = tmp_path / "airframe_types.csv"

    count = export_events.export_query(
        fake_connection,
        "airframe_types",
        "SELECT id, name FROM airframe_types",
        (),
        output_path,
    )

    assert count == 1
    assert output_path.exists()

    with output_path.open(newline="") as csv_file:
        rows = list(csv.reader(csv_file))
    assert rows[0] == ["id", "name"]
    assert rows[1] == ["1", "Fixed Wing Single Engine"]

    schema_path = output_path.with_suffix(".avsc")
    assert schema_path.exists()
    schema = json.loads(schema_path.read_text())
    parsed = avro.schema.parse(json.dumps(schema))
    assert parsed.name == "airframe_types"


def test_export_query_writes_null_as_empty_csv_field(
    fake_connection: FakeConnection,
    tmp_path: Path,
) -> None:
    """A NULL column value is written as an empty CSV field.

    Args:
        fake_connection: A healthy fake database connection.
        tmp_path: Temporary directory for the output files.
    """
    output_path = tmp_path / "event_definitions.csv"

    export_events.export_query(
        fake_connection,
        "event_definitions",
        "SELECT id, fleet_id, airframe_id, airframe_type_id, name FROM event_definitions",
        (),
        output_path,
    )

    with output_path.open(newline="") as csv_file:
        rows = list(csv.reader(csv_file))
    # airframe_type_id is NULL in the canned row -> empty string field.
    assert rows[1] == ["10", "1", "3", "", "low fuel"]


def test_fetch_foreign_keys_returns_column_mapping(
    fake_connection: FakeConnection,
) -> None:
    """fetch_foreign_keys maps FK columns to their referenced table/column.

    Args:
        fake_connection: A healthy fake database connection.
    """
    references = export_events.fetch_foreign_keys(fake_connection, "airframes")

    assert references == {"type_id": ("airframe_types", "id")}


def test_fetch_foreign_keys_merges_supplemental_keys(
    fake_connection: FakeConnection,
) -> None:
    """fetch_foreign_keys adds undeclared logical FKs from the supplement.

    Args:
        fake_connection: A healthy fake database connection.
    """
    references = export_events.fetch_foreign_keys(fake_connection, "event_definitions")

    assert references == {
        "airframe_type_id": ("airframe_types", "id"),  # declared constraint
        "fleet_id": ("fleet", "id"),  # supplemental (undeclared)
        "airframe_id": ("airframes", "id"),  # supplemental (undeclared)
    }


def test_export_query_annotates_foreign_keys_in_schema(
    fake_connection: FakeConnection,
    tmp_path: Path,
) -> None:
    """The generated schema tags FK columns with a 'references' attribute.

    Args:
        fake_connection: A healthy fake database connection.
        tmp_path: Temporary directory for the output files.
    """
    output_path = tmp_path / "event_definitions.csv"

    export_events.export_query(
        fake_connection,
        "event_definitions",
        "SELECT id, fleet_id, airframe_id, airframe_type_id, name FROM event_definitions",
        (),
        output_path,
    )

    schema = json.loads(output_path.with_suffix(".avsc").read_text())
    fields = {field["name"]: field for field in schema["fields"]}

    assert fields["airframe_type_id"]["references"] == {
        "table": "airframe_types",
        "column": "id",
    }
    # Supplemental (undeclared) logical foreign keys are annotated too.
    assert fields["fleet_id"]["references"] == {"table": "fleet", "column": "id"}
    assert fields["airframe_id"]["references"] == {"table": "airframes", "column": "id"}
    # A non-foreign-key column carries no references metadata.
    assert "references" not in fields["name"]
    # The schema still parses as valid Avro with the extra field attribute.
    avro.schema.parse(json.dumps(schema))
