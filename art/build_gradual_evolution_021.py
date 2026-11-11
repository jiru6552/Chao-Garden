#!/usr/bin/env python3
"""Restrained, family-specific young→fully-aged Chao adaptations.

The independent age coordinate is not another training axis.  Each rig contains
one young form and nine fully aged targets, all on the unchanged native joints.
Reference photos inform shapes/palettes; their pixels are never used as art.
"""
from pathlib import Path
import copy,hashlib,json,math
import numpy as np
import build_forms_019 as base

ROOT=Path(__file__).resolve().parents[1]
ASSET=ROOT/'src/main/resources/assets/chao_garden'
MODELS=ASSET/'models/entity/gameplay'
AXES=[(sf,rp) for sf in (-1,0,1) for rp in (-1,0,1)]
PRIMARY=list(base.BASE)
SIDES=base.SIDES
# A path is four skull-local control points and root/tip square radii. Every
# block overlaps its neighbour. The six shared slots allow continuous blends
# between a single curl, paired feelers, three petals or five swept quills.
def path(points,r=.52,tip=.12):return {'p':[list(p) for p in points],'r':r,'tip':tip}
def mirror(p):return {**p,'p':[[-x,y,z] for x,y,z in p['p']]}
def pair(p):return [p,mirror(p)]
def stem(end=(0,-7.8,1),bend=None,r=.5,tip=.12):
 end=list(end);bend=list(bend or [end[0]*.5,-6.6,end[2]*.5])
 return path([[0,-5.7,.8],[0,-6.1,.8],bend,end],r,tip)
def horns(end=(4,-7.2,1.5),bend=None,r=.55,tip=.12,root=(1.45,-5.55,.6)):
 e=list(end);b=list(bend or [3,-6.2,1.2]);return pair(path([list(root),[2.2,-6,.9],b,e],r,tip))
def petals(end=(5.1,-1.2,1.7),r=.6,tip=.20):
 # A rounded game skull recedes at its sides, whereas the donor's square skull
 # does not. Route the shoulders OUTSIDE that fixed cube before turning down;
 # otherwise a long petal is almost entirely embedded and reads as a stripe.
 x,y,z=end;bulge=max(5.4,x+.8)
 return pair(path([[1.5,-5.6,.4],[3.6,-6.55,.5],
                   [bulge,min(-3.4,y-1.3),z*.8],[x,y,z]],r,tip))
def quills(length=5.6,up=0,spread=1,thick=.65):
 out=[]
 for side,level in [(0,0),(-1,0),(1,0),(-1,1),(1,1)]:
  # Three vertically separated tapered clusters make the profile a swept
  # Sonic/Shadow-like fan rather than five thin, parallel comb teeth.
  x=side*(1.65+level*.45);y=-3.6 if side==0 else -5.1 if level==0 else -1.75
  tip_y=y+(up if side else up*.4)+(-.35 if side and not level else .55 if level else .2)
  end_x=side*(4.0+level*.2)*spread
  out.append(path([[x,y,1.0],[x*1.2,y,2.5],[end_x*.82,y+(tip_y-y)*.55,4.4],
                   [end_x,tip_y,length-level*.25]],thick*1.65,.13))
 return out

def styles(align,primary):
 # Entries: young; aged Normal, Swim, Fly, Run, Power. Different primary
 # families deliberately use different branch topologies and directions.
 if align=='neutral' and primary=='normal':
  return [[stem((0,-7.7,.5),(0,-7.2,.7),.65,.32)], [stem((0,-7.5,.8),(0,-7.0,.8),.63,.29)],
   [stem((0,-8.5,-.1),(0,-8.5,1.0),.63,.25)], [stem((0,-7.8,-1.8),(0,-8.5,-.4),.6,.25)],
   [stem((0,-6.4,4.6),(0,-6.7,3.0),.63,.22)], [stem((0,-8.7,.8),r=.68,tip=.27)]]
 if align=='neutral' and primary=='swim':
  young=horns((4.9,-4.1,2.3),(4.3,-4.8,1.5),.68,.20,(2.55,-4.8,.4))
  return [young,horns((5.5,-4.0,2.7),(4.9,-4.8,1.8),.72,.21,(2.6,-4.8,.4)),
   petals((5.35,-.4,2.1),.80,.23),[stem((0,-8.0,.1),r=.56)]+horns((4.9,-4.3,2.4),(4.4,-5.0,1.6),.58),
   horns((4.9,-6.5,3.3),(4.3,-6.5,2.1),.60),horns((5.5,-4.3,2.1),(4.6,-4.7,1.3),.86,.34,(2.5,-4.6,.4))]
 if align=='neutral' and primary=='fly':
  young=horns((5.3,-6.7,3.6),(4.4,-7.6,2.6),.68,.25)
  young[0]['p'][1]=[2.5,-7.3,.6];young[1]=mirror(young[0])
  full=horns((6.3,-6.8,5.5),(5.1,-8.1,3.8),.72,.23);full[0]['p'][1]=[2.5,-7.5,.8];full[1]=mirror(full[0])
  return [young,horns((5.8,-6.7,4.9),(4.5,-7.9,3.1),.67,.24),
   horns((5.3,-5.7,3.3),(4.3,-7.1,2.5),.82,.28),full,
   horns((5.0,-8.0,3.5),(4.4,-8.8,2.5),.65,.25),horns((2.1,-10.0,-.5),(3.8,-9.6,1.8),.81,.25)]
 if align=='neutral' and primary=='run':
  return [quills(5.5,.2,.95,.60),quills(5.5,.3,1,.6),quills(5.3,1.0,.82,.75),
   quills(5.3,-2.7,.95,.50),quills(6.25,.7,1.03,.73),
   [stem((0,-8.9,.4),r=.84)]+horns((2.7,-7.5,2.5),r=.5)]
 if align=='neutral' and primary=='power':
  young=horns((4.1,-7.4,1.9),(3.2,-7.0,1.1),.75,.25)
  return [young,horns((3.7,-6.5,2.3),r=.72,tip=.18),horns((4.3,-5.2,2.7),r=.86,tip=.28),
   [stem((0,-8.0,.2),r=.8)]+horns((2.5,-8.4,1.0),r=.65),
   horns((2.7,-9.0,1.1),(2.4,-7.3,1),.48),[stem((0,-8.7,.5),r=.95)]+horns((3.8,-7.8,2),r=.91,tip=.22)]
 if align=='hero' and primary=='normal':
  young=horns((1.4,-6.7,.8),r=.46,tip=.38,root=(.65,-5.6,.6))
  return [young,horns((1.6,-6.9,.7),r=.5,tip=.43,root=(.65,-5.6,.6)),
   horns((2.6,-8.5,.7),(2,-7.4,.8),.51,.15,root=(.8,-5.6,.6)),
   horns((1.9,-7.1,.9),r=.47,tip=.58,root=(.65,-5.6,.6)),
   horns((3.3,-8.4,1.1),(2,-7.4,.8),.37,.07,root=(.65,-5.6,.6)),
   horns((1.8,-7.2,1),r=.52,tip=.63,root=(.65,-5.6,.6))]
 if align=='hero' and primary=='swim':
  young=petals((4.85,-3.8,1.3),.61,.22)
  return [young,petals((5.1,-3.2,1.5),.64,.23),petals((5.3,.35,1.4),.70,.22),
   horns((4.9,-8.0,1.9),(4.8,-6.2,1.3),.65),petals((5.55,-3.9,1.8),.79,.27),
   horns((5.6,-6.6,1.6),(4.7,-6.5,1.1),.92,.39)]
 if align=='hero' and primary=='fly':
  young=horns((4.35,-6.3,2.5),(3.8,-8.1,1.5),.34,.12)
  four=horns((6.15,-5.2,4.0),(5.2,-8.7,2.5),.31,.10)
  four+=horns((4.7,-4.6,4.4),(3.6,-8.4,3.1),.28,.09,root=(.6,-5.6,1.2))
  jagged=horns((5.2,-7.0,2.8),(3.9,-7.8,2.5),.32,.08)
  return [young,horns((5.1,-6.6,3.1),(4.1,-8.1,2.0),.33),jagged,four,
   horns((2.4,-8.4,1.7),(2.2,-7.4,1.3),.62,.21),
   horns((3.9,-9.1,1.8),(2.6,-7.6,1.3),.34,.08)]
 if align=='hero' and primary=='run':
  young=[stem((0,-7.15,1),r=.33)]
  return [young,[stem((0,-8.4,2.4),(0,-7.7,1.1),.34)],
   horns((2.8,-7.4,2.7),(1.7,-7.2,1.4),.53,.2,root=(.15,-5.6,.7)),
   horns((2.7,-8.5,1.9),(1.5,-8.0,.9),.73,.35,root=(.25,-5.6,.7)),
   [stem((0,-8.8,1.8),r=.58)]+horns((2.9,-8.4,2.2),r=.52,root=(.3,-5.6,.7)),
   [stem((0,-8.6,1.2),(0,-7.6,.8),.32,.77)]]
 if align=='hero' and primary=='power':
  young=[stem((0,-6.8,.8),r=.5,tip=.32)]+horns((1.7,-6.5,1.4),r=.42,tip=.26,root=(.6,-5.6,.6))
  return [young,[stem((0,-7.0,1),r=.61,tip=.35)]+horns((2.0,-6.9,1.6),r=.52,root=(.8,-5.6,.6)),
   horns((3.6,-7.9,1.6),(2.1,-7.1,.9),.8,.26),horns((3.0,-7.3,2),r=.85,tip=.52),
   [stem((0,-8.1,.9),(0,-7.7,.8),.43,.94)],
   [stem((0,-8.8,1.6),r=.79)]+horns((3.6,-8.0,2.5),r=.77,tip=.25)]
 if align=='dark' and primary=='normal':
  return [[path([[0,-4.5,2.2],[0,-5.2,2.9],[0,-5.4,3.8],[0,-5.0,4.5]],.59,.12)],
   [path([[0,-4.5,2.2],[0,-5.3,3],[0,-5.5,4.3],[0,-5.0,4.8]],.59,.12)],
   [path([[0,-4.5,2.2],[0,-4.7,3.2],[0,-3.6,4.7],[0,-2.0,5.4]],.61,.14)],
   [path([[0,-4.5,2.2],[0,-5.5,3.2],[0,-6.9,4.3],[0,-7.4,4.5]],.51,.08)],
   [path([[0,-4.5,2.2],[0,-5.0,3.3],[0,-5.3,4.8],[0,-4.5,5.9]],.57,.10)],
   [path([[0,-4.5,2.2],[0,-5.0,3.3],[0,-4.9,5],[0,-4.2,6.2]],.67,.13)]]
 if align=='dark' and primary=='swim':
  young=[path([[0,-4.3,2.1],[0,-5.4,2.9],[0,-5.9,3.8],[0,-5.5,4.4]],.59,.13)]
  return [young,[path([[0,-4.3,2.1],[0,-5.3,3],[0,-5.4,4.3],[0,-4.5,5]],.62,.15)],
   [path([[0,-4.3,2.1],[0,-5.1,3.1],[0,-5.1,4.9],[0,-3.8,5.8]],.69,.12)],
   [stem((0,-8.0,2.6),r=.52)]+horns((3.2,-6.4,3.4),r=.48),
   [path([[0,-4.3,2.1],[0,-5.5,3.3],[0,-5.5,5],[0,-4.8,5.7]],.60,.12)],
   [stem((0,-8.4,.8),r=.65)]+horns((2.8,-7.5,2),r=.60) ]
 if align=='dark' and primary=='fly':
  young=horns((5.0,-5.1,2.0),(4.5,-7.5,1.2),.65,.20)
  young[0]['p'][1]=[2.7,-7.1,.7];young[1]=mirror(young[0])
  normal=horns((5.7,-4.2,2.7),(4.9,-7.4,1.7),.64,.19)
  normal[0]['p'][1]=[2.7,-7.2,.7];normal[1]=mirror(normal[0])
  flight=horns((5.9,-6.4,2.7),(4.8,-8.1,1.7),.57,.13)
  flight[0]['p'][1]=[2.7,-7.4,.7];flight[1]=mirror(flight[0])
  return [young,normal,petals((5.4,-.6,2.0),.64,.20),flight,
   horns((3.2,-6.5,1.7),r=.57,tip=.78),horns((3.1,-10.0,.7),(4.0,-8.2,1.8),.70,.12)]
 if align=='dark' and primary=='run':
  return [quills(5.5,.4,.95,.66),quills(5.5,.8,.9,.63),quills(5.7,1.4,.86,.62),
   quills(5.8,-1.5,1.1,.51),quills(6.3,.75,1.03,.78),
   [stem((0,-8.8,1.8),r=.57)]+quills(5.4,.5,.88,.63)[1:3]]
 if align=='dark' and primary=='power':
  young=[stem((0,-7.15,1.1),r=.8)]
  return [young,[stem((0,-7.6,1.4),(0,-7,.9),.82)],
   [stem((0,-8.0,2.8),(0,-7.3,1.5),.78)], [stem((0,-8.0,-.7),(0,-7.3,.4),.77)],
   [stem((0,-7.0,4.7),(0,-7.5,2.2),.87)], [stem((0,-8.6,3.8),(0,-8.0,2.1),1.02,.16)]]
 raise ValueError((align,primary))

# Per-reference fully aged palettes, author-chosen rather than sampled pixels.
# Order Normal/Swim/Fly/Run/Power; Hero keeps a white body and colored markings.
PALETTES={
'neutral_normal': [('69c5d3','e4ed87'),('50bccc','a8e551'),('53bdd1','aee765'),('60bfd0','b9dd4b'),('63bfc8','b5e649')],
'neutral_swim': [('9acb39','e0d770'),('75bf57','a6d9d1'),('b6ce50','d5a9b1'),('24bfac','b1ee4d'),('b0cf2f','e4df69')],
'neutral_fly': [('d77dce','e3b051'),('e38c83','f0d46c'),('9b6ed5','f3de4f'),('bd7b9d','e8cb68'),('d64380','ee9d2e')],
'neutral_run': [('488ac7','a9d667'),('61c7d1','cce8d5'),('9c91d4','f1e1a0'),('375fc3','e8ddad'),('a95db7','da5aaf')],
'neutral_power': [('e48a38','eda7bc'),('db5947','b864c5'),('e86639','f0cf77'),('dc7846','f4c567'),('d75542','eab867')],
'hero_normal': [('edf1ec','afc6e1'),('edf2ec','7dd7c5'),('f0efea','c4b4d5'),('edf1ed','7aafcf'),('eeefed','aa9ed4')],
'hero_swim': [('edf2ec','8accc1'),('edf2ed','67b1ca'),('edf0e8','a2cc6f'),('edf0ec','549cbd'),('edf0e7','c9bf73')],
'hero_fly': [('eef0ed','b3acd7'),('eef0eb','a4badb'),('eef0ec','a0a0d3'),('eef0eb','bea2d5'),('eef0eb','bca8d7')],
'hero_run': [('edf0e9','a8b2c2'),('eef2e7','87b4b9'),('edf1e8','b6a7d1'),('edf1e8','90b5d6'),('edf0e8','c5afd7')],
'hero_power': [('f1eee4','de9b4f'),('f0ece8','cc8aab'),('f0ede7','b79cce'),('f2eee5','d99e83'),('f0e9e5','d77f88')],
'dark_normal': [('31202d','76528b'),('2d2920','c09a75'),('302035','8f71b0'),('29203b','9476bc'),('30231b','ba8d4d')],
'dark_swim': [('172e2c','5da8b3'),('152f2e','65c6b3'),('1c2236','6f7eb9'),('102e35','55b3be'),('193124','74bf56')],
'dark_fly': [('40213c','b0529a'),('25302b','8baec0'),('302036','914581'),('30243e','a37bc5'),('382632','c56d99')],
'dark_run': [('1b2031','5368ac'),('1f213c','b0a3d0'),('202734','c1bf55'),('211d29','ce474e'),('1e2440','675cb3')],
'dark_power': [('35242b','ad7298'),('20332d','81bfc0'),('2d273d','b194c4'),('263727','caa16c'),('392720','bb734b')],
}

def combine(a,b,t=.5):
 return {'p':[[a['p'][j][i]*(1-t)+b['p'][j][i]*t for i in range(3)] for j in range(4)],'r':a['r']*(1-t)+b['r']*t,'tip':a['tip']*(1-t)+b['tip']*t}
def dormant():return path([[0,-3,.8],[0,-3,.8],[0,-3,.8],[0,-3,.8]],.035,.035)
def padded(paths):
 out=[dormant() for _ in range(6)];occupied=set()
 for p in paths:
  x=p['p'][-1][0]
  slots=[1,3] if x>.05 else [2,4] if x<-.05 else [0,5]
  slot=next((s for s in slots if s not in occupied),None)
  if slot is None:raise AssertionError('Too many branches in anatomical lane')
  out[slot]=p;occupied.add(slot)
 return out
def endpoint_styles(group):
 pure=[padded(x) for x in group[1:]];out=[]
 for sf,rp in AXES:
  a=pure[1 if sf<0 else 2] if sf else None;b=pure[3 if rp<0 else 4] if rp else None
  out.append([combine(x,y) for x,y in zip(a,b)] if a and b else a or b or pure[0])
 return padded(group[0]),out

def sample(p,t):
 a,b,c,d=p['p'];return [(1-t)**3*a[i]+3*(1-t)**2*t*b[i]+3*(1-t)*t*t*c[i]+t**3*d[i] for i in range(3)]
def stroke(p,segment,top,sx):
 a=sample(p,segment/6);b=sample(p,(segment+1)/6);r=p['r']+(p['tip']-p['r'])*(segment+.5)/6
 # The source family may have a slightly different crown height/width. Never
 # move its semantic skull joint or its fixed eye panels.
 lo=[min(a[i],b[i])-r for i in range(3)];hi=[max(a[i],b[i])+r for i in range(3)]
 for v in (lo,hi):v[0]*=sx;v[1]+=top+6
 return base.points(lo,hi)
def vector(v):return [round(float(x),6) for x in v]
def core(data):
 choices=[]
 for b in data['bones']:
  if b['name'].startswith('skull__cube_'):
   for m in b['meshes']:
    vs=m['vertices'];lo=np.min(vs,axis=0);hi=np.max(vs,axis=0);choices.append((float(np.prod(hi-lo)),b,m,lo,hi))
 return max(choices,key=lambda x:x[0])
def scales(align,primary,secondary):
 # head(x,y,z), body(x,y,z), hands, feet(width,length), wings. These are
 # anatomical family changes rather than a universal head enlargement.
 h=[1.,1.,1.];b=[1.015,1.01,1.015];hand=1.;foot=[1.,1.];wing=1.
 if primary=='swim':
  if secondary=='normal':h=[1.02,1.04,1.02]
  if secondary=='swim':h=[1.07,1.02,1.06];foot=[1.14,1.12]
  if secondary=='fly':h=[.95,1.06,.97];b=[.90,1.04,.94];wing=1.20
  if secondary=='run':h=[.97,.99,1.03];b=[.95,.99,.96]
  if secondary=='power':b=[1.25,1.06,1.33];hand=1.13;foot=[1.14,1.13]
 if primary=='fly':
  if secondary=='swim':h=[1.08,.98,1.05];foot=[1.20,1.24]
  if secondary=='fly':wing=1.48
  if secondary=='run':foot=[1.03,1.12];wing=1.12
  if secondary=='power':hand=1.13;b=[1.06,1.02,1.07]
 if primary=='run':
  if secondary=='swim':foot=[1.07,1.36];h=[1.025,1.01,1.05]
  if secondary=='fly':h=[.99,1.035,1.02];wing=1.28
  if secondary=='run':foot=[1.05,1.18];h=[1.025,1.01,1.04]
  if secondary=='power':h=[1.03,1.06,1.025];hand=1.10
 if primary=='power':
  if secondary=='normal':h=[1.035,1.015,1.02];b=[1.06,1.015,1.06]
  if secondary=='swim':b=[1.13,1.025,1.15];foot=[1.18,1.20]
  if secondary=='fly':hand=1.23;wing=1.17
  if secondary=='run':foot=[1.06,1.12]
  if secondary=='power':h=[1.07,1.06,1.06];b=[1.21,1.055,1.22];hand=1.28;foot=[1.18,1.20]
 if primary=='normal' and secondary!='normal':h=[1.005,1.005,1.005]
 if align=='hero':h=[1+(x-1)*.65 for x in h]
 if align=='dark':h=[1+(x-1)*.8 for x in h]
 return [*h,*b,hand,*foot,wing]
def endpoint_scales(align,primary):
 pure=[scales(align,primary,s) for s in ['normal','swim','fly','run','power']];out=[]
 for sf,rp in AXES:
  a=pure[1 if sf<0 else 2] if sf else None;b=pure[3 if rp<0 else 4] if rp else None
  out.append([(x+y)/2 for x,y in zip(a,b)] if a and b else a or b or pure[0])
 return out

def rgb(h):return [int(h[i:i+2],16) for i in [0,2,4]]
def palette(align,primary):
 pairs=PALETTES[align+'_'+primary]
 if align=='neutral' and primary=='swim':pairs=[(b,a) for a,b in pairs]
 pure=[{'body':rgb(a),'accent':rgb(b),'wings':rgb(b)} for a,b in pairs];out=[]
 for sf,rp in AXES:
  a=pure[1 if sf<0 else 2] if sf else None;b=pure[3 if rp<0 else 4] if rp else None
  color={k:[round((x+y)/2) for x,y in zip(a[k],b[k])] for k in a} if a and b else a or b or pure[0]
  out.append(color)
 # Iconic orange/yellow fully grown flight wings remain distinct from skin.
 if align=='neutral' and primary=='fly':
  out[4]['wings']=rgb('e1a55b');out[7]['wings']=rgb('f2d44c')
 if align=='hero' and primary=='fly':out[7]['wings']=rgb('7199d1')
 return out

def author(key):
 donor=base.BASE[key];data=copy.deepcopy(base.source(donor));main,accent,white=base.samples(donor)
 align='hero' if key.startswith('hero') else 'dark' if key.startswith('dark') else 'neutral'
 primary=key.split('_')[-1] if '_'in key else key
 if primary in ('baby','hero','dark'):primary='normal'
 # Preserve the source semantic hierarchy and skin UVs, replacing only the
 # native head ornaments with topology-compatible stepped adaptations.
 native_decor=list(data['head_decorations']);hidden=set()
 for root in native_decor:hidden|=base.descendants(data,root)
 for b in data['bones']:
  if b['name'] in hidden:b['meshes']=[]
 base.marker(data,align,accent);base.lenses(data,white);base.expressions(data,donor)
 _,headbone,head,lo,hi=core(data);top=float(lo[1]+headbone['pivot'][1]);width=float(hi[0]-lo[0]);young,targets=endpoint_styles(styles(align,primary));development=endpoint_scales(align,primary)
 # Native face panels/iris geometry stay fixed; the main head only changes
 # behind its fixed front plane and upward from its fixed lower edge.
 for b in data['bones']:
  n=b['name']
  for m in b['meshes']:
   if n.startswith('expression_') or m.get('material')=='lens':continue
   original=copy.deepcopy(m['vertices']);v=np.asarray(original,dtype=float);vlo=v.min(axis=0);vhi=v.max(axis=0);morph=[]
   for scale in development:
    hs=scale[:3];bs=scale[3:6];hand,fw,fl,wing=scale[6:]
    q=v.copy()
    if m is head:
     q[:,0]*=hs[0];q[:,1]=vhi[1]-(vhi[1]-v[:,1])*hs[1];q[:,2]=vlo[2]+(v[:,2]-vlo[2])*hs[2]
    elif n.startswith('body__'):
     q[:,0]*=bs[0];q[:,2]*=bs[2];q[:,1]=vlo[1]+(v[:,1]-vlo[1])*bs[1]
    elif n.startswith('arm_'):
     if n.endswith('__cube_1'):q[:,0]*=hand;q[:,1]*=1+(hand-1)*.45;q[:,2]*=hand
    elif n.startswith(('foot_','feet_')):
     q[:,0]*=fw;q[:,2]=vhi[2]-(vhi[2]-v[:,2])*fl
    elif n.startswith('wing_'):
     q[:,0]*=wing;q[:,1]*=1+(wing-1)*.45
    elif n.startswith('tail'):q[:,2]*=1.035
    morph.append([vector(x) for x in q])
   m['morph_vertices']=morph
 meshes=[]
 for slot in range(6):
  for segment in range(6):
   youngvs=stroke(young[slot],segment,top,width/7)
   meshes.append({'name':f'family_crest_{slot}_{segment}','material':'official','shading':'flat','vertices':[vector(x) for x in youngvs],
    'morph_vertices':[[vector(x) for x in stroke(t[slot],segment,top,width/7)] for t in targets],
    'faces':[{'v':f,'uv':[list(accent if align=='hero' or segment>=4 and (align=='dark' or primary in ('normal','swim','power')) else main) for _ in f]} for f in SIDES]})
 base.add_bone(data,'gradual_head_021','skull',meshes,decor=True)
 base.monster_crown(data,accent)
 crown=next(b for b in data['bones'] if b['name']=='monster_crown');attached=max(v[1] for m in crown['meshes'] for v in m['vertices'])
 for m in crown['meshes']:
  for v in m['vertices']:v[1]-=attached
 crown['pivot'][1]=attached
 data['source_uv_layout']=donor;data['authored_gameplay_form']=key;data['development_axes']=[list(a) for a in AXES]
 data['evolution_revision']='0.21.0';data['ordinary_axis_range']=[-1,1]
 swatch=None
 if key=='hero_swim':
  # The four aliased source UV layouts are not Hero layouts. Reserve a texel
  # unused by every original face, instead of recoloring an eye-white texel.
  im=base.source_image(donor);used=np.zeros((im.height,im.width),bool)
  for bone in base.source(donor)['bones']:
   for mesh in bone['meshes']:
    for face in mesh['faces']:
     for uvs in [face['uv'],face.get('gameplay_uv',face['uv'])]:
      u=[p[0]*im.width/data['texture_width'] for p in uvs];v=[p[1]*im.height/data['texture_height'] for p in uvs]
      x0=max(0,int(math.floor(min(u))));x1=min(im.width,int(math.ceil(max(u))))
      y0=max(0,int(math.floor(min(v))));y1=min(im.height,int(math.ceil(max(v))))
      used[y0:y1,x0:x1]=True
  candidates=[(x,y) for y in range(im.height-1,-1,-1) for x in range(im.width-1,-1,-1) if not used[y,x] and im.getpixel((x,y))[3]==0]
  assert candidates,'No protected Hero core swatch'
  x,y=candidates[0];uv=[(x+.5)*data['texture_width']/im.width,(y+.5)*data['texture_height']/im.height]
  swatch={'bone':headbone['name'],'uv':uv,'pixel':[x,y],'layout':donor}
  data['natural_hero_core']=swatch

 data['gradual_evolution']={'strength_range':[0,1.2],'vertices':'young adult','morph_vertices':'fully aged at strength1.2','axis_order':'Swim/Fly thenRun/Power','head_family':align+'/'+primary,'reference_page':f'https://chao-island.com/gallery/advanced-evolution/{align}.html'}
 # Donor roots may be empty after replacement but remain valid and safely hidden
 # by hats. The actual head core and all native lenses are never decorations.
 data['head_decorations']=list(dict.fromkeys([*native_decor,*data['head_decorations']]))
 p=MODELS/f'chao_{key}.json';base.write(p,data)
 return {'key':key,'donor':donor,'alignment':align,'primary':primary,'endpoints':palette(align,primary),'source_page':data['gradual_evolution']['reference_page'],'hero_core_swatch':swatch,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()}

def main():
 records=[author(k) for k in PRIMARY]
 base.write(ASSET/'gradual_evolution_palettes_021.json',{'version':'0.21.0','strength_max':1.2,'profiles':{r['key']:r['endpoints'] for r in records},'hero_core_swatch':{r['key']:r['hero_core_swatch'] for r in records if r['hero_core_swatch']}})
 base.write(ASSET/'gradual_evolution_models_021.json',{'version':'0.21.0','primary_forms':15,'young_forms':15,'fully_aged_forms':135,'models':records})
 print('Gradual021:15 restrained young forms +135 family-specific aged endpoints; independent age/axes; native joints/eyes retained.')
if __name__=='__main__':main()
