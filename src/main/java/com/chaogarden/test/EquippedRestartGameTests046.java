package com.chaogarden.test;

import com.chaogarden.companion.AccessoriesBridge;
import com.chaogarden.companion.CarryRules;
import com.chaogarden.companion.CompanionContent;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoCarrierItem;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Two actual server processes preserve an equipped Chao and a mutable vanilla item. */
public final class EquippedRestartGameTests046 {
  private static final UUID KEEPER=UUID.nameUUIDFromBytes("equipped-046-real-restart".getBytes(java.nio.charset.StandardCharsets.UTF_8));
  private static Path expected(GameTestHelper h){
    return h.getLevel().getServer().getWorldPath(LevelResource.ROOT).resolve("chao-equipped046-restart-expected.nbt");
  }

  @GameTestHolder("chao_equipped46_save") @PrefixGameTestTemplate(false)
  public static final class Save {
    @GameTest(template="empty",timeoutTicks=80)
    public static void saveRealEquippedKeeper(GameTestHelper h){
      h.assertTrue(AccessoriesBridge.available(),"Real Accessories runtime required for save");
      var keeper=TestConnections.makeSurvivalServerPlayer(h,KEEPER);
      var state=new ChaoState();state.adult=true;state.adultAgeTicks=400;state.hunger=65;state.energy=83;state.setEvolutionStrength(.48f);
      var e=CompanionSkillGameTests.equip(h,state,keeper);
      h.runAtTickTime(18,()->{
        var chao=CompanionSkillGameTests.body(h,e);
        chao.getPersistentData().putInt("ChaoEquippedCareTicks",5998);
        chao.getPersistentData().putString("Test046Foreign","preserved");
        var sword=new ItemStack(Items.DIAMOND_SWORD);sword.setDamageValue(17);
        sword.set(DataComponents.CUSTOM_NAME,Component.literal("Native restart sword"));
        var expectedSword=sword.copy();
        keeper.setItemInHand(InteractionHand.MAIN_HAND,sword);chao.mobInteract(keeper,InteractionHand.MAIN_HAND);
        h.assertTrue(chao.isHandlingItem()&&ItemStack.matches(chao.getMainHandItem(),expectedSword),"Actual offered vanilla sword was not held before save");
      });
      h.runAtTickTime(23,()->{
        var chao=CompanionSkillGameTests.body(h,e);
        h.assertTrue(chao.state().hunger==64&&chao.state().energy==82&&chao.state().bond==1,"Care boundary did not advance exactly once before save");
        var tag=new CompoundTag();tag.putUUID("Chao",chao.getUUID());tag.putString("State",chao.state().encode());
        tag.putInt("Ticks",chao.getPersistentData().getInt("ChaoEquippedCareTicks"));tag.put("Held",chao.getMainHandItem().save(keeper.registryAccess()));
        try{NbtIo.writeCompressed(tag,expected(h));}catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}
        // This exercises normal player-file and Accessories attachment persistence.
        keeper.server.getPlayerList().remove(keeper);h.succeed();
      });
    }
  }

  @GameTestHolder("chao_equipped46_load") @PrefixGameTestTemplate(false)
  public static final class Load {
    @GameTest(template="empty",timeoutTicks=90)
    public static void loadRealEquippedKeeper(GameTestHelper h)throws Exception{
      h.assertTrue(AccessoriesBridge.available(),"Real Accessories runtime required for load");
      var saved=NbtIo.readCompressed(expected(h),NbtAccounter.unlimitedHeap());
      var keeper=TestConnections.makeSurvivalServerPlayer(h,KEEPER);var at=h.absolutePos(new BlockPos(2,2,2));
      keeper.teleportTo(at.getX(),at.getY(),at.getZ());var equipped=AccessoriesBridge.equipped(keeper);
      h.assertTrue(equipped.size()==1,"Player file did not restore exactly one real equipped companion");
      var entity=ChaoCarrierItem.entityData(equipped.getFirst());
      h.assertTrue(saved.getUUID("Chao").equals(entity.getUUID("UUID"))&&saved.getString("State").equals(entity.getString("Chao")),"UUID or full care/genetic state changed across process restart");
      h.assertTrue(entity.getCompound("NeoForgeData").getInt("ChaoEquippedCareTicks")==saved.getInt("Ticks"),"Partial care interval was lost across restart");
      h.runAtTickTime(12,()->{
        var actual=CompanionContent.findEntity(keeper.server,saved.getUUID("Chao"));
        h.assertTrue(actual instanceof ChaoEntity,"Restored real slot did not summon its original body UUID");
        var chao=(ChaoEntity)actual;var held=ItemStack.parseOptional(keeper.registryAccess(),saved.getCompound("Held"));
        h.assertTrue(chao.isHandlingItem()&&ItemStack.matches(chao.getMainHandItem(),held)&&chao.state().adultAgeTicks==400,"Pending named/damaged item or paused age did not survive restart");
        h.assertTrue(chao.getPersistentData().getInt("ChaoEquippedCareTicks")>saved.getInt("Ticks")&&"preserved".equals(chao.getPersistentData().getString("Test046Foreign")),"Saved partial care or opaque entity data did not resume");
        h.assertTrue(chao.companionCarryPosition()==CarryRules.HEAD&&chao.companionKeeperId()==keeper.getId()&&!chao.isCompanionGripping(),"Restored untrained runner did not resume its head perch");
        keeper.server.getPlayerList().remove(keeper);h.succeed();
      });
    }
  }
}
