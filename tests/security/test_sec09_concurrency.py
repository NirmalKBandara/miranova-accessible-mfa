"""SEC-09 — Parallel submission of the same proof (race on challenge consumption, AC-03)."""

from concurrent.futures import ThreadPoolExecutor

import httpx
import pytest

import sec_contract as contract
from sec_config import CONCURRENCY
from sec_helpers import response_has_token

pytestmark = pytest.mark.backend


def test_parallel_identical_proofs_yield_at_most_one_session(api, enrolled):
    """Send the same valid device proof N times concurrently.
    Refs: TM-04, TH-13, TC-13, FR-21, NFR-R02, CTL-05
    Expected: at most one response carries a session; no 5xx errors.
    """
    user, device = enrolled
    _, body = api.signed_proof(user, device)
    path = contract.ENDPOINTS["device_verify"]

    def submit(_):
        with httpx.Client(base_url=api.base_url, timeout=api.timeout) as client:
            return client.post(path, json=body)

    with ThreadPoolExecutor(max_workers=CONCURRENCY) as pool:
        responses = list(pool.map(submit, range(CONCURRENCY)))

    server_errors = [r.status_code for r in responses if r.status_code >= 500]
    sessions = [r for r in responses if 200 <= r.status_code < 300 and response_has_token(r)]
    assert not server_errors, f"server errors under concurrency: {server_errors}"
    assert len(sessions) <= 1, f"{len(sessions)} sessions issued for one challenge (double-spend)"
    assert len(sessions) == 1, "no request succeeded; the legitimate proof should succeed exactly once"
