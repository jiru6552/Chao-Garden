package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.LobbyApproachGeometry0255;
import com.chaogarden.core.GardenGateGeometry0256;
import com.chaogarden.hub.HubContent;
import com.chaogarden.hub.HubRooms;
import com.chaogarden.hub.LobbyApproachDecor0255;
import com.chaogarden.hub.LobbyDecor025;
import com.chaogarden.hub.PortalScenes0251;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real curved stair collision and every joined lower/upper perimeter guard. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class LobbyApproachGameTests0255 {
  private static ServerLevel hub(GameTestHelper h){var level=h.getLevel().getServer().getLevel(HubContent.HUB);h.assertTrue(level!=null&&HubContent.ensure(level),"Authored lobby unavailable");return level;}
  private static void rails(GameTestHelper h,ServerLevel level,boolean hero,boolean upper){
    for(var rail:LobbyApproachGeometry0255.rails(hero,upper)){
      var a=rail.edge().a();var b=rail.edge().b();double dx=b.x()-a.x(),dz=b.z()-a.z(),length=Math.hypot(dx,dz);
      for(double t:new double[]{.03,.5,.97}){
        double x=.5+a.x()+dx*t,z=.5+a.z()+dz*t,y=rail.y0()+(rail.y1()-rail.y0())*t+.6,nx=-dz/length*.35,nz=dx/length*.35;
        var hit=level.clip(new ClipContext(new Vec3(x-nx,y,z-nz),new Vec3(x+nx,y,z+nz),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,net.minecraft.world.phys.shapes.CollisionContext.empty()));
        h.assertTrue(hit.getType()==HitResult.Type.BLOCK,"Open railing edge/turn at "+x+","+y+","+z);
        if(!hero&&!upper){
          var low=level.clip(new ClipContext(new Vec3(x-nx,y-1,z-nz),new Vec3(x+nx,y-1,z+nz),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,net.minecraft.world.phys.shapes.CollisionContext.empty()));
          h.assertTrue(low.getType()==HitResult.Type.BLOCK,"Descending glass skirt leaves a gap under the rail at "+x+","+(y-1)+","+z);
        }
      }
    }
  }
  @GameTest(template="empty",batch="lobby0255_approaches",timeoutTicks=180)
  public static void realFlightsUseCelestialStairsAndAnOpenLowerFloorWithoutQuartzOrClosedDarkPlaceholder(GameTestHelper h){
    var level=hub(h);
    h.assertTrue(HubRooms.HERO_DOOR.getZ()==-12&&HubRooms.HERO_FACING==net.minecraft.core.Direction.SOUTH,
        "Hero gate did not move to the north landing and face its reversed approach");
    var heroStart=LobbyApproachGeometry0255.centre(true,LobbyApproachGeometry0255.START_ANGLE,80);
    var heroEnd=LobbyApproachGeometry0255.centre(true,LobbyApproachGeometry0255.END_ANGLE,89);
    h.assertTrue(heroStart.z()>0&&heroEnd.z()<0,"Hero stairs still climb in their old north-to-south direction");
    h.assertTrue(GardenGateGeometry0256.darkFlames().size()==2,"Dark gate needs two live flame sources");
    for(var local:GardenGateGeometry0256.darkFlames()){
      var flame=HubRooms.gateWorldPoint(HubRooms.DARK_DOOR,HubRooms.DARK_FACING,local);
      h.assertTrue(flame.z<HubRooms.DARK_DOOR.getZ()+.5&&Math.abs(flame.y-(HubRooms.DARK_DOOR.getY()+2.78))<1e-8,
          "Native flame source drifted away from the front-facing gate cup");
      var state=level.getBlockState(BlockPos.containing(flame));
      h.assertTrue(!state.is(Blocks.FIRE)&&!state.is(Blocks.SOUL_FIRE),"Decorative gate fire should not create a damaging fire block");
    }
    for(var cell:LobbyApproachGeometry0255.nativeCells().keySet()){
      var at=new BlockPos(cell.x(),cell.y(),cell.z());var state=level.getBlockState(at);
      // The active portal anchor and its masonry guards intentionally replace
      // cells at the gate; all floors and exposed rail runs remain exact.
      if(at.equals(HubRooms.HERO_DOOR)||at.equals(HubRooms.DARK_DOOR)||state.is(Blocks.BARRIER))continue;
      h.assertTrue(state.is(LobbyApproachDecor0255.FLOOR_STAIRS.get())&&state.getRenderShape()==RenderShape.INVISIBLE,"Curved approach native cell missing at "+at);
      h.assertTrue(!state.getCollisionShape(level,at).isEmpty(),"Scene tread or guard has empty native collision at "+at);
    }
    for(boolean hero:new boolean[]{true,false}){rails(h,level,hero,false);if(!hero)rails(h,level,false,true);}
    for(int x:new int[]{-18,-12,12,18})h.assertTrue(level.getBlockState(new BlockPos(x,78,7)).isAir(),"Old central staircase still has blocks beneath the lobby floor");
    var hero=level.getBlockState(HubRooms.HERO_DOOR);var dark=level.getBlockState(HubRooms.DARK_DOOR);
    h.assertTrue(hero.getValue(LobbyDecor025.SceneAnchor.STYLE)==PortalScenes0251.HERO&&dark.getValue(LobbyDecor025.SceneAnchor.STYLE)==PortalScenes0251.DARK,"Garden gate route/style is inactive");
    h.assertTrue(HubRooms.MIN_VISITOR_Y<HubRooms.DARK_RETURN_LANDING.y,"Lower garden landing lies outside the visitor interval");h.succeed();
  }
  private static void move(GameTestHelper h,ServerPlayer player,ServerLevel level,double x,double z,String context){
    double dx=x-player.getX(),dz=z-player.getZ(),length=Math.hypot(dx,dz);int count=Math.max(1,(int)Math.ceil(length/.10));
    dx/=count;dz/=count;
    for(int i=0;i<count;i++){
      var before=player.position();player.move(MoverType.SELF,new Vec3(dx,-.12,dz));var body=player.getBoundingBox().deflate(1e-5);
      if(!level.noCollision(player,body)){
        var blockers=new StringBuilder();
        for(var at:BlockPos.betweenClosed(BlockPos.containing(body.minX,body.minY,body.minZ),BlockPos.containing(body.maxX,body.maxY,body.maxZ))){
          var state=level.getBlockState(at);
          for(var box:state.getCollisionShape(level,at,net.minecraft.world.phys.shapes.CollisionContext.of(player)).toAabbs()){
            var world=box.move(at);if(world.intersects(body))blockers.append(at).append(':').append(state).append(':').append(world).append(';');
          }
        }
        var entities=level.getEntities(player,body).stream().map(e->e.getClass().getSimpleName()+"@"+e.position()).toList();
        var diagnostic="context="+context+" substep="+i+" before="+before+" after="+player.position()+" delta="+dx+",-.12,"+dz+" target="+x+","+z+" body="+body+" blocks="+blockers+" entities="+entities
            +" noPhysics="+player.noPhysics+" actualLevel="+player.level().dimension().location()+" expectedLevel="+level.dimension().location()
            +" removed="+player.isRemoved()+" changingDimension="+player.isChangingDimension()+" chunkPresent="+level.hasChunkAt(BlockPos.containing(before));
        com.mojang.logging.LogUtils.getLogger().error("CHAO_APPROACH0257_COLLISION {}",diagnostic);
        h.assertTrue(false,"Keeper clipped a curved tread or joined rail "+context+" at="+player.position()+" noPhysics="+player.noPhysics+" level="+player.level().dimension().location());
      }
    }
    h.assertTrue(Math.hypot(player.getX()-x,player.getZ()-z)<.035,"Keeper hit an obstruction on the real perimeter route at "+player.position()+" target="+x+","+z);
  }
  private static void walk(GameTestHelper h,ServerPlayer player,ServerLevel level,boolean hero,boolean reverse){
    double begin=LobbyApproachGeometry0255.START_ANGLE-.025,end=LobbyApproachGeometry0255.END_ANGLE+.015;
    double a=reverse?end:begin;var first=LobbyApproachGeometry0255.centre(hero,a,0);double y=reverse?(hero?89.05:71.05):80.05;
    player.teleportTo(level,first.x()+.5,y,first.z()+.5,0,0);player.hasChangedDimension();player.setNoGravity(true);
    int samples=360;
    for(int i=1;i<=samples;i++){
      double t=(double)i/samples,angle=reverse?end+(begin-end)*t:begin+(end-begin)*t;var next=LobbyApproachGeometry0255.centre(hero,angle,0);
      move(h,player,level,next.x()+.5,next.z()+.5,"hero="+hero+" reverse="+reverse+" sample="+i+" angle="+angle);
    }
    double expected=reverse?80:hero?89:71;
    // A horizontal endpoint may be reached on the tick that the keeper leaves
    // its last half-step. Let native downward movement finish the landing;
    // never set Y, onGround or a synthetic floor result in the fixture.
    var endpoint=player.position();
    for(int i=0;i<20&&Math.abs(player.getY()-expected)>=.06;i++){
      player.move(MoverType.SELF,new Vec3(0,-.12,0));
      h.assertTrue(level.noCollision(player,player.getBoundingBox().deflate(1e-5)),"Keeper clipped the landing while settling after its curved walk");
      h.assertTrue(Math.hypot(player.getX()-endpoint.x,player.getZ()-endpoint.z)<1e-8,"Settling shifted the keeper off its real route endpoint");
    }
    h.assertTrue(Math.abs(player.getY()-expected)<.06,"Keeper failed the native curved half-step flight: "+player.position());
    if(!reverse){var landing=hero?HubRooms.HERO_RETURN_LANDING:HubRooms.DARK_RETURN_LANDING;move(h,player,level,landing.x,landing.z,"hero="+hero+" reverse="+reverse+" landing");h.assertTrue(Math.abs(player.getY()-landing.y+.1)<.07,"Gate return point lacks a walkable landing");}
  }
  @GameTest(template="empty",batch="lobby0255_approaches",timeoutTicks=180)
  public static void aRealKeeperWalksUpAndDownBothFlightsAndReachesTheTwoElevatedGardenLandings(GameTestHelper h){
    var level=hub(h);var player=TestConnections.makeSurvivalServerPlayer(h);
    boolean[] done={false};Runnable cleanup=()->{if(!done[0]){done[0]=true;player.server.getPlayerList().remove(player);}};h.runAtTickTime(179,cleanup);
    var start=LobbyApproachGeometry0255.centre(true,LobbyApproachGeometry0255.START_ANGLE-.025,0);
    player.teleportTo(level,start.x()+.5,80.05,start.z()+.5,0,0);player.hasChangedDimension();player.setNoGravity(true);
    int joinedAt=player.tickCount;
    var chunks=new java.util.LinkedHashSet<net.minecraft.world.level.ChunkPos>();
    for(var cell:LobbyApproachGeometry0255.nativeCells().keySet())chunks.add(new net.minecraft.world.level.ChunkPos(new BlockPos(cell.x(),cell.y(),cell.z())));
    // A fresh cross-dimension fixture may read authored blocks synchronously
    // before those chunks are published to native collision queries. Wait for
    // the real player tick and the collision getter, not merely hasChunkAt.
    h.startSequence().thenWaitUntil(()->{
      h.assertTrue(player.tickCount>joinedAt&&player.serverLevel()==level&&!player.isRemoved()&&!player.noPhysics&&!player.isChangingDimension(),
          "Perimeter walking fixture awaits an ordinary colliding Hub player tick");
      for(var chunk:chunks)h.assertTrue(level.getChunkForCollisions(chunk.x,chunk.z)!=null,"Perimeter collision chunk is not published yet: "+chunk);
      h.assertTrue(level.getBlockCollisions(player,player.getBoundingBox().move(0,-.12,0)).iterator().hasNext(),
          "Initial lobby floor is not visible through native block collision queries yet");
    }).thenExecute(()->{
      try{for(boolean hero:new boolean[]{true,false}){walk(h,player,level,hero,false);walk(h,player,level,hero,true);}}finally{cleanup.run();}
    }).thenSucceed();
  }
  private LobbyApproachGameTests0255(){}
}
