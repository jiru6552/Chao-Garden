package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.ChaoSounds;
import com.chaogarden.companion.AccessoriesBridge;
import com.chaogarden.companion.CompanionContent;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.hub.HubContent;
import com.chaogarden.item.ChaoCarrierItem;
import com.chaogarden.kindergarten.KindergartenContent;
import com.chaogarden.market.BlackMarketTrader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Opt-in integration profile using the actual Re:Avaritia and Accessories mods. */
@GameTestHolder("chao_care_tests")
@PrefixGameTestTemplate(false)
public final class CompanionCareUpdateGameTests {
  private static final BlockPos CENTER = new BlockPos(2,2,2);

  private static ItemStack marked(GameTestHelper h,String id) {
    var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    h.assertTrue(item != Items.AIR,"Required real item missing: " + id);
    var stack = new ItemStack(item);
    stack.set(DataComponents.CUSTOM_NAME,Component.literal("Chao transfer " + id));
    if (stack.isDamageableItem()) stack.setDamageValue(17);
    var tag = new CompoundTag();
    tag.putUUID("OwnerProof",java.util.UUID.randomUUID());
    stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    stack.enchant(h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.UNBREAKING),2);
    return stack;
  }

  private static ChaoEntity offer(GameTestHelper h,ItemStack expected) {
    var p = h.makeMockPlayer(GameType.SURVIVAL);
    p.getAbilities().instabuild = false;
    var c = ChaoGarden.CHAO.get().create(h.getLevel());
    c.setNoAi(true);
    c.setNoGravity(true);
    h.setBlock(CENTER.below(),Blocks.STONE);
    c.setPos(h.absolutePos(CENTER).getCenter());
    h.getLevel().addFreshEntity(c);
    p.setItemInHand(InteractionHand.MAIN_HAND,expected.copy());
    c.mobInteract(p,InteractionHand.MAIN_HAND);
    h.assertTrue(p.getMainHandItem().isEmpty() && ItemStack.matches(c.getMainHandItem(),expected)
        && c.getItemAction()==ChaoEntity.ItemAction.INSPECT,"Offer must have one real owner");
    return c;
  }

  private static List<ItemEntity> drops(GameTestHelper h,ItemStack expected) {
    return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(h.absolutePos(CENTER)).inflate(30),
        e -> e.isAlive() && ItemStack.isSameItemSameComponents(e.getItem(),expected));
  }

  private static void transfer(GameTestHelper h,String id,boolean cancel) {
    var expected = marked(h,id);
    var c = offer(h,expected);
    if (cancel) {
      int[] attempts={0};
      Consumer<EntityJoinLevelEvent> veto = event -> {
        if (event.getEntity() instanceof ItemEntity e && ItemStack.matches(e.getItem(),expected)) {
          attempts[0]++;
          h.assertTrue(c.getMainHandItem().isEmpty(),"Reentrant listener saw two owners");
          event.setCanceled(true);
        }
      };
      NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class,veto);
      try {
        WardrobeGameTests.expire(c);
        WardrobeGameTests.expire(c);
        h.assertTrue(attempts[0]==1 && drops(h,expected).isEmpty() && ItemStack.matches(c.getMainHandItem(),expected),
            "Cancelled final entity must leave one exact held stack");
        var saved=c.saveWithoutId(new CompoundTag());
        c.load(saved);
      } finally { NeoForge.EVENT_BUS.unregister(veto); }
    }
    h.runAtTickTime(100,()->{
      var found=drops(h,expected);
      h.assertTrue(found.size()==1 && ItemStack.matches(found.getFirst().getItem(),expected)
          && c.getMainHandItem().isEmpty(),"Wrong item count, durability, name, enchantment or component after release: " + id);
      var drop=found.getFirst();
      h.assertTrue(!c.mayInspectDroppedItem(drop),"Rejected item was immediately collectible by the Chao");
      if (id.contains("infinity")) h.assertTrue(BuiltInRegistries.ENTITY_TYPE.getKey(drop.getType()).getNamespace().equals("avaritia"),
          "Real modded ground entity was lost");
      c.discard(); drop.discard(); h.succeed();
    });
  }

  @GameTest(template="empty",timeoutTicks=120) public static void vanillaSword(GameTestHelper h){transfer(h,"minecraft:diamond_sword",false);}
  @GameTest(template="empty",timeoutTicks=120) public static void cancelledVanillaTool(GameTestHelper h){transfer(h,"minecraft:iron_pickaxe",true);}
  @GameTest(template="empty",timeoutTicks=120) public static void cosmosSword(GameTestHelper h){transfer(h,"avaritia:infinity_sword",false);}
  @GameTest(template="empty",timeoutTicks=120) public static void cancelledCosmosSword(GameTestHelper h){transfer(h,"avaritia:infinity_sword",true);}
  @GameTest(template="empty",timeoutTicks=120) public static void infinityPickaxe(GameTestHelper h){transfer(h,"avaritia:infinity_pickaxe",false);}
  @GameTest(template="empty",timeoutTicks=120) public static void crystalSword(GameTestHelper h){transfer(h,"avaritia:crystal_sword",false);}

  @GameTest(template="empty",timeoutTicks=150)
  public static void deathDuringCancelledDrop(GameTestHelper h) {
    var expected=marked(h,"avaritia:infinity_sword");var c=offer(h,expected);
    Consumer<EntityJoinLevelEvent> veto=e->{if(e.getEntity() instanceof ItemEntity item && ItemStack.matches(item.getItem(),expected))e.setCanceled(true);};
    NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class,veto);
    c.hurt(c.damageSources().genericKill(),Float.MAX_VALUE);
    h.runAtTickTime(36,()->{
      try {
        h.assertTrue(!c.isRemoved() && ItemStack.matches(c.getMainHandItem(),expected) && drops(h,expected).isEmpty(),"Terminal cancellation lost item");
        c.load(c.saveWithoutId(new CompoundTag()));
      } finally {NeoForge.EVENT_BUS.unregister(veto);}
    });
    h.runAtTickTime(80,()->{h.assertTrue(c.isRemoved() && drops(h,expected).size()==1,"Terminal retry failed to return exactly one");h.succeed();});
  }

  @GameTest(template="empty",timeoutTicks=180) public static void acceptedFood(GameTestHelper h){ItemHandlingGameTests.hungryFruitIsHeldThenEatenAndCommittedOnceAcrossReload(h);}
  @GameTest(template="empty",timeoutTicks=180) public static void pickupAndRelease(GameTestHelper h){ItemHandlingGameTests.droppedItemsRespectPickupDelayAndAreInspectedWithoutDeletion(h);}
  @GameTest(template="empty",timeoutTicks=100) public static void keptWearable(GameTestHelper h){WardrobeGameTests.wardrobeOffersHoldThenDecideAndAcceptedChoiceSurvivesMoodChangeAndReload(h);}
  @GameTest(template="empty",timeoutTicks=100) public static void wardrobeRollback(GameTestHelper h){WardrobeGameTests.deniedReplacementReturnRetainsDecisionAndBlocksCaptureDamageAndRecursiveOffers(h);}
  @GameTest(template="empty",timeoutTicks=160) public static void recoveryWithSlotSplit(GameTestHelper h) throws Exception {
    h.assertTrue(AccessoriesBridge.available(),"Actual Accessories required");CompanionGameTests.realAccessoriesEquipDefeatRecoveryAndUnequip(h);
  }

  @GameTest(template="empty",timeoutTicks=1500,batch="companion_equipped")
  public static void careFlightAndFullRecovery(GameTestHelper h) {
    h.assertTrue(AccessoriesBridge.available(),"Actual Accessories required");
    var state=new ChaoState();state.adult=true;state.energy=80;state.hunger=80;state.happiness=40;
    state.ageTicks=1000;state.adultAgeTicks=300;state.setEvolutionStrength(.25f);state.levels[0]=3;
    var e=CompanionSkillGameTests.equip(h,state);
    var keeper=e.keeper();
    h.runAtTickTime(15,()->{
      var c=CompanionSkillGameTests.body(h,e);
      c.getPersistentData().putInt("ChaoEquippedCareTicks",5999);
      h.assertFalse(c.hurt(c.damageSources().fall(),100),"Tagged fall damage reached equipped Chao");
      float hp=c.getHealth();c.causeFallDamage(100,1,c.damageSources().fall());
      h.assertTrue(c.getHealth()==hp,"Actual landing removed equipped health");
      keeper.getAbilities().mayfly=true;keeper.getAbilities().flying=true;keeper.setOnGround(false);
      keeper.teleportTo(keeper.getX(),keeper.getY()+7,keeper.getZ());
    });
    h.runAtTickTime(25,()->{
      var c=CompanionSkillGameTests.body(h,e);
      h.assertTrue(c.isCompanionGripping() && c.companionKeeperId()==keeper.getId() && !keeper.isFallFlying(),"Creative back grip absent");
      var s=c.state();h.assertTrue(s.ageTicks==1000 && s.adultAgeTicks==300 && s.evolutionStrength()>.25f && s.bond==1
          && s.hunger==79 && s.energy==79,"Equipped care clock or paused age wrong");
      keeper.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);c.mobInteract(keeper,InteractionHand.MAIN_HAND);
      h.assertTrue(s.happiness>40 && s.bond>1,"Actual equipped petting did not improve happiness and bond");
      keeper.getAbilities().flying=false;
      int happy=s.happiness, bond=s.bond;
      h.assertTrue(c.hurt(c.damageSources().playerAttack(keeper),1) && s.happiness<happy && s.bond<bond,"Actual keeper injury did not affect care");
      c.invulnerableTime=0;
    });
    h.runAtTickTime(32,()->{
      var c=CompanionSkillGameTests.body(h,e);
      h.assertTrue(!c.isCompanionGripping() && c.companionCarryPosition()==com.chaogarden.companion.CarryRules.HEAD
          && c.companionKeeperId()==keeper.getId() && c.noPhysics,"An adult without the Run unlock did not return to the head after flight");
      c.state().energy=0;keeper.getAbilities().flying=true;keeper.setOnGround(false);
    });
    h.runAtTickTime(40,()->{
      var c=CompanionSkillGameTests.body(h,e);h.assertTrue(c.isCompanionGripping(),"Exhaustion detached a genuinely flying keeper");
      keeper.getAbilities().flying=false;keeper.setOnGround(true);
      var expected=c.state().happiness-10;
      c.hurt(c.damageSources().genericKill(),Float.MAX_VALUE);
      h.assertTrue(c.isRemoved() && CompanionContent.remaining(e.carrier(),keeper.server)==1200,"Recovery duration changed");
      h.assertTrue(ChaoCarrierItem.state(e.carrier()).happiness==expected,"Wrong happiness penalty");
      var tag=e.carrier().save(keeper.registryAccess());var reload=ItemStack.parseOptional(keeper.registryAccess(),(CompoundTag)tag);
      h.assertTrue(ItemStack.matches(reload,e.carrier()) && CompanionContent.remaining(reload,keeper.server)==1200,"Saved carrier lost cooldown or care");
    });
    h.runAtTickTime(1238,()->h.assertTrue(h.getLevel().getEntity(e.chao())==null && CompanionContent.remaining(e.carrier(),keeper.server)>0,"Recovered early"));
    h.runAtTickTime(1250,()->{
      var c=CompanionSkillGameTests.body(h,e);
      h.assertTrue(!c.isCompanionGripping() && c.state().ageTicks==1000 && c.state().adultAgeTicks==300,"Recovery carried flight or lifespan forward");
      CompanionContent.beforeUnequip(keeper,e.carrier());CompanionContent.unequip(keeper,e.carrier());e.inventory().setItem(0,ItemStack.EMPTY);
      var released=ChaoGarden.CHAO.get().create(h.getLevel());released.load(ChaoCarrierItem.entityData(e.carrier()));
      h.assertTrue(!CompanionContent.isManaged(released)&&!released.isNoGravity()&&!released.isCompanionGripping(),"Released body kept companion physics");
      h.assertTrue(released.hurt(released.damageSources().fall(),2),"Fall protection leaked after unequip");
      released.aiStep();h.assertTrue(released.state().adultAgeTicks==301,"Released lifespan did not resume");
      released.discard();CompanionSkillGameTests.finish(h,e);
    });
  }

  @GameTest(template="empty",timeoutTicks=2700,batch="kindergarten_idle")
  public static void observeIdleSchool(GameTestHelper h) {
    var p=TestConnections.makeSurvivalServerPlayer(h);
    var l=p.server.getLevel(KindergartenContent.DIMENSION);var at=KindergartenContent.MARKET_ANCHOR;
    h.assertTrue(l!=null && HubContent.ensure(l),"School missing");
    p.teleportTo(l,at.getX()+3.5,at.getY(),at.getZ()+.5,90,0);p.hasChangedDimension();
    var sounds=new java.util.TreeMap<String,Integer>();var damage=new ArrayList<String>();
    Consumer<PlayLevelSoundEvent.AtPosition> heard=e->{
      if(e.getLevel()==l) sounds.merge(e.getSound().value().getLocation().toString(),1,Integer::sum);
    };
    Consumer<net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post> damaged=e->{
      if(e.getEntity().level()==l) damage.add(e.getEntity().getType()+" "+e.getNewDamage());
    };
    NeoForge.EVENT_BUS.addListener(PlayLevelSoundEvent.AtPosition.class,heard);
    NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post.class,damaged);
    h.runAtTickTime(2400,()->{
      try {
        var residents=l.getEntitiesOfClass(BlackMarketTrader.class,new AABB(at).inflate(4),BlackMarketTrader::resident);
        h.assertTrue(residents.size()==1 && residents.getFirst().getHealth()==residents.getFirst().getMaxHealth(),"Idle resident lost health");
        com.mojang.logging.LogUtils.getLogger().info("CHAO039_IDLE ticks=2400 sounds={} damage={} residentHealth={}",sounds,damage,residents.getFirst().getHealth());
        h.assertTrue(damage.isEmpty() && !sounds.containsKey("chao_garden:chao.hurt"),"Unexpected idle damage/sound: "+damage+" "+sounds);
      } finally {
        NeoForge.EVENT_BUS.unregister(heard);NeoForge.EVENT_BUS.unregister(damaged);
        p.server.getPlayerList().remove(p);
      }
      h.succeed();
    });
  }

  @GameTest(template="empty",timeoutTicks=600,batch="kindergarten_resident")
  public static void marketRefusalAndResidentHealth(GameTestHelper h) {
    var player=TestConnections.makeSurvivalServerPlayer(h);
    var level=player.server.getLevel(KindergartenContent.DIMENSION);var pos=KindergartenContent.MARKET_ANCHOR;
    h.assertTrue(level!=null && HubContent.ensure(level),"School missing");
    player.teleportTo(level,pos.getX()+2.5,pos.getY(),pos.getZ()+.5,0,0);player.hasChangedDimension();
    h.succeedWhen(()->{
      KindergartenContent.ensureServices(level);
      var traders=level.getEntitiesOfClass(BlackMarketTrader.class,new AABB(pos).inflate(4),BlackMarketTrader::resident);
      h.assertTrue(traders.size()==1,"Waiting for the real market resident");
      var t=traders.getFirst();var before=t.getHealth();var sounds=new ArrayList<String>();
      Consumer<PlayLevelSoundEvent.AtPosition> listen=event->{if(event.getLevel()==level && event.getPosition().distanceToSqr(t.position())<.1)
        sounds.add(event.getSound().value().getLocation().toString());};
      NeoForge.EVENT_BUS.addListener(PlayLevelSoundEvent.AtPosition.class,listen);
      try {
        t.notifyTradeUpdated(ItemStack.EMPTY);
        h.assertTrue(sounds.contains(ChaoSounds.AMBIENT.get().getLocation().toString()),"Waiting for non-damage refusal cue: "+sounds);
        h.assertFalse(t.hurt(t.damageSources().inWall(),2),"Resident actually took wall damage");
        h.assertTrue(t.getHealth()==before && t.hurtTime==0,"Resident health changed");
      } finally {NeoForge.EVENT_BUS.unregister(listen);}
      player.server.getPlayerList().remove(player);
    });
  }
}
