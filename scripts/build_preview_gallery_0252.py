#!/usr/bin/env python3
"""Build the offline 0.25.2 gallery from exact native client receipts and review."""
from pathlib import Path
import argparse
import base64
import hashlib
import html
import io
import json
import math
import re
import shutil
import tempfile
from fractions import Fraction
from PIL import Image
from publish_release025 import publish
from review_ledger0252 import assert_frozen, publish_json, read_snapshot, verify_report_refs

ROOT = Path(__file__).resolve().parents[1]


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def copy(source, destination):
    destination.parent.mkdir(parents=True, exist_ok=True)
    if source.resolve() != destination.resolve():
        shutil.copy2(source, destination)


def data(path, reference=False):
    if not reference:
        return 'data:image/png;base64,' + base64.b64encode(path.read_bytes()).decode()
    with Image.open(path) as image:
        image.thumbnail((1280, 720))
        output = io.BytesIO()
        image.convert('RGB').save(output, 'JPEG', quality=90)
    return 'data:image/jpeg;base64,' + base64.b64encode(output.getvalue()).decode()


def phases(scene):
    if scene.get('walk_to'):
        return ['start', 'arrived']
    return ['start', 'end'] if scene.get('motion') or scene.get('walk_distance') or scene.get('camera_path') else ['still']


def filename(scene, phase):
    return 'chao0252-' + scene['key'] + ('' if phase == 'still' else '-' + phase) + '.png'


def receipts(path, plan):
    log = path.read_text(errors='replace')
    starts = list(re.finditer(r'CHAO_WORLD0252_PLAN scenes=(\d+) expectedFrames=(\d+) sha256=([a-f0-9]{64})', log))
    require(bool(starts), 'No current 0.25.2 plan receipt')
    start = starts[-1]
    require((int(start[1]), int(start[2]), start[3]) ==
            (plan['scene_count'], plan['expected_frames'], sha(ROOT / 'docs/release-scenes-0.25.2.json')),
            'The running client used a different scene plan')
    log = log[start.start():]
    complete = re.search(r'CHAO_WORLD0252_COMPLETE scenes=(\d+) frames=(\d+) saves=(\d+) returnRestored=true', log)
    require(complete and tuple(map(int, complete.groups())) ==
            (plan['scene_count'], plan['expected_frames'], plan['expected_frames']),
            'One coherent complete current client run is required')
    captures = re.findall(r'(CHAO_WORLD0252_CAPTURE file=(\S+) stage=(\d+) scene=([\w-]+) phase=(\w+) dimension=(\S+) mode=(\w+) stableFrames=(\d+) [^\n]+)', log)
    saves = re.findall(r'CHAO_WORLD0252_SAVED file=(\S+) stage=(\d+) sha256=([a-f0-9]{64})', log)
    require(len(captures) == len(saves) == plan['expected_frames'], 'Missing or duplicate capture/save receipt')
    require(len({x[1] for x in captures}) == len(captures) and len({x[0] for x in saves}) == len(saves), 'Duplicate native image names')
    return log, {x[1]: x for x in captures}, {name: (int(stage), digest) for name, stage, digest in saves}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, default=ROOT / 'run/screenshots')
    parser.add_argument('--log', type=Path, default=ROOT / 'docs/validation-0.25.2/world-capture.txt')
    parser.add_argument('--motion-provenance', type=Path, default=ROOT / 'docs/motion-0.25.2/video/motion-provenance.json')
    parser.add_argument('--standalone', type=Path, default=ROOT.parent / 'deliverables/chao-garden-0.25.2-gallery.html')
    parser.add_argument('--allow-unreviewed', action='store_true')
    args = parser.parse_args()
    plan_path = ROOT / 'docs/release-scenes-0.25.2.json'
    plan, plan_sha = read_snapshot(plan_path)
    review_snapshots = {plan_path: plan_sha, args.log: sha(args.log)}
    require(plan['release'] == '0.25.2', 'Wrong release scene plan')
    require(plan['scene_count'] == len(plan['scenes']), 'Scene count drift')
    require(plan['expected_frames'] == sum(len(phases(row)) for row in plan['scenes']), 'Frame count drift')
    log, events, saves = receipts(args.log, plan)
    require(re.search(r'CHAO_WORLD0252_RAY_MATRIX rays=288 misses=0 phantomAirHits=0 collisionMismatches=0 result=PASS', log),
            'Full actual native selection/collision matrix receipt missing')
    proof = ROOT / 'docs/validation-0.25.2'
    proof.mkdir(parents=True, exist_ok=True)
    frame_review_path = proof / 'world-visual-review.json'
    motion_review_path = proof / 'motion-visual-review.json'
    frame_review, frame_sha = read_snapshot(frame_review_path) if frame_review_path.exists() else ({}, None)
    motion_review, motion_sha = read_snapshot(motion_review_path) if motion_review_path.exists() else ({}, None)
    for path, digest in ((frame_review_path, frame_sha), (motion_review_path, motion_sha)):
        if digest:
            review_snapshots[path] = digest
    review_snapshots.update(verify_report_refs(frame_review, ROOT))
    review_snapshots.update(verify_report_refs(motion_review, ROOT))
    if not args.allow_unreviewed:
        cohort_path = proof / 'source-cohort.json'
        cohort, cohort_sha = read_snapshot(cohort_path)
        review_snapshots[cohort_path] = cohort_sha
        require(cohort.get('release') == '0.25.2' and cohort.get('status') == 'FROZEN',
                'Final gallery requires the frozen current source cohort')
        for row in cohort['sources']:
            source_path = ROOT / row['file']
            require(source_path.resolve().is_relative_to(ROOT.resolve()) and sha(source_path) == row['sha256'],
                    'Changed frozen source: ' + row['file'])
            review_snapshots[source_path] = row['sha256']
        for label, report in (('still', frame_review), ('motion', motion_review)):
            require(report.get('release') == '0.25.2' and report.get('result') == 'PASS',
                    'Final gallery requires current top-level ' + label + ' review PASS')
            require(report.get('scene_plan_sha256') == plan_sha and
                    report.get('source_log_sha256') == sha(args.log) and
                    report.get('source_cohort_sha256') == cohort_sha,
                    'Final gallery ' + label + ' review belongs to a different plan/run/source cohort')
    checked_frames = {row['file']: row for row in frame_review.get('captures', [])}
    checked_clips = {row['key']: row for row in motion_review.get('clips', [])}
    provenance, provenance_sha = read_snapshot(args.motion_provenance)
    review_snapshots[args.motion_provenance] = provenance_sha
    require(provenance['source_log_sha256'] == sha(args.log), 'Video belongs to a different client run')
    require(provenance['scene_plan_sha256'] == sha(plan_path), 'Video belongs to a different scene plan')
    raw = args.motion_provenance.parent / Path(provenance['source_video']).name
    require(raw.is_file() and sha(raw) == provenance['source_video_sha256'], 'Missing or changed raw recording')
    review_snapshots[raw] = provenance['source_video_sha256']
    capture_metadata_path = raw.with_suffix('.capture.json')
    require(sha(capture_metadata_path) == provenance['capture_metadata_sha256'], 'Changed native recorder metadata')
    review_snapshots[capture_metadata_path] = provenance['capture_metadata_sha256']
    capture_metadata = json.loads(capture_metadata_path.read_text())
    require(capture_metadata['fps_requested'] == 30 and capture_metadata['fps_mode'] == 'passthrough',
            'Native recorder was not a 30 Hz passthrough capture')
    require(28 <= float(Fraction(provenance['source_probe']['avg_frame_rate'])) <= 32,
            'Native average capture cadence is outside the declared 30 Hz bounds')
    required_clips = {row['key'] for row in plan['scenes'] if row.get('motion') or row.get('walk_to') or row.get('walk_distance') or row.get('camera_path')}
    clips = {row['scene']: row for row in provenance['clips']}
    require(len(clips) == len(provenance['clips']) and required_clips == set(clips),
            'Current continuous clips must match the exact reviewed scene plan')
    cards, capture_records, clip_records, resource_urls = [], [], [], {}
    reference_records = {}
    for stage, scene in enumerate(plan['scenes']):
        key = scene['key']
        views = []
        for phase in phases(scene):
            name = filename(scene, phase)
            source = args.source / name
            digest = sha(source)
            review_snapshots[source] = digest
            event = events.get(name)
            require(event and int(event[2]) == stage and event[3:5] == (key, phase), 'Capture phase/order mismatch: ' + name)
            expected_space = scene['walk_to'] if phase == 'arrived' else scene['space']
            expected_dimension = 'chao_garden:' + ('kindergarten' if expected_space == 'school' else expected_space)
            require(event[5] == expected_dimension, 'Capture is in the wrong actual realm: ' + name)
            require(event[6] == 'ADVENTURE' and int(event[7]) >= 40 and
                    'allBoundsChunksReceived=true renderQueueEmpty=true' in event[0], 'Native render/mode readiness mismatch: ' + name)
            require(saves.get(name) == (stage, digest), 'Native saved PNG SHA mismatch: ' + name)
            with Image.open(source) as image:
                require(image.size == (1280, 720), 'Native framebuffer changed: ' + name)
                image.load()
            if not args.allow_unreviewed:
                review = checked_frames.get(name, {})
                require(review.get('sha256') == digest and review.get('result') == 'PASS', 'Exact current frame not reviewed PASS: ' + name)
            destination = ROOT / 'docs/screenshots-0.25.2' / name
            copy(source, destination)
            review_snapshots[destination] = digest
            capture_records.append(dict(key=key, phase=phase, file=destination.relative_to(ROOT / 'docs').as_posix(),
                                        sha256=digest, capture_event=event[0]))
            encoded_image = data(destination)
            resource_urls[encoded_image] = destination.relative_to(ROOT / 'docs').as_posix()
            views.append('<figure><figcaption>CURRENT MINECRAFT · ' + html.escape(phase.upper()) +
                         '</figcaption><img data-current="' + name + '" src="' + encoded_image +
                         '" width="1280" height="720" alt="' + html.escape(key + ' ' + phase) + '"></figure>')
        for reference in scene.get('references', []):
            path = ROOT / 'docs' / reference['file']
            review_snapshots[path] = sha(path)
            record = reference_records.setdefault(reference['file'], dict(file=reference['file'], sha256=review_snapshots[path], scene_keys=[]))
            require(record['sha256'] == review_snapshots[path], 'Reference changed between scenes')
            if key not in record['scene_keys']:
                record['scene_keys'].append(key)
            label = reference['label'] + (' · ' + reference['timecode'] if reference.get('timecode') else '')
            encoded_reference = data(path, True)
            resource_urls[encoded_reference] = reference['file']
            views.append('<figure class="reference"><figcaption>' + html.escape(label) +
                         '</figcaption><img src="' + encoded_reference + '" alt="' + html.escape(label) + '"></figure>')
        video = ''
        if key in required_clips:
            clip = clips[key]
            path = args.motion_provenance.parent / clip['file']
            require(sha(path) == clip['sha256'], 'Current clip SHA mismatch: ' + key)
            review_snapshots[path] = clip['sha256']
            clip_hz = float(Fraction(clip['probe']['avg_frame_rate']))
            require((clip['probe']['width'], clip['probe']['height']) == (1280, 720) and
                    math.isfinite(clip_hz) and clip_hz > 0,
                    'Current clip must retain a finite positive actual native capture cadence')
            require(int(clip['probe']['nb_frames']) == clip['contiguous_source_frame_count'] ==
                    clip['source_last_frame_index'] - clip['source_first_frame_index'] + 1 and
                    clip['unique_monotonic_native_pts'] and clip['maximum_relative_pts_error_seconds'] <= 1.e-5,
                    'Current clip changed native contiguous frame identity or source PTS intervals')
            if not args.allow_unreviewed:
                review = checked_clips.get(key, {})
                require(review.get('sha256') == clip['sha256'] and review.get('result') == 'PASS', 'Exact native clip not reviewed PASS: ' + key)
            payload = base64.b64encode(path.read_bytes()).decode()
            encoded_video = 'data:video/mp4;base64,' + payload
            resource_urls[encoded_video] = path.relative_to(ROOT / 'docs').as_posix()
            video = '<div class="movie"><p>Contiguous actual game-window frames recorded at a requested 30 Hz, preserving each clip’s actual native cadence and timestamps. No interpolation or retiming.</p><video controls loop playsinline preload="metadata"><source src="' + encoded_video + '" type="video/mp4"></video></div>'
            clip_records.append(dict(key=key, file=path.relative_to(ROOT / 'docs').as_posix(), sha256=clip['sha256'],
                                     probe=clip['probe'], actual_average_capture_hz=clip_hz,
                                     source_first_frame_index=clip['source_first_frame_index'], source_last_frame_index=clip['source_last_frame_index'],
                                     contiguous_source_frame_count=clip['contiguous_source_frame_count'],
                                     unique_monotonic_native_pts=clip['unique_monotonic_native_pts'],
                                     maximum_relative_pts_error_seconds=clip['maximum_relative_pts_error_seconds'],
                                     source_start_seconds=clip['source_start_seconds'], source_end_seconds=clip['source_end_seconds']))
        if scene.get('walk_to'):
            require('CHAO_WORLD0252_TRAVEL_BEGIN scene=' + key in log and
                    'CHAO_WORLD0252_TRAVEL_ARRIVED scene=' + key in log, 'Native walking journey did not arrive: ' + key)
        if scene.get('action') in ('door-toggle', 'door-denied-toggle'):
            require('CHAO_WORLD0252_ACTION_VERIFIED scene=' + key in log and
                    'CHAO_WORLD0252_ACTION_SERVER scene=' + key in log, 'Native door action not verified: ' + key)
            denied = scene['action'] == 'door-denied-toggle'
            parts = int(scene.get('door_parts', 15))
            require(parts == 15, 'Unexpected current complete-pair part count')
            expected_mask = (1 << parts) - 1 if (denied or scene['expect_open']) else 0
            require(re.search(r'CHAO_WORLD0252_ACTION_RAY scene=' + re.escape(key) +
                    r' nativeCameraRay=true manufacturedHit=false hit=true[^\n]*nativeCrosshair=true[^\n]*nativeOutlineClip=true', log),
                    'Door action did not use the actual native selected slab: ' + key)
            predicted = re.search(r'CHAO_WORLD0252_ACTION_PREDICTED scene=' + re.escape(key) +
                r' mask=(\d+) parts=' + str(parts) + r' sequence=(\d+) denied=(true|false)', log)
            require(predicted and int(predicted[1]) == expected_mask and
                    (predicted[3] == 'true') == denied, 'Native whole-pair prediction missing: ' + key)
            require(re.search(r'CHAO_WORLD0252_ACTION_PACKET scene=' + re.escape(key) +
                    r' sequence=' + predicted[2] +
                    r' actualNativePacket=true readOnlyObserver=true', log),
                    'Actual outgoing native door request missing: ' + key)
            ack = re.search(r'CHAO_WORLD0252_ACTION_ACK scene=' + re.escape(key) +
                r' sequence=(\d+) incomingSequence=(\d+) actualNativePacket=true', log)
            require(ack and int(ack[1]) == int(predicted[2]) and int(ack[2]) >= int(ack[1]),
                    'Actual native door acknowledgement missing: ' + key)
            summary = re.search(r'CHAO_WORLD0252_DOOR_RENDER_SUMMARY scene=' + re.escape(key) +
                r' frames=(\d+) mixedFrames=0 parts=' + str(parts) + r' denied=(true|false) nativeAck=true rollback=(true|false)', log)
            require(summary and int(summary[1]) >= 20 and
                    (summary[2] == 'true') == denied and (summary[3] == 'true') == denied,
                    'Complete-pair rendered door coherence missing: ' + key)
            if denied:
                require(re.search(r'CHAO_WORLD0252_ACTION_DENIED scene=' + re.escape(key) +
                    r' serverMask=0 parts=' + str(parts) + r' nativeEvent=true mode=ADVENTURE mayBuild=false', log) and
                    re.search(r'CHAO_WORLD0252_ACTION_ROLLBACK scene=' + re.escape(key) +
                    r' parts=' + str(parts) + r' mask=0 sequence=' + predicted[2] +
                    r' serverCanceled=true nativeAck=true forcedClientWrites=false', log),
                    'Native canceled-click whole-pair rollback missing: ' + key)
        if scene.get('camera_path'):
            video = '<p>Controlled camera inspection: camera position moves through the real live renderer. Video frames retain native order and timestamps; this is not a player walk or video-frame interpolation.</p>' + video
        cards.append('<article id="' + key + '"><h2>' + html.escape(key.replace('-', ' ')) + '</h2><p>' +
                     html.escape(scene.get('review_focus', '')) + '</p><div class="views">' + ''.join(views) +
                     '</div>' + video + '<a href="#top">Back to contents ↑</a></article>')
    status = 'CAPTURED — visual review pending' if args.allow_unreviewed else 'PASS — every exact current frame and native clip reviewed'
    nav = ''.join('<a href="#' + row['key'] + '">' + html.escape(row['key'].replace('-', ' ')) + '</a>' for row in plan['scenes'])
    page = '''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Chao Garden 0.25.2 — actual client refinements</title><style>
    :root{color-scheme:dark}*{box-sizing:border-box}body{margin:0;background:#0b1720;color:#edf6f6;font:16px/1.55 system-ui,sans-serif}header,main,footer{max-width:1600px;margin:auto;padding:28px}h1{font-size:clamp(28px,4vw,46px);line-height:1.12}h2{font-size:24px}p{color:#bfd5df}a{color:#b7e9f4}nav{display:flex;gap:7px;flex-wrap:wrap}nav a{padding:5px 9px;background:#213e4d;border-radius:6px;font-size:13px;text-decoration:none}article{padding:22px;background:#142733;border:1px solid #315362;border-radius:12px;margin:0 0 30px;scroll-margin-top:15px}.views{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,490px),1fr));gap:15px}figure{margin:0;background:#09141b;border:1px solid #32505e;border-radius:7px;overflow:hidden}figcaption{font-size:12px;padding:9px 12px;letter-spacing:.4px;color:#9edcea}.reference figcaption{color:#f2cb89}img,video{display:block;width:100%;height:auto}.movie{max-width:1000px;margin:18px auto}.badge{color:#d6f5cc}footer{color:#a7c6d4}</style><header id="top"><h1>Chao Garden 0.25.2</h1><p>Current Minecraft Java 1.21.1 frames and continuous native game-window clips for the latest school, lobby and compact first-garden refinements. Actual door selection and reverse faces, closed entrance geometry, Doctor lighting, complete themed approaches and the hidden shop address the supplied feedback. User before screenshots are labelled separately.</p><p class="badge">''' + html.escape(status) + '''</p><p>All required images and videos are embedded: this page works offline. Native current PNGs are unmodified. Walking views use actual client forward input; door actions use the actual native camera-selected slab hit and right-click networking. Frame-based visual review does not claim uninterrupted real-time playback.</p><nav>''' + nav + '</nav></header><main>' + ''.join(cards) + '''</main><footer>Optional complete-download recording: <a href="motion-0.25.2/video/world-actual-final.mp4">full actual client capture</a>. This complete-download link is optional; the required clips above are available independently. The complete download retains the original 0.25 baby evidence as historical material. Earlier 0.25.1 native media is not duplicated here and is not relabelled as current 0.25.2 evidence.</footer></html>'''
    gallery = ROOT / 'docs/release-gallery-0.25.2.html'
    ids = re.findall(r'id="([^"]+)"', page)
    require(len(ids) == len(set(ids)) and all(x in ids for x in re.findall(r'href="#([^"]+)"', page)), 'Broken gallery navigation')
    require('<script' not in page and not re.search(r'(?:src|href)="https?://', page), 'Gallery has network or script dependencies')
    embedded = re.findall(r'data-current="([^"]+)" src="data:image/png;base64,([^"]+)"', page)
    require(len(embedded) == len(capture_records), 'Wrong embedded native PNG count')
    digests = {Path(row['file']).name: row['sha256'] for row in capture_records}
    require(all(hashlib.sha256(base64.b64decode(payload, validate=True)).hexdigest() == digests[name] for name, payload in embedded), 'Embedded current image changed')
    for payload in re.findall(r'src="data:image/[^;]+;base64,([^"]+)"', page):
        with Image.open(io.BytesIO(base64.b64decode(payload, validate=True))) as image:
            image.load()
    videos = re.findall(r'src="data:video/mp4;base64,([^"]+)"', page)
    require(len(videos) == len(clip_records) and all(hashlib.sha256(base64.b64decode(payload, validate=True)).hexdigest() == row['sha256'] for payload, row in zip(videos, clip_records)), 'Embedded current clip changed')
    # The complete source uses one copy of each native media file. A separate
    # self-contained download embeds the exact same required payloads.
    relative_page = re.sub(r'src="(data:[^"]+)"', lambda match: 'src="' + resource_urls[match[1]] + '"', page)
    relative_page = relative_page.replace('All required images and videos are embedded: this page works offline.',
        'This complete-download gallery works offline using the native media files beside it. The separate standalone gallery embeds every required image and video.')
    relative_urls = [value for value in re.findall(r'(?:src|href)="([^"]+)"', relative_page) if not value.startswith('#')]
    require(all(not value.startswith(('data:', 'http:', 'https:', '/', 'file:')) for value in relative_urls), 'Canonical gallery has nonportable resource URLs')
    for value in relative_urls:
        local = (gallery.parent / value).resolve()
        require(local.is_relative_to((ROOT / 'docs').resolve()) and local.is_file(), 'Unresolved canonical gallery resource: ' + value)
    # Finish/verify outside synchronization, then publish immutable bytes with
    # the same atomic copy/rename method used for release archives.
    with tempfile.TemporaryDirectory(prefix='chao0252-gallery-') as temporary:
        staging = Path(temporary)
        canonical_stage = staging / 'canonical.html'
        standalone_stage = staging / 'standalone.html'
        canonical_stage.write_text(relative_page)
        standalone_stage.write_text(page)
        verify_report_refs(motion_review, ROOT)
        assert_frozen(review_snapshots)
        publish(canonical_stage, gallery)
        publish(standalone_stage, args.standalone)
    verify_report_refs(motion_review, ROOT)
    assert_frozen(review_snapshots)
    copy(args.log, proof / 'world-capture.txt')
    result = 'CAPTURED' if args.allow_unreviewed else 'PASS'
    manifest = dict(release='0.25.2', result=result, capture_count=len(capture_records), scene_count=len(plan['scenes']),
                    gallery=dict(file=gallery.relative_to(ROOT / 'docs').as_posix(), sha256=sha(gallery), size_bytes=gallery.stat().st_size,
                                 self_contained=False, offline=True, all_relative_resources_resolved=True),
                    standalone_gallery=dict(file=args.standalone.name, sha256=sha(args.standalone), size_bytes=args.standalone.stat().st_size,
                                            self_contained=True, all_required_media_embedded=True),
                    captures=capture_records, motion=clip_records, references=[reference_records[key] for key in sorted(reference_records)], plan_sha256=sha(plan_path), log_sha256=sha(proof / 'world-capture.txt'))
    publish_json(manifest, ROOT / 'docs/release-capture-manifest-0.25.2.json', ROOT, review_snapshots)
    gallery_proof = dict(release='0.25.2', result=result, canonical_offline=True,
        canonical_relative_resource_urls_checked=len(relative_urls), all_canonical_resources_resolved=True,
        standalone_self_contained=True, standalone_gallery=manifest['standalone_gallery'],
        embedded_current_stills=len(capture_records), embedded_native_timestamp_videos=len(clip_records),
        all_embedded_images_decoded=True, all_embedded_media_digest_bound=True, network_dependencies=0,
        referenced_originals_count=len(reference_records), all_reference_originals_digest_bound=True,
        valid_navigation=True, gallery_sha256=sha(gallery))
    publish_json(gallery_proof, proof / 'gallery.json', ROOT, review_snapshots)
    print(f'{result}: {len(capture_records)} exact native PNGs; {len(clip_records)} native clips; canonical {gallery.stat().st_size:,} bytes; standalone {args.standalone.stat().st_size:,} bytes')


if __name__ == '__main__':
    main()
