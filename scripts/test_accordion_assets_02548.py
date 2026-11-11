"""Preserve the supplied accordion surfaces, expose its real keys, and decode the original reeds."""
from pathlib import Path
import array,collections,hashlib,json,math,subprocess,unittest

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/chao_garden'
SOURCE=ASSETS/'models/cwe02516/chao_accordion.obj'

def dot(a,b):return sum(x*y for x,y in zip(a,b))
def length(a):return math.sqrt(dot(a,a))
def subtract(a,b):return tuple(x-y for x,y in zip(a,b))
def cross(a,b):return a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]
def faces(path):
 vertices=[];uv=[];normals=[];out=[];material=''
 for line in path.read_text().splitlines():
  a=line.split()
  if not a:continue
  if a[0]=='v':vertices.append(tuple(map(float,a[1:])))
  elif a[0]=='vt':uv.append(tuple(map(float,a[1:])))
  elif a[0]=='vn':normals.append(tuple(map(float,a[1:])))
  elif a[0]=='usemtl':material=a[1]
  elif a[0]=='f':out.append((material,[vertices[int(w.split('/')[0])-1] for w in a[1:]],
                            [uv[int(w.split('/')[1])-1] for w in a[1:]],normals[int(a[1].split('/')[2])-1]))
 return out

class AccordionAssets(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  cls.source=faces(SOURCE)
  cls.parts={part:faces(ASSETS/f'models/accordion02548/{part}.obj') for part in ('keys','bass','bellows')}
  cls.manifest=json.loads((ASSETS/'models/accordion02548/manifest.json').read_text())

 def test_existing_item_and_all_source_material_uv_triangles_are_preserved(self):
  item=json.loads((ASSETS/'models/item/chao_accordion.json').read_text())
  self.assertEqual(item['base']['parent'],'chao_garden:item/cwe02516/chao_accordion')
  self.assertEqual(self.manifest['source_sha256'],hashlib.sha256(SOURCE.read_bytes()).hexdigest())
  def signatures(mesh):return collections.Counter((m,tuple(tuple(round(v,7) for v in p) for p in uv)) for m,_,uv,_ in mesh)
  generated=[face for part in self.parts.values() for face in part]
  self.assertEqual(len(generated),619)
  self.assertEqual(signatures(self.source),signatures(generated))
  self.assertEqual({p:len(mesh) for p,mesh in self.parts.items()},{'keys':63,'bass':24,'bellows':532})

 def test_real_textured_key_front_faces_out_and_rigid_ends_keep_their_shape(self):
  # This sign comes from the actual source's outward piano-key triangles,
  # independently of the Java prop matrix or generated manifest vectors.
  front=(.8329784336,-.2746999560,.4802987230)
  expansion=(.5529550825,.3824004520,-.7402773609)
  originals={'keys':[],'bass':[],'bellows':[]}
  for face in self.source:
   material,points,_,_=face
   part='keys' if 'piano01' in material else ('keys' if sum(dot(p,expansion) for p in points)<0 else 'bass') if 'kanoke03' in material else 'bellows'
   originals[part].append(face)
  actual_fronts=0
  for part in ('keys','bass'):
   for original,generated in zip(originals[part],self.parts[part]):
    for a,b in ((0,1),(1,2),(2,0)):
     self.assertAlmostEqual(length(subtract(original[1][a],original[1][b])),length(subtract(generated[1][a],generated[1][b])),places=6)
    if 'piano01' in original[0] and dot(original[3],front)>.999:
     actual_fronts+=1;self.assertLess(generated[3][2],-.999)
     self.assertAlmostEqual(generated[1][0][2],.543575,places=4)
  self.assertGreaterEqual(actual_fronts,2)
  for mesh in self.parts.values():
   for _,points,_,normal in mesh:
    self.assertAlmostEqual(length(normal),1,places=6)
    self.assertGreater(length(cross(subtract(points[1],points[0]),subtract(points[2],points[0]))),1e-10)

 def test_baked_parts_use_existing_opaque_cutout_materials_and_no_extra_item(self):
  source=json.loads((ASSETS/'models/item/cwe02516/chao_accordion.json').read_text())
  for part in self.parts:
   model=json.loads((ASSETS/f'models/item/chao_accordion_hand_{part}.json').read_text())
   self.assertEqual(model['textures'],source['textures'])
   self.assertEqual(model['render_type'],'minecraft:cutout')
   self.assertFalse(model['automatic_culling'])
   self.assertNotIn('display',model)
   for value in model['textures'].values():self.assertTrue((ASSETS/('textures/'+value.split(':')[1]+'.png')).is_file())

 def test_straightened_bellows_seams_stay_joined_during_compression(self):
  # The actual rigid inner seams are vertical after source fan removal; the
  # flexible pleats slightly overlap them, throughout the complete stroke.
  key_x=[p[0]-.5 for _,points,_,_ in self.parts['keys'] for p in points]
  bass_x=[p[0]-.5 for _,points,_,_ in self.parts['bass'] for p in points]
  bellows_x=[p[0]-.5 for _,points,_,_ in self.parts['bellows'] for p in points]
  self.assertTrue(any(abs(x+.16)<1e-6 for x in key_x))
  self.assertAlmostEqual(min(bass_x),.16,places=6)
  for expansion in (0,.25,.5,.75,1):
   width=.32*(.65+.65*expansion);change=(width-.32)/2
   self.assertLess(min(bellows_x)*width/.32,-.16-change)
   self.assertGreater(max(bellows_x)*width/.32,.16+change)

 def test_bass_grip_is_on_the_genuine_outer_side(self):
  point=self.manifest['bass_contact'];grip_normal=self.manifest['bass_contact_normal'];matches=0
  for _,vertices,_,normal in self.parts['bass']:
   if normal[0]<.9 or normal[2]>-.4:continue
   a,b,c=[tuple(v-.5 for v in p) for p in vertices]
   u,v,p=subtract(b,a),subtract(c,a),subtract(point,a)
   if abs(dot(normal,p))>1e-6:continue
   uu,vv,uv,pu,pv=dot(u,u),dot(v,v),dot(u,v),dot(p,u),dot(p,v)
   denominator=uu*vv-uv*uv
   first,second=(vv*pu-uv*pv)/denominator,(uu*pv-uv*pu)/denominator
   if first>=-1e-6 and second>=-1e-6 and first+second<=1+1e-6:
    matches+=1;self.assertGreater(dot(normal,grip_normal),.99999)
  self.assertGreater(matches,0)
  self.assertLess(abs(point[2]),.001)

 def test_original_reed_tone_decodes_with_safe_envelope_and_registration(self):
  data=json.loads((ASSETS/'sounds.json').read_text())['toy.accordion']
  self.assertEqual(len(data['sounds']),1)
  self.assertEqual(data['sounds'][0]['name'],'chao_garden:toys/accordion')
  self.assertFalse(data['sounds'][0].get('stream',False))
  lang=json.loads((ASSETS/'lang/en_us.json').read_text());self.assertIn('Accordion',lang[data['subtitle']])
  path=ASSETS/'sounds/toys/accordion.ogg'
  meta=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_streams','-of','json',str(path)]))['streams'][0]
  self.assertEqual(meta['channels'],1);self.assertEqual(int(meta['sample_rate']),44100)
  self.assertAlmostEqual(float(meta['duration']),.68,places=2)
  samples=array.array('f',subprocess.check_output(['ffmpeg','-v','error','-i',str(path),'-f','f32le','-acodec','pcm_f32le','-']))
  self.assertGreater(max(map(abs,samples)),.3);self.assertLess(max(map(abs,samples)),.90)
  self.assertLess(abs(samples[0]),.04);self.assertLess(max(map(abs,samples[-100:])),.04)
  # Harmonic energy must include both the principal free reed and its octave.
  section=samples[4410:22050]
  def energy(hz):
   real=sum(v*math.cos(2*math.pi*hz*i/44100) for i,v in enumerate(section))
   imag=sum(v*math.sin(2*math.pi*hz*i/44100) for i,v in enumerate(section))
   return math.hypot(real,imag)/len(section)
  self.assertGreater(energy(261.625565),.015);self.assertGreater(energy(523.25113),.005)

if __name__=='__main__':unittest.main(verbosity=2)
