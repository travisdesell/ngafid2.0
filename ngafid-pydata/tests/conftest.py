"""Shared pytest fixtures and database fakes for the foundry_export tests.

Provides an in-memory fake MySQL connection whose cursor returns canned column
metadata and rows for the ``events``, ``event_definitions``, ``airframes``, and
``airframe_types`` tables, so the export code can be exercised without a real
database.
"""

from __future__ import annotations

from collections.abc import Callable, Sequence
from typing import Any

import mysql.connector
import pytest
from mysql.connector import FieldType


def _classify(query: str) -> str:
    """Return the table key a query targets, based on its SQL text.

    Checks the outer ``FROM`` table first so the flights semi-join (whose
    subquery selects ``FROM events``) is classified as ``"flights"``.

    Args:
        query: The SQL query string being executed.

    Returns:
        One of ``"flights"``, ``"tails"``, ``"event_definitions"``,
        ``"airframe_types"``, ``"airframes"``, or ``"events"``; defaults to
        ``"events"``.
    """
    if "FROM flights" in query:
        return "flights"
    if "FROM tails" in query:
        return "tails"
    if "event_definitions" in query:
        return "event_definitions"
    if "airframe_types" in query:
        return "airframe_types"
    if "FROM airframes" in query:
        return "airframes"
    return "events"


def _column(name: str, type_code: int, nullable: bool = False) -> tuple[Any, ...]:
    """Build a DB-API ``cursor.description`` tuple for one column.

    Args:
        name: The column name.
        type_code: The MySQL field-type code (a
            :class:`mysql.connector.FieldType` constant).
        nullable: Whether the column permits ``NULL`` (the DB-API ``null_ok``
            flag).

    Returns:
        A 8-element description tuple matching the layout produced by
        mysql-connector-python.
    """
    return (name, type_code, None, None, None, None, 1 if nullable else 0, 0)


# Canned column metadata and rows keyed by table name.
TABLE_DATA: dict[str, dict[str, Any]] = {
    "events": {
        "description": [
            _column("id", FieldType.LONG),
            _column("flight_id", FieldType.LONG),
            _column("start_time", FieldType.DATETIME, nullable=True),
            _column("severity", FieldType.DOUBLE),
            _column("min_latitude", FieldType.DOUBLE, nullable=True),
        ],
        "rows": [
            (1, 7, "2024-01-05 10:00:00", 5.0, None),
            (2, 8, "2024-01-06 09:00:00", 9.5, 41.2),
        ],
    },
    "flights": {
        "description": [
            _column("id", FieldType.LONG),
            _column("fleet_id", FieldType.LONG),
            _column("system_id", FieldType.VAR_STRING),
            _column("airframe_id", FieldType.LONG),
            _column("start_time", FieldType.DATETIME, nullable=True),
            _column("end_time", FieldType.DATETIME, nullable=True),
        ],
        "rows": [
            (7, 1, "N123AB", 3, "2024-01-05 09:30:00", "2024-01-05 10:30:00"),
            (8, 1, "N456CD", 3, "2024-01-06 08:00:00", "2024-01-06 09:15:00"),
        ],
    },
    "event_definitions": {
        "description": [
            _column("id", FieldType.LONG),
            _column("fleet_id", FieldType.LONG),
            _column("airframe_id", FieldType.LONG),
            _column("airframe_type_id", FieldType.LONG, nullable=True),
            _column("name", FieldType.VAR_STRING),
        ],
        "rows": [(10, 1, 3, None, "low fuel")],
    },
    "airframes": {
        "description": [
            _column("id", FieldType.LONG),
            _column("airframe", FieldType.VAR_STRING),
            _column("type_id", FieldType.LONG),
        ],
        "rows": [(3, "PA-28", 1)],
    },
    "airframe_types": {
        "description": [
            _column("id", FieldType.LONG),
            _column("name", FieldType.VAR_STRING),
        ],
        "rows": [(1, "Fixed Wing Single Engine")],
    },
    "tails": {
        "description": [
            _column("system_id", FieldType.VAR_STRING),
            _column("fleet_id", FieldType.LONG),
            _column("tail", FieldType.VAR_STRING, nullable=True),
            _column("confirmed", FieldType.TINY),
        ],
        "rows": [
            ("N123AB", 1, "N123AB", 1),
            ("N456CD", 1, "N456CD", 0),
        ],
    },
}

# Canned foreign-key rows per table, matching the columns returned by the
# information_schema.KEY_COLUMN_USAGE lookup:
# (COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME).
FOREIGN_KEYS: dict[str, list[tuple[str, str, str]]] = {
    "events": [
        ("fleet_id", "fleet", "id"),
        ("flight_id", "flights", "id"),
        ("other_flight_id", "flights", "id"),
        ("event_definition_id", "event_definitions", "id"),
    ],
    "flights": [
        # fleet_id appears twice (single-column FK to fleet and as part of the
        # composite key to tails); the first should win deterministically.
        ("fleet_id", "fleet", "id"),
        ("fleet_id", "tails", "fleet_id"),
        ("system_id", "tails", "system_id"),
        ("airframe_id", "airframes", "id"),
        ("uploader_id", "user", "id"),
        ("upload_id", "uploads", "id"),
    ],
    "event_definitions": [
        ("airframe_type_id", "airframe_types", "id"),
    ],
    "airframes": [
        ("type_id", "airframe_types", "id"),
    ],
    "airframe_types": [],
    "tails": [
        ("fleet_id", "fleet", "id"),
    ],
}


class FakeCursor:
    """A minimal DB-API cursor served from :data:`TABLE_DATA`."""

    def __init__(self, connection: FakeConnection) -> None:
        """Initialize the cursor.

        Args:
            connection: The owning :class:`FakeConnection`.
        """
        self._connection = connection
        self._mode: str | None = None
        self._table: str | None = None
        self._fk_table: str | None = None
        self._rows: list[tuple[Any, ...]] = []
        self._position = 0

    def execute(self, query: str, params: Sequence[Any] = ()) -> None:
        """Record the query and select which canned result it targets.

        Foreign-key lookups (``information_schema.KEY_COLUMN_USAGE`` queries)
        are served from :data:`FOREIGN_KEYS`; all other queries are served from
        :data:`TABLE_DATA` based on the table named in the query. Resets the
        row cursor so ``fetchmany``/``fetchall`` iterate the new result set.

        Args:
            query: The SQL query string, used to pick the canned result set.
            params: The bound query parameters, recorded for later assertions.

        Raises:
            mysql.connector.Error: If the owning connection is configured to
                simulate a query failure.
        """
        if self._connection.error_on_query:
            raise mysql.connector.Error("simulated query failure")
        self._connection.executed.append((query, tuple(params)))
        if "KEY_COLUMN_USAGE" in query:
            self._mode = "foreign_keys"
            self._fk_table = params[0]
            self._rows = list(FOREIGN_KEYS.get(self._fk_table, []))
        else:
            self._mode = "table"
            self._table = _classify(query)
            self._rows = list(TABLE_DATA[self._table]["rows"])
        self._position = 0

    def fetchall(self) -> list[tuple[Any, ...]]:
        """Return all rows not yet consumed from the current result set.

        Returns:
            The remaining result rows for the most recently executed query.
        """
        remaining = self._rows[self._position :]
        self._position = len(self._rows)
        return remaining

    def fetchmany(self, size: int = 1) -> list[tuple[Any, ...]]:
        """Return up to ``size`` rows from the current result set.

        Args:
            size: The maximum number of rows to return.

        Returns:
            The next batch of result rows, or an empty list when exhausted.
        """
        batch = self._rows[self._position : self._position + size]
        self._position += len(batch)
        return batch

    @property
    def description(self) -> list[tuple[Any, ...]]:
        """Return the canned column description for the executed table query.

        Returns:
            The DB-API description tuples for the selected table.
        """
        assert self._table is not None
        return list(TABLE_DATA[self._table]["description"])

    def close(self) -> None:
        """Close the cursor (a no-op for the fake).

        Returns:
            None.
        """
        return None


class FakeConnection:
    """A minimal DB-API connection producing :class:`FakeCursor` objects."""

    def __init__(self, error_on_query: bool = False) -> None:
        """Initialize the fake connection.

        Args:
            error_on_query: When ``True``, every ``cursor.execute`` raises a
                :class:`mysql.connector.Error` to simulate a query failure.
        """
        self.error_on_query = error_on_query
        self.closed = False
        self.executed: list[tuple[str, tuple[Any, ...]]] = []

    def cursor(self) -> FakeCursor:
        """Return a new fake cursor bound to this connection.

        Returns:
            A fresh :class:`FakeCursor`.
        """
        return FakeCursor(self)

    def close(self) -> None:
        """Mark the connection closed.

        Returns:
            None. Sets :attr:`closed` to ``True``.
        """
        self.closed = True


@pytest.fixture
def make_fake_connection() -> Callable[..., FakeConnection]:
    """Provide a factory for :class:`FakeConnection` instances.

    Returns:
        A callable ``build(error_on_query=False)`` returning a new fake
        connection.
    """

    def _build(error_on_query: bool = False) -> FakeConnection:
        """Build a fake connection with the given failure behavior."""
        return FakeConnection(error_on_query=error_on_query)

    return _build


@pytest.fixture
def fake_connection(
    make_fake_connection: Callable[..., FakeConnection],
) -> FakeConnection:
    """Provide a healthy fake connection.

    Args:
        make_fake_connection: The connection factory fixture.

    Returns:
        A :class:`FakeConnection` that succeeds on all queries.
    """
    return make_fake_connection()


@pytest.fixture
def patch_connect(monkeypatch: pytest.MonkeyPatch) -> Callable[[Any], None]:
    """Provide a helper to patch :func:`mysql.connector.connect`.

    Args:
        monkeypatch: The pytest monkeypatch fixture.

    Returns:
        A function accepting either a :class:`FakeConnection` (returned from
        ``connect``) or an :class:`Exception` instance (raised from
        ``connect``).
    """

    def _patch(result: Any) -> None:
        """Install a fake ``connect`` returning or raising ``result``."""

        def _connect(**kwargs: Any) -> Any:
            """Stand-in for mysql.connector.connect."""
            if isinstance(result, Exception):
                raise result
            return result

        monkeypatch.setattr(mysql.connector, "connect", _connect)

    return _patch


@pytest.fixture
def make_column() -> Callable[..., tuple[Any, ...]]:
    """Provide a helper for building DB-API description tuples in tests.

    Returns:
        A callable ``column(name, type_code, nullable=False)`` returning a
        description tuple.
    """

    def _column_factory(name: str, type_code: int, nullable: bool = False) -> tuple[Any, ...]:
        """Build a single description tuple."""
        return _column(name, type_code, nullable=nullable)

    return _column_factory
