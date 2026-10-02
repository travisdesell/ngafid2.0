"""Unit tests for argument parsing and the main entry point."""

from __future__ import annotations

import argparse
import json
from collections.abc import Callable
from datetime import date, datetime
from pathlib import Path
from typing import Any

import avro.schema
import mysql.connector
import pytest

from foundry_export import export_events
from tests.conftest import FakeConnection

# Common valid arguments; individual tests append an --output-dir.
BASE_ARGS = [
    "--start-date",
    "2024-01-01",
    "--end-date",
    "2024-01-31",
    "--password",
    "secret",
]

EXPECTED_FILENAMES = {
    "events.csv",
    "events.avsc",
    "flights.csv",
    "flights.avsc",
    "event_definitions.csv",
    "event_definitions.avsc",
    "airframes.csv",
    "airframes.avsc",
    "airframe_types.csv",
    "airframe_types.avsc",
    "tails.csv",
    "tails.avsc",
}


def test_valid_date_parses_iso() -> None:
    """valid_date parses an ISO YYYY-MM-DD string into a date."""
    assert export_events.valid_date("2024-02-29") == date(2024, 2, 29)


def test_valid_date_rejects_bad_value() -> None:
    """valid_date raises ArgumentTypeError on a malformed date."""
    with pytest.raises(argparse.ArgumentTypeError):
        export_events.valid_date("2024-13-40")


def test_parse_args_uses_connection_defaults(tmp_path: Path) -> None:
    """parse_args fills in the documented connection defaults.

    Args:
        tmp_path: Temporary directory used as the output directory.
    """
    args = export_events.parse_args([*BASE_ARGS, "--output-dir", str(tmp_path)])

    assert args.host == export_events.DEFAULT_HOST
    assert args.port == export_events.DEFAULT_PORT
    assert args.user == export_events.DEFAULT_USER
    assert args.database == export_events.DEFAULT_DATABASE
    assert args.start_date == date(2024, 1, 1)
    assert args.output_dir == str(tmp_path)


def test_parse_args_requires_output_dir() -> None:
    """parse_args exits when --output-dir is missing."""
    with pytest.raises(SystemExit):
        export_events.parse_args(BASE_ARGS)


def test_main_returns_1_when_start_after_end(tmp_path: Path) -> None:
    """main returns 1 and writes nothing when start date is after end date.

    Args:
        tmp_path: Temporary directory used as the output directory.
    """
    output_dir = tmp_path / "out"

    exit_code = export_events.main(
        [
            "--start-date",
            "2024-02-01",
            "--end-date",
            "2024-01-01",
            "--password",
            "secret",
            "--output-dir",
            str(output_dir),
        ]
    )

    assert exit_code == 1
    assert not output_dir.exists()


def test_main_writes_all_csv_and_avsc(
    fake_connection: FakeConnection,
    patch_connect: Callable[[Any], None],
    tmp_path: Path,
) -> None:
    """main writes every CSV/AVSC pair into a recursively created directory.

    Args:
        fake_connection: A healthy fake database connection.
        patch_connect: Helper to patch mysql.connector.connect.
        tmp_path: Temporary directory root for the (nested) output directory.
    """
    patch_connect(fake_connection)
    output_dir = tmp_path / "nested" / "2024-01"

    exit_code = export_events.main([*BASE_ARGS, "--output-dir", str(output_dir)])

    assert exit_code == 0
    assert {path.name for path in output_dir.iterdir()} == EXPECTED_FILENAMES
    assert fake_connection.closed

    for avsc_path in output_dir.glob("*.avsc"):
        avro.schema.parse(avsc_path.read_text())


def test_main_binds_full_day_date_range(
    fake_connection: FakeConnection,
    patch_connect: Callable[[Any], None],
    tmp_path: Path,
) -> None:
    """main binds the events query to the full inclusive day range.

    Args:
        fake_connection: A healthy fake database connection.
        patch_connect: Helper to patch mysql.connector.connect.
        tmp_path: Temporary directory root for the output directory.
    """
    patch_connect(fake_connection)

    export_events.main([*BASE_ARGS, "--output-dir", str(tmp_path / "out")])

    events_params = [params for query, params in fake_connection.executed if query.startswith("SELECT * FROM events")]
    assert len(events_params) == 1
    start, end = events_params[0]
    assert start == datetime(2024, 1, 1, 0, 0, 0)
    assert end == datetime(2024, 1, 31, 23, 59, 59, 999999)


def test_main_exports_flights_via_semijoin(
    fake_connection: FakeConnection,
    patch_connect: Callable[[Any], None],
    tmp_path: Path,
) -> None:
    """flights.csv is filled by a date-bound semi-join and annotated with FKs.

    Args:
        fake_connection: A healthy fake database connection.
        patch_connect: Helper to patch mysql.connector.connect.
        tmp_path: Temporary directory root for the output directory.
    """
    patch_connect(fake_connection)
    output_dir = tmp_path / "out"

    export_events.main([*BASE_ARGS, "--output-dir", str(output_dir)])

    # The flights query is a single semi-join that keeps the flight-id set in
    # the database, bound to the full-day date range.
    flights_calls = [
        (query, params)
        for query, params in fake_connection.executed
        if query.startswith("SELECT id, fleet_id, system_id")
    ]
    assert len(flights_calls) == 1
    flights_query, flights_params = flights_calls[0]
    assert "IN (SELECT flight_id FROM events" in flights_query
    assert flights_params == (
        datetime(2024, 1, 1, 0, 0, 0),
        datetime(2024, 1, 31, 23, 59, 59, 999999),
    )

    schema = json.loads((output_dir / "flights.avsc").read_text())
    fields = {field["name"]: field for field in schema["fields"]}
    # fleet_id references both fleet and (composite) tails; fleet wins.
    assert fields["fleet_id"]["references"] == {"table": "fleet", "column": "id"}
    assert fields["system_id"]["references"] == {"table": "tails", "column": "system_id"}
    assert fields["airframe_id"]["references"] == {"table": "airframes", "column": "id"}


def test_main_returns_2_on_connection_error(
    patch_connect: Callable[[Any], None],
    tmp_path: Path,
) -> None:
    """main returns 2 when the database connection fails.

    Args:
        patch_connect: Helper to patch mysql.connector.connect.
        tmp_path: Temporary directory root for the output directory.
    """
    patch_connect(mysql.connector.Error("access denied"))

    exit_code = export_events.main([*BASE_ARGS, "--output-dir", str(tmp_path / "out")])

    assert exit_code == 2


def test_main_returns_2_on_query_error(
    make_fake_connection: Callable[..., FakeConnection],
    patch_connect: Callable[[Any], None],
    tmp_path: Path,
) -> None:
    """main returns 2 and still closes the connection when a query fails.

    Args:
        make_fake_connection: Factory for fake connections.
        patch_connect: Helper to patch mysql.connector.connect.
        tmp_path: Temporary directory root for the output directory.
    """
    connection = make_fake_connection(error_on_query=True)
    patch_connect(connection)

    exit_code = export_events.main([*BASE_ARGS, "--output-dir", str(tmp_path / "out")])

    assert exit_code == 2
    assert connection.closed


def test_main_prompts_for_password_when_omitted(
    fake_connection: FakeConnection,
    patch_connect: Callable[[Any], None],
    monkeypatch: pytest.MonkeyPatch,
    tmp_path: Path,
) -> None:
    """main prompts via getpass when --password is not supplied.

    Args:
        fake_connection: A healthy fake database connection.
        patch_connect: Helper to patch mysql.connector.connect.
        monkeypatch: The pytest monkeypatch fixture.
        tmp_path: Temporary directory root for the output directory.
    """
    patch_connect(fake_connection)
    prompts: list[str] = []

    def fake_getpass(prompt: str = "") -> str:
        """Record the prompt and return a canned password."""
        prompts.append(prompt)
        return "from-prompt"

    monkeypatch.setattr(export_events.getpass, "getpass", fake_getpass)

    exit_code = export_events.main(
        [
            "--start-date",
            "2024-01-01",
            "--end-date",
            "2024-01-31",
            "--output-dir",
            str(tmp_path / "out"),
        ]
    )

    assert exit_code == 0
    assert len(prompts) == 1
