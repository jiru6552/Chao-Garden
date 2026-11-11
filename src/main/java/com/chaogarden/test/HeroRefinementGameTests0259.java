package com.chaogarden.test;
import com.chaogarden.ChaoGarden;
import com.chaogarden.hub.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class HeroRefinementGameTests0259 {
  @GameTest(template="empty",batch="hero0259_refinement",timeoutTicks=120)
  public static void slimPavilionColumnsHaveNativeCollisionAndShallowSupportedRoof(GameTestHelper h){
    var level=h.getLevel().getServer().getLevel(HubContent.HERO_GARDEN);
    h.assertTrue(level!=null&&HubContent.ensure(level),"Hero garden did not prepare");
    for(int x:new int[]{-11,-5})for(int z:new int[]{7,13}){
      for(int y=83;y<=88;y++){
        var at=new BlockPos(x,y,z);var state=level.getBlockState(at);
        h.assertTrue(state.is(HeroGardenDecor0253.block("hero_column")),"Missing lavender column");
        var part=y==83?HeroColumn0259.Part.BASE:y==88?HeroColumn0259.Part.CAPITAL:HeroColumn0259.Part.SHAFT;
        h.assertTrue(state.getValue(HeroColumn0259.PART)==part,"Column member order changed");
        var shape=state.getCollisionShape(level,at);
        h.assertTrue(!shape.isEmpty()&&shape.bounds().minY==0&&shape.bounds().maxY==1,"Vertical column support is broken");
        if(part==HeroColumn0259.Part.SHAFT){
          h.assertTrue(shape.bounds().minX==.25&&shape.bounds().maxX==.75,"Pavilion shaft reverted to a full cube");
          h.assertTrue(shape.toAabbs().stream().noneMatch(b->b.contains(.26,.5,.26)),"Clipped column corner became solid");
        }
      }
      h.assertTrue(!level.getBlockState(new BlockPos(x,89,z)).isAir(),"Column detached from entablature");
    }
    for(var at:BlockPos.betweenClosed(-12,92,6,-4,94,14))
      h.assertTrue(level.getBlockState(at).isAir(),"Tall stepped pavilion cap returned");
    h.assertTrue(level.getBlockState(new BlockPos(-8,91,10)).is(HeroGardenDecor0253.block("hero_marble")),"Shallow cap missing");
    for(var at:BlockPos.betweenClosed(-25,91,-9,-22,94,1))
      h.assertTrue(!level.getBlockState(at).is(HeroGardenDecor0253.block("hero_marble")),"Tall fountain pediment returned");
    h.succeed();
  }
}
