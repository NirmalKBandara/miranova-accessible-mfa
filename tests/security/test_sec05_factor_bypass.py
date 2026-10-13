"""SEC-05 — Skipping factors, out-of-order calls and forged client flags (AC-04)."""

import uuid

import pytest

import sec_contract as contract
from sec_helpers import assert_denied

pytestmark = pytest.mark.backend

FORGED_FLAGS = {
    "secret_verified": True,
    "knowledge_verified": True,
    "factors_passed": ["knowledge", "possession"],
    "mfa_complete": True,
    "state": "Authenticated",
}


def _start(api, user):
    body = api.expect_ok(api.post("auth_start", contract.auth_start_body(user.username)), "auth start")
    return contract.parse_transaction_id(body)


def _try_device_step(api, txn, device, extra=None):
    """Try to obtain and answer a device challenge; return the first denying response (or the verify response)."""
    req = contract.device_challenge_body(txn, device.device_id)
    req.update(extra or {})
    resp = api.post("device_challenge", req)
    if resp.status_code >= 400:
        return resp
    challenge = contract.parse_challenge(resp.json())
    challenge.transaction_id = challenge.transaction_id or txn
    body = contract.device_verify_body(challenge, device.device_id, device.key.sign(challenge.bytes_to_sign()))
    body.update(extra or {})
    return api.post("device_verify", body)


def test_device_factor_without_knowledge_factor_is_denied(api, enrolled):
    """Start a login and go straight to the device step without verify-secret.
    Refs: TM-07, TH-04, TH-03, FR-22, FR-23, CTL-09
    Expected: refused at device-challenge or device-verify; no session (device alone is insufficient).
    """
    user, device = enrolled
    txn = _start(api, user)
    assert_denied(_try_device_step(api, txn, device), "device factor without secret")


def test_device_factor_after_wrong_secret_is_denied(api, enrolled):
    """Submit a wrong secret, then a valid device proof in the same transaction.
    Refs: TC-04, TM-07, FR-22
    Expected: no session.
    """
    user, device = enrolled
    txn = _start(api, user)
    api.post("verify_secret", contract.verify_secret_body(txn, user.secret + "-wrong"))
    assert_denied(_try_device_step(api, txn, device), "device factor after wrong secret")


def test_forged_factor_flags_are_ignored(api, enrolled):
    """Add client-side 'factor passed' flags to skip the knowledge factor.
    Refs: TM-07, TH-04, FR-02, ASM-THR-02, CTL-09
    Expected: flags are ignored or rejected; no session.
    """
    user, device = enrolled
    txn = _start(api, user)
    assert_denied(_try_device_step(api, txn, device, extra=FORGED_FLAGS), "forged factor flags")


def test_device_verify_before_challenge_is_denied(api, enrolled):
    """Call device-verify with a made-up challenge before any challenge was issued.
    Refs: TM-07, FR-18, FR-23
    Expected: denied with a 4xx.
    """
    user, device = enrolled
    txn = api.start_and_verify_secret(user)
    fake = contract.ChallengeInfo(transaction_id=txn, challenge_id=str(uuid.uuid4()), nonce="AAAA", expires_at=None)
    body = contract.device_verify_body(fake, device.device_id, device.key.sign(fake.bytes_to_sign()))
    assert_denied(api.post("device_verify", body), "device-verify before device-challenge")
