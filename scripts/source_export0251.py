"""Current editable source export; old generated media remain in prior releases."""
from pathlib import Path
import importlib.util
import hashlib
import json

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('chao_release_source_helpers', ROOT / 'scripts/package_chao017.py')
helpers = importlib.util.module_from_spec(spec)
spec.loader.exec_module(helpers)
helpers.VERSION = '0.25.1'


def validate_review_closure(root=ROOT):
    """Resolve nested review references, including each clip's actual reviewer."""
    def check(file, expected):
        path = (root / file).resolve()
        if not path.is_relative_to(root.resolve()) or not path.is_file():
            raise RuntimeError('Missing/unsafe nested review input: ' + file)
        with path.open('rb') as stream:
            actual = hashlib.file_digest(stream, 'sha256').hexdigest()
        if actual != expected:
            raise RuntimeError('Stale nested review input: ' + file)

    for name in ('motion-visual-review', 'visual-review'):
        report = json.loads((root / 'docs/validation-0.25.1' / (name + '.json')).read_text())
        if report.get('release') != '0.25.1' or report.get('result') != 'PASS':
            raise RuntimeError('Incomplete nested review: ' + name)
        for reference in report.get('review_reports', []):
            check(reference['file'], reference['sha256'])
        clips = report.get('clips', report.get('motion_sequences', []))
        for clip in clips:
            check(clip['review_report'], clip['review_report_sha256'])


def source_files(root=ROOT):
    for path, relative in helpers.source_files(root):
        if relative == 'docs/release-gallery-0.25.0.html':
            # Its embedded media duplicate the retained native captures/clips.
            # The exact self-contained study remains in the prior saved release.
            continue
        # Only generated historical previews are omitted. All native authoring
        # textures/models, raw imported inputs and prior textual research remain.
        if relative.startswith('docs/screenshots/') or (relative.startswith(('docs/screenshots-', 'docs/motion-')) and not relative.startswith(('docs/screenshots-0.25/', 'docs/motion-0.25/', 'docs/screenshots-0.25.1/', 'docs/motion-0.25.1/'))):
            continue
        if relative.startswith('docs/') and len(Path(relative).parts) == 2:
            if path.suffix.lower() in {'.png', '.jpg', '.jpeg', '.gif', '.mp4', '.pdf', '.html'} and '0.25' not in path.name:
                continue
        yield path, relative
