from flask import current_app, request


SCRIPTS = {
    "liquibase/update",
    "liquibase/clear-checksums",
    "liquibase/hourly-materialized-views",
    "liquibase/daily-materialized-views",
}


def register(app):
    @app.route("/api/run/liquibase/status")
    def api_run_liquibase_status():
        ctx = current_app.config["NGAFID"]
        state = ctx["get_run_state"]()
        return ctx["ok"](data={
            "hourly": state.get("liquibase/hourly-materialized-views"),
            "daily": state.get("liquibase/daily-materialized-views"),
        })

    @app.route("/api/run/liquibase", methods=["POST"])
    def api_run_liquibase():
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
        "title": "Liquibase",
        "order": 50,
    }
