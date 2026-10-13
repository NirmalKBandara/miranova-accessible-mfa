"""SEC-06 — Session token forgery, tampering, expiry and reuse after logout (AC-07)."""

import time

import pytest

from sec_config import TEST_JWT_SECRET
from sec_helpers import is_jwt, json_or_empty, jwt_encode, jwt_hs256, jwt_parts

pytestmark = pytest.mark.backend

SENSITIVE_KEYS = {"secret", "password", "pin", "secret_hash", "password_hash", "hash", "private_key", "salt"}


def _assert_rejected(api, token, what):
    resp = api.get("me", token=token)
    assert resp.status_code in (401, 403), f"{what}: /v1/me returned {resp.status_code}: {resp.text[:200]}"


def _find_sensitive_keys(value, path=""):
    found = []
    if isinstance(value, dict):
        for key, inner in value.items():
            if str(key).lower() in SENSITIVE_KEYS:
                found.append(f"{path}.{key}")
            found += _find_sensitive_keys(inner, f"{path}.{key}")
    elif isinstance(value, list):
        for i, inner in enumerate(value):
            found += _find_sensitive_keys(inner, f"{path}[{i}]")
    return found


@pytest.mark.parametrize("token", ["garbage", "Bearer", "a.b.c", "eyJhbGciOiJIUzI1NiJ9.e30."])
def test_malformed_tokens_are_rejected(api, token):
    """Present malformed bearer tokens.
    Refs: TM-16, TC-07, FR-27, CTL-18
    Expected: 401/403.
    """
    _assert_rejected(api, token, f"malformed token {token!r}")


def test_tampered_token_is_rejected(api, enrolled):
    """Change the token's claims (JWT) or one character (opaque) and reuse it.
    Refs: TM-16, TC-07, FR-27, CTL-18
    Expected: 401/403; integrity is checked.
    """
    user, device = enrolled
    token = api.login(user, device).token
    if is_jwt(token):
        header, payload, signature = jwt_parts(token)
        payload["sub"] = "sec-attacker"
        payload["exp"] = int(time.time()) + 365 * 24 * 3600
        forged = jwt_encode(header, payload, signature)
    else:
        middle = len(token) // 2
        forged = token[:middle] + ("A" if token[middle] != "A" else "B") + token[middle + 1 :]
    _assert_rejected(api, forged, "tampered token")


def test_alg_none_token_is_rejected(api, enrolled):
    """Re-issue the real token claims with `alg: none` and no signature.
    Refs: TM-16, FR-27, RFC 8725 §3.1–3.2, CTL-18
    Expected: 401/403; the server pins its algorithm.
    """
    user, device = enrolled
    token = api.login(user, device).token
    if not is_jwt(token):
        pytest.skip("session token is not a JWT; alg=none attack not applicable")
    _, payload, _ = jwt_parts(token)
    _assert_rejected(api, jwt_encode({"alg": "none", "typ": "JWT"}, payload, ""), "alg=none token")


def test_expired_token_is_rejected(api, enrolled):
    """Sign the real claims with an `exp` in the past using the local test backend's key.
    Refs: TM-16, TM-17, TC-07, FR-27
    Expected: 401/403. Needs MIRANOVA_TEST_JWT_SECRET (local test key only).
    """
    if not TEST_JWT_SECRET:
        pytest.skip("MIRANOVA_TEST_JWT_SECRET not set; cannot mint a correctly signed expired token")
    user, device = enrolled
    token = api.login(user, device).token
    if not is_jwt(token):
        pytest.skip("session token is not a JWT")
    header, payload, _ = jwt_parts(token)
    if header.get("alg") != "HS256":
        # Minting with HS256 against an RS/ES backend would be rejected for the wrong reason.
        pytest.skip(f"backend signs with {header.get('alg')}; expired-token minting supports HS256 only")
    payload.update({"iat": int(time.time()) - 7200, "exp": int(time.time()) - 3600})
    _assert_rejected(api, jwt_hs256(payload, TEST_JWT_SECRET.encode()), "expired token")


def test_token_is_rejected_after_logout(api, enrolled):
    """Log out, then reuse the same bearer token.
    Refs: TM-17, TC-08, FR-29, CTL-19
    Expected: logout succeeds; the old token is then refused by /v1/me.
    """
    user, device = enrolled
    token = api.login(user, device).token
    assert api.get("me", token=token).status_code == 200
    logout = api.post("logout", {}, token=token)
    assert 200 <= logout.status_code < 300, f"logout failed: {logout.status_code} {logout.text[:200]}"
    _assert_rejected(api, token, "token reused after logout")


def test_protected_profile_has_no_secret_material(api, enrolled):
    """Inspect the /v1/me body for secret or hash fields.
    Refs: TM-29, TM-18, FR-30 (sanitised profile), NFR-S04
    Expected: no secret, hash, salt or private-key fields.
    """
    user, device = enrolled
    token = api.login(user, device).token
    body = json_or_empty(api.get("me", token=token))
    assert not _find_sensitive_keys(body), f"/v1/me exposes sensitive fields: {_find_sensitive_keys(body)}"
    assert user.secret not in str(body), "/v1/me echoes the user's secret"
