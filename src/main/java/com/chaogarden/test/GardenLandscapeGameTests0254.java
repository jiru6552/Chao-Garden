package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.PortalGeometry0251;
import com.chaogarden.hub.GardenRooms025;
import com.chaogarden.hub.HubContent;
import com.chaogarden.hub.LobbyDecor025;
import com.chaogarden.hub.PortalScenes0251;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Physical regressions for the requested west shift, mountain seam and exact tunnel throat. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class GardenLandscapeGameTests0254 {
  private GardenLandscapeGameTests0254(){}
  private static boolean field(int x,int z){return Math.pow(x/(x<0?39.0:24.0),4)+Math.pow((z+2)/20.0,4)<=1;}
  @GameTest(template="empty",batch="garden0254_landscape",timeoutTicks=100)
  public static void westExtendedGardenHasThreeClosedMountainsAndTwoDeepUnframedTunnel(GameTestHelper h){
    var level=h.getLevel().getServer().getLevel(HubContent.GARDEN);
    h.assertTrue(level!=null&&HubContent.ensure(level),"Neutral garden unavailable");
    int columns=0;
    for(int x=-40;x<=25;x++)for(int z=-23;z<=19;z++){
      h.assertTrue(GardenRooms025.insideField(x,z)==field(x,z),"Western extension moved an unchanged east/north/south field edge at "+x+","+z);
      if(field(x,z))columns++;
    }
    h.assertTrue(columns==2298&&GardenRooms025.POND_CENTER_X==-20&&GardenRooms025.POND_CENTER_Z==-8,"Fifteen-block west extension changed its exact footprint or receiving-pool centre");
    for(int z=-21;z<=17;z++){
      int west=-39,east=24;while(!field(west,z))west++;while(!field(east,z))east--;
      for(int edge:new int[]{west,east})for(int y=80;y<=86;y++){
        var at=new BlockPos(edge,y,z);var state=level.getBlockState(at);int crest=GardenRooms025.surface(edge,z);
        // The authored limestone wall has a full solid turf cap at its exact
        // crest. Grass at a lower cell, air or a partial collision is a hole.
        boolean material=state.is(LobbyDecor025.CLIFF.get())||state.is(com.chaogarden.hub.NeutralGardenDecor02510.CASCADE_ROCK.get())||y==crest&&state.is(LobbyDecor025.TURF.get());
        h.assertTrue(material&&Block.isShapeFullBlock(state.getCollisionShape(level,at)),"A western/eastern edge lacks its solid mountain column at "+at+" actual="+state+" crestY="+crest);
      }
    }
    // Independently inspect every column along the waterfall-to-north-mountain
    // join. A nominal bounding box can pass while leaving the visible slit open.
    for(int x=-37;x<=-26;x++)for(int z=-13;z<=-9;z++)for(int y=80;y<=90;y++)
      h.assertTrue(level.getBlockState(new BlockPos(x,y,z)).is(com.chaogarden.hub.NeutralGardenDecor02510.CASCADE_ROCK.get()),"Waterfall's northern shoulder has a visible seam at "+x+","+y+","+z);
    for(int y=81;y<=90;y++)h.assertTrue(level.getBlockState(new BlockPos(0,y,18)).isAir(),"Southern horizon was closed by visible mountain blocks");
    var anchor=level.getBlockState(GardenRooms025.EXIT_PORTAL);
    h.assertTrue(anchor.is(LobbyDecor025.SCENE_ANCHOR.get())&&anchor.getValue(LobbyDecor025.SceneAnchor.STYLE)==PortalScenes0251.TUNNEL,"Garden portal retained its quartz frame style");
    h.assertTrue(PortalGeometry0251.width(PortalScenes0251.TUNNEL)==3&&PortalGeometry0251.height(PortalScenes0251.TUNNEL)==4&&!PortalGeometry0251.hasFrame(PortalScenes0251.TUNNEL)&&PortalGeometry0251.aperture(PortalScenes0251.TUNNEL).size()==4,"Unframed portal surface is not rectangular3x4");
    for(int x=4;x<=6;x++)for(int z=-18;z<=-17;z++){
      for(int y=80;y<=82;y++){var at=new BlockPos(x,y,z);h.assertTrue(level.getBlockState(at).getCollisionShape(level,at).isEmpty(),"Three-wide/three-high tunnel clearance is blocked at "+at);}
      var top=new BlockPos(x,83,z);h.assertTrue(x==5?level.getBlockState(top).getCollisionShape(level,top).isEmpty():level.getBlockState(top).is(LobbyDecor025.CLIFF.get()),"Only the tunnel centre should be four blocks high at "+top);
      h.assertTrue(level.getBlockState(new BlockPos(x,84,z)).is(LobbyDecor025.CLIFF.get()),"Tunnel has no solid ceiling");
    }
    for(int x=4;x<=6;x++)for(int y=80;y<=83;y++){
      h.assertTrue(level.getBlockState(new BlockPos(x,y,-19)).is(LobbyDecor025.CLIFF.get()),"Tunnel has no solid back cap");
      var approach=new BlockPos(x,y,-16);h.assertTrue(level.getBlockState(approach).getCollisionShape(level,approach).isEmpty(),"Tunnel remains deeper than two blocks");
    }
    for(var at:BlockPos.betweenClosed(3,79,-19,7,84,-16))h.assertTrue(!level.getBlockState(at).is(Blocks.QUARTZ_BLOCK)&&!level.getBlockState(at).is(Blocks.QUARTZ_PILLAR),"Unrequested quartz frame/threshold remains at "+at);
    com.mojang.logging.LogUtils.getLogger().info("CHAO_LANDSCAPE0254 westShift=15 mask={} pondCenter=-20,-8 westNorthJoin=solid southHorizon=open tunnel=3x3+centre4 depth=2 portal=3x4 unframed=true",columns);
    h.succeed();
  }
}
