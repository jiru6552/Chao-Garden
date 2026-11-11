#!/usr/bin/env python3
"""Freeze or verify the code/resources/reference inputs for current native evidence."""
from pathlib import Path
import argparse
import hashlib
import json
import time
from review_ledger0252 import publish_json, read_snapshot

ROOT=Path(__file__).resolve().parents[1]
TARGET=ROOT/'docs/validation-0.25.2/source-cohort.json'

def sha(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream,'sha256').hexdigest()

def inputs():
    found=set()
    for directory in ('src','scripts','art'):
        for path in (ROOT/directory).rglob('*'):
            if not path.is_file() or '__pycache__' in path.parts:
                continue
            if directory=='src' or path.suffix in ('.py','.sh','.gradle'):
                found.add(path)
    found.update(path for path in (ROOT/'docs/validation-0.25.2/tooling').rglob('*')
                 if path.is_file() and path.suffix in ('.py','.sh','.gradle') and '__pycache__' not in path.parts)
    for directory in ('docs/references-0.25.2','docs/references-0.25/native-keyframes','docs/references-0.25.1/native-keyframes'):
        found.update(path for path in (ROOT/directory).rglob('*') if path.is_file())
    for name in ('build.gradle','gradle.properties','settings.gradle','gradlew','gradlew.bat',
                 'docs/release-scenes-0.25.2.json','docs/visual-review-contract-0.25.2.json',
                 'docs/REFERENCE_REFINEMENTS_0252.md'):
        path=ROOT/name
        if not path.is_file():
            raise RuntimeError('Required source-cohort input missing: '+name)
        found.add(path)
    for path in found:
        if path.is_symlink() or not path.resolve().is_relative_to(ROOT):
            raise RuntimeError('Unsafe source-cohort input: '+str(path))
    return sorted(found,key=lambda p:p.relative_to(ROOT).as_posix())

def snapshot():
    return [{'file':p.relative_to(ROOT).as_posix(),'sha256':sha(p)} for p in inputs()]

def verify():
    report,digest=read_snapshot(TARGET)
    if report.get('release')!='0.25.2' or report.get('status')!='FROZEN' or report['sources']!=snapshot():
        raise RuntimeError('Current source cohort changed; fresh evidence is required for changed inputs')
    return digest,report

def main(check=False):
    if check:
        digest,report=verify()
    else:
        sources=snapshot()
        report={'release':'0.25.2','status':'FROZEN','captured_ns':time.time_ns(),
                'source_count':len(sources),'sources':sources,
                'scope':'Exact current src payloads, authoring and validation scripts, build/version inputs, native study tooling/plan, user reference originals and fixed visual obligations. Generated evidence, reports, galleries, release prose and archives are outputs and are excluded to avoid cycles.'}
        digest=publish_json(report,TARGET,ROOT,{ROOT/row['file']:row['sha256'] for row in sources})
        verify()
    print(json.dumps({'release':'0.25.2','status':'FROZEN','source_count':report['source_count'],'cohort_sha256':digest,'verified':check}))

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--verify',action='store_true')
    main(parser.parse_args().verify)
