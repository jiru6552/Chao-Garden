#!/usr/bin/env python3
"""Fail-closed release evidence assembly for the focused 0.25.13 pass."""
from pathlib import Path
import argparse,hashlib,json,re,shutil
ROOT=Path(__file__).resolve().parents[1]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main(work):
 out=ROOT/'docs/validation-0.25.13';out.mkdir(exist_ok=True)
 logs={n:(work/n).read_text() for n in ['final-build.log','server-default.log','server-accessories.log','client-final.log','asset-rebuild.log']}
 assert all('BUILD SUCCESSFUL' in logs[n] for n in ['final-build.log','server-default.log','server-accessories.log','client-final.log'])
 for n in ['server-default.log','server-accessories.log']:
  assert 'All 266 required tests passed' in logs[n] and 'failed at' not in logs[n]
 counts=[int(v) for v in re.findall(r': ([\d,]+) checks passed',logs['final-build.log'])]
 assert len(counts)==18 and sum(counts)==1710878,(len(counts),sum(counts))
 client=logs['client-final.log']
 assert 'items=74 contexts=592 missingMaterials=0 finitePositiveTransforms=true' in client
 assert 'scenes=7 frames=10 saves=10 returnRestored=true' in client
 assert all('CHAO_FRUIT02513_COMPLETE scene=fruit-use-'+s in client for s in ['eat','plant','water'])
 saves=re.findall(r'CHAO_FRUIT02513_SAVED file=(\S+)',client);assert len(saves)==len(set(saves))==33
 replay=work/'replay'
 before=json.loads((work/'replay-java-before.json').read_text())
 assert all(sha(replay/k)==v for k,v in before.items()),'Pipeline rewrote Java'
 a={p.relative_to(ROOT/'src/main/resources').as_posix():sha(p) for p in (ROOT/'src/main/resources').rglob('*') if p.is_file()}
 b={p.relative_to(replay/'src/main/resources').as_posix():sha(p) for p in (replay/'src/main/resources').rglob('*') if p.is_file()}
 assert a==b and len(a)==7087,'Asset replay mismatch'
 assert 'Traceback' not in logs['asset-rebuild.log']
 for n in logs:shutil.copyfile(work/n,out/n)
 shutil.copyfile(work/'client-plan.json',out/'client-plan.json')
 shutil.copyfile(work/'fruit-validate.log',out/'fruit-validate.log')
 jar=ROOT/'build/libs/chao_garden-0.25.13.jar'
 report=dict(release='0.25.13',result='PASS',jar_sha256=sha(jar),jar_bytes=jar.stat().st_size,
  build=dict(core_suites=len(counts),core_checks=sum(counts),result='PASS'),
  native_servers=dict(default=dict(passed=266,required=266),accessories_owo=dict(passed=266,required=266,accessories='1.1.0-beta.54+1.21.1',owo='0.12.15.6-beta.1+1.21')),
  client=dict(items=74,contexts=592,missing_materials=0,scenes=7,scene_captures=10,action_captures=33,return_restored=True,live_action_results=['eat','plant','water'],review='visual-review.txt'),
  asset_replay=dict(resources=7087,byte_identical=True,java_files_unchanged=len(before)),
  resource_changes=dict(item_display_json=74,other_resources_byte_identical_to_02512=7013,ogg_unchanged=19),
  new_native_regressions=['Exact work-spot approach from several blocks away','Watering recovery retained after a single commit','Displacement cancels watering before commit'],
  limits=['Representative child-rig live interactions; no exhaustive evolved-form/accessory visual matrix.','All items checked structurally across eight render contexts; player third-person poses not exhaustively reviewed.','No Android, arbitrary shader/resource-pack or every-FOV coverage.','Dev-only trace and terminal-fixture cleanup were adjusted after the reviewed client run; final production code was unchanged and compiled/tested afterward.'],
  logs={n:sha(out/n) for n in logs})
 (out/'verification.json').write_text(json.dumps(report,indent=2)+'\n')
 text=f'''# Chao Garden 0.25.13 verification

Status: PASS
Target: Minecraft Java 1.21.1 / NeoForge 21.1.219 / Java 21.

## Changes

- Fruits and seeds sit higher and slightly smaller in both first-person hands; the watering can has an additional inset for its spout.
- Chao keep a real walking stride while carrying a seed.
- Gardening paths reach the intended adjacent work spot instead of stopping one block early.
- Watering faces the can toward the plant, tips it through the effect at tick 40 and retains the lowering gesture through tick 55.
- Displacement beyond the three-block reach cancels an uncommitted watering effect.

## Completed checks

- Final build: 18 core suites, 1,710,878 assertions passed.
- Default native server: 266/266 required tests passed.
- Accessories 1.1.0-beta.54+1.21.1 + owo 0.12.15.6-beta.1+1.21: 266/266 passed.
- Three new native regressions cover approach distance, single watering commit with recovery, and interruption after displacement.
- Ordinary client: all 74 food/seed/can items resolve in eight contexts (592 combinations), with finite positive transforms and zero missing materials.
- Actual first-person and dropped-item views reviewed. Real Chao eating, walking with a seed, planting and watering reviewed in disposable fixtures: 10 scene captures plus 33 action captures. All three actual action results verified; normal return restored.
- Full independent asset pipeline: all 7,087 resource files reproduced byte for byte; all {len(before)} replay Java inputs remain unchanged.
- Only 74 item-display JSON files differ from 0.25.12. All other 7,013 resources, including textures, fruit effects, garden scenery and all 19 OGG recordings, are byte-identical.

## Package integrity

Installable JAR: {jar.name}
Bytes: {jar.stat().st_size}
SHA-256: {sha(jar)}

The packager compares all production classes and runtime resources with the final JAR, validates ZIP CRCs and safe paths, verifies every archived source hash and executable wrapper permissions, and excludes test/visual-harness classes and the development dimension preset from the installable JAR.

## Practical limits

Live visual review covers representative child-rig interactions. Every item/context combination was checked structurally; this is not an exhaustive visual matrix of evolved forms, animal parts, character variants, accessories, third-person player poses, FOV settings or shaders. Android was not tested. Dev-only trace and terminal-fixture cleanup were adjusted after the reviewed client run; production changes were compiled and tested afterward. Internal screenshots/worlds are not included.

Replace the previous mod JAR on both client and server. No extra game files or resource pack are needed. The complete ZIP contains source, generators, references and verification evidence. No preview gallery is included.
'''
 (ROOT/'TESTING.md').write_text(text)
 print(json.dumps(report,indent=2))
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('work',type=Path);main(p.parse_args().work.resolve())
