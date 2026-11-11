package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.hub.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class GardenRefinementGameTests02510 {
  private static boolean water(int x,int z){return (x+15)*(x+15)/64.0+(z+3)*(z+3)/121.0<1;}
  @GameTest(template="empty",batch="hero02510_border",timeoutTicks=100)
  public static void heroShorelineRepairsEveryLandCellTouchingWater(GameTestHelper h){
    var level=h.getLevel().getServer().getLevel(HubContent.HERO_GARDEN);
    h.assertTrue(level!=null&&HubContent.ensure(level),"Hero garden unavailable");
    // Reproduce the two lawn-side gaps visible in the user's screenshot.
    for(int z:new int[]{-8,2})level.setBlock(new BlockPos(-7,79,z),LobbyDecor025.TURF.get().defaultBlockState(),2);
    HeroGardenRooms0253.ensure(level);
    int border=0;
    for(int x=-25;x<=-5;x++)for(int z=-16;z<=10;z++){
      if(water(x,z))continue;
      boolean touches=false;for(var side:Direction.Plane.HORIZONTAL)touches|=water(x+side.getStepX(),z+side.getStepZ());
      if(!touches)continue;border++;
      var at=new BlockPos(x,79,z);var state=level.getBlockState(at);
      h.assertTrue(state.is(HeroGardenDecor0253.block("hero_marble"))&&Block.isShapeFullBlock(state.getCollisionShape(level,at)),"Marble outline still skips a water-adjacent block: "+at);
    }
    h.assertTrue(border>40,"Shoreline test covered too little perimeter");h.succeed();
  }
  @GameTest(template="empty",batch="neutral02510_scenery",timeoutTicks=100)
  public static void paleCascadePlantsHaveDrySupportsAndLeaveWaterAndWalkwaysClear(GameTestHelper h){
    var level=h.getLevel().getServer().getLevel(HubContent.GARDEN);
    h.assertTrue(level!=null&&HubContent.ensure(level),"Neutral garden unavailable");
    for(var xyz:GardenRooms025.WATERFALL_PLANTS){
      var at=new BlockPos(xyz[0],xyz[1],xyz[2]);var state=level.getBlockState(at);
      h.assertTrue(state.is(NeutralGardenDecor02510.BROADLEAF.get()),"Missing reference terrace foliage");
      h.assertTrue(state.getCollisionShape(level,at).isEmpty()&&state.getFluidState().isEmpty(),"Terrace foliage blocks a route or occupies water");
      h.assertTrue(level.getBlockState(at.below()).isFaceSturdy(level,at.below(),Direction.UP),"Terrace plant floats");
    }
    for(int step=0;step<4;step++){
      int x=-27-step*3,y=82+step*3;
      h.assertTrue(level.getBlockState(new BlockPos(x,y,-3)).is(NeutralGardenDecor02510.CASCADE_ROCK.get()),"Pale limestone terrace missing");
      h.assertTrue(level.getFluidState(new BlockPos(x,y,-5)).isSource(),"Foliage/stone displaced the cascade");
    }
    h.assertTrue(level.getBiome(new BlockPos(-20,79,-8)).value().getWaterColor()==0x80bcc5,"Neutral pool retained the dark-blue default water tint");
    h.succeed();
  }
}
