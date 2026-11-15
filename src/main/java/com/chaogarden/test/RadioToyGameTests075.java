package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoIllness;
import com.chaogarden.core.ChaoPreferences;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.market.MarketStock;
import com.chaogarden.toys.ChaoToyBlock;
import com.chaogarden.toys.RadioPlayRules075;
import com.chaogarden.toys.ToyContent;
import com.chaogarden.toys.ToyPreview;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
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

/** Actual radio navigation, finite block-owned cues and reward-free operator controls.
 * These assertions do not establish visual quality, perceived loudness or remote reconnects. */
@GameTestHolder("chao_radio75") @PrefixGameTestTemplate(false) @EventBusSubscriber(modid=ChaoGarden.MODID)
public final class RadioToyGameTests075 {
  private static final String KIND="chao_radio", TEMPLATE="chao_radio75:activity_test";
  private record Cue(long tick,float volume,float pitch){}
  private record Fixture(ChaoEntity c,ServerPlayer p,BlockPos radio){}
  private static final Map<ChaoEntity,List<Cue>> CUES=new WeakHashMap<>();
  private static final Map<ChaoEntity,List<Long>> VOICES=new WeakHashMap<>();
  private static final Map<ChaoEntity,BlockPos> SOURCES=new WeakHashMap<>();
  private RadioToyGameTests075(){}

  @SubscribeEvent public static void positionedSound(PlayLevelSoundEvent.AtPosition e){
    if(e.getSound()!=null)record(e.getLevel(),e.getPosition(),e.getSound().value().getLocation().toString(),e.getNewVolume(),e.getNewPitch());
  }
  @SubscribeEvent public static void entitySound(PlayLevelSoundEvent.AtEntity e){
    if(e.getSound()!=null)record(e.getLevel(),e.getEntity().position(),e.getSound().value().getLocation().toString(),e.getNewVolume(),e.getNewPitch());
  }
  private static void record(net.minecraft.world.level.Level level,Vec3 position,String sound,float volume,float pitch){
    if(level.isClientSide)return;
    if(sound.equals("minecraft:block.note_block.harp"))CUES.forEach((c,list)->{
      if(c.level()==level&&BlockPos.containing(position).equals(SOURCES.get(c)))list.add(new Cue(level.getGameTime(),volume,pitch));
    });
    if(sound.equals("chao_garden:chao.happy"))VOICES.forEach((c,list)->{
      if(c.level()==level&&c.position().distanceToSqr(position)<.1)list.add(level.getGameTime());
    });
  }

  @GameTestGenerator public static Collection<TestFunction> cases(){
    var out=new ArrayList<TestFunction>();
    for(boolean adult:new boolean[]{false,true})for(Direction facing:Direction.Plane.HORIZONTAL)for(Direction start:Direction.Plane.HORIZONTAL)
      add(out,"radio_approach_"+(adult?"adult":"newborn")+"_front_"+facing+"_from_"+start,2100,h->natural(h,adult,facing,start,false));
    for(Direction facing:Direction.Plane.HORIZONTAL)add(out,"radio_newborn_rear_alcove_"+facing,2100,h->natural(h,false,facing,facing.getOpposite(),true));
    for(boolean adult:new boolean[]{false,true})for(boolean loop:new boolean[]{false,true})
      add(out,"radio_preview_"+(adult?"adult":"baby")+"_"+(loop?"loop":"once"),430,h->preview(h,adult,loop));
    for(String mode:List.of("stop","damage","pet","remove","rotate","obstruct","displace","unload","logout","range","reload","original_noai","missing_cooldown"))
      add(out,"radio_preview_interrupt_"+mode,260,h->interrupt(h,mode));
    for(String mode:List.of("rear","blocked","absent","reserved","carrying","water"))add(out,"radio_preview_rejects_"+mode,80,h->reject(h,mode));
    add(out,"radio_native_permissions_and_catalog",100,RadioToyGameTests075::permissions);
    add(out,"radio_same_block_reservation_has_one_owner",310,RadioToyGameTests075::reservation);
    add(out,"radio_nearby_simultaneous_sources_share_quiet_window",250,h->neighbours(h,4,0));
    add(out,"radio_nearby_staggered_sources_do_not_overlap",280,h->neighbours(h,4,32));
    add(out,"radio_distant_sources_keep_independent_phrases",250,h->neighbours(h,15,0));
    add(out,"radio_duplicate_ticks_and_skipped_clock_cannot_burst",100,RadioToyGameTests075::clockJumps);
    add(out,"radio_idle_and_stale_saved_frames_remain_silent",80,RadioToyGameTests075::stale);
    add(out,"radio_existing_item_state_placement_and_single_drop",120,RadioToyGameTests075::compatibility);
    add(out,"radio_actual_tracked_clock_and_entity_save_reload",150,RadioToyGameTests075::replication);
    for(String mode:List.of("remove","damage","rotate"))add(out,"radio_natural_interrupt_"+mode,700,h->naturalInterrupt(h,mode));
    return out;
  }
  private static void add(List<TestFunction> tests,String name,int timeout,java.util.function.Consumer<GameTestHelper> run){
    tests.add(new TestFunction("radio075_"+(tests.size()/5),name,TEMPLATE,timeout,0,true,run));
  }
  private static BlockPos floor(GameTestHelper h){
    for(int x=1;x<=23;x++)for(int z=1;z<=23;z++){
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
  private static ChaoEntity actor(GameTestHelper h,Vec3 point,boolean adult,BlockPos radio){
    var c=ChaoGarden.CHAO.get().create(h.getLevel());h.assertTrue(c!=null,"Production Chao creates");
    var s=new ChaoState();s.adult=adult;s.align=adult?-1:0;s.primaryType=adult?ChaoState.RUN_TYPE:ChaoState.NORMAL;
    s.hunger=90;s.energy=100;s.happiness=20;s.boredom=100;s.personality=ChaoPreferences.EASILY_BORED;
    if(adult){s.levels[ChaoState.RUN]=9;s.progress[ChaoState.RUN]=60;s.points[ChaoState.RUN]=175;s.setEvolutionStrength(1.2f);}
    for(int i=0;i<ChaoIllness.COUNT;i++)s.setIllness(i,100);
    c.setState(ChaoState.decode(s.encode()));c.setInvulnerable(true);c.getRandom().setSeed(18);c.moveTo(point);c.setOnGround(true);
    h.getLevel().addFreshEntity(c);c.goalSelector.removeAllGoals(g->true);c.targetSelector.removeAllGoals(g->true);
    CUES.put(c,new ArrayList<>());VOICES.put(c,new ArrayList<>());SOURCES.put(c,radio.immutable());
    cleanup(h,()->{ToyPreview.stop(c);CUES.remove(c);VOICES.remove(c);SOURCES.remove(c);c.discard();});return c;
  }
  private static void place(GameTestHelper h,BlockPos at,Direction facing){
    h.getLevel().setBlock(at,ToyContent.block(KIND).defaultBlockState().setValue(ChaoToyBlock.FACING,facing),3);
  }
  private static Fixture fixture(GameTestHelper h,boolean adult){
    var at=floor(h);place(h,at,Direction.SOUTH);
    var c=actor(h,Vec3.atBottomCenterOf(at.south()),adult,at);var anchor=ToyContent.radioAnchor(c,at);c.moveTo(anchor);c.setOnGround(true);
    c.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.CARVED_PUMPKIN));c.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.STICK));
    c.goalSelector.addGoal(7,new Goal(){ {setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));} public boolean canUse(){return false;} });
    c.getPersistentData().putLong("ChaoToyNextPlay",h.getLevel().getGameTime()+12345);
    var p=TestConnections.makeServerPlayer(h);p.teleportTo(c.getX()+2,c.getY(),c.getZ());p.getInventory().setItem(12,new ItemStack(Items.DIAMOND,3));c.owner=p.getUUID();
    cleanup(h,()->{if(p.server.getPlayerList().getPlayer(p.getUUID())!=null)p.server.getPlayerList().remove(p);});return new Fixture(c,p,at);
  }
  private static int command(ServerPlayer p,String text,int permission){
    try{return p.server.getCommands().getDispatcher().execute(text,p.createCommandSourceStack().withPermission(permission));}
    catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){return -1;}
  }
  private static int command(ServerPlayer p,String text){return command(p,text,2);}
  private static void facing(GameTestHelper h,ChaoEntity c,BlockPos radio){
    var toward=ToyContent.radioSpeaker(c,radio).subtract(c.position());var look=Vec3.directionFromRotation(0,c.getYRot());
    double dot=look.x*toward.x+look.z*toward.z;h.assertTrue(dot/Math.hypot(toward.x,toward.z)>.995,"Body faces actual front speaker");
    h.assertTrue(Float.isFinite(c.getXRot())&&Math.abs(c.getXRot())<=30.001f,"Speaker gaze remains finite and bounded");
  }
  private static void physical(GameTestHelper h,ChaoEntity c,BlockPos radio){
    h.assertTrue(c.distanceToSqr(ToyContent.radioAnchor(c,radio))<.015*.015,"Radio session uses its calibrated front seat");
    var delta=c.position().subtract(Vec3.atBottomCenterOf(radio));var front=h.getLevel().getBlockState(radio).getValue(ChaoToyBlock.FACING);
    h.assertTrue(Math.abs(delta.horizontalDistance()-.60)<.015&&Math.abs(delta.y)<.015,"Literal close-seat center distance and floor height remain fixed");
    h.assertTrue(delta.x*front.getStepX()+delta.z*front.getStepZ()>.585&&Math.abs(delta.x*front.getStepZ()-delta.z*front.getStepX())<.015,"Listener occupies the actual front side with no lateral drift");
    h.assertTrue(h.getLevel().noCollision(c,c.getBoundingBox()),"Listener does not enter the physical radio");facing(h,c,radio);
  }
  private static void identity(GameTestHelper h,ChaoEntity c,boolean adult){
    h.assertTrue(c.state().adult==adult,"Toy does not change biological stage");
    if(adult)h.assertTrue(c.state().lockedAlignment()==-1&&c.state().lockedPrimaryType()==ChaoState.RUN_TYPE,"Adult first alignment and type remain locked");
    else h.assertTrue(c.state().primaryType==ChaoState.NORMAL,"Newborn remains unevolved while ordinary walking may change prospective type");
  }
  private static void quiet(GameTestHelper h,ChaoEntity c){
    for(var q:CUES.get(c))h.assertTrue(q.volume>0&&q.volume<=.061f&&Float.isFinite(q.pitch),"Radio uses quiet finite note source values");
  }
  private static void phrase(GameTestHelper h,ChaoEntity c,long start,int expected){
    var cues=CUES.get(c);h.assertTrue(cues.size()==expected,"Expected finite radio phrase count: "+cues.size()+" versus "+expected);quiet(h,c);
    for(int i=0;i<expected;i++){
      var cue=cues.get(i);long wanted=start+(i/4)*160+20+(i%4)*32;
      h.assertTrue(cue.tick>=wanted&&cue.tick<=wanted+1,"Radio note follows owned session clock, not world modulo: "+cue.tick+" versus "+wanted);
      h.assertTrue(Math.abs(cue.pitch-RadioPlayRules075.cuePitch(i%4))<.00001,"Authored phrase pitch survives actual sound dispatch");
    }
  }
  private static void natural(GameTestHelper h,boolean adult,Direction front,Direction start,boolean alcove){
    var at=floor(h);place(h,at,front);var level=h.getLevel();var side=front.getClockWise();
    if(alcove){
      level.setBlock(at.above(),Blocks.STONE.defaultBlockState(),3);
      for(var cell:List.of(at.relative(side),at.relative(side).relative(front.getOpposite())))for(int y=0;y<2;y++)level.setBlock(cell.above(y),Blocks.STONE.defaultBlockState(),3);
    }
    var c=actor(h,Vec3.atBottomCenterOf(at.relative(start,alcove?3:2)),adult,at);c.goalSelector.addGoal(4,new ToyContent.PlayGoal(c));
    var played=new AtomicBoolean();var first=new AtomicLong(-1);var previous=new AtomicReference<>(c.position());var lateral=new double[]{0};
    h.onEachTick(()->{
      h.assertTrue(c.position().distanceTo(previous.get())<.7,"Natural radio approach walks instead of remotely relocating");previous.set(c.position());
      var delta=c.position().subtract(Vec3.atBottomCenterOf(at));lateral[0]=Math.max(lateral[0],Math.abs(delta.x*side.getStepX()+delta.z*side.getStepZ()));
      var path=c.getNavigation().getPath();if(path!=null)for(int i=0;i<path.getNodeCount();i++){
        var node=path.getNode(i);h.assertTrue(node.x!=at.getX()||node.z!=at.getZ()||node.y<at.getY()||node.y>at.getY()+1,"Natural route avoids climbing through/on the radio");
      }
      if(ToyContent.isPlaying(c)){
        played.set(true);first.compareAndSet(-1,c.getPersistentData().getLong("ChaoToyUntil")-160);physical(h,c,at);identity(h,c,adult);
        int t=ToyContent.sessionElapsed(level,at),frame=level.getBlockState(at).getValue(ChaoToyBlock.FRAME);
        if(t>=22&&t<58)h.assertTrue(frame==1,"Settled radio first frame follows the owned clock");
        if(t>=62&&t<98)h.assertTrue(frame==2,"Radio program advances without a repeated model bob");
        if(t>=102&&t<138)h.assertTrue(frame==3,"Radio third frame holds through final phrase");
        if(t>=142)h.assertTrue(frame==0,"Recovery parks the physical model");
      }
    });
    h.succeedWhen(()->{
      h.assertTrue(played.get()&&!ToyContent.isPlaying(c),"Natural listening completes");h.assertTrue(c.state().happiness==24,"Exactly one ordinary completion reward");
      phrase(h,c,first.get(),4);h.assertTrue(VOICES.get(c).isEmpty(),"Radio start does not add a disruptive happy shout");
      if(start==front.getOpposite())h.assertTrue(lateral[0]>.55,"Rear route detours around radio instead of stepping over it");
      idleBlock(h,at);identity(h,c,adult);
    });
  }
  private static void idleBlock(GameTestHelper h,BlockPos at){
    var s=h.getLevel().getBlockState(at);if(s.is(ToyContent.block(KIND)))h.assertTrue(!s.getValue(ChaoToyBlock.PLAYING)&&s.getValue(ChaoToyBlock.FRAME)==0,"One real radio returns to idle");
  }
  private static void idle(GameTestHelper h,Fixture f,String before,long cooldown,boolean hadCooldown,boolean noAi){
    h.assertTrue(!ToyPreview.active(f.c)&&f.c.toyPlayingTicks()==0&&f.c.toyTarget()==null&&!f.c.getPersistentData().contains("ChaoToyTarget")&&!f.c.getPersistentData().contains("ChaoToyUntil"),"Preview releases real reservation and tracked clock");
    h.assertTrue(f.c.state().encode().equals(before),"Preview preserves saved care, genes, stats and age through authoritative cleanup");
    h.assertTrue(f.c.isNoAi()==noAi&&!f.c.getPersistentData().contains("ChaoToyPreviewRestoreNoAi"),"Original NoAI restored without temporary flag");
    h.assertTrue(f.c.getPersistentData().contains("ChaoToyNextPlay")==hadCooldown&&f.c.getPersistentData().getLong("ChaoToyNextPlay")==cooldown,"Prior cooldown presence and value restored");
    h.assertTrue(f.c.getItemBySlot(EquipmentSlot.HEAD).is(Items.CARVED_PUMPKIN)&&f.c.getItemBySlot(EquipmentSlot.OFFHAND).is(Items.STICK)&&f.p.getInventory().getItem(12).is(Items.DIAMOND)&&f.p.getInventory().getItem(12).getCount()==3,"Preview keeps actual Chao equipment and keeper inventory");idleBlock(h,f.radio);
  }
  private static void preview(GameTestHelper h,boolean adult,boolean loop){
    var f=fixture(h,adult);var before=f.c.state().encode();long cooldown=f.c.getPersistentData().getLong("ChaoToyNextPlay");var goals=Set.copyOf(f.c.goalSelector.getAvailableGoals());long start=h.getLevel().getGameTime();
    h.assertTrue(command(f.p,"chao testtoy radio "+(loop?"loop":"once"))==1,"Actual operator dispatcher starts real radio preview");physical(h,f.c,f.radio);
    h.runAfterDelay(80,()->{h.assertTrue(ToyPreview.active(f.c)&&f.c.state().encode().equals(before)&&f.c.toyTarget().equals(f.radio),"Active radio preview pauses saved simulation and owns original radio");physical(h,f.c,f.radio);h.assertTrue(CUES.get(f.c).size()==2,"Only first two quiet phrase notes before80t");});
    if(!loop)h.runAfterDelay(159,()->h.assertTrue(ToyPreview.active(f.c)&&f.c.state().encode().equals(before),"Saved state stays exact through final active preview tick"));
    h.runAfterDelay(loop?330:160,()->{
      if(loop){h.assertTrue(ToyPreview.active(f.c),"Loop renews actual session");h.assertTrue(command(f.p,"chao testtoy radio stop")==1,"Native stop ends radio loop");}
      else if(ToyPreview.active(f.c))ToyPreview.tick(f.c);
      idle(h,f,before,cooldown,true,false);phrase(h,f.c,start,loop?8:4);h.assertTrue(goals.equals(f.c.goalSelector.getAvailableGoals()),"Preview preserves original goal objects and priorities");h.assertTrue(VOICES.get(f.c).isEmpty(),"No session-start happy shout");
      int n=CUES.get(f.c).size();h.runAfterDelay(12,()->{h.assertTrue(!ToyPreview.active(f.c)&&f.c.toyPlayingTicks()==0&&CUES.get(f.c).size()==n,"Idle has no lingering animation clock or notes");h.succeed();});
    });
  }
  private static void interrupt(GameTestHelper h,String mode){
    var f=fixture(h,true);boolean noAi=mode.equals("original_noai");f.c.setNoAi(noAi);if(mode.equals("missing_cooldown"))f.c.getPersistentData().remove("ChaoToyNextPlay");
    f.c.setYRot(31);f.c.setXRot(7);f.c.setYHeadRot(34);f.c.yBodyRot=29;
    boolean had=f.c.getPersistentData().contains("ChaoToyNextPlay");long cooldown=f.c.getPersistentData().getLong("ChaoToyNextPlay");String before=f.c.state().encode();
    h.assertTrue(command(f.p,"chao testtoy radio loop")==1,"Start production radio preview before interruption");
    h.runAfterDelay(70,()->{
      switch(mode){
        case "stop","original_noai","missing_cooldown"->h.assertTrue(command(f.p,"chao testtoy radio stop")==1,"Actual explicit stop accepted");
        case "damage"->{f.c.setInvulnerable(false);h.assertTrue(f.c.hurt(h.getLevel().damageSources().generic(),1),"Real damage interrupts");}
        case "pet"->{h.assertTrue(f.c.mobInteract(f.p,InteractionHand.MAIN_HAND).consumesAction(),"Actual pet interrupts");h.assertTrue(f.c.getAffectionTicks()>0,"Pet starts after preview cleanup");}
        case "remove"->h.getLevel().destroyBlock(f.radio,false);
        case "rotate"->h.getLevel().setBlock(f.radio,h.getLevel().getBlockState(f.radio).setValue(ChaoToyBlock.FACING,Direction.NORTH),3);
        case "obstruct"->h.getLevel().setBlock(f.radio.south(),Blocks.STONE.defaultBlockState(),3);
        case "displace"->f.c.setPos(f.c.getX()+2,f.c.getY(),f.c.getZ());
        case "unload"->f.c.discard();
        case "logout"->f.p.server.getPlayerList().remove(f.p);
        case "range"->f.p.teleportTo(f.c.getX()+70,f.c.getY(),f.c.getZ());
        case "reload"->{
          var tag=new CompoundTag();f.c.saveWithoutId(tag);h.assertTrue(!tag.getBoolean("NoAI")&&!tag.contains("ToyPlayingKind")&&!tag.contains("ToyPlayingRemaining"),"Preview save writes ordinary flags and no resumable preview");
          h.assertTrue(tag.getList("Rotation",net.minecraft.nbt.Tag.TAG_FLOAT).getFloat(0)==31&&tag.getList("Rotation",net.minecraft.nbt.Tag.TAG_FLOAT).getFloat(1)==7,"Save fallback preserves original orientation");
          f.c.discard();var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(tag);h.getLevel().addFreshEntity(restored);
          h.assertTrue(!ToyPreview.active(restored)&&restored.toyPlayingTicks()==0&&!restored.isNoAi()&&restored.state().encode().equals(before),"Entity reload cannot resume radio preview or change saved Chao");restored.discard();
        }
      }
      String expected=mode.equals("damage")||mode.equals("pet")?f.c.state().encode():before;
      boolean environment=List.of("remove","rotate","obstruct","displace").contains(mode);
      Runnable verify=()->{
        if(ToyPreview.active(f.c))ToyPreview.tick(f.c);idle(h,f,expected,cooldown,had,noAi);
        if(!environment)h.assertTrue(f.c.getXRot()==7&&f.c.getYRot()==31&&f.c.getYHeadRot()==34&&f.c.yBodyRot==29,"Synchronous cleanup restores temporary radio aim");
        int n=CUES.get(f.c).size();h.runAfterDelay(24,()->{h.assertTrue(!ToyPreview.active(f.c)&&f.c.toyPlayingTicks()==0&&CUES.get(f.c).size()==n,"No late radio notes after interruption");h.succeed();});
      };
      if(environment)h.runAfterDelay(1,verify);else verify.run();
    });
  }
  private static void reject(GameTestHelper h,String mode){
    var f=fixture(h,false);
    switch(mode){
      case "rear"->f.c.setPos(f.radio.getX()+.5,f.radio.getY(),f.radio.getZ()-.6);
      case "blocked"->h.getLevel().setBlock(f.radio.south(),Blocks.STONE.defaultBlockState(),3);
      case "absent"->h.getLevel().destroyBlock(f.radio,false);
      case "reserved"->{var other=actor(h,Vec3.atBottomCenterOf(f.radio.south(2)),true,f.radio);other.setNoAi(true);other.getPersistentData().putLong("ChaoToyTarget",f.radio.asLong());other.getPersistentData().putLong("ChaoToyApproachUntil",h.getLevel().getGameTime()+100);}
      case "carrying"->f.c.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.APPLE));
      case "water"->h.getLevel().setBlock(f.radio.south(),Blocks.WATER.defaultBlockState(),3);
    }
    String before=f.c.state().encode();var point=f.c.position();var held=f.c.getMainHandItem().copy();
    h.assertTrue(command(f.p,"chao testtoy radio once")<=0&&!ToyPreview.active(f.c)&&f.c.state().encode().equals(before)&&f.c.position().equals(point),"Invalid radio preview refuses instead of stealing/moving/minting");
    h.assertTrue(ItemStack.isSameItemSameComponents(held,f.c.getMainHandItem())&&held.getCount()==f.c.getMainHandItem().getCount(),"Rejected preview preserves real held item");h.succeed();
  }
  private static void permissions(GameTestHelper h){
    var f=fixture(h,false);String before=f.c.state().encode();h.assertTrue(command(f.p,"chao testtoy radio once",0)<=0&&!ToyPreview.active(f.c)&&f.c.state().encode().equals(before),"Unprivileged source cannot begin/edit radio");
    var survival=TestConnections.makeSurvivalServerPlayer(h);survival.teleportTo(f.p.getX(),f.p.getY(),f.p.getZ());cleanup(h,()->{if(survival.server.getPlayerList().getPlayer(survival.getUUID())!=null)survival.server.getPlayerList().remove(survival);});
    h.assertTrue(command(survival,"chao testtoy radio loop",0)<=0&&!ToyPreview.active(f.c),"Ordinary Survival source remains unauthorized");
    h.assertTrue(command(f.p,"chao testtoy list")==1,"Operator catalog works");var dispatcher=f.p.server.getCommands().getDispatcher();
    var node=dispatcher.getRoot().getChild("chao").getChild("testtoy").getChild("radio");h.assertTrue(node!=null&&node.getChild("once")!=null&&node.getChild("loop")!=null&&node.getChild("stop")!=null,"Real native command tree has all radio controls");
    var suggestions=dispatcher.getCompletionSuggestions(dispatcher.parse("chao testtoy r",f.p.createCommandSourceStack().withPermission(2))).join();h.assertTrue(suggestions.getList().stream().anyMatch(s->s.getText().equals("radio")),"Radio appears in actual suggestions");h.succeed();
  }
  private static void reservation(GameTestHelper h){
    var f=fixture(h,true);var seat=f.c.position();var waiting=seat.add(-2,0,0);
    h.assertTrue(command(f.p,"chao testtoy radio once")==1,"First listener claims radio");
    var second=actor(h,waiting,false,f.radio);second.setNoAi(true);String before=second.state().encode();
    // Isolate ownership from physical occupancy without allowing a world tick
    // in the temporary arrangement. Natural approach is covered separately.
    f.c.setPos(seat.x+2,seat.y,seat.z);second.setPos(seat.x,seat.y,seat.z);
    h.assertTrue(ToyPreview.active(f.c)&&f.radio.equals(f.c.toyTarget())&&h.getLevel().noCollision(second,second.getBoundingBox()),"Claim persists while the physical listening seat is clear");
    h.assertTrue(ToyContent.previewRadio(second)==null,"Public radio selection rejects a second owner of the same real block");
    h.assertTrue(!ToyPreview.start(second,f.p,"radio",false).isEmpty()&&!ToyPreview.active(second)&&second.state().encode().equals(before),"Second listener cannot steal occupied source or mutate saved state");
    f.c.setPos(seat.x,seat.y,seat.z);second.setPos(waiting.x,waiting.y,waiting.z);
    h.runAfterDelay(160,()->{
      if(ToyPreview.active(f.c))ToyPreview.tick(f.c);h.assertTrue(CUES.get(f.c).size()==4,"One block ownership produces one finite phrase");
      f.c.setNoAi(true);f.c.setPos(f.c.getX()-3,f.c.getY(),f.c.getZ());
      second.setPos(seat.x,seat.y,seat.z);second.setOnGround(true);
      String secondBefore=second.state().encode();h.assertTrue(f.radio.equals(ToyContent.previewRadio(second)),"After the first actor clears the seat, public selection sees the released radio");
      h.assertTrue(ToyPreview.start(second,f.p,"radio",false).isEmpty()&&ToyPreview.active(second)&&f.radio.equals(second.toyTarget()),"Second listener can actually begin on the released real source");
      ToyPreview.stop(second);h.assertTrue(second.isNoAi()&&second.state().encode().equals(secondBefore),"Reservation handoff adds no preview care reward or ordinary AI mutation");h.succeed();
    });
  }
  private static void neighbours(GameTestHelper h,int distance,int offset){
    var origin=floor(h);var a=origin.west(6);var b=a.east(distance);place(h,a,Direction.SOUTH);place(h,b,Direction.SOUTH);
    var one=actor(h,Vec3.atBottomCenterOf(a.south()),true,a);var two=actor(h,Vec3.atBottomCenterOf(b.south()),true,b);
    one.moveTo(ToyContent.radioAnchor(one,a));two.moveTo(ToyContent.radioAnchor(two,b));one.setOnGround(true);two.setOnGround(true);
    var p=TestConnections.makeServerPlayer(h);p.teleportTo(one.getX()+2,one.getY(),one.getZ());cleanup(h,()->{if(p.server.getPlayerList().getPlayer(p.getUUID())!=null)p.server.getPlayerList().remove(p);});
    h.assertTrue(ToyPreview.start(one,p,"radio",false).isEmpty(),"First real radio begins");
    Runnable begin=()->h.assertTrue(ToyPreview.start(two,p,"radio",false).isEmpty(),"Second real radio begins without shared ownership");if(offset==0)begin.run();else h.runAfterDelay(offset,begin);
    h.runAfterDelay(160+offset,()->{
      if(ToyPreview.active(one))ToyPreview.tick(one);if(ToyPreview.active(two))ToyPreview.tick(two);
      var events=new ArrayList<Cue>();events.addAll(CUES.get(one));events.addAll(CUES.get(two));events.sort(Comparator.comparingLong(Cue::tick));quiet(h,one);quiet(h,two);
      h.assertTrue(!ToyPreview.active(one)&&!ToyPreview.active(two),"Both finite sessions release independently");
      if(distance<12){h.assertTrue(events.size()==(offset==0?4:5),"Nearby radios share finite spatial admission instead of doubling notes: "+events.size());for(int i=1;i<events.size();i++)h.assertTrue(events.get(i).tick-events.get(i-1).tick>=32,"No overlapping nearby radio note sources within32ticks");}
      else h.assertTrue(CUES.get(one).size()==4&&CUES.get(two).size()==4,"Distant radios retain independent full phrases rather than a global mute");
      idleBlock(h,a);idleBlock(h,b);h.succeed();
    });
  }
  private static void clockJumps(GameTestHelper h){
    var f=fixture(h,true);h.assertTrue(command(f.p,"chao testtoy radio once")==1,"Real radio owns clock probe");long now=h.getLevel().getGameTime();
    f.c.getPersistentData().putLong("ChaoToyUntil",now+140);f.c.setToyPlaying(KIND,140);
    for(int i=0;i<5;i++)h.getLevel().getBlockState(f.radio).tick(h.getLevel(),f.radio,RandomSource.create(75+i));
    h.assertTrue(CUES.get(f.c).size()==1,"Duplicate processing at an actual cue tick emits once");
    f.c.getPersistentData().putLong("ChaoToyUntil",now+90);f.c.setToyPlaying(KIND,90);
    h.getLevel().getBlockState(f.radio).tick(h.getLevel(),f.radio,RandomSource.create(85));
    h.assertTrue(CUES.get(f.c).size()==1,"Forward resynchronization to70t does not backfill missed notes as a burst");
    h.assertTrue(command(f.p,"chao testtoy radio stop")==1,"Stop clock fixture");int count=CUES.get(f.c).size();h.runAfterDelay(40,()->{h.assertTrue(CUES.get(f.c).size()==count,"No delayed catch-up notes after stop");h.succeed();});
  }
  private static void stale(GameTestHelper h){
    var at=floor(h);place(h,at,Direction.WEST);var c=actor(h,Vec3.atBottomCenterOf(at.west(2)),true,at);c.setNoAi(true);
    h.getLevel().setBlock(at,h.getLevel().getBlockState(at).setValue(ChaoToyBlock.PLAYING,true).setValue(ChaoToyBlock.FRAME,3),3);h.getLevel().scheduleTick(at,ToyContent.block(KIND),1);
    h.runAfterDelay(5,()->{idleBlock(h,at);h.assertTrue(CUES.get(c).isEmpty()&&VOICES.get(c).isEmpty(),"Stale active import returns idle without playing or rewarding");h.assertTrue(c.state().happiness==20,"Unowned block never rewards a Chao");h.runAfterDelay(35,()->{h.assertTrue(CUES.get(c).isEmpty(),"Idle radio never loops by itself");h.succeed();});});
  }
  private static AABB arena(GameTestHelper h){return new AABB(Vec3.atLowerCornerOf(h.absolutePos(BlockPos.ZERO)),Vec3.atLowerCornerOf(h.absolutePos(new BlockPos(24,7,24))));}
  private static int drops(GameTestHelper h){return h.getLevel().getEntitiesOfClass(ItemEntity.class,arena(h),e->e.getItem().is(ChaoGarden.item(KIND))).stream().mapToInt(e->e.getItem().getCount()).sum();}
  private static void compatibility(GameTestHelper h){
    var at=floor(h);var item=ChaoGarden.item(KIND);h.assertTrue(item instanceof BlockItem&&((BlockItem)item).getBlock()==ToyContent.block(KIND)&&MarketStock.TOYS.contains(KIND),"Existing block/item/acquisition identity remains");
    h.assertTrue(ToyContent.block(KIND).defaultBlockState().getValue(ChaoToyBlock.FACING)==Direction.NORTH&&!ToyContent.block(KIND).defaultBlockState().getValue(ChaoToyBlock.PLAYING)&&ToyContent.block(KIND).defaultBlockState().getValue(ChaoToyBlock.FRAME)==0,"Existing idle defaults remain");
    for(Direction facing:Direction.Plane.HORIZONTAL){place(h,at,facing);var state=h.getLevel().getBlockState(at);h.assertTrue(state.getValue(ChaoToyBlock.FACING)==facing&&!state.getCollisionShape(h.getLevel(),at).isEmpty(),"All old oriented physical states remain valid");}
    h.getLevel().destroyBlock(at,true);h.runAfterDelay(3,()->{h.assertTrue(h.getLevel().getBlockState(at).isAir()&&drops(h)==1,"Breaking radio produces exactly its one original item");h.assertTrue(h.getLevel().getEntitiesOfClass(Display.ItemDisplay.class,arena(h)).isEmpty(),"No loose demonstration radio duplicates");h.succeed();});
  }
  private static void replication(GameTestHelper h){
    var f=fixture(h,true);String before=f.c.state().encode();h.assertTrue(command(f.p,"chao testtoy radio once")==1,"Start actual radio before tracked/save checks");
    h.runAfterDelay(64,()->{
      var values=f.c.getEntityData().getNonDefaultValues();h.assertTrue(values!=null,"Actual tracked data contains active radio");var observed=ChaoGarden.CHAO.get().create(h.getLevel());observed.getEntityData().assignValues(values);
      h.assertTrue(observed.toyPlayingKind().equals(KIND)&&observed.toyPlayingTicks()==f.c.toyPlayingTicks()&&f.radio.equals(observed.toyTarget()),"Tracked observer receives authoritative radio kind, deadline and physical target");observed.discard();
      var tag=new CompoundTag();f.c.saveWithoutId(tag);h.assertTrue(!tag.getBoolean("NoAI")&&!tag.contains("ToyPlayingKind"),"Active preview serializes ordinary AI and no resumable toy");
      h.assertTrue(command(f.p,"chao testtoy radio stop")==1,"Stop before entity reload");f.c.discard();var copy=ChaoGarden.CHAO.get().create(h.getLevel());copy.load(tag);h.getLevel().addFreshEntity(copy);
      h.assertTrue(copy.state().encode().equals(before)&&!copy.isNoAi()&&!ToyPreview.active(copy)&&copy.toyPlayingTicks()==0&&copy.toyTarget()==null,"Reload keeps locked identity/care/equipment without restarting audio or toy ownership");
      h.assertTrue(copy.getItemBySlot(EquipmentSlot.HEAD).is(Items.CARVED_PUMPKIN)&&copy.getItemBySlot(EquipmentSlot.OFFHAND).is(Items.STICK),"Actual equipment survives save/load");copy.discard();
      int n=CUES.get(f.c).size();h.runAfterDelay(20,()->{idleBlock(h,f.radio);h.assertTrue(CUES.get(f.c).size()==n,"Reload cannot produce extra radio notes");h.succeed();});
    });
  }
  private static void naturalInterrupt(GameTestHelper h,String mode){
    var at=floor(h);place(h,at,Direction.SOUTH);var c=actor(h,Vec3.atBottomCenterOf(at.south()),true,at);c.goalSelector.addGoal(4,new ToyContent.PlayGoal(c));var done=new AtomicBoolean();
    h.onEachTick(()->{
      if(done.get()||!ToyContent.isPlaying(c)||ToyContent.sessionElapsed(h.getLevel(),at)<65)return;done.set(true);
      if(mode.equals("remove"))h.getLevel().destroyBlock(at,true);
      if(mode.equals("rotate"))h.getLevel().setBlock(at,h.getLevel().getBlockState(at).setValue(ChaoToyBlock.FACING,Direction.NORTH),3);
      if(mode.equals("damage")){c.setInvulnerable(false);h.assertTrue(c.hurt(h.getLevel().damageSources().generic(),1),"Real natural-play damage accepted");}
      int happiness=c.state().happiness,n=CUES.get(c).size();h.runAfterDelay(14,()->{
        h.assertTrue(!ToyContent.isPlaying(c)&&c.toyPlayingTicks()==0&&c.state().happiness==happiness,"Interrupted natural radio releases without completion reward");
        idleBlock(h,at);h.assertTrue(CUES.get(c).size()==n&&drops(h)==(mode.equals("remove")?1:0),"Interruption has no delayed phrase or item duplication");h.succeed();
      });
    });
  }
}
