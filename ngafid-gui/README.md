# WIP Experimental NGAFID GUI Control Tool

### About

Runs a small local server that provides an in-browser GUI for managing the NGAFID services, viewing logs, etc.

**⚠ This is all WIP!**

<sup>Last Updated: 2/11/26</sup>

---

### Config

Create a `guiconfig.properties` file in this folder by copying `guiconfig.template.properties`, then edit values as needed.

### Usage

This section assumes that you are using the GUI server while SSH'd into the beta server using the `local` mode, and that you're using the default HTTP port `5000`.

- Switch to the `ngafid` user (if you haven't already): `$ sudo su - ngafid`
- ❔ If the GUI server is already open:
  - Ensure that Port `5000` is forwarded with the Forwarded Address `localhost:5000`
- ❓ If the GUI server is _not_ already open:
  - Move to the GUI folder: `$ cd ./ngafid-gui`
  - Set `NGAFID_GUI_MODE=local` inside `guiconfig.properties`
  - If using Docker, set `NGAFID_GUI_LOCAL_DRIVER=docker`
  - ❔ If the Python virtual environment (i.e., `./ngafid-gui/.venv`) already exists:
    - Open the virtual environment: `$ source ./.venv/bin/activate`
  - ❓ If the Python virtual environment does _not_ already exist:
    - Create the virtual environment: `$ python3 -m venv .venv`
    - Open the virtual environment: `$ source ./.venv/bin/activate`
    - Install `flask` and `paramiko`: `$ pip install flask paramiko`
  - Run the server: `$ python3 server_manager.py`
- Open the page in your browser: `http://127.0.0.1:5000` or `http://localhost:5000/`

---

### Functionality

- Services
  - Start / Stop / Restart all services
  - Start / Stop / Restart individual services
  - View the status of individual services

- Logs
  - View the logs for a service
  - Auto-refresh the output for the service

- Git
  - Deploy the latest changes (Pull → Build → Restart services)

- GUI Server Control
  - Stop the server
