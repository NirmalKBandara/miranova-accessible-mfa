"""SEC-11 — No secrets, keys or credential stores committed to the repository.

Runs without a backend: it scans files tracked by Git.
"""

import re
import subprocess
from pathlib import Path

import pytest

REPO_ROOT = Path(__file__).resolve().parents[2]
MAX_SCAN_BYTES = 2_000_000

CONTENT_PATTERNS = {
    "PEM private key": re.compile(r"-----BEGIN (?:RSA |EC |DSA |OPENSSH |ENCRYPTED )?PRIVATE KEY-----"),
    "JSON Web Token": re.compile(r"eyJ[A-Za-z0-9_-]{10,}\.eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}"),
    "AWS access key id": re.compile(r"\bAKIA[0-9A-Z]{16}\b"),
    "Google API key": re.compile(r"\bAIza[0-9A-Za-z_-]{35}\b"),
    "Slack token": re.compile(r"\bxox[baprs]-[0-9A-Za-z-]{10,}\b"),
}

FORBIDDEN_NAME = re.compile(
    r"(^|/)(\.env(\.(?!example$)[^/]+)?|id_rsa|id_ecdsa|id_ed25519)$"
    r"|\.(pem|key|p12|pfx|jks|keystore|db|sqlite|sqlite3)$",
    re.IGNORECASE,
)


def _git(*args):
    return subprocess.run(["git", *args], cwd=REPO_ROOT, capture_output=True, text=True, timeout=30)


@pytest.fixture(scope="module")
def tracked_files():
    result = _git("ls-files", "-z")
    if result.returncode != 0:
        pytest.skip(f"not a git checkout: {result.stderr.strip()}")
    return [f for f in result.stdout.split("\0") if f]


def test_no_secret_patterns_in_tracked_files(tracked_files):
    """Scan every tracked text file for private keys, JWTs and cloud API keys.
    Refs: TM-28, FR-03, CTL-27
    Expected: no matches.
    """
    hits = []
    for rel in tracked_files:
        path = REPO_ROOT / rel
        try:
            if not path.is_file() or path.stat().st_size > MAX_SCAN_BYTES:
                continue
            text = path.read_text(encoding="utf-8")
        except (UnicodeDecodeError, OSError):
            continue  # binary or unreadable
        for name, pattern in CONTENT_PATTERNS.items():
            if pattern.search(text):
                hits.append(f"{rel}: {name}")
    assert not hits, "possible secrets committed:\n" + "\n".join(hits)


def test_no_credential_files_tracked(tracked_files):
    """No .env files (except .env.example), private keys, keystores or database files are tracked.
    Refs: TM-28, TM-18, FR-03, NFR-S08, CTL-27
    Expected: none tracked.
    """
    bad = [f for f in tracked_files if FORBIDDEN_NAME.search(f)]
    assert not bad, f"credential or data files tracked by Git: {bad}"


@pytest.mark.xfail(reason="F-11: root .gitignore is empty, so .env is not ignored (owner #43)", strict=False)
def test_gitignore_excludes_local_secret_files():
    """Check that Git would ignore typical local secret files.
    Refs: TM-28, FR-03, finding F-11, CTL-27
    Expected: .env, backend/.env, a .pem key and a SQLite DB are all ignored.
    """
    candidates = [".env", "backend/.env", "backend/dev.db", "keys/server.pem"]
    not_ignored = [c for c in candidates if _git("check-ignore", "-q", "--no-index", c).returncode != 0]
    assert not not_ignored, f"not ignored by .gitignore: {not_ignored}"
