"""Articulate the supplied CWE accordion and synthesize an original free-reed tone.

The source is fan-open. Straighten its flexible pleats around the measured fan
hinge, keeping both rigid ends rigid, so continuous bellows motion needs only
three ordinary baked OBJ models. The original item and placed meshes stay intact.
"""
from pathlib import Path
import hashlib,json,math,struct,subprocess,tempfile,wave

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/chao_garden'
SOURCE=ASSETS/'models/cwe02516/chao_accordion.obj'
FRONT=(.8329784336,-.2746999560,.4802987230)
UP=(.0196877097,.8822186965,.4704280663)
EXPANSION=(.5529550825,.3824004520,-.7402773609)
SEAM=-.01271294063
HINGE=-.09564203165
ANGLE=.42275815876
WIDTH=.32
CENTER_U=.65
CENTER_FRONT=.509

def dot(a,b):return sum(x*y for x,y in zip(a,b))
def source_coordinates(point):return dot(point,EXPANSION),dot(point,UP),dot(point,FRONT)

def part_for(material,points):
 if 'piano01' in material:return 'keys'
 if 'kanoke03' in material:
  # There are no triangles crossing the two separate rigid ends.
  return 'keys' if sum(source_coordinates(p)[0] for p in points)/len(points)<0 else 'bass'
 return 'bellows'

def canonical(point,part):
 e,u,front=source_coordinates(point)
 if part=='bellows':
  de,du=e-SEAM,u-HINGE
  e=SEAM+WIDTH*math.atan2(de,du)/ANGLE
  u=HINGE+math.hypot(de,du)
 elif part=='bass':
  de,du=e-SEAM,u-HINGE
  e=SEAM+WIDTH+math.cos(ANGLE)*de-math.sin(ANGLE)*du
  u=HINGE+math.sin(ANGLE)*de+math.cos(ANGLE)*du
 # Baked items are centered by ItemRenderer. Model axes are Chao X, down Y,
 # back Z: the keyboard is on the right and both visible fronts face outward.
 return e-SEAM-WIDTH/2+.5,CENTER_U-u+.5,CENTER_FRONT-front+.5

def source_faces():
 vertices=[];uv=[];faces=[];material=''
 for line in SOURCE.read_text().splitlines():
  words=line.split()
  if not words:continue
  if words[0]=='v':vertices.append(tuple(map(float,words[1:])))
  elif words[0]=='vt':uv.append(tuple(map(float,words[1:])))
  elif words[0]=='usemtl':material=words[1]
  elif words[0]=='f':
   faces.append((material,[vertices[int(w.split('/')[0])-1] for w in words[1:]],
                 [uv[int(w.split('/')[1])-1] for w in words[1:]]))
 return faces

def normal(points):
 a=[points[1][i]-points[0][i] for i in range(3)]
 b=[points[2][i]-points[0][i] for i in range(3)]
 n=[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]]
 length=math.sqrt(dot(n,n))
 if length<1e-12:raise ValueError('Collapsed accordion triangle')
 return [v/length for v in n]

def models():
 faces=source_faces();parts={name:[] for name in ('keys','bass','bellows')}
 for material,points,uv in faces:
  part=part_for(material,points)
  parts[part].append((material,[canonical(p,part) for p in points],uv))
 template=json.loads((ASSETS/'models/item/cwe02516/chao_accordion.json').read_text())
 template.pop('display',None)
 directory=ASSETS/'models/accordion02548';directory.mkdir(parents=True,exist_ok=True)
 records={}
 for part,triangles in parts.items():
  lines=['# Original CWE accordion surfaces; articulated source adaptation 0.25.48.',
         'mtllib chao_garden:models/cwe02516/materials.mtl']
  vertices=[]
  for index,(material,points,uv) in enumerate(triangles):
   lines.append('usemtl '+material)
   lines.extend('v '+' '.join(f'{v:.9f}' for v in p) for p in points)
   lines.extend('vt '+' '.join(f'{v:.9f}' for v in p) for p in uv)
   lines.append('vn '+' '.join(f'{v:.9f}' for v in normal(points)))
   first=3*index+1;n=index+1
   lines.append('f '+' '.join(f'{first+i}/{first+i}/{n}' for i in range(3)))
   vertices.extend(points)
  (directory/(part+'.obj')).write_text('\n'.join(lines)+'\n')
  data={**template,'model':f'chao_garden:models/accordion02548/{part}.obj'}
  (ASSETS/'models/item'/('chao_accordion_hand_'+part+'.json')).write_text(json.dumps(data,indent=2)+'\n')
  records[part]={'triangles':len(triangles),'bounds':[[min(p[i] for p in vertices),max(p[i] for p in vertices)] for i in range(3)]}
 front_contact=[v-.5 for v in canonical(tuple(EXPANSION[i]*.346+UP[i]*.596+FRONT[i]*.63121 for i in range(3)),'bass')]
 # A convex blend of the two actual outer-side triangle centroids gives a
 # stable grip on the sloping end, rather than reaching across its far front.
 side_faces=[points for _,points,_ in parts['bass'] if normal(points)[0]>.9 and abs(normal(points)[1])<.00001 and normal(points)[2]<-.4]
 if len(side_faces)!=2:raise ValueError('Expected two genuine bass-side triangles')
 centers=[[sum(point[i] for point in points)/3-.5 for i in range(3)] for points in side_faces]
 fraction=(front_contact[1]-centers[0][1])/(centers[1][1]-centers[0][1])
 side_contact=[centers[0][i]+fraction*(centers[1][i]-centers[0][i]) for i in range(3)]
 # This authored side is vertical, so slide the support lower on that same
 # plane to leave room for the large adult Power faces above the wrist.
 side_contact[1]+=.08
 manifest={'source':'models/cwe02516/chao_accordion.obj','source_sha256':hashlib.sha256(SOURCE.read_bytes()).hexdigest(),
           'front':FRONT,'up':UP,'expansion':EXPANSION,'key_seam':SEAM,'fan_hinge_u':HINGE,'source_fan_radians':ANGLE,
           'bellows_rest_width':WIDTH,'center_u':CENTER_U,'center_front':CENTER_FRONT,'parts':records,
           'key_contact':[v-.5 for v in canonical(tuple(EXPANSION[i]*(-.225)+UP[i]*.53+FRONT[i]*.465425 for i in range(3)),'keys')],
           'bass_contact':side_contact,'bass_contact_normal':normal(side_faces[0]),'bass_front_reference':front_contact,
           'provenance':'Original supplied CWE triangles, materials and UVs; fan straightened only for the handheld articulation. No new item.'}
 (directory/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
 print('Accordion source articulation:',{k:v['triangles'] for k,v in records.items()})
 print('Contacts:',manifest['key_contact'],manifest['bass_contact'])

def sound():
 rate=44100;duration=.68;values=[]
 # Two slightly detuned free reeds, a quieter octave reed and a low reed.
 # Their harmonic envelopes and breath onset are synthesized, not sampled.
 for i in range(round(rate*duration)):
  t=i/rate
  attack=min(1,t/.018);release=min(1,(duration-t)/.095)
  breath=1+.025*math.sin(2*math.pi*4.7*t)
  sample=0
  for hz,gain,phase in ((261.625565,.47,0),(262.48,.31,.23),(523.25113,.15,.12),(130.81278,.12,.47)):
   reed=sum(math.sin(2*math.pi*hz*h*t+phase*h)/(h**1.24)*(1 if h%2 else .66)
            for h in range(1,13))
   sample+=gain*reed
  values.append(sample*attack*release*breath)
 peak=max(map(abs,values));pcm=[round(v/peak*.66*32767) for v in values]
 target=ASSETS/'sounds/toys/accordion.ogg';target.parent.mkdir(parents=True,exist_ok=True)
 with tempfile.TemporaryDirectory() as tmp:
  wav=Path(tmp)/'accordion.wav'
  with wave.open(str(wav),'wb') as out:
   out.setnchannels(1);out.setsampwidth(2);out.setframerate(rate)
   out.writeframes(struct.pack('<'+'h'*len(pcm),*pcm))
  subprocess.run(['ffmpeg','-v','error','-y','-i',str(wav),'-fflags','+bitexact','-flags:a','+bitexact','-c:a','libvorbis','-q:a','5',str(target)],check=True)
 print('Original 0.68-second accordion free-reed tone generated')

if __name__=='__main__':models();sound()
