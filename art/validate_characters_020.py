#!/usr/bin/env python3
"""Independent actual-asset contracts for the authored Amy/Omochao refinements."""
from pathlib import Path
import hashlib,json,math
from PIL import Image
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/chao_garden';M=A/'models/entity';T=A/'textures/entity/chao'
checks=0

def test(ok,message):
 global checks;checks+=1
 if not ok:raise AssertionError(message)
def load(p):return json.loads(p.read_text())
def bounds(mesh):return [min(v[i] for v in mesh['vertices'])for i in range(3)]+[max(v[i] for v in mesh['vertices'])for i in range(3)]
def intersects(a,b):return all(min(a[i+3],b[i+3])+1e-5>=max(a[i],b[i])for i in range(3))
def connected(boxes,label):
 seen={0}
 while True:
  extra={i for i,a in enumerate(boxes)if i not in seen and any(intersects(a,boxes[j])for j in seen)}
  if not extra:break
  seen|=extra
 test(len(seen)==len(boxes),label+' detached block components '+str(set(range(len(boxes)))-seen))

raw=load(M/'official/chao_baby.json');native={b['name']:b for b in raw['bones']}
characters={key:load(M/'gameplay'/f'chao_{key}.json')for key in ('amy','omochao')}
for key,data in characters.items():
 bones={b['name']:b for b in data['bones']};image=Image.open(T/'gameplay'/f'character_{key}.png').convert('RGBA')
 test(data['character_revision']=='0.20.0',key+' revision')
 test(len(bones)==len(data['bones']),key+' duplicate bone')
 for name,bone in bones.items():
  seen=set();at=name
  while at:
   test(at in bones,key+' missing ancestor '+at);test(at not in seen,key+' cyclic ancestor '+at);seen.add(at);at=bones[at].get('parent')
  test(all(math.isfinite(x)for x in bone.get('pivot',[0,0,0])+bone.get('rotation',[0,0,0])),key+' transform')
  for mesh in bone['meshes']:
   test(all(math.isfinite(x)for v in mesh['vertices']for x in v),key+' nonfinite vertices')
   for face in mesh['faces']:
    test(len(face['v'])==len(face['uv'])and all(0<=i<len(mesh['vertices'])for i in face['v']),key+' invalid face topology')
    # Four supplied reaction faces exceed the atlas. ChaoMeshAsset clamps these
    # specific inherited glyph faces to60.5/1.5 only for gameplay rendering.
    valid_uv=all(0<=p[0]<=data['texture_width']and 0<=p[1]<=data['texture_height']for p in face['uv'])
    inherited_glyph=bone['name'].startswith(('emotion_question','emotion_dizzy'))and bone['name']in native and mesh['faces']==native[bone['name']]['meshes'][0]['faces']
    test(valid_uv or inherited_glyph,key+' UV bounds')
   if mesh.get('material','').startswith(('character_','robot_')):
    bb=bounds(mesh);test(all(bb[i+3]>bb[i]for i in range(3)),key+' collapsed authored cuboid')
    for face in mesh['faces']:
     u,v=face['uv'][0];test(image.getpixel((int(u*image.width/data['texture_width']),int(v*image.height/data['texture_height'])))[3]>0,key+' invisible authored swatch')
 for name,bone in native.items():
  if '__cube_'not in name:
   test(all(bones[name].get(k)==bone.get(k)for k in ('parent','pivot','rotation')),key+' changed semantic joint '+name)
 test(set(data['attachment_landmarks'])=={'eyes'},key+' missing eye attachment')

robot=characters['omochao'];rb={b['name']:b for b in robot['bones']}
for name in ('skull__cube_2','skull__cube_3'):
 test(not rb[name]['meshes'],'robot still has organic raised eye pair')
test(not any(m.get('material')=='lens'for b in robot['bones']for m in b['meshes']),'robot has generic eyes over mechanical eyes')
for side,x in (('left',1.9),('right',-1.9)):
 eye=rb['character_robot_eye_'+side];meshes=eye['meshes'];test(len(meshes)==8,'robot needs one socket+7spiral segments')
 test(sum(m['name']=='eye_socket'for m in meshes)==1,'robot socket count')
 spiral=[m for m in meshes if m['name'].startswith('spiral_')];test(len(spiral)==7,'mechanical spiral incomplete')
 bb=[bounds(m)for m in spiral];connected(bb,'spiral_'+side)
 test(abs((min(b[0]for b in bb)+max(b[3]for b in bb))/2-x)<.01,'robot spiral horizontal center')
 test(abs((min(b[1]for b in bb)+max(b[4]for b in bb))/2+2.35)<.01,'robot spiral vertical center')
 test(bool(rb['arm_'+side+'__cube_0']['meshes']),'robot sleeve fitting has no real upper arm')
for name,bone in rb.items():
 if name.startswith(('wing_','tail')):test(not bone['meshes'],'robot organic remnant '+name)
for name in ('skull__cube_0','body__cube_0','arm_left__cube_1','arm_right__cube_1','foot_left__cube_0','foot_right__cube_0'):
 test([m['faces']for m in rb[name]['meshes']]==[m['faces']for m in native[name]['meshes']],'robot lost inherited native coat UV topology '+name)
test(rb['character_propeller']['parent']=='skull'and rb['character_windup_key']['parent']=='body','mechanical moving parts wrong anchors')

amy=characters['amy'];ab={b['name']:b for b in amy['bones']}
head=[bounds(ab['skull__cube_0']['meshes'][0])]+[bounds(m)for m in ab['character_amy_head']['meshes']]
connected(head+[bounds(m)for m in ab['character_hair']['meshes']],'Amy connected head/quills/ears/fringe')
test('character_amy_band'not in ab,'Amy Chao has an unsupported Amy Rose headband')
for side in ('left','right'):
 hair={m['name']:m for m in ab['character_hair']['meshes']};bottom=bounds(hair[side+'_ear_0']);tip=bounds(hair[side+'_ear_1'])
 test(tip[3]-tip[0]<bottom[3]-bottom[0]and tip[1]<bottom[1],'Amy ears do not taper upward')
 test(side+'_inner_ear'in hair,'Amy missing light inner ear')
test(any(b['meshes']for b in amy['bones']if b['name'].startswith('wing_')),'Amy small wings lost')
test(any(b['meshes']for b in amy['bones']if b['name'].startswith('tail')),'Amy round tail lost')

palette=load(A/'omochao_palette_020.json');skin=Image.open(T/'gameplay/masks/baby.png').convert('RGBA');template=Image.open(T/'gameplay/character_omochao.png').convert('RGBA')
all_pixels=list(palette['fixed_pixels'].values())+list(palette['variable_pixels'].values())
test(len({tuple(v)for v in all_pixels})==len(all_pixels),'hardware/plate palette collision')
occupied=set()
for bone in native.values():
 for mesh in bone['meshes']:
  for face in mesh['faces']:
   uv=face.get('gameplay_uv',face['uv']);sx=template.width/raw['texture_width'];sy=template.height/raw['texture_height']
   occupied.update((x,y)for x in range(math.floor(min(p[0]for p in uv)*sx),math.ceil(max(p[0]for p in uv)*sx))for y in range(math.floor(min(p[1]for p in uv)*sy),math.ceil(max(p[1]for p in uv)*sy)))
for xy in all_pixels:
 test(tuple(xy)not in occupied,'mechanical hardware swatch overlaps native source UV')
 test(not skin.getpixel(tuple(xy))[3],'mechanical hardware swatch inside skin mask')
for image_key in ('red_two','blue_two','blue_mono','jewel_silver_two','jewel_sapphire_two'):
 image=Image.open(T/'official/coats/baby'/f'{image_key}.png').convert('RGBA');test(image.size==template.size,'coat layout dimension mismatch')
 test(sum(1 for rgba,m in zip(image.get_flattened_data(),skin.get_flattened_data())if rgba[3]and m[3])>1000,'insufficient inherited visible coat pixels')
 image_data=bytes(c for rgba,m in zip(image.get_flattened_data(),skin.get_flattened_data())if m[3]for c in rgba)
 globals().setdefault('coat_hashes',set()).add(hashlib.sha256(image_data).hexdigest())
test(len(coat_hashes)==5,'distinct source colors/tone/jewel do not produce distinct visible coat pixels')
test((T/'gameplay/robot_coat_amy.png').exists(),'Amy torso color has no semantic robot remap')
report={'result':'PASS','checks':checks,'characters':['amy','omochao'],'robot_eye_pairs':1,'inherited_native_uv_bones':6,'fixed_hardware_swatches':len(palette['fixed_pixels']),'variable_plate_swatches':len(palette['variable_pixels']),'distinct_actual_source_coats':5,'semantic_joints':'native baby preserved'}
(ROOT/'docs/research/characters020-validation.json').write_text(json.dumps(report,indent=2)+'\n')
print('Characters020:',checks,'independent checks; single mechanical eye pair; native inherited UVs; connected Amy head; PASS')
