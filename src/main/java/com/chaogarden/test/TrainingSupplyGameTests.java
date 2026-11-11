package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.genes.EnemyDriveDrops;
import com.chaogarden.genes.GeneItem;
import com.chaogarden.genes.GenericChestTrainingModifier;
import com.chaogarden.genes.TrainingSupplyRules;
import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class TrainingSupplyGameTests {
  private static AABB area(GameTestHelper h){return new AABB(h.absolutePos(new BlockPos(2,2,2))).inflate(1.4);}
  private static int drives(GameTestHelper h){return h.getLevel().getEntitiesOfClass(ItemEntity.class,area(h),e->EnemyDriveDrops.drive(e.getItem())).stream().mapToInt(e->e.getItem().getCount()).sum();}
  private static void clearDrives(GameTestHelper h){h.getLevel().getEntitiesOfClass(ItemEntity.class,area(h),e->EnemyDriveDrops.drive(e.getItem())).forEach(ItemEntity::discard);}
  private static Zombie zombie(GameTestHelper h){
    var mob=EntityType.ZOMBIE.create(h.getLevel());mob.setNoAi(true);mob.setNoGravity(true);
    mob.moveTo(Vec3.atCenterOf(h.absolutePos(new BlockPos(2,2,2))));h.getLevel().addFreshEntity(mob);return mob;
  }
  @GameTest(template="empty",timeoutTicks=80)
  public static void actualEnemyKillsRollRareBaseDriveIncludingLootingAndHeldDrops(GameTestHelper h){
    var player=TestConnections.makeSurvivalServerPlayer(h);var sword=new ItemStack(Items.NETHERITE_SWORD);
    sword.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING),5);
    player.setItemSlot(EquipmentSlot.MAINHAND,sword);
    var seen=new java.util.HashSet<String>();
    for(int trial=0;trial<512;trial++){
      clearDrives(h);var mob=zombie(h);mob.getRandom().setSeed(24000+trial);
      if(trial%2==0){mob.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(ChaoGarden.item("run_drive"),8));mob.setDropChance(EquipmentSlot.MAINHAND,2);}
      mob.hurt(trial%3==0?mob.damageSources().genericKill():mob.damageSources().playerAttack(player),Float.MAX_VALUE);
      h.assertTrue(drives(h)<=1,"Enemy kill produced multiple base drives, including Looting/held gear trial"+trial);
      for(var drop:h.getLevel().getEntitiesOfClass(ItemEntity.class,area(h),e->EnemyDriveDrops.drive(e.getItem())))
        seen.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(drop.getItem().getItem()).getPath());
      mob.discard();
    }
    h.assertTrue(seen.size()==4,"Rare enemy reward did not reach all four drive colors");clearDrives(h);
    var cow=EntityType.COW.create(h.getLevel());cow.moveTo(Vec3.atCenterOf(h.absolutePos(new BlockPos(2,2,2))));h.getLevel().addFreshEntity(cow);
    cow.hurt(cow.damageSources().genericKill(),Float.MAX_VALUE);h.assertTrue(drives(h)==0,"Ordinary animal kill received hostile drive reward");cow.discard();
    boolean previous=h.getLevel().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT);
    try{
      h.getLevel().getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(false,h.getLevel().getServer());
      var mob=zombie(h);mob.hurt(mob.damageSources().genericKill(),Float.MAX_VALUE);h.assertTrue(drives(h)==0,"Drive reward bypassed doMobLoot=false");mob.discard();
    }finally{h.getLevel().getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(previous,h.getLevel().getServer());}
    var cancelledDrops=new java.util.ArrayList<ItemEntity>();var original=new ItemEntity(h.getLevel(),0,0,0,new ItemStack(Items.DIAMOND,3));cancelledDrops.add(original);
    var cancelledMob=zombie(h);var event=new net.neoforged.neoforge.event.entity.living.LivingDropsEvent(cancelledMob,cancelledMob.damageSources().generic(),cancelledDrops,false);
    event.setCanceled(true);net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
    h.assertTrue(cancelledDrops.size()==1&&cancelledDrops.getFirst()==original,"Cancelled death loot was modified by guaranteed-drive hook");cancelledMob.discard();
    player.server.getPlayerList().remove(player);h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=60)
  public static void dragonDeathSequenceAwardsOneDriveAcrossAllExperienceBurstsAndReload(GameTestHelper h){
    var dragon=EntityType.ENDER_DRAGON.create(h.getLevel());var origin=Vec3.atCenterOf(h.absolutePos(new BlockPos(2,2,2)));
    dragon.moveTo(origin);dragon.setNoAi(true);dragon.setSilent(true);dragon.setHealth(0);dragon.getRandom().setSeed(24024);
    var region=new AABB(origin.x-3,origin.y-1,origin.z-3,origin.x+3,origin.y+40,origin.z+3);
    try{
      var method=dragon.getClass().getDeclaredMethod("tickDeath");method.setAccessible(true);
      for(int n=0;n<170;n++)method.invoke(dragon);
      int committed=h.getLevel().getEntitiesOfClass(ItemEntity.class,region,e->EnemyDriveDrops.drive(e.getItem())).stream().mapToInt(e->e.getItem().getCount()).sum();
      h.assertTrue(committed<=1&&dragon.getPersistentData().getBoolean("ChaoDragonDriveCommitted"),"Dragon must commit one rare roll across XP bursts");
      var saved=new net.minecraft.nbt.CompoundTag();dragon.saveWithoutId(saved);
      var restored=EntityType.ENDER_DRAGON.create(h.getLevel());restored.load(saved);restored.setSilent(true);
      for(int n=0;n<200;n++)method.invoke(restored);
      h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,region,e->EnemyDriveDrops.drive(e.getItem())).stream().mapToInt(e->e.getItem().getCount()).sum()==committed,
          "Reloading a committed dragon death duplicated the drive");
      restored.discard();dragon.discard();h.getLevel().getEntitiesOfClass(ItemEntity.class,region,e->EnemyDriveDrops.drive(e.getItem())).forEach(ItemEntity::discard);
    }catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
    h.succeed();
  }

  @GameTest(template="empty",timeoutTicks=40)
  public static void weightedGenesCoverAllRegisteredAnimalsInAnyDeclaredChest(GameTestHelper h){
    h.assertTrue(TrainingSupplyRules.ANIMALS.equals(java.util.List.of(ChaoGarden.ANIMALS)),"Runtime animal order diverged from weighted samples");
    var params=new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.ORIGIN,Vec3.ZERO).create(LootContextParamSets.CHEST);
    var source=new GenericChestTrainingModifier(new LootItemCondition[0],0,1);var found=new java.util.HashSet<Integer>();
    for(int seed=0;seed<4096;seed++){
      var context=new LootContext.Builder(params).withQueriedLootTableId(ResourceLocation.parse("chao_garden:distribution_test/unconventional"))
          .withOptionalRandomSeed(seed).create(Optional.empty());
      var loot=new it.unimi.dsi.fastutil.objects.ObjectArrayList<ItemStack>();var original=new ItemStack(Items.DIAMOND,2);loot.add(original);source.apply(loot,context);
      h.assertTrue(loot.size()==2&&loot.getFirst()==original&&original.getCount()==2&&loot.get(1).getItem() instanceof GeneItem,"Declared arbitrary-ID chest failed one additive DNA roll");
      found.add(((GeneItem)loot.get(1).getItem()).index);
    }
    h.assertTrue(found.size()==31,"Some registered DNA variants cannot appear in arbitrary chest loot");
    for(String id:new String[]{"minecraft:entities/zombie","minecraft:blocks/stone","chao_garden:distribution_test/fishing","chao_garden:distribution_test/chests/generic"}){
      var context=new LootContext.Builder(params).withQueriedLootTableId(ResourceLocation.parse(id)).create(Optional.empty());
      var loot=new it.unimi.dsi.fastutil.objects.ObjectArrayList<ItemStack>();source.apply(loot,context);h.assertTrue(loot.isEmpty(),"Unproven nonchest source gained chest DNA");
    }
    var pos=new BlockPos(2,2,2);h.setBlock(pos,Blocks.CHEST);var chest=(net.minecraft.world.Container)h.getLevel().getBlockEntity(h.absolutePos(pos));
    var table=h.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
        ResourceLocation.parse("chao_garden:distribution_test/chests/generic")));
    boolean gene=false;var random=new Random(24024);
    for(int n=0;n<256&&!gene;n++){
      chest.clearContent();table.fill(chest,params,random.nextLong());
      for(int slot=0;slot<chest.getContainerSize();slot++)gene|=chest.getItem(slot).getItem() instanceof GeneItem;
    }
    h.assertTrue(gene,"Genuine generic-table physical chest did not receive registered DNA policy");h.succeed();
  }
}
