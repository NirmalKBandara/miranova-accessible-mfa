"""SEC-01 — Replay of a previously accepted signed device proof (AC-02)."""

import pytest

from sec_helpers import assert_denied

pytestmark = pytest.mark.backend


def test_replayed_device_proof_is_rejected(api, enrolled):
    """Resend the exact device-verify request that already produced a session.
    Refs: TM-03, TH-02, TC-05, FR-21, NFR-S02, CTL-05
    Expected: second submission denied with a 4xx; no new session.
    """
    user, device = enrolled
    first = api.login(user, device)
    replay = api.post("device_verify", first.verify_body)
    assert_denied(replay, "replay of accepted proof")


def test_replayed_proof_is_rejected_while_new_login_is_open(api, enrolled):
    """Replay an old accepted proof while the same user has a fresh login in progress.
    Refs: TM-03, TM-05, TH-02, FR-21, FR-23
    Expected: denied; the old challenge stays consumed even when a new transaction exists.
    """
    user, device = enrolled
    first = api.login(user, device)
    txn = api.start_and_verify_secret(user)
    api.device_challenge(txn, device.device_id)
    replay = api.post("device_verify", first.verify_body)
    assert_denied(replay, "replay during a new open transaction")
