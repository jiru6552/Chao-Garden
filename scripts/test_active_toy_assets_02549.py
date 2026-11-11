"""Focused, independent model/contact/provenance/audio checks for the new active toys."""
from pathlib import Path
import json,math,unittest,subprocess,array
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/chao_garden';MAN=json.loads((A/'models/active02549/manifest.json').read_text())
KINDS=['chao_toy_car','chao_sonic_doll','chao_shadow_doll','chao_pogo_stick']
def vertices(path):return [tuple(map(float,l.split()[1:])) for l in path.read_text().splitlines() if l.startswith('v ')]
class ActiveAssets(unittest.TestCase):
 def test_every_toy_uses_supplied_source_models_with_explicit_shadow_adaptation(self):
  self.assertEqual({k:MAN[k]['source_triangles'] for k in KINDS},{KINDS[0]:80,KINDS[1]:120,KINDS[2]:120,KINDS[3]:100})
  self.assertIn('adaptation',MAN['chao_shadow_doll']['provenance'])
  self.assertEqual(MAN['chao_shadow_doll']['offset'],MAN['chao_sonic_doll']['offset'])
  for k in KINDS:self.assertIn('Supplied SA2B',MAN[k]['provenance'])
 def test_each_floor_model_has_real_ground_contact_and_is_flat(self):
  for k in KINDS:
   ps=vertices(A/f'models/active02549/{k}_floor.obj');lo=[min(p[i] for p in ps) for i in range(3)];hi=[max(p[i] for p in ps) for i in range(3)]
   self.assertAlmostEqual(lo[1],0,places=7);self.assertLessEqual(hi[1],.301)
   if k!=KINDS[0]:self.assertLessEqual(hi[1],.188)
   self.assertGreater(max(hi[i]-lo[i] for i in (0,2)),hi[1]*1.25)
 def test_hand_models_are_distinct_and_keep_all_source_surfaces(self):
  for k in KINDS:
   lines=(A/f'models/active02549/{k}_hand.obj').read_text().splitlines();self.assertEqual(sum(l.startswith('f ') for l in lines),{KINDS[0]:88,KINDS[1]:124,KINDS[2]:124,KINDS[3]:160}[k])
   self.assertNotEqual((A/f'models/active02549/{k}_floor.obj').read_bytes(),(A/f'models/active02549/{k}_hand.obj').read_bytes())
   d=json.loads((A/f'models/item/{k}_hand.json').read_text());self.assertEqual(d['render_type'],'minecraft:cutout')
   for name,path in d['textures'].items():self.assertTrue((A/('textures/'+path.split(':')[1]+'.png')).is_file())
 def test_all_four_orientations_reference_only_the_floor_models(self):
  for k in KINDS:
   variants=json.loads((A/f'blockstates/{k}.json').read_text())['variants'];self.assertEqual(len(variants),32)
   for key,value in variants.items():
    self.assertEqual(value['model'],'chao_garden:block/'+k+'_idle')
    self.assertEqual(value['y'],{'north':0,'east':90,'south':180,'west':270}[key.split(',')[0].split('=')[1]])
 def test_loot_recipes_and_icons_exist_without_new_dependencies(self):
  for k in KINDS:
   recipe=json.loads((ROOT/f'src/main/resources/data/chao_garden/recipe/{k}.json').read_text());self.assertEqual(recipe['result']['id'],'chao_garden:'+k)
   loot=json.loads((ROOT/f'src/main/resources/data/chao_garden/loot_table/blocks/{k}.json').read_text());self.assertEqual(loot['pools'][0]['entries'][0]['name'],'chao_garden:'+k)
   self.assertTrue((A/f'textures/item/{k}.png').is_file())
 def test_original_short_sounds_decode_without_clipping(self):
  for k in ['car','doll','pogo']:
   raw=subprocess.check_output(['ffmpeg','-v','error','-i',str(A/f'sounds/toys/{k}.ogg'),'-f','f32le','-acodec','pcm_f32le','-'])
   samples=array.array('f',raw);peak=max(map(abs,samples));self.assertGreater(peak,.1);self.assertLess(peak,.9);self.assertLess(max(map(abs,samples[-30:])),.005)
if __name__=='__main__':unittest.main(verbosity=2)
