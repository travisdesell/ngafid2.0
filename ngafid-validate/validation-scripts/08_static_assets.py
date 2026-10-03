"""Startup check that static web assets and Mustache templates are present.

Runs as part of the ``FS`` category of the NGAFID startup validator: resolves the
static-asset and Mustache-template directories from the effective configuration,
verifies they exist and are readable, and confirms each contains the expected
asset/template files.
"""

from __future__ import annotations

from pathlib import Path
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from validator import Validator


def run_check(validator: Validator) -> None:
    """Validate the static-asset and Mustache-template directories on disk.

    Resolves each directory from the effective configuration and records a fail
    when a path cannot be resolved. For each resolved directory, verifies read
    access and then checks that the expected static assets (js/css/html) or
    template files are present. All outcomes are recorded through the validator's
    pass/fail helpers.

    Args:
        validator: The running startup validator; supplies the effective-property
            resolver, the directory check helper, and the pass/fail recording
            helpers.
    """
    category = "FS"
    static_dir = validator._effective_property("ngafid.static.dir")
    templates_dir = validator._effective_property("ngafid.mustache.template.dir")

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

    if templates_dir:
        if validator._check_dir_rw(category, "template dir", templates_dir, check_write=False):
            _check_template_assets(validator, category, Path(templates_dir))
    else:
        validator._fail(
            category,
            "template dir",
            "could not resolve effective ngafid.mustache.template.dir",
            "set template directory properties",
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


def _check_template_assets(validator, category, templates_dir: Path):
    template_count = len(list(templates_dir.glob("*.html"))) + len(list(templates_dir.glob("*.mustache")))
    if template_count < 1:
        validator._fail(
            category,
            "template files",
            f"no template files found in {templates_dir}",
            "ensure ngafid-static/templates contains HTML/mustache templates",
        )
    else:
        validator._pass(
            category,
            "template files",
            f"template count={template_count}",
        )
