"""
The script downloads and processes tif files,and generates tiles for aviation charts from the FAA website.
@Usage: The script is intended to be called from chartServer.py. But it can also be run separately for testing.
        python chartProcessor.py --chart_date MM-DD-YYYY  - provide date in MM-DD-YYYY format
        The date should be the date of the chart update, the dates are in the config.json file or in the FAA website.
        Example: python chartProcessor.py --chart_date 12-26-2024

        The file config.json contains the configurations such as URLs for downloading the TIF files,
        the areas for each chart type, and the update dates.
        The original schedule update can be found here: https://www.faa.gov/air_traffic/flight_info/aeronav/productcatalog/doles/media/Product_Schedule.pdf
        The original raster charts can be found here: https://www.faa.gov/air_traffic/flight_info/aeronav/digital_products/vfr/

@Author: Roman Kozulia
"""
import os
import subprocess
import requests
import zipfile
from enum import Enum
import json
import argparse
import tempfile
import logging
import datetime
import shutil
import sys

from logging.handlers import RotatingFileHandler

def check_dependencies():
    """Dependency that may need to be installed manually"""
    commands = ["gdalwarp", "gdal2tiles.py", "gdal_translate"]
    for command in commands:
        if not shutil.which(command):
            logging.error(f"Dependency not found: {command}. Please install it before running the script.")
            sys.exit(1)


"""Configure logging. Log files will be rotating if the size will reach 10 MB"""""
log_file = "ngafid-chart-processor/chart_processor.log"
log_dir = os.path.dirname(log_file)

os.makedirs(log_dir, exist_ok=True)

if not os.path.isfile(log_file):
    with open(log_file, 'w') as f:
        f.write("")  # Create an empty log file

max_log_file_size = 10 * 1024 * 1024  # 10 MB
backup_count = 2  # Number of backup files to keep

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[
        RotatingFileHandler(log_file, maxBytes=max_log_file_size, backupCount=backup_count),
        logging.StreamHandler()
    ]
)


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[
        RotatingFileHandler(log_file, maxBytes=max_log_file_size, backupCount=backup_count),
        logging.StreamHandler()
    ]
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

configuration_file = "ngafid-chart-processor/chart_service_config.json"

def validate_date(date_str):
    """
    Checks if date passed is in the format: MM-DD-YYYY
    :param date_str: date
    :return: none
    """

    try:
        datetime.datetime.strptime(date_str, "%m-%d-%Y")
    except ValueError:
        raise argparse.ArgumentTypeError(f"Invalid date format: {date_str}. Expected MM-DD-YYYY.")
    return date_str

def parse_arguments():
    """
    Parse command-line arguments.
    Expected argument for running the chartProcessor.py: --chart_date, example: --chart_date 12-26-2024
    :param: none
    :return: none
    """
    parser = argparse.ArgumentParser(description="Process aviation charts.")
    parser.add_argument(
        "--chart_date",
        type=validate_date,
        help="The date for which to process charts (format: MM-DD-YYYY)."
    )
    return parser.parse_args()

# Load configuration
def load_config(config_path=configuration_file):
    """
    Load the configuration from the JSON file.
    :param config_path: Path to the Json configuration file
    :return:
    """
    if not os.path.exists(config_path):
        logging.error(f"Configuration file {config_path} not found.")
        raise FileNotFoundError(f"Configuration file {config_path} not found.")

    with open(config_path, "r") as f:
        try:
            data = json.load(f)
            return data  # Return the entire configuration
        except json.JSONDecodeError as e:
            logging.error(f"Error parsing JSON in {config_path}: {e}")
            raise ValueError(f"Error parsing JSON in {config_path}: {e}")

# Load the configuration file
CONFIG = load_config()

def download_and_extract_tifs(tifs_path, date, chart_type: ChartType):
    """
    Downloads and extracts TIF files for a given date and chart type.
    :param tifs_path: The path to store the extracted TIF files.
    :param date: The date for which to download the TIF files.
    :param chart_type: he type of chart to download and extract.
    :return: none
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
            # Create a temporary directory for extraction
            with tempfile.TemporaryDirectory() as temp_dir:
                zip_path = os.path.join(temp_dir, f"{area}.zip")

                # Download the ZIP file
                logging.info(f"Downloading {zip_url}")
                response = requests.get(zip_url)
                response.raise_for_status()

                with open(zip_path, 'wb') as f:
                    f.write(response.content)

                logging.info(f"Extracting {zip_path}")
                with zipfile.ZipFile(zip_path, 'r') as zip_ref:
                    zip_ref.extractall(temp_dir)

                logging.info(f"Downloaded and extracted files for {area}")

                # Move TIF files to the target directory
                for file_name in os.listdir(temp_dir):
                    if file_name.endswith(".tif"):
                        input_tif = os.path.join(temp_dir, file_name)
                        
                        # For IFR_ENROUTE_LOW or IFR_ENROUTE_HIGH, convert to lowercase
                        if chart_type in [ChartType.IFR_ENROUTE_LOW, ChartType.IFR_ENROUTE_HIGH]:
                            base_name, ext = os.path.splitext(file_name)
                            file_name = f"{base_name.lower()}{ext}"  # Lowercase name

                        output_tif = os.path.join(tifs_path, file_name)

                        shutil.move(input_tif, output_tif)
                        logging.info(f"Moved {input_tif} to {output_tif}")

        except requests.RequestException as e:
            logging.info(f"Failed to download {zip_url}: {e}")
        except zipfile.BadZipFile as e:
            logging.info(f"Failed to extract {zip_path}: {e}")


def download_terminal_area_set(base_url, date, save_path):
    """
     Downloads and extracts terminal area charts containing 'TAC' in their filenames.
    :param base_url: The base URL for downloading the terminal area ZIP file.
    :param date: The date for which to download the charts (MM-DD-YYYY).
    :param save_path: The directory to save the filtered TIF files.
    :return: none
    """

    os.makedirs(save_path, exist_ok=True)

    # Format the URL with the provided date
    terminal_zip_url = base_url.format(date=date)

    try:
        # Create a temporary directory for extraction
        with tempfile.TemporaryDirectory() as temp_dir:
            zip_path = os.path.join(temp_dir, "Terminal.zip")

            # Download the ZIP file
            logging.info(f"Downloading {terminal_zip_url}")
            response = requests.get(terminal_zip_url)
            response.raise_for_status()

            with open(zip_path, 'wb') as f:
                f.write(response.content)

            logging.info(f"Extracting {zip_path}")
            with zipfile.ZipFile(zip_path, 'r') as zip_ref:
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


def crop_tifs(shape_file_paths, tifs_path, cropped_tifs_path):
    """
    Crops TIF files to the shape of the sectional charts.
    :param shape_file_paths: The path to the directory containing shape file folders.
    :param tifs_path: The path to the directory containing TIF files.
    :param cropped_tifs_path: The path to store the cropped TIF files.
    :return:
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
            "-cutline", shp_file_path,
            "-crop_to_cutline",
       #     "-dstalpha",
       #     "-dstnodata", "0",
            tif_file_path,
            cropped_tif_path
        ]

        try:
            subprocess.run(command, check=True)
            logging.info(f"Successfully cropped {tif_file_path} to {cropped_tif_path}")
        except subprocess.CalledProcessError as e:
            logging.info(f"Failed to crop {tif_file_path}: {e}")


def convert_to_rgba(cropped_tifs_path, output_tifs_path):
    """
    Converts to rgba tif format. Needed for accurate color rendering.
    Only need to be applied to Sectional and Terminal area charts.
    :param cropped_tifs_path: Path to the directory where cropped tif files stored
    :param output_tifs_path: Path to where converted tif files will be stored
    :return: none
    """

    os.makedirs(output_tifs_path, exist_ok=True)

    for file_name in os.listdir(cropped_tifs_path):
        if file_name.endswith(".tif"):
            input_tif = os.path.join(cropped_tifs_path, file_name)
            output_tif = os.path.join(output_tifs_path, file_name)

            # Use gdal_translate with -expand rgba to convert palette to RGBA
            command = [
                "gdal_translate",
                "-of", "GTiff",         # Ensure output is in GeoTIFF format
                "-expand", "rgba",      # Convert to RGBA
                "-a_nodata", "255",     # Explicitly set NoData to white
                "-co", "COMPRESS=LZW",  # Lossless compression
                "-co", "TILED=YES",     # Enable tiling
                input_tif,
                output_tif
            ]

            try:
                subprocess.run(command, check=True)
                logging.info(f"Successfully converted {input_tif} to {output_tif} (RGBA)")
            except subprocess.CalledProcessError as e:
                logging.info(f"Failed to convert {input_tif} to RGBA: {e}")


def convert_to_rgb(cropped_tifs_path, output_tifs_path):
    """
    Converts palette-based TIFs to true RGB (3-band) without transparency.
    Strips out alpha that may exist in color table.
    """
    os.makedirs(output_tifs_path, exist_ok=True)

    for file_name in os.listdir(cropped_tifs_path):
        if file_name.endswith(".tif"):
            input_tif = os.path.join(cropped_tifs_path, file_name)
            output_tif = os.path.join(output_tifs_path, file_name)

            command = [
                "gdal_translate",
                "-of", "GTiff",
                "-expand", "rgb",  # ✅ Force RGB, drops any alpha band
                "-co", "COMPRESS=LZW",
                "-co", "TILED=YES",
                input_tif,
                output_tif
            ]

            try:
                subprocess.run(command, check=True)
                logging.info(f"Converted {input_tif} to RGB (no transparency)")
            except subprocess.CalledProcessError as e:
                logging.error(f"Failed to convert {file_name} to RGB: {e}")



def reproject_tifs(input_tifs_path, reprojected_tifs_path):
    """
    Reprojects tif files to EPSG:3857 system.
    :param input_tifs_path: Path to the input files
    :param reprojected_tifs_path: Path to the output files
    :return: hone
    """
    os.makedirs(reprojected_tifs_path, exist_ok=True)

    # Loop through all TIF files in the input_tifs_path
    for file_name in os.listdir(input_tifs_path):
        if file_name.endswith(".tif"):
            input_tif = os.path.join(input_tifs_path, file_name)
            output_tif = os.path.join(reprojected_tifs_path, file_name)  # Keep the original file name

            command = [
                "gdalwarp",
                "-t_srs", "EPSG:3857",  # Reproject to Web Mercator
                "-dstnodata", "0",
                #  "-co", "TILED=YES",     # Enable tiling for efficiency
                input_tif,
                output_tif
            ]

            try:
                subprocess.run(command, check=True)
                logging.info(f"Successfully reprojected {input_tif} to {output_tif}")
            except subprocess.CalledProcessError as e:
                logging.info(f"Failed to reproject {input_tif}: {e}")

def create_virtual_raster(reprojected_tifs_path, virtual_raster_path):
    """
    Combines tif files into a single virtual raster.
    :param reprojected_tifs_path: Path to tif files to be combined
    :param virtual_raster_path: Path to where the single raster to be stored
    :return: none
    """
    os.makedirs(os.path.dirname(virtual_raster_path), exist_ok=True)

    # Find all reprojected TIF files
    input_files = [
        os.path.join(reprojected_tifs_path, file_name)
        for file_name in os.listdir(reprojected_tifs_path)
        if file_name.endswith(".tif")
    ]

    command = [
        "gdalbuildvrt",
        virtual_raster_path
    ] + input_files

    try:
        subprocess.run(command, check=True)
        logging.info(f"Successfully created virtual raster at {virtual_raster_path}")
    except subprocess.CalledProcessError as e:
        logging.info(f"Failed to create virtual raster: {e}")

def generate_tiles(virtual_raster_path, tiles_output_path):
    """
    Generates tiles that allow zoom capability. Zoom lever is defined as 0-10
    :param virtual_raster_path:
    :param tiles_output_path:
    :return: none
    """
    os.makedirs(tiles_output_path, exist_ok=True)
    command = [
        "gdal2tiles.py",
        "--zoom=0-10",
        virtual_raster_path,
        tiles_output_path
    ]
    try:
        subprocess.run(command, check=True)
        logging.info(f"Successfully generated tiles at {tiles_output_path}")
    except subprocess.CalledProcessError as e:
        logging.info(f"Failed to generate tiles: {e}")

def clean_resources(paths):
    """
    Cleans temp file directories for before processing
    :param paths: paths to the directories to be cleaned.
    :return: none
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

def get_chart_paths(chart_type):
    """
    Generates files paths to directories for different tif file processes.
    :param chart_type: Type of the chart being processed.
    :return: dictionary with paths to directories needed for tif file processing.
    """
    """Returns paths for a given chart type."""
    base_path = os.getenv("NGAFID_CHART_PROCESSOR_PATH", "ngafid-chart-processor/")
    temp_file_path = os.path.join(base_path, "temp_files")

    return {
        "tifs_path": os.path.join(base_path, "tifs_original", chart_type),
        "shapes_path": os.path.join(base_path, "shape_files", chart_type),
        "charts_output_path": os.path.join(base_path, "charts", chart_type.replace("_", "-")),
        "cropped_tifs_path": os.path.join(temp_file_path, "cropped_tifs"),
        "reprojected_tifs_path": os.path.join(temp_file_path, "reprojected_tifs"),
        "rgb_tifs_path": os.path.join(temp_file_path, "cropped_rgb_tifs"),
        "virtual_raster_path": os.path.join(temp_file_path, "virtual_raster", "combined.vrt"),
    }

def process_sectional(chart_date):
    """Processing steps for sectional charts."""
    paths = get_chart_paths("sectional")
    clean_resources([
        paths["cropped_tifs_path"],
        paths["reprojected_tifs_path"],
        os.path.dirname(paths["virtual_raster_path"]),
        paths["rgb_tifs_path"],
    ])
    logging.info("\n*** Processing Sectional Charts ***\n")
    download_and_extract_tifs(paths["tifs_path"], chart_date, ChartType.SECTIONAL)
    crop_tifs(paths["shapes_path"], paths["tifs_path"], paths["cropped_tifs_path"])
    convert_to_rgba(paths["cropped_tifs_path"], paths["rgb_tifs_path"])
    reproject_tifs(paths["rgb_tifs_path"], paths["reprojected_tifs_path"])
    create_virtual_raster(paths["reprojected_tifs_path"], paths["virtual_raster_path"])
    generate_tiles(paths["virtual_raster_path"], paths["charts_output_path"])
    logging.info("Sectional Charts processing completed.")


def process_terminal_area(chart_date):
    """
    Processing steps for Terminal Area charts.
    NOTE! Sectional charts are processed as they are. We don't crop terminal area tifs, since they may
    change, and we can not maintain(and update) shape file for terminal area tifs. Also, terminal
    area charts do not cover the entire area of the USA, and they rarely overlap.
    :param chart_date: date when the chart is released / scheduled to be updated.
    :return: none
    """
    paths = get_chart_paths("terminal_area")
    clean_resources([
        paths["cropped_tifs_path"],
        paths["reprojected_tifs_path"],
        os.path.dirname(paths["virtual_raster_path"]),
        paths["rgb_tifs_path"],
    ])
    logging.info("\n\n *** Processing Terminal Area Charts *** \n")
    download_and_extract_tifs(paths["tifs_path"], chart_date, ChartType.TERMINAL_AREA)
    convert_to_rgba(paths["tifs_path"], paths["rgb_tifs_path"])
    reproject_tifs(paths["rgb_tifs_path"], paths["reprojected_tifs_path"])
    create_virtual_raster(paths["reprojected_tifs_path"], paths["virtual_raster_path"])
    generate_tiles(paths["virtual_raster_path"], paths["charts_output_path"])
    logging.info("Terminal Area Charts processing completed.")


def process_enroute_low(chart_date):
    """
    Processing steps for Enroute Low charts.
    :param chart_date: date when the chart is released / scheduled to be updated.
    :return: none
    """
    paths = get_chart_paths("ifr_enroute_low")
    clean_resources([
        paths["cropped_tifs_path"],
        paths["reprojected_tifs_path"],
        os.path.dirname(paths["virtual_raster_path"]),
    ])
    logging.info("\n\n*** Processing IFR Enroute Low Charts *** \n")
    download_and_extract_tifs(paths["tifs_path"], chart_date, ChartType.IFR_ENROUTE_LOW)
    crop_tifs(paths["shapes_path"], paths["tifs_path"], paths["cropped_tifs_path"])
    reproject_tifs(paths["cropped_tifs_path"], paths["reprojected_tifs_path"])
    create_virtual_raster(paths["reprojected_tifs_path"], paths["virtual_raster_path"])
    generate_tiles(paths["virtual_raster_path"], paths["charts_output_path"])
    logging.info("IFR Enroute Low Charts processing completed.")


def process_enroute_high(chart_date):
    """
    Processing steps for Enroute High charts.
    :param chart_date: date when the chart is released / scheduled to be updated.
    :return: none
    """
    paths = get_chart_paths("ifr_enroute_high")
    clean_resources([
        paths["cropped_tifs_path"],
        paths["reprojected_tifs_path"],
        os.path.dirname(paths["virtual_raster_path"]),
    ])
    logging.info("\n\n*** Processing IFR Enroute High Charts ***\n")
    download_and_extract_tifs(paths["tifs_path"], chart_date, ChartType.IFR_ENROUTE_HIGH)
    crop_tifs(paths["shapes_path"], paths["tifs_path"], paths["cropped_tifs_path"])
    reproject_tifs(paths["cropped_tifs_path"], paths["reprojected_tifs_path"])
    create_virtual_raster(paths["reprojected_tifs_path"], paths["virtual_raster_path"])
    generate_tiles(paths["virtual_raster_path"], paths["charts_output_path"])
    logging.info("IFR Enroute High Charts processing completed.")


def process_helicopter(chart_date):
    """
    Processing steps for Helicopter.
    :param chart_date: date when the chart is released / scheduled to be updated.
    :return: none
    """
    paths = get_chart_paths("helicopter")
    clean_resources([
        paths["cropped_tifs_path"],
        paths["reprojected_tifs_path"],
        os.path.dirname(paths["virtual_raster_path"]),
        paths["rgb_tifs_path"],
    ])
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
