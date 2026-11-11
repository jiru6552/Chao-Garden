"""Original Bedrock-DLC-inspired Chao reconstruction and editable Blockbench projects.

Run: python3 art/build_adventure_assets.py (Pillow required).
The editable .bbmodel uses real Generic/free cube elements plus alpha planes and small icon meshes.
"""
from __future__ import annotations
import base64, copy, io, json, math, uuid
from pathlib import Path
from PIL import Image, ImageDraw
from chao_palettes import NORMAL_PALETTES, CHAOS_PALETTES, palette_for
from variant_catalog import VARIANTS
from variant_icons import theme_icon
from chao_variant_assets import append_variant_bones
from chao_head_morphs import HEAD_PROFILES, manifest as head_manifest
from chao_goggles import GOGGLE_PALETTES

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources/assets/chao_garden'
MODEL=RES/'models/entity'; TEX=RES/'textures/entity'; BB=ROOT/'art/blockbench'
for p in (MODEL,TEX/'chao',TEX/'eggs',BB):p.mkdir(parents=True,exist_ok=True)
COLORS={'white':(239,237,223),'red':(219,73,69),'yellow':(245,212,86),'blue':(67,114,216),'aqua':(89,205,215),'pink':(236,156,195),'orange':(237,143,65),'purple':(157,109,198),'brown':(155,111,79),'green':(78,165,99),'grey':(147,162,170),'lime':(168,209,83),'black':(56,62,75),'sky_blue':(140,193,234),'cyan':(52,190,178)}
JEWELS={'silver':(185,210,225),'gold':(233,179,58),'ruby':(195,36,83),'sapphire':(49,96,210),'emerald':(28,174,126),'amethyst':(144,80,206),'aquamarine':(64,205,220),'garnet':(153,46,66),'onyx':(45,44,73),'diamond':(180,228,240),'peridot':(166,197,61),'topaz':(230,162,52)}
NORMAL={alignment:[p['body'] for p in palettes] for alignment,palettes in NORMAL_PALETTES.items()}
ANIMALS=['penguin','seal','otter','peacock','parrot','condor','rabbit','cheetah','boar','bear','tiger','gorilla','skunk','sheep','raccoon','dragon','unicorn','phoenix','half_fish','skeleton_dog','bat']
# Animal genes add independent, animated cuboid body parts to the shared rig.
INCLUDE_ANIMAL_VARIATIONS=True
ANIMAL_COLORS=[((30,39,52),(235,239,226)),((148,172,184),(217,226,226)),((125,86,56),(229,202,149)),((33,137,137),(91,189,93)),((220,63,70),(66,142,214)),((69,65,79),(225,222,208)),((235,230,212),(237,166,178)),((222,185,105),(55,43,30)),((120,82,65),(172,131,94)),((127,84,56),(68,47,34)),((235,143,55),(37,36,36)),((64,68,76),(164,163,157)),((38,37,47),(238,235,219)),((237,234,220),(148,107,76)),((147,144,136),(43,44,52)),((139,82,164),(229,110,105)),((241,237,224),(240,193,97)),((242,132,47),(250,215,83)),((111,183,192),(208,233,223)),((223,219,193),(127,135,134)),((93,72,116),(165,117,145))]
MATERIALS={'body':[0,0,32,32],'accent':[32,0,32,32],'wing':[64,0,32,32],'eye':[96,0,16,16],'pupil':[96,0,16,16],'white':[112,0,16,16],'mouth':[96,16,16,16],'orb':[112,16,16,16],'dark_orb':[0,32,16,16],'heart':[16,32,16,16],'flame':[32,32,32,32],'cocoon':[64,32,32,32],'shine':[96,32,16,16],'tuft':[0,64,32,32],'head':[32,64,32,32]}
MATERIALS.update({'happy_eye':[96,64,8,8],'lid':[104,64,8,8],'mouth_open':[112,64,8,8]})
MATERIALS.update({'brow':[96,80,8,8],'grin':[104,80,8,8],'belly':[112,80,8,8]})
MATERIALS.update({'crest_tip':[0,96,32,32],'hand':[32,96,32,32],'toe':[64,96,32,32],'heel':[96,96,32,32],'collar':[0,128,32,32]})
MATERIALS['variant_detail']=[32,128,32,32]
MATERIALS.update({'goggle_frame':[64,128,32,32], 'goggle_lens':[96,128,32,32],
                  'goggle_strap':[0,160,32,32], 'goggle_highlight':[32,160,32,32]})
VARIANT_BY_ID={entry['id']:entry for entry in VARIANTS}
HEAD_FRONT=-3.25
for i in range(21):MATERIALS[f'animal_{i}']=[128+(i%4)*32,(i//4)*32,32,32]
def shade(c,f):return tuple(max(0,min(255,round(v*f))) for v in c)
def mix(a,b,t):return tuple(round(a[i]*(1-t)+b[i]*t) for i in range(3))
def animal_tile(i):
    b,a=ANIMAL_COLORS[i];im=Image.new('RGBA',(32,32),b+(255,));p=im.load()
    for y in range(32):
      for x in range(32):
        c=b
        if i==0 and 8<=x<=24:c=a
        elif i in(1,2,8,9) and y>21:c=mix(b,a,.45)
        elif i==3:
          r=(x%12-6)**2+(y%12-6)**2
          if r<5:c=(40,74,131)
          elif r<13:c=(232,195,71)
          elif r<24:c=a
        elif i==4:c=a if y<8 else (242,207,78) if y<12 else b
        elif i==5 and y<7:c=a
        elif i==6 and 11<x<21:c=a
        elif i==7 and ((x+(y//8%2)*4)%9-4)**2+(y%9-4)**2<4:c=a
        elif i==10 and (x+y//2)%11<3:c=a
        elif i==11 and y>12:c=mix(b,a,.5)
        elif i==12 and 12<x<20:c=a
        elif i==13:c=a if y>25 else shade(b,1+((x//4+y//4)%3-1)*.025)
        elif i==14 and (y//5)%2==0:c=a
        elif i==15 and (x+2*y)%13<3:c=a
        elif i==16 and x<7:c=a
        elif i==17 and y//6%2==0:c=a
        elif i>17 and (x//5+y//6)%3==0:c=a
        p[x,y]=shade(c,1+((x//3+y//4)%3-1)*.009)+(255,)
    for y in range(24,32):
      for x in range(24,32):p[x,y]=(0,0,0,0)
    for yy,row in enumerate(['...####','..#####','.######','#######','.#####.','..###..']):
      for xx,c in enumerate(row):
        if c=='#':p[24+xx,24+yy]=(a if (xx+yy)%4==0 else b)+(255,)
    # Quiet solid swatches keep inset ears, beaks, hoofs and feather tips
    # readable at game distance instead of burying them in the body pattern.
    for y in range(24,32):
      for x in range(24):
        inset=a if i==6 else mix(a,(250,206,177),.65)
        p[x,y]=(b if x<8 else a if x<16 else inset)+(255,)
    return im

ANIMAL_TILES=[animal_tile(i) for i in range(21)]

def material_texture(name,base,two,shiny,jewel=False,alignment='neutral'):
    im=Image.new('RGBA',(256,256),(0,0,0,255));p=im.load()
    typ=int(name.split('_')[2]) if name.startswith('normal_') else 0
    accent=(247,232,139) if two else base
    wing=mix(base,(208,161,223),.82) if two else base
    orb=(251,238,113) if alignment=='neutral' else (91,172,245) if alignment=='hero' else accent
    colors={'body':base,'head':base,'tuft':base,'accent':accent,'wing':wing,'eye':(28,40,50),'pupil':(28,40,50),'white':(244,250,250),'mouth':(30,50,61),'orb':orb,'dark_orb':orb,'heart':(246,105,146),'flame':(81,219,164),'cocoon':(230,179,212),'shine':(246,255,255),'happy_eye':(28,40,50),'lid':(28,40,50),'mouth_open':(31,35,48),'brow':shade(base,.60),'grin':(245,248,238),'belly':accent}
    colors.update({'crest_tip':accent,'hand':accent,'toe':accent,'heel':base,'collar':base})
    colors.update({'goggle_frame':base,'goggle_lens':accent,
                   'goggle_strap':base,'goggle_highlight':accent})
    normal=name.startswith('normal_')
    chaos=name[6:] if name.startswith('chaos_') else None
    theme=VARIANT_BY_ID.get(name[8:]) if name.startswith('variant_') else None
    colors['variant_detail']=accent
    if normal and two:
      colors.update(palette_for(alignment,typ));colors['dark_orb']=colors['orb']
      colors['accent']=colors['crest_tip'];colors['brow']=shade(colors['head'],.60)
    if chaos:
      colors.update(CHAOS_PALETTES[chaos])
      colors['accent']=colors['crest_tip'];colors['dark_orb']=colors['orb']
      colors['eye']={'light':(65,229,106),'angel':(77,179,237),'devil':(252,219,64)}[chaos]
      colors['flame']=(180,102,240) if chaos=='devil' else colors['orb']
    if theme:
      colors.update({key:tuple(value) for key,value in theme['palette'].items()})
      colors['dark_orb']=colors['orb'];colors['brow']=shade(colors['head'],.6)
      colors.update(GOGGLE_PALETTES.get(theme['id'], {}))
    wing=colors['wing']
    for mat,(ox,oy,w,h) in MATERIALS.items():
      if mat.startswith('animal_'):
        im.paste(ANIMAL_TILES[int(mat[7:])],(ox,oy));continue
      color=colors[mat]
      for y in range(h):
       for x in range(w):
        sample=color
        f=1.0
        if mat in('body','head','tuft','accent','wing','crest_tip','hand','toe','heel','collar'):
          # Broad, quiet painted bands from the DLC reference, without mottling.
          local_y=y%16;local_x=x%16
          f+=.025 if local_y<2 else -.035 if local_y>=6 else 0
          if mat in('head','body') and 3<=local_x<7 and 2<=local_y<5:f+=.025
          if normal and two and mat=='body' and local_y<1:sample=colors['collar']
          if normal and two and mat=='head' and typ==1 and local_y>=5 and not(x>=16 and y>=16):
            sample=colors['collar']
          if normal and two and mat=='head' and alignment=='dark' and typ==3 and x<16 and y<16:
            if local_y<1 and 2<=local_x<6 or local_y<2 and 3<=local_x<5:sample=colors['crest_tip']
          if chaos=='angel' and mat in('body','heel','toe') and (local_x*7+local_y*11)%23==0:
            sample=mix(color,(204,143,190),.4)
          if chaos=='devil' and mat in('hand','crest_tip'):
            sample=mix((180,48,54),(255,196,69),max(0,min(1,(7-local_y)/7)))
          if shiny:f+=.10
          if jewel:f+=.085*math.sin(x*.28+y*.19)+.05*((x//8+y//8)%2)
        p[ox+x,oy+y]=shade(sample,f)+(255,)
      if mat in('body','head','tuft','accent','wing','crest_tip','hand','toe','heel','collar') and (shiny or jewel):
        d=ImageDraw.Draw(im)
        d.line([(ox+7,oy+3),(ox+7,oy+10)],fill=(245,255,255),width=1)
        d.line([(ox+4,oy+6),(ox+10,oy+6)],fill=(245,255,255),width=1)
    # Low-resolution pixel expressions live on flat planes. The empty texels
    # are actually transparent, so no coloured rectangle covers the head.
    # Two texels per unit on faces: narrow white columns around a tall pupil.
    patterns={'eye':['W##W']*6,'lid':['.....','.....','#####','#####','.....','.....'],'happy_eye':['.....','.###.','##.##','#...#','.....','.....'],'mouth':['####','.##.'],'mouth_open':['###','#P#'],'brow':['####','####'],'grin':['WWWWWWWW','WWWWWWWW'],'belly':['#..#','#..#','####','....'] if normal and alignment=='dark' else ['########']*8}
    if chaos:patterns['eye']=['.##.','####','####','####','####','.##.']
    if chaos=='devil':patterns['belly']=['#..#','#..#','####','....']
    for mat,rows in patterns.items():
      ox,oy,w,h=MATERIALS[mat]
      for yy in range(h):
       for xx in range(w):p[ox+xx,oy+yy]=(0,0,0,0)
      for yy,row in enumerate(rows):
       for xx,c in enumerate(row):
        if c!='.':p[ox+xx,oy+yy]=(242,250,250,255) if c=='W' else (228,137,160,255) if c=='P' else colors[mat]+(255,)
    wx,wy,_,_=MATERIALS['wing']
    for yy in range(16):
      for xx in range(16):p[wx+xx,wy+yy]=(0,0,0,0)
    for xx,yy in [(x,y) for y,row in enumerate(['..##','.###','####','.##.']) for x,c in enumerate(row) if c=='#']:p[wx+xx,wy+yy]=wing+(255,)
    for yy,row in enumerate(['...####','..#####','.######','#######','.#####.','..###..']):
      for xx,c in enumerate(row):
        if c=='#':p[wx+8+xx,wy+yy]=wing+(255,)
    dx,dy,dw,dh=MATERIALS['variant_detail']
    ImageDraw.Draw(im).rectangle((dx,dy,dx+dw-1,dy+dh-1),fill=(0,0,0,0))
    if theme:im.paste(theme_icon(theme,16),(dx,dy))
    im.save(TEX/'chao'/f'{name}.png',optimize=True)
    if chaos:return im
    # Larger, readable Adventure-style color patches instead of visual noise.
    shell=mix(base,(247,247,230),.88 if two else .18)
    if jewel:shell=shade(base,1.05)
    egg=Image.new('RGBA',(64,64),shell+(255,));d=ImageDraw.Draw(egg)
    spots=[[(4,6),(11,3),(16,8),(14,15),(7,17),(2,12)],[(33,1),(39,4),(42,10),(36,14),(29,10)],[(48,20),(57,17),(62,23),(59,31),(50,30),(46,26)],[(19,25),(27,23),(33,29),(30,37),(22,38),(17,32)],[(2,46),(9,41),(15,46),(15,54),(6,57),(0,52)],[(38,47),(45,43),(51,49),(49,57),(41,61),(35,54)]]
    for poly in spots:d.polygon(poly,fill=shade(base,.95)+(255,))
    # Placed eggs use a moving reflection render pass. A sparkle painted near
    # atlas y=10 wraps onto the shell's lower pole and looks like a white foot.
    if theme and theme['id'] in ('nether','end'):
      from dimension_egg_art import surface
      egg=surface(theme['id'],64)
    egg.save(TEX/'eggs'/f'{name}.png',optimize=True)
    return im

# Mesh helpers. Runtime coordinates match vanilla ModelPart's downward Y axis.
BONES=[]
def bone(name,parent,pivot,meshes=None):
    b={'name':name,'parent':parent,'pivot':pivot,'meshes':meshes or []};BONES.append(b);return b

def mesh(name,material,vertices,faces,uv=None):
    rect=MATERIALS.get(material,[0,0,32,32]);x,y,w,h=rect
    records=[]
    for f in faces:
      faceuv=uv[len(records)] if uv else [[x+.5,y+.5],[x+w-.5,y+.5],[x+w-.5,y+h-.5],[x+.5,y+h-.5]][:len(f)]
      records.append({'v':list(f),'uv':faceuv})
    return {'name':name,'material':material,'vertices':[[round(v,5) for v in p] for p in vertices],'faces':records,'shading':'flat'}
def cross(a,b):return [a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]]
def sub(a,b):return [a[i]-b[i] for i in range(3)]
def dot(a,b):return sum(x*y for x,y in zip(a,b))
def orient(vertices,faces,center):
    out=[]
    for f in faces:
      n=cross(sub(vertices[f[1]],vertices[f[0]]),sub(vertices[f[2]],vertices[f[0]]))
      c=[sum(vertices[j][i] for j in f)/len(f)-center[i] for i in range(3)]
      out.append(list(reversed(f)) if dot(n,c)<0 else f)
    return out

def ellipsoid(name,mat,center,radii,segments=12,rings=8):
    v=[]
    # Use tiny top/bottom rings so all quads remain nondegenerate and editable.
    for j in range(rings+1):
      lat=math.pi*(.003+.994*j/rings)
      for i in range(segments):
        ang=2*math.pi*i/segments
        v.append([center[0]+radii[0]*math.sin(lat)*math.cos(ang),center[1]-radii[1]*math.cos(lat),center[2]+radii[2]*math.sin(lat)*math.sin(ang)])
    faces=[]
    for j in range(rings):
      for i in range(segments):faces.append([j*segments+i,j*segments+(i+1)%segments,(j+1)*segments+(i+1)%segments,(j+1)*segments+i])
    faces.append(list(reversed(list(range(segments)))))
    faces.append(list(range(rings*segments,(rings+1)*segments)))
    # Fans for cap polygons; native bridge only uses triangles and quads.
    faces=orient(v,faces,center)
    out=[]
    for f in faces:
      if len(f)<=4:out.append(f)
      else:
       for i in range(1,len(f)-1):out.append([f[0],f[i],f[i+1]])
    result=mesh(name,mat,v,out)
    result['shading']='smooth'
    return cylindrical_uv(result,segments,rings)

def tube(name,mat,sections,segments=10):
    # sections: center x,y,z, xradius,zradius; path can sweep for organic tips.
    v=[]
    for x,y,z,rx,rz in sections:
      for i in range(segments):
        a=2*math.pi*i/segments;v.append([x+rx*math.cos(a),y,z+rz*math.sin(a)])
    faces=[]
    for j in range(len(sections)-1):
      for i in range(segments):faces.append([j*segments+i,j*segments+(i+1)%segments,(j+1)*segments+(i+1)%segments,(j+1)*segments+i])
    for r,rev in ((0,True),(len(sections)-1,False)):
      f=list(range(r*segments,(r+1)*segments));f=f[::-1] if rev else f
      for i in range(1,len(f)-1):faces.append([f[0],f[i],f[i+1]])
    center=[sum(s[i] for s in sections)/len(sections) for i in range(3)]
    result=mesh(name,mat,v,orient(v,faces,center))
    result['shading']='smooth'
    return cylindrical_uv(result,segments,len(sections)-1)

def cylindrical_uv(result,segments,rows):
    x,y,w,h=MATERIALS[result['material']]
    for face in result['faces']:
      columns=[i%segments for i in face['v']]
      seam=0 in columns and segments-1 in columns
      face['uv']=[[x+.5+(w-1)*(1 if seam and i%segments==0 else (i%segments)/segments),y+.5+(h-1)*(i//segments)/rows] for i in face['v']]
    return result

def polygon_triangles(outline):
    # Ear clipping respects concave hearts, stars and wing outlines.
    ids=list(range(len(outline)));triangles=[]
    area=sum(outline[i][0]*outline[(i+1)%len(outline)][1]-outline[(i+1)%len(outline)][0]*outline[i][1] for i in ids)
    if area<0:ids.reverse()
    def cross2(a,b,c):return (b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0])
    def inside(p,a,b,c):return cross2(a,b,p)>=-1e-8 and cross2(b,c,p)>=-1e-8 and cross2(c,a,p)>=-1e-8
    while len(ids)>3:
      found=False
      for k,mid in enumerate(ids):
        prev,nxt=ids[k-1],ids[(k+1)%len(ids)]
        a,b,c=outline[prev],outline[mid],outline[nxt]
        if cross2(a,b,c)<=1e-8:continue
        if any(inside(outline[j],a,b,c) for j in ids if j not in(prev,mid,nxt)):continue
        triangles.append([prev,mid,nxt]);ids.pop(k);found=True;break
      if not found:raise ValueError('Cannot triangulate self-intersecting outline')
    triangles.append(ids);return triangles

def extrude(name,mat,outline,depth=.4,center=(0,0,0)):
    # Front face points towards negative Z, sides are one watertight component.
    if sum(outline[i][0]*outline[(i+1)%len(outline)][1]-outline[(i+1)%len(outline)][0]*outline[i][1] for i in range(len(outline)))<0:outline=outline[::-1]
    v=[[x+center[0],y+center[1],center[2]-depth/2] for x,y in outline]+[[x+center[0],y+center[1],center[2]+depth/2] for x,y in outline]
    n=len(outline);f=[]
    for tri in polygon_triangles(outline):f.extend([tri[::-1],[n+i for i in tri]])
    for i in range(n):j=(i+1)%n;f.append([i,j,n+j,n+i])
    result=mesh(name,mat,v,f)
    ox,oy,w,h=MATERIALS[mat];minx=min(p[0] for p in v);maxx=max(p[0] for p in v);miny=min(p[1] for p in v);maxy=max(p[1] for p in v)
    for face in result['faces']:face['uv']=[[ox+(v[i][0]-minx),oy+(v[i][1]-miny)] for i in face['v']]
    return result

def torus(name,mat,radius,tube_r,segments=16,sides=5):
    v=[]
    for i in range(segments):
      a=i*math.tau/segments
      for j in range(sides):
        b=j*math.tau/sides;v.append([(radius+tube_r*math.cos(b))*math.cos(a),tube_r*math.sin(b),(radius+tube_r*math.cos(b))*math.sin(a)])
    f=[]
    for i in range(segments):
      for j in range(sides):f.append([i*sides+j,((i+1)%segments)*sides+j,((i+1)%segments)*sides+(j+1)%sides,i*sides+(j+1)%sides])
    result=mesh(name,mat,v,f);result['shading']='smooth';return result

def head_z(x,y,offset=.08):return -4.4*math.sqrt(max(.025,1-(x/5.8)**2-((y+.6)/4.8)**2))-offset

def facial_disc(name,mat,cx,cy,rx,ry,offset=.08,pivot=(0,0,0),segments=16):
    v=[[cx-pivot[0],cy-pivot[1],head_z(cx,cy,offset)-pivot[2]]]
    for i in range(segments):
      a=i*math.tau/segments;x=cx+rx*math.cos(a);y=cy+ry*math.sin(a)
      v.append([x-pivot[0],y-pivot[1],head_z(x,y,offset)-pivot[2]])
    f=[[0,(i+1)%segments+1,i+1] for i in range(segments)]
    return mesh(name,mat,v,f)

def facial_line(name,mat,points,width=.16,offset=.10,pivot=(0,0,0)):
    v=[];f=[]
    for x,y in points:
      for sy in(-width/2,width/2):v.append([x-pivot[0],y+sy-pivot[1],head_z(x,y+sy,offset)-pivot[2]])
    for i in range(len(points)-1):f.append([2*i,2*i+1,2*i+3,2*i+2])
    return mesh(name,mat,v,f)

def wing_mesh(name,mat,side=1,big=False):
    # Curved butterfly/leaf wing rather than rectangular plates.
    s=1.75 if big else 1
    outline=[(0,0),(.6,-1.5),(1.8,-2.5),(3.0,-2.6),(3.6,-1.7),(3.3,-.3),(2.7,.6),(1.1,1.2),(.2,.65)]
    result=extrude(name,mat,[(side*x*s,y*s) for x,y in outline],.48 if big else .32,center=(0,0,.35))
    # Sweep outer half backward in Z for a feather-like curved profile.
    for vert in result['vertices']:vert[2]+=abs(vert[0])*.20
    return result

# Cuboid anatomy follows the official DLC reference images. Curved helpers
# remain reserved for the evolution cocoon and egg shell.
sphere_mesh=ellipsoid
rounded_tube=tube

def snap(v,step=.5):return round(v/step)*step

def cube_uv(material,size):
    x,y,w,h=MATERIALS[material];sx,sy,sz=size
    qx=16 if w>=32 else 0;qy=16 if h>=32 else 0
    # Four 16px atlas quadrants. Every side spans its actual model dimensions.
    # Opposing sides share a painted material patch; engine lighting supplies
    # the face shading. This is deliberately not a stretched 32px swatch.
    return {'north':[x,y,x+sx,y+sy],'south':[x+qx,y,x+qx+sx,y+sy],
            'west':[x,y+qy,x+sz,y+qy+sy],'east':[x,y+qy,x+sz,y+qy+sy],
            'up':[x+qx,y+qy,x+qx+sx,y+qy+sz],'down':[x+qx,y+qy,x+qx+sx,y+qy+sz]}

def rotate_point(v,rotation,pivot):
    x,y,z=[v[i]-pivot[i] for i in range(3)]
    rx,ry,rz=[math.radians(a) for a in rotation]
    y,z=y*math.cos(rx)-z*math.sin(rx),y*math.sin(rx)+z*math.cos(rx)
    x,z=x*math.cos(ry)+z*math.sin(ry),-x*math.sin(ry)+z*math.cos(ry)
    x,y=x*math.cos(rz)-y*math.sin(rz),x*math.sin(rz)+y*math.cos(rz)
    return [x+pivot[0],y+pivot[1],z+pivot[2]]

def cuboid(name,mat,center,size,rotation=(0,0,0),pivot=None):
    size=[max(.5,round(float(n)*2)/2) for n in size];pivot=list(pivot or center)
    a=[center[i]-size[i]/2 for i in range(3)];b=[center[i]+size[i]/2 for i in range(3)]
    v=[[a[0],a[1],a[2]],[b[0],a[1],a[2]],[b[0],b[1],a[2]],[a[0],b[1],a[2]],
       [a[0],a[1],b[2]],[b[0],a[1],b[2]],[b[0],b[1],b[2]],[a[0],b[1],b[2]]]
    sides=[('north',[0,3,2,1]),('south',[4,5,6,7]),('west',[0,4,7,3]),('east',[1,2,6,5]),('up',[0,1,5,4]),('down',[3,7,6,2])]
    rects=cube_uv(mat,size);uv=[]
    for side,face in sides:
      u0,v0,u1,v1=rects[side];uv.append([[u0,v0],[u0,v1],[u1,v1],[u1,v0]])
    result=mesh(name,mat,[rotate_point(p,rotation,pivot) for p in v],[f for _,f in sides],uv)
    result['_cubes']=[{'name':name,'from':a,'to':b,'origin':pivot,'rotation':list(rotation),'faces':rects,'material':mat}]
    return result

def joined(name,mat,parts):
    vertices=[];faces=[];cubes=[]
    for part in parts:
      offset=len(vertices);vertices.extend(part['vertices'])
      faces.extend({'v':[i+offset for i in f['v']],'uv':f['uv']} for f in part['faces'])
      cubes.extend(part.get('_cubes',[]))
    return {'name':name,'material':mat,'vertices':vertices,'faces':faces,'shading':'flat','_cubes':cubes}

def animal_detail(name,mat,center,size,rotation=(0,0,0),pivot=None,inset=False):
    """A unit-density cuboid sampled from the animal's inset/detail swatch."""
    part=cuboid(name,mat,center,size,rotation,pivot)
    ox,oy,_,_=MATERIALS[mat]
    for rect,face in zip(part['_cubes'][0]['faces'].values(),part['faces']):
      u,v=rect[0],rect[1]
      du,dv=ox+(16 if inset else 8)-u,oy+24-v
      rect[:]=[rect[0]+du,rect[1]+dv,rect[2]+du,rect[3]+dv]
      face['uv']=[[x+du,y+dv] for x,y in face['uv']]
    return part

def ellipsoid(name,mat,center,radii,segments=12,rings=8):
    # Animal replacements are intentionally box forms too, not low-segment balls.
    return cuboid(name,mat,[snap(v) for v in center],[max(.5,snap(2*v)) for v in radii])

def tube(name,mat,sections,segments=10):
    parts=[]
    for i,(a,b) in enumerate(zip(sections,sections[1:])):
      delta=[b[k]-a[k] for k in range(3)];length=math.sqrt(sum(n*n for n in delta))
      if length<.05:continue
      center=[snap((a[k]+b[k])/2,.25) for k in range(3)]
      rx=max(.25,snap(max(a[3],b[3]),.25));rz=max(.25,snap(max(a[4],b[4]),.25))
      angles=[math.degrees(math.atan2(delta[2],delta[1])),0,-math.degrees(math.atan2(delta[0],math.sqrt(delta[1]**2+delta[2]**2)))]
      parts.append(cuboid(name+'_'+str(i),mat,center,[rx*2,max(.5,snap(length,.25)),rz*2],angles))
    return joined(name,mat,parts)

def pixel_plane(name,mat,left,top,width,height,z=HEAD_FRONT-.015,uv_origin=None,pivot=(0,0,0),density=1,uv_size=None):
    ox,oy,_,_=MATERIALS[mat];ux,uy=uv_origin or (0,0)
    uw,uh=uv_size or (width*density,height*density)
    v=[[left-pivot[0],top-pivot[1],z-pivot[2]],
       [left-pivot[0],top+height-pivot[1],z-pivot[2]],
       [left+width-pivot[0],top+height-pivot[1],z-pivot[2]],
       [left+width-pivot[0],top-pivot[1],z-pivot[2]]]
    return mesh(name,mat,v,[[0,1,2,3]],[[[ox+ux,oy+uy],[ox+ux,oy+uy+uh],[ox+ux+uw,oy+uy+uh],[ox+ux+uw,oy+uy]]])

def facial_disc(name,mat,cx,cy,rx,ry,offset=.08,pivot=(0,0,0),segments=16):
    return pixel_plane(name,mat,cx-rx,cy-ry,rx*2,ry*2,HEAD_FRONT-offset,pivot=pivot)

def wing_mesh(name,mat,side=1,big=False):
    # A single alpha-textured wing plane without a duplicate coplanar back face.
    w,h=(7,6) if big else (4,4)
    x0,x1=(0,w) if side>0 else (-w,0)
    verts=[[x0,-h+1,.3+abs(x0)*.22],[x0,1,.3+abs(x0)*.22],
           [x1,1,.3+abs(x1)*.22],[x1,-h+1,.3+abs(x1)*.22]]
    ox,oy,_,_=MATERIALS[mat]
    ux,uy=(24,24) if mat.startswith('animal_') else ((8,0) if big else (0,0))
    return mesh(name,mat,verts,[[0,1,2,3]],[[[ox+ux,oy+uy],[ox+ux,oy+uy+h],[ox+ux+w,oy+uy+h],[ox+ux+w,oy+uy]]])

bone('root',None,[0,0,0])
body=bone('body','root',[0,19.75,0],[cuboid('torso','body',(0,0,0),(5,4.5,3.5))])
head=bone('head','root',[0,14.75,0],[cuboid('head','head',(0,-.5,0),(8,6.5,6.5))])
# Original reconstruction from the official screenshots: a broad hooked crest.
bone('tuft','head',[0,0,0],[cuboid('crest_base','tuft',(0,-3.9,.15),(4,1.5,2.5),(-22.5,0,0)),cuboid('crest_cap','crest_tip',(0,-4.8,.65),(4,1,2.5),(-22.5,0,0))])
bone('hero_crest','head',[0,0,0],sum(([cuboid('hero_fork_'+str(side),'tuft',(side*.8,-4.45,.4),(1.5,2.5,1.5),(0,0,side*30)),cuboid('hero_cap_'+str(side),'crest_tip',(side*1.45,-5.5,.4),(1.5,.5,1.5),(0,0,side*30))] for side in(-1,1)),[]))
bone('dark_crest','head',[0,0,0],[cuboid('dark_hook','tuft',(0,-4.0,.5),(3,1.5,3),(-35,0,0)),cuboid('dark_tip','crest_tip',(0,-4.5,2.0),(2,.5,2),(-10,0,0))])
bone('belly','body',[0,0,0],[pixel_plane('adult_chest_mark','belly',-1,-1,2,2,-1.768,density=2)])
bone('tail','body',[0,1.0,1.75],[cuboid('tail_base','body',(0,0,.75),(1.5,1.5,1.5),(15,0,0),(0,0,0)),cuboid('tail_cap','crest_tip',(0,0,1.75),(1.5,1.5,.5),(15,0,0),(0,0,0))])
for side,label in((1,'left'),(-1,'right')):
 # A low shoulder and hanging hand form the neutral pose in both the game and
 # an unanimated Blockbench viewport. Keep the upper edge below the head.
 bone(label+'_arm','body',[side*2.65,-1.10,0],[cuboid('arm_connector','body',(side*.10,.50,0),(1,1,1)),cuboid('hand','hand',(side*.35,1.40,-.15),(2,2,2))])
 bone(label+'_foot','root',[side*1.5,23,0],[cuboid('heel','heel',(0,0,1),(2.5,2,1)),cuboid('toe','toe',(0,0,-.5),(2.5,2,2))])
 bone(label+'_wing','body',[side*2,-1,1.75],[wing_mesh('small_wing','wing',side)])
 bone('type_swim_'+label,label+'_arm',[0,0,0],[cuboid('swim_hand','hand',(side*.35,1.40,-.15),(2,2,1))])
 bone('swim_foot_'+label,label+'_foot',[0,0,0],[cuboid('swim_ankle','heel',(0,-.5,.25),(2.5,1,2)),cuboid('swim_paddle','toe',(side*.15,.25,-.65),(3.5,1.5,4.5),(0,-side*8,0))])
 bone('type_fly_'+label,label+'_wing',[0,0,0],[wing_mesh('flight_wing','wing',side,True)])
 for form in ('light','angel','devil'):
  bone('chaos_'+form+'_wing_'+label,'body',[side*2,-1,1.75],
       [extrude('tiny_chaos_wing','crest_tip',[(0,-.4),(side*1.25,.1),(0,.7)],.2,center=(0,0,.35))])
 bone('chaos_devil_spike_'+label,label+'_arm',[0,0,0],
      [cuboid('arm_spike','crest_tip',(side*1.10,.95,.5),(1,1,1),(0,0,-side*30))])
eyes=bone('eyes','head',[0,0,0])
lids=bone('lids','head',[0,0,0])
happy=bone('eyes_happy','head',[0,0,0])
for x in(-2.25,2.25):
 eyes['meshes'].append(pixel_plane('column_eye','eye',x-1.25,-1,2.5,3,uv_size=(4,6)))
 lids['meshes'].append(pixel_plane('closed_eye','lid',x-1.25,-1,2.5,3,density=2))
 happy['meshes'].append(pixel_plane('happy_eye','happy_eye',x-1.25,-1,2.5,3,density=2))
mouthpivot=(0,2.1,0)
bone('mouth','head',list(mouthpivot),[pixel_plane('small_smile','mouth',-1,1.65,2,1,HEAD_FRONT-.025,pivot=mouthpivot,density=2)])
bone('mouth_open','head',list(mouthpivot),[pixel_plane('eating_mouth','mouth_open',-.75,1.65,1.5,1,HEAD_FRONT-.025,pivot=mouthpivot,density=2)])
bone('dark_grin','head',[0,0,0],[pixel_plane('dark_toothy_grin','grin',-2,1.65,4,1,HEAD_FRONT-.03,density=2)])
bone('dark_brows','head',[0,0,0],[cuboid('brow_'+str(side),'brow',(side*2.25,-1.5,HEAD_FRONT-.06),(3,.5,.5),(0,0,-side*12)) for side in(-1,1)])
for name,mat,parts in [('orb','orb',[cuboid('emotion_cube','orb',(0,0,0),(2.5,2.5,2.5))]),('halo','orb',[cuboid('hero_emotion_cube','orb',(0,0,0),(2.5,2.5,2.5))]),('star','dark_orb',[cuboid('dark_emotion_cube','dark_orb',(0,0,0),(2.5,2.5,2.5))]),('heart','heart',[extrude('happy_heart','heart',[(0,1.3),(-1.1,.25),(-1.45,-.5),(-1.25,-1.15),(-.65,-1.35),(0,-.8),(.65,-1.35),(1.25,-1.15),(1.45,-.5),(1.1,.25)],.45)]),('flame','flame',[tube('emotion_flame','flame',[(0,1,0,.85,.7),(-.2,0,0,1.0,.8),(.2,-1.0,0,.7,.55),(-.15,-2.05,.1,.35,.25),(.4,-2.8,.25,.02,.02)],10)])]:
 bone(name,'head',[0,-8.8,.75],parts)
bone('cocoon','root',[0,12.5,0],[sphere_mesh('evolution_shell','cocoon',(0,0,0),(5.8,9.2,5.2),16,12)])
bone('crest_swim','head',[0,0,0],sum(([cuboid('swim_head_fin_'+str(side),'tuft',(side*3.35,-2.5,2.1),(2.5,1.5,4),(0,side*12,-side*12)),cuboid('swim_fin_bar_'+str(side),'crest_tip',(side*3.55,-2.5,3.4),(2.5,.5,1.5),(0,side*12,-side*12))] for side in(-1,1)),[]))
bone('crest_fly','head',[0,0,0],sum(([cuboid('fly_head_fin_'+str(side),'tuft',(side*3.65,-4.1,.2),(2,3.5,2),(0,0,side*50)),cuboid('fly_fin_tip_'+str(side),'crest_tip',(side*4.95,-5.2,.2),(2,1,2),(0,0,side*50))] for side in(-1,1)),[]))
bone('crest_hero_fly','head',[0,0,0],sum(([cuboid('hero_fly_fork_'+str(side),'tuft',(side*3.35,-4.5,.2),(5,1,1),(0,0,-side*18)),cuboid('hero_fly_tip_'+str(side),'crest_tip',(side*5.65,-5.25,.2),(1,1,1),(0,0,-side*18))] for side in(-1,1)),[]))
bone('crest_run','head',[0,0,0],[cuboid('run_center','tuft',(0,-4.05,.8),(3,2,3),(-35,0,0)),cuboid('run_center_bar','crest_tip',(0,-4.7,1.9),(3,.5,1.5),(-35,0,0))]+sum(([cuboid('run_side_blade_'+str(side),'tuft',(side*3.55,-1.5,2.0),(2,1.5,4),(10,side*25,-side*10)),cuboid('run_side_bar_'+str(side),'crest_tip',(side*3.95,-1.35,3.1),(2,.5,2.5),(10,side*25,-side*10))] for side in(-1,1)),[]))
bone('crest_power','head',[0,0,0],[cuboid('power_swept_crown','tuft',(0,-4.15,.65),(4,2,3),(-35,0,0)),cuboid('power_crown_border','crest_tip',(0,-4.9,1.65),(4,.5,2),(-35,0,0))]+sum(([cuboid('power_temple_fin_'+str(side),'tuft',(side*3.4,-2.45,1.75),(2,2,3.5),(-15,side*28,side*12)),cuboid('power_fin_border_'+str(side),'crest_tip',(side*3.9,-2.2,3.0),(2,2,.5),(-15,side*28,side*12))] for side in(-1,1)),[]))
bone('crest_hero_power','head',[0,0,0],sum(([cuboid('hero_power_prong_'+str(side),'tuft',(side*.9,-4.35,.3),(1.5,2,1.5),(0,0,side*20)),cuboid('hero_power_cap_'+str(side),'crest_tip',(side*1.2,-5.2,.3),(1.5,.5,1.5),(0,0,side*20))] for side in(-1,1)),[]))
bone('crest_dark_power','head',[0,0,0],[cuboid('dark_power_base','tuft',(0,-4.2,.65),(3,2,3),(-20,0,0)),cuboid('dark_power_sweep','tuft',(0,-5.65,1.15),(2,2,2),(-20,0,0)),cuboid('dark_power_tip','crest_tip',(0,-6.85,1.6),(1,1,1),(-25,0,0))])
bone('crest_hero_run','head',[0,0,0],[cuboid('hero_run_prong','tuft',(0,-4.7,.45),(1.5,3,1.5),(-25,0,0)),cuboid('hero_run_tip','crest_tip',(0,-5.9,1.0),(1.5,.5,1.5),(-25,0,0))])
# Chaos forms retain the same compact face, with their own iconic silhouettes.
# These are original cuboid interpretations of the game/viewer references.
bone('chaos_light_crest','head',[0,0,0],[
 cuboid('droplet_base','tuft',(0,-4.15,.8),(3.5,2,3),(-15,0,0)),
 cuboid('droplet_sweep','tuft',(0,-5.8,1.25),(2.5,2,2.5),(-18,0,0)),
 cuboid('droplet_tip','crest_tip',(0,-7.25,1.65),(1,1.5,1.5),(-18,0,0)),
 cuboid('downturned_rear','tuft',(0,1.8,3.8),(3,3,1.5),(22.5,0,0)),
 ]+sum(([cuboid('water_lobe_'+str(side),'tuft',(side*3.65,-.1,2),(2,2.5,2.5),(0,side*15,-side*12)),
         cuboid('water_lobe_tip_'+str(side),'crest_tip',(side*4.0,1.25,2.2),(1,1,1.5),(0,side*15,-side*12))] for side in(-1,1)),[]))
bone('chaos_angel_crest','head',[0,0,0],sum(([
 cuboid('waved_horn_peak_'+str(side),'crest_tip',(side*2.1,-6.45,1.0),(1.5,2.5,1.5),(0,0,side*20)),
 cuboid('waved_horn_arch_'+str(side),'tuft',(side*3.35,-4.75,1.0),(1.5,3,2),(0,0,-side*30)),
 cuboid('waved_horn_outer_'+str(side),'tuft',(side*4.65,-2.2,1.25),(1.5,3,2),(0,0,-side*20)),
 cuboid('waved_horn_end_'+str(side),'crest_tip',(side*5.3,-.2,1.5),(1.5,1.5,1.5),(0,0,-side*10)),
 ] for side in(-1,1)),[])+[
 cuboid('angel_rear_spike','tuft',(0,1.65,4.0),(2.5,4,1.5),(25,0,0)),
 cuboid('angel_rear_tip','crest_tip',(0,3.65,5.05),(1.5,1.5,1),(25,0,0))])
bone('chaos_devil_crest','head',[0,0,0],sum(([
 cuboid('upper_horn_base_'+str(side),'tuft',(side*3.55,-3.95,1.0),(2,2.5,2),(0,0,side*35)),
 cuboid('upper_horn_'+str(side),'tuft',(side*4.5,-5.75,1.0),(1.5,2.5,1.5),(0,0,-side*8)),
 cuboid('upper_flame_tip_'+str(side),'crest_tip',(side*4.2,-7.2,1.0),(1,1.5,1),(0,0,-side*20)),
 cuboid('lower_horn_'+str(side),'tuft',(side*3.8,.1,1.9),(2,2.5,2),(0,0,-side*20)),
 cuboid('lower_flame_tip_'+str(side),'crest_tip',(side*4.35,1.75,2.0),(1,1.5,1.5),(0,0,-side*20)),
 ] for side in(-1,1)),[]))
bone('chaos_devil_tail','body',[0,1,1.75],[
 cuboid('long_tail_base','tuft',(0,0,1.25),(1,1,2.5)),
 cuboid('long_tail_sweep','tuft',(0,-.35,3.1),(1,1,2),(15,0,0)),
 ]+ [cuboid('forked_tail_'+str(side),'crest_tip',(side*.6,-.9,4.0),(1,1.5,1),(0,0,side*30)) for side in(-1,1)])
bone('chaos_light_marker','head',[0,-10.3,.75],[cuboid('light_emote','orb',(0,0,0),(2,2,2))])
bone('chaos_angel_marker','head',[0,-10.3,.75],[torus('angel_halo','orb',1.6,.30,16,6)])
bone('chaos_devil_marker','head',[0,-9.5,.75],[tube('purple_flame','flame',[(0,1,0,.7,.6),(-.2,0,0,.9,.7),(.15,-1,0,.6,.5),(-.1,-2,0,.3,.25),(.35,-2.8,.1,.02,.02)],10)])

# SA2/B-style animal gene slots. The new pieces retain the add-on's chunky
# anatomy, leave the entire facial plane clear, and inherit the limb animations.
ANIMAL_SLOTS = [
 (0,2,4),(0,6),(0,1,3,4,6),(2,4,6,7),(2,4,6,7),(1,4,6,7),
 (0,1,4,6),(0,1,4,6),(0,1,3,4,6),(0,1,4),(0,1,4,6),(0,1,2,4),
 (0,2,4,6),(0,1,4,5,6),(0,1,4),(0,1,4,5,6,7),(0,1,2,4,6),
 (2,4,6,7),(),(),(),
]
for animal in (range(21) if INCLUDE_ANIMAL_VARIATIONS else ()):
 mat='animal_'+str(animal)
 for slot in ANIMAL_SLOTS[animal]:
  if slot in (0,4,7):
   for side,label in ((1,'left'),(-1,'right')):
    parent={0:label+'_arm',4:label+'_foot',7:label+'_wing'}[slot]
    if slot == 0:
     # Keep the shoulder clearance, and put extra paw/flipper volume below it.
     width=3 if animal in (9,10,11,15) else 2.5
     depth=1 if animal in (0,1,2) else 2
     parts=[cuboid('gene_arm_connector',mat,(side*.10,.50,0),(1,1,1)),
            cuboid('gene_hand',mat,(side*.35,1.65,-.15),(width,2.5,depth))]
     if animal in (9,10,11,15):
      parts += [animal_detail('paw_digit_'+str(n),mat,(side*.35+n*.75,2.5,-1.0),(.5,.5,.5)) for n in (-1,0,1)]
     elif animal == 6:
      parts.append(animal_detail('rabbit_paw',mat,(side*.35,2.15,-1.15),(1.5,1,.5)))
    elif slot == 4:
     paddle=animal in (0,2,3,4,5,15,17)
     long_paw=animal in (6,7,10)
     parts=[cuboid('gene_heel',mat,(0,0,1),(2.5,2,1.5)),
            cuboid('gene_toe',mat,(0,.25,-.75),(3 if paddle or long_paw else 2.5,2.5,4 if long_paw else 3.5 if paddle else 2.5))]
     if animal in (8,13,16):
      parts.append(animal_detail('split_hoof',mat,(0,.5,-2.1),(.5,1.5,.5)))
     elif paddle:
      parts += [animal_detail('toe_digit_'+str(n),mat,(n,.5,-2.35),(.5,1,.5)) for n in (-1,0,1)]
     elif long_paw:
      parts.append(animal_detail('paw_toe_patch',mat,(0,-1.05,-1.3),(2,.5,2)))
    else:
     # Three stepped feathers replace the old two tiny strips. The broad
     # root stays behind the shoulder and each feather has a contrasting tip.
     parts=[cuboid('gene_wing_root',mat,(side*1,.7,2.2),(2.5,2,.5),(0,0,-side*10))]
     for n in range(3):
      center=(side*(2.3+n*.65),.3+n*.55,2.25+n*.15)
      parts.append(cuboid('gene_wing_feather_'+str(n),mat,center,(2.5,1,.5),(0,0,-side*(20-n*10))))
      parts.append(animal_detail('gene_wing_tip_'+str(n),mat,(center[0]+side*1,center[1]-.15,center[2]-.05),(.5,1,.5),(0,0,-side*(20-n*10))))
    bone(f'animal_{slot}_{animal}_{label}',parent,[0,0,0],parts)
  else:
   parent='body' if slot == 6 else 'head'
   if slot == 1:
    parts=[]
    for side in (-1,1):
     if animal == 6:
      root=(side*3.35,-3.45,.7);rot=(0,0,side*12)
      parts += [cuboid('rabbit_ear_root_'+str(side),mat,(side*3.35,-4.05,.7),(2,1.5,1.5),rot,root),
                cuboid('rabbit_ear_outer_'+str(side),mat,(side*3.35,-5.65,.7),(2,3,1.5),rot,root),
                cuboid('rabbit_ear_cap_'+str(side),mat,(side*3.35,-7.25,.7),(1.5,1,1.5),rot,root),
                animal_detail('rabbit_ear_inset_'+str(side),mat,(side*3.35,-5.65,-.05),(1,3,.5),rot,root,inset=True),
                animal_detail('rabbit_ear_inset_tip_'+str(side),mat,(side*3.35,-7.0,-.05),(.5,.5,.5),rot,root,inset=True)]
     elif animal in (7,10):
      root=(side*3.75,-3.25,.8);rot=(0,0,side*18)
      parts += [cuboid('cat_ear_root_'+str(side),mat,(side*3.75,-4.0,.8),(2,2,1.5),rot,root),
                cuboid('cat_ear_tip_'+str(side),mat,(side*3.75,-5.25,.8),(1,1,1.5),rot,root),
                animal_detail('cat_ear_inner_'+str(side),mat,(side*3.75,-4.4,.05),(1,1.5,.5),rot,root,inset=True)]
     elif animal in (8,13,15):
      rot=(0,0,-side*20)
      parts += [cuboid('floppy_ear_'+str(side),mat,(side*4.45,-3.55,.9),(2.5,1.5,1.5),rot),
                animal_detail('floppy_ear_inner_'+str(side),mat,(side*4.6,-3.55,.15),(1.5,.5,.5),rot,inset=True)]
     else:
      rot=(0,0,side*10)
      parts += [cuboid('gene_ear_root_'+str(side),mat,(side*3.8,-3.8,.8),(2,2,1.5),rot),
                cuboid('gene_ear_cap_'+str(side),mat,(side*3.8,-4.8,.8),(1.5,.5,1.5),rot),
                animal_detail('gene_ear_inner_'+str(side),mat,(side*3.8,-4.0,.05),(1,1,.5),rot,inset=True)]
   elif slot == 2:
    if animal == 16:
     parts=[cuboid('unicorn_root',mat,(0,-4.25,.4),(1.5,1.5,1.5)),
            animal_detail('unicorn_spire',mat,(0,-5.5,.4),(1,2,1)),
            animal_detail('unicorn_tip',mat,(0,-6.75,.4),(.5,.5,.5))]
    elif animal in (3,4,17):
     parts=[cuboid('feather_crest_root',mat,(0,-3.9,.4),(3,1,2))]
     for n in (-1,0,1):
      parts += [cuboid('crest_feather_'+str(n),mat,(n*.9,-5.0-abs(n)*.15,.5),(1,2.5,1),(0,0,n*15)),
                animal_detail('crest_feather_tip_'+str(n),mat,(n*1.2,-6.25-abs(n)*.15,.5),(1,.5,1),(0,0,n*15))]
    else:
     parts=[cuboid('gene_crest_root',mat,(0,-4.1,.4),(3,1.5,2.5),(-15,0,0)),
            cuboid('gene_crest_mid',mat,(0,-5.0,.7),(2.5,1,2),(-15,0,0)),
            animal_detail('gene_crest_tip',mat,(0,-5.65,.85),(2,.5,1.5),(-15,0,0))]
   elif slot == 3:
    if animal == 8:
     parts=[cuboid('boar_snout',mat,(0,.8,HEAD_FRONT-.55),(1.5,1,1)),
            animal_detail('snout_bridge',mat,(0,.8,HEAD_FRONT-1.1),(.5,.5,.5))]
    else:
     parts=[cuboid('otter_nose',mat,(0,1.1,HEAD_FRONT-.35),(1,.5,.5))]
     parts += [animal_detail('otter_whisker_'+str(side),mat,(side*4.35,1.0,HEAD_FRONT-.1),(1.5,.5,.5),(0,0,side*10)) for side in (-1,1)]
   elif slot == 5:
    if animal == 13:
     parts=sum(([cuboid('sheep_horn_top_'+str(side),mat,(side*4.0,-3.3,1),(2,1.5,2)),
                 animal_detail('sheep_horn_outer_'+str(side),mat,(side*4.85,-2.5,1.2),(1.5,2,2)),
                 cuboid('sheep_horn_end_'+str(side),mat,(side*4.25,-1.65,.8),(1.5,.5,1.5))] for side in (-1,1)),[])
    else:
     parts=sum(([cuboid('dragon_horn_root_'+str(side),mat,(side*3.5,-4.4,1),(1.5,2,1.5)),
                 animal_detail('dragon_horn_upper_'+str(side),mat,(side*3.85,-6.0,1.3),(1,1.5,1),(0,0,side*15)),
                 animal_detail('dragon_horn_cap_'+str(side),mat,(side*4.1,-7.0,1.3),(.5,.5,.5))] for side in (-1,1)),[])
   elif slot == 6:
    if animal in (3,4,5,17):
     parts=[cuboid('gene_tail_root',mat,(0,1.1,2.5),(2,1.5,2))]
     for n in (-2,-1,0,1,2):
      center=(n*1.0,.5+abs(n)*.25,3.6+abs(n)*.15)
      parts += [cuboid('gene_tail_feather_'+str(n),mat,center,(1,3 if animal==3 else 2.5,.5),(0,0,n*12)),
                animal_detail('gene_tail_eye_'+str(n),mat,(center[0],center[1]-.55,center[2]+.3),(.5,1,.5),(0,0,n*12))]
    elif animal == 6:
     parts=[cuboid('rabbit_tail_root',mat,(0,1.1,2.8),(2.5,2.5,2)),
            cuboid('rabbit_tail_cap',mat,(0,1.1,3.9),(1.5,1.5,.5))]
    elif animal in (7,10,14):
     parts=[cuboid('long_tail_root',mat,(0,1.1,2.8),(1.5,1.5,2.5)),
            cuboid('long_tail_mid',mat,(0,.7,4.65),(1,1,2),(-15,0,0)),
            animal_detail('long_tail_tip',mat,(0,.0,5.75),(1,1.5,1),(-25,0,0))]
    elif animal == 12:
     parts=[cuboid('skunk_tail_root',mat,(0,1.1,2.8),(2,2,2)),
            cuboid('skunk_tail_bush',mat,(0,.0,4.1),(2.5,3,1.5),(-20,0,0)),
            animal_detail('skunk_tail_stripe',mat,(0,.0,4.9),(1,2.5,.5),(-20,0,0))]
    elif animal == 2:
     parts=[cuboid('otter_tail_base',mat,(0,1.3,3.0),(2,1,3)),
            cuboid('otter_tail_paddle',mat,(0,1.6,4.75),(2.5,.5,1.5))]
    else:
     parts=[cuboid('gene_tail_root',mat,(0,1.3,2.7),(2,1.5,2.5)),
            animal_detail('gene_tail_tip',mat,(0,1.05,4.25),(1.5,1,1.5),(-15,0,0))]
   bone(f'animal_{slot}_{animal}',parent,[0,0,0],parts)

append_variant_bones(globals(),VARIANTS)
CHAO={'format_version':1,'texture_width':256,'texture_height':256,'ground_y':24,'materials':MATERIALS,'bones':BONES}
EGG={'format_version':1,'texture_width':64,'texture_height':64,'ground_y':0,'materials':{'shell':[0,0,64,64]},'bones':[{'name':'egg','parent':None,'pivot':[0,0,0],'meshes':[]}]}
# An egg's upper half narrows smoothly; ring UVs wrap over the spotted shell.
egg=rounded_tube('incubating_egg','body',[(0,0,0,.3,.3),(0,-.7,0,2.55,2.55),(0,-2.4,0,3.75,3.75),(0,-4.8,0,3.7,3.7),(0,-7.0,0,3.0,3.0),(0,-8.9,0,1.9,1.9),(0,-9.8,0,.25,.25)],16)
egg['material']='shell'
# Use full-shell UVs based on actual longitudinal position and height.
for f in egg['faces']:
 f['uv']=[]
 for index in f['v']:
  x,y,z=egg['vertices'][index];f['uv'].append([((math.atan2(z,x)/math.tau)%1)*63.5,max(.5,min(63.5,-y/9.8*63.0))])
EGG['bones'][0]['meshes']=[egg]

def repair_and_canonicalize(data):
    # Orient each connected component by shared topology, never global centroids.
    # This preserves normals on bent tubes, torus holes and concave wing silhouettes.
    for b in data['bones']:
      b['pivot']=[round(float(n),5) for n in b['pivot']]
      for m in b['meshes']:
        for point in m['vertices']:
          for k,n in enumerate(point):point[k]=round(float(n),5)
        faces=m['faces'];edges={}
        for fi,f in enumerate(faces):
          f['uv']=[[round(float(n),5) for n in uv] for uv in f['uv']]
          ids=f['v']
          for i,a in enumerate(ids):
            z=ids[(i+1)%len(ids)];edges.setdefault(tuple(sorted((a,z))),[]).append((fi,a,z))
        flips={};components=[]
        for first in range(len(faces)):
          if first in flips:continue
          flips[first]=False;pending=[first];component=[]
          while pending:
            fi=pending.pop();component.append(fi);ids=faces[fi]['v']
            for i,a in enumerate(ids):
              z=ids[(i+1)%len(ids)]
              for other,oa,oz in edges[tuple(sorted((a,z)))]:
                if other==fi:continue
                wanted=flips[fi] ^ (oa==a and oz==z)
                if other not in flips:flips[other]=wanted;pending.append(other)
                elif flips[other]!=wanted:raise ValueError('Non-orientable mesh '+m['name'])
          components.append(component)
        for fi,flip in flips.items():
          if flip:faces[fi]['v'].reverse();faces[fi]['uv'].reverse()
        for component in components:
          closed=all(len(edges[tuple(sorted((a,f['v'][(i+1)%len(f['v'])])))] )==2 for fi in component for f in [faces[fi]] for i,a in enumerate(f['v']))
          if not closed:continue
          volume=0
          for fi in component:
            ids=faces[fi]['v'];v0=m['vertices'][ids[0]]
            for j in range(1,len(ids)-1):volume+=dot(v0,cross(m['vertices'][ids[j]],m['vertices'][ids[j+1]]))/6
          if volume<0:
            for fi in component:faces[fi]['v'].reverse();faces[fi]['uv'].reverse()
    children={}
    for b in data['bones']:children.setdefault(b['parent'],[]).append(b)
    ordered=[]
    def append(parent):
      for b in children.get(parent,[]):ordered.append(b);append(b['name'])
    append(None);data['bones']=ordered
    return data

def save_runtime():
 for name,data in [('chao_mesh',CHAO),('egg_mesh',EGG)]:
  repair_and_canonicalize(data)
  (MODEL/f'{name}.json').write_text(json.dumps(data,separators=(',',':')))

def uid(value):return str(uuid.uuid5(uuid.NAMESPACE_URL,'https://chaogarden.local/'+value))
def absolute_pivots(data):
 out={}
 for b in data['bones']:
  p=out.get(b['parent'],[0,0,0]);out[b['name']]=[p[i]+b['pivot'][i] for i in range(3)]
 return out

def bbmodel(data,name,texture_path,alignment='neutral'):
 ground=data.get('ground_y',24);pivots=absolute_pivots(data);elements=[];groups={};roots=[]
 default={'root','body','head','tuft','eyes','tail','left_arm','right_arm','left_foot','right_foot','left_wing','right_wing','orb','egg'}
 if alignment=='hero':default.discard('orb');default.discard('tuft');default.update(('halo','hero_crest','belly'))
 if alignment=='dark':default.discard('orb');default.discard('tuft');default.update(('star','dark_crest','dark_grin','dark_brows','belly'))
 if alignment.startswith('chaos_'):
  default.difference_update(('orb','tuft','tail','left_wing','right_wing'))
  default.update((alignment+'_crest',alignment+'_marker',alignment+'_wing_left',alignment+'_wing_right','belly'))
  if alignment=='chaos_devil':default.update(('chaos_devil_spike_left','chaos_devil_spike_right','chaos_devil_tail'))
 if alignment.startswith('variant_'):
  default.difference_update(('orb','tuft','belly'))
  default.update((alignment+'_head',alignment+'_body',alignment+'_marker'))
 for b in data['bones']:
  n=b['name'];world=pivots[n];origin=[world[0],ground-world[1],world[2]]
  group={'name':n,'origin':origin,'rotation':[0,0,0],'color':0,'uuid':uid(name+'/bone/'+n),'export':True,'visibility':n in default,'isOpen':False,'children':[]}
  groups[n]=group
  for mi,m in enumerate(b['meshes']):
   if m.get('_cubes'):
    for ci,cube in enumerate(m['_cubes']):
     mid=uid(name+'/cube/'+n+'/'+str(mi)+'/'+str(ci))
     a,z=cube['from'],cube['to'];cp=cube['origin'];rot=cube['rotation']
     cf=[world[0]+a[0],ground-world[1]-z[1],world[2]+a[2]]
     ct=[world[0]+z[0],ground-world[1]-a[1],world[2]+z[2]]
     co=[world[0]+cp[0],ground-world[1]-cp[1],world[2]+cp[2]]
     faces={key:{'uv':rect,'texture':0,'rotation':0} for key,rect in cube['faces'].items()}
     show_cube = True
     elements.append({'name':cube['name'],'type':'cube','uuid':mid,'from':cf,'to':ct,'origin':co,'rotation':[-rot[0],rot[1],-rot[2]],'inflate':0,'box_uv':False,'mirror_uv':False,'faces':faces,'visibility':show_cube,'export':True,'shading':'flat','chao_material':cube['material']})
     group['children'].append(mid)
    continue
   mid=uid(name+'/mesh/'+n+'/'+str(mi));verts={f'v{i}':[v[0],-v[1],v[2]] for i,v in enumerate(m['vertices'])};faces={}
   for fi,f in enumerate(m['faces']):
    vs=list(reversed(f['v']));uv=list(reversed(f['uv']))
    faces[f'f{fi}']={'vertices':[f'v{i}' for i in vs],'uv':{f'v{i}':point for i,point in zip(vs,uv)},'texture':0}
   elements.append({'name':m['name'],'type':'mesh','uuid':mid,'origin':origin,'rotation':[0,0,0],'visibility':True,'export':True,'shading':m.get('shading','flat'),'vertices':verts,'faces':faces,'chao_material':m['material']})
   group['children'].append(mid)
  if b['parent'] is None:roots.append(group)
  else:groups[b['parent']]['children'].append(group)
 animations=[]
 if name!='chao_egg':
  recipes={
   'idle':(3,{'left_arm':('rotation',[(0,[0,0,5]),(1.5,[0,0,6]),(3,[0,0,5])]),'right_arm':('rotation',[(0,[0,0,-5]),(1.5,[0,0,-6]),(3,[0,0,-5])]),'head':('rotation',[(0,[0,0,0]),(1.5,[-3,4,-2]),(3,[0,0,0])]),'orb':('position',[(0,[0,0,0]),(1.5,[0,.45,0]),(3,[0,0,0])])}),
   'walk':(1,{'left_arm':('rotation',[(0,[26,0,5]),(.5,[-26,0,5]),(1,[26,0,5])]),'right_arm':('rotation',[(0,[-26,0,-5]),(.5,[26,0,-5]),(1,[-26,0,-5])]),'left_foot':('rotation',[(0,[-22,0,-7]),(.5,[22,0,-7]),(1,[-22,0,-7])]),'right_foot':('rotation',[(0,[22,0,7]),(.5,[-22,0,7]),(1,[22,0,7])])}),
   'swim':(1.6,{'body':('rotation',[(0,[18,0,0]),(1.6,[18,0,0])]),'left_arm':('rotation',[(0,[37,0,5]),(.8,[-37,0,5]),(1.6,[37,0,5])]),'right_arm':('rotation',[(0,[-37,0,-5]),(.8,[37,0,-5]),(1.6,[-37,0,-5])])}),
   'eat':(1.2,{'left_arm':('rotation',[(0,[34,0,5]),(.6,[40,0,5]),(1.2,[34,0,5])]),'right_arm':('rotation',[(0,[34,0,-5]),(.6,[40,0,-5]),(1.2,[34,0,-5])]),'head':('rotation',[(0,[-5,0,0]),(.6,[-12,0,0]),(1.2,[-5,0,0])])}),
   'sleep':(4,{'left_arm':('rotation',[(0,[0,0,8]),(4,[0,0,8])]),'right_arm':('rotation',[(0,[0,0,-8]),(4,[0,0,-8])]),'body':('position',[(0,[0,-2.5,0]),(2,[0,-2.8,0]),(4,[0,-2.5,0])]),'head':('rotation',[(0,[-8,0,8]),(4,[-8,0,8])])}),
   'happy':(1.6,{'body':('position',[(0,[0,0,0]),(.4,[0,.9,0]),(.8,[0,0,0]),(1.2,[0,.9,0]),(1.6,[0,0,0])]),'left_arm':('rotation',[(0,[7,0,14]),(.8,[7,0,18]),(1.6,[7,0,14])]),'right_arm':('rotation',[(0,[7,0,-14]),(.8,[7,0,-18]),(1.6,[7,0,-14])])})}
  for anim,(length,channels) in recipes.items():
   animators={}
   for bn,(channel,keys) in channels.items():
    animators[groups[bn]['uuid']]={'name':bn,'type':'bone','keyframes':[{'channel':channel,'data_points':[dict(zip(('x','y','z'),[str(v) for v in values]))],'uuid':uid(name+'/key/'+anim+'/'+bn+'/'+str(t)),'time':t,'color':-1,'interpolation':'catmullrom'} for t,values in keys]}
   animations.append({'uuid':uid(name+'/animation/'+anim),'name':anim,'loop':'loop','override':False,'length':length,'snapping':20,'anim_time_update':'','blend_weight':'','start_delay':'','loop_delay':'','animators':animators})
 texture=Image.open(texture_path);buf=io.BytesIO();texture.save(buf,format='PNG')
 return {'meta':{'format_version':'4.0','model_format':'free','box_uv':False},'name':name,'model_identifier':name,'visible_box':[4,4,0],'variable_placeholders':'','resolution':{'width':data['texture_width'],'height':data['texture_height']},'elements':elements,'outliner':roots,'textures':[{'path':'','name':texture_path.name,'folder':'','namespace':'chao_garden','id':'0','uuid':uid(name+'/texture'),'width':texture.width,'height':texture.height,'uv_width':texture.width,'uv_height':texture.height,'mode':'bitmap','source':'data:image/png;base64,'+base64.b64encode(buf.getvalue()).decode()}],'animations':animations,'chao_runtime':{'format_version':1,'ground_y':ground,'materials':data['materials'],'schema':'chao_garden rigged mesh'}}


def validate(data):
 names={b['name'] for b in data['bones']};faces=0
 for b in data['bones']:
  assert b['parent'] is None or b['parent'] in names
  for m in b['meshes']:
   for f in m['faces']:
    assert len(f['v']) in(3,4) and len(f['v'])==len(f['uv'])
    assert all(0<=i<len(m['vertices']) for i in f['v'])
    assert all(math.isfinite(c) for pt in m['vertices'] for c in pt)
    assert all(0<=uv[0]<=data['texture_width'] and 0<=uv[1]<=data['texture_height'] for uv in f['uv'])
    faces+=1
 return faces

if __name__=='__main__':
 import argparse
 from blockbench_to_runtime import export_model
 parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--prototype',action='store_true');parser.add_argument('--entity-only',action='store_true',help='Rebuild rig, entity atlases and Blockbench projects without rewriting item resources');args=parser.parse_args()
 if args.prototype:
  for alignment in ('neutral','hero','dark'):material_texture(f'normal_{alignment}_0_two',NORMAL[alignment][0],True,False,alignment=alignment)
  material_texture('jewel_sapphire_two_shiny',JEWELS['sapphire'],True,True,True)
 else:
  for two in(False,True):
   for shiny in(False,True):
    suffix=('_two' if two else '_mono')+('_shiny' if shiny else '')
    for name,color in COLORS.items():material_texture(name+suffix,color,two,shiny)
    for alignment,palette in NORMAL.items():
     for typ,color in enumerate(palette):material_texture(f'normal_{alignment}_{typ}'+suffix,color,two,shiny,alignment=alignment)
    for name,color in JEWELS.items():material_texture('jewel_'+name+suffix,color,two,shiny,True)
 for form,palette in CHAOS_PALETTES.items():material_texture('chaos_'+form,palette['body'],True,False)
 for entry in VARIANTS:material_texture('variant_'+entry['id'],tuple(entry['palette']['body']),True,False)
 (RES/'chao_variants.json').write_text(json.dumps({'format_version':1,'variants':VARIANTS},indent=2)+'\n')
 active_ids={e['id'] for e in VARIANTS}
 current_heads=head_manifest()
 current_heads['ids']=[i for i in current_heads['ids'] if i in active_ids]
 current_heads['preview_ids']=[i for i in current_heads['preview_ids'] if i in active_ids]
 current_heads['profiles']={i:p for i,p in current_heads['profiles'].items() if i in active_ids}
 (RES/'chao_head_morphs.json').write_text(json.dumps(current_heads,indent=2)+'\n')
 for name,data in [('chao_mesh',CHAO),('egg_mesh',EGG)]:repair_and_canonicalize(data)
 for name,alignment,key in [('chao_adventure_master','neutral','normal_neutral_0_two'),('chao_hero','hero','normal_hero_0_two'),('chao_dark','dark','normal_dark_0_two')]+[('chao_chaos_'+form,'chaos_'+form,'chaos_'+form) for form in CHAOS_PALETTES]+[('chao_mod_variants','variant_create','variant_create')]:
  project=bbmodel(CHAO,name,TEX/'chao'/f'{key}.png',alignment)
  (BB/f'{name}.bbmodel').write_text(json.dumps(project,separators=(',',':')))
  if name=='chao_adventure_master':(MODEL/'chao_mesh.json').write_text(json.dumps(export_model(project),separators=(',',':')))
 project=bbmodel(EGG,'chao_egg',TEX/'eggs/normal_neutral_0_two.png')
 (BB/'chao_egg.bbmodel').write_text(json.dumps(project,separators=(',',':')))
 (MODEL/'egg_mesh.json').write_text(json.dumps(export_model(project),separators=(',',':')))
 if not args.entity_only:
  from generate_variant_resources import main as generate_theme_resources
  generate_theme_resources()
 print(f"Generated DLC-reference Chao reconstruction prototype={args.prototype}: {len(BONES)} bones, {len(VARIANTS)} themes and 8 editable Blockbench projects")
