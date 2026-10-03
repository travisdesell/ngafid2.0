"""Download FAA aviation chart TIFs and process them into web map tiles.

For a given update date, downloads chart TIF files from the FAA website, then
crops, color-converts, reprojects (to EPSG:3857), and mosaics them before
generating zoomable map tiles for each chart type (sectional, terminal-area, IFR
enroute low/high, helicopter).

Intended to be invoked from ``chartServer.py``, but can be run directly for
testing::

    python chartProcessor.py --chart_date 12-26-2024

The chart dates, download URLs, and per-chart areas are read from the JSON config
(``chart_service_config.default.json``). Reference pages on the FAA site:
    https://www.faa.gov/air_traffic/flight_info/aeronav/productcatalog/doles/media/Product_Schedule.pdf
    https://www.faa.gov/air_traffic/flight_info/aeronav/digital_products/vfr/

@Author: Roman Kozulia
"""

import argparse
import datetime
import json
import logging
import os
import shutil
import subprocess
import sys
import tempfile
import zipfile
from enum import Enum
from logging.handlers import RotatingFileHandler
from typing import Any

import requests


def check_dependencies() -> None:
    """Verify the required GDAL command-line tools are installed, exiting if not.

    Checks that ``gdalwarp``, ``gdal2tiles.py``, and ``gdal_translate`` are on the
    PATH (they must be installed manually). Logs an error and calls
    ``sys.exit(1)`` on the first one that is missing.
    """
    commands = ["gdalwarp", "gdal2tiles.py", "gdal_translate"]
    for command in commands:
        if not shutil.which(command):
            logging.error(f"Dependency not found: {command}. Please install it before running the script.")
            sys.exit(1)


# Configure logging. Log files rotate once they reach 10 MB.
log_file = "ngafid-chart-processor/log"
log_dir = os.path.dirname(log_file)

os.makedirs(log_dir, exist_ok=True)

if not os.path.isfile(log_file):
    with open(log_file, "w") as f:
        f.write("")  # Create an empty log file

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

# Global GDAL Configuration
os.environ["GTIFF_SRS_SOURCE"] = "EPSG"


class ChartType(Enum):
    """Types of charts available for download."""

    SECTIONAL = "sectional"
    TERMINAL_AREA = "terminal_area"
    IFR_ENROUTE_LOW = "ifr_enroute_low"
    IFR_ENROUTE_HIGH = "ifr_enroute_high"
    HELICOPTER = "helicopter"


configuration_file = "ngafid-chart-processor/chart_service_config.default.json"


def validate_date(date_str: str) -> str:
    """Validate that a date string is in ``MM-DD-YYYY`` format (argparse type).

    Args:
        date_str: The date string to validate.

    Returns:
        The same string, unchanged, when it parses as ``MM-DD-YYYY``.

    Raises:
        argparse.ArgumentTypeError: If the string is not a valid ``MM-DD-YYYY``
            date.
    """
    try:
        datetime.datetime.strptime(date_str, "%m-%d-%Y")
    except ValueError as exc:
        raise argparse.ArgumentTypeError(f"Invalid date format: {date_str}. Expected MM-DD-YYYY.") from exc
    return date_str


def parse_arguments() -> argparse.Namespace:
    """Parse command-line arguments for the chart processor.

    Defines ``--chart_date`` (an ``MM-DD-YYYY`` date validated by
    :func:`validate_date`) and ``--config`` (path to the JSON config, defaulting
    to ``chart_service_config.default.json``), and logs the raw and parsed args.

    Returns:
        The parsed arguments namespace.
    """
    parser = argparse.ArgumentParser(description="Process aviation charts.")
    parser.add_argument(
        "--chart_date", type=validate_date, help="The date for which to process charts (format: MM-DD-YYYY)."
    )
    parser.add_argument(
        "--config",
        type=str,
        help="Config file path",
        default="ngafid-chart-processor/chart_service_config.default.json",
    )
    logging.info(f"{sys.argv}")
    parsed = parser.parse_args()
    logging.info(f"Parsed args: {parsed}")
    return parsed


# Load configuration
def load_config(config_path: str) -> dict[str, Any]:
    """Load and parse the JSON configuration file.

    Args:
        config_path: Path to the JSON configuration file.

    Returns:
        The parsed configuration as a dictionary.

    Raises:
        FileNotFoundError: If the configuration file does not exist.
        ValueError: If the file exists but does not contain valid JSON.
    """
    if not os.path.exists(config_path):
        logging.error(f"Configuration file {config_path} not found.")
        raise FileNotFoundError(f"Configuration file {config_path} not found.")

    with open(config_path) as f:
        try:
            data = json.load(f)
            return data  # Return the entire configuration
        except json.JSONDecodeError as e:
            logging.error(f"Error parsing JSON in {config_path}: {e}")
            raise ValueError(f"Error parsing JSON in {config_path}: {e}") from e


# Load the configuration file
CONFIG = load_config(parse_arguments().config)
# Extract required paths from config (raise error if missing)
try:
    PATHS = CONFIG["paths"]
    TIFS_ORIGINAL_DIR = os.path.abspath(PATHS["tifs_original"])
    TEMP_FILES_DIR = os.path.abspath(PATHS["temp_files"])
    CHARTS_DIR = os.path.abspath(PATHS["charts"])
    logging.info(f"paths = {PATHS}")
except KeyError as e:
    logging.error(f"Missing required path in configuration: {e}")
    raise ValueError(f"Missing required path in configuration: {e}") from e


def download_and_extract_tifs(tifs_path: str, date: str, chart_type: ChartType) -> None:
    """Download and extract the TIF files for a given date and chart type.

    Looks up the chart's configured base URL and areas, downloads each area's ZIP
    into a temporary directory, extracts the ``.tif`` files, and moves them into
    ``tifs_path`` (lowercasing names for IFR enroute charts). Logs and skips areas
    whose download or extraction fails; a no-op when the chart type has no config.

    Args:
        tifs_path: Directory to store the extracted TIF files.
        date: The date for which to download the TIF files (``MM-DD-YYYY``).
        chart_type: The type of chart to download and extract.
    """
    chart_key = chart_type.name
    if chart_key not in CONFIG["chart_files"]:
        logging.warning(f"No configuration found for chart type {chart_key}")
        return

    chart_config = CONFIG["chart_files"][chart_key]
    base_url = chart_config["base_url"].format(date=date)
    areas = chart_config["areas"]

    # Directory to store extracted TIF files
    os.makedirs(tifs_path, exist_ok=True)

    for area in areas:
        zip_url = f"{base_url}{area}.zip"
        try:
            os.makedirs(TEMP_FILES_DIR, exist_ok=True)
            # Create a temporary directory for extraction
            with tempfile.TemporaryDirectory(dir=TEMP_FILES_DIR) as temp_dir:
                zip_path = os.path.join(temp_dir, f"{area}.zip")

                # Download the ZIP file
                logging.info(f"Downloading {zip_url}")
                response = requests.get(zip_url)
                response.raise_for_status()

                with open(zip_path, "wb") as f:
                    f.write(response.content)

                logging.info(f"Extracting {zip_path}")
                with zipfile.ZipFile(zip_path, "r") as zip_ref:
                    zip_ref.extractall(temp_dir)

                logging.info(f"Downloaded and extracted files for {area}")

                # Move TIF files to the target directory
                for file_name in os.listdir(temp_dir):
                    if file_name.endswith(".tif"):
                        input_tif = os.path.join(temp_dir, file_name)
                        if chart_type in [ChartType.IFR_ENROUTE_LOW, ChartType.IFR_ENROUTE_HIGH]:
                            # For IFR_ENROUTE_LOW or IFR_ENROUTE_HIGH, convert to lowercase
                            base_name, ext = os.path.splitext(file_name)
                            file_name = f"{base_name.lower()}{ext}"  # Lowercase name

                        output_tif = os.path.join(tifs_path, file_name)

                        os.rename(input_tif, output_tif)
                        logging.info(f"Moved {input_tif} to {output_tif}")

        except requests.RequestException as e:
            logging.info(f"Failed to download {zip_url}: {e}")
        except zipfile.BadZipFile as e:
            logging.info(f"Failed to extract {zip_path}: {e}")


def download_terminal_area_set(base_url: str, date: str, save_path: str) -> None:
    """Download terminal-area charts and keep only the ``TAC`` (non-VFR) TIFs.

    Downloads the terminal-area ZIP for the given date into a temporary directory,
    extracts it, and moves any ``.tif`` whose name contains ``TAC`` but not
    ``VFR`` into ``save_path``. Download or extraction failures are logged and
    swallowed.

    Args:
        base_url: Base URL template for the terminal-area ZIP (``{date}`` is
            substituted).
        date: The date for which to download the charts (``MM-DD-YYYY``).
        save_path: Directory to save the filtered TIF files.
    """
    os.makedirs(save_path, exist_ok=True)

    # Format the URL with the provided date
    terminal_zip_url = base_url.format(date=date)

    try:
        # Create a temporary directory for extraction
        with tempfile.TemporaryDirectory(dir=TEMP_FILES_DIR) as temp_dir:
            zip_path = os.path.join(temp_dir, "Terminal.zip")

            # Download the ZIP file
            logging.info(f"Downloading {terminal_zip_url}")
            response = requests.get(terminal_zip_url)
            response.raise_for_status()

            with open(zip_path, "wb") as f:
                f.write(response.content)

            logging.info(f"Extracting {zip_path}")
            with zipfile.ZipFile(zip_path, "r") as zip_ref:
                zip_ref.extractall(temp_dir)

            for file_name in os.listdir(temp_dir):
                if file_name.endswith(".tif") and "TAC" in file_name and "VFR" not in file_name:
                    source_file = os.path.join(temp_dir, file_name)
                    destination_file = os.path.join(save_path, file_name)
                    os.rename(source_file, destination_file)
                    logging.info(f"Moved {source_file} to {destination_file}")

            logging.info(f"All 'TAC' files successfully downloaded and moved to {save_path}")

    except requests.RequestException as e:
        logging.info(f"Failed to download {terminal_zip_url}: {e}")
    except zipfile.BadZipFile as e:
        logging.info(f"Failed to extract {zip_path}: {e}")


def crop_tifs(shape_file_paths: str, tifs_path: str, cropped_tifs_path: str) -> None:
    """Crop each TIF to its matching shapefile outline using ``gdalwarp``.

    For every subfolder of ``shape_file_paths`` containing a ``.shp`` file, crops
    the like-named TIF in ``tifs_path`` to that cutline and writes the result into
    ``cropped_tifs_path``. Folders without a shapefile, or TIFs that are missing or
    fail to crop, are logged and skipped.

    Args:
        shape_file_paths: Directory containing per-chart shapefile folders.
        tifs_path: Directory containing the input TIF files.
        cropped_tifs_path: Directory to store the cropped TIF files.
    """
    os.makedirs(cropped_tifs_path, exist_ok=True)  # Ensure the output directory exists

    for folder_name in os.listdir(shape_file_paths):
        folder_path = os.path.join(shape_file_paths, folder_name)

        if not os.path.isdir(folder_path):
            continue

        # Look for the .shp file in the folder
        shp_file_path = None
        for file_name in os.listdir(folder_path):
            if file_name.endswith(".shp"):
                shp_file_path = os.path.join(folder_path, file_name)
                break

        if not shp_file_path:
            logging.info(f"No .shp file found in folder: {folder_name}")
            continue

        # Construct the paths for the TIF file and cropped output
        tif_file_path = os.path.join(tifs_path, f"{folder_name}.tif")
        cropped_tif_path = os.path.join(cropped_tifs_path, f"{folder_name}.tif")

        if not os.path.exists(tif_file_path):
            logging.info(f"Current working directory: {os.getcwd()}")
            logging.info(f"TIF file not found for folder: {folder_name} and tif folder path: {tif_file_path}")
            continue

        logging.info(f"Processing TIF: {tif_file_path} with Shape: {shp_file_path}")

        command = [
            "gdalwarp",
            "-cutline",
            shp_file_path,
            "-crop_to_cutline",
            "-dstalpha",
            #     "-dstnodata", "0",
            tif_file_path,
            cropped_tif_path,
        ]

        try:
            subprocess.run(command, check=True)
            logging.info(f"Successfully cropped {tif_file_path} to {cropped_tif_path}")
        except subprocess.CalledProcessError as e:
            logging.info(f"Failed to crop {tif_file_path}: {e}")


def convert_to_rgba(cropped_tifs_path: str, output_tifs_path: str) -> None:
    """Expand palette-based TIFs to RGBA with ``gdal_translate`` for color accuracy.

    Converts every ``.tif`` in ``cropped_tifs_path`` to a 4-band RGBA GeoTIFF
    (LZW-compressed, tiled, white as NoData) in ``output_tifs_path``. Applied only
    to sectional and terminal-area charts. Per-file failures are logged and
    skipped.

    Args:
        cropped_tifs_path: Directory holding the cropped input TIF files.
        output_tifs_path: Directory to store the converted RGBA TIF files.
    """
    os.makedirs(output_tifs_path, exist_ok=True)

    for file_name in os.listdir(cropped_tifs_path):
        if file_name.endswith(".tif"):
            input_tif = os.path.join(cropped_tifs_path, file_name)
            output_tif = os.path.join(output_tifs_path, file_name)

            # Use gdal_translate with -expand rgba to convert palette to RGBA
            command = [
                "gdal_translate",
                "-of",
                "GTiff",  # Ensure output is in GeoTIFF format
                "-expand",
                "rgba",  # Convert to RGBA
                "-a_nodata",
                "255",  # Explicitly set NoData to white
                "-co",
                "COMPRESS=LZW",  # Lossless compression
                "-co",
                "TILED=YES",  # Enable tiling
                input_tif,
                output_tif,
            ]

            try:
                subprocess.run(command, check=True)
                logging.info(f"Successfully converted {input_tif} to {output_tif} (RGBA)")
            except subprocess.CalledProcessError as e:
                logging.info(f"Failed to convert {input_tif} to RGBA: {e}")


def convert_to_rgb(cropped_tifs_path: str, output_tifs_path: str) -> None:
    """Expand palette-based TIFs to 3-band RGB (no transparency) via ``gdal_translate``.

    Converts every ``.tif`` in ``cropped_tifs_path`` to an LZW-compressed, tiled
    RGB GeoTIFF in ``output_tifs_path``, stripping any alpha carried in the color
    table. Used for helicopter charts. Per-file failures are logged and skipped.

    Args:
        cropped_tifs_path: Directory holding the cropped input TIF files.
        output_tifs_path: Directory to store the converted RGB TIF files.
    """
    os.makedirs(output_tifs_path, exist_ok=True)

    for file_name in os.listdir(cropped_tifs_path):
        if file_name.endswith(".tif"):
            input_tif = os.path.join(cropped_tifs_path, file_name)
            output_tif = os.path.join(output_tifs_path, file_name)

            command = [
                "gdal_translate",
                "-of",
                "GTiff",
                "-expand",
                "rgb",
                "-co",
                "COMPRESS=LZW",
                "-co",
                "TILED=YES",
                input_tif,
                output_tif,
            ]

            try:
                subprocess.run(command, check=True)
                logging.info(f"Converted {input_tif} to RGB (no transparency)")
            except subprocess.CalledProcessError as e:
                logging.error(f"Failed to convert {file_name} to RGB: {e}")


def reproject_tifs(input_tifs_path: str, reprojected_tifs_path: str) -> None:
    """Reproject TIF files to the EPSG:3857 (web Mercator) CRS with ``gdalwarp``.

    Reprojects every ``.tif`` in ``input_tifs_path`` to EPSG:3857 (adding an alpha
    band, LZW-compressed and tiled) into ``reprojected_tifs_path``, preserving file
    names. Per-file failures are logged and skipped.

    Args:
        input_tifs_path: Directory containing the input TIF files.
        reprojected_tifs_path: Directory to store the reprojected TIF files.
    """
    os.makedirs(reprojected_tifs_path, exist_ok=True)

    # Loop through all TIF files in the input_tifs_path
    for file_name in os.listdir(input_tifs_path):
        if file_name.endswith(".tif"):
            input_tif = os.path.join(input_tifs_path, file_name)
            output_tif = os.path.join(reprojected_tifs_path, file_name)  # Keep the original file name

            command = [
                "gdalwarp",
                "-t_srs",
                "EPSG:3857",
                "-dstalpha",
                "-co",
                "TILED=YES",
                "-co",
                "COMPRESS=LZW",
                input_tif,
                output_tif,
            ]

            try:
                subprocess.run(command, check=True)
                logging.info(f"Successfully reprojected {input_tif} to {output_tif}")
            except subprocess.CalledProcessError as e:
                logging.info(f"Failed to reproject {input_tif}: {e}")


def create_virtual_raster(reprojected_tifs_path: str, virtual_raster_path: str) -> None:
    """Mosaic the reprojected TIFs into a single virtual raster (VRT).

    Runs ``gdalbuildvrt`` over every ``.tif`` in ``reprojected_tifs_path`` to build
    one combined ``.vrt`` at ``virtual_raster_path`` (creating its parent
    directory). Failures are logged and swallowed.

    Args:
        reprojected_tifs_path: Directory of reprojected TIFs to combine.
        virtual_raster_path: Output path for the combined ``.vrt`` file.
    """
    os.makedirs(os.path.dirname(virtual_raster_path), exist_ok=True)

    # Find all reprojected TIF files
    input_files = [
        os.path.join(reprojected_tifs_path, file_name)
        for file_name in os.listdir(reprojected_tifs_path)
        if file_name.endswith(".tif")
    ]

    command = ["gdalbuildvrt", virtual_raster_path] + input_files

    try:
        subprocess.run(command, check=True)
        logging.info(f"Successfully created virtual raster at {virtual_raster_path}")
    except subprocess.CalledProcessError as e:
        logging.info(f"Failed to create virtual raster: {e}")


def generate_tiles(virtual_raster_path: str, tiles_output_path: str) -> None:
    """Generate zoomable map tiles from the virtual raster with ``gdal2tiles.py``.

    Produces an XYZ tile pyramid for zoom levels 0-13 from the given VRT into
    ``tiles_output_path``. Failures are logged and swallowed.

    Args:
        virtual_raster_path: Path to the combined virtual raster (``.vrt``).
        tiles_output_path: Directory to write the generated tile pyramid into.
    """
    os.makedirs(tiles_output_path, exist_ok=True)
    command = ["gdal2tiles.py", "--zoom=0-13", virtual_raster_path, tiles_output_path]
    try:
        subprocess.run(command, check=True)
        logging.info(f"Successfully generated tiles at {tiles_output_path}")
    except subprocess.CalledProcessError as e:
        logging.info(f"Failed to generate tiles: {e}")


def clean_resources(paths: list[str]) -> None:
    """Delete the given temporary directories and files before processing.

    For each path, recursively removes a directory's contents and subdirectories
    (leaving nothing behind) or deletes a single file. Individual file-deletion
    failures are logged and skipped.

    Args:
        paths: Directory or file paths to remove.
    """
    for path in paths:
        if os.path.isdir(path):
            for root, dirs, files in os.walk(path, topdown=False):
                for file in files:
                    file_path = os.path.join(root, file)
                    try:
                        os.remove(file_path)
                    except OSError as e:
                        logging.error(f"Failed to delete file {file_path}: {e}")
                for dir in dirs:
                    dir_path = os.path.join(root, dir)
                    os.rmdir(dir_path)
            logging.info(f"Cleaned directory: {path}")
        elif os.path.isfile(path):
            os.remove(path)
            logging.info(f"Removed file: {path}")


def get_chart_paths(chart_type: str) -> dict[str, str]:
    """Build the set of input/intermediate/output paths for one chart type.

    Combines the config-loaded base directories with the chart type to produce the
    per-stage paths (original TIFs, shapefiles, cropped/reprojected/RGB temp dirs,
    virtual raster, and final charts output). The shapefiles path is intentionally
    kept hardcoded under ``resources/shape_files``.

    Args:
        chart_type: The chart type key (e.g. ``sectional``, ``ifr_enroute_low``).

    Returns:
        A mapping from stage name to its filesystem path.
    """
    return {
        "tifs_path": os.path.join(TIFS_ORIGINAL_DIR, chart_type),
        "shapes_path": os.path.join("resources/shape_files", chart_type),  # keep hardcoded
        "charts_output_path": os.path.join(CHARTS_DIR, chart_type.replace("_", "-")),
        "cropped_tifs_path": os.path.join(TEMP_FILES_DIR, "cropped_tifs"),
        "reprojected_tifs_path": os.path.join(TEMP_FILES_DIR, "reprojected_tifs"),
        "rgb_tifs_path": os.path.join(TEMP_FILES_DIR, "cropped_rgb_tifs"),
        "virtual_raster_path": os.path.join(TEMP_FILES_DIR, "virtual_raster", "combined.vrt"),
    }


def process_sectional(chart_date: str) -> None:
    """Run the full sectional-chart pipeline for the given date.

    Cleans temp directories, then downloads, crops, converts to RGBA, reprojects,
    mosaics, and tiles the sectional charts for ``chart_date``.

    Args:
        chart_date: The chart release date to process (``MM-DD-YYYY``).
    """
    paths = get_chart_paths("sectional")
    logging.info(paths)
    clean_resources(
        [
            paths["cropped_tifs_path"],
            paths["reprojected_tifs_path"],
            os.path.dirname(paths["virtual_raster_path"]),
            paths["rgb_tifs_path"],
        ]
    )
    logging.info("\n*** Processing Sectional Charts ***\n")
    download_and_extract_tifs(paths["tifs_path"], chart_date, ChartType.SECTIONAL)
    crop_tifs(paths["shapes_path"], paths["tifs_path"], paths["cropped_tifs_path"])
    convert_to_rgba(paths["cropped_tifs_path"], paths["rgb_tifs_path"])
    reproject_tifs(paths["rgb_tifs_path"], paths["reprojected_tifs_path"])
    create_virtual_raster(paths["reprojected_tifs_path"], paths["virtual_raster_path"])
    generate_tiles(paths["virtual_raster_path"], paths["charts_output_path"])
    logging.info("Sectional Charts processing completed.")


def process_terminal_area(chart_date: str) -> None:
    """Run the terminal-area chart pipeline for the given date (no cropping).

    Cleans temp directories, then downloads, converts to RGBA, reprojects,
    mosaics, and tiles the terminal-area charts for ``chart_date``. Unlike
    sectionals, terminal-area TIFs are not cropped: they change over time and no
    maintained shapefile exists for them, they do not cover the entire USA, and
    they rarely overlap.

    Args:
        chart_date: The chart release date to process (``MM-DD-YYYY``).
    """
    paths = get_chart_paths("terminal_area")
    logging.info(paths)
    clean_resources(
        [
            paths["cropped_tifs_path"],
            paths["reprojected_tifs_path"],
            os.path.dirname(paths["virtual_raster_path"]),
            paths["rgb_tifs_path"],
        ]
    )
    logging.info("\n\n *** Processing Terminal Area Charts *** \n")
    download_and_extract_tifs(paths["tifs_path"], chart_date, ChartType.TERMINAL_AREA)
    convert_to_rgba(paths["tifs_path"], paths["rgb_tifs_path"])
    reproject_tifs(paths["rgb_tifs_path"], paths["reprojected_tifs_path"])
    create_virtual_raster(paths["reprojected_tifs_path"], paths["virtual_raster_path"])
    generate_tiles(paths["virtual_raster_path"], paths["charts_output_path"])
    logging.info("Terminal Area Charts processing completed.")


def process_enroute_low(chart_date: str) -> None:
    """Run the IFR enroute-low chart pipeline for the given date.

    Cleans temp directories, then downloads, crops, reprojects, mosaics, and tiles
    the IFR enroute-low charts for ``chart_date``.

    Args:
        chart_date: The chart release date to process (``MM-DD-YYYY``).
    """
    paths = get_chart_paths("ifr_enroute_low")
    clean_resources(
        [
            paths["cropped_tifs_path"],
            paths["reprojected_tifs_path"],
            os.path.dirname(paths["virtual_raster_path"]),
        ]
    )
    logging.info("\n\n*** Processing IFR Enroute Low Charts *** \n")
    download_and_extract_tifs(paths["tifs_path"], chart_date, ChartType.IFR_ENROUTE_LOW)
    crop_tifs(paths["shapes_path"], paths["tifs_path"], paths["cropped_tifs_path"])
    reproject_tifs(paths["cropped_tifs_path"], paths["reprojected_tifs_path"])
    create_virtual_raster(paths["reprojected_tifs_path"], paths["virtual_raster_path"])
    generate_tiles(paths["virtual_raster_path"], paths["charts_output_path"])
    logging.info("IFR Enroute Low Charts processing completed.")


def process_enroute_high(chart_date: str) -> None:
    """Run the IFR enroute-high chart pipeline for the given date.

    Cleans temp directories, then downloads, crops, reprojects, mosaics, and tiles
    the IFR enroute-high charts for ``chart_date``.

    Args:
        chart_date: The chart release date to process (``MM-DD-YYYY``).
    """
    paths = get_chart_paths("ifr_enroute_high")
    clean_resources(
        [
            paths["cropped_tifs_path"],
            paths["reprojected_tifs_path"],
            os.path.dirname(paths["virtual_raster_path"]),
        ]
    )
    logging.info("\n\n*** Processing IFR Enroute High Charts ***\n")
    download_and_extract_tifs(paths["tifs_path"], chart_date, ChartType.IFR_ENROUTE_HIGH)
    crop_tifs(paths["shapes_path"], paths["tifs_path"], paths["cropped_tifs_path"])
    reproject_tifs(paths["cropped_tifs_path"], paths["reprojected_tifs_path"])
    create_virtual_raster(paths["reprojected_tifs_path"], paths["virtual_raster_path"])
    generate_tiles(paths["virtual_raster_path"], paths["charts_output_path"])
    logging.info("IFR Enroute High Charts processing completed.")


def process_helicopter(chart_date: str) -> None:
    """Run the helicopter chart pipeline for the given date.

    Cleans temp directories, then downloads, crops, converts to RGB, reprojects,
    mosaics, and tiles the helicopter charts for ``chart_date``.

    Args:
        chart_date: The chart release date to process (``MM-DD-YYYY``).
    """
    paths = get_chart_paths("helicopter")
    clean_resources(
        [
            paths["cropped_tifs_path"],
            paths["reprojected_tifs_path"],
            os.path.dirname(paths["virtual_raster_path"]),
            paths["rgb_tifs_path"],
        ]
    )
    logging.info("\n\n*** Processing Helicopter Charts ***\n")
    download_and_extract_tifs(paths["tifs_path"], chart_date, ChartType.HELICOPTER)
    crop_tifs(paths["shapes_path"], paths["tifs_path"], paths["cropped_tifs_path"])
    convert_to_rgb(paths["cropped_tifs_path"], paths["rgb_tifs_path"])
    reproject_tifs(paths["rgb_tifs_path"], paths["reprojected_tifs_path"])

    create_virtual_raster(paths["reprojected_tifs_path"], paths["virtual_raster_path"])
    generate_tiles(paths["virtual_raster_path"], paths["charts_output_path"])
    logging.info("IFR HELICOPTER processing completed.")


if __name__ == "__main__":
    logging.info("\n\n*** Start processing charts *** \n")

    check_dependencies()
    args = parse_arguments()
    chart_date = args.chart_date

    # Process charts
    process_terminal_area(chart_date)
    process_sectional(chart_date)
    process_enroute_low(chart_date)
    process_enroute_high(chart_date)
    process_helicopter(chart_date)

    logging.info("\n\n*** End processing charts *** \n")
