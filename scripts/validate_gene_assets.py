#!/usr/bin/env python3
"""Validate gene resources, additive generation and exact cuboid slot coverage."""
from __future__ import annotations

import argparse
import contextlib
import importlib.util
import io
import json
import re
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/"art"))
from runtime_resource_contract_022 import prototype_path, ATTACHMENT_LAYOUTS
import tempfile

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ANIMALS = ('penguin', 'seal', 'otter', 'peacock', 'parrot', 'condor', 'rabbit',
           'cheetah', 'warthog', 'bear', 'tiger', 'gorilla', 'skunk', 'sheep',
           'raccoon', 'dragon', 'unicorn', 'phoenix', 'half_fish', 'skeleton_dog', 'bat')
SLOTS = ((0,2,4),(0,6),(0,1,3,4,6),(2,4,6,7),(2,4,6,7),(1,4,6,7),
         (0,1,4,6),(0,1,4,6),(0,1,3,4,6),(0,1,4),(0,1,4,6),(0,1,2,4),
         (0,2,4,6),(0,1,4,5,6),(0,1,4),(0,1,4,5,6,7),(0,1,2,4,6),
         (2,4,6,7),(),(),())

EXTRA_ANIMALS = ('bee','ladybug','scorpion','deer','elephant','kangaroo','koala','lion','mole','swallow')
EXTRA_SLOTS = ((4,5,7),(2,5,7),(0,4,6),(0,1,4,5,6),(0,1,4,6),
               (0,1,4,6),(0,1,4,6),(0,1,2,4,6),(0,4,6),(4,6,7))
EXTRA_TRAINING = ((0,250,0,0),(0,250,0,0),(0,0,0,250),(-4,-12,32,16),
                  (-8,-16,20,36),(-8,-8,40,8),(20,16,4,8),(8,-16,4,36),
                  (8,8,16,16),(20,60,-24,16))
checks=0

def require(condition, message):
    global checks
    checks += 1
    if not condition:
        raise AssertionError(message)


def read(path):
    return json.loads(path.read_text())


def validate_items(root, include_cwe=True):
    asset = root/'src/main/resources/assets/chao_garden'
    language = read(asset/'lang/en_us.json')
    images = set()
    for animal in ANIMALS:
        copies = []
        for suffix in ('_animal_gene', '_animal'):
            ident = animal+suffix
            image = Image.open(asset/'textures/item'/f'{ident}.png').convert('RGBA')
            require(image.size == (32,32), ident+': expected native 32-pixel vial icon')
            require(image.getpixel((0,0))[3] == 0 and image.getbbox(), ident+': cutout is empty or opaque')
            require(len(image.getcolors(256) or []) >= 9, ident+': missing glass, liquid and contrasting DNA')
            copies.append(image.tobytes())
            model = read(asset/'models/item'/f'{ident}.json')
            require(model.get('parent') == 'minecraft:item/generated'
                    and model.get('textures',{}).get('layer0') == 'chao_garden:item/'+ident,
                    ident+': generated inventory model does not resolve the vial')
            require('Animal Genes' in language.get('item.chao_garden.'+ident,''), ident+': missing item name')
        require(copies[0] == copies[1], animal+': legacy appearance differs from canonical genes')
        images.add(copies[0])
    for animal in EXTRA_ANIMALS if include_cwe else ():
        ident=animal+'_animal_gene'
        image=Image.open(asset/'textures/item'/f'{ident}.png').convert('RGBA')
        require(image.size==(32,32),ident+': expected native32-pixel vial')
        require(image.getpixel((0,0))[3]==0 and image.getbbox(),ident+': empty/opaque cutout')
        require({pixel[3] for pixel in image.get_flattened_data()}=={0,255},ident+': alpha must be binary')
        require(len(image.getcolors(256) or [])>=9,ident+': missing glass/liquid/contrastingDNA')
        model=read(asset/'models/item'/f'{ident}.json')
        require(model=={'parent':'minecraft:item/generated','textures':{'layer0':'chao_garden:item/'+ident}},ident+': incorrect model binding')
        require('Animal Genes' in language.get('item.chao_garden.'+ident,''),ident+': missing translated name')
        require(not(asset/'models/item'/f'{animal}_animal.json').exists(),animal+': invented legacy alias')
        images.add(image.tobytes())
    require(len(images)==(31 if include_cwe else 21),'Canonical samples must all have distinct color/helix appearances')
    data = root/'src/main/resources/data'
    tagged = set(read(data/'chao_garden/tags/item/animal_genes.json')['values'])
    expected = {'chao_garden:'+animal+suffix for animal in ANIMALS
                for suffix in ('_animal_gene','_animal')}
    if include_cwe:expected.update('chao_garden:'+animal+'_animal_gene' for animal in EXTRA_ANIMALS)
    require(tagged==expected, f'Exact gene tag differs: missing{expected-tagged},extra{tagged-expected}')
    require(len(read(data/'chao_garden/tags/item/animal_genes.json')['values'])==len(expected),'Gene tag duplicates a sample')
    modifier = read(data/'chao_garden/loot_modifiers/generic_chest_training.json')
    require(modifier == {'type':'chao_garden:generic_chest_training','conditions':[],
                         'drive_chance':.40,'gene_chance':.12}, 'Chest training defaults differ')
    entries = read(data/'neoforge/loot_modifiers/global_loot_modifiers.json')['entries']
    require(entries.count('chao_garden:generic_chest_training') == 1, 'Generic chest GLM missing or duplicated')
    return 52 if include_cwe else 42


def validate_geometry(root):
    asset = prototype_path(root)
    bones = {bone['name']:bone for bone in read(asset)['bones']}
    expected = set()
    for animal, slots in enumerate(SLOTS):
        for slot in slots:
            for side in (('_left','_right') if slot in (0,4,7) else ('',)):
                expected.add(f'animal_{slot}_{animal}'+side)
    actual = {name for name in bones if name.startswith('animal_')}
    require(actual == expected, f'Animal slot coverage differs: missing {expected-actual}, extra {actual-expected}')
    cubes = 0
    for name in actual:
        bone = bones[name]
        _, slot, animal, *side = name.split('_'); slot, animal = int(slot),int(animal)
        parent = ({0:side[0]+'_arm',4:side[0]+'_foot',7:side[0]+'_wing'}[slot]
                  if slot in (0,4,7) else 'body' if slot == 6 else 'head')
        require(bone['parent'] == parent and bone['pivot'] == [0,0,0], name+': loses parent animation transform')
        require(bone['meshes'], name+': supplied slot is empty')
        for mesh in bone['meshes']:
            require(mesh.get('primitive') == 'cube' and mesh.get('material') == f'animal_{animal}',
                    name+': expected textured native cuboid addition')
            require(len(mesh['vertices']) == 8 and len(mesh['faces']) == 6, name+': malformed cuboid')
            if slot in (1,2,5):
                require(min(point[2] for point in mesh['vertices']) > -2,
                        name+': head attachment enters the front expression plane')
            cubes += 1
    # Muzzle additions fit between or beyond the eye columns and above the mouth.
    # The eye columns occupy x [-3.5,-1] and [1,3.5], y [-1,2].
    # Keep a positive gap instead of accepting coplanar overlaps/z-fighting.
    for animal in (2,8):
        for mesh in bones[f'animal_3_{animal}']['meshes']:
            xs = [p[0] for p in mesh['vertices']]; ys = [p[1] for p in mesh['vertices']]
            require(max(ys) < 1.65, mesh['name']+': muzzle obscures pet/eating mouth')
            for low,high in ((-3.5,-1),(1,3.5)):
                require(max(xs) < low-.01 or min(xs) > high+.01,
                        mesh['name']+': muzzle obscures an eye column')
    return len(actual), cubes


def validate_additive_generator(root):
    spec = importlib.util.spec_from_file_location('gene_asset_fixture',root/'scripts/generate_gene_assets.py')
    module = importlib.util.module_from_spec(spec); spec.loader.exec_module(module)
    with tempfile.TemporaryDirectory(prefix='chao-genes-idempotence-') as directory:
        module.ROOT = Path(directory); module.RES = module.ROOT/'src/main/resources'
        module.ASSET = module.RES/'assets/chao_garden'
        language = module.ASSET/'lang/en_us.json'; language.parent.mkdir(parents=True)
        language.write_text(json.dumps({'item.other.keep':'Keep this unrelated translation'}))
        global_path = module.RES/'data/neoforge/loot_modifiers/global_loot_modifiers.json'
        global_path.parent.mkdir(parents=True)
        global_path.write_text(json.dumps({'replace':False,'entries':['other_mod:original']}))
        own_loot = module.RES/'data/chao_garden/loot_table/test.json'
        own_loot.parent.mkdir(parents=True)
        own_loot.write_text(json.dumps({'pools':[{'entries':[
            {'type':'minecraft:item','name':'chao_garden:penguin_animal'},
            {'type':'minecraft:item','name':'other_mod:penguin_animal'}]}]}))
        foreign_loot = module.RES/'data/other_mod/loot_table/test.json'
        foreign_loot.parent.mkdir(parents=True)
        foreign_loot.write_text(json.dumps({'name':'chao_garden:penguin_animal'}))
        with contextlib.redirect_stdout(io.StringIO()): module.main()
        first = {str(p.relative_to(module.ROOT)):p.read_bytes()
                 for p in module.ROOT.rglob('*') if p.is_file()}
        with contextlib.redirect_stdout(io.StringIO()): module.main()
        second = {str(p.relative_to(module.ROOT)):p.read_bytes()
                  for p in module.ROOT.rglob('*') if p.is_file()}
        require(first == second, 'Running the gene generator twice changes its output')
        require(read(language)['item.other.keep'] == 'Keep this unrelated translation',
                'Gene generator overwrites translations from other features')
        require(read(global_path)['entries'] == ['other_mod:original','chao_garden:generic_chest_training'],
                'Gene generator replaces another feature\'s global loot modifier')
        require(read(own_loot)['pools'][0]['entries'] == [
            {'type':'minecraft:item','name':'chao_garden:penguin_animal_gene'},
            {'type':'minecraft:item','name':'other_mod:penguin_animal'}],
            'Canonical gene drops changed an unrelated mod item or retained a legacy ID')
        require(read(foreign_loot) == {'name':'chao_garden:penguin_animal'},
                'Gene migration rewrote another namespace\'s loot resources')
        validate_items(module.ROOT,include_cwe=False)
    return len(first)


def validate_cwe_descriptors_and_geometry(root):
    source=(root/'src/main/java/com/chaogarden/core/ChaoState.java').read_text()
    stats=source.split('ANIMAL_STATS = {',1)[1].split('};',1)[0]
    rows=[tuple(int(v) for v in re.findall(r'-?\d+',row)) for row in re.findall(r'\{([^{}]+)\}',stats)]
    require(len(rows)==31 and tuple(rows[21:])==EXTRA_TRAINING,'New DNA tooltip/training descriptors differ from source-verified matrix')
    tooltip=(root/'src/main/java/com/chaogarden/genes/GeneContent.java').read_text()
    require('ChaoState.animalTraining(index)' in tooltip and 'ChaoState.animalSlots(index)' in tooltip,'Tooltips must derive actual training and parts, not stale duplicate tables')
    catalog=read(root/'docs/cwe_content_019.json')
    require(catalog['genes']==list(EXTRA_ANIMALS),'New DNA catalog/order differs')
    validate_chest_gene_policy(root)
    attraction=(root/'src/main/java/com/chaogarden/cwe/CweContent.java').read_text()
    pools=re.findall(r'"([a-z_]+_fruit)",new int\[\]\{([^}]+)\}',attraction)
    require(len(pools)==8,'Missing source fruit-attraction pool')
    attracted=set()
    for fruit,raw in pools:
        pairs=[int(x)for x in raw.split(',')]
        require(len(pairs)%2==0 and sum(pairs[1::2])==100,fruit+': probabilities must total100')
        require(all(0<=i<31 for i in pairs[::2]),fruit+': invalid DNA index')
        attracted.update(pairs[::2])
    require(set(range(21,31))<=attracted,'New DNA species unreachable through fruit attraction')
    directory=root/'src/main/resources/assets/chao_garden/models/entity/cwe_parts'
    rigs=sorted(directory.glob('*.json'));require({p.stem.removeprefix('chao_') for p in rigs}==set(ATTACHMENT_LAYOUTS),'Expected all13 runtime source-fit overlay rigs')
    expected=set()
    for offset,slots in enumerate(EXTRA_SLOTS,21):
        for slot in slots:
            for side in (('_left','_right') if slot in (0,4,7) else ('',)):expected.add(f'animal_{slot}_{offset}'+side)
    total=0
    for path in rigs:
        bones={b['name']:b for b in read(path)['bones']};actual={n for n in bones if n.startswith('animal_')}
        require(actual==expected,f'{path.name}: exact additional DNA slot coverage differs')
        for name in actual:
            bone=bones[name];require(bool(bone.get('meshes')),name+': empty model addition')
            for mesh in bone['meshes']:
                require(len(mesh['vertices'])==8 and len(mesh['faces'])==6,name+': native cuboid contract differs')
                for face in mesh['faces']:require(all(0<=v<8 for v in face['v']),name+': invalid vertex')
                total+=1
    return len(rigs),total


def validate_chest_gene_policy(root):
    """Check the final weighted policy against registry order and actual resources.

    Uniform selection was intentionally replaced in0.24: every ordinary animal
    owns three tickets and each mythical animal owns two. Checking only a call
    to nextInt(ANIMALS.length) would reject the correct policy and miss drift
    between its indices, registered items and generated loot.
    """
    java=root/'src/main/java/com/chaogarden'
    registry=(java/'ChaoGarden.java').read_text()
    registered=tuple(re.findall(r'"([a-z_]+)"',registry.split('String[] ANIMALS = {',1)[1].split('};',1)[0]))
    expected=ANIMALS+EXTRA_ANIMALS
    require(registered==expected,'Canonical31 animal registry order differs from asset identities')
    rules=(java/'genes/TrainingSupplyRules.java').read_text()
    def catalog(name):
        match=re.search(r'\b'+name+r'\s*=\s*List\.of\((.*?)\);',rules,re.S)
        require(match is not None,'Missing weighted gene catalog '+name)
        return tuple(re.findall(r'"([a-z_]+)"',match.group(1)))
    require(catalog('ANIMALS')==registered,'Weighted gene indices differ from canonical registration order')
    mythical=catalog('MYTHICAL')
    require(mythical==('dragon','unicorn','phoenix'),'Mythical DNA category differs from intended modest-rarity animals')
    def constant(name):
        match=re.search(r'\b'+name+r'\s*=\s*(\d+)',rules)
        require(match is not None,'Missing weighted gene constant '+name)
        return int(match.group(1))
    ordinary_weight,mythical_weight=constant('ORDINARY_WEIGHT'),constant('MYTHICAL_WEIGHT')
    total=constant('TOTAL_GENE_WEIGHT')
    require((ordinary_weight,mythical_weight,total)==(3,2,90),'Ordinary/mythical weights must be3/2 across90 tickets')
    weights=[mythical_weight if animal in mythical else ordinary_weight for animal in registered]
    require(sum(weights)==total and all(weight>0 for weight in weights),'Weighted policy excludes a gene or loses tickets')
    compact=re.sub(r'\s+','',rules)
    require('mythical(ANIMALS.get(index))?MYTHICAL_WEIGHT:ORDINARY_WEIGHT' in compact,
            'Runtime index weights do not use the validated animal category')
    require('intticket=random.applyAsInt(TOTAL_GENE_WEIGHT);' in compact
            and 'for(inti=0;i<ANIMALS.size();i++){ticket-=geneWeight(i);if(ticket<0)returni;}' in compact,
            'Runtime weighted selector does not consume exactly one validated ticket')
    rescued=read(root/'src/main/resources/data/chao_garden/loot_table/bonus/rescued_animals.json')['pools'][0]['entries']
    resource_weights={entry['name']:entry.get('weight',1) for entry in rescued}
    require(len(rescued)==31 and resource_weights=={'chao_garden:'+animal+'_animal_gene':weight for animal,weight in zip(registered,weights)},
            'Generated DNA loot differs from the complete weighted runtime catalog')
    loot=re.sub(r'\s+','',(java/'genes/GenericChestTrainingModifier.java').read_text())
    guard='if(!com.chaogarden.distribution.ChestEggLootModifier.isChest(context))returngenerated;'
    selection='GeneContent.gene(TrainingSupplyRules.geneIndex(random::nextInt))'
    require(guard in loot and selection in loot and loot.index(guard)<loot.index(selection),
            'Chest DNA must use the shared chest classifier before selecting a registered weighted sample')
    require(loot.count('GeneContent.gene(')==1 and 'if(random.nextFloat()<geneChance)generated.add(newItemStack('+selection+'));' in loot,
            'Chest DNA must have one independently chance-gated weighted item roll')
    classifier=(java/'distribution/ChestEggLootModifier.java').read_text().split('public static boolean isChest(LootContext context)',1)[1].split('@Override',1)[0]
    require('table.getParamSet()==LootContextParamSets.CHEST||ChestFillWitness.matches(context,table)' in re.sub(r'\s+','',classifier),
            'Arbitrary namespace chest DNA requires declared CHEST tables or a real physical chest transaction')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--generator-only',action='store_true',help='Validate isolated generation before shared assets exist')
    args = parser.parse_args()
    files = validate_additive_generator(ROOT)
    if args.generator_only:
        print(f'Additive gene generation passed: {files} resources remain byte-identical after repeat generation.')
    else:
        items = validate_items(ROOT); bones, cubes = validate_geometry(ROOT)
        rigs,extra_cubes=validate_cwe_descriptors_and_geometry(ROOT)
        print(f'Gene assets passed: {items} canonical/legacy icons,31 distinct DNA samples, '
              f'{bones} supplied-slot bones and {cubes} native cuboids; expression plane remains clear.')
        print(f'CWE gene descriptors/attraction and{rigs} native fits passed:{extra_cubes} additional cuboids; {checks} assertions total.')
        report=ROOT/'docs/validation-0.19/gene-resources.json';report.parent.mkdir(parents=True,exist_ok=True);report.write_text(json.dumps({'checks':checks,'result':'passed','canonical_samples':31,'legacy_aliases':21,'original_bones':bones,'original_cuboids':cubes,'new_fitted_rigs':rigs,'new_cuboids':extra_cubes,'isolated_additive_resources':files},indent=2)+'\n')
        print(f'Isolated additive generation passed: {files} resources remain byte-identical; unrelated translations/loot preserved.')


if __name__ == '__main__': main()
