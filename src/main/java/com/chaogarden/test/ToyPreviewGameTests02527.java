package com.chaogarden.test;

import com.chaogarden.*;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.*;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("chao_garden") @PrefixGameTestTemplate(false) @EventBusSubscriber(modid="chao_garden")
public final class ToyPreviewGameTests02527 {
  private static final Map<ChaoEntity,List<String>> SOUNDS=new WeakHashMap<>();
  @SubscribeEvent public static void sound(PlayLevelSoundEvent.AtPosition e){
    if(e.getLevel().isClientSide||e.getSound()==null)return;
    SOUNDS.forEach((c,events)->{if(c.level()==e.getLevel()&&e.getPosition().distanceToSqr(c.position())<4)events.add(e.getSound().value().getLocation()+"@"+e.getLevel().getGameTime());});
  }
  private record Fixture(ChaoEntity c,ServerPlayer p){}
  private static Fixture fixture(GameTestHelper h,boolean adult){
    var pos=RattleGameTests02520.floor(h);h.getLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),3);
    var c=RattleGameTests02520.actor(h,pos,adult,ChaoState.NORMAL,false);c.goalSelector.removeAllGoals(g->true);c.setOnGround(true);
    var p=TestConnections.makeServerPlayer(h);p.getInventory().clearContent();p.teleportTo(c.getX()+2,c.getY(),c.getZ());c.owner=p.getUUID();
    SOUNDS.put(c,new ArrayList<>());
    h.testInfo.addListener(new GameTestListener(){public void testStructureLoaded(GameTestInfo i){}public void testAddedForRerun(GameTestInfo a,GameTestInfo b,GameTestRunner r){}public void testPassed(GameTestInfo i,GameTestRunner r){clean();}public void testFailed(GameTestInfo i,GameTestRunner r){clean();}private void clean(){ToyPreview.stop(c);SOUNDS.remove(c);c.discard();if(p.server.getPlayerList().getPlayer(p.getUUID())!=null)p.server.getPlayerList().remove(p);}});
    return new Fixture(c,p);
  }
  private static int command(ServerPlayer p,String text,int permission){
    try{return p.server.getCommands().getDispatcher().execute(text,p.createCommandSourceStack().withPermission(permission));}
    catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){return -1;}
  }
  private static int command(ServerPlayer p,String text){return command(p,text,2);}
  private static String preview(String id){return "chao "+(id.equals("sit")?"testpose ":"testtoy ")+id;}
  private static int cues(String id){return switch(id){case "sit"->0;case "rattle"->29;case "bubble_wand"->3;case "maracas"->31;case "castanets"->15;case "cymbals"->4;case "drum"->16;default->8;};}
  private static String event(String id){return id.equals("triangle")?"minecraft:block.note_block.bell":id.equals("trumpet")?"minecraft:block.note_block.didgeridoo":id.equals("flute")?"minecraft:block.note_block.flute":id.equals("drum")?"minecraft:block.note_block.basedrum":id.equals("bubble_wand")?"minecraft:block.bubble_column.bubble_pop":"chao_garden:toy."+(id.equals("bell")?"hand_bell":id);}
  private static List<String> sounds(ChaoEntity c,String id){return SOUNDS.get(c).stream().filter(s->s.startsWith(event(id)+"@")).toList();}
  @GameTestGenerator public static Collection<TestFunction> previews(){
    var tests=new ArrayList<TestFunction>();int[] n={0};
    java.util.function.BiConsumer<String,java.util.function.Consumer<GameTestHelper>> add=(name,body)->tests.add(new TestFunction("toy_preview02527_"+(n[0]++/4),name,"chao_garden:activity_test",600,0,true,body));
    for(var clip:HandToyEffects.clips())for(boolean adult:new boolean[]{false,true})for(boolean loop:new boolean[]{false,true})add.accept("preview_"+clip.id()+"_"+adult+"_"+loop,h->play(h,clip.id(),adult,loop));
    for(boolean adult:new boolean[]{false,true})for(boolean loop:new boolean[]{false,true})add.accept("preview_sit_"+adult+"_"+loop,h->play(h,"sit",adult,loop));
    for(boolean adult:new boolean[]{false,true})for(String mode:List.of("stop","hurt","pet","reload","logout","range","switch"))add.accept("preview_interrupt_"+adult+"_"+mode,h->interrupt(h,adult,mode));
    for(boolean adult:new boolean[]{false,true})for(String mode:List.of("stop","hurt","pet","reload","logout","range","switch"))add.accept("sitting_preview_interrupt_"+adult+"_"+mode,h->interrupt(h,adult,mode,"sit"));
    for(String mode:List.of("held","behavior","cocoon","comparison","construction","approach"))add.accept("preview_guard_"+mode,h->guard(h,mode));
    add.accept("preview_permissions_and_catalog",ToyPreviewGameTests02527::permissions);
    add.accept("preview_restore_original_noai_after_reload",ToyPreviewGameTests02527::savedNoAi);
    add.accept("preview_rejects_school_resident",ToyPreviewGameTests02527::staff);
    add.accept("preview_targets_only_nearest",ToyPreviewGameTests02527::nearest);
    return tests;
  }
  private static void play(GameTestHelper h,String id,boolean adult,boolean loop){
    var f=fixture(h,adult);var c=f.c;String state=c.state().encode();boolean stay=c.isStaying();
    h.assertTrue(command(f.p,preview(id)+(loop?" loop":" once"))==1,"real command starts "+id);
    h.assertTrue(c.isNoAi()&&c.toyPlayingKind().equals(id.equals("sit")?ToyPreview.SITTING_KIND:"chao_"+id),"normal synced clock is active");
    long start=h.getLevel().getGameTime();
    h.onEachTick(()->{if(ToyPreview.active(c)){ToyPreview.tick(c);ToyPreview.tick(c);h.assertTrue(c.state().encode().equals(state),"preview freezes saved care/genetics/age/lessons");h.assertTrue(c.getHeldChaoItem().isEmpty(),"no real prop minted");}});
    h.runAtTickTime(loop?325:160,()->{
      if(loop){h.assertTrue(ToyPreview.active(c),"loop remains active after two full cycles");h.assertTrue(command(f.p,preview(id)+" stop")==1,"per-toy stop works");}
      else ToyPreview.tick(c);
      h.assertTrue(!ToyPreview.active(c)&&!c.isNoAi()&&c.toyPlayingTicks()==0&&c.isStaying()==stay,"stop restores AI, stay and toy data");
      h.assertTrue(c.state().encode().equals(state),"no lesson, reward, age or care mutation");
      var events=sounds(c,id);h.assertTrue(events.size()==cues(id)*(loop?2:1),"exact real sound counts: "+id+" "+events);
      h.assertTrue(events.stream().distinct().count()==events.size(),"double ticks cannot duplicate cues");
      for(var item:h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,c.getBoundingBox().inflate(4)))h.assertTrue(false,"preview created a dropped item");
      h.succeed();
    });
  }
  private static void interrupt(GameTestHelper h,boolean adult,String mode){interrupt(h,adult,mode,"tambourine");}
  private static void interrupt(GameTestHelper h,boolean adult,String mode,String id){
    var f=fixture(h,adult);var c=f.c;h.assertTrue(command(f.p,preview(id)+" loop")==1,"loop starts");
    h.runAtTickTime(55,()->{
      int before=sounds(c,id).size();
      switch(mode){
        case "stop"->h.assertTrue(command(f.p,"chao testtoy stop")==1,"global stop works");
        case "hurt"->{c.setInvulnerable(false);h.assertTrue(c.hurt(h.getLevel().damageSources().generic(),1),"actual damage accepted");}
        case "pet"->{h.assertTrue(c.mobInteract(f.p,net.minecraft.world.InteractionHand.MAIN_HAND).consumesAction(),"actual pet accepted");h.assertTrue(c.getAffectionTicks()>0,"pet starts after preview stops");}
        case "reload"->{var tag=new CompoundTag();c.saveWithoutId(tag);h.assertTrue(!tag.getBoolean("NoAI")&&!tag.contains("ToyPlayingKind")&&!tag.contains("ToyPlayingRemaining"),"saved data is safe for older fallback JARs without preview cleanup");h.assertTrue(ToyPreview.active(c)&&c.isNoAi()&&c.toyPlayingTicks()>0,"saving does not interrupt live playback");c.discard();var copy=ChaoGarden.CHAO.get().create(h.getLevel());copy.load(tag);h.getLevel().addFreshEntity(copy);h.assertTrue(!copy.isNoAi()&&copy.toyPlayingTicks()==0&&!ToyPreview.active(copy),"reload restores AI and cancels playback");copy.discard();}
        case "logout"->f.p.server.getPlayerList().remove(f.p);
        case "range"->f.p.teleportTo(c.getX()+70,c.getY(),c.getZ());
        case "switch"->{h.assertTrue(command(f.p,"chao testtoy bell once")==1,"switch resets current preview");h.assertTrue(c.toyPlayingKind().equals("chao_bell")&&c.toyPlayingTicks()==160,"new kind begins at phase zero");h.assertTrue(command(f.p,"chao testtoy tambourine stop")==0&&ToyPreview.active(c),"wrong named stop does not cancel another toy");command(f.p,"chao testtoy stop");}
      }
      ToyPreview.tick(c);h.assertTrue(!ToyPreview.active(c)&&!c.isNoAi()&&c.toyPlayingTicks()==0,"interruption cleans up "+mode);
      h.runAfterDelay(20,()->{h.assertTrue(sounds(c,id).size()==before,"no cues after interruption");h.succeed();});
    });
  }
  private static void guard(GameTestHelper h,String mode){
    var f=fixture(h,false);var c=f.c;
    switch(mode){
      case "held"->c.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new ItemStack(Items.APPLE));
      case "behavior"->c.startBehavior(ChaoEntity.Behavior.HATCH,100);
      case "cocoon"->{c.state().cocoonTicks=100;c.sync();}
      case "comparison"->c.setOfficialComparison(true);
      case "construction"->c.state().setTraitInt("content.construction",2);
      case "approach"->c.getPersistentData().putLong("ChaoToyApproachUntil",h.getLevel().getGameTime()+100);
    }
    String saved=c.state().encode();h.assertTrue(command(f.p,"chao testtoy rattle")==0,"busy/unsupported start rejected: "+mode);
    h.assertTrue(!ToyPreview.active(c)&&!c.isNoAi()&&c.state().encode().equals(saved),"rejection leaves actor untouched");h.succeed();
  }
  private static void permissions(GameTestHelper h){
    var f=fixture(h,false);
    h.assertTrue(command(f.p,"chao testpose sit",0)==-1,"non-operator cannot preview sitting");
    h.assertTrue(command(f.p,"chao testpose list")==1,"sitting help executes");
    h.assertTrue(command(f.p,"chao testtoy rattle",0)==-1,"non-operator cannot preview");
    h.assertTrue(command(f.p,"chao testtoy unknown")==-1&&!ToyPreview.active(f.c),"unknown IDs cannot mutate the actor");
    h.assertTrue(command(f.p,"chao testtoy list")==1&&command(f.p,"chao testtoy")==1,"help/catalog execute");
    var dispatcher=f.p.server.getCommands().getDispatcher();var parsed=dispatcher.parse("chao testtoy ",f.p.createCommandSourceStack().withPermission(2));
    var names=dispatcher.getCompletionSuggestions(parsed).join().getList().stream().map(com.mojang.brigadier.suggestion.Suggestion::getText).toList();
    h.assertTrue(names.containsAll(HandToyEffects.clips().stream().map(HandToyEffects.Clip::id).toList()),"every production preview is tab-completable");h.succeed();
  }
  private static void savedNoAi(GameTestHelper h){
    var f=fixture(h,true);f.c.setNoAi(true);h.assertTrue(command(f.p,"chao testtoy maracas loop")==1,"original NoAI is allowed");
    var tag=new CompoundTag();f.c.saveWithoutId(tag);h.assertTrue(tag.getBoolean("NoAI")&&!tag.contains("ToyPlayingKind")&&!tag.contains("ToyPlayingRemaining"),"fallback save preserves original true NoAI without a preview clock");f.c.discard();var copy=ChaoGarden.CHAO.get().create(h.getLevel());copy.load(tag);h.getLevel().addFreshEntity(copy);
    h.assertTrue(copy.isNoAi()&&!ToyPreview.active(copy)&&copy.toyPlayingTicks()==0,"reload preserves original true NoAI");copy.discard();h.succeed();
  }
  private static void staff(GameTestHelper h){
    var f=fixture(h,false);f.c.discard();var npc=com.chaogarden.kindergarten.KindergartenContent.NPC.get().create(h.getLevel());npc.moveTo(f.p.position());h.getLevel().addFreshEntity(npc);
    h.assertTrue(command(f.p,"chao testtoy bell")==0&&!ToyPreview.unavailable(npc).isEmpty(),"school staff excluded");npc.discard();h.succeed();
  }
  private static void nearest(GameTestHelper h){
    var f=fixture(h,false);var second=ChaoGarden.CHAO.get().create(h.getLevel());second.moveTo(f.p.getX()+5,f.p.getY(),f.p.getZ());second.setOnGround(true);h.getLevel().addFreshEntity(second);
    h.assertTrue(command(f.p,"chao testtoy rattle")==1&&ToyPreview.active(f.c)&&!ToyPreview.active(second),"only nearest real Chao changes");second.discard();h.succeed();
  }
}
