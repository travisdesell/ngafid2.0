#!/usr/bin/env python3
import os
import shlex
import importlib.util
import json
from datetime import datetime
from typing import Dict, Tuple
from flask import Flask, jsonify, send_from_directory
import subprocess

try:
    import paramiko
except Exception:
    paramiko = None

app = Flask(__name__)

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MODULES_DIR = os.path.join(BASE_DIR, "modules")
CONFIG_PATH = os.environ.get("NGAFID_GUI_CONFIG", os.path.join(BASE_DIR, "guiconfig.properties"))
PROJECT_ROOT = os.path.abspath(os.path.join(BASE_DIR, ".."))
RUN_DIR = os.path.join(PROJECT_ROOT, "run")
RUN_STATE_PATH = os.path.join(BASE_DIR, ".run_state.json")
JAVADOCS_MODULES = [
    "ngafid-core",
    "ngafid-airsync",
    "ngafid-www",
    "ngafid-data-processor",
    "ngafid-db",
]


# ----------------------------
# Configuration
# ----------------------------

def read_properties(path: str) -> Dict[str, str]:
    props: Dict[str, str] = {}
    if not os.path.isfile(path):
        return props
    with open(path, "r", encoding="utf-8") as handle:
        for raw in handle:
            line = raw.strip()
            if not line or line.startswith("#") or line.startswith(";"):
                continue
            if "=" not in line:
                continue
            key, value = line.split("=", 1)
            props[key.strip()] = value.strip()
    return props


PROPERTIES = read_properties(CONFIG_PATH)


def _get_value(key: str) -> str:
    if key in os.environ:
        return os.environ.get(key, "")
    return PROPERTIES.get(key, "")


def _get_int(key: str, default: int) -> int:
    raw = _get_value(key)
    try:
        return int(raw)
    except (TypeError, ValueError):
        return default


def _get_bool(key: str, default: bool) -> bool:
    raw = _get_value(key).strip().lower()
    if raw in {"1", "true", "yes", "on"}:
        return True
    if raw in {"0", "false", "no", "off"}:
        return False
    return default


def _get_list(key: str) -> list:
    raw = _get_value(key)
    return [item.strip() for item in raw.split(",") if item.strip()]


MODE = _get_value("NGAFID_GUI_MODE").lower()  # 'mock' | 'ssh' | 'local'
REMOTE_HOST = _get_value("NGAFID_GUI_HOST")
REMOTE_USER = _get_value("NGAFID_GUI_USER")
REMOTE_PORT = _get_int("NGAFID_GUI_PORT", 22)
REMOTE_KEY = _get_value("NGAFID_GUI_KEY")  # <-- e.g. ~/.ssh/id_rsa (or blank to use agent/known keys)

# HTTP listener (separate from SSH port!)
HTTP_HOST = _get_value("NGAFID_GUI_HTTP_HOST")
HTTP_PORT = _get_int("NGAFID_GUI_HTTP_PORT", 5000)

# Optional server self-stop (disabled by default)
ALLOW_STOP = _get_bool("NGAFID_GUI_ALLOW_STOP", False)

# Services
SERVICES = _get_list("NGAFID_GUI_SERVICES")
SERVICES_AUTO = (not SERVICES) or (len(SERVICES) == 1 and SERVICES[0].lower() == "auto")

LOG_DIR = _get_value("NGAFID_GUI_LOG_DIR")
LOCAL_DRIVER = _get_value("NGAFID_GUI_LOCAL_DRIVER").lower()
DOCKER_COMPOSE_FILE = _get_value("NGAFID_GUI_DOCKER_COMPOSE_FILE")
DOCKER_CMD = _get_value("NGAFID_GUI_DOCKER_CMD")
DOCKER_PROJECT = _get_value("NGAFID_GUI_DOCKER_PROJECT")

# Version
VERSION = "0.4.0"

# Discovered modules
MODULES: Dict[str, Dict] = {}
MODULE_ERRORS: Dict[str, str] = {}


# ----------------------------
# Helpers
# ----------------------------

def _ok(out: str = "", err: str = "", data=None, code: int = 0):
    return jsonify({"ok": True, "code": code, "out": out, "err": err, "data": data or {}}), 200


def _fail(msg: str, code: int = 500):
    return jsonify({"ok": False, "code": code, "error": msg}), code


def exec_local(cmd: str) -> Tuple[int, str, str]:
    p = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    return p.returncode, p.stdout, p.stderr


def exec_ssh(cmd: str) -> Tuple[int, str, str]:
    if paramiko is None:
        return 1, "", "Paramiko not available"
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    try:
        ssh.connect(
            hostname=str(REMOTE_HOST),
            username=str(REMOTE_USER),
            port=int(REMOTE_PORT),
            allow_agent=True,
            look_for_keys=True,
            timeout=float(20),
            key_filename=os.path.expanduser(REMOTE_KEY) if REMOTE_KEY else None,
        )
        stdin, stdout, stderr = ssh.exec_command(cmd)
        out = stdout.read().decode()
        err = stderr.read().decode()
        rc = stdout.channel.recv_exit_status()
        ssh.close()
        return rc, out, err
    except Exception as e:
        return 1, "", f"{type(e).__name__}: {e}"


def load_modules():
    global MODULES, MODULE_ERRORS
    MODULES = {}
    MODULE_ERRORS = {}

    if not os.path.isdir(MODULES_DIR):
        return

    for entry in sorted(os.listdir(MODULES_DIR)):
        module_dir = os.path.join(MODULES_DIR, entry)
        if not os.path.isdir(module_dir):
            continue

        html_path = os.path.join(module_dir, "module.html")
        py_path = os.path.join(module_dir, "module.py")

        if not (os.path.isfile(html_path) and os.path.isfile(py_path)):
            MODULE_ERRORS[entry] = "Missing module.html or module.py"
            continue

        meta = {
            "name": entry,
            "title": entry,
            "order": 100,
            "assetsBase": f"/modules/{entry}",
            "htmlPath": f"/api/modules/{entry}/html",
        }

        try:
            spec = importlib.util.spec_from_file_location(
                f"ngafid_gui.modules.{entry}", py_path
            )
            if spec and spec.loader:
                mod = importlib.util.module_from_spec(spec)
                spec.loader.exec_module(mod)
                if hasattr(mod, "register"):
                    result = mod.register(app)
                    if isinstance(result, dict):
                        meta.update({k: v for k, v in result.items() if v is not None})
        except Exception as e:
            MODULE_ERRORS[entry] = f"{type(e).__name__}: {e}"
            continue

        MODULES[entry] = meta


# In-memory demo state for MOCK mode
MOCK_STATES: Dict[str, str] = {s: "inactive" for s in SERVICES}


def mock_set(svc: str, state: str):
    if svc in MOCK_STATES:
        MOCK_STATES[svc] = state


def _load_run_state() -> Dict[str, str]:
    if not os.path.isfile(RUN_STATE_PATH):
        return {}
    try:
        with open(RUN_STATE_PATH, "r", encoding="utf-8") as handle:
            data = json.load(handle)
        return data if isinstance(data, dict) else {}
    except (OSError, json.JSONDecodeError):
        return {}


def _save_run_state(state: Dict[str, str]) -> None:
    try:
        with open(RUN_STATE_PATH, "w", encoding="utf-8") as handle:
            json.dump(state, handle, indent=2, sort_keys=True)
            handle.write("\n")
    except OSError:
        return


def _set_run_state(key: str) -> None:
    state = _load_run_state()
    state[key] = datetime.utcnow().isoformat() + "Z"
    _save_run_state(state)


def _compose_base_cmd() -> str:
    base = DOCKER_CMD or "docker compose"
    parts = [base, "-f", DOCKER_COMPOSE_FILE]
    if DOCKER_PROJECT:
        parts.extend(["-p", DOCKER_PROJECT])
    return " ".join(parts)


def _discover_services() -> list:
    if MODE != "local" or LOCAL_DRIVER != "docker":
        return SERVICES
    base = _compose_base_cmd()
    rc, out, _ = exec_local(f"{base} config --services")
    if rc != 0:
        return SERVICES
    return [line.strip() for line in (out or "").splitlines() if line.strip()]


def _resolve_services() -> list:
    return _discover_services() if SERVICES_AUTO else SERVICES


ALLOWED_RUN_SCRIPTS = {
    "build",
    "package",
    "generate_javadocs",
    "generate_csvs",
    "generate_email_types",
    "setup_dat_importing",
    "event_helper",
    "upload_helper",
    "extract_flights",
    "extract_maintenance_flights",
    "liquibase/update",
    "liquibase/clear-checksums",
    "liquibase/hourly-materialized-views",
    "liquibase/daily-materialized-views",
    "kafka/create_topics",
    "kafka/purge_topics",
    "kafka/event_consumer",
    "kafka/event_observer",
    "kafka/upload_consumer",
    "kafka/email_consumer",
}


def run_script(script_rel: str, args: str = "") -> Tuple[int, str, str]:
    if script_rel not in ALLOWED_RUN_SCRIPTS:
        return 1, "", f"Script not allowed: {script_rel}"

    script_path = os.path.join(RUN_DIR, script_rel)
    if not os.path.isfile(script_path):
        return 1, "", f"Script not found: {script_rel}"

    arg_list = shlex.split(args or "")

    if MODE == "mock":
        rc, out, err = 0, f"[MOCK] run {script_rel} {' '.join(arg_list)}", ""
        if script_rel in {
            "liquibase/hourly-materialized-views",
            "liquibase/daily-materialized-views",
            "build",
            "package",
            "generate_javadocs",
        }:
            _set_run_state(script_rel)
        return rc, out, err

    if MODE == "local":
        cmd = ["bash", script_path] + arg_list
        p = subprocess.run(cmd, capture_output=True, text=True, cwd=PROJECT_ROOT)
        if p.returncode == 0 and script_rel in {
            "liquibase/hourly-materialized-views",
            "liquibase/daily-materialized-views",
            "build",
            "package",
            "generate_javadocs",
        }:
            _set_run_state(script_rel)
        return p.returncode, p.stdout, p.stderr

    if MODE == "ssh":
        safe_args = " ".join(shlex.quote(a) for a in arg_list)
        script_rel_path = os.path.join("run", script_rel)
        cmd = f"cd {shlex.quote(PROJECT_ROOT)} && bash {shlex.quote(script_rel_path)} {safe_args}".strip()
        rc, out, err = exec_ssh(cmd)
        if rc == 0 and script_rel in {
            "liquibase/hourly-materialized-views",
            "liquibase/daily-materialized-views",
            "build",
            "package",
            "generate_javadocs",
        }:
            _set_run_state(script_rel)
        return rc, out, err

    return 1, "", "Unknown mode"


# --- SERVICES ---
def get_statuses() -> Dict[str, str]:
    """Return per-service status mapping."""
    if MODE == "mock":
        return dict(MOCK_STATES)

    if MODE == "ssh":
        statuses = {}
        for s in SERVICES:
            rc, out, _ = exec_ssh(f"systemctl --user is-active {s} || true")
            val = (out or "").strip() or ("inactive" if rc != 0 else "active")
            statuses[s] = val
        return statuses

    if MODE == "local":
        if LOCAL_DRIVER == "docker":
            base = _compose_base_cmd()
            rc, out, err = exec_local(f"{base} ps --status running --services")
            if rc != 0:
                services = _resolve_services()
                return {s: "unknown" for s in services}
            running = {line.strip() for line in (out or "").splitlines() if line.strip()}
            services = _resolve_services()
            return {s: ("active" if s in running else "inactive") for s in services}
        statuses = {}
        for s in _resolve_services():
            rc, out, _ = exec_local(f"systemctl --user is-active {s} || true")
            val = (out or "").strip() or ("inactive" if rc != 0 else "active")
            statuses[s] = val
        return statuses

    return {s: "unknown" for s in SERVICES}


def perform_action(action: str, service: str) -> Tuple[int, str, str]:
    valid = {"start", "stop", "restart"}
    if action not in valid:
        return 1, "", f"Invalid action '{action}'"

    if MODE == "mock":
        targets = _resolve_services() if service == "all" else [service]
        for t in targets:
            if action == "start":
                mock_set(t, "active")
            elif action == "stop":
                mock_set(t, "inactive")
            elif action == "restart":
                mock_set(t, "active")
        return 0, f"[MOCK] {action} -> {', '.join(targets)}", ""

    if service == "all":
        cmd = f"systemctl --user {action} {' '.join(_resolve_services())}"
    else:
        cmd = f"systemctl --user {action} {service}"

    if MODE == "ssh":
        return exec_ssh(cmd)
    elif MODE == "local":
        if LOCAL_DRIVER == "docker":
            base = _compose_base_cmd()
            if service == "all":
                return exec_local(f"{base} {action}")
            return exec_local(f"{base} {action} {service}")
        return exec_local(cmd)
    else:
        return 1, "", "Unknown mode"


# --- LOGS ---
def _log_path(service: str, kind: str) -> str:
    # kind: 'log' or 'err'
    suffix = "err" if kind == "err" else "log"
    return f"{LOG_DIR}/{service}.{suffix}"


def _file_exists_local(path: str) -> bool:
    return os.path.exists(path)


def _file_exists_ssh(path: str) -> bool:
    rc, _, _ = exec_ssh(f"test -f {path}")
    return rc == 0


def check_log_availability(service: str) -> Dict[str, bool]:
    """Return which streams are available: log/err/journal."""
    if MODE == "mock":
        return {"log": True, "err": True, "journal": True}

    if MODE == "local" and LOCAL_DRIVER == "docker":
        return {"log": True, "err": False, "journal": True}

    avail = {"log": False, "err": False, "journal": True}  # journal always possible (may be empty)
    log_path = _log_path(service, "log")
    err_path = _log_path(service, "err")

    if MODE == "ssh":
        avail["log"] = _file_exists_ssh(log_path)
        avail["err"] = _file_exists_ssh(err_path)
        return avail

    if MODE == "local":
        avail["log"] = _file_exists_local(log_path)
        avail["err"] = _file_exists_local(err_path)
        return avail

    return {"log": False, "err": False, "journal": True}


def read_logs(service: str, n: int = 200, kind: str = "log") -> Tuple[int, str, str]:
    """
    kind: 'log' | 'err' | 'journal'
    - log/err read from LOG_DIR/{service}.log/.err
    - journal tries multiple strategies:
        1) journalctl _SYSTEMD_USER_UNIT={service}.service
        2) journalctl --user-unit {service}.service
        3) journalctl -u {service}.service
    We treat a zero-length output as "not helpful" and keep trying.
    """
    if MODE == "mock":
        from datetime import datetime
        now = datetime.now().strftime("%H:%M:%S")
        if kind == "err":
            lines = [
                f"{now} [{service}] ERROR: example failure A",
                f"{now} [{service}] WARN:  something questionable",
                f"{now} [{service}] ERROR: example failure B",
            ]
        elif kind == "journal":
            lines = [
                f"{now} [{service}] (journal) started mock unit",
                f"{now} [{service}] (journal) doing things...",
                f"{now} [{service}] (journal) finished",
            ]
        else:
            lines = [
                f"{now} [{service}] demo line 1",
                f"{now} [{service}] demo line 2",
                f"{now} [{service}] demo line 3",
            ]
        return 0, "\n".join(lines), ""

    if MODE == "local" and LOCAL_DRIVER == "docker":
        base = _compose_base_cmd()
        cmd = f"{base} logs --tail {n} --no-color {service}"
        return exec_local(cmd)

    # .log / .err file tails
    if kind in ("log", "err"):
        path = _log_path(service, kind)
        tail_cmd = f"tail -n {n} {path}"
        if MODE == "ssh":
            rc, out, err = exec_ssh(tail_cmd)
            if rc != 0 or not (out or "").strip():
                hint = f"No {kind} file at {path}; try journal."
                return 0, (out + err).strip() or hint, ""
            return rc, out, err
        if MODE == "local":
            if not os.path.exists(path):
                return 0, f"[LOCAL] No {kind} at {path}", ""
            return exec_local(tail_cmd)

    # journalctl tails (try several variants)
    if kind == "journal":
        unit = f"{service}.service"
        cmds = [
            # Most robust for user units that end up in the system journal
            f"journalctl _SYSTEMD_USER_UNIT={unit} -n {n} --no-pager 2>&1 || true",
            # Native user journal (often empty in your scenario)
            f"journalctl --user-unit {unit} -n {n} --no-pager 2>&1 || true",
            # System unit fallback (in case it’s a system service, not user)
            f"journalctl -u {unit} -n {n} --no-pager 2>&1 || true",
        ]

        def _run(cmd):
            return exec_ssh(cmd) if MODE == "ssh" else exec_local(cmd)

        out = ""
        err = ""
        for cmd in cmds:
            rc, out, err = _run(cmd)
            text = (out + err).strip()
            if text and "No journal files were found." not in text and "-- No entries --" not in text:
                return 0, text, ""
        # Show whatever the last attempt produced for transparency
        return 0, (out + err).strip() or "(no journal entries found)", ""

    return 1, "", "Unknown mode"


# --- GIT ---
def deploy_latest() -> Tuple[int, str, str]:
    if MODE == "mock":
        out = [
            "[MOCK] cd ~/ngafid2.0 && git pull -> Already up to date.",
            "[MOCK] cd ~/ngafid2.0/ngafid-frontend && npm run build -> build complete",
            f"[MOCK] systemctl --user restart {' '.join(SERVICES)} -> ok"
        ]
        return 0, "\n".join(out), ""
    if MODE == "ssh":
        composite = []
        for c in [
            "cd ~/ngafid2.0 && git pull",
            "cd ~/ngafid2.0/ngafid-frontend && npm run build",
            f"systemctl --user restart {' '.join(SERVICES)}",
        ]:
            rc, out, err = exec_ssh(c)
            composite.append(f"$ {c}\n{out}{err}")
            if rc != 0:
                return rc, "\n\n".join(composite), f"Command failed: {c}"
        return 0, "\n\n".join(composite), ""
    if MODE == "local":
        if LOCAL_DRIVER == "docker":
            composite = []
            base = _compose_base_cmd()
            cmd = f"{base} pull"
            rc, out, err = exec_local(cmd)
            composite.append(f"$ {cmd}\n{out}{err}")
            if rc != 0:
                return rc, "\n\n".join(composite), "Command failed: docker compose pull"
            cmd = f"{base} up -d"
            rc, out, err = exec_local(cmd)
            composite.append(f"$ {cmd}\n{out}{err}")
            if rc != 0:
                return rc, "\n\n".join(composite), "Command failed: docker compose up"
            return 0, "\n\n".join(composite), ""
        composite = []
        for c in [
            "cd ~/ngafid2.0 && git pull",
            "cd ~/ngafid2.0/ngafid-frontend && npm run build",
            f"systemctl --user restart {' '.join(SERVICES)}",
        ]:
            rc, out, err = exec_local(c)
            composite.append(f"$ {c}\n{out}{err}")
            if rc != 0:
                return rc, "\n\n".join(composite), f"Command failed: {c}"
        return 0, "\n\n".join(composite), ""
    return 1, "", "Unknown mode"


app.config["NGAFID"] = {
    "mode": MODE,
    "services": _resolve_services(),
    "host": REMOTE_HOST,
    "user": REMOTE_USER,
    "logDir": LOG_DIR,
    "httpHost": HTTP_HOST,
    "httpPort": HTTP_PORT,
    "allowStop": ALLOW_STOP,
    "version": VERSION,
    "configPath": CONFIG_PATH,
    "projectRoot": PROJECT_ROOT,
    "runDir": RUN_DIR,
    "allowedRunScripts": sorted(ALLOWED_RUN_SCRIPTS),
    "get_run_state": _load_run_state,
    "localDriver": LOCAL_DRIVER,
    "dockerComposeFile": DOCKER_COMPOSE_FILE,
    "ok": _ok,
    "fail": _fail,
    "run_script": run_script,
    "get_statuses": get_statuses,
    "perform_action": perform_action,
    "read_logs": read_logs,
    "check_log_availability": check_log_availability,
    "deploy_latest": deploy_latest,
}

load_modules()


# ----------------------------
# Routes
# ----------------------------

@app.route("/")
def root():
    return send_from_directory(".", "index.html")


@app.route("/api/config")
def config():
    return _ok(data={
        "mode": MODE,
        "services": _resolve_services(),
        "host": REMOTE_HOST,
        "user": REMOTE_USER,
        "logDir": LOG_DIR,
        "httpHost": HTTP_HOST,
        "httpPort": HTTP_PORT,
        "allowStop": ALLOW_STOP,
        "version": VERSION,
        "configPath": CONFIG_PATH,
        "localDriver": LOCAL_DRIVER,
        "dockerComposeFile": DOCKER_COMPOSE_FILE,
        "modules": sorted(MODULES.values(), key=lambda m: (m.get("order", 100), m.get("name", ""))),
    })



# --- GUI SERVER META ---
@app.route("/api/health")
def api_health():
    # Basic sanity check: python running, we can render, and subprocess is callable
    return _ok(data={"status": "ok", "time": datetime.utcnow().isoformat() + "Z", "version": VERSION})



@app.route("/api/version")
def api_version():
    return _ok(data={"version": VERSION})



# --- MODULES ---
@app.route("/api/modules")
def api_modules():
    modules = sorted(MODULES.values(), key=lambda m: (m.get("order", 100), m.get("name", "")))
    return _ok(data={"modules": modules, "errors": MODULE_ERRORS})



@app.route("/api/modules/<name>/html")
def api_module_html(name: str):
    if name not in MODULES:
        return _fail("unknown module", 404)
    module_dir = os.path.join(MODULES_DIR, name)
    return send_from_directory(module_dir, "module.html")



@app.route("/modules/<name>/<path:filename>")
def api_module_asset(name: str, filename: str):
    if name not in MODULES:
        return _fail("unknown module", 404)
    module_dir = os.path.join(MODULES_DIR, name)
    return send_from_directory(module_dir, filename)


def _javadocs_dir(module: str) -> str:
    return os.path.join(PROJECT_ROOT, module, "target", "site", "apidocs")


@app.route("/javadocs/")
def javadocs_index():
    items = []
    for module in JAVADOCS_MODULES:
        module_dir = _javadocs_dir(module)
        if os.path.isdir(module_dir):
            items.append(f"<li><a href=\"/javadocs/{module}/\">{module}</a></li>")
        else:
            items.append(f"<li><span style=\"opacity:.6;\">{module} (missing)</span></li>")

    if not any(os.path.isdir(_javadocs_dir(m)) for m in JAVADOCS_MODULES):
        return "Javadocs not found. Run the Generate Javadocs task first.", 404

    html = "".join([
        "<!DOCTYPE html>",
        "<meta charset=\"utf-8\">",
        "<title>NGAFID Javadocs</title>",
        "<style>body{font:16px/1.4 system-ui,sans-serif;padding:24px;}ul{padding-left:20px;}</style>",
        "<h1>NGAFID Javadocs</h1>",
        "<ul>",
        *items,
        "</ul>",
    ])
    return html, 200


@app.route("/javadocs/<module>/")
@app.route("/javadocs/<module>/<path:filename>")
def javadocs_module(module: str, filename: str = "index.html"):
    if module not in JAVADOCS_MODULES:
        return "Unknown module", 404
    module_dir = _javadocs_dir(module)
    if not os.path.isdir(module_dir):
        return "Javadocs not found. Run the Generate Javadocs task first.", 404
    return send_from_directory(module_dir, filename)



@app.route("/favicon.ico")
def favicon():
    return "", 204



if __name__ == "__main__":
    print(f"NGAFID GUI {VERSION} starting in MODE='{MODE}', LOG_DIR='{LOG_DIR}', on {HTTP_HOST}:{HTTP_PORT}")
    app.run(host=HTTP_HOST, port=HTTP_PORT)
