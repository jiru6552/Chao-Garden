package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.GardenConfig;
import com.chaogarden.care.ChaoCareNetwork;
import com.chaogarden.core.AnalyzerStatRules;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.DriveRules;
import com.chaogarden.core.HeroRunMorphology052;
import com.chaogarden.entity.ChaoEntity;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Authoritative age/training boundaries for the approved blocky Run path.
 * These cases deliberately test saved policy, actual handlers and transport;
 * real geometry, accessory placement and motion need the separate client pass. */
@GameTestHolder("chao_morph55") @PrefixGameTestTemplate(false)
public final class BlockyRunMorphologyGameTests055 {
  private static final BlockPos AT=new BlockPos(3,2,3);
  // Reuse the existing empty activity structure; no new worldgen fixture.
  private static final String TEMPLATE="chao_morph54:activity_test";
  private BlockyRunMorphologyGameTests055() {}

  @GameTestGenerator public static Collection<TestFunction> cases() {
    var tests=new ArrayList<TestFunction>();
    for(int age=0;age<=4;age++)for(int training=0;training<=2;training++) {
      int a=age,t=training;
      add(tests,"baby_age_"+(age*25)+"_training_"+(training*50),40,h->babyGrid(h,a,t));
    }
    add(tests,"real_baby_clock_then_training_reversal",40,BlockyRunMorphologyGameTests055::babyClock);
    add(tests,"creative_alignment_and_stat_edits_keep_biology",40,BlockyRunMorphologyGameTests055::analyzer);
    add(tests,"combined_transaction_and_loss_use_current_training",40,BlockyRunMorphologyGameTests055::combined);
    add(tests,"accepted053_baby_real_first_cocoon",40,BlockyRunMorphologyGameTests055::firstCocoon);
    for(int index:new int[]{9,11,13,18}) {
      int fixture=index;add(tests,"accepted053_inputs_and_reload_"+index,40,h->legacy(h,fixture));
    }
    for(int sign:new int[]{-1,1}) {
      int axis=sign;add(tests,"compatible_secondary_axis_"+(sign<0?"swim":"fly"),40,h->compatible(h,axis));
    }
    add(tests,"adult_reversal_keeps_locked_identity_and_age",40,BlockyRunMorphologyGameTests055::adultReverse);
    return tests;
  }

  private static void add(List<TestFunction> tests,String name,int timeout,java.util.function.Consumer<GameTestHelper> body) {
    tests.add(new TestFunction("morph055_"+(tests.size()/8),name,TEMPLATE,timeout,0,true,body));
  }
  private static void cleanup(GameTestHelper h,Runnable action) {
    h.testInfo.addListener(new GameTestListener() {
      public void testStructureLoaded(GameTestInfo i) {}
      public void testPassed(GameTestInfo i,GameTestRunner r) {action.run();}
      public void testFailed(GameTestInfo i,GameTestRunner r) {action.run();}
      public void testAddedForRerun(GameTestInfo a,GameTestInfo b,GameTestRunner r) {action.run();}
    });
  }
  private static ServerPlayer keeper(GameTestHelper h,boolean creative) {
    var p=TestConnections.makeSurvivalServerPlayer(h);if(creative)p.setGameMode(GameType.CREATIVE);
    p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ChaoGarden.item("chao_analyzer")));
    cleanup(h,()->{if(p.server.getPlayerList().getPlayer(p.getUUID())==p)p.server.getPlayerList().remove(p);});return p;
  }
  private static ChaoEntity actor(GameTestHelper h,ServerPlayer p,ChaoState state) {
    for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)h.setBlock(AT.offset(x,-1,z),Blocks.STONE);
    var c=ChaoGarden.CHAO.get().create(h.getLevel());h.assertTrue(c!=null,"Real Chao creates");
    c.setState(state);c.owner=p.getUUID();c.setNoAi(true);c.setNoGravity(true);
    c.moveTo(Vec3.atBottomCenterOf(h.absolutePos(AT)));h.getLevel().addFreshEntity(c);cleanup(h,c::discard);
    p.teleportTo(c.getX()+1,c.getY(),c.getZ());return c;
  }
  private static ChaoState baby(float maturity,int run) {
    var s=new ChaoState();s.align=.8f;s.ageTicks=123;s.hunger=80;s.energy=100;s.happiness=70;
    s.setEvolutionStrength(maturity);s.train(ChaoState.RUN,run,new Random(55));
    s.setTrait("morph55.foreign","preserve this unrelated earned record");return s;
  }
  private static ChaoState adult() {
    var s=new ChaoState();s.adult=true;s.primaryType=ChaoState.RUN_TYPE;s.align=.8f;
    s.ageTicks=4321;s.adultAgeTicks=876;s.hunger=80;s.energy=100;s.setEvolutionStrength(.6f);
    s.train(ChaoState.RUN,960,new Random(55));s.setTrait("morph55.foreign","preserve this unrelated earned record");
    // Encoding fixes first identity through the same ordinary persistence path.
    return ChaoState.decode(s.encode());
  }
  private static void near(GameTestHelper h,float actual,double expected,String why) {
    h.assertTrue(Math.abs(actual-expected)<.00001,why+": "+actual+" vs "+expected);
  }
  private static void unchangedBiology(GameTestHelper h,ChaoState s,int age,int adultAge,float strength) {
    h.assertTrue(s.ageTicks==age&&s.adultAgeTicks==adultAge&&s.evolutionStrength()==strength,
        "A training/alignment edit cannot rewrite biological age or maturity");
  }
  private static void assertSyncAndNbt(GameTestHelper h,ChaoEntity c) {
    c.sync();String encoded=c.state().encode();var sample=HeroRunMorphology052.sample(c.state());
    var updates=c.getEntityData().getNonDefaultValues();
    h.assertTrue(updates!=null&&updates.stream().anyMatch(v->v.value() instanceof String text&&text.equals(encoded)),
        "Server entity data contains the complete authoritative age/training state");
    var observer=ChaoGarden.CHAO.get().create(h.getLevel());observer.getEntityData().assignValues(updates);
    h.assertTrue(observer.getEntityData().getNonDefaultValues().stream().anyMatch(v->v.value() instanceof String text&&text.equals(encoded)),
        "Observer receives the same authoritative serialized values");
    var tag=new CompoundTag();c.saveWithoutId(tag);var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(tag);
    h.assertTrue(restored.state().encode().equals(encoded)&&restored.getUUID().equals(c.getUUID())&&Objects.equals(restored.owner,c.owner),
        "Real entity NBT preserves every saved field, owner and UUID");
    h.assertTrue(HeroRunMorphology052.sample(restored.state()).equals(sample),
        "Reload derives exactly the same raw and age-conditioned morphology inputs");
    observer.discard();restored.discard();
  }

  private static void babyGrid(GameTestHelper h,int age,int training) {
    var p=keeper(h,false);var s=baby(age*.25f,training*240);var c=actor(h,p,s);String before=s.encode();
    var sample=HeroRunMorphology052.sample(s);
    h.assertTrue(sample.enabled()&&!sample.adult(),"Ordinary Hero baby remains on the continuous path");
    near(h,sample.babyRun(),training*.5,"Raw Run training target remains independent of age");
    near(h,sample.maturity(),age*.25,"The visible-age input comes from existing biological maturity");
    near(h,sample.babyGrowth(),age*.25*training*.5,"Age-conditioned visibility cannot skip either independent input");
    h.assertTrue(before.equals(s.encode()),"Reading the visual policy creates no state or earned reward");
    assertSyncAndNbt(h,c);h.succeed();
  }
  private static void babyClock(GameTestHelper h) {
    var p=keeper(h,false);var s=baby(0,480);s.ageTicks=0;s.setEvolutionStrength(0);
    // Exercise ordinary code with a shortened fixture duration, not a config
    // edit or a claim of watching the full natural multi-hour lifecycle.
    for(int tick=1;tick<=15;tick++) {
      h.assertTrue(s.tick(20,100000,new Random(tick))==ChaoState.Transition.NONE,"Fixture stays a baby before its real duration");
      if(tick%5==0)near(h,HeroRunMorphology052.sample(s).babyGrowth(),tick/20.0,"Existing clock reveals the fixed training gradually");
    }
    var c=actor(h,p,s);int age=s.ageTicks,adultAge=s.adultAgeTicks;float maturity=s.evolutionStrength();
    DriveRules.change(s,ChaoState.RUN,-240,new Random(55));
    near(h,HeroRunMorphology052.sample(s).babyRun(),.5,"Real current-stat decrease halves the target");
    near(h,HeroRunMorphology052.sample(s).babyGrowth(),.375,"Training loss recedes visibility at the same biological age");
    DriveRules.change(s,ChaoState.RUN,240,new Random(55));
    near(h,HeroRunMorphology052.sample(s).babyGrowth(),.75,"Restoring the same training restores the same visibility");
    unchangedBiology(h,s,age,adultAge,maturity);assertSyncAndNbt(h,c);h.succeed();
  }
  private static void analyzer(GameTestHelper h) {
    var p=keeper(h,true);var s=baby(.5f,240);var c=actor(h,p,s);
    int age=s.ageTicks,adultAge=s.adultAgeTicks;float maturity=s.evolutionStrength();
    for(int alignment:new int[]{-1000,0,1000}) {
      var expected=ChaoState.decode(s.encode());expected.align=alignment/1000f;
      h.assertTrue(ChaoCareNetwork.editAlignment(p,new ChaoCareNetwork.AlignmentEdit(c.getUUID().toString(),alignment)),
          "Actual Creative Analyzer alignment handler accepts each different baby side");
      h.assertTrue(s.encode().equals(expected.encode()),"Alignment drag changes only its authorized saved alignment");
      near(h,HeroRunMorphology052.sample(s).babyGrowth(),.25,"Alignment cannot erase or invent training/age development");
    }
    h.assertTrue(ChaoCareNetwork.editStats(p,new ChaoCareNetwork.StatEdit(c.getUUID().toString(),ChaoState.RUN,AnalyzerStatRules.LEVEL,1)),
        "Actual Creative stat handler raises Run");
    near(h,HeroRunMorphology052.sample(s).babyGrowth(),340.0/480*.5,"A level increase is visible without a drive-specific hook");
    h.assertTrue(ChaoCareNetwork.editStats(p,new ChaoCareNetwork.StatEdit(c.getUUID().toString(),ChaoState.POWER,AnalyzerStatRules.LEVEL,1)),
        "Actual opposing Power increase applies");
    near(h,HeroRunMorphology052.sample(s).babyGrowth(),.25,"Opposing Power recedes the same age-conditioned Run appearance");
    p.setGameMode(GameType.SURVIVAL);String before=s.encode();
    h.assertFalse(ChaoCareNetwork.editAlignment(p,new ChaoCareNetwork.AlignmentEdit(c.getUUID().toString(),-1000)),
        "Switching to Survival immediately revokes the Creative alignment authority");
    h.assertTrue(before.equals(s.encode()),"Denied alignment request is a complete saved-state no-op");
    unchangedBiology(h,s,age,adultAge,maturity);assertSyncAndNbt(h,c);h.succeed();
  }
  private static void combined(GameTestHelper h) {
    var p=keeper(h,false);var s=baby(.75f,240);var c=actor(h,p,s);
    int age=s.ageTicks,adultAge=s.adultAgeTicks;float maturity=s.evolutionStrength();
    h.assertTrue(DriveRules.apply(s,DriveRules.Tier.CHAOTIC,-1,new Random(55)),"Actual balanced combined drive applies");
    near(h,HeroRunMorphology052.sample(s).babyGrowth(),.375,"Equal Run/Power gains do not invent net visual preference");
    h.assertTrue(s.lastTrainedStat()==ChaoState.RUN,"One combined transaction preserves the prior tied-stat recency instead of loop order");
    var loss=new Random(55){@Override public int nextInt(int bound){return bound==100?99:0;}};
    h.assertTrue(DriveRules.apply(s,DriveRules.Tier.UNSTABLE_SOLID,ChaoState.RUN,loss),"Actual unstable Run loss applies");
    near(h,HeroRunMorphology052.sample(s).babyGrowth(),0,"Combined-drive loss immediately removes current Run visibility");
    unchangedBiology(h,s,age,adultAge,maturity);assertSyncAndNbt(h,c);h.succeed();
  }

  private record Source(int index,String sha,String encoded) {}
  private static Source saved(int index) {
    try(var input=BlockyRunMorphologyGameTests055.class.getResourceAsStream("/data/chao_morph54/states_053.json")) {
      if(input==null)throw new IllegalStateException("Missing accepted .53 source records");
      var json=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
      if(!json.get("source_commit").getAsString().equals("ddec17ce70988168874fbebfe37528efa7bc29e5"))throw new IllegalStateException("Wrong delivered source baseline");
      for(var entry:json.getAsJsonArray("states")) {
        var v=entry.getAsJsonObject();if(v.get("case_index").getAsInt()==index)return new Source(index,v.get("sha256").getAsString(),v.get("encoded").getAsString());
      }
      throw new IllegalStateException("Missing accepted source "+index);
    }catch(Exception failure){throw new IllegalStateException("Cannot read accepted source",failure);}
  }
  private static void legacy(GameTestHelper h,int index) {
    var source=saved(index);
    try{h.assertTrue(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source.encoded().getBytes(StandardCharsets.UTF_8))).equals(source.sha()),
        "Literal accepted .53 fixture matches its original hash");}
    catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    var s=ChaoState.decode(source.encoded());h.assertTrue(s.encode().equals(source.encoded()),"New derived visibility adds no migration or persistent-field rewrite");
    var sample=HeroRunMorphology052.sample(s);
    if(s.adult)near(h,sample.babyGrowth(),0,"Accepted adults never render baby-only growth");
    else near(h,sample.babyGrowth(),sample.babyRun()*sample.maturity(),"Older babies derive age-conditioned appearance from retained values");
    var p=keeper(h,false);var c=actor(h,p,s);assertSyncAndNbt(h,c);h.succeed();
  }
  private static void firstCocoon(GameTestHelper h) {
    var source=saved(11);var s=ChaoState.decode(source.encoded());
    s.ageTicks=GardenConfig.CHILD_TICKS.get()-1;s.setEvolutionStrength(.9f);
    int[] levels=s.levels.clone(),progress=s.progress.clone(),points=s.points.clone();String lineage=s.lineageId();
    var p=keeper(h,false);var c=actor(h,p,s);c.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.SKELETON_SKULL));
    near(h,HeroRunMorphology052.sample(s).babyGrowth(),.9,"The accepted trained baby shows its late child development before evolution");
    h.runAtTickTime(4,()->{
      h.assertTrue(s.adult&&s.primaryType==ChaoState.RUN_TYPE&&s.lockedAlignment()>=.5f&&s.cocoonTicks>0,
          "Actual entity starts its ordinary Hero Run first cocoon");
      near(h,HeroRunMorphology052.sample(s).babyGrowth(),0,"Adult identity no longer samples baby-only additions");
      near(h,HeroRunMorphology052.sample(s).adultGrowth(),0,"Fresh adult starts at its short first form, not mature Run/Run");
      h.assertTrue(s.adultAgeTicks==0&&s.evolutionStrength()==0&&Arrays.equals(s.levels,levels)&&Arrays.equals(s.progress,progress)&&Arrays.equals(s.points,points),
          "First evolution resets adult biology without secretly changing earned training");
      h.assertTrue(s.lineageId().equals(lineage)&&c.getItemBySlot(EquipmentSlot.HEAD).is(Items.SKELETON_SKULL),
          "The real cocoon preserves lineage and its actual existing hat");
      assertSyncAndNbt(h,c);h.succeed();
    });
  }
  private static void compatible(GameTestHelper h,int axis) {
    var p=keeper(h,true);var s=adult();s.swimFly=.75f*axis;s.runPower=-.75f;var c=actor(h,p,s);
    int secondary=axis<0?ChaoState.SWIM_TYPE:ChaoState.FLY_TYPE;
    h.assertTrue(Arrays.stream(s.secondaryTypes()).anyMatch(v->v==secondary)&&Arrays.stream(s.secondaryTypes()).anyMatch(v->v==ChaoState.RUN_TYPE),
        "Existing independent secondary axes retain a compatible Swim/Run or Fly/Run pair");
    near(h,HeroRunMorphology052.sample(s).adultGrowth(),.5,"Compatible Swim/Fly does not erase current Run training or biological maturity");
    s.align=-1;s.primaryType=ChaoState.POWER_TYPE;c.sync();
    h.assertTrue(s.lockedAlignment()==.8f&&s.align==.8f&&s.primaryType==ChaoState.RUN_TYPE,
        "Stale public edits cannot rewrite the saved Hero Run first identity");
    h.assertTrue(s.swimFly==.75f*axis&&s.runPower==-.75f,"Repairing identity does not silently erase compatible axes");
    String before=s.encode();h.assertFalse(ChaoCareNetwork.editAlignment(p,new ChaoCareNetwork.AlignmentEdit(c.getUUID().toString(),-1000)),
        "Even Creative control is read-only after first evolution");
    h.assertTrue(before.equals(s.encode()),"Rejected adult request leaves every saved axis and maturity field unchanged");
    assertSyncAndNbt(h,c);h.succeed();
  }
  private static void adultReverse(GameTestHelper h) {
    var p=keeper(h,true);var s=adult();var c=actor(h,p,s);
    int age=s.ageTicks,adultAge=s.adultAgeTicks;float maturity=s.evolutionStrength();
    near(h,HeroRunMorphology052.sample(s).adultGrowth(),.5,"Baseline has full training at half adult biological maturity");
    h.assertTrue(ChaoCareNetwork.editStats(p,new ChaoCareNetwork.StatEdit(c.getUUID().toString(),ChaoState.RUN,AnalyzerStatRules.LEVEL,-10)),
        "Actual adult Analyzer decrease applies");
    near(h,HeroRunMorphology052.sample(s).adultGrowth(),60.0/960*.5,"Level editing retains the real partial progress bar rather than silently erasing it");
    h.assertTrue(ChaoCareNetwork.editStats(p,new ChaoCareNetwork.StatEdit(c.getUUID().toString(),ChaoState.RUN,AnalyzerStatRules.PROGRESS,0)),
        "Actual Analyzer progress edit removes the remaining partial bar");
    near(h,HeroRunMorphology052.sample(s).adultGrowth(),0,"Losing Run training removes specialized appearance at unchanged age");
    h.assertTrue(ChaoCareNetwork.editStats(p,new ChaoCareNetwork.StatEdit(c.getUUID().toString(),ChaoState.RUN,AnalyzerStatRules.LEVEL,10)),
        "Actual adult Analyzer restoration applies");
    near(h,HeroRunMorphology052.sample(s).adultGrowth(),.5,"Restoring training cannot skip remaining biological maturity");
    unchangedBiology(h,s,age,adultAge,maturity);
    h.assertTrue(s.lockedPrimaryType()==ChaoState.RUN_TYPE&&s.lockedAlignment()==.8f,"Reversible specialization keeps first type and alignment");
    assertSyncAndNbt(h,c);h.succeed();
  }
}
