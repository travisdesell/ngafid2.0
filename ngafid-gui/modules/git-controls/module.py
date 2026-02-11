from flask import current_app


def register(app):
    @app.route("/api/deploy", methods=["POST"])
    def api_deploy():
        ctx = current_app.config["NGAFID"]
        rc, out, err = ctx["deploy_latest"]()
        if rc == 0:
            return ctx["ok"](out=out, err=err)
        return ctx["fail"](err or out or "unknown error", 500)

    return {
        "title": "Git Controls",
        "order": 30,
    }
