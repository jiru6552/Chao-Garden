"""Independent shipped SA2 horse geometry/contact/resource checks, not client acceptance."""
from pathlib import Path
import hashlib,json,math,unittest
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/chao_garden'
SOURCE=ASSETS/'models/sa2_02515/chao_rocking_horse_idle.obj'

def geometry():
 vertices=[];faces=[];material=''
 for line in SOURCE.read_text().splitlines():
  words=line.split()
  if not words:continue
  if words[0]=='usemtl':material=words[1]
  elif words[0]=='v':vertices.append(tuple(map(float,words[1:])))
  elif words[0]=='f':faces.append((material,[vertices[int(v.split('/')[0])-1] for v in words[1:]]))
 return vertices,faces

def sub(a,b):return tuple(x-y for x,y in zip(a,b))
def dot(a,b):return sum(x*y for x,y in zip(a,b))
def cross(a,b):return (a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0])
def face_distance(face,point):
 a,b,c=face;u=sub(b,a);v=sub(c,a);p=sub(point,a);n=cross(u,v);nn=dot(n,n)
 if nn<1e-20:return 999
 distance=dot(p,n)/math.sqrt(nn);projection=tuple(point[i]-n[i]*dot(p,n)/nn for i in range(3));p=sub(projection,a)
 uu=dot(u,u);vv=dot(v,v);uv=dot(u,v);pu=dot(p,u);pv=dot(p,v);den=uu*vv-uv*uv
 s=(vv*pu-uv*pv)/den;t=(uu*pv-uv*pu)/den
 if s>=-1e-6 and t>=-1e-6 and s+t<=1+1e-6:return abs(distance)
 result=999
 for start,end in [(a,b),(b,c),(c,a)]:
  d=sub(end,start);q=sub(point,start);w=max(0,min(1,dot(q,d)/dot(d,d)));r=tuple(start[i]+w*d[i] for i in range(3));result=min(result,math.dist(point,r))
 return result

class HorseAssets(unittest.TestCase):
 def test_official_idle_and_inventory_meshes_are_unchanged(self):
  self.assertEqual(hashlib.sha256(SOURCE.read_bytes()).hexdigest(),'8df293107bb9c34cf18adf6f9af23fa587098a8e4f438d07e6a53bde18dbb4e5')
  self.assertEqual(hashlib.sha256((ASSETS/'models/item/chao_rocking_horse.json').read_bytes()).hexdigest(),'b6a64fc809c501cff1ca2f29cf6f451cfd0969efc929d6ed6a23c189d4a4eff2')
  self.assertEqual(len(geometry()[1]),124)
 def test_seat_crown_and_adaptive_support_use_actual_back_surface(self):
  faces=[p for m,p in geometry()[1] if m=='sa2_horse02_52' and min(v[1] for v in p)>.38]
  self.assertTrue(faces)
  # The original back has a shallow crown, not a perfectly horizontal slab.
  self.assertLess(min(face_distance(f,(.5,.4283479,.35657245)) for f in faces),1e-6)
  for z in [.32,.34,.36,.38,.40]:
   if z<.35657245:y=.3907218+(z-.3023668)*(.4283479-.3907218)/(.35657245-.3023668)
   else:y=.4283479+(z-.35657245)*(.42717-.4283479)/(.5089568-.35657245)
   self.assertLess(min(face_distance(f,(.5,y,z)) for f in faces),1e-6)
 def test_palms_meet_real_crossbar_not_the_muzzle(self):
  bar=[p for m,p in geometry()[1] if m=='sa2_rappa_gold_52' and min(v[1] for v in p)>.60]
  self.assertTrue(bar)
  for x in [.37,.63]:self.assertLess(min(face_distance(f,(x,.6427,.62125)) for f in bar),.02)
  muzzle=[p for m,p in geometry()[1] if m=='sa2_horse03_52']
  self.assertGreater(min(v[2] for f in muzzle for v in f),.74)
 def test_continuous_original_rocker_clears_ground_and_keeps_rigid_shape(self):
  vertices=geometry()[0]
  for i in range(257):
   angle=math.radians(-6+12*i/256);c=math.cos(angle);s=math.sin(angle)
   for x,y,z in vertices:
    actual=(x,.005+(y-.005)*c-(z-.5)*s,.5+(y-.005)*s+(z-.5)*c)
    self.assertGreaterEqual(actual[1],-1e-7)
    self.assertAlmostEqual(math.dist(actual,(.5,.005,.5)),math.dist((x,y,z),(.5,.005,.5)),places=6)
 def test_continuous_baked_model_reuses_the_existing_source_and_cutout_materials(self):
  idle=json.loads((ASSETS/'models/block/chao_rocking_horse_idle.json').read_text())
  motion=json.loads((ASSETS/'models/item/chao_rocking_horse_motion.json').read_text())
  self.assertEqual(motion,idle);self.assertEqual(motion['render_type'],'minecraft:cutout');self.assertFalse(motion['automatic_culling'])
  self.assertEqual(motion['model'],'chao_garden:models/sa2_02515/chao_rocking_horse_idle.obj')
  self.assertNotIn('display',motion)

if __name__=='__main__':unittest.main(verbosity=2)
