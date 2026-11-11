#!/usr/bin/env python3
"""Save a recoverable current source snapshot without claiming release validation."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import tempfile
import zipfile
from source_export025 import ROOT, source_files
from publish_release025 import publish


def sha(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def main(verified=False):
    target = ROOT.parent / 'deliverables/chao-garden-0.25.0-source-checkpoint.zip'
    target.parent.mkdir(parents=True, exist_ok=True)
    stage = Path(tempfile.mkdtemp(prefix='chao025-source-checkpoint-')) / target.name
    entries = list(source_files())
    proof_inputs = {}
    if verified:
        for name in ('core', 'server-default', 'server-accessories', 'runtime-client', 'visual-review', 'production-jar', 'asset-rebuild'):
            report_path = ROOT / 'docs/validation-0.25' / (name + '.json')
            report_payload = report_path.read_bytes()
            report = json.loads(report_payload)
            proof_inputs[report_path.relative_to(ROOT).as_posix()] = hashlib.sha256(report_payload).hexdigest()
            require(report.get('release') == '0.25.0' and report.get('result') == 'PASS', 'Incomplete current proof: ' + name)
            for source in report.get('sources', []):
                require(sha(ROOT / source['file']) == source['sha256'], 'Changed proof input: ' + source['file'])
                proof_inputs[source['file']] = source['sha256']
    # Read each file once, and bind precisely the captured bytes. Owners may
    # continue after a WIP snapshot; verified mode additionally freezes proof inputs.
    captured = {}
    with zipfile.ZipFile(stage, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
        status = 'Verified current release source; see source/TESTING.md for exact checks and scope.' if verified else 'Work in progress; current runtime and visual validation are not complete.'
        archive.writestr('RECOVERY.txt', 'Chao Garden 0.25.0 — source recovery checkpoint.\n' + status + '\nExtract source/ and use Java21 / Minecraft1.21.1 / NeoForge21.1.219.\nHistorical generated previews remain in prior saved releases.\n')
        for path, relative in entries:
            payload = path.read_bytes()
            captured[relative] = hashlib.sha256(payload).hexdigest()
            info = zipfile.ZipInfo.from_file(path, 'source/' + relative)
            archive.writestr(info, payload, compress_type=zipfile.ZIP_DEFLATED, compresslevel=6)
        archive.writestr('SOURCE-SHA256.json', json.dumps(captured, indent=2) + '\n')
    for relative, expected in proof_inputs.items():
        require(captured.get(relative) == expected and sha(ROOT / relative) == expected, 'Proof changed during checkpoint: ' + relative)
    with zipfile.ZipFile(stage) as archive:
        names = archive.namelist()
        require(len(names) == len(set(names)) and archive.testzip() is None, 'ZIP duplicate paths or CRC failure')
        require(all(not name.startswith('/') and '..' not in Path(name).parts for name in names), 'Unsafe ZIP path')
        require((archive.getinfo('source/gradlew').external_attr >> 16) & 0o100, 'Gradle launcher lost its executable permission')
        for relative, digest in captured.items():
            require(hashlib.sha256(archive.read('source/' + relative)).hexdigest() == digest, 'Checkpoint source payload differs: ' + relative)
    with stage.open('rb') as stream:
        os.fsync(stream.fileno())
    publish(stage, target)
    print(json.dumps(dict(result='VERIFIED_SOURCE_RECOVERY' if verified else 'WIP_RECOVERY', path=str(target), source_files=len(entries), size_bytes=target.stat().st_size, sha256=sha(target)), indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--verified', action='store_true', help='Require all seven current release proofs before snapshotting')
    main(parser.parse_args().verified)
