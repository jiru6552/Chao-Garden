package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoIllness;
import com.chaogarden.core.ChaoPreferences;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.market.MarketStock;
import com.chaogarden.toys.BallPlayRules074;
import com.chaogarden.toys.BallToyBlockEntity074;
import com.chaogarden.toys.ChaoBallBlock074;
import com.chaogarden.toys.ChaoToyBlock;
import com.chaogarden.toys.ToyContent;
import com.chaogarden.toys.ToyInteractionRules;
import com.chaogarden.toys.ToyPreview;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real ball ownership, navigation/contact and reward-free moving operator previews.
 * Client poses and audible sound comfort are separate checks. */
@GameTestHolder("chao_ball74") @PrefixGameTestTemplate(false) @EventBusSubscriber(modid=ChaoGarden.MODID)
public final class BallToyGameTests074 {
  private static final String KIND="chao_ball",TEMPLATE="chao_ball74:activity_test";
  private static final int CLEANUP_DELAY=BallPlayRules074.PRESENTATION_TICKS+2;
  private record Cue(long tick,float volume,float pitch){}
  private static final Map<ChaoEntity,List<Cue>> CUES=new WeakHashMap<>();
  private record Fixture(ChaoEntity c,ServerPlayer p,BlockPos origin,Goal sentinel){}
  private BallToyGameTests074(){}

  @SubscribeEvent public static void positionedSound(PlayLevelSoundEvent.AtPosition e){
    if(e.getSound()!=null&&e.getSound().value().getLocation().toString().equals("minecraft:block.slime_block.step"))
      recordCue(e.getLevel(),e.getPosition(),e.getNewVolume(),e.getNewPitch());
  }
  @SubscribeEvent public static void entitySound(PlayLevelSoundEvent.AtEntity e){
    if(e.getSound()!=null&&e.getSound().value().getLocation().toString().equals("minecraft:block.slime_block.step"))
      recordCue(e.getLevel(),e.getEntity().position(),e.getNewVolume(),e.getNewPitch());
  }
  private static void recordCue(net.minecraft.world.level.Level level,Vec3 at,float volume,float pitch){
    if(level.isClientSide)return;
    CUES.forEach((c,list)->{if(c.level()==level&&c.position().distanceToSqr(at)<9)list.add(new Cue(level.getGameTime(),volume,pitch));});
  }

  @GameTestGenerator public static Collection<TestFunction> cases(){
    var out=new ArrayList<TestFunction>();
    for(boolean adult:new boolean[]{false,true})for(Direction side:Direction.Plane.HORIZONTAL)
      add(out,"ball_natural_"+(adult?"adult":"newborn")+"_from_"+side,1800,h->natural(h,adult,side));
    add(out,"ball_natural_neutral_fly_adult_still_crawls",1800,h->natural(h,true,Direction.NORTH,ChaoState.FLY_TYPE,0,0));
    add(out,"ball_natural_hero_swim_adult_still_crawls",1800,h->natural(h,true,Direction.WEST,ChaoState.SWIM_TYPE,1,0));
    for(boolean adult:new boolean[]{false,true})for(boolean loop:new boolean[]{false,true})
      add(out,"ball_preview_"+(adult?"adult":"baby")+"_"+(loop?"loop":"once"),430,h->preview(h,adult,loop));
    for(String mode:List.of("stop","damage","pet","remove","displace","entity_leave","logout","range","reload","original_noai","missing_cooldown"))
      add(out,"ball_preview_interrupt_mid_roll_"+mode,270,h->interrupt(h,mode));
    for(String mode:List.of("solid","edge","living_destination","living_sweep","late_blocker"))
      add(out,"ball_roll_rejects_"+mode,190,h->blocked(h,mode));
    for(String mode:List.of("absent","reserved","carrying"))add(out,"ball_preview_rejects_"+mode,60,h->reject(h,mode));
    add(out,"ball_native_permissions_and_commands",100,BallToyGameTests074::permissions);
    add(out,"ball_saved_identity_item_and_legacy_acquisition",100,BallToyGameTests074::compatibility);
    add(out,"ball_network_motion_and_disk_reload_are_distinct",250,BallToyGameTests074::motionReplication);
    add(out,"ball_stop_before_contact_clears_active_claim_and_scopes_clearance",250,BallToyGameTests074::stopBeforeContact);
    add(out,"ball_natural_blocked_terminal_keeps_clearance_without_reward_duplication",600,BallToyGameTests074::blockedTerminal);
    add(out,"ball_precontact_noai_save_reload_keeps_only_safe_clearance",250,BallToyGameTests074::precontactReload);
    add(out,"ball_contact_move_control_delegates_ordinary_jumps",80,BallToyGameTests074::moveControlDelegation);
    add(out,"ball_loop_renewal_keeps_inflight_contact_action",1250,BallToyGameTests074::loopActionSeam);
    for(boolean remove:new boolean[]{false,true})add(out,"ball_natural_interrupt_"+(remove?"remove":"damage"),1000,h->naturalInterrupt(h,remove));
    // This unchanged production regression includes the protected ambient pupil's real ball reservation.
    add(out,"ball_keeps_protected_school_sessions",550,
        KindergartenPupilsGameTests0257::pupilsUseCurrentLessonAnimationsAndRealToySessionsWithoutAgingOrLearningRewards);
    return out;
  }
  private static void add(List<TestFunction> out,String name,int timeout,java.util.function.Consumer<GameTestHelper> run){
    out.add(new TestFunction("ball074_"+(out.size()/5),name,TEMPLATE,timeout,0,true,run));
  }
  private static BlockPos floor(GameTestHelper h){
    for(int x=1;x<=21;x++)for(int z=1;z<=21;z++){
      h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
      for(int y=1;y<=5;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
    }
    return h.absolutePos(new BlockPos(10,1,10));
  }
  private static void cleanup(GameTestHelper h,Runnable action){h.testInfo.addListener(new GameTestListener(){
    public void testStructureLoaded(GameTestInfo i){}
    public void testPassed(GameTestInfo i,GameTestRunner r){action.run();}
    public void testFailed(GameTestInfo i,GameTestRunner r){action.run();}
    public void testAddedForRerun(GameTestInfo a,GameTestInfo b,GameTestRunner r){action.run();}
  });}
  private static ChaoEntity actor(GameTestHelper h,Vec3 position,boolean adult){
    return actor(h,position,adult,adult?ChaoState.RUN_TYPE:ChaoState.NORMAL,adult?-1:0,adult?175:0);
  }
  private static ChaoEntity actor(GameTestHelper h,Vec3 position,boolean adult,int first,float alignment,int runPoints){
    var c=ChaoGarden.CHAO.get().create(h.getLevel());h.assertTrue(c!=null,"Production Chao creates");
    var s=new ChaoState();s.adult=adult;s.align=alignment;s.primaryType=first;
    s.happiness=20;s.hunger=90;s.energy=100;s.boredom=100;s.personality=ChaoPreferences.EASILY_BORED;
    if(adult){s.levels[runPoints==0?ChaoState.FLY:ChaoState.RUN]=9;s.progress[runPoints==0?ChaoState.FLY:ChaoState.RUN]=60;s.points[ChaoState.RUN]=runPoints;s.setEvolutionStrength(1.2f);}
    for(int i=0;i<ChaoIllness.COUNT;i++)s.setIllness(i,100);
    c.setState(ChaoState.decode(s.encode()));c.getRandom().setSeed(18);c.setInvulnerable(true);c.moveTo(position);c.setOnGround(true);
    h.getLevel().addFreshEntity(c);c.goalSelector.removeAllGoals(g->true);c.targetSelector.removeAllGoals(g->true);
    CUES.put(c,new ArrayList<>());cleanup(h,()->{ToyPreview.stop(c);CUES.remove(c);c.discard();});return c;
  }
  private static int countBalls(GameTestHelper h){
    int total=0;for(var p:BlockPos.betweenClosed(h.absolutePos(new BlockPos(0,1,0)),h.absolutePos(new BlockPos(23,4,23))))
      if(h.getLevel().getBlockState(p).is(ToyContent.block(KIND)))total++;
    return total;
  }
  private static int drops(GameTestHelper h){
    return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(Vec3.atLowerCornerOf(h.absolutePos(BlockPos.ZERO)),Vec3.atLowerCornerOf(h.absolutePos(new BlockPos(24,7,24)))),
        e->e.getItem().is(ChaoGarden.item(KIND))).stream().mapToInt(e->e.getItem().getCount()).sum();
  }
  private static void identity(GameTestHelper h,ChaoEntity c,boolean adult){
    identity(h,c,adult,adult?ChaoState.RUN_TYPE:ChaoState.NORMAL,adult?-1:0);
  }
  private static void identity(GameTestHelper h,ChaoEntity c,boolean adult,int first,float alignment){
    h.assertTrue(c.state().adult==adult,"Toy does not change the biological stage");
    if(adult)h.assertTrue(c.state().lockedPrimaryType()==first&&c.state().lockedAlignment()==alignment,
        "Adult alignment and first type remain locked");
    else h.assertTrue(c.state().primaryType==ChaoState.NORMAL,"Newborn stays unevolved; ordinary walking may change prospective type");
  }
  private static void physicalIdle(GameTestHelper h,ChaoEntity c){
    h.assertTrue(h.getLevel().getEntitiesOfClass(Display.ItemDisplay.class,
        new AABB(Vec3.atLowerCornerOf(h.absolutePos(BlockPos.ZERO)),Vec3.atLowerCornerOf(h.absolutePos(new BlockPos(24,7,24))))).isEmpty(),"The real block is not duplicated as a loose display prop");
    h.assertTrue(CUES.get(c).stream().allMatch(q->Float.isFinite(q.volume)&&q.volume>0&&q.volume<=.081f&&q.pitch==1),
        "Contact cues retain the authored quiet source volume/pitch; physical listening is separate");
    h.assertTrue(!ToyPreview.active(c)&&c.toyPlayingTicks()==0&&c.toyTarget()==null
        &&!c.getPersistentData().contains("ChaoToyTarget")&&!c.getPersistentData().contains("ChaoToyUntil"),
        "Session releases preview, synced clock, synced target and reservation");
    h.assertTrue(c.level().noCollision(c,c.getBoundingBox()),"Ending a close-contact action leaves the actor's actual collision context clear");
    for(var p:BlockPos.betweenClosed(h.absolutePos(new BlockPos(0,1,0)),h.absolutePos(new BlockPos(23,4,23)))){
      var state=h.getLevel().getBlockState(p);if(!state.is(ToyContent.block(KIND)))continue;
      h.assertTrue(!state.getValue(ChaoToyBlock.PLAYING)&&!state.getValue(ChaoBallBlock074.ROLLING)
          &&state.getValue(ChaoToyBlock.FRAME)==0,"One recoverable ball finishes cosmetic motion and returns to idle");
    }
  }
  private static void natural(GameTestHelper h,boolean adult,Direction side){
    natural(h,adult,side,adult?ChaoState.RUN_TYPE:ChaoState.NORMAL,adult?-1:0,adult?175:0);
  }
  private static void natural(GameTestHelper h,boolean adult,Direction side,int first,float alignment,int runPoints){
    var at=floor(h);h.getLevel().setBlock(at,ToyContent.block(KIND).defaultBlockState(),3);
    var c=actor(h,Vec3.atBottomCenterOf(at.relative(side,2)),adult,first,alignment,runPoints);c.goalSelector.addGoal(4,new ToyContent.PlayGoal(c));
    var played=new AtomicBoolean();var moved=new AtomicInteger();var chased=new AtomicBoolean();var lastBall=new AtomicReference<>(at);var lastActor=new AtomicReference<>(c.position());
    h.onEachTick(()->{
      h.assertTrue(c.position().distanceTo(lastActor.get())<.7,"Natural approach/chase uses physical movement instead of remote relocation");
      if(ToyContent.isPlaying(c)){
        played.set(true);var now=ToyContent.target(c);var old=lastBall.getAndSet(now);
        h.assertTrue(now.equals(c.toyTarget()),"Client-synced ball target matches the server reservation after every move");
        if(!now.equals(old)){
          moved.incrementAndGet();h.assertTrue(now.distManhattan(old)==1,"A contact commits exactly one adjacent cell");
          h.assertTrue(c.toyKickElapsed()>=ToyInteractionRules.BALL_CONTACT_TICK&&c.toyKickElapsed()<ToyInteractionRules.BALL_KICK_TICKS,
              "Physical movement is synchronized to the contact phase");
          var delta=c.position().subtract(Vec3.atBottomCenterOf(old));
          h.assertTrue(ToyInteractionRules.ballWithinReach(delta.horizontalDistanceSqr(),delta.y,c.getBbWidth()),"No remote physical kick");
          calibratedContact(h,c,old);
          h.assertTrue(h.getLevel().getBlockState(old).isAir()&&h.getLevel().getBlockState(now).is(ToyContent.block(KIND)),"Only the actual ball transfers");
          h.assertTrue(countBalls(h)==1&&drops(h)==0,"Rolling neither copies nor drops a toy");
        }
        if(moved.get()>0&&c.toyKickElapsed()<0&&c.distanceToSqr(lastActor.get())>.000001)chased.set(true);
        identity(h,c,adult,first,alignment);
      }
      lastActor.set(c.position());
    });
    h.succeedWhen(()->{
      h.assertTrue(played.get()&&moved.get()>0&&chased.get()&&!ToyContent.isPlaying(c)&&c.state().happiness==24,
          "Voluntary contact and actual pursuit finish with exactly one reward");
      h.assertTrue(CUES.get(c).size()==moved.get(),"Only successful physical contacts emit one quiet ball cue");
      h.assertTrue(countBalls(h)==1&&drops(h)==0,"Completed play retains one recoverable physical ball");
      physicalIdle(h,c);identity(h,c,adult,first,alignment);
    });
  }
  private static void calibratedContact(GameTestHelper h,ChaoEntity c,BlockPos source){
    var delta=c.position().subtract(Vec3.atBottomCenterOf(source));
    var side=Direction.getNearest(delta.x,0,delta.z);double reach=BallPlayRules074.contactDistance(c.state(),c.getChaoHat());
    var expected=Vec3.atBottomCenterOf(source).add(side.getStepX()*reach,0,side.getStepZ()*reach);
    h.assertTrue(c.position().distanceToSqr(expected)<.015*.015,"Successful movement occurs at the calibrated anatomy/hat contact root, not only the legacy loose reach guard");
    var toward=Vec3.atBottomCenterOf(source).subtract(c.position()).multiply(1,0,1).normalize();
    h.assertTrue(Vec3.directionFromRotation(0,c.getYRot()).dot(toward)>.995,"The actual body faces its source ball at contact");
  }
  private static void beforeFirstContact(GameTestHelper h,Fixture f,Runnable action){
    var done=new AtomicBoolean();h.onEachTick(()->{
      int phase=f.c.toyKickElapsed();
      if(done.get()||phase<1||phase>=ToyInteractionRules.BALL_CONTACT_TICK)return;
      done.set(true);h.assertTrue(f.c.toyTarget()!=null&&f.c.toyTarget().equals(f.origin)&&CUES.get(f.c).isEmpty(),"Probe occurs before the first actual contact/transfer");
      action.run();
    });
  }
  private static void stopBeforeContact(GameTestHelper h){
    var f=fixture(h,true);String before=f.c.state().encode();h.assertTrue(command(f.p,"chao testtoy ball loop")==1,"Start actual close-contact preview");
    beforeFirstContact(h,f,()->{
      h.assertTrue(f.c.state().encode().equals(before),"Active pre-contact care remains exact");
      h.assertTrue(command(f.p,"chao testtoy ball stop")==1,"Stop before the first contact");
      h.runAfterDelay(CLEANUP_DELAY,()->{
        physicalIdle(h,f.c);h.assertTrue(countBalls(h)==1&&drops(h)==0&&CUES.get(f.c).isEmpty(),"Pre-contact cancellation cannot move/copy/drop/sound the ball");
        var contact=f.c.position();var clear=contact.add(-.7,0,0);f.c.setPos(clear.x,clear.y,clear.z);
        h.runAfterDelay(2,()->{
          h.assertTrue(!f.c.getPersistentData().contains("ChaoBallContactClearance074")&&!ToyContent.ownsBallContact(f.c,f.origin),"The narrow clearance lease expires when the actual body leaves that real ball");
          f.c.setPos(contact.x,contact.y,contact.z);
          h.assertTrue(!f.c.level().noCollision(f.c,f.c.getBoundingBox()),"A later unclaimed re-entry cannot reuse the released collision exception");
          f.c.setPos(clear.x,clear.y,clear.z);h.succeed();
        });
      });
    });
  }
  private static void blockedTerminal(GameTestHelper h){
    var at=floor(h);h.getLevel().setBlock(at,ToyContent.block(KIND).defaultBlockState(),3);h.getLevel().setBlock(at.east(),Blocks.STONE.defaultBlockState(),3);
    var c=actor(h,Vec3.atBottomCenterOf(at.west()),true);c.goalSelector.addGoal(4,new ToyContent.PlayGoal(c));var played=new AtomicBoolean();var contact=new AtomicBoolean();
    h.onEachTick(()->{
      if(!ToyContent.isPlaying(c))return;played.set(true);h.assertTrue(c.toyTarget().equals(at),"A blocked terminal fixture never commits a roll");
      if(c.toyKickElapsed()>=ToyInteractionRules.BALL_CONTACT_TICK)contact.set(true);
    });
    h.succeedWhen(()->{
      h.assertTrue(played.get()&&contact.get()&&!ToyContent.isPlaying(c)&&c.state().happiness==24,"An ordinary finite blocked nudge session completes with its existing single reward");
      physicalIdle(h,c);h.assertTrue(countBalls(h)==1&&drops(h)==0&&CUES.get(c).isEmpty(),"The waiting actor and its sole unmoved ball remain collision-clear after terminal cleanup");
    });
  }
  private static void precontactReload(GameTestHelper h){
    var f=fixture(h,true);f.c.setNoAi(true);h.getLevel().setBlock(f.origin.east(),Blocks.STONE.defaultBlockState(),3);String before=f.c.state().encode();
    h.assertTrue(command(f.p,"chao testtoy ball loop")==1,"Original-NoAI actor can use actual temporary moving preview");
    beforeFirstContact(h,f,()->{
      var tag=new CompoundTag();f.c.saveWithoutId(tag);h.assertTrue(tag.getBoolean("NoAI"),"Ordinary saved NoAI survives active-preview serialization");
      f.c.discard();var loaded=ChaoGarden.CHAO.get().create(h.getLevel());loaded.load(tag);h.getLevel().addFreshEntity(loaded);
      CUES.put(loaded,new ArrayList<>());cleanup(h,()->{ToyPreview.stop(loaded);CUES.remove(loaded);loaded.discard();});
      h.assertTrue(!ToyPreview.active(loaded)&&loaded.toyPlayingTicks()==0&&loaded.toyTarget()==null&&loaded.isNoAi()
          &&loaded.state().encode().equals(before),"Reload restores ordinary frozen-AI Chao state without resuming a preview");
      h.assertTrue(loaded.getPersistentData().contains("ChaoBallContactClearance074")&&ToyContent.ownsBallContact(loaded,f.origin)
          &&loaded.level().noCollision(loaded,loaded.getBoundingBox()),"A saved pre-contact oversized body retains only its valid real-ball clearance");
      h.assertTrue(!ToyContent.ownsBallContact(loaded,f.origin.east(2)),"Persisted clearance cannot apply to another cell");
      h.runAfterDelay(CLEANUP_DELAY,()->{
        physicalIdle(h,loaded);h.assertTrue(loaded.isNoAi()&&countBalls(h)==1&&drops(h)==0&&CUES.get(loaded).isEmpty(),"NoAI reload neither starts a future kick nor duplicates/consumes a ball");
        var clear=loaded.position().add(-.7,0,0);loaded.setPos(clear.x,clear.y,clear.z);
        h.runAfterDelay(2,()->{h.assertTrue(!loaded.getPersistentData().contains("ChaoBallContactClearance074")&&!ToyContent.ownsBallContact(loaded,f.origin),"Saved clearance self-cleans when its real overlap ends");h.succeed();});
      });
    });
  }
  private static final class JumpObservedChao extends ChaoEntity {
    int jumpRequests;
    JumpObservedChao(net.minecraft.world.level.Level level){super(ChaoGarden.CHAO.get(),level);}
    @Override public void setJumping(boolean jumping){super.setJumping(jumping);if(jumping)jumpRequests++;}
  }
  private static void moveControlDelegation(GameTestHelper h){
    var at=floor(h);var c=new JumpObservedChao(h.getLevel());c.moveTo(Vec3.atBottomCenterOf(at));c.setOnGround(true);c.setNoAi(true);c.setInvulnerable(true);
    h.getLevel().addFreshEntity(c);c.goalSelector.removeAllGoals(g->true);cleanup(h,c::discard);
    h.assertTrue(c.getMoveControl() instanceof com.chaogarden.entity.BallContactMoveControl074,"Actual Chao constructor installs the narrow ball contact control");
    h.getLevel().setBlock(at,Blocks.OAK_SLAB.defaultBlockState(),3);
    c.getMoveControl().setWantedPosition(c.getX()+1,c.getY(),c.getZ(),.9);c.getMoveControl().tick();c.getJumpControl().tick();
    h.assertTrue(c.jumpRequests>0,"Ordinary low-obstacle movement still delegates vanilla's jump request");
    c.getMoveControl().tick();c.getJumpControl().tick();c.jumpRequests=0;
    h.getLevel().setBlock(at,ToyContent.block(KIND).defaultBlockState(),3);var point=Vec3.atBottomCenterOf(at).add(-BallPlayRules074.contactDistance(c.state(),c.getChaoHat()),0,0);c.setPos(point.x,point.y,point.z);c.setOnGround(true);
    c.getPersistentData().putLong("ChaoToyTarget",at.asLong());c.getPersistentData().putLong("ChaoToyApproachUntil",h.getLevel().getGameTime()+20);
    c.getMoveControl().setWantedPosition(c.getX()+.025,c.getY(),c.getZ(),.9);c.getMoveControl().tick();c.getJumpControl().tick();
    h.assertTrue(c.jumpRequests==0&&c.getSpeed()>0,"Only the claimed low ball's empty contact space suppresses the false ground-level jump");
    c.getPersistentData().remove("ChaoToyTarget");c.getPersistentData().remove("ChaoToyApproachUntil");c.jumpRequests=0;
    c.getMoveControl().setWantedPosition(c.getX()+.025,c.getY(),c.getZ(),.9);c.getMoveControl().tick();c.getJumpControl().tick();
    h.assertTrue(c.jumpRequests>0,"An unclaimed ball retains ordinary collision/jump behavior");
    c.getMoveControl().tick();c.getJumpControl().tick();c.jumpRequests=0;
    c.getPersistentData().putLong("ChaoToyTarget",at.asLong());c.getPersistentData().putLong("ChaoToyApproachUntil",h.getLevel().getGameTime()+20);
    c.getMoveControl().setWantedPosition(c.getX()+.025,c.getY()+1,c.getZ(),.9);c.getMoveControl().tick();c.getJumpControl().tick();
    h.assertTrue(c.jumpRequests>0,"A real height change still delegates vanilla jumping even at a claimed ball");
    c.getPersistentData().remove("ChaoToyTarget");c.getPersistentData().remove("ChaoToyApproachUntil");h.succeed();
  }
  private static void loopActionSeam(GameTestHelper h){
    var f=fixture(h,true);h.getLevel().setBlock(f.origin.east(),Blocks.STONE.defaultBlockState(),3);String before=f.c.state().encode();long start=h.getLevel().getGameTime();
    var boundary=new long[]{-1};var phase=new int[]{-1};var done=new AtomicBoolean();
    h.assertTrue(command(f.p,"chao testtoy ball loop")==1,"Start actual bounded blocked-ball loop");
    h.onEachTick(()->{
      if(done.get())return;long now=h.getLevel().getGameTime(),elapsed=now-start;int action=f.c.toyKickElapsed();
      h.assertTrue(ToyPreview.active(f.c)&&f.c.state().encode().equals(before),"Longer loop preserves the complete ordinary Chao state");
      if(elapsed%ToyContent.PLAY_TICKS==159&&action>=0&&action<BallPlayRules074.ACTION_TICKS-2){boundary[0]=now+1;phase[0]=action;}
      if(now==boundary[0])h.assertTrue(action==phase[0]+1,"Loop renewal retains the same in-flight action at its first boundary tick");
      if(now==boundary[0]+1&&boundary[0]>=0){
        h.assertTrue(action==phase[0]+2&&f.c.toyTarget().equals(f.origin),"In-flight action and target remain continuous after the next entity tick too");
        done.set(true);h.assertTrue(command(f.p,"chao testtoy ball stop")==1,"Stop observed continuous action");
        h.runAfterDelay(CLEANUP_DELAY,()->{physicalIdle(h,f.c);h.assertTrue(countBalls(h)==1&&drops(h)==0&&CUES.get(f.c).isEmpty(),"Repeated blocked loop has no extra real ball, cue or reward");h.succeed();});
      }
    });
  }
  private static Fixture fixture(GameTestHelper h,boolean adult){
    var at=floor(h);h.getLevel().setBlock(at,ToyContent.block(KIND).defaultBlockState().setValue(ChaoToyBlock.FACING,Direction.SOUTH),3);
    var c=actor(h,Vec3.atBottomCenterOf(at).add(-.7,0,0),adult);
    c.getPersistentData().putLong("ChaoToyNextPlay",h.getLevel().getGameTime()+12345);
    var sentinel=new Goal(){public boolean canUse(){return false;}};c.goalSelector.addGoal(9,sentinel);
    var p=TestConnections.makeSurvivalServerPlayer(h);p.teleportTo(c.getX(),c.getY(),c.getZ()+2);c.owner=p.getUUID();
    p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK,3));
    c.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.FEATHER,2));
    cleanup(h,()->{if(p.server.getPlayerList().getPlayer(p.getUUID())!=null)p.server.getPlayerList().remove(p);});return new Fixture(c,p,at,sentinel);
  }
  private static int command(ServerPlayer p,String text,int permission){
    try{return p.server.getCommands().getDispatcher().execute(text,p.createCommandSourceStack().withPermission(permission));}
    catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){return -1;}
  }
  private static int command(ServerPlayer p,String text){return command(p,text,2);}
  private static Map<Goal,Integer> goals(ChaoEntity c){return c.goalSelector.getAvailableGoals().stream().collect(java.util.stream.Collectors.toMap(g->g.getGoal(),g->g.getPriority()));}
  private static void preview(GameTestHelper h,boolean adult,boolean loop){
    var f=fixture(h,adult);String before=f.c.state().encode();long cooldown=f.c.getPersistentData().getLong("ChaoToyNextPlay");var originalGoals=goals(f.c);
    var moved=new AtomicBoolean();var lastBall=new AtomicReference<>(f.origin);h.onEachTick(()->{
      if(ToyPreview.active(f.c)&&f.c.toyTarget()!=null){var now=f.c.toyTarget();var old=lastBall.getAndSet(now);if(!now.equals(old)){moved.set(true);calibratedContact(h,f.c,old);}}
    });
    h.assertTrue(command(f.p,"chao testtoy ball "+(loop?"loop":"once"))==1,"Native dispatcher starts the real placed-ball preview");
    h.runAfterDelay(80,()->{h.assertTrue(ToyPreview.active(f.c)&&f.c.state().encode().equals(before),"Moving preview pauses care, age, genes and training");
      h.assertTrue(countBalls(h)==1&&drops(h)==0,"Preview moves one real ball without minted items");});
    h.runAfterDelay(159,()->h.assertTrue(ToyPreview.active(f.c)&&f.c.state().encode().equals(before),"Exact state remains through the last active once tick"));
    h.runAfterDelay(loop?330:160,()->{
      if(loop){h.assertTrue(ToyPreview.active(f.c),"Explicit loop repeats the production session");h.assertTrue(command(f.p,"chao testtoy ball stop")==1,"Native loop stop");}
      else if(ToyPreview.active(f.c))ToyPreview.tick(f.c);
      h.assertTrue(!ToyPreview.active(f.c)&&f.c.state().encode().equals(before),"Terminal preview tick adds no reward, care or training");
      h.assertTrue(f.c.getPersistentData().getLong("ChaoToyNextPlay")==cooldown&&!f.c.isNoAi(),"Ordinary cooldown and AI restored");
      h.assertTrue(goals(f.c).equals(originalGoals),"Temporary moving-preview goal removed; original goals retained");
      h.assertTrue(moved.get(),"This close preview actually contacted and moved its real ball");
      h.assertTrue(f.p.getMainHandItem().is(Items.STICK)&&f.p.getMainHandItem().getCount()==3
          &&f.c.getOffhandItem().is(Items.FEATHER)&&f.c.getOffhandItem().getCount()==2,"Real keeper/equipped items are unchanged");
      int cues=CUES.get(f.c).size();h.runAfterDelay(CLEANUP_DELAY,()->{physicalIdle(h,f.c);identity(h,f.c,adult);
        h.assertTrue(countBalls(h)==1&&drops(h)==0&&CUES.get(f.c).size()==cues,"No future kick, item copy or cue after preview finishes");h.succeed();});
    });
  }
  private static void interrupt(GameTestHelper h,String mode){
    var f=fixture(h,true);boolean originalNoAi=mode.equals("original_noai");f.c.setNoAi(originalNoAi);
    if(mode.equals("missing_cooldown"))f.c.getPersistentData().remove("ChaoToyNextPlay");
    String before=f.c.state().encode();long cooldown=f.c.getPersistentData().getLong("ChaoToyNextPlay");boolean hadCooldown=f.c.getPersistentData().contains("ChaoToyNextPlay");var originalGoals=goals(f.c);var requested=new AtomicBoolean();
    h.assertTrue(command(f.p,"chao testtoy ball loop")==1,"Start production moving preview before interruption");
    h.onEachTick(()->{
      if(requested.get()||f.c.toyTarget()==null||f.c.toyTarget().equals(f.origin))return;
      requested.set(true);h.runAfterDelay(2,()->{
        h.assertTrue(f.c.state().encode().equals(before),"Preview state exact through the deliberate interruption");
        var committed=f.c.toyTarget();h.assertTrue(committed!=null&&h.getLevel().getBlockState(committed).getValue(ChaoBallBlock074.ROLLING),"Interruption exercises the actual committed mid-roll interval");
        switch(mode){
          case "stop","original_noai","missing_cooldown"->h.assertTrue(command(f.p,"chao testtoy stop")==1,"Explicit native stop");
          case "damage"->{f.c.setInvulnerable(false);h.assertTrue(f.c.hurt(h.getLevel().damageSources().generic(),1),"Actual damage accepted");}
          case "pet"->{f.p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);h.assertTrue(f.c.mobInteract(f.p,InteractionHand.MAIN_HAND).consumesAction()&&f.c.getAffectionTicks()>0,"Actual pet begins after cancellation");}
          case "remove"->h.assertTrue(h.getLevel().destroyBlock(committed,true),"Break the actual committed ball");
          case "displace"->f.c.setPos(f.c.getX()-6,f.c.getY(),f.c.getZ());
          case "entity_leave"->f.c.discard();
          case "logout"->f.p.server.getPlayerList().remove(f.p);
          case "range"->f.p.teleportTo(f.c.getX()+70,f.c.getY(),f.c.getZ());
          case "reload"->{
            var tag=new CompoundTag();f.c.saveWithoutId(tag);
            h.assertTrue(tag.getBoolean("NoAI")==originalNoAi&&!tag.contains("ToyPlayingKind")&&!tag.contains("ToyPlayingRemaining"),"Disk fallback contains ordinary flags and no preview clock");
            f.c.discard();var loaded=ChaoGarden.CHAO.get().create(h.getLevel());loaded.load(tag);h.getLevel().addFreshEntity(loaded);
            h.assertTrue(!ToyPreview.active(loaded)&&loaded.toyPlayingTicks()==0&&!loaded.isNoAi()&&loaded.state().encode().equals(before),"Chao reload cannot resume preview, age it or mint a toy");loaded.discard();
          }
          default->throw new IllegalStateException(mode);
        }
        int happiness=f.c.state().happiness;
        if(ToyPreview.active(f.c))ToyPreview.tick(f.c);
        h.runAfterDelay(CLEANUP_DELAY,()->{
          physicalIdle(h,f.c);h.assertTrue(f.c.isNoAi()==originalNoAi&&goals(f.c).equals(originalGoals),"Original AI flags/goals restored after interruption");
          h.assertTrue(f.c.getPersistentData().getLong("ChaoToyNextPlay")==cooldown
              &&f.c.getPersistentData().contains("ChaoToyNextPlay")==hadCooldown&&f.c.state().happiness==happiness,"No preview reward or cooldown value/presence mutation");
          h.assertTrue(countBalls(h)==(mode.equals("remove")?0:1)&&drops(h)==(mode.equals("remove")?1:0),"Interruption retains exactly the actual physical item ownership");
          if(!mode.equals("remove"))h.assertTrue(h.getLevel().getBlockState(committed).is(ToyContent.block(KIND)),"A committed roll stays at its actual cell after cancellation");
          int cues=CUES.get(f.c).size();h.runAfterDelay(16,()->{h.assertTrue(CUES.get(f.c).size()==cues&&!ToyPreview.active(f.c)&&f.c.toyPlayingTicks()==0,"No ghost pursuit, future roll or cue after cancellation");h.succeed();});
        });
      });
    });
  }
  private static void blocked(GameTestHelper h,String mode){
    var f=fixture(h,true);var destination=f.origin.east();var attempted=new AtomicBoolean();
    Runnable obstacle=()->{
      if(mode.equals("solid")||mode.equals("late_blocker"))h.getLevel().setBlock(destination,Blocks.STONE.defaultBlockState(),3);
      else if(mode.equals("edge"))h.getLevel().setBlock(destination.below(),Blocks.AIR.defaultBlockState(),3);
      else{
        var b=new ArmorStand(h.getLevel(),f.origin.getX()+(mode.equals("living_sweep")?.95:1.5),f.origin.getY(),f.origin.getZ()+.5);b.setNoGravity(true);
        h.getLevel().addFreshEntity(b);cleanup(h,b::discard);
        if(mode.equals("living_sweep")){
          var end=ToyContent.block(KIND).defaultBlockState().getCollisionShape(h.getLevel(),destination).bounds().move(destination);
          h.assertTrue(!b.getBoundingBox().intersects(end),"Sweep blocker is intentionally outside the final-cell collision box");
        }
      }
    };
    if(!mode.equals("late_blocker"))obstacle.run();
    h.assertTrue(command(f.p,"chao testtoy ball loop")==1,"Start contact attempt with the real placed ball");
    var added=new AtomicBoolean();h.onEachTick(()->{
      if(mode.equals("late_blocker")&&!added.get()&&f.c.toyKickElapsed()>=1&&f.c.toyKickElapsed()<ToyInteractionRules.BALL_CONTACT_TICK){added.set(true);obstacle.run();}
      if(f.c.toyKickElapsed()>=ToyInteractionRules.BALL_CONTACT_TICK)attempted.set(true);
    });
    h.runAfterDelay(110,()->{
      h.assertTrue(attempted.get(),"A real action reached its contact boundary");
      h.assertTrue(f.c.toyTarget()!=null&&f.c.toyTarget().equals(f.origin)&&countBalls(h)==1&&drops(h)==0,"Blocked/swept/unsupported destination cannot move the physical ball");
      h.assertTrue(CUES.get(f.c).isEmpty(),"Blocked contact emits no successful-roll sound");
      command(f.p,"chao testtoy stop");h.runAfterDelay(CLEANUP_DELAY,()->{physicalIdle(h,f.c);h.succeed();});
    });
  }
  private static void reject(GameTestHelper h,String mode){
    var f=fixture(h,true);String before=f.c.state().encode();var originalGoals=goals(f.c);
    if(mode.equals("absent"))h.getLevel().setBlock(f.origin,Blocks.AIR.defaultBlockState(),3);
    if(mode.equals("reserved")){
      var other=actor(h,Vec3.atBottomCenterOf(f.origin.north()),true);other.setNoAi(true);
      other.getPersistentData().putLong("ChaoToyTarget",f.origin.asLong());other.getPersistentData().putLong("ChaoToyUntil",h.getLevel().getGameTime()+160);other.setToyPlaying(KIND,160);other.setToyTarget(f.origin);
    }
    if(mode.equals("carrying"))f.c.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.APPLE));
    h.assertTrue(command(f.p,"chao testtoy ball once")!=1,"Invalid preview has clear refusal instead of stealing/minting a ball");
    h.assertTrue(!ToyPreview.active(f.c)&&f.c.toyPlayingTicks()==0&&!f.c.isNoAi()&&f.c.state().encode().equals(before)&&goals(f.c).equals(originalGoals),"Rejected command leaves ordinary Chao state/AI/goals intact");
    h.assertTrue(drops(h)==0,"Rejected command does not create a real item");h.succeed();
  }
  private static void permissions(GameTestHelper h){
    var f=fixture(h,true);String before=f.c.state().encode();
    h.assertTrue(command(f.p,"chao testtoy ball once",0)!=1&&!ToyPreview.active(f.c),"Ordinary Survival source cannot edit or animate another Chao");
    h.assertTrue(command(f.p,"chao testtoy list")==1,"Operator catalog is native dispatcher accessible");
    var source=f.p.createCommandSourceStack().withPermission(2);var dispatcher=f.p.server.getCommands().getDispatcher();
    var node=dispatcher.getRoot().getChild("chao").getChild("testtoy").getChild("ball");
    h.assertTrue(node!=null&&node.getChild("once")!=null&&node.getChild("loop")!=null&&node.getChild("stop")!=null,"Ball registers once/loop/stop in the real command tree");
    h.assertTrue(dispatcher.getCompletionSuggestions(dispatcher.parse("chao testtoy b",source)).join().getList().stream().anyMatch(s->s.getText().equals("ball")),"Native completion suggests ball");
    h.assertTrue(command(f.p,"chao testtoy ball impossible")!=1&&f.c.state().encode().equals(before),"Invalid playback mode cannot mutate a Chao");
    h.assertTrue(command(f.p,"chao testtoy ball once")==1&&ToyPreview.active(f.c),"Authorized operator reaches the actual moving production adapter");
    h.assertTrue(command(f.p,"chao testtoy ball stop")==1&&!ToyPreview.active(f.c)&&f.c.state().encode().equals(before),"Native explicit stop preserves the actual saved state");h.succeed();
  }
  private static void compatibility(GameTestHelper h){
    var at=floor(h);var state=ToyContent.block(KIND).defaultBlockState().setValue(ChaoToyBlock.FACING,Direction.WEST).setValue(ChaoToyBlock.FRAME,3);
    h.getLevel().setBlock(at,state,3);h.assertTrue(state.hasProperty(ChaoToyBlock.WATERLOGGED)&&!state.getValue(ChaoBallBlock074.ROLLING),"Legacy ball state keys remain and new rolling state defaults safely");
    var item=new ItemStack(ChaoGarden.item(KIND));h.assertTrue(item.getItem() instanceof BlockItem b&&b.getBlock()==ToyContent.block(KIND)&&item.getMaxStackSize()==16,"Existing ID, BlockItem and stack size preserved");
    h.assertTrue(MarketStock.LEGACY_TOYS.contains(KIND)&&!MarketStock.TOYS.contains(KIND),"Acquisition is not silently moved into new merchant stock");
    h.getLevel().removeBlockEntity(at);h.assertTrue(h.getLevel().getBlockState(at).is(ToyContent.block(KIND))&&h.getLevel().getBlockEntity(at)!=null,"Old placed balls missing the newly introduced block entity acquire it without block/item replacement");
    h.assertTrue(h.getLevel().destroyBlock(at,true),"Existing break/recovery path remains available");h.runAfterDelay(2,()->{h.assertTrue(countBalls(h)==0&&drops(h)==1,"One old-ID ball drops exactly one recoverable old-ID item");h.succeed();});
  }
  private static void motionReplication(GameTestHelper h){
    var f=fixture(h,true);var observed=new AtomicBoolean();
    h.assertTrue(command(f.p,"chao testtoy ball loop")==1,"Start real moving preview for replication/reload observation");
    h.onEachTick(()->{
      if(observed.get()||f.c.toyTarget()==null||f.c.toyTarget().equals(f.origin))return;
      observed.set(true);h.runAfterDelay(2,()->{
        var at=f.c.toyTarget();h.assertTrue(h.getLevel().getBlockEntity(at) instanceof BallToyBlockEntity074,"The committed source owns its actual motion block entity");
        var real=(BallToyBlockEntity074)h.getLevel().getBlockEntity(at);var state=h.getLevel().getBlockState(at);var lookup=h.getLevel().registryAccess();
        h.assertTrue(real.rolling(0)&&real.progress(0)>0&&real.progress(0)<1,"Observe genuine intermediate motion rather than an endpoint");
        var update=real.getUpdateTag(lookup);var packet=real.getUpdatePacket();
        h.assertTrue(update.getBoolean("RollNetwork074")&&packet!=null&&packet.getPos().equals(at)
            &&packet.getType()==ToyContent.BALL_ENTITY.get()&&packet.getTag().equals(update),"Actual native packet identifies the committed source and carries explicit transient motion state");
        var late=new BallToyBlockEntity074(at,state);late.setLevel(h.getLevel());late.handleUpdateTag(update,lookup);
        h.assertTrue(late.rollStarted()==real.rollStarted()&&late.rollDirection()==real.rollDirection()
            &&late.offset(0).distanceToSqr(real.offset(0))<1e-12&&late.rolling(0),"Current update-tag round trip preserves the same intermediate ball position and direction");
        var packetReceiver=new BallToyBlockEntity074(at,state);packetReceiver.setLevel(h.getLevel());packetReceiver.loadWithComponents(packet.getTag(),lookup);
        h.assertTrue(packetReceiver.rolling(0)&&packetReceiver.offset(0).distanceToSqr(real.offset(0))<1e-12,"The block-entity packet load path reproduces current motion too");
        var futureTag=update.copy();futureTag.putLong("RollStarted",h.getLevel().getGameTime()+2);
        var skewed=new BallToyBlockEntity074(at,state);skewed.setLevel(h.getLevel());skewed.handleUpdateTag(futureTag,lookup);
        h.assertTrue(skewed.rolling(0)&&skewed.progress(0)==0&&Math.abs(skewed.offset(0).lengthSqr()-1)<1e-12,
            "A small clock lead waits at the real source instead of snapping to the committed endpoint");
        var pending=new BallToyBlockEntity074(at,state);pending.setLevel(h.getLevel());pending.handleUpdateTag(new CompoundTag(),lookup);
        h.assertTrue(!pending.rolling(0)&&pending.offset(0).lengthSqr()<1e-12,"Missing motion data has a finite committed-cell fallback");
        var disk=real.saveWithoutMetadata(lookup);
        h.assertTrue(!disk.contains("RollNetwork074")&&!disk.contains("RollFrom")&&!disk.contains("RollStarted"),"Disk persistence stores only the committed real ball, not a resumable visual transfer");
        var restored=new BallToyBlockEntity074(at,state);restored.setLevel(h.getLevel());restored.loadWithComponents(disk,lookup);
        h.assertTrue(!restored.rolling(0)&&restored.offset(0).lengthSqr()<1e-12,"Reload cannot revive a ghost source cell or unfinished cosmetic roll");
        h.assertTrue(command(f.p,"chao testtoy ball stop")==1,"Stop movement before final observer checks");
        h.runAfterDelay(CLEANUP_DELAY,()->{
          var expired=new BallToyBlockEntity074(at,state);expired.setLevel(h.getLevel());expired.handleUpdateTag(update,lookup);
          h.assertTrue(!expired.rolling(0)&&expired.offset(0).lengthSqr()<1e-12&&!skewed.rolling(0)&&skewed.offset(0).lengthSqr()<1e-12,
              "Old and briefly clock-ahead motion packets both reach the committed endpoint for a late observer");
          physicalIdle(h,f.c);h.assertTrue(countBalls(h)==1&&drops(h)==0,"Update/save probes never create a collectible or duplicate physical toy");h.succeed();
        });
      });
    });
  }
  private static void naturalInterrupt(GameTestHelper h,boolean remove){
    var at=floor(h);h.getLevel().setBlock(at,ToyContent.block(KIND).defaultBlockState(),3);
    var c=actor(h,Vec3.atBottomCenterOf(at.west()),true);c.goalSelector.addGoal(4,new ToyContent.PlayGoal(c));var done=new AtomicBoolean();
    h.onEachTick(()->{
      if(done.get()||!ToyContent.isPlaying(c)||c.toyKickElapsed()<1||c.toyKickElapsed()>=ToyInteractionRules.BALL_CONTACT_TICK)return;
      done.set(true);var target=c.toyTarget();if(remove)h.getLevel().destroyBlock(target,true);else{c.setInvulnerable(false);h.assertTrue(c.hurt(h.getLevel().damageSources().generic(),1),"Real natural-play damage accepted");}
      int happiness=c.state().happiness;h.runAfterDelay(12,()->{physicalIdle(h,c);h.assertTrue(c.state().happiness==happiness,"Interrupted natural play cannot award completion");
        h.assertTrue(countBalls(h)==(remove?0:1)&&drops(h)==(remove?1:0),"Natural interruption retains exactly physical ownership");
        h.assertTrue(CUES.get(c).isEmpty(),"Interruption before contact cannot produce a remote kick or contact cue");h.succeed();});
    });
  }
}
