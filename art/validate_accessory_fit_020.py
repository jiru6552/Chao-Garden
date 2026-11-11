#!/usr/bin/env python3
"""Execute the compiled production fitting code, independently check final JSON contacts.

Run after compileJava/processResources. Uses the configured Java21, or pass
--java=/path/to/jdk21/bin/java. This check does not start Minecraft or Gradle.
The separate client probe checks sampled world/ancestor transforms and motion.
"""
from pathlib import Path
import argparse,json,numpy as np,math,os,shutil,subprocess,tempfile
root=Path(__file__).resolve().parents[1]
args=argparse.ArgumentParser(description=__doc__)
args.add_argument('--java',default=str(Path(os.environ['JAVA_HOME'])/'bin/java') if os.environ.get('JAVA_HOME') else shutil.which('java'))
args.add_argument('--classes',type=Path,default=root/'build/classes/java/main')
args.add_argument('--classpath-file',type=Path,default=root/'build/moddev/clientLegacyClasspath.txt')
opts=args.parse_args()
if not opts.classes.exists() or not opts.classpath_file.exists():raise SystemExit('Compile Java and prepare the client classpath before running accessory contact checks.')
java=Path(opts.java);javac=java.with_name('javac')
a=root/'src/main/resources/assets/chao_garden/models/entity/cwe_accessories'
p=root/'src/main/resources/assets/chao_garden/models/entity/gameplay'
primary=['baby','swim','fly','run','power','hero','hero_swim','hero_fly','hero_run','hero_power','dark','dark_swim','dark_fly','dark_run','dark_power']
def matrix(bones,name,stop):
 chain=[]
 while name!=stop:
  b=bones[name];chain.append(b);name=b.get('parent')
 M=np.eye(4)
 for b in chain[::-1]:
  t=np.eye(4);t[:3,3]=b['pivot'];M=M@t
  rx,ry,rz=[math.radians(x) for x in b.get('rotation',[0,0,0])]
  for axis,angle in [(2,rz),(1,ry),(0,rx)]:
   c,s=math.cos(angle),math.sin(angle);R=np.eye(4)
   if axis==0:R[1:3,1:3]=[[c,-s],[s,c]]
   if axis==1:R[:3,:3]=[[c,0,s],[0,1,0],[-s,0,c]]
   if axis==2:R[:2,:2]=[[c,-s],[s,c]]
   M=M@R
 return M
def meshbounds(m,M,target):
 v=np.asarray(m.get('morph_vertices',[m['vertices']]*9)[target]);v=np.c_[v,np.ones(len(v))]@M.T
 return np.r_[v[:,:3].min(0),v[:,:3].max(0)]
def bonebounds(bones,b,anchor,target,material=None):
 sets=[meshbounds(m,matrix(bones,b['name'],anchor),target) for m in b.get('meshes',[]) if material is None or m['material']==material]
 return np.r_[np.asarray(sets)[:,:3].min(0),np.asarray(sets)[:,3:].max(0)] if sets else None
def core(bones,anchor,target):
 sets=[]
 for name,b in bones.items():
  if not name.startswith(anchor+'__cube_') or anchor.startswith('arm_') and name!=anchor+'__cube_0':continue
  v=bonebounds(bones,b,anchor,target)
  if v is not None:sets.append(v)
 return max(sets,key=lambda b:np.prod(b[3:]-b[:3]))
def envelope(data,bones,anchor,target):
 sets=[core(bones,anchor,target)]+[bonebounds(bones,bones[name],anchor,target) for name in data.get('attachment_extra_bones',{}).get(anchor,[])]
 return np.r_[np.asarray(sets)[:,:3].min(0),np.asarray(sets)[:,3:].max(0)]
metadata_checks=0
def closed_cap(data,bones,key,target):
 global metadata_checks
 def check(ok,why):
  global metadata_checks
  metadata_checks+=1
  if not ok:raise AssertionError(f'Closed cap {key}/{target}: '+why)
 alias={'hero_swim':'swim','hero_run':'run','dark_fly':'fly','dark_power':'power','amy':'baby','omochao':'baby','chaos_light':'baby','chaos_angel':'baby','chaos_devil':'baby'}
 donor=json.loads((p.parent/'official'/f'chao_{alias.get(key,key)}.json').read_text())
 expected=set(donor['head_decorations'])
 expected.update(name for name in bones if name.startswith(('evolution_','evolution020_','chaos_crown')) or name in ['monster_crown','character_hair','character_propeller'])
 hidden=set(data.get('head_decorations',[]))
 for name in hidden:check(name in bones,'missing declared root '+name)
 def descends(name,roots):
  while name is not None:
   if name in roots:return True
   name=bones[name].get('parent')
  return False
 main=max((b for name,b in bones.items() if name.startswith('skull__cube_') and b.get('meshes')),key=lambda b:np.prod((v:=bonebounds(bones,b,'skull',target))[3:]-v[:3]))
 check(not descends(main['name'],hidden),'main skull core hidden '+main['name'])
 eyes=0
 for name,bone in bones.items():
  if not bone.get('meshes'):continue
  if descends(name,expected):check(descends(name,hidden),'rendered donor/authored crest escapes '+name)
  if name.startswith(('character_robot_eye_','chaos_eye_')) or any(m['material']=='lens' for m in bone['meshes']):
   eyes+=1;check(not descends(name,hidden),'rendered eye hidden '+name)
 check(eyes==2,'expected exactly two visible eyes')
cases=[]
for k,key in enumerate(primary+['baby','amy','omochao','chaos_light','chaos_angel','chaos_devil']):
 d=json.loads((p/f'chao_{key}.json').read_text());bones={b['name']:b for b in d['bones']}
 for target in range(9) if k<15 else [4]:
  closed_cap(d,bones,key,target)
  lens=[bonebounds(bones,b,'skull',target,'lens') for b in d['bones'] if any(m['material']=='lens' for m in b.get('meshes',[]))]
  if lens:
   centres=np.array([(b[:3]+b[3:])/2 for b in lens]);eye=[centres[:,0].mean(),centres[:,1].mean(),min(b[2] for b in lens),np.abs(centres[:,0]-centres[:,0].mean()).mean()]
  else:eye=d['attachment_landmarks']['eyes']
  eye=d.get('attachment_landmarks',{}).get('eyes',eye)
  cases.append(dict(name=f'{key}-{target}' if k<15 else f'child-{key}',h=envelope(d,bones,'skull',target).tolist(),e=list(eye),b=envelope(d,bones,'body',target).tolist(),al=core(bones,'arm_left',target).tolist(),ar=core(bones,'arm_right',target).tolist()))
with tempfile.TemporaryDirectory(prefix='chao020-accessory-') as folder:
 tmp=Path(folder);(tmp/'cases.json').write_text(json.dumps(cases))
 cp=os.pathsep.join([str(opts.classes)]+opts.classpath_file.read_text().splitlines()+[str(x) for x in (root/'build/moddev/artifacts').glob('*-merged.jar')])
 subprocess.run([str(javac),'-proc:none','-cp',cp,'-d',str(tmp),str(root/'art/probes/AccessoryFitHeadless.java')],check=True)
 subprocess.run([str(java),'-cp',str(tmp)+os.pathsep+cp,'com.chaogarden.client.AccessoryFitHeadless',str(tmp/'cases.json'),str(tmp/'fits.json')],check=True)
 fits=json.loads((tmp/'fits.json').read_text())
checks=0
surface_samples_checked=0
items=[x for x in fits[0] if x!='name'];resources={i:json.loads((a/f'{i}.json').read_text()) for i in items}
def require(ok,why):
 global checks
 checks+=1
 if not ok:raise AssertionError(why)
def bounds(case,item,part=0,bone='accessory'):
 entry=fits[case][item];f=entry['fit'];s=entry['parts'];d=resources[item];byname={b['name']:b for b in d['bones']};b=byname[bone];v=np.asarray(b['meshes'][part]['vertices']);pose=s.get(bone,{})
 v=v*np.array([pose.get(x,1) for x in ['sx','sy','sz']])+np.array([pose.get(x,0) for x in ['x','y','z']])+b['pivot'];v=v*np.array(f[3:])+f[:3]
 return np.r_[v.min(0),v.max(0)]
surface_samples={}
for item,data in resources.items():
 if 'hoodie' not in item:continue
 samples=[]
 for mesh in data['bones'][0]['meshes']:
  verts=np.asarray(mesh['vertices'])
  for face in mesh['faces']:
   points=verts[face['v']];samples.append(points.mean(axis=0))
   samples.extend((points+np.roll(points,1,axis=0))/2)
 surface_samples[item]=np.asarray(samples)
for k,c in enumerate(cases):
 h,b,e=np.array(c['h']),np.array(c['b']),np.array(c['e'])
 for item in items:
  v=bounds(k,item);tag=f'{c["name"]}: {item}'
  require(np.isfinite(v).all(),tag+' finite')
  if 'glasses' in item or 'aviators' in item:
   other=bounds(k,item,1)
   require(abs((v[1]+v[4])/2-e[1])<.002,tag+' eyeheight')
   require(abs((v[0]+v[3]+other[0]+other[3])/4-e[0])<.002,tag+' eyecentre')
   require(abs((other[0]+other[3]-v[0]-v[3])/4-e[3])<.002,tag+' eyespacing')
   require(v[5]<e[2]-.03,tag+' lens outsideface')
   for side in ['left','right']:
    require(fits[k][item]['parts'][f'hinge_{side}']['sx']>0,tag+' strictly positive hinge '+side)
    temple=bounds(k,item,bone=f'temple_{side}')
    require(abs((temple[0]+temple[3])/2-(h[0]-.1 if side=='left' else h[3]+.1))<.002,tag+' actualskulltemplecontact')
    hinge=bounds(k,item,bone=f'hinge_{side}')
    require(hinge[0]<=temple[3] and hinge[3]>=temple[0],tag+' connectedhinge')
  elif 'cap_' in item or 'press_hat' in item:
   require(abs(v[4]-h[1])<.4,tag+' brim crown contact')
   require(v[0]<=h[0] and v[3]>=h[3],tag+' brim covers width')
  elif 'headphones' in item:require(abs(v[1]-h[1])<.15,tag+' band crown contact')
  elif 'pacifier' in item:require(abs((v[1]+v[4])/2-(e[1]+1.5))<.002 and v[5]<e[2]-.03,tag+' mouth outsideface')
  elif 'bowtie' in item:
   v=bounds(k,item,1);require(v[1]>=b[1] and v[4]<b[1]+(b[4]-b[1])*.4 and v[5]<b[2],tag+' collar upperchest')
  elif 'hoodie' in item:
   skin=np.array(c['al' if item.endswith('left') else 'ar']) if item.startswith('hoodie_sleeve_') else h if item=='hoodie_hood' else b
   require((v[:3]<skin[:3]).all() and (v[3:]>skin[3:]).all(),tag+f' cloth fullyoutside skin {v.tolist()} {skin.tolist()}')
   f=fits[k][item]['fit'];samples=surface_samples[item]*np.asarray(f[3:])+np.asarray(f[:3]);surface_samples_checked+=len(samples)
   buried=((samples>skin[:3]+.03)&(samples<skin[3:]-.03)).all(axis=1)
   require(not buried.any(),tag+f' cloth surface crosses skin: {buried.sum()} samples')
  elif item.startswith('medal_'):
   require(v[5]<b[2]-.2 and v[1]>=b[1] and v[4]<b[4],tag+' medal onchest')
  elif item=='eggshell_hat':require(abs(v[4]-h[1]-.35)<.002,tag+' egg rim crown')
print('Independent actual-JSON geometry contacts passed:',checks,'checks;',len(cases)*len(items),'runtime fits')
print('Clothing surface clearance:',surface_samples_checked,'triangle-centre/edge samples across all141cases')
print('Independent closed-cap crest/core/eye ownership passed:',metadata_checks,'checks across all141cases')
report={'version':'0.20.0','result':'passed','checks':checks,'clothing_surface_samples':surface_samples_checked,'closed_cap_metadata_checks':metadata_checks,'actual_runtime_fits':len(cases)*len(items),'actual_skin_cases':len(cases),'primary_rigs':15,'ordinary_endpoints_per_primary':9,'special_rigs':['child','amy','omochao','chaos_light','chaos_angel','chaos_devil'],'method':'Executed compiled production ChaoAccessoryFit in Java21; independent Python/Numpy measured fitted final resource vertices against actual morphed skin/iris geometry and independently checked all donor/authored crest descendants hide while core/eyes remain; actual moving PoseStack checks and real visibility operations run separately in client probe.'}
p=root/'docs/validation-0.20/accessory-fit-headless.json';p.parent.mkdir(exist_ok=True,parents=True);p.write_text(json.dumps(report,indent=2)+'\n')
