# flake8: noqa: E501
from pathlib import Path


def run_check(validator):
    category = "FS"
    static_dir = validator._effective_property("ngafid.static.dir")

    if static_dir:
        if validator._check_dir_rw(category, "static dir", static_dir, check_write=False):
            _check_static_assets(validator, category, Path(static_dir))
    else:
        validator._fail(
            category,
            "static dir",
            "could not resolve effective ngafid.static.dir",
            "set static directory properties",
        )


def _check_static_assets(validator, category, static_dir: Path):
    js_count = len(list(static_dir.rglob("*.js")))
    css_count = len(list(static_dir.rglob("*.css")))
    html_count = len(list(static_dir.rglob("*.html")))

    if js_count + css_count + html_count < 3:
        validator._fail(
            category,
            "static asset files",
            f"static dir {static_dir} has too few web assets (js={js_count}, css={css_count}, html={html_count})",
            "build frontend/static assets and ensure mount points are correct",
        )
    else:
        validator._pass(
            category,
            "static asset files",
            f"asset counts look healthy (js={js_count}, css={css_count}, html={html_count})",
        )