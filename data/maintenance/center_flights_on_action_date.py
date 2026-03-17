#!/usr/bin/env python3
"""
Create a 0-centered copy of extracted maintenance flights for public use.

Reads data/maintenance/manifest.json and data/maintenance/extracted_flights/.
For each work order, the record's actionDate is treated as time 0. All flight
CSV timestamps are converted to OffsetSeconds (signed seconds from action
date midnight; negative = before, positive = after). Output is written to data/maintenance/extracted_flights_0_centered/
with the same directory structure. Record JSONs are copied with real dates
removed (open_date, close_date, action_date omitted for anonymization).

Usage (from repo root or data/maintenance):
  python center_flights_on_action_date.py [--dry-run]
"""

from __future__ import annotations

import argparse
import csv
import json
import sys
from datetime import datetime
from pathlib import Path

# Default base dir: data/maintenance (script may live there or in repo root)
SCRIPT_DIR = Path(__file__).resolve().parent
MAINTENANCE_DIR = SCRIPT_DIR
EXTRACTED_DIR = MAINTENANCE_DIR / "extracted_flights"
OUTPUT_DIR = MAINTENANCE_DIR / "extracted_flights_0_centered"
MANIFEST_PATH = MAINTENANCE_DIR / "manifest.json"

# Action date is interpreted as midnight (start of day) for offset calculation


def load_manifest(path: Path) -> dict:
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def load_record(extracted_root: Path, record_path: str) -> dict:
    full = extracted_root / record_path
    with open(full, "r", encoding="utf-8") as f:
        return json.load(f)


def parse_action_date(record: dict) -> datetime:
    """Parse actionDate (yyyy-mm-dd) as midnight for offset reference."""
    raw = record.get("actionDate")
    if not raw:
        raise ValueError("record has no actionDate")
    return datetime.strptime(raw.strip(), "%Y-%m-%d")


def offset_seconds_from_action(dt: datetime, action_midnight: datetime) -> int:
    """Seconds from action midnight. Negative = before, positive = after."""
    delta = dt - action_midnight
    return int(round(delta.total_seconds()))


def process_raw_csv(
    in_path: Path,
    out_path: Path,
    action_midnight: datetime,
    dry_run: bool,
) -> None:
    """
    Process a raw flight CSV (Lcl Date, Lcl Time, UTCOfst,...).
    Replaces first three columns with OffsetSeconds (seconds from action date midnight).
    """
    with open(in_path, "r", encoding="utf-8", newline="") as f:
        lines = f.readlines()

    out_lines = []
    for i, line in enumerate(lines):
        if line.startswith("#"):
            # Fix units line: first 3 columns are now OffsetSeconds (seconds), not date/time/offset
            if "yyy-mm-dd" in line or "hh:mm:ss" in line:
                parts = list(csv.reader([line[1:].strip()]))[0]  # strip leading #
                if len(parts) >= 3:
                    new_units = ["s"] + parts[3:]  # s = seconds from action date midnight
                    line = "#" + ",".join(new_units) + "\n"
            out_lines.append(line)
            continue
        stripped = line.strip()
        if not stripped:
            out_lines.append(line)
            continue
        parts = list(csv.reader([stripped]))[0]
        if i == 2 and len(parts) >= 3 and parts[0] == "Lcl Date" and parts[1] == "Lcl Time":
            # Header: replace Lcl Date, Lcl Time, UTCOfst with OffsetSeconds
            new_header = ["OffsetSeconds"] + parts[3:]
            out_lines.append(",".join(new_header) + "\n")
            continue
        if len(parts) < 3:
            out_lines.append(line)
            continue
        date_s, time_s, utcofs = parts[0].strip(), parts[1].strip(), parts[2].strip()
        try:
            dt = datetime.strptime(date_s + " " + time_s, "%Y-%m-%d %H:%M:%S")
        except ValueError:
            out_lines.append(line)
            continue
        offset = offset_seconds_from_action(dt, action_midnight)
        new_row = [str(offset)] + parts[3:]
        out_lines.append(",".join(new_row) + "\n")

    if not dry_run:
        out_path.parent.mkdir(parents=True, exist_ok=True)
        with open(out_path, "w", encoding="utf-8", newline="") as f:
            f.writelines(out_lines)


def process_phases_csv(
    in_path: Path,
    out_path: Path,
    action_midnight: datetime,
    dry_run: bool,
) -> None:
    """
    Process a _phases.csv (tab-separated, Row_Index, Timestamp, ...).
    Replaces Timestamp with OffsetSeconds.
    """
    with open(in_path, "r", encoding="utf-8", newline="") as f:
        lines = f.readlines()

    out_lines = []
    for i, line in enumerate(lines):
        if line.startswith("#"):
            if "Timestamp" in line and "Row_Index" in line:
                line = line.replace("\tTimestamp\t", "\tOffsetSeconds\t").replace("Timestamp\t", "OffsetSeconds\t")
            out_lines.append(line)
            continue
        if not line.strip():
            out_lines.append(line)
            continue
        parts = line.strip().split("\t")
        if len(parts) < 2:
            out_lines.append(line)
            continue
        # Header row has Row_Index, Timestamp, ...
        if parts[1] == "Timestamp":
            new_parts = [parts[0], "OffsetSeconds"] + parts[2:]
            out_lines.append("\t".join(new_parts) + "\n")
            continue
        try:
            ts = parts[1].strip()
            # ISO format e.g. 2018-08-02T11:49:29-05:00; use first 19 chars for naive parse
            if "T" in ts:
                dt = datetime.strptime(ts[:19], "%Y-%m-%dT%H:%M:%S")
            else:
                dt = datetime.strptime(ts[:19], "%Y-%m-%d %H:%M:%S")
        except ValueError:
            out_lines.append(line)
            continue
        offset = offset_seconds_from_action(dt, action_midnight)
        new_parts = [parts[0], str(offset)] + parts[2:]
        out_lines.append("\t".join(new_parts) + "\n")

    if not dry_run:
        out_path.parent.mkdir(parents=True, exist_ok=True)
        with open(out_path, "w", encoding="utf-8", newline="") as f:
            f.writelines(out_lines)


def sanitize_record(record: dict) -> dict:
    """Return a copy with real dates removed for anonymization."""
    out = dict(record)
    for key in ("openDate", "closeDate", "actionDate", "open_date", "close_date", "action_date", "open_date_time", "close_date_time", "openDateTime", "closeDateTime"):
        out.pop(key, None)
    out["reference_date"] = "0"  # action date is time 0
    return out


def sanitize_workorder_for_manifest(wo: dict) -> dict:
    """Return a copy of workorder with real dates removed for 0-centered manifest."""
    out = dict(wo)
    for key in ("open_date", "close_date", "action_date", "open_date_time", "close_date_time"):
        out.pop(key, None)
    return out


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dry-run", action="store_true", help="Print actions without writing files")
    args = parser.parse_args()
    dry_run = args.dry_run

    if not MANIFEST_PATH.is_file():
        print(f"Manifest not found: {MANIFEST_PATH}", file=sys.stderr)
        return 1
    if not EXTRACTED_DIR.is_dir():
        print(f"Extracted flights dir not found: {EXTRACTED_DIR}", file=sys.stderr)
        return 1

    manifest = load_manifest(MANIFEST_PATH)
    workorders = manifest.get("workorders", [])
    if not workorders:
        print("No workorders in manifest.", file=sys.stderr)
        return 0

    if not dry_run:
        OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    new_workorders = []
    for wo in workorders:
        record_path = wo.get("record_json")
        if not record_path:
            print(f"Workorder {wo.get('workorder')} has no record_json, skipping.", file=sys.stderr)
            continue
        try:
            record = load_record(EXTRACTED_DIR, record_path)
        except Exception as e:
            print(f"Failed to load record {record_path}: {e}", file=sys.stderr)
            continue
        try:
            action_midnight = parse_action_date(record)
        except ValueError as e:
            print(f"Record {record_path}: {e}", file=sys.stderr)
            continue

        # Copy record JSON (sanitized) to output
        out_record_path = OUTPUT_DIR / record_path
        if not dry_run:
            out_record_path.parent.mkdir(parents=True, exist_ok=True)
            with open(out_record_path, "w", encoding="utf-8") as f:
                json.dump(sanitize_record(record), f, indent="\t")
        else:
            print(f"Would write record: {out_record_path}")

        flights = wo.get("flights", {})
        for phase in ("before", "during", "after"):
            for rel_path in flights.get(phase, []):
                in_path = EXTRACTED_DIR / rel_path
                out_path = OUTPUT_DIR / rel_path
                if not in_path.is_file():
                    print(f"Missing: {in_path}", file=sys.stderr)
                    continue
                if rel_path.endswith("_phases.csv"):
                    process_phases_csv(in_path, out_path, action_midnight, dry_run)
                else:
                    process_raw_csv(in_path, out_path, action_midnight, dry_run)
                if dry_run:
                    print(f"Would process: {rel_path} -> {out_path}")

        new_workorders.append(sanitize_workorder_for_manifest(wo))

    # Write new manifest (same structure; paths still relative under extracted_flights_0_centered)
    new_manifest = {
        "generated_at": datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z",
        "description": "0-centered on action date; real dates removed for anonymization.",
        "statistics": manifest.get("statistics", {}),
        "single_day_during_paths": manifest.get("single_day_during_paths", {}),
        "workorders": new_workorders,
    }
    out_manifest = OUTPUT_DIR / "manifest.json"
    if not dry_run:
        with open(out_manifest, "w", encoding="utf-8") as f:
            json.dump(new_manifest, f, indent=2)
    else:
        print(f"Would write manifest: {out_manifest}")

    print("Done." if not dry_run else "Dry run done.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
