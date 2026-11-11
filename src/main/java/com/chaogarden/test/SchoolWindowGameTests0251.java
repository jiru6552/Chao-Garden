package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.kindergarten.KindergartenDecor025;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The reusable2x2 casement must collide where its actual handed wall opening is rendered. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class SchoolWindowGameTests0251 {
  @GameTest(template="activity_test",batch="kindergarten_doors0251",timeoutTicks=100)
  public static void everyWindowFacingMatchesItsVisibleApertureAndDoesNotLeavePhantomGlass(GameTestHelper h){
    var level=h.getLevel();var at=h.absolutePos(new BlockPos(6,1,6));
    for(int x=2;x<=12;x++)for(int z=2;z<=12;z++){
      h.setBlock(new BlockPos(x,0,z),Blocks.STONE);for(int y=1;y<=4;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
    }
    var player=TestConnections.makeSurvivalServerPlayer(h);player.setNoGravity(true);
    try{
      for(Direction face:Direction.Plane.HORIZONTAL){
        var state=KindergartenDecor025.state("school_round_window",face);level.setBlock(at,state,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
        AABB expected=switch(face){
          case NORTH->new AABB(-1,0,0,1,2,1);case EAST->new AABB(0,0,-1,1,2,1);case WEST->new AABB(0,0,0,1,2,2);default->new AABB(0,0,0,2,2,1);
        };
        h.assertTrue(state.getCollisionShape(level,at).bounds().equals(expected),"Window collision does not rotate with its native two-by-two mesh: "+face);
        Vec3 normal=new Vec3(face.getStepX(),0,face.getStepZ());
        // NORTH/EAST deliberately sample the negative half of the real opening. The old unrotated shape missed it.
        Vec3 inside=switch(face){case NORTH->new Vec3(-.5,.01,.5);case EAST->new Vec3(.5,.01,-.5);case WEST->new Vec3(.5,.01,1.5);default->new Vec3(1.5,.01,.5);};
        Vec3 center=Vec3.atLowerCornerOf(at).add(inside),start=center.subtract(normal.scale(1.2));
        player.teleportTo(level,start.x,start.y,start.z,0,0);player.setDeltaMovement(Vec3.ZERO);
        player.move(MoverType.SELF,normal.scale(2.4));
        h.assertTrue(player.position().subtract(center).dot(normal)<.2,"A player walked through the visible "+face+" window glass");
        Vec3 outside=switch(face){case NORTH->new Vec3(1.5,.01,.5);case EAST->new Vec3(.5,.01,1.5);case WEST->new Vec3(.5,.01,-.5);default->new Vec3(-.5,.01,.5);};
        center=Vec3.atLowerCornerOf(at).add(outside);start=center.subtract(normal.scale(1.2));
        player.teleportTo(level,start.x,start.y,start.z,0,0);player.setDeltaMovement(Vec3.ZERO);
        player.move(MoverType.SELF,normal.scale(2.4));
        h.assertTrue(player.position().subtract(center).dot(normal)>1,"An invisible extension of the "+face+" window blocks the adjacent clear floor");
      }
    }finally{level.setBlock(at,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);player.server.getPlayerList().remove(player);}
    h.succeed();
  }
}
