#!/usr/bin/env python3
"""Author fixed adult Kindergarten professions on the native Chao skeleton.

The source Chao mesh/PNG stay untouched. New complete role meshes share its
semantic joints and atlas pixels, with separate stepped profession details.
"""
from pathlib import Path
import copy,json,math,hashlib
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/chao_garden'
PALETTE={
 'skin':'#84edf4','skin_shadow':'#64cbd7','white':'#f0f3ec','hair':'#d6dfe0',
 'hair_dark':'#aababe','ink':'#203e4b','gold':'#ebc54b','gold_shadow':'#b38d2d',
 'purple':'#8056ac','purple_dark':'#4e3376','violet':'#bda5d4','silver':'#b8c7ca',
 'silver_light':'#e4ebdf','blue':'#416ab1','red':'#c7646f','green':'#346c68',
 'cream':'#e9debd','brown':'#72553b','paper':'#f4edcf','yellow':'#f5e49b',
 'teal':'#3e9a9c','lens':'#b5ced8','rose':'#eab1c1','book':'#547c88'}
COLORS=list(PALETTE)
def uv(mat):
 i=COLORS.index(mat);x=64+(i%8)*8;y=(i//8)*8
 return [[x+2,y+2],[x+6,y+2],[x+6,y+6],[x+2,y+6]]
def mesh(vertices,faces,mat,name):
 return {'name':name,'material':'body','shading':'flat','vertices':vertices,'faces':[{'v':f,'uv':[uv(mat)[i%4] for i in range(len(f))]} for f in faces]}
def box(bounds,mat,name):
 x,y,z,X,Y,Z=bounds
 return mesh([[x,y,z],[X,y,z],[X,Y,z],[x,Y,z],[x,y,Z],[X,y,Z],[X,Y,Z],[x,Y,Z]],
 [[1,0,3,2],[4,5,6,7],[0,4,7,3],[5,1,2,6],[3,7,6,2],[0,1,5,4]],mat,name)
def disc(cx,cy,z,rx,ry,depth,mat,name,segments=12):
 verts=[]
 for zz in (z,z+depth):
  for i in range(segments):
   a=2*math.pi*i/segments;verts.append([cx+rx*math.cos(a),cy+ry*math.sin(a),zz])
 faces=[list(reversed(range(segments))),list(range(segments,2*segments))]
 faces.extend([[i,(i+1)%segments,(i+1)%segments+segments,i+segments] for i in range(segments)])
 return mesh(verts,faces,mat,name)
def ring(cx,cy,z,r,width,depth,mat,name,segments=12):
 verts=[]
 for zz in (z,z+depth):
  for rr in (r,r-width):
   for i in range(segments):
    a=2*math.pi*i/segments;verts.append([cx+rr*math.cos(a),cy+rr*math.sin(a),zz])
 n=segments;faces=[]
 for i in range(n):
  j=(i+1)%n;faces += [[j,i,n+i,n+j],[2*n+i,2*n+j,3*n+j,3*n+i],[i,j,2*n+j,2*n+i],[n+j,n+i,3*n+i,3*n+j]]
 return mesh(verts,faces,mat,name)
def diamond(cx,cy,z,rx,ry,depth,mat,name):
 verts=[[cx,cy-ry,z+depth/2],[cx+rx,cy,z+depth/2],[cx,cy+ry,z+depth/2],[cx-rx,cy,z+depth/2],[cx,cy,z],[cx,cy,z+depth]]
 return mesh(verts,[[0,4,1],[1,4,2],[2,4,3],[3,4,0],[1,5,0],[2,5,1],[3,5,2],[0,5,3]],mat,name)
def rod(a,b,width,mat,name):
 # A low-sided prism around a line, with stable axis choice for all directions.
 d=[b[i]-a[i] for i in range(3)];length=math.sqrt(sum(x*x for x in d));assert length>0
 axis=[x/length for x in d];aux=[0,0,1] if abs(axis[2])<.9 else [0,1,0]
 u=[axis[1]*aux[2]-axis[2]*aux[1],axis[2]*aux[0]-axis[0]*aux[2],axis[0]*aux[1]-axis[1]*aux[0]]
 norm=math.sqrt(sum(x*x for x in u));u=[x/norm*width/2 for x in u]
 v=[axis[1]*u[2]-axis[2]*u[1],axis[2]*u[0]-axis[0]*u[2],axis[0]*u[1]-axis[1]*u[0]]
 verts=[]
 for at in (a,b):
  for su,sv in ((-1,-1),(1,-1),(1,1),(-1,1)):verts.append([at[i]+su*u[i]+sv*v[i] for i in range(3)])
 return mesh(verts,[[3,2,1,0],[4,5,6,7],[0,1,5,4],[1,2,6,5],[2,3,7,6],[3,0,4,7]],mat,name)
def bone(name,parent,parts,pivot=(0,0,0),rotation=(0,0,0)):
 return {'name':name,'parent':parent,'pivot':list(pivot),'rotation':list(rotation),'meshes':parts}
def eye(side,role):
 # One replacement eye pair. Original raised eye meshes are removed, not layered.
 x=-2 if side=='left' else 2
 parts=[disc(0,0,-.10,.86,1.12,.15,'white','eye_white'),box((-.22,-.68,-.18,.22,.65,-.10),'ink','pupil')]
 if role=='doctor':parts.append(box((-.90,-1.16,-.25,.90,-.18,-.17),'skin','relaxed_upper_lid'))
 elif role=='fortune':
  # The fortune teller keeps the soft violet eyes closed in the reference.
  parts=[disc(0,0,-.12,.86,1.12,.15,'violet','violet_closed_lid'),rod((-.64,.65,-.27),(.64,.65,-.27),.10,'purple_dark','closed_lid_edge')]
 if role=='principal':
  parts=[disc(0,0,-.1,.82,.95,.12,'lens','spectacle_lens'),box((-.16,-.39,-.18,.16,.42,-.11),'hair_dark','quiet_pupil'),ring(0,0,-.28,1.03,.16,.15,'silver_light','round_spectacle_rim')]
 return bone('staff_eye_'+side,'skull',parts,(x,-2.05,-3.2))
def role_model(role,source):
 data=copy.deepcopy(source);data['texture_width']=128;data['texture_height']=64
 # Neutral adult shares the original neutral skeleton, not the baby crawl state.
 for b in data['bones']:
  if b['name'] in ('skull__cube_2','skull__cube_3'):b['meshes']=[]
 data['bones'] += [eye('left',role),eye('right',role)]
 def add(name,parent,parts,pivot=(0,0,0),rotation=(0,0,0)):data['bones'].append(bone('staff_'+name,parent,parts,pivot,rotation))
 if role=='doctor':
  parts=[]
  for sign in (-1,1):
   for i,(y,z,w) in enumerate([(-5.05,.9,1.2),(-4.5,.35,1.45),(-4.0,-.35,1.25)]):
    lo,hi=sorted((sign*3.1,sign*(3.1+w)));parts.append(box((lo,y,z-.7,hi,y+.5,z+1.5),'hair' if i%2==0 else 'hair_dark','temple_hair_'+str(sign)+'_'+str(i)))
  add('doctor_hair','skull',parts)
  tube=[(-3.35,-1.15,-3.32),(-2.6,-.05,-3.42),(0,.45,-3.44),(2.6,-.05,-3.42),(3.35,-1.15,-3.32)]
  parts=[rod(a,b,.22,'silver','stethoscope_tube') for a,b in zip(tube,tube[1:])]
  parts += [disc(-3.35,-.9,-3.58,.53,.53,.22,'silver_light','medical_receiver'),disc(-3.35,-.9,-3.64,.38,.38,.06,'blue','blue_receiver'),box((-3.44,-1.16,-3.72,-3.26,-.64,-3.64),'white','medical_cross_vertical'),box((-3.61,-.99,-3.72,-3.09,-.81,-3.64),'white','medical_cross_horizontal')]
  add('doctor_stethoscope_headset','skull',parts)
  add('doctor_chestpiece','body',[rod((0,-3.05,-2.16),(0,-1.22,-2.2),.18,'ink','stethoscope_drop'),disc(0,-1,-2.48,.4,.4,.24,'silver','stethoscope_chestpiece')])
  # Two swept gray lobes read as facial hair at Minecraft scale. There are
  # no white tooth slabs, gaps or grinning mouth lines beneath the moustache.
  parts=[];outline=[(0,-.35),(.35,-.67),(.95,-.70),(2.05,-.56),(1.78,.08),(1,.12),(.3,-.04)]
  for sign in (-1,1):
   shape=[(sign*x,y) for x,y in outline];n=len(shape)
   vertices=[[x,y,z] for z in (-3.58,-3.25) for x,y in shape]
   area=sum(shape[i][0]*shape[(i+1)%n][1]-shape[(i+1)%n][0]*shape[i][1] for i in range(n))
   front=list(range(n));front=front if area<0 else list(reversed(front))
   sides=[[i,(i+1)%n,(i+1)%n+n,i+n] for i in range(n)]
   # Mirroring x reverses handedness. Caps already use the polygon area;
   # reverse the reflected perimeter too so its flat normals point outward.
   if area<0:sides=[list(reversed(face)) for face in sides]
   faces=[front,[i+n for i in reversed(front)]]+sides
   parts.append(mesh(vertices,faces,'hair_dark','gray_moustache_lobe_'+str(sign)))
   parts.append(rod((sign*.40,-.38,-3.61),(sign*1.30,-.18,-3.61),.07,'hair','moustache_hair_highlight_'+str(sign)))
  add('doctor_moustache','skull',parts)
 elif role=='principal':
  parts=[box((-1.05,-2.17,-3.46,1.05,-1.99,-3.31),'ink','spectacle_bridge')]
  for sign in (-1,1):
   lo,hi=sorted((sign*2.95,sign*3.66));parts.append(box((lo,-2.18,-3.29,hi,-2.0,2.25),'ink','spectacle_temple'))
   lo,hi=sorted((sign*.94,sign*3.05));parts.append(box((lo,-3.5,-3.4,hi,-3.22,-3.15),'hair','bushy_eyebrow'))
  add('principal_spectacles','skull',parts)
  add('moustache_left','skull',[box((-2.45,-.52,-3.62,0,.10,-3.20),'hair','moustache_left'),box((-2.9,-.73,-3.55,-1.75,-.30,-3.19),'white','moustache_tip')],rotation=(0,0,10))
  add('moustache_right','skull',[box((0,-.52,-3.62,2.45,.10,-3.20),'hair','moustache_right'),box((1.75,-.73,-3.55,2.9,-.30,-3.19),'white','moustache_tip')],rotation=(0,0,-10))
  add('principal_beard','skull',[box((-.64,-.1,-3.43,.64,.85,-3.13),'hair','beard_upper'),box((-.38,.85,-3.37,.38,1.5,-3.14),'white','beard_tip')])
 elif role=='fortune':
  add('fortune_circlet','skull',[rod((-3.5,-4.65,-3.22),(0,-3.78,-3.29),.39,'gold','circlet_left'),rod((0,-3.78,-3.29),(3.5,-4.65,-3.22),.39,'gold','circlet_right'),box((-3.66,-4.85,-3.18,-3.45,-4.35,2.85),'gold','circlet_side_left'),box((3.45,-4.85,-3.18,3.66,-4.35,2.85),'gold','circlet_side_right'),disc(0,-3.8,-3.5,.66,.82,.2,'gold_shadow','gem_setting'),diamond(0,-3.8,-3.83,.45,.65,.35,'purple_dark','forehead_amethyst')])
  add('fortune_stole','body',[box((-2.16,-3.4,-2.18,-1.25,.6,-2.02),'purple','stole_left'),box((1.25,-3.4,-2.18,2.16,.6,-2.02),'purple','stole_right'),box((-1.25,-3.4,-2.21,-1.08,.65,-2.05),'gold','stole_trim_left'),box((1.08,-3.4,-2.21,1.25,.65,-2.05),'gold','stole_trim_right'),disc(0,-2.5,-2.37,.45,.45,.17,'gold','gold_brooch'),diamond(0,-2.5,-2.55,.25,.32,.17,'violet','brooch_gem')])
  curls=[]
  for sign in (-1,1):
   for i,(w,y,z) in enumerate(((.95,-.65,-1.6),(.65,-.2,-.8))):
    lo,hi=sorted((sign*3.35,sign*(3.35+w)));curls.append(box((lo,y,z,hi,y+.3,z+1.5),'white' if i==0 else 'hair','fortune_side_curl_'+str(sign)+'_'+str(i)))
  add('fortune_side_curls','skull',curls)
  add('fortune_smile','skull',[rod((-.65,-.27,-3.25),(0,-.06,-3.27),.08,'ink','mouth_left'),rod((0,-.06,-3.27),(.65,-.27,-3.25),.08,'ink','mouth_right')])
 elif role=='teacher':
  # An invented teacher: academic cap, soft waistcoat, tie and open lesson book.
  for b in data['bones']:
   if b['name']=='skull__cube_1':b['meshes']=[]
  add('teacher_cap','skull',[box((-2.6,-6.7,-2.4,2.6,-5.78,2.4),'green','cap_band'),box((-4.4,-7.1,-4.2,4.4,-6.63,4.2),'green','academic_board'),box((-.25,-7.27,-.25,.25,-7.08,.25),'gold','cap_button'),rod((0,-7.25,0),(3.3,-7.25,.3),.12,'gold','tassel_top'),rod((3.3,-7.2,.3),(3.65,-4.6,.3),.12,'gold','tassel_cord'),box((3.4,-4.65,.07,3.9,-3.95,.52),'gold','tassel_end')])
  add('teacher_waistcoat','body',[box((-2.12,-3.42,-2.15,-.2,.52,-2.02),'cream','waistcoat_left'),box((.2,-3.42,-2.15,2.12,.52,-2.02),'cream','waistcoat_right'),box((-.35,-3.35,-2.3,.35,-2.65,-2.15),'red','tie_knot'),box((-.22,-2.65,-2.3,.22,-1.18,-2.15),'red','tie_tail')])
  add('lesson_book','body',[box((-2.0,-1.5,-3.7,2.0,-.9,-2.3),'book','book_cover'),box((-1.82,-1.64,-3.53,-.09,-1.48,-2.38),'paper','left_page'),box((.09,-1.64,-3.53,1.82,-1.48,-2.38),'paper','right_page'),box((-.06,-1.69,-3.6,.06,-1.48,-2.38),'brown','book_spine')])
  add('teacher_smile','skull',[rod((-.72,-.28,-3.25),(0,-.02,-3.27),.08,'ink','smile_left'),rod((0,-.02,-3.27),(.72,-.28,-3.25),.08,'ink','smile_right')])
 data['staff_025']={'role':role,'adult':True,'rendering':'fixed native neutral adult stance, original eye pair replaced once','video_seconds':{'doctor':134,'principal':167,'fortune':69}.get(role),'teacher_design':'Original fitting teacher; no teacher exists in supplied game classroom' if role=='teacher' else None,'source_mesh_sha256':hashlib.sha256((A/'models/entity/official/chao_baby.json').read_bytes()).hexdigest()}
 if role=='doctor':data['staff_025']['face']='Gray swept moustache; no toothy grin or beard (0.25.1)'
 return data

def main():
 base=Image.open(A/'textures/entity/chao/official/baby.png').convert('RGBA')
 assert base.size==(128,64)
 atlas=Image.new('RGBA',(256,128));atlas.paste(base,(0,0));d=ImageDraw.Draw(atlas)
 for i,(name,col) in enumerate(PALETTE.items()):
  x=128+(i%8)*16;y=(i//8)*16;d.rectangle((x,y,x+15,y+15),fill=col)
 out=A/'textures/entity/chao/staff025.png';out.parent.mkdir(parents=True,exist_ok=True);atlas.save(out,optimize=True)
 source=json.loads((A/'models/entity/official/chao_baby.json').read_text())
 for role in ('teacher','doctor','principal','fortune'):
  model=role_model(role,source);out=A/'models/entity/staff025'/f'{role}.json';out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(model,separators=(',',':'))+'\n')
 print('Staff025: four upright adult profession meshes, one native-derived atlas; source inputs untouched.')
if __name__=='__main__':main()
