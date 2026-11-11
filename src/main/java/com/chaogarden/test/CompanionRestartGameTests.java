package com.chaogarden.test;

import com.chaogarden.companion.AccessoriesBridge;
import com.chaogarden.companion.CompanionContent;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoCarrierItem;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Run the save and load namespaces in separate JVMs with the same development world. */
public final class CompanionRestartGameTests {
  private static final UUID KEEPER=UUID.nameUUIDFromBytes("companion-039-restart".getBytes(java.nio.charset.StandardCharsets.UTF_8));
  private static Path expected(GameTestHelper h) {
    return h.getLevel().getServer().getWorldPath(LevelResource.ROOT).resolve("chao-companion-restart-expected.nbt");
  }
  @GameTestHolder("chao_care_save") @PrefixGameTestTemplate(false)
  public static final class Save {
    @GameTest(template="empty",timeoutTicks=80)
    public static void saveEquippedKeeper(GameTestHelper h) {
      h.assertTrue(AccessoriesBridge.available(),"Accessories missing");
      var keeper=TestConnections.makeSurvivalServerPlayer(h,KEEPER);
      var s=new ChaoState();s.adult=true;s.adultAgeTicks=400;s.hunger=65;s.energy=83;s.setEvolutionStrength(.48f);
      var e=CompanionSkillGameTests.equip(h,s,keeper);
      h.runAtTickTime(18,()->{
        var c=CompanionSkillGameTests.body(h,e);
        c.getPersistentData().putInt("ChaoEquippedCareTicks",5998);
        var sword=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("avaritia:infinity_sword")));
        sword.set(DataComponents.CUSTOM_NAME,Component.literal("Restart sword"));keeper.setItemInHand(InteractionHand.MAIN_HAND,sword);
        c.mobInteract(keeper,InteractionHand.MAIN_HAND);
      });
      h.runAtTickTime(23,()->{
        var c=CompanionSkillGameTests.body(h,e);
        h.assertTrue(c.state().hunger==64 && c.state().energy==82 && c.state().bond==1,"Boundary did not advance before save");
        var data=new CompoundTag();data.putUUID("Chao",c.getUUID());data.putString("State",c.state().encode());
        data.putInt("Ticks",c.getPersistentData().getInt("ChaoEquippedCareTicks"));
        data.put("Held",c.getMainHandItem().save(keeper.registryAccess()));
        try{NbtIo.writeCompressed(data,expected(h));}catch(java.io.IOException ex){throw new java.io.UncheckedIOException(ex);}
        keeper.server.getPlayerList().remove(keeper);h.succeed();
      });
    }
  }
  @GameTestHolder("chao_care_load") @PrefixGameTestTemplate(false)
  public static final class Load {
    @GameTest(template="empty",timeoutTicks=90)
    public static void loadEquippedKeeper(GameTestHelper h) throws Exception {
      var expected=NbtIo.readCompressed(expected(h),NbtAccounter.unlimitedHeap());
      var p=TestConnections.makeSurvivalServerPlayer(h,KEEPER);
      var at=h.absolutePos(new BlockPos(2,2,2));p.teleportTo(at.getX(),at.getY(),at.getZ());
      var items=AccessoriesBridge.equipped(p);
      h.assertTrue(items.size()==1,"Player file did not restore exactly one equipped companion");
      var saved=ChaoCarrierItem.entityData(items.getFirst());
      h.assertTrue(saved.getString("Chao").equals(expected.getString("State")) && expected.getUUID("Chao").equals(saved.getUUID("UUID")),
          "Saved UUID or full care state changed across process restart");
      h.assertTrue(saved.getCompound("NeoForgeData").getInt("ChaoEquippedCareTicks")==expected.getInt("Ticks"),"Partial care interval lost");
      h.runAtTickTime(12,()->{
        var c=(ChaoEntity)CompanionContent.findEntity(p.server,expected.getUUID("Chao"));
        var held=ItemStack.parseOptional(p.registryAccess(),expected.getCompound("Held"));
        h.assertTrue(c!=null && c.isHandlingItem() && ItemStack.matches(c.getMainHandItem(),held) && c.state().adultAgeTicks==400,
            "Fresh server lost the pending sword or advanced the paused age");
        h.assertTrue(c.getPersistentData().getInt("ChaoEquippedCareTicks")>expected.getInt("Ticks") && !c.isCompanionGripping(),"Loaded care or flight flags did not resume correctly");
        p.server.getPlayerList().remove(p);h.succeed();
      });
    }
  }
}
