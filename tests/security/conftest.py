"""Fixtures and configuration for the MiraNova adversarial security tests (#57).

Configuration (environment variables):
    MIRANOVA_BASE_URL              backend under test (default http://127.0.0.1:8000)
    MIRANOVA_TIMEOUT               HTTP timeout in seconds (default 10)
    MIRANOVA_ALLOW_REMOTE          set to 1 to allow a non-local BASE_URL (team-owned demo host only)
    MIRANOVA_LOG_PATH              backend log file to scan in SEC-10 (optional)
    MIRANOVA_MAX_FAILED_ATTEMPTS   wrong-secret attempts before throttling is expected (default 10)
    MIRANOVA_MAX_EXPIRY_WAIT       max seconds SEC-08 may wait for a challenge to expire (default 130)
    MIRANOVA_TEST_JWT_SECRET       HS256 key of a *local test* backend, enables the expired-token test (optional)
"""

from __future__ import annotations

import datetime as dt
import subprocess
from pathlib import Path
from urllib.parse import urlparse

import httpx
import pytest

import sec_contract as contract
from sec_config import ALLOW_REMOTE, BASE_URL, LOCAL_HOSTS, TIMEOUT
from sec_helpers import Device, SecApi, User


# ---------------------------------------------------------------------------
# Ethics guard (SRS §12, ASM-THR-06): only the team's own local environment
# ---------------------------------------------------------------------------
def pytest_configure(config: pytest.Config) -> None:
    config.addinivalue_line("markers", "backend: needs a running MiraNova backend at MIRANOVA_BASE_URL")
    config.addinivalue_line("markers", "ratelimit: deliberately triggers throttling; run separately if limits are per-IP")
    config.addinivalue_line("markers", "slow: waits for real expiry timers")

    host = urlparse(BASE_URL).hostname or ""
    if host not in LOCAL_HOSTS and not ALLOW_REMOTE:
        pytest.exit(
            f"Refusing to run security tests against non-local host '{host}'. These tests are only for the "
            "team's own environment with synthetic accounts (SRS §12). Set MIRANOVA_ALLOW_REMOTE=1 only for a "
            "team-owned demo host.",
            returncode=4,
        )


def pytest_addoption(parser: pytest.Parser) -> None:
    parser.addoption(
        "--sec-report",
        action="store",
        default=None,
        help="write a Markdown evidence report (SRS §13.3 fields) to this path",
    )


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------
_backend_state: dict[str, str] = {}


@pytest.fixture(scope="session")
def api() -> SecApi:
    client = SecApi(BASE_URL, TIMEOUT)
    try:
        resp = client.client.get(contract.ENDPOINTS["health"] or "/health")
    except httpx.HTTPError as exc:
        client.close()
        _backend_state["status"] = f"not reachable ({type(exc).__name__})"
        pytest.skip(f"backend not reachable at {BASE_URL} ({type(exc).__name__}); start it or set MIRANOVA_BASE_URL")
    if resp.status_code != 200:
        client.close()
        _backend_state["status"] = f"health returned HTTP {resp.status_code}"
        pytest.skip(f"backend health check returned HTTP {resp.status_code} at {BASE_URL}")
    _backend_state["status"] = f"reachable, health: {resp.text[:120]}"
    yield client
    client.close()


@pytest.fixture
def user(api: SecApi) -> User:
    return api.create_user("victim")


@pytest.fixture
def enrolled(api: SecApi, user: User) -> tuple[User, Device]:
    return user, api.enroll(user)


@pytest.fixture
def other_enrolled(api: SecApi) -> tuple[User, Device]:
    other = api.create_user("other")
    return other, api.enroll(other)


# ---------------------------------------------------------------------------
# Evidence report (SRS §13.3): test ID, threats, expected, outcome, details
# ---------------------------------------------------------------------------
_docs: dict[str, str] = {}
_results: dict[str, tuple[str, str]] = {}


def pytest_collection_modifyitems(items: list[pytest.Item]) -> None:
    for item in items:
        func = getattr(item, "function", None)
        _docs[item.nodeid] = (func.__doc__ or "").strip() if func else ""


def pytest_runtest_logreport(report: pytest.TestReport) -> None:
    if report.when == "call" or (report.when == "setup" and report.outcome != "passed"):
        if report.outcome == "skipped" and isinstance(report.longrepr, tuple):
            detail = str(report.longrepr[2])
        elif report.failed:
            lines = report.longreprtext.strip().splitlines()
            detail = lines[-1] if lines else ""
        else:
            detail = ""
        outcome = report.outcome
        if hasattr(report, "wasxfail"):
            outcome = "xfail (known defect)" if report.skipped else "XPASS (defect fixed?)"
        _results[report.nodeid] = (outcome, detail)


def _git_commit() -> str:
    try:
        return subprocess.run(
            ["git", "rev-parse", "--short", "HEAD"], capture_output=True, text=True, check=True, timeout=10
        ).stdout.strip()
    except (OSError, subprocess.SubprocessError):
        return "unknown"


def pytest_sessionfinish(session: pytest.Session) -> None:
    target = session.config.getoption("--sec-report")
    if not target:
        return
    rows = []
    for nodeid, (outcome, detail) in _results.items():
        doc = _docs.get(nodeid, "")
        summary = doc.splitlines()[0] if doc else ""
        refs = next((line.split(":", 1)[1].strip() for line in doc.splitlines() if line.strip().startswith("Refs:")), "")
        expected = next(
            (line.split(":", 1)[1].strip() for line in doc.splitlines() if line.strip().startswith("Expected:")), ""
        )
        module_tag = Path(nodeid.split("::")[0]).stem.replace("test_", "").split("_")[0]  # e.g. "sec01"
        test_id = f"SEC-{module_tag[3:]}" if module_tag.startswith("sec") else module_tag.upper()
        name = nodeid.split("::")[-1]
        cell = lambda s: s.replace("|", "\\|").replace("\n", " ")[:220]  # noqa: E731
        rows.append(
            f"| {test_id} | `{name}` | {cell(summary)} | {cell(refs)} | {cell(expected)} | **{outcome}** | {cell(detail)} |"
        )
    counts: dict[str, int] = {}
    for outcome, _ in _results.values():
        counts[outcome] = counts.get(outcome, 0) + 1
    lines = [
        "# MiraNova Security Test Report (issue #57)",
        "",
        f"* **Run at (UTC):** {dt.datetime.now(dt.timezone.utc).strftime('%Y-%m-%d %H:%M:%S')}",
        f"* **Repository commit:** `{_git_commit()}`",
        f"* **Backend under test:** `{BASE_URL}` — {_backend_state.get('status', 'not contacted')}",
        "* **Totals:** " + ", ".join(f"{k}: {v}" for k, v in sorted(counts.items())),
        "",
        "Outcome meaning: **passed** = the attack was rejected as expected; **failed** = security defect "
        "(open a tracked issue before the demo); **skipped** = precondition missing (see details); "
        "**xfail** = known open defect.",
        "",
        "| Test | Case | What is attempted | Threats / requirements | Expected secure result | Outcome | Details |",
        "| :--- | :--- | :--- | :--- | :--- | :--- | :--- |",
        *rows,
        "",
    ]
    path = Path(target)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines), encoding="utf-8")
