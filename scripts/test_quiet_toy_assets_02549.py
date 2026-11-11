"""Independent original-surface, floor, resource and audio tests for quiet toys."""
from pathlib import Path
import sys,json,math,struct,unittest,array,subprocess,re
import numpy as np
from PIL import Image
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'art'))
import extract_sa2_02515 as source
A=ROOT/'src/main/resources/assets/chao_garden';D=A/'models/quiet02549'
def obj(name):
 v=[];faces=[];normals=[];uv=[];mat=''
 for line in (D/(name+'.obj')).read_text().splitlines():
  w=line.split()
  if not w:continue
  if w[0]=='v':v.append(tuple(map(float,w[1:])))
  elif w[0]=='vt':uv.append(tuple(map(float,w[1:])))
  elif w[0]=='vn':normals.append(tuple(map(float,w[1:])))
  elif w[0]=='usemtl':mat=w[1]
  elif w[0]=='f':faces.append((mat,[v[int(t.split('/')[0])-1] for t in w[1:]]))
 return v,faces,normals,uv
class QuietAssets(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  cls.binary=source.relocate(source.prs((ROOT/'art/sa2_02514/original/ChaoMain.prs').read_bytes()));cls.names=source.texture_names(source.prs((ROOT/'art/sa2_02514/original/al_toy.prs').read_bytes()));cls.manifest=json.loads((D/'manifest.json').read_text())
 def test_actual_supplied_surface_positions_and_materials_preserved(self):
  total=0
  for part in ['broom','book_left','book_right','book_page','crayon_box','crayon']:
   info=self.manifest['parts'][part];raw=source.chunk(self.binary,int(info['address'],16),self.names);_,current,_,_=obj('hand_'+part);index=0
   self.assertEqual(len(raw),info['source_triangles']);total+=len(raw)
   for face in raw:
    material,points=current[index];expected=[(p[0]+.5,.5-p[1],.5-p[2]) for p in face['points']]
    self.assertEqual(material,'sa2_'+face['texture']+'_'+str(face['wrap_flags']))
    for p,q in zip(points,expected):np.testing.assert_allclose(p,q,atol=1e-8)
    index+=1
    if face['double_sided']:
     self.assertEqual(current[index][0],material);np.testing.assert_allclose(current[index][1],list(reversed(points)),atol=1e-8);index+=1
   self.assertEqual(index,len(current))
  self.assertEqual(total,166)
 def test_world_models_lie_on_ground_with_actual_measured_bounds(self):
  for name,expected_long_axis in [('chao_broom',2),('chao_picture_book',2),('chao_crayons',0)]:
   points=np.array(obj(name+'_floor')[0]);bounds=np.r_[points.min(0),points.max(0)]
   np.testing.assert_allclose(bounds,self.manifest['parts'][name]['world_bounds'],atol=1e-8)
   self.assertAlmostEqual(bounds[1],0,places=8);self.assertLess(bounds[4],.22)
   sizes=points.max(0)-points.min(0);self.assertEqual(int(sizes.argmax()),expected_long_axis)
   self.assertGreater(max(sizes[0],sizes[2]),sizes[1]*2.9)
 def test_all_four_block_orientations_and_no_held_floor_alias(self):
  for name in ['chao_broom','chao_picture_book','chao_crayons']:
   block=json.loads((A/'models/block'/(name+'.json')).read_text());item=json.loads((A/'models/item'/(name+'.json')).read_text())
   self.assertIn('display',item);states=json.loads((A/'blockstates'/(name+'.json')).read_text())['variants']
   for facing,angle in [('north',0),('east',90),('south',180),('west',270)]:self.assertEqual(states['facing='+facing]['y'],angle)
   self.assertTrue(block['model'].endswith(name+'_floor.obj'))
  for p in (A/'models/item').glob('chao_quiet_hand_*.json'):
   data=json.loads(p.read_text());self.assertNotIn('_floor',data['model']);self.assertNotIn('display',data)
 def test_texture_normals_and_exact_custom_material_names_resolve(self):
  for p in (A/'models/item').glob('chao_quiet_hand_*.json'):
   data=json.loads(p.read_text());self.assertEqual(data['render_type'],'minecraft:cutout')
   for key,path in data['textures'].items():self.assertTrue((A/'textures'/(path.split(':',1)[1]+'.png')).is_file(),path)
   _,faces,normals,uv=obj(Path(data['model']).stem)
   for mat,_ in faces:self.assertIn(mat,data['textures'])
   for n in normals:self.assertAlmostEqual(sum(v*v for v in n),1,places=5)
   for u,v in uv:self.assertTrue(0<=u<=1 and 0<=v<=1)
 def test_drawing_is_temporary_paper_with_eight_incremental_strokes(self):
  for stage in range(9):
   _,faces,_,_=obj('hand_paper_'+str(stage));self.assertEqual(len(faces),2+stage)
   self.assertEqual([mat for mat,_ in faces[2:]],['quiet_ink_'+str(i) for i in range(stage)])
   for mat,vertices in faces[2:]:
    a=np.array(vertices[1])-vertices[0];b=np.array(vertices[2])-vertices[0];n=np.cross(a,b);n=n/np.linalg.norm(n);self.assertLess(n[1],-.999,'ink faces upwards in Chao model space')
  java=(ROOT/'src/main/java/com/chaogarden/client/ChaoQuietToyAnimation049.java').read_text();track=re.search(r'STROKES=\{(.*?)\};',java).group(1)
  values=[float(v) for v in re.findall(r'(-?[0-9]*\.[0-9]+)f',track)];np.testing.assert_allclose(np.array(values).reshape(8,4),self.manifest['strokes'],atol=1e-7)
  self.assertEqual(len(self.manifest['strokes']),8)
  self.assertIn('New paper/ink presentation',self.manifest['provenance'])
 def test_broom_brush_and_side_box_stay_on_ground_during_presentation(self):
  vertices=np.array(obj('hand_broom')[0])-.5
  for scale in [.64,.80,.86]:
   for sweep in np.linspace(-1,1,101):
    a=.10*sweep;c,s=math.cos(a),math.sin(a);support=1.8833337*c+.5665011*abs(s)
    y=(vertices[:,0]*s+(vertices[:,1]-support)*c)*.23
    ground=.008-y.max()*scale;self.assertGreaterEqual(ground,-.000001);self.assertLess(ground,.02)
  box=np.array(obj('hand_crayon_box')[0])-.5
  self.assertAlmostEqual(float(box[:,1].max()),.1,places=6)
 def test_new_items_have_one_recoverable_loot_and_explicit_recipes(self):
  for name in ['chao_broom','chao_picture_book','chao_crayons']:
   recipe=json.loads((ROOT/'src/main/resources/data/chao_garden/recipe'/(name+'.json')).read_text());self.assertEqual(recipe['result']['id'],'chao_garden:'+name)
   loot=json.loads((ROOT/'src/main/resources/data/chao_garden/loot_table/blocks'/(name+'.json')).read_text());self.assertEqual(loot['pools'][0]['entries'][0]['name'],'chao_garden:'+name);self.assertEqual(loot['pools'][0]['rolls'],1)
 def test_original_sound_samples_decode_with_safe_envelopes(self):
  for name,duration in [('broom',.29),('book_page',.32),('crayon',.48)]:
   path=A/'sounds/toys'/(name+'.ogg');meta=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_streams','-of','json',str(path)]))['streams'][0]
   self.assertEqual(meta['channels'],1);self.assertEqual(int(meta['sample_rate']),44100);self.assertAlmostEqual(float(meta['duration']),duration,places=2)
   samples=array.array('f',subprocess.check_output(['ffmpeg','-v','error','-i',str(path),'-f','f32le','-acodec','pcm_f32le','-']))
   self.assertGreater(max(map(abs,samples)),.3);self.assertLess(max(map(abs,samples)),.8);self.assertLess(max(map(abs,samples[:100])),.15);self.assertLess(max(map(abs,samples[-100:])),.05)
if __name__=='__main__':unittest.main(verbosity=2)
