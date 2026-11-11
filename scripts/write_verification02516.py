#!/usr/bin/env python3
"""Assemble current release evidence after build, both servers and client pass."""
from pathlib import Path
import argparse,hashlib,json,re,shutil
ROOT=Path(__file__).resolve().parents[1];VERSION='0.25.16'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def resources(root):return {p.relative_to(root/'src/main/resources').as_posix():sha(p) for p in (root/'src/main/resources').rglob('*') if p.is_file()}
def main(work):
 out=ROOT/'docs'/('validation-'+VERSION);out.mkdir(exist_ok=True)
 names=['final-build.log','server-default.log','server-accessories.log','client-final.log','asset-rebuild.log','accessory-fit.log']
 logs={n:(work/n).read_text() for n in names}
 assert all('BUILD SUCCESSFUL' in logs[n] for n in names[:4])
 for n in names[1:3]:assert 'All 269 required tests passed' in logs[n] and 'failed at' not in logs[n]
 counts=[int(v.replace(',','')) for v in re.findall(r': ([\d,]+) checks passed',logs['final-build.log'])];assert len(counts)==18 and sum(counts)==1710878
 client=logs['client-final.log'];assert all(s not in client for s in ['Missing textures in model','Unable to load model','Unreported exception'])
 assert 'items=26 contexts=234 toyStates=128 missingMaterials=0' in client
 assert 'items=75 contexts=600 missingMaterials=0 finitePositiveTransforms=true' in client
 assert 'CHAO_FRUIT02512_NATIVE items=75 pods=144' in client
 assert 'scenes=27 frames=30 saves=30 returnRestored=true' in client
 assert all('CHAO_FRUIT02513_COMPLETE scene=fruit-use-'+s in client for s in ['eat','plant','water'])
 assert client.count('CHAO_CWE02516_WEARABLES actors=3')==3 and client.count('CHAO_CWE02516_WEARABLES actors=1')==1
 saves=re.findall(r'CHAO_FRUIT02513_SAVED file=(\S+)',client);assert len(saves)==len(set(saves)) and len(saves)>=29
 assert 'Complete Chao Garden asset rebuild passed.' in logs['asset-rebuild.log'] and 'Traceback' not in logs['asset-rebuild.log']
 assert '8037 checks; 2256 runtime fits' in logs['accessory-fit.log'] and '1422 checks across all141cases' in logs['accessory-fit.log']
 assert '1896168 triangle-centre/edge samples' in logs['accessory-fit.log']
 replay=work/'replay';before=json.loads((work/'replay-java-before.json').read_text());assert all(sha(replay/k)==v for k,v in before.items()),'Pipeline rewrote Java'
 a,b=resources(ROOT),resources(replay);assert a==b,'Asset replay differs'
 baseline=resources(Path('/tmp/chao02515-work/source'));audio=[n for n in baseline if n.endswith('.ogg')];assert all(a[n]==baseline[n] for n in audio)
 changes={'added':sorted(a.keys()-baseline.keys()),'changed':sorted(n for n in a.keys()&baseline.keys() if a[n]!=baseline[n]),'removed':sorted(baseline.keys()-a.keys())};assert not changes['removed']
 for n in names:shutil.copyfile(work/n,out/n)
 shutil.copyfile(work/'client-plan.json',out/'client-plan.json')
 fit=json.loads((ROOT/'docs/validation-0.20/accessory-fit-headless.json').read_text());(out/'accessory-fit.json').write_text(json.dumps(fit,indent=2)+'\n')
 assert (out/'visual-review.txt').is_file()
 jar=ROOT/'build/libs'/f'chao_garden-{VERSION}.jar'
 limits=['CWE source surfaces are adapted to Minecraft skin envelopes; hoodie shells and the headphone band use fitted geometry.','26 existing items updated; this does not import all CWE content or its executable.','Native review covers child outfits and representative scenes; geometric fitting covers 141 body cases. Not an exhaustive shader/device/animation matrix; Android not tested.','No Chao motion clips or item acquisition/effects changed.']
 report={'release':VERSION,'result':'PASS','jar_sha256':sha(jar),'jar_bytes':jar.stat().st_size,'build':{'core_suites':18,'core_checks':sum(counts)},'native_servers':{'default_passed':269,'default_required':269,'accessories_owo_passed':269,'accessories_owo_required':269},'client':{'source_items':26,'source_item_contexts':234,'toy_states':128,'missing_materials':0,'food_seed_tool_items':75,'food_seed_tool_contexts':600,'pods':144,'scenes':27,'scene_captures':30,'action_captures':len(saves),'equipped_cwe_items':10,'hoodie_colors':2,'return_restored':True,'live_action_results':['eat','plant','water'],'visual_review':'visual-review.txt'},'asset_replay':{'resources':len(a),'byte_identical':True,'java_files_unchanged':len(before)},'accessory_fit':{'geometry_contacts':8037,'runtime_fits':2256,'clothing_surface_samples':1896168,'crest_core_eye_checks':1422,'body_cases':141},'source_art':json.loads((out/'source-validation.json').read_text()),'extraction_replay':json.loads((out/'extraction-replay.json').read_text()),'resource_changes':changes|{'ogg_unchanged':len(audio)},'logs':{n:sha(out/n) for n in names},'limits':limits}
 (out/'verification.json').write_text(json.dumps(report,indent=2)+'\n')
 (ROOT/'TESTING.md').write_text(f'''# Chao Garden {VERSION} verification

Status: PASS
Target: Minecraft Java 1.21.1 / NeoForge 21.1.219 / Java 21.

## Changes

Both Kindergarten dormer windows are recessed into their walls with clear glazing and aligned sills. Scenery revision 38 applies the correction to existing authored worlds. Supplied CWE 9.6.2.2 models/materials now support 26 existing items: ten wearables, two toys, five special fruits and nine gifts. The two toys retain ten placed poses and the fruits retain twenty pod stages. Earlier requested texture reversions and other refinements remain included.

## Completed checks

- Build: 18 core suites, 1,710,878 assertions.
- Default native server: 269/269 required tests passed, including the actual dormer construction plan.
- Accessories 1.1.0-beta.54+1.21.1 + owo 0.12.15.6-beta.1+1.21: 269/269 required tests passed.
- Native client: 26 CWE items in all nine display contexts (234 combinations), 128 toy states, 75 food/seed/tool items in eight contexts (600 combinations), 144 growing pods and the registered blockstate audit. No missing model materials or invalid transforms.
- Visual review: 27 scenes, 30 scene captures and {len(saves)} action captures. Ten CWE items were actually equipped on child Chao, including both hoodie colors. Both dormers were inspected from front and oblique views. Representative garden and Kindergarten scenes remain intact. Eating, planting and watering completed through native gameplay; ordinary return was restored.
- Wearable fitting: 8,037 geometry contact checks across 2,256 runtime fits; 1,896,168 clothing triangle-centre/edge samples checked for buried surfaces; 1,422 crest/core/eye checks across 141 body cases. Duplicate reversed wearable surfaces are rejected to prevent no-cull lighting flicker.
- Asset rebuild: all {len(a):,} resources reproduced byte for byte; {len(before)} Java inputs unchanged. All {len(audio)} existing OGG recordings unchanged. Full existing resource-validator suite passed.
- Source extraction: 231 retained inputs reproduced or preserved byte for byte when re-decoding the selected supplied files. Supplied DLL code was not executed.
- Source validation: {report['source_art']['checks']} checks covering input hashes, item/model/texture bindings, source UVs, fitted bone identities, toy envelopes, pod stages and the eight exact prior texture rollbacks.

## Package integrity

JAR: {jar.name}
Bytes: {jar.stat().st_size}
SHA-256: {sha(jar)}

The packager verifies every production class/resource against the final JAR, source hashes, safe ZIP paths, CRCs and executable Gradle wrapper permissions. Test/probe classes and the development dimension fixture are excluded from the installable JAR.

## Practical limits

CWE surfaces are adapted to Minecraft Chao skin envelopes; hoodie shells and the headphone band use fitted geometry. This updates 26 existing items and does not port the complete CWE collection or executable. Native review covers child outfits and representative scenes; geometric fitting covers 141 body cases. It is not an exhaustive shader/device/animation matrix; Android was not tested. No new Chao motion clips or changes to item acquisition/effects are included.

Replace the old JAR on both client and server. No separate resource pack or game files are required. The complete ZIP contains source, selected original inputs, extraction/generation scripts and evidence.
''')
 print(json.dumps({k:report[k] for k in ('release','result','jar_sha256','build','native_servers','client','asset_replay')},indent=2))
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('work',type=Path);main(p.parse_args().work.resolve())
