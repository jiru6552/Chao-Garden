package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoEmotion;
import com.chaogarden.core.ChaoFaceRules076;
import com.chaogarden.core.ChaoFaceRules076.Eyes;
import com.chaogarden.core.ChaoFaceRules076.Mouth;
import com.chaogarden.core.ChaoFaceRules076.Signals;
import com.chaogarden.core.ChaoFaceRules076.Source;
import com.chaogarden.core.ChaoFaceRules076.Symptom;
import com.chaogarden.core.ChaoIllness;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.entity.ChaoEntity.Behavior;
import com.chaogarden.interaction.ChaoBehaviorGoal;
import com.chaogarden.item.ChaoCarrierItem;
import com.chaogarden.toys.ToyContent;
import com.chaogarden.toys.ToyPreview;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.bundle.PacketAndPayloadAcceptor;

/** Native commands, transactions and real tracker-wire state; no rendered-client claim. */
@GameTestHolder("chao_faces76") @PrefixGameTestTemplate(false)
public final class FaceGameTests076 {
  private static final String TEMPLATE="chao_faces76:activity_test";
  private record Preview(String id,Eyes eyes,Mouth mouth,Source source) {}
  private static final List<Preview> PREVIEWS=List.of(
      new Preview("happy",Eyes.HAPPY_CLOSED,Mouth.SMILE,Source.ACTIVITY),
      new Preview("distress",Eyes.DISTRESS,Mouth.OPEN,Source.DANGER),
      new Preview("tired",Eyes.HALF_CLOSED,Mouth.WAVY,Source.MOOD),
      new Preview("sleep",Eyes.SLEEP,Mouth.NONE,Source.SLEEP),
      new Preview("cough",Eyes.DISTRESS,Mouth.OPEN,Source.SYMPTOM),
      new Preview("cold",Eyes.DISTRESS,Mouth.OPEN,Source.SYMPTOM),
      new Preview("rash",Eyes.HALF_CLOSED,Mouth.WAVY,Source.SYMPTOM),
      new Preview("runny_nose",Eyes.HALF_CLOSED,Mouth.WAVY,Source.SYMPTOM),
      new Preview("hiccups",Eyes.DISTRESS,Mouth.OPEN,Source.SYMPTOM),
      new Preview("stomach_ache",Eyes.HALF_CLOSED,Mouth.WAVY,Source.SYMPTOM));
  private record Fixture(ChaoEntity chao,ServerPlayer keeper) {}
  private record Observer(ChaoState wireState,String kind,int remaining) {}
  private FaceGameTests076() {}

  @GameTestGenerator public static Collection<TestFunction> cases() {
    var tests=new ArrayList<TestFunction>();
    for(var entry:PREVIEWS)for(boolean loop:new boolean[]{false,true})
      add(tests,"face_preview_"+entry.id()+"_"+(loop?"adult_loop":"child_once"),390,h->preview(h,entry,loop));
    for(String mode:List.of("named_stop","global_stop","toy_global_stop","alias_stop","damage","pet",
        "reload","unload","logout","range","original_noai","missing_cooldown"))
      add(tests,"face_preview_interrupt_"+mode,160,h->interrupt(h,mode));
    for(String mode:List.of("chaos","omochao","held_item","sleep","cocoon","water","air","comparison"))
      add(tests,"face_preview_rejects_"+mode,40,h->reject(h,mode));
    add(tests,"face_native_permissions_catalogue_alias_and_matching_stop",40,FaceGameTests076::permissions);
    add(tests,"face_native_nearest_selection_and_twelve_block_boundary",40,FaceGameTests076::selection);
    for(int illness=0;illness<6;illness++) {
      final int index=illness;add(tests,"face_actual_illness_"+illness+"_symptom_medicine_and_reload",700,h->illness(h,index));
    }
    add(tests,"face_actual_fear_save_sync_and_recovery",40,h->mood(h,ChaoEmotion.FEAR,115,Eyes.SURPRISED,Mouth.OPEN));
    add(tests,"face_actual_anger_save_sync_and_recovery",40,h->mood(h,ChaoEmotion.ANGER,105,Eyes.MEAN,Mouth.FROWN));
    add(tests,"face_actual_joy_save_sync_and_recovery",40,h->mood(h,ChaoEmotion.JOY,115,Eyes.HAPPY_CLOSED,Mouth.SMILE));
    add(tests,"face_actual_preview_early_late_and_rejoining_wire_observers",150,FaceGameTests076::previewObservers);
    add(tests,"face_actual_illness_and_cure_wire_observers",40,FaceGameTests076::illnessObservers);
    add(tests,"face_inherited_personality_and_baseline_survive_real_carrier_codec",40,FaceGameTests076::carrier);
    return tests;
  }
  private static void add(List<TestFunction> tests,String name,int timeout,java.util.function.Consumer<GameTestHelper> run) {
    tests.add(new TestFunction("faces076_"+(tests.size()/5),name,TEMPLATE,timeout,0,true,run));
  }
  private static void cleanup(GameTestHelper h,Runnable action) {
    h.testInfo.addListener(new GameTestListener() {
      public void testStructureLoaded(GameTestInfo info) {}
      public void testPassed(GameTestInfo info,GameTestRunner runner) {action.run();}
      public void testFailed(GameTestInfo info,GameTestRunner runner) {action.run();}
      public void testAddedForRerun(GameTestInfo oldInfo,GameTestInfo newInfo,GameTestRunner runner) {action.run();}
    });
  }
  private static Fixture fixture(GameTestHelper h,boolean adult) {
    for(int x=1;x<=23;x++)for(int z=1;z<=23;z++) {
      h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
      for(int y=1;y<=5;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
    }
    var chao=ChaoGarden.CHAO.get().create(h.getLevel());h.assertTrue(chao!=null,"Production Chao creates");
    var state=new ChaoState();state.setBaselineFace076(1,4);state.adult=adult;state.align=adult?1:0;
    state.ageTicks=321;state.hunger=83;state.energy=100;state.boredom=42;
    state.setEmotion(ChaoEmotion.JOY,17);state.setEmotion(ChaoEmotion.FEAR,0);
    state.setEmotion(ChaoEmotion.HUNGRY,1701);state.eyeColor=7;
    state.setTrait("face076.foreign","retain");
    for(int i=0;i<6;i++)state.setIllness(i,100);
    chao.setState(ChaoState.decode(state.encode()));chao.setNoAi(false);chao.setInvulnerable(true);
    chao.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(10,1,10))));chao.setOnGround(true);
    h.getLevel().addFreshEntity(chao);chao.goalSelector.removeAllGoals(goal->true);chao.targetSelector.removeAllGoals(goal->true);
    chao.goalSelector.addGoal(7,new Goal() {
      {setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
      public boolean canUse() {return false;}
    });
    chao.getPersistentData().putLong("ChaoToyNextPlay",h.getLevel().getGameTime()+12345);
    chao.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.CARVED_PUMPKIN));
    chao.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.STICK));
    var keeper=TestConnections.makeSurvivalServerPlayer(h);keeper.teleportTo(chao.getX()+2,chao.getY(),chao.getZ());
    keeper.getInventory().setItem(12,new ItemStack(Items.DIAMOND,3));chao.owner=keeper.getUUID();
    cleanup(h,()->{ToyPreview.stop(chao);chao.discard();if(keeper.server.getPlayerList().getPlayer(keeper.getUUID())!=null)keeper.server.getPlayerList().remove(keeper);});
    return new Fixture(chao,keeper);
  }
  private static int command(ServerPlayer player,String command,int permission) {
    try {return player.server.getCommands().getDispatcher().execute(command,player.createCommandSourceStack().withPermission(permission));}
    catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected) {return -1;}
  }
  private static int command(ServerPlayer player,String command) {return command(player,command,2);}
  private static void shape(GameTestHelper h,ChaoState state,Signals signals,Eyes eyes,Mouth mouth,Source source) {
    var result=ChaoFaceRules076.resolve(state,signals);
    h.assertTrue(result.eyes()==eyes&&result.mouth()==mouth&&result.eyeSource()==source&&result.mouthSource()==source,
        "Exact production face projection: expected "+eyes+"/"+mouth+"/"+source+", got "+result);
  }
  private static void baseline(GameTestHelper h,ChaoState state) {
    h.assertTrue(state.baselineEyes()==1&&state.baselineMouth()==4&&state.eyeColor==7,"Saved intrinsic face and independent iris are unchanged");
  }
  private static void idle(GameTestHelper h,Fixture fixture,String expected,long cooldown,boolean hadCooldown,boolean noAi) {
    var chao=fixture.chao();
    h.assertTrue(!ToyPreview.active(chao)&&chao.toyPlayingKind().isEmpty()&&chao.toyPlayingTicks()==0&&chao.toyTarget()==null,
        "Face preview clears its shared production metadata and ownership");
    h.assertTrue(chao.state().encode().equals(expected),"Preview cleanup preserves exact saved care, genetics, face, emotions and age");
    h.assertTrue(chao.isNoAi()==noAi&&!chao.getPersistentData().contains("ChaoToyPreviewRestoreNoAi"),"Preview restores original NoAI and clears its temporary flag");
    h.assertTrue(chao.getPersistentData().contains("ChaoToyNextPlay")==hadCooldown&&chao.getPersistentData().getLong("ChaoToyNextPlay")==cooldown,
        "Face preview preserves cooldown presence and value");
    h.assertTrue(chao.getItemBySlot(EquipmentSlot.HEAD).is(Items.CARVED_PUMPKIN)&&chao.getItemBySlot(EquipmentSlot.OFFHAND).is(Items.STICK)
        &&fixture.keeper().getInventory().getItem(12).is(Items.DIAMOND)&&fixture.keeper().getInventory().getItem(12).getCount()==3,
        "Face preview preserves real equipment and keeper inventory");
    baseline(h,chao.state());
  }
  private static void preview(GameTestHelper h,Preview entry,boolean loop) {
    var fixture=fixture(h,loop);var chao=fixture.chao();chao.setStaying(loop);
    String before=chao.state().encode();long cooldown=chao.getPersistentData().getLong("ChaoToyNextPlay");
    var goals=Set.copyOf(chao.goalSelector.getAvailableGoals());
    h.assertTrue(command(fixture.keeper(),"chao testface "+entry.id()+" "+(loop?"loop":"once"))==1,"Actual native dispatcher starts requested face preview");
    h.assertTrue(chao.toyPlayingKind().equals("preview_face_"+entry.id())&&chao.isNoAi(),"Face preview uses existing synchronized160-tick clock");
    h.runAfterDelay(80,()-> {
      h.assertTrue(ToyPreview.active(chao)&&chao.state().encode().equals(before),"Active face preview pauses saved simulation without rewards");
      float elapsed=ToyContent.PLAY_TICKS-chao.toyPlayingTicks();
      shape(h,chao.state(),ChaoFaceRules076.previewSignals(Signals.quiet(),chao.toyPlayingKind(),elapsed),entry.eyes(),entry.mouth(),entry.source());
    });
    if(!loop)h.runAfterDelay(159,()->h.assertTrue(ToyPreview.active(chao)&&chao.state().encode().equals(before),"State stays exact through the final active once tick"));
    h.runAfterDelay(loop?330:160,()-> {
      if(loop) {
        h.assertTrue(ToyPreview.active(chao)&&chao.toyPlayingKind().equals("preview_face_"+entry.id()),"Loop renews same production face request");
        h.assertTrue(command(fixture.keeper(),"chao testface "+entry.id()+" stop")==1,"Matching named native stop ends loop");
      }else if(ToyPreview.active(chao))ToyPreview.tick(chao);
      idle(h,fixture,before,cooldown,true,false);
      h.assertTrue(chao.isStaying()==loop&&goals.equals(chao.goalSelector.getAvailableGoals()),"Preview preserves Stay and the original goal objects/priorities");
      shape(h,chao.state(),Signals.quiet(),Eyes.HALF_CLOSED,Mouth.WAVY,Source.BASELINE);
      h.runAfterDelay(12,()-> {h.assertTrue(!ToyPreview.active(chao)&&chao.toyPlayingTicks()==0,"No delayed face preview resumes after stop");baseline(h,chao.state());h.succeed();});
    });
  }
  private static void interrupt(GameTestHelper h,String mode) {
    var fixture=fixture(h,true);var chao=fixture.chao();boolean noAi=mode.equals("original_noai");chao.setNoAi(noAi);
    if(mode.equals("missing_cooldown"))chao.getPersistentData().remove("ChaoToyNextPlay");
    String before=chao.state().encode(),genes=chao.state().dnaSummary();
    long cooldown=chao.getPersistentData().getLong("ChaoToyNextPlay");boolean had=chao.getPersistentData().contains("ChaoToyNextPlay");
    h.assertTrue(command(fixture.keeper(),"chao testface happy loop")==1,"Start native face loop before real interruption");
    h.runAfterDelay(70,()-> {
      switch(mode) {
        case "named_stop","original_noai","missing_cooldown" -> h.assertTrue(command(fixture.keeper(),"chao testface happy stop")==1,"Named stop accepted");
        case "global_stop" -> h.assertTrue(command(fixture.keeper(),"chao testface stop")==1,"Global face stop accepted");
        case "toy_global_stop" -> h.assertTrue(command(fixture.keeper(),"chao testtoy stop")==1,"Existing global toy stop also clears face preview");
        case "alias_stop" -> h.assertTrue(command(fixture.keeper(),"chao testtoy face_happy stop")==1,"Matching toy alias stop accepted");
        case "damage" -> {chao.setInvulnerable(false);h.assertTrue(chao.hurt(h.getLevel().damageSources().generic(),1),"Real damage accepted");}
        case "pet" -> {h.assertTrue(chao.mobInteract(fixture.keeper(),InteractionHand.MAIN_HAND).consumesAction(),"Real keeper petting accepted");h.assertTrue(chao.getAffectionTicks()>0,"Pet reaction begins after preview interruption");}
        case "unload" -> chao.discard();
        case "logout" -> fixture.keeper().server.getPlayerList().remove(fixture.keeper());
        case "range" -> fixture.keeper().teleportTo(chao.getX()+70,chao.getY(),chao.getZ());
        case "reload" -> {
          var tag=new CompoundTag();chao.saveWithoutId(tag);
          h.assertTrue(!tag.getBoolean("NoAI")&&!tag.contains("ToyPlayingKind")&&!tag.contains("ToyPlayingRemaining"),"Preview save contains ordinary AI and no resumable request");
          chao.discard();var copy=ChaoGarden.CHAO.get().create(h.getLevel());copy.load(tag);h.getLevel().addFreshEntity(copy);cleanup(h,copy::discard);
          h.assertTrue(copy.state().encode().equals(before)&&!copy.isNoAi()&&!ToyPreview.active(copy)&&copy.toyPlayingTicks()==0,
              "Actual entity reload preserves fixed face/state and cannot resume preview");baseline(h,copy.state());
        }
        default -> throw new IllegalArgumentException(mode);
      }
      if(ToyPreview.active(chao))ToyPreview.tick(chao);
      String expected=mode.equals("damage")||mode.equals("pet")?chao.state().encode():before;
      idle(h,fixture,expected,cooldown,had,noAi);
      h.assertTrue(chao.state().dnaSummary().equals(genes),"Actual interruption never changes inherited genotype");
      if(mode.equals("damage"))shape(h,chao.state(),new Signals(false,true,false,false,false,Symptom.NONE,false,true,true,Mouth.OPEN,6,true),Eyes.DISTRESS,Mouth.FROWN,Source.DAMAGE);
      h.runAfterDelay(20,()-> {h.assertTrue(!ToyPreview.active(chao)&&chao.toyPlayingTicks()==0,"No stale preview returns after interruption");baseline(h,chao.state());h.succeed();});
    });
  }
  private static void reject(GameTestHelper h,String mode) {
    var fixture=fixture(h,true);var chao=fixture.chao();
    switch(mode) {
      case "chaos" -> {chao.state().immortal=true;chao.sync();}
      case "omochao" -> {chao.state().character="omochao";chao.sync();}
      case "held_item" -> chao.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.STONE));
      case "sleep" -> {chao.state().sleeping=true;chao.state().setEmotion(ChaoEmotion.SLEEPINESS,9500);chao.sync();}
      case "cocoon" -> {chao.state().cocoonTicks=100;chao.sync();}
      case "water" -> h.getLevel().setBlock(chao.blockPosition(),Blocks.WATER.defaultBlockState(),3);
      case "air" -> {chao.setNoGravity(true);chao.setPos(chao.getX(),chao.getY()+2,chao.getZ());chao.setOnGround(false);}
      case "comparison" -> chao.setOfficialComparison(true);
      default -> throw new IllegalArgumentException(mode);
    }
    h.runAfterDelay(3,()-> {
      if(mode.equals("sleep"))h.assertTrue(chao.isResting(),"Rejected sleep fixture reaches real resting state");
      String before=chao.state().encode();boolean noAi=chao.isNoAi();
      h.assertTrue(command(fixture.keeper(),"chao testface happy once")<=0&&!ToyPreview.active(chao)&&chao.toyPlayingTicks()==0,
          "Unavailable/native anatomy refuses preview through actual dispatcher");
      h.assertTrue(chao.state().encode().equals(before)&&chao.isNoAi()==noAi,"Rejected command changes no saved state or AI flag");h.succeed();
    });
  }
  private static void permissions(GameTestHelper h) {
    var fixture=fixture(h,false);var chao=fixture.chao();String before=chao.state().encode();
    h.assertTrue(command(fixture.keeper(),"chao testface happy once",0)<=0&&!ToyPreview.active(chao)&&chao.state().encode().equals(before),"Unprivileged native source cannot start a face preview");
    h.assertTrue(command(fixture.keeper(),"chao testface unknown once")<=0&&!ToyPreview.active(chao),"Invalid expression refuses without side effects");
    h.assertTrue(command(fixture.keeper(),"chao testface list")==1&&command(fixture.keeper(),"chao testtoy list")==1,"Both operator lists execute natively");
    var dispatcher=fixture.keeper().server.getCommands().getDispatcher();
    var source=fixture.keeper().createCommandSourceStack().withPermission(2);
    var suggestions=dispatcher.getCompletionSuggestions(dispatcher.parse("chao testface ",source)).join().getList();
    for(var entry:PREVIEWS)h.assertTrue(suggestions.stream().anyMatch(suggestion->suggestion.getText().equals(entry.id())),"Native suggestions include "+entry.id());
    h.assertTrue(command(fixture.keeper(),"chao testtoy face_happy once")==1&&chao.toyPlayingKind().equals("preview_face_happy"),"Existing toy alias starts same production face token");
    h.assertTrue(command(fixture.keeper(),"chao testface cough stop")==0&&ToyPreview.active(chao),"Mismatched named stop cannot clear a different face request");
    h.assertTrue(command(fixture.keeper(),"chao testtoy face_happy stop")==1&&!ToyPreview.active(chao)&&chao.state().encode().equals(before),"Matching alias stop preserves exact ordinary state");h.succeed();
  }
  private static void selection(GameTestHelper h) {
    var fixture=fixture(h,false);var first=fixture.chao();
    var second=ChaoGarden.CHAO.get().create(h.getLevel());second.setState(ChaoState.decode(first.state().encode()));second.setNoAi(true);
    second.moveTo(first.position().add(6,0,0));second.setOnGround(true);h.getLevel().addFreshEntity(second);cleanup(h,()->{ToyPreview.stop(second);second.discard();});
    String before=second.state().encode();
    h.assertTrue(command(fixture.keeper(),"chao testface happy once")==1&&ToyPreview.active(first)&&!ToyPreview.active(second),"Native command chooses the nearer actual Chao");
    h.assertTrue(second.state().encode().equals(before),"Unselected Chao retains its face and care");command(fixture.keeper(),"chao testface stop");
    fixture.keeper().teleportTo(first.getX()+15,first.getY(),first.getZ());
    h.assertTrue(command(fixture.keeper(),"chao testface tired once")==1&&!ToyPreview.active(first)&&ToyPreview.active(second),"Twelve-block native selection excludes farther Chao and selects the one in range");
    command(fixture.keeper(),"chao testface stop");h.succeed();
  }
  private static Behavior symptomBehavior(int illness) {
    return new Behavior[]{Behavior.COUGH,Behavior.SNEEZE,Behavior.SCRATCH,Behavior.RUNNY_NOSE,Behavior.HICCUP,Behavior.STOMACH_ACHE}[illness];
  }
  private static Symptom symptom(int illness) {
    return new Symptom[]{Symptom.COUGH,Symptom.SNEEZE,Symptom.SCRATCH,Symptom.RUNNY_NOSE,Symptom.HICCUP,Symptom.STOMACH_ACHE}[illness];
  }
  private static void illness(GameTestHelper h,int illness) {
    var fixture=fixture(h,true);var chao=fixture.chao();chao.state().setIllness(illness,-100);chao.state().setEmotion(ChaoEmotion.CONDITION,0);chao.sync();
    chao.goalSelector.addGoal(2,new ChaoBehaviorGoal(chao));var seen=new AtomicBoolean();
    String genes=chao.state().dnaSummary();var live=new ChaoEntity[]{chao};
    h.onEachTick(()-> {
      if(seen.get()||chao.getBehavior()!=symptomBehavior(illness))return;seen.set(true);
      h.assertTrue(chao.state().illness(illness)<0,"Actual voluntary illness behavior remains a symptom, not a cure");
      Eyes eyes=new Eyes[]{Eyes.DISTRESS,Eyes.DISTRESS,Eyes.HALF_CLOSED,Eyes.HALF_CLOSED,Eyes.DISTRESS,Eyes.HALF_CLOSED}[illness];
      Mouth mouth=new Mouth[]{Mouth.OPEN,Mouth.OPEN,Mouth.WAVY,Mouth.WAVY,Mouth.OPEN,Mouth.WAVY}[illness];
      shape(h,chao.state(),new Signals(false,false,false,false,false,symptom(illness),true,false,false,null,-1,false),eyes,mouth,Source.SYMPTOM);
      var dose=new ItemStack(ChaoGarden.item("chao_medicine"),2);fixture.keeper().setItemInHand(InteractionHand.MAIN_HAND,dose);
      h.assertTrue(chao.offerItem(fixture.keeper(),InteractionHand.MAIN_HAND).consumesAction()&&dose.getCount()==1&&chao.state().isIll(),"One real medicine dose enters consideration without an instant cure");
      chao.setNoAi(true);
      h.runAfterDelay(45,()-> {
        h.assertTrue(chao.state().isIll()&&chao.getItemAction()==ChaoEntity.ItemAction.TRAIN,"Illness persists through consideration into real treatment animation");
        var tag=new CompoundTag();chao.saveWithoutId(tag);chao.discard();var copy=ChaoGarden.CHAO.get().create(h.getLevel());copy.load(tag);h.getLevel().addFreshEntity(copy);cleanup(h,copy::discard);live[0]=copy;
      });
      h.runAfterDelay(130,()-> {
        var restored=live[0];h.assertTrue(!restored.state().isIll()&&restored.state().illness(illness)==100&&restored.state().illnessImmunityTicks(illness)>23000,
            "Saved real treatment commits exactly one cure with persistent immunity");
        h.assertTrue(dose.getCount()==1&&restored.getMainHandItem().isEmpty(),"Medicine consumes the transferred dose only");
        h.assertTrue(restored.state().dnaSummary().equals(genes),"Treatment and reload preserve inherited genotype");baseline(h,restored.state());
        shape(h,restored.state(),Signals.quiet(),Eyes.HALF_CLOSED,Mouth.WAVY,Source.BASELINE);
        h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,restored.getBoundingBox().inflate(4),item->item.getItem().is(ChaoGarden.item("chao_medicine"))).isEmpty(),"Consumed medicine is never also returned as a drop");h.succeed();
      });
    });
  }
  private static ChaoEntity reloaded(GameTestHelper h,ChaoEntity source) {
    var tag=new CompoundTag();source.saveWithoutId(tag);var copy=ChaoGarden.CHAO.get().create(h.getLevel());copy.load(tag);cleanup(h,copy::discard);return copy;
  }
  private static void mood(GameTestHelper h,int emotion,int value,Eyes eyes,Mouth mouth) {
    var fixture=fixture(h,true);var chao=fixture.chao();chao.state().setEmotion(emotion,value);chao.sync();
    String before=chao.state().encode();shape(h,chao.state(),Signals.quiet(),eyes,mouth,Source.MOOD);
    var wire=observer(h,chao,fixture.keeper());h.assertTrue(wire.wireState().encode().equals(before),"Real metadata wire retains exact mood fractions and baseline");
    var copy=reloaded(h,chao);h.assertTrue(copy.state().encode().equals(before),"Actual entity NBT preserves active mood without reroll");shape(h,copy.state(),Signals.quiet(),eyes,mouth,Source.MOOD);
    copy.state().setEmotion(emotion,0);copy.sync();shape(h,copy.state(),Signals.quiet(),Eyes.HALF_CLOSED,Mouth.WAVY,Source.BASELINE);baseline(h,copy.state());h.succeed();
  }
  private static Observer observer(GameTestHelper h,ChaoEntity source,ServerPlayer viewer) {
    source.sync();var state=new String[]{null};var replica=ChaoGarden.CHAO.get().create(h.getLevel());var packets=new int[1];
    var tracker=new ServerEntity(h.getLevel(),source,1,true,packet->{});
    tracker.sendPairingData(viewer,new PacketAndPayloadAcceptor<>(packet-> {
      if(!(packet instanceof ClientboundSetEntityDataPacket metadata))return;
      var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess());
      try {
        ClientboundSetEntityDataPacket.STREAM_CODEC.encode(buffer,metadata);var decoded=ClientboundSetEntityDataPacket.STREAM_CODEC.decode(buffer);
        h.assertTrue(decoded.id()==source.getId()&&!buffer.isReadable(),"Complete actual pairing metadata packet roundtrips");
        replica.getEntityData().assignValues(decoded.packedItems());packets[0]++;
        for(var value:decoded.packedItems())if(value.value() instanceof String encoded&&encoded.startsWith("6;")&&encoded.equals(source.state().encode()))state[0]=encoded;
      }finally {buffer.release();}
    }));
    h.assertTrue(packets[0]==1&&state[0]!=null,"Joining observer receives authoritative STATE through real tracker codec");
    // Server-level replica.state() is intentionally not used: only a client level decodes that cache.
    var result=new Observer(ChaoState.decode(state[0]),replica.toyPlayingKind(),replica.toyPlayingTicks());replica.discard();return result;
  }
  private static void previewObservers(GameTestHelper h) {
    var fixture=fixture(h,true);var chao=fixture.chao();String before=chao.state().encode();
    h.assertTrue(command(fixture.keeper(),"chao testface happy loop")==1,"Native loop starts before observer joins");
    var first=new Observer[1];var second=new Observer[1];
    h.runAfterDelay(20,()->first[0]=observer(h,chao,fixture.keeper()));
    h.runAfterDelay(65,()-> {
      second[0]=observer(h,chao,fixture.keeper());
      h.assertTrue(first[0].wireState().encode().equals(before)&&second[0].wireState().encode().equals(before),"Independent early and late observers receive same fixed face/care");
      h.assertTrue(first[0].kind().equals("preview_face_happy")&&second[0].kind().equals("preview_face_happy")&&second[0].remaining()<first[0].remaining(),"Late observer receives advancing production phase without restarting preview");
    });
    h.runAfterDelay(90,()-> {
      var rejoined=observer(h,chao,fixture.keeper());h.assertTrue(rejoined.remaining()<second[0].remaining()&&rejoined.wireState().encode().equals(before),"Rejoining observer does not reroll face or restart action");
      h.assertTrue(command(fixture.keeper(),"chao testface stop")==1,"Native stop clears observer session");
      for(int observer=0;observer<2;observer++) {
        var cleared=observer(h,chao,fixture.keeper());h.assertTrue(cleared.kind().isEmpty()&&cleared.remaining()==0&&cleared.wireState().encode().equals(before),"Every observer receives cleared clock and unchanged baseline after stop");
      }
      h.assertTrue(chao.state().encode().equals(before),"Observer pairing cannot duplicate rewards or mutate saved care");h.succeed();
    });
  }
  private static void illnessObservers(GameTestHelper h) {
    var fixture=fixture(h,true);var chao=fixture.chao();chao.state().setIllness(ChaoIllness.COUGH,-100);chao.sync();
    var early=observer(h,chao,fixture.keeper());shape(h,early.wireState(),Signals.quiet(),Eyes.HALF_CLOSED,Mouth.WAVY,Source.ILLNESS);baseline(h,early.wireState());
    h.assertTrue(chao.state().cureIllness(ChaoIllness.COUGH),"Actual illness cure occurs once before late observer");chao.sync();
    var late=observer(h,chao,fixture.keeper());var rejoined=observer(h,chao,fixture.keeper());
    h.assertTrue(late.wireState().illness(ChaoIllness.COUGH)==100&&late.wireState().illnessImmunityTicks(ChaoIllness.COUGH)==24000
        &&late.wireState().encode().equals(rejoined.wireState().encode()),"Late/rejoining wire state carries actual cure and exact immunity timer");
    shape(h,late.wireState(),Signals.quiet(),Eyes.HALF_CLOSED,Mouth.WAVY,Source.BASELINE);baseline(h,rejoined.wireState());h.succeed();
  }
  private static void carrier(GameTestHelper h) {
    var fixture=fixture(h,true);var chao=fixture.chao();var first=new ChaoState();var second=new ChaoState();first.adult=second.adult=true;
    Arrays.fill(first.personalityGenesA,0);Arrays.fill(first.personalityGenesB,4);Arrays.fill(second.personalityGenesA,1);Arrays.fill(second.personalityGenesB,3);
    var child=ChaoState.breed(first,second,new Random(76));child.setBaselineFace076(1,4);child.eyeColor=7;
    child.setEmotion(ChaoEmotion.FEAR,114);child.setIllness(ChaoIllness.RASH,-100);chao.setState(child);String before=chao.state().encode();
    int[] genesA=chao.state().personalityGenesA.clone(),genesB=chao.state().personalityGenesB.clone();String ancestry=chao.state().trait("family.parent.0.id");
    var held=new ItemStack(ChaoGarden.item("chao_carrier"));
    h.assertTrue(ChaoCarrierItem.capture(held,chao,fixture.keeper())&&chao.isRemoved(),"Real keeper capture stores one native Chao body");
    var roundtrip=ItemStack.parseOptional(fixture.keeper().registryAccess(),(CompoundTag)held.save(fixture.keeper().registryAccess()));
    var restored=ChaoCarrierItem.state(roundtrip);
    h.assertTrue(restored.encode().equals(before)&&Arrays.equals(restored.personalityGenesA,genesA)&&Arrays.equals(restored.personalityGenesB,genesB),"Actual carrier item codec preserves exact inherited personality, mood, illness and baseline");
    h.assertTrue(restored.trait("family.parent.0.id").equals(ancestry)&&ChaoCarrierItem.storedUUID(roundtrip).equals(chao.getUUID()),"Carrier preserves ancestry and actual creature UUID");baseline(h,restored);h.succeed();
  }
}
