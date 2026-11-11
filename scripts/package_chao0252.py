#!/usr/bin/env python3
"""Freeze the video-referenced release only after current runtime and visual proof."""
from pathlib import Path
import argparse
import fnmatch
import hashlib
import json
import os
import re
import tempfile
import zipfile
from source_export0252 import ROOT, helpers, source_files, validate_review_closure
from publish_release025 import publish
from review_ledger0252 import assert_frozen

VERSION = '0.25.2'


def sha(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def require(value, message):
    if not value:
        raise RuntimeError(message)


def main(out):
    out.mkdir(parents=True, exist_ok=True)
    evidence = {}
    report_hashes = {}
    for name in ('core', 'server-default', 'server-accessories', 'runtime-client', 'visual-review', 'production-jar', 'asset-rebuild'):
        path = ROOT / 'docs/validation-0.25.2' / (name + '.json')
        report = json.loads(path.read_text())
        require(report.get('release') == VERSION and report.get('result') == 'PASS', 'Incomplete current proof: ' + name)
        for source in report.get('sources', []):
            require(sha(ROOT / source['file']) == source['sha256'], 'Changed evidence input: ' + source['file'])
        evidence[name] = report
        report_hashes[name] = sha(path)
    review_snapshots = validate_review_closure()
    jar = ROOT / f'build/libs/chao_garden-{VERSION}.jar'
    jar_sha256 = sha(jar)
    jar_size = jar.stat().st_size
    require(evidence['production-jar']['artifact']['sha256'] == jar_sha256, 'Final artifact differs from production proof')
    require(evidence['runtime-client']['final_production_jar_sha256'] == jar_sha256, 'Final artifact differs from native client proof')
    for profile in ('server-default', 'server-accessories'):
        require(evidence[profile]['server_tested_artifact']['sha256'] == jar_sha256, 'Final artifact differs from tested server: ' + profile)
    require('verification is in progress' not in (ROOT / 'TESTING.md').read_text().lower(), 'Testing document is still a draft')
    resources = ROOT / 'src/main/resources'
    require(not (resources / 'data/minecraft/worldgen/world_preset/flat.json').exists(), 'Development preset in ordinary resources')
    fixture = ROOT / 'src/gameTestDimensions/resources/data/minecraft/worldgen/world_preset/flat.json'
    domains = {'minecraft:overworld', 'minecraft:the_nether', 'minecraft:the_end', 'chao_garden:hub', 'chao_garden:kindergarten', 'chao_garden:garden'}
    require(set(json.loads(fixture.read_text())['dimensions']) == domains, 'Incorrect isolated six-dimension fixture')
    require(evidence['asset-rebuild']['dev_fixture_absent_from_main'] and evidence['asset-rebuild']['separated_gametest_resources_unchanged'], 'Unverified development-resource isolation')
    helpers.validate_production_jar(jar, ROOT)
    with zipfile.ZipFile(jar) as archive:
        names = archive.namelist()
        require(len(names) == len(set(names)) and archive.testzip() is None, 'JAR duplicate paths or CRC failure')
        require(not any('VisualSmoke' in n or '/test/' in n or 'PoseProbe' in n for n in names), 'Development helper in release')
        for needed in ('hub/LobbyDecor025', 'hub/GardenRooms025', 'kindergarten/KindergartenRooms025', 'kindergarten/KindergartenHallThemes0252', 'kindergarten/KindergartenHallDisplays0252', 'kindergarten/KindergartenMarket025', 'client/StaffChaoRenderer025', 'client/KindergartenScreen', 'hub/PortalScenes0251', 'core/PortalGeometry0251', 'core/LobbyGeometry0252'):
            require('com/chaogarden/' + needed + '.class' in names, 'Missing production feature: ' + needed)
        for name in names:
            if name.startswith('data/chao_garden/recipe/') and name.endswith('.json'):
                result = json.loads(archive.read(name)).get('result', {})
                item = result if isinstance(result, str) else result.get('id', result.get('item', ''))
                require(not (item.startswith('chao_garden:') and item.endswith('_egg')), 'Egg crafting returned: ' + name)
        classes = {n for n in names if n.startswith('com/chaogarden/') and n.endswith('.class')}
        patterns = re.findall(r"(?m)^\s*exclude '([^']+)'", (ROOT / 'build.gradle').read_text())
        compiled = ROOT / 'build/classes/java/main'
        expected = {p.relative_to(compiled).as_posix() for p in (compiled / 'com/chaogarden').rglob('*.class')
                    if not any(fnmatch.fnmatchcase(p.relative_to(compiled).as_posix(), pattern) for pattern in patterns)}
        require(classes == expected, 'Incomplete production class domain')
        require(all(archive.read(name) == (compiled / name).read_bytes() for name in classes), 'Stale compiled class payload')
        expected_resources = {p.relative_to(resources).as_posix() for p in resources.rglob('*')
                              if p.is_file() and p.suffix != '.bbmodel'
                              and not any(fnmatch.fnmatchcase(p.relative_to(resources).as_posix(), pattern) for pattern in patterns)}
        runtime_domain = {name for name in names if name.startswith(('assets/', 'data/')) and not name.endswith('/')}
        require(runtime_domain == {name for name in expected_resources if name.startswith(('assets/', 'data/'))}, 'Missing or stale extra runtime resources')
    manifest_path = ROOT / 'docs/release-capture-manifest-0.25.2.json'
    manifest = json.loads(manifest_path.read_text())
    require(manifest['release'] == VERSION and manifest['result'] == 'PASS' and manifest['capture_count'] == evidence['runtime-client']['native_saved_frames'] == evidence['visual-review']['reviewed_captures'], 'Unfinished current gallery')
    for record in manifest['captures']:
        require(sha(ROOT / 'docs' / record['file']) == record['sha256'], 'Changed current screenshot: ' + record['key'])
    for sequence in manifest.get('motion', []):
        require(sha(ROOT / 'docs' / sequence['file']) == sequence['sha256'], 'Changed current motion clip: ' + sequence['key'])
    entries = sorted(source_files(), key=lambda row: row[1])
    snapshot = {relative: sha(path) for path, relative in entries}
    prefix = f'Chao-Garden-{VERSION}/'
    target = out / f'chao-garden-{VERSION}-complete.zip'
    stage = Path(tempfile.mkdtemp(prefix='chao025-release-package-')) / target.name
    require(publish(jar, out / jar.name) == jar_sha256, 'JAR changed before publication')
    gallery = ROOT / 'docs/release-gallery-0.25.2.html'
    gallery_sha256 = evidence['visual-review']['offline_gallery']['sha256']
    require(sha(gallery) == gallery_sha256, 'Source gallery differs from reviewed bytes')
    standalone_gallery = out / f'chao-garden-{VERSION}-gallery.html'
    standalone_sha256 = evidence['visual-review']['standalone_gallery']['sha256']
    require(sha(standalone_gallery) == standalone_sha256, 'Standalone gallery differs from reviewed bytes')
    start = f'''CHAO GARDEN {VERSION}
Minecraft Java1.21.1 / NeoForge21.1.219 / Java21

Install {jar.name} in mods/ on client and server, replacing the previous JAR.
Accessories and its dependency are optional; no furnishing mod is required.

Walk into a placed Chao Teleporter. The blue-column arch leads to the
Kindergarten campus; the stone arch leads to the first Neutral Garden.
Walk inside the school and interact with adult staff to open their service.
All Chao World realms retain Adventure mode. The Garden supports normal Chao release, eggs, toys, fruit and activities. Sneak-right-click placed garden props with empty hands to recover them.
Its cave returns to the lobby. The central lobby transporter returns home.
Use /chao return as an emergency exit. Original game mode is restored.

previews/index.html: current Minecraft/reference comparisons and motion
source/README.md: installation, controls and gameplay
source/TESTING.md: exact fresh validation and scope
source/docs/release-0.25.2.md: changes and adaptations
source/: complete editable code/native assets/current proof/reference frames
Blockbench/: editable authored model projects

Earlier generated galleries remain in earlier saved releases. The two supplied
original recordings are represented by provenance hashes and selected native
frames; the large recordings are not duplicated in this source export.
'''
    landing = '<!doctype html><meta charset="utf-8"><title>Chao Garden 0.25.2 previews</title><h1>Chao Garden 0.25.2</h1><p><a href="../source/docs/release-gallery-0.25.2.html">Open current garden, rooms, staff, doors and portal motion</a></p>'
    with zipfile.ZipFile(stage, 'w', zipfile.ZIP_DEFLATED, compresslevel=6, allowZip64=True) as archive:
        archive.writestr(prefix + 'START-HERE.txt', start)
        archive.write(jar, prefix + jar.name)
        archive.writestr(prefix + 'SHA256SUMS.txt', jar_sha256 + '  ' + jar.name + '\n')
        archive.writestr(prefix + 'previews/index.html', landing)
        for path, relative in entries:
            archive.write(path, prefix + 'source/' + relative)
        for path in sorted((ROOT / 'art/blockbench').glob('*.bbmodel')):
            archive.write(path, prefix + 'Blockbench/' + path.name)
        archive.write(ROOT / 'art/blockbench/README.md', prefix + 'Blockbench/EDITING.md')
    with zipfile.ZipFile(stage) as archive:
        names = archive.namelist()
        require(len(names) == len(set(names)) and archive.testzip() is None, 'ZIP duplicate paths or CRC failure')
        require(all(not n.startswith('/') and '..' not in Path(n).parts for n in names), 'Unsafe ZIP path')
        require((archive.getinfo(prefix + 'source/gradlew').external_attr >> 16) & 0o100, 'Gradle launcher lost its executable permission')
        for path, relative in entries:
            require(sha(path) == snapshot[relative], 'Source changed during freeze: ' + relative)
            require(hashlib.sha256(archive.read(prefix + 'source/' + relative)).hexdigest() == snapshot[relative], 'ZIP source payload differs: ' + relative)
        require(hashlib.sha256(archive.read(prefix + jar.name)).hexdigest() == jar_sha256, 'Bundled artifact differs from tested bytes')
        current_gallery = archive.read(prefix + 'source/docs/release-gallery-0.25.2.html').decode()
        require(not re.search(r'(?:src|href)="https?://', current_gallery), 'Gallery requires a network')
    require(stage.stat().st_size < 512 * 1024 * 1024, 'Single-file transfer cap exceeded; inspect generated media duplication')
    require(sha(jar) == jar_sha256 and sha(out / jar.name) == jar_sha256, 'JAR changed during source freeze')
    require(sha(gallery) == gallery_sha256 and sha(standalone_gallery) == standalone_sha256, 'Gallery changed during source freeze')
    for name, expected_hash in report_hashes.items():
        require(sha(ROOT / 'docs/validation-0.25.2' / (name + '.json')) == expected_hash, 'Validation report changed during freeze: ' + name)
    assert_frozen(review_snapshots)
    validate_review_closure()
    with stage.open('rb') as stream:
        os.fsync(stream.fileno())
    publish(stage, target)
    result = dict(release=VERSION, result='PASS', archive=target.name, size_bytes=target.stat().st_size,
                  sha256=sha(target), entries=len(names), source_files=len(entries), source_payloads_compared=len(entries),
                  jar=jar.name, jar_sha256=jar_sha256, jar_size_bytes=jar_size,
                  production_classes=len(classes), validation_reports=report_hashes,
                  gallery=f'chao-garden-{VERSION}-gallery.html')
    helpers.write_flushed(out / f'chao-garden-{VERSION}-package-audit.json', (json.dumps(result, indent=2) + '\n').encode())
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output-dir', type=Path, default=ROOT.parent / 'deliverables')
    main(parser.parse_args().output_dir.resolve())
