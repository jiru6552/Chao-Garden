package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoIllness;
import com.chaogarden.core.ChaoPreferences;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.ChaoToyBlock;
import com.chaogarden.toys.ToyContent;
import com.chaogarden.toys.ToyPreview;
import com.chaogarden.toys.TvWatchRules073;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real navigation, physical screen contact and reward-free production TV previews. */
@GameTestHolder("chao_tv73") @PrefixGameTestTemplate(false) @EventBusSubscriber(modid=ChaoGarden.MODID)
public final class TvWatchGameTests073 {
  private static final String TEMPLATE="chao_tv73:activity_test";
  private static final Map<ChaoEntity,List<Long>> CUES=new WeakHashMap<>();
  private record Fixture(ChaoEntity c,ServerPlayer p,BlockPos tv){}
  private TvWatchGameTests073(){}
  @SubscribeEvent public static void sound(PlayLevelSoundEvent.AtPosition e){
    if(e.getLevel().isClientSide||e.getSound()==null||!e.getSound().value().getLocation().toString().equals("minecraft:block.note_block.bit"))return;
    CUES.forEach((c,events)->{if(c.level()==e.getLevel()&&e.getPosition().distanceToSqr(c.position())<4)events.add(e.getLevel().getGameTime());});
  }
  @GameTestGenerator public static Collection<TestFunction> cases(){
    var tests=new ArrayList<TestFunction>();
    for(boolean adult:new boolean[]{false,true})for(Direction facing:Direction.Plane.HORIZONTAL)for(Direction start:Direction.Plane.HORIZONTAL)
      add(tests,"tv_approach_"+(adult?"adult":"newborn")+"_screen_"+facing+"_from_"+start,2000,h->approach(h,adult,facing,start,false));
    for(Direction facing:Direction.Plane.HORIZONTAL)add(tests,"tv_newborn_rear_alcove_"+facing,2000,h->approach(h,false,facing,facing.getOpposite(),true));
    for(boolean adult:new boolean[]{false,true})for(boolean loop:new boolean[]{false,true})
      add(tests,"tv_preview_"+(adult?"adult":"baby")+"_"+(loop?"loop":"once"),430,h->preview(h,adult,loop));
    for(String mode:List.of("stop","damage","pet","remove","rotate","obstruct","displace","unload","logout","range","reload","original_noai"))
      add(tests,"tv_preview_interrupt_"+mode,260,h->interrupt(h,mode));
    for(String mode:List.of("rear","blocked","absent","reserved"))add(tests,"tv_preview_rejects_"+mode,60,h->reject(h,mode));
    add(tests,"tv_native_permission_and_command_catalog",60,TvWatchGameTests073::permissions);
    add(tests,"tv_natural_rotated_screen_cancels_without_reward",500,h->naturalInterrupt(h,true));
    add(tests,"tv_natural_removed_screen_cancels_without_reward",500,h->naturalInterrupt(h,false));
    return tests;
  }
  private static void add(List<TestFunction> tests,String name,int timeout,java.util.function.Consumer<GameTestHelper> run){
    tests.add(new TestFunction("tv073_"+(tests.size()/6),name,TEMPLATE,timeout,0,true,run));
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
  private static ChaoEntity actor(GameTestHelper h,BlockPos at,boolean adult){
    var c=ChaoGarden.CHAO.get().create(h.getLevel());h.assertTrue(c!=null,"Production actor creates");
    var s=new ChaoState();s.adult=adult;s.align=adult?-1:0;s.primaryType=adult?ChaoState.RUN_TYPE:ChaoState.NORMAL;
    s.hunger=90;s.energy=100;s.happiness=20;s.boredom=100;s.personality=ChaoPreferences.EASILY_BORED;
    if(adult){s.levels[ChaoState.RUN]=9;s.progress[ChaoState.RUN]=60;s.points[ChaoState.RUN]=175;s.setEvolutionStrength(1.2f);}
    for(int illness=0;illness<ChaoIllness.COUNT;illness++)s.setIllness(illness,100);
    c.setState(ChaoState.decode(s.encode()));c.getRandom().setSeed(18);c.setInvulnerable(true);c.moveTo(Vec3.atBottomCenterOf(at));
    h.getLevel().addFreshEntity(c);c.goalSelector.removeAllGoals(g->true);c.targetSelector.removeAllGoals(g->true);
    CUES.put(c,new ArrayList<>());cleanup(h,()->{ToyPreview.stop(c);CUES.remove(c);c.discard();});return c;
  }
  private static void approach(GameTestHelper h,boolean adult,Direction facing,Direction start,boolean alcove){
    var at=floor(h);var level=h.getLevel();var tv=ToyContent.block(TvWatchRules073.KIND).defaultBlockState().setValue(ChaoToyBlock.FACING,facing);level.setBlock(at,tv,3);
    var side=facing.getClockWise();if(alcove){
      level.setBlock(at.above(),Blocks.STONE.defaultBlockState(),3);
      for(var block:List.of(at.relative(side),at.relative(side).relative(facing.getOpposite())))for(int y=0;y<2;y++)level.setBlock(block.above(y),Blocks.STONE.defaultBlockState(),3);
    }
    var c=actor(h,at.relative(start,alcove?3:2),adult);c.goalSelector.addGoal(4,new ToyContent.PlayGoal(c));
    var played=new AtomicBoolean();var previous=new AtomicReference<>(c.position());var maxSide=new AtomicReference<>(0d);var firstTick=new long[]{-1};var initialType=c.state().lockedPrimaryType();var initialAlign=c.state().lockedAlignment();
    h.onEachTick(()->{
      var path=c.getNavigation().getPath();if(path!=null)for(int i=0;i<path.getNodeCount();i++){
        var node=path.getNode(i);h.assertTrue(node.x!=at.getX()||node.z!=at.getZ()||node.y<at.getY()||node.y>at.getY()+1,"Path must not cross TV or its failed jump-on-top node");
      }
      var delta=c.position().subtract(Vec3.atBottomCenterOf(at));maxSide.set(Math.max(maxSide.get(),Math.abs(delta.x*side.getStepX()+delta.z*side.getStepZ())));
      h.assertTrue(c.position().distanceTo(previous.get())<.7,"Natural approach must walk rather than remotely snap to the front");previous.set(c.position());
      if(ToyContent.isPlaying(c)){
        played.set(true);if(firstTick[0]<0)firstTick[0]=level.getGameTime();
        var anchor=ToyContent.tvAnchor(c,at);h.assertTrue(c.distanceToSqr(anchor)<.0001,"Watching at the collision-clear screen-side seat");
        var toward=ToyContent.tvScreen(c,at).subtract(c.position());var look=Vec3.directionFromRotation(0,c.getYRot());
        h.assertTrue(look.x*toward.x+look.z*toward.z>.7,"Body faces the actual rendered screen, not the rear cabinet");
        if(adult)h.assertTrue(c.state().lockedPrimaryType()==initialType&&c.state().lockedAlignment()==initialAlign,
            "Adult first identity remains locked: expected="+initialType+"/"+initialAlign+" actual="+c.state().lockedPrimaryType()+"/"+c.state().lockedAlignment());
        else h.assertTrue(!c.state().adult&&c.state().primaryType==ChaoState.NORMAL&&c.state().lockedAlignment()==initialAlign,
            "A newborn retains its unevolved identity/alignment while ordinary movement may change its prospective first type: raw="+c.state().primaryType+" prospective="+c.state().lockedPrimaryType()+" points="+java.util.Arrays.toString(c.state().points));
        int elapsed=(int)(160-c.toyPlayingTicks());int frame=level.getBlockState(at).getValue(ChaoToyBlock.FRAME);
        if(elapsed>=22&&elapsed<58)h.assertTrue(frame==1,"First program frame holds after the one startup cue");
        if(elapsed>=62&&elapsed<98)h.assertTrue(frame==2,"Second program frame advances on the shared session clock");
        if(elapsed>=102&&elapsed<138)h.assertTrue(frame==3,"Third program frame advances without a chime loop");
      }
    });
    h.succeedWhen(()->{
      h.assertTrue(played.get()&&!ToyContent.isPlaying(c)&&!c.getPersistentData().contains("ChaoToyUntil")&&c.state().happiness>20,"Real voluntary session completes before its reward");
      h.assertTrue(CUES.get(c).size()==1,"Exactly one quiet TV cue per actual watch session");
      h.assertTrue(CUES.get(c).getFirst()-firstTick[0]>=18&&CUES.get(c).getFirst()-firstTick[0]<=21,"Startup cue occurs at the shared settle boundary");
      h.assertTrue(level.getBlockState(at).is(ToyContent.block(TvWatchRules073.KIND))&&!level.getBlockState(at).getValue(ChaoToyBlock.PLAYING),"One physical TV remains inactive after completion");
      if(start==facing.getOpposite())h.assertTrue(maxSide.get()>.65,"Rear approach physically detours around the TV");
      h.assertTrue(c.state().adult==adult,"No age-stage change from toy use");
    });
  }
  private static Fixture fixture(GameTestHelper h,boolean adult){
    var at=floor(h);h.getLevel().setBlock(at,ToyContent.block(TvWatchRules073.KIND).defaultBlockState().setValue(ChaoToyBlock.FACING,Direction.SOUTH),3);
    var c=actor(h,at.south(),adult);c.setOnGround(true);c.getPersistentData().putLong("ChaoToyNextPlay",h.getLevel().getGameTime()+12345);
    var p=TestConnections.makeServerPlayer(h);p.teleportTo(c.getX()+2,c.getY(),c.getZ());c.owner=p.getUUID();
    cleanup(h,()->{if(p.server.getPlayerList().getPlayer(p.getUUID())!=null)p.server.getPlayerList().remove(p);});return new Fixture(c,p,at);
  }
  private static int command(ServerPlayer p,String text,int permission){
    try{return p.server.getCommands().getDispatcher().execute(text,p.createCommandSourceStack().withPermission(permission));}
    catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){return -1;}
  }
  private static int command(ServerPlayer p,String text){return command(p,text,2);}
  private static void aim(GameTestHelper h,Fixture f){
    var screen=ToyContent.tvScreen(f.c,f.tv);var c=f.c;double dx=screen.x-c.getX(),dz=screen.z-c.getZ();
    float pitch=net.minecraft.util.Mth.clamp((float)-Math.toDegrees(Math.atan2(screen.y-c.getEyeY(),Math.hypot(dx,dz))),-30,30);
    h.assertTrue(Math.abs(c.getXRot()-pitch)<.0001,"Frozen-AI preview still uses the actual screen-height pitch");
    double yaw=Math.toRadians(c.getYHeadRot());double dot=-Math.sin(yaw)*dx+Math.cos(yaw)*dz;
    h.assertTrue(dot>.75,"Frozen-AI head looks toward the actual screen-plane center");
  }
  private static void idle(GameTestHelper h,Fixture f,String state,long cooldown,boolean noAi){
    h.assertTrue(!ToyPreview.active(f.c)&&f.c.toyPlayingTicks()==0&&f.c.toyTarget()==null&&!f.c.getPersistentData().contains("ChaoToyTarget"),
        "Preview releases clock/reservation after its authoritative validation: active="+ToyPreview.active(f.c)+" remaining="+f.c.toyPlayingTicks()+" target="+f.c.toyTarget()+" rawTarget="+f.c.getPersistentData().contains("ChaoToyTarget")+" noAi="+f.c.isNoAi()+" worldTick="+f.c.level().getGameTime());
    h.assertTrue(f.c.state().encode().equals(state)&&f.c.getPersistentData().getLong("ChaoToyNextPlay")==cooldown,
        "Preview care/cooldown changed before ordinary care resumes: age="+f.c.state().ageTicks+" adultAge="+f.c.state().adultAgeTicks+" expectedState="+state+" actualState="+f.c.state().encode()+" expectedCooldown="+cooldown+" actualCooldown="+f.c.getPersistentData().getLong("ChaoToyNextPlay"));
    h.assertTrue(f.c.isNoAi()==noAi&&!f.c.getPersistentData().contains("ChaoToyPreviewRestoreNoAi"),"Original NoAI restored without temporary flag leakage");
    var block=h.getLevel().getBlockState(f.tv);if(block.is(ToyContent.block(TvWatchRules073.KIND)))h.assertTrue(!block.getValue(ChaoToyBlock.PLAYING)&&block.getValue(ChaoToyBlock.FRAME)==0,"Real TV returns to idle");
  }
  private static void preview(GameTestHelper h,boolean adult,boolean loop){
    var f=fixture(h,adult);String before=f.c.state().encode();long cooldown=f.c.getPersistentData().getLong("ChaoToyNextPlay");
    h.assertTrue(command(f.p,"chao testtoy tv "+(loop?"loop":"once"))==1,"Native dispatcher starts real placed-TV preview");aim(h,f);
    h.runAfterDelay(80,()->{h.assertTrue(ToyPreview.active(f.c)&&f.c.toyTarget().equals(f.tv),"Preview owns the real unchanged TV");aim(h,f);h.assertTrue(CUES.get(f.c).size()==1,"One soft cue in first session");h.assertTrue(f.c.state().encode().equals(before),"Active preview freezes care without rewards");});
    if(!loop)h.runAfterDelay(159,()->h.assertTrue(ToyPreview.active(f.c)&&f.c.state().encode().equals(before),"Care remains exact through the final active preview tick"));
    h.runAfterDelay(loop?330:160,()->{
      if(loop){h.assertTrue(ToyPreview.active(f.c)&&CUES.get(f.c).size()==2,"Explicit loop restarts the same finite clock, with one cue per completed cycle");h.assertTrue(command(f.p,"chao testtoy tv stop")==1,"Native stop command");}
      else if(ToyPreview.active(f.c))ToyPreview.tick(f.c);
      idle(h,f,before,cooldown,false);h.assertTrue(f.p.getMainHandItem().isEmpty(),"Preview neither grants nor consumes keeper items");
      com.mojang.logging.LogUtils.getLogger().info("TV073_PREVIEW_END adult={} loop={} worldTick={} age={} adultAge={} stateExact=true",adult,loop,f.c.level().getGameTime(),f.c.state().ageTicks,f.c.state().adultAgeTicks);
      int cues=CUES.get(f.c).size();h.runAfterDelay(10,()->{
        h.assertTrue(!ToyPreview.active(f.c)&&f.c.toyPlayingTicks()==0&&f.c.toyTarget()==null&&!f.c.isNoAi()&&CUES.get(f.c).size()==cues,"Ordinary idle resumes without leftover preview/audio");
        h.assertTrue(f.c.state().adult==adult,"Ordinary idle does not duplicate evolution");
        com.mojang.logging.LogUtils.getLogger().info("TV073_IDLE_RESUMED adult={} worldTick={} age={} adultAge={} noPreview=true",adult,f.c.level().getGameTime(),f.c.state().ageTicks,f.c.state().adultAgeTicks);h.succeed();
      });
    });
  }
  private static void interrupt(GameTestHelper h,String mode){
    var f=fixture(h,true);boolean noAi=mode.equals("original_noai");f.c.setNoAi(noAi);f.c.setXRot(7);f.c.setYRot(31);f.c.setYHeadRot(34);f.c.setYBodyRot(29);
    String before=f.c.state().encode();long cooldown=f.c.getPersistentData().getLong("ChaoToyNextPlay");
    h.assertTrue(command(f.p,"chao testtoy tv loop")==1,"Start preview before interruption");
    h.runAfterDelay(42,()->{
      switch(mode){
        case "stop","original_noai"->h.assertTrue(command(f.p,"chao testtoy stop")==1,"Explicit operator stop");
        case "damage"->{f.c.setInvulnerable(false);h.assertTrue(f.c.hurt(h.getLevel().damageSources().generic(),1),"Real damage is accepted");}
        case "pet"->{h.assertTrue(f.c.mobInteract(f.p,InteractionHand.MAIN_HAND).consumesAction(),"Actual pet interrupts preview");h.assertTrue(f.c.getAffectionTicks()>0,"Actual pet begins after cleanup");}
        case "remove"->h.getLevel().setBlock(f.tv,Blocks.AIR.defaultBlockState(),3);
        case "rotate"->h.getLevel().setBlock(f.tv,h.getLevel().getBlockState(f.tv).setValue(ChaoToyBlock.FACING,Direction.NORTH),3);
        case "obstruct"->h.getLevel().setBlock(f.tv.south(),Blocks.STONE.defaultBlockState(),3);
        case "displace"->f.c.setPos(f.c.getX()+.6,f.c.getY(),f.c.getZ());
        case "unload"->f.c.discard();
        case "logout"->f.p.server.getPlayerList().remove(f.p);
        case "range"->f.p.teleportTo(f.c.getX()+70,f.c.getY(),f.c.getZ());
        case "reload"->{
          var tag=new CompoundTag();f.c.saveWithoutId(tag);
          h.assertTrue(!tag.getBoolean("NoAI")&&!tag.contains("ToyPlayingKind")&&!tag.contains("ToyPlayingRemaining"),"Fallback save contains ordinary flags and no preview clock");
          h.assertTrue(tag.getList("Rotation",net.minecraft.nbt.Tag.TAG_FLOAT).getFloat(0)==31&&tag.getList("Rotation",net.minecraft.nbt.Tag.TAG_FLOAT).getFloat(1)==7,"Fallback save retains pre-preview orientation");
          f.c.discard();var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(tag);h.getLevel().addFreshEntity(restored);
          h.assertTrue(!ToyPreview.active(restored)&&restored.toyPlayingTicks()==0&&!restored.isNoAi()&&restored.state().encode().equals(before),"Reload does not resume preview or alter saved Chao");restored.discard();
        }
      }
      // Legitimate damage/petting may change care. Cleanup must add no preview reward or mutation.
      String expected=mode.equals("damage")||mode.equals("pet")?f.c.state().encode():before;
      boolean environment=List.of("remove","rotate","obstruct","displace").contains(mode);
      if(ToyPreview.active(f.c))ToyPreview.tick(f.c);
      com.mojang.logging.LogUtils.getLogger().info("TV073_INTERRUPT_REQUEST mode={} worldTick={} active={} remaining={} target={} age={} state={}",mode,f.c.level().getGameTime(),ToyPreview.active(f.c),f.c.toyPlayingTicks(),f.c.toyTarget(),f.c.state().ageTicks,f.c.state().encode());
      Runnable verify=()->{
        if(ToyPreview.active(f.c))ToyPreview.tick(f.c);
        idle(h,f,expected,cooldown,noAi);
        // Once ordinary AI resumes it may legitimately update gaze again.
        if(!environment)h.assertTrue(f.c.getXRot()==7&&f.c.getYRot()==31&&f.c.getYHeadRot()==34&&f.c.yBodyRot==29,"TV preview restores its temporary screen aim at interruption");
        com.mojang.logging.LogUtils.getLogger().info("TV073_INTERRUPT_CLEAN mode={} worldTick={} active={} remaining={} target={} noAi={} age={} stateExact=true",mode,f.c.level().getGameTime(),ToyPreview.active(f.c),f.c.toyPlayingTicks(),f.c.toyTarget(),f.c.isNoAi(),f.c.state().ageTicks);
        int cues=CUES.get(f.c).size();h.runAfterDelay(20,()->{h.assertTrue(CUES.get(f.c).size()==cues&&!ToyPreview.active(f.c)&&f.c.toyPlayingTicks()==0,"No lingering screen/audio/session after interruption");h.succeed();});
      };
      // World changes made after this tick's controller pass validate on its next authoritative tick.
      if(environment)h.runAfterDelay(1,verify);else verify.run();
    });
  }
  private static void reject(GameTestHelper h,String mode){
    var f=fixture(h,false);
    if(mode.equals("rear"))f.c.setPos(f.tv.getX()+.5,f.tv.getY(),f.tv.getZ()-1.5);
    if(mode.equals("blocked"))h.getLevel().setBlock(f.tv.south(),Blocks.STONE.defaultBlockState(),3);
    if(mode.equals("absent"))h.getLevel().setBlock(f.tv,Blocks.AIR.defaultBlockState(),3);
    if(mode.equals("reserved")){
      var other=actor(h,f.tv.south(),false);other.setNoAi(true);other.getPersistentData().putLong("ChaoToyTarget",f.tv.asLong());other.getPersistentData().putLong("ChaoToyApproachUntil",h.getLevel().getGameTime()+100);
    }
    String before=f.c.state().encode();var pos=f.c.position();
    h.assertTrue(command(f.p,"chao testtoy tv once")==0&&!ToyPreview.active(f.c)&&f.c.state().encode().equals(before)&&f.c.position().equals(pos),"Invalid TV preview reports failure without remote movement or saved changes");h.succeed();
  }
  private static void permissions(GameTestHelper h){
    var f=fixture(h,false);String before=f.c.state().encode();
    h.assertTrue(command(f.p,"chao testtoy tv loop",0)<=0&&!ToyPreview.active(f.c)&&f.c.state().encode().equals(before),"Unprivileged command cannot edit or start preview");
    h.assertTrue(command(f.p,"chao testtoy list")==1,"Current command catalog remains available to operators");
    var dispatcher=f.p.server.getCommands().getDispatcher();var suggestions=dispatcher.getCompletionSuggestions(dispatcher.parse("chao testtoy t",f.p.createCommandSourceStack().withPermission(2))).join();
    h.assertTrue(suggestions.getList().stream().anyMatch(s->s.getText().equals("tv")),"tv preview appears in actual command suggestions");
    f.c.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.APPLE));var held=f.c.getMainHandItem().copy();
    h.assertTrue(command(f.p,"chao testtoy tv")==0&&ItemStack.isSameItemSameComponents(held,f.c.getMainHandItem())&&f.c.getMainHandItem().getCount()==1,"Preview never steals a real Chao item");h.succeed();
  }
  private static void naturalInterrupt(GameTestHelper h,boolean rotate){
    var at=floor(h);var tv=ToyContent.block(TvWatchRules073.KIND).defaultBlockState().setValue(ChaoToyBlock.FACING,Direction.SOUTH);h.getLevel().setBlock(at,tv,3);
    var c=actor(h,at.south(),true);c.goalSelector.addGoal(4,new ToyContent.PlayGoal(c));var played=new AtomicBoolean();var changed=new AtomicBoolean();
    h.onEachTick(()->{
      if(!played.get()&&ToyContent.isPlaying(c)){
        played.set(true);
        h.runAfterDelay(40,()->{if(rotate)h.getLevel().setBlock(at,tv.setValue(ChaoToyBlock.FACING,Direction.NORTH),3);else h.getLevel().setBlock(at,Blocks.AIR.defaultBlockState(),3);changed.set(true);});
      }
    });
    h.succeedWhen(()->{
      h.assertTrue(changed.get()&&!ToyContent.isPlaying(c)&&!c.getPersistentData().contains("ChaoToyUntil"),"Invalid screen ends real session");
      h.assertTrue(c.state().happiness==20,"Interrupted session receives no completion reward");
      if(rotate)h.assertTrue(!h.getLevel().getBlockState(at).getValue(ChaoToyBlock.PLAYING),"Rotated TV is idle after cancellation");
    });
  }
}
