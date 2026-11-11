#!/usr/bin/env python3
"""Bind completed focused current native-clip reviews; never classify pictures."""
from pathlib import Path
import hashlib
import math
from fractions import Fraction
from review_ledger0252 import assert_frozen, publish_json, read_snapshot, verify_report_refs

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / 'docs'
PROOF = DOCS / 'validation-0.25.2'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    plan_path = DOCS / 'release-scenes-0.25.2.json'
    provenance_path = DOCS / 'motion-0.25.2/video/motion-provenance.json'
    log_path = PROOF / 'world-capture.txt'
    cohort_path = PROOF / 'source-cohort.json'
    plan, plan_sha = read_snapshot(plan_path)
    provenance, provenance_sha = read_snapshot(provenance_path)
    cohort, cohort_sha = read_snapshot(cohort_path)
    snapshots = {plan_path: plan_sha, provenance_path: provenance_sha,
                 log_path: sha(log_path), cohort_path: cohort_sha}
    assert cohort['release'] == '0.25.2' and cohort['status'] == 'FROZEN'
    for row in cohort['sources']:
        source = ROOT / row['file']
        assert source.resolve().is_relative_to(ROOT.resolve()) and sha(source) == row['sha256']
        snapshots[source] = row['sha256']
    assert plan_sha == provenance['scene_plan_sha256']
    assert sha(log_path) == provenance['source_log_sha256']
    required = {row['key'] for row in plan['scenes']
                if row.get('motion') or row.get('walk_to') or row.get('walk_distance') or row.get('camera_path')}
    scenes = {row['key']: row for row in plan['scenes']}
    clips = {row['scene']: row for row in provenance['clips']}
    assert len(clips) == len(provenance['clips']) and set(clips) == required
    reviews, reports = {}, []
    for role in ('school', 'staff', 'lobby', 'garden'):
        report_path = PROOF / (role + '-visual-review.json')
        report, report_sha = read_snapshot(report_path)
        snapshots[report_path] = report_sha
        snapshots.update(verify_report_refs(report, ROOT))
        assert report['release'] == '0.25.2' and report['result'] == 'PASS'
        assert report['source_log_sha256'] == sha(log_path)
        assert report['scene_plan_sha256'] == plan_sha
        assert report['source_cohort_sha256'] == cohort_sha
        reports.append(dict(file=report_path.relative_to(ROOT).as_posix(), sha256=report_sha))
        for row in report.get('clips', []):
            key = row['key']
            assert key in required and key not in reviews, 'Missing or duplicate scope: ' + key
            clip = clips[key]
            native_path = provenance_path.parent / clip['file']
            assert row['result'] == 'PASS' and row['sha256'] == clip['sha256'] == sha(native_path)
            snapshots[native_path] = clip['sha256']
            actual_hz = float(Fraction(clip['probe']['avg_frame_rate']))
            assert math.isfinite(actual_hz) and actual_hz > 0
            assert int(clip['probe']['nb_frames']) == clip['contiguous_source_frame_count'] == clip['source_last_frame_index'] - clip['source_first_frame_index'] + 1
            assert clip['unique_monotonic_native_pts'] and clip['maximum_relative_pts_error_seconds'] <= 1.e-5
            assert row['file'] == native_path.relative_to(ROOT).as_posix()
            assert row['method'] and row['observations'] and row['limitations']
            assert 0 < row['reviewed_native_frame_count'] <= clip['contiguous_source_frame_count']
            if scenes[key].get('action') in ('door-toggle', 'door-denied-toggle'):
                assert row['all_native_frames_reviewed'] is True
                assert row['reviewed_native_frame_count'] == clip['contiguous_source_frame_count']
            reviews[key] = dict(key=key, file=native_path.relative_to(ROOT).as_posix(),
                sha256=clip['sha256'], result='PASS', reviewer='review0252 focused ' + role + ' inspection',
                review_report=report_path.relative_to(ROOT).as_posix(), review_report_sha256=report_sha,
                native_frame_count=clip['contiguous_source_frame_count'], actual_average_capture_hz=actual_hz,
                unique_monotonic_native_pts=clip['unique_monotonic_native_pts'],
                maximum_relative_pts_error_seconds=clip['maximum_relative_pts_error_seconds'],
                source_first_frame_index=clip['source_first_frame_index'],
                source_last_frame_index=clip['source_last_frame_index'],
                visual_sampling_evidence=row)
    assert set(reviews) == required, 'Unreviewed clips: ' + repr(sorted(required - set(reviews)))
    result = dict(release='0.25.2', result='PASS',
        method='This aggregator binds completed focused manual frame reviews. Each owner entry declares its precise native-frame and PTS sampling scope, observations and limitations. Door action clips require every decoded native frame to have been inspected. It does not inspect pictures itself or assert uninterrupted real-time playback.',
        scene_plan_sha256=plan_sha, source_log_sha256=sha(log_path), source_cohort_sha256=cohort_sha,
        source_provenance_sha256=provenance_sha, reviewed_clip_count=len(reviews),
        unreviewed_clip_count=0, known_blocking_visual_defects=0, review_reports=reports,
        limitations=['The 288 exposed-face ray matrix uses a restored isolated native fixture; actual authored school actions cover reachable faces and disclose wall-occluded opened backs.',
                     'Actual native selection/collision and packet/authority receipts complement visual mesh inspection; one cannot substitute for the other.',
                     'Prior baby and 0.25.1 garden media is historical; current 0.25.2 garden claims bind new native evidence.'],
        clips=[reviews[row['key']] for row in plan['scenes'] if row['key'] in required])
    digest = publish_json(result, PROOF / 'motion-visual-review.json', ROOT, snapshots)
    assert_frozen(snapshots)
    print('PASS', len(reviews), 'exact current native clips; nested references and source cohort closed;', digest)


if __name__ == '__main__':
    main()
