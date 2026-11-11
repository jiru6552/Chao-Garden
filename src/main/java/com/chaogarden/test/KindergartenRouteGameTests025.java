package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.hub.HubContent;
import com.chaogarden.kindergarten.KindergartenContent;
import com.chaogarden.kindergarten.KindergartenRooms025;
import com.chaogarden.kindergarten.KindergartenDecor025;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Walk an ordinary full-size player through the real school's six public routes and back. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class KindergartenRouteGameTests025 {
  private static Vec3 p(double x,double z){return new Vec3(x,80.01,z);}
  private static final Vec3 START=p(.5,1.5);
  private static final Vec3[][] ROUTES={
      {START,p(.5,-3.5),p(-12.5,-3.5),p(-12.5,-10.5),p(-17.5,-10.5),p(-17.5,-12)},
      {START,p(.5,-3.5),p(14.5,-3.5),p(14.5,-10.5),p(18.5,-10.5),p(18.5,-12.5)},
      {START,p(.5,-10.5),p(.5,-18.5)},
      {START,p(.5,5.5),p(19.5,5.5),p(20.5,5.5)},
      {START,p(.5,5.5),p(-18.5,5.5)},
      {START,p(.5,8.5),p(.5,19.5),p(.5,24.5)}
  };
  private static void walk(GameTestHelper h,ServerPlayer player,Vec3 target,int route){
    Vec3 origin=player.position(),delta=target.subtract(origin);int steps=Math.max(1,(int)Math.ceil(delta.length()/.2));
    for(int i=1;i<=steps;i++){
      Vec3 next=origin.add(delta.scale(i/(double)steps));
      // This is the real vanilla collision-resolving movement operation, not setPos/teleport or an eye-only camera sample.
      // Supply the ordinary downward movement that keeps a walking body grounded.
      // A horizontal-only frozen-Y fixture cannot auto-step over the metal cabinet's real one-eighth-block sill.
      player.move(MoverType.SELF,new Vec3(next.x-player.getX(),-.08,next.z-player.getZ()));
      h.assertTrue(Math.abs(player.getX()-next.x)<.0001&&Math.abs(player.getZ()-next.z)<.0001,"School route "+route+" blocked near "+next+"; actual body="+player.getBoundingBox()+" position="+player.position());
      // Walking off the sill briefly leaves the body above the floor while it descends.
      h.assertTrue(player.getY()>=79.99999&&player.getY()<=80.135,"School route "+route+" feet left the bounded floor/cabinet sill height: "+player.position());
      h.assertTrue(player.level().noCollision(player,player.getBoundingBox()),"School route "+route+" intersects an architectural or furniture collision at "+next);
      h.assertTrue(!player.level().getBlockState(net.minecraft.core.BlockPos.containing(next.x,79.9,next.z)).getCollisionShape(player.level(),net.minecraft.core.BlockPos.containing(next.x,79.9,next.z)).isEmpty(),"School route "+route+" has no supporting floor at "+next);
    }
    // All authored route waypoints lie on the level floor, away from the cabinet lip.
    // Finish the ordinary short descent through collision resolution and require real grounding.
    player.move(MoverType.SELF,new Vec3(0,-.16,0));
    h.assertTrue(player.onGround()&&Math.abs(player.getY()-80)<.00001,"School route "+route+" did not settle onto its waypoint floor: "+player.position());
    h.assertTrue(player.level().noCollision(player,player.getBoundingBox()),"School route "+route+" waypoint settle intersects solid scenery");
  }
  @GameTest(template="empty",timeoutTicks=80)
  public static void sixFoyerDoorsActuallyAdmitAFullSizePlayerToEachServiceAndTheCourtyard(GameTestHelper h){
    var player=TestConnections.makeSurvivalServerPlayer(h);var level=player.server.getLevel(KindergartenContent.DIMENSION);
    h.assertTrue(level!=null&&HubContent.ensure(level),"Actual school dimension failed to build");
    // Enter the actual realm before interacting. A remote mock player does not
    // hold the school's chunks; complete() correctly rejects unloaded door parts.
    player.teleportTo(level,START.x,START.y,START.z,0,0);player.hasChangedDimension();
    for(var pair:KindergartenRooms025.doorPairs())
      KindergartenDecor025.SchoolDoorBlock.pairStates(KindergartenDecor025.block(pair.id()),pair.center(),pair.facing(),false)
          .keySet().forEach(level::getChunkAt);
    try {
    // Walk only after normally interacting with every real closed pair. Opening never invokes a service menu.
    for(var pair:KindergartenRooms025.doorPairs()){
      var pos=pair.leftBase();var state=level.getBlockState(pos);
      h.assertTrue(state.getBlock() instanceof KindergartenDecor025.SchoolDoorBlock&&!state.getValue(KindergartenDecor025.SchoolDoorBlock.OPEN),"School pair must begin as a real closed door: "+pair);
      state.useWithoutItem(level,player,new BlockHitResult(pos.getCenter(),pair.facing(),pos,false));
      h.assertTrue(level.getBlockState(pos).getValue(KindergartenDecor025.SchoolDoorBlock.OPEN),"Normal interaction did not open school pair: "+pair);
    }
    player.teleportTo(level,START.x,START.y,START.z,0,0);player.hasChangedDimension();player.setNoGravity(false);player.setOnGround(true);
    h.assertTrue(!player.noPhysics&&player.getBbHeight()>1.7&&player.getBbWidth()>.5,"Walking proof must use a colliding full-size player body");
    for(int r=0;r<ROUTES.length;r++){
      var route=ROUTES[r];for(int i=1;i<route.length;i++)walk(h,player,route[i],r);
      for(int i=route.length-2;i>=0;i--)walk(h,player,route[i],r);
    }
    for(var pair:KindergartenRooms025.doorPairs()){
      var pos=pair.rightBase().above(2);level.getBlockState(pos).useWithoutItem(level,player,new BlockHitResult(pos.getCenter(),pair.facing(),pos,false));
      h.assertTrue(!level.getBlockState(pair.leftBase()).getValue(KindergartenDecor025.SchoolDoorBlock.OPEN),"Upper right leaf did not close the matching pair");
    }
    } finally {player.server.getPlayerList().remove(player);}
    h.succeed();
  }
}
