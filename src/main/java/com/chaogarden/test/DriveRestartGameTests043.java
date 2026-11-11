package com.chaogarden.test;
import com.chaogarden.*;
import com.chaogarden.core.*;
import com.chaogarden.entity.ChaoEntity;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
public final class DriveRestartGameTests043 {
 static final UUID OWNER=new UUID(43,1),CHAO=new UUID(43,2);static final BlockPos AT=new BlockPos(-505,90,505);
 @GameTestHolder("chao_drive_save43") @PrefixGameTestTemplate(false) public static class Save {
  @GameTest(template="empty",timeoutTicks=120) public static void saveDriveUse(GameTestHelper h){
   var l=h.getLevel();l.getChunkAt(AT);l.setBlock(AT.below(),Blocks.STONE.defaultBlockState(),3);
   var c=ChaoGarden.CHAO.get().create(l);c.setUUID(CHAO);c.owner=OWNER;c.setNoAi(true);c.setNoGravity(true);c.moveTo(AT.getBottomCenter());
   var s=c.state();s.levels[0]=26;s.progress[0]=43;s.points[0]=650;s.grades[0]=6;
   DriveRules.apply(s,DriveRules.Tier.SOLID,0,new Random(43));c.sync();l.addFreshEntity(c);
   l.setBlock(AT.east(),Blocks.CHEST.defaultBlockState(),3);var chest=(net.minecraft.world.Container)l.getBlockEntity(AT.east());
   int slot=0;for(String id:DriveGameTests043.ids())chest.setItem(slot++,DriveGameTests043.item(id));chest.setChanged();
   h.assertTrue(s.levels[0]==27&&s.progress[0]==43,"new stat before disk save");h.succeed();
  }
 }
 @GameTestHolder("chao_drive_load43") @PrefixGameTestTemplate(false) public static class Load {
  @GameTest(template="empty",timeoutTicks=1800) public static void loadDriveUse(GameTestHelper h){
   var l=h.getLevel();var p=TestConnections.makeSurvivalServerPlayer(h,OWNER);p.teleportTo(l,AT.getX(),90,AT.getZ(),0,0);p.setNoGravity(true);l.getChunkSource().move(p);l.getChunkAt(AT);l.setChunkForced(AT.getX()>>4,AT.getZ()>>4,true);
   h.succeedWhen(()->{h.assertTrue(l.getEntity(CHAO) instanceof ChaoEntity,"saved actor loaded");var c=(ChaoEntity)l.getEntity(CHAO);var s=c.state();
    h.assertTrue(s.levels[0]==27&&s.progress[0]==43&&s.grades[0]==6&&s.driveCount==1&&s.hiddenProgress[0]==40&&s.hiddenProgress[1]==20,"stats, X rank and hidden progress survive JVM restart");
    var chest=(net.minecraft.world.Container)l.getBlockEntity(AT.east());int slot=0;for(String id:DriveGameTests043.ids())h.assertTrue(chest.getItem(slot++).is(ChaoGarden.item(id)),"combined item ID persists: "+id);
    l.setChunkForced(AT.getX()>>4,AT.getZ()>>4,false);p.server.getPlayerList().remove(p);
   });
  }
 }
}
