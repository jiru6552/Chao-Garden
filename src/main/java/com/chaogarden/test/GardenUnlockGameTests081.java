package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.hub.GardenUnlocks081;
import com.chaogarden.hub.HubWorldData;
import com.chaogarden.item.ChaoCarrierItem;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Isolated SavedData/storage evidence tests never reset server-wide acquisition history. */
@GameTestHolder("chao_unlock81") @PrefixGameTestTemplate(false)
public final class GardenUnlockGameTests081 {
  private static ChaoState child(float alignment){var state=new ChaoState();state.align=alignment;return state;}
  private static ItemStack carrier(ChaoState state){
    var stack=new ItemStack(ChaoGarden.item("chao_carrier"));var entity=new CompoundTag();
    entity.putUUID("UUID",UUID.randomUUID());entity.putUUID("Keeper",UUID.randomUUID());entity.putString("Chao",state.encode());
    ChaoCarrierItem.storeEntity(stack,entity);return stack;
  }
  @GameTest(template="activity_test",timeoutTicks=30) public static void missingLegacyUnlockDataStartsBothLockedAndPreservesJournals(GameTestHelper h){
    var raw=new CompoundTag();var revisions=new CompoundTag();revisions.putInt("chao_garden:hub",38);raw.put("Revisions",revisions);
    var data=HubWorldData.load(raw,h.getLevel().registryAccess());var id=UUID.randomUUID();var visit=new CompoundTag();visit.putString("Dimension","minecraft:overworld");data.put(id,visit);
    h.assertTrue(!data.heroGardenUnlocked()&&!data.darkGardenUnlocked(),"Missing acquisition history is locked, not inferred from old open portals");
    var saved=data.save(new CompoundTag(),h.getLevel().registryAccess());var loaded=HubWorldData.load(saved,h.getLevel().registryAccess());
    h.assertTrue(loaded.visit(id).equals(visit)&&saved.getCompound("Revisions").getInt("chao_garden:hub")==38,"Unlock persistence preserves existing return journal and scene revisions");h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=30) public static void heroHistoryPersistsIndependentlyAndCannotRelock(GameTestHelper h){
    var data=new HubWorldData();h.assertTrue(data.unlockGarden(true)&&!data.unlockGarden(true),"One Hero acquisition commits once");
    var loaded=HubWorldData.load(data.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());
    h.assertTrue(loaded.heroGardenUnlocked()&&!loaded.darkGardenUnlocked(),"Hero history persists without opening Dark");
    h.assertTrue(loaded.unlockGarden(false)&&loaded.allGardensUnlocked(),"Independent later Dark unlock retains Hero");h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=30) public static void darkChildQualifiesAtExistingThresholdWithoutOpeningHero(GameTestHelper h){
    var data=new HubWorldData();h.assertTrue(GardenUnlocks081.record(data,child(-.5f)),"A visibly Dark child is a Dark acquisition");
    h.assertTrue(data.darkGardenUnlocked()&&!data.heroGardenUnlocked(),"Dark child does not open Hero");
    h.assertTrue(GardenUnlocks081.qualifyingAlignment(child(.499f))==0&&GardenUnlocks081.qualifyingAlignment(child(-.499f))==0,"Neutral interval respects existing alignment thresholds");h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=30) public static void adultsUseLockedAlignmentEvenIfRawAlignmentIsEdited(GameTestHelper h){
    var state=child(.75f);state.adult=true;state.primaryType=ChaoState.RUN_TYPE;state=ChaoState.decode(state.encode());state.align=-1;
    h.assertTrue(GardenUnlocks081.qualifyingAlignment(state)==1,"An evolved Hero cannot count as Dark through a raw alignment edit");
    h.assertTrue(state.lockedPrimaryType()==ChaoState.RUN_TYPE,"Unlock observation does not change the locked first type");h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=30) public static void storedCarrierEvidenceIsReadOnlyAndKeepsCreatureIdentity(GameTestHelper h){
    var stack=carrier(child(.8f));stack.set(DataComponents.CUSTOM_NAME,Component.literal("Original stored Hero"));var before=stack.save(h.getLevel().registryAccess());
    var data=new HubWorldData();h.assertTrue(GardenUnlocks081.observeStored(data,stack)&&data.heroGardenUnlocked()&&!data.darkGardenUnlocked(),"Real stored Hero evidence unlocks only its side");
    h.assertTrue(before.equals(stack.save(h.getLevel().registryAccess())),"Evidence scan cannot consume, rewrite, duplicate or retokenize a carrier");h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=30) public static void nestedShulkerAndBundleEvidenceRemainsReadOnly(GameTestHelper h){
    var hero=carrier(child(1));var dark=carrier(child(-1));
    var bundle=new ItemStack(Items.BUNDLE);bundle.set(DataComponents.BUNDLE_CONTENTS,new BundleContents(List.of(dark)));
    var shulker=new ItemStack(Items.SHULKER_BOX);shulker.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND),hero,bundle)));
    var before=shulker.save(h.getLevel().registryAccess());var data=new HubWorldData();
    h.assertTrue(GardenUnlocks081.observeStored(data,shulker)&&data.allGardensUnlocked(),"Accessible ordinary nested containers and bundles preserve both acquisition histories");
    h.assertTrue(before.equals(shulker.save(h.getLevel().registryAccess())),"Nested observation never rewrites components or inventory");h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=30) public static void largeContainerCannotStarveALateTopLevelCarrier(GameTestHelper h){
    var container=new SimpleContainer(600);for(int i=0;i<599;i++)container.setItem(i,new ItemStack(Items.STONE));
    var stack=carrier(child(-.8f));container.setItem(599,stack);var before=stack.save(h.getLevel().registryAccess());var data=new HubWorldData();
    h.assertTrue(GardenUnlocks081.observeStoredContainer(data,container)&&data.darkGardenUnlocked(),"Top-level acquisition pass reaches later slots despite the nested traversal budget");
    h.assertTrue(container.getItem(599)==stack&&before.equals(stack.save(h.getLevel().registryAccess())),"Large-container observation leaves the original slot untouched");h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=30) public static void invalidDeadConstructionAndUnidentifiedItemsDoNotUnlock(GameTestHelper h){
    var data=new HubWorldData();var dead=child(1);dead.dead=true;var construction=child(-1);construction.setTrait("content.construction",1);
    h.assertTrue(!GardenUnlocks081.record(data,dead)&&!GardenUnlocks081.record(data,construction),"Dead and construction states are not qualifying acquisitions");
    var empty=new ItemStack(ChaoGarden.item("chao_carrier"));h.assertTrue(!GardenUnlocks081.observeStored(data,empty),"Empty legacy carriers do not invent a creature");
    var malformed=new CompoundTag();malformed.putString("Chao",child(1).encode());ChaoCarrierItem.storeEntity(empty,malformed);
    h.assertTrue(!GardenUnlocks081.observeStored(data,empty)&&!data.allGardensUnlocked()&&!data.heroGardenUnlocked()&&!data.darkGardenUnlocked(),"A stored state without a creature UUID is not reliable historical evidence");h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=30) public static void administrativeUnlockIsDeniedWithoutServerPermission(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);var data=HubWorldData.get(p.server);var before=data.save(new CompoundTag(),p.registryAccess());boolean denied=false;
    try{p.server.getCommands().getDispatcher().execute("chao gardens unlock all",p.createCommandSourceStack().withPermission(0));}
    catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){denied=true;}
    finally{p.server.getPlayerList().remove(p);}
    h.assertTrue(denied&&before.equals(data.save(new CompoundTag(),h.getLevel().registryAccess())),"Unprivileged command cannot change shared acquisition history or scenes");h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=30) public static void existingOwnedRecoverySnapshotIsReadOnlyAcquisitionEvidence(GameTestHelper h){
    var record=new CompoundTag();record.putUUID("Owner",UUID.randomUUID());record.put("Entity",ChaoCarrierItem.entityData(carrier(child(1))));
    var list=new net.minecraft.nbt.ListTag();list.add(record);var root=new CompoundTag();root.put("Records",list);var before=root.copy();var data=new HubWorldData();
    h.assertTrue(GardenUnlocks081.observeRecoverySnapshot(data,root)&&data.heroGardenUnlocked()&&!data.darkGardenUnlocked(),"Existing pending recovery identifies a previously obtained Hero without publishing a copy");
    h.assertTrue(root.equals(before),"Recovery history, snapshot, tickets and delivery stage are untouched");
    var terminal=child(.8f);terminal.adult=true;terminal.primaryType=ChaoState.RUN_TYPE;terminal=ChaoState.decode(terminal.encode());terminal.dead=true;
    var terminalItem=carrier(terminal);record.put("Entity",ChaoCarrierItem.entityData(terminalItem));var terminalBefore=root.copy();var historical=new HubWorldData();
    h.assertTrue(GardenUnlocks081.observeRecoverySnapshot(historical,root)&&historical.heroGardenUnlocked()&&!historical.darkGardenUnlocked(),"An owned identified terminal snapshot proves the Hero acquired before death");
    h.assertTrue(root.equals(terminalBefore)&&ChaoState.decode(record.getCompound("Entity").getString("Chao")).dead,"Historical detection cannot revive or rewrite the terminal creature");
    var storedHistory=new HubWorldData();var itemBefore=terminalItem.save(h.getLevel().registryAccess());
    h.assertTrue(GardenUnlocks081.observeStored(storedHistory,terminalItem)&&storedHistory.heroGardenUnlocked()&&itemBefore.equals(terminalItem.save(h.getLevel().registryAccess())),"Identified keeper-owned terminal carriers also retain read-only acquisition history");
    record.remove("Owner");var absent=new HubWorldData();h.assertTrue(!GardenUnlocks081.observeRecoverySnapshot(absent,root),"Unowned recovery metadata cannot invent an acquisition");
    record.putUUID("Owner",UUID.randomUUID());terminal.setTrait("content.construction",1);record.getCompound("Entity").putString("Chao",terminal.encode());
    h.assertTrue(!GardenUnlocks081.observeRecoverySnapshot(new HubWorldData(),root),"Terminal construction snapshots remain excluded from historical acquisition");h.succeed();
  }
}
