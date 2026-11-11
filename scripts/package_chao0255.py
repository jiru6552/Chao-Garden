#!/usr/bin/env python3
"""Package the verified production JAR and complete editable project."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import tempfile
import zipfile
from source_export0255 import (ROOT, VERSION, PROOF, VERIFICATION_REPORTS, helpers,
    source_files, validate_review_closure, sha, require, assert_frozen, write_json)
from publish_release025 import publish


def main(out):
    out.mkdir(parents=True, exist_ok=True)
    frozen = validate_review_closure()
    reports = {name: json.loads((ROOT/PROOF/(name+'.json')).read_text())
               for name in VERIFICATION_REPORTS}
    jar = ROOT/f'build/libs/chao_garden-{VERSION}.jar'
    jar_hash = sha(jar)
    require(reports['production-jar']['artifact']['sha256'] == jar_hash,
            'Production artifact changed')
    require(reports['client-smoke']['artifact_sha256'] == jar_hash,
            'Native client checked another production artifact')
    for profile in ('server-default', 'server-accessories'):
        require(reports[profile]['server_tested_artifact']['sha256'] == jar_hash,
                'Server used another artifact: '+profile)
    require('verification is in progress' not in (ROOT/'TESTING.md').read_text().lower(),
            'Verification prose is still a draft')
    helpers.validate_production_jar(jar, ROOT)
    with zipfile.ZipFile(jar) as bundle:
        names = bundle.namelist()
        require(len(names) == len(set(names)) and bundle.testzip() is None,
                'Production JAR CRC/duplicate failure')
        require(not any('VisualSmoke' in name or '/test/' in name or
                    name.startswith('data/minecraft/worldgen/world_preset/') for name in names),
                'Development content leaked into production')
        for feature in ('hub/HeroGardenRooms0253', 'hub/DarkGardenRooms0255', 'hub/DarkGardenDecor0255', 'hub/LobbyApproachDecor0255', 'hub/GardenRooms025',
                        'kindergarten/KindergartenIonicColumn0254',
                        'activities/GardenTransporterBlock', 'core/SchoolDoorGeometry0255'):
            require('com/chaogarden/'+feature+'.class' in names,
                    'Missing production feature: '+feature)
        # validate_production_jar already compares every production resource
        # and compiled class byte-for-byte, excluding development fixtures.
    fixture = ROOT/'src/gameTestDimensions/resources/data/minecraft/worldgen/world_preset/flat.json'
    require(set(json.loads(fixture.read_text())['dimensions']) == {
        'minecraft:overworld', 'minecraft:the_nether', 'minecraft:the_end',
        'chao_garden:hub', 'chao_garden:kindergarten', 'chao_garden:garden',
        'chao_garden:hero_garden', 'chao_garden:dark_garden'}, 'Incorrect eight-dimension development fixture')
    require(not (ROOT/'src/main/resources/data/minecraft/worldgen/world_preset/flat.json').exists(),
            'Development preset leaked into production')
    require(reports['asset-rebuild']['dev_fixture_absent_from_main'] and
            reports['asset-rebuild']['separated_gametest_resources_unchanged'],
            'Asset replay did not verify resource isolation')
    entries = list(source_files())
    snapshot = {relative: sha(path) for path, relative in entries}
    require(len(entries) == len(snapshot), 'Duplicate source paths')
    require(publish(jar, out/jar.name) == jar_hash, 'Published JAR changed')
    start = f'''CHAO GARDEN {VERSION}
Minecraft Java 1.21.1 / NeoForge 21.1.219 / Java 21

Install {jar.name} in mods/ on client and server, replacing the old JAR.
No extra furnishing mod is required. Accessories and owo are optional.

This update opens the first playable Dark Garden, rebuilds both elevated/lower
lobby approaches with celestial stairs and distinct gateways, rounds the school
door profiles and thinner frames, and relocates the board and Fortune curtain.

Existing authored rooms advance to revision 30 when entered. Back up your world before updating.
Use /chao return for an emergency exit.

Read source/README.md, source/TESTING.md and source/docs/release-{VERSION}.md.
The complete editable source, native authoring inputs, original references and
current verification logs are included. Preview galleries are omitted as requested.
'''
    target = out/f'chao-garden-{VERSION}-complete.zip'
    staging = Path(tempfile.mkdtemp(prefix='chao0255-package-'))
    stage = staging/target.name
    prefix = f'Chao-Garden-{VERSION}/'
    try:
        with zipfile.ZipFile(stage, 'w', zipfile.ZIP_DEFLATED, compresslevel=6, allowZip64=True) as archive:
            archive.writestr(prefix+'START-HERE.txt', start)
            archive.writestr(prefix+'SOURCE-SHA256.json', json.dumps(snapshot, indent=2)+'\n')
            archive.write(jar, prefix+jar.name)
            for path, relative in entries:
                archive.write(path, prefix+'source/'+relative)
        with zipfile.ZipFile(stage) as archive:
            names = archive.namelist()
            require(len(names) == len(set(names)) and archive.testzip() is None,
                    'Archive CRC/duplicate failure')
            require(all(not name.startswith('/') and '..' not in Path(name).parts for name in names),
                    'Unsafe archive paths')
            require((archive.getinfo(prefix+'source/gradlew').external_attr >> 16) & 0o100,
                    'Gradle launcher lost executable permission')
            for path, relative in entries:
                require(sha(path) == snapshot[relative], 'Source changed during package: '+relative)
                require(hashlib.sha256(archive.read(prefix+'source/'+relative)).hexdigest() == snapshot[relative],
                        'Archived source payload changed: '+relative)
            require(hashlib.sha256(archive.read(prefix+jar.name)).hexdigest() == jar_hash,
                    'Bundled JAR changed')
        require(stage.stat().st_size < 500_000_000, 'Archive exceeds delivery budget')
        assert_frozen(frozen)
        validate_review_closure()
        publish(stage, target)
    finally:
        shutil.rmtree(staging)
    audit = dict(release=VERSION, result='PASS',
        jar=dict(file=jar.name, sha256=jar_hash, size_bytes=jar.stat().st_size),
        archive=dict(file=target.name, sha256=sha(target), size_bytes=target.stat().st_size,
                     source_payloads_compared=len(entries)),
        source_files=len(entries), source_payload_bytes=sum(path.stat().st_size for path, _ in entries),
        proof_reports={name: sha(ROOT/PROOF/(name+'.json')) for name in reports})
    write_json(audit, out/f'chao-garden-{VERSION}-package-audit.json')
    print(json.dumps(audit, indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output-dir', type=Path, default=ROOT.parent/'deliverables')
    main(parser.parse_args().output_dir.resolve())
