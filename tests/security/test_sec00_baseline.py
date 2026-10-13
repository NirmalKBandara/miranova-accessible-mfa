"""SEC-00 — Baseline: the legitimate flow must work before negative results mean anything.

A negative test that "passes" against a broken backend proves nothing. These
checks confirm the positive path (SRS TC-02) so that every denial observed in
SEC-01…SEC-10 is attributable to a security control, not to a broken flow.
"""

import pytest

from sec_helpers import assert_no_internal_details

pytestmark = pytest.mark.backend


def test_health_endpoint_is_non_sensitive(api):
    """GET /health returns 200 without internal details.
    Refs: FR-04, FR-44
    Expected: HTTP 200; no stack traces, DB driver names or hashes in the body.
    """
    resp = api.get("health")
    assert resp.status_code == 200
    assert_no_internal_details(resp, "health")


def test_full_mfa_flow_reaches_protected_resource(api, enrolled):
    """Correct secret + valid fresh device proof gives a session that opens /v1/me.
    Refs: TC-02, FR-22, FR-26, FR-30
    Expected: device-verify returns a token; GET /v1/me with it returns 200.
    """
    user, device = enrolled
    result = api.login(user, device)
    me = api.get("me", token=result.token)
    assert me.status_code == 200, f"/v1/me with a fresh session returned {me.status_code}: {me.text[:200]}"


def test_protected_resource_requires_session(api):
    """GET /v1/me without any credentials is refused.
    Refs: TC-07, FR-30, TM-07
    Expected: HTTP 401 (or 403); no profile data.
    """
    resp = api.get("me")
    assert resp.status_code in (401, 403), f"unauthenticated /v1/me returned {resp.status_code}"
