"""Independent primary-texture, UV, GUI isolation and car-scale checks for0.25.51."""
from pathlib import Path
import sys,json,hashlib,struct,unittest
from PIL import Image,ImageOps
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'art'))
import extract_sa2_02515 as decode
A=ROOT/'src/main/resources/assets/chao_garden'
M=json.loads((A/'models/toy_polish02551/manifest.json').read_text())
QUIET=('chao_broom','chao_picture_book','chao_crayons')
def canonical(value):return json.dumps(value,sort_keys=True,separators=(',',':')).encode()
def rows(path,key):return [tuple(map(float,l.split()[1:])) for l in path.read_text().splitlines() if l.startswith(key+' ')]
class ToyPolishAssets(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  raw=decode.prs((ROOT/'art/sa2_02514/original/al_toy.prs').read_bytes());names=decode.texture_names(raw);p=8+struct.unpack_from('<I',raw,4)[0]
  for name in names:
   length=struct.unpack_from('<I',raw,p+4)[0]
   if name=='sh_doll01':cls.reference=decode.decode(raw[p:p+8+length]).convert('RGBA');break
   p+=8+length
  else:raise AssertionError('Missing primary Shadow doll face texture')
 def test_primary_shadow_face_is_wrapped_and_mirrored_without_stretching(self):
  folder=A/'textures/block/active02549/chao_shadow_doll'
  left=Image.open(folder/'sa2_soni03_52.png').convert('RGBA')
  atlas=Image.open(folder/'sa2_soni03_148.png').convert('RGBA')
  self.assertEqual(left.size,(32,32));self.assertEqual(atlas.size,(64,32))
  self.assertEqual(left.tobytes(),self.reference.tobytes())
  self.assertEqual(atlas.crop((0,0,32,32)).tobytes(),self.reference.tobytes())
  self.assertEqual(atlas.crop((32,0,64,32)).tobytes(),ImageOps.mirror(self.reference).tobytes())
  primary=json.loads((A/'models/active02549/manifest.json').read_text())['shadow_face_02551']
  self.assertEqual(primary['source_rgba_sha256'],hashlib.sha256(self.reference.tobytes()).hexdigest())
 def test_shadow_retains_donor_surfaces_and_only_retargets_the_black_nose(self):
  root=A/'models/active02549';sonic=root/'chao_sonic_doll_hand.obj';shadow=root/'chao_shadow_doll_hand.obj'
  self.assertEqual(rows(sonic,'v'),rows(shadow,'v'));self.assertEqual(rows(sonic,'vn'),rows(shadow,'vn'))
  before=rows(sonic,'vt');after=rows(shadow,'vt');changed=[(a,b) for a,b in zip(before,after) if a!=b]
  self.assertEqual(len(before),len(after));self.assertEqual(len(changed),12) # four original triangles
  atlas=Image.open(A/'textures/block/active02549/chao_shadow_doll/sa2_soni03_148.png').convert('RGBA')
  for old,new in changed:
   self.assertEqual(old,(.25,.5));self.assertEqual(new,(.0078125,.984375))
   rgb=atlas.getpixel((int(new[0]*atlas.width),int((1-new[1])*atlas.height)))
   self.assertTrue(max(rgb[:3])<32 and rgb[3]==255,'nose must use black, not the red pupil')
 def test_car_is_uniformly_seventy_percent_and_keeps_the_floor_plane(self):
  points=rows(A/'models/active02549/chao_toy_car_floor.obj','v');bounds=[min(p[i] for p in points)*16 for i in range(3)]+[max(p[i] for p in points)*16 for i in range(3)]
  old=M['car_floor_baseline_bounds_pixels'];expected=[(8+(v-8)*.7) if i%3!=1 else v*.7 for i,v in enumerate(old)]
  for a,b in zip(bounds,expected):self.assertAlmostEqual(a,b,places=6)
  self.assertEqual(bounds[1],0);self.assertAlmostEqual(bounds[4],3.36,places=6)
  for facing in range(4):
   # All block rotations turn around Y and therefore retain the same ground plane.
   import math
   c,s=round(math.cos(facing*math.pi/2)),round(math.sin(facing*math.pi/2))
   transformed=[((p[0]-.5)*c-(p[2]-.5)*s+.5,p[1],(p[0]-.5)*s+(p[2]-.5)*c+.5) for p in points]
   self.assertEqual(min(p[1] for p in transformed),0)
 def test_gui_models_keep_the_exact_previous_non_gui_item_payload(self):
  for name in QUIET:
   item=json.loads((A/'models/item'/(name+'.json')).read_text())
   self.assertEqual(item['loader'],'neoforge:separate_transforms')
   self.assertEqual(set(item['perspectives']),{'gui'})
   self.assertEqual(item['perspectives']['gui'],{'parent':'minecraft:item/generated','textures':{'layer0':'chao_garden:item/'+name}})
   self.assertEqual(hashlib.sha256(canonical(item['base'])).hexdigest(),M['non_gui_item_model_sha256'][name])
   self.assertEqual(item['base'],json.loads((A/'models/block'/(name+'.json')).read_text()))
 def test_all_protected_held_floor_and_material_assets_are_unchanged(self):
  self.assertEqual(len(M['protected_sha256']),63)
  for path,expected in M['protected_sha256'].items():self.assertEqual(hashlib.sha256((A/path).read_bytes()).hexdigest(),expected,path)
 def test_inventory_silhouettes_show_broad_recognizable_source_faces(self):
  for name in QUIET:
   image=Image.open(A/'textures/item'/(name+'.png')).convert('RGBA');self.assertEqual(image.size,(32,32))
   alpha=image.getchannel('A');box=alpha.getbbox();self.assertIsNotNone(box)
   width,height=box[2]-box[0],box[3]-box[1]
   self.assertGreaterEqual(max(width,height),27)
   area=sum(a>127 for a in alpha.tobytes())
   if name=='chao_broom':
    self.assertGreater(area,80);self.assertGreaterEqual(min(width,height),14)
    self.assertGreater(sum(r>130 and g>100 and b<100 and a>127 for r,g,b,a in zip(image.tobytes()[0::4],image.tobytes()[1::4],image.tobytes()[2::4],image.tobytes()[3::4])),20)
   else:
    self.assertGreater(area,300);self.assertGreaterEqual(min(width,height),20)
if __name__=='__main__':unittest.main(verbosity=2)
