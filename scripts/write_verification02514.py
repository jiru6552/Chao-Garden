#!/usr/bin/env python3
"""Assemble release evidence only after the final build and native checks pass."""
from pathlib import Path
import argparse,hashlib,json,re,shutil
ROOT=Path(__file__).resolve().parents[1]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main(work):
 out=ROOT/'docs/validation-0.25.14';out.mkdir(exist_ok=True)
 names=['final-build.log','server-default.log','server-accessories.log','client-final.log','asset-rebuild.log']
 logs={n:(work/n).read_text() for n in names}
 assert all('BUILD SUCCESSFUL' in logs[n] for n in names[:-1])
 for n in ('server-default.log','server-accessories.log'):assert 'All 266 required tests passed' in logs[n] and 'failed at' not in logs[n]
 counts=[int(v.replace(',','')) for v in re.findall(r': ([\d,]+) checks passed',logs['final-build.log'])]
 assert len(counts)==18 and sum(counts)==1710878,(len(counts),sum(counts))
 client=logs['client-final.log']
 assert 'Missing textures in model' not in client and 'Unable to load model' not in client
 assert 'items=75 contexts=600 missingMaterials=0 finitePositiveTransforms=true' in client
 assert 'CHAO_FRUIT02512_NATIVE items=75 pods=144' in client
 assert 'scenes=13 frames=16 saves=16 returnRestored=true' in client
 assert all('CHAO_FRUIT02513_COMPLETE scene=fruit-use-'+s in client for s in ['eat','plant','water'])
 saves=re.findall(r'CHAO_FRUIT02513_SAVED file=(\S+)',client);assert len(saves)==len(set(saves)) and len(saves)>=29
 assert 'Complete Chao Garden asset rebuild passed.' in logs['asset-rebuild.log'] and 'Traceback' not in logs['asset-rebuild.log']
 replay=work/'replay';before=json.loads((work/'replay-java-before.json').read_text())
 assert all(sha(replay/k)==v for k,v in before.items()),'Pipeline rewrote Java'
 def resources(root):return {p.relative_to(root/'src/main/resources').as_posix():sha(p) for p in (root/'src/main/resources').rglob('*') if p.is_file()}
 a=resources(ROOT);b=resources(replay);assert a==b,'Asset replay differs'
 baseline=resources(Path('/tmp/chao02513-work/source'))
 added=sorted(a.keys()-baseline.keys());changed=sorted(n for n in a.keys()&baseline.keys() if a[n]!=baseline[n]);removed=sorted(baseline.keys()-a.keys())
 audio=[n for n in baseline if n.endswith('.ogg')];assert all(a[n]==baseline[n] for n in audio)
 assert not removed,'Unexpected deleted resource'
 for n in names:shutil.copyfile(work/n,out/n)
 shutil.copyfile(work/'client-plan.json',out/'client-plan.json')
 jar=ROOT/'build/libs/chao_garden-0.25.14.jar'
 report={'release':'0.25.14','result':'PASS','jar_sha256':sha(jar),'jar_bytes':jar.stat().st_size,
 'build':{'core_suites':len(counts),'core_checks':sum(counts)},
 'native_servers':{'default_passed':266,'default_required':266,'accessories_owo_passed':266,'accessories_owo_required':266},
 'client':{'items':75,'contexts':600,'missing_materials':0,'pods':144,'scenes':13,'scene_captures':16,'action_captures':len(saves),'return_restored':True,'live_action_results':['eat','plant','water'],'visual_review':'visual-review.txt'},
 'asset_replay':{'resources':len(a),'byte_identical':True,'java_files_unchanged':len(before)},
 'resource_changes':{'changed':changed,'added':added,'removed':removed,'ogg_unchanged':len(audio)},
 'source_art':json.loads((out/'source-validation.json').read_text()),
 'limits':['Fifteen foods and twenty-nine extension/compatibility seeds retain authored models; the supplied files have no confirmed matching source model.','Representative child-rig interaction and garden views; not an exhaustive evolved-form, accessory, shader, device or FOV visual matrix.','Garden layout/collision and existing gameplay retained; no new Chao motion clips imported.'],
 'logs':{n:sha(out/n) for n in names}}
 (out/'verification.json').write_text(json.dumps(report,indent=2)+'\n')
 (ROOT/'TESTING.md').write_text(f'''# Chao Garden 0.25.14 verification

Status: PASS
Target: Minecraft Java 1.21.1 / NeoForge 21.1.219 / Java 21.

## Changes

- Adapted 22 food models, seven Battle seed models, and the shovel/watering can from the supplied SA2 files, with matching native meshes, inventory sprites, pods and seedlings.
- Original texture repeats, mirrors and transparency are baked safely inside Minecraft's atlas; fine sprout connectors are thickened for readable inventory icons.
- Five placed shovel poses match the new tool. Chao-held shovel scale now matches its item geometry.
- Eighteen garden materials cover palm bark/end grain, Neutral terrain and plants, Hero masonry/trim/emblem, and Dark decorations. Corner trim joins continuously. Existing layouts, collision, gameplay and saved identities remain intact.

## Completed checks

- Final build: 18 core suites, 1,710,878 assertions passed.
- Default native server: 266/266 required tests passed.
- Accessories 1.1.0-beta.54+1.21.1 + owo 0.12.15.6-beta.1+1.21: 266/266 required tests passed.
- Ordinary Minecraft client: 75 food/seed/tool items across eight contexts (600 combinations), 144 growing pod states and all registered block states; no missing item materials or invalid transforms.
- Native inventory/mesh grids, held/dropped objects, actual Chao eating/planting/watering, and all three gardens reviewed: 13 scenes, 16 scene captures and {len(saves)} action captures. All three gameplay action results verified and the ordinary return restored.
- Offline reference extraction reproduces the curated inputs exactly, including named texture assignments and model UV data. Source hashes and format references are included.
- Full independent asset rebuild: all {len(a):,} resource files reproduced byte for byte; {len(before)} Java inputs unchanged. All {len(audio)} existing OGG recordings are unchanged.
- Source/model validation, finite geometry, atlas bounds, native bindings, connected seed icons, garden material/trim contracts, and the full existing asset validator suite passed.

## Package integrity

Installable JAR: {jar.name}
Bytes: {jar.stat().st_size}
SHA-256: {sha(jar)}

The packager verifies every production class and runtime resource against the final JAR, ZIP CRCs, safe paths, archived source hashes and executable Gradle wrapper permissions. Test/visual-harness classes and the development dimension preset are excluded from the installable JAR.

## Practical limits

Fifteen additional foods and twenty-nine extension/compatibility seeds retain their established authored models because these files contain no confirmed matching model. Native visual review covers representative child-rig interactions and garden viewpoints, not every evolved form, animal part, accessory, shader, FOV or device. Android was not tested. This release does not import new Chao animation clips or alter garden layouts/collision.

Replace the old JAR on both client and server. No extra game files or resource pack are needed. The complete ZIP includes source, selected original reference inputs, extraction/generation scripts and verification. No preview gallery is included.
''')
 print(json.dumps({k:report[k] for k in ('release','result','jar_sha256','build','native_servers','client','asset_replay')},indent=2))
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('work',type=Path);main(p.parse_args().work.resolve())
