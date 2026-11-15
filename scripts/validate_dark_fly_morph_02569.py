#!/usr/bin/env python3
"""Independent JSON geometry diagnostics for Dark/Fly069, not visual proof.

No authoring generator import. Rigid parameter blends, SAT contact and actual
face clipping check delivered boxes, unchanged source rig and the signed grid.
"""
from pathlib import Path
import copy
import hashlib
import json
import math
import random
from validate_blocky_run_02555 import vertices, connected, volume, overlaps, rotate
from validate_neutral_swim_morph_02559 import bounds, closed
from validate_neutral_power_morph_02563 import no_same_facing_coplanar

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/chao_garden'
MODELS = ASSETS / 'models/entity/gameplay'
MODEL, SOURCE = MODELS / 'chao_dark_fly_proto069.json', MODELS / 'chao_baby.json'
FIELD, REGION = 'dark_fly_box_069', 'dark_fly_region_069'
TARGETS = ['dark_fly_young', 'run_swim', 'swim', 'power_swim', 'run', 'balanced',
           'power', 'run_fly', 'fly', 'power_fly']
# .72 deliberately added reciprocal Swim/Fly rows; only those two pins
# advance. Historical rig/head/retained-target preservation was audited.
PROTECTED = {
    'chao_baby.json': 'ae3d4da88b84905f8726b863bdb072e3a26eb404f38e565d0781b92b01e2cf9b',
    'chao_hero_run_proto055.json': '77b41d4501cba585b83be91b4052e8c56e03bff02e47db2b209f146d36390f08',
    'chao_neutral_proto056.json': 'a354458cae5605b873464614b57f9f9bd04d4a4e01d1ed793cf949194c4d14db',
    'chao_hero_normal_proto057.json': 'd0052db7f401ecc545e7a3db2992933b5671387520c82429d4a8aa336f28232d',
    'chao_neutral_swim_proto059.json': 'de2972ea6780a4252a98bbbdd84d674dd25a9ea4f3c1ea04be7124e3283593b1',
    'chao_dark_normal_proto060.json': '36ae27aed51674ea31310ab4af602e3e7127e1507c736ee9b6f75f3728acb514',
    'chao_neutral_fly_proto061.json': '05ce4c9211bee32d055575864e27e7e1a925ca68a838481810a57df7e507e203',
    'chao_neutral_run_proto062.json': 'cfed60309de43bc854272a4869d455d9ec76281a37a1787ff223f69b7ed85881',
    'chao_neutral_power_proto063.json': 'c3bfcbb2fb1730bbd23b8799e35d9a2edf33b3f4fab06c11a4638bacd1db07f0',
    'chao_hero_swim_proto064.json': '73a773cf29df2b8aa2e8e6d471428d8b3a7412a4ba1eab3809dace7f00a0b324',
    'chao_hero_fly_proto065.json': 'f5847fe9bc3429c03c5340f0cf65ff99649537759df9f5c993d25b53feb332cf',
    'chao_hero_run_proto066.json': '6ee8f47a220d769fa1e66a60cade8b7e8d211b16bafeca78dcd77074a8ae0e17',
    'chao_hero_power_proto067.json': '37d01b59876860b7ae11f895169a8a0a59f1b32dd716481a996d1a3d52e9d467',
    'chao_dark_swim_proto068.json': '815857229b825c097a0245806ba9362bc0970d61b5f304f0eb042b4f2c27241e'}


def weights(age, rp, sf):
    return [1-age] + [age*max(0, 1-abs(rp-x))*max(0, 1-abs(sf-y))
                      for y in (-1, 0, 1) for x in (-1, 0, 1)]


def endpoint(mesh, target):
    w = [0]*10
    w[target] = 1
    return vertices(mesh, w)


def distal(mesh, target):
    p = endpoint(mesh, target)
    return [sum(p[i][a] for i in (4, 5, 6, 7))/4 for a in range(3)]


def semantic_endpoints(features):
    named = {m['name']: m for m in features}
    assert list(named) == ['dark_fly_left_root', 'dark_fly_left_cap',
                           'dark_fly_right_root', 'dark_fly_right_cap']
    root, cap = named['dark_fly_right_root'], named['dark_fly_right_cap']
    roots = {i: distal(root, i) for i in range(10)}
    ends = {i: distal(cap, i) for i in range(10)}
    # Broad directions from the actual nine-form front/back and side matrix.
    for i in range(10):
        assert roots[i][0] > 2.6 and roots[i][2] > .8, 'Lateral roots must emerge from the rear upper corners'
    assert ends[5][1] > roots[5][1]+1.5, 'Balanced caps must hang visibly down'
    assert ends[0][1] > -5 and ends[5][1] > -4.25, 'Fresh and balanced caps must descend below the fixed skull top instead of reading as bare rising sticks'
    assert ends[5][1] > ends[0][1]+.4, 'Maturity must lengthen the common fresh hanging cap'
    assert ends[4][0] < roots[4][0]-.5, 'Run must turn its compact cap inward'
    assert roots[6][1] < roots[5][1]-1.5, 'Power roots must grow much taller than balanced'
    assert ends[6][0] < roots[6][0]-.6, 'Power tips must bend inward into pincers'
    assert ends[8][0] > roots[8][0]+.5 and ends[8][1] < roots[8][1]-.3, 'Fly caps must kink outward/up'
    assert roots[2][1] > roots[5][1]+1 and ends[2][1] > ends[5][1]+1, 'Swim roots must flatten and caps descend lower'
    assert ends[2][1] > ends[1][1]+1, 'Long Swim caps must remain distinct from compact RunSwim'
    assert ends[9][1] < roots[9][1]-.4 and ends[9][0] > roots[9][0], 'PowerFly tips must point up/out'
    assert cap[FIELD]['sizes'][3][2] < cap[FIELD]['sizes'][2][2]*.5, 'PowerSwim colored ends must remain short'
    signatures = [tuple(round(x, 7) for m in features for p in endpoint(m, i) for x in p) for i in range(1, 10)]
    assert len(set(signatures)) == 9
    for i in range(10):
        for suffix in ('root', 'cap'):
            left, right = [endpoint(named['dark_fly_'+side+'_'+suffix], i) for side in ('left', 'right')]
            assert sorted(tuple(round(-p[a] if a == 0 else p[a], 7) for a in range(3)) for p in left) == sorted(tuple(round(p[a], 7) for a in range(3)) for p in right)
    return {TARGETS[i]: {'right_root_terminal': roots[i], 'right_cap_terminal': ends[i]} for i in range(10)}


def main():
    for name,expected in PROTECTED.items():
        assert hashlib.sha256((MODELS/name).read_bytes()).hexdigest() == expected, ('Prior gameplay model changed',name)
    data, source = [json.loads(p.read_text()) for p in (MODEL, SOURCE)]
    bones, old = [{b['name']: b for b in d['bones']} for d in (data, source)]
    assert list(bones) == list(old) and len(bones) == 59
    for name, original in old.items():
        for field in ('pivot', 'rotation', 'parent'):
            assert bones[name].get(field) == original.get(field), (name, field)
        if name.startswith(('skull', 'expression_', 'emotion_')):
            if name == 'emotion_dot__cube_0':
                assert bones[name]['meshes'][0] == original['meshes'][0]
                assert len(bones[name]['meshes']) == 7
            else:
                assert bones[name] == original, name
    for field in ('head_decorations', 'materials'):
        assert data[field] == source[field]
    for name in ('tail', 'tail__cube_1'):
        assert bones[name] == old[name]
    head_mesh = old['skull__cube_0']['meshes'][0]
    head = head_mesh['vertices']
    assert bounds(head) == [[-3.5, 3.5], [-6, 0], [-3, 3]]
    path = data['dark_fly_path_069']
    assert path['targets'] == TARGETS and path['source_geometry_copied'] is False
    assert path['reserved_pixels'] == {'feature_body': [105,63], 'feature_mid': [104,63],
        'feature_tip': [103,63], 'feature_underside': [102,63], 'wing_root': [101,63], 'wing_tip': [100,63]}
    boxes, features, wings, markers, tails = [], [], [], [], []
    for bone in data['bones']:
        for original in bone.get('meshes', []):
            if FIELD not in original:
                continue
            mesh = copy.deepcopy(original)
            mesh['blocky_box_055'] = mesh[FIELD]
            boxes.append(mesh)
            closed(mesh)
            for field in ('centers', 'sizes', 'rotations'):
                values = mesh[FIELD][field]
                assert len(values) == 10 and all(len(p) == 3 for p in values)
                assert all(math.isfinite(x) for p in values for x in p)
            assert min(x for p in mesh[FIELD]['sizes'] for x in p) >= .01
            assert max(abs(a-b) for p,q in zip(endpoint(mesh,0),mesh['vertices']) for a,b in zip(p,q)) < 1e-7
            if bone['name'] == 'gradual_head_021': features.append(mesh)
            if bone['name'].startswith('wing_'): wings.append((bone['name'],mesh))
            if bone['name'] == 'emotion_dot__cube_0': markers.append(mesh)
            if bone['name'] == 'tail__cube_0': tails.append(mesh)
    assert (len(boxes),len(features),len(wings),len(markers),len(tails)) == (23,4,6,6,2)
    facts = semantic_endpoints(features)
    eyes = [m['vertices'] for n in ('skull__cube_2','skull__cube_3') for m in old[n]['meshes']]
    core_mesh = old['emotion_dot__cube_0']['meshes'][0]
    core = core_mesh['vertices']
    marker_local = [core] + [endpoint(m,0) for m in markers]
    for part in marker_local[1:]: assert overlaps(part,core)
    no_same_facing_coplanar([(core_mesh,core)] + list(zip(markers,marker_local[1:])))
    marker_skull = [[[q[0],q[1]-11,q[2]] for q in [rotate(p,[-22.5,0,0]) for p in part]] for part in marker_local]
    for mesh in markers:
        assert all(p == mesh[FIELD][k][0] for k in ('centers','sizes','rotations') for p in mesh[FIELD][k])
    rng = random.Random(25069)
    samples = [(a/20,x/10-1,y/10-1) for a in range(21) for x in range(21) for y in range(21)]
    samples += [(rng.random(),rng.uniform(-1,1),rng.uniform(-1,1)) for _ in range(256)]
    minimum_volume, plane_pairs, maximum_step = math.inf, 0, 0
    for age,rp,sf in samples:
        w = weights(age,rp,sf)
        assert abs(sum(w)-1) < 1e-9 and min(w) >= 0
        parts = [vertices(m,w) for m in features]
        assert connected(parts,head), (age,rp,sf,'Detached head feature')
        try:
            count,_ = no_same_facing_coplanar([(head_mesh,head)] + list(zip(features,parts)))
        except AssertionError as error:
            raise AssertionError((age,rp,sf,str(error))) from error
        plane_pairs += count
        for m,p in zip(features,parts):
            assert not any(overlaps(p,eye) for eye in eyes), (age,rp,sf,m['name'],'Eye collision')
            assert not any(overlaps(p,orb) for orb in marker_skull), (age,rp,sf,m['name'],'Tilted orb collision')
        tail_parts = [vertices(m,w) for m in tails]
        assert overlaps(*tail_parts) and overlaps(tail_parts[0],old['tail__cube_1']['meshes'][0]['vertices']), (age,rp,sf,'Tail graph detached')
        for mesh in boxes:
            minimum_volume = min(minimum_volume,volume(vertices(mesh,w),mesh['faces']))
            assert minimum_volume > 1e-9
        if age <= .95:
            after = [vertices(m,weights(age+.05,rp,sf)) for m in features]
            maximum_step = max(maximum_step,max(abs(a-b) for p,q in zip(parts,after) for u,v in zip(p,q) for a,b in zip(u,v)))
    for name in ('arm_left__cube_1','arm_right__cube_1','foot_left__cube_0','foot_right__cube_0'):
        m = copy.deepcopy(bones[name]['meshes'][0]); m['blocky_box_055'] = m[FIELD]
        for target in range(10):
            p = endpoint(m,target)
            if name.startswith('arm'):
                assert abs(min(q[1] for q in p)) < 1e-7, 'Wrist upper plane moved'
            else:
                assert abs(max(q[1] for q in p)-2) < 1e-7 and abs(max(q[2] for q in p)) < 1e-7
    for bone,wing in wings:
        sign = -1 if 'left' in bone else 1
        for target in range(10):
            p = endpoint(wing,target)
            # Proximal face center stays the exact original joint hinge.
            ids = (0,3,4,7) if sign < 0 else (1,2,5,6)
            center = [sum(p[i][a] for i in ids)/4 for a in range(3)]
            assert max(abs(a-b) for a,b in zip(center,[0,.25,0])) < 1e-7
        distal_ids = (1,2,5,6) if sign < 0 else (0,3,4,7)
        for f in wing['faces']:
            assert f['uv'] == [[50.25 if i in distal_ids else 50.75,31.75] for i in f['v']]
    manifest = json.loads((ASSETS/'dark_fly069.json').read_text())
    for field,path in [('sha256',MODEL),('source_model_sha256',SOURCE)]:
        assert manifest[field] == hashlib.sha256(path.read_bytes()).hexdigest()
    for path,expected in manifest['construction_reference_sha256'].items():
        assert hashlib.sha256((ROOT/path).read_bytes()).hexdigest() == expected
    print(json.dumps({'family':'DarkFly069','positive_closed_boxes':len(boxes),'signed_blends':len(samples),
        'canonical_joints':59,'head_dimensions':[7,6,6],'minimum_box_volume':minimum_volume,
        'same_facing_face_pairs':plane_pairs,'maximum_5percent_age_vertex_step':maximum_step,
        'endpoint_semantics':facts},sort_keys=True))


if __name__ == '__main__':
    main()
