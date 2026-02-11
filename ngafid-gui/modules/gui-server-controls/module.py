import os
from flask import current_app, request


def register(app):
    @app.route("/api/server/stop", methods=["POST"])
    def api_server_stop():
        ctx = current_app.config["NGAFID"]
        if not ctx["allowStop"]:
            return ctx["fail"]("Stop disabled. Set NGAFID_GUI_ALLOW_STOP=1 to enable.", 403)
        func = request.environ.get("werkzeug.server.shutdown")
        if func is not None:
            func()
            return ctx["ok"](out="Shutting down via werkzeug…")
        try:
            os.kill(os.getpid(), 15)
            return ctx["ok"](out="Shutting down via SIGTERM…")
        except Exception as e:
            return ctx["fail"](f"stop error: {e}")

    return {
        "title": "GUI Server Controls",
        "order": 40,
    }
