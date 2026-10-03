"""Startup check for optional subsystems: email delivery and chart tiles.

Runs as the ``OPTIONAL`` category of the NGAFID startup validator: when email is
enabled, verifies the email-info file is mounted and populated; and when a chart
tile base URL is configured, verifies it is a well-formed http(s) URL. Both
subsystems are skipped (recorded as passes) when not configured.
"""

from __future__ import annotations

from pathlib import Path
from typing import TYPE_CHECKING
from urllib.parse import urlparse

if TYPE_CHECKING:
    from validator import Validator


def run_check(validator: Validator) -> None:
    """Validate the optional email and chart-tile subsystems when configured.

    When email is enabled, requires ``/etc/ngafid-email.conf`` to be readable and
    to hold at least two non-empty lines (username and password); otherwise
    records the email checks as skipped. When ``ngafid.chart.tile.base.url`` is
    set, requires it to parse as an http/https URL with a host. All outcomes are
    recorded through the validator's pass/fail helpers.

    Args:
        validator: The running startup validator; supplies the email-enabled
            check, the parsed properties, the file-readable helper, and the
            pass/fail recording helpers.
    """
    category = "OPTIONAL"
    email_enabled = validator._is_email_enabled()
    if email_enabled:
        if validator._check_file_readable(
            category,
            "email info file",
            "/etc/ngafid-email.conf",
            "mount email file or disable email",
        ):
            lines = Path("/etc/ngafid-email.conf").read_text(encoding="utf-8").splitlines()
            non_empty = [line for line in lines if line.strip()]
            if len(non_empty) >= 2:
                validator._pass(
                    category,
                    "email info content",
                    "email info has at least two non-empty lines",
                )
            else:
                validator._fail(
                    category,
                    "email info content",
                    "email_info.txt must contain at least username and password lines",
                    "populate /etc/ngafid-email.conf with credentials",
                )
    else:
        validator._pass(category, "email checks", "skipped because email is disabled")

    chart_url = validator.properties.get("ngafid.chart.tile.base.url", "").strip()
    if chart_url:
        parsed = urlparse(chart_url)
        if parsed.scheme in {"http", "https"} and parsed.netloc:
            validator._pass(category, "chart url", f"configured: {chart_url}")
        else:
            validator._fail(
                category,
                "chart url",
                f"invalid chart URL format: {chart_url}",
                "set ngafid.chart.tile.base.url to a valid http(s) URL",
            )
    else:
        validator._pass(
            category,
            "chart url",
            "skipped because ngafid.chart.tile.base.url is not set",
        )
