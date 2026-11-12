package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.care.ChaoCareNetwork;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.CreativeChaoEditRules056;
import com.chaogarden.core.MaturityEditRules056;
import com.chaogarden.entity.ChaoEntity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Native dispatcher authorization/selection plus real maturity payload/save/sync boundaries. */
@GameTestHolder("chao_edit56") @PrefixGameTestTemplate(false)
public final class CreativeEditingGameTests056 {
  private static final BlockPos AT=new BlockPos(3,2,3);
  private CreativeEditingGameTests056() {}
  @GameTestGenerator public static Collection<TestFunction> cases() {
    var result=new ArrayList<TestFunction>();
    for(String stat:CreativeChaoEditRules056.STAT_NAMES)add(result,"command_stat_"+stat.toLowerCase(java.util.Locale.ROOT),h->stat(h,stat));
    for(String selection:List.of("quoted_name","reserved_literal_name","case_insensitive","ambiguous","missing_name","missing_target","nearest_empty","nearest_sphere","nearest_boundary","nearest_tie","unloaded_identity"))
      add(result,"selection_"+selection,h->selection(h,selection));
    for(String gate:List.of("survival","forged_creative_abilities","spectator","creative_nonop","survival_operator"))
      add(result,"command_authority_"+gate,h->commandGate(h,gate));
    for(String invalid:List.of("stat","alignment_low","alignment_high","level_low","level_high","incomplete"))
      add(result,"command_invalid_"+invalid,h->invalid(h,invalid));
    add(result,"adult_conflict_is_atomic_and_current_alignment_allows_stat",CreativeEditingGameTests056::adult);
    add(result,"actual_main_increases_update_ties_and_decreases_preserve_recency",CreativeEditingGameTests056::ties);
    for(boolean adult:new boolean[]{false,true})for(int value:new int[]{0,500,990,1000}) {
      int milli=value;add(result,"maturity_"+(adult?"adult":"child")+"_"+value,h->maturity(h,adult,milli));
    }
    for(String gate:List.of("survival","forged_creative_abilities","spectator","no_analyzer","far","cocoon","busy","dead","immortal","malformed","missing","numeric_low","numeric_high"))
      add(result,"maturity_reject_"+gate,h->maturityGate(h,gate));
    add(result,"maturity_wire_codec_and_truncation",CreativeEditingGameTests056::codec);
    add(result,"maturity_offhand_and_foreign_owner_follow_existing_creative_override",h->{
      var p=player(h);var c=actor(h,p);c.owner=UUID.randomUUID();
      p.setItemInHand(InteractionHand.OFF_HAND,p.getMainHandItem().copy());p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
      h.assertTrue(maturity(p,c,750),"Offhand Analyzer and existing Creative ownership override are accepted");persisted(h,c);h.succeed();
    });
    add(result,"command_root_missing_target_has_usage_and_no_mutation",h->{var p=player(h);var c=actor(h,p);String before=c.state().encode();
      h.assertTrue(command(p,"chao",0)==0&&c.state().encode().equals(before),"Missing target fails without selecting nearest");h.succeed();});
    return result;
  }
  private static void add(List<TestFunction> list,String name,java.util.function.Consumer<GameTestHelper> body) {
    list.add(new TestFunction("editing056_"+(list.size()/8),name,"chao_morph52:activity_test",60,0,true,body));
  }
  private static void cleanup(GameTestHelper h,Runnable action) {
    h.testInfo.addListener(new GameTestListener() {
      public void testStructureLoaded(GameTestInfo i) {}
      public void testPassed(GameTestInfo i,GameTestRunner r) {action.run();}
      public void testFailed(GameTestInfo i,GameTestRunner r) {action.run();}
      public void testAddedForRerun(GameTestInfo a,GameTestInfo b,GameTestRunner r) {action.run();}
    });
  }
  private static ServerPlayer player(GameTestHelper h) {
    var p=TestConnections.makeSurvivalServerPlayer(h);p.setGameMode(GameType.CREATIVE);
    p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ChaoGarden.item("chao_analyzer")));
    cleanup(h,()->{if(p.server.getPlayerList().getPlayer(p.getUUID())==p)p.server.getPlayerList().remove(p);});return p;
  }
  private static ChaoEntity actor(GameTestHelper h,ServerPlayer p) {
    var c=ChaoGarden.CHAO.get().create(h.getLevel());h.assertTrue(c!=null,"Actual Chao must create");
    var s=new ChaoState();s.ageTicks=1234;s.happiness=80;s.setEvolutionStrength(.4f);s.setTrait("edit56.foreign","keep");
    c.setState(s);c.owner=p.getUUID();c.setNoAi(true);c.setNoGravity(true);
    for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)h.setBlock(AT.offset(x,-1,z),Blocks.STONE);
    c.moveTo(Vec3.atBottomCenterOf(h.absolutePos(AT)));h.getLevel().addFreshEntity(c);
    c.setCustomName(Component.literal("Edit56 "+UUID.randomUUID().toString().substring(0,8)));cleanup(h,c::discard);
    p.teleportTo(c.getX()+1,c.getY(),c.getZ());return c;
  }
  private static int command(ServerPlayer p,String text,int permission) {
    try{return p.server.getCommands().getDispatcher().execute(text,p.createCommandSourceStack().withPermission(permission));}
    catch(com.mojang.brigadier.exceptions.CommandSyntaxException invalid){return 0;}
  }
  private static String quoted(ChaoEntity c) {return "\""+c.getName().getString()+"\"";}
  private static void persisted(GameTestHelper h,ChaoEntity c) {
    c.sync();String encoded=c.state().encode();var values=c.getEntityData().getNonDefaultValues();
    h.assertTrue(values!=null&&values.stream().anyMatch(value->value.value() instanceof String s&&s.equals(encoded)),"Server entity data publishes edited inputs");
    var observer=ChaoGarden.CHAO.get().create(h.getLevel());observer.getEntityData().assignValues(values);
    h.assertTrue(observer.getEntityData().getNonDefaultValues().stream().anyMatch(value->value.value() instanceof String s&&s.equals(encoded)),"Client observer receives same authoritative input");
    var tag=new CompoundTag();c.saveWithoutId(tag);var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(tag);
    h.assertTrue(restored.state().encode().equals(encoded),"Entity NBT persists edited state exactly");
  }
  private static void stat(GameTestHelper h,String name) {
    var p=player(h);var c=actor(h,p);
    h.assertTrue(command(p,"chao ~ 100 "+name+" 37",0)==1,"Native Creative non-op command accepts "+name);
    h.assertTrue(CreativeChaoEditRules056.level(c.state(),CreativeChaoEditRules056.stat(name))==37&&c.state().align==1,"Named stat level and baby alignment update");
    h.assertTrue(c.state().ageTicks==1234&&c.state().evolutionStrength()==.4f&&c.state().trait("edit56.foreign").equals("keep"),"Unrelated saved data preserved");
    persisted(h,c);h.succeed();
  }
  private static void selection(GameTestHelper h,String selection) {
    var p=player(h);var c=actor(h,p);String before=c.state().encode(),target=quoted(c);int expected=0;
    switch(selection) {
      case "quoted_name"->expected=1;
      case "reserved_literal_name"->{c.setCustomName(Component.literal("rebirth"));target=quoted(c);expected=1;}
      case "case_insensitive"->{target=target.toUpperCase(java.util.Locale.ROOT);expected=1;}
      case "ambiguous"->{var other=actor(h,p);other.setCustomName(c.getCustomName());p.teleportTo(c.getX()+1,c.getY(),c.getZ());}
      case "missing_name"->target="\"Missing "+UUID.randomUUID()+"\"";
      case "missing_target"->target="\"\"";
      case "nearest_empty"->{target="~";p.teleportTo(c.getX()+1000,c.getY(),c.getZ());}
      case "nearest_sphere"->{target="~";c.moveTo(c.getX(),c.getY()+32,c.getZ());p.teleportTo(c.getX()+10,c.getY()+10,c.getZ());}
      case "nearest_boundary"->{target="~";p.teleportTo(c.getX(),c.getY()+12,c.getZ());expected=1;}
      case "nearest_tie"->{target="~";var other=actor(h,p);other.moveTo(c.getX()+2,c.getY(),c.getZ());p.teleportTo(c.getX()+1,c.getY(),c.getZ());}
      case "unloaded_identity"->c.discard();
      default->throw new IllegalStateException(selection);
    }
    h.assertTrue(command(p,"chao "+target+" -100 Run 12",0)==expected,"Explicit selection result: "+selection);
    h.assertTrue(expected==1?c.state().align==-1&&c.state().levels[ChaoState.RUN]==12:c.state().encode().equals(before),"Selection cannot silently edit wrong or ambiguous target");h.succeed();
  }
  private static void commandGate(GameTestHelper h,String gate) {
    var p=player(h);var c=actor(h,p);int permission=0;boolean allow=false;
    switch(gate) {
      case "survival"->p.setGameMode(GameType.SURVIVAL);
      case "forged_creative_abilities"->{p.setGameMode(GameType.SURVIVAL);p.getAbilities().instabuild=true;}
      case "spectator"->p.setGameMode(GameType.SPECTATOR);
      case "creative_nonop"->allow=true;
      case "survival_operator"->{p.setGameMode(GameType.SURVIVAL);permission=2;allow=true;}
    }
    String before=c.state().encode();h.assertTrue(command(p,"chao ~ 0 Luck 99",permission)==(allow?1:0),"Authoritative command gate: "+gate);
    h.assertTrue(allow?c.state().hiddenLevels[ChaoState.HIDDEN_LUCK]==99:c.state().encode().equals(before),"Denied command cannot mutate state");h.succeed();
  }
  private static void invalid(GameTestHelper h,String invalid) {
    var p=player(h);var c=actor(h,p);String before=c.state().encode();
    String suffix=switch(invalid) {case "stat"->"0 Stamina 9";case "alignment_low"->"-101 Run 9";case "alignment_high"->"101 Run 9";case "level_low"->"0 Run -1";case "level_high"->"0 Run 100";default->"0 Run";};
    h.assertTrue(command(p,"chao ~ "+suffix,0)==0&&c.state().encode().equals(before),"Invalid argument is atomic: "+invalid);h.succeed();
  }
  private static void adult(GameTestHelper h) {
    var p=player(h);var c=actor(h,p);var s=c.state();s.adult=true;s.align=-.802f;s.primaryType=ChaoState.RUN_TYPE;s.setEvolutionStrength(.6f);
    c.setState(ChaoState.decode(s.encode()));s=c.state();String before=s.encode();
    h.assertTrue(command(p,"chao ~ 100 Run 99",0)==0&&s.encode().equals(before),"Adult opposite alignment rejects whole edit");
    h.assertTrue(command(p,"chao ~ -80 Run 99",0)==1&&s.levels[ChaoState.RUN]==99&&s.lockedAlignment()==-.802f&&s.lockedPrimaryType()==ChaoState.RUN_TYPE,"Current locked displayed alignment permits stat test without replacing identity");
    persisted(h,c);h.succeed();
  }
  private static void ties(GameTestHelper h) {
    var p=player(h);var c=actor(h,p);
    command(p,"chao ~ 0 Run 10",0);command(p,"chao ~ 0 Fly 10",0);
    h.assertTrue(c.state().prospectivePrimaryType()==ChaoState.FLY_TYPE,"Most recently actually raised tie selected");
    command(p,"chao ~ 0 Run 10",0);h.assertTrue(c.state().prospectivePrimaryType()==ChaoState.FLY_TYPE,"Same value does not invent recency");
    command(p,"chao ~ 0 Fly 9",0);h.assertTrue(c.state().prospectivePrimaryType()==ChaoState.RUN_TYPE,"Decrease updates highest earned stat naturally");h.succeed();
  }
  private static boolean maturity(ServerPlayer p,ChaoEntity c,int value) {
    return ChaoCareNetwork.editMaturity(p,new ChaoCareNetwork.MaturityEdit(c.getUUID().toString(),value));
  }
  private static void maturity(GameTestHelper h,boolean adult,int value) {
    var p=player(h);var c=actor(h,p);var s=c.state();s.adult=adult;s.align=.8f;s.primaryType=ChaoState.RUN_TYPE;s.adultAgeTicks=456;s.setEvolutionStrength(.4f);
    String before=s.encode();boolean allowed=adult||value<=990;
    h.assertTrue(maturity(p,c,value)==allowed,"Current-stage maturity bound");
    if(allowed) {
      h.assertTrue(Math.abs(s.evolutionStrength()-value/1000f*(adult?1.2f:1))<.00001,"Authoritative displayed growth set");
      h.assertTrue(s.adult==adult&&s.ageTicks==1234&&s.adultAgeTicks==456&&s.cocoonTicks==0&&!s.dead&&s.reincarnations==0,"No lifespan or identity transition from bar");
      persisted(h,c);
    } else h.assertTrue(s.encode().equals(before),"Rejected child100% cannot trigger evolution");
    h.succeed();
  }
  private static void maturityGate(GameTestHelper h,String gate) {
    var p=player(h);var c=actor(h,p);int value=500;String id=c.getUUID().toString();
    switch(gate) {
      case "survival"->p.setGameMode(GameType.SURVIVAL);
      case "forged_creative_abilities"->{p.setGameMode(GameType.SURVIVAL);p.getAbilities().instabuild=true;}
      case "spectator"->p.setGameMode(GameType.SPECTATOR);
      case "no_analyzer"->p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
      case "far"->p.teleportTo(c.getX()+8.01,c.getY(),c.getZ());
      case "cocoon"->c.state().cocoonTicks=200;
      case "busy"->c.getPersistentData().putBoolean("ChaoActivity",true);
      case "dead"->c.state().dead=true;
      case "immortal"->c.state().immortal=true;
      case "malformed"->id="not-a-uuid";
      case "missing"->id=UUID.randomUUID().toString();
      case "numeric_low"->value=-1;
      case "numeric_high"->value=1001;
    }
    String before=c.state().encode();boolean accepted=ChaoCareNetwork.editMaturity(p,new ChaoCareNetwork.MaturityEdit(id,value));
    c.getPersistentData().remove("ChaoActivity");
    h.assertFalse(accepted,"Actual maturity payload authorization: "+gate);
    h.assertTrue(c.state().encode().equals(before),"Forged drag has no saved side effects");h.succeed();
  }
  private static void codec(GameTestHelper h) {
    var access=h.getLevel().registryAccess();String uuid=UUID.randomUUID().toString();
    for(int value:new int[]{0,500,990,1000,-1,Integer.MAX_VALUE}) {
      var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),access);
      try {var input=new ChaoCareNetwork.MaturityEdit(uuid,value);ChaoCareNetwork.MaturityEdit.CODEC.encode(buffer,input);
        h.assertTrue(ChaoCareNetwork.MaturityEdit.CODEC.decode(buffer).equals(input),"Wire preserves integer, handler validates bounds");
      }finally{buffer.release();}
    }
    var truncated=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),access);boolean rejected=false;
    try {truncated.writeUtf(uuid,36);ChaoCareNetwork.MaturityEdit.CODEC.decode(truncated);}catch(Exception malformed){rejected=true;}finally{truncated.release();}
    h.assertTrue(rejected,"Truncated maturity payload cannot decode");h.succeed();
  }
}
