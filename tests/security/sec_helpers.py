"""Shared helpers for the MiraNova adversarial security tests (issue #57)."""

from __future__ import annotations

import base64
import hashlib
import hmac
import json
import secrets
import uuid
from dataclasses import dataclass
from typing import Any, Optional

import httpx
import pytest
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec

import sec_contract as contract


# ---------------------------------------------------------------------------
# Software stand-in for the Android Keystore device key
# ---------------------------------------------------------------------------
class SoftwareDeviceKey:
    """ECDSA P-256 key that signs exactly like AndroidKeyStoreManager.sign().

    Android uses SHA256withECDSA, which produces an ASN.1 DER signature, and
    exports the public key as X.509 SubjectPublicKeyInfo (DER, Base64). This
    key lives in test memory only, so a test can act as a legitimate device or
    as an attacker device. It is never written to disk.
    """

    def __init__(self) -> None:
        self._private_key = ec.generate_private_key(ec.SECP256R1())

    @property
    def public_key_b64(self) -> str:
        der = self._private_key.public_key().public_bytes(
            serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo
        )
        return contract.b64e(der)

    def sign(self, message: bytes) -> str:
        return contract.b64e(self._private_key.sign(message, ec.ECDSA(hashes.SHA256())))


def flip_signature(signature_b64: str) -> str:
    """Return a structurally valid Base64 signature with one bit changed."""
    raw = bytearray(contract.b64d(signature_b64))
    raw[-1] ^= 0x01
    return contract.b64e(bytes(raw))


# ---------------------------------------------------------------------------
# Data holders
# ---------------------------------------------------------------------------
@dataclass
class User:
    username: str
    secret: str
    account: contract.AccountInfo


@dataclass
class Device:
    device_id: str
    key: SoftwareDeviceKey


@dataclass
class LoginResult:
    token: str
    verify_body: dict[str, Any]
    challenge: contract.ChallengeInfo
    response: httpx.Response


# ---------------------------------------------------------------------------
# Assertions
# ---------------------------------------------------------------------------
def json_or_empty(resp: httpx.Response) -> Any:
    try:
        return resp.json()
    except ValueError:
        return {}


def response_has_token(resp: httpx.Response) -> bool:
    body = json_or_empty(resp)
    if isinstance(body, dict) and contract.parse_token(body):
        return True
    cookie = resp.headers.get("set-cookie", "").lower()
    return "session" in cookie or "token" in cookie


def assert_denied(resp: httpx.Response, what: str) -> None:
    """The server must refuse with a controlled 4xx and must not issue a session."""
    assert resp.status_code < 500, (
        f"{what}: server returned HTTP {resp.status_code} instead of a controlled 4xx denial "
        f"(fail-closed but unhandled error; see FR-44/NFR-S03). Body: {resp.text[:300]}"
    )
    assert resp.status_code >= 400, (
        f"{what}: expected denial but got HTTP {resp.status_code}. Body: {resp.text[:300]}"
    )
    assert not response_has_token(resp), f"{what}: a session/token was issued in a denied response"


LEAK_MARKERS = (
    "Traceback (most recent call last)",
    'File "',
    "sqlalchemy",
    "psycopg",
    "sqlite3.",
    "BEGIN PRIVATE KEY",
    "secret_hash",
    "password_hash",
    "$2b$",  # bcrypt hash prefix
    "$argon2",
)


def assert_no_internal_details(resp: httpx.Response, what: str) -> None:
    text = resp.text
    for marker in LEAK_MARKERS:
        assert marker not in text, f"{what}: response leaks internal detail {marker!r} (FR-44, NFR-S04)"


def error_shape(resp: httpx.Response) -> tuple[int, Any]:
    """Status code plus error body without per-request fields, for comparing two errors."""
    body = json_or_empty(resp)

    def strip(value: Any) -> Any:
        if isinstance(value, dict):
            return {
                k: strip(v)
                for k, v in value.items()
                if k not in ("request_id", "requestId", "timestamp", "trace_id", "correlation_id")
            }
        if isinstance(value, list):
            return [strip(v) for v in value]
        return value

    return resp.status_code, strip(body)


# ---------------------------------------------------------------------------
# JWT helpers (no external JWT library, so that malformed tokens can be built)
# ---------------------------------------------------------------------------
def _b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def _b64url_decode(text: str) -> bytes:
    return base64.urlsafe_b64decode(text + "=" * (-len(text) % 4))


def is_jwt(token: str) -> bool:
    parts = token.split(".")
    if len(parts) != 3:
        return False
    try:
        json.loads(_b64url_decode(parts[0]))
        json.loads(_b64url_decode(parts[1]))
        return True
    except (ValueError, json.JSONDecodeError):
        return False


def jwt_parts(token: str) -> tuple[dict[str, Any], dict[str, Any], str]:
    header_b64, payload_b64, signature = token.split(".")
    return json.loads(_b64url_decode(header_b64)), json.loads(_b64url_decode(payload_b64)), signature


def jwt_encode(header: dict[str, Any], payload: dict[str, Any], signature: str) -> str:
    return ".".join(
        [
            _b64url(json.dumps(header, separators=(",", ":")).encode()),
            _b64url(json.dumps(payload, separators=(",", ":")).encode()),
            signature,
        ]
    )


def jwt_hs256(payload: dict[str, Any], key: bytes) -> str:
    header = {"alg": "HS256", "typ": "JWT"}
    signing_input = ".".join(
        [
            _b64url(json.dumps(header, separators=(",", ":")).encode()),
            _b64url(json.dumps(payload, separators=(",", ":")).encode()),
        ]
    )
    sig = hmac.new(key, signing_input.encode(), hashlib.sha256).digest()
    return f"{signing_input}.{_b64url(sig)}"


# ---------------------------------------------------------------------------
# API client with the legitimate end-to-end flow (SRS UC-01, UC-02)
# ---------------------------------------------------------------------------
class SecApi:
    def __init__(self, base_url: str, timeout: float) -> None:
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout
        self.client = httpx.Client(base_url=self.base_url, timeout=timeout)

    def close(self) -> None:
        self.client.close()

    # -- raw calls ---------------------------------------------------------
    @staticmethod
    def path(name: str) -> str:
        path = contract.ENDPOINTS.get(name)
        if not path:
            pytest.skip(f"endpoint '{name}' is not offered by the configured contract (sec_contract.py)")
        return path

    def post(self, name: str, body: dict[str, Any], token: Optional[str] = None) -> httpx.Response:
        headers = {"Authorization": f"Bearer {token}"} if token else {}
        return self.client.post(self.path(name), json=body, headers=headers)

    def get(self, name: str, token: Optional[str] = None) -> httpx.Response:
        headers = {"Authorization": f"Bearer {token}"} if token else {}
        return self.client.get(self.path(name), headers=headers)

    @staticmethod
    def expect_ok(resp: httpx.Response, step: str) -> dict[str, Any]:
        if not 200 <= resp.status_code < 300:
            pytest.fail(
                f"setup step '{step}' failed with HTTP {resp.status_code}: {resp.text[:300]}. "
                "The legitimate flow must work before negative tests are meaningful; "
                "check sec_contract.py against docs/07-api-contract.md."
            )
        body = json_or_empty(resp)
        return body if isinstance(body, dict) else {}

    # -- legitimate flow ---------------------------------------------------
    def create_user(self, label: str = "user") -> User:
        username = f"sec-{label}-{uuid.uuid4().hex[:10]}"
        secret = "Sx!" + secrets.token_urlsafe(18)
        body = self.expect_ok(
            self.post("create_account", contract.create_account_body(username, secret)), "create account"
        )
        return User(username=username, secret=secret, account=contract.parse_account(body))

    def enroll(self, user: User, key: Optional[SoftwareDeviceKey] = None) -> Device:
        key = key or SoftwareDeviceKey()
        challenge_body = self.expect_ok(
            self.post(
                "enrollment_challenge",
                contract.enrollment_challenge_body(user.username, user.secret, user.account),
            ),
            "enrollment challenge",
        )
        challenge = contract.parse_challenge(challenge_body)
        signature = key.sign(challenge.bytes_to_sign())
        reg = self.expect_ok(
            self.post("register_device", contract.register_device_body(challenge, key.public_key_b64, signature)),
            "register device",
        )
        device_id = contract.parse_device_id(reg)
        if not device_id:
            pytest.fail(f"register device response has no device id: {reg}")
        return Device(device_id=str(device_id), key=key)

    def start_and_verify_secret(self, user: User, secret: Optional[str] = None) -> str:
        start = self.expect_ok(self.post("auth_start", contract.auth_start_body(user.username)), "auth start")
        txn = contract.parse_transaction_id(start)
        if not txn:
            pytest.fail(f"auth start response has no transaction id: {start}")
        self.expect_ok(
            self.post("verify_secret", contract.verify_secret_body(txn, secret or user.secret)), "verify secret"
        )
        return str(txn)

    def device_challenge(self, txn: str, device_id: str) -> contract.ChallengeInfo:
        body = self.expect_ok(
            self.post("device_challenge", contract.device_challenge_body(txn, device_id)), "device challenge"
        )
        challenge = contract.parse_challenge(body)
        if challenge.transaction_id is None:
            challenge.transaction_id = txn
        return challenge

    def signed_proof(self, user: User, device: Device) -> tuple[contract.ChallengeInfo, dict[str, Any]]:
        """Run login up to a valid, *unsent* device proof."""
        txn = self.start_and_verify_secret(user)
        challenge = self.device_challenge(txn, device.device_id)
        signature = device.key.sign(challenge.bytes_to_sign())
        return challenge, contract.device_verify_body(challenge, device.device_id, signature)

    def login(self, user: User, device: Device) -> LoginResult:
        challenge, verify_body = self.signed_proof(user, device)
        resp = self.post("device_verify", verify_body)
        body = self.expect_ok(resp, "device verify")
        token = contract.parse_token(body)
        if not token:
            pytest.fail(f"device verify succeeded but returned no session token: {body}")
        return LoginResult(token=str(token), verify_body=verify_body, challenge=challenge, response=resp)
