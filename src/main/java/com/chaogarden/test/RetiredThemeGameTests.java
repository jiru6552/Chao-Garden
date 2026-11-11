package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.ChaoVariants;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEggEntity;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoCarrierItem;
import com.chaogarden.item.ChaoEggItem;
import com.chaogarden.item.RetiredChaoEggItem;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Exercises former registry IDs and real serialized creatures through production handlers. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class RetiredThemeGameTests {
  private static final BlockPos CENTER = new BlockPos(2, 2, 2);

  @GameTest(template="empty", timeoutTicks=40)
  public static void retiredEggsPlaceOrdinaryWithoutLosingData(GameTestHelper helper) {
    var world=helper.getLevel(); var keeper=helper.makeMockPlayer(GameType.SURVIVAL);
    keeper.getAbilities().mayBuild=true; keeper.getAbilities().instabuild=false;
    helper.setBlock(CENTER.below(),Blocks.STONE);
    BlockPos absolute=helper.absolutePos(CENTER), floor=absolute.below();
    var hit=new BlockHitResult(Vec3.atCenterOf(floor).add(0,.5,0),Direction.UP,floor,false);
    for (String retired:ChaoVariants.retiredIds()) {
      var item=ChaoGarden.item(ChaoVariants.itemId(retired));
      helper.assertTrue(item instanceof RetiredChaoEggItem && !ChaoGarden.visibleInCreative(item),
          "Former theme shell must be a hidden ordinary compatibility egg: "+retired);
      helper.assertTrue(world.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(
          ChaoGarden.MODID,ChaoVariants.itemId(retired))).isEmpty(),"Retired recipe remains: "+retired);
      ChaoState old=ChaoState.egg(8,true,false,3,new Random(1400));
      old.variant=retired;old.reincarnations=2;old.happiness=83;old.train(ChaoState.RUN,432,new Random(1401));
      ChaoState expected=ChaoState.decode(old.encode());expected.variant="normal";
      CompoundTag data=new CompoundTag();data.putString("Chao",old.encode());data.putInt("HatchTicks",321);
      data.putString("ChaoName",Component.Serializer.toJson(Component.literal("Remember Me"),world.registryAccess()));
      ItemStack stack=new ItemStack(item);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
      keeper.setItemInHand(InteractionHand.MAIN_HAND,stack);
      var result=item.useOn(new UseOnContext(keeper,InteractionHand.MAIN_HAND,hit));
      var eggs=world.getEntitiesOfClass(ChaoEggEntity.class,new AABB(absolute).inflate(.5),
          egg->keeper.getUUID().equals(egg.owner));
      helper.assertTrue(result.consumesAction() && stack.isEmpty() && eggs.size()==1,
          "Legacy shell failed to place an ordinary egg: "+retired);
      var egg=eggs.getFirst();CompoundTag saved=new CompoundTag();egg.saveWithoutId(saved);
      helper.assertTrue(egg.state().encode().equals(expected.encode()) && saved.getInt("Incubation")==321
          && egg.chaoName.getString().equals("Remember Me"),"Legacy egg lost genes/incubation/name: "+retired);
      ItemStack nativeShell=ChaoEggItem.containing(egg.state(),321);
      ChaoState recovered=ChaoState.decode(nativeShell.get(DataComponents.CUSTOM_DATA).copyTag().getString("Chao"));
      helper.assertTrue(nativeShell.is(ChaoGarden.item("ruby_jewel_egg"))
          &&recovered.encode().equals(expected.encode())&&recovered.variant.equals("normal"),
          "Recovery failed to match the preserved native coat or recreated a retired theme: "+retired);
      egg.discard();
    }
    keeper.getInventory().clearContent();helper.succeed();
  }

  @GameTest(template="empty", timeoutTicks=40)
  public static void retiredCreaturesAndCarriersPreserveIdentity(GameTestHelper helper) {
    var world=helper.getLevel();var keeper=helper.makeMockPlayer(GameType.SURVIVAL);
    keeper.getAbilities().mayBuild=true;keeper.getAbilities().instabuild=false;
    helper.setBlock(CENTER.below(),Blocks.STONE);
    BlockPos absolute=helper.absolutePos(CENTER),floor=absolute.below();
    var hit=new BlockHitResult(Vec3.atCenterOf(floor).add(0,.5,0),Direction.UP,floor,false);
    for (String retired:ChaoVariants.retiredIds()) {
      ChaoState state=ChaoState.egg(7,true,true,4,new Random(1410));state.adult=true;
      state.variant=retired;state.giveAnimal(6);state.happiness=87;state.train(ChaoState.FLY,675,new Random(1411));
      ChaoState expected=ChaoState.decode(state.encode());expected.variant="normal";
      ChaoEntity original=ChaoGarden.CHAO.get().create(world);original.owner=keeper.getUUID();
      original.setCustomName(Component.literal("Old Friend"));
      CompoundTag data=new CompoundTag();original.saveWithoutId(data);data.putString("Chao",state.encode());
      ChaoEntity reloaded=ChaoGarden.CHAO.get().create(world);reloaded.load(data);
      helper.assertTrue(reloaded.state().encode().equals(expected.encode())
          && reloaded.getUUID().equals(original.getUUID()) && reloaded.owner.equals(keeper.getUUID())
          && reloaded.getName().getString().equals("Old Friend"),"Retired creature migration lost identity/data: "+retired);
      ItemStack carrier=new ItemStack(ChaoGarden.item("chao_carrier"));ChaoCarrierItem.storeEntity(carrier,data);
      helper.assertTrue(ChaoCarrierItem.state(carrier).encode().equals(expected.encode()),
          "Held carrier did not migrate the retired appearance: "+retired);
      keeper.setItemInHand(InteractionHand.MAIN_HAND,carrier);
      var result=carrier.getItem().useOn(new UseOnContext(keeper,InteractionHand.MAIN_HAND,hit));
      var released=world.getEntitiesOfClass(ChaoEntity.class,new AABB(absolute).inflate(1),
          chao->chao.getUUID().equals(original.getUUID()));
      helper.assertTrue(result.consumesAction() && released.size()==1 && !ChaoCarrierItem.filled(carrier),
          "Legacy carrier failed safe release: "+retired);
      var chao=released.getFirst();
      helper.assertTrue(chao.state().encode().equals(expected.encode()) && chao.owner.equals(keeper.getUUID())
          && chao.getName().getString().equals("Old Friend"),"Released migrated Chao lost data: "+retired);
      chao.discard();
    }
    keeper.getInventory().clearContent();helper.succeed();
  }
}
