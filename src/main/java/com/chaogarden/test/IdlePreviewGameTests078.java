package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoEmotion;
import com.chaogarden.core.ChaoIdleRules078;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.ToyContent;
import com.chaogarden.toys.ToyPreview;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.bundle.PacketAndPayloadAcceptor;

/** Native idle-preview commands, interruption transactions and real tracker metadata. */
@GameTestHolder("chao_idle78") @PrefixGameTestTemplate(false)
public final class IdlePreviewGameTests078 {
  private static final String TEMPLATE="chao_idle78:activity_test";
  private record Fixture(ChaoEntity chao,ServerPlayer keeper) {}
  private record Observer(ChaoState state,String kind,int remaining) {}
  private IdlePreviewGameTests078() {}

  @GameTestGenerator public static Collection<TestFunction> cases() {
    var tests=new ArrayList<TestFunction>();
    for(String id:ChaoIdleRules078.IDS)for(boolean loop:new boolean[]{false,true})
      add(tests,"idle_preview_"+id+"_"+(loop?"adult_loop":"child_once"),390,h->preview(h,id,loop));
    for(String mode:List.of("named_stop","global_stop","toy_global_stop","alias_stop","damage","pet",
        "reload","unload","logout","range","original_noai","missing_cooldown"))
      add(tests,"idle_preview_interrupt_"+mode,150,h->interrupt(h,mode));
    for(String mode:List.of("held_item","busy","water","air","comparison"))
      add(tests,"idle_preview_rejects_"+mode,40,h->reject(h,mode));
    add(tests,"idle_native_permissions_catalogue_alias_and_matching_stop",40,IdlePreviewGameTests078::permissions);
    add(tests,"idle_native_nearest_selection_and_range_errors",40,IdlePreviewGameTests078::selection);
    add(tests,"idle_preview_early_late_and_rejoining_metadata_observers",150,IdlePreviewGameTests078::observers);
    return tests;
  }
  private static void add(List<TestFunction> tests,String name,int timeout,java.util.function.Consumer<GameTestHelper> run) {
    tests.add(new TestFunction("idle078_"+(tests.size()/5),name,TEMPLATE,timeout,0,true,run));
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
    var state=new ChaoState();state.setBaselineFace076(1,4);state.eyeColor=7;
    state.adult=adult;state.align=adult?1:0;state.ageTicks=321;state.hunger=83;state.energy=100;state.boredom=42;
    state.setEmotion(ChaoEmotion.JOY,17);state.setEmotion(ChaoEmotion.FEAR,0);
    state.setEmotion(ChaoEmotion.HUNGRY,1701);state.setEmotion(ChaoEmotion.SLEEPINESS,0);
    state.setEmotion(ChaoEmotion.TIREDNESS,0);state.setEmotion(ChaoEmotion.TIRE,0);
    state.setTrait("idle078.foreign","retain");
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
  private static void baseline(GameTestHelper h,ChaoState state) {
    h.assertTrue(state.baselineEyes()==1&&state.baselineMouth()==4&&state.eyeColor==7,"Preview preserves fixed native face and independent iris");
  }
  private static void stopped(GameTestHelper h,Fixture fixture,String expected,long cooldown,boolean hadCooldown,boolean noAi) {
    var chao=fixture.chao();
    h.assertTrue(!ToyPreview.active(chao)&&chao.toyPlayingKind().isEmpty()&&chao.toyPlayingTicks()==0&&chao.toyTarget()==null,
        "Idle preview clears existing production metadata and ownership");
    h.assertTrue(chao.state().encode().equals(expected),"Cleanup preserves exact saved care, genetics, lessons, face, emotions and age");
    h.assertTrue(chao.isNoAi()==noAi&&!chao.getPersistentData().contains("ChaoToyPreviewRestoreNoAi"),"Cleanup restores original AI flag and removes temporary marker");
    h.assertTrue(chao.getPersistentData().contains("ChaoToyNextPlay")==hadCooldown&&chao.getPersistentData().getLong("ChaoToyNextPlay")==cooldown,
        "Preview preserves original play cooldown presence and value");
    h.assertTrue(chao.getItemBySlot(EquipmentSlot.HEAD).is(Items.CARVED_PUMPKIN)&&chao.getItemBySlot(EquipmentSlot.OFFHAND).is(Items.STICK)
        &&fixture.keeper().getInventory().getItem(12).is(Items.DIAMOND)&&fixture.keeper().getInventory().getItem(12).getCount()==3,
        "No real Chao equipment or keeper items are consumed or rewarded");
    baseline(h,chao.state());
  }
  private static void preview(GameTestHelper h,String id,boolean loop) {
    var fixture=fixture(h,loop);var chao=fixture.chao();chao.setStaying(loop);
    String before=chao.state().encode();long cooldown=chao.getPersistentData().getLong("ChaoToyNextPlay");
    var goals=Set.copyOf(chao.goalSelector.getAvailableGoals());
    if(id.equals("tired"))h.assertTrue(!ChaoIdleRules078.allowed(chao.state(),id,true),"Rested fixture would not choose tired naturally");
    h.assertTrue(command(fixture.keeper(),"chao testidle "+id+" "+(loop?"loop":"once"))==1,"Native dispatcher starts selected production idle");
    h.assertTrue(chao.toyPlayingKind().equals("preview_idle_"+id)&&ToyPreview.idleId(chao.toyPlayingKind()).equals(id)
        &&chao.toyPlayingTicks()==ToyContent.PLAY_TICKS&&chao.isNoAi(),"Idle uses the existing synchronized 160-tick preview clock");
    h.runAfterDelay(20,()-> {
      float elapsed=ToyContent.PLAY_TICKS-chao.toyPlayingTicks();
      h.assertTrue(ToyPreview.active(chao)&&elapsed>=19&&elapsed<=21&&ChaoIdleRules078.weight(id,elapsed)>0,
          "Production idle advances through its visible clip on the shared clock");
      h.assertTrue(chao.state().encode().equals(before),"Forced idle, including tired on a rested Chao, cannot edit saved care or give rewards");
    });
    h.runAfterDelay(ChaoIdleRules078.duration(id)+4,()-> {
      float elapsed=ToyContent.PLAY_TICKS-chao.toyPlayingTicks();
      h.assertTrue(ToyPreview.active(chao)&&elapsed>ChaoIdleRules078.duration(id)&&ChaoIdleRules078.weight(id,elapsed)==0,
          "Short production clip returns to calm for the remainder of the shared 160-tick session");
      h.assertTrue(chao.state().encode().equals(before),"Calm preview tail still pauses permanent simulation");
    });
    if(!loop)h.runAfterDelay(159,()->h.assertTrue(ToyPreview.active(chao)&&chao.state().encode().equals(before),"Final once tick preserves exact permanent state"));
    h.runAfterDelay(loop?330:160,()-> {
      if(loop) {
        h.assertTrue(ToyPreview.active(chao)&&chao.toyPlayingKind().equals("preview_idle_"+id)&&chao.toyPlayingTicks()>140,
            "Loop renews same production request after each complete 160-tick session");
        h.assertTrue(command(fixture.keeper(),"chao testidle "+id+" stop")==1,"Matching native named stop ends loop");
      }else if(ToyPreview.active(chao))ToyPreview.tick(chao);
      stopped(h,fixture,before,cooldown,true,false);
      h.assertTrue(chao.isStaying()==loop&&goals.equals(chao.goalSelector.getAvailableGoals()),"Preview preserves Stay and original goal objects and priorities");
      h.runAfterDelay(12,()-> {h.assertTrue(!ToyPreview.active(chao)&&chao.toyPlayingTicks()==0,"No delayed idle preview returns after stop");baseline(h,chao.state());h.succeed();});
    });
  }
  private static void interrupt(GameTestHelper h,String mode) {
    var fixture=fixture(h,true);var chao=fixture.chao();boolean noAi=mode.equals("original_noai");chao.setNoAi(noAi);
    if(mode.equals("missing_cooldown"))chao.getPersistentData().remove("ChaoToyNextPlay");
    String before=chao.state().encode(),genes=chao.state().dnaSummary();
    long cooldown=chao.getPersistentData().getLong("ChaoToyNextPlay");boolean had=chao.getPersistentData().contains("ChaoToyNextPlay");
    h.assertTrue(command(fixture.keeper(),"chao testidle ground_play loop")==1,"Native idle loop starts before real interruption");
    h.runAfterDelay(50,()-> {
      switch(mode) {
        case "named_stop","original_noai","missing_cooldown" -> h.assertTrue(command(fixture.keeper(),"chao testidle ground_play stop")==1,"Named stop accepted");
        case "global_stop" -> h.assertTrue(command(fixture.keeper(),"chao testidle stop")==1,"Global idle stop accepted");
        case "toy_global_stop" -> h.assertTrue(command(fixture.keeper(),"chao testtoy stop")==1,"Existing global toy stop clears idle preview");
        case "alias_stop" -> h.assertTrue(command(fixture.keeper(),"chao testtoy idle_ground_play stop")==1,"Matching toy alias stop accepted");
        case "damage" -> {chao.setInvulnerable(false);h.assertTrue(chao.hurt(h.getLevel().damageSources().generic(),1),"Real damage accepted");}
        case "pet" -> {h.assertTrue(chao.mobInteract(fixture.keeper(),InteractionHand.MAIN_HAND).consumesAction(),"Real keeper interaction accepted");h.assertTrue(chao.getAffectionTicks()>0,"Pet reaction begins after idle interruption");}
        case "unload" -> chao.discard();
        case "logout" -> fixture.keeper().server.getPlayerList().remove(fixture.keeper());
        case "range" -> fixture.keeper().teleportTo(chao.getX()+70,chao.getY(),chao.getZ());
        case "reload" -> {
          var tag=new CompoundTag();chao.saveWithoutId(tag);
          h.assertTrue(!tag.getBoolean("NoAI")&&!tag.contains("ToyPlayingKind")&&!tag.contains("ToyPlayingRemaining"),"Actual NBT save contains ordinary AI with no resumable preview");
          chao.discard();var copy=ChaoGarden.CHAO.get().create(h.getLevel());copy.load(tag);h.getLevel().addFreshEntity(copy);cleanup(h,copy::discard);
          h.assertTrue(copy.state().encode().equals(before)&&copy.state().dnaSummary().equals(genes)&&!copy.isNoAi()
              &&!ToyPreview.active(copy)&&copy.toyPlayingTicks()==0&&!copy.getPersistentData().contains("ChaoToyPreviewRestoreNoAi"),
              "Actual entity reload retains exact care and inherited features without reroll or preview resumption");baseline(h,copy.state());
        }
        default -> throw new IllegalArgumentException(mode);
      }
      if(ToyPreview.active(chao))ToyPreview.tick(chao);
      String expected=mode.equals("damage")||mode.equals("pet")?chao.state().encode():before;
      stopped(h,fixture,expected,cooldown,had,noAi);
      h.assertTrue(chao.state().dnaSummary().equals(genes),"Real interruption never changes inherited genotype");
      h.runAfterDelay(20,()-> {h.assertTrue(!ToyPreview.active(chao)&&chao.toyPlayingTicks()==0,"No stale idle request returns after interruption");baseline(h,chao.state());h.succeed();});
    });
  }
  private static void reject(GameTestHelper h,String mode) {
    var fixture=fixture(h,true);var chao=fixture.chao();
    switch(mode) {
      case "held_item" -> chao.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.STONE));
      case "busy" -> chao.startBehavior(ChaoEntity.Behavior.GREET,100);
      case "water" -> h.getLevel().setBlock(chao.blockPosition(),Blocks.WATER.defaultBlockState(),3);
      case "air" -> {chao.setNoGravity(true);chao.setPos(chao.getX(),chao.getY()+2,chao.getZ());chao.setOnGround(false);}
      case "comparison" -> chao.setOfficialComparison(true);
      default -> throw new IllegalArgumentException(mode);
    }
    h.runAfterDelay(3,()-> {
      String before=chao.state().encode();boolean noAi=chao.isNoAi();
      h.assertTrue(command(fixture.keeper(),"chao testidle look_around once")<=0&&!ToyPreview.active(chao)&&chao.toyPlayingTicks()==0,
          "Native dispatcher refuses existing unavailable state");
      h.assertTrue(chao.state().encode().equals(before)&&chao.isNoAi()==noAi,"Rejected preview changes no saved state or AI flag");h.succeed();
    });
  }
  private static void permissions(GameTestHelper h) {
    var fixture=fixture(h,false);var chao=fixture.chao();String before=chao.state().encode();
    h.assertTrue(command(fixture.keeper(),"chao testidle tired once",0)<=0&&!ToyPreview.active(chao)&&chao.state().encode().equals(before),"Native permission requirement blocks unprivileged preview");
    h.assertTrue(command(fixture.keeper(),"chao testidle unknown once")<=0&&!ToyPreview.active(chao),"Invalid idle refuses without side effects");
    h.assertTrue(command(fixture.keeper(),"chao testidle")==1&&command(fixture.keeper(),"chao testidle list")==1
        &&command(fixture.keeper(),"chao testtoy list")==1,"Current operator catalogue and help execute through native dispatcher");
    var dispatcher=fixture.keeper().server.getCommands().getDispatcher();var source=fixture.keeper().createCommandSourceStack().withPermission(2);
    var suggestions=dispatcher.getCompletionSuggestions(dispatcher.parse("chao testidle ",source)).join().getList();
    var aliases=dispatcher.getCompletionSuggestions(dispatcher.parse("chao testtoy idle_",source)).join().getList();
    for(String id:ChaoIdleRules078.IDS) {
      h.assertTrue(suggestions.stream().anyMatch(suggestion->suggestion.getText().equals(id)),"Native completion includes idle "+id);
      h.assertTrue(aliases.stream().anyMatch(suggestion->suggestion.getText().equals("idle_"+id)),"Existing testtoy completion includes idle alias "+id);
    }
    var modes=dispatcher.getCompletionSuggestions(dispatcher.parse("chao testidle tired ",source)).join().getList();
    for(String mode:List.of("once","loop","stop"))h.assertTrue(modes.stream().anyMatch(suggestion->suggestion.getText().equals(mode)),"Native completion includes mode "+mode);
    h.assertTrue(command(fixture.keeper(),"chao testtoy idle_tired once")==1&&chao.toyPlayingKind().equals("preview_idle_tired"),"Existing toy alias requests same production idle token");
    h.assertTrue(command(fixture.keeper(),"chao testidle look_up stop")==0&&ToyPreview.active(chao),"Mismatched named stop preserves active idle");
    h.assertTrue(command(fixture.keeper(),"chao testidle tired stop")==1&&!ToyPreview.active(chao)&&chao.state().encode().equals(before),"Matching named stop clears alias preview with exact care preserved");
    h.assertTrue(ToyPreview.idleId("preview_idle_unknown").isEmpty()&&ToyPreview.idleId("chao_rattle").isEmpty(),"Unrelated or invalid tracked tokens cannot select an idle");h.succeed();
  }
  private static void selection(GameTestHelper h) {
    var fixture=fixture(h,false);var first=fixture.chao();
    var second=ChaoGarden.CHAO.get().create(h.getLevel());second.setState(ChaoState.decode(first.state().encode()));second.setNoAi(true);
    second.moveTo(first.position().add(6,0,0));second.setOnGround(true);h.getLevel().addFreshEntity(second);cleanup(h,()->{ToyPreview.stop(second);second.discard();});
    String before=second.state().encode();
    h.assertTrue(command(fixture.keeper(),"chao testidle bashful once")==1&&ToyPreview.active(first)&&!ToyPreview.active(second),"Command chooses nearest actual Chao");
    h.assertTrue(second.state().encode().equals(before),"Unselected Chao keeps its permanent care");command(fixture.keeper(),"chao testidle stop");
    first.startBehavior(ChaoEntity.Behavior.GREET,100);
    h.assertTrue(command(fixture.keeper(),"chao testidle bashful once")==0&&!ToyPreview.active(first)&&!ToyPreview.active(second),
        "Busy nearest Chao returns its existing unavailable error without silently selecting the farther body");
    fixture.keeper().teleportTo(first.getX()+15,first.getY(),first.getZ());
    h.assertTrue(command(fixture.keeper(),"chao testidle tired once")==1&&!ToyPreview.active(first)&&ToyPreview.active(second),"Twelve-block boundary excludes farther Chao and selects eligible nearer body");
    command(fixture.keeper(),"chao testidle stop");fixture.keeper().teleportTo(first.getX()+100,first.getY(),first.getZ());
    h.assertTrue(command(fixture.keeper(),"chao testidle think once")==0&&command(fixture.keeper(),"chao testidle stop")==0,
        "Missing nearby Chao and missing nearby active preview return selection errors");h.succeed();
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
        for(var value:decoded.packedItems())if(value.value() instanceof String encoded&&encoded.equals(source.state().encode()))state[0]=encoded;
      }finally {buffer.release();}
    }));
    h.assertTrue(packets[0]==1&&state[0]!=null,"Joining observer receives authoritative STATE through actual tracker codec");
    var result=new Observer(ChaoState.decode(state[0]),replica.toyPlayingKind(),replica.toyPlayingTicks());replica.discard();return result;
  }
  private static void observers(GameTestHelper h) {
    var fixture=fixture(h,true);var chao=fixture.chao();String before=chao.state().encode();
    h.assertTrue(command(fixture.keeper(),"chao testidle ground_play loop")==1,"Native loop starts before observer joins");
    var early=new Observer[1];var late=new Observer[1];
    h.runAfterDelay(20,()->early[0]=observer(h,chao,fixture.keeper()));
    h.runAfterDelay(65,()-> {
      late[0]=observer(h,chao,fixture.keeper());
      h.assertTrue(early[0].state().encode().equals(before)&&late[0].state().encode().equals(before),"Early and late metadata observers receive identical inherited face and permanent care");
      h.assertTrue(ToyPreview.idleId(early[0].kind()).equals("ground_play")&&early[0].kind().equals(late[0].kind())
          &&late[0].remaining()<early[0].remaining(),"Late observer receives advancing shared production idle phase without restarting it");
    });
    h.runAfterDelay(90,()-> {
      var rejoined=observer(h,chao,fixture.keeper());
      h.assertTrue(rejoined.remaining()<late[0].remaining()&&rejoined.state().encode().equals(before),"Rejoining metadata does not reroll native features or restart idle");
      h.assertTrue(command(fixture.keeper(),"chao testidle stop")==1,"Native stop ends observed idle loop");
      for(int i=0;i<2;i++) {
        var cleared=observer(h,chao,fixture.keeper());
        h.assertTrue(cleared.kind().isEmpty()&&cleared.remaining()==0&&cleared.state().encode().equals(before),"Observers receive cleared phase and unchanged permanent state after stop");baseline(h,cleared.state());
      }
      h.assertTrue(chao.state().encode().equals(before),"Observer joins cannot duplicate rewards or edit saved care");h.succeed();
    });
  }
}
