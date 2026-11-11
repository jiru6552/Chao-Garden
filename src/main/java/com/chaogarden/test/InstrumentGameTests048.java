package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.*;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.interaction.ChaoBehaviorGoal;
import com.chaogarden.toys.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
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

/** Real goal, saved-body, classroom and operator-command sessions for the two existing toys. */
@GameTestHolder("chao_instruments48") @PrefixGameTestTemplate(false)
@EventBusSubscriber(modid=ChaoGarden.MODID)
public final class InstrumentGameTests048 {
  private static final String PIANO="chao_piano", ACCORDION="chao_accordion";
  private record Heard(String kind,List<Long> ticks) {}
  private static final Map<ChaoEntity,Heard> CUES=new WeakHashMap<>();

  @SubscribeEvent public static void sound(PlayLevelSoundEvent.AtPosition event){
    if(event.getLevel().isClientSide||event.getSound()==null)return;
    String id=BuiltInRegistries.SOUND_EVENT.getKey(event.getSound().value()).toString();
    if(!id.equals("chao_garden:toy.accordion")&&!id.equals("minecraft:block.note_block.harp")&&!id.equals("minecraft:block.note_block.flute"))return;
    CUES.forEach((c,heard)->{
      if(c.level()!=event.getLevel())return;
      if(heard.kind.equals(PIANO)){
        var at=c.toyTarget();
        if(at!=null&&id.equals("minecraft:block.note_block.harp")&&event.getPosition().distanceToSqr(new Vec3(at.getX()+.5,at.getY()+.21,at.getZ()+.5))<.0004)
          heard.ticks.add(c.level().getGameTime());
      }else if(event.getPosition().distanceToSqr(c.position().add(0,c.getBbHeight()*.55,0))<1e-8){
        heard.ticks.add(id.equals("chao_garden:toy.accordion")?c.level().getGameTime():Long.MIN_VALUE);
      }
    });
  }
  private static void cleanup(GameTestHelper h,Runnable action){
    h.testInfo.addListener(new GameTestListener(){
      public void testStructureLoaded(GameTestInfo i){}
      public void testPassed(GameTestInfo i,GameTestRunner r){action.run();}
      public void testFailed(GameTestInfo i,GameTestRunner r){action.run();}
      public void testAddedForRerun(GameTestInfo a,GameTestInfo b,GameTestRunner r){action.run();}
    });
  }
  private static void remove(ServerPlayer p){if(p!=null&&p.server.getPlayerList().getPlayer(p.getUUID())==p)p.server.getPlayerList().remove(p);}
  private static BlockPos floor(GameTestHelper h,String kind,Direction facing){
    var at=RattleGameTests02520.floor(h);
    h.getLevel().setBlock(at,ToyContent.block(kind).defaultBlockState().setValue(ChaoToyBlock.FACING,facing),3);return at;
  }
  private static ChaoEntity actor(GameTestHelper h,BlockPos at,String kind,boolean adult,boolean hands){
    var c=RattleGameTests02520.actor(h,at,adult,ChaoState.NORMAL,hands);
    c.setState(ChaoState.decode(c.state().encode()));
    // Walking babies and replacement adult hands exercise the production contact fit.
    if(!adult)c.state().points[ChaoState.RUN]=LocomotionRules.WALK_POINTS;
    if(kind.equals(ACCORDION))c.state().setTraitInt("learned.accordion",1);
    c.sync();CUES.put(c,new Heard(kind,new ArrayList<>()));return c;
  }
  private static List<Long> heard(ChaoEntity c){return CUES.get(c).ticks;}
  private static long start(ChaoEntity c){return c.getPersistentData().getLong("ChaoToyUntil")-160;}
  // Keep the authored note times independent of the production predicate so a
  // duplicated generic pulse or accidental cadence change cannot pass with it.
  private static boolean due(String kind,int phase){return phase>=24&&phase<=(kind.equals(PIANO)?144:136)&&(phase-24)%(kind.equals(PIANO)?8:16)==0;}
  private static List<Long> expected(String kind,long began,int cutoff){
    var result=new ArrayList<Long>();for(int t=0;t<cutoff;t++)if(due(kind,t%160))result.add(began+t);return result;
  }
  private static int drops(GameTestHelper h,BlockPos at,String kind){
    return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(at).inflate(4),e->e.getItem().is(ChaoGarden.item(kind))).stream().mapToInt(e->e.getItem().getCount()).sum();
  }
  private static int command(ServerPlayer p,String text){try{return p.server.getCommands().getDispatcher().execute(text,p.createCommandSourceStack().withPermission(2));}catch(Exception e){throw new RuntimeException(e);}}

  @GameTestGenerator public static Collection<TestFunction> cases(){
    var tests=new ArrayList<TestFunction>();int[] n={0};
    java.util.function.BiConsumer<String,java.util.function.Consumer<GameTestHelper>> add=(name,body)->tests.add(new TestFunction("instruments048_"+(n[0]++/8),name,"chao_instruments48:activity_test",1800,0,true,body));
    for(boolean adult:new boolean[]{false,true}){
      for(Direction facing:Direction.Plane.HORIZONTAL){
        add.accept("piano_complete_"+adult+"_"+facing,h->placed(h,PIANO,adult,facing,"complete"));
        add.accept("piano_max_run_"+adult+"_"+facing,h->placed(h,PIANO,adult,facing,"max_run"));
        add.accept("accordion_complete_"+adult+"_"+facing,h->placed(h,ACCORDION,adult,facing,"complete"));
      }
      add.accept("accordion_unlearned_"+adult,h->unlearned(h,adult));
      for(String mode:List.of("complete","reload"))add.accept("accordion_learned_"+adult+"_"+mode,h->learned(h,adult,mode));
      for(String kind:List.of(PIANO,ACCORDION))for(boolean loop:new boolean[]{false,true})
        add.accept(kind+"_preview_"+adult+"_"+loop,h->preview(h,kind,adult,loop));
    }
    for(Direction facing:Direction.Plane.HORIZONTAL)add.accept("piano_front_blocked_"+facing,h->blocked(h,facing));
    add.accept("piano_close_clearance_all_ages_facings",InstrumentGameTests048::clearance);
    for(String kind:List.of(PIANO,ACCORDION)){
      for(String mode:List.of("hurt","move","remove","reload"))add.accept(kind+"_placed_"+mode,h->placed(h,kind,mode.equals("move")||mode.equals("reload"),Direction.WEST,mode));
      for(String mode:List.of("hurt","pet","reload","logout","range"))add.accept(kind+"_preview_interrupt_"+mode,h->previewInterrupt(h,kind,mode));
    }
    for(String mode:List.of("move","remove"))add.accept("piano_preview_interrupt_"+mode,h->previewInterrupt(h,PIANO,mode));
    add.accept("accordion_preview_global_stop",h->previewInterrupt(h,ACCORDION,"stop"));
    add.accept("accordion_learned_stay",h->learned(h,false,"stay"));
    add.accept("accordion_learned_damage",h->learned(h,true,"hurt"));
    for(String specimen:List.of("animal_rabbit_baby","chaos_2"))for(String kind:List.of(PIANO,ACCORDION))
      add.accept(kind+"_variant_"+specimen,h->variant(h,kind,specimen));
    return tests;
  }

  private static void placed(GameTestHelper h,String kind,boolean adult,Direction facing,String mode){
    var at=floor(h,kind,facing);var l=h.getLevel();var c=actor(h,at,kind,adult,adult);
    if(mode.equals("max_run"))c.state().points[ChaoState.RUN]=9999;
    // Starting behind the cabinet catches side/rear fallback and stair-stepping onto its lid.
    c.moveTo(Vec3.atBottomCenterOf(at.relative(facing.getOpposite(),3)));
    var anchor=kind.equals(PIANO)?ToyContent.keyboardAnchor(c,at):Vec3.ZERO;
    long[] began={-1},changed={-1};int[] countAtStop={-1};float[] happiness={20};var restored=new ChaoEntity[1];
    cleanup(h,()->{ToyPreview.stop(c);CUES.remove(c);c.discard();if(restored[0]!=null)restored[0].discard();});
    h.onEachTick(()->{
      if(mode.equals("max_run")&&c.tickCount>1)h.assertTrue(Math.abs(c.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)-.44)<1e-6,"max-Run test uses the real physical speed cap");
      if(kind.equals(PIANO)&&began[0]<0)h.assertTrue(c.getY()<anchor.y+.15,"piano approach goes round the cabinet without climbing its lid");
      if(ToyContent.isPlaying(c)&&began[0]<0)began[0]=start(c);
      if(began[0]<0||changed[0]>=0)return;
      if(ToyContent.isPlaying(c)){
        h.assertTrue(kind.equals(c.toyPlayingKind())&&at.equals(c.toyTarget()),"actual placed instrument remains reserved");
        h.assertTrue(c.getMainHandItem().isEmpty()&&c.getHeldChaoItem().isEmpty(),"animation creates no real inventory copy");
        h.assertTrue(l.noCollision(c,c.getBoundingBox()),"body remains clear of source collision");
        if(kind.equals(PIANO)){
          h.assertTrue(c.position().distanceToSqr(anchor)<.007,"piano stays at its measured front anchor");
          double front=c.position().subtract(Vec3.atBottomCenterOf(at)).dot(new Vec3(facing.getStepX(),0,facing.getStepZ()));
          h.assertTrue(front<=.56&&Math.abs(front-ToyInteractionRules.keyboardDistance(kind,c.getBbWidth()))<.02,"close piano stance avoids the previous long arm reach");
          if(l.getGameTime()-began[0]>1)h.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(c.getYRot()-facing.getOpposite().toYRot()))<1,"piano faces the keys rather than a side");
        }else{
          h.assertTrue(l.getBlockState(at).getRenderShape()==RenderShape.INVISIBLE,"accordion source is hidden only while its held presentation plays");
          Direction side=Direction.getNearest(c.getX()-at.getX()-.5,0,c.getZ()-at.getZ()-.5);
          h.assertTrue(c.distanceToSqr(ToyContent.stationaryAnchor(c,at,at.relative(side)))<.007,"accordion uses its collision-safe stationary anchor");
        }
      }
      if(!mode.equals("complete")&&!mode.equals("max_run")&&l.getGameTime()-began[0]>=64){
        changed[0]=l.getGameTime();countAtStop[0]=heard(c).size();
        switch(mode){
          case "hurt"->{c.setInvulnerable(false);h.assertTrue(c.hurt(l.damageSources().generic(),1),"real damage accepted");happiness[0]=c.state().happiness;}
          case "move"->c.setPos(c.getX()+3,c.getY(),c.getZ());
          case "remove"->l.destroyBlock(at,true);
          case "reload"->{
            String before=c.state().encode();var tag=new CompoundTag();c.saveWithoutId(tag);c.discard();
            var copy=ChaoGarden.CHAO.get().create(l);copy.load(tag);restored[0]=copy;
            h.assertTrue(copy.state().encode().equals(before),"native reload preserves exact care, genetics, age and lessons");
            copy.goalSelector.removeAllGoals(g->true);copy.targetSelector.removeAllGoals(g->true);copy.goalSelector.addGoal(4,new ToyContent.PlayGoal(copy));l.addFreshEntity(copy);
            h.assertTrue(copy.toyTarget()==null&&copy.toyPlayingTicks()==0,"existing save cannot resume a stale placed reservation");
          }
        }
      }
    });
    h.succeedWhen(()->{
      h.assertTrue(began[0]>=0&&l.getGameTime()-began[0]>=180,"observe whole production interaction; kind="+kind+" pos="+c.position()+" anchor="+anchor+" nav="+c.getNavigation().isDone());
      var live=restored[0]==null?c:restored[0];boolean complete=mode.equals("complete")||mode.equals("max_run");
      h.assertTrue(!ToyContent.isPlaying(live)&&live.toyTarget()==null,"exit clears production clock and ownership");
      h.assertTrue(live.state().happiness==(complete?24:happiness[0]),"completion rewards once; interruption retains only ordinary care effects");
      if(complete){
        h.assertTrue(heard(c).equals(expected(kind,began[0],160)),"actual sounds follow authored production notes, without generic pulses: "+heard(c));
        h.assertTrue(live.getPersistentData().getLong("ChaoToyNextPlay")>l.getGameTime(),"ordinary cooldown survives completion");
      }else h.assertTrue(changed[0]>=0&&heard(c).size()==countAtStop[0],"interruption produces no later note");
      if(mode.equals("remove"))h.assertTrue(l.getBlockState(at).isAir()&&drops(h,at,kind)==1,"breaking source recovers exactly one real instrument");
      else h.assertTrue(l.getBlockState(at).getRenderShape()==RenderShape.MODEL&&!l.getBlockState(at).getValue(ChaoToyBlock.PLAYING)&&drops(h,at,kind)==0,"source returns visible and idle without duplicate items");
      h.assertTrue(live.getMainHandItem().isEmpty(),"exit leaves no held inventory copy");
    });
  }

  private static void blocked(GameTestHelper h,Direction facing){
    var at=floor(h,PIANO,facing);var l=h.getLevel();l.setBlock(at.relative(facing),Blocks.STONE.defaultBlockState(),3);
    var c=actor(h,at,PIANO,true,false);c.moveTo(Vec3.atBottomCenterOf(at.relative(facing.getOpposite(),3)));
    cleanup(h,()->{CUES.remove(c);c.discard();});
    h.onEachTick(()->h.assertTrue(!ToyContent.isPlaying(c)&&c.toyTarget()==null,"blocked keys cannot use a side or the rear"));
    h.runAfterDelay(420,()->{h.assertTrue(heard(c).isEmpty()&&c.state().happiness==20,"unreachable keys grant no cue or reward");h.succeed();});
  }
  private static void clearance(GameTestHelper h){
    var at=floor(h,PIANO,Direction.NORTH);var l=h.getLevel();
    for(boolean adult:new boolean[]{false,true})for(Direction facing:Direction.Plane.HORIZONTAL){
      var state=ToyContent.block(PIANO).defaultBlockState().setValue(ChaoToyBlock.FACING,facing);l.setBlock(at,state,3);
      var c=actor(h,at,PIANO,adult,false);c.setNoAi(true);c.moveTo(ToyContent.keyboardAnchor(c,at));
      h.assertTrue(l.noCollision(c,c.getBoundingBox()),"baby/adult close stance clears real piano collision in "+facing);
      h.assertFalse(l.noCollision(c,c.getBoundingBox().move(-facing.getStepX()*.06,0,-facing.getStepZ()*.06)),"rear cabinet remains solid at close stance in "+facing);
      h.assertTrue(state.getCollisionShape(l,at).bounds().equals(state.getShape(l,at).bounds()),"normal player collision still follows the full piano outline");
      l.setBlock(at.relative(facing),Blocks.STONE.defaultBlockState(),3);
      h.assertTrue(ToyContent.previewKeyboard(c,PIANO)==null&&!ToyContent.holdKeyboard(c,at),"real obstruction rejects approach and preview at the front");
      l.setBlock(at.relative(facing),Blocks.AIR.defaultBlockState(),3);
      h.assertTrue(at.equals(ToyContent.previewKeyboard(c,PIANO)),"clear front restores preview contact");CUES.remove(c);c.discard();
    }
    h.succeed();
  }
  private static void unlearned(GameTestHelper h,boolean adult){
    var at=floor(h,ACCORDION,Direction.NORTH);var c=actor(h,at,ACCORDION,adult,false);c.state().setTraitInt("learned.accordion",0);c.sync();
    cleanup(h,()->{CUES.remove(c);c.discard();});
    h.onEachTick(()->h.assertTrue(!ToyContent.isPlaying(c)&&c.toyTarget()==null,"unlearned accordion cannot reserve the real source"));
    h.runAfterDelay(250,()->{h.assertTrue(heard(c).isEmpty()&&c.state().happiness==20,"lesson gating grants no reward or sounds");h.succeed();});
  }
  private static void variant(GameTestHelper h,String kind,String specimen){
    var at=floor(h,kind,Direction.SOUTH);var s=ToyVariantCases02522.all().stream().filter(v->v.name().equals(specimen)).findFirst().orElseThrow().state();
    s.setTraitInt("learned.accordion",1);if(!s.adult)s.points[ChaoState.RUN]=LocomotionRules.WALK_POINTS;
    String character=s.character;boolean immortal=s.immortal;
    var c=actor(h,at,kind,s.adult,false);c.setState(s);long[] began={-1};
    cleanup(h,()->{CUES.remove(c);c.discard();});h.onEachTick(()->{if(ToyContent.isPlaying(c)&&began[0]<0)began[0]=start(c);});
    h.succeedWhen(()->{
      h.assertTrue(began[0]>=0&&h.getLevel().getGameTime()-began[0]>=180,"observe full actual variant interaction");
      h.assertTrue(!ToyContent.isPlaying(c)&&c.state().happiness==24,"replacement-hands/Chaos variant completes once");
      h.assertTrue(heard(c).equals(expected(kind,began[0],160)),"variant uses the same actual note clock");
      h.assertTrue(c.state().character.equals(character)&&c.state().immortal==immortal,"variant identity survives interaction");
    });
  }

  private static void learned(GameTestHelper h,boolean adult,String mode){
    var at=floor(h,ACCORDION,Direction.NORTH);h.getLevel().setBlock(at,Blocks.AIR.defaultBlockState(),3);
    var current=new ChaoEntity[]{actor(h,at,ACCORDION,adult,false)};InstrumentPerformanceGameTests02532.prepare(current[0],"accordion");
    long[] began={-1};boolean[] changed={false};int[] atStop={-1};var original=current[0];
    cleanup(h,()->{CUES.remove(original);CUES.remove(current[0]);current[0].discard();});
    h.onEachTick(()->{
      var c=current[0];if(c.getBehavior()==ChaoEntity.Behavior.INSTRUMENT&&began[0]<0){began[0]=h.getLevel().getGameTime();h.assertTrue(c.behaviorDetail().equals("accordion"),"real behavior goal chooses the learned accordion");}
      if(began[0]<0)return;
      if(c.getBehavior()==ChaoEntity.Behavior.INSTRUMENT){
        h.assertTrue(c.getMainHandItem().isEmpty()&&c.getHeldChaoItem().isEmpty(),"learned instrument never mints a prop item");
        InstrumentPerformance.tick(c);InstrumentPerformance.tick(c);
      }
      // Save exactly on a sounding beat; the persistent cue claim must prevent a reload replay.
      if(!mode.equals("complete")&&!changed[0]&&c.getBehaviorElapsed(0)>=72){
        changed[0]=true;atStop[0]=heard(c).size();
        if(mode.equals("stay"))c.setStaying(true);
        else if(mode.equals("hurt")){c.setInvulnerable(false);h.assertTrue(c.hurt(h.getLevel().damageSources().generic(),1),"real learned-player damage accepted");}
        else{
          float phase=c.getBehaviorElapsed(0);int remaining=c.getBehaviorTicks();var events=CUES.remove(c);int count=events.ticks.size();
          var tag=new CompoundTag();c.saveWithoutId(tag);c.discard();var copy=ChaoGarden.CHAO.get().create(h.getLevel());copy.load(tag);
          copy.goalSelector.removeAllGoals(g->true);copy.targetSelector.removeAllGoals(g->true);copy.goalSelector.addGoal(2,new ChaoBehaviorGoal(copy));h.getLevel().addFreshEntity(copy);current[0]=copy;CUES.put(copy,events);
          h.assertTrue(copy.getBehaviorElapsed(0)==phase&&copy.getBehaviorTicks()==remaining,"native learned save preserves authored phase and remaining time");
          InstrumentPerformance.tick(copy);h.assertTrue(events.ticks.size()==count,"sounding-beat reload cannot replay the same reed note");
        }
      }
    });
    h.succeedWhen(()->{
      h.assertTrue(began[0]>=0&&h.getLevel().getGameTime()-began[0]>=185,"observe learned phrase and exit");var c=current[0];
      h.assertTrue(c.getBehavior()!=ChaoEntity.Behavior.INSTRUMENT,"actual learned behavior exits");
      int count=mode.equals("complete")||mode.equals("reload")?8:atStop[0];
      h.assertTrue(count>0&&heard(c).size()==count&&new HashSet<>(heard(c)).size()==count&&!heard(c).contains(Long.MIN_VALUE),"authored reed notes stop immediately on interruption, without generic flute pulses: "+heard(c));
      h.assertTrue(c.state().traitInt("learned.accordion",0)==1,"performing does not reteach or forget the lesson");
    });
  }

  private static void preview(GameTestHelper h,String kind,boolean adult,boolean loop){
    var at=floor(h,kind,Direction.EAST);var c=actor(h,at,kind,adult,adult);c.goalSelector.removeAllGoals(g->true);
    if(kind.equals(PIANO))c.moveTo(ToyContent.keyboardAnchor(c,at));else h.getLevel().setBlock(at,Blocks.AIR.defaultBlockState(),3);
    c.setOnGround(true);c.setNoAi(adult);c.state().setTraitInt("learned.accordion",0);String before=c.state().encode();boolean priorAi=c.isNoAi();
    c.getPersistentData().putLong("ChaoToyNextPlay",123456789);var p=TestConnections.makeSurvivalServerPlayer(h);p.teleportTo(c.getX()+1,c.getY(),c.getZ());
    cleanup(h,()->{ToyPreview.stop(c);CUES.remove(c);c.discard();remove(p);});String name=kind.substring(5);
    h.assertTrue(command(p,"chao testtoy "+name+" "+(loop?"loop":"once"))==1,"actual dispatcher starts "+name+" preview without lesson or inventory item");long began=h.getLevel().getGameTime();
    if(kind.equals(PIANO)){
      var other=actor(h,at,kind,!adult,false);other.setNoAi(true);other.moveTo(c.position());
      h.assertTrue(ToyContent.previewKeyboard(other,kind)==null,"one piano reservation excludes a second performer");CUES.remove(other);other.discard();
    }
    h.onEachTick(()->{if(ToyPreview.active(c)){ToyPreview.tick(c);ToyPreview.tick(c);h.assertTrue(c.state().encode().equals(before),"preview preserves exact care, genetics, lessons and age");h.assertTrue(c.getMainHandItem().isEmpty()&&c.getHeldChaoItem().isEmpty(),"preview never makes inventory copies");}});
    // Assert on the terminal preview tick, before normal age/care simulation
    // legitimately resumes on the next tick.
    h.runAtTickTime(loop?325:160,()->{
      if(loop){h.assertTrue(ToyPreview.active(c),"real loop enters its third phrase");h.assertTrue(command(p,"chao testtoy "+name+" stop")==1,"actual named stop command");}else ToyPreview.tick(c);
      h.assertTrue(!ToyPreview.active(c)&&c.isNoAi()==priorAi&&c.toyPlayingTicks()==0&&c.toyTarget()==null,"once/stop restores original AI and clears presentation");
      h.assertTrue(c.state().encode().equals(before)&&c.getPersistentData().getLong("ChaoToyNextPlay")==123456789,"no reward, care mutation or cooldown change");
      h.assertTrue(heard(c).equals(expected(kind,began,loop?320:160)),"once/loop use production notes exactly and remain idempotent per loaded tick: "+heard(c));
      if(kind.equals(PIANO))h.assertFalse(h.getLevel().getBlockState(at).getValue(ChaoToyBlock.PLAYING),"preview returns the actual piano idle");h.succeed();
    });
  }
  private static void previewInterrupt(GameTestHelper h,String kind,String mode){
    var at=floor(h,kind,Direction.SOUTH);var c=actor(h,at,kind,false,true);c.goalSelector.removeAllGoals(g->true);
    if(kind.equals(PIANO))c.moveTo(ToyContent.keyboardAnchor(c,at));else h.getLevel().setBlock(at,Blocks.AIR.defaultBlockState(),3);
    c.setOnGround(true);var p=TestConnections.makeSurvivalServerPlayer(h);p.teleportTo(c.getX()+1,c.getY(),c.getZ());c.owner=p.getUUID();
    cleanup(h,()->{ToyPreview.stop(c);CUES.remove(c);c.discard();remove(p);});
    h.assertTrue(command(p,"chao testtoy "+kind.substring(5)+" loop")==1,"actual preview loop starts");
    h.runAtTickTime(55,()->{
      int count=heard(c).size();
      switch(mode){
        case "stop"->h.assertTrue(command(p,"chao testtoy stop")==1,"actual global stop command");
        case "hurt"->{c.setInvulnerable(false);h.assertTrue(c.hurt(h.getLevel().damageSources().generic(),1),"real preview damage accepted");}
        case "pet"->h.assertTrue(c.mobInteract(p,InteractionHand.MAIN_HAND).consumesAction(),"real care interaction accepted");
        case "move"->c.setPos(c.getX()+3,c.getY(),c.getZ());
        case "remove"->h.getLevel().destroyBlock(at,true);
        case "reload"->{var tag=new CompoundTag();c.saveWithoutId(tag);c.discard();var copy=ChaoGarden.CHAO.get().create(h.getLevel());copy.load(tag);h.getLevel().addFreshEntity(copy);h.assertTrue(!copy.isNoAi()&&copy.toyTarget()==null&&copy.toyPlayingTicks()==0,"native load restores pre-preview AI and never resumes the command");copy.discard();}
        case "logout"->remove(p);
        case "range"->p.teleportTo(c.getX()+70,c.getY(),c.getZ());
      }
      Runnable cancelled=()->{
        ToyPreview.tick(c);h.assertTrue(!ToyPreview.active(c)&&!c.isNoAi()&&c.toyPlayingTicks()==0&&c.toyTarget()==null,"interruption cleans production clock, target and temporary AI");
        h.runAfterDelay(20,()->{h.assertTrue(heard(c).size()==count,"no note after preview cancellation");h.succeed();});
      };
      // A block/position change made after this tick's preview update is
      // observed on the next simulation tick. Re-entering the same clock tick
      // deliberately cannot run the presentation/environment update twice.
      if(mode.equals("move")||mode.equals("remove"))h.runAfterDelay(1,cancelled);else cancelled.run();
    });
  }

  @GameTest(template="empty",timeoutTicks=1400,batch="instruments048_school")
  public static void actualAccordionClassroomPausesCompletesAndReturnsOneRewardEach(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);p.getInventory().clearContent();
    var school=p.server.getLevel(com.chaogarden.kindergarten.KindergartenContent.DIMENSION);
    h.assertTrue(com.chaogarden.hub.HubContent.ensure(school),"real school builds");InstrumentSchoolFixture02525.visit(p);
    var actors=new ArrayList<ChaoEntity>();long[] began={-1};boolean[] paused={false},resumed={false};int[] silence={0},pausedRemaining={-1};
    cleanup(h,()->{var roster=com.chaogarden.kindergarten.KindergartenRoster.get(p.server);for(var c:actors){roster.students.remove(c.getUUID());CUES.remove(c);c.discard();}roster.setDirty();remove(p);});
    h.onEachTick(()->{
      if(began[0]<0)return;long elapsed=school.getGameTime()-began[0];
      for(var c:actors)if(InstrumentPerformance.active(c,ACCORDION))h.assertTrue(c.getMainHandItem().isEmpty()&&c.getHeldChaoItem().isEmpty(),"practice never mints an accordion item");
      var adult=actors.get(1);
      if(elapsed>=70&&!paused[0]){paused[0]=true;adult.state().hunger=10;com.chaogarden.care.ChaoCareServices.tickLearning(adult);silence[0]=heard(adult).size();pausedRemaining[0]=adult.state().traitInt("lesson.remaining",-1);}
      if(paused[0]&&!resumed[0]){
        h.assertTrue(!InstrumentPerformance.active(adult,ACCORDION)&&heard(adult).size()==silence[0],"ordinary lesson pause removes prop and sounds");
        h.assertTrue(adult.state().traitInt("lesson.remaining",-1)==pausedRemaining[0],"paused classroom retains its remaining lesson time");
        if(elapsed>=90){resumed[0]=true;adult.state().hunger=83;}
      }
    });
    h.startSequence().thenWaitUntil(()->{
      com.chaogarden.kindergarten.KindergartenContent.ensureServices(school);
      h.assertTrue(school.isPositionEntityTicking(com.chaogarden.kindergarten.KindergartenContent.Role.TEACHER.anchor),"wait for real classroom ticking");
    }).thenExecute(()->{
      for(int n=0;n<2;n++){var c=InstrumentSchoolFixture02525.enroll(p,n,"accordion",n==1,335);actors.add(c);CUES.put(c,new Heard(ACCORDION,new ArrayList<>()));}began[0]=school.getGameTime();
    }).thenWaitUntil(()->{
      h.assertTrue(school.getGameTime()-began[0]>400,"observe two phrases, pause and completion");
      for(int n=0;n<2;n++){
        var c=actors.get(n);var cues=heard(c);
        h.assertTrue(!com.chaogarden.care.ChaoCareServices.hasLesson(c)&&c.state().traitInt("learned.accordion",0)==1,"actual lesson teaches accordion exactly once");
        // Existing school care freezes remaining lesson time and begins a new
        // entry when resumed. This fixture yields three pre-pause notes and
        // thirteen after resumption, rather than shortening the lesson.
        h.assertTrue(cues.size()==16,"both pupils retain all authored classroom notes: "+cues);
        if(n==1)h.assertTrue(cues.get(3)-cues.get(2)>48,"resumed adult notes move after the actual silent pause and new entry");
        h.assertTrue(new HashSet<>(cues).size()==cues.size()&&!cues.contains(Long.MIN_VALUE),"classroom uses unique actual reed cues");
        h.assertTrue(c.state().happiness==84&&c.state().adult==(n==1)&&com.chaogarden.kindergarten.KindergartenContent.isBoarded(c),"classroom care, age and custody stay intact");
      }
    }).thenExecute(()->{
      for(var c:actors)h.assertTrue(com.chaogarden.kindergarten.KindergartenContent.retrieve(p,c.getUUID()),"real teacher retrieves completed pupil");
      int rewards=0;for(int n=0;n<36;n++){var item=p.getInventory().getItem(n);if(item.is(ChaoGarden.item(ACCORDION)))rewards+=item.getCount();}
      h.assertTrue(rewards==2,"one real accordion reward per pupil, never per phrase");
    }).thenSucceed();
  }
}
