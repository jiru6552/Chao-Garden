#!/usr/bin/env python3
"""Freeze the verified distribution release and retain earlier model references."""
from pathlib import Path
import argparse, fnmatch, hashlib, importlib.util, json, os, posixpath, re, zipfile

ROOT=Path(__file__).resolve().parents[1]
VERSION='0.23.0'
spec=importlib.util.spec_from_file_location('release_helpers',ROOT/'scripts/package_chao017.py')
helpers=importlib.util.module_from_spec(spec);spec.loader.exec_module(helpers)
helpers.VERSION=VERSION

def sha(path):
    with path.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()

def require(value,message):
    if not value:raise RuntimeError(message)

def main(out):
    out.mkdir(parents=True,exist_ok=True)
    reports={}
    for name in ('core','server-default','server-accessories','runtime-client','visual-review','production-jar','asset-rebuild'):
        path=ROOT/'docs/validation-0.23'/(name+'.json');report=json.loads(path.read_text())
        require(report.get('release')==VERSION and report.get('result')=='PASS','Unfinished fresh validation: '+name)
        for source in report.get('sources',[]):
            require(sha(ROOT/source['file'])==source['sha256'],'Changed evidence input: '+source['file'])
        reports[name]=sha(path)
    jar=ROOT/f'build/libs/chao_garden-{VERSION}.jar'
    helpers.validate_production_jar(jar,ROOT)
    with zipfile.ZipFile(jar) as z:
        names=z.namelist()
        require(len(names)==len(set(names)),'Duplicate JAR paths')
        require(not any('Audit023' in n or 'VisualStudy023' in n for n in names),'Study helper in production')
        for needed in ('com/chaogarden/distribution/ChestEggLootModifier.class','com/chaogarden/client/CompanionBackLayer.class'):
            require(needed in names,'Missing distribution/flight behavior: '+needed)
        for name in names:
            if name.startswith('data/chao_garden/recipe/') and name.endswith('.json'):
                result=json.loads(z.read(name)).get('result',{})
                item=result if isinstance(result,str) else result.get('id',result.get('item',''))
                require(not(item.startswith('chao_garden:') and item.endswith('_egg')),'Egg crafting returned: '+name)
        classes={n for n in names if n.startswith('com/chaogarden/') and n.endswith('.class')}
        patterns=re.findall(r"(?m)^\s*exclude '([^']+)'",(ROOT/'build.gradle').read_text())
        compiled=ROOT/'build/classes/java/main'
        expected={p.relative_to(compiled).as_posix() for p in (compiled/'com/chaogarden').rglob('*.class')
                  if not any(fnmatch.fnmatchcase(p.relative_to(compiled).as_posix(),pat) for pat in patterns)}
        require(classes==expected,'Incomplete or unexpected production class set')
    entries=list(helpers.source_files(ROOT));historical=ROOT/'docs/chao-garden-0.22.0-preview-gallery.pdf'
    rel=historical.relative_to(ROOT).as_posix()
    if rel not in {n for _,n in entries}:entries.append((historical,rel))
    entries.sort(key=lambda row:row[1]);snapshot={rel:sha(p) for p,rel in entries}
    prefix=f'Chao-Garden-{VERSION}/';target=out/f'chao-garden-{VERSION}-complete.zip';stage=target.with_suffix('.zip.building')
    helpers.copy_flushed(jar,out/jar.name)
    start=(f'CHAO GARDEN {VERSION}\nMinecraft Java 1.21.1 / NeoForge21.1.219 / Java21\n\n'
           f'Install {jar.name} in mods/ on the client and server, replacing the prior JAR.\n'
           'Accessories and its dependency are optional and are not bundled.\n\n'
           'Eggs cannot be crafted. All16 ordinary base colors are always sold by visiting\n'
           'Black Market Chao; one shiny rotates and gems are extremely rare.\n'
           'See source/docs/release-0.23.0.md for exact loot rules and Moon configuration.\n'
           'End companion Ender Chest key: V (rebindable). Omochao boost: R.\n\n'
           'source/README.md: installation and gameplay\nsource/TESTING.md: fresh validation and scope\n'
           'previews/index.html: current actual game views\n'
           'source/docs/chao-garden-0.22.0-preview-gallery.pdf: historical anatomy reference\n'
           'source/: full editable Gradle project and supplied asset-only authoring inputs\n')
    landing=('<!doctype html><meta charset="utf-8"><title>Chao Garden0.23.0 previews</title>'
             '<h1>Chao Garden0.23.0</h1><p><a href="../source/docs/release-gallery-0.23.0.html">'
             'Open current companion flight previews</a></p><p><a href="../source/docs/release-gallery-0.22.0.html">'
             'Earlier model, item and lifecycle references (0.22)</a></p>')
    with zipfile.ZipFile(stage,'w',zipfile.ZIP_DEFLATED,compresslevel=9,allowZip64=True) as z:
        z.writestr(prefix+'START-HERE.txt',start);z.write(jar,prefix+jar.name)
        z.writestr(prefix+'SHA256SUMS.txt',sha(jar)+'  '+jar.name+'\n')
        z.writestr(prefix+'previews/index.html',landing)
        for p,rel in entries:z.write(p,prefix+'source/'+rel)
        for p in sorted((ROOT/'art/blockbench').glob('*.bbmodel')):z.write(p,prefix+'Blockbench/'+p.name)
        z.write(ROOT/'art/blockbench/README.md',prefix+'Blockbench/EDITING.md')
    with zipfile.ZipFile(stage) as z:
        names=z.namelist();require(len(names)==len(set(names)) and z.testzip() is None,'ZIP duplicate or CRC failure')
        for name in names:require(not name.startswith('/') and '..' not in Path(name).parts,'Unsafe ZIP path')
        for p,rel in entries:
            require(sha(p)==snapshot[rel],'Source changed during freeze: '+rel)
            require(hashlib.sha256(z.read(prefix+'source/'+rel)).hexdigest()==snapshot[rel],'ZIP payload mismatch: '+rel)
        require(z.read(prefix+jar.name)==jar.read_bytes(),'Bundled JAR differs')
        for rel in ('docs/release-gallery-0.23.0.html','docs/release-gallery-0.22.0.html'):
            location=prefix+'source/'+rel
            for src in re.findall(r'<img[^>]+src=["\']([^"\']+)["\']',z.read(location).decode()):
                if not src.startswith('data:'):
                    require(posixpath.normpath(posixpath.join(posixpath.dirname(location),src)) in names,'Broken image: '+src)
    with stage.open('rb') as f:os.fsync(f.fileno())
    os.replace(stage,target);helpers.fsync_directory(out)
    result=dict(release=VERSION,result='PASS',archive=target.name,size_bytes=target.stat().st_size,sha256=sha(target),
                entries=len(names),source_files=len(entries),source_payloads_compared=len(entries),jar=jar.name,
                jar_sha256=sha(jar),jar_size_bytes=jar.stat().st_size,production_classes=len(classes),validation_reports=reports)
    helpers.write_flushed(out/f'chao-garden-{VERSION}-package-audit.json',(json.dumps(result,indent=2)+'\n').encode())
    print(json.dumps(result,indent=2))

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--output-dir',type=Path,default=ROOT.parent/'deliverables')
    main(parser.parse_args().output_dir.resolve())
