#!/usr/bin/env python3
"""Decode labelled native-frame contacts for review; never declares visual PASS."""
from pathlib import Path
import argparse
import hashlib
import json
import math
import re
import subprocess
import tempfile
from fractions import Fraction
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--provenance', type=Path, default=ROOT / 'docs/motion-0.25.1/video/motion-provenance.json')
    parser.add_argument('--output', type=Path, default=ROOT / 'docs/motion-0.25.1/review')
    parser.add_argument('--log', type=Path, default=ROOT / 'docs/validation-0.25.1/world-capture.txt')
    parser.add_argument('--plan', type=Path, default=ROOT / 'docs/release-scenes-0.25.1.json')
    args = parser.parse_args()
    provenance = json.loads(args.provenance.read_text())
    plan = json.loads(args.plan.read_text())
    assert sha(args.plan) == provenance['scene_plan_sha256']
    action_keys = {row['key'] for row in plan['scenes'] if row.get('action')}
    log = args.log.read_text()
    assert sha(args.log) == provenance['source_log_sha256']
    args.output.mkdir(parents=True, exist_ok=True)
    font = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 15)
    rows = []
    for clip in provenance['clips']:
        video = args.provenance.parent / clip['file']
        assert sha(video) == clip['sha256']
        probe = json.loads(subprocess.check_output(['ffprobe', '-v', 'error', '-select_streams', 'v:0',
            '-show_entries', 'stream=width,height,r_frame_rate,avg_frame_rate,nb_frames,duration', '-of', 'json', str(video)], text=True))['streams'][0]
        assert (probe['width'], probe['height']) == (1280, 720)
        assert 28 <= float(Fraction(probe['avg_frame_rate'])) <= 32
        count = int(probe['nb_frames'])
        timestamps = [float(row['best_effort_timestamp_time']) for row in json.loads(subprocess.check_output(
            ['ffprobe', '-v', 'error', '-select_streams', 'v:0', '-show_entries',
             'frame=best_effort_timestamp_time', '-of', 'json', str(video)], text=True))['frames']]
        assert len(timestamps) == count and all(a < b for a, b in zip(timestamps, timestamps[1:]))
        # Door prediction must also be checked for brief partially rebuilt
        # segments. Preserve every native frame of every actual action clip.
        timeline = list(range(count)) if clip['scene'] in action_keys else sorted(set(range(0, count, 5)) | {count - 1})
        # Actual consecutive input frames, never optical-flow/interpolated frames.
        dense_start = min(15, max(0, count - 24))
        dense_groups = [list(range(dense_start, min(count, dense_start + 24)))]
        sampled_events = []
        for event_name in ('ACTION_SENT', 'TRAVEL_ARRIVED', 'FORTUNE_INTERACT'):
            match = re.search(r'CHAO_WORLD0251_' + event_name + r' scene=' + re.escape(clip['scene']) +
                              r' [^\n]*?wallClockMillis=(\d+)', log)
            if match:
                event_pts = int(match[1]) / 1000 - provenance['first_x11_input_epoch_seconds'] - clip['source_start_seconds']
                start = max(0, min(count - 24, round(event_pts * 30) - 6))
                dense_groups.append(list(range(start, min(count, start + 24))))
                sampled_events.append(dict(event=event_name, receipt=match[0], approximate_clip_pts=round(event_pts, 6),
                                           consecutive_group=len(dense_groups) - 1))
                if event_name == 'FORTUNE_INTERACT':
                    start = max(0, min(count - 24, round((event_pts + .8) * 30)))
                    dense_groups.append(list(range(start, min(count, start + 24))))
        if clip['scene'] == 'staff-fortune-no-blink':
            for old_period in (8.65, 17.3):
                start = max(0, min(count - 24, round(old_period * 30) - 12))
                dense_groups.append(list(range(start, min(count, start + 24))))
        selected = sorted(set(timeline).union(*dense_groups))
        with tempfile.TemporaryDirectory(prefix='chao0251-native-review-') as temporary:
            temp = Path(temporary)
            select = '+'.join('eq(n\\,%d)' % index for index in selected)
            subprocess.run(['ffmpeg', '-v', 'error', '-i', str(video), '-vf',
                'select=' + select + ',scale=320:180:flags=lanczos,setpts=N/TB',
                '-fps_mode', 'passthrough', '-enc_time_base', '1:1',
                '-q:v', '2', str(temp / '%05d.jpg')], check=True)
            images = sorted(temp.glob('*.jpg'))
            assert len(images) == len(selected)
            lookup = dict(zip(selected, images))
            groups = [('timeline', timeline)] + [('consecutive-%02d' % i, indices) for i, indices in enumerate(dense_groups)]
            sheets = []
            for group, indices in groups:
                for page in range(math.ceil(len(indices) / 12)):
                    batch = indices[page * 12:(page + 1) * 12]
                    sheet = Image.new('RGB', (1280, 678), '#0b1720')
                    draw = ImageDraw.Draw(sheet)
                    draw.text((12, 8), clip['scene'] + ' / ' + group + ' / actual native-frame samples', font=font, fill='white')
                    for position, index in enumerate(batch):
                        x, y = (position % 4) * 320, 36 + (position // 4) * 214
                        with Image.open(lookup[index]) as image:
                            sheet.paste(image, (x, y))
                        draw.text((x + 5, y + 183), 'frame %d  PTS %.3fs' % (index, timestamps[index]), font=font, fill='#b7e9f4')
                    target = args.output / (clip['scene'] + '-' + group + '-%02d.jpg' % page)
                    sheet.save(target, quality=94)
                    sheets.append(dict(file=target.name, sha256=sha(target), group=group, frame_indices=batch,
                                       clip_pts_seconds=[timestamps[index] for index in batch]))
        rows.append(dict(key=clip['scene'], clip=clip['file'], sha256=clip['sha256'], native_frame_count=count,
                         timeline_stride=1 if clip['scene'] in action_keys else 5,
                         timeline_frame_indices=timeline, consecutive_frame_groups=dense_groups,
                         sampled_receipt_events=sampled_events, unique_sampled_frame_indices=selected, sheets=sheets))
        print(clip['scene'], count, 'native frames;', len(selected), 'sampled;', len(sheets), 'contact sheets')
    result = dict(release='0.25.1', result='GENERATED_REVIEW_AIDS_ONLY',
        method='Each contact uses decoded source MP4 frame indices and actual source PTS. Action-clip timelines include every native frame to expose partial door rebuilding; other timelines sample every fifth native frame plus last. Dense groups use consecutive native frames, including receipt-timed door toggles/travel arrivals/Fortune interaction where present. Receipt timing locates approximate windows; recorded frame indices are exact. Temporary JPEG encoding uses ordinal timestamps only; it creates no video and changes no native clip. Thumbnails are scaled only for review sheets. This generator does not inspect pictures or declare visual PASS, real-time playback, or all-frame review.',
        scene_plan_sha256=sha(args.plan),
        source_provenance_sha256=sha(args.provenance), clips=rows)
    (args.output / 'review-aid-index.json').write_text(json.dumps(result, indent=2) + '\n')


if __name__ == '__main__':
    main()
