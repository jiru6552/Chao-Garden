package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.ChaoSounds;
import com.chaogarden.core.*;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.interaction.ChaoBehaviorGoal;
import com.chaogarden.toys.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.gametest.*;

/** Native goal/command/cue checks for the existing recoverable guitar; no inventory prop copies. */
@GameTestHolder("chao_guitar45") @PrefixGameTestTemplate(false)
@EventBusSubscriber(modid=ChaoGarden.MODID)
public final class GuitarGameTests045 {
  private static final Map<ChaoEntity,List<Long>> CUES=new WeakHashMap<>();
  @SubscribeEvent public static void sound(PlayLevelSoundEvent.AtPosition event){
    if(event.getLevel().isClientSide||event.getSound()==null)return;
    boolean guitar=event.getSound().value()==ChaoSounds.GUITAR.get();
    boolean oldPulse=event.getSound().value()==net.minecraft.sounds.SoundEvents.NOTE_BLOCK_GUITAR.value();
    if(!guitar&&!oldPulse)return;
    CUES.forEach((c,list)->{
      if(c.level()==event.getLevel()&&event.getPosition().distanceToSqr(c.position().add(0,c.getBbHeight()*.55,0))<1e-8)
        list.add(guitar?c.level().getGameTime():Long.MIN_VALUE);
    });
  }
  private static void cleanup(GameTestHelper h,Runnable run){
    h.testInfo.addListener(new GameTestListener(){
      public void testStructureLoaded(GameTestInfo i){}
      public void testPassed(GameTestInfo i,GameTestRunner r){run.run();}
      public void testFailed(GameTestInfo i,GameTestRunner r){run.run();}
      public void testAddedForRerun(GameTestInfo a,GameTestInfo b,GameTestRunner r){run.run();}
    });
  }
  private static void remove(ServerPlayer p){if(p!=null&&p.server.getPlayerList().getPlayer(p.getUUID())==p)p.server.getPlayerList().remove(p);}
  private static BlockPos floor(GameTestHelper h,Direction facing){
    var at=RattleGameTests02520.floor(h);
    h.getLevel().setBlock(at,ToyContent.block(GuitarPlayRules.KIND).defaultBlockState().setValue(ChaoToyBlock.FACING,facing),3);return at;
  }
  private static ChaoEntity actor(GameTestHelper h,BlockPos at,boolean adult,boolean hands){
    var c=RattleGameTests02520.actor(h,at,adult,ChaoState.NORMAL,hands);
    // Match an actual persisted creature: assigning replacement parts directly in the fixture
    // otherwise leaves the derived mask stale until the first decode.
    c.setState(ChaoState.decode(c.state().encode()));
    c.state().setTraitInt("learned.guitar",1);c.sync();CUES.put(c,new ArrayList<>());return c;
  }
  private static int drops(GameTestHelper h,BlockPos at){return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(at).inflate(4),e->e.getItem().is(ChaoGarden.item(GuitarPlayRules.KIND))).stream().mapToInt(e->e.getItem().getCount()).sum();}
  private static long start(ChaoEntity c){return c.getPersistentData().getLong("ChaoToyUntil")-160;}
  private static List<Long> expected(long start,int duration){var result=new ArrayList<Long>();for(int t=0;t<duration;t++)if(GuitarPlayRules.soundDue(t))result.add(start+t);return result;}
  @GameTestGenerator public static Collection<TestFunction> cases(){
    var tests=new ArrayList<TestFunction>();int[] n={0};
    java.util.function.BiConsumer<String,java.util.function.Consumer<GameTestHelper>> add=(name,body)->tests.add(new TestFunction("guitar045_"+(n[0]++/8),name,"chao_guitar45:activity_test",1800,0,true,body));
    for(boolean adult:new boolean[]{false,true}){
      for(Direction facing:Direction.Plane.HORIZONTAL)add.accept("guitar_complete_"+adult+"_"+facing,h->placed(h,adult,facing,"complete"));
      for(String mode:List.of("hurt","move","remove","reload"))add.accept("guitar_placed_"+adult+"_"+mode,h->placed(h,adult,Direction.NORTH,mode));
      add.accept("guitar_unlearned_"+adult,h->unlearned(h,adult));
      for(String mode:List.of("complete","stay","hurt","reload"))add.accept("guitar_learned_"+adult+"_"+mode,h->learned(h,adult,mode));
      for(boolean loop:new boolean[]{false,true})add.accept("guitar_preview_"+adult+"_"+loop,h->preview(h,adult,loop));
    }
    for(String specimen:List.of("animal_rabbit_baby","animal_scorpion_adult","chaos_2","tails_adult"))add.accept("guitar_variant_"+specimen,h->variant(h,specimen));
    for(String mode:List.of("stop","hurt","pet","reload","logout","range"))add.accept("guitar_preview_interrupt_"+mode,h->previewInterrupt(h,mode));
    return tests;
  }
  private static void placed(GameTestHelper h,boolean adult,Direction facing,String mode){
    var at=floor(h,facing);var l=h.getLevel();var c=actor(h,at,adult,adult);
    c.moveTo(Vec3.atBottomCenterOf(at.relative(facing.getOpposite(),2)));
    long[] began={-1},changed={-1};int[] cueCount={0};float[] happiness={20};
    var restored=new ChaoEntity[1];
    cleanup(h,()->{CUES.remove(c);c.discard();if(restored[0]!=null)restored[0].discard();});
    h.onEachTick(()->{
      if(ToyContent.isPlaying(c)&&began[0]<0)began[0]=start(c);
      if(began[0]<0||changed[0]>=0)return;
      if(ToyContent.isPlaying(c)){
        h.assertTrue(GuitarPlayRules.KIND.equals(c.toyPlayingKind())&&at.equals(c.toyTarget()),"one real guitar remains reserved");
        var source=l.getBlockState(at);
        h.assertTrue(source.is(ToyContent.block(GuitarPlayRules.KIND))&&source.getRenderShape()==RenderShape.INVISIBLE,"held presentation hides the recoverable source");
        h.assertTrue(c.getHeldChaoItem().isEmpty()&&c.getMainHandItem().isEmpty(),"presentation never creates a real held guitar");
        Direction side=Direction.getNearest(c.getX()-at.getX()-.5,0,c.getZ()-at.getZ()-.5);
        h.assertTrue(c.distanceToSqr(ToyContent.stationaryAnchor(c,at,at.relative(side)))<.007&&l.noCollision(c,c.getBoundingBox()),"stationary approach/contact remains collision-safe");
      }
      if(!mode.equals("complete")&&l.getGameTime()-began[0]>=60){
        changed[0]=l.getGameTime();cueCount[0]=CUES.get(c).size();
        switch(mode){
          case "hurt"->{c.setInvulnerable(false);h.assertTrue(c.hurt(l.damageSources().generic(),1),"actual damage accepted");happiness[0]=c.state().happiness;}
          case "move"->c.setPos(c.getX()+3,c.getY(),c.getZ());
          case "remove"->l.destroyBlock(at,true);
          case "reload"->{
            var before=c.state().encode();var saved=new CompoundTag();c.saveWithoutId(saved);c.discard();
            restored[0]=ChaoGarden.CHAO.get().create(l);restored[0].load(saved);
            h.assertTrue(restored[0].state().encode().equals(before),"native reload preserves exact canonical care/genetics/age/lesson state");
            restored[0].goalSelector.removeAllGoals(g->true);restored[0].targetSelector.removeAllGoals(g->true);
            restored[0].goalSelector.addGoal(4,new ToyContent.PlayGoal(restored[0]));l.addFreshEntity(restored[0]);
          }
        }
      }
    });
    h.succeedWhen(()->{
      h.assertTrue(began[0]>=0&&l.getGameTime()-began[0]>=180,"observe entire guitar interaction");
      var live=restored[0]==null?c:restored[0];
      h.assertTrue(!ToyContent.isPlaying(live)&&live.toyTarget()==null,"completion/interruption clears the synced clock and reservation");
      h.assertTrue(live.state().happiness==(mode.equals("complete")?24:happiness[0]),"one completion reward only, with ordinary damage effects retained; actual="+live.state().happiness+" mode="+mode);
      if(mode.equals("complete"))h.assertTrue(CUES.get(c).equals(expected(began[0],160)),"all 16 real strums follow the production clock exactly: "+CUES.get(c));
      else h.assertTrue(changed[0]>=0&&CUES.get(c).size()==cueCount[0],"no guitar cue after interruption");
      if(mode.equals("remove"))h.assertTrue(l.getBlockState(at).isAir()&&drops(h,at)==1,"destroying a reserved guitar drops one real item");
      else h.assertTrue(l.getBlockState(at).getRenderShape()==RenderShape.MODEL&&!l.getBlockState(at).getValue(ChaoToyBlock.PLAYING)&&drops(h,at)==0,"one visible idle guitar returns without duplicate drops");
      h.assertTrue(live.getMainHandItem().isEmpty(),"no cloned item remains");
      if(mode.equals("complete"))h.assertTrue(live.getPersistentData().getLong("ChaoToyNextPlay")>l.getGameTime(),"existing rest cooldown retained");
    });
  }
  private static void unlearned(GameTestHelper h,boolean adult){
    var at=floor(h,Direction.NORTH);var c=actor(h,at,adult,false);c.state().setTraitInt("learned.guitar",0);c.sync();
    cleanup(h,()->{CUES.remove(c);c.discard();});
    h.onEachTick(()->h.assertTrue(!ToyContent.isPlaying(c)&&c.toyTarget()==null,"unlearned Chao must not reserve a placed guitar"));
    h.runAfterDelay(250,()->{h.assertTrue(CUES.get(c).isEmpty()&&c.state().happiness==20,"lesson gating has no cues/reward");h.succeed();});
  }
  private static void variant(GameTestHelper h,String specimen){
    var at=floor(h,Direction.SOUTH);var state=ToyVariantCases02522.all().stream().filter(s->s.name().equals(specimen)).findFirst().orElseThrow().state();
    state.setTraitInt("learned.guitar",1);var c=actor(h,at,state.adult,false);c.setState(state);var began=new long[]{-1};
    cleanup(h,()->{CUES.remove(c);c.discard();});
    h.onEachTick(()->{if(ToyContent.isPlaying(c)){if(began[0]<0)began[0]=start(c);h.assertTrue(h.getLevel().getBlockState(at).getRenderShape()==RenderShape.INVISIBLE,"variant uses one hand-held presentation");}});
    h.succeedWhen(()->{h.assertTrue(began[0]>=0&&h.getLevel().getGameTime()-began[0]>=180,"observe whole variant session");h.assertTrue(!ToyContent.isPlaying(c)&&c.state().happiness==24,"variant completes once");h.assertTrue(CUES.get(c).equals(expected(began[0],160)),"variant receives the same strum clock");h.assertTrue(c.state().character.equals(state.character)&&c.state().immortal==state.immortal,"variant identity preserved");});
  }
  private static void learned(GameTestHelper h,boolean adult,String mode){
    var at=floor(h,Direction.NORTH);h.getLevel().setBlock(at,Blocks.AIR.defaultBlockState(),3);
    var current=new ChaoEntity[]{actor(h,at,adult,false)};InstrumentPerformanceGameTests02532.prepare(current[0],"guitar");
    long[] began={-1};boolean[] changed={false};int[] atStop={-1};var original=current[0];
    cleanup(h,()->{CUES.remove(original);CUES.remove(current[0]);current[0].discard();});
    h.onEachTick(()->{
      var c=current[0];
      if(c.getBehavior()==ChaoEntity.Behavior.INSTRUMENT&&began[0]<0){began[0]=h.getLevel().getGameTime();h.assertTrue(c.behaviorDetail().equals("guitar"),"actual behavior goal selects learned guitar");}
      if(began[0]<0)return;
      if(c.getBehavior()==ChaoEntity.Behavior.INSTRUMENT){
        h.assertTrue(c.getMainHandItem().isEmpty()&&c.getHeldChaoItem().isEmpty(),"learned guitar creates no inventory item");
        InstrumentPerformance.tick(c);InstrumentPerformance.tick(c);
      }
      if(!changed[0]&&!mode.equals("complete")&&c.getBehaviorElapsed(0)>=64){
        changed[0]=true;atStop[0]=CUES.get(c).size();
        switch(mode){
          case "stay"->c.setStaying(true);
          case "hurt"->{c.setInvulnerable(false);c.hurt(h.getLevel().damageSources().generic(),1);}
          case "reload"->{
            var before=c.getBehaviorElapsed(0);var count=c.getBehaviorTicks();var events=CUES.remove(c);var saved=new CompoundTag();c.saveWithoutId(saved);c.discard();
            var copy=ChaoGarden.CHAO.get().create(h.getLevel());copy.load(saved);copy.goalSelector.removeAllGoals(g->true);copy.targetSelector.removeAllGoals(g->true);copy.goalSelector.addGoal(2,new ChaoBehaviorGoal(copy));h.getLevel().addFreshEntity(copy);current[0]=copy;CUES.put(copy,events);
            h.assertTrue(copy.getBehaviorElapsed(0)==before&&copy.getBehaviorTicks()==count,"learned reload preserves the production phase");InstrumentPerformance.tick(copy);h.assertTrue(events.size()==atStop[0],"reload cannot replay a strum");
          }
        }
      }
    });
    h.succeedWhen(()->{
      h.assertTrue(began[0]>=0&&h.getLevel().getGameTime()-began[0]>=185,"observe learned performance and exit");var c=current[0];
      h.assertTrue(c.getBehavior()!=ChaoEntity.Behavior.INSTRUMENT,"learned behavior completes/interrupts");
      int expected=mode.equals("complete")||mode.equals("reload")?16:atStop[0];
      h.assertTrue(CUES.get(c).size()==expected&&new HashSet<>(CUES.get(c)).size()==expected,"one cue per strum; no old generic pulse or cancellation replay: "+CUES.get(c));
      h.assertTrue(c.state().traitInt("learned.guitar",0)==1,"playing does not reteach the lesson");
    });
  }
  private static int command(ServerPlayer p,String text){try{return p.server.getCommands().getDispatcher().execute(text,p.createCommandSourceStack().withPermission(2));}catch(Exception e){throw new RuntimeException(e);}}
  private static void preview(GameTestHelper h,boolean adult,boolean loop){
    var at=floor(h,Direction.NORTH);h.getLevel().setBlock(at,Blocks.AIR.defaultBlockState(),3);var c=actor(h,at,adult,false);c.goalSelector.removeAllGoals(g->true);c.setOnGround(true);c.setNoAi(adult);
    c.state().setTraitInt("learned.guitar",0);String before=c.state().encode();boolean oldAi=c.isNoAi();c.getPersistentData().putLong("ChaoToyNextPlay",123456789);
    var p=TestConnections.makeSurvivalServerPlayer(h);p.teleportTo(c.getX()+2,c.getY(),c.getZ());
    cleanup(h,()->{ToyPreview.stop(c);CUES.remove(c);c.discard();remove(p);});
    h.assertTrue(command(p,"chao testtoy guitar "+(loop?"loop":"once"))==1,"native guitar command needs no learned lesson or inventory item");long began=h.getLevel().getGameTime();
    h.onEachTick(()->{if(ToyPreview.active(c)){ToyPreview.tick(c);ToyPreview.tick(c);h.assertTrue(c.state().encode().equals(before),"preview freezes care, genetics, age and lessons");h.assertTrue(c.getMainHandItem().isEmpty(),"preview creates no real prop");}});
    h.runAtTickTime(loop?325:160,()->{
      if(loop){h.assertTrue(ToyPreview.active(c),"two-cycle loop still active");h.assertTrue(command(p,"chao testtoy guitar stop")==1,"native named stop works");}else ToyPreview.tick(c);
      h.assertTrue(!ToyPreview.active(c)&&c.isNoAi()==oldAi&&c.toyPlayingTicks()==0,"preview cleans up and restores prior AI");
      h.assertTrue(c.state().encode().equals(before)&&c.getPersistentData().getLong("ChaoToyNextPlay")==123456789,"no saved changes or cooldown loss");
      var expected=new ArrayList<Long>();expected.addAll(expected(began,160));if(loop)expected.addAll(expected(began+160,160));h.assertTrue(CUES.get(c).equals(expected),"once/loop use exactly the production strum clock");h.succeed();
    });
  }
  private static void previewInterrupt(GameTestHelper h,String mode){
    var at=floor(h,Direction.NORTH);h.getLevel().setBlock(at,Blocks.AIR.defaultBlockState(),3);var c=actor(h,at,false,false);c.goalSelector.removeAllGoals(g->true);c.setOnGround(true);
    var p=TestConnections.makeSurvivalServerPlayer(h);p.teleportTo(c.getX()+2,c.getY(),c.getZ());c.owner=p.getUUID();
    cleanup(h,()->{ToyPreview.stop(c);CUES.remove(c);c.discard();remove(p);});h.assertTrue(command(p,"chao testtoy guitar loop")==1,"native loop starts");
    h.runAtTickTime(55,()->{
      int count=CUES.get(c).size();
      switch(mode){
        case "stop"->h.assertTrue(command(p,"chao testtoy stop")==1,"native global stop works");
        case "hurt"->{c.setInvulnerable(false);c.hurt(h.getLevel().damageSources().generic(),1);}
        case "pet"->c.mobInteract(p,InteractionHand.MAIN_HAND);
        case "reload"->{var saved=new CompoundTag();c.saveWithoutId(saved);c.discard();var copy=ChaoGarden.CHAO.get().create(h.getLevel());copy.load(saved);h.getLevel().addFreshEntity(copy);h.assertTrue(!copy.isNoAi()&&copy.toyPlayingTicks()==0,"reload cancels preview and restores AI");copy.discard();}
        case "logout"->remove(p);
        case "range"->p.teleportTo(c.getX()+70,c.getY(),c.getZ());
      }
      ToyPreview.tick(c);h.assertTrue(!ToyPreview.active(c)&&!c.isNoAi()&&c.toyPlayingTicks()==0,"interruption clears preview/AI state");
      h.runAfterDelay(20,()->{h.assertTrue(CUES.get(c).size()==count,"no strums after preview cancellation");h.succeed();});
    });
  }
  @GameTest(template="empty",timeoutTicks=1400,batch="guitar045_school")
  public static void enrolledBabyAndAdultPracticePauseCompleteAndReturnOneRewardEach(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);p.getInventory().clearContent();
    var school=p.server.getLevel(com.chaogarden.kindergarten.KindergartenContent.DIMENSION);
    h.assertTrue(com.chaogarden.hub.HubContent.ensure(school),"actual school builds");InstrumentSchoolFixture02525.visit(p);
    var actors=new ArrayList<ChaoEntity>();long[] start={-1};boolean[] paused={false},resumed={false};int[] silence={0};
    cleanup(h,()->{
      var roster=com.chaogarden.kindergarten.KindergartenRoster.get(p.server);
      for(var c:actors){roster.students.remove(c.getUUID());CUES.remove(c);c.discard();}
      roster.setDirty();remove(p);
    });
    h.onEachTick(()->{
      if(start[0]<0)return;long elapsed=school.getGameTime()-start[0];
      for(var c:actors)if(InstrumentPerformance.active(c,GuitarPlayRules.KIND))h.assertTrue(c.getHeldChaoItem().isEmpty()&&c.getMainHandItem().isEmpty(),"class practice does not mint an inventory prop");
      var c=actors.get(1);
      if(elapsed>=70&&!paused[0]){paused[0]=true;c.state().hunger=10;com.chaogarden.care.ChaoCareServices.tickLearning(c);silence[0]=CUES.get(c).size();}
      if(paused[0]&&!resumed[0]){
        h.assertTrue(!InstrumentPerformance.active(c,GuitarPlayRules.KIND)&&CUES.get(c).size()==silence[0],"paused guitar lesson is silent with no active prop");
        if(elapsed>=90){resumed[0]=true;c.state().hunger=83;}
      }
    });
    h.startSequence().thenWaitUntil(()->{
      com.chaogarden.kindergarten.KindergartenContent.ensureServices(school);
      h.assertTrue(school.isPositionEntityTicking(com.chaogarden.kindergarten.KindergartenContent.Role.TEACHER.anchor),"wait for real classroom chunks to tick");
    }).thenExecute(()->{
      for(int n=0;n<2;n++){var c=InstrumentSchoolFixture02525.enroll(p,n,"guitar",n==1,335);actors.add(c);CUES.put(c,new ArrayList<>());}start[0]=school.getGameTime();
    }).thenWaitUntil(()->{
      h.assertTrue(school.getGameTime()-start[0]>400,"observe two phrase cycles, pause and completion");
      for(int n=0;n<2;n++){
        var c=actors.get(n);var cues=CUES.get(c);
        h.assertTrue(!com.chaogarden.care.ChaoCareServices.hasLesson(c)&&c.state().traitInt("learned.guitar",0)==1,"actual guitar lesson completes exactly once");
        h.assertTrue(n==0?cues.size()==32:cues.size()>0&&cues.size()<32,"two-cycle cue count honors real lesson pause: "+cues);
        h.assertTrue(new HashSet<>(cues).size()==cues.size(),"classroom produces no duplicate strums");
        h.assertTrue(c.state().happiness==84&&c.state().adult==(n==1),"practice preserves ordinary classroom care and age");
        h.assertTrue(com.chaogarden.kindergarten.KindergartenContent.isBoarded(c),"same student body remains owned by daycare");
      }
    }).thenExecute(()->{
      for(var c:actors)h.assertTrue(com.chaogarden.kindergarten.KindergartenContent.retrieve(p,c.getUUID()),"actual teacher returns the completed student");
      int rewards=0;for(int n=0;n<36;n++){var item=p.getInventory().getItem(n);if(item.is(ChaoGarden.item(GuitarPlayRules.KIND)))rewards+=item.getCount();}
      h.assertTrue(rewards==2,"one real guitar reward per pupil, not per animation cycle");
    }).thenSucceed();
  }

}
