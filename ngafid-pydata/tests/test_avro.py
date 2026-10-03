"""Unit tests for Avro type mapping and schema generation."""

from __future__ import annotations

import json
from collections.abc import Callable
from typing import Any

import avro.schema
import pytest
from mysql.connector import FieldType

from foundry_export import export_events


@pytest.mark.parametrize(
    ("type_code", "expected"),
    [
        (FieldType.TINY, "int"),
        (FieldType.SHORT, "int"),
        (FieldType.INT24, "int"),
        (FieldType.LONG, "int"),
        (FieldType.YEAR, "int"),
        (FieldType.LONGLONG, "long"),
        (FieldType.BIT, "long"),
        (FieldType.FLOAT, "float"),
        (FieldType.DOUBLE, "double"),
        (FieldType.DECIMAL, "double"),
        (FieldType.NEWDECIMAL, "double"),
        (FieldType.VAR_STRING, "string"),
        (FieldType.STRING, "string"),
        (FieldType.DATETIME, "string"),
        (FieldType.DATE, "string"),
        (FieldType.BLOB, "string"),
    ],
)
def test_avro_type_for(type_code: int, expected: str) -> None:
    """avro_type_for maps a MySQL type code to the expected Avro primitive.

    Args:
        type_code: The MySQL field-type code under test.
        expected: The Avro primitive type name expected for it.
    """
    assert export_events.avro_type_for(type_code) == expected


def test_avro_schema_marks_nullable_columns(
    make_column: Callable[..., tuple[Any, ...]],
) -> None:
    """Nullable columns become a ['null', type] union with a null default.

    Args:
        make_column: Fixture building DB-API description tuples.
    """
    description = [
        make_column("id", FieldType.LONG),
        make_column("airframe_type_id", FieldType.LONG, nullable=True),
    ]

    schema = export_events.avro_schema("event_definitions", description)

    assert schema["type"] == "record"
    assert schema["name"] == "event_definitions"
    assert schema["namespace"] == export_events.AVRO_NAMESPACE

    fields = {field["name"]: field for field in schema["fields"]}
    assert fields["id"]["type"] == "int"
    assert "default" not in fields["id"]
    assert fields["airframe_type_id"]["type"] == ["null", "int"]
    assert fields["airframe_type_id"]["default"] is None


def test_avro_schema_adds_foreign_key_references(
    make_column: Callable[..., tuple[Any, ...]],
) -> None:
    """Foreign-key columns get a 'references' attribute; others do not.

    Args:
        make_column: Fixture building DB-API description tuples.
    """
    description = [
        make_column("id", FieldType.LONG),
        make_column("type_id", FieldType.LONG),
    ]
    foreign_keys = {"type_id": ("airframe_types", "id")}

    schema = export_events.avro_schema("airframes", description, foreign_keys)
    fields = {field["name"]: field for field in schema["fields"]}

    assert fields["type_id"]["references"] == {"table": "airframe_types", "column": "id"}
    assert "references" not in fields["id"]


def test_avro_schema_preserves_column_order(
    make_column: Callable[..., tuple[Any, ...]],
) -> None:
    """Schema fields appear in the same order as the query columns.

    Args:
        make_column: Fixture building DB-API description tuples.
    """
    description = [
        make_column("a", FieldType.LONG),
        make_column("b", FieldType.VAR_STRING),
        make_column("c", FieldType.DOUBLE),
    ]

    schema = export_events.avro_schema("t", description)

    assert [field["name"] for field in schema["fields"]] == ["a", "b", "c"]


@pytest.mark.parametrize(
    ("raw_name", "expected_name"),
    [
        ("events", "events"),
        ("my-table", "my_table"),
        ("weird.name", "weird_name"),
        ("123nums", "_123nums"),
    ],
)
def test_avro_schema_sanitizes_record_name(
    make_column: Callable[..., tuple[Any, ...]],
    raw_name: str,
    expected_name: str,
) -> None:
    """Record names are sanitized to valid Avro identifiers.

    Args:
        make_column: Fixture building DB-API description tuples.
        raw_name: The raw base name passed to avro_schema.
        expected_name: The sanitized Avro record name expected.
    """
    schema = export_events.avro_schema(raw_name, [make_column("id", FieldType.LONG)])

    assert schema["name"] == expected_name


def test_avro_schema_parses_with_avro_library(
    make_column: Callable[..., tuple[Any, ...]],
) -> None:
    """A generated schema is accepted by the reference Avro parser.

    Args:
        make_column: Fixture building DB-API description tuples.
    """
    description = [
        make_column("id", FieldType.LONG),
        make_column("name", FieldType.VAR_STRING),
        make_column("value", FieldType.DOUBLE, nullable=True),
    ]

    schema = export_events.avro_schema("sample", description)
    parsed = avro.schema.parse(json.dumps(schema))

    assert parsed.type == "record"
    assert parsed.name == "sample"
