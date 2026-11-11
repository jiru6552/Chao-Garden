package com.chaogarden.test;
import com.chaogarden.*;
import com.chaogarden.activities.*;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.care.ChaoCareNetwork;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.*;
public final class GardenRestartGameTests042 {
 static final UUID OWNER=new UUID(42,1),CHAO=new UUID(42,2);static final BlockPos AT=new BlockPos(-505,90,505),OLD=AT.east(90);
 @GameTestHolder("chao_garden_save42") @PrefixGameTestTemplate(false) public static class Save {
  @GameTest(template="empty",timeoutTicks=100) public static void setupAndEdit(GameTestHelper h){
   var l=h.getLevel();var p=TestConnections.makeSurvivalServerPlayer(h,OWNER);p.teleportTo(AT.getX(),90,AT.getZ());p.setNoGravity(true);
   for(var b:BlockPos.betweenClosed(AT.offset(-2,-1,-2),AT.offset(3,0,3)))l.setBlock(b,b.getY()==89?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
   l.setBlock(AT,ActivityContent.GARDEN.get().defaultBlockState(),3);var data=GardenRegistry.get(p.server);var g=data.register(p,AT);
   h.assertTrue(data.activate(p,g.place()),"real setup without fence or food");
   l.getChunkAt(OLD);l.setBlock(OLD,ActivityContent.GARDEN.get().defaultBlockState(),3);var old=data.register(p,OLD);
   data.areas.put(old.place(),new GardenArea(false,List.of(OLD.east().asLong()),OLD.east()));data.setDirty();
   var c=ChaoGarden.CHAO.get().create(l);c.setUUID(CHAO);c.owner=OWNER;c.setNoAi(true);c.setNoGravity(true);c.moveTo(AT.east().getBottomCenter());l.addFreshEntity(c);data.placed(c);
   h.assertTrue(data.entries.containsKey(CHAO),"automatic detection in square");p.setGameMode(GameType.CREATIVE);
   h.assertTrue(ChaoCareNetwork.editStats(p,new ChaoCareNetwork.StatEdit(CHAO.toString(),2,1,10)),"edit before real disk save");
   p.server.getPlayerList().remove(p);h.succeed();
  }
 }
 @GameTestHolder("chao_garden_load42") @PrefixGameTestTemplate(false) public static class Load {
  @GameTest(template="empty",timeoutTicks=1800) public static void reloadDetectRemoveAndReturn(GameTestHelper h){
   var l=h.getLevel();var p=TestConnections.makeSurvivalServerPlayer(h,OWNER);p.teleportTo(l,AT.getX(),90,AT.getZ(),0,0);p.setNoGravity(true);l.getChunkSource().move(p);l.getChunkAt(AT);l.getChunkAt(OLD);l.setChunkForced(AT.getX()>>4,AT.getZ()>>4,true);
   h.succeedWhen(()->{
    var data=GardenRegistry.get(p.server);var place=GardenRegistry.Place.at(l,AT);var legacy=data.areas.get(GardenRegistry.Place.at(l,OLD));
    h.assertTrue(data.areas.get(place).chunkSquare&&!legacy.chunkSquare,"both new and legacy format retained on restart");
    h.assertTrue(l.getEntity(CHAO) instanceof ChaoEntity,"resident loads");var c=(ChaoEntity)l.getEntity(CHAO);
    h.assertTrue(c.state().levels[2]==10,"Creative stat edit survived JVM restart");data.placed(c);data.placed(c);h.assertTrue(data.entries.get(CHAO).garden.equals(place),"one detected record");
    var out=AT.east(60);c.teleportTo(out.getX(),90,out.getZ());h.assertFalse(data.entries.containsKey(CHAO),"explicit boundary crossing releases slot");
    c.teleportTo(AT.getX()+1.5,90,AT.getZ()+.5);data.placed(c);h.assertTrue(data.entries.containsKey(CHAO),"return rediscovered");
    var stack=new net.minecraft.world.item.ItemStack(ChaoGarden.item("chao_carrier"));h.assertTrue(com.chaogarden.item.ChaoCarrierItem.capture(stack,c,p),"pickup succeeds after restart");h.assertFalse(data.entries.containsKey(CHAO),"pickup leaves no duplicate record");
    l.setChunkForced(AT.getX()>>4,AT.getZ()>>4,false);p.server.getPlayerList().remove(p);
   });
  }
 }
}
