# flake8: noqa: E501
import csv
import os
import re
import shutil
from pathlib import Path


def run_check(validator):
    category = "FS"
    upload_dir = validator._effective_property("ngafid.upload.dir")
    archive_dir = validator._effective_property("ngafid.archive.dir")
    terrain_dir = validator._effective_property("ngafid.terrain.dir")
    airports_file = validator._effective_property("ngafid.airports.file")
    runways_file = validator._effective_property("ngafid.runways.file")

    if upload_dir:
        if validator._check_dir_rw(category, "upload dir", upload_dir, check_write=True):
            _check_free_space(validator, category, Path(upload_dir), "upload dir")
    else:
        validator._fail(
            category,
            "upload dir",
            "could not resolve effective ngafid.upload.dir",
            "set upload dir properties",
        )

    if archive_dir:
        if validator._check_dir_rw(category, "archive dir", archive_dir, check_write=True):
            _check_free_space(validator, category, Path(archive_dir), "archive dir")
    else:
        validator._fail(
            category,
            "archive dir",
            "could not resolve effective ngafid.archive.dir",
            "set archive dir properties",
        )

    if terrain_dir:
        if validator._check_dir_rw(category, "terrain dir", terrain_dir, check_write=False):
            _check_terrain_tiles(validator, Path(terrain_dir))
    else:
        validator._fail(
            category,
            "terrain dir",
            "could not resolve effective ngafid.terrain.dir",
            "set terrain dir properties",
        )

    if airports_file:
        if validator._check_file_readable(
            category,
            "airports csv",
            airports_file,
            "fix ngafid.airports.file or mount source file",
        ):
            _check_csv_integrity(validator, category, Path(airports_file), "airports csv", min_rows=10)
    else:
        validator._fail(
            category,
            "airports csv",
            "could not resolve effective ngafid.airports.file",
            "set airports file properties",
        )

    if runways_file:
        if validator._check_file_readable(
            category,
            "runways csv",
            runways_file,
            "fix ngafid.runways.file or mount source file",
        ):
            _check_csv_integrity(validator, category, Path(runways_file), "runways csv", min_rows=10)
    else:
        validator._fail(
            category,
            "runways csv",
            "could not resolve effective ngafid.runways.file",
            "set runways file properties",
        )


def _check_terrain_tiles(validator, terrain_dir: Path):
    category = "FS"
    try:
        children = [p for p in terrain_dir.iterdir() if p.is_dir()]
    except OSError as exc:
        validator._fail(
            category,
            "terrain tiles",
            f"could not list terrain dir: {exc}",
            "fix mount or permissions",
        )
        return

    if not children:
        validator._fail(
            category,
            "terrain tiles",
            f"{terrain_dir} has no tile subdirectories",
            "populate terrain data in configured terrain path",
        )
        return

    validator._pass(
        category,
        "terrain tiles",
        f"{terrain_dir} has {len(children)} subdirectories",
    )

    pattern = re.compile(r"^[A-Z]\d{2}$")
    invalid_names = [p.name for p in children if not pattern.match(p.name)]
    if invalid_names:
        preview = ", ".join(invalid_names[:5])
        validator._fail(
            category,
            "terrain tile naming",
            f"unexpected terrain directory names found: {preview}",
            "ensure terrain tiles use expected naming like H11, I10, J17",
        )
    else:
        validator._pass(
            category,
            "terrain tile naming",
            "terrain tile directory names match expected pattern",
        )


def _check_free_space(validator, category, path: Path, label: str):
    min_free_bytes = int(os.environ.get("VALIDATION_MIN_FREE_BYTES", str(1 * 1024 * 1024 * 1024)))
    try:
        usage = shutil.disk_usage(path)
    except OSError as exc:
        validator._fail(
            category,
            f"{label} free space",
            f"could not read disk usage for {path}: {exc}",
            "verify mounted path and filesystem health",
        )
        return

    if usage.free < min_free_bytes:
        validator._fail(
            category,
            f"{label} free space",
            f"low disk space at {path}: {usage.free} bytes free",
            f"free up space or increase storage (minimum threshold: {min_free_bytes} bytes)",
        )
    else:
        validator._pass(
            category,
            f"{label} free space",
            f"{usage.free} bytes free",
        )


def _check_csv_integrity(validator, category, file_path: Path, label: str, min_rows: int):
    rows = []
    try:
        with file_path.open("r", encoding="utf-8", newline="") as csv_file:
            reader = csv.reader(csv_file)
            for idx, row in enumerate(reader):
                if idx >= 100:
                    break
                if row:
                    rows.append(row)
    except OSError as exc:
        validator._fail(
            category,
            f"{label} parse",
            f"unable to read {file_path}: {exc}",
            "verify file readability and encoding",
        )
        return

    if len(rows) < min_rows:
        validator._fail(
            category,
            f"{label} row count",
            f"{file_path} has too few parseable rows ({len(rows)})",
            f"verify {label} source data is complete",
        )
        return

    too_short = [row for row in rows if len(row) < 2]
    if too_short:
        validator._fail(
            category,
            f"{label} structure",
            "encountered CSV rows with fewer than 2 columns",
            f"verify {label} CSV delimiter and source format",
        )
        return

    validator._pass(
        category,
        f"{label} integrity",
        f"parsed {len(rows)} sample rows successfully",
    )
