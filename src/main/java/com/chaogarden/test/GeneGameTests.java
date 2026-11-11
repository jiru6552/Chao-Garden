package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.genes.GeneItem;
import com.chaogarden.genes.GenericChestTrainingModifier;
import com.chaogarden.item.CareItem;
import com.chaogarden.item.ChaoCarrierItem;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Optional;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class GeneGameTests {
  @GameTest(template = "activity_test", timeoutTicks = 160)
  public static void allGeneItemsApplyThroughCareAndProtectOwnership(GameTestHelper helper) {
    var level=helper.getLevel();
    var keeper=helper.makeMockPlayer(GameType.SURVIVAL);
    keeper.getAbilities().instabuild=false;
    var samples=new java.util.ArrayList<com.chaogarden.entity.ChaoEntity>();
    var expected=new java.util.ArrayList<ChaoState>();
    for(int index=0;index<ChaoGarden.ANIMALS.length;index++) {
      var item=ChaoGarden.item(ChaoGarden.ANIMALS[index]+"_animal_gene");
      helper.assertTrue(item instanceof GeneItem && ((GeneItem)item).index==index,"Gene registration/index mismatch");
      var chao=ChaoGarden.CHAO.get().create(level);
      chao.setState(new ChaoState());chao.owner=keeper.getUUID();
      chao.setNoAi(true);chao.setNoGravity(true);
      chao.moveTo(Vec3.atCenterOf(helper.absolutePos(new BlockPos(2+index%6*2,2,2+index/6*2))));level.addFreshEntity(chao);
      var state=new ChaoState();state.giveAnimal(index);expected.add(state);samples.add(chao);
      var stack=new ItemStack(item,2);keeper.setItemInHand(InteractionHand.MAIN_HAND,stack);
      helper.assertTrue(chao.mobInteract(keeper,InteractionHand.MAIN_HAND).consumesAction()
          && stack.getCount()==1 && chao.getMainHandItem().is(item),"Offering did not transfer exactly one sample");
      helper.assertTrue(chao.state().animalMask==0 && Arrays.stream(chao.state().animalParts).allMatch(x->x<0),
          "Gene applied instantly instead of being considered");
      var unavailable=ChaoGarden.CHAO.get().create(level);unavailable.owner=UUID.randomUUID();
      var refused=new ItemStack(item,2);keeper.setItemInHand(InteractionHand.MAIN_HAND,refused);
      unavailable.mobInteract(keeper,InteractionHand.MAIN_HAND);
      helper.assertTrue(refused.getCount()==2 && !unavailable.isHandlingItem(),"Gene bypassed keeper ownership");
      unavailable.discard();
    }
    helper.runAtTickTime(115,()->{
      for(int i=0;i<samples.size();i++) {
        var actual=samples.get(i).state();var wanted=expected.get(i);
        helper.assertTrue(actual.animalMask==wanted.animalMask
            && Arrays.equals(actual.animalParts,wanted.animalParts)
            && Arrays.equals(actual.progress,wanted.progress) && Arrays.equals(actual.levels,wanted.levels)
            && actual.flameOrb==wanted.flameOrb && actual.ghostBody==wanted.ghostBody
            && actual.hatsUnlocked==wanted.hatsUnlocked,"Delayed gene commit differs from authoritative effect: "+i
            +" actualMask="+actual.animalMask+" wantedMask="+wanted.animalMask
            +" actualParts="+Arrays.toString(actual.animalParts)+" wantedParts="+Arrays.toString(wanted.animalParts)
            +" actualProgress="+Arrays.toString(actual.progress)+" wantedProgress="+Arrays.toString(wanted.progress)
            +" actualLevels="+Arrays.toString(actual.levels)+" wantedLevels="+Arrays.toString(wanted.levels)
            +" itemAction="+samples.get(i).getItemAction()+" ticks="+samples.get(i).getItemActionTicks());
        helper.assertTrue(samples.get(i).getMainHandItem().isEmpty() && !samples.get(i).isHandlingItem(),
            "Committed gene retained a duplicate sample or stuck action");
        samples.get(i).discard();
      }
      helper.succeed();
    });
  }

  @GameTest(template = "empty", timeoutTicks = 1200)
  public static void repeatedGeneSamplesAccumulateMixedPartsAndPersistInCarrier(GameTestHelper helper) {
    var level=helper.getLevel();var keeper=helper.makeMockPlayer(GameType.SURVIVAL);
    keeper.getAbilities().instabuild=false;
    var chao=ChaoGarden.CHAO.get().create(level);chao.owner=keeper.getUUID();chao.setState(new ChaoState());
    chao.setNoAi(true);chao.setNoGravity(true);
    chao.moveTo(Vec3.atCenterOf(helper.absolutePos(new BlockPos(2,2,2))));level.addFreshEntity(chao);
    for(int sample=0;sample<8;sample++) {
      final int step=sample;final int[] before=new int[8];
      helper.runAtTickTime(1+sample*130L,()->{
        System.arraycopy(chao.state().animalParts,0,before,0,8);
        int animal=step<4?6:3;
        var stack=new ItemStack(ChaoGarden.item(ChaoGarden.ANIMALS[animal]+"_animal_gene"),2);
        keeper.setItemInHand(InteractionHand.MAIN_HAND,stack);chao.mobInteract(keeper,InteractionHand.MAIN_HAND);
        helper.assertTrue(stack.getCount()==1 && Arrays.equals(before,chao.state().animalParts),
            "Sample transfer changed parts before the consideration/training animation");
      });
      helper.runAtTickTime(95+sample*130L,()->{
        int animal=step<4?6:3,changed=0,matching=0;
        for(int slot=0;slot<8;slot++) {
          if(before[slot]!=chao.state().animalParts[slot])changed++;
          if(chao.state().animalParts[slot]==animal)matching++;
        }
        helper.assertTrue(changed==1 && matching==step%4+1,"Sample did not express exactly one remaining part");
        if(step>=4)helper.assertTrue(chao.state().animalParts[ChaoState.EARS]==6
            && chao.state().animalParts[ChaoState.ARMS]==6,"Peacock erased rabbit parts outside its slots");
        if(step==7) {
          var carrier=new ItemStack(ChaoGarden.item("chao_carrier"));
          String saved=chao.state().encode();int[] mixed=chao.state().animalParts.clone();UUID uuid=chao.getUUID();
          helper.assertTrue(ChaoCarrierItem.capture(carrier,chao,keeper),"Mixed Chao capture failed");
          helper.assertTrue(saved.equals(ChaoCarrierItem.state(carrier).encode()),"Carrier changed mixed genes");
          var restored=ChaoGarden.CHAO.get().create(level);restored.load(ChaoCarrierItem.entityData(carrier));
          helper.assertTrue(restored.getUUID().equals(uuid) && Arrays.equals(mixed,restored.state().animalParts),
              "Reload changed mixed appearance or creature identity");restored.discard();helper.succeed();
        }
      });
    }
  }

  @GameTest(template = "empty", timeoutTicks = 40)
  public static void genericChestModifierSupportsForeignTablesAndPreservesLoot(GameTestHelper helper) {
    var params = new LootParams.Builder(helper.getLevel())
        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
        .create(LootContextParamSets.CHEST);
    var source = new GenericChestTrainingModifier(new LootItemCondition[0], 1, 1);
    var encoded = GenericChestTrainingModifier.CODEC.codec().encodeStart(JsonOps.INSTANCE, source)
        .getOrThrow();
    var modifier = GenericChestTrainingModifier.CODEC.codec().parse(JsonOps.INSTANCE, encoded)
        .getOrThrow();
    for (String id : new String[] {"minecraft:chests/village/village_tannery",
                                   "chao_garden:distribution_test/unconventional"}) {
      var context = new LootContext.Builder(params).withOptionalRandomSeed(1024)
          .withQueriedLootTableId(ResourceLocation.parse(id)).create(Optional.empty());
      var loot = new ObjectArrayList<ItemStack>();
      var original = new ItemStack(Items.DIAMOND, 3); loot.add(original);
      modifier.apply(loot, context);
      helper.assertTrue(loot.size() == 3 && loot.getFirst() == original
          && original.getCount() == 3, "Chest modifier replaced existing rewards");
      helper.assertTrue(loot.get(1).getItem() instanceof CareItem drive
          && drive.kind == CareItem.Kind.DRIVE && loot.get(2).getItem() instanceof GeneItem,
          "Chest modifier did not supply a registered drive and DNA sample");
    }
    var otherContext = new LootContext.Builder(params)
        .withQueriedLootTableId(ResourceLocation.parse("minecraft:blocks/stone"))
        .create(Optional.empty());
    var untouched = new ObjectArrayList<ItemStack>(); untouched.add(new ItemStack(Items.STONE));
    modifier.apply(untouched, otherContext);
    helper.assertTrue(untouched.size() == 1 && untouched.getFirst().is(Items.STONE),
        "Generic chest modifier leaked into a non-chest table");

    // This vanilla chest was outside the previous short whitelist. Rolling the
    // actual registered table exercises the data-driven GLM registration too.
    var key = ResourceKey.create(Registries.LOOT_TABLE,
        ResourceLocation.withDefaultNamespace("chests/village/village_tannery"));
    var table = helper.getLevel().getServer().reloadableRegistries().getLootTable(key);
    boolean gene = false, drive = false, vanilla = false;
    Random seeds = new Random(0x47454e4553L);
    for (int roll = 0; roll < 256 && !(gene && drive && vanilla); roll++)
      for (var stack : table.getRandomItems(params, seeds.nextLong())) {
        gene |= stack.getItem() instanceof GeneItem;
        drive |= stack.getItem() instanceof CareItem care && care.kind == CareItem.Kind.DRIVE;
        vanilla |= BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("minecraft");
      }
    helper.assertTrue(gene && drive && vanilla,
        "Registered generic chest loot failed or removed the vanilla chest rewards");
    helper.succeed();
  }
}
