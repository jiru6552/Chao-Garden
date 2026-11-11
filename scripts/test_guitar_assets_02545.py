"""The playable guitar must use its measured source mesh and a decodable, unclipped strum."""
from pathlib import Path
import array,json,subprocess,unittest
ROOT=Path(__file__).resolve().parents[1];ASSETS=ROOT/'src/main/resources/assets/chao_garden'
class GuitarAssets(unittest.TestCase):
 def test_existing_item_uses_measured_cwe_guitar(self):
  item=json.loads((ASSETS/'models/item/chao_guitar.json').read_text())
  self.assertEqual(item['base']['parent'],'chao_garden:item/cwe02516/chao_guitar')
  mesh=json.loads((ASSETS/'models/item/cwe02516/chao_guitar.json').read_text())
  self.assertEqual(mesh['model'],'chao_garden:models/cwe02516/chao_guitar.obj')
  vertices=[tuple(map(float,l.split()[1:])) for l in (ASSETS/'models/cwe02516/chao_guitar.obj').read_text().splitlines() if l.startswith('v ')]
  self.assertGreater(len(vertices),100)
  self.assertAlmostEqual(min(v[0] for v in vertices),.0625,places=5)
  self.assertAlmostEqual(max(v[0] for v in vertices),.9375,places=5)
 def test_custom_sound_and_subtitle_are_registered(self):
  data=json.loads((ASSETS/'sounds.json').read_text())['toy.guitar']
  self.assertEqual(data['sounds'],[{'name':'chao_garden:toys/guitar','stream':False}])
  lang=json.loads((ASSETS/'lang/en_us.json').read_text());self.assertEqual(lang[data['subtitle']],'Guitar strums')
 def test_chord_decodes_and_has_safe_ends_and_peak(self):
  path=ASSETS/'sounds/toys/guitar.ogg'
  meta=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_streams','-of','json',str(path)]))['streams'][0]
  self.assertEqual(meta['channels'],1);self.assertEqual(int(meta['sample_rate']),44100)
  self.assertAlmostEqual(float(meta['duration']),.52,places=2)
  samples=array.array('f',subprocess.check_output(['ffmpeg','-v','error','-i',str(path),'-f','f32le','-acodec','pcm_f32le','-']))
  self.assertGreater(max(map(abs,samples)),.3);self.assertLess(max(map(abs,samples)),.95)
  self.assertLess(abs(samples[0]),.05);self.assertLess(max(map(abs,samples[-100:])),.05)
if __name__=='__main__':unittest.main(verbosity=2)
