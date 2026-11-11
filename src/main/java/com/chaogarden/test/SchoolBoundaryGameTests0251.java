package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.hub.HubContent;
import com.chaogarden.kindergarten.KindergartenContent;
import com.chaogarden.kindergarten.KindergartenDecor025;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** A real full-size Adventure body must not leave the public approach through the porch/hedge gap. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class SchoolBoundaryGameTests0251 {
  private static Vec3 at(double x,double z){return new Vec3(x,80.01,z);}
  private static boolean move(ServerPlayer p,Vec3 target){
    Vec3 start=p.position(),delta=target.subtract(start);int steps=Math.max(1,(int)Math.ceil(delta.length()/.15));
    for(int i=1;i<=steps;i++){
      Vec3 next=start.add(delta.scale(i/(double)steps));p.move(MoverType.SELF,next.subtract(p.position()));
      if(p.position().distanceToSqr(next)>.0001)return false;
    }
    return true;
  }
  @GameTest(template="empty",batch="kindergarten_boundaries0251",timeoutTicks=120)
  public static void publicApproachStaysOpenButPorchCannotBypassEitherHedgeBoundary(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);var level=p.server.getLevel(KindergartenContent.DIMENSION);
    h.assertTrue(level!=null&&HubContent.ensure(level),"Actual school dimension was not built");
    try{
      p.gameMode.changeGameModeForPlayer(GameType.ADVENTURE);p.setNoGravity(true);
      h.assertTrue(!p.noPhysics&&p.getBbHeight()>1.7&&p.getBbWidth()>.5,"Boundary proof requires an ordinary full-size colliding Adventure player");
      for(int sign:new int[]{1,-1}){
        Vec3 entry=at(.5,40.5);p.teleportTo(level,entry.x,entry.y,entry.z,0,0);p.hasChangedDimension();p.setDeltaMovement(Vec3.ZERO);
        h.assertTrue(move(p,at(.5,19.5)),"Main public approach became blocked before the school entrance");
        double publicX=.5+sign*4;
        h.assertTrue(move(p,at(publicX,19.5))&&move(p,at(publicX,20.5)),"Public side of the front porch became inaccessible");
        // Atz19 a pillar stands atx6; the original bypass sidesteps it by one block,
        // then walks UNDER the barrier's formerY82 bottom at the hedge's openz20 end.
        boolean crossedBoundary=move(p,at(sign*7.5,20.5));
        boolean reachedSide=false;
        if(crossedBoundary){
          // Flowerboxes occupyz20 atx10..13. This actual bypass first goes around
          // those decorations, so a later flowerbox collision cannot masquerade as a good boundary.
          h.assertTrue(move(p,at(sign*7.5,22.5))&&move(p,at(sign*29.5,22.5))&&move(p,at(sign*29.5,10.5)),"Fixture bypass unexpectedly hit unrelated school scenery after already crossing the real hedge boundary: "+p.position());
          reachedSide=true;
        }
        h.assertTrue(!crossedBoundary&&!reachedSide,"A full-size Adventure body bypassed the porch/hedge boundary and reached the building side: sign="+sign+" actual="+p.position());
        h.assertTrue(sign>0?p.getBoundingBox().maxX<=5.0001:p.getBoundingBox().minX>=-4.0001,"Another obstacle beyond the hedge boundary gave a false boundary success");
        h.assertTrue(move(p,at(publicX,20.5))&&move(p,at(.5,20.5))&&move(p,entry),"Boundary trapped the player or closed the return approach");
      }
      // The ordinary return aperture remains reachable before its invisible rear end wall.
      Vec3 entry=at(.5,40.5);p.teleportTo(level,entry.x,entry.y,entry.z,0,0);p.setDeltaMovement(Vec3.ZERO);
      h.assertTrue(move(p,at(.5,49.5))&&move(p,at(.5,50.5)),"Boundary obstructed the actual return portal approach");
      for(int sign:new int[]{1,-1}){
        h.assertTrue(!move(p,at(sign*7.5,50.5)),"A player walked around the far end of the hedge wall beside the return portal");
        h.assertTrue(sign>0?p.getBoundingBox().maxX<=5.0001:p.getBoundingBox().minX>=-4.0001,"Far-end protection stopped at an unrelated outside obstacle");
        h.assertTrue(move(p,at(.5,50.5)),"Far-end side wall trapped the return approach");
      }
      h.assertTrue(!move(p,at(.5,53.5))&&p.getBoundingBox().maxZ<=51.0001,"A player walked behind the return-end closure");
      // Initial positions are inside the protected volume. Every attempted crossing is real collision-resolving movement.
      p.teleportTo(level,.5,100,30.5,0,0);p.setDeltaMovement(Vec3.ZERO);
      h.assertTrue(!move(p,new Vec3(.5,104,30.5))&&p.getBoundingBox().maxY<=102.0001,"A full-size body flew through the invisible approach cap");
      p.teleportTo(level,.5,96,19.5,0,0);p.setDeltaMovement(Vec3.ZERO);
      h.assertTrue(!move(p,new Vec3(.5,96,15.5))&&p.getBoundingBox().minZ>=18.9999,"A full-size body flew over the front gable out of the approach volume");
      for(int sign:new int[]{-1,1}){
        var leaf=level.getBlockState(new BlockPos(sign*5,80,30));
        h.assertTrue(leaf.is(Blocks.OAK_LEAVES)&&leaf.getValue(LeavesBlock.PERSISTENT),"Boundary replaced an actual persistent hedge");
        for(int y=80;y<=84;y++)h.assertTrue(level.getBlockState(new BlockPos(sign*6,y,19)).is(KindergartenDecor025.block("school_twisted_column")),"Boundary replaced a porch pillar or ceiling connection");
      }
    }finally{if(HubContent.hasReturnJournal(p))HubContent.returnHome(p);p.server.getPlayerList().remove(p);}
    h.succeed();
  }
}
