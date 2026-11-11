#!/usr/bin/env python3
"""Bind completed independent native-clip reviews; does not inspect images."""
from pathlib import Path
import hashlib
import json
from review_ledger0251 import assert_frozen, publish_json, read_snapshot, verify_report_refs

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / 'docs'
PROOF = DOCS / 'validation-0.25.1'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def read(path):
    return json.loads(path.read_text())


def main():
    plan_path = DOCS / 'release-scenes-0.25.1.json'
    provenance_path = DOCS / 'motion-0.25.1/video/motion-provenance.json'
    log_path = PROOF / 'world-capture.txt'
    plan, plan_sha = read_snapshot(plan_path)
    provenance, provenance_sha = read_snapshot(provenance_path)
    snapshots = {plan_path: plan_sha, provenance_path: provenance_sha, log_path: sha(log_path)}
    assert sha(plan_path) == provenance['scene_plan_sha256']
    assert sha(log_path) == provenance['source_log_sha256']
    required = {row['key'] for row in plan['scenes'] if row.get('motion') or row.get('walk_to')}
    clips = {row['scene']: row for row in provenance['clips']}
    assert len(clips) == len(provenance['clips']) and set(clips) == required
    reviews = {}
    reports = []
    for role in ('portal', 'garden', 'staff', 'school'):
        report_path = PROOF / (role + '-visual-review.json')
        report, report_sha = read_snapshot(report_path)
        snapshots[report_path] = report_sha
        verify_report_refs(report, ROOT)
        assert report['release'] == '0.25.1' and report['result'] == 'PASS'
        assert sha(log_path) in json.dumps(report), 'Stale independent review: ' + role
        reports.append(dict(file=report_path.relative_to(ROOT).as_posix(), sha256=report_sha))
        rows = report.get('clips', [])
        if role == 'staff':
            motion = report['fortune_native_motion']
            rows = [dict(key='staff-fortune-no-blink', result=report['result'], **motion)]
        for row in rows:
            key = row['key']
            assert key in required and key not in reviews, 'Missing or duplicate scope: ' + key
            clip = clips[key]
            assert row['result'] == 'PASS' and row['sha256'] == clip['sha256']
            native_path = provenance_path.parent / clip['file']
            assert sha(native_path) == clip['sha256']
            assert row.get('file', row.get('path')) == native_path.relative_to(ROOT).as_posix()
            reviews[key] = dict(key=key, file=native_path.relative_to(ROOT).as_posix(),
                sha256=clip['sha256'], result='PASS', reviewer=role + ' independent visual owner',
                review_report=report_path.relative_to(ROOT).as_posix(), review_report_sha256=report_sha,
                native_frame_count=clip['contiguous_source_frame_count'],
                source_first_frame_index=clip['source_first_frame_index'],
                source_last_frame_index=clip['source_last_frame_index'],
                independent_sampling_evidence=row)
    assert set(reviews) == required, 'Unreviewed clips: ' + repr(sorted(required - set(reviews)))
    result = dict(release='0.25.1', result='PASS',
        method='Collective manual frame-based inspection by the named independent visual owners. Each entry retains its exact clip SHA, source-contiguous frame interval, owner report SHA and precise native-index/PTS sampling scope. Door-action timelines cover every recorded native frame; the Fortune face review covers every decoded face crop; other clips use full-timeline sampled contacts and selected consecutive/full-resolution native frames as stated in their reports. This aggregation only binds completed manual reviews; it does not classify images or assert real-time playback or all-frame full-screen review.',
        scene_plan_sha256=sha(plan_path), source_log_sha256=sha(log_path),
        source_provenance_sha256=sha(provenance_path), reviewed_clip_count=len(reviews),
        unreviewed_clip_count=0, known_blocking_visual_defects=0, review_reports=reports,
        limitations=['Dimension transfers retain the native Loading terrain screen and brief destination client mesh streaming; they are not described as seamless.',
            'Renderer-state authority probes and actual server mechanics complement visual sampling and are recorded separately in the runtime proof.'],
        clips=[reviews[row['key']] for row in plan['scenes'] if row['key'] in required])
    target = PROOF / 'motion-visual-review.json'
    assert_frozen(snapshots)
    digest = publish_json(result, target, ROOT, snapshots)
    print('PASS', len(reviews), 'exact native clips; all nested report refs stable before/after;', digest)


if __name__ == '__main__':
    main()
