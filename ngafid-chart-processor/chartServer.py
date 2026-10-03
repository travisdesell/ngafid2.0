"""Serve aviation chart tiles over HTTP and process scheduled chart updates.

When first started, this script checks whether today is a scheduled update date
and, if so, invokes ``chartProcessor.py`` to download and process new charts. If
the charts directory (or any of its ``sectional``, ``terminal-area``,
``ifr-enroute-low``, ``ifr-enroute-high`` subfolders) is missing, it downloads
charts from the closest release date before starting the server. To trigger a
fresh download, delete the charts folder and restart. The script also checks
nightly (at 00:00) whether an update is due.

For testing, run an update for a specific date without starting the web server::

    python3 chartServer.py --test-date 12-26-2024

@Author: Roman Kozulia
"""

import argparse
import json
import logging
import os
import platform
import signal
import subprocess
import sys
import threading
from datetime import datetime
from http.server import HTTPServer, SimpleHTTPRequestHandler
from logging.handlers import RotatingFileHandler
from socketserver import ThreadingMixIn
from types import FrameType

# Configure logging. Log files rotate once they reach 10 MB.
log_file = "./chart_server.log"
log_dir = os.path.dirname(log_file)

os.makedirs(log_dir, exist_ok=True)

if not os.path.isfile(log_file):
    with open(log_file, "w") as f:
        f.write("")

max_log_file_size = 10 * 1024 * 1024  # 10 MB
backup_count = 2  # Number of backup files to keep

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[
        RotatingFileHandler(log_file, maxBytes=max_log_file_size, backupCount=backup_count),
        logging.StreamHandler(),
    ],
)

stop_event = threading.Event()


def parse_arguments() -> argparse.Namespace:
    """Parse command-line arguments.

    Defines ``--test-date`` (run an update for a specific date without starting the
    server) and ``--config`` (path to the chart service JSON config).

    Returns:
        The parsed arguments namespace.
    """
    parser = argparse.ArgumentParser(description="Serve aviation chart tiles and check for updates.")
    parser.add_argument(
        "--test-date", type=str, help="Run the script in test mode for a specific date (format: YYYY-MM-DD)."
    )
    parser.add_argument(
        "--config",
        type=str,
        help="Config file path",
        default="ngafid-chart-processor/chart_service_config.default.json",
    )
    return parser.parse_args()


def handle_exit_signal(signum: int, frame: FrameType | None) -> None:
    """Handle SIGINT/SIGTERM by signalling the update checker to stop and exiting.

    Args:
        signum: The signal number delivered (e.g. ``signal.SIGINT``).
        frame: The interrupted stack frame, as passed by the signal machinery;
            unused but required by the handler signature.
    """
    logging.info("Received termination signal. Stopping update checker.")
    stop_event.set()
    sys.exit(0)


# Load configuration
def load_config(config_path):
    """Load configuration values from the JSON file."""
    if not os.path.exists(config_path):
        logging.error(f"Configuration file {config_path} not found.")
        raise FileNotFoundError(f"Configuration file {config_path} not found.")

    with open(config_path) as f:
        try:
            data = json.load(f)
            logging.info("Configuration file loaded successfully.")
            return data
        except json.JSONDecodeError as e:
            logging.error(f"Error parsing JSON in {config_path}: {e}")
            raise ValueError(f"Error parsing JSON in {config_path}: {e}") from e


CONFIG = load_config(parse_arguments().config)
PATHS = CONFIG.get("paths", {})

# Extract paths from config
try:
    CHARTS_DIR = os.path.abspath(PATHS["charts"])
except KeyError as e:
    logging.error(f"Missing required path in configuration: {e}")
    raise ValueError(f"Missing required path in configuration: {e}") from e


def free_port(port: int) -> None:
    """Free up the given TCP port by killing any process currently bound to it.

    Uses ``lsof`` to find processes holding the port and ``kill -9`` to terminate
    them. No-op on Windows (where ``lsof`` is unavailable) and when the port is
    already free.

    Args:
        port: The TCP port number to free.
    """
    if platform.system() == "Windows":
        logging.info("Port freeing not implemented on Windows.")
        return
    try:
        # Find the PID using the port
        result = subprocess.run(["lsof", "-i", f":{port}"], capture_output=True, text=True, check=True)
        lines = result.stdout.splitlines()
        if len(lines) > 1:
            # Skip the header and extract PIDs
            for line in lines[1:]:
                pid = int(line.split()[1])
                # Kill the process
                logging.info(f"Killing process {pid} using port {port}")
                subprocess.run(["kill", "-9", str(pid)], check=True)
    except subprocess.CalledProcessError:
        logging.info(f"Port {port} is already free.")


def load_schedule() -> list[str]:
    """Load and sort the chart update schedule from the configuration.

    Flattens the per-year ``update_schedule`` entries in ``CONFIG`` into a single
    list of date strings and sorts it ascending. Dates are in ``%m-%d-%Y`` format
    (e.g. ``12-26-2024``). Returns an empty list if the schedule cannot be read.

    Returns:
        The update dates as ``MM-DD-YYYY`` strings, sorted ascending.
    """
    try:
        # Extract and flatten the update_schedule
        schedule = []
        for year_entry in CONFIG.get("update_schedule", []):
            schedule.extend(year_entry["dates"])
        sorted_schedule = sorted(schedule, key=lambda date: datetime.strptime(date, "%m-%d-%Y"))
        logging.info("Update schedule loaded and sorted successfully.")
        return sorted_schedule

    except Exception as e:
        logging.error(f"Error loading schedule: {e}")
        return []


def is_update_due(schedule, today):
    """Check if the current date matches an update date."""
    return today in schedule


def run_chart_processor(date: str) -> None:
    """Run ``chartProcessor.py`` as a subprocess for the given release date.

    Invokes the chart processor with ``--chart_date=<date>`` and logs its output.
    All failures (non-zero exit, missing script, or any unexpected error) are
    caught and logged rather than propagated.

    Args:
        date: The chart release date to process, in ``MM-DD-YYYY`` format.
    """
    try:
        logging.info(f"Running chartProcessor.py with --chart_date={date}")
        result = subprocess.run(
            ["python3", "ngafid-chart-processor/chartProcessor.py", "--chart_date", date], text=True, check=True
        )
        logging.info(f"ChartProcessor output:\n{result.stdout}")
    except subprocess.CalledProcessError as e:
        logging.error(f"ChartProcessor failed with exit code {e.returncode}.")
        logging.error(f"Error output: {e.stderr}")
    except FileNotFoundError as e:
        logging.error(f"ChartProcessor script not found: {e}")
    except Exception as e:
        logging.error(f"Unexpected error running chartProcessor: {e}")


def get_next_update_date(schedule: list[str], today_date: str | datetime) -> str | None:
    """Find the first scheduled update date strictly after today.

    Scans ``schedule`` in order and returns the first date later than
    ``today_date``. A string ``today_date`` is parsed with the ``%m-%d-%Y``
    format first.

    Args:
        schedule: Update dates in ``MM-DD-YYYY`` format (assumed ascending).
        today_date: Today's date, either a ``datetime`` or an ``MM-DD-YYYY``
            string.

    Returns:
        The next update date as an ``MM-DD-YYYY`` string, or ``None`` if there is
        no future date in the schedule.
    """
    # Convert today_date to datetime if it's a string
    if isinstance(today_date, str):
        today_date = datetime.strptime(today_date, "%m-%d-%Y")

    for date_str in schedule:
        update_date = datetime.strptime(date_str, "%m-%d-%Y")
        if update_date > today_date:
            return date_str

    return None  # No future update dates found


def start_update_checker() -> None:
    """Start a daemon thread that periodically runs the chart update check.

    Launches the nested ``checker`` loop on a daemon thread so update checking
    runs in the background while the HTTP server serves tiles. The thread exits
    with the process (daemon) or when ``stop_event`` is set.
    """

    def checker() -> None:
        """Run the update-check loop until ``stop_event`` is set.

        Checks immediately on first run and thereafter only at midnight: when an
        update is due for today it runs the chart processor, otherwise it logs the
        next scheduled date. Waits one hour between iterations and logs, without
        propagating, any error raised during a cycle.
        """
        schedule = load_schedule()
        isFirstUpdate = True

        while not stop_event.is_set():
            try:
                now = datetime.now()
                today = now.strftime("%m-%d-%Y")
                logging.info(f"Checking update schedule at: {now.strftime('%Y-%m-%d %H:%M:%S')}")

                # Perform update only at midnight
                if isFirstUpdate or now.hour == 0:
                    if isFirstUpdate:
                        logging.info("Performing initial update update check.")
                        isFirstUpdate = False
                    else:
                        logging.info("Performing scheduled update check.")

                    if is_update_due(schedule, today):
                        logging.info(f"Update due for today: {today}. Running chart processor.")
                        run_chart_processor(today)
                    else:
                        logging.info(f"No update due today: {today}.")
                        nextUpdate = get_next_update_date(schedule, today)
                        logging.info(f"Next update is due: {nextUpdate}")

                # Sleep for 1 hour until the next check
                stop_event.wait(timeout=3600)

            except Exception as e:
                logging.error(f"Unexpected error in update checker loop: {e}", exc_info=True)

    thread = threading.Thread(target=checker, daemon=True)
    thread.start()


class TileRequestHandler(SimpleHTTPRequestHandler):
    """Serve chart tile files from the charts directory (``BASE_DIR``)."""

    BASE_DIR = os.path.abspath(CHARTS_DIR)  # Static base directory

    def translate_path(self, path):
        """Translate the path to serve files from the BASE_DIR."""
        # Strip the leading slash and join the path with the BASE_DIR
        relative_path = path.lstrip("/")
        full_path = os.path.join(self.BASE_DIR, relative_path)
        logging.info(f"Serving file: {full_path}")
        return full_path


class ThreadingHTTPServer(ThreadingMixIn, HTTPServer):
    """Handle requests in a separate thread for concurrency."""

    pass


def run_server():
    """Run the HTTP server to serve tiles."""
    # Define host and port
    server_config = CONFIG.get("server_config", {})
    if not server_config:
        logging.error("'server_config' is missing in the configuration file.")
        raise ValueError("Error: 'server_config' is missing in the configuration file.")

    host = server_config.get("host")
    port = server_config.get("port")
    if not host:
        logging.error("'host' is not defined in 'server_config'.")
        raise ValueError("Error: 'host' is not defined in 'server_config'. Please specify a valid host.")
    if not port:
        logging.error("'port' is not defined in 'server_config'.")
        raise ValueError("Error: 'port' is not defined in 'server_config'. Please specify a valid port.")

    free_port(port)

    # Create and start the server
    server_address = (host, port)
    http = ThreadingHTTPServer(server_address, TileRequestHandler)

    # Print the served addresses
    base_url = f"http://{host}:{port}"
    logging.info("\nServing the following tile directories:")
    logging.info(f"  - Sectional Charts: {base_url}/sectional/{{z}}/{{x}}/{{-y}}.png")
    logging.info(f"  - Terminal Area Charts: {base_url}/terminal-area/{{z}}/{{x}}/{{-y}}.png")
    logging.info(f"  - IFR Enroute Low Charts: {base_url}/ifr-enroute-low/{{z}}/{{x}}/{{-y}}.png")
    logging.info(f"  - IFR Enroute High Charts: {base_url}/ifr-enroute-high/{{z}}/{{x}}/{{-y}}.png")
    logging.info(f"  - Helicopter Charts: {base_url}/helicopter/{{z}}/{{x}}/{{-y}}.png")
    logging.info(f"\nTiles are being served on: {base_url}\n")

    try:
        http.serve_forever()
    except KeyboardInterrupt:
        logging.info("Server stopped.")


def initial_download() -> None:
    """Download charts on first run when the charts directory is incomplete.

    Determines which required subdirectories (``sectional``, ``terminal-area``,
    ``ifr-enroute-low``, ``ifr-enroute-high``, ``helicopter``) are missing and, if
    any are, picks the most recent scheduled date on or before today and runs the
    chart processor for it. No-op when all subdirectories are already present.
    """
    charts_dir = CHARTS_DIR
    required_subdirs = ["sectional", "terminal-area", "ifr-enroute-low", "ifr-enroute-high", "helicopter"]

    # Check if the charts directory exists
    if not os.path.exists(charts_dir):
        logging.info("Charts directory does not exist. Proceeding with initial download.")
        os.makedirs(charts_dir, exist_ok=True)
        missing_subdirs = required_subdirs  # All subdirectories are missing if the main directory doesn't exist
    else:
        # Check for the presence of required subdirectories
        existing_subdirs = [
            subdir for subdir in os.listdir(charts_dir) if os.path.isdir(os.path.join(charts_dir, subdir))
        ]
        missing_subdirs = [subdir for subdir in required_subdirs if subdir not in existing_subdirs]

    if not missing_subdirs:
        logging.info("All required chart subdirectories are present. Skipping initial download.")
        return

    logging.info(f"Missing subdirectories: {missing_subdirs}. Proceeding with initial download.")

    # Get today's date in MM-DD-YYYY format
    today_date = datetime.now().strftime("%m-%d-%Y")
    logging.info(f"Today's date: {today_date}")

    all_dates = load_schedule()
    # Find the closest date
    target_date = None
    today_datetime = datetime.strptime(today_date, "%m-%d-%Y")
    for schedule_date in all_dates:
        schedule_datetime = datetime.strptime(schedule_date, "%m-%d-%Y")
        if schedule_datetime <= today_datetime:
            target_date = schedule_date  # Keep updating until we pass today
        else:
            break

    if not target_date:
        logging.error("No valid update date found in the schedule.")
        return

    logging.info(f"Determined date for initial download: {target_date}")
    run_chart_processor(target_date)


if __name__ == "__main__":
    args = parse_arguments()
    test_date = args.test_date

    signal.signal(signal.SIGINT, handle_exit_signal)
    signal.signal(signal.SIGTERM, handle_exit_signal)

    try:
        if test_date:
            # If a test date is provided, run in test mode
            logging.info(f"Running in test mode for date: {test_date}")
            run_chart_processor(test_date)
        else:
            # Perform initial download if chart folders are missing.
            logging.info("Checking for initial download...")
            initial_download()

            # Start the update checker and HTTP server
            logging.info("Starting the update checker thread.")
            start_update_checker()

            logging.info("Starting the HTTP chart server.")
            run_server()
    finally:
        logging.info("Shutting down the update checker thread.")
        stop_event.set()
