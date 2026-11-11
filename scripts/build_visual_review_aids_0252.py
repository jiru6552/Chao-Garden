#!/usr/bin/env python3
"""Decode exact native clip frames into labelled review aids; never visual PASS."""
from pathlib import Path
import argparse
import bisect
import hashlib
import json
import math
import re
import subprocess
import tempfile
from fractions import Fraction
from PIL import Image, ImageDraw, ImageFont
from publish_release025 import publish
from review_ledger0252 import assert_frozen, publish_json, read_snapshot

ROOT = Path(__file__).resolve().parents[1]


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def probe_json(arguments):
    return json.loads(subprocess.check_output(['ffprobe', '-v', 'error', '-select_streams', 'v:0',
        *arguments, '-of', 'json'], text=True))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--provenance', type=Path, default=ROOT / 'docs/motion-0.25.2/video/motion-provenance.json')
    parser.add_argument('--output', type=Path, default=ROOT / 'docs/motion-0.25.2/review')
    parser.add_argument('--log', type=Path, default=ROOT / 'docs/validation-0.25.2/world-capture.txt')
    parser.add_argument('--plan', type=Path, default=ROOT / 'docs/release-scenes-0.25.2.json')
    args = parser.parse_args()
    provenance, provenance_sha = read_snapshot(args.provenance)
    plan, plan_sha = read_snapshot(args.plan)
    snapshots = {args.provenance: provenance_sha, args.plan: plan_sha, args.log: sha(args.log)}
    assert plan['release'] == '0.25.2' and plan_sha == provenance['scene_plan_sha256']
    assert snapshots[args.log] == provenance['source_log_sha256']
    scenes = {row['key']: row for row in plan['scenes']}
    action_keys = {row['key'] for row in plan['scenes'] if row.get('action') in ('door-toggle', 'door-denied-toggle')}
    # Every decoded frame is retained for action coherence and view-dependent
    # alpha/lighting/camera inspections. Longer walks/falls use honest samples.
    all_frame_keys = {row['key'] for row in plan['scenes']
        if row['key'] in action_keys or row.get('camera_path') or row.get('probe') == 'market-service'
        or row['key'].startswith('lobby-machine-')}
    for key in action_keys:
        assert int(scenes[key].get('door_parts', 15)) == 15
    log = args.log.read_text()
    args.output.mkdir(parents=True, exist_ok=True)
    font = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 15)
    rows = []
    for clip in provenance['clips']:
        key = clip['scene']
        assert key in scenes
        video = args.provenance.parent / clip['file']
        assert sha(video) == clip['sha256']
        snapshots[video] = clip['sha256']
        probe = probe_json(['-show_entries', 'stream=width,height,r_frame_rate,avg_frame_rate,nb_frames,duration', str(video)])['streams'][0]
        assert (probe['width'], probe['height']) == (1280, 720)
        actual_hz = float(Fraction(probe['avg_frame_rate']))
        assert math.isfinite(actual_hz) and actual_hz > 0
        count = int(probe['nb_frames'])
        timestamps = [float(row['best_effort_timestamp_time']) for row in probe_json(
            ['-show_entries', 'frame=best_effort_timestamp_time', str(video)])['frames']]
        assert count == clip['contiguous_source_frame_count'] == len(timestamps) == clip['source_last_frame_index'] - clip['source_first_frame_index'] + 1
        assert clip['unique_monotonic_native_pts'] and clip['maximum_relative_pts_error_seconds'] <= 1.e-5
        assert all(a < b for a, b in zip(timestamps, timestamps[1:]))
        timeline = list(range(count)) if key in all_frame_keys else sorted(set(range(0, count, 5)) | {count - 1})
        dense_groups = [] if key in all_frame_keys else [list(range(min(15, max(0, count - 24)), min(count, min(15, max(0, count - 24)) + 24)))]
        sampled_events = []
        for event_name in ('ACTION_RAY', 'ACTION_SENT', 'ACTION_ACK', 'ACTION_ROLLBACK',
                           'TRAVEL_ARRIVED', 'MARKET_INTERACT', 'MARKET_UI'):
            match = re.search(r'CHAO_WORLD0252_' + event_name + r' scene=' + re.escape(key) +
                              r' [^\n]*?wallClockMillis=(\d+)', log)
            if not match:
                continue
            event_pts = int(match[1]) / 1000 - provenance['first_x11_input_epoch_seconds'] - clip['source_start_seconds']
            # Receipt clocks locate an approximate window. Select that window
            # by actual decoded PTS, never by assuming index = seconds * 30.
            nearest = min(count - 1, bisect.bisect_left(timestamps, event_pts))
            start = max(0, min(max(0, count - 24), nearest - 6))
            indices = list(range(start, min(count, start + 24)))
            if key not in all_frame_keys and indices not in dense_groups:
                dense_groups.append(indices)
            sampled_events.append(dict(event=event_name, receipt=match[0], approximate_clip_pts=round(event_pts, 6),
                                       nearest_decoded_frame_index=nearest, native_frame_window=indices))
        selected = sorted(set(timeline).union(*dense_groups))
        with tempfile.TemporaryDirectory(prefix='chao0252-native-review-') as temporary:
            temp = Path(temporary)
            select = '+'.join('eq(n\\,%d)' % index for index in selected)
            subprocess.run(['ffmpeg', '-v', 'error', '-i', str(video), '-vf',
                'select=' + select + ',scale=320:180:flags=lanczos,setpts=N/TB',
                '-fps_mode', 'passthrough', '-enc_time_base', '1:1', '-threads', '1',
                '-q:v', '2', str(temp / '%05d.jpg')], check=True)
            images = sorted(temp.glob('*.jpg'))
            assert len(images) == len(selected)
            lookup = dict(zip(selected, images))
            # Hash the selected full decoded RGB frames before any thumbnail
            # scaling/JPEG encoding. These hashes are of this exact clip decode,
            # not an assertion that separate lossy encodes share RGB bytes.
            hashes_stage = temp / 'decoded-rgb24.framehash'
            subprocess.run(['ffmpeg', '-v', 'error', '-i', str(video), '-vf',
                'select=' + select + ',format=rgb24', '-fps_mode', 'passthrough',
                '-c:v', 'rawvideo', '-threads', '1', '-f', 'framehash', '-hash', 'sha256', str(hashes_stage)], check=True)
            hashes = []
            for line in hashes_stage.read_text().splitlines():
                if not line or line.startswith('#'):
                    continue
                fields = [field.strip() for field in line.split(',')]
                assert int(fields[-2]) == 1280 * 720 * 3 and re.fullmatch('[a-f0-9]{64}', fields[-1])
                hashes.append(fields[-1])
            assert len(hashes) == len(selected)
            hashes_target = args.output / (key + '-decoded-rgb24.framehash')
            assert_frozen(snapshots)
            publish(hashes_stage, hashes_target)
            snapshots[hashes_target] = sha(hashes_target)
            groups = [('timeline', timeline)] + [('consecutive-%02d' % i, indices) for i, indices in enumerate(dense_groups)]
            sheets = []
            for group, indices in groups:
                for page in range(math.ceil(len(indices) / 12)):
                    batch = indices[page * 12:(page + 1) * 12]
                    sheet = Image.new('RGB', (1280, 678), '#0b1720')
                    draw = ImageDraw.Draw(sheet)
                    draw.text((12, 8), key + ' / ' + group + ' / exact decoded native frame samples', font=font, fill='white')
                    for position, index in enumerate(batch):
                        x, y = (position % 4) * 320, 36 + (position // 4) * 214
                        with Image.open(lookup[index]) as image:
                            sheet.paste(image, (x, y))
                        draw.text((x + 5, y + 183), 'clip %d  source %d' % (index, clip['source_first_frame_index'] + index), font=font, fill='#b7e9f4')
                        draw.text((x + 5, y + 200), 'native clip PTS %.6fs' % timestamps[index], font=font, fill='#b7e9f4')
                    name = key + '-' + group + '-%02d.jpg' % page
                    stage = temp / ('sheet-' + name)
                    sheet.save(stage, quality=94, subsampling=0)
                    target = args.output / name
                    assert_frozen(snapshots)
                    publish(stage, target)
                    snapshots[target] = sha(target)
                    sheets.append(dict(file=target.name, sha256=snapshots[target], group=group, frame_indices=batch,
                                       source_frame_indices=[clip['source_first_frame_index'] + i for i in batch],
                                       clip_pts_seconds=[timestamps[index] for index in batch]))
        rows.append(dict(key=key, clip=clip['file'], sha256=clip['sha256'], native_frame_count=count, actual_average_capture_hz=actual_hz,
            timeline_stride=1 if key in all_frame_keys else 5, all_native_frames_in_timeline=key in all_frame_keys,
            action_parts=15 if key in action_keys else None,
            action_target_policy='Actual native camera ray selected the authored slab; isolated fixture matrix separately labelled.' if key in action_keys else None,
            timeline_frame_indices=timeline, consecutive_frame_groups=dense_groups,
            sampled_receipt_events=sampled_events, unique_sampled_frame_indices=selected,
            selected_decoded_rgb24_frame_hashes=[dict(clip_frame_index=i, source_frame_index=clip['source_first_frame_index'] + i,
                clip_pts_seconds=timestamps[i], sha256=digest) for i, digest in zip(selected, hashes)],
            decoded_rgb24_framehash_file=hashes_target.name, decoded_rgb24_framehash_sha256=snapshots[hashes_target], sheets=sheets))
        print(key, count, 'native frames;', len(selected), 'indexed samples;', len(sheets), 'contact sheets')
    result = dict(release='0.25.2', result='GENERATED_REVIEW_AIDS_ONLY',
        method='Contact labels bind exact current clip frame indices, source-contiguous frame indices, actual decoded PTS and full decoded RGB24 frame hashes. All recorded frames are included for fifteen-part native door actions, controlled camera paths, machine alpha diagnostics and resident-merchant UI; other clips use every fifth frame plus the last and declared consecutive windows. Receipt clocks only locate approximate windows; actual PTS selects the indices. Temporary JPEGs use ordinal timestamps for extraction only, creating no video. Scaled contacts are review aids, not extra native screenshots, image classification, real-time playback or automatic visual PASS. RGB hashes refer to this exact current clip decode, not equality across separate lossy encodes.',
        scene_plan_sha256=plan_sha, source_log_sha256=snapshots[args.log],
        source_provenance_sha256=provenance_sha, clips=rows)
    publish_json(result, args.output / 'review-aid-index.json', ROOT, snapshots)
    assert_frozen(snapshots)


if __name__ == '__main__':
    main()
