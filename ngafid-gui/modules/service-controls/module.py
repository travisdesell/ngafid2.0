from flask import current_app, request


def register(app):
    @app.route("/api/services/status")
    def api_status():
        ctx = current_app.config["NGAFID"]
        return ctx["ok"](data={"statuses": ctx["get_statuses"]()})

    @app.route("/api/services/<action>", methods=["POST"])
    def api_action(action: str):
        ctx = current_app.config["NGAFID"]
        payload = request.get_json(silent=True) or {}
        service = payload.get("service", "")
        if not service:
            return ctx["fail"]("Missing 'service' field", 400)
        rc, out, err = ctx["perform_action"](action, service)
        status_after = ctx["get_statuses"]()
        if rc == 0:
            return ctx["ok"](out=out, err=err, data={"statuses": status_after})
        return ctx["fail"](err or out or "unknown error", 500)

    return {
        "title": "Service Controls",
        "order": 10,
    }
