"""Single place that maps the security tests onto the backend HTTP contract.

The tests in this folder are black-box: they only use HTTP requests that any
client (or attacker, AT-01/AT-06 in docs/08-threat-model.md) could send.

The API contract is not frozen yet (docs/07-api-contract.md, issues #37/#45).
The defaults below follow the *proposed* contract in SRS §10.3. When the
backend lands, adjust ONLY this file: endpoint paths, request field names and
response field names. The test logic itself should not need to change.
"""

from __future__ import annotations

import base64
from dataclasses import dataclass, field
from typing import Any, Optional

# ---------------------------------------------------------------------------
# Endpoints (SRS §10.3). Set an entry to None if the backend does not offer it;
# tests that need it are then skipped with a clear reason.
# ---------------------------------------------------------------------------
ENDPOINTS: dict[str, Optional[str]] = {
    "health": "/health",
    "create_account": "/v1/accounts",
    "enrollment_challenge": "/v1/devices/enrollment-challenge",
    "register_device": "/v1/devices/register",
    "auth_start": "/v1/auth/start",
    "verify_secret": "/v1/auth/verify-secret",
    "device_challenge": "/v1/auth/device-challenge",
    "device_verify": "/v1/auth/device-verify",
    "me": "/v1/me",
    "logout": "/v1/auth/logout",
    # Conditional in SRS §10.3 (depends on DEC-08 / ADR). None = not offered.
    "revoke_device": None,
}

# Signature / key encoding used by the Android client (ChallengeSigner.kt,
# Base64Util.kt): SHA256withECDSA (ASN.1 DER signature), X.509
# SubjectPublicKeyInfo public key, standard Base64.
ALGORITHM = "ES256"

# Fallback signing format = what the Android client signs today
# (CanonicalChallenge.kt). Used only if the server does not return the exact
# bytes to sign in `signing_payload` (recommended fix for finding F-01).
FALLBACK_ORIGIN = "https://mfa.miranova.internal"
FALLBACK_ACTION = "authenticate"
FALLBACK_PREFIX = "miranova:auth:v1"

# Response fields that would indicate a usable session was issued.
TOKEN_FIELDS = ("access_token", "token", "session_token", "refresh_token")


def b64e(data: bytes) -> str:
    return base64.b64encode(data).decode("ascii")


def b64d(text: str) -> bytes:
    """Decode standard or URL-safe Base64, with or without padding."""
    text = text.strip()
    padded = text + "=" * (-len(text) % 4)
    if "-" in text or "_" in text:
        return base64.urlsafe_b64decode(padded)
    return base64.b64decode(padded)


# ---------------------------------------------------------------------------
# Request builders
# ---------------------------------------------------------------------------
def create_account_body(username: str, secret: str) -> dict[str, Any]:
    return {"username": username, "secret": secret}


def enrollment_challenge_body(username: str, secret: str, account: "AccountInfo") -> dict[str, Any]:
    body: dict[str, Any] = {"username": username, "secret": secret}
    if account.enrollment_token:
        body["enrollment_token"] = account.enrollment_token
    return body


def register_device_body(challenge: "ChallengeInfo", public_key_b64: str, signature_b64: str) -> dict[str, Any]:
    return {
        "transaction_id": challenge.transaction_id,
        "challenge_id": challenge.challenge_id,
        "public_key": public_key_b64,
        "algorithm": ALGORITHM,
        "signature": signature_b64,
    }


def auth_start_body(username: str) -> dict[str, Any]:
    return {"username": username}


def verify_secret_body(transaction_id: str, secret: str) -> dict[str, Any]:
    return {"transaction_id": transaction_id, "secret": secret}


def device_challenge_body(transaction_id: str, device_id: str) -> dict[str, Any]:
    return {"transaction_id": transaction_id, "device_id": device_id}


def device_verify_body(challenge: "ChallengeInfo", device_id: str, signature_b64: str) -> dict[str, Any]:
    # Deliberately does NOT include a public key or the signed message:
    # the server must use its own stored records (finding F-02).
    return {
        "transaction_id": challenge.transaction_id,
        "challenge_id": challenge.challenge_id,
        "device_id": device_id,
        "signature": signature_b64,
    }


def revoke_device_body(device_id: str) -> dict[str, Any]:
    return {"device_id": device_id}


# ---------------------------------------------------------------------------
# Response parsing
# ---------------------------------------------------------------------------
@dataclass
class AccountInfo:
    account_id: Optional[str]
    enrollment_token: Optional[str] = None


@dataclass
class ChallengeInfo:
    transaction_id: Optional[str]
    challenge_id: Optional[str]
    nonce: str
    expires_at: Optional[str]
    signing_payload: Optional[bytes] = None
    raw: dict[str, Any] = field(default_factory=dict)

    def bytes_to_sign(self, timestamp: Optional[int] = None) -> bytes:
        """Exact bytes the device must sign for this challenge."""
        if self.signing_payload is not None:
            return self.signing_payload
        ts = timestamp if timestamp is not None else int(self.raw.get("timestamp", 0) or 0)
        return f"{FALLBACK_PREFIX}:{self.nonce}:{ts}:{FALLBACK_ORIGIN}:{FALLBACK_ACTION}".encode("utf-8")


def _first(data: dict[str, Any], *names: str) -> Any:
    for name in names:
        if name in data and data[name] not in (None, ""):
            return data[name]
    return None


def parse_account(data: dict[str, Any]) -> AccountInfo:
    return AccountInfo(
        account_id=_first(data, "account_id", "accountId", "user_id", "userId", "id"),
        enrollment_token=_first(data, "enrollment_token", "enrollmentToken"),
    )


def parse_challenge(data: dict[str, Any]) -> ChallengeInfo:
    payload = _first(data, "signing_payload", "signingPayload")
    return ChallengeInfo(
        transaction_id=_first(data, "transaction_id", "transactionId", "enrollment_transaction_id"),
        challenge_id=_first(data, "challenge_id", "challengeId"),
        nonce=str(_first(data, "nonce", "challenge") or ""),
        expires_at=_first(data, "expires_at", "expiresAt"),
        signing_payload=b64d(payload) if payload else None,
        raw=data,
    )


def parse_transaction_id(data: dict[str, Any]) -> Optional[str]:
    return _first(data, "transaction_id", "transactionId", "auth_transaction_id")


def parse_device_id(data: dict[str, Any]) -> Optional[str]:
    return _first(data, "device_id", "deviceId", "id")


def parse_token(data: dict[str, Any]) -> Optional[str]:
    return _first(data, *TOKEN_FIELDS)
