#!/usr/bin/env python3
"""Read-only Hero Run audit of supplied SA2B PRS members; writes only an audit JSON.

This is an offline authoring/research tool. It adds no Minecraft/game dependency
and does not regenerate existing toy, Chao or texture assets.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,math,struct,sys
import numpy as np

ROOT=Path(__file__).resolve().parents[1]
ART=ROOT/'art'
INPUTS={
 'art/sa2_02514/original/ChaoMain.prs':'d296ebbb14a8386e86eba3035b9fb35c6e1add0e563ef233ef67c8830be17dc8',
 'art/sa2_02515/original/al_body.prs':'ab4932311f7600654b91dbacba5bc8320048af469c44fb120722c42c387d380c',
}
PARTS=['root','body','arm_left_position','arm_left','hand_left','leg_left_position','leg_left','tail_position','tail',
 'arm_right_position','arm_right','hand_right','leg_right_position','leg_right','neck','head_position','head',
 'eye_left_position','eye_left','eyelid_left','eye_right_position','eye_right','eyelid_right','feeler_left','ear_left',
 'feeler_right','ear_right','mouth','tongue_position','forehead','horn_left','horn_right','office_position','office',
 'back','navel','wing_left_position','wing_left','wing_right_position','wing_right']
SHAPES={'neutral_child':0,'hero_child':6,'hero_child_run':10,'hero_run_young':78,'hero_run_aged_normal':79,'hero_run_aged_run':82}
# Offsets follow the corroborated CHAO_GLOBAL field layout, not float-name guesses.
CONSTANTS={'DeformLimit':0x40,'GrowthLimit':0x44,'GrowthMayuPoint':0x48,'NormalBoundary':0x4c,'NeutralBoundary':0x50,
 'AddGrowthTime':0x54,'TimeAddGrowthChild':0x58,'TimeAddGrowthAdult':0x5c,'TimeAddAttrChild':0x60,'TimeAddAttrAdult':0x64,
 'FruitAddGrowthChild':0xac,'FruitAddGrowthAdult':0xb0,'MinimalAddFormChild':0xd0,'MinimalAddFormAdult':0xd4,
 'MinimalAddGrowthChild':0xe0,'MinimalAddGrowthAdult':0xe4}

def sha(data):return hashlib.sha256(data).hexdigest()
def transform(data,p):
 flags=struct.unpack_from('>I',data,p)[0];pos=struct.unpack_from('>3f',data,p+8)
 angles=struct.unpack_from('>3i',data,p+20);scale=struct.unpack_from('>3f',data,p+32);out=np.eye(4)
 if not flags&1:out[:3,3]=pos
 if not flags&2:
  # SAModel.Direct3D ProcessTransforms + MatrixFunctions.RotateXYZ/RotateZYX,
  # converted from row-basis mutation to column-vector matrix composition.
  for axis in ((1,0,2) if flags&0x20 else (2,1,0)):
   theta=angles[axis]*math.tau/65536;c,s=math.cos(theta),math.sin(theta)
   if axis==1:s=-s
   r=np.eye(4);a,b=[j for j in range(3) if j!=axis]
   r[a,a]=r[b,b]=c;r[a,b]=-s;r[b,a]=s;out=out@r
 if not flags&4:out=out@np.diag([*scale,1])
 return out

def audit():
 for name,expected in INPUTS.items():
  assert sha((ROOT/name).read_bytes())==expected,'Input differs from audited supplied member: '+name
 sys.path.insert(0,str(ART))
 spec=importlib.util.spec_from_file_location('sa2_source_decoder_052',ART/'extract_sa2_02515.py')
 decoder=importlib.util.module_from_spec(spec);spec.loader.exec_module(decoder)
 decompressed=decoder.prs((ROOT/next(iter(INPUTS))).read_bytes());data=decoder.relocate(decompressed)
 names=decoder.texture_names(decoder.prs((ROOT/'art/sa2_02515/original/al_body.prs').read_bytes()))
 u=lambda p:struct.unpack_from('>I',data,p)[0]
 table=0x3a4cd0;roots=[u(table+i*4) for i in range(144)]
 # Exactly 24 types (child=2 through Amy=25), six source targets per type.
 assert len(roots)==144 and all(0<p<len(data)-52 and p%4==0 for p in roots)
 shapes={}
 for name,index in SHAPES.items():
  nodes=[];all_points=[]
  def walk(p,parent=np.eye(4),parent_index=None):
   while p:
    idx=len(nodes);matrix=parent@transform(data,p);faces=decoder.chunk(data,p,names)
    points=[(matrix@np.asarray([*v,1]))[:3] for f in faces for v in f['points']]
    bounds=[np.min(points,axis=0).tolist(),np.max(points,axis=0).tolist()] if points else None
    all_points.extend(points)
    nodes.append({'index':idx,'part':PARTS[idx] if idx<len(PARTS) else 'unknown','object_offset':hex(p),
      'parent_index':parent_index,'pivot_world':matrix[:3,3].tolist(),'local_position':list(struct.unpack_from('>3f',data,p+8)),
      'flags':hex(u(p)),'triangles':len(faces),'bind_pose_bounds':bounds})
    child=u(p+44)
    if child:walk(child,matrix,idx)
    p=u(p+48)
  walk(roots[index]);assert len(nodes)==40,name
  total=sum(n['triangles'] for n in nodes);assert total==720,(name,total)
  shapes[name]={'table_index':index,'root_object':hex(roots[index]),'nodes':nodes,'triangles':total,
    'bind_pose_bounds':[np.min(all_points,axis=0).tolist(),np.max(all_points,axis=0).tolist()]}
 constants={name:{'decompressed_offset':hex(0x310700+offset),'float32':struct.unpack_from('>f',decompressed,0x310700+offset)[0]}
            for name,offset in CONSTANTS.items()}
 assert abs(constants['GrowthLimit']['float32']-1.2)<1e-6
 ratio=constants['TimeAddGrowthAdult']['float32']/constants['TimeAddGrowthChild']['float32'];assert abs(ratio-.25)<1e-6
 return {'prototype':'0.25.52','evidence_kind':'offline supplied-source audit, not a Minecraft or original-game play session',
   'input_sha256':INPUTS,'decompressed_ChaoMain_bytes':len(decompressed),'decompressed_ChaoMain_sha256':sha(decompressed),
   'root_table_offset':hex(table),'root_table_count':len(roots),'shape_order':['young','aged_normal','aged_swim','aged_fly','aged_run','aged_power'],
   'part_layout_reference':'https://github.com/Exant64/CWE/blob/0aea72e06c7eb213769b7ee5fd53ef5db57b7e37/CWE/al_draw.h',
   'type_and_group_reference':'https://github.com/Exant64/CWE/blob/0aea72e06c7eb213769b7ee5fd53ef5db57b7e37/CWE/api/api_customchao.cpp',
   'transform_reference':'https://github.com/X-Hax/sa_tools/blob/58e6cd1ee3f1f275ccbe288f46944fec28d3883b/Libraries/SAModel.Direct3D/MatrixFunctions.cs',
   'constant_layout_reference':'https://github.com/Exant64/CWE/blob/0aea72e06c7eb213769b7ee5fd53ef5db57b7e37/CWE/al_behavior/al_intention.h',
   'constant_table':constants,'adult_child_time_growth_ratio':ratio,
   'limits':['Absolute original wall-clock callback frequency is not verified by these constants.',
     'Source body textures use CI4 indexed palettes; the supplied al_body member alone does not establish final in-game RGB colours.',
     'Bind-pose extents include source crests and facial surfaces; they are not biological growth rates or Minecraft hitboxes.',
     'The existing historical source extractor is imported only for PRS/chunk decoding; this audit supplies its own corroborated transform and changes no old exports.'],
   'shapes':shapes}

if __name__=='__main__':
 parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--output',type=Path,default=ROOT/'docs/research/hero-run-02552-source-audit.json');args=parser.parse_args()
 out=audit();args.output.parent.mkdir(parents=True,exist_ok=True);args.output.write_text(json.dumps(out,indent=2)+'\n')
 print('Audited supplied Hero Run source: 6 shapes, 40 nodes / 720 triangles each; growth cap 1.2; adult/child constant ratio 0.25')
 print(args.output)
