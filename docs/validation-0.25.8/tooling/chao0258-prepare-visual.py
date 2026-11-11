#!/usr/bin/env python3
"""Create only the tagged disposable0258 world, baking current eight dimension stems.
No fixture resource is added to the ordinary client/main resource path.
"""
from pathlib import Path
import argparse,gzip,hashlib,json,os,re,shutil,struct,time
from chao_artifact_io import write_json_atomic
p=argparse.ArgumentParser();p.add_argument('root',type=Path);p.add_argument('--reset',action='store_true');p.add_argument('--run-dir',type=Path);p.add_argument('--inspect-template',action='store_true');p.add_argument('--template',required=True,type=Path);a=p.parse_args();root=a.root.resolve()
template=a.template.resolve();assert template.is_file(), 'Pass the fresh0258 native test world level.dat as --template'
def sha(f):
 with f.open('rb') as h:return hashlib.file_digest(h,'sha256').hexdigest()
class Reader:
 def __init__(self,b):self.b=b;self.i=0
 def take(self,n):v=self.b[self.i:self.i+n];self.i+=n;assert len(v)==n;return v
 def num(self,f):return struct.unpack('>'+f,self.take(struct.calcsize('>'+f)))[0]
 def string(self):return self.take(self.num('H')).decode('utf-8')
 def payload(self,t):
  if t in (1,2,3,4,5,6):return self.num({1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}[t])
  if t==7:return self.take(self.num('i'))
  if t==8:return self.string()
  if t==9:
   item=self.num('B');return (item,[self.payload(item) for _ in range(self.num('i'))])
  if t==10:
   result={}
   while (kind:=self.num('B'))!=0:
    name=self.string();result[name]=(kind,self.payload(kind))
   return result
  if t in (11,12):return [self.num('i' if t==11 else 'q') for _ in range(self.num('i'))]
  raise ValueError(t)
def string(v):b=v.encode();return struct.pack('>H',len(b))+b
def encode(t,v):
 if t in (1,2,3,4,5,6):return struct.pack('>'+{1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}[t],v)
 if t==7:return struct.pack('>i',len(v))+v
 if t==8:return string(v)
 if t==9:
  k,rows=v;return struct.pack('>Bi',k,len(rows))+b''.join(encode(k,x) for x in rows)
 if t==10:return b''.join(bytes([k])+string(n)+encode(k,x) for n,(k,x) in v.items())+b'\0'
 if t in (11,12):return struct.pack('>i',len(v))+b''.join(struct.pack('>i' if t==11 else '>q',x) for x in v)
 raise ValueError(t)
def from_json(x):
 if isinstance(x,bool):return (1,int(x))
 if isinstance(x,int):return (3,x)
 if isinstance(x,float):return (6,x)
 if isinstance(x,str):return (8,x)
 if isinstance(x,dict):return (10,{k:from_json(v) for k,v in x.items()})
 if isinstance(x,list):
  rows=[from_json(v) for v in x];k=rows[0][0] if rows else 0;assert all(t==k for t,v in rows);return(9,(k,[v for t,v in rows]))
 raise ValueError(type(x))
raw=gzip.decompress(template.read_bytes());r=Reader(raw);kind=r.num('B');name=r.string();tree=r.payload(kind);assert r.i==len(raw) and kind==10 and tree['Data'][0]==10
assert bytes([kind])+string(name)+encode(kind,tree)==raw,'Synthetic NBT codec must round-trip every template byte'
data=tree['Data'][1];settings=data['WorldGenSettings'][1]
if a.inspect_template:
 print(json.dumps(dict(level_name=data['LevelName'][1],dimensions=sorted(settings['dimensions'][1]),spawn={k:data[k][1] for k in ['SpawnX','SpawnY','SpawnZ'] if k in data},template_sha256=sha(template))));raise SystemExit()
fixture=root/'src/gameTestDimensions/resources/data/minecraft/worldgen/world_preset/flat.json';stems=json.loads(fixture.read_text())['dimensions']
expected={'minecraft:overworld','minecraft:the_nether','minecraft:the_end','chao_garden:hub','chao_garden:garden','chao_garden:kindergarten','chao_garden:hero_garden','chao_garden:dark_garden'};assert set(stems)==expected
for room in ['hub','garden','kindergarten','hero_garden','dark_garden']:
 current=json.loads((root/f'src/main/resources/data/chao_garden/dimension/{room}.json').read_text());assert stems['chao_garden:'+room]==current
settings['dimensions']=from_json(stems);data['LevelName']=(8,'ChaoVisual');data['GameType']=(3,0);data['SpawnX']=(3,0);data['SpawnY']=(3,-59);data['SpawnZ']=(3,0);data['Time']=(4,0);data['DayTime']=(4,6000)
# No Player or old generator/entity/region state is copied into this new world.
data.pop('Player',None)
# Exercise authored-realm spawn isolation under normal ordinary-world spawning.
assert data['GameRules'][0]==10
data['GameRules'][1]['doMobSpawning']=(8,'true')
run=(a.run_dir or Path('/workspace/tools/chao0258-smoke/run-1')).resolve();assert run.parent==Path('/workspace/tools/chao0258-smoke') and re.fullmatch(r'run-[0-9]+',run.name), 'Only a tagged external0258 synthetic run directory is allowed'
world=run/'saves/ChaoVisual';marker=run/'chao-visual-fixture0258.json'
if world.exists():
 assert a.reset and marker.is_file(),'Existing save is not authorized as this tagged disposable fixture'
 previous=json.loads(marker.read_text());assert previous.get('disposable_synthetic_fixture') is True and previous.get('world')==str(world)
 shutil.rmtree(world)
world.mkdir(parents=True)
encoded=gzip.compress(bytes([kind])+string(name)+encode(kind,tree),mtime=0)
tmp=world/'level.dat.tmp'
with tmp.open('wb') as f:f.write(encoded);f.flush();os.fsync(f.fileno())
os.replace(tmp,world/'level.dat')
options='''version:3955
onboardAccessibility:false
autoJump:false
enableVsync:false
maxFps:30
renderDistance:12
simulationDistance:8
guiScale:2
bobView:false
fovEffectScale:0.0
screenEffectScale:0.0
soundCategory_master:0.3
soundCategory_music:0.4
pauseOnLostFocus:false
tutorialStep:none
fullscreen:false
overrideWidth:1280
overrideHeight:720
'''
(run/'options.txt').write_text(options)
report=dict(release='0.25.8',result='READY',disposable_synthetic_fixture=True,world=str(world),created_ns=time.time_ns(),template_sha256=sha(template),current_game_test_stem_fixture_sha256=sha(fixture),level_dat_sha256=sha(world/'level.dat'),dimension_keys=sorted(stems),ordinary_world_mob_spawning=True,custom_biomes={k:v['generator']['settings']['biome'] for k,v in stems.items() if k.startswith('chao_garden:')},method='Only synthetic bootstrap level.dat structure reused. Current eight stems baked into the disposable save NBT; no old entities/regions/player state copied. Ordinary client/main resource preset remains untouched and must independently resolve vanilla3. Normal mob spawning explicitly enabled in the disposable world; source code must isolate authored realms without changing this shared rule. Synthetic options only.')
write_json_atomic(marker,report);write_json_atomic(run/'visual-fixture.json',report);print(json.dumps(report,indent=2))
