package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.companion.*;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.EquippedAbilityRules080;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoCarrierItem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Actual Accessories ownership, native melee hooks and registered packet handlers.
 * Embedded channels do not replace the separate genuine-client multiplayer checks. */
@GameTestHolder("chao_bonus80") @PrefixGameTestTemplate(false)
public final class EquippedAbilityGameTests080 {
  private static final String TEMPLATE="chao_bonus80:activity_test";
  private static final BlockPos CENTER=new BlockPos(2,2,2);
  private static final java.util.Map<MinecraftServer,PvpLease> PVP_LEASES=new java.util.IdentityHashMap<>();
  private static final class PvpLease {
    final boolean previous;int users;
    PvpLease(MinecraftServer server){previous=server.isPvpAllowed();}
  }
  private static void near(GameTestHelper h,double actual,double expected,double tolerance,String message){h.assertTrue(Double.isFinite(actual)&&Math.abs(actual-expected)<=tolerance,message+" actual="+actual+" expected="+expected);}
  private static void cleanup(GameTestHelper h,Runnable action){h.testInfo.addListener(new GameTestListener(){
    public void testStructureLoaded(GameTestInfo i){}
    public void testPassed(GameTestInfo i,GameTestRunner r){action.run();}
    public void testFailed(GameTestInfo i,GameTestRunner r){action.run();}
    public void testAddedForRerun(GameTestInfo a,GameTestInfo b,GameTestRunner r){action.run();}
  });}
  private static void pvpFixture(GameTestHelper h,MinecraftServer server){
    // GameTestServer defaults PvP off. Lease its original policy across all
    // concurrent player-target fixtures and restore after the last cleanup.
    var lease=PVP_LEASES.computeIfAbsent(server,PvpLease::new);lease.users++;server.setPvpAllowed(true);
    boolean[] released={false};cleanup(h,()->{
      if(released[0])return;released[0]=true;
      if(--lease.users==0){PVP_LEASES.remove(server,lease);server.setPvpAllowed(lease.previous);}
    });
  }
  private static ChaoState state(int stat,int alignment){
    var s=ChaoState.egg(0,false,true,0,new Random(8080+stat));s.adult=true;s.align=alignment;s.primaryType=stat+1;
    s.grades[stat]=5;s.gradeA[stat]=s.gradeB[stat]=5;
    if(!s.useHyperFruit(stat))throw new AssertionError("Native expressed-X fixture failed");
    s.levels[stat]=99;s.points[stat]=9900;s.energy=100;s.hunger=100;s.setEvolutionStrength(0);
    return ChaoState.decode(s.encode());
  }
  private static CompanionSkillGameTests.Equipped equipped(GameTestHelper h,int stat,int alignment){
    h.assertTrue(AccessoriesBridge.available(),"Required real Accessories runtime must be enabled for bonus tests");
    for(int x=0;x<12;x++)for(int z=0;z<12;z++)h.setBlock(new BlockPos(x,1,z),Blocks.STONE);
    var p=TestConnections.makeSurvivalServerPlayer(h);p.getInventory().clearContent();
    var e=CompanionSkillGameTests.equip(h,state(stat,alignment),p);
    cleanup(h,()->{
      CompanionContent.unequip(p,e.inventory().getItem(0));e.inventory().setItem(0,ItemStack.EMPTY);
      p.server.getPlayerList().remove(p);
    });return e;
  }
  private static void air(ServerPlayer p){p.setShiftKeyDown(true);p.setXRot(0);p.setYRot(0);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);}
  private static boolean cycle(CompanionSkillGameTests.Equipped e,int sequence){
    return CompanionContent.requestAbilityCycle080(e.keeper(),sequence,e.keeper().level().dimension().location(),e.chao());
  }
  private static double amount(ServerPlayer p){var m=p.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(CompanionContent.RUN_SPEED);return m==null?0:m.amount();}
  private static void charged(ServerPlayer p){
    try{var f=LivingEntity.class.getDeclaredField("attackStrengthTicker");f.setAccessible(true);f.setInt(p,1000);}
    catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
  }
  private static void damageReady(ServerPlayer player){
    // Native login protection is60 ticks. These controlled fixture attacks run
    // immediately; production players and their spawn rules remain untouched.
    try{var field=ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");field.setAccessible(true);field.setInt(player,0);}
    catch(ReflectiveOperationException failure){throw new IllegalStateException("Could not finish controlled fixture spawn protection",failure);}
    player.invulnerableTime=0;
  }
  private static Cow cow(GameTestHelper h,ServerPlayer p,double x,double z){
    var c=EntityType.COW.create(h.getLevel());c.setNoAi(true);c.setNoGravity(true);c.setOnGround(true);
    c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);c.setHealth(1000);
    c.moveTo(p.getX()+x,p.getY(),p.getZ()+z);h.getLevel().addFreshEntity(c);cleanup(h,c::discard);return c;
  }
  private static Vec3 hit(ServerPlayer p,LivingEntity target,boolean sprint,int enchantment,Vec3 before){
    damageReady(p);if(target instanceof ServerPlayer other)damageReady(other);
    target.invulnerableTime=0;target.hurtTime=0;target.setHealth(target.getMaxHealth());target.setDeltaMovement(before);target.setOnGround(true);
    var weapon=enchantment>0?new ItemStack(Items.NETHERITE_SWORD):ItemStack.EMPTY;
    if(enchantment>0)weapon.enchant(p.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.KNOCKBACK),enchantment);
    p.setItemInHand(InteractionHand.MAIN_HAND,weapon);p.setSprinting(sprint);p.setShiftKeyDown(false);p.setYRot(-90);p.setOnGround(true);charged(p);
    p.attack(target);return target.getDeltaMovement();
  }
  private static Vec3 chaoHit(GameTestHelper h,CompanionSkillGameTests.Equipped e,LivingEntity target,Vec3 before){
    var actor=CompanionSkillGameTests.body(h,e);var keeper=e.keeper();
    actor.setPos(keeper.getX(),keeper.getY(),keeper.getZ());actor.setDeltaMovement(Vec3.ZERO);
    if(target instanceof ServerPlayer player)damageReady(player);
    target.invulnerableTime=0;target.hurtTime=0;target.setHealth(target.getMaxHealth());target.setDeltaMovement(before);target.setOnGround(true);
    float damage=CompanionRules.powerDamage(actor.state().levels[ChaoState.POWER]);
    h.assertTrue(target.hurt(actor.damageSources().mobAttack(actor),damage),"Actual companion direct mob attack is accepted");
    return target.getDeltaMovement();
  }
  @GameTestGenerator public static Collection<TestFunction> cases(){
    var out=new ArrayList<TestFunction>();
    for(int align=-1;align<=1;align++){
      final int a=align;out.add(new TestFunction("bonus080_run","run_alignment_"+a,TEMPLATE,130,0,true,h->run(h,a)));
      for(boolean player:new boolean[]{false,true})for(boolean enchanted:new boolean[]{false,true})
        out.add(new TestFunction("bonus080_power","power_"+a+"_"+player+"_"+enchanted,TEMPLATE,100,0,true,h->power(h,a,player,enchanted)));
    }
    for(String reason:List.of("not_crouching","main_item","offhand_item","block_hit","entity_hit","wrong_uuid","wrong_dimension","bad_sequence","rank_s","level_98","mixed_fly","mixed_swim"))
      out.add(new TestFunction("bonus080_input","reject_"+reason,TEMPLATE,100,0,true,h->reject(h,reason)));
        for(int stat:new int[]{ChaoState.RUN,ChaoState.POWER})
      out.add(new TestFunction("bonus080_cycle","registered_payload_cycle_all_settings_"+stat,TEMPLATE,180,0,true,h->allSettings(h,stat)));
    out.add(new TestFunction("bonus080_sweep","sweep_actual_extra_and_hurt_calls",TEMPLATE,120,0,true,EquippedAbilityGameTests080::sweep));
    out.add(new TestFunction("bonus080_travel","actual_native_ground_acceleration",TEMPLATE,100,0,true,EquippedAbilityGameTests080::travel));
    out.add(new TestFunction("bonus080_scope","cancelled_unrelated_thorns_and_exception_scope",TEMPLATE,120,0,true,EquippedAbilityGameTests080::scope));
    out.add(new TestFunction("bonus080_save","setting_survives_slot_copy_reload_and_cleanup",TEMPLATE,120,0,true,EquippedAbilityGameTests080::reload));
    out.add(new TestFunction("bonus080_dimension","native_dimension_round_trip_preserves_setting_and_cleans_modifiers",TEMPLATE,220,0,true,EquippedAbilityGameTests080::dimension));
    out.add(new TestFunction("bonus080_validation","native_first_impulse_step_survives_server_friction",TEMPLATE,100,0,true,h->validationMoves(h,false)));
    out.add(new TestFunction("bonus080_validation","native_two_packet_burst_survives_server_friction",TEMPLATE,100,0,true,h->validationMoves(h,true)));
    out.add(new TestFunction("bonus080_validation","unmarked_and_expired_velocity_windows_do_not_bypass_checks",TEMPLATE,100,0,true,EquippedAbilityGameTests080::validationExpiry));
    out.add(new TestFunction("bonus080_validation","native_dimension_change_discards_velocity_allowance",TEMPLATE,100,0,true,EquippedAbilityGameTests080::validationDimension));
    out.add(new TestFunction("bonus080_tracker","native_player_tracker_settles_and_late_pairing_uses_current_velocity",TEMPLATE,100,0,true,EquippedAbilityGameTests080::trackerCleanup));
    return out;
  }
  private static ServerPlayer validationVictim(GameTestHelper h,CompanionSkillGameTests.Equipped e){
    h.assertTrue(CompanionSkillGameTests.body(h,e).state().setTrait(EquippedAbilityRules080.POWER_SETTING,100),"Bounded native movement fixture selects persisted100 tier");
    var other=TestConnections.makeSurvivalServerPlayer(h);other.setNoGravity(true);
    other.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);other.setHealth(1000);
    other.teleportTo(e.keeper().getX()+1.5,e.keeper().getY(),e.keeper().getZ());acknowledgeTeleport(other);
    cleanup(h,()->other.server.getPlayerList().remove(other));return other;
  }
  private static double expectedMovement(ServerPlayer player,int burst){var velocity=player.getDeltaMovement();return EquippedKnockback080.movementExpected080(player,velocity,velocity.lengthSqr(),burst);}
  private static void validationMoves(GameTestHelper h,boolean burst){
    var e=equipped(h,ChaoState.POWER,0);pvpFixture(h,e.keeper().server);h.runAfterDelay(12,()->{
      // Keep the long native movement probes above all neighboring templates;
      // collision remains enabled and is explicitly checked for the full path.
      var attacker=e.keeper();attacker.teleportTo(attacker.getX(),h.getLevel().getMaxBuildHeight()-8,attacker.getZ());acknowledgeTeleport(attacker);
      var victim=validationVictim(h,e);var base=victim.position();
      for(int x=-1;x<=96;x++)for(int z=-1;z<=1;z++)h.getLevel().getChunkAt(BlockPos.containing(base).offset(x,0,z));
      var corridor=victim.getBoundingBox().expandTowards(90,1,0);
      h.assertTrue(h.getLevel().noCollision(victim,corridor),"Validation fixture requires a loaded collision-free swept corridor for its whole intended native movement");
      var impulse=chaoHit(h,e,victim,Vec3.ZERO);
      h.assertTrue(victim.getHealth()<victim.getMaxHealth(),"Validation fixture uses an accepted real companion mob attack");
      near(h,impulse.x,40.4,.0001,"Validation fixture has the requested actual impulse before server physics");
      // The real connection step runs living physics then resets its position
      // to firstGood, exactly reproducing the client/server friction race.
      victim.connection.tick();
      h.assertTrue(victim.getDeltaMovement().x<impulse.x,"Real server physics decays the impulse before the inbound move");
      if(burst){
        victim.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(base.x+impulse.x*.568,base.y+.2,base.z,false));
        victim.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(base.x+impulse.x*1.085,base.y+.1,base.z,false));
        near(h,victim.getX(),base.x+impulse.x*1.085,.00001,"Two real native position packets are accepted in one server tick without a rollback");
      }else{
        victim.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(base.x+impulse.x,base.y+impulse.y,base.z+impulse.z,false));
        near(h,victim.getX(),base.x+impulse.x,.00001,"The real native movement handler accepts the full initial40.4 companion step after server friction");
      }
      var check=EquippedKnockback080.movementCheck080(victim);
      h.assertTrue(check!=null&&check.burst()==(burst?2:1)&&check.authorizedExpectedSquared()>check.nativeExpectedSquared(),"Actual native expected-motion hook records the bounded real packet burst");
      var accepted=victim.position();
      com.mojang.logging.LogUtils.getLogger().info("BONUS080_EXPECTED_NATIVE_REJECTION displacement1000 victim={}",victim.getUUID());
      victim.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(base.x+1000,base.y,base.z,false));
      near(h,victim.getX(),accepted.x,.00001,"The original native too-quickly threshold still rejects a huge displacement");h.succeed();
    });
  }
  private static void validationExpiry(GameTestHelper h){
    var e=equipped(h,ChaoState.POWER,0);pvpFixture(h,e.keeper().server);h.runAfterDelay(12,()->{
      var victim=validationVictim(h,e);victim.setDeltaMovement(Vec3.ZERO);
      near(h,expectedMovement(victim,5),0,.000001,"An ordinary unmarked player receives no extra movement allowance");
      chaoHit(h,e,victim,Vec3.ZERO);victim.connection.tick();
      h.assertTrue(expectedMovement(victim,1)>victim.getDeltaMovement().lengthSqr(),"An accepted server-authored hit creates the short native allowance");
      victim.setDeltaMovement(Vec3.ZERO);h.runAfterDelay(21,()->{
        near(h,expectedMovement(victim,5),0,.000001,"Expired authorization cannot permit extra displacement");
        h.assertTrue(EquippedKnockback080.movementCheck080(victim)==null,"Expired diagnostic state is unavailable");h.succeed();
      });
    });
  }
  private static void validationDimension(GameTestHelper h){
    var e=equipped(h,ChaoState.POWER,0);pvpFixture(h,e.keeper().server);h.runAfterDelay(12,()->{
      var victim=validationVictim(h,e);chaoHit(h,e,victim,Vec3.ZERO);
      var away=victim.server.getLevel(Level.NETHER);h.assertTrue(away!=null,"Other native level exists for movement identity guard");
      h.assertTrue(victim.changeDimension(new DimensionTransition(away,new Vec3(13.5,160,13.5),Vec3.ZERO,0,0,DimensionTransition.DO_NOTHING))==victim,"Real victim dimension transition completes");
      acknowledgeTeleport(victim);victim.setDeltaMovement(Vec3.ZERO);
      near(h,expectedMovement(victim,5),0,.000001,"A real dimension change cannot reuse an old impulse allowance");
      h.assertTrue(EquippedKnockback080.movementCheck080(victim)==null,"Old-dimension diagnostic state is unavailable");h.succeed();
    });
  }
  private static void trackerCleanup(GameTestHelper h){
    var e=equipped(h,ChaoState.POWER,0);pvpFixture(h,e.keeper().server);h.runAfterDelay(12,()->{
      var victim=validationVictim(h,e);var tracker=new ServerEntity(h.getLevel(),victim,1,false,packet->{});
      chaoHit(h,e,victim,Vec3.ZERO);tracker.sendChanges();
      near(h,tracker.getLastSentMovement().x,40.4,.0001,"A marked native player tracker initially records its full authorized velocity");
      var below=new Vec3(3.2,.1,0);victim.setDeltaMovement(below);tracker.sendChanges();
      h.assertTrue(victim.getDeltaMovement().equals(below),"Under-packet-range observer cleanup never changes the authoritative victim velocity");
      h.assertTrue(tracker.getLastSentMovement().equals(below),"Marked player tracker cache follows actual current velocity below the packet limit");
      var stopped=new Vec3(0,-.0784,0);victim.setDeltaMovement(stopped);tracker.sendChanges();
      h.assertTrue(victim.getDeltaMovement().equals(stopped)&&tracker.getLastSentMovement().equals(stopped),"Final observer cleanup preserves the exact actual settled vector");
      var pairing=new ClientboundAddEntityPacket(victim,tracker);
      near(h,pairing.getXa(),0,.000001,"Real late-pairing spawn packet cannot resurrect an old horizontal impulse");
      near(h,pairing.getZa(),0,.000001,"Real late-pairing spawn packet remains settled on both horizontal axes");
      // The marked watcher is now gone. A subsequent ordinary zero-bonus hit
      // must follow vanilla and must not receive custom tracker cache changes.
      h.assertTrue(CompanionSkillGameTests.body(h,e).state().setTrait(EquippedAbilityRules080.POWER_SETTING,0),"Persist ordinary zero-bonus control");
      chaoHit(h,e,victim,Vec3.ZERO);victim.hasImpulse=false;victim.hurtMarked=false;
      var ordinary=new Vec3(.25,0,0);victim.setDeltaMovement(ordinary);tracker.sendChanges();
      h.assertTrue(tracker.getLastSentMovement().equals(stopped),"Unmarked ordinary PvP does not inherit a global tracker override");
      h.assertTrue(victim.getDeltaMovement().equals(ordinary),"Ordinary unmarked victim physics is unchanged");h.succeed();
    });
  }
  private static void run(GameTestHelper h,int alignment){
    var e=equipped(h,ChaoState.RUN,alignment);h.runAfterDelay(12,()->{
      var c=CompanionSkillGameTests.body(h,e);var p=e.keeper();
      near(h,amount(p),1000,.000001,"Maximum Run installs exactly +100000% as one modifier");
      near(h,p.getAttributeValue(Attributes.MOVEMENT_SPEED)/p.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue(),1001,.00001,"Actual unclamped movement attribute is1001x base");
      c.state().energy=0;c.sync();
      h.runAfterDelay(2,()->{
        near(h,amount(p),1000,.000001,"Low energy does not silently add a new secret gate");
        air(p);h.assertTrue(cycle(e,1),"Valid empty-air activation changes setting");
        near(h,amount(p),900,.000001,"1000 to900 applies immediately");
        h.assertTrue(EquippedAbilityRules080.percent(ChaoCarrierItem.state(e.inventory().getItem(0)),ChaoState.RUN)==900,"Actual carrier snapshot is updated immediately");
        h.assertFalse(cycle(e,1),"Duplicate packet sequence cannot step twice");
        h.runAfterDelay(5,()->{
          h.assertFalse(cycle(e,1),"Old packet replay cannot step after cooldown");
          c.state().grades[ChaoState.RUN]=5;c.sync();
          h.runAfterDelay(2,()->{
            h.assertTrue(amount(p)==0,"Ineligible exhausted Chao cannot retain secret modifier");
            c.state().energy=100;c.sync();h.runAfterDelay(2,()->{
              near(h,amount(p),1,.000001,"Ineligible Chao resumes existing ordinary level99 perk");h.succeed();
            });
          });
        });
      });
    });
  }
  private static void power(GameTestHelper h,int alignment,boolean playerTarget,boolean enchanted){
    var e=equipped(h,ChaoState.POWER,alignment);if(playerTarget)pvpFixture(h,e.keeper().server);h.runAfterDelay(12,()->{
      var c=CompanionSkillGameTests.body(h,e);var p=e.keeper();LivingEntity target;
      if(playerTarget){var other=TestConnections.makeSurvivalServerPlayer(h);other.setNoGravity(true);other.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);other.setHealth(1000);other.teleportTo(p.getX()+1.5,p.getY(),p.getZ());cleanup(h,()->other.server.getPlayerList().remove(other));target=other;}
      else target=cow(h,p,1.5,0);
      if(target instanceof ServerPlayer other)h.assertTrue(p.server.isPvpAllowed()&&p.canHarmPlayer(other)&&other.canHarmPlayer(p),"Controlled native PvP fixture policy and teams really permit the attack");
      c.state().grades[ChaoState.POWER]=5;var before=new Vec3(.6,0,0);
      var ordinary=chaoHit(h,e,target,before);
      near(h,ordinary.x-before.x/2,.4,.00001,"Ineligible companion uses ordinary native mob knockback");
      near(h,target.getMaxHealth()-target.getHealth(),11,.00001,"Existing level99 companion damage is11 before hidden eligibility");
      h.assertTrue(c.state().useHyperFruit(ChaoState.POWER),"Live expressed rank restored through existing X action");c.state().energy=0;
      // Keeper weapons/sprint do not add enchantment or extra damage to the Chao's own hurt call.
      p.setSprinting(enchanted);p.setItemInHand(InteractionHand.MAIN_HAND,enchanted?new ItemStack(Items.NETHERITE_SWORD):ItemStack.EMPTY);
      var boosted=chaoHit(h,e,target,before);
      near(h,target.getMaxHealth()-target.getHealth(),11,.00001,"Hidden Power preserves current companion damage11");
      near(h,boosted.x-before.x/2,.4*1001,.0001,"Chao's own native horizontal impulse receives exactly +100000% bonus once");
      h.assertTrue(boosted.y<=.400001,"Vanilla vertical cap remains");
      near(h,p.getAttributeValue(Attributes.ATTACK_KNOCKBACK),0,.000001,"No giant player attack-knockback attribute is installed");
      var capped=new ClientboundSetEntityMotionPacket(target.getId(),boosted);
      near(h,capped.getXa(),3.9,.000001,"Native short packet caps the full companion impulse, requiring existing full-vector corrections");
      h.assertTrue(EquippedKnockback080.scopeDepth()==0,"Successful companion hurt clears thread-local scope");
      var requests=new ArrayList<Double>();Consumer<LivingKnockBackEvent> playerForce=event->{if(event.getEntity()==target)requests.add((double)event.getStrength());};
      NeoForge.EVENT_BUS.addListener(playerForce);
      try{hit(p,target,enchanted,enchanted?2:0,Vec3.ZERO);}finally{NeoForge.EVENT_BUS.unregister(playerForce);}
      h.assertTrue(!requests.isEmpty()&&requests.stream().allMatch(value->value<=1.500001),"Equipped keeper's actual player attack remains ordinary, including sprint/enchantment calls");
      if(!playerTarget){
        target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        near(h,chaoHit(h,e,target,Vec3.ZERO).lengthSqr(),0,.000001,"Full native resistance remains effective");
        target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(.5);
        near(h,chaoHit(h,e,target,Vec3.ZERO).x,.4*1001*.5,.0001,"Half resistance halves the enlarged companion impulse");
        target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0);
        h.assertTrue(c.state().setTrait(EquippedAbilityRules080.POWER_SETTING,0),"Persist zero Power setting");
        near(h,chaoHit(h,e,target,Vec3.ZERO).x,.4,.0001,"Zero selected Power preserves ordinary companion knockback");
      }
      h.succeed();
    });
  }

  private static void reject(GameTestHelper h,String reason){
    var e=equipped(h,ChaoState.RUN,0);h.runAfterDelay(12,()->{
      var c=CompanionSkillGameTests.body(h,e);var p=e.keeper();air(p);int sequence=1;
      UUID expected=e.chao();ResourceLocation dimension=p.level().dimension().location();
      switch(reason){
        case "not_crouching"->p.setShiftKeyDown(false);
        case "main_item"->p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIRT));
        case "offhand_item"->p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));
        case "block_hit"->p.setXRot(90);
        case "entity_hit"->{
          p.setXRot(0);p.setYRot(-90);p.setYHeadRot(-90);var target=cow(h,p,1.5,0);
          // Align to the current real eye height rather than guessing whether
          // this just-crouched fixture has already updated its standing pose.
          target.setPos(p.getX()+1.5,p.getEyeY()-.7,p.getZ());
          var from=p.getEyePosition();var ray=p.getViewVector(1).scale(p.entityInteractionRange());
          var actual=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(p,from,from.add(ray),
              p.getBoundingBox().expandTowards(ray).inflate(1),candidate->!candidate.isSpectator()&&candidate.isPickable(),ray.lengthSqr());
          h.assertTrue(actual!=null&&actual.getEntity()==target,"Native entity-hit rejection fixture must really intersect its intended cow");
        }
        case "wrong_uuid"->expected=UUID.randomUUID();
        case "wrong_dimension"->dimension=ResourceLocation.withDefaultNamespace("the_nether");
        case "bad_sequence"->sequence=0;
        case "rank_s"->c.state().grades[ChaoState.RUN]=5;
        case "level_98"->c.state().levels[ChaoState.RUN]=98;
        case "mixed_fly"->c.state().levels[ChaoState.FLY]=5;
        case "mixed_swim"->c.state().levels[ChaoState.SWIM]=5;
      }
      h.assertFalse(CompanionContent.requestAbilityCycle080(p,sequence,dimension,expected),"Invalid input must not change a setting: "+reason);
      h.assertTrue(EquippedAbilityRules080.percent(c.state(),ChaoState.RUN)==1000,"Rejected input retains stable saved setting");h.succeed();
    });
  }
  private static void allSettings(GameTestHelper h,int stat){
    var e=equipped(h,stat,0);h.runAfterDelay(12,()->{air(e.keeper());step(h,e,stat,1);});
  }
  private static void step(GameTestHelper h,CompanionSkillGameTests.Equipped e,int stat,int seq){
    var p=e.keeper();p.connection.handleCustomPayload(new ServerboundCustomPayloadPacket(new CompanionNetwork.AbilityCycle080(seq,p.level().dimension().location(),e.chao())));
    h.runAfterDelay(1,()->{
      int expected=seq<=10?1000-seq*100:1000;
      var c=CompanionSkillGameTests.body(h,e);h.assertTrue(EquippedAbilityRules080.percent(c.state(),stat)==expected,"Registered packet applies one exact percentage step");
      near(h,stat==ChaoState.RUN?amount(p):CompanionContent.secretPowerBonus080(p),expected,.000001,"Every setting supplies the requested bonus, including zero");
      h.assertTrue(EquippedAbilityRules080.percent(ChaoCarrierItem.state(e.inventory().getItem(0)),stat)==expected,"Each step snapshots exact carrier state");
      for(int i=0;i<5;i++)p.connection.handleCustomPayload(new ServerboundCustomPayloadPacket(new CompanionNetwork.AbilityCycle080(seq,p.level().dimension().location(),e.chao())));
      if(seq==11){h.succeed();return;}
      h.runAfterDelay(4,()->step(h,e,stat,seq+1));
    });
  }
  private static void travel(GameTestHelper h){
    var e=equipped(h,ChaoState.RUN,0);h.runAfterDelay(12,()->{
      var p=e.keeper();var control=TestConnections.makeSurvivalServerPlayer(h);control.setNoGravity(true);
      // The1001x first step needs a real collision-free stone runway, rather
      // than silently truncating at the old12-block fixture floor or a neighbor.
      var origin=BlockPos.containing(p.getX(),h.getLevel().getMaxBuildHeight()-8,p.getZ());
      var prior=new java.util.LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
      for(int x=-1;x<=7;x++)for(int z=-2;z<=132;z++)for(int y=-1;y<=2;y++){
        var at=origin.offset(x,y,z);prior.put(at,h.getLevel().getBlockState(at));
        h.getLevel().setBlock(at,y==-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
      }
      cleanup(h,()->prior.forEach((at,state)->h.getLevel().setBlock(at,state,3)));
      p.teleportTo(origin.getX()+.5,origin.getY(),origin.getZ()+.5);acknowledgeTeleport(p);
      control.teleportTo(p.getX()+5,p.getY(),p.getZ());acknowledgeTeleport(control);cleanup(h,()->control.server.getPlayerList().remove(control));
      p.setShiftKeyDown(false);control.setShiftKeyDown(false);p.setSprinting(false);control.setSprinting(false);
      p.setYRot(0);control.setYRot(0);p.setDeltaMovement(Vec3.ZERO);control.setDeltaMovement(Vec3.ZERO);p.setOnGround(true);control.setOnGround(true);
      Vec3 from=p.position(),base=control.position();
      p.travel(new Vec3(0,0,1));control.travel(new Vec3(0,0,1));
      double actual=p.position().subtract(from).horizontalDistance(),ordinary=control.position().subtract(base).horizontalDistance();
      h.assertTrue(ordinary>.01,"Native control really moved on the common stone floor");
      near(h,actual/ordinary,1001,.001,"Actual first native ground acceleration is1001x ordinary player, not just a displayed attribute");h.succeed();
    });
  }
  private static void sweep(GameTestHelper h){
    var e=equipped(h,ChaoState.POWER,0);h.runAfterDelay(12,()->{
      var p=e.keeper();var c=CompanionSkillGameTests.body(h,e);var main=cow(h,p,1.5,0);var neighbor=cow(h,p,1.5,.75);Vec3 ordinary=null;
      for(int pass=0;pass<2;pass++){
        if(pass==0)c.state().grades[ChaoState.POWER]=5;else h.assertTrue(c.state().useHyperFruit(ChaoState.POWER),"Restore native expressed X");
        for(var target:List.of(main,neighbor)){target.setHealth(1000);target.invulnerableTime=0;target.hurtTime=0;target.setDeltaMovement(Vec3.ZERO);target.setOnGround(true);}
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));p.setSprinting(false);p.setShiftKeyDown(false);p.setYRot(-90);p.setOnGround(true);p.fallDistance=0;charged(p);
        p.attack(main);h.assertTrue(neighbor.getHealth()<1000,"Native keeper sword sweep actually hits its secondary target");
        if(pass==0)ordinary=neighbor.getDeltaMovement();
        else{near(h,neighbor.getDeltaMovement().x,ordinary.x,.0001,"Player sweep X gains no companion Power");near(h,neighbor.getDeltaMovement().z,ordinary.z,.0001,"Player sweep Z gains no companion Power");}
      }
      h.assertTrue(EquippedKnockback080.scopeDepth()==0,"Player sweep leaves no companion scope");h.succeed();
    });
  }

  private static void scope(GameTestHelper h){
    var e=equipped(h,ChaoState.POWER,0);h.runAfterDelay(12,()->{
      var p=e.keeper();var c=CompanionSkillGameTests.body(h,e);var main=cow(h,p,1.5,0);var other=cow(h,p,0,2);
      h.assertTrue(EquippedKnockback080.begin(p)==null,"Player never opens a hidden companion attack scope");
      var garden=ChaoGarden.CHAO.get().create(h.getLevel());garden.owner=p.getUUID();garden.setState(state(ChaoState.POWER,0));
      garden.moveTo(p.getX()-2,p.getY(),p.getZ());h.getLevel().addFreshEntity(garden);cleanup(h,garden::discard);
      h.assertTrue(EquippedKnockback080.beginMob(main,garden.damageSources().mobAttack(garden))==null,"An eligible owned garden Chao is not the equipped attack actor");
      garden.getPersistentData().merge(c.getPersistentData().copy());
      h.assertTrue(CompanionContent.isManaged(garden)&&CompanionContent.secretPowerBonus084(garden)==0,"A copied managed marker cannot impersonate the exact active body");
      h.assertTrue(EquippedKnockback080.beginMob(main,garden.damageSources().mobAttack(garden))==null,"A stale/manual marker cannot open a Power scope");
      h.assertTrue(EquippedKnockback080.beginMob(main,p.damageSources().mobAttack(p))==null,"A player-caused mob damage type cannot borrow its companion");
      h.assertTrue(EquippedKnockback080.beginMob(main,c.damageSources().thorns(c))==null,"Chao thorns is not direct melee Power");
      var indirect=new net.minecraft.world.damagesource.DamageSource(c.damageSources().mobAttack(c).typeHolder(),other,c);
      h.assertTrue(EquippedKnockback080.beginMob(main,indirect)==null,"A different direct entity cannot borrow Chao-caused damage");
      other.knockback(.4,1,0);near(h,other.getDeltaMovement().x,-.4,.000001,"Unrelated knockback outside companion hurt remains ordinary");
      other.setDeltaMovement(Vec3.ZERO);
      Consumer<LivingKnockBackEvent> cancel=event->{if(event.getEntity()==main)event.setCanceled(true);};NeoForge.EVENT_BUS.addListener(cancel);
      try{near(h,chaoHit(h,e,main,Vec3.ZERO).lengthSqr(),0,.000001,"Cancelled native companion knockback stays cancelled");}finally{NeoForge.EVENT_BUS.unregister(cancel);}
      boolean[] retaliationAccepted={false};var retaliationStrengths=new ArrayList<Double>();
      Consumer<LivingIncomingDamageEvent> thorns=event->{if(event.getEntity()==main){other.knockback(.4,1,0);c.invulnerableTime=0;c.hurtTime=0;retaliationAccepted[0]=c.hurt(c.damageSources().thorns(main),1);}};
      Consumer<LivingKnockBackEvent> retaliation=event->{if(event.getEntity()==c)retaliationStrengths.add((double)event.getStrength());};
      NeoForge.EVENT_BUS.addListener(thorns);NeoForge.EVENT_BUS.addListener(retaliation);
      try{
        c.setDeltaMovement(Vec3.ZERO);float health=c.getHealth();chaoHit(h,e,main,Vec3.ZERO);
        near(h,other.getDeltaMovement().x,-.4,.000001,"Unrelated callback push cannot borrow active companion Power");
        h.assertTrue(retaliationAccepted[0]&&c.getHealth()<health,"Actual thorns retaliation damages the Chao attacker");
        h.assertTrue(retaliationStrengths.size()==1,"Actual thorns requests one native knockback");
        near(h,retaliationStrengths.getFirst(),.4,.000001,"Retaliation strength remains ordinary during companion scope");
        near(h,c.getDeltaMovement().x,-.4,.000001,"Chao attacker receives ordinary retaliation impulse");
      }finally{NeoForge.EVENT_BUS.unregister(thorns);NeoForge.EVENT_BUS.unregister(retaliation);}
      int[] nested={0};Consumer<LivingIncomingDamageEvent> nest=event->{if(event.getEntity()==main){chaoHit(h,e,other,Vec3.ZERO);nested[0]++;h.assertTrue(EquippedKnockback080.scopeDepth()==1,"Nested legitimate companion hurt restores outer scope");}};
      NeoForge.EVENT_BUS.addListener(nest);
      try{chaoHit(h,e,main,Vec3.ZERO);h.assertTrue(nested[0]==1,"One nested actual companion attack executes");near(h,other.getDeltaMovement().horizontalDistance(),.4*1001,.0001,"Nested actual companion attack scales exactly once");}
      finally{NeoForge.EVENT_BUS.unregister(nest);}
      final class Expected extends RuntimeException {}
      Consumer<LivingIncomingDamageEvent> throwsEvent=event->{if(event.getEntity()==main)throw new Expected();};NeoForge.EVENT_BUS.addListener(throwsEvent);
      try{try{chaoHit(h,e,main,Vec3.ZERO);h.fail("Expected damage callback exception missing");}catch(Expected correct){h.assertTrue(EquippedKnockback080.scopeDepth()==0,"Throwing hurt always clears companion scope");}}
      finally{NeoForge.EVENT_BUS.unregister(throwsEvent);}
      h.assertTrue(EquippedKnockback080.scopeDepth()==0,"No permanent companion scope");h.succeed();
    });
  }

  private static void reload(GameTestHelper h){
    var e=equipped(h,ChaoState.RUN,0);h.runAfterDelay(12,()->{
      air(e.keeper());h.assertTrue(cycle(e,1),"Initial selected setting persisted");
      var saved=ItemStack.parseOptional(h.getLevel().registryAccess(),(net.minecraft.nbt.CompoundTag)e.inventory().getItem(0).save(h.getLevel().registryAccess()));
      CompanionContent.unequip(e.keeper(),e.inventory().getItem(0));e.inventory().setItem(0,ItemStack.EMPTY);
      near(h,amount(e.keeper()),0,.000001,"Unequip removes transient modifier immediately");
      e.inventory().setItem(0,saved);h.runAfterDelay(8,()->{
        var c=CompanionSkillGameTests.body(h,e);h.assertTrue(EquippedAbilityRules080.percent(c.state(),ChaoState.RUN)==900,"Actual reloaded slot/body retains selected setting");
        near(h,amount(e.keeper()),900,.000001,"Reequip does not stack or reset the selected boost");
        h.assertFalse(cycle(e,1),"Slot copy cannot replay the old input sequence");h.succeed();
      });
    });
  }
  private static void acknowledgeTeleport(ServerPlayer player){
    try{
      var field=net.minecraft.server.network.ServerGamePacketListenerImpl.class.getDeclaredField("awaitingTeleport");field.setAccessible(true);
      player.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(field.getInt(player.connection)));
    }catch(ReflectiveOperationException failure){throw new IllegalStateException("Could not acknowledge native dimension teleport",failure);}
  }
  private static String dimensionDiagnostic(CompanionSkillGameTests.Equipped e,ServerLevel expected){
    var p=e.keeper();var stack=e.inventory().getItem(0);var slots=new ArrayList<String>();
    for(var actual:AccessoriesBridge.equipped(p))if(ChaoCarrierItem.filled(actual))slots.add("uuid="+ChaoCarrierItem.storedUUID(actual)+",token="+CompanionContent.token(actual,false));
    var found=CompanionContent.findEntity(p.server,e.chao());
    return "serverTick="+p.server.getTickCount()+",playerTick="+p.tickCount+",alive="+p.isAlive()+",removed="+p.isRemoved()
        +",changing="+p.isChangingDimension()+",playerLevel="+p.level().dimension().location()+",expected="+expected.dimension().location()
        +",trackedPlayer="+expected.players().contains(p)+",chunkLoaded="+expected.hasChunkAt(p.blockPosition())+",slots="+slots
        +",carrierUUID="+ChaoCarrierItem.storedUUID(stack)+",carrierToken="+CompanionContent.token(stack,false)
        +",carrierChoice="+(ChaoCarrierItem.filled(stack)?EquippedAbilityRules080.percent(ChaoCarrierItem.state(stack),ChaoState.RUN):-1)
        +",registered="+CompanionContent.itemIsSummoned(stack)+",body="+(found==null?"none":found.getClass().getSimpleName()+"@"+found.level().dimension().location()+",removed="+found.isRemoved());
  }
  private static void awaitDimensionBody(GameTestHelper h,CompanionSkillGameTests.Equipped e,ServerLevel expected,String stage,long deadline,Consumer<ChaoEntity> ready){
    long remaining=deadline-e.keeper().server.getTickCount();
    if(remaining<=0){h.fail("Actual slot did not stably resummon its saved UUID within80 ticks ("+stage+"): "+dimensionDiagnostic(e,expected));return;}
    var found=expected.getEntity(e.chao());
    if(found!=null&&!(found instanceof ChaoEntity)){h.fail("Saved Chao UUID unexpectedly belongs to another entity: "+dimensionDiagnostic(e,expected));return;}
    if(found instanceof ChaoEntity body&&!body.isRemoved()){
      // Poll only the real slot-driven projection. One ordinary server tick
      // lets its existing benefits/synchronization finish before exact checks.
      h.runAfterDelay(1,()->{
        if(expected.getEntity(e.chao())!=body||body.isRemoved()){awaitDimensionBody(h,e,expected,stage,deadline,ready);return;}
        com.mojang.logging.LogUtils.getLogger().info("BONUS080_DIMENSION_READY stage={} elapsed={} {}",stage,80-(deadline-e.keeper().server.getTickCount()),dimensionDiagnostic(e,expected));
        ready.accept(body);
      });
    }else{
      if(remaining%20==0)com.mojang.logging.LogUtils.getLogger().info("BONUS080_DIMENSION_WAIT stage={} remaining={} {}",stage,remaining,dimensionDiagnostic(e,expected));
      h.runAfterDelay(1,()->awaitDimensionBody(h,e,expected,stage,deadline,ready));
    }
  }
  private static void dimension(GameTestHelper h){
    var e=equipped(h,ChaoState.RUN,0);h.runAfterDelay(12,()->{
      var p=e.keeper();var origin=p.serverLevel();var originalPosition=p.position();
      var away=p.server.getLevel(Level.NETHER);h.assertTrue(away!=null&&away!=origin,"Native dimension fixture requires a different loaded Nether level");
      var launch=new BlockPos(h.absolutePos(CENTER).getX()+4096,160,h.absolutePos(CENTER).getZ()+4096);
      var prior=new java.util.LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
      for(var cursor:BlockPos.betweenClosed(launch.offset(-4,-1,-4),launch.offset(4,4,4))){
        var at=cursor.immutable();prior.put(at,away.getBlockState(at));
        away.setBlock(at,at.getY()==launch.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
      }
      cleanup(h,()->prior.forEach((at,state)->away.setBlock(at,state,3)));
      var oldBody=CompanionSkillGameTests.body(h,e);var savedToken=CompanionContent.token(e.inventory().getItem(0),false);air(p);
      h.assertTrue(cycle(e,1),"Select900 through authenticated input before native dimension travel");
      near(h,amount(p),900,.000001,"Selected900 applies before dimension transfer");
      h.assertTrue(p.changeDimension(new DimensionTransition(away,Vec3.atBottomCenterOf(launch),Vec3.ZERO,0,0,DimensionTransition.DO_NOTHING))==p,
          "Native ServerPlayer dimension transition actually completes");
      h.assertTrue(p.serverLevel()==away,"Keeper actually enters the other server level");
      near(h,amount(p),0,.000001,"Dimension event removes the old world transient speed modifier immediately");
      h.assertTrue(oldBody.isRemoved()&&!CompanionContent.itemIsSummoned(e.inventory().getItem(0)),"Old world projection is recalled and its active slot registration is removed");
      h.assertTrue(e.chao().equals(ChaoCarrierItem.storedUUID(e.inventory().getItem(0)))
          &&EquippedAbilityRules080.percent(ChaoCarrierItem.state(e.inventory().getItem(0)),ChaoState.RUN)==900,
          "Real Accessories slot retains the same Chao and selected900 during transfer");
      acknowledgeTeleport(p);
      awaitDimensionBody(h,e,away,"outbound",p.server.getTickCount()+80,body->{
        h.assertTrue(body!=oldBody&&p.getUUID().equals(body.owner)&&CompanionContent.isManaged(body),"Destination projection belongs to this keeper without reusing the old body");
        h.assertTrue(e.chao().equals(body.getUUID())&&savedToken.equals(CompanionContent.token(e.inventory().getItem(0),false)),"Destination retains the exact stored Chao UUID and authenticated carrier token");
        near(h,amount(p),900,.000001,"Native destination restores exactly the saved900 bonus without stacking");
        near(h,p.getAttributeValue(Attributes.MOVEMENT_SPEED)/p.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue(),901,.00001,"Actual destination speed is base times901 for +90000%");
        h.assertTrue(EquippedAbilityRules080.percent(body.state(),ChaoState.RUN)==900,"Destination live state retains the selection");
        air(p);h.assertFalse(CompanionContent.requestAbilityCycle080(p,2,origin.dimension().location(),e.chao()),"Delayed input from the prior actual dimension cannot edit this Chao");
        h.runAfterDelay(5,()->{
          h.assertTrue(cycle(e,3),"Fresh destination input still controls the authenticated current slot");
          near(h,amount(p),800,.000001,"Fresh destination activation selects800 exactly");
          h.assertTrue(p.changeDimension(new DimensionTransition(origin,originalPosition,Vec3.ZERO,0,0,DimensionTransition.DO_NOTHING))==p,"Native return dimension transition completes");
          near(h,amount(p),0,.000001,"Returning dimensions removes the destination modifier immediately");
          h.assertTrue(body.isRemoved(),"Destination projection is recalled on return");acknowledgeTeleport(p);
          awaitDimensionBody(h,e,origin,"return",p.server.getTickCount()+80,returned->{
            h.assertTrue(e.chao().equals(returned.getUUID())&&savedToken.equals(CompanionContent.token(e.inventory().getItem(0),false)),"Native return preserves the exact Chao UUID and carrier token");
            near(h,amount(p),800,.000001,"Return to original world restores the current800 setting without an old900 snapshot");
            h.assertTrue(EquippedAbilityRules080.percent(returned.state(),ChaoState.RUN)==800
                &&EquippedAbilityRules080.percent(ChaoCarrierItem.state(e.inventory().getItem(0)),ChaoState.RUN)==800,
                "Current body and actual saved carrier agree after native round trip");
            returned.state().grades[ChaoState.RUN]=5;returned.sync();
            h.runAfterDelay(2,()->{
              near(h,amount(p),1,.000001,"Rank loss after travel clears hidden800 and resumes only the ordinary +100% perk");
              h.assertTrue(returned.state().useHyperFruit(ChaoState.RUN),"Restore the expressed rank through its existing action");returned.sync();
              h.runAfterDelay(2,()->{
                near(h,amount(p),800,.000001,"Eligibility restoration recovers the same persisted800 setting");
                CompanionContent.unequip(p,e.inventory().getItem(0));
                h.assertTrue(EquippedAbilityRules080.percent(ChaoCarrierItem.state(e.inventory().getItem(0)),ChaoState.RUN)==800,"Unequip preserves the saved selection");
                e.inventory().setItem(0,ItemStack.EMPTY);near(h,amount(p),0,.000001,"Final unequip removes every companion speed modifier");
                h.assertTrue(returned.isRemoved()&&p.serverLevel()==origin,"Round trip finishes in the original level with no detached managed body");h.succeed();
              });
            });
          });
        });
      });
    });
  }
}
