"""Export an edited Generic/free Blockbench rig into the mod's native mesh format.

Usage: python3 art/blockbench_to_runtime.py INPUT.bbmodel OUTPUT.json
       [--texture-output OUTPUT.png]

Native cube bounds, face UVs, cube rotation, inflation/stretch and mesh vertices
are preserved together with bone pivots/rest rotations. Animation clips remain
editable Blockbench previews; gameplay uses the matching procedural Java rig.
Supports both 4.x and 5.x group layouts.
"""
from __future__ import annotations
import argparse, base64, copy, json, math
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
DEFAULT_MATERIALS={'body':[0,0,32,32],'accent':[32,0,32,32],'wing':[64,0,32,32],'eye':[96,0,16,16],'pupil':[96,0,16,16],'white':[112,0,16,16],'mouth':[96,16,16,16],'orb':[112,16,16,16],'dark_orb':[0,32,16,16],'heart':[16,32,16,16],'flame':[32,32,32,32],'cocoon':[64,32,32,32],'shine':[96,32,16,16],'tuft':[0,64,32,32],'head':[32,64,32,32]}
DEFAULT_MATERIALS.update({'happy_eye':[96,64,8,8],'lid':[104,64,8,8],'mouth_open':[112,64,8,8]})
DEFAULT_MATERIALS.update({'brow':[96,80,8,8],'grin':[104,80,8,8],'belly':[112,80,8,8],'crest_tip':[0,96,32,32],'hand':[32,96,32,32],'toe':[64,96,32,32],'heel':[96,96,32,32],'collar':[0,128,32,32]})
DEFAULT_MATERIALS['variant_detail']=[32,128,32,32]
DEFAULT_MATERIALS.update({'goggle_frame':[64,128,32,32], 'goggle_lens':[96,128,32,32],
                         'goggle_strap':[0,160,32,32], 'goggle_highlight':[32,160,32,32]})
for i in range(21):DEFAULT_MATERIALS[f'animal_{i}']=[128+(i%4)*32,(i//4)*32,32,32]

def rounded(v):return [round(float(n),5) for n in v]
def rotate(v,rotation):
    # Blockbench's object Euler order is ZYX: X is applied first to a vertex.
    x,y,z=v
    rx,ry,rz=[math.radians(float(a)) for a in rotation]
    y,z=y*math.cos(rx)-z*math.sin(rx),y*math.sin(rx)+z*math.cos(rx)
    x,z=x*math.cos(ry)+z*math.sin(ry),-x*math.sin(ry)+z*math.cos(ry)
    x,y=x*math.cos(rz)-y*math.sin(rz),x*math.sin(rz)+y*math.cos(rz)
    return [x,y,z]
def infer_material(uv,materials):
    if not uv:return 'body'
    u=sum(p[0] for p in uv)/len(uv);v=sum(p[1] for p in uv)/len(uv)
    for name,(x,y,w,h) in materials.items():
      if x<=u<x+w and y<=v<y+h:return name
    return 'body'

def _finite_vector(value, size, label):
    if not isinstance(value,(list,tuple)) or len(value)!=size:
      raise ValueError(f'{label} must contain {size} numbers')
    if not all(isinstance(n,(int,float)) and not isinstance(n,bool) and math.isfinite(n) for n in value):
      raise ValueError(f'{label} must contain finite numbers')
    return [float(n) for n in value]

def _cube_face_uvs(obj, from_, to_):
    """Blockbench Cube.preview_controller.updateUV's box layout, before UVToLocal.

    Generic cube projects normally save explicit per-face UVs. Box UV is also
    accepted so adding an editor-created cube does not require converting it to
    a mesh. mirror_uv affects Box UV only, exactly as in Blockbench's controller.
    """
    source=obj.get('faces',{})
    if not isinstance(source,dict):raise ValueError('Cube faces must be an object')
    faces={direction:dict(face) for direction,face in source.items() if isinstance(face,dict)}
    if not obj.get('box_uv',False):return faces
    x,y,z=[abs(to_[i]-from_[i]) for i in range(3)]
    offsets={
      'east':([0,z],[z,y]), 'west':([z+x,z],[z,y]),
      'up':([z+x,z],[-x,-z]), 'down':([z+x*2,0],[-x,z]),
      'south':([z*2+x,z],[x,y]), 'north':([z,z],[x,y]),
    }
    if obj.get('mirror_uv',False):
      for origin,size in offsets.values():origin[0]+=size[0];size[0]*=-1
      offsets['east'],offsets['west']=offsets['west'],offsets['east']
    offset=_finite_vector(obj.get('uv_offset',[0,0]),2,'Cube UV offset')
    for direction,(origin,size) in offsets.items():
      face=faces.setdefault(direction,{'texture':0})
      face['uv']=[origin[0]+offset[0],origin[1]+offset[1],origin[0]+size[0]+offset[0],origin[1]+size[1]+offset[1]]
    return faces

def cube_to_runtime(obj, bone_world, materials):
    """Export a real Generic Blockbench cube as outward runtime quads.

    Cube from/to and origin are absolute, Y-up Blockbench positions, unlike
    Mesh.vertices (which are local to a mesh object's origin). Face coordinates
    follow Blockbench's own CubeFace.UVToLocal mapping. Reversed UV bounds and
    90-degree face rotations therefore remain editable without texture flips.
    """
    from_=_finite_vector(obj.get('from'),3,'Cube from')
    to_=_finite_vector(obj.get('to'),3,'Cube to')
    origin=_finite_vector(obj.get('origin',[0,0,0]),3,'Cube origin')
    rotation=_finite_vector(obj.get('rotation',[0,0,0]),3,'Cube rotation')
    world=_finite_vector(bone_world,3,'Bone origin')
    inflate=obj.get('inflate',0)
    if not isinstance(inflate,(int,float)) or not math.isfinite(inflate):raise ValueError('Cube inflation must be finite')
    stretch=_finite_vector(obj.get('stretch',[1,1,1]),3,'Cube stretch')
    faces=_cube_face_uvs(obj,from_,to_)
    center=[(from_[i]+to_[i])/2 for i in range(3)]
    half=[((to_[i]-from_[i])/2+inflate)*stretch[i] for i in range(3)]
    low=[center[i]-half[i] for i in range(3)];high=[center[i]+half[i] for i in range(3)]
    rescale=[1,1,1]
    if obj.get('rescale',False):
      factors=[]
      for angle in rotation:
        angle=abs(angle)%90
        if angle>45:angle=90-angle
        factors.append(1/math.cos(math.radians(angle)))
      rescale=[factors[1]*factors[2],factors[0]*factors[2],factors[0]*factors[1]]
    def transform(point):
      local=[(point[i]-origin[i])*rescale[i] for i in range(3)]
      rotated=rotate(local,rotation)
      absolute=[origin[i]+rotated[i] for i in range(3)]
      return rounded([absolute[0]-world[0],-(absolute[1]-world[1]),absolute[2]-world[2]])
    runtime_center=transform(center)
    def position(direction,u,v):
      # Official CubeFace.UVToLocal: image V runs downward while model Y is up.
      x0,y0,z0=low;x1,y1,z1=high
      if direction=='east':return [x1,y1+(y0-y1)*v,z1+(z0-z1)*u]
      if direction=='west':return [x0,y1+(y0-y1)*v,z0+(z1-z0)*u]
      if direction=='up':return [x0+(x1-x0)*u,y1,z0+(z1-z0)*v]
      if direction=='down':return [x0+(x1-x0)*u,y0,z1+(z0-z1)*v]
      if direction=='south':return [x0+(x1-x0)*u,y1+(y0-y1)*v,z1]
      if direction=='north':return [x1+(x0-x1)*u,y1+(y0-y1)*v,z0]
      raise ValueError('Unknown cube face '+direction)
    vertices=[];indices={};output=[];alluv=[];surfaces=set()
    for direction in ('north','east','south','west','up','down'):
      face=faces.get(direction)
      if not face or face.get('texture',0) is None or face.get('enabled',True) is False:continue
      uv=_finite_vector(face.get('uv',[0,0,0,0]),4,'Cube face UV')
      angle=face.get('rotation',0)
      if not isinstance(angle,(int,float)) or not math.isfinite(angle) or abs(angle/90-round(angle/90))>1e-6:
        raise ValueError('Cube face UV rotation must be a multiple of 90 degrees')
      coords=[]
      for u,v in ((0,0),(1,0),(1,1),(0,1)):
        for _ in range(int(round(angle/90))%4):u,v=1-v,u
        coords.append(transform(position(direction,u,v)))
      # Thin face planes are valid, but their zero-area side faces are not.
      a=[coords[1][i]-coords[0][i] for i in range(3)]
      b=[coords[2][i]-coords[0][i] for i in range(3)]
      normal=[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]]
      if sum(n*n for n in normal)<1e-14:continue
      # The native material is no-cull. A zero-thickness cube's coincident
      # opposite face would otherwise draw twice and fight for the same depth.
      surface=tuple(sorted(tuple(point) for point in coords))
      if surface in surfaces:continue
      surfaces.add(surface)
      points=[]
      for point in coords:
        key=tuple(point)
        if key not in indices:indices[key]=len(vertices);vertices.append(point)
        points.append(indices[key])
      faceuv=[[uv[0],uv[1]],[uv[2],uv[1]],[uv[2],uv[3]],[uv[0],uv[3]]]
      delta=[sum(point[i] for point in coords)/4-runtime_center[i] for i in range(3)]
      if sum(normal[i]*delta[i] for i in range(3))<0:points.reverse();faceuv.reverse()
      faceuv=[rounded(point) for point in faceuv]
      output.append({'v':points,'uv':faceuv});alluv.extend(faceuv)
    if not output:return None
    return {'name':obj.get('name','cube'),'material':obj.get('chao_material') or infer_material(alluv,materials),'vertices':vertices,'faces':output,'shading':'flat','primitive':'cube'}

def export_model(project:dict)->dict:
    if project.get('meta',{}).get('model_format')!='free':raise ValueError('Use a Generic/free Blockbench project; Java cube exports cannot preserve this mesh rig.')
    resolution=project.get('resolution',{})
    width=int(resolution.get('width',256));height=int(resolution.get('height',256))
    extra=project.get('chao_runtime',{})
    ground=float(extra.get('ground_y',0 if width==64 and height==64 else 24))
    materials=copy.deepcopy(extra.get('materials',{'shell':[0,0,64,64]} if ground==0 else DEFAULT_MATERIALS))
    elements={e['uuid']:e for e in project.get('elements',[])}
    groups={g['uuid']:g for g in project.get('groups',[])}
    bones=[];seen=set();usednames=set()
    def visit(node,parent_name=None,parent_world=None):
      if isinstance(node,str):
        if node in elements:raise ValueError('Place loose meshes inside a named rig group before exporting.')
        if node in groups:node=groups[node]
        else:raise ValueError('Outliner references an unknown group/mesh UUID: '+node)
      if not isinstance(node,dict):raise ValueError('Malformed Blockbench outliner node')
      # v5 outliner entries reference groups[] and keep only expansion/children.
      node={**groups.get(node.get('uuid'),{}),**node}
      if node.get('export') is False:return
      name=node.get('name','bone')
      if name in usednames:raise ValueError('Bone names must be unique: '+name)
      usednames.add(name)
      uuid=node.get('uuid')
      if uuid in seen:raise ValueError('Cyclic/repeated outliner group: '+str(uuid))
      seen.add(uuid)
      world=[float(v) for v in node.get('origin',[0,0,0])]
      runtimeworld=[world[0],ground-world[1],world[2]]
      parent_world=parent_world or [0,0,0]
      bone={'name':name,'parent':parent_name,'pivot':rounded([runtimeworld[i]-parent_world[i] for i in range(3)]),'meshes':[]}
      rotation=node.get('rotation',[0,0,0])
      if any(abs(float(a))>.00001 for a in rotation):bone['rotation']=rounded([-float(rotation[0]),float(rotation[1]),-float(rotation[2])])
      bones.append(bone)
      for child in node.get('children',[]):
       if isinstance(child,str) and child in elements:
        obj=elements[child]
        if obj.get('export') is False:continue
        if obj.get('type')=='cube':
          if 'box_uv' not in obj:obj={**obj,'box_uv':project.get('meta',{}).get('box_uv',False)}
          cube=cube_to_runtime(obj,world,materials)
          if cube is not None:bone['meshes'].append(cube)
          continue
        if obj.get('type')!='mesh':raise ValueError('Only Generic cube and mesh elements are supported.')
        points=obj.get('vertices',{});keys=list(points);indices={k:i for i,k in enumerate(keys)}
        origin=[float(v) for v in obj.get('origin',[0,0,0])];rot=obj.get('rotation',[0,0,0]);vertices=[]
        for key in keys:
          rotated=rotate(points[key],rot)
          absolute=[origin[i]+rotated[i] for i in range(3)]
          relative=[absolute[0]-world[0],-(absolute[1]-world[1]),absolute[2]-world[2]]
          vertices.append(rounded(relative))
        faces=[];alluv=[]
        for face in obj.get('faces',{}).values():
          vk=list(reversed(face.get('vertices',[])))
          if len(vk)<3:continue
          if face.get('texture') is None:continue
          uv=[rounded(face.get('uv',{}).get(k,[0,0])) for k in vk]
          if len(vk)<=4:
            faces.append({'v':[indices[k] for k in vk],'uv':uv});alluv.extend(uv)
          else:
            for j in range(1,len(vk)-1):
              selected=[0,j,j+1];faces.append({'v':[indices[vk[i]] for i in selected],'uv':[uv[i] for i in selected]});alluv.extend(uv[i] for i in selected)
        if not faces:continue
        material=obj.get('chao_material') or infer_material(alluv,materials)
        bone['meshes'].append({'name':obj.get('name','mesh'),'material':material,'vertices':vertices,'faces':faces,'shading':obj.get('shading','flat')})
       else:visit(child,name,runtimeworld)
    for node in project.get('outliner',[]):visit(node)
    if not bones:raise ValueError('No rig groups found; keep meshes in named rig groups.')
    return {'format_version':1,'texture_width':width,'texture_height':height,'ground_y':int(ground) if ground.is_integer() else ground,'materials':materials,'bones':bones}

def embedded_texture(project:dict)->bytes:
    textures=project.get('textures',[])
    if not textures:raise ValueError('Project has no embedded texture.')
    source=textures[0].get('source','')
    if not source.startswith('data:image/png;base64,'):raise ValueError('Embed the PNG texture before exporting.')
    return base64.b64decode(source.split(',',1)[1],validate=True)

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('input',type=Path);p.add_argument('output',type=Path);p.add_argument('--texture-output',type=Path)
 args=p.parse_args();project=json.loads(args.input.read_text());model=export_model(project)
 args.output.parent.mkdir(parents=True,exist_ok=True);args.output.write_text(json.dumps(model,separators=(',',':')))
 if args.texture_output:args.texture_output.parent.mkdir(parents=True,exist_ok=True);args.texture_output.write_bytes(embedded_texture(project))
 print(f"Exported {len(model['bones'])} bones to {args.output}")
if __name__=='__main__':main()
