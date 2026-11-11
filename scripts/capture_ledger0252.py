"""Immutable native capture identity, independent of mutable gallery status."""
import hashlib
import json


def capture_ledger_sha256(manifest):
    assert manifest['release'] == '0.25.2'
    captures = manifest['captures']
    assert len(captures) == len({row['file'] for row in captures})
    document = dict(release=manifest['release'], scene_plan_sha256=manifest['plan_sha256'],
                    source_log_sha256=manifest['log_sha256'], captures=captures)
    payload = json.dumps(document, sort_keys=True, separators=(',', ':')).encode()
    return hashlib.sha256(payload).hexdigest()
