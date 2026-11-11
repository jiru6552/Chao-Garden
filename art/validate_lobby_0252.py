#!/usr/bin/env python3
"""Bind tested gate geometry and a single sorted alpha pass to the native renderer.

The separate LobbyGeometry0252Test exercises emitted numeric geometry. This
guard verifies that the real renderer consumes it and preserves depth/order;
only actual oblique client footage can complete the appearance review.
"""
from pathlib import Path
import argparse
import hashlib
import json

ROOT=Path(__file__).resolve().parents[1]
RENDERER='src/main/java/com/chaogarden/client/LobbySceneRenderer025.java'
GEOMETRY='src/main/java/com/chaogarden/core/LobbyGeometry0252.java'
TEST='src/test/java/com/chaogarden/core/LobbyGeometry0252Test.java'
CHECKS=0

def require(ok,message):
    global CHECKS
    CHECKS+=1
    if not ok:raise AssertionError(message)

def contract(renderer,geometry):
    start=renderer.index('@Override public void render(')
    end=renderer.index('private static Mesh portalFrame(',start)
    render=renderer[start:renderer.index('public static final class TranslucentPass',start)]
    late=renderer[renderer.index('public static final class TranslucentPass',start):end]
    opaque='flush(buffers,Material.get(WHITE,1))'
    glazing='GLAZING.draw(poses,buffers,WHITE,0)'
    energy='energy(poses,buffers,scene.phase())'
    alpha='flush(buffers,Material.get(WHITE,0))'
    queue='ALPHA_SCENES.add(new AlphaScene(new Matrix4f(poses.last().pose()),(time+partial)*.09f))'
    require('PAD.draw(poses,buffers,PAD_TEX,1)' in render,'Lower disc lacks depth-write occlusion')
    require('UPPER_PAD.draw(poses,buffers,PAD_TEX,1)' in render,'Upper disc lacks depth-write occlusion')
    require(render.index(opaque)<render.index(queue),'Transparent scene transform was queued before native opaque depth')
    require(glazing not in render and alpha not in render,'Early block-entity alpha can be overwritten by a later End portal')
    require('RenderLevelStageEvent.Stage.AFTER_PARTICLES' in late,'Transparent scene does not wait for all native End-portal surfaces')
    require('RenderLevelStageEvent.Stage.AFTER_SKY){ALPHA_SCENES.clear();mainSceneSubmitted=false;return;}' in late and 'finally{ALPHA_SCENES.clear();}' in late,'Transparent queue can survive into a different frame')
    require('poses.last().pose().set(scene.pose())' in late,'Late alpha lost its captured camera-relative transform')
    require(late.index(glazing)<late.index(energy)<late.index(alpha),'Late glass/ribbon order changed')
    require(late.count(alpha)==1 and alpha not in late[late.index(glazing):late.index(energy)],'Tube walls and ribbons are split into separate transparent flushes')
    require('262144,false,mode!=1,' in renderer,'Native translucent RenderType no longer sorts quads on upload')
    require('.setWriteMaskState(mode==0?COLOR_WRITE:COLOR_DEPTH_WRITE)' in renderer,'Depth-write roles no longer match the discs and tubes')
    require('.setDepthTestState(LEQUAL_DEPTH_TEST)' in renderer,'Translucent tubes ignore opaque disc depth')
    require('m.tubeWalls(x,.35,z,.29,4.8,24,15,0x5968e8ed)' in renderer,'Tall unsplit glass quads undermine oblique alpha sorting')
    require('.setTransparencyState(mode!=1?TRANSLUCENT_TRANSPARENCY:NO_TRANSPARENCY)' in renderer,'Native scene blend states changed')
    require('m.cylinder(side*3.3,LobbyGeometry0252.PLINTH_BOTTOM,21.1,1.12,LobbyGeometry0252.PLINTH_TOP' in renderer,'Gate front plinth is disconnected from the tested floor height')
    require('PLINTH_BOTTOM=0,PLINTH_TOP=.34,STATUE_PLATFORM_TOP=.55' in geometry,'Gate plinth floats above the native floor')
    require('LobbyGeometry0252.statueHead(sx,1.66,sz-.03)' in renderer,'Tested three-dimensional head is not rendered')
    require('m.prism(new double[][]{{sx-.23,1.96}' not in renderer,'Blunt extruded head ridge returned')
    require(renderer.count('LobbyGeometry0252.urn(')==2,'Original neutral gate urns must use the tested open bowl mesh')
    require('m.geometry(LobbyGeometry0252.leaves(x,base,z,size)' in renderer,'Tested rim-clear leaves are not rendered')
    require('m.stem(stem,.018*size' in renderer and 'LobbyGeometry0252.flowers(x,base,z,size)' in renderer,'Visible blooms have no actual continuous stalk geometry')
    require('base+LobbyGeometry0252.SOIL_Y*size' in renderer,'The open urn has no filled soil surface')
    require('var flower=stem.bloom()' in renderer,'Blossoms are detached from tested stalk tips')
    require('GLASS_TEX,0' in render,'Transparent lobby floor changed to an opaque foundation')

def main(root,report=None):
    renderer=(root/RENDERER).read_text();geometry=(root/GEOMETRY).read_text()
    contract(renderer,geometry)
    negatives=[
      ('lower disc ignores depth','renderer','PAD.draw(poses,buffers,PAD_TEX,1)','PAD.draw(poses,buffers,PAD_TEX,0)'),
      ('early tube flush','renderer','GLAZING.draw(poses,buffers,WHITE,0);','GLAZING.draw(poses,buffers,WHITE,0);flush(buffers,Material.get(WHITE,0));'),
      ('early block-entity alpha','renderer','ALPHA_SCENES.add(new AlphaScene','GLAZING.draw(poses,buffers,WHITE,0);ALPHA_SCENES.add(new AlphaScene'),
      ('late alpha stage removed','renderer','Stage.AFTER_PARTICLES','Stage.AFTER_ENTITIES'),
      ('mutable camera transform','renderer','new Matrix4f(poses.last().pose())','poses.last().pose()'),
      ('stale frame queue','renderer','finally{ALPHA_SCENES.clear();}','finally{}'),
      ('unsorted alpha','renderer','262144,false,mode!=1,','262144,false,false,'),
      ('unsplit glass','renderer','m.tubeWalls(x,.35,z,.29,4.8,24,15,0x5968e8ed)','m.cylinder(x,.35,z,.29,4.8,24,0x5968e8ed)'),
      ('floating plinth','geometry','PLINTH_BOTTOM=0,','PLINTH_BOTTOM=.46,'),
      ('missing stalk','renderer','m.stem(stem,.018*size','m.ellipsoid(stem,.018*size'),
      ('detached head','renderer','LobbyGeometry0252.statueHead(sx,1.66,sz-.03)','legacyHead(sx,1.66,sz-.03)'),
    ]
    for name,kind,before,after in negatives:
        source=renderer if kind=='renderer' else geometry
        require(before in source,'Malformed fixture does not reach live source: '+name)
        try:contract(renderer.replace(before,after) if kind=='renderer' else renderer,geometry.replace(before,after) if kind=='geometry' else geometry)
        except (AssertionError,ValueError):pass
        else:raise AssertionError('Accepted malformed '+name)
    print(f'Lobby0252 renderer contract: {CHECKS} assertions; {len(negatives)} malformed regressions rejected. Numeric geometry is covered separately; native oblique review pending.')
    if report:
        paths=[RENDERER,GEOMETRY,TEST,'art/validate_lobby_0252.py']
        sources=[{'file':p,'sha256':hashlib.sha256((root/p).read_bytes()).hexdigest()} for p in paths]
        report.parent.mkdir(parents=True,exist_ok=True)
        report.write_text(json.dumps(dict(release='0.25.2',result='PASS',scope='Static renderer binding and pass-order contract only; no native visual signoff.',assertions=CHECKS,malformed_regressions_rejected=len(negatives),sources=sources),indent=2)+'\n')

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--root',type=Path,default=ROOT);p.add_argument('--report',type=Path)
    args=p.parse_args();main(args.root.resolve(),args.report)
