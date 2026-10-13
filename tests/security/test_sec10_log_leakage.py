"""SEC-10 — Backend logs record security events without secrets (TC-12, FR-42)."""

from pathlib import Path

import pytest

import sec_contract as contract
from sec_config import LOG_PATH
from sec_helpers import SoftwareDeviceKey, json_or_empty

pytestmark = pytest.mark.backend


@pytest.fixture
def log_file():
    if not LOG_PATH:
        pytest.skip("MIRANOVA_LOG_PATH not set; point it at the backend's log file to run log checks")
    path = Path(LOG_PATH)
    if not path.is_file():
        pytest.skip(f"log file {path} does not exist")
    return path


def _read_from(path: Path, offset: int) -> str:
    with path.open("rb") as handle:
        handle.seek(offset)
        return handle.read().decode("utf-8", "replace")


def test_logs_do_not_contain_secrets_tokens_or_signatures(api, enrolled, log_file):
    """Run a successful and a failed login, then scan new log lines for sensitive values.
    Refs: TM-19, TM-17, TC-12, FR-42, NFR-S04, CTL-20
    Expected: none of the secret, wrong secret, bearer token, signature or PEM markers appear.
    """
    user, device = enrolled
    offset = log_file.stat().st_size
    result = api.login(user, device)
    wrong_secret = "Wrong-" + user.secret
    start = api.post("auth_start", contract.auth_start_body(user.username))
    if start.status_code < 400:
        txn = contract.parse_transaction_id(json_or_empty(start)) or ""
        api.post("verify_secret", contract.verify_secret_body(txn, wrong_secret))
    api.get("me", token=result.token)
    new_lines = _read_from(log_file, offset)

    leaks = {
        "secret": user.secret,
        "wrong secret": wrong_secret,
        "bearer token": result.token,
        "device signature": result.verify_body["signature"],
        "PEM private key marker": "PRIVATE KEY-----",
    }
    found = [name for name, value in leaks.items() if value and value in new_lines]
    assert not found, f"backend log contains: {found}"


def test_authentication_events_are_logged(api, enrolled, log_file):
    """A login attempt produces at least one new log record (detective control).
    Refs: TM-26, FR-24, FR-42, CTL-26
    Expected: the log grows after a failed and a successful login.
    """
    user, device = enrolled
    offset = log_file.stat().st_size
    api.post("auth_start", contract.auth_start_body(user.username))
    api.login(user, device)
    assert _read_from(log_file, offset).strip(), "no log records written for authentication events"


def test_enrollment_does_not_log_private_key_material(api, enrolled, log_file):
    """Enroll another device key and scan the new log lines for private-key blocks.
    Refs: TM-13, TM-19, FR-08, FR-42
    Expected: no PEM private-key block appears in new log lines.
    """
    user, _ = enrolled
    offset = log_file.stat().st_size
    api.enroll(user, SoftwareDeviceKey())
    assert "PRIVATE KEY" not in _read_from(log_file, offset)
