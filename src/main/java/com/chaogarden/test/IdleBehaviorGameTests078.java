package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoEmotion;
import com.chaogarden.core.ChaoIdleRules078;
import com.chaogarden.core.ChaoPreferences;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.entity.ChaoEntity.Behavior;
import com.chaogarden.interaction.ChaoBehaviorGoal;
import com.chaogarden.toys.ChaoToyBlock;
import com.chaogarden.toys.ToyContent;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.bundle.PacketAndPayloadAcceptor;

/** Live goal arbitration and natural clocks. These fixtures remove unrelated stroll/look/follow goals. */
@GameTestHolder("chao_idle78") @PrefixGameTestTemplate(false)
public final class IdleBehaviorGameTests078 {
  private static final String TEMPLATE="chao_idle78:activity_test";
  private record Fixture(ChaoEntity chao,ServerPlayer keeper,long created,String genes,int[] points) {}
  private record Observer(ChaoState state,Behavior behavior,String detail,int duration,int remaining,float elapsed) {}
  private IdleBehaviorGameTests078() {}

  @GameTestGenerator public static Collection<TestFunction> cases() {
    var tests=new ArrayList<TestFunction>();
    add(tests,"idle_natural_two_cycles_rest_without_tired_or_rewards",2200,IdleBehaviorGameTests078::lifecycle);
    add(tests,"idle_natural_priority_two_need_replaces_gesture",1100,IdleBehaviorGameTests078::need);
    add(tests,"idle_natural_priority_six_navigation_preempts_gesture",1100,IdleBehaviorGameTests078::movement);
    add(tests,"idle_natural_real_toy_selection_preempts_gesture",1250,IdleBehaviorGameTests078::toy);
    add(tests,"idle_natural_sleep_need_interrupts_gesture",1050,IdleBehaviorGameTests078::sleep);
    add(tests,"idle_natural_real_item_offer_interrupts_gesture",1050,IdleBehaviorGameTests078::item);
    add(tests,"idle_natural_real_pet_interrupts_gesture",1050,IdleBehaviorGameTests078::pet);
    add(tests,"idle_natural_real_damage_owns_recovery_clock",1050,IdleBehaviorGameTests078::damage);
    add(tests,"idle_natural_reload_cancels_transient_clock_and_waits_fresh_gap",2000,IdleBehaviorGameTests078::reload);
    add(tests,"idle_natural_early_late_and_rejoining_tracker_metadata",1100,IdleBehaviorGameTests078::observers);
    return tests;
  }
  private static void add(List<TestFunction> tests,String name,int timeout,Consumer<GameTestHelper> run) {
    tests.add(new TestFunction("idle078_natural_"+(tests.size()/5),name,TEMPLATE,timeout,0,true,run));
  }
  private static void cleanup(GameTestHelper h,Runnable action) {
    h.testInfo.addListener(new GameTestListener() {
      public void testStructureLoaded(GameTestInfo info) {}
      public void testPassed(GameTestInfo info,GameTestRunner runner) {action.run();}
      public void testFailed(GameTestInfo info,GameTestRunner runner) {action.run();}
      public void testAddedForRerun(GameTestInfo oldInfo,GameTestInfo newInfo,GameTestRunner runner) {action.run();}
    });
  }
  private static void controlledGoals(ChaoEntity c,boolean needs) {
    c.goalSelector.removeAllGoals(goal->true);c.targetSelector.removeAllGoals(goal->true);
    if(needs)c.goalSelector.addGoal(2,new ChaoBehaviorGoal(c));
    c.goalSelector.addGoal(7,ChaoBehaviorGoal.idle(c));
  }
  private static Fixture fixture(GameTestHelper h,boolean needs) {
    for(int x=1;x<=23;x++)for(int z=1;z<=23;z++) {
      h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
      for(int y=1;y<=5;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
    }
    var c=ChaoGarden.CHAO.get().create(h.getLevel());h.assertTrue(c!=null,"Production Chao creates");
    var s=new ChaoState();s.adult=true;s.hunger=100;s.energy=100;s.happiness=30;s.bond=0;s.boredom=20;
    s.ageTicks=321;s.points[ChaoState.RUN]=667;s.setBaselineFace076(1,4);s.eyeColor=7;
    for(int emotion=0;emotion<ChaoEmotion.COUNT;emotion++)s.setEmotion(emotion,0);
    s.setEmotion(ChaoEmotion.ENERGY,10000);s.setEmotion(ChaoEmotion.BOREDOM,2000);
    s.setTrait("idle078.sentinel","unchanged");
    for(int illness=0;illness<6;illness++)s.setIllness(illness,100);
    c.setState(ChaoState.decode(s.encode()));c.setNoAi(false);c.setInvulnerable(true);c.getRandom().setSeed(78);
    c.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(10,1,10))));c.setOnGround(true);
    h.getLevel().addFreshEntity(c);
    h.assertTrue(c.goalSelector.getAvailableGoals().stream().anyMatch(g->g.getPriority()==7&&g.getGoal() instanceof ChaoBehaviorGoal),
        "Ordinary production registration includes the lower-priority idle goal");
    controlledGoals(c,needs);
    c.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.CARVED_PUMPKIN));
    c.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.STICK));
    var keeper=TestConnections.makeSurvivalServerPlayer(h);keeper.teleportTo(c.getX()+2,c.getY(),c.getZ());c.owner=keeper.getUUID();
    var result=new Fixture(c,keeper,h.getLevel().getGameTime(),c.state().dnaSummary(),c.state().points.clone());
    cleanup(h,()->{ToyContent.endPlaying(c,false);c.discard();if(keeper.server.getPlayerList().getPlayer(keeper.getUUID())!=null)keeper.server.getPlayerList().remove(keeper);});
    return result;
  }
  private static void identity(GameTestHelper h,Fixture f,ChaoEntity c) {
    h.assertTrue(c.state().dnaSummary().equals(f.genes())&&c.state().baselineEyes()==1&&c.state().baselineMouth()==4&&c.state().eyeColor==7,
        "Natural gestures preserve inherited genes, fixed face and iris");
    h.assertTrue(c.state().trait("idle078.sentinel").equals("unchanged")&&c.getItemBySlot(EquipmentSlot.HEAD).is(Items.CARVED_PUMPKIN)
        &&c.getItemBySlot(EquipmentSlot.OFFHAND).is(Items.STICK),"Natural gestures preserve unrelated saved traits and real equipment");
  }
  private static void naturalStart(GameTestHelper h,Fixture f,ChaoEntity c,long earliest) {
    long began=h.getLevel().getGameTime()-(long)c.getBehaviorElapsed(0);
    h.assertTrue(began-earliest>=399&&began-earliest<=915,"Actual goal waits its production 400–900 tick quiet gap before selection");
    h.assertTrue(ChaoIdleRules078.valid(c.behaviorDetail())&&c.getBehaviorDuration()==ChaoIdleRules078.duration(c.behaviorDetail())
        &&c.getBehaviorElapsed(0)<=3&&!c.isNoAi()&&c.behaviorPartner()==null&&c.toyPlayingTicks()==0,
        "Live selector starts a valid unpaired production detail with its authored clock and ordinary AI");
    h.assertTrue(!c.behaviorDetail().equals("tired")&&!ChaoIdleRules078.allowed(c.state(),"tired",true),
        "A rested Chao never spontaneously selects the tired gesture");identity(h,f,c);
  }
  private static void onFirstIdle(GameTestHelper h,Fixture f,Consumer<ChaoEntity> action) {
    var started=new AtomicBoolean();
    h.onEachTick(()-> {
      if(started.get()||f.chao().getBehavior()!=Behavior.IDLE)return;
      started.set(true);naturalStart(h,f,f.chao(),f.created());action.accept(f.chao());
    });
  }
  private static void lifecycle(GameTestHelper h) {
    var f=fixture(h,false);var c=f.chao();var active=new boolean[1];var cycles=new int[1];var duration=new int[1];
    var began=new long[1];var ended=new long[]{f.created()};
    h.onEachTick(()-> {
      boolean idle=c.getBehavior()==Behavior.IDLE;
      if(idle&&!active[0]) {
        naturalStart(h,f,c,ended[0]);cycles[0]++;duration[0]=c.getBehaviorDuration();
        began[0]=h.getLevel().getGameTime()-(long)c.getBehaviorElapsed(0);
      }else if(!idle&&active[0]&&c.behaviorDetail().isEmpty()) {
        ended[0]=h.getLevel().getGameTime();
        h.assertTrue(ended[0]-began[0]>=duration[0]&&ended[0]-began[0]<=duration[0]+3
            &&c.behaviorDetail().isEmpty()&&c.nextBehaviorTime()<=ended[0],"Natural expiry clears the gesture without deferring real needs");
        h.assertTrue(Arrays.equals(c.state().points,f.points())&&c.state().happiness==30&&c.state().bond==0
            &&c.state().traitInt("learned.song",0)==0&&c.getMainHandItem().isEmpty(),"Two quiet gesture cycles grant no stats, care, lessons or items");
        identity(h,f,c);
        active[0]=false;
        if(cycles[0]==2){h.assertTrue(c.state().ageTicks>321,"Normal age and care simulation continue throughout natural idle");h.succeed();}
      }
      if(idle)active[0]=true;
    });
  }
  private static void need(GameTestHelper h) {
    var f=fixture(h,true);
    onFirstIdle(h,f,c-> {
      c.state().setEmotion(ChaoEmotion.ANGER,150);c.sync();
      h.runAfterDelay(75,()-> {
        h.assertTrue(c.getBehavior()==Behavior.TANTRUM&&c.behaviorDetail().isEmpty()&&c.state().emotion(ChaoEmotion.ANGER)>=140,
            "Priority-two real need replaces optional idle without a premature emotion reward");identity(h,f,c);h.succeed();
      });
    });
  }
  private static void movement(GameTestHelper h) {
    var f=fixture(h,false);
    onFirstIdle(h,f,c-> {
      Vec3 before=c.position();var moving=new AtomicBoolean(true);var started=new AtomicBoolean();
      // A deterministic fixture movement goal owns the same flags and priority as ordinary strolling.
      c.goalSelector.addGoal(6,new Goal() {
        {setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP));}
        public boolean canUse(){return moving.get();}
        public boolean canContinueToUse(){return moving.get();}
        public void start(){started.set(c.getNavigation().moveTo(before.x+4,before.y,before.z,.8));}
        public void stop(){c.getNavigation().stop();}
      });
      h.runAfterDelay(20,()-> {
        h.assertTrue(started.get()&&c.getBehavior()!=Behavior.IDLE&&c.behaviorDetail().isEmpty()&&c.distanceToSqr(before)>.01,
            "Higher-priority movement preempts natural idle and actually navigates");moving.set(false);identity(h,f,c);
      });
      h.runAfterDelay(50,()-> {h.assertTrue(c.getBehavior()!=Behavior.IDLE,"Interrupted idle respects its new quiet gap");h.succeed();});
    });
  }
  private static void toy(GameTestHelper h) {
    var f=fixture(h,false);var at=new BlockPos[1];var selected=new AtomicBoolean();var played=new AtomicBoolean();
    h.onEachTick(()-> {
      var c=f.chao();if(at[0]==null||played.get()||c.toyPlayingTicks()==0)return;
      h.assertTrue(selected.get()&&c.getBehavior()!=Behavior.IDLE&&c.behaviorDetail().isEmpty()&&c.toyPlayingKind().equals("chao_rattle")
          &&h.getLevel().getBlockState(at[0]).getValue(ChaoToyBlock.PLAYING),"Production toy goal preempts idle and starts its real placed source");
      h.assertTrue(c.getMainHandItem().isEmpty()&&c.getHeldChaoItem().isEmpty(),"Toy takeover creates no real hand item");
      played.set(true);identity(h,f,c);h.succeed();
    });
    onFirstIdle(h,f,c-> {
      h.assertTrue(ToyContent.mayPlay(c),"Natural IDLE does not block ordinary toy eligibility");
      at[0]=c.blockPosition().east();h.getLevel().setBlock(at[0],ToyContent.block("chao_rattle").defaultBlockState(),3);
      c.state().setEmotion(ChaoEmotion.BOREDOM,10000);
      // Fixture adapter controls only the willingness draw at its call site; all selection and contact code is the real PlayGoal.
      int interest=ChaoPreferences.playChanceDenominator(c.state());long seed=0;
      for(;seed<1000;seed++){var random=RandomSource.create(seed);random.nextInt(61);if(random.nextInt(interest)==0)break;}
      long willingnessSeed=seed,naturalEnd=h.getLevel().getGameTime()+c.getBehaviorTicks();
      var play=new ToyContent.PlayGoal(c);
      c.goalSelector.addGoal(4,new Goal() {
        {setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP));}
        public boolean canUse(){c.getRandom().setSeed(willingnessSeed);return play.canUse();}
        public boolean canContinueToUse(){return play.canContinueToUse();}
        public void start(){
          h.assertTrue(h.getLevel().getGameTime()<naturalEnd,"Real toy selection preempts a still-unfinished idle");selected.set(true);play.start();
          h.runAfterDelay(240,()->h.assertTrue(played.get(),"Selected toy reaches its real contact session within the production approach window: behavior="+c.getBehavior()
              +" detail="+c.behaviorDetail()+" kind="+c.toyPlayingKind()+" remaining="+c.toyPlayingTicks()+" pos="+c.position()
              +" velocity="+c.getDeltaMovement()+" ground="+c.onGround()+" navDone="+c.getNavigation().isDone()
              +" mayPlay="+ToyContent.mayPlay(c)+" source="+h.getLevel().getBlockState(at[0])+" reservation="+c.getPersistentData()));
        }
        public void tick(){play.tick();}
        public void stop(){play.stop();}
        public boolean requiresUpdateEveryTick(){return true;}
      });
      h.runAfterDelay(8,()->h.assertTrue(selected.get()&&c.getBehavior()!=Behavior.IDLE&&c.behaviorDetail().isEmpty(),
          "Higher-priority toy selection promptly takes ownership from the still-unfinished natural idle"));
    });
  }
  private static void sleep(GameTestHelper h) {
    var f=fixture(h,false);onFirstIdle(h,f,c-> {
      c.state().setEmotion(ChaoEmotion.SLEEPINESS,9000);c.sync();
      h.runAfterDelay(4,()-> {h.assertTrue(c.getBehavior()!=Behavior.IDLE&&c.behaviorDetail().isEmpty()&&c.state().sleeping&&c.isResting(),
          "Real sleep need interrupts optional idle and enters the normal resting state");identity(h,f,c);h.succeed();});
    });
  }
  private static void item(GameTestHelper h) {
    var f=fixture(h,false);onFirstIdle(h,f,c-> {
      var stack=new ItemStack(Items.STONE,3);f.keeper().setItemInHand(InteractionHand.MAIN_HAND,stack);
      h.assertTrue(c.offerItem(f.keeper(),InteractionHand.MAIN_HAND).consumesAction()&&stack.getCount()==2
          &&c.getMainHandItem().is(Items.STONE)&&c.getMainHandItem().getCount()==1&&c.isHandlingItem()
          &&c.getBehavior()!=Behavior.IDLE&&c.behaviorDetail().isEmpty(),"Actual offer transfers exactly one item and immediately replaces optional idle");
      h.runAfterDelay(4,()-> {h.assertTrue(c.isHandlingItem()&&c.getBehavior()!=Behavior.IDLE,"Real consideration owns the subsequent server ticks");identity(h,f,c);h.succeed();});
    });
  }
  private static void pet(GameTestHelper h) {
    var f=fixture(h,false);onFirstIdle(h,f,c-> {
      h.assertTrue(c.mobInteract(f.keeper(),InteractionHand.MAIN_HAND).consumesAction()&&c.getAffectionTicks()>0
          &&c.getBehavior()!=Behavior.IDLE&&c.behaviorDetail().isEmpty(),"Real keeper petting immediately replaces natural idle with affection");
      h.runAfterDelay(4,()-> {h.assertTrue(c.getAffectionTicks()>0&&c.getBehavior()!=Behavior.IDLE,"Pet reaction keeps its production clock");identity(h,f,c);h.succeed();});
    });
  }
  private static void damage(GameTestHelper h) {
    var f=fixture(h,false);onFirstIdle(h,f,c-> {
      c.setInvulnerable(false);h.assertTrue(c.hurt(h.getLevel().damageSources().playerAttack(f.keeper()),1),"Actual player damage is accepted");
      h.assertTrue(c.getBehavior()==Behavior.COWER&&c.behaviorDetail().isEmpty()&&c.getBehaviorTicks()==60,
          "Damage immediately replaces idle with the existing recovery behavior");
      h.runAfterDelay(5,()-> {h.assertTrue(c.getBehavior()==Behavior.COWER&&c.getBehaviorTicks()<60&&c.getBehaviorTicks()>50,
          "Recovery advances independently of the interrupted idle clock");identity(h,f,c);h.succeed();});
    });
  }
  private static void reload(GameTestHelper h) {
    var f=fixture(h,false);var loadedCopy=new ChaoEntity[1];var loadedAt=new long[1];var resumed=new AtomicBoolean();
    h.onEachTick(()-> {
      var copy=loadedCopy[0];if(copy==null||resumed.get()||copy.getBehavior()!=Behavior.IDLE)return;
      resumed.set(true);naturalStart(h,f,copy,loadedAt[0]);identity(h,f,copy);h.succeed();
    });
    onFirstIdle(h,f,c-> {
      var tag=new CompoundTag();c.saveWithoutId(tag);String exact=c.state().encode();
      h.assertTrue(tag.getInt("Behavior")==Behavior.IDLE.ordinal()&&ChaoIdleRules078.valid(tag.getString("BehaviorDetail")),
          "Actual save captures the naturally selected transient idle");
      c.discard();var copy=ChaoGarden.CHAO.get().create(h.getLevel());h.assertTrue(copy!=null,"Production reload Chao creates");copy.load(tag);
      h.assertTrue(copy.state().encode().equals(exact)&&!copy.isBehaving()&&copy.behaviorDetail().isEmpty()&&!copy.isNoAi(),
          "Actual NBT load cancels transient idle while retaining exact saved care and phenotype without reroll");
      h.getLevel().addFreshEntity(copy);controlledGoals(copy,false);cleanup(h,copy::discard);loadedAt[0]=h.getLevel().getGameTime();loadedCopy[0]=copy;
      h.runAfterDelay(380,()->h.assertTrue(!copy.isBehaving()&&copy.behaviorDetail().isEmpty(),"Reload cannot resume a half-finished idle or skip its new quiet gap"));
    });
  }
  private static Observer observer(GameTestHelper h,ChaoEntity source,ServerPlayer viewer) {
    source.sync();var state=new String[]{null};var replica=ChaoGarden.CHAO.get().create(h.getLevel());var packets=new int[1];
    var tracker=new ServerEntity(h.getLevel(),source,1,true,packet->{});
    tracker.sendPairingData(viewer,new PacketAndPayloadAcceptor<>(packet-> {
      if(!(packet instanceof ClientboundSetEntityDataPacket metadata))return;
      var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess());
      try {
        ClientboundSetEntityDataPacket.STREAM_CODEC.encode(buffer,metadata);var decoded=ClientboundSetEntityDataPacket.STREAM_CODEC.decode(buffer);
        h.assertTrue(decoded.id()==source.getId()&&!buffer.isReadable(),"Actual pairing metadata packet completes its registry-aware wire roundtrip");
        replica.getEntityData().assignValues(decoded.packedItems());packets[0]++;
        for(var value:decoded.packedItems())if(value.value() instanceof String encoded&&encoded.equals(source.state().encode()))state[0]=encoded;
      }finally {buffer.release();}
    }));
    h.assertTrue(packets[0]==1&&state[0]!=null,"Joining observer receives the authoritative actual STATE metadata");
    var result=new Observer(ChaoState.decode(state[0]),replica.getBehavior(),replica.behaviorDetail(),replica.getBehaviorDuration(),replica.getBehaviorTicks(),replica.getBehaviorElapsed(0));
    replica.discard();return result;
  }
  private static void observers(GameTestHelper h) {
    var f=fixture(h,false);onFirstIdle(h,f,c-> {
      var early=observer(h,c,f.keeper());var late=new Observer[1];String detail=c.behaviorDetail();int duration=c.getBehaviorDuration();
      h.assertTrue(early.behavior()==Behavior.IDLE&&early.detail().equals(detail)&&early.duration()==duration,"Early observer receives natural idle detail and its production deadline");
      h.runAfterDelay(20,()-> {
        late[0]=observer(h,c,f.keeper());h.assertTrue(late[0].behavior()==Behavior.IDLE&&late[0].detail().equals(detail)&&late[0].duration()==duration
            &&late[0].remaining()<early.remaining()&&late[0].elapsed()>early.elapsed(),"Late observer receives advancing idle phase without restarting the gesture");
      });
      h.runAfterDelay(35,()-> {
        var joined=observer(h,c,f.keeper());h.assertTrue(joined.behavior()==Behavior.IDLE&&joined.detail().equals(detail)&&joined.remaining()<late[0].remaining()
            &&joined.state().dnaSummary().equals(f.genes())&&joined.state().baselineEyes()==1&&joined.state().baselineMouth()==4,
            "Rejoining observer receives the same gesture and fixed phenotype with a later phase");
      });
      h.runAfterDelay(duration+5,()-> {
        var ended=observer(h,c,f.keeper());h.assertTrue(ended.behavior()==Behavior.NONE&&ended.detail().isEmpty()&&ended.remaining()==0,
            "Natural expiry clears authoritative behavior metadata for later observers");identity(h,f,c);h.succeed();
      });
    });
  }
}
