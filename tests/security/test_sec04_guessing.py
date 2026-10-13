"""SEC-04 — Knowledge-factor guessing, enumeration and error leakage."""

import uuid

import pytest

import sec_contract as contract
from sec_config import MAX_FAILED_ATTEMPTS
from sec_helpers import assert_denied, assert_no_internal_details, error_shape, json_or_empty, response_has_token

pytestmark = pytest.mark.backend


def _attempt(api, username, secret):
    """Run auth/start + verify-secret; return (step, response) of the step that decided the outcome."""
    start = api.post("auth_start", contract.auth_start_body(username))
    if start.status_code >= 400:
        return "auth_start", start
    txn = contract.parse_transaction_id(json_or_empty(start)) or ""
    return "verify_secret", api.post("verify_secret", contract.verify_secret_body(txn, secret))


def test_wrong_secret_is_denied_without_details(api, enrolled):
    """Submit a wrong knowledge secret for a real account.
    Refs: TM-01, TM-20, TC-04, FR-13, FR-16, FR-44
    Expected: 4xx; no token; no stack traces or hash material in the body.
    """
    user, _ = enrolled
    step, resp = _attempt(api, user.username, user.secret + "-wrong")
    assert_denied(resp, f"wrong secret at {step}")
    assert_no_internal_details(resp, "wrong secret")


def test_unknown_account_and_wrong_secret_are_indistinguishable(api, enrolled):
    """Compare responses for a non-existent account and a wrong secret on a real account.
    Refs: TM-02, TH-10, FR-16, CTL-04
    Expected: failure happens at the same step with the same status and the same error body
    (ignoring request IDs), so account existence is not revealed.
    """
    user, _ = enrolled
    real_step, real = _attempt(api, user.username, "Wrong-Secret-123!")
    fake_step, fake = _attempt(api, f"sec-nobody-{uuid.uuid4().hex[:10]}", "Wrong-Secret-123!")
    assert real_step == fake_step, (
        f"enumeration: unknown account fails at '{fake_step}' but real account fails at '{real_step}'"
    )
    assert error_shape(real) == error_shape(fake), (
        f"enumeration: responses differ. real={error_shape(real)} unknown={error_shape(fake)}"
    )


def test_verify_secret_alone_never_issues_a_session(api, enrolled):
    """A correct secret only advances the transaction; it must not return an access token.
    Refs: TM-07, FR-26, SRS §10.3 (verify-secret: no access session)
    Expected: 2xx progression response without any token field.
    """
    user, _ = enrolled
    start = api.expect_ok(api.post("auth_start", contract.auth_start_body(user.username)), "auth start")
    txn = contract.parse_transaction_id(start)
    resp = api.post("verify_secret", contract.verify_secret_body(txn, user.secret))
    assert 200 <= resp.status_code < 300, f"correct secret rejected: {resp.status_code}"
    assert not response_has_token(resp), "verify-secret issued a token before the device factor"


@pytest.mark.ratelimit
def test_repeated_wrong_secrets_are_throttled(api, enrolled):
    """Repeat wrong-secret attempts against one account until throttling should start.
    Refs: TM-01, TM-25, TH-01, TH-10, FR-17, NFR-S05, CTL-02
    Expected: within MIRANOVA_MAX_FAILED_ATTEMPTS (+5 margin) the server answers 429/423 or a
    rate-limit error code; no attempt ever yields a token.
    """
    user, _ = enrolled
    throttled_at = None
    for attempt in range(1, MAX_FAILED_ATTEMPTS + 6):
        _, resp = _attempt(api, user.username, f"guess-{attempt}")
        assert not response_has_token(resp), f"attempt {attempt} produced a token"
        assert resp.status_code < 500, f"attempt {attempt}: server error {resp.status_code}"
        body_text = resp.text.upper()
        if resp.status_code in (423, 429) or "RATE_LIMIT" in body_text or "LOCKED" in body_text:
            throttled_at = attempt
            break
    assert throttled_at is not None, (
        f"no throttling observed after {MAX_FAILED_ATTEMPTS + 5} wrong secrets (FR-17). "
        "Set MIRANOVA_MAX_FAILED_ATTEMPTS to the documented policy if it is higher."
    )
