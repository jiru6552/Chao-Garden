package com.chaogarden.test;
import com.chaogarden.ChaoGarden;
import com.chaogarden.companion.*;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoCarrierItem;
import com.chaogarden.recovery.ChaoRecovery;
import java.util.UUID;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.gametest.*;
public final class RecoveryRestartGameTests {
 static final UUID OWNER=UUID.nameUUIDFromBytes("chao041-recovery-owner".getBytes(java.nio.charset.StandardCharsets.UTF_8));
 static java.nio.file.Path expected(GameTestHelper h){return h.getLevel().getServer().getWorldPath(LevelResource.ROOT).resolve("recovery041.nbt");}
 @GameTestHolder("chao_recovery_save") @PrefixGameTestTemplate(false) public static class Save {
  @GameTest(template="empty",timeoutTicks=100) public static void pendingAndBabyCooldown(GameTestHelper h){
   var p=TestConnections.makeSurvivalServerPlayer(h,OWNER);var state=new ChaoState();state.ageTicks=400;state.bond=31;
   var e=CompanionSkillGameTests.equip(h,state,p);
   h.runAtTickTime(20,()->{
    var baby=CompanionSkillGameTests.body(h,e);baby.hurt(baby.damageSources().genericKill(),10000);
    var expected=new CompoundTag();expected.putUUID("Baby",e.chao());expected.put("Accessory",e.carrier().save(p.registryAccess()));
    p.server.getPlayerList().remove(p);
    var c=ChaoGarden.CHAO.get().create(h.getLevel());c.owner=OWNER;c.state().adult=true;c.state().ageTicks=500;c.state().bond=70;
    c.moveTo(h.absolutePos(new net.minecraft.core.BlockPos(2,2,2)).getBottomCenter());h.getLevel().addFreshEntity(c);
    c.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,new ItemStack(Items.DIAMOND_SWORD));c.getOffhandItem().setDamageValue(77);
    c.hurt(c.damageSources().genericKill(),10000);ChaoRecovery.get(p.server).retry(p.server);
    expected.putUUID("Pending",c.getUUID());expected.putString("State",c.state().encode());
    try{NbtIo.writeCompressed(expected,expected(h));}catch(java.io.IOException ex){throw new java.io.UncheckedIOException(ex);}
    h.assertTrue(ChaoRecovery.get(p.server).pendingCount()>0,"Offline recovery not queued");h.succeed();
   });
  }
 }
 @GameTestHolder("chao_recovery_load") @PrefixGameTestTemplate(false) public static class Load {
  @GameTest(template="empty",timeoutTicks=100) public static void offlineLoginAndSavedCooldown(GameTestHelper h)throws Exception{
   var expected=NbtIo.readCompressed(expected(h),NbtAccounter.unlimitedHeap());var p=TestConnections.makeSurvivalServerPlayer(h,OWNER);
   var items=AccessoriesBridge.equipped(p);h.assertTrue(items.size()==1,"Equipped baby missing after JVM restart");
   var before=ItemStack.parseOptional(p.registryAccess(),expected.getCompound("Accessory"));
   h.assertTrue(ItemStack.matches(items.getFirst(),before)&&CompanionContent.remaining(items.getFirst(),p.server)>0,"Cooldown or complete item data changed");
   h.runAtTickTime(30,()->{
    ChaoRecovery.get(p.server).retry(p.server);ChaoRecovery.get(p.server).retry(p.server);
    var found=p.getInventory().items.stream().filter(s->expected.getUUID("Pending").equals(ChaoCarrierItem.storedUUID(s))).toList();
    h.assertTrue(found.size()==1,"Login did not deliver exactly one pending Chao");
    var c=ChaoGarden.CHAO.get().create(h.getLevel());c.load(ChaoCarrierItem.entityData(found.getFirst()));
    h.assertTrue(c.state().encode().equals(expected.getString("State"))&&c.getOffhandItem().getDamageValue()==77,"Pending state or equipment lost on real restart");
    h.assertTrue(CompanionContent.findEntity(p.server,expected.getUUID("Baby"))==null,"Saved cooldown returned baby early");
    p.server.getPlayerList().remove(p);h.succeed();
   });
  }
 }
}
