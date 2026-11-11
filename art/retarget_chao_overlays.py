"""Fit original theme/animal/Chaos additions to the imported Chao skeletons.

The source rig remains the editable authoring source for these additions. Only
its custom groups are retained; the imported models supply the body and motion.
"""
from pathlib import Path
import copy
import json
import math
import sys

from PIL import Image, ImageDraw
from variant_catalog import VARIANTS

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources/assets/chao_garden'
MODEL = RES / 'models/entity'
OUT = MODEL / 'overlays'
TEXTURES = RES / 'textures/entity/chao/official'
SOURCE = ROOT / 'art/imported_chao'

ANCHORS = {'head':'skull', 'body':'body', 'left_arm':'arm_left',
           'right_arm':'arm_right', 'left_foot':'foot_left',
           'right_foot':'foot_right', 'left_wing':'wing_right',
           'right_wing':'wing_left', 'tail':'tail'}


def inverse_rotation(point, degrees):
    # Invert the runtime's Rz * Ry * Rx static bone rotation.
    x,y,z = point
    a,b,c = [math.radians(v) for v in degrees]
    x,y = x*math.cos(c)+y*math.sin(c), -x*math.sin(c)+y*math.cos(c)
    x,z = x*math.cos(b)-z*math.sin(b), x*math.sin(b)+z*math.cos(b)
    y,z = y*math.cos(a)+z*math.sin(a), -y*math.sin(a)+z*math.cos(a)
    return [x,y,z]


def fitting(old, official):
    """Return retained custom groups, fitted around actual rig anchors."""
    bases = {b['name']:copy.deepcopy(b) for b in official['bones']
             if '__cube_' not in b['name']}
    for b in bases.values():
        b['meshes'] = []
    # Some collectible source forms omit arms. Our regular form selection does
    # not use those forms, but keep a complete overlay skeleton for editor use.
    baby = json.loads((MODEL/'official/chao_baby.json').read_text())
    baby_bones = {b['name']:b for b in baby['bones'] if '__cube_' not in b['name']}
    for name in ANCHORS.values():
        if name not in bases:
            bases[name] = copy.deepcopy(baby_bones[name]); bases[name]['meshes'] = []
    # Source skull core height differs among the imported Hero/Dark/Swim rigs.
    candidates=[b for b in official['bones'] if b['name'].startswith('skull__cube_')]
    def volume(b):
        a=[v for m in b.get('meshes',[]) for v in m['vertices']]
        return math.prod(max(v[i] for v in a)-min(v[i] for v in a) for i in range(3))
    cube=max(candidates,key=volume)
    coordinates = [v for m in cube.get('meshes',[]) for v in m['vertices']]
    head_height = max(v[1] for v in coordinates)-min(v[1] for v in coordinates)
    head_bottom = max(v[1] for v in coordinates)+cube['pivot'][1]
    if not 4 <= head_height <= 9: raise ValueError('Unexpected source skull')
    hs = head_height / 6.5
    active = {v['id'] for v in VARIANTS}
    retained = []
    for bone in old['bones']:
        name=bone['name']
        if name.startswith('variant_'):
            if not any(name.startswith('variant_'+ident+'_') for ident in active): continue
        elif not (name.startswith('animal_') or name.startswith('chaos_') or name=='flame'):
            continue
        parent=bone.get('parent'); target=ANCHORS.get(parent)
        if not target: continue
        slot = int(name.split('_')[1]) if name.startswith('animal_') else None
        arm = parent.endswith('_arm')
        foot = parent.endswith('_foot')
        wing = parent.endswith('_wing') or name.startswith('chaos_') and '_wing_' in name
        marker = name.endswith('_marker') or name=='flame'
        if wing:
            target='wing_right' if name.endswith('_left') else 'wing_left'
        if name=='chaos_devil_tail': target='tail'
        if slot==6: target='tail'
        b=copy.deepcopy(bone); b['parent']=target;b['pivot']=[0,0,0];b.pop('rotation',None)
        side = 1 if parent.startswith('left') or name.endswith('_left') else -1
        if marker:
            b['parent']='skull'; b['pivot']=[0,head_bottom-head_height-5,0]
        for mesh in b['meshes']:
            converted=[]
            for original in mesh['vertices']:
                x,y,z=original
                if marker:
                    point=[x*.9,y*.9,z*.9]
                elif arm:
                    if mesh.get('name')=='gene_arm_connector':
                        point=[side*y*2.2,.6-side*x,z*.95]
                    else:
                        point=[side*(y*.8+1.9),1.28-side*x*.8,z*.95-.3575]
                elif foot:
                    point=[x*.8+side,y*.8+.8,z-1.25]
                elif wing:
                    # Move broad custom feathers to the imported wing roots;
                    # the source wing's own tilt and animation are inherited.
                    if name.startswith('animal_'):
                        point=[x*.8+side*.9,y*.9,(z-2.2)*.6+.2]
                    else:
                        point=[x*.9+side,y*.9,z*.7+.2]
                elif parent=='head':
                    # Old skull center is -.5, with top -3.75 and bottom2.75.
                    point=[x*.875,(y-2.75)*hs+head_bottom,z*(6/6.5)]
                elif name=='chaos_devil_tail' or slot==6:
                    # The previous tail was body-relative. Fit its visible
                    # shape to the imported tail anchor and cancel static tilt.
                    ox,oy,oz=bone['pivot']
                    anchor=bases['tail']['pivot']
                    point=[(x+ox)*.8-anchor[0],(y+oy)*(4/4.5)-1.5-anchor[1],(z+oz)*(4/3.5)-anchor[2]]
                    point=inverse_rotation(point,bases['tail'].get('rotation',[0,0,0]))
                else:
                    ox,oy,oz=bone['pivot']
                    point=[(x+ox)*.8,(y+oy)*(4/4.5)-1.5,(z+oz)*(4/3.5)]
                converted.append([round(v,6) for v in point])
            mesh['vertices']=converted
        retained.append(b)
    return {'format_version':old.get('format_version',1),
            'texture_width':old['texture_width'],'texture_height':old['texture_height'],
            'materials':copy.deepcopy(old['materials']),
            'bones':list(bases.values())+retained,
            'retarget':{'head_core':[7,head_height,6],'body_core':[4,4,4],
                        'custom_bones':len(retained)}}


def recolor(palette):
    base=Image.open(SOURCE/'textures/baby.png').convert('RGBA')
    pixels=base.load()
    for y in range(base.height):
        for x in range(base.width):
            r,g,b,a=pixels[x,y]
            if not a:continue
            if b>180 and g>160 and r<200:
                target=palette['head'] if y<26 else palette['body']
                shade=(r+g+b)/(138+239+255)
            elif r>170 and g>170 and b<170:
                target=palette.get('accent',palette['tuft'])
                shade=(r+g+b)/(236+255+141)
            elif r>150 and b>150 and g<190:
                target=palette.get('wing',palette['tuft'])
                shade=(r+g+b)/(234+169+227)
            else:continue # Preserve the official eye whites and dark ink.
            pixels[x,y]=tuple(max(0,min(255,round(c*shade))) for c in target)+(a,)
    return base


def textures():
    TEXTURES.mkdir(parents=True,exist_ok=True)
    for v in VARIANTS:
        recolor(v['palette']).save(TEXTURES/('variant_'+v['id']+'.png'))
    # Existing canonical Chaos palettes retain their color identity on the new
    # base UVs. Their unique silhouette is supplied by the fitted overlay.
    sys.path.insert(0,str(ROOT/'art'))
    from build_adventure_assets import CHAOS_PALETTES
    for form,palette in CHAOS_PALETTES.items():
        recolor(palette).save(TEXTURES/('chaos_'+form+'.png'))
    # The SA2 merchant has the ordinary neutral blue coat. Round spectacles and
    # its pale mask are separate fitted geometry, authored in build_market_017.
    merchant=Image.open(SOURCE/'textures/baby.png').convert('RGBA')
    merchant.save(TEXTURES/'black_market.png')


def main():
    OUT.mkdir(parents=True,exist_ok=True)
    old=json.loads((MODEL/'chao_mesh.json').read_text())
    records=[]
    for source in sorted((MODEL/'official').glob('chao_*.json')):
        data=fitting(old,json.loads(source.read_text()))
        target=OUT/source.name
        target.write_text(json.dumps(data,separators=(',',':')))
        for b in data['bones']:
            assert b['parent'] is None or b['parent'] in {b['name'] for b in data['bones']}
            for m in b['meshes']:
                assert all(math.isfinite(c) for v in m['vertices'] for c in v)
        records.append({'rig':source.stem,'head_core':data['retarget']['head_core'],
                        'custom_bones':data['retarget']['custom_bones']})
    assert len(records)==20
    textures()
    visible_animals = {b['name'].split('_')[2] for b in old['bones']
                       if b['name'].startswith('animal_')}
    (RES/'chao_overlay_retarget.json').write_text(json.dumps({'format_version':1,
        'theme_count':len(VARIANTS),'animal_count':21,
        'visible_animal_part_sets':len(visible_animals),'chaos_forms':3,
        'rigs':records},indent=2)+'\n')
    print(f'Fitted {len(records)} overlay skeletons, {len(VARIANTS)} theme coats, '
          f'{len(visible_animals)} visible animal-part sets (21 animal genes), '
          'and 3 Chaos forms.')


if __name__=='__main__':main()
