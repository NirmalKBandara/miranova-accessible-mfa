"""SEC-07 — Enrollment hijack, proof of possession, and revoked/lost devices (AC-01)."""

import pytest

import sec_contract as contract
from sec_helpers import SoftwareDeviceKey, assert_denied

pytestmark = pytest.mark.backend


def _register_with(api, username, secret, account, key, pub_override=None):
    """Try to enroll `key` for `username`; return the first denying response or the register response."""
    resp = api.post("enrollment_challenge", contract.enrollment_challenge_body(username, secret, account))
    if resp.status_code >= 400:
        return resp
    challenge = contract.parse_challenge(resp.json())
    body = contract.register_device_body(
        challenge, pub_override or key.public_key_b64, key.sign(challenge.bytes_to_sign())
    )
    return api.post("register_device", body)


def test_attacker_cannot_enroll_device_for_victim_with_wrong_secret(api, enrolled):
    """Register an attacker key under an existing victim username with a guessed secret.
    Refs: TM-09, TH-09, AC-01, FR-06, FR-07, CTL-11, finding F-03
    Expected: refused; the victim's account gets no attacker-controlled device.
    """
    victim, _ = enrolled
    resp = _register_with(api, victim.username, "Guessed-Secret-1!", contract.AccountInfo(None), SoftwareDeviceKey())
    assert_denied(resp, "enrollment for victim with wrong secret")


def test_attacker_cannot_enroll_device_with_username_only(api, enrolled):
    """Register an attacker key with only the victim's username (what the current Android payload sends).
    Refs: TM-09, TH-09, finding F-03, F-06, CTL-11
    Expected: refused.
    """
    victim, _ = enrolled
    attacker = SoftwareDeviceKey()
    resp = api.post("enrollment_challenge", {"username": victim.username})
    if resp.status_code >= 400:
        assert_denied(resp, "enrollment challenge with username only")
        return
    challenge = contract.parse_challenge(resp.json())
    body = contract.register_device_body(challenge, attacker.public_key_b64, attacker.sign(challenge.bytes_to_sign()))
    body["username"] = victim.username
    assert_denied(api.post("register_device", body), "registration with username only")


def test_registration_requires_proof_of_possession(api, user):
    """Submit public key B with a signature made by key A.
    Refs: TM-06, TM-09, FR-07, FR-10, CTL-12
    Expected: refused; the server verifies the enrollment signature with the submitted key.
    """
    key_a, key_b = SoftwareDeviceKey(), SoftwareDeviceKey()
    resp = _register_with(api, user.username, user.secret, user.account, key_a, pub_override=key_b.public_key_b64)
    assert_denied(resp, "registration without proof of possession")


def test_enrollment_registration_cannot_be_replayed(api, user):
    """Resend a successful device registration request.
    Refs: TM-03, TM-09, FR-10, FR-21
    Expected: second registration with the same challenge is refused.
    """
    key = SoftwareDeviceKey()
    challenge = contract.parse_challenge(
        api.expect_ok(
            api.post("enrollment_challenge", contract.enrollment_challenge_body(user.username, user.secret, user.account)),
            "enrollment challenge",
        )
    )
    body = contract.register_device_body(challenge, key.public_key_b64, key.sign(challenge.bytes_to_sign()))
    api.expect_ok(api.post("register_device", body), "first registration")
    assert_denied(api.post("register_device", body), "replayed registration")


def test_revoked_device_cannot_authenticate(api, enrolled):
    """Revoke a device (simulated loss), then try to log in with it.
    Refs: TM-10, TM-11, TH-03, TH-09, TC-14, FR-12, CTL-15
    Expected: refused at device-challenge or device-verify. Skipped if no revocation endpoint exists.
    """
    user, device = enrolled
    token = api.login(user, device).token
    revoke = api.post("revoke_device", contract.revoke_device_body(device.device_id), token=token)
    assert 200 <= revoke.status_code < 300, f"revocation failed: {revoke.status_code} {revoke.text[:200]}"
    txn = api.start_and_verify_secret(user)
    resp = api.post("device_challenge", contract.device_challenge_body(txn, device.device_id))
    if resp.status_code >= 400:
        assert_denied(resp, "challenge for revoked device")
        return
    challenge = contract.parse_challenge(resp.json())
    challenge.transaction_id = challenge.transaction_id or txn
    body = contract.device_verify_body(challenge, device.device_id, device.key.sign(challenge.bytes_to_sign()))
    assert_denied(api.post("device_verify", body), "proof from revoked device")
