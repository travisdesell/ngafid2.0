from flask import current_app, request


def register(app):
    @app.route("/api/logs/<service>")
    def api_logs(service: str):
        ctx = current_app.config["NGAFID"]
        n = int(request.args.get("n", "200"))
        kind = request.args.get("kind", "log")
        rc, out, err = ctx["read_logs"](service, n=n, kind=kind)
        if rc == 0:
            return ctx["ok"](out=out, err=err)
        return ctx["fail"](err or out or "unknown error", 500)

    @app.route("/api/logs/<service>/<kind>")
    def api_logs_kind(service: str, kind: str):
        ctx = current_app.config["NGAFID"]
        n = int(request.args.get("n", "200"))
        rc, out, err = ctx["read_logs"](service, n=n, kind=kind)
        if rc == 0:
            return ctx["ok"](out=out, err=err)
        return ctx["fail"](err or out or "unknown error", 500)

    @app.route("/api/logs/<service>/available")
    def api_logs_available(service: str):
        ctx = current_app.config["NGAFID"]
        return ctx["ok"](data={"available": ctx["check_log_availability"](service)})

    return {
        "title": "Log Controls",
        "order": 20,
    }
