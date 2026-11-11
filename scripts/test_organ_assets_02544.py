"""Independent mesh/audio checks for the existing source organ."""
from pathlib import Path
import json, subprocess, unittest
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/chao_garden'
def mesh(path):
    material='';result=[]
    for line in path.read_text().splitlines():
        if line.startswith('usemtl '):material=line.split()[1]
        if line.startswith('v '):result.append((material,tuple(map(float,line.split()[1:]))))
    return result
class OrganAssets(unittest.TestCase):
    def test_keys_are_on_north_front_of_cabinet(self):
        vertices=mesh(ASSETS/'models/sa2_02515/chao_organ_idle.obj')
        keys=[v for material,v in vertices if material=='sa2_piano01_20']
        self.assertEqual(len(keys),6)
        for x,y,z in keys:
            self.assertAlmostEqual(y,.18667,places=5)
            self.assertLess(z,.5)
        self.assertAlmostEqual(min(v[2] for v in keys),.39713,places=5)
        self.assertAlmostEqual(max(v[2] for v in keys),.48796,places=5)
        for facing,rotation in [('north',0),('east',90),('south',180),('west',270)]:
            states=json.loads((ASSETS/'blockstates/chao_organ.json').read_text())['variants']
            for key,value in states.items():
                if key.startswith('facing='+facing+','):self.assertEqual(value.get('y',0),rotation)
    def test_cabinet_never_moves_and_only_keys_depress(self):
        source=mesh(ASSETS/'models/sa2_02515/chao_organ_idle.obj')
        for frame in range(4):
            current=mesh(ASSETS/f'models/sa2_02515/chao_organ_play_{frame}.obj')
            self.assertEqual(len(current),len(source))
            for (name,expected),(actual_name,actual) in zip(source,current):
                self.assertEqual(name,actual_name)
                offset=.003 if name=='sa2_piano01_20' and frame==1 else 0
                for axis in range(3):self.assertAlmostEqual(actual[axis],expected[axis]-(offset if axis==1 else 0),places=6)
    def test_original_organ_tone_decodes_without_clipping(self):
        import array
        path=ASSETS/'sounds/toys/organ.ogg'
        meta=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_streams','-of','json',str(path)]))['streams'][0]
        self.assertEqual(meta['channels'],1);self.assertEqual(int(meta['sample_rate']),44100)
        self.assertAlmostEqual(float(meta['duration']),.60,places=2)
        samples=array.array('f',subprocess.check_output(['ffmpeg','-v','error','-i',str(path),'-f','f32le','-acodec','pcm_f32le','-']))
        self.assertGreater(max(map(abs,samples)),.3);self.assertLess(max(map(abs,samples)),.95)
        self.assertLess(max(map(abs,samples[:100])),.05);self.assertLess(max(map(abs,samples[-100:])),.05)
if __name__=='__main__':unittest.main(verbosity=2)
