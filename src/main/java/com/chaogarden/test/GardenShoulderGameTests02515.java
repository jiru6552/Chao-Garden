package com.chaogarden.test;
import com.chaogarden.ChaoGarden;
import com.chaogarden.hub.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
@GameTestHolder(ChaoGarden.MODID) @PrefixGameTestTemplate(false)
public final class GardenShoulderGameTests02515 {
 @GameTest(template="empty",batch="garden02515_shoulders",timeoutTicks=120)
 public static void neutralShoulderHasWalkableStepsAndKeepsCareRoutes(GameTestHelper h){
  var p=GardenRooms025.plan();
  for(int x=16;x<=18;x++)for(int z=-12;z<=-6;z++){
   int y=GardenRooms025.surface(x,z);h.assertTrue(y==79+Math.max(0,x-16),"Neutral source shoulder lost its one-block steps");
   h.assertTrue(p.blocks.get(new BlockPos(x,y,z)).is(LobbyDecor025.TURF.get()),"Neutral shoulder lacks grass support");
  }
  h.assertTrue(GardenRooms025.surface(0,0)==79&&GardenRooms025.surface(17,9)==80,"Neutral care lawn or transporter moved");h.succeed();
 }
 @GameTest(template="empty",batch="garden02515_shoulders",timeoutTicks=120)
 public static void heroShoulderPreservesTheGateAndPool(GameTestHelper h){
  var p=HeroGardenRooms0253.plan();
  for(int x=18;x<=20;x++)for(int z=-12;z<=-7;z++){
   int y=HeroGardenRooms0253.surface(x,z);h.assertTrue(y==79+Math.max(0,x-18),"Hero source shoulder lost its one-block steps");
   h.assertTrue(p.blocks.get(new BlockPos(x,y,z)).is(LobbyDecor025.TURF.get()),"Hero shoulder lacks grass support");
  }
  for(int x=17;x<=21;x++)for(int z=6;z<=10;z++)h.assertTrue(p.blocks.get(new BlockPos(x,79,z)).isFaceSturdy(h.getLevel(),new BlockPos(x,79,z),net.minecraft.core.Direction.UP),"Hero five-wide gate approach changed");
  h.assertTrue(p.blocks.get(new BlockPos(-15,79,-3)).is(net.minecraft.world.level.block.Blocks.WATER),"Hero pool changed");h.succeed();
 }
}
