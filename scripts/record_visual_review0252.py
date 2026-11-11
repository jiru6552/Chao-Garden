#!/usr/bin/env python3
"""Bind completed manual 0.25.2 frame/clip reviews and offline gallery bytes."""
from pathlib import Path
import hashlib
import json
from capture_ledger0252 import capture_ledger_sha256
from review_ledger0252 import assert_frozen, publish_json, read_snapshot, verify_report_refs

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / 'docs'
PROOF = DOCS / 'validation-0.25.2'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def read(path):
    return json.loads(path.read_text())


def main():
    plan_path = DOCS / 'release-scenes-0.25.2.json'
    manifest_path = DOCS / 'release-capture-manifest-0.25.2.json'
    plan, plan_sha = read_snapshot(plan_path)
    manifest, manifest_sha = read_snapshot(manifest_path)
    cohort_path = PROOF / 'source-cohort.json'
    cohort, cohort_sha = read_snapshot(cohort_path)
    assert cohort['release'] == '0.25.2' and cohort['status'] == 'FROZEN'
    snapshots = {plan_path: plan_sha, manifest_path: manifest_sha, cohort_path: cohort_sha}
    for row in cohort['sources']:
        source = ROOT / row['file']
        assert source.resolve().is_relative_to(ROOT.resolve()) and sha(source) == row['sha256']
        snapshots[source] = row['sha256']
    frame_path, motion_path = PROOF / 'world-visual-review.json', PROOF / 'motion-visual-review.json'
    frame_review, frame_sha = read_snapshot(frame_path)
    motion_review, motion_sha = read_snapshot(motion_path)
    snapshots.update({frame_path: frame_sha, motion_path: motion_sha})
    snapshots.update(verify_report_refs(frame_review, ROOT))
    snapshots.update(verify_report_refs(motion_review, ROOT))
    assert frame_review['source_cohort_sha256'] == motion_review['source_cohort_sha256'] == cohort_sha
    assert frame_review['scene_plan_sha256'] == motion_review['scene_plan_sha256'] == plan_sha
    assert manifest['release'] == '0.25.2' and manifest['result'] == 'PASS'
    log_sha = sha(PROOF / 'world-capture.txt')
    assert frame_review['source_log_sha256'] == motion_review['source_log_sha256'] == manifest['log_sha256'] == log_sha
    aid_path = PROOF / 'still-review/index.json'
    aids, aid_sha = read_snapshot(aid_path)
    snapshots[aid_path] = aid_sha
    assert aids['release'] == '0.25.2' and aids['status'] == 'REVIEW_AIDS_ONLY_NO_VISUAL_JUDGMENT'
    assert aids['capture_ledger_sha256'] == capture_ledger_sha256(manifest)
    aid_captures = {row['file']: row for row in aids['captures']}
    assert len(aid_captures) == len(aids['captures']) == len(manifest['captures'])
    assert set(aid_captures) == {'docs/' + row['file'] for row in manifest['captures']}
    for row in manifest['captures']:
        assert aid_captures['docs/' + row['file']]['sha256'] == row['sha256']
    for row in aids['sheets']:
        sheet = ROOT / row['file']
        assert sheet.resolve().is_relative_to(ROOT.resolve()) and sha(sheet) == row['sha256']
        snapshots[sheet] = row['sha256']
    assert manifest['plan_sha256'] == sha(plan_path)
    assert len(manifest['captures']) == plan['expected_frames']
    assert len(manifest['motion']) == sum(bool(x.get('motion') or x.get('walk_to') or x.get('walk_distance') or x.get('camera_path')) for x in plan['scenes'])
    checked_frames = {x['file']: x for x in frame_review['captures']}
    checked_clips = {x['key']: x for x in motion_review['clips']}
    assert len(checked_frames) == len(frame_review['captures']) == plan['expected_frames']
    assert set(checked_frames) == {Path(x['file']).name for x in manifest['captures']}
    assert len(checked_clips) == len(motion_review['clips']) == len(manifest['motion'])
    assert set(checked_clips) == {x['key'] for x in manifest['motion']}
    for capture in manifest['captures']:
        file = DOCS / capture['file']
        review = checked_frames[file.name]
        assert sha(file) == capture['sha256'] == review['sha256'] and review['result'] == 'PASS'
    for clip in manifest['motion']:
        review = checked_clips[clip['key']]
        assert sha(DOCS / clip['file']) == clip['sha256'] == review['sha256'] and review['result'] == 'PASS'
    assert frame_review['result'] == motion_review['result'] == 'PASS'
    gallery_path = DOCS / manifest['gallery']['file']
    gallery_proof_path = PROOF / 'gallery.json'
    gallery_proof, gallery_proof_sha = read_snapshot(gallery_proof_path)
    snapshots[gallery_proof_path] = gallery_proof_sha
    assert sha(gallery_path) == manifest['gallery']['sha256'] == gallery_proof['gallery_sha256']
    assert gallery_proof['result'] == 'PASS' and gallery_proof['all_canonical_resources_resolved']
    assert gallery_proof['standalone_self_contained']
    provenance_path = DOCS / 'motion-0.25.2/video/motion-provenance.json'
    provenance, provenance_sha = read_snapshot(provenance_path)
    snapshots[provenance_path] = provenance_sha
    raw = provenance_path.parent / provenance['source_video']
    assert sha(raw) == provenance['source_video_sha256']
    assert sha(PROOF / 'world-capture.txt') == provenance['source_log_sha256'] == manifest['log_sha256']
    sources = {plan_path, manifest_path, frame_path, motion_path, gallery_path, gallery_proof_path,
               provenance_path, raw, raw.with_suffix('.capture.json'), PROOF / 'world-capture.txt', Path(__file__),
               ROOT / 'scripts/build_preview_gallery_0252.py', ROOT / 'scripts/build_visual_review_aids_0252.py',
               ROOT / 'scripts/aggregate_motion_reviews0252.py', ROOT / 'scripts/build_still_review_aids0252.py', ROOT / 'scripts/review_ledger0252.py', ROOT / 'scripts/capture_ledger0252.py'}
    sources.update(DOCS / row['file'] for row in manifest['captures'])
    sources.update(DOCS / row['file'] for row in manifest['motion'])
    for row in manifest['references']:
        reference = DOCS / row['file']
        assert reference.resolve().is_relative_to(DOCS.resolve()) and sha(reference) == row['sha256']
        snapshots[reference] = row['sha256']
        sources.add(reference)
    for directory in (DOCS / 'references-0.25.2', DOCS / 'motion-0.25.2/review', PROOF / 'staff-review', PROOF / 'still-review'):
        sources.update(x for x in directory.rglob('*') if x.is_file()
                       and not any(part.startswith('.') for part in x.relative_to(directory).parts))
    owner_reports = []
    for name in ('staff-visual-review.json', 'school-visual-review.json', 'lobby-visual-review.json', 'garden-visual-review.json'):
        candidate = PROOF / name
        owner, owner_sha = read_snapshot(candidate)
        snapshots[candidate] = owner_sha
        snapshots.update(verify_report_refs(owner, ROOT))
        assert owner.get('release') == '0.25.2' and owner.get('result') == 'PASS'
        assert sha(PROOF / 'world-capture.txt') in json.dumps(owner), 'Stale owner review: ' + name
        sources.add(candidate)
        owner_reports.append(dict(file=candidate.relative_to(ROOT).as_posix(), sha256=owner_sha))
    sources.update({cohort_path, DOCS / 'REFERENCE_REFINEMENTS_0252.md', DOCS / 'visual-review-contract-0.25.2.json'})
    assert all(x.is_file() and x.resolve().is_relative_to(ROOT.resolve()) for x in sources)
    result = dict(release='0.25.2', result='PASS',
        method='Exact final native 1280×720 PNGs and continuous native mesh clips were manually inspected against all thirteen latest before screenshots and current themed-approach, hidden-market, lobby and compact-garden obligations. The cited owner reports declare exact hashes, native-frame/PTS coverage, observations and limitations. Door action clips require all decoded frames reviewed. This binds those completed reviews and does not assert uninterrupted real-time playback.',
        reviewed_captures=len(manifest['captures']), unreviewed_captures=0, missing_frames=0,
        known_blocking_visual_defects=0, reviewed_motion_sequences=len(manifest['motion']),
        scene_count=plan['scene_count'], scene_plan_sha256=sha(plan_path),
        source_cohort_sha256=cohort_sha,
        scope='School entrance header closed at user low oblique angles; visible panel native door selection and reverse-face UV orientation with complete-pair prediction/acknowledgement/rollback; Doctor reflected upper-face native shading; complete themed staff approaches; hidden cabinet and active merchant; aligned frieze; machine translucency, grounded statues and connected urn flowers; compact SA2 first-garden layout.',
        review_reports=[dict(file=frame_path.relative_to(ROOT).as_posix(), sha256=sha(frame_path)),
                        dict(file=motion_path.relative_to(ROOT).as_posix(), sha256=sha(motion_path))] + owner_reports,
        capture_rate_policy='Whole recording requests30Hz with actual mean28–32Hz. Local clips retain their measured positive native cadence; contiguous source indices and exact relative PTS intervals govern validation, with no interpolation or retiming.',
        continuous_video=dict(raw_file=raw.relative_to(ROOT).as_posix(), raw_sha256=sha(raw),
                              source_probe=provenance['source_probe'], clip_count=len(manifest['motion']),
                              provenance=provenance_path.relative_to(ROOT).as_posix(), interpolation=False,
                              retiming=False, audio=False),
        offline_gallery=dict(file=gallery_path.relative_to(ROOT).as_posix(), sha256=sha(gallery_path),
                             size_bytes=gallery_path.stat().st_size, self_contained=False,
                             offline=True, all_relative_resources_resolved=True),
        standalone_gallery=manifest['standalone_gallery'],
        historical_media=dict(retained_payload_release='0.25.0', earlier_0251_native_media_duplicated=False, newly_recaptured=False,
            note='Retained original baby payload is historical. Earlier 0.25.1 native media is not duplicated in the new complete archive; current garden/lobby claims bind only 0.25.2 captures.'),
        limitations=['The full 288 exposed-face native ray matrix uses a labelled restored isolated fixture. Actual authored school actions cover reachable faces; opened wall-facing backs are legitimately occluded.',
                     'The exact frame-based visual inspection method is declared per owner/clip; contacts are review aids, not new native evidence.',
                     'Procedural Minecraft geometry and destination themes are adaptations; this study does not claim recovered Sonic source meshes or measured original dimensions.'],
        captures=frame_review['captures'], motion_sequences=motion_review['clips'],
        primary_and_before_references=manifest['references'],
        sources=[dict(file=x.relative_to(ROOT).as_posix(), sha256=sha(x)) for x in sorted(sources)])
    target = PROOF / 'visual-review.json'
    snapshots.update({ROOT / row['file']: row['sha256'] for row in result['sources']})
    assert_frozen(snapshots)
    digest = publish_json(result, target, ROOT, snapshots)
    print('PASS', len(manifest['captures']), 'native images;', len(manifest['motion']), 'native clips;',
          len(sources), 'project-bound sources; nested refs stable before/after;', digest)


if __name__ == '__main__':
    main()
