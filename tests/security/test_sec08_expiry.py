"""SEC-08 — Expired challenges are rejected using server time."""

import datetime as dt
import time

import pytest

import sec_contract as contract
from sec_config import MAX_EXPIRY_WAIT
from sec_helpers import assert_denied

pytestmark = [pytest.mark.backend, pytest.mark.slow]


def _parse_time(value):
    if value is None:
        return None
    if isinstance(value, (int, float)):
        return dt.datetime.fromtimestamp(value / 1000 if value > 1e12 else value, dt.timezone.utc)
    text = str(value).replace("Z", "+00:00")
    parsed = dt.datetime.fromisoformat(text)
    return parsed if parsed.tzinfo else parsed.replace(tzinfo=dt.timezone.utc)


def test_device_challenge_states_an_expiry(api, enrolled):
    """Every device challenge carries a server-defined expiry.
    Refs: TM-27, FR-18, CTL-06
    Expected: `expires_at` present and in the future.
    """
    user, device = enrolled
    txn = api.start_and_verify_secret(user)
    challenge = api.device_challenge(txn, device.device_id)
    expires = _parse_time(challenge.expires_at)
    assert expires is not None, f"device challenge has no expiry: {challenge.raw}"
    assert expires > dt.datetime.now(dt.timezone.utc) - dt.timedelta(seconds=5), "challenge already expired when issued"


def test_expired_device_challenge_is_rejected(api, enrolled):
    """Wait until the challenge expires, then submit a valid signature.
    Refs: TM-27, TC-06, FR-18, FR-23, CTL-06
    Expected: denied. Skipped if the expiry is longer than MIRANOVA_MAX_EXPIRY_WAIT seconds.
    """
    user, device = enrolled
    txn = api.start_and_verify_secret(user)
    challenge = api.device_challenge(txn, device.device_id)
    expires = _parse_time(challenge.expires_at)
    if expires is None:
        pytest.fail("device challenge has no expiry (FR-18)")
    wait = (expires - dt.datetime.now(dt.timezone.utc)).total_seconds() + 2
    if wait > MAX_EXPIRY_WAIT:
        pytest.skip(f"challenge TTL {wait:.0f}s exceeds MIRANOVA_MAX_EXPIRY_WAIT={MAX_EXPIRY_WAIT}s")
    signature = device.key.sign(challenge.bytes_to_sign())
    time.sleep(max(wait, 0))
    body = contract.device_verify_body(challenge, device.device_id, signature)
    assert_denied(api.post("device_verify", body), "expired challenge")
