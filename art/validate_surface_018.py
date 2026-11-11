#!/usr/bin/env python3
"""Check reflection continuity across actual atlas seams and accessory joins.

The material field must depend on shared geometry, never on texture islands.
The accessory check uses convex-polyhedron intersections, so a bounding-box
overlap alone cannot disguise a gap beside a round spectacle lens.
"""
from pathlib import Path
from collections import defaultdict
import itertools
import json
import math
import numpy as np

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/chao_garden'


def matrix(bone):
    x,y,z=np.radians(bone.get('rotation',[0,0,0]))
    rx=np.array([[1,0,0,0],[0,math.cos(x),-math.sin(x),0],[0,math.sin(x),math.cos(x),0],[0,0,0,1.]])
    ry=np.array([[math.cos(y),0,math.sin(y),0],[0,1,0,0],[-math.sin(y),0,math.cos(y),0],[0,0,0,1.]])
    rz=np.array([[math.cos(z),-math.sin(z),0,0],[math.sin(z),math.cos(z),0,0],[0,0,1,0],[0,0,0,1.]])
    t=np.eye(4);t[:3,3]=bone['pivot']
    return t@rz@ry@rx


def mesh_edges(path):
    data=json.loads(path.read_text());bones={b['name']:b for b in data['bones']};world={}
    def transform(key):
        if key not in world:
            b=bones[key];world[key]=(transform(b['parent']) if b.get('parent') else np.eye(4))@matrix(b)
        return world[key]
    edges=defaultdict(list)
    for bone in bones.values():
        t=transform(bone['name'])
        for mesh in bone.get('meshes',[]):
            vertices=[(t@np.array([*v,1.]))[:3]/16 for v in mesh['vertices']]
            for face in mesh['faces']:
                for a in range(len(face['v'])):
                    b=(a+1)%len(face['v']);va=vertices[face['v'][a]];vb=vertices[face['v'][b]]
                    if np.linalg.norm(va-vb)<1e-7:continue
                    uva=np.array(face['uv'][a])/[data['texture_width'],data['texture_height']]
                    uvb=np.array(face['uv'][b])/[data['texture_width'],data['texture_height']]
                    pa=tuple(np.round(va,6));pb=tuple(np.round(vb,6))
                    if pa>pb:pa,pb=pb,pa;va,vb=vb,va;uva,uvb=uvb,uva
                    edges[(pa,pb)].append((va,vb,uva,uvb))
    return edges


def continuity():
    shader=(ASSETS/'shaders/core/chao_reflection.vsh').read_text()
    expression=shader.split('vec2 surfaceProjection =',1)[1].split(';',1)[0]
    assert 'UV0' not in expression and 'viewPosition' in expression
    assert 'vec4(surfaceProjection' in shader and 'sourceUv = UV0' in shader
    paths=[ASSETS/'models/entity/egg_mesh.json',*sorted((ASSETS/'models/entity/official').glob('chao_*.json'))]
    # These include nonuniform viewport views, to exercise both front and
    # steep oblique camera angles independently of the source UV mapping.
    views=[]
    for yaw,pitch in [(0,0),(37,25),(90,-15),(180,60),(271,-47)]:
        views.append(matrix({'pivot':[1.2,-.4,-3.7],'rotation':[pitch,yaw,11]}))
    pairs=points=old_discontinuous=0;max_error=0.;per_mesh={}
    for path in paths:
        n=0
        for values in mesh_edges(path).values():
            if len(values)<2:continue
            first=values[0]
            for other in values[1:]:
                if max(np.linalg.norm(first[i]-other[i]) for i in [2,3])<1e-6:continue
                pairs+=1;n+=1
                for f in [0,.25,.5,.75,1]:
                    a=first[0]*(1-f)+first[1]*f;b=other[0]*(1-f)+other[1]*f
                    old_a=first[2]*(1-f)+first[3]*f;old_b=other[2]*(1-f)+other[3]*f
                    if np.linalg.norm(old_a-old_b)>1e-4:old_discontinuous+=1
                    for view in views:
                        p=(view@np.array([*a,1]))[:3];q=(view@np.array([*b,1]))[:3]
                        uvp=p[:2]*.25+p[2]*np.array([.06,.035])
                        uvq=q[:2]*.25+q[2]*np.array([.06,.035])
                        error=float(np.max(np.abs(uvp-uvq)));max_error=max(max_error,error);points+=1
                        assert error<1e-6,(path,error)
        per_mesh[path.stem]=n
    assert len(paths)==21 and per_mesh['egg_mesh']>0 and old_discontinuous>500
    return dict(models=len(paths),shared_geometric_edges_with_different_uv=pairs,
                projected_edge_samples=points,old_atlas_uv_discontinuous_samples=old_discontinuous,
                maximum_new_projection_difference=max_error,per_mesh=per_mesh)


def axes_and_edges(mesh):
    v=np.array(mesh['vertices']);axes=[];edges=[]
    for f in mesh['faces']:
        ids=f['v'];n=np.cross(v[ids[1]]-v[ids[0]],v[ids[2]]-v[ids[0]])
        if np.linalg.norm(n)>1e-7:axes.append(n/np.linalg.norm(n))
        for a,b in zip(ids,ids[1:]+ids[:1]):
            e=v[a]-v[b]
            if np.linalg.norm(e)>1e-7:edges.append(e/np.linalg.norm(e))
    return v,axes,edges


def intersects(a,b):
    va,aa,ea=axes_and_edges(a);vb,ab,eb=axes_and_edges(b)
    axes=aa+ab
    for u in ea:
        for v in eb:
            axis=np.cross(u,v)
            if np.linalg.norm(axis)>1e-7:axes.append(axis/np.linalg.norm(axis))
    return all(not (max(va@axis)<min(vb@axis)-1e-6 or max(vb@axis)<min(va@axis)-1e-6) for axis in axes)


def joins():
    path=ASSETS/'models/entity/market_accessories.json';data=json.loads(path.read_text())
    meshes=next(b for b in data['bones'] if b['name']=='merchant_accessories')['meshes']
    groups={
      'spectacles':[m for m in meshes if m['name'] in ('round_lens','frame_corner','temple','temple_hook','nose_bridge')],
      'mask':[m for m in meshes if m['name'].startswith(('face_mask','mask_'))]}
    result={}
    for name,parts in groups.items():
        adjacency={i:set() for i in range(len(parts))};count=0
        for a,b in itertools.combinations(range(len(parts)),2):
            if intersects(parts[a],parts[b]):adjacency[a].add(b);adjacency[b].add(a);count+=1
        reached=set();remaining={0}
        while remaining:
            at=remaining.pop();reached.add(at);remaining.update(adjacency[at]-reached)
        assert len(reached)==len(parts),(name,[parts[i]['name'] for i in range(len(parts)) if i not in reached])
        result[name]=dict(mesh_parts=len(parts),solid_intersections=count,connected_components=1)
    assert len(groups['spectacles'])==9 and len(groups['mask'])==12
    return result


if __name__=='__main__':
    report={'reflection':continuity(),'accessories':joins(),
      'scope':'Rest geometry; five affine camera views; five positions per matching edge. Actual material/animation needs client review.'}
    dest=ROOT/'docs/validation-0.18/surfaces-and-accessory-joins.json';dest.parent.mkdir(parents=True,exist_ok=True)
    dest.write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps(report,indent=2))
