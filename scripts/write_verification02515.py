#!/usr/bin/env python3
"""Assemble evidence only after final build, both native servers and client pass."""
from pathlib import Path
import argparse,hashlib,json,re,shutil
ROOT=Path(__file__).resolve().parents[1];VERSION='0.25.15'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def resources(root):return {p.relative_to(root/'src/main/resources').as_posix():sha(p) for p in (root/'src/main/resources').rglob('*') if p.is_file()}
def main(work):
 out=ROOT/'docs'/('validation-'+VERSION);out.mkdir(exist_ok=True)
 names=['final-build.log','server-default.log','server-accessories.log','client-final.log','asset-rebuild.log','accessory-fit.log']
 logs={n:(work/n).read_text() for n in names}
 assert all('BUILD SUCCESSFUL' in logs[n] for n in names[:4])
 for n in names[1:3]:assert 'All 268 required tests passed' in logs[n] and 'failed at' not in logs[n]
 counts=[int(v.replace(',','')) for v in re.findall(r': ([\d,]+) checks passed',logs['final-build.log'])];assert len(counts)==18 and sum(counts)==1710878
 client=logs['client-final.log'];assert 'Missing textures in model' not in client and 'Unable to load model' not in client and 'Unreported exception' not in client
 assert 'items=29 contexts=261 toyStates=1088 missingMaterials=0' in client
 assert 'items=75 contexts=600 missingMaterials=0 finitePositiveTransforms=true' in client
 assert 'CHAO_FRUIT02512_NATIVE items=75 pods=144' in client
 assert 'scenes=24 frames=27 saves=27 returnRestored=true' in client
 assert all('CHAO_FRUIT02513_COMPLETE scene=fruit-use-'+s in client for s in ['eat','plant','water'])
 assert client.count('CHAO_REFINEMENT02515_WEARABLES actors=3')==4
 saves=re.findall(r'CHAO_FRUIT02513_SAVED file=(\S+)',client);assert len(saves)==len(set(saves)) and len(saves)>=29
 assert 'Complete Chao Garden asset rebuild passed.' in logs['asset-rebuild.log'] and 'Traceback' not in logs['asset-rebuild.log']
 assert '7050 checks; 2115 runtime fits' in logs['accessory-fit.log'] and '1422 checks across all141cases' in logs['accessory-fit.log']
 replay=work/'replay';before=json.loads((work/'replay-java-before.json').read_text());assert all(sha(replay/k)==v for k,v in before.items()),'Pipeline rewrote Java'
 a,b=resources(ROOT),resources(replay);assert a==b,'Asset replay differs'
 baseline=resources(Path('/tmp/chao02514-work/source'));audio=[n for n in baseline if n.endswith('.ogg')];assert all(a[n]==baseline[n] for n in audio)
 changes={'added':sorted(a.keys()-baseline.keys()),'changed':sorted(n for n in a.keys()&baseline.keys() if a[n]!=baseline[n]),'removed':sorted(baseline.keys()-a.keys())};assert not changes['removed']
 for n in names:shutil.copyfile(work/n,out/n)
 shutil.copyfile(work/'client-plan.json',out/'client-plan.json')
 jar=ROOT/'build/libs'/f'chao_garden-{VERSION}.jar'
 report={'release':VERSION,'result':'PASS','jar_sha256':sha(jar),'jar_bytes':jar.stat().st_size,'build':{'core_suites':18,'core_checks':sum(counts)},'native_servers':{'default_passed':268,'default_required':268,'accessories_owo_passed':268,'accessories_owo_required':268},'client':{'source_items':29,'source_item_contexts':261,'toy_states':1088,'missing_materials':0,'food_seed_tool_items':75,'food_seed_tool_contexts':600,'pods':144,'scenes':24,'scene_captures':27,'action_captures':len(saves),'equipped_hat_models':12,'hoodie_colors':2,'return_restored':True,'live_action_results':['eat','plant','water'],'visual_review':'visual-review.txt'},'asset_replay':{'resources':len(a),'byte_identical':True,'java_files_unchanged':len(before)},'accessory_fit':{'geometry_contacts':7050,'runtime_fits':2115,'crest_core_eye_checks':1422,'body_cases':141},'source_art':json.loads((out/'source-validation.json').read_text()),'resource_changes':changes|{'ogg_unchanged':len(audio)},'logs':{n:sha(out/n) for n in names},'limits':['Targeted Minecraft-scale garden terraces; not a complete original stage import.','Original hats are fitted to established envelopes; custom hoodies use an adapted wool material. Unmatched custom toys/accessories retain their authored models.','Native visual review includes twelve child-rig hats, two hoodie colors, source toys, original school art and representative garden views. Not an exhaustive shader/device/evolved-form matrix; Android not tested.','No Chao motion clips or gameplay rules imported or changed.']}
 (out/'verification.json').write_text(json.dumps(report,indent=2)+'\n')
 (ROOT/'TESTING.md').write_text(f'''# Chao Garden {VERSION} verification

Status: PASS
Target: Minecraft Java 1.21.1 / NeoForge 21.1.219 / Java 21.

## Changes

Eight exact requested texture rollbacks; less noisy source grass top; ten original Kindergarten drawings/photos/posters; 17 source toys with 85 placed poses; twelve source hats and their item icons; chamfered blue/pink hoodies and sleeves with an adapted wool material. Small Neutral/Hero grass terraces follow the inspected original stage shoulders. Scenery revision 37 uses the existing migration/preflight path.

## Completed checks

- Build: 18 core suites, 1,710,878 assertions.
- Default native server: 268/268 required tests passed.
- Accessories 1.1.0-beta.54+1.21.1 + owo 0.12.15.6-beta.1+1.21: 268/268 required tests passed.
- Native client: 29 source toy/hat items in all nine display contexts (261 combinations), 1,088 toy states, 75 food/seed/tool items in eight contexts (600 combinations), 144 growing pods and the registered blockstate audit. No missing model materials or invalid transforms.
- Visual review: 24 scenes, 27 scene captures and {len(saves)} action captures. All twelve hats were actually equipped on Chao with blue/pink hoodies. Original school art and all three garden material passes were inspected. Eating, planting and watering completed through native gameplay; ordinary return was restored.
- Independent wearable fitting: 7,050 geometry contact checks across 2,115 runtime fits; 1,422 crest/core/eye checks across 141 body cases.
- Asset rebuild: all {len(a):,} resources reproduced byte for byte; {len(before)} Java inputs unchanged. All {len(audio)} existing OGG recordings unchanged. Full existing resource-validator suite passed.
- Source checks: rollback PNG identity, source input hashes, item/icon agreement, original artwork identity, mesh/atlas bounds, toy collision envelopes and unchanged hat/hoodie fitting envelopes.

## Package integrity

JAR: {jar.name}
Bytes: {jar.stat().st_size}
SHA-256: {sha(jar)}

The packager verifies every production class/resource against the final JAR, source hashes, safe ZIP paths, CRCs and executable Gradle wrapper permissions. Test/probe classes and the development dimension fixture are excluded from the installable JAR.

## Practical limits

Garden changes are targeted voxel terraces, not a complete original-stage transplant. Source hats are adapted to the existing Chao attachment envelopes. Hoodies are custom mod garments with a source-material adaptation. Unmatched toys/accessories keep their existing authored assets. Native review covers representative scenes and child-rig outfits, not every evolved form, shader, FOV or device; Android was not tested. No new Chao motion clips or gameplay rules are included.

Replace the old JAR on both client and server. No separate resource pack or game files are required. The complete ZIP contains source, selected original inputs, extraction/generation scripts and evidence.
''')
 print(json.dumps({k:report[k] for k in ('release','result','jar_sha256','build','native_servers','client','asset_replay')},indent=2))
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('work',type=Path);main(p.parse_args().work.resolve())
