"""Current editable source export; old generated media remain in prior releases."""
from pathlib import Path
import importlib.util

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('chao_release_source_helpers', ROOT / 'scripts/package_chao017.py')
helpers = importlib.util.module_from_spec(spec)
spec.loader.exec_module(helpers)
helpers.VERSION = '0.25.0'


def source_files(root=ROOT):
    for path, relative in helpers.source_files(root):
        # Only generated historical previews are omitted. All native authoring
        # textures/models, raw imported inputs and prior textual research remain.
        if relative.startswith('docs/screenshots/') or (relative.startswith(('docs/screenshots-', 'docs/motion-')) and not relative.startswith(('docs/screenshots-0.25/', 'docs/motion-0.25/'))):
            continue
        if relative.startswith('docs/') and len(Path(relative).parts) == 2:
            if path.suffix.lower() in {'.png', '.jpg', '.jpeg', '.gif', '.mp4', '.pdf', '.html'} and '0.25' not in path.name:
                continue
        yield path, relative
