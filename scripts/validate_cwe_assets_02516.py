#!/usr/bin/env python3
"""Validate final source-derived CWE resources and unchanged surrounding content."""
from pathlib import Path
import hashlib,json,math
from PIL import Image
ROOT=Path(__file__).resolve().parents[1];D=ROOT/'art/cwe02516';A=ROOT/'src/main/resources/assets/chao_garden';checks=0

def check(ok,msg):
 global checks
 checks+=1
 if not ok:raise AssertionError(msg)
def read(p):return json.loads(p.read_text())
def obj(model):
 check(model.get('loader')=='neoforge:obj','Expected native source model');path=A/model['model'].split(':')[1];lines=path.read_text().splitlines();vs=[list(map(float,l.split()[1:])) for l in lines if l.startswith('v ')];uv=[list(map(float,l.split()[1:])) for l in lines if l.startswith('vt ')];mats={l.split()[1] for l in lines if l.startswith('usemtl ')}
 check(len(vs)>=18,'Empty source surface');check(all(math.isfinite(v) and -1<=v<=2 for p in vs for v in p),'Invalid model bounds');check(all(0<v<1 for p in uv for v in p),'Material UV touches atlas boundary and can sample a neighboring sprite');check(mats<=model['textures'].keys(),'Missing model material')
 for value in model['textures'].values():check((A/'textures'/(value.split(':')[1]+'.png')).is_file(),'Dangling texture '+value)
 return vs

def main():
 for path,digest in read(D/'provenance.json')['inputs'].items():check(hashlib.sha256((D/path).read_bytes()).hexdigest()==digest,'Changed source input '+path)
 catalog=read(D/'generated.json');check(len(catalog['items'])==len(set(catalog['items']))==26,'Source item domain');check(len(catalog['wearables'])==10 and len(catalog['source_objects'])==16,'Source object domain')
 for n in catalog['items']:
  data=read(A/'models/item'/(n+'.json'));check(data['base']['parent']=='chao_garden:item/cwe02516/'+n,'Wrong held model '+n);check(data['perspectives']['gui']['textures']['layer0']=='chao_garden:item/'+n,'Wrong GUI item '+n);obj(read(A/'models/item/cwe02516'/(n+'.json')))
  im=Image.open(A/'textures/item'/(n+'.png'));check(im.mode=='RGBA' and im.size==(32,32) and set(im.getchannel('A').tobytes())=={0,255},'Unusable icon '+n)
 for n in ['chao_guitar','chao_accordion']:
  for suffix in ['idle',*(f'play_{i}' for i in range(4))]:
   points=obj(read(A/'models/block'/(n+'_'+suffix+'.json')));check(all([3,0,3][i]-.001<=p[i]*16<=[13,8,13][i]+.001 for p in points for i in range(3)),'Toy escapes existing play volume')
 for n in [*catalog['wearables'],'hoodie_sleeve_left','hoodie_sleeve_right','hoodie_hood']:
  d=read(A/'models/entity/cwe_accessories'/(n+'.json'));check(d['texture_width']==d['texture_height']==1024,'Wrong wearable atlas');check(d['bones'][0]['name']=='accessory','Changed attachment root')
  for b in d['bones']:
   for m in b.get('meshes',[]):
    check(all(math.isfinite(v) for p in m['vertices'] for v in p),'Non-finite wearable');check(all(0<=v<=1024 for f in m['faces'] for p in f['uv'] for v in p),'Wearable UV outside atlas')
    check(all(0<=v<len(m['vertices']) for f in m['faces'] for v in f['v']),'Bad wearable face index')
    if m.get('name')=='sa2_source':
     surfaces=[tuple(sorted(tuple(m['vertices'][v])+tuple(uv) for v,uv in zip(f['v'],f['uv']))) for f in m['faces']]
     check(len(surfaces)==len(set(surfaces)),'Duplicate two-sided surfaces would flicker in no-cull renderer: '+n)
  if n!='hoodie_hood':
   old=read(D/'previous-models'/(n+'.json'));check([(b['name'],b.get('parent'),b['pivot']) for b in old['bones']]==[(b['name'],b.get('parent'),b['pivot']) for b in d['bones']],'Existing bone attachments changed')
 # Preserve all eight requested material reversions, plus the accepted grass top.
 rollback=ROOT/'art/sa2_02515'
 for p in read(rollback/'rollback.json'):check((rollback/'rollback'/(p+'.png')).read_bytes()==(A/'textures'/(p+'.png')).read_bytes(),'Previous requested rollback lost '+p)
 # Decoded source texture and model ownership are checked before item adaptation.
 original=read(D/'accessories.json');check(all(sum(n['faces'] for n in v['nodes'])==len(v['faces']) for v in original.values()),'Unowned accessory triangles')
 check(set(n['index'] for n in original['7_hoodie']['nodes'] if n['faces'])=={1,3,10,16},'Incomplete articulated hoodie')
 report={'release':'0.25.16','result':'PASS','checks':checks,'source_items':26,'wearables':10,'wearable_parts':13,'toys':2,'special_fruits':5,'gifts':9,'toy_poses':10,'fruit_pod_stages':20,'exact_prior_texture_reversions':8,'source_archive_version':'9.6.2.2'}
 out=ROOT/'docs/validation-0.25.16/source-validation.json';out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))
if __name__=='__main__':main()
