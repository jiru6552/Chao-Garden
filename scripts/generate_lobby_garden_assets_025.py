#!/usr/bin/env python3
"""Authored native textures/materials for the transparent SA2-style lobby and garden."""
from pathlib import Path
import json, math, random
from lobby_material_manifest0255 import render_textures
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources';AS=RES/'assets/chao_garden';TEX=AS/'textures/scene';TEX.mkdir(parents=True,exist_ok=True)
def write(path,value):
 p=RES/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,indent=2)+'\n')
def save(name,image):image.save(TEX/(name+'.png'),optimize=True)
def noise(x,y,seed=0):return math.sin(x*.371+y*.197+seed)*math.sin(x*.113-y*.281+seed*2)
def lerp(a,b,t):return tuple(round(x+(y-x)*t) for x,y in zip(a,b))
def smoothstep(a,b,v):
 t=max(0,min(1,(v-a)/(b-a)));return t*t*(3-2*t)
r=random.Random(2501)
# A celestial sphere whose lower hemisphere remains plainly visible through the floor.
im=Image.new('RGBA',(1024,512));p=im.load()
keys=[(0,(189,215,217)),(.23,(188,198,194)),(.42,(75,71,112)),(.52,(24,29,69)),(.66,(93,64,100)),(.86,(79,36,74)),(1,(20,22,48))]
for y in range(512):
 v=y/511
 for i in range(len(keys)-1):
  if keys[i][0]<=v<=keys[i+1][0]:base=lerp(keys[i][1],keys[i+1][1],(v-keys[i][0])/(keys[i+1][0]-keys[i][0]));break
 for x in range(1024):
  u=x/1023*math.tau
  n=(math.sin(u*3+v*13)+.55*math.sin(u*7-v*19)+.23*math.sin(u*13+v*31))/1.78
  # Smooth latitude envelopes replace the visible hard cloud cutoff rings.
  # Pole fade and an exact 2π wrap keep the whole sphere continuous at every view.
  envelope=.08+.70*(1-smoothstep(.29,.48,v))+.37*smoothstep(.52,.72,v)
  cloud=max(0,min(1,(n+.06)*2))*envelope*max(0,math.sin(v*math.pi))**.4
  light=lerp((237,231,210),(211,135,136),smoothstep(.42,.61,v))
  p[x,y]=tuple(round(base[c]*(1-cloud)+light[c]*cloud) for c in range(3))+(255,)
# A uniform endpoint alone cannot hide the fan formed by nearby sphere rings.
# Blend each polar row toward its mean cloud tone, keeping the first/last cap
# constant around longitude and restoring full cloud detail with a smooth fade.
for y in range(512):
 v=y/511;weight=smoothstep(.03,.18,min(v,1-v))
 if weight<1:
  mean=tuple(round(sum(p[x,y][c] for x in range(1024))/1024) for c in range(3))
  for x in range(1024):p[x,y]=lerp(mean,p[x,y][:3],weight)+(255,)
d=ImageDraw.Draw(im)
for i in range(185):
 x=r.randrange(1024);y=r.randrange(190,365);size=r.choice([1,1,2,2,3]);col=(255,226,117,255)
 if size==3:
  d.polygon([(x,y-3),(x+1,y-1),(x+3,y),(x+1,y+1),(x,y+3),(x-1,y+1),(x-3,y),(x-1,y-1)],fill=col)
 else:d.ellipse((x-size/2,y-size/2,x+size/2,y+size/2),fill=col)
# Equirectangular endpoints are the same meridian. Both poles collapse to one
# point, so neither seam nor a multi-coloured pinwheel is allowed there.
for y in range(512):im.putpixel((1023,y),im.getpixel((0,y)))
for y in (0,511):
 color=im.getpixel((0,y))
 for x in range(1024):im.putpixel((x,y),color)
save('lobby_celestial_sky',im)
# Star-pane motif and wood/timber edging use purpose-authored subdued palettes.
im=Image.new('RGBA',(256,128),(7,16,44,246));d=ImageDraw.Draw(im)
for i in range(120):
 x,y=r.randrange(256),r.randrange(128);c=r.choice([(159,208,255,150),(255,239,174,190),(75,121,192,150)])
 d.point((x,y),fill=c)
for x,y in [(27,35),(98,85),(177,47),(235,100)]:d.line((x-3,y,x+3,y),fill=(253,229,125,230));d.line((x,y-3,x,y+3),fill=(253,229,125,230))
save('lobby_star_panel',im)
for name,base in [('lobby_wood',(119,76,46)),('garden_limestone',(187,178,172)),('garden_sand',(220,205,158)),('garden_turf',(91,181,40)),('garden_palm_bark',(146,133,64))]:
 im=Image.new('RGBA',(64,64));p=im.load()
 for y in range(64):
  for x in range(64):
   n=noise(x,y,3)
   if name=='garden_limestone':
    fissure=abs(math.sin(x*.086+math.sin(y*.067)*.8));n=n*3-12*max(0,1-fissure*11)
   elif name=='garden_turf':n=n*7+3*math.sin(x*.7+y*.4)
   elif name=='garden_sand':n=n*3
   elif name=='garden_palm_bark':
    diamond=(abs((x/8)%2-1)+abs((y/12)%2-1));n=14*math.cos(diamond*math.pi)+5*noise(x,y)
   else:n=n*7+3*math.sin(x*.24)
   p[x,y]=tuple(max(0,min(255,round(c+n))) for c in base)+(255,)
 save(name,im)
# Retire the unused underside tile after grass-covered slopes became the scene model.
(TEX/'garden_soil.png').unlink(missing_ok=True)
# Soft leaf strips rendered on faceted palms; no 1m leaf cubes hide the frond silhouettes.
im=Image.new('RGBA',(64,32),(0,0,0,0));d=ImageDraw.Draw(im)
for x in range(64):
 width=max(1,int(13*math.sin(math.pi*x/68)**.65));c=(31+int(x*.15),146+int(x*.6),34,255)
 d.line((x,16-width,x,16+width),fill=c)
 if x%5==0:d.line((x,16-width,x+3,16),fill=(51,175,47,255));d.line((x,16+width,x+3,16),fill=(23,116,29,255))
d.line((0,16,63,16),fill=(109,182,53,255),width=2);save('garden_palm_frond',im)
Image.new('RGBA',(1,1),(255,255,255,255)).save(TEX/'white.png')
Image.new('RGBA',(8,8),(21,37,68,59)).save(TEX/'lobby_glass.png')
# Portal sign: readable faithful wording instead of four tiny vanilla-sign lines.
im=Image.new('RGBA',(512,128),(0,0,0,0));d=ImageDraw.Draw(im)
font=ImageFont.load_default(size=48);small=ImageFont.load_default(size=30)
d.text((256,14),'C H A O',font=small,anchor='mt',fill=(73,181,92),stroke_width=2,stroke_fill=(246,244,215))
d.text((256,55),'KINDERGARTEN',font=font,anchor='mt',fill=(23,51,170),stroke_width=3,stroke_fill=(247,247,224));save('kindergarten_portal_sign',im)
# The gazebo exit pad carries the original scene's soft blue/violet energy swirl.
im=Image.new('RGBA',(128,128),(0,0,0,0));pix=im.load()
for y in range(128):
 for x in range(128):
  dx=(x-63.5)/63.5;dy=(y-63.5)/63.5;radius=math.hypot(dx,dy)
  if radius<=1:
   wave=(math.sin(math.atan2(dy,dx)*4-radius*16)+1)/2
   base=lerp((88,70,201),(114,236,246),wave)
   center=max(0,1-radius*1.4)*.78
   pix[x,y]=tuple(round(v*(1-center)+245*center) for v in base)+(round(255*min(1,(1-radius)*16)),)
save('lobby_exit_pad',im)
im=Image.new('RGBA',(256,64),(0,0,0,0));d=ImageDraw.Draw(im)
d.text((128,8),'E X I T',font=ImageFont.load_default(size=42),anchor='mt',fill=(247,230,148),stroke_width=2,stroke_fill=(99,69,27));save('lobby_exit_sign',im)
# Native, simple block models for the authored scenery blocks.
ids=['lobby_glass_floor','lobby_scene_anchor','garden_turf','garden_limestone','garden_sand','garden_palm_trunk','garden_palm_crown','garden_palm_fruit_support']
for block in ids:
 write('assets/chao_garden/blockstates/'+block+'.json',{'variants':{'':{'model':'chao_garden:block/'+block}}})
 if block=='garden_turf':model={'parent':'minecraft:block/cube_all','textures':{'all':'chao_garden:scene/garden_turf'}}
 elif block in ['garden_limestone','garden_sand']:model={'parent':'minecraft:block/cube_all','textures':{'all':'chao_garden:scene/'+block}}
 elif block=='garden_palm_trunk':
  elems=[]
  for a,b in [([5,0,4],[11,16,12]),([4,0,5],[12,16,11])]:elems.append({'from':a,'to':b,'faces':{s:{'texture':'#bark'} for s in ['up','down','north','south','east','west']}})
  model={'parent':'minecraft:block/block','ambientocclusion':False,'textures':{'particle':'chao_garden:scene/garden_palm_bark','bark':'chao_garden:scene/garden_palm_bark'},'elements':elems}
 else:model={'parent':'minecraft:block/block','textures':{'particle':'minecraft:block/glass'},'elements':[]}
 write('assets/chao_garden/models/block/'+block+'.json',model)
# Native block models need explicit sprite sources outside the vanilla block/item
# directories. These additive singles keep the large standalone sky out of atlas.
atlas_materials=['scene/garden_turf','scene/garden_limestone','scene/garden_sand','scene/garden_palm_bark']
write('assets/minecraft/atlases/blocks.json',{'sources':[{'type':'minecraft:single','resource':'chao_garden:'+name} for name in atlas_materials]})
write('data/minecraft/tags/block/dirt.json',{'replace':False,'values':['chao_garden:garden_turf']})
write('data/chao_garden/tags/block/garden_palm_fruit_supports.json',{'replace':False,'values':['chao_garden:garden_palm_fruit_support']})
# Single unlit material: no fog mutation or global renderer state leaks.
shader={'vertex':'chao_garden:scene_unlit','fragment':'chao_garden:scene_unlit','samplers':[{'name':'Sampler0'}],
 'attributes':['Position','UV0','Color'],'uniforms':[{'name':'ModelViewMat','type':'matrix4x4','count':16,'values':[1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1]},
 {'name':'ProjMat','type':'matrix4x4','count':16,'values':[1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1]}, {'name':'ColorModulator','type':'float','count':4,'values':[1,1,1,1]}]}
write('assets/chao_garden/shaders/core/scene_unlit.json',shader)
(AS/'shaders/core/scene_unlit.vsh').write_text('#version 150\nin vec3 Position;\nin vec2 UV0;\nin vec4 Color;\nuniform mat4 ModelViewMat;\nuniform mat4 ProjMat;\nout vec2 texCoord0;\nout vec4 vertexColor;\nvoid main(){gl_Position=ProjMat*ModelViewMat*vec4(Position,1.0);texCoord0=UV0;vertexColor=Color;}\n')
(AS/'shaders/core/scene_unlit.fsh').write_text('#version 150\nuniform sampler2D Sampler0;\nuniform vec4 ColorModulator;\nin vec2 texCoord0;\nin vec4 vertexColor;\nout vec4 fragColor;\nvoid main(){vec4 color=texture(Sampler0,texCoord0)*vertexColor*ColorModulator;if(color.a<0.002)discard;fragColor=color;}\n')
manifest={'version':'0.25.0','blocks':[{'id':b,'item':False} for b in ids], 'render_textures':render_textures(ROOT,atlas_materials), 'shader':'scene_unlit','block_atlas_textures':atlas_materials,'glass_material':{'texture':'scene/lobby_glass','rgba':[21,37,68,59],'render_mode':'translucent','depth_write':False},'generator':'scripts/generate_lobby_garden_assets_025.py'}
(ROOT/'art/lobby_garden_manifest_025.json').write_text(json.dumps(manifest,indent=2)+'\n')
(ROOT/'art/lobby_garden_lang_fragment_025.json').write_text(json.dumps({f'block.chao_garden.{b}':b.replace('_',' ').title() for b in ids},indent=2)+'\n')
print('Lobby/garden025: transparent scene material, celestial underside, pale basin/palm textures and8 internal native blocks generated.')
