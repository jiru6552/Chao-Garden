package com.chaogarden.test;
import com.chaogarden.ChaoGarden;
import com.chaogarden.kindergarten.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.*;
import java.util.Map;
@GameTestHolder(ChaoGarden.MODID) @PrefixGameTestTemplate(false)
public final class SchoolDormerGameTests02516 {
 @GameTest(template="empty",batch="school02516_dormer",timeoutTicks=100)
 @SuppressWarnings("unchecked")
 public static void dormersHaveRecessedClearGlazingAndSupportedSills(GameTestHelper h)throws Exception {
  var method=KindergartenRooms025.class.getDeclaredMethod("plan");method.setAccessible(true);var plan=method.invoke(null);
  var field=plan.getClass().getDeclaredField("blocks");field.setAccessible(true);var blocks=(Map<BlockPos,BlockState>)field.get(plan);
  for(int center:new int[]{-18,18}) {
   var root=new BlockPos(center-1,88,14);var window=blocks.get(root);
   h.assertTrue(window.is(KindergartenDecor025.block("school_round_window")),"Missing recessed dormer");
   h.assertTrue(window.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)==Direction.SOUTH,"Dormer faces into roof");
   for(int x=center-1;x<=center;x++)for(int y=88;y<=89;y++) {
    var p=new BlockPos(x,y,14);
    h.assertTrue(p.equals(root)||blocks.get(p).isAir(),"Masonry covers glazing");
    h.assertTrue(blocks.getOrDefault(p.south(),Blocks.AIR.defaultBlockState()).isAir(),"Roof hides front of window");
    h.assertTrue(blocks.get(p.north()).isAir()&&blocks.get(p.north(2)).isAir(),"Opaque wall blocks the interior glazing");
   }
   h.assertTrue(blocks.get(new BlockPos(center+1,88,14)).is(KindergartenDecor025.block("school_clapboard")),"Dormer lost its side wall support");
   for(int x=center-1;x<=center;x++)h.assertTrue(blocks.get(new BlockPos(x,87,14)).is(Blocks.GREEN_CONCRETE),"Missing aligned sill");
   h.assertTrue(!blocks.get(new BlockPos(center-1,87,15)).is(KindergartenDecor025.block("school_round_window")),"Old projecting window remains");
  }
  h.succeed();
 }
}
