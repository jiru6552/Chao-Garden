#!/usr/bin/env python3
"""Independent production selector, source preservation and cleanup contracts."""
from pathlib import Path
import argparse,copy,hashlib,json,re
from runtime_resource_contract_022 import *
ROOT=Path(__file__).resolve().parents[1]

def method(source,signature):
    start=source.index('{',source.index(signature));depth=1
    for end in range(start+1,len(source)):
        depth+=(source[end]=='{')-(source[end]=='}')
        if not depth:return source[start+1:end]
    raise AssertionError('Unclosed method '+signature)

def selector_contract(root=ROOT,source=None):
    root=Path(root)
    java=root/'src/main/java/com/chaogarden/client/OfficialChaoRig.java'
    text=source if source is not None else java.read_text()
    reference=re.search(r'REFERENCE_MODELS\s*=\s*List\.of\((.*?)\);',text,re.S)
    assert reference, 'Exact reference selector missing'
    reference_names=re.findall(r'"([a-z_]+)"',reference.group(1))
    assert len(reference_names)==20 and set(reference_names)==set(REFERENCE_MODELS), 'Exact reference selector changed'
    body=method(text,'public static String geometryKey(')
    literals=set(re.findall(r'"([a-z_]+)"',body))
    assert literals==set(GAMEPLAY_FORMS)|{'normal'}, 'Production geometry selector domain changed: '+str(literals^set(GAMEPLAY_FORMS)^{'normal'})
    assert 'Set.of("tails","amy","knuckles","omochao").contains(state.character)' in body, 'Dynamic character return lacks its finite guard'
    aliases={'hero_swim':'swim','hero_run':'run','dark_fly':'fly','dark_power':'power','amy':'baby','omochao':'baby','chaos_light':'baby','chaos_angel':'baby','chaos_devil':'baby'}
    alias_body=method(text,'public static String sourceGeometryKey(')
    assert set(re.findall(r'"([a-z_]+)"',alias_body))==set(aliases)|set(aliases.values()), 'Native alias selector domain changed'
    assert {aliases.get(k,k) for k in GAMEPLAY_FORMS}==set(ATTACHMENT_LAYOUTS)
    callers=[]
    for p in (root/'src/main/java').rglob('*.java'):
        if any(word in p.name for word in ('Visual','Probe')) or '/test/' in str(p):continue
        for argument in re.findall(r'\.animateGameplay\(\s*([^,]+),',p.read_text()):
            assert (p.name=='ChaoModel.java' and argument=='model') or (p.name in ('MarketModel.java','StaffChaoModel025.java') and argument=='"baby"'), 'Unreviewed gameplay caller '+str(p)+': '+argument
            callers.append({'class':p.name,'argument':argument})
    assert any(r['class']=='ChaoModel.java' for r in callers) and any(r['class']=='MarketModel.java' for r in callers)
    return {'gameplay_forms':sorted(GAMEPLAY_FORMS),'attachment_layouts':sorted(ATTACHMENT_LAYOUTS),'explicit_callers':callers}

def validate(root=ROOT,self_test=False):
    root=Path(root);assets=root/'src/main/resources/assets/chao_garden';checks=0
    def require(ok,label):
        nonlocal checks;checks+=1
        if not ok:raise AssertionError(label)
    selectors=selector_contract(root);checks+=4
    actual={p.stem.removeprefix('chao_') for p in (assets/'models/entity/gameplay').glob('chao_*.json')}
    expected=set(GAMEPLAY_FORMS)|{'chaos_light_glass','chaos_angel_glass','chaos_devil_glass'}
    require(actual==expected,'Gameplay mesh domain differs')
    for folder in ('overlays','hats','cwe_parts','party_hats'):
        require({p.stem.removeprefix('chao_') for p in (assets/'models/entity'/folder).glob('*.json')}==set(ATTACHMENT_LAYOUTS),'Attachment domain '+folder)
    require({p.name for p in (assets/'textures/entity/chao/official/coats').iterdir() if p.is_dir()}==set(COAT_LAYOUTS),'Coat domain')
    for p in pruning_paths(root):require(not (assets/p).exists(),'Unreachable resource returned: '+p)
    prototype=json.loads((root/'art/legacy_runtime/chao_mesh.json').read_text());minimal=json.loads((assets/'models/entity/chao_cocoon.json').read_text())
    require(len(prototype['bones'])==222 and len(minimal['bones'])==2,'Prototype/runtime size')
    originals={b['name']:b for b in prototype['bones']}
    for bone in minimal['bones']:require(bone==originals[bone['name']],'Cocoon topology/pivots changed')
    require(minimal['materials']=={'cocoon':prototype['materials']['cocoon']},'Cocoon material differs')
    raw=root/'art/imported_chao';inputs={str(p.relative_to(raw)):hashlib.sha256(p.read_bytes()).hexdigest() for p in raw.rglob('*') if p.is_file()}
    controls=json.loads((root/'art/controls022/raw-inputs.json').read_text())
    require(len(inputs)==41 and inputs==controls,'Original asset input changed')
    for key in REFERENCE_MODELS:
        require((assets/'models/entity/official'/f'chao_{key}.json').is_file(),'Exact reference rig missing')
        require((assets/'textures/entity/chao/official'/f'{key}.png').read_bytes()==(raw/'textures'/f'{key}.png').read_bytes(),'Reference PNG altered')
    manifest=json.loads((assets/'heritage_coats_018.json').read_text())
    require(len(manifest['resources'])==manifest['resource_count']==755,'Heritage count')
    require(manifest['moon_resources']==53,'Moon count')
    for p in manifest['resources']:require((assets/p).is_file(),'Heritage binding '+p)
    rejected=0
    if self_test:
        original=(root/'src/main/java/com/chaogarden/client/OfficialChaoRig.java').read_text()
        for a,b in [('case 1 -> "swim"','case 1 -> "unknown_future_layout"'),('return state.character;','return "unbounded_future_model";')]:
            require(a in original,'Selector regression fixture missing')
            try:selector_contract(root,original.replace(a,b,1))
            except AssertionError:rejected+=1
            else:raise AssertionError('Changed production selector was accepted')
    result={'release':'0.22.0','result':'PASS','checks':checks,'selectors':selectors,'raw_inputs':len(inputs),'pruned_resources':len(pruning_paths(root)),'runtime_gameplay_meshes':25,'native_coat_layouts':11,'attachment_layouts':13,'rejected_selector_corruptions':rejected,'prototype_sha256':hashlib.sha256((root/'art/legacy_runtime/chao_mesh.json').read_bytes()).hexdigest()}
    output=root/'docs/research/RESOURCE_HYGIENE_022_CHECKS.json';output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(result,indent=2)+'\n')
    print(f'Resource hygiene022: {checks}checks;{len(pruning_paths(root))}unreachableassets excluded;PASS')
    return result

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--self-test',action='store_true');args=parser.parse_args();validate(self_test=args.self_test)
