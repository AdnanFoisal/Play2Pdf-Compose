"""
Tests for Gemini resilience: retry + model fallback + friendly errors.

This exists because a real 503 ("This model is currently experiencing high
demand") reached a user: retrying one hot model wasn't enough, so
_gemini_generate now walks a fallback chain. These tests simulate the
provider — no network, no API key.

Usage: python test_gemini_fallback.py
"""
import sys

import server
from google.genai import errors as genai_errors

PAYLOAD_503 = {"error": {"code": 503, "message": "This model is currently experiencing high demand.", "status": "UNAVAILABLE"}}
PAYLOAD_400 = {"error": {"code": 400, "message": "API key not valid", "status": "INVALID_ARGUMENT"}}


class _Resp:
    def __init__(self, text):
        self.text = text


class _FakeModels:
    def __init__(self, fail_models, calls, error_payload=PAYLOAD_503, error_cls=genai_errors.ServerError, code=503):
        self.fail_models = fail_models
        self.calls = calls
        self.error_payload = error_payload
        self.error_cls = error_cls
        self.code = code

    def generate_content(self, *, model, contents, config):
        self.calls.append(model)
        if model in self.fail_models:
            raise self.error_cls(self.code, self.error_payload)
        return _Resp('["ok"]')


class _FakeClient:
    def __init__(self, models):
        self.models = models


def install_fake(fail_models, error_cls=genai_errors.ServerError, payload=PAYLOAD_503, code=503):
    calls = []
    fake = _FakeClient(_FakeModels(set(fail_models), calls, payload, error_cls, code))
    server._get_genai_client = lambda api_key: fake
    return calls


def main():
    failures = []
    server._RETRY_DELAYS = (0.0, 0.0)  # keep the suite fast

    # 1. Primary overloaded -> falls back to the next model and succeeds.
    chain = server.MODEL_MATCH_CHAIN
    calls = install_fake([chain[0]])
    try:
        out = server._gemini_generate("k", chain, "prompt")
        used = calls[-1]
        print(f"1. primary 503 -> fell back to {used} (attempts: {len(calls)})")
        if out != '["ok"]' or used == chain[0]:
            failures.append("did not fall back to a healthy model")
        if calls.count(chain[0]) != len(server._RETRY_DELAYS) + 1:
            failures.append(f"primary should be retried {len(server._RETRY_DELAYS)+1}x, got {calls.count(chain[0])}")
    except Exception as e:
        failures.append(f"fallback raised {type(e).__name__}: {e}")

    # 2. Whole chain overloaded -> GeminiUnavailable with a friendly message.
    calls = install_fake(list(chain))
    try:
        server._gemini_generate("k", chain, "prompt")
        failures.append("exhausted chain should raise GeminiUnavailable")
    except server.GeminiUnavailable as e:
        expected_calls = len(chain) * (len(server._RETRY_DELAYS) + 1)
        print(f"2. whole chain 503 -> GeminiUnavailable after {len(calls)} attempts")
        print(f"   message: {e}")
        if len(calls) != expected_calls:
            failures.append(f"expected {expected_calls} attempts, got {len(calls)}")
        if "try again" not in str(e).lower():
            failures.append("message should tell the user to retry")
    except Exception as e:
        failures.append(f"expected GeminiUnavailable, got {type(e).__name__}")

    # 3. Non-transient error (bad key) -> fails fast, no chain walk.
    calls = install_fake(list(chain), genai_errors.ClientError, PAYLOAD_400, 400)
    try:
        server._gemini_generate("k", chain, "prompt")
        failures.append("bad key should raise")
    except server.GeminiUnavailable:
        failures.append("bad key must NOT be treated as transient")
    except genai_errors.ClientError:
        print(f"3. bad key (400) -> failed fast after {len(calls)} attempt(s)")
        if len(calls) != 1:
            failures.append(f"bad key should not retry/fallback, got {len(calls)} attempts")

    # 4. friendly_error() never leaks raw provider payloads.
    cases = [
        (server.GeminiUnavailable("All Gemini models are busy right now. Please try again in a minute."), "busy"),
        (genai_errors.ServerError(503, PAYLOAD_503), "busy"),
        (genai_errors.ClientError(400, PAYLOAD_400), "key"),
    ]
    print("4. friendly_error():")
    for exc, expect in cases:
        msg = server.friendly_error(exc)
        print(f"   {type(exc).__name__:18} -> {msg}")
        if "{" in msg or "'error'" in msg:
            failures.append(f"friendly_error leaked a payload: {msg}")
        if expect not in msg.lower():
            failures.append(f"friendly_error missing '{expect}': {msg}")

    if failures:
        print("\nFAILURES:")
        for f in failures:
            print(" -", f)
        sys.exit(1)
    print("\nGEMINI RESILIENCE: ALL CHECKS PASS")


if __name__ == "__main__":
    main()
