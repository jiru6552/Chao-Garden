package com.chaogarden.genes;

import com.chaogarden.ChaoGarden;
import com.chaogarden.item.CareItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;

/** One base-drive roll per hostile death, with vanilla player kill credit. */
@EventBusSubscriber(modid=ChaoGarden.MODID)
public final class EnemyDriveDrops {
  public static final TagKey<EntityType<?>> ENEMIES=TagKey.create(Registries.ENTITY_TYPE,
      ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,"chaos_drive_enemies"));
  private static final String DRAGON_COMMITTED="ChaoDragonDriveCommitted";
  public static final int NORMAL_CHANCE_PERCENT=5;
  private EnemyDriveDrops() {}
  public static boolean equipped(net.minecraft.world.entity.player.Player player){
    return player instanceof net.minecraft.server.level.ServerPlayer
        && com.chaogarden.companion.AccessoriesBridge.equipped(player).stream()
          .anyMatch(com.chaogarden.companion.CompanionContent::isEquippable);
  }
  public static boolean shouldDrop(net.minecraft.world.entity.player.Player player,net.minecraft.util.RandomSource random){
    return equipped(player)||random.nextInt(100)<NORMAL_CHANCE_PERCENT;
  }
  public static net.minecraft.world.entity.player.Player credited(net.minecraft.world.entity.LivingEntity victim){
    // Prefer vanilla's recent player / tamed-wolf credit. Companion attacks use their living body.
    var credit=victim.getKillCredit();
    if(credit instanceof net.minecraft.world.entity.player.Player player)return player;
    if(credit instanceof com.chaogarden.entity.ChaoEntity chao && chao.owner!=null
        && victim.level() instanceof ServerLevel level)
      return level.getServer().getPlayerList().getPlayer(chao.owner);
    return null;
  }
  public static boolean drive(ItemStack stack){
    return stack.getItem() instanceof CareItem item && item.kind==CareItem.Kind.DRIVE
        && !(item instanceof com.chaogarden.item.CombinedDriveItem);
  }
  @SubscribeEvent(priority=EventPriority.LOWEST)
  public static void drops(LivingDropsEvent event){
    var entity=event.getEntity();
    if(entity.level().isClientSide || !(entity instanceof Enemy || entity.getType().getCategory()==MobCategory.MONSTER || entity.getType().is(ENEMIES))
        || !entity.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT))return;
    // Replace legacy base-drive loot pools with one roll. Crafted drives in real gear stay intact.
    event.getDrops().removeIf(drop->drive(drop.getItem()));
    if(entity instanceof EnderDragon && entity.getPersistentData().getBoolean(DRAGON_COMMITTED))return;
    if(entity instanceof EnderDragon)entity.getPersistentData().putBoolean(DRAGON_COMMITTED,true);
    if(!shouldDrop(credited(entity),entity.getRandom()))return;
    String id=TrainingSupplyRules.DRIVES.get(entity.getRandom().nextInt(4));
    event.getDrops().add(new ItemEntity(entity.level(),entity.getX(),entity.getY(),entity.getZ(),
        new ItemStack(ChaoGarden.item(id))));
    if(entity instanceof EnderDragon)entity.getPersistentData().putBoolean(DRAGON_COMMITTED,true);
  }
  /** Vanilla dragons bypass ordinary item-death loot. Their real death sequence
   * emits several experience bursts; one persistent commit covers the sequence.
   * Experience cancellation is unrelated to item rewards and is left untouched. */
  @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true)
  public static void dragonDeath(LivingExperienceDropEvent event){
    if(!(event.getEntity() instanceof EnderDragon dragon) || !(dragon.level() instanceof ServerLevel level)
        || !dragon.isDeadOrDying() || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)
        || dragon.getPersistentData().getBoolean(DRAGON_COMMITTED))return;
    // Commit the roll, including a miss or cancelled spawn, before the next XP burst/reload.
    dragon.getPersistentData().putBoolean(DRAGON_COMMITTED,true);
    if(!shouldDrop(event.getAttackingPlayer()!=null?event.getAttackingPlayer():credited(dragon),dragon.getRandom()))return;
    String id=TrainingSupplyRules.DRIVES.get(dragon.getRandom().nextInt(4));
    level.addFreshEntity(new ItemEntity(level,dragon.getX(),dragon.getY(),dragon.getZ(),new ItemStack(ChaoGarden.item(id))));
  }
}
