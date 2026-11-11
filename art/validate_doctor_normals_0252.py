#!/usr/bin/env python3
"""Inspect emitted Doctor geometry with the native loader's flat-normal rule.

This exercises the stored mesh rather than checking generator source strings.
It also rebuilds the actual 0.25.1 side-winding defect as a negative control.
Native client views remain necessary for the final appearance review.
"""
from pathlib import Path
import argparse,copy,hashlib,json,math
from collections import Counter
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/chao_garden'

def sub(a,b):return [x-y for x,y in zip(a,b)]
def dot(a,b):return sum(x*y for x,y in zip(a,b))
def cross(a,b):return [a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]]
def normalized(v):
 length=math.sqrt(dot(v,v))
 if length<1e-9:raise AssertionError('Degenerate moustache face')
 return [x/length for x in v]
def normal(mesh,face):
 # ChaoMeshAsset.load: (vertex1-vertex0) cross (vertex2-vertex0).
 a,b,c=[mesh['vertices'][i] for i in face['v'][:3]]
 return normalized(cross(sub(b,a),sub(c,a)))
def near(a,b):return all(abs(x-y)<1e-8 for x,y in zip(a,b))
def centroid(mesh,face):
 vertices=[mesh['vertices'][i] for i in face['v']]
 return [sum(v[i] for v in vertices)/len(vertices) for i in range(3)]
def reflect(v):return [-v[0],v[1],v[2]]
def surface(mesh):
 vertices=mesh['vertices'];n=len(vertices)//2;shape=vertices[:n]
 area=sum(shape[i][0]*shape[(i+1)%n][1]-shape[(i+1)%n][0]*shape[i][1] for i in range(n))
 edges=Counter((a,b) for face in mesh['faces'] for a,b in zip(face['v'],face['v'][1:]+face['v'][:1]))
 coherent=all(count==1 and edges[(b,a)]==1 for (a,b),count in edges.items())
 normals=[normal(mesh,face) for face in mesh['faces']]
 outward=[]
 for i in range(n):
  dx,dy,_=sub(shape[(i+1)%n],shape[i]);sign=1 if area>0 else -1
  expected=normalized([sign*dy,-sign*dx,0])
  outward.append(dot(normals[i+2],expected)>1-1e-8)
 volume=0
 for face in mesh['faces']:
  v=[vertices[i] for i in face['v']]
  for i in range(1,len(v)-1):volume+=dot(v[0],cross(v[i],v[i+1]))/6
 return {'coherent_directed_edges':coherent,'caps_outward':near(normals[0],[0,0,-1]) and near(normals[1],[0,0,1]),
         'perimeter_outward':all(outward),'positive_signed_volume':volume>0,
         'signed_volume':volume,'normals':normals,'perimeter_faces':len(outward),'outward_perimeter_faces':sum(outward)}

def validate_doctor_normals(model,atlas):
 parts=next(b['meshes'] for b in model['bones'] if b['name']=='staff_doctor_moustache')
 lobes={int(m['name'].rsplit('_',1)[-1]):m for m in parts if m['name'].startswith('gray_moustache_lobe_')}
 if set(lobes)!={-1,1}:raise AssertionError('Expected two Doctor moustache lobes')
 surfaces={str(sign):surface(mesh) for sign,mesh in lobes.items()}
 checks={}
 for sign,data in surfaces.items():
  for key in ('coherent_directed_edges','caps_outward','perimeter_outward','positive_signed_volume'):
   checks[f'lobe_{sign}_{key}']=data[key]
 left,right=lobes[-1],lobes[1]
 checks['reflected_vertices']=len(left['vertices'])==len(right['vertices']) and all(near(a,reflect(b)) for a,b in zip(left['vertices'],right['vertices']))
 reflected_normals=[];lit_samples=[];material_samples=[]
 # Reflected light directions must yield the same Lambert term. Overhead and
 # oblique lights expose a flipped top normal that a front view can conceal.
 lights=[[0,-1,0],[0,-1,-1],[-1,-2,-1],[1,-2,-1],[0,1,0],[0,0,-1]]
 for lf in left['faces']:
  match=[rf for rf in right['faces'] if near(centroid(left,lf),reflect(centroid(right,rf)))]
  if len(match)!=1:raise AssertionError('Ambiguous mirrored moustache face')
  rf=match[0];ln,rn=normal(left,lf),normal(right,rf)
  reflected_normals.append(near(ln,reflect(rn)))
  lit_samples.extend(abs(max(0,dot(ln,normalized(light)))-max(0,dot(rn,normalized(reflect(light)))))<1e-8 for light in lights)
  for mesh,face in ((left,lf),(right,rf)):
   for u,v in face['uv']:
    # JSON UV dimensions are half the actual 256x128 staff texture.
    material_samples.append(atlas.getpixel((int(u*2),int(v*2))))
 checks['reflected_face_normals']=all(reflected_normals)
 checks['mirrored_directional_light_response']=all(lit_samples)
 checks['both_lobes_uniform_gray_hair']=all(pixel==(170,186,190,255) for pixel in material_samples)
 # Recreate the exact pre-fix defect: only the negative-x perimeter keeps the
 # unreflected order. The checks must reject it for normals and closed edges.
 broken=copy.deepcopy(left)
 for face in broken['faces'][2:]:
  face['v'].reverse()
 old=surface(broken)
 checks['rejects_0251_reflected_side_winding']=not old['coherent_directed_edges'] and not old['perimeter_outward']
 return {'checks':checks,'lobe_surfaces':surfaces,'mirrored_face_pairs':len(reflected_normals),
         'directional_light_samples':len(lit_samples),'gray_material_samples':len(material_samples),
         'negative_control':{key:old[key] for key in ('coherent_directed_edges','perimeter_outward','outward_perimeter_faces','perimeter_faces')},
         'scope':'Stored flat-normal geometry and atlas verification; no native render claim.'}

def main():
 parser=argparse.ArgumentParser();parser.add_argument('--report',type=Path);args=parser.parse_args()
 path=A/'models/entity/staff025/doctor.json';model=json.loads(path.read_text())
 with Image.open(A/'textures/entity/chao/staff025.png') as atlas:report=validate_doctor_normals(model,atlas.convert('RGBA'))
 report['doctor_mesh_sha256']=hashlib.sha256(path.read_bytes()).hexdigest()
 historical=copy.deepcopy(model)
 lobe=next(m for b in historical['bones'] for m in b.get('meshes',[]) if m.get('name')=='gray_moustache_lobe_-1')
 for face in lobe['faces'][2:]:face['v'].reverse()
 report['historical_mesh_reconstructed_sha256']=hashlib.sha256((json.dumps(historical,separators=(',',':'))+'\n').encode()).hexdigest()
 for label,ok in report['checks'].items():
  if not ok:raise AssertionError(label)
 if args.report:
  args.report.parent.mkdir(parents=True,exist_ok=True);args.report.write_text(json.dumps(report,indent=2)+'\n')
 print(f"Doctor moustache: {len(report['checks'])} geometry/material assertions, {report['mirrored_face_pairs']} mirrored face pairs, {report['directional_light_samples']} light samples; historical defect rejected.")
if __name__=='__main__':main()
