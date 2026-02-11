from flask import current_app, request


SCRIPTS = {
    "setup_dat_importing",
    "generate_csvs",
    "generate_email_types",
    "extract_flights",
    "extract_maintenance_flights",
    "event_helper",
    "upload_helper",
}


def register(app):
    @app.route("/api/run/data-tools", methods=["POST"])
    def api_run_data_tools():
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
        "title": "Data Tools",
        "order": 80,
    }
