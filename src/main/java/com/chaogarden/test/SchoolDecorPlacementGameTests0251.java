package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.hub.HubContent;
import com.chaogarden.kindergarten.KindergartenContent;
import com.chaogarden.kindergarten.KindergartenDecor025;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The corner portrait must use a supported free wall cell without replacing any column segment. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class SchoolDecorPlacementGameTests0251 {
  @GameTest(template="empty",batch="kindergarten_doors0251",timeoutTicks=100)
  public static void cornerPortraitHasASolidBackingAndLeavesEveryHallColumnComplete(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);var level=p.server.getLevel(KindergartenContent.DIMENSION);
    try{
      h.assertTrue(level!=null&&HubContent.ensure(level),"Actual school dimension was not built");
      h.assertTrue(level.getBlockState(new BlockPos(6,82,7)).is(KindergartenDecor025.block("school_art_chao_small")),"Corner portrait is absent from its free flat-wall mount");
      h.assertTrue(level.getBlockState(new BlockPos(7,82,7)).is(KindergartenDecor025.block("school_cream_wall")),"Corner portrait again replaced or exposes a hole in its actual backing wall");
      for(int[] corner:new int[][]{{-5,-6},{5,-6},{5,8}})for(int y=80;y<=84;y++)
        h.assertTrue(level.getBlockState(new BlockPos(corner[0],y,corner[1])).is(KindergartenDecor025.block("school_hall_column")),"Artwork interrupted a live floor-to-ceiling hall column at "+corner[0]+","+y+","+corner[1]);
    }finally{p.server.getPlayerList().remove(p);}
    h.succeed();
  }
}
