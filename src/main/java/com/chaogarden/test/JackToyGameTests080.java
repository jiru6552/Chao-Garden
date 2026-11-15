package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoIllness;
import com.chaogarden.core.ChaoPreferences;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real placed ownership, front navigation, quiet pop, and native preview transactions.
 * These tests are not a visual/listening or independent remote-client pass. */
@GameTestHolder("chao_jack80") @PrefixGameTestTemplate(false) @EventBusSubscriber(modid=ChaoGarden.MODID)
public final class JackToyGameTests080 {
  private static final String KIND=JackPlayRules080.KIND,TEMPLATE="chao_jack80:activity_test";
  private record Cue(long tick,float volume,float pitch){}
  private record Fixture(ChaoEntity c,ServerPlayer p,BlockPos at){}
  private static final Map<ChaoEntity,List<Cue>> CUES=new WeakHashMap<>();
  private static final Map<ChaoEntity,BlockPos> SOURCES=new WeakHashMap<>();
  private JackToyGameTests080(){}
  @SubscribeEvent public static void sound(PlayLevelSoundEvent.AtPosition e){
    if(e.getLevel().isClientSide||e.getSound()==null||!e.getSound().value().getLocation().toString().equals("minecraft:block.note_block.pling"))return;
    CUES.forEach((c,list)->{if(c.level()==e.getLevel()&&BlockPos.containing(e.getPosition()).equals(SOURCES.get(c)))
      list.add(new Cue(e.getLevel().getGameTime(),e.getNewVolume(),e.getNewPitch()));});
  }
  @GameTestGenerator public static Collection<TestFunction> cases(){
    var out=new ArrayList<TestFunction>();
    for(boolean adult:new boolean[]{false,true})for(Direction front:Direction.Plane.HORIZONTAL)for(Direction from:Direction.Plane.HORIZONTAL)
      add(out,"jack_approach_"+(adult?"adult":"baby")+"_"+front+"_from_"+from,2100,h->natural(h,adult,front,from));
    for(boolean adult:new boolean[]{false,true})for(boolean loop:new boolean[]{false,true})
      add(out,"jack_preview_"+(adult?"adult":"baby")+"_"+(loop?"loop":"once"),400,h->preview(h,adult,loop));
    for(String mode:List.of("stop","damage","pet","break","rotate","obstruct","displace","unload","logout","range","reload","original_noai","missing_cooldown"))
      add(out,"jack_interrupt_"+mode,220,h->interrupt(h,mode));
    for(String mode:List.of("rear","blocked","absent","reserved","carrying"))add(out,"jack_reject_"+mode,80,h->reject(h,mode));
    add(out,"jack_native_permissions_and_suggestions",80,JackToyGameTests080::permissions);
    add(out,"jack_idle_and_stale_active_block_close_without_cues",60,JackToyGameTests080::stale);
    return out;
  }
  private static void add(List<TestFunction> out,String name,int ticks,java.util.function.Consumer<GameTestHelper> test){
    out.add(new TestFunction("jack080_"+(out.size()/5),name,TEMPLATE,ticks,0,true,test));
  }
  private static void cleanup(GameTestHelper h,Runnable task){h.testInfo.addListener(new GameTestListener(){
    public void testStructureLoaded(GameTestInfo i){}
    public void testPassed(GameTestInfo i,GameTestRunner r){task.run();}
    public void testFailed(GameTestInfo i,GameTestRunner r){task.run();}
    public void testAddedForRerun(GameTestInfo a,GameTestInfo b,GameTestRunner r){task.run();}
  });}
  private static BlockPos floor(GameTestHelper h){
    for(int x=1;x<=18;x++)for(int z=1;z<=18;z++){
      h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
      for(int y=1;y<=5;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
    }return h.absolutePos(new BlockPos(9,1,9));
  }
  private static ChaoEntity actor(GameTestHelper h,Vec3 point,boolean adult,BlockPos at){
    var c=ChaoGarden.CHAO.get().create(h.getLevel());var s=new ChaoState();
    s.adult=adult;s.align=adult?1:0;s.primaryType=adult?ChaoState.FLY_TYPE:ChaoState.NORMAL;
    s.hunger=90;s.energy=100;s.happiness=20;s.boredom=100;s.personality=ChaoPreferences.EASILY_BORED;
    if(adult){s.levels[ChaoState.RUN]=9;s.setEvolutionStrength(1.2f);}
    for(int i=0;i<ChaoIllness.COUNT;i++)s.setIllness(i,100);
    c.setState(ChaoState.decode(s.encode()));c.setInvulnerable(true);c.getRandom().setSeed(18);c.moveTo(point);c.setOnGround(true);
    h.getLevel().addFreshEntity(c);c.goalSelector.removeAllGoals(g->true);c.targetSelector.removeAllGoals(g->true);
    CUES.put(c,new ArrayList<>());SOURCES.put(c,at.immutable());
    cleanup(h,()->{ToyPreview.stop(c);CUES.remove(c);SOURCES.remove(c);c.discard();});return c;
  }
  private static Fixture fixture(GameTestHelper h,boolean adult){
    var at=floor(h);h.getLevel().setBlock(at,ToyContent.block(KIND).defaultBlockState().setValue(ChaoToyBlock.FACING,Direction.SOUTH),3);
    var c=actor(h,Vec3.atBottomCenterOf(at.south()),adult,at);c.moveTo(ToyContent.jackAnchor(c,at));c.setOnGround(true);
    c.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ChaoGarden.item("red_wool_beanie")));
    c.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(ChaoGarden.item("aviators_accessory")));
    c.getPersistentData().putLong("ChaoToyNextPlay",h.getLevel().getGameTime()+12345);
    var p=TestConnections.makeServerPlayer(h);p.teleportTo(c.getX()+2,c.getY(),c.getZ());c.owner=p.getUUID();
    p.getInventory().setItem(12,new ItemStack(Items.DIAMOND,3));
    cleanup(h,()->{if(p.server.getPlayerList().getPlayer(p.getUUID())!=null)p.server.getPlayerList().remove(p);});return new Fixture(c,p,at);
  }
  private static int command(ServerPlayer p,String text,int permission){
    try{return p.server.getCommands().getDispatcher().execute(text,p.createCommandSourceStack().withPermission(permission));}
    catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){return -1;}
  }
  private static int command(ServerPlayer p,String text){return command(p,text,2);}
  private static void physical(GameTestHelper h,ChaoEntity c,BlockPos at){
    var anchor=ToyContent.jackAnchor(c,at);h.assertTrue(c.distanceToSqr(anchor)<.015*.015,"Jack remains at its real clear front watch position");
    var direction=c.position().subtract(Vec3.atBottomCenterOf(at));var facing=h.getLevel().getBlockState(at).getValue(ChaoToyBlock.FACING);
    h.assertTrue(direction.x*facing.getStepX()+direction.z*facing.getStepZ()>.98,"No watching through the back");
    h.assertTrue(h.getLevel().noCollision(c,c.getBoundingBox()),"Actual collision stays outside box");
    h.assertTrue(Float.isFinite(c.getXRot())&&Math.abs(c.getXRot())<=12.001f,"Bounded gaze cannot bury face in chest");
  }
  private static void natural(GameTestHelper h,boolean adult,Direction front,Direction from){
    var at=floor(h);h.getLevel().setBlock(at,ToyContent.block(KIND).defaultBlockState().setValue(ChaoToyBlock.FACING,front),3);
    var c=actor(h,Vec3.atBottomCenterOf(at.relative(from,2)),adult,at);c.goalSelector.addGoal(4,new ToyContent.PlayGoal(c));
    var start=new AtomicLong(-1);var full=new AtomicBoolean();var previous=new AtomicReference<>(c.position());
    h.onEachTick(()->{
      h.assertTrue(c.position().distanceTo(previous.get())<.7,"Approach walks around box without remote relocation");previous.set(c.position());
      if(ToyContent.isPlaying(c)){
        physical(h,c,at);int elapsed=ToyContent.sessionElapsed(h.getLevel(),at);start.compareAndSet(-1,h.getLevel().getGameTime()-elapsed);
        if(h.getLevel().getBlockState(at).getValue(ChaoToyBlock.FRAME)==2)full.set(true);
      }else if(start.get()>=0)h.assertTrue(h.getLevel().getGameTime()-start.get()>=ToyInteractionRules.JACK_REACTION_TICK,"Fear/outcome follows actual full reveal");
    });
    h.succeedWhen(()->{
      h.assertTrue(start.get()>=0&&!ToyContent.isPlaying(c)&&full.get(),"Actual front approach and reveal completed or reacted");
      h.assertTrue(CUES.get(c).size()==1,"Natural session has one pop cue, not uncontrolled openings");
      h.assertTrue(c.state().adult==adult&&(!adult||c.state().lockedPrimaryType()==ChaoState.FLY_TYPE),"Play preserves first-type and stage");
    });
  }
  private static void idle(GameTestHelper h,Fixture f,String before,long next,boolean had,boolean noAi){
    h.assertTrue(!ToyPreview.active(f.c)&&f.c.toyPlayingTicks()==0&&f.c.toyTarget()==null,"Tracked preview and ownership cleared");
    h.assertTrue(f.c.state().encode().equals(before),"No preview care, age, mood or genetics mutation/reward");
    h.assertTrue(f.c.isNoAi()==noAi&&!f.c.getPersistentData().contains("ChaoToyPreviewRestoreNoAi"),"Original AI flag restored");
    h.assertTrue(f.c.getPersistentData().contains("ChaoToyNextPlay")==had&&f.c.getPersistentData().getLong("ChaoToyNextPlay")==next,"Cooldown presence and value retained");
    h.assertTrue(f.c.getItemBySlot(EquipmentSlot.HEAD).is(ChaoGarden.item("red_wool_beanie"))&&f.c.getItemBySlot(EquipmentSlot.OFFHAND).is(ChaoGarden.item("aviators_accessory"))
        &&f.p.getInventory().getItem(12).is(Items.DIAMOND)&&f.p.getInventory().getItem(12).getCount()==3,"Existing equipment and real inventory retained");
    var state=h.getLevel().getBlockState(f.at);if(state.is(ToyContent.block(KIND)))h.assertTrue(!state.getValue(ChaoToyBlock.PLAYING)&&state.getValue(ChaoToyBlock.FRAME)==0,"Box closes and remains collectible");
  }
  private static void preview(GameTestHelper h,boolean adult,boolean loop){
    var f=fixture(h,adult);String before=f.c.state().encode();long next=f.c.getPersistentData().getLong("ChaoToyNextPlay"),start=h.getLevel().getGameTime();
    h.assertTrue(command(f.p,"chao testtoy jack_in_box "+(loop?"loop":"once"))==1,"Native dispatcher starts real placed preview");
    h.runAfterDelay(38,()->h.assertTrue(CUES.get(f.c).isEmpty()&&h.getLevel().getBlockState(f.at).getValue(ChaoToyBlock.FRAME)==0,"Closed quiet anticipation"));
    h.runAfterDelay(70,()->{physical(h,f.c,f.at);h.assertTrue(ToyPreview.active(f.c)&&before.equals(f.c.state().encode()),"Surprise uses reward-free saved-state-preserving preview");});
    h.runAfterDelay(loop?330:160,()->{
      if(loop)h.assertTrue(command(f.p,"chao testtoy jack_in_box stop")==1,"Explicit stop closes loop");else if(ToyPreview.active(f.c))ToyPreview.tick(f.c);
      idle(h,f,before,next,true,false);var cues=CUES.get(f.c);h.assertTrue(cues.size()==(loop?2:1),"One cue per complete160-tick episode");
      for(int i=0;i<cues.size();i++){
        var cue=cues.get(i);long when=start+40+i*160;
        h.assertTrue(cue.tick>=when&&cue.tick<=when+1,"Actual cue follows session pop clock");
        h.assertTrue(Math.abs(cue.volume-JackPlayRules080.POP_VOLUME)<.00001&&Math.abs(cue.pitch-JackPlayRules080.POP_PITCH)<.00001,"Quiet authored pop survives native dispatch");
      }
      h.succeed();
    });
  }
  private static void interrupt(GameTestHelper h,String mode){
    var f=fixture(h,true);boolean noAi=mode.equals("original_noai");f.c.setNoAi(noAi);
    if(mode.equals("missing_cooldown"))f.c.getPersistentData().remove("ChaoToyNextPlay");
    f.c.setYRot(31);f.c.setXRot(7);f.c.setYHeadRot(34);f.c.yBodyRot=29;
    long next=f.c.getPersistentData().getLong("ChaoToyNextPlay");boolean had=f.c.getPersistentData().contains("ChaoToyNextPlay");String before=f.c.state().encode();
    h.assertTrue(command(f.p,"chao testtoy jack_in_box loop")==1,"Start interruptible physical preview");
    h.runAfterDelay(70,()->{
      switch(mode){
        case "stop","original_noai","missing_cooldown"->h.assertTrue(command(f.p,"chao testtoy jack_in_box stop")==1,"Explicit stop accepted");
        case "damage"->{f.c.setInvulnerable(false);h.assertTrue(f.c.hurt(h.getLevel().damageSources().generic(),1),"Actual damage interrupts");}
        case "pet"->h.assertTrue(f.c.mobInteract(f.p,InteractionHand.MAIN_HAND).consumesAction(),"Actual pet interrupts");
        case "break"->h.getLevel().destroyBlock(f.at,true);
        case "rotate"->h.getLevel().setBlock(f.at,h.getLevel().getBlockState(f.at).setValue(ChaoToyBlock.FACING,Direction.NORTH),3);
        case "obstruct"->h.getLevel().setBlock(f.at.south(),Blocks.STONE.defaultBlockState(),3);
        case "displace"->f.c.setPos(f.c.getX()+2,f.c.getY(),f.c.getZ());
        case "unload"->f.c.discard();
        case "logout"->f.p.server.getPlayerList().remove(f.p);
        case "range"->f.p.teleportTo(f.c.getX()+70,f.c.getY(),f.c.getZ());
        case "reload"->{
          var saved=new CompoundTag();f.c.saveWithoutId(saved);
          h.assertTrue(!saved.getBoolean("NoAI")&&!saved.contains("ToyPlayingKind"),"Saved preview cannot resume or persist temporary AI");
          h.assertTrue(saved.getList("Rotation",net.minecraft.nbt.Tag.TAG_FLOAT).getFloat(0)==31&&saved.getList("Rotation",net.minecraft.nbt.Tag.TAG_FLOAT).getFloat(1)==7,"Fallback save retains original aim");
          f.c.discard();var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(saved);
          h.assertTrue(!ToyPreview.active(restored)&&restored.toyPlayingTicks()==0&&restored.state().encode().equals(before),"Reload restores ordinary saved identity without toy/reward");
        }
      }
      Runnable verify=()->{
        if(ToyPreview.active(f.c))ToyPreview.tick(f.c);
        String expected=mode.equals("damage")||mode.equals("pet")?f.c.state().encode():before;idle(h,f,expected,next,had,noAi);
        if(List.of("break","rotate","obstruct","displace").contains(mode)){
          // These are observed after a production AI tick. The restored ordinary
          // look/body controls may already turn again; direct stop/reload cases
          // separately require the exact saved angles at the cleanup boundary.
          h.assertTrue(Float.isFinite(f.c.getXRot())&&Float.isFinite(f.c.getYRot())
              &&Float.isFinite(f.c.getYHeadRot())&&Float.isFinite(f.c.yBodyRot)
              &&Math.abs(net.minecraft.util.Mth.wrapDegrees(f.c.getYHeadRot()-f.c.yBodyRot))<=75.001f,
              "Resumed ordinary gaze remains finite and connected after environmental cleanup");
        }else h.assertTrue(f.c.getXRot()==7&&f.c.getYRot()==31&&f.c.getYHeadRot()==34&&f.c.yBodyRot==29,"Cleanup restores temporary aim");
        int cueCount=CUES.get(f.c).size();
        if(mode.equals("break"))h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(f.at).inflate(2),i->i.getItem().is(ChaoGarden.item(KIND))).stream().mapToInt(i->i.getItem().getCount()).sum()==1,"Break returns exactly one real item");
        h.runAfterDelay(12,()->{h.assertTrue(!ToyPreview.active(f.c)&&CUES.get(f.c).size()==cueCount,"No lingering opening/cue after interruption");h.succeed();});
      };
      // These changes have no direct interaction event: the next production
      // tick validates the environment. Same-tick tick() calls are deduplicated.
      if(List.of("break","rotate","obstruct","displace").contains(mode))h.runAfterDelay(1,verify);
      else verify.run();
    });
  }
  private static void reject(GameTestHelper h,String mode){
    var f=fixture(h,false);ChaoEntity other=null;
    switch(mode){
      case "rear"->f.c.setPos(f.at.getX()+.5,f.at.getY(),f.at.getZ()-.5);
      case "blocked"->h.getLevel().setBlock(f.at.south(),Blocks.STONE.defaultBlockState(),3);
      case "absent"->h.getLevel().destroyBlock(f.at,false);
      case "carrying"->f.c.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.APPLE));
      case "reserved"->{var at=f.c.position();f.c.moveTo(at.add(3,0,0));other=actor(h,at,true,f.at);other.setNoAi(true);h.assertTrue(ToyContent.beginJackPreview(other,f.at),"Other actor owns actual block");f.c.moveTo(at);h.assertTrue(ToyContent.previewJack(f.c)==null,"Second actor cannot steal the occupied source");h.succeed();return;}
    }
    String saved=f.c.state().encode();var point=f.c.position();
    h.assertTrue(command(f.p,"chao testtoy jack_in_box once")<=0&&!ToyPreview.active(f.c)&&f.c.state().encode().equals(saved)&&f.c.position().equals(point),"Unavailable seat/item refuses without moving/minting/changing care");h.succeed();
  }
  private static void permissions(GameTestHelper h){
    var f=fixture(h,false);h.assertTrue(command(f.p,"chao testtoy jack_in_box once",0)<=0&&!ToyPreview.active(f.c),"Nonoperator denied on server");
    var dispatcher=f.p.server.getCommands().getDispatcher();var node=dispatcher.getRoot().getChild("chao").getChild("testtoy").getChild("jack_in_box");
    h.assertTrue(node!=null&&node.getChild("once")!=null&&node.getChild("loop")!=null&&node.getChild("stop")!=null,"All native preview controls registered");
    var suggestions=dispatcher.getCompletionSuggestions(dispatcher.parse("chao testtoy j",f.p.createCommandSourceStack().withPermission(2))).join();
    h.assertTrue(suggestions.getList().stream().anyMatch(s->s.getText().equals("jack_in_box")),"Actual completion lists jack");h.succeed();
  }
  private static void stale(GameTestHelper h){
    var at=floor(h);h.getLevel().setBlock(at,ToyContent.block(KIND).defaultBlockState().setValue(ChaoToyBlock.PLAYING,true).setValue(ChaoToyBlock.FRAME,2),3);
    h.getLevel().scheduleTick(at,ToyContent.block(KIND),1);
    h.runAfterDelay(3,()->{var state=h.getLevel().getBlockState(at);h.assertTrue(!state.getValue(ChaoToyBlock.PLAYING)&&state.getValue(ChaoToyBlock.FRAME)==0,"Stale saved active source closes without ownership");h.succeed();});
  }
}
