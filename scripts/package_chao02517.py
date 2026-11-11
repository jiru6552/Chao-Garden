#!/usr/bin/env python3
"""Package current verified source and its installable JAR; retain original reference inputs."""
from pathlib import Path
import argparse, fnmatch, hashlib, json, re, shutil, zipfile
ROOT=Path(__file__).resolve().parents[1]
VERSION='0.25.17'
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def main(out):
    report=json.loads((ROOT/'docs/validation-0.25.17/verification.json').read_text())
    assert report['result']=='PASS' and report['release']==VERSION
    jar=ROOT/f'build/libs/chao_garden-{VERSION}.jar'
    assert report['jar_sha256']==sha(jar)
    assert (ROOT/'TESTING.md').read_text().startswith('# Chao Garden 0.25.17 verification')
    exclusions=re.findall(r"(?m)^\s*exclude '([^']+)'",(ROOT/'build.gradle').read_text())
    with zipfile.ZipFile(jar) as z:
        names=z.namelist()
        assert len(names)==len(set(names)) and z.testzip() is None
        assert not any('/test/' in n or 'VisualSmoke' in n or n=='data/minecraft/worldgen/world_preset/flat.json' for n in names)
        compiled=ROOT/'build/classes/java/main'
        expected_classes={p.relative_to(compiled).as_posix() for p in compiled.rglob('*.class')
                          if not any(fnmatch.fnmatchcase(p.relative_to(compiled).as_posix(),pat) for pat in exclusions)}
        assert {n for n in names if n.endswith('.class')}==expected_classes
        count=0
        for n in names:
            if n.endswith('.class'):
                p=ROOT/'build/classes/java/main'/n
                assert p.read_bytes()==z.read(n),n
                count+=1
            elif n.startswith(('assets/','data/')) and not n.endswith('/'):
                p=ROOT/'src/main/resources'/n
                assert p.read_bytes()==z.read(n),n
        expected={p.relative_to(ROOT/'src/main/resources').as_posix() for p in (ROOT/'src/main/resources').rglob('*') if p.is_file()
                  and not any(fnmatch.fnmatchcase(p.relative_to(ROOT/'src/main/resources').as_posix(),pat) for pat in exclusions)}
        actual={n for n in names if n.startswith(('assets/','data/')) and not n.endswith('/')}
        assert {n for n in expected if n.startswith(('assets/','data/'))}==actual
    entries=[]
    for p in sorted(ROOT.rglob('*')):
        rel=p.relative_to(ROOT)
        if rel.parts[0] in {'build','run','.gradle','.git'}:continue
        if any(x in {'.rsync-tmp','__pycache__'} for x in rel.parts) or p.suffix=='.pyc':continue
        if p.is_file():
            assert not p.is_symlink()
            entries.append((p,rel.as_posix()))
    snapshot={n:sha(p) for p,n in entries}
    out.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(jar,out/jar.name)
    target=out/f'chao-garden-{VERSION}-complete.zip';temporary=target.with_suffix('.zip.tmp')
    prefix=f'Chao-Garden-{VERSION}/'
    start=f"""CHAO GARDEN {VERSION}
Minecraft Java 1.21.1 / NeoForge 21.1.219 / Java 21

Replace the previous mod JAR with {jar.name} on client and server.
Activate a Chao Garden from its UI after enclosing it with fences or walls
and growing a regular, Hero or Dark Garden fruit tree inside. The scan covers
the device chunk and its eight neighbors (3x3 chunks). These trees never wither.
Authored garden dimensions already cover their whole garden, without a scan.
Drag carried Chao into six registration boxes. The items stay in your inventory.
Release registered Chao inside the garden to make them residents; left-click
an occupied registration box to unregister that Chao.
One karate mat places a complete 7x7 carpet centered on the clicked position.
The Keeper's Journal now has broad chapters, sections and a search field.
Unneeded item tooltips are removed; benefits, requirements, toy descriptions,
lens effects and other blue descriptions remain. Robo Hearts explain rebuilding.
Garden Bells are retired; saved Bells become Chao Gardens. Old robot components
become Parts Boxes, and dead Omochao now drop a Parts Box.
Accordion texture-edge speckling and coffin lid texture bleeding are corrected.
The music disc uses a vanilla-style sprite.
Five supplied SA2 area themes and six classroom singing tracks are bundled.
No separate resource pack is required. Details: source/docs/SA2-AUDIO-IMPORT.md.
Current release details and validation are in source/docs/release-{VERSION}.md
and source/TESTING.md. No new preview gallery is included.
"""
    with zipfile.ZipFile(temporary,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        z.writestr(prefix+'START-HERE.txt',start)
        z.writestr(prefix+'SOURCE-SHA256.json',json.dumps(snapshot,indent=2)+'\n')
        z.write(jar,prefix+jar.name)
        for p,n in entries:z.write(p,prefix+'source/'+n)
    with zipfile.ZipFile(temporary) as z:
        names=z.namelist();assert len(names)==len(set(names)) and z.testzip() is None
        assert all(not n.startswith('/') and '..' not in Path(n).parts for n in names)
        assert z.getinfo(prefix+'source/gradlew').external_attr>>16&0o100
        for p,n in entries:
            assert sha(p)==snapshot[n]
            assert hashlib.sha256(z.read(prefix+'source/'+n)).hexdigest()==snapshot[n]
        assert hashlib.sha256(z.read(prefix+jar.name)).hexdigest()==sha(jar)
    temporary.replace(target)
    (out/f'chao-garden-{VERSION}-verification.txt').write_text((ROOT/'TESTING.md').read_text().removeprefix('# '))
    print(json.dumps({'jar':str(out/jar.name),'jar_sha256':sha(jar),'archive':str(target),'archive_sha256':sha(target),
                      'archive_bytes':target.stat().st_size,'source_files':len(entries),'production_classes_compared':count},indent=2))
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('output',type=Path);main(p.parse_args().output.resolve())
