#!/usr/bin/env python3
"""Deterministic editable 0.25.7 source inventory, with current proof logs intact."""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import os
import re
import tempfile

ROOT=Path(__file__).resolve().parents[1]
VERSION='0.25.7'
PROOF=Path('docs/validation-'+VERSION)
INVENTORY=PROOF/'source-inventory.json'
MAX_FILE_BYTES=500_000_000
TREES=('src','scripts','art','docs','gradle','.github')
REFERENCES=tuple('docs/references-'+v for v in ('0.25','0.25.1','0.25.2','0.25.3','0.25.4','0.25.5','0.25.6','0.25.7'))
VERIFICATION_REPORTS=('core','server-default','server-accessories','production-jar','asset-rebuild','client-smoke')
REQUIRED_LOGS=tuple(PROOF/(name+'.txt') for name in
                   ('production-build','server-default','server-accessories','asset-rebuild','client-smoke'))

# Keep the existing production-JAR validator available to the final packager.
spec=importlib.util.spec_from_file_location('chao0257_release_helpers',ROOT/'scripts/package_chao017.py')
helpers=importlib.util.module_from_spec(spec)
spec.loader.exec_module(helpers)
helpers.VERSION=VERSION


def require(condition,message):
    if not condition:raise RuntimeError(message)


def sha(path):
    with path.open('rb') as stream:return hashlib.file_digest(stream,'sha256').hexdigest()


def read_snapshot(path):
    payload=path.read_bytes();digest=hashlib.sha256(payload).hexdigest()
    require(hashlib.sha256(path.read_bytes()).hexdigest()==digest,'Input changed during read: '+str(path))
    return json.loads(payload),digest


def assert_frozen(snapshots):
    for path,digest in snapshots.items():require(sha(path)==digest,'Input changed during generation: '+str(path))


def write_json(document,target):
    target.parent.mkdir(parents=True,exist_ok=True)
    payload=(json.dumps(document,indent=2)+'\n').encode()
    with tempfile.NamedTemporaryFile(dir=target.parent,prefix=target.name+'.',suffix='.writing',delete=False) as stream:
        temporary=Path(stream.name);stream.write(payload);stream.flush();os.fsync(stream.fileno())
    try:
        require(temporary.read_bytes()==payload,'Incomplete JSON stage: '+str(target))
        os.replace(temporary,target)
    finally:temporary.unlink(missing_ok=True)
    return hashlib.sha256(payload).hexdigest()


def omitted(relative):
    """Only generated history is omitted; original reference trees are retained."""
    parts=Path(relative).parts
    if len(parts)>1 and parts[0]=='docs':
        folder=parts[1]
        if folder.startswith('references-'):return False
        if folder.startswith(('screenshots','motion-')):return True
        if folder.startswith('validation-'):return folder!='validation-'+VERSION
        if folder=='previews':return True
        if len(parts)==2:
            generated=('release-gallery-','release-scenes-','release-capture-manifest-',
                       'release-capture-record-','visual-review-contract-','pdf-source-manifest-',
                       'chao-garden-')
            if folder.startswith(generated):return VERSION not in folder
            if Path(folder).suffix.lower() in {'.png','.jpg','.jpeg','.gif','.mp4','.pdf','.html'}:
                return VERSION not in folder
    return False


def candidates(root):
    for top in TREES:
        base=root/top
        if not base.is_dir():continue
        for directory,dirs,files in os.walk(base,followlinks=False):
            dirs[:]=sorted(name for name in dirs if name not in
                {'__pycache__','.git','.gradle','.cache','node_modules','.idea','.vscode','.sonic-private-inspection'})
            for name in sorted(files):yield Path(directory)/name
    for path in root.iterdir():
        if path.is_file():yield path


def source_files(root=ROOT):
    root=root.resolve();found={};imported=set()
    for path in candidates(root):
        relative=path.relative_to(root).as_posix();name=path.name.lower()
        if omitted(relative):continue
        require(not path.is_symlink() and path.resolve().is_relative_to(root),'Unsafe source path: '+relative)
        require(not any(part in relative.lower() for part in ('entoptions','entitlement','keymaterial','.sonic-private-inspection')),'Private inspection input: '+relative)
        require(name not in {'options.txt','options.json','keys.json','key.txt'},'Options/key input: '+relative)
        require(not name.endswith(('.key','.pem','.pfx','.p12','.mcworld','.mcpack','.mctemplate')),'Private/archive input: '+relative)
        if name.endswith(('.pyc','.writing','.copying','.tmp','.zip','.gif.building')):continue
        if name.endswith('.jar') and relative!='gradle/wrapper/gradle-wrapper.jar':continue
        # Earlier helpers dropped every .log. Current recorder logs are proof
        # inputs and must survive export alongside all current validation logs.
        if name.endswith('.log') and not relative.startswith((str(PROOF)+'/', 'docs/motion-'+VERSION+'/')):continue
        require(path.stat().st_size<MAX_FILE_BYTES,'Individual source exceeds 500 MB: '+relative)
        if relative.startswith('art/imported_chao/'):
            asset=relative.removeprefix('art/imported_chao/')
            require(asset in helpers.IMPORTED_ALLOWLIST,'Unknown imported Chao input: '+relative);imported.add(asset)
        require(relative not in found,'Duplicate source path: '+relative);found[relative]=path
    require(imported==helpers.IMPORTED_ALLOWLIST,'Primary imported Chao authoring inventory changed')
    primary=set()
    for tree in ('src','art','gradle',*REFERENCES):
        primary.update(path.relative_to(root).as_posix() for path in (root/tree).rglob('*')
                       if path.is_file() and '__pycache__' not in path.parts
                       and not path.name.endswith(('.pyc','.tmp','.writing','.copying')))
    require(primary<=found.keys(),'Primary source/reference inventory omitted: '+str(sorted(primary-found.keys())[:8]))
    yield from ((found[relative],relative) for relative in sorted(found))


def check_reference(root,reference,snapshots):
    require(isinstance(reference,dict),'Malformed evidence reference')
    file=reference.get('file');expected=reference.get('sha256')
    require(isinstance(file,str) and isinstance(expected,str) and re.fullmatch('[a-f0-9]{64}',expected),'Incomplete evidence file/hash reference')
    path=root/file
    require(not Path(file).is_absolute() and path.resolve().is_relative_to(root) and not path.is_symlink() and path.is_file(),'Missing/unsafe evidence input: '+file)
    require(sha(path)==expected,'Changed evidence input: '+file);snapshots[path.resolve()]=expected


def validate_review_closure(root=ROOT):
    """Verify current known PASS reports and direct SHA-bound inputs, without old chains."""
    root=root.resolve();require(root==ROOT,'Release closure must use its own source root')
    snapshots={}
    exported={relative for _,relative in source_files(root)}
    for name in VERIFICATION_REPORTS:
        path=root/PROOF/(name+'.json');report,digest=read_snapshot(path)
        require(report.get('release')==VERSION and report.get('result')=='PASS','Incomplete current verification: '+name)
        refs=report.get('sources');require(isinstance(refs,list) and refs,'Verification has no SHA-bound evidence inputs: '+name)
        snapshots[path]=digest
        for reference in refs:
            check_reference(root,reference,snapshots)
            require(reference['file'] in exported or reference['file']==f'build/libs/chao_garden-{VERSION}.jar','Verified evidence omitted from editable source export: '+reference['file'])
    for relative in REQUIRED_LOGS:
        require(str(relative) in exported and (root/relative).is_file(),'Required proof log omitted: '+str(relative))
    assert_frozen(snapshots);return snapshots


def inventory(root=ROOT):
    root=root.resolve();entries=[]
    for path,relative in source_files(root):
        if relative==str(INVENTORY):continue # A manifest cannot hash itself.
        entries.append(dict(file=relative,size_bytes=path.stat().st_size,sha256=sha(path)))
    groups={}
    for entry in entries:
        key=entry['file'].split('/')[0];group=groups.setdefault(key,dict(files=0,size_bytes=0))
        group['files']+=1;group['size_bytes']+=entry['size_bytes']
    return dict(release=VERSION,kind='SOURCE_INVENTORY',status='INVENTORY_ONLY',
        scope='Union of editable src, native authoring/import inputs, all primary025/0251/0252/0253/0254/0255/0256/0257 references, build/tooling and current0257 evidence. Historical generated media/validation chains, caches, worlds, third-party JARs and archives are excluded. This generated inventory excludes itself and is not a test PASS.',
        file_count=len(entries),total_uncompressed_bytes=sum(entry['size_bytes'] for entry in entries),
        individual_file_limit_bytes=MAX_FILE_BYTES,groups=groups,files=entries)


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output',type=Path,default=ROOT/INVENTORY)
    parser.add_argument('--verify-evidence',action='store_true')
    args=parser.parse_args()
    if args.verify_evidence:validate_review_closure()
    report=inventory();digest=write_json(report,args.output.resolve())
    print(json.dumps(dict(release=VERSION,file_count=report['file_count'],total_uncompressed_bytes=report['total_uncompressed_bytes'],manifest=str(args.output.resolve()),sha256=digest,evidence_verified=args.verify_evidence)))
