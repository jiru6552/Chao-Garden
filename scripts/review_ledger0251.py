#!/usr/bin/env python3
"""Stable JSON snapshots and recursive review-report ledger validation."""
from pathlib import Path
import hashlib
import json
import tempfile
from publish_release025 import publish


def digest_bytes(data):
    return hashlib.sha256(data).hexdigest()


def read_snapshot(path):
    data = path.read_bytes()
    document = json.loads(data)
    digest = digest_bytes(data)
    assert digest_bytes(path.read_bytes()) == digest, 'JSON changed during read: ' + str(path)
    return document, digest


def assert_frozen(snapshots):
    for path, digest in snapshots.items():
        assert digest_bytes(path.read_bytes()) == digest, 'Input changed during generation: ' + str(path)


def verify_report_refs(document, root):
    """Check all declared report lists and per-clip report links recursively."""
    checked = {}

    def reference(file, expected):
        path = root / file
        assert not Path(file).is_absolute() and path.resolve().is_relative_to(root.resolve()), file
        assert path.is_file() and len(expected) == 64
        _, actual = read_snapshot(path)
        assert actual == expected, 'Stale nested review reference: ' + file
        checked[path] = actual

    def visit(value):
        if isinstance(value, dict):
            if 'review_report' in value or 'review_report_sha256' in value:
                assert 'review_report' in value and 'review_report_sha256' in value
                reference(value['review_report'], value['review_report_sha256'])
            if 'review_reports' in value:
                assert isinstance(value['review_reports'], list)
                for row in value['review_reports']:
                    reference(row['file'], row['sha256'])
            for child in value.values():
                visit(child)
        elif isinstance(value, list):
            for child in value:
                visit(child)

    visit(document)
    assert_frozen(checked)
    return checked


def publish_json(document, target, root, snapshots):
    """Validate before and after atomic publication, including nested reports."""
    refs = verify_report_refs(document, root)
    frozen = dict(snapshots)
    frozen.update(refs)
    assert_frozen(frozen)
    payload = (json.dumps(document, indent=2) + '\n').encode()
    with tempfile.TemporaryDirectory(prefix='chao0251-review-ledger-') as temporary:
        stage = Path(temporary) / target.name
        stage.write_bytes(payload)
        staged, staged_sha = read_snapshot(stage)
        assert staged_sha == digest_bytes(payload)
        verify_report_refs(staged, root)
        assert_frozen(frozen)
        publish(stage, target)
    saved, actual = read_snapshot(target)
    assert actual == digest_bytes(payload)
    verify_report_refs(saved, root)
    assert_frozen(frozen)
    return actual
