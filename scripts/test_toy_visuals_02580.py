"""Ordinary toy light flags and semantic Shadow crown/ear charts; no game launched."""
from pathlib import Path
import json,math,unittest
from PIL import Image
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/chao_garden'
KINDS=('chao_toy_car','chao_sonic_doll','chao_shadow_doll','chao_pogo_stick')

def parse(path):
 vertices=[];coords=[];normals=[];faces=[];material=None
 for line in path.read_text().splitlines():
  values=line.split()
  if not values:continue
  if values[0]=='v':vertices.append(tuple(map(float,values[1:])))
  elif values[0]=='vt':coords.append((float(values[1]),1-float(values[2])))
  elif values[0]=='vn':normals.append(tuple(map(float,values[1:])))
  elif values[0]=='usemtl':material=values[1]
  elif values[0]=='f':
   ids=[tuple(int(x)-1 for x in word.split('/')) for word in values[1:]]
   faces.append({'material':material,'points':[vertices[x[0]] for x in ids],'uv':[coords[x[1]] for x in ids],'normals':[normals[x[2]] for x in ids]})
 return vertices,normals,faces

def samples(face,image):
 uv=face['uv']
 for i in range(13):
  for j in range(13-i):
   a=i/12;b=j/12;c=1-a-b;u=a*uv[0][0]+b*uv[1][0]+c*uv[2][0];v=a*uv[0][1]+b*uv[1][1]+c*uv[2][1]
   yield image.getpixel((min(image.width-1,max(0,int(u*image.width))),min(image.height-1,max(0,int(v*image.height)))))

class ToyVisuals080(unittest.TestCase):
 def test_scene_lighting_is_explicit_for_held_and_placed_active_models(self):
  for kind in KINDS:
   for path in [A/f'models/item/{kind}_hand.json',A/f'models/block/{kind}_idle.json']:
    d=json.loads(path.read_text());self.assertIs(d.get('emissive_ambient'),False,str(path));self.assertIs(d.get('shade_quads'),True)
  material=None
  for line in (A/'models/active02549/materials.mtl').read_text().splitlines():
   words=line.split()
   if words and words[0]=='newmtl':material=words[1]
   if words and words[0] in ('Ka','Ke'):self.assertEqual(tuple(map(float,words[1:4])),(0,0,0),material)
 def test_shadow_keeps_donor_vertices_normals_contact_geometry_and_face(self):
  man=json.loads((A/'models/active02549/manifest.json').read_text());shadow=man['chao_shadow_doll'];sonic=man['chao_sonic_doll']
  self.assertEqual(shadow['offset'],sonic['offset']);self.assertEqual(shadow['contacts'],sonic['contacts'])
  for stage in ('hand','floor'):
   av,an,af=parse(A/f'models/active02549/chao_shadow_doll_{stage}.obj');bv,bn,bf=parse(A/f'models/active02549/chao_sonic_doll_{stage}.obj')
   self.assertEqual(av,bv);self.assertEqual(an,bn);self.assertEqual(len(af),124);self.assertEqual(len(bf),124)
   front=[f for f in af if f['material']=='sa2_soni03_148'];donor=[f for f in bf if f['material']=='sa2_soni03_148'];self.assertEqual(len(front),10)
   for current,old in zip(front,donor):
    self.assertEqual(current['points'],old['points'])
    if all(abs(u-.25)<1e-8 and abs(v-.5)<1e-8 for u,v in old['uv']):
     for u,v in current['uv']:self.assertAlmostEqual(u,1/128);self.assertAlmostEqual(v,1/64)
    else:self.assertEqual(current['uv'],old['uv'])
 def test_crown_has_a_continuous_black_red_chart_without_face_art_bleed(self):
  for stage in ('hand','floor'):
   data=json.loads((A/f'models/{"item/chao_shadow_doll_hand" if stage=="hand" else "block/chao_shadow_doll_idle"}.json').read_text())
   _,_,faces=parse(A/f'models/active02549/chao_shadow_doll_{stage}.obj');caps=[f for f in faces if f['material']=='shadow_crown080'];self.assertEqual(len(caps),9)
   with Image.open(A/('textures/'+data['textures']['shadow_crown080'].split(':')[1]+'.png')) as im:
    im=im.convert('RGBA');colors=[p for f in caps for p in samples(f,im)]
   self.assertEqual(set(colors),{(25,25,31,255),(190,25,40,255)})
   seams={}
   for face in caps:
    for position,coord in zip(face['points'],face['uv']):
     if position in seams:self.assertEqual(coord,seams[position],'Crown texture chart must meet at shared geometry vertices')
     else:seams[position]=coord
 def test_ears_have_tan_front_insets_black_reverse_and_opaque_cloth(self):
  for stage in ('hand','floor'):
   d=json.loads((A/f'models/{"item/chao_shadow_doll_hand" if stage=="hand" else "block/chao_shadow_doll_idle"}.json').read_text())
   _,_,faces=parse(A/f'models/active02549/chao_shadow_doll_{stage}.obj');ears=[f for f in faces if f['material']=='shadow_ears080'];self.assertEqual(len(ears),8)
   with Image.open(A/('textures/'+d['textures']['shadow_ears080'].split(':')[1]+'.png')) as im:
    im=im.convert('RGBA');front=[p for f in ears[::2] for p in samples(f,im)];back=[p for f in ears[1::2] for p in samples(f,im)]
   # First tile is the existing primary SA2 face atlas, before its mirrored repeat.
   with Image.open(A/'textures/block/active02549/chao_shadow_doll/sa2_soni03_148.png') as ref:cream=ref.convert('RGBA').getpixel((17,27))
   self.assertIn(cream,front);self.assertEqual(set(back),{(25,25,31,255)});self.assertTrue(all(c[3]==255 for c in front+back))
   for a,b in zip(ears[::2],ears[1::2]):
    self.assertEqual(a['points'],list(reversed(b['points'])));self.assertLess(sum(x*y for x,y in zip(a['normals'][0],b['normals'][0])),-.999)
if __name__=='__main__':unittest.main(verbosity=2)
