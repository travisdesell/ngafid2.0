#!/usr/bin/env python3
# flake8: noqa: E501
"""NGAFID startup preflight validator.

Runs required checks for configuration, filesystem, DB, and Kafka.
Intended for both automatic docker-compose startup gating and manual invocation.
"""

from __future__ import annotations

import argparse
import importlib.util
import os
import re
import sys
from datetime import datetime
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Optional, Sequence, Tuple


REQUIRED_TOPICS = [
    "upload",
    "upload-retry",
    "upload-dlq",
    "email",
    "email-retry",
    "email-dlq",
    "event",
    "event-retry",
    "event-dlq",
]

REQUIRED_PROP_KEYS = [
    "ngafid.repo.path",
    "ngafid.data.folder",
    "ngafid.upload.dir",
    "ngafid.archive.dir",
    "ngafid.terrain.dir",
    "ngafid.db.info",
    "ngafid.docker.db.info",
    "ngafid.kafka.bootstrap.servers",
    "ngafid.docker.kafka.bootstrap.servers",
    "ngafid.port",
]

JAR_ARTIFACTS = [
    "ngafid-core/target/ngafid-core-1.0-SNAPSHOT-jar-with-dependencies.jar",
    "ngafid-www/target/ngafid-www-1.0-SNAPSHOT-jar-with-dependencies.jar",
    "ngafid-data-processor/target/ngafid-data-processor-1.0-SNAPSHOT-jar-with-dependencies.jar",
]

SCHEMA_TABLES = ["databasechangelog", "databasechangeloglock", "user", "fleet", "uploads"]

CRITICAL_DB_TABLES = [
    "user",
    "fleet",
    "uploads",
    "airframe_types",
    "airframes",
    "fleet_airframes",
    "flights",
    "flight_processed",
    "event_definitions",
    "events",
    "event_metadata_keys",
    "event_metadata",
    "visited_airports",
    "visited_runways",
    "databasechangelog",
    "databasechangeloglock",
]

MATERIALIZED_VIEW_TABLES = [
    "m_fleet_airframe_monthly_event_counts",
    "m_fleet_airframe_30_day_event_counts",
    "m_fleet_airframe_event_processed_flight_count",
    "m_fleet_monthly_flight_counts",
    "m_fleet_30_day_flight_counts",
    "m_fleet_monthly_upload_counts",
]

EXPECTED_KAFKA_PARTITIONS = 6
EXPECTED_KAFKA_REPLICATION_FACTOR = 1


@dataclass
class CheckResult:
    category: str
    name: str
    ok: bool
    detail: str
    action: Optional[str] = None


class Validator:
    def __init__(self, args: argparse.Namespace) -> None:
        self.args = args
        self.results: List[CheckResult] = []
        self.in_docker = Path("/.dockerenv").exists()
        self.properties: Dict[str, str] = {}
        self.required_topics = REQUIRED_TOPICS
        self.required_prop_keys = REQUIRED_PROP_KEYS
        self.jar_artifacts = JAR_ARTIFACTS
        self.schema_tables = SCHEMA_TABLES
        self.critical_db_tables = CRITICAL_DB_TABLES
        self.materialized_view_tables = MATERIALIZED_VIEW_TABLES
        self.expected_kafka_partitions = EXPECTED_KAFKA_PARTITIONS
        self.expected_kafka_replication_factor = EXPECTED_KAFKA_REPLICATION_FACTOR

    def run(self) -> int:
        self._run_discovered_checks()
        return self._print_summary()

    def _run_discovered_checks(self) -> None:
        scripts_dir = Path(__file__).parent / "validation-scripts"
        if not scripts_dir.exists() or not scripts_dir.is_dir():
            self._fail(
                "SYSTEM",
                "check script directory",
                f"{scripts_dir} missing",
                "create ngafid-validate/validation-scripts with check files",
            )
            return

        check_scripts = sorted(
            path for path in scripts_dir.iterdir() if path.suffix == ".py" and not path.name.startswith("_")
        )
        if not check_scripts:
            self._fail(
                "SYSTEM",
                "check script discovery",
                f"no check scripts found in {scripts_dir}",
                "add at least one check script exposing run_check(validator)",
            )
            return

        for script_path in check_scripts:
            module_name = f"ngafid_validate_check_{script_path.stem}"
            try:
                spec = importlib.util.spec_from_file_location(module_name, script_path)
                if spec is None or spec.loader is None:
                    raise ImportError(f"unable to load spec for {script_path.name}")
                module = importlib.util.module_from_spec(spec)
                spec.loader.exec_module(module)
            except Exception as exc:  # pylint: disable=broad-except
                self._fail(
                    "SYSTEM",
                    f"load {script_path.name}",
                    str(exc),
                    "fix syntax/import issues in the check script",
                )
                continue

            run_check = getattr(module, "run_check", None)
            if not callable(run_check):
                self._fail(
                    "SYSTEM",
                    f"run_check in {script_path.name}",
                    "missing callable run_check(validator)",
                    "define run_check(validator) in the check script",
                )
                continue

            try:
                run_check(self)
            except Exception as exc:  # pylint: disable=broad-except
                self._fail(
                    "SYSTEM",
                    f"execute {script_path.name}",
                    str(exc),
                    "fix runtime error in the check script",
                )

    def _record(
        self,
        category: str,
        name: str,
        ok: bool,
        detail: str,
        action: Optional[str] = None,
    ) -> None:
        self.results.append(CheckResult(category, name, ok, detail, action))

    def _pass(self, category: str, name: str, detail: str) -> None:
        self._record(category, name, True, detail)

    def _fail(self, category: str, name: str, detail: str, action: Optional[str] = None) -> None:
        self._record(category, name, False, detail, action)

    def _check_file_readable(self, category: str, name: str, path: str, action: str) -> bool:
        p = Path(path)
        if not p.exists():
            self._fail(category, name, f"{path} does not exist", action)
            return False
        if not os.access(path, os.R_OK):
            self._fail(category, name, f"{path} is not readable", action)
            return False
        self._pass(category, name, f"{path} exists and is readable")
        return True

    def _check_dir_rw(self, category: str, name: str, path: str, check_write: bool = True) -> bool:
        p = Path(path)
        if not p.exists() or not p.is_dir():
            self._fail(category, name, f"{path} missing or not a directory", "create directory and mount it correctly")
            return False
        if not os.access(path, os.R_OK):
            self._fail(category, name, f"{path} is not readable", "adjust permissions for validator/service user")
            return False
        if check_write and not os.access(path, os.W_OK):
            self._fail(category, name, f"{path} is not writable", "adjust permissions or mount as writable")
            return False
        perms = "read/write" if check_write else "read"
        self._pass(category, name, f"{path} is {perms} accessible")
        return True

    def _effective_property(self, key: str) -> Optional[str]:
        if self.in_docker:
            docker_key = f"ngafid.docker.{key.split('ngafid.', 1)[1]}" if key.startswith("ngafid.") else key
            if docker_key in self.properties and self.properties[docker_key].strip():
                return self._resolve_value(self.properties[docker_key])
        if key in self.properties and self.properties[key].strip():
            return self._resolve_value(self.properties[key])
        return None

    def _resolve_value(self, value: str, max_depth: int = 10) -> str:
        pattern = re.compile(r"\$\{([^}]+)\}")
        current = value
        for _ in range(max_depth):
            changed = False

            def _replace(match: re.Match[str]) -> str:
                nonlocal changed
                key = match.group(1)
                replacement = self.properties.get(key)
                if replacement is None:
                    return match.group(0)
                changed = True
                return replacement

            new_value = pattern.sub(_replace, current)
            current = new_value
            if not changed:
                break
        return current

    def _parse_jdbc_mysql(self, url: str) -> Optional[Tuple[str, int, str]]:
        match = re.match(r"^jdbc:mysql://([^/:?#]+)(?::(\d+))?/([^?]+)", url.strip())
        if not match:
            return None
        host = match.group(1)
        port = int(match.group(2) or "3306")
        db_name = match.group(3)
        return host, port, db_name

    def _is_email_enabled_from_file(self, properties_path: str) -> bool:
        props = self._parse_properties(Path(properties_path))
        value = props.get("ngafid.email.enabled", "false").strip().lower()
        docker_value = props.get("ngafid.docker.email.enabled", value).strip().lower()
        active = docker_value if self.in_docker else value
        return active in {"1", "true", "yes", "on"}

    def _is_email_enabled(self) -> bool:
        value = self._effective_property("ngafid.email.enabled")
        if value is None:
            raw = self.properties.get("ngafid.email.enabled", "false")
            return raw.strip().lower() in {"1", "true", "yes", "on"}
        return value.strip().lower() in {"1", "true", "yes", "on"}

    def _parse_properties(self, path: Path) -> Dict[str, str]:
        properties: Dict[str, str] = {}
        if not path.exists():
            return properties
        try:
            content = path.read_text(encoding="utf-8")
        except OSError:
            return properties

        for raw_line in content.splitlines():
            line = raw_line.strip()
            if not line or line.startswith("#"):
                continue
            if "=" not in line:
                continue
            key, value = line.split("=", 1)
            properties[key.strip()] = value.strip()
        return properties

    def _print_summary(self) -> int:
        log_lines: List[str] = []

        # Add human-readable date to the top of the log
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        log_lines.append(f"Validation run at: {timestamp}")

        for result in self.results:
            status = "PASS" if result.ok else "FAIL"
            line = f"[{status}] {result.category}: {result.name} - {result.detail}"
            print(line)
            log_lines.append(line)

            if not result.ok and result.action:
                action_line = f"Action: {result.action}"
                print(action_line)
                log_lines.append(action_line)

        failures = [r for r in self.results if not r.ok]
        status = "FAIL" if failures else "PASS"

        if failures:
            summary_line = f"Validation failed: {len(failures)} required checks failed."
            print(f"\n{summary_line}")
            log_lines.append("")
            log_lines.append(summary_line)
        else:
            summary_line = "Validation succeeded: all required checks passed."
            print(f"\n{summary_line}")
            log_lines.append("")
            log_lines.append(summary_line)

        log_path, log_error = self._write_results_log(log_lines, status)
        if log_error:
            print(f"[FAIL] SYSTEM: validation log write - {log_error}")
            print("Action: ensure results directory exists and is writable")
            return 1

        print(f"Validation log written: {log_path}")
        return 1 if failures else 0

    def _write_results_log(self, lines: List[str], status: str) -> Tuple[str, Optional[str]]:
        results_dir = Path(self.args.results_dir)
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"validationlog_{timestamp}_{status}.log"

        try:
            results_dir.mkdir(parents=True, exist_ok=True)
            output_path = results_dir / filename
            output_path.write_text("\n".join(lines) + "\n", encoding="utf-8")
            return str(output_path), None
        except OSError as exc:
            return str(results_dir / filename), str(exc)


def parse_args(argv: Optional[Sequence[str]] = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Validate NGAFID startup preconditions")
    parser.add_argument(
        "--skip-build-artifacts",
        action="store_true",
        help="Skip build artifact existence checks",
    )
    parser.add_argument(
        "--timeout",
        type=int,
        default=8,
        help="Connection timeout in seconds for DB/Kafka checks",
    )
    parser.add_argument(
        "--results-dir",
        default=os.environ.get("VALIDATION_RESULTS_DIR", "/validator/validation-results"),
        help="Directory where validation log files are written",
    )
    return parser.parse_args(argv)


def main(argv: Optional[Sequence[str]] = None) -> int:
    args = parse_args(argv)
    validator = Validator(args)
    return validator.run()


if __name__ == "__main__":
    sys.exit(main())
