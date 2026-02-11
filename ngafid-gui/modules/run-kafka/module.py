from flask import current_app, request


SCRIPTS = {
    "kafka/create_topics",
    "kafka/purge_topics",
    "kafka/event_consumer",
    "kafka/event_observer",
    "kafka/upload_consumer",
    "kafka/email_consumer",
}


def register(app):
    @app.route("/api/run/kafka", methods=["POST"])
    def api_run_kafka():
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
        "title": "Kafka Tools",
        "order": 60,
    }
