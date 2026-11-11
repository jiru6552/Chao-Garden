"""Current editable source export; old generated media remain in prior releases."""
from pathlib import Path
import importlib.util
import hashlib
import json

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('chao_release_source_helpers', ROOT / 'scripts/package_chao017.py')
helpers = importlib.util.module_from_spec(spec)
spec.loader.exec_module(helpers)
helpers.VERSION = '0.25.2'


def validate_review_closure(root=ROOT):
    """Resolve nested review references, including each clip's actual reviewer."""
    from freeze_source_cohort0252 import verify
    from review_ledger0252 import assert_frozen, read_snapshot, verify_report_refs
    if root.resolve()!=ROOT.resolve():
        raise RuntimeError('Current release closure must resolve against its own source root')
    verify()
    def check(file, expected):
        path = (root / file).resolve()
        if not path.is_relative_to(root.resolve()) or not path.is_file():
            raise RuntimeError('Missing/unsafe nested review input: ' + file)
        with path.open('rb') as stream:
            actual = hashlib.file_digest(stream, 'sha256').hexdigest()
        if actual != expected:
            raise RuntimeError('Stale nested review input: ' + file)

    snapshots={}
    for name in ('motion-visual-review', 'visual-review'):
        path=root / 'docs/validation-0.25.2' / (name + '.json')
        report,digest=read_snapshot(path)
        snapshots[path]=digest
        if report.get('release') != '0.25.2' or report.get('result') != 'PASS':
            raise RuntimeError('Incomplete nested review: ' + name)
        snapshots.update(verify_report_refs(report,root))
        for reference in report.get('review_reports', []):
            check(reference['file'], reference['sha256'])
        clips = report.get('clips', report.get('motion_sequences', []))
        for clip in clips:
            check(clip['review_report'], clip['review_report_sha256'])
    assert_frozen(snapshots)
    return snapshots


def source_files(root=ROOT):
    for path, relative in helpers.source_files(root):
        if relative in ('docs/release-gallery-0.25.0.html', 'docs/release-gallery-0.25.1.html'):
            # Its embedded media duplicate the retained native captures/clips.
            # The exact self-contained study remains in the prior saved release.
            continue
        if relative.startswith(('docs/screenshots-0.25.1/', 'docs/motion-0.25.1/')):
            # Native media are retained in the separately saved 0.25.1 complete
            # release. Avoid duplicating them in this current refinement export.
            continue
        if relative.startswith('docs/validation-0.25.1/') and path.suffix.lower() in {'.png', '.jpg', '.jpeg', '.gif', '.mp4'}:
            continue
        if relative.startswith('docs/screenshots-0.25/') and path.name!='chao025-staff-adult-baby-comparison.png':
            # Earlier broad world/room screenshots remain in the prior release.
            # Historical baby clips and timed motion frames remain below.
            continue
        if relative.startswith('docs/motion-0.25/review/') or (Path(relative).parent==Path('docs/motion-0.25') and path.suffix.lower() in {'.gif','.jpg','.jpeg'}):
            # Retain original historical baby clips and timed native frames;
            # their derived GIF/contact-sheet duplicates remain in 0.25.1.
            continue
        if relative=='docs/motion-0.25/video/baby-actual-final.mp4':
            # Its nine contiguous native baby clips and timed PNGs are retained;
            # the older full recording remains in the prior complete release.
            continue
        # Only generated historical previews are omitted. All native authoring
        # textures/models, raw imported inputs and prior textual research remain.
        if relative.startswith('docs/screenshots/') or (relative.startswith(('docs/screenshots-', 'docs/motion-')) and not relative.startswith(('docs/screenshots-0.25/', 'docs/motion-0.25/', 'docs/screenshots-0.25.2/', 'docs/motion-0.25.2/'))):
            continue
        if relative.startswith('docs/') and len(Path(relative).parts) == 2:
            if path.suffix.lower() in {'.png', '.jpg', '.jpeg', '.gif', '.mp4', '.pdf', '.html'} and '0.25' not in path.name:
                continue
        yield path, relative
