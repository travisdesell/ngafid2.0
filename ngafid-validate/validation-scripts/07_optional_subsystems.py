# flake8: noqa: E501
from urllib.parse import urlparse
from pathlib import Path


def run_check(validator):
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
