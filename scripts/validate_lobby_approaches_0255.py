#!/usr/bin/env python3
"""Bind perimeter treads, complete rail unions and photographed garden gates to the real renderer."""
from pathlib import Path
import argparse,json
ROOT=Path(__file__).resolve().parents[1]
CHECKS=0
def require(c,m):
 global CHECKS;CHECKS+=1
 if not c:raise AssertionError(m)
def method(t,n):
 a=t.index(n);a=t.index('{',a);depth=1;b=a+1
 while depth:depth+=(t[b]=='{')-(t[b]=='}');b+=1
 return t[a+1:b-1]
def contract(sources):
 hub,renderer,core,block=sources
 require('REVISION=38' in hub,'Existing authored rooms will not receive the latest recessed Kindergarten windows')
 require('HERO_DOOR=new BlockPos(18,89,-12),DARK_DOOR=new BlockPos(-18,71,12)' in hub,'Gate footprints no longer fit the circular lobby')
 require('HERO_FACING=Direction.SOUTH,DARK_FACING=Direction.NORTH' in hub,'Gate mouths are disconnected from the curved landings')
 require('!LobbyApproachGeometry0255.floorOpening(x,z)' in hub,'Native main floor covers descending approach')
 require('LobbyApproachGeometry0255.nativeCells().keySet()' in hub,'Shared curved geometry no longer places native collisions')
 require('p.put(x,y-1,z,LobbyDecor025.GLASS_FLOOR.get())' not in hub,'Old bulky staircase foundation returned')
 require('PortalScenes0251.place(p,HERO_DOOR,HERO_FACING,PortalScenes0251.HERO)' in hub and 'PortalScenes0251.place(p,DARK_DOOR,DARK_FACING,PortalScenes0251.DARK)' in hub,'A garden route is inactive')
 require('MIN_VISITOR_Y=68' in hub,'Lower landing falls outside visitor safety interval')
 require('extends Block' in block and 'return RenderShape.INVISIBLE' in block and '.dynamicShape()' in block,'Finite native curved collision is not rendered separately')
 require('LobbyApproachGeometry0255.nativeBoxes(p.getX(),p.getY(),p.getZ())' in block,'Native cell shapes are disconnected from shared geometry')
 require('noLootTable()' in block and 'register("lobby_floor_stairs"' in block and 'BlockItem' not in block,'Internal stair ID became obtainable or changed')
 require('TREAD_THICKNESS=.125' in core and 'top-TREAD_THICKNESS' in core,'Treads regained thick block supports')
 require('INNER_RADIUS=20.5,OUTER_RADIUS=23.5' in core and 'Math.toRadians(-40)' in core and 'Math.toRadians(20)' in core,'Three-wide wall-following arc disappeared')
 require('if(!entryEdge(hero,edge))' in core and 'rails(false,true)' in core,'Open upper-rim or lower rail section returned')
 require('upper||hero?.12:.65' in core and 'ground-rail.skirt' in core and 'LobbyApproachGeometry0255.RAIL_HEIGHT+rail.skirt()' in renderer,'Descending rail glass skirt does not meet its half-height treads')
 require('if(!hidden)result.add(new Edge' in core,'Landing/flight turns are not joined using their actual union boundary')
 require('floorPieces(polygon)' in renderer and 'm.floorQuad(' in method(renderer,'private static Mesh floor()'),'Scene floor covers physical descending void')
 grid=method(renderer,'private static Mesh grid()')
 require('m.floorQuad(' in grid and grid.count('m.floorRing(')==4 and 'm.ring(' not in grid,'Floating grid lines cross descending cutout')
 glass=method(renderer,'private static Mesh approachGlass()')
 require('LobbyApproachGeometry0255.stairFaces(hero)' in glass and 'LobbyApproachGeometry0255.rails(hero,false)' in glass and 'LobbyApproachGeometry0255.rails(false,true)' in glass,'Visual treads or upper/lower guards differ from native plan')
 require('m.cylinder(a.x(),y0,a.z(),.081,height,8,color)' in renderer and 'm.cylinder(b.x(),y1,b.z(),.081,height,8,color)' in renderer,'Handrail turn caps have gaps')
 require('APPROACH_STARS.draw(poses,buffers,STAR_TEX,0)' in renderer and 'APPROACH_GLASS.draw(poses,buffers,GLASS_TEX,0)' in renderer,'Celestial material lost')
 require('APPROACH_TRIM.draw(poses,buffers,WHITE,1)' in renderer,'Gold rail caps disappeared')
 require('import com.chaogarden.core.GardenGateGeometry0256;' in renderer and 'import com.chaogarden.hub.HubRooms;' in renderer,'Real gates are disconnected from tested reference geometry')
 require('APPROACH_GATES=approachGates(false),APPROACH_GATE_MASONRY=approachGates(true)' in renderer and 'APPROACH_GATE_MASONRY.draw(poses,buffers,STONE,1)' in renderer,'Dark gate masonry has no actual stone material batch')
 gate=method(renderer,'private static Mesh approachGates(')
 require('GardenGateGeometry0256.gate(hero)' in gate and 'HubRooms.HERO_DOOR' in gate and 'HubRooms.DARK_DOOR' in gate and 'gatePoint(' in gate,'Gate scene ignores relocated native anchors')
 require('HubRooms.gateWorldPoint(anchor,facing,v)' in method(renderer,'private static double[] gatePoint(') and 'switch(facing)' in method(hub,'public static Vec3 gateWorldPoint('),'Gate visuals and particles do not turn with native apertures')
 require('sceneZ(boolean hero,double z){return hero?-z:z;}' in core and 'travelAngle(hero,x,z)' in core,'Hero geometry has not flipped independently from Dark')
 require('LobbyApproachGeometry0255.sceneZ(hero,LobbyApproachGeometry0255.INNER_RADIUS*Math.sin(a))' in renderer,'Hero gold tread edges did not follow the flipped walkable treads')
 fire=method(renderer,'private static void darkFire(')
 require('block/soul_fire_' in fire and 'TextureAtlas.LOCATION_BLOCKS' in fire and 'sprite.getU0()' in fire,'Dark fire does not use Minecraft animated soul-fire sprites')
 require('Math.sin(time*' in fire and 'darkFire(poses,buffers,' in renderer,'Soul fire is static or disconnected from the scene render')
 render=method(renderer,'@Override public void render(')
 require('if(mainSceneSubmitted)return;\n      mainSceneSubmitted=true;' in render and render.index('mainSceneSubmitted=true;')<render.index('poses.translate(.5,2.002,.5)'), 'Native/fallback paths can submit the shared scene twice')
 require('Stage.AFTER_SKY){ALPHA_SCENES.clear();mainSceneSubmitted=false;return;}' in renderer,'Per-frame submission guard is stale')
 require('Stage.AFTER_BLOCK_ENTITIES){submitUnregisteredLobby(event);return;}' in renderer,'Fresh-facing-away lobby anchor has no post-portal render fallback')
 fallback=method(renderer,'private static void submitUnregisteredLobby(')
 require('if(mainSceneSubmitted)return;' in fallback and 'mc.level.dimension().equals(com.chaogarden.hub.HubContent.HUB)' in fallback,'Fallback is duplicated or leaks into another dimension')
 require('mc.level.getBlockEntity(pos) instanceof LobbyDecor025.SceneEntity anchor' in fallback and 'new net.minecraft.core.BlockPos(0,78,0)' in fallback and 'getValue(LobbyDecor025.SceneAnchor.PORTAL)' in fallback,'Fallback does not use the actual received central native scene entity')
 require('poses.translate(pos.getX()-camera.x,pos.getY()-camera.y,pos.getZ()-camera.z)' in fallback and 'mc.getBlockEntityRenderDispatcher().render(anchor,' in fallback and 'getGameTimeDeltaPartialTick(false)' in fallback,'Fallback bypasses native renderer transform or time')
 require('getChunk(' not in fallback and 'new LobbyDecor025.SceneEntity' not in fallback,'Fallback manufactures an unloaded scene')
def main(root):
 paths=['src/main/java/com/chaogarden/hub/HubRooms.java','src/main/java/com/chaogarden/client/LobbySceneRenderer025.java','src/main/java/com/chaogarden/core/LobbyApproachGeometry0255.java','src/main/java/com/chaogarden/hub/LobbyApproachDecor0255.java']
 sources=[(root/p).read_text() for p in paths];contract(sources)
 particle=(root/'src/main/java/com/chaogarden/client/DarkGateFlames0257.java').read_text()
 gatecore=(root/'src/main/java/com/chaogarden/core/GardenGateGeometry0256.java').read_text()
 require('ParticleTypes.SOUL_FIRE_FLAME' in particle and 'ParticleTypes.SMOKE' in particle,'Dark cups lost native fire and smoke particles')
 require('ClientTickEvent.Post' in particle and 'mc.isPaused()' in particle and 'HubContent.HUB' in particle and '48*48' in particle,'Live fire emitter is not bounded to active nearby lobby clients')
 require('HubRooms.gateWorldPoint(HubRooms.DARK_DOOR,HubRooms.DARK_FACING,local)' in particle,'Particle sources drift from rendered cups')
 require('private static void flame(' not in gatecore and 'flame(m,x,' not in gatecore,'Rigid sculpted flames remain under native animated fire')
 state=json.loads((root/'src/main/resources/assets/chao_garden/blockstates/lobby_floor_stairs.json').read_text());model=json.loads((root/'src/main/resources/assets/chao_garden/models/block/lobby_floor_stairs.json').read_text())
 require(state=={'variants':{'':{'model':'chao_garden:block/lobby_floor_stairs'}}},'Finite collision cells have missing model binding')
 require(model=={'textures':{'particle':'chao_garden:scene/lobby_glass'},'elements':[]},'Native model obscures celestial surface')
 negatives=[(0,'REVISION=38','REVISION=33','stale authored room revision'),(0,'!LobbyApproachGeometry0255.floorOpening(x,z)','true','covered descent'),(0,'MIN_VISITOR_Y=68','MIN_VISITOR_Y=76','lower route rescue'),(1,'APPROACH_STARS.draw(poses,buffers,STAR_TEX,0)','APPROACH_STARS.draw(poses,buffers,WHITE,0)','lost celestial tread'),(1,'floorPieces(polygon)','unclippedFloor(polygon)','floating floor'),(1,'m.cylinder(a.x(),y0,a.z(),.081,height,8,color)','omitCornerCap()','rail turn gap'),(2,'TREAD_THICKNESS=.125','TREAD_THICKNESS=1','block foundation'),(2,'if(!entryEdge(hero,edge))','if(true)','fenced entry'),(3,'return RenderShape.INVISIBLE','return RenderShape.MODEL','duplicate native surface'),(2,'return hero?-z:z','return z','unflipped Hero'),(1,'block/soul_fire_','block/fire_','orange or missing soul fire'),
 (1,'if(mainSceneSubmitted)return;','if(false)return;','duplicate scene submission'),(1,'mainSceneSubmitted=false;return;','return;','stale submission flag'),(1,'Stage.AFTER_BLOCK_ENTITIES){submitUnregisteredLobby(event);return;}','Stage.AFTER_BLOCK_ENTITIES){return;}','missing fresh anchor fallback'),(1,'mc.level.dimension().equals(com.chaogarden.hub.HubContent.HUB)','mc.level.dimension().equals(com.chaogarden.hub.HubContent.GARDEN)','fallback wrong dimension'),(1,'poses.translate(pos.getX()-camera.x,pos.getY()-camera.y,pos.getZ()-camera.z)','poses.translate(pos.getX(),pos.getY(),pos.getZ())','fallback wrong camera space')]
 for i,before,after,label in negatives:
  require(before in sources[i],'Mutant missed live source: '+label);broken=list(sources);broken[i]=broken[i].replace(before,after)
  try:contract(broken)
  except (AssertionError,ValueError):pass
  else:raise AssertionError('Accepted malformed '+label)
 print(f'Lobby perimeter approaches0257: {CHECKS} bindings; {len(negatives)} malformed regressions rejected.')
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--root',type=Path,default=ROOT);main(p.parse_args().root.resolve())
