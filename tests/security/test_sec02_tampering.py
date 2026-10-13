"""SEC-02 — Tampering with nonce / transaction / challenge / signature fields (AC-06)."""

import uuid

import pytest

import sec_contract as contract
from sec_helpers import assert_denied, flip_signature

pytestmark = pytest.mark.backend


def test_modified_signature_is_rejected(api, enrolled):
    """Flip one bit of an otherwise valid signature.
    Refs: TM-05, TH-05, FR-20, CTL-08
    Expected: denied; no session.
    """
    user, device = enrolled
    _, body = api.signed_proof(user, device)
    body = dict(body, signature=flip_signature(body["signature"]))
    assert_denied(api.post("device_verify", body), "bit-flipped signature")


def test_signature_over_different_nonce_is_rejected(api, enrolled):
    """Sign a message containing a different nonce than the one the server issued.
    Refs: TM-05, TH-05, FR-19, CTL-07
    Expected: denied; the server rebuilds the signed bytes from its own record.
    """
    user, device = enrolled
    txn = api.start_and_verify_secret(user)
    challenge = api.device_challenge(txn, device.device_id)
    original = challenge.bytes_to_sign()
    forged_bytes = original.replace(challenge.nonce.encode(), b"attacker-chosen-nonce") if challenge.nonce else original
    if forged_bytes == original:  # nonce not visible in the payload: change the bytes another way
        forged_bytes = original + b"|attacker-chosen-nonce"
    body = contract.device_verify_body(challenge, device.device_id, device.key.sign(forged_bytes))
    assert_denied(api.post("device_verify", body), "signature over attacker-chosen nonce")


def test_proof_moved_to_another_transaction_is_rejected(api, enrolled):
    """Sign transaction A's challenge, then submit it under transaction B's IDs.
    Refs: TM-05, TH-05, AC-06, FR-19, FR-23, CTL-07
    Expected: denied; signatures are bound to one transaction and challenge.
    """
    user, device = enrolled
    txn_a = api.start_and_verify_secret(user)
    challenge_a = api.device_challenge(txn_a, device.device_id)
    txn_b = api.start_and_verify_secret(user)
    challenge_b = api.device_challenge(txn_b, device.device_id)
    signature_for_a = device.key.sign(challenge_a.bytes_to_sign())
    body = contract.device_verify_body(challenge_b, device.device_id, signature_for_a)
    assert_denied(api.post("device_verify", body), "proof for transaction A submitted as B")


def test_unknown_transaction_id_is_rejected(api, enrolled):
    """Replace the transaction ID with a random value.
    Refs: TM-05, TM-07, FR-23
    Expected: denied with a 4xx (not a server error).
    """
    user, device = enrolled
    _, body = api.signed_proof(user, device)
    body = dict(body, transaction_id=str(uuid.uuid4()))
    assert_denied(api.post("device_verify", body), "random transaction id")


def test_unknown_challenge_id_is_rejected(api, enrolled):
    """Replace the challenge ID with a random value.
    Refs: TM-05, FR-18, FR-23
    Expected: denied with a 4xx.
    """
    user, device = enrolled
    _, body = api.signed_proof(user, device)
    body = dict(body, challenge_id=str(uuid.uuid4()))
    assert_denied(api.post("device_verify", body), "random challenge id")


@pytest.mark.parametrize(
    "signature",
    ["", "not-base64!!", "AAAA", 12345, None],
    ids=["empty", "non-base64", "too-short", "wrong-type-int", "null"],
)
def test_malformed_signature_fails_closed_without_server_error(api, enrolled, signature):
    """Send malformed signature values.
    Refs: TM-20, FR-44, NFR-S03, NFR-S04
    Expected: controlled 4xx (not 5xx); no session; no internal details.
    """
    user, device = enrolled
    _, body = api.signed_proof(user, device)
    body = dict(body, signature=signature)
    assert_denied(api.post("device_verify", body), f"malformed signature {signature!r}")
