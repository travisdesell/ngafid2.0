# ngafid-pydata

Python data-analysis scripts for the NGAFID MySQL database.

The database is typically reached over an SSH tunnel to the database server.
See `database_tunnel.sh` (opens the tunnel) and `database_connect.sh` (a plain
`mysql` client connection through it).

## Setup

Requires Python 3.10 or newer. Create a virtual environment and install the
package (editable, with dev tooling) from this directory:

```bash
python3.14 -m venv .venv
source .venv/bin/activate
pip install -e ".[dev]"
```

(Use any Python >= 3.10; the macOS system `python3` is 3.9 and will not work.
`python3.14` is available via Homebrew.)

## Usage

With the SSH tunnel running, export events within an inclusive date range,
along with the related reference tables, into an output directory:

```bash
ngafid-export-events \
    --start-date 2024-01-01 \
    --end-date 2024-01-31 \
    --output-dir export/2024-01
```

The output directory is created recursively if needed, and the following CSV
files are written into it:

- `events.csv` — all events whose `start_time` is within the date range.
- `flights.csv` — `id`, `fleet_id`, `system_id`, `airframe_id`, `start_time`,
  `end_time` for the flights referenced by those events. The flights are
  selected with a single semi-join (`WHERE id IN (SELECT flight_id FROM events
WHERE ...)`) so the flight-id set stays in the database — this scales to
  hundreds of thousands of flights without shipping ids back to the client.
- `event_definitions.csv` — `id`, `fleet_id`, `airframe_id`,
  `airframe_type_id`, `name` from `event_definitions`.
- `airframes.csv` — `id`, `airframe`, `type_id` from `airframes`.
- `airframe_types.csv` — `id`, `name` from `airframe_types`.
- `tails.csv` — `system_id`, `fleet_id`, `tail`, `confirmed` from `tails`.

Alongside each `*.csv` file, a matching Avro schema (`*.avsc`) describing its
columns is written (e.g. `events.avsc`). Numeric MySQL columns map to the
corresponding Avro numeric types; all other columns map to `string`, and
nullable columns become an Avro `["null", <type>]` union.

Foreign-key columns (read from `information_schema` at export time) are
annotated in the schema with a custom `references` attribute naming the
referenced table and column, for example:

```json
{
  "name": "flight_id",
  "type": "int",
  "references": { "table": "flights", "column": "id" }
}
```

Each query streams its rows to disk while a progress bar (one per table,
labeled with the table name) reports rows written on standard error, so long
exports are visible as they run. The bars are shown only on an interactive
terminal and are suppressed automatically when output is piped or redirected.

Connection defaults (`127.0.0.1:3306`, user `ngafid_user`, database `ngafid`)
can each be overridden on the command line; run with `--help` for the full
list. If `--password` is omitted you are prompted interactively so the
password is kept out of your shell history.

## Testing

Unit tests live in `tests/` and use [pytest](https://docs.pytest.org/). They
run against an in-memory fake database connection (no live database or tunnel
needed) and validate the generated Avro schemas with the reference `avro`
parser:

```bash
pytest
```

## Linting

Code in this package is kept clean under [ruff](https://docs.astral.sh/ruff/)
(configured in `pyproject.toml`):

```bash
ruff check .
```

## Operational scripts

`scripts/raise/` holds standalone scripts for the RAISE rotorcraft
data-sharing workflow (`transfer_rotorcraft_data.py`,
`get_transfer_statistics.py`). They live here so they share this package's
linting (they are covered by `ruff check .`). They need extra dependencies
(`mysqlclient`, `pysftp`), installed via the `raise` extra:

```bash
pip install -e ".[raise]"
```
