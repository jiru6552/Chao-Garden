#!/usr/bin/env python3
"""Bind portal materials/placements to the real renderer and reject opaque regressions."""
from pathlib import Path
import argparse,json,re
from statistics import median
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
FILES={
 'renderer':'src/main/java/com/chaogarden/client/LobbySceneRenderer025.java',
 'helper':'src/main/java/com/chaogarden/hub/PortalScenes0251.java',
 'hub':'src/main/java/com/chaogarden/hub/HubRooms.java',
 'school':'src/main/java/com/chaogarden/kindergarten/KindergartenRooms025.java',
 'decor':'src/main/java/com/chaogarden/hub/LobbyDecor025.java',
 'trigger':'src/main/java/com/chaogarden/hub/HubContent.java',
 'garden':'src/main/java/com/chaogarden/hub/GardenRooms025.java',
 'geometry':'src/main/java/com/chaogarden/core/PortalGeometry0251.java',
}
CHECKS=0
def require(ok,message):
 global CHECKS
 CHECKS+=1
 if not ok:raise AssertionError(message)
def method(source,name):
 start=source.index(name);start=source.index('{',start);depth=1;cursor=start+1
 while depth and cursor<len(source):
  depth+=(source[cursor]=='{')-(source[cursor]=='}');cursor+=1
 require(depth==0,'Unclosed Java method '+name)
 return source[start+1:cursor-1]
def contract(source,manifest):
 renderer=source['renderer'];portal=method(renderer,'private static void portal(')
 require('getBuffer(RenderType.endPortal())' in portal and 'flush(buffers,RenderType.endPortal())' in portal,'Vertical portal lost real vanilla shader or batch closure')
 require(portal.count('v.addVertex(matrix,')==8,'Portal must emit opposite four-vertex windings')
 require(',.001f)' in portal and ',-.001f)' in portal,'Both sides must have explicit shallow depth separation')
 require('.setUv(' not in portal and '.setColor(' not in portal,'End shader requires its own position-only material')
 require('PORTAL_OPENINGS.get(style)' in portal,'Visible aperture no longer uses shared geometry')
 require('portal(entity,poses,buffers);return;' in renderer,'A portal anchor must not render the entire lobby/sky')
 require('PortalGeometry0251.frame(style)' in renderer and 'PORTAL_FRAMES[style].draw(poses,buffers,QUARTZ,1)' in portal,'Closed quartz geometry disconnected from actual material')
 require('"minecraft","textures/block/quartz_block_side.png"' in renderer,'Quartz frame material binding changed')
 prism=method(renderer,'void prism(')
 require(prism.count('quad(')==3 and 'i+1<outline.length' in prism and 'outline[(i+1)%outline.length]' in prism,'Solid roof prism lost a front/back cap or perimeter reveal')
 require('m.prism(' in method(renderer,'private static Mesh architecture()'),'Kindergarten arch reverted to uncapped planes')
 require('m.cylinder(0,5.30,0,3.34,.08,32' in renderer,'Gazebo ceiling lost its closed opaque core')
 require('UPPER_PAD.draw(poses,buffers,PAD_TEX,1)' in renderer,'Upper energy disk no longer writes depth')
 require('EXIT.draw(poses,buffers,EXIT_TEX,2)' in renderer and 'setCullState(mode==2?CULL:NO_CULL)' in renderer,'EXIT text can be viewed mirrored through its backface')
 label=method(renderer,'private static Mesh exitSign()')
 require(label.count('m.quad(')==2 and 'side*3.72' not in label and '-3.77' in label and '-3.39' in label,'EXIT labels must sit separated from their own two real crest faces')
 for name in ('hub','school'):
  require('Blocks.BLACK_CONCRETE' not in source[name],'Opaque backing returned to '+name+' portal plan')
 require('PortalScenes0251.HUB_SCHOOL,Direction.SOUTH,PortalScenes0251.SCHOOL' in source['hub'],'Hub school portal missing/wrong style')
 require('PortalScenes0251.HUB_GARDEN,Direction.NORTH,PortalScenes0251.GARDEN' in source['hub'],'Hub garden portal missing/wrong style')
 require('PortalScenes0251.state(Direction.NORTH,PortalScenes0251.RETURN)' in source['school'],'School return aperture missing')
 require('SceneAnchor.PORTAL,true' in source['helper'] and 'SceneAnchor.STYLE,style' in source['helper'],'Portal helper lost native state binding')
 require('builder.add(PORTAL,FACING,STYLE)' in source['decor'],'Native portal state definition incomplete')
 require('new AABB(worldPosition).inflate(5,8,5)' in source['decor'],'Portal view bounds no longer include tall gate')
 require('PortalGeometry0251.contains' in source['trigger'],'Server trigger no longer follows visible aperture')
 require('PortalScenes0251.TUNNEL' in method(source['trigger'],'boolean inGardenCave('),
         'Garden cave trigger must use the rectangular tunnel opening')
 require('PortalScenes0251.place(p,EXIT_PORTAL,Direction.SOUTH,PortalScenes0251.TUNNEL)' in source['garden'],
         'Garden scene and trigger disagree about the unframed tunnel style')
 frame=method(renderer,'private static Mesh portalFrame(')
 require('if(!PortalGeometry0251.hasFrame(style))return mesh;' in frame and
         frame.index('hasFrame(style)') < frame.index('mesh.box('),
         'Unframed tunnel unexpectedly renders a quartz foot or frame')
 require('TUNNEL=3' in source['geometry'].replace(' ','') and
         'style!=TUNNEL' in method(source['geometry'],'boolean hasFrame(').replace(' ',''),
         'Tunnel geometry lost its distinct frame-free style')
 require(len(manifest['blocks'])==8 and all(not b['item'] for b in manifest['blocks']),'Internal portal anchor became an obtainable item/new registry ID')

def sky_contract(image):
 require(image.mode=='RGBA' and image.size==(1024,512),'Wrong dome texture domain')
 require(all(image.getpixel((0,y))==image.getpixel((1023,y)) for y in range(512)),'Dome longitude seam is visible')
 require(all(image.getpixel((x,y))==image.getpixel((0,y)) for x in range(1024) for y in (0,511)),'Dome pole pinwheel is visible')
 for y in list(range(16))+list(range(496,512)):
  require(all(image.getpixel((x,y))==image.getpixel((0,y)) for x in range(1024)),'Near-pole cloud fan returned')
 # Ignore isolated stars through the median; an abrupt cloud cutoff changes
 # the continuous sky background over most of the row and must be rejected.
 for v in (.38,.58):
  center=round(v*511)
  for y in range(center-2,center+3):
   jumps=[max(abs(image.getpixel((x,y))[c]-image.getpixel((x,y-1))[c]) for c in range(3)) for x in range(1024)]
   require(median(jumps)<=4,'Dome cloud latitude cutoff returned')

def main(root):
 source={name:(root/path).read_text() for name,path in FILES.items()}
 manifest=json.loads((root/'art/lobby_garden_manifest_025.json').read_text())
 contract(source,manifest)
 with Image.open(root/'src/main/resources/assets/chao_garden/textures/scene/lobby_celestial_sky.png') as im:sky=im.copy()
 sky_contract(sky)
 negatives=[('vanilla shader', 'renderer','getBuffer(RenderType.endPortal())','getBuffer(RenderType.solid())'),
  ('one-sided surface','renderer','v.addVertex(matrix,0,cy,-.001f);',''),
  ('opaque hub backing','hub','PortalScenes0251.place(p,PortalScenes0251.HUB_SCHOOL','p.box(-2,80,-24,2,85,-24,Blocks.BLACK_CONCRETE); PortalScenes0251.place(p,PortalScenes0251.HUB_SCHOOL'),
  ('leaking sky branch','renderer','portal(entity,poses,buffers);return;','portal(entity,poses,buffers);'),
  ('uncapped roof','renderer','quad(p(first[0],first[1],z1),p(c[0],c[1],z1),p(b[0],b[1],z1),p(first[0],first[1],z1),shade(color,.9));',''),
  ('transparent ceiling','renderer','UPPER_PAD.draw(poses,buffers,PAD_TEX,1)','UPPER_PAD.draw(poses,buffers,PAD_TEX,0)'),
  ('mirrored EXIT','renderer','setCullState(mode==2?CULL:NO_CULL)','setCullState(NO_CULL)'),
  ('detached server aperture','trigger','PortalGeometry0251.contains','PortalGeometry0251.unrelated'),
  ('tunnel rim returned','renderer','if(!PortalGeometry0251.hasFrame(style))return mesh;',''),
  ('tunnel trigger style','trigger','GardenRooms025.EXIT_PORTAL,PortalScenes0251.TUNNEL','GardenRooms025.EXIT_PORTAL,PortalScenes0251.GARDEN')]
 for label,key,before,after in negatives:
  require(before in source[key],'Negative fixture no longer reaches live '+label)
  malformed=dict(source);malformed[key]=malformed[key].replace(before,after)
  try:contract(malformed,manifest)
  except AssertionError:pass
  else:raise AssertionError('Accepted malformed '+label)
 for label,kind in [('dome wrap seam',0),('dome cloud cutoff',1),('dome pole',2),('near-pole fan',3)]:
  bad=sky.copy()
  if kind==0:bad.putpixel((1023,240),(0,0,0,255))
  elif kind==1:
   y=round(.38*511)
   for x in range(1024):bad.putpixel((x,y),(255,255,255,255))
  elif kind==2:bad.putpixel((100,0),(0,0,0,255))
  else:bad.putpixel((100,10),(0,0,0,255))
  try:sky_contract(bad)
  except AssertionError:pass
  else:raise AssertionError('Accepted malformed '+label)
 print(f'Portal0251 source/material assertions passed: {CHECKS}; real two-sided vanilla shader, closed quartz/roof geometry, readable EXIT and seamless dome; {len(negatives)+4} malformed fixtures rejected.')
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--root',type=Path,default=ROOT);main(p.parse_args().root.resolve())
