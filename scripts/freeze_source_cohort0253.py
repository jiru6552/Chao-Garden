#!/usr/bin/env python3
"""Freeze or verify the current code, native assets, tooling and primary references."""
from pathlib import Path
import argparse
import json
import time
from source_export0253 import ROOT, VERSION, PROOF, REFERENCES, sha, require, read_snapshot, assert_frozen, write_json

TARGET=ROOT/PROOF/'source-cohort.json'


def inputs():
    found=set()
    # Include native master assets/imports, not merely their generated runtime
    # copies; source evidence must bind all reproducible authoring inputs too.
    for directory in ('src','scripts','art','gradle',*REFERENCES):
        base=ROOT/directory
        require(base.is_dir(),'Required source-cohort tree missing: '+directory)
        found.update(path for path in base.rglob('*') if path.is_file()
                     and not any(part in {'__pycache__','.cache'} for part in path.parts)
                     and not path.name.endswith(('.pyc','.writing','.copying','.tmp')))
    tooling=ROOT/PROOF/'tooling'
    require(tooling.is_dir(),'Current verification tooling is missing')
    found.update(path for path in tooling.rglob('*') if path.is_file()
                 and '__pycache__' not in path.parts and (path.suffix in {'.py','.sh','.gradle','.java'}
                 or path.name in {'run-chao-gradle','xvfb-package-manifest.json'}))
    for name in ('build.gradle','gradle.properties','settings.gradle','gradlew','gradlew.bat',
                 'docs/release-scenes-0.25.3.json','docs/validation-0.25.3/baseline.json',
                 'docs/GARDEN_WATERFALL_0253.md','docs/HERO_REFERENCE_0253.md'):
        path=ROOT/name;require(path.is_file(),'Required source-cohort input missing: '+name);found.add(path)
    # The scene plan already carries fixed visual obligations. Include a more
    # detailed current contract if one is provided before the source is frozen.
    contract=ROOT/'docs/visual-review-contract-0.25.3.json'
    if contract.is_file():found.add(contract)
    found.update((ROOT/'docs').glob('*0253*.md'))
    for path in found:
        require(not path.is_symlink() and path.resolve().is_relative_to(ROOT),'Unsafe source-cohort input: '+str(path))
    return sorted(found,key=lambda path:path.relative_to(ROOT).as_posix())


def snapshot():
    return [dict(file=path.relative_to(ROOT).as_posix(),size_bytes=path.stat().st_size,sha256=sha(path)) for path in inputs()]


def verify():
    report,digest=read_snapshot(TARGET)
    require(report.get('release')==VERSION and report.get('status')=='FROZEN'
            and report.get('sources')==snapshot(),'Current source cohort changed; fresh evidence is required for changed inputs')
    require(report.get('source_count')==len(report['sources']),'Source-cohort count is inconsistent')
    return digest,report


def main(check=False):
    if check:digest,report=verify()
    else:
        sources=snapshot();frozen={ROOT/entry['file']:entry['sha256'] for entry in sources}
        report=dict(release=VERSION,status='FROZEN',captured_ns=time.time_ns(),source_count=len(sources),sources=sources,
            scope='Exact current code/resources, native authoring/import assets, build inputs, verification/capture tooling, fixed scene obligations and all retained primary025/0251/0252/0253 references. Generated captures, motion, logs, PASS reports, inventories, galleries, release prose, build products and archives are outputs excluded to avoid cycles. No prior release validation chain is required.')
        assert_frozen(frozen);digest=write_json(report,TARGET);assert_frozen(frozen);verify()
    print(json.dumps(dict(release=VERSION,status='FROZEN',source_count=report['source_count'],cohort_sha256=digest,verified=check)))


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--verify',action='store_true')
    main(parser.parse_args().verify)
