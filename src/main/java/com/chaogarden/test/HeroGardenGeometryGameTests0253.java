package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.activities.ActivityContent;
import com.chaogarden.core.HeroCloudGeometry0253;
import com.chaogarden.fruit.FruitContent;
import com.chaogarden.fruit.HangingFruitBlock;
import com.chaogarden.hub.HeroGardenDecor0253;
import com.chaogarden.hub.HeroGardenRooms0253;
import com.chaogarden.hub.HubContent;
import com.chaogarden.hub.LobbyDecor025;
import com.chaogarden.hub.PortalScenes0251;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Actual authored terrain, fluid ticking, structures and safe horizon regressions. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class HeroGardenGeometryGameTests0253 {
  private HeroGardenGeometryGameTests0253(){}
  private static ServerLevel hero(GameTestHelper h){
    var level=h.getLevel().getServer().getLevel(HubContent.HERO_GARDEN);
    h.assertTrue(level!=null&&HubContent.ensure(level),"Hero dimension could not prepare its playable scene");return level;
  }
  private static void source(GameTestHelper h,ServerLevel level,BlockPos at){
    var fluid=level.getFluidState(at);h.assertTrue(fluid.is(Fluids.WATER)&&fluid.isSource(),"Hero pool has a void or partial water cell at "+at);
  }
  @GameTest(template="empty",batch="hero0253_geometry",timeoutTicks=350)
  public static void heroPoolFountainAndFourColumnPavilionRemainActualSupportedStructures(GameTestHelper h){
    var level=hero(h);var forced=new ArrayList<net.minecraft.world.level.ChunkPos>();boolean[] done={false};
    Runnable cleanup=()->{if(done[0])return;done[0]=true;for(var chunk:forced)level.setChunkForced(chunk.x,chunk.z,false);};
    h.runAtTickTime(349,cleanup);
    for(int cx=-2;cx<=1;cx++)for(int cz=-3;cz<=1;cz++)if(!level.getForcedChunks().contains(net.minecraft.world.level.ChunkPos.asLong(cx,cz))){level.setChunkForced(cx,cz,true);forced.add(new net.minecraft.world.level.ChunkPos(cx,cz));}
    h.startSequence().thenWaitUntil(()->h.assertTrue(level.isPositionEntityTicking(HeroGardenRooms0253.FOUNTAIN_EMBLEM)&&level.isPositionEntityTicking(HeroGardenRooms0253.BELL_ISLAND),"Hero fixture awaits genuinely ticking landmark chunks"))
      .thenIdle(100).thenExecute(()->{try{
        int filled=0,covered=0;
        // Independent expected basin and exact three solid shore-pier columns:
        // the raised pavilion deliberately covers this corner of the ellipse.
        var pier=java.util.Set.of(new BlockPos(-12,79,6),new BlockPos(-12,79,7),new BlockPos(-11,79,6));
        for(int x=-23;x<=-7;x++)for(int z=-14;z<=8;z++)if(Math.pow((x+15)/8.0,2)+Math.pow((z+3)/11.0,2)<1){
          var surface=new BlockPos(x,79,z);source(h,level,new BlockPos(x,78,z));
          h.assertTrue(level.getBlockState(new BlockPos(x,77,z)).is(HeroGardenDecor0253.block("hero_marble")),"Hero pool has no continuous solid basin");
          if(pier.contains(surface)){
            covered++;h.assertTrue(!HeroGardenRooms0253.poolCell(x,z),"Open water predicate includes the solid pavilion pier");
            for(int y=79;y<=81;y++){var support=new BlockPos(x,y,z);var state=level.getBlockState(support);
              h.assertTrue(state.is(HeroGardenDecor0253.block("hero_marble"))&&state.isFaceSturdy(level,support,Direction.UP),"Pavilion shore pier was flooded or left hollow at "+support);}
          }else{filled++;h.assertTrue(HeroGardenRooms0253.poolCell(x,z),"Open water predicate omits a real pond cell");source(h,level,surface);}
        }
        h.assertTrue(filled==268&&covered==3,"Hero pond lost its exact wet surface/solid shore-pier footprint: "+filled+" water / "+covered+" pier");
        for(int y=79;y<=83;y++)for(int z=-6;z<=-2;z++)source(h,level,new BlockPos(-21,y,z));
        h.assertTrue(level.getBlockState(HeroGardenRooms0253.FOUNTAIN_EMBLEM).is(HeroGardenDecor0253.block("hero_fountain_emblem")),"Hero's cyan fountain emblem disappeared from the west temple");
        for(int x:new int[]{-11,-5})for(int z:new int[]{7,13}){
          for(int y=84;y<=87;y++)h.assertTrue(level.getBlockState(new BlockPos(x,y,z)).is(HeroGardenDecor0253.block("hero_column")),"Pavilion column has a gap");
          for(int y:new int[]{82,89,90})h.assertTrue(level.getBlockState(new BlockPos(x,y,z)).isFaceSturdy(level,new BlockPos(x,y,z),Direction.UP),"Pavilion column does not meet its plinth or roof at "+x+","+y+","+z);
        }
        for(int x=-10;x<=-6;x++)for(int[] step:new int[][]{{80,16},{81,15},{82,14}}){
          var state=level.getBlockState(new BlockPos(x,step[0],step[1]));
          h.assertTrue(state.is(Blocks.QUARTZ_STAIRS)&&state.getValue(StairBlock.FACING)==Direction.NORTH,"Pavilion needs three properly facing half-height entry stairs");
        }
        var bell=new BlockPos(-9,84,-38);var bellState=level.getBlockState(bell);
        h.assertTrue(bellState.is(Blocks.BELL)&&bellState.getValue(BellBlock.ATTACHMENT)==BellAttachType.CEILING&&bellState.canSurvive(level,bell),"Detached bell-island bell floats without a real ceiling attachment");
        var exit=level.getBlockState(HeroGardenRooms0253.EXIT_PORTAL);
        h.assertTrue(exit.is(LobbyDecor025.SCENE_ANCHOR.get())&&exit.getValue(LobbyDecor025.SceneAnchor.PORTAL)
            &&exit.getValue(LobbyDecor025.SceneAnchor.STYLE)==PortalScenes0251.RETURN&&exit.getValue(LobbyDecor025.SceneAnchor.FACING)==Direction.WEST,"East exit needs the real west-facing return portal");
        h.assertTrue(HeroGardenRooms0253.EXIT_PORTAL.getX()>20&&HeroGardenRooms0253.FOUNTAIN_EMBLEM.getX()<-20,"Hero fountain and exit must occupy distinct rock flanks");
        for(var at:BlockPos.betweenClosed(-10,81,-24,10,125,-24))h.assertTrue(level.getBlockState(at).isAir(),"The northern cloud gap was sealed with a visible wall");
        for(var at:BlockPos.betweenClosed(-10,110,-10,10,110,10))h.assertTrue(level.getBlockState(at).isAir(),"Hero lawn acquired a visible ceiling");
        h.assertTrue(!HeroCloudGeometry0253.vertices().isEmpty()&&HeroCloudGeometry0253.vertices().stream().allMatch(v->v.y()<79),"Cloud sea must remain below the playable garden, never a roof");
        cleanup.run();h.succeed();
      }catch(RuntimeException failure){cleanup.run();throw failure;}});
  }

  // Chunk generation/entity publication is asynchronous. Give startup its own
  // bounded allowance; every terrain, collision and care assertion stays intact.
  @GameTest(template="empty",batch="hero0253_route",timeoutTicks=460)
  public static void heroLandingLawnAndCloudEdgeSupportRealBodiesWithoutBlockingCare(GameTestHelper h){
    var level=hero(h);var keeper=TestConnections.makeSurvivalServerPlayer(h);keeper.setNoGravity(true);
    var child=ChaoGarden.CHAO.get().create(level);child.setNoAi(true);child.setNoGravity(true);boolean[] done={false};
    var childAt=new BlockPos(5,80,-22);var childChunk=new net.minecraft.world.level.ChunkPos(childAt);
    boolean leaseChildChunk=!level.getForcedChunks().contains(childChunk.toLong());boolean[] tickingReady={false},publicationObserved={false};
    if(leaseChildChunk)level.setChunkForced(childChunk.x,childChunk.z,true);
    Runnable cleanup=()->{if(done[0])return;done[0]=true;child.discard();if(leaseChildChunk)level.setChunkForced(childChunk.x,childChunk.z,false);if(HubContent.hasReturnJournal(keeper))HubContent.returnHome(keeper);keeper.server.getPlayerList().remove(keeper);};h.runAtTickTime(459,cleanup);
    h.runAtTickTime(400,()->{if(!done[0]&&!publicationObserved[0]){var ticking=level.isPositionEntityTicking(childAt);var tracked=level.getEntity(child.getUUID())==child;cleanup.run();h.fail("Hero edge fixture deadline: chunk="+childChunk+" tickingReadyObserved="+tickingReady[0]+" currentlyTicking="+ticking+" sameUuidTracked="+tracked+"; expected ready chunk before native publication");}});
    try{
      keeper.teleportTo(level,18.5,80.1,8.5,115,0);child.moveTo(5.5,80,-21.5,0,0);
      h.assertTrue(level.noCollision(keeper)&&level.getBlockState(keeper.blockPosition().below()).is(LobbyDecor025.TURF.get()),"Hero arrival is inside terrain or missing its dry floor");
      keeper.move(MoverType.SELF,new Vec3(-5,0,0));
      h.assertTrue(Math.abs(keeper.getX()-13.5)<.001&&level.noCollision(keeper)&&level.getBlockState(keeper.blockPosition().below()).is(LobbyDecor025.TURF.get()),"Hero's first five metres are obstructed or unsupported");
      for(int x=5;x<=18;x++)for(int z=-17;z<=8;z++)if((x==5||z==8)&&!level.getBlockState(new BlockPos(x,80,z)).is(LobbyDecor025.PALM_TRUNK.get())){
        var at=new BlockPos(x,79,z);h.assertTrue(level.getBlockState(at).isFaceSturdy(level,at,Direction.UP),"Broad Hero lawn route crosses a floor hole");
      }
      h.startSequence().thenWaitUntil(()->h.assertTrue(level.isPositionEntityTicking(childAt),"Hero edge fixture awaits its leased entity-ticking chunk "+childChunk))
        .thenExecute(()->{try{tickingReady[0]=true;h.assertTrue(level.addFreshEntity(child),"Hero edge body was not published into its ready chunk");}catch(RuntimeException failure){cleanup.run();throw failure;}})
        .thenWaitUntil(()->h.assertTrue(level.getEntity(child.getUUID())==child,"Hero edge fixture awaits same-UUID native publication in leased chunk "+childChunk))
        .thenExecute(()->{try{
          publicationObserved[0]=true;
          com.mojang.logging.LogUtils.getLogger().info("CHAO_HERO_EDGE_READY testTick={} chunk={}",h.getTick(),childChunk);
          child.move(MoverType.SELF,new Vec3(0,0,-12));
          h.assertTrue(child.getZ()>-26&&HeroGardenRooms0253.insideField(child.blockPosition().getX(),child.blockPosition().getZ()),"A Chao could fall through the open northern horizon");
          h.assertTrue(level.getBlockState(new BlockPos(5,120,-26)).is(Blocks.BARRIER),"Invisible cliff containment can be jumped or thrown over");
          var transporter=level.getBlockState(HeroGardenRooms0253.TRANSPORTER);
          h.assertTrue(transporter.is(ActivityContent.GARDEN.get())&&!HeroGardenRooms0253.breakable(HeroGardenRooms0253.TRANSPORTER,transporter),"Authored Hero transporter is missing or breakable");
          for(var tree:HeroGardenRooms0253.PALMS)for(Direction facing:Direction.Plane.HORIZONTAL){
            var pod=new BlockPos(tree[0],HeroGardenRooms0253.surface(tree[0],tree[1])+tree[2],tree[1]).relative(facing);var state=level.getBlockState(pod);
            h.assertTrue(state.is(FruitContent.podForFruit("garden_fruit"))&&state.getValue(HangingFruitBlock.AGE)==3&&state.canSurvive(level,pod)&&!HeroGardenRooms0253.breakable(pod,state),"Hero native fruit pod is unsupported or mutable as bought care furniture");
          }
          int rawLobbyAnchors=0;for(var state:HeroGardenRooms0253.plan().blocks.values())if(state.is(LobbyDecor025.SCENE_ANCHOR.get())&&!state.getValue(LobbyDecor025.SceneAnchor.PORTAL))rawLobbyAnchors++;
          h.assertTrue(rawLobbyAnchors==0,"Hero contains a full-lobby renderer anchor instead of its own garden");
          cleanup.run();h.succeed();
        }catch(RuntimeException failure){cleanup.run();throw failure;}});
    }catch(RuntimeException failure){cleanup.run();throw failure;}
  }
}
