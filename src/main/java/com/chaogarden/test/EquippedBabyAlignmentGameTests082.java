package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.GardenConfig;
import com.chaogarden.companion.AccessoriesBridge;
import com.chaogarden.companion.BabyKillAlignment082;
import com.chaogarden.companion.CompanionContent;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.EquippedBabyAlignmentRules082;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.genes.EnemyDriveDrops;
import com.chaogarden.hub.HubContent;
import com.chaogarden.item.ChaoCarrierItem;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Actual accepted native deaths and the real optional accessory slot. Embedded
 * connections exercise server state, not a visual or public-network playtest. */
@GameTestHolder("chao_baby_align82") @PrefixGameTestTemplate(false)
public final class EquippedBabyAlignmentGameTests082 {
  private static final String DARK_TICKS="ChaoBabyAlignmentNetherTicks082";
  private static final String HERO_TICKS="ChaoBabyAlignmentHeroTicks082";
  private static final String LAST_TICK="ChaoEquippedCareLastTick082";
  private static final String ALIGNMENT_LAST_TICK="ChaoBabyAlignmentLastTick082";
  private static final BlockPos CENTER=new BlockPos(2,2,2);
  private static final ResourceKey<Level> AETHER=ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse("aether:the_aether"));
  private static final java.util.Map<ServerLevel,java.util.Map<Long,ChunkLease>> FORCED=new java.util.IdentityHashMap<>();
  private static final java.util.Map<GameTestHelper,Integer> SITES=new java.util.IdentityHashMap<>();
  private static final java.util.Set<Integer> USED_SITES=new java.util.HashSet<>();
  private static final class ChunkLease{final boolean previous;int users;ChunkLease(boolean previous){this.previous=previous;}}
  private EquippedBabyAlignmentGameTests082(){}

  private static void near(GameTestHelper h,double actual,double expected,String reason){
    h.assertTrue(Double.isFinite(actual)&&Math.abs(actual-expected)<.000004,reason+" actual="+actual+" expected="+expected);
  }
  private static void cleanup(GameTestHelper h,Runnable action){h.testInfo.addListener(new GameTestListener(){
    public void testStructureLoaded(GameTestInfo i){}
    public void testPassed(GameTestInfo i,GameTestRunner r){action.run();}
    public void testFailed(GameTestInfo i,GameTestRunner r){action.run();}
    public void testAddedForRerun(GameTestInfo a,GameTestInfo b,GameTestRunner r){action.run();}
  });}
  private static ChaoState baby(){var s=new ChaoState();s.energy=100;s.hunger=100;return s;}
  private static CompanionSkillGameTests.Equipped equipped(GameTestHelper h,ChaoState state){
    h.assertTrue(AccessoriesBridge.available(),"Actual Accessories runtime required; a skipped fixture is not a slot check");
    for(int x=0;x<12;x++)for(int z=0;z<12;z++)h.setBlock(new BlockPos(x,1,z),Blocks.STONE);
    var player=TestConnections.makeSurvivalServerPlayer(h);var e=CompanionSkillGameTests.equip(h,state,player);
    cleanup(h,()->{if(player.server.getPlayerList().getPlayer(player.getUUID())==player){CompanionContent.unequip(player,e.inventory().getItem(0));e.inventory().setItem(0,ItemStack.EMPTY);player.server.getPlayerList().remove(player);}});
    return e;
  }
  private static ChaoEntity body(GameTestHelper h,CompanionSkillGameTests.Equipped e){
    var found=CompanionContent.findEntity(e.keeper().server,e.chao());
    h.assertTrue(found instanceof ChaoEntity,"Actual equipped body with the saved creature UUID must exist");return (ChaoEntity)found;
  }
  private static <T extends LivingEntity> T victim(GameTestHelper h,EntityType<T> type,int offset){
    var entity=type.create(h.getLevel());h.assertTrue(entity!=null,"Registered victim type creates");
    var at=h.absolutePos(new BlockPos(7,2,2+offset));entity.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
    entity.setNoGravity(true);if(entity instanceof Mob mob)mob.setNoAi(true);
    h.assertTrue(h.getLevel().addFreshEntity(entity),"Registered victim is a real published entity");cleanup(h,entity::discard);return entity;
  }
  private static void kill(GameTestHelper h,LivingEntity target,ServerPlayer keeper){
    target.invulnerableTime=0;h.assertTrue(target.hurt(target.damageSources().playerAttack(keeper),Float.MAX_VALUE),"Actual player damage reaches native death pipeline");
    h.assertTrue(target.isDeadOrDying(),"Native victim reached accepted death");
  }
  private static double heroKill(){return GardenConfig.BABY_HOSTILE_KILL_ALIGNMENT_POINTS.get()/100.0;}
  private static double darkKill(){return GardenConfig.BABY_PEACEFUL_KILL_ALIGNMENT_POINTS.get()/100.0;}
  private static void forceDestination(GameTestHelper h,ServerLevel level,Vec3 at){
    // Embedded native connections have no real client's simulation tickets.
    // Lease only these finite destination chunks, preserving prior forcing and
    // other concurrent fixtures until the last exact lease is released.
    var center=new ChunkPos(BlockPos.containing(at));
    for(int x=center.x-1;x<=center.x+1;x++)for(int z=center.z-1;z<=center.z+1;z++){
      long key=ChunkPos.asLong(x,z);var leases=FORCED.computeIfAbsent(level,l->new java.util.HashMap<>());
      var lease=leases.computeIfAbsent(key,k->new ChunkLease(level.getForcedChunks().contains(key)));lease.users++;
      // A force ticket schedules loading; it does not guarantee that a fast
      // GameTest's next few ticks have finished preparing a far-away chunk.
      level.getChunk(x,z);
      if(!lease.previous)level.setChunkForced(x,z,true);final int cx=x,cz=z;boolean[] released={false};
      cleanup(h,()->{if(released[0])return;released[0]=true;if(--lease.users==0){leases.remove(key);if(!lease.previous)level.setChunkForced(cx,cz,false);if(leases.isEmpty())FORCED.remove(level);}});
    }
  }
  private static void teleport(GameTestHelper h,ServerPlayer p,ServerLevel level,Vec3 at){
    forceDestination(h,level,at);
    var target=at;
    // Official Aether terrain can occupy the otherwise convenient staging
    // height. Find existing clear sky without carving or replacing any world
    // block; the same finite horizontal chunks are already synchronously ready.
    if(!level.dimension().equals(HubContent.HERO_GARDEN)){
      boolean clear=false;
      for(int y=(int)at.y;y<Math.min(level.getMaxBuildHeight()-5,281);y+=4){
        var base=BlockPos.containing(at.x,y,at.z);boolean open=true;
        for(var cell:BlockPos.betweenClosed(base.offset(-2,0,-2),base.offset(2,4,2)))if(!level.getBlockState(cell).isAir()){open=false;break;}
        if(open){target=new Vec3(at.x,y,at.z);clear=true;break;}
      }
      h.assertTrue(clear,"Finite destination staging area needs clear existing sky in "+level.dimension().location()+" at "+at);
    }
    p.changeDimension(new DimensionTransition(level,target,Vec3.ZERO,0,0,DimensionTransition.DO_NOTHING));p.hasChangedDimension();p.setNoGravity(true);
  }
  private static String diagnostic(CompanionSkillGameTests.Equipped e,ServerLevel expected){
    var p=e.keeper();var found=CompanionContent.findEntity(p.server,e.chao());var stack=e.inventory().getItem(0);
    var entity=ChaoCarrierItem.entityData(stack);var saved=entity.contains("NeoForgeData")?entity.getCompound("NeoForgeData"):entity.getCompound("ForgeData");
    String live=found instanceof ChaoEntity c?bodyDiagnostic(c):"body="+found;
    return " [serverTick="+p.server.getTickCount()+",expected="+expected.dimension().location()+",keeperDim="+p.level().dimension().location()+",keeperPos="+p.position()
        +",keeperUUID="+p.getUUID()+",keeperAlive="+p.isAlive()+",keeperHealth="+p.getHealth()+",slotEmpty="+stack.isEmpty()+",slotUUID="+ChaoCarrierItem.storedUUID(stack)+",expectedUUID="+e.chao()
        +",slotKeeper="+(entity.hasUUID("Keeper")?entity.getUUID("Keeper"):"none")+",slotToken="+CompanionContent.token(stack,false)
        +",recoveryTicks="+CompanionContent.remaining(stack,p.server)+",savedHealth="+entity.getFloat("Health")
        +",savedDark="+saved.getInt(DARK_TICKS)+",savedHero="+saved.getInt(HERO_TICKS)+",savedCare="+saved.getInt("ChaoEquippedCareTicks")
        +",savedCareLast="+saved.getLong(LAST_TICK)+",savedPassiveLast="+saved.getLong(ALIGNMENT_LAST_TICK)+","+live+","+activeDiagnostic(p,stack)+"]";
  }
  private static String bodyDiagnostic(ChaoEntity c){
    var level=(ServerLevel)c.level();var damage=c.getLastDamageSource();
    return "bodyUUID="+c.getUUID()+",bodyDim="+level.dimension().location()+",bodyPos="+c.position()+",managed="+CompanionContent.isManaged(c)
        +",bodyRemoved="+c.isRemoved()+",removalReason="+c.getRemovalReason()+",bodyAlive="+c.isAlive()+",bodyHealth="+c.getHealth()+",bodyDeadState="+c.state().dead
        +",lastDamage="+(damage==null?"none":damage.getMsgId())+",bodyLookupSame="+(level.getEntity(c.getUUID())==c)
        +",bodyTicking="+level.isPositionEntityTicking(c.blockPosition())+",owner="+c.owner+",care="+c.getPersistentData().getInt("ChaoEquippedCareTicks")
        +",dark="+c.getPersistentData().getInt(DARK_TICKS)+",hero="+c.getPersistentData().getInt(HERO_TICKS);
  }
  /** Test-only read access distinguishes a removed/recovering projection from
   * an active object that a cross-mod level lookup has not registered. */
  private static String activeDiagnostic(ServerPlayer player,ItemStack actualStack){
    try{
      var field=CompanionContent.class.getDeclaredField("ACTIVE");field.setAccessible(true);
      var active=((java.util.Map<?,?>)field.get(null)).get(player.getUUID());if(active==null)return "active=null";
      var bodyField=active.getClass().getDeclaredField("body");bodyField.setAccessible(true);
      var stackField=active.getClass().getDeclaredField("stack");stackField.setAccessible(true);
      var lastField=active.getClass().getDeclaredField("lastSeen");lastField.setAccessible(true);
      var activeBody=bodyField.get(active);var activeStack=(ItemStack)stackField.get(active);
      return "activeLastSeen="+lastField.getLong(active)+",activeStackSame="+(activeStack==actualStack)+",activeStackCount="+activeStack.getCount()
          +",activeStackUUID="+ChaoCarrierItem.storedUUID(activeStack)+",activeObject={"+(activeBody instanceof ChaoEntity c?bodyDiagnostic(c):activeBody)+"}";
    }catch(ReflectiveOperationException failure){return "activeDiagnosticUnavailable="+failure;}
  }
  private static void awaitBody(GameTestHelper h,CompanionSkillGameTests.Equipped e,ServerLevel level,long deadline,Consumer<ChaoEntity> action){
    awaitReadyBody(h,e,level,deadline,null,-1,action);
  }
  private static void awaitReadyBody(GameTestHelper h,CompanionSkillGameTests.Equipped e,ServerLevel level,long deadline,ChaoEntity previous,int previousCare,Consumer<ChaoEntity> action){
    var found=CompanionContent.findEntity(e.keeper().server,e.chao());
    var candidate=found instanceof ChaoEntity c&&c.level()==level&&!c.isRemoved()?c:null;
    int care=candidate==null?-1:candidate.getPersistentData().getInt("ChaoEquippedCareTicks");
    if(candidate!=null&&candidate==previous&&level.isPositionEntityTicking(candidate.blockPosition())&&care!=previousCare){action.accept(candidate);return;}
    if(level.dimension().equals(AETHER)&&h.getTick()%100==0)com.mojang.logging.LogUtils.getLogger().info("CHAO_BABY082_AETHER_WAIT{}",diagnostic(e,level));
    h.assertTrue(h.getTick()<deadline,"Actual accessory body needs entity-ticking readiness and real care advancement before measurements"+diagnostic(e,level));
    h.runAfterDelay(1,()->awaitReadyBody(h,e,level,deadline,candidate,care,action));
  }
  private static Vec3 highPosition(GameTestHelper h){
    Integer site=SITES.get(h);
    if(site==null){int next=0;while(USED_SITES.contains(next))next++;site=next;SITES.put(h,site);USED_SITES.add(site);final int assigned=site;
      cleanup(h,()->{SITES.remove(h);USED_SITES.remove(assigned);});}
    // Independent small near-origin plots avoid both million-coordinate
    // generation and interference between concurrently active native cases.
    // Keep the plots outside the End's central dragon-fight arena as well as
    // away from one another; an unrelated boss must not defeat the fixture's
    // companion while it verifies End-to-Aether substitution.
    return new Vec3(2048.5+site*64,160,2048.5);
  }
  private static Container actualSlot(ServerPlayer player){
    try{
      var capabilityType=Class.forName("io.wispforest.accessories.api.AccessoriesCapability");
      var containerType=Class.forName("io.wispforest.accessories.api.AccessoriesContainer");
      var capability=capabilityType.getMethod("get",LivingEntity.class).invoke(null,player);
      var containers=(java.util.Map<?,?>)capabilityType.getMethod("getContainers").invoke(capability);
      return (Container)containerType.getMethod("getAccessories").invoke(containers.get("chao_companion"));
    }catch(ReflectiveOperationException failure){throw new IllegalStateException("Actual Accessories reconnect slot unavailable",failure);}
  }
  private static void actualAccessoryTick(ServerPlayer player,ItemStack stack){
    try{
      var slotType=Class.forName("io.wispforest.accessories.api.slot.SlotReference");
      var accessoryType=Class.forName("io.wispforest.accessories.api.Accessory");
      Object reference=slotType.getMethod("of",LivingEntity.class,String.class,int.class).invoke(null,player,"chao_companion",0);
      Object accessory=Class.forName("io.wispforest.accessories.api.AccessoriesAPI")
          .getMethod("getAccessory",net.minecraft.world.item.Item.class).invoke(null,stack.getItem());
      accessoryType.getMethod("tick",ItemStack.class,slotType).invoke(accessory,stack,reference);
    }catch(ReflectiveOperationException failure){throw new IllegalStateException("Real Accessory.tick callback unavailable",failure);}
  }

  @GameTest(template="activity_test",timeoutTicks=40)
  public static void actualVanillaAndRegisteredModdedClassificationIsConservative(GameTestHelper h){
    var hostile=List.of(EntityType.ZOMBIE,EntityType.SKELETON,EntityType.CREEPER,EntityType.BLAZE,EntityType.HOGLIN,EntityType.PIGLIN_BRUTE);
    var peaceful=List.of(EntityType.COW,EntityType.PIG,EntityType.SHEEP,EntityType.CHICKEN,EntityType.RABBIT,EntityType.CAT,EntityType.COD,EntityType.VILLAGER,EntityType.WANDERING_TRADER);
    var unknown=List.of(EntityType.ENDERMAN,EntityType.ZOMBIFIED_PIGLIN,EntityType.PIGLIN,EntityType.SPIDER,EntityType.WOLF,EntityType.BEE,EntityType.POLAR_BEAR,EntityType.DOLPHIN,EntityType.PUFFERFISH,EntityType.GOAT,EntityType.FOX,EntityType.SNOW_GOLEM,EntityType.IRON_GOLEM);
    for(var type:hostile)h.assertTrue(BabyKillAlignment082.classify(type.create(h.getLevel()))==BabyKillAlignment082.Kind.HOSTILE,"Defensible hostile classification: "+type);
    for(var type:peaceful)h.assertTrue(BabyKillAlignment082.classify(type.create(h.getLevel()))==BabyKillAlignment082.Kind.PEACEFUL,"Known passive or villager classification: "+type);
    for(var type:unknown)h.assertTrue(BabyKillAlignment082.classify(type.create(h.getLevel()))==BabyKillAlignment082.Kind.UNCLASSIFIED,"Neutral/ambiguous entity must not be guessed: "+type);
    var rabbit=EntityType.RABBIT.create(h.getLevel());rabbit.setVariant(Rabbit.Variant.EVIL);
    h.assertTrue(BabyKillAlignment082.classify(rabbit)==BabyKillAlignment082.Kind.HOSTILE,"Vanilla killer rabbit is the hostile special case, not an ordinary passive rabbit");
    h.assertTrue(BabyKillAlignment082.classify(ChaoGarden.CHAO.get().create(h.getLevel()))==BabyKillAlignment082.Kind.PEACEFUL,"The mod's known peaceful Chao type is explicitly classified, rather than guessing every modded Animal");
    if(ModList.get().isLoaded("aether")){
      for(var pair:List.of(new String[]{"aether:zephyr","HOSTILE"},new String[]{"aether:flying_cow","PEACEFUL"},new String[]{"aether:aerwhale","UNCLASSIFIED"})){
        var type=BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(pair[0])).orElseThrow();var entity=type.create(h.getLevel());
        h.assertTrue(entity instanceof LivingEntity&&BabyKillAlignment082.classify((LivingEntity)entity).name().equals(pair[1]),"Actual optional Aether type follows the reviewed classification: "+pair[0]);
      }
    }
    h.succeed();
  }

  @GameTest(template="activity_test",timeoutTicks=80)
  public static void acceptedHostileDeathCountsOnceAndKeepsNormalDriveReward(GameTestHelper h){
    var e=equipped(h,baby());h.runAfterDelay(12,()->{
      var c=body(h,e);var zombie=victim(h,EntityType.ZOMBIE,0);float before=c.state().align;int age=c.state().ageTicks;float strength=c.state().evolutionStrength();
      kill(h,zombie,e.keeper());near(h,c.state().align,before+heroKill(),"One accepted hostile kill gives configured Hero points");
      near(h,ChaoCarrierItem.state(e.inventory().getItem(0)).align,c.state().align,"Alignment is committed to the real carrier immediately");
      h.assertTrue(c.state().ageTicks==age&&c.state().evolutionStrength()==strength,"Kill alignment cannot advance life or visual strength");
      for(int i=0;i<8;i++)zombie.die(zombie.damageSources().playerAttack(e.keeper()));
      near(h,c.state().align,before+heroKill(),"Repeated accepted death callbacks cannot spend the same victim twice");
      int drives=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(zombie.blockPosition()).inflate(1),drop->EnemyDriveDrops.drive(drop.getItem())).stream().mapToInt(drop->drop.getItem().getCount()).sum();
      h.assertTrue(drives==1,"Existing actual equipped hostile drive reward remains exactly one");
      if(ModList.get().isLoaded("aether")){
        int index=0;for(var id:List.of("aether:zephyr","aether:flying_cow")){
          var type=BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(id)).orElseThrow();var entity=type.create(h.getLevel());
          h.assertTrue(entity instanceof LivingEntity,"Actual optional Aether victim is a living registered type: "+id);var target=(LivingEntity)entity;
          var at=h.absolutePos(new BlockPos(7,2,6+index++));target.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);target.setNoGravity(true);if(target instanceof Mob mob)mob.setNoAi(true);
          h.assertTrue(h.getLevel().addFreshEntity(target),"Actual optional mob is published for its native lethal pipeline: "+id);cleanup(h,target::discard);kill(h,target,e.keeper());
          h.assertTrue(EnemyDriveDrops.credited(target)==e.keeper(),"Actual Aether kill retains the same vanilla responsible-player attribution: "+id);
          near(h,c.state().align,before+2*heroKill()-(id.endsWith("flying_cow")?darkKill():0),"Optional hostile/passive deaths apply the reviewed live-mob classification: "+id);
        }
      }
      h.succeed();
    });
  }

  @GameTest(template="activity_test",timeoutTicks=100)
  public static void cowsVillagersAndAnotherPlayerMoveOnlyTheKillerBabyDark(GameTestHelper h){
    var e=equipped(h,baby());var other=TestConnections.makeSurvivalServerPlayer(h);cleanup(h,()->other.server.getPlayerList().remove(other));
    h.runAfterDelay(12,()->{
      var c=body(h,e);float before=c.state().align;kill(h,victim(h,EntityType.COW,0),e.keeper());kill(h,victim(h,EntityType.VILLAGER,2),e.keeper());
      // GameTestServer deliberately disables PvP. Establish real vanilla player
      // credit then run its native die pipeline without changing that policy.
      other.setLastHurtByPlayer(e.keeper());other.setHealth(0);other.die(other.damageSources().playerAttack(e.keeper()));
      h.assertTrue(other.isDeadOrDying(),"Other actual server player reaches the native death pipeline");
      near(h,c.state().align,before-3*darkKill(),"Peaceful, villager and other-player deaths each apply one configured Dark increment");h.succeed();
    });
  }

  @GameTest(template="activity_test",timeoutTicks=100)
  public static void actualLastPlayerProjectileAndTamedPetAttributionStaySeparate(GameTestHelper h){
    var first=equipped(h,baby());var second=equipped(h,baby());
    h.runAfterDelay(12,()->{
      var a=body(h,first);var b=body(h,second);var z=victim(h,EntityType.ZOMBIE,0);
      z.hurt(z.damageSources().playerAttack(first.keeper()),1);z.invulnerableTime=0;kill(h,z,second.keeper());
      near(h,a.state().align,0,"An earlier attacker cannot borrow final player credit");near(h,b.state().align,heroKill(),"The actually credited second player owns its baby's influence");
      var arrow=new Arrow(EntityType.ARROW,h.getLevel());arrow.setOwner(first.keeper());var shot=victim(h,EntityType.ZOMBIE,2);
      h.assertTrue(shot.hurt(shot.damageSources().arrow(arrow,first.keeper()),Float.MAX_VALUE),"Real arrow damage source accepts the player owner");
      near(h,a.state().align,heroKill(),"Projectile owner receives vanilla attribution");
      var wolf=victim(h,EntityType.WOLF,4);wolf.tame(first.keeper());var bitten=victim(h,EntityType.ZOMBIE,6);
      h.assertTrue(bitten.hurt(bitten.damageSources().mobAttack(wolf),Float.MAX_VALUE)&&EnemyDriveDrops.credited(bitten)==first.keeper(),"Vanilla tame-pet kill credit resolves to its actual owner");
      near(h,a.state().align,2*heroKill(),"Existing tamed-pet owner credit is reused without borrowing another baby");near(h,b.state().align,heroKill(),"Other equipped baby's alignment remains isolated");h.succeed();
    });
  }

  @GameTest(template="activity_test",timeoutTicks=100)
  public static void cancelledAndUncreditedDeathsDoNotSpendAlignment(GameTestHelper h){
    var e=equipped(h,baby());h.runAfterDelay(12,()->{
      var c=body(h,e);var z=victim(h,EntityType.ZOMBIE,0);float before=c.state().align;
      Consumer<LivingDeathEvent> veto=event->{if(event.getEntity()==z){z.setHealth(1);event.setCanceled(true);}};
      NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,veto);
      try{z.hurt(z.damageSources().playerAttack(e.keeper()),Float.MAX_VALUE);near(h,c.state().align,before,"A canceled native death cannot apply or commit alignment");}
      finally{NeoForge.EVENT_BUS.unregister(veto);}
      kill(h,z,e.keeper());near(h,c.state().align,before+heroKill(),"Later actual accepted death remains eligible after a veto");
      var uncredited=victim(h,EntityType.ZOMBIE,2);uncredited.hurt(uncredited.damageSources().genericKill(),Float.MAX_VALUE);
      near(h,c.state().align,before+heroKill(),"Uncredited environment death cannot borrow a nearby player's equipped baby");h.succeed();
    });
  }

  @GameTest(template="activity_test",timeoutTicks=100)
  public static void unequippedAtDeathAndAdultsCannotBorrowCachedState(GameTestHelper h){
    var baby=equipped(h,baby());var adultState=baby();adultState.adult=true;adultState.align=.8f;adultState.primaryType=ChaoState.POWER_TYPE;adultState=ChaoState.decode(adultState.encode());var adult=equipped(h,adultState);
    h.runAfterDelay(12,()->{
      var child=body(h,baby);var grown=body(h,adult);float before=child.state().align;String originalAdult=grown.state().encode();
      var stack=baby.inventory().getItem(0);baby.inventory().setItem(0,ItemStack.EMPTY);
      kill(h,victim(h,EntityType.ZOMBIE,0),baby.keeper());near(h,child.state().align,before,"A cached live body cannot count after its actual accessory slot is empty");
      baby.inventory().setItem(0,stack);kill(h,victim(h,EntityType.ZOMBIE,2),adult.keeper());kill(h,victim(h,EntityType.COW,4),adult.keeper());
      h.assertTrue(originalAdult.equals(grown.state().encode()),"Both evolved alignment and first type, plus all adult saved bytes, remain unchanged by both kill signs");
      for(var type:List.of(EntityType.WOLF,EntityType.BEE,EntityType.ENDERMAN))kill(h,victim(h,type,6),baby.keeper());
      near(h,child.state().align,before,"Ambiguous neutral deaths do not guess Hero or Dark");h.succeed();
    });
  }

  @GameTest(template="activity_test",timeoutTicks=1450,batch="baby_alignment82_minute")
  public static void actualNetherMinuteCountsEligibleTicksOnceWithoutAgeOrStatGrowth(GameTestHelper h){
    var e=equipped(h,baby());h.runAfterDelay(12,()->{
      var nether=e.keeper().server.getLevel(Level.NETHER);h.assertTrue(nether!=null,"Actual Nether dimension exists");teleport(h,e.keeper(),nether,highPosition(h));
      awaitBody(h,e,nether,h.getTick()+140,c->{
        var tag=c.getPersistentData();int startTicks=tag.getInt(DARK_TICKS);float start=c.state().align;int age=c.state().ageTicks,adultAge=c.state().adultAgeTicks;long clock=e.keeper().server.getTickCount();
        h.runAfterDelay(1210,()->{
          var current=body(h,e);long elapsed=e.keeper().server.getTickCount()-clock;int intervals=(int)((startTicks+elapsed)/1200);
          near(h,current.state().align,start-intervals*.01,"Actual equipped Nether time spends one displayed Dark point per complete minute");
          h.assertTrue(intervals==1&&Math.abs(current.getPersistentData().getInt(DARK_TICKS)-(startTicks+elapsed)%1200)<=1,"Partial actual-time counter retains the remainder without duplicate ticks");
          h.assertTrue(current.state().ageTicks==age&&current.state().adultAgeTicks==adultAge&&current.state().evolutionStrength()==0,"Passive dimension alignment leaves equipped lifespan and baby maturity paused");
          for(int stat=0;stat<5;stat++)h.assertTrue(current.state().levels[stat]==0&&current.state().progress[stat]==0&&current.state().points[stat]==0,"Passive alignment does not invent stat training");
          int before=tag.getInt(DARK_TICKS),care=current.getPersistentData().getInt("ChaoEquippedCareTicks");
          int legitimate=tag.getLong(LAST_TICK)==e.keeper().server.getTickCount()?0:1;
          int passiveLegitimate=tag.getLong(ALIGNMENT_LAST_TICK)==e.keeper().server.getTickCount()?0:1;
          actualAccessoryTick(e.keeper(),e.inventory().getItem(0));actualAccessoryTick(e.keeper(),e.inventory().getItem(0));
          CompanionContent.tickChao(current);CompanionContent.tickChao(current);
          h.assertTrue(current.getPersistentData().getInt(DARK_TICKS)==(before+passiveLegitimate)%1200
              &&current.getPersistentData().getInt("ChaoEquippedCareTicks")==Math.floorMod(care+legitimate,6000),"Repeated production care invocation at one server time cannot advance either clock twice");h.succeed();
        });
      });
    });
  }

  @GameTest(template="activity_test",timeoutTicks=3400,batch="baby_alignment82_selection")
  public static void actualEndOrInstalledAetherSelectsExactlyOneHeroDimension(GameTestHelper h){
    var e=equipped(h,baby());h.runAfterDelay(12,()->{
      boolean installed=ModList.get().isLoaded("aether");var end=e.keeper().server.getLevel(Level.END);h.assertTrue(end!=null,"Actual End exists");teleport(h,e.keeper(),end,highPosition(h));
      awaitBody(h,e,end,h.getTick()+140,c->{
        com.mojang.logging.LogUtils.getLogger().info("CHAO_BABY082_SELECTION stage=end-ready{}",diagnostic(e,end));
        c.getPersistentData().putInt(HERO_TICKS,1199);float before=c.state().align;
        h.runAfterDelay(12,()->{
          var inEnd=body(h,e);near(h,inEnd.state().align,before+(installed?0:.01),"The actual End grants Hero only when the Aether mod is absent");
          com.mojang.logging.LogUtils.getLogger().info("CHAO_BABY082_SELECTION stage=end-checked{}",diagnostic(e,end));
          if(!installed){h.assertTrue(inEnd.getPersistentData().getInt(HERO_TICKS)<20,"End spent precisely the seeded remainder");h.succeed();return;}
          h.assertTrue(inEnd.getPersistentData().getInt(HERO_TICKS)==1199,"Installed Aether substitutes rather than accumulating a second End influence");
          var aether=e.keeper().server.getLevel(AETHER);h.assertTrue(aether!=null,"With-mod runtime must provide the real official Aether level; string-only detection is not a dimension test");
          teleport(h,e.keeper(),aether,highPosition(h));long entered=e.keeper().server.getTickCount();
          com.mojang.logging.LogUtils.getLogger().info("CHAO_BABY082_SELECTION stage=aether-transitioned{}",diagnostic(e,aether));
          // The actual noise/lighting/entity-accessibility futures of a real
          // Aether world can outlast140 accelerated GameTest ticks. Require
          // genuine registered, ticking, care-advancing readiness; do not force
          // or substitute a projection merely to finish the fixture sooner.
          awaitBody(h,e,aether,h.getTick()+3000,inAether->{
            com.mojang.logging.LogUtils.getLogger().info("CHAO_BABY082_SELECTION stage=aether-ready{}",diagnostic(e,aether));
            h.runAfterDelay(12,()->{
              var current=body(h,e);long elapsed=e.keeper().server.getTickCount()-entered;
              int remainder=current.getPersistentData().getInt(HERO_TICKS);boolean matched=false;
              // Accessory and GameTest callbacks can observe opposite ends of
              // one server tick. Both the actual partial counter and earned
              // whole points must match that same one-tick-bounded timeline.
              for(int phase=-1;phase<=1;phase++){
                long total=1199+Math.max(0,elapsed+phase),minutes=total/1200;
                if(remainder==total%1200&&minutes>=1&&Math.abs(current.state().align-(before+minutes*.01))<.000004)matched=true;
              }
              h.assertTrue(matched,"Actual Aether equipped time consumes the saved1199 remainder and all additional complete minutes exactly once; elapsed="+elapsed
                  +",remainder="+remainder+",alignment="+current.state().align+",before="+before+diagnostic(e,aether));
              h.assertTrue(current.state().ageTicks==0&&current.state().adultAgeTicks==0&&current.state().evolutionStrength()==0,"Legitimate Aether readiness time cannot advance baby lifespan or maturity");h.succeed();
            });
          });
        });
      });
    });
  }

  @GameTest(template="activity_test",timeoutTicks=340,batch="baby_alignment82_pause")
  public static void savedCarrierPauseAndReequipRetainBothIndependentCounters(GameTestHelper h){
    var e=equipped(h,baby());h.runAfterDelay(12,()->{
      var c=body(h,e);c.getPersistentData().putInt(DARK_TICKS,1150);c.getPersistentData().putInt(HERO_TICKS,731);CompanionContent.beforeUnequip(e.keeper(),e.inventory().getItem(0));
      var original=e.inventory().getItem(0);CompanionContent.unequip(e.keeper(),original);e.inventory().setItem(0,ItemStack.EMPTY);
      var saved=original.save(e.keeper().registryAccess());var reload=ItemStack.parseOptional(e.keeper().registryAccess(),(CompoundTag)saved);
      int storedSlot=e.keeper().getInventory().getFreeSlot();h.assertTrue(storedSlot>=0,"Fixture has an actual empty inventory slot for the serialized Chao");e.keeper().getInventory().setItem(storedSlot,reload);
      h.assertTrue(ChaoCarrierItem.entityData(reload).getCompound("NeoForgeData").getInt(DARK_TICKS)==1150&&ChaoCarrierItem.entityData(reload).getCompound("NeoForgeData").getInt(HERO_TICKS)==731,"Both partial dimension counters survive actual item serialization");
      var frozen=ChaoCarrierItem.entityData(reload).copy();
      h.runAfterDelay(25,()->{
        h.assertTrue(ChaoCarrierItem.entityData(reload).equals(frozen),"Inventory-only Chao receives no background equipped time");
        e.keeper().getInventory().removeItem(reload);e.inventory().setItem(0,reload);var nether=e.keeper().server.getLevel(Level.NETHER);teleport(h,e.keeper(),nether,highPosition(h));
        awaitBody(h,e,nether,h.getTick()+140,back->{
          h.runAfterDelay(65,()->{
            var current=body(h,e);near(h,current.state().align,-.01,"Only the remaining actual equipped Nether ticks complete the paused minute");
            h.assertTrue(current.getPersistentData().getInt(HERO_TICKS)==731,"Nether time cannot spend or add to the separate Hero remainder");h.succeed();
          });
        });
      });
    });
  }

  @GameTest(template="activity_test",timeoutTicks=200,batch="baby_alignment82_garden")
  public static void existingGardenChildInfluenceDoesNotAlsoTickTheDimensionMinute(GameTestHelper h){
    var e=equipped(h,baby());h.runAfterDelay(12,()->{
      var hero=e.keeper().server.getLevel(HubContent.HERO_GARDEN);h.assertTrue(hero!=null&&HubContent.ensure(hero),"Actual Hero Garden is available");
      teleport(h,e.keeper(),hero,com.chaogarden.hub.HeroGardenRooms0253.SPAWN);
      awaitBody(h,e,hero,h.getTick()+140,c->{
        c.getPersistentData().putInt(HERO_TICKS,1199);float alignment=c.state().align;int care=c.getPersistentData().getInt("ChaoEquippedCareTicks");long clock=e.keeper().server.getTickCount();
        h.runAfterDelay(25,()->{
          var current=body(h,e);int elapsed=Math.floorMod(current.getPersistentData().getInt("ChaoEquippedCareTicks")-care,6000);
          h.assertTrue(Math.abs(elapsed-(e.keeper().server.getTickCount()-clock))<=1,"Ordinary equipped care still runs once per actual tick careDelta="+elapsed+",serverDelta="+(e.keeper().server.getTickCount()-clock)+diagnostic(e,hero));
          near(h,current.state().align,alignment+elapsed/(double)GardenConfig.CHILD_TICKS.get(),"Existing child garden nudge is applied once and remains independent");
          h.assertTrue(current.getPersistentData().getInt(HERO_TICKS)==1199,"Hero Garden is not the End/Aether and cannot spend its passive dimension clock");h.succeed();
        });
      });
    });
  }

  @GameTest(template="activity_test",timeoutTicks=700,batch="baby_alignment82_reconnect")
  public static void realPlayerFileReconnectPreservesPartialTimeWithoutOfflineCatchup(GameTestHelper h){
    var e=equipped(h,baby());var reconnected=new ServerPlayer[1];
    // The normal fixture already steps the original embedded connection. Only
    // the new connection needs this same native step after real relogin.
    h.onEachTick(()->{
      var p=reconnected[0];if(p==null||p.isRemoved()||p.server.getPlayerList().getPlayer(p.getUUID())!=p)return;
      if(!p.server.getConnection().getConnections().contains(p.connection.getConnection())){p.doTick();p.serverLevel().getChunkSource().move(p);}
    });
    cleanup(h,()->{
      var p=reconnected[0];if(p==null||p.server.getPlayerList().getPlayer(p.getUUID())!=p)return;
      var slot=actualSlot(p);CompanionContent.unequip(p,slot.getItem(0));slot.setItem(0,ItemStack.EMPTY);p.server.getPlayerList().remove(p);
    });
    h.runAfterDelay(12,()->{
      var nether=e.keeper().server.getLevel(Level.NETHER);teleport(h,e.keeper(),nether,highPosition(h));
      awaitBody(h,e,nether,h.getTick()+140,c->{
        c.getPersistentData().putInt(DARK_TICKS,1000);c.getPersistentData().putInt(HERO_TICKS,77);
        CompanionContent.itemStateChanged(c);UUID keeper=e.keeper().getUUID();float alignment=c.state().align;
        e.keeper().server.getPlayerList().remove(e.keeper());
        h.assertTrue(CompanionContent.findEntity(h.getLevel().getServer(),e.chao())==null,"Logout recalls the live projection before its ordinary player file is saved");
        h.runAfterDelay(40,()->{
          var p=TestConnections.makeSurvivalServerPlayer(h,keeper);reconnected[0]=p;
          h.assertTrue(AccessoriesBridge.equipped(p).size()==1,"Actual PlayerList login restores exactly one accessory from the saved player file");
          var slot=actualSlot(p);var stack=slot.getItem(0);var raw=ChaoCarrierItem.entityData(stack);
          h.assertTrue(e.chao().equals(ChaoCarrierItem.storedUUID(stack))&&raw.getCompound("NeoForgeData").getInt(DARK_TICKS)==1000
              &&raw.getCompound("NeoForgeData").getInt(HERO_TICKS)==77,"Actual disk login retains the same creature and both partial dimension clocks");
          var resumed=new CompanionSkillGameTests.Equipped(p,slot,stack,e.chao());
          teleport(h,p,nether,highPosition(h));long start=p.server.getTickCount();
          awaitBody(h,resumed,nether,h.getTick()+140,back->{
            h.runAfterDelay(20,()->{
              var current=body(h,resumed);long elapsed=p.server.getTickCount()-start;
              h.assertTrue(Math.abs(current.getPersistentData().getInt(DARK_TICKS)-1000-elapsed)<=2,"Only newly equipped online ticks accrue; forty offline ticks are not replayed");
              near(h,current.state().align,alignment,"Reconnecting cannot grant a fresh immediate minute");
              h.runAfterDelay(210,()->{
                var last=body(h,resumed);near(h,last.state().align,alignment-.01,"The saved minute completes only after its remaining actual online time");
                h.assertTrue(last.getPersistentData().getInt(HERO_TICKS)==77&&last.state().ageTicks==0,"Reconnect preserves the other dimension remainder and paused baby age");h.succeed();
              });
            });
          });
        });
      });
    });
  }

  @GameTest(template="activity_test",timeoutTicks=100)
  public static void actualDragonLethalPartHitUsesInitialKillTimeNotLaterExperienceBursts(GameTestHelper h){
    var e=equipped(h,baby());h.runAfterDelay(12,()->{
      var c=body(h,e);float before=c.state().align;var dragon=victim(h,EntityType.ENDER_DRAGON,0);
      // Newly constructed dragons HOVER (a sitting phase); an ordinary
      // airborne fight must be established before testing its DYING route.
      dragon.getPhaseManager().setPhase(net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.HOLDING_PATTERN);dragon.setHealth(1);
      h.assertTrue(dragon.hurt(dragon.head,dragon.damageSources().playerAttack(e.keeper()),1000),"Actual player head-part hit enters the dragon's lethal damage route");
      h.assertTrue(dragon.getPhaseManager().getCurrentPhase().getPhase()==net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.DYING,
          "The real dragon restores one health and enters its delayed DYING phase; actual="+dragon.getPhaseManager().getCurrentPhase().getPhase()+",health="+dragon.getHealth());
      near(h,c.state().align,before+heroKill(),"Accepted lethal hit credits the equipped baby before the dragon's delayed XP sequence");
      CompanionContent.unequip(e.keeper(),e.inventory().getItem(0));e.inventory().setItem(0,ItemStack.EMPTY);
      dragon.setHealth(0); // The later real XP-burst stage uses its terminal health.
      for(int i=0;i<8;i++)NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent(dragon,e.keeper(),100));
      near(h,ChaoCarrierItem.state(e.carrier()).align,before+heroKill(),"Later native XP bursts cannot re-credit a recalled baby or reinterpret the kill-time slot");h.succeed();
    });
  }

  @GameTest(template="activity_test",timeoutTicks=80)
  public static void immediateActualSlotWorksBeforeSummonButRejectsConflictingUuidAndKeeper(GameTestHelper h){
    var e=equipped(h,baby());
    h.assertTrue(CompanionContent.findEntity(e.keeper().server,e.chao())==null,"Fixture is the real equipped slot before its first summon callback");
    kill(h,victim(h,EntityType.ZOMBIE,0),e.keeper());
    near(h,ChaoCarrierItem.state(e.inventory().getItem(0)).align,heroKill(),"A valid equipped baby receives kill alignment before a live projection exists");
    var raw=ChaoCarrierItem.entityData(e.inventory().getItem(0));var ordinary=ChaoGarden.CHAO.get().create(h.getLevel());ordinary.load(raw);
    var at=h.absolutePos(new BlockPos(4,2,7));ordinary.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);ordinary.setNoAi(true);ordinary.setNoGravity(true);
    h.assertTrue(h.getLevel().addFreshEntity(ordinary),"An actual ordinary body can reveal a conflicting copied creature UUID");cleanup(h,ordinary::discard);
    kill(h,victim(h,EntityType.ZOMBIE,2),e.keeper());
    near(h,ChaoCarrierItem.state(e.inventory().getItem(0)).align,heroKill(),"A slot cannot use an item fallback while its creature UUID already exists outside its own managed projection");
    ordinary.discard();raw=ChaoCarrierItem.entityData(e.inventory().getItem(0));raw.putUUID("Keeper",UUID.randomUUID());ChaoCarrierItem.storeEntity(e.inventory().getItem(0),raw);
    kill(h,victim(h,EntityType.ZOMBIE,4),e.keeper());
    near(h,ChaoCarrierItem.state(e.inventory().getItem(0)).align,heroKill(),"An unequippable foreign keeper cannot borrow its holder's kill credit");h.succeed();
  }

  @GameTest(template="activity_test",timeoutTicks=160,batch="baby_alignment82_unsummoned")
  public static void recoveryEquippedCarrierStillCountsActualDimensionTimeAndKillsWithoutABody(GameTestHelper h){
    var e=equipped(h,baby());h.runAfterDelay(12,()->{
      var stack=e.inventory().getItem(0);CompanionContent.beforeUnequip(e.keeper(),stack);CompanionContent.unequip(e.keeper(),stack);e.inventory().setItem(0,ItemStack.EMPTY);
      var data=ChaoCarrierItem.data(stack);data.putLong(CompanionContent.DISABLED_UNTIL,e.keeper().server.overworld().getGameTime()+4000);ChaoCarrierItem.setData(stack,data);
      var entity=ChaoCarrierItem.entityData(stack);var saved=entity.getCompound("NeoForgeData");saved.putInt(DARK_TICKS,1190);saved.putInt(HERO_TICKS,88);entity.put("NeoForgeData",saved);ChaoCarrierItem.storeEntity(stack,entity);
      e.inventory().setItem(0,stack);var nether=e.keeper().server.getLevel(Level.NETHER);teleport(h,e.keeper(),nether,highPosition(h));
      h.runAfterDelay(15,()->{
        h.assertTrue(CompanionContent.findEntity(e.keeper().server,e.chao())==null&&CompanionContent.remaining(e.inventory().getItem(0),e.keeper().server)>0,"An actually recovering equipped carrier has no live body");
        var current=e.inventory().getItem(0);var state=ChaoCarrierItem.state(current);near(h,state.align,-.01,"Actual equipped Nether callbacks finish the saved minute even during recovery");
        var timer=ChaoCarrierItem.entityData(current).getCompound("NeoForgeData");
        h.assertTrue(timer.getInt(DARK_TICKS)<20&&timer.getInt(HERO_TICKS)==88&&state.ageTicks==0&&state.evolutionStrength()==0,"Unsummoned timer persists independently of the other counter and paused ordinary care");
        var zombie=EntityType.ZOMBIE.create(nether);zombie.moveTo(e.keeper().getX()+4,e.keeper().getY(),e.keeper().getZ(),0,0);zombie.setNoGravity(true);zombie.setNoAi(true);
        h.assertTrue(nether.addFreshEntity(zombie),"Actual Nether victim can be published near the recovering keeper");cleanup(h,zombie::discard);kill(h,zombie,e.keeper());
        near(h,ChaoCarrierItem.state(e.inventory().getItem(0)).align,-.01+heroKill(),"At-death slot verification also applies to a genuine equipped recovery item");h.succeed();
      });
    });
  }
}
