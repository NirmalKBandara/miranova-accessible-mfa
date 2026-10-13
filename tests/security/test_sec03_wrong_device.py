"""SEC-03 — Unregistered, foreign, or substituted device keys (AC-05)."""

import uuid

import pytest

import sec_contract as contract
from sec_helpers import SoftwareDeviceKey, assert_denied

pytestmark = pytest.mark.backend


def test_unregistered_key_cannot_sign_for_registered_device(api, enrolled):
    """An attacker key signs the real challenge and claims the victim's device ID.
    Refs: TM-06, TH-05, TC-03, FR-20, CTL-08
    Expected: denied; verification uses the stored public key only.
    """
    user, device = enrolled
    attacker = SoftwareDeviceKey()
    txn = api.start_and_verify_secret(user)
    challenge = api.device_challenge(txn, device.device_id)
    body = contract.device_verify_body(challenge, device.device_id, attacker.sign(challenge.bytes_to_sign()))
    assert_denied(api.post("device_verify", body), "attacker key for victim device")


def test_client_supplied_public_key_is_ignored(api, enrolled):
    """Add the attacker's public key (and signed message) to the proof, as SignedChallengeResponse does.
    Refs: TM-06, finding F-02, FR-20, CTL-08
    Expected: denied; extra key/message fields must not be used for verification.
    """
    user, device = enrolled
    attacker = SoftwareDeviceKey()
    txn = api.start_and_verify_secret(user)
    challenge = api.device_challenge(txn, device.device_id)
    message = challenge.bytes_to_sign()
    body = contract.device_verify_body(challenge, device.device_id, attacker.sign(message))
    body.update(
        {
            "public_key": attacker.public_key_b64,
            "publicKeyBase64": attacker.public_key_b64,
            "canonical_message": message.decode("utf-8", "replace"),
        }
    )
    assert_denied(api.post("device_verify", body), "proof with substituted public key")


def test_other_users_device_cannot_complete_victims_login(api, enrolled, other_enrolled):
    """Use another account's registered device (and its key) inside the victim's transaction.
    Refs: TM-05, TM-06, TH-05, FR-20, FR-23
    Expected: refused at device-challenge or device-verify; never a session.
    """
    victim, _ = enrolled
    _, other_device = other_enrolled
    txn = api.start_and_verify_secret(victim)
    resp = api.post("device_challenge", contract.device_challenge_body(txn, other_device.device_id))
    if resp.status_code >= 400:
        assert_denied(resp, "device challenge for another account's device")
        return
    challenge = contract.parse_challenge(resp.json())
    challenge.transaction_id = challenge.transaction_id or txn
    body = contract.device_verify_body(
        challenge, other_device.device_id, other_device.key.sign(challenge.bytes_to_sign())
    )
    assert_denied(api.post("device_verify", body), "another account's device completing victim login")


def test_unknown_device_id_is_rejected(api, enrolled):
    """Request a device challenge for a device ID that was never registered.
    Refs: TM-06, FR-20
    Expected: denied with a 4xx at device-challenge (or at device-verify).
    """
    user, _ = enrolled
    txn = api.start_and_verify_secret(user)
    fake_device = str(uuid.uuid4())
    resp = api.post("device_challenge", contract.device_challenge_body(txn, fake_device))
    if resp.status_code >= 400:
        assert_denied(resp, "challenge for unknown device")
        return
    challenge = contract.parse_challenge(resp.json())
    challenge.transaction_id = challenge.transaction_id or txn
    body = contract.device_verify_body(challenge, fake_device, SoftwareDeviceKey().sign(challenge.bytes_to_sign()))
    assert_denied(api.post("device_verify", body), "proof for unknown device")
