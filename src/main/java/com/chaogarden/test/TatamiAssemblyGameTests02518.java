package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.activities.ActivityContent;
import com.chaogarden.activities.ActivityManager;
import com.chaogarden.activities.KarateMatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class TatamiAssemblyGameTests02518 {
  private static BlockPos floor(GameTestHelper h){
    var center=h.absolutePos(new BlockPos(10,1,10));var level=h.getLevel();
    for(var p:BlockPos.betweenClosed(center.offset(-4,-1,-4),center.offset(11,2,4)))
      level.setBlock(p,p.getY()==center.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
    return center;
  }
  private static void place(GameTestHelper h,ServerPlayer player,BlockPos center){
    player.setPos(center.getX()+5,center.getY(),center.getZ()+5);
    var item=new ItemStack(ChaoGarden.item("karate_mat"));player.setItemInHand(InteractionHand.MAIN_HAND,item);
    var hit=new BlockHitResult(center.below().getCenter(),Direction.UP,center.below(),false);
    var context=new net.minecraft.world.item.context.UseOnContext(player,InteractionHand.MAIN_HAND,hit);
    h.assertTrue(item.getItem().useOn(context).consumesAction()&&item.isEmpty(),"Native mat placement failed");
  }
  private static int drops(GameTestHelper h,BlockPos center){
    return h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(center).inflate(12),
        e->e.getItem().is(ChaoGarden.item("karate_mat"))).stream().mapToInt(e->e.getItem().getCount()).sum();
  }
  @GameTest(template="activity_test",timeoutTicks=80)
  public static void adjacentAssembledMatsHaveIndependentArenasAndControllers(GameTestHelper h){
    var center=floor(h);var other=center.east(7);var level=h.getLevel();var player=TestConnections.makeSurvivalServerPlayer(h);
    try{
      place(h,player,center);var before=ActivityManager.mat(level,center);place(h,player,other);
      h.assertTrue(before.size()==49&&before.equals(ActivityManager.mat(level,center)),"Neighbor placement changed an existing arena");
      var second=ActivityManager.mat(level,other);
      h.assertTrue(second.size()==49&&java.util.Collections.disjoint(before,second),"Adjacent assemblies were merged");
      for(var tile:before)h.assertTrue(ActivityManager.controller(level,tile).equals(center.offset(-3,0,-3)),"First assembly controller changed");
      for(var tile:second)h.assertTrue(ActivityManager.controller(level,tile).equals(other.offset(-3,0,-3)),"Second assembly controller changed");
      level.destroyBlock(center.east(3),true);
      h.assertTrue(ActivityManager.mat(level,other).equals(second)&&drops(h,center)==1,"Edge destruction damaged the neighboring assembly or lost loot");
    }finally{player.server.getPlayerList().remove(player);}
    h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=80)
  public static void nonPlayerEdgeDestructionDropsExactlyOneMat(GameTestHelper h){
    var center=floor(h);var level=h.getLevel();var player=TestConnections.makeSurvivalServerPlayer(h);
    try{place(h,player,center);}finally{player.server.getPlayerList().remove(player);}
    h.assertTrue(level.destroyBlock(center.offset(-3,0,-3),true),"Edge destruction failed");
    for(int i=0;i<49;i++)h.assertTrue(level.getBlockState(center.offset(KarateMatBlock.offset(i))).isAir(),"Destruction left orphan tiles");
    h.assertTrue(drops(h,center)==1,"Non-player edge destruction did not recover exactly one mat");
    h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=80)
  public static void nativeEdgeExplosionRecoversOneMatWithAndWithoutDecay(GameTestHelper h){
    var center=floor(h);var other=center.east(7);var level=h.getLevel();var player=TestConnections.makeSurvivalServerPlayer(h);
    try{
      place(h,player,other);int expected=0;
      for(var mode:java.util.List.of(net.minecraft.world.level.Explosion.BlockInteraction.DESTROY,
          net.minecraft.world.level.Explosion.BlockInteraction.DESTROY_WITH_DECAY)){
        place(h,player,center);var corner=center.offset(-3,0,-3);
        // Exercise native explosion loot and removal callbacks, not destroyBlock.
        // A fixed two-edge impact list excludes the center and removes ray-cast
        // randomness. Radius 1 gives survives_explosion probability 1 even when
        // the decay mode supplies EXPLOSION_RADIUS. No global gamerules change.
        var explosion=new net.minecraft.world.level.Explosion(level,null,
            corner.getX()+.5,corner.getY()+.5,corner.getZ()+.5,1f,false,mode,
            java.util.List.of(corner,corner.east()));
        explosion.finalizeExplosion(false);
        for(int i=0;i<49;i++)h.assertTrue(level.getBlockState(center.offset(KarateMatBlock.offset(i))).isAir(),"Explosion left orphan tiles");
        h.assertTrue(drops(h,center)==++expected,"Native edge explosion duplicated or lost the one recoverable mat: "+mode);
        h.assertTrue(ActivityManager.mat(level,other).size()==49,"Explosion cleanup damaged the adjacent assembly");
      }
    }finally{player.server.getPlayerList().remove(player);}
    h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=80)
  public static void noDropEdgeRemovalDoesNotCreateMatLoot(GameTestHelper h){
    var center=floor(h);var level=h.getLevel();var player=TestConnections.makeSurvivalServerPlayer(h);
    try{place(h,player,center);}finally{player.server.getPlayerList().remove(player);}
    level.destroyBlock(center.offset(3,0,3),false);
    h.assertTrue(level.getBlockState(center).isAir()&&drops(h,center)==0,"No-drop destruction created loot or left the center");
    h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=80)
  public static void survivalAndCreativeHarvestingKeepTheirDropRules(GameTestHelper h){
    var center=floor(h);var level=h.getLevel();var player=TestConnections.makeSurvivalServerPlayer(h);
    try{
      place(h,player,center);player.gameMode.destroyBlock(center.offset(-3,0,-3));
      h.assertTrue(drops(h,center)==1,"Survival edge harvesting duplicated or lost loot");
      place(h,player,center);player.gameMode.destroyBlock(center);
      h.assertTrue(drops(h,center)==2,"Survival center harvesting duplicated or lost loot");
      place(h,player,center);player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
      h.assertTrue(player.isCreative()&&player.getAbilities().instabuild,"Creative harvesting fixture uses real creative mode");
      player.gameMode.destroyBlock(center.offset(3,0,3));
      h.assertTrue(level.getBlockState(center).isAir()&&drops(h,center)==2,"Creative edge harvesting created loot");
    }finally{player.server.getPlayerList().remove(player);}
    h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=80)
  public static void legacyArenaBesideAssemblyKeepsHistoricalController(GameTestHelper h){
    var center=floor(h);var other=center.east(7);var level=h.getLevel();var player=TestConnections.makeSurvivalServerPlayer(h);
    for(int i=0;i<49;i++)level.setBlock(center.offset(KarateMatBlock.offset(i)),ActivityContent.MAT.get().defaultBlockState(),3);
    try{place(h,player,other);}finally{player.server.getPlayerList().remove(player);}
    h.assertTrue(ActivityManager.mat(level,center).size()==49&&ActivityManager.mat(level,other).size()==49,"Legacy and assembled carpets interfered");
    h.assertTrue(ActivityManager.controller(level,center).equals(center.offset(-3,0,-3)),"Legacy controller migrated");
    level.destroyBlock(center,true);
    h.assertTrue(drops(h,center)==1&&!level.getBlockState(center.east()).isAir(),"Legacy single-tile harvesting changed");
    h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=80)
  public static void malformedAssemblyCannotBorrowLegacyOrNeighborTiles(GameTestHelper h){
    var center=floor(h);var level=h.getLevel();var player=TestConnections.makeSurvivalServerPlayer(h);
    try{place(h,player,center);}finally{player.server.getPlayerList().remove(player);}
    var corner=center.offset(-3,0,-3);
    // A same-block state edit does not invoke assembly removal; mimic an old or malformed save.
    level.setBlock(corner,ActivityContent.MAT.get().defaultBlockState(),2);
    h.assertTrue(ActivityManager.mat(level,center).isEmpty(),"Assembly accepted a mismatched legacy tile");
    h.succeed();
  }
}
