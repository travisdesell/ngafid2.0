import os
from datetime import datetime
from flask import current_app, request


SCRIPTS = {
    "build",
    "package",
    "generate_javadocs",
}


def register(app):
    @app.route("/api/run/build/status")
    def api_run_build_status():
        ctx = current_app.config["NGAFID"]
        root = ctx["projectRoot"]
        state = ctx["get_run_state"]()

        module_targets = [
            os.path.join(root, "ngafid-core", "target"),
            os.path.join(root, "ngafid-airsync", "target"),
            os.path.join(root, "ngafid-www", "target"),
            os.path.join(root, "ngafid-data-processor", "target"),
            os.path.join(root, "ngafid-db", "target"),
        ]

        javadocs_paths = [
            os.path.join(root, "ngafid-core", "target", "site", "apidocs", "index.html"),
            os.path.join(root, "ngafid-airsync", "target", "site", "apidocs", "index.html"),
            os.path.join(root, "ngafid-www", "target", "site", "apidocs", "index.html"),
            os.path.join(root, "ngafid-data-processor", "target", "site", "apidocs", "index.html"),
            os.path.join(root, "ngafid-db", "target", "site", "apidocs", "index.html"),
        ]

        def latest_mtime(paths):
            mtimes = [os.path.getmtime(p) for p in paths if os.path.isfile(p)]
            if not mtimes:
                return None
            return datetime.utcfromtimestamp(max(mtimes)).isoformat() + "Z"

        def latest_mtime_in_targets(patterns):
            mtimes = []
            for target_dir in module_targets:
                if not os.path.isdir(target_dir):
                    continue
                for root_dir, _, files in os.walk(target_dir):
                    for name in files:
                        if any(name.endswith(p) for p in patterns):
                            mtimes.append(os.path.getmtime(os.path.join(root_dir, name)))
            if not mtimes:
                return None
            return datetime.utcfromtimestamp(max(mtimes)).isoformat() + "Z"

        build_time = latest_mtime_in_targets([".jar", ".class"]) or state.get("build")
        package_time = latest_mtime_in_targets(["-jar-with-dependencies.jar"]) or state.get("package")
        javadocs_time = latest_mtime(javadocs_paths) or state.get("generate_javadocs")

        return ctx["ok"](data={
            "build": build_time,
            "package": package_time,
            "javadocs": javadocs_time,
        })

    @app.route("/api/run/build", methods=["POST"])
    def api_run_build():
        ctx = current_app.config["NGAFID"]
        payload = request.get_json(silent=True) or {}
        script = payload.get("script", "")
        args = payload.get("args", "")
        if script not in SCRIPTS:
            return ctx["fail"]("Script not allowed", 400)
        rc, out, err = ctx["run_script"](script, args)
        if rc == 0:
            return ctx["ok"](out=out, err=err)
        return ctx["fail"](err or out or "unknown error", 500)

    return {
        "title": "Build & Docs",
        "order": 70,
    }
