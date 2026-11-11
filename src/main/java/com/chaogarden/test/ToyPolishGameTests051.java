package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real session/dispatcher regressions and sound absence, independent of client pose tests. */
@GameTestHolder("chao_polish51") @PrefixGameTestTemplate(false)
@EventBusSubscriber(modid=ChaoGarden.MODID)
public final class ToyPolishGameTests051 {
  private static final List<String> REVISED=List.of("chao_toy_car","chao_sonic_doll","chao_shadow_doll",
      "chao_broom","chao_picture_book","chao_crayons");
  private static final List<String> SILENT=List.of("chao_sonic_doll","chao_shadow_doll","chao_picture_book","chao_crayons");
  private static final List<String> AUDIBLE=List.of("chao_toy_car","chao_broom","chao_pogo_stick");
  private static final Map<ChaoEntity,List<String>> HEARD=new WeakHashMap<>();
  private ToyPolishGameTests051() {}

  @SubscribeEvent public static void sound(PlayLevelSoundEvent event) {
    if(event.getLevel().isClientSide||event.getSound()==null)return;
    var key=BuiltInRegistries.SOUND_EVENT.getKey(event.getSound().value());
    HEARD.forEach((c,sounds)->{
      if(c.level()!=event.getLevel())return;
      boolean actor=event instanceof PlayLevelSoundEvent.AtEntity at&&at.getEntity()==c;
      boolean nearby=event instanceof PlayLevelSoundEvent.AtPosition at&&at.getPosition().distanceToSqr(c.position())<4;
      if(actor||nearby)sounds.add(String.valueOf(key));
    });
  }
  private static void cleanup(GameTestHelper h,Runnable action) {
    h.testInfo.addListener(new GameTestListener() {
      public void testStructureLoaded(GameTestInfo i) {}
      public void testPassed(GameTestInfo i,GameTestRunner r) {action.run();}
      public void testFailed(GameTestInfo i,GameTestRunner r) {action.run();}
      public void testAddedForRerun(GameTestInfo a,GameTestInfo b,GameTestRunner r) {action.run();}
    });
  }
  private static void remove(ServerPlayer player) {
    if(player!=null&&player.server.getPlayerList().getPlayer(player.getUUID())==player)
      player.server.getPlayerList().remove(player);
  }
  private record Fixture(ChaoEntity actor,BlockPos source) {}
  private static Fixture actor(GameTestHelper h,String kind,boolean adult) {
    var at=RattleGameTests02520.floor(h);
    h.getLevel().setBlock(at,ToyContent.block(kind).defaultBlockState(),3);
    var c=RattleGameTests02520.actor(h,at,adult,ChaoState.NORMAL,false);
    c.goalSelector.removeAllGoals(g->true);c.targetSelector.removeAllGoals(g->true);
    c.setNoAi(true);c.setOnGround(true);c.state().setTrait("polish51.foreign","preserve this state");
    HEARD.put(c,new ArrayList<>());
    cleanup(h,()->{ToyPreview.stop(c);HEARD.remove(c);c.discard();});
    return new Fixture(c,at);
  }
  private static int command(ServerPlayer player,String text) {
    try {return player.server.getCommands().getDispatcher().execute(text,
        player.createCommandSourceStack().withPermission(2));}
    catch(Exception failure) {throw new IllegalStateException("Native command failed: "+text,failure);}
  }
  // Explicit old cue times are kept separate from the production predicates.
  private static boolean audibleDue(String kind,int tick) {
    return kind.equals("chao_toy_car")?tick==32||tick==64||tick==96||tick==128:
        tick>=24&&tick<=136&&(tick-24)%16==0;
  }
  private static String soundId(String kind) {
    return "chao_garden:"+switch(kind) {
      case "chao_toy_car"->"toy.car";
      case "chao_broom"->"toy.broom";
      case "chao_pogo_stick"->"toy.pogo";
      default->throw new IllegalArgumentException(kind);
    };
  }

  @GameTestGenerator public static Collection<TestFunction> cases() {
    var cases=new ArrayList<TestFunction>();
    for(var existing:GardenToyGameTests049.cases()) {
      String name=existing.testName();
      boolean revised=REVISED.stream().anyMatch(kind->name.startsWith(kind.substring(5)+"_"));
      boolean control=List.of("rocking_horse","pogo_stick").stream().anyMatch(kind->
          name.startsWith(kind+"_complete_true_")||name.equals(kind+"_preview_true_false")
              ||name.equals(kind+"_placed_remove"));
      if(revised||control)cases.add(new TestFunction("polish051_"+existing.batchName(),
          "polish051_"+name,"chao_polish51:activity_test",existing.rotation(),existing.maxTicks(),
          existing.setupTicks(),existing.required(),existing.manualOnly(),existing.maxAttempts(),
          existing.requiredSuccesses(),existing.skyAccess(),existing.function()));
    }
    for(String kind:SILENT)for(boolean adult:new boolean[]{false,true})
      cases.add(new TestFunction("polish051_sound","forced_silent_"+kind.substring(5)+"_"+adult,
          "chao_polish51:activity_test",40,0,true,h->silentDispatch(h,kind,adult)));
    for(String kind:AUDIBLE)for(boolean adult:new boolean[]{false,true})
      cases.add(new TestFunction("polish051_sound","retained_cue_clock_"+kind.substring(5)+"_"+adult,
          "chao_polish51:activity_test",40,0,true,h->audibleDispatch(h,kind,adult)));
    for(String kind:REVISED)for(boolean originalNoAi:new boolean[]{false,true})
      cases.add(new TestFunction("polish051_unload","actual_unload_"+kind.substring(5)+"_"+originalNoAi,
          "chao_polish51:activity_test",160,0,true,h->unload(h,kind,originalNoAi)));
    for(String kind:List.of("chao_toy_car","chao_crayons"))for(boolean adult:new boolean[]{false,true})
      for(Direction facing:Direction.Plane.HORIZONTAL) {
        String suffix=kind.substring(5)+"_"+(adult?"adult":"baby")+"_"+facing.getName();
        cases.add(new TestFunction("polish051_space","prone_wall_ledge_source_"+suffix,
            "chao_polish51:activity_test",60,0,true,h->proneSpace(h,kind,adult,facing)));
        cases.add(new TestFunction("polish051_space_preview","prone_preview_hazards_"+suffix,
            "chao_polish51:activity_test",140,0,true,h->pronePreview(h,kind,adult,facing)));
      }
    for(boolean adult:new boolean[]{false,true})for(boolean front:new boolean[]{false,true})
      for(Direction facing:Direction.Plane.HORIZONTAL) {
        String name="car_route_"+(adult?"adult":"run0_baby")+"_"+(front?"front":"rear")+"_"+facing.getName();
        cases.add(new TestFunction("polish051_car_navigation",name,"chao_polish51:activity_test",
            2600,0,true,h->carNavigation(h,adult,front,facing)));
      }
    return cases;
  }

  private static String carNavigationDiagnostic(ChaoEntity c,BlockPos at,long began,double distance,double maxY,double maxStep) {
    var path=c.getNavigation().getPath();
    return "pos="+c.position()+" velocity="+c.getDeltaMovement()+" onGround="+c.onGround()
        +" navDone="+c.getNavigation().isDone()+" path="+(path==null?"none":path.getNextNodeIndex()+"/"+path.getNodeCount()+" reaches="+path.canReach())
        +" wanted="+c.getMoveControl().getWantedX()+","+c.getMoveControl().getWantedY()+","+c.getMoveControl().getWantedZ()
        +" playing="+c.toyPlayingKind()+" target="+c.toyTarget()+" source="+at+" began="+began
        +" distance="+distance+" maxY="+maxY+" maxStep="+maxStep;
  }

  private static void carNavigation(GameTestHelper h,boolean adult,boolean startAtFront,Direction facing) {
    String kind="chao_toy_car";var level=h.getLevel();var at=RattleGameTests02520.floor(h);
    level.setBlock(at,ToyContent.block(kind).defaultBlockState().setValue(ChaoToyBlock.FACING,facing),3);
    // Unlike the old general-toy fixture, this baby keeps its genuine Run-0
    // movement. Starting at the hood forces natural navigation around the car
    // to its required rear contact instead of beginning on the convenient side.
    var c=RattleGameTests02520.actor(h,at,adult,ChaoState.NORMAL,false);
    c.state().setTrait("polish51.navigation","keep this history");c.sync();
    var side=startAtFront?facing:facing.getOpposite();var origin=Vec3.atBottomCenterOf(at.relative(side,3));
    c.moveTo(origin.x,origin.y,origin.z,side.getOpposite().toYRot(),0);c.setOnGround(true);
    cleanup(h,()->{ToyContent.endPlaying(c,false);c.discard();});
    var car=level.getBlockState(at);
    h.assertTrue(ToyContent.block(kind).getBlockPathType(car,level,at,null)==PathType.FENCE,
        "the mob-independent path cache must reject the car cell rather than cache an open/jump route");
    h.assertTrue(ToyContent.block(kind).getBlockPathType(car,level,at,c)==PathType.FENCE,
        "the actual Chao path query must share the cache's blocked-car policy");
    long[] began={-1},ended={-1};int[] activeFrames={0};double[] walked={0},maxY={c.getY()},maxStep={0};
    var previous=new Vec3[]{c.position()};
    h.onEachTick(()->{
      var now=c.position();var step=now.subtract(previous[0]);
      walked[0]+=step.horizontalDistance();maxStep[0]=Math.max(maxStep[0],step.length());maxY[0]=Math.max(maxY[0],now.y);previous[0]=now;
      String diagnostic=carNavigationDiagnostic(c,at,began[0],walked[0],maxY[0],maxStep[0]);
      h.assertTrue(c.state().adult==adult&&c.state().points[ChaoState.RUN]==(adult?667:0),"navigation retains the real age and Run points: "+diagnostic);
      h.assertTrue(maxY[0]<=at.getY()+.08,"car route must stay on the floor instead of climbing onto the toy: "+diagnostic);
      h.assertTrue(maxStep[0]<.35,"navigation uses ordinary bounded motion and the short contact handoff, not a teleport: "+diagnostic);
      h.assertTrue(c.getMainHandItem().isEmpty()&&c.getHeldChaoItem().isEmpty(),"approach and play create no inventory prop");
      var source=level.getBlockState(at);h.assertTrue(source.is(ToyContent.block(kind)),"navigation preserves the one physical car");
      if(ToyContent.isPlaying(c)) {
        if(began[0]<0)began[0]=c.getPersistentData().getLong("ChaoToyUntil")-160;
        activeFrames[0]++;
        h.assertTrue(kind.equals(c.toyPlayingKind())&&at.equals(c.toyTarget()),"the actual AI reserves the intended car");
        var relative=c.position().subtract(Vec3.atBottomCenterOf(at));
        h.assertTrue(relative.x*facing.getStepX()+relative.z*facing.getStepZ()<-.01,
            "play must begin at the rear rather than the hood or a side: "+diagnostic);
        h.assertTrue(level.noCollision(c,c.getBoundingBox())&&source.getRenderShape()==RenderShape.INVISIBLE,
            "the grounded performer clears the reserved and hidden car source");
      }else{
        if(began[0]>=0&&ended[0]<0)ended[0]=level.getGameTime();
        if(began[0]<0||level.getGameTime()>began[0]+160)
          h.assertTrue(source.getRenderShape()==RenderShape.MODEL,"the source is visible outside its real play session");
      }
    });
    h.succeedWhen(()->{
      String diagnostic=carNavigationDiagnostic(c,at,began[0],walked[0],maxY[0],maxStep[0]);
      h.assertTrue(began[0]>=0&&level.getGameTime()-began[0]>=180,"observe the actual route, complete phrase and exit: "+diagnostic);
      h.assertTrue(walked[0]>1,"the Chao must actually navigate to the rear contact: "+diagnostic);
      h.assertTrue(ended[0]-began[0]==160&&activeFrames[0]>=158,"the complete production phrase lasts160ticks without early cancellation: "+diagnostic);
      h.assertTrue(!ToyContent.isPlaying(c)&&c.toyTarget()==null&&!c.isNoGravity(),"completion clears temporary source ownership and physics");
      h.assertTrue(c.state().happiness==24&&c.state().trait("polish51.navigation").equals("keep this history"),
          "natural completion grants exactly four happiness and preserves unrelated history");
      h.assertTrue(c.getPersistentData().getLong("ChaoToyNextPlay")>level.getGameTime(),"completed navigation/play keeps the ordinary cooldown");
      var source=level.getBlockState(at);
      h.assertTrue(source.getRenderShape()==RenderShape.MODEL&&!source.getValue(ChaoToyBlock.PLAYING)&&source.getValue(ChaoToyBlock.FRAME)==0,
          "the one original car returns to its visible idle state");
      h.assertTrue(level.getEntitiesOfClass(ItemEntity.class,new AABB(at).inflate(4),e->e.getItem().is(ChaoGarden.item(kind))).isEmpty(),
          "navigation and completion cannot duplicate a car item");
    });
  }

  private static Fixture spaceFixture(GameTestHelper h,String kind,boolean adult,Direction facing) {
    var fixture=actor(h,kind,adult);var c=fixture.actor();var at=fixture.source();
    h.getLevel().setBlock(at,Blocks.AIR.defaultBlockState(),3);
    c.moveTo(Vec3.atBottomCenterOf(at));c.setOnGround(true);
    c.setYRot(facing.toYRot());c.yBodyRot=c.yBodyRotO=c.yHeadRot=c.yHeadRotO=facing.toYRot();
    return fixture;
  }

  private static void proneSpace(GameTestHelper h,String kind,boolean adult,Direction facing) {
    var fixture=spaceFixture(h,kind,adult,facing);var c=fixture.actor();var at=fixture.source();
    var level=h.getLevel();var anchor=c.position();float yaw=facing.toYRot();var standing=c.getBoundingBox();
    String before=c.state().encode();var front=at.relative(facing);
    h.assertTrue(level.noCollision(c,standing)&&ToyContent.proneSpaceReady(c,kind,anchor,yaw,null),
        "clear supported floor admits the full prone presentation");
    // The adjacent wall and ledge are outside the unchanged standing hitbox.
    // They still intersect the measured forward extent of belly/head/props.
    level.setBlock(front,Blocks.STONE.defaultBlockState(),3);
    h.assertTrue(level.noCollision(c,standing),"wall fixture must leave the standing hitbox clear");
    h.assertFalse(ToyContent.proneSpaceReady(c,kind,anchor,yaw,null),
        "one-cell forward wall must reject the visible prone footprint");
    level.setBlock(front,Blocks.AIR.defaultBlockState(),3);
    level.setBlock(front.below(),Blocks.AIR.defaultBlockState(),3);
    h.assertTrue(level.noCollision(c,standing),"ledge fixture must leave the standing hitbox clear");
    h.assertFalse(ToyContent.proneSpaceReady(c,kind,anchor,yaw,null),
        "supported standing feet alone cannot admit a prone body extending over a ledge");
    level.setBlock(front.below(),Blocks.STONE.defaultBlockState(),3);
    h.assertTrue(ToyContent.proneSpaceReady(c,kind,anchor,yaw,null),"restoring real floor restores safe prone space");

    // Place this probe inside the visual footprint so even the very low crayon
    // box actually intersects it for a baby. No standing-physics exemption is
    // asserted here: the real callers retain their separate standing check.
    level.setBlock(at,ToyContent.block(kind).defaultBlockState().setValue(ChaoToyBlock.FACING,facing),3);
    h.assertFalse(ToyContent.proneSpaceReady(c,kind,anchor,yaw,null),
        "an unowned matching toy cannot be silently ignored");
    h.assertFalse(ToyContent.proneSpaceReady(c,kind,anchor,yaw,front),
        "an unrelated source coordinate cannot ignore another occupied cell");
    h.assertTrue(ToyContent.proneSpaceReady(c,kind,anchor,yaw,at),
        "only the explicitly owned matching source is exempt from visual occupancy");
    String other=kind.equals("chao_toy_car")?"chao_crayons":"chao_toy_car";
    level.setBlock(at,ToyContent.block(other).defaultBlockState(),3);
    h.assertFalse(ToyContent.proneSpaceReady(c,kind,anchor,yaw,at),"a different toy at the source coordinate still blocks");
    level.setBlock(at,Blocks.STONE.defaultBlockState(),3);
    h.assertFalse(ToyContent.proneSpaceReady(c,kind,anchor,yaw,at),"the source exemption never ignores a replaced solid block");
    level.setBlock(at,Blocks.AIR.defaultBlockState(),3);
    h.assertTrue(c.position().equals(anchor)&&c.getBoundingBox().equals(standing)
        &&c.state().encode().equals(before),"space probes cannot move the actor, resize its hitbox or alter saved care/DNA");
    h.succeed();
  }

  private static void pronePreview(GameTestHelper h,String kind,boolean adult,Direction facing) {
    var fixture=spaceFixture(h,kind,adult,facing);var c=fixture.actor();var at=fixture.source();
    var level=h.getLevel();var front=at.relative(facing);var standing=c.getBoundingBox();
    boolean originalFlags=adult;c.setNoAi(originalFlags);c.setNoGravity(originalFlags);c.setOnGround(true);
    c.getPersistentData().putLong("ChaoToyNextPlay",123456789);String before=c.state().encode();
    var player=TestConnections.makeSurvivalServerPlayer(h);
    var away=at.relative(facing.getClockWise(),2).relative(facing.getOpposite(),2);
    player.teleportTo(away.getX()+.5,c.getY(),away.getZ()+.5);c.owner=player.getUUID();cleanup(h,()->remove(player));
    String prefix="chao testtoy "+kind.substring(5);
    level.setBlock(front,Blocks.STONE.defaultBlockState(),3);
    h.assertTrue(level.noCollision(c,standing),"unsafe-preview wall is clear of ordinary standing collision");
    h.assertTrue(command(player,prefix+" once")==0&&!ToyPreview.active(c),"real dispatcher refuses a prone preview behind a forward wall");
    level.setBlock(front,Blocks.AIR.defaultBlockState(),3);level.setBlock(front.below(),Blocks.AIR.defaultBlockState(),3);
    h.assertTrue(command(player,prefix+" loop")==0&&!ToyPreview.active(c),"real dispatcher refuses a prone preview over an unsupported ledge");
    h.assertTrue(c.state().encode().equals(before)&&c.isNoAi()==originalFlags&&c.isNoGravity()==originalFlags
        &&c.getPersistentData().getLong("ChaoToyNextPlay")==123456789,"refused previews leave care, cooldown and original physics flags intact");
    level.setBlock(front.below(),Blocks.STONE.defaultBlockState(),3);
    h.assertTrue(command(player,prefix+" loop")==1&&ToyPreview.active(c),"the same real preview starts once its space becomes safe");
    h.onEachTick(()->{
      if(ToyPreview.active(c))h.assertTrue(c.state().encode().equals(before),"safe prone preview changes no saved care or genetics");
    });
    h.runAtTickTime(64,()->{
      int count=HEARD.get(c).size();level.setBlock(front,Blocks.STONE.defaultBlockState(),3);
      h.assertTrue(level.noCollision(c,c.getBoundingBox()),"new wall must still leave the standing hitbox clear");
      ToyPreview.tick(c);
      h.assertTrue(!ToyPreview.active(c)&&c.toyPlayingTicks()==0&&c.toyTarget()==null
          &&c.isNoAi()==originalFlags&&c.isNoGravity()==originalFlags,"a newly blocked prone footprint cancels and restores both original flags");
      h.assertTrue(c.state().encode().equals(before)&&c.getPersistentData().getLong("ChaoToyNextPlay")==123456789,
          "new-wall cancellation gives no reward or permanent care/cooldown change");
      h.runAfterDelay(24,()->{
        h.assertTrue(!ToyPreview.active(c)&&HEARD.get(c).size()==count,"new-wall cancellation leaves no later toy cue or restarted preview");
        h.assertTrue(c.isNoAi()==originalFlags&&c.isNoGravity()==originalFlags,"restored flags remain stable after cancellation");h.succeed();
      });
    });
  }

  private static void silentDispatch(GameTestHelper h,String kind,boolean adult) {
    var c=actor(h,kind,adult).actor();c.setToyPlaying(kind,160);
    String before=c.state().encode();var events=HEARD.get(c);
    for(int tick=0;tick<=160;tick++) {
      // A stale or explicit caller must not revive an intentionally silent toy.
      GardenToyPerformance049.cue(c,kind);
      var clip=HandToyEffects.find(kind.substring(5));
      h.assertTrue(clip!=null&&clip.kind().equals(kind),"silent activity keeps its real preview adapter");
      h.assertFalse(clip.due().test(tick),"silent activity still schedules a toy sound at "+tick);
      HandToyEffects.tick(c,kind,tick);
    }
    h.assertTrue(events.isEmpty(),"muted production/cue/preview paths attempted sound dispatch: "+events);
    h.assertTrue(c.state().encode().equals(before),"sound policy cannot change care or DNA");h.succeed();
  }

  private static void audibleDispatch(GameTestHelper h,String kind,boolean adult) {
    var c=actor(h,kind,adult).actor();c.setToyPlaying(kind,160);
    var events=HEARD.get(c);String expected=soundId(kind);
    GardenToyPerformance049.cue(c,kind);
    h.assertTrue(events.equals(List.of(expected)),"retained cue must dispatch exactly once: "+events);
    events.clear();int count=0;
    for(int tick=0;tick<=160;tick++) {
      var clip=HandToyEffects.find(kind.substring(5));
      h.assertTrue(clip!=null&&clip.due().test(tick)==audibleDue(kind,tick),
          "audible activity changed its existing cue clock at "+tick);
      if(audibleDue(kind,tick))count++;
      HandToyEffects.tick(c,kind,tick);
    }
    h.assertTrue(events.size()==count&&events.stream().allMatch(expected::equals),
        "retained production clock emitted wrong or duplicate sounds: "+events);h.succeed();
  }

  private static void unload(GameTestHelper h,String kind,boolean originalNoAi) {
    var fixture=actor(h,kind,originalNoAi);var c=fixture.actor();var at=fixture.source();
    // The operator preview uses its actual source-free hand adapter.
    h.getLevel().setBlock(at,Blocks.AIR.defaultBlockState(),3);c.setNoAi(originalNoAi);c.setOnGround(true);
    c.getPersistentData().putLong("ChaoToyNextPlay",123456789);
    String before=c.state().encode();var player=TestConnections.makeSurvivalServerPlayer(h);
    player.teleportTo(c.getX()+1.5,c.getY(),c.getZ()+1.5);c.owner=player.getUUID();
    cleanup(h,()->remove(player));
    h.assertTrue(command(player,"chao testtoy "+kind.substring(5)+" loop")==1,"native loop starts before unload");
    h.onEachTick(()->{
      if(ToyPreview.active(c))h.assertTrue(c.state().encode().equals(before),"preview changes no saved state before unload");
    });
    h.runAtTickTime(64,()->{
      int count=HEARD.get(c).size();var saved=new CompoundTag();c.saveWithoutId(saved);
      h.assertTrue(saved.getBoolean("NoAI")==originalNoAi,"saved preview writes ordinary AI flags");
      c.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
      h.runAfterDelay(2,()->{
        h.assertTrue(!ToyPreview.active(c)&&c.isNoAi()==originalNoAi&&!c.isNoGravity()
            &&c.toyTarget()==null&&c.toyPlayingTicks()==0,"real unload restores temporary flags and playback state");
        var loaded=ChaoGarden.CHAO.get().create(h.getLevel());h.assertTrue(loaded!=null,"saved actor creates");
        cleanup(h,loaded::discard);loaded.load(saved);
        h.assertTrue(loaded.state().encode().equals(before)&&loaded.isNoAi()==originalNoAi
            &&!loaded.isNoGravity()&&!ToyPreview.active(loaded)&&loaded.toyPlayingTicks()==0
            &&loaded.toyTarget()==null,"unloaded preview snapshot reloads as an ordinary unchanged actor");
        h.assertTrue(loaded.getPersistentData().getLong("ChaoToyNextPlay")==123456789,
            "unload and reload preserve the original cooldown");
        h.runAfterDelay(24,()->{
          h.assertTrue(HEARD.get(c).size()==count,"no later cue follows actual unload");h.succeed();
        });
      });
    });
  }

  @GameTest(template="activity_test",timeoutTicks=40,batch="polish051_commands")
  public static void realDispatcherListsAllRevisedToyModes(GameTestHelper h) {
    var player=TestConnections.makeSurvivalServerPlayer(h);cleanup(h,()->remove(player));
    var dispatcher=player.server.getCommands().getDispatcher();
    var source=player.createCommandSourceStack().withPermission(2);
    h.assertTrue(command(player,"chao testtoy list")==1,"operator preview help remains available");
    var suggestions=dispatcher.getCompletionSuggestions(dispatcher.parse("chao testtoy ",source)).join()
        .getList().stream().map(s->s.getText()).toList();
    for(String kind:REVISED) {
      String id=kind.substring(5);h.assertTrue(suggestions.contains(id),"preview suggestions omit "+id);
      var modes=dispatcher.getCompletionSuggestions(dispatcher.parse("chao testtoy "+id+" ",source)).join()
          .getList().stream().map(s->s.getText()).toList();
      h.assertTrue(modes.containsAll(List.of("once","loop","stop")),"production preview modes missing for "+id);
    }
    h.succeed();
  }

  @GameTest(template="activity_test",timeoutTicks=40,batch="polish051_space")
  public static void proneGuardDoesNotChangeOtherToySpacePolicies(GameTestHelper h) {
    var fixture=spaceFixture(h,"chao_toy_car",true,Direction.SOUTH);var c=fixture.actor();var at=fixture.source();
    h.getLevel().setBlock(at.south(),Blocks.STONE.defaultBlockState(),3);
    h.getLevel().setBlock(at.south().below(),Blocks.AIR.defaultBlockState(),3);
    h.assertFalse(ToyContent.proneSpaceReady(c,"chao_toy_car",c.position(),0,null),"hazard fixture really rejects prone play");
    for(String kind:ToyContent.ids())if(!kind.equals("chao_toy_car")&&!kind.equals("chao_crayons"))
      h.assertTrue(ToyContent.proneSpaceReady(c,kind,c.position(),0,null),"unrelated toy keeps its own existing space policy: "+kind);
    h.succeed();
  }
}
