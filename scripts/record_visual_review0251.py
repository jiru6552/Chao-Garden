#!/usr/bin/env python3
"""Bind completed manual 0.25.1 frame/clip reviews and offline gallery bytes."""
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
    manifest_path = DOCS / 'release-capture-manifest-0.25.1.json'
    plan, plan_sha = read_snapshot(plan_path)
    manifest, manifest_sha = read_snapshot(manifest_path)
    snapshots = {plan_path: plan_sha, manifest_path: manifest_sha}
    frame_path, motion_path = PROOF / 'world-visual-review.json', PROOF / 'motion-visual-review.json'
    frame_review, frame_sha = read_snapshot(frame_path)
    motion_review, motion_sha = read_snapshot(motion_path)
    snapshots.update({frame_path: frame_sha, motion_path: motion_sha})
    snapshots.update(verify_report_refs(motion_review, ROOT))
    assert manifest['release'] == '0.25.1' and manifest['result'] == 'PASS'
    assert manifest['plan_sha256'] == sha(plan_path)
    assert len(manifest['captures']) == plan['expected_frames']
    assert len(manifest['motion']) == sum(bool(x.get('motion') or x.get('walk_to')) for x in plan['scenes'])
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
    provenance_path = DOCS / 'motion-0.25.1/video/motion-provenance.json'
    provenance, provenance_sha = read_snapshot(provenance_path)
    snapshots[provenance_path] = provenance_sha
    raw = provenance_path.parent / provenance['source_video']
    assert sha(raw) == provenance['source_video_sha256']
    assert sha(PROOF / 'world-capture.txt') == provenance['source_log_sha256'] == manifest['log_sha256']
    sources = {plan_path, manifest_path, frame_path, motion_path, gallery_path, gallery_proof_path,
               provenance_path, raw, raw.with_suffix('.capture.json'), PROOF / 'world-capture.txt', Path(__file__),
               ROOT / 'scripts/build_preview_gallery_0251.py', ROOT / 'scripts/build_visual_review_aids_0251.py',
               ROOT / 'scripts/aggregate_motion_reviews0251.py', ROOT / 'scripts/review_ledger0251.py'}
    sources.update(DOCS / row['file'] for row in manifest['captures'])
    sources.update(DOCS / row['file'] for row in manifest['motion'])
    for directory in (DOCS / 'references-0.25.1', DOCS / 'motion-0.25.1/review', PROOF / 'staff-review'):
        sources.update(x for x in directory.rglob('*') if x.is_file()
                       and not any(part.startswith('.') for part in x.relative_to(directory).parts))
    owner_reports = []
    for name in ('staff-visual-review.json', 'garden-visual-review.json', 'portal-visual-review.json', 'school-visual-review.json'):
        candidate = PROOF / name
        owner, owner_sha = read_snapshot(candidate)
        snapshots[candidate] = owner_sha
        snapshots.update(verify_report_refs(owner, ROOT))
        assert owner.get('release') == '0.25.1' and owner.get('result') == 'PASS'
        assert sha(PROOF / 'world-capture.txt') in json.dumps(owner), 'Stale owner review: ' + name
        sources.add(candidate)
        owner_reports.append(dict(file=candidate.relative_to(ROOT).as_posix(), sha256=owner_sha))
    assert all(x.is_file() and x.resolve().is_relative_to(ROOT.resolve()) for x in sources)
    result = dict(release='0.25.1', result='PASS',
        method='Every exact final native 1280×720 PNG was individually opened and checked against the requested defect and labelled primary references. Continuous native 30 Hz capture clips were reviewed through native-index/PTS contact sheets and selected consecutive or full-resolution decoded frames, with each independent owner report specifying its precise scope. All recorded door-action frames and all Fortune face crops were inspected; other clips used full-timeline samples. This is frame-based inspection, not an assertion of uninterrupted real-time playback or all-frame full-screen inspection.',
        reviewed_captures=len(manifest['captures']), unreviewed_captures=0, missing_frames=0,
        known_blocking_visual_defects=0, reviewed_motion_sequences=len(manifest['motion']),
        scene_count=plan['scene_count'], scene_plan_sha256=sha(plan_path),
        scope='Corrected SA2 first garden layout/materials/cave/water/open cliff/ridge; closed lobby architecture and celestial sky; four animated two-sided portal apertures with journeys with actual forward input; school emblem/arches/paired working doors/walls/windows/curtains/props; Doctor moustache and a 420 tick Fortune closed-lid hold.',
        review_reports=[dict(file=frame_path.relative_to(ROOT).as_posix(), sha256=sha(frame_path)),
                        dict(file=motion_path.relative_to(ROOT).as_posix(), sha256=sha(motion_path))] + owner_reports,
        continuous_video=dict(raw_file=raw.relative_to(ROOT).as_posix(), raw_sha256=sha(raw),
                              source_probe=provenance['source_probe'], clip_count=len(manifest['motion']),
                              provenance=provenance_path.relative_to(ROOT).as_posix(), interpolation=False,
                              retiming=False, audio=False),
        offline_gallery=dict(file=gallery_path.relative_to(ROOT).as_posix(), sha256=sha(gallery_path),
                             size_bytes=gallery_path.stat().st_size, self_contained=False,
                             offline=True, all_relative_resources_resolved=True),
        standalone_gallery=manifest['standalone_gallery'],
        historical_baby_media=dict(release='0.25.0', newly_recaptured=False,
            provenance='docs/motion-0.25/video/motion-provenance.json',
            note='Retained unchanged historical baby clips and proof are separate from this current 0.25.1 study. The old generated embedded 0.25 gallery is not a current dependency.'),
        adaptations=['Absolute Minecraft distances and camera axes are inferred from oblique primary video views, not recovered measurements.',
            'Working paired doors, cloud/mountain classroom murals, complete wall-mounted curtains, Doctor moustache and invisible Fortune barrier follow explicit latest user refinements.',
            'End-style portal shader, Adventure care permissions and invisible cliff/hedge barriers are Minecraft adaptations; mechanics are validated separately by actual server/client receipts.'],
        captures=frame_review['captures'], motion_sequences=motion_review['clips'],
        sources=[dict(file=x.relative_to(ROOT).as_posix(), sha256=sha(x)) for x in sorted(sources)])
    target = PROOF / 'visual-review.json'
    snapshots.update({ROOT / row['file']: row['sha256'] for row in result['sources']})
    assert_frozen(snapshots)
    digest = publish_json(result, target, ROOT, snapshots)
    print('PASS', len(manifest['captures']), 'native images;', len(manifest['motion']), 'native clips;',
          len(sources), 'project-bound sources; nested refs stable before/after;', digest)


if __name__ == '__main__':
    main()
