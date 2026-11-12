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
import java.util.UUID;
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

/** The .54 render update must not fork the saved .53 gameplay journey.
 * Uses accepted .53 client/NBT-validated snapshots; no user save is edited.
 * Actual silhouette and elapsed-life visuals are verified separately in a client. */
@GameTestHolder("chao_morph54") @PrefixGameTestTemplate(false)
public final class HeroRunJourneyGameTests054 {
  private static final BlockPos AT=new BlockPos(3,2,3);
  private static final String TEMPLATE="chao_morph54:activity_test";
  private HeroRunJourneyGameTests054() {}

  @GameTestGenerator public static Collection<TestFunction> cases() {
    var tests=new ArrayList<TestFunction>();
    add(tests,"accepted053_snapshots_are_byte_exact",40,HeroRunJourneyGameTests054::snapshots);
    add(tests,"accepted053_analyzer_baby_real_first_cocoon",40,h->firstEvolution(h,11,false));
    add(tests,"accepted053_dna_baby_real_first_cocoon",40,h->firstEvolution(h,18,true));
    add(tests,"saved_baby_to_young_mature_reversible_adult",40,HeroRunJourneyGameTests054::journey);
    add(tests,"continued_combined_and_analyzer_rules",40,HeroRunJourneyGameTests054::continuedTraining);
    add(tests,"accepted053_adult_stays_locked_after_reload",40,HeroRunJourneyGameTests054::oldAdult);
    add(tests,"coat_parts_and_cosmetics_share_policy_inputs",40,HeroRunJourneyGameTests054::coatIndependence);
    add(tests,"run_appearance_does_not_force_first_type",40,HeroRunJourneyGameTests054::otherFirstType);
    return tests;
  }
  private static void add(List<TestFunction> tests,String name,int timeout,java.util.function.Consumer<GameTestHelper> body) {
    tests.add(new TestFunction("journey054",name,TEMPLATE,timeout,0,true,body));
  }
  private static void cleanup(GameTestHelper h,Runnable action) {
    h.testInfo.addListener(new GameTestListener() {
      public void testStructureLoaded(GameTestInfo i) {}
      public void testPassed(GameTestInfo i,GameTestRunner r) {action.run();}
      public void testFailed(GameTestInfo i,GameTestRunner r) {action.run();}
      public void testAddedForRerun(GameTestInfo a,GameTestInfo b,GameTestRunner r) {action.run();}
    });
  }
  private static ServerPlayer keeper(GameTestHelper h) {
    var p=TestConnections.makeSurvivalServerPlayer(h);
    p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ChaoGarden.item("chao_analyzer")));
    cleanup(h,()->{if(p.server.getPlayerList().getPlayer(p.getUUID())==p)p.server.getPlayerList().remove(p);});return p;
  }
  private static ChaoEntity actor(GameTestHelper h,ServerPlayer p,ChaoState state) {
    for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)h.setBlock(AT.offset(x,-1,z),Blocks.STONE);
    var c=ChaoGarden.CHAO.get().create(h.getLevel());h.assertTrue(c!=null,"Actual Chao creates");
    c.setState(state);c.owner=p.getUUID();c.setNoAi(true);c.setNoGravity(true);c.moveTo(Vec3.atBottomCenterOf(h.absolutePos(AT)));
    h.getLevel().addFreshEntity(c);cleanup(h,c::discard);p.teleportTo(c.getX()+1,c.getY(),c.getZ());return c;
  }
  private static void near(GameTestHelper h,float actual,double expected,String message) {
    h.assertTrue(Math.abs(actual-expected)<.00001,message+": "+actual+" vs "+expected);
  }
  private record Source(int caseIndex,String role,String sha256,String encoded) {}
  private static List<Source> sources() {
    try(var input=HeroRunJourneyGameTests054.class.getResourceAsStream("/data/chao_morph54/states_053.json")) {
      if(input==null)throw new IllegalStateException("Missing accepted .53 source states");
      var json=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
      if(!json.get("source_commit").getAsString().equals("ddec17ce70988168874fbebfe37528efa7bc29e5"))throw new IllegalStateException("Wrong delivered .53 baseline");
      var result=new ArrayList<Source>();
      for(var entry:json.getAsJsonArray("states")) {
        var value=entry.getAsJsonObject();result.add(new Source(value.get("case_index").getAsInt(),value.get("role").getAsString(),value.get("sha256").getAsString(),value.get("encoded").getAsString()));
      }
      return result;
    }catch(Exception failure){throw new IllegalStateException("Cannot read accepted .53 source states",failure);}
  }
  private static ChaoState saved(int index) {
    return ChaoState.decode(sources().stream().filter(source->source.caseIndex()==index).findFirst().orElseThrow().encoded());
  }
  private static String heritage(ChaoState s) {
    return Arrays.toString(s.colorGenes)+Arrays.toString(s.toneGenes)+Arrays.toString(s.shinyGenes)
        +Arrays.toString(s.jewelGenes)+":"+s.expressedColor+":"+s.expressedJewel+":"+s.twoTone+":"+s.shiny+":"+s.lineageId();
  }
  private record Baseline(String heritage,int dna,int partMask,int[] parts,int[] levels,int[] progress,int[] points,
                          int fruit,int drives,int pets,float alignment,int runGrade) {}
  private static Baseline before(ChaoState s) {
    return new Baseline(heritage(s),s.animalMask,s.animalPartsMask,s.animalParts.clone(),s.levels.clone(),s.progress.clone(),s.points.clone(),
        s.fruitCount,s.driveCount,s.petCount,s.align,s.grades[ChaoState.RUN]);
  }
  private static void inherited(GameTestHelper h,ChaoState s,Baseline old) {
    h.assertTrue(heritage(s).equals(old.heritage())&&s.animalMask==old.dna()&&s.animalPartsMask==old.partMask()
        &&Arrays.equals(s.animalParts,old.parts()),"The render revision cannot replace lineage, coat heritage or recorded DNA/parts");
  }
  private static void assertSyncAndNbt(GameTestHelper h,ChaoEntity c) {
    c.sync();String encoded=c.state().encode();var updates=c.getEntityData().getNonDefaultValues();
    h.assertTrue(updates!=null&&updates.stream().anyMatch(value->value.value() instanceof String text&&text.equals(encoded)),
        "Actual server entity data publishes the complete authoritative journey state");
    var observer=ChaoGarden.CHAO.get().create(h.getLevel());observer.getEntityData().assignValues(updates);
    h.assertTrue(observer.getEntityData().getNonDefaultValues().stream().anyMatch(value->value.value() instanceof String text&&text.equals(encoded)),
        "An observer receives the same authoritative progression inputs");
    var tag=new CompoundTag();c.saveWithoutId(tag);var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(tag);
    h.assertTrue(restored.state().encode().equals(encoded)&&restored.getUUID().equals(c.getUUID())&&Objects.equals(restored.owner,c.owner),
        "Entity NBT keeps all journey values, UUID and owner without a new persistence format");
    h.assertTrue(HeroRunMorphology052.sample(restored.state()).equals(HeroRunMorphology052.sample(c.state())),
        "Reload derives exactly the same morphology policy inputs");
    observer.discard();restored.discard();
  }
  private static void snapshots(GameTestHelper h) {
    var records=sources();h.assertTrue(records.stream().map(Source::caseIndex).toList().equals(List.of(9,11,13,18)),"Use the accepted .53 baby, actual Analyzer edit, adult and DNA reference states");
    var p=keeper(h);
    for(var source:records) {
      try{h.assertTrue(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source.encoded().getBytes(StandardCharsets.UTF_8))).equals(source.sha256()),"Prior accepted state matches its literal source SHA: "+source.role());}
      catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
      var state=ChaoState.decode(source.encoded());h.assertTrue(state.encode().equals(source.encoded()),"Accepted .53 source roundtrips byte-exactly: "+source.role());
      var c=actor(h,p,state);assertSyncAndNbt(h,c);c.discard();
    }
    h.succeed();
  }
  private static ChaoEntity prepareEvolution(GameTestHelper h,ServerPlayer p,int sourceIndex) {
    var state=saved(sourceIndex);h.assertTrue(!state.adult&&state.align>=.5f,"Source fixture is a real accepted Hero baby");
    near(h,HeroRunMorphology052.sample(state).babyRun(),1,"Source baby has its documented twenty net ordinary-drive units");
    // Only the fixture clock is moved to the real host's next first-evolution tick.
    state.ageTicks=GardenConfig.CHILD_TICKS.get()-1;state.setEvolutionStrength(.9f);return actor(h,p,state);
  }
  private static void fresh(GameTestHelper h,ChaoEntity c,Baseline old) {
    var s=c.state();h.assertTrue(s.adult&&!s.immortal&&s.primaryType==ChaoState.RUN_TYPE&&s.cocoonTicks>0,
        "The actual entity starts its existing first cocoon as an ordinary Hero Run adult");
    near(h,s.lockedAlignment(),old.alignment(),"First evolution freezes the same accepted Hero alignment");
    h.assertTrue(s.adultAgeTicks==0&&s.evolutionStrength()==0,"Newly evolved adult begins at its existing young biological state");
    h.assertTrue(Arrays.equals(s.levels,old.levels())&&Arrays.equals(s.progress,old.progress())&&Arrays.equals(s.points,old.points()),
        "First evolution does not silently train or lower any visible ability");
    h.assertTrue(s.grades[ChaoState.RUN]==Math.min(5,old.runGrade()+1),"The existing first-type Run grade upgrade remains");
    h.assertTrue(s.fruitCount==old.fruit()&&s.driveCount==old.drives()&&s.petCount==old.pets(),"The visual revision grants no extra care/item rewards");
    inherited(h,s,old);var sample=HeroRunMorphology052.sample(s);
    near(h,sample.specialization(),.5,"Baby480Run progress carries into the existing adult960-progress target");
    near(h,sample.maturity(),0,"Fresh adult maturity is zero");near(h,sample.adultGrowth(),0,"Fresh first form has not already jumped to its Run/Run endpoint");
  }
  private static void firstEvolution(GameTestHelper h,int sourceIndex,boolean skull) {
    var p=keeper(h);var c=prepareEvolution(h,p,sourceIndex);var old=before(c.state());
    if(skull)c.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.SKELETON_SKULL));
    h.runAtTickTime(4,()->{
      fresh(h,c,old);if(skull)h.assertTrue(c.getItemBySlot(EquipmentSlot.HEAD).is(Items.SKELETON_SKULL),"Real first cocoon preserves the existing vanilla hat");
      assertSyncAndNbt(h,c);h.succeed();
    });
  }
  private static void matureHalf(GameTestHelper h,ChaoState s) {
    // Exercise the unchanged production clock at a shortened rule-test duration,
    // not a claim of watching multi-hour natural aging or a server config edit.
    s.cocoonTicks=0;int age=s.ageTicks,adultAge=s.adultAgeTicks;
    var random=new Random(54);for(int i=0;i<48;i++)h.assertTrue(s.tick(20,100000,random)==ChaoState.Transition.NONE,"Shortened existing clock does not start another evolution");
    h.assertTrue(s.ageTicks==age+48&&s.adultAgeTicks==adultAge+48,"Biological counters advance only by the exercised production ticks");
    near(h,s.evolutionStrength(),.6,"48ticks at one-quarter of a20tick child duration retain strength .6");
    near(h,HeroRunMorphology052.sample(s).maturity(),.5,"Strength .6 is half of the existing1.2adult maturity cap");
  }
  private static void rejectAdultAlignment(GameTestHelper h,ServerPlayer p,ChaoEntity c) {
    p.setGameMode(GameType.CREATIVE);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ChaoGarden.item("chao_analyzer")));
    String before=c.state().encode();
    h.assertFalse(ChaoCareNetwork.editAlignment(p,new ChaoCareNetwork.AlignmentEdit(c.getUUID().toString(),-1000)),
        "The actual Creative Analyzer remains locked after this first evolution");
    h.assertTrue(before.equals(c.state().encode()),"Rejected adult alignment changes no saved field");
  }
  private static void journey(GameTestHelper h) {
    var p=keeper(h);var c=prepareEvolution(h,p,11);var old=before(c.state());
    h.runAtTickTime(4,()->{
      fresh(h,c,old);assertSyncAndNbt(h,c);var s=c.state();matureHalf(h,s);
      near(h,HeroRunMorphology052.sample(s).adultGrowth(),.25,"Half-trained and half-mature adult continues to quarter Run/Run growth");
      int age=s.ageTicks,adultAge=s.adultAgeTicks;float maturity=s.evolutionStrength();
      p.setGameMode(GameType.CREATIVE);
      h.assertTrue(ChaoCareNetwork.editStats(p,new ChaoCareNetwork.StatEdit(c.getUUID().toString(),ChaoState.RUN,AnalyzerStatRules.LEVEL,10)),"Actual Creative stat handler continues the source adult's Run training");
      near(h,HeroRunMorphology052.sample(s).specialization(),1,"Current Run training reaches the existing full adult target");
      near(h,HeroRunMorphology052.sample(s).adultGrowth(),.5,"Training cannot skip the independent half-mature body");
      h.assertTrue(ChaoCareNetwork.editStats(p,new ChaoCareNetwork.StatEdit(c.getUUID().toString(),ChaoState.RUN,AnalyzerStatRules.LEVEL,-10)),"Actual Analyzer loss reverses the added training");
      near(h,HeroRunMorphology052.sample(s).adultGrowth(),.25,"Undoing the same training restores the same adult growth input");
      h.assertTrue(s.ageTicks==age&&s.adultAgeTicks==adultAge&&s.evolutionStrength()==maturity,"Stat editing does not make the adult biologically younger");
      inherited(h,s,old);rejectAdultAlignment(h,p,c);assertSyncAndNbt(h,c);h.succeed();
    });
  }
  private static void continuedTraining(GameTestHelper h) {
    var p=keeper(h);var c=prepareEvolution(h,p,18);var old=before(c.state());
    h.runAtTickTime(4,()->{
      fresh(h,c,old);var s=c.state();matureHalf(h,s);int age=s.ageTicks,adultAge=s.adultAgeTicks;float maturity=s.evolutionStrength();
      h.assertTrue(DriveRules.apply(s,DriveRules.Tier.CHAOTIC,-1,new Random(54)),"Balanced combined drive remains usable after the same first evolution");
      near(h,HeroRunMorphology052.sample(s).specialization(),.5,"Equal Run and Power gains cancel rather than inventing a visual preference");
      var loss=new Random(54){@Override public int nextInt(int bound){return bound==100?99:0;}};
      h.assertTrue(DriveRules.apply(s,DriveRules.Tier.UNSTABLE_SOLID,ChaoState.RUN,loss),"A forced documented unstable loss applies through the real drive policy");
      near(h,HeroRunMorphology052.sample(s).specialization(),0,"An actual Run loss removes specialized training");
      DriveRules.setTotal(s,ChaoState.RUN,DriveRules.total(s,ChaoState.POWER)+960,new Random(54));
      near(h,HeroRunMorphology052.sample(s).adultGrowth(),.5,"Restored net Run training reuses the unchanged half biological maturity");
      DriveRules.setTotal(s,ChaoState.POWER,DriveRules.total(s,ChaoState.RUN),new Random(54));
      near(h,HeroRunMorphology052.sample(s).adultGrowth(),0,"Reversing the opposing Power side also removes Run-specific appearance");
      h.assertTrue(s.ageTicks==age&&s.adultAgeTicks==adultAge&&s.evolutionStrength()==maturity,"Combined gains/losses and opposing training do not rewrite biological age");
      h.assertTrue(s.lockedPrimaryType()==ChaoState.RUN_TYPE,"New stat leaders cannot select a different first type after evolution");
      inherited(h,s,old);rejectAdultAlignment(h,p,c);assertSyncAndNbt(h,c);h.succeed();
    });
  }
  private static void oldAdult(GameTestHelper h) {
    var p=keeper(h);var s=saved(13);var c=actor(h,p,s);String before=s.encode();
    h.assertTrue(s.adult&&s.lockedPrimaryType()==ChaoState.RUN_TYPE,"Prior accepted source is the already locked .53 Hero Run adult");
    near(h,HeroRunMorphology052.sample(s).adultGrowth(),1,"Its existing fully mature/full-trained progression remains representable");
    rejectAdultAlignment(h,p,c);assertSyncAndNbt(h,c);h.assertTrue(before.equals(s.encode()),"Render-only revision does not migrate or rewrite accepted mature .53 state");h.succeed();
  }
  private static void coatIndependence(GameTestHelper h) {
    var original=saved(9);var baseline=HeroRunMorphology052.sample(original);var p=keeper(h);
    for(int variant=0;variant<6;variant++) {
      var s=ChaoState.decode(original.encode());
      if(variant==1){s.expressedColor=2;s.colorGenes=new int[]{2,2};}
      if(variant==2){s.shiny=true;s.shinyGenes=new int[]{1,1};}
      if(variant==3){s.expressedJewel=3;s.jewelGenes=new int[]{3,3};}
      if(variant==4){s.twoTone=false;s.toneGenes=new int[]{0,0};s.negativePalette=true;s.brightCoat=true;s.heroBrushType=3;}
      if(variant==5){s.animalParts[ChaoState.ARMS]=6;s.animalParts[ChaoState.FEET]=6;s.animalParts[ChaoState.WINGS]=17;s.animalMask=(1<<6)|(1<<17);s.animalPartsMask=s.animalMask;}
      String before=s.encode();h.assertTrue(HeroRunMorphology052.sample(s).equals(baseline),"Coats, cosmetic flags and animal parts must not fork the saved age/alignment/Run inputs");
      h.assertTrue(before.equals(s.encode()),"Reading a coated morph remains pure");var c=actor(h,p,s);assertSyncAndNbt(h,c);c.discard();
    }
    h.succeed();
  }
  private static void otherFirstType(GameTestHelper h) {
    var p=keeper(h);var s=saved(9);s.points[ChaoState.FLY]=s.points[ChaoState.RUN]+1;s.points[ChaoState.STAMINA]=9999;
    s.ageTicks=GardenConfig.CHILD_TICKS.get()-1;s.setEvolutionStrength(.9f);var c=actor(h,p,s);
    near(h,HeroRunMorphology052.sample(s).babyRun(),1,"The baby can have maximum Run anatomy before its separate first-type selection");
    h.runAtTickTime(4,()->{
      h.assertTrue(s.adult&&s.primaryType==ChaoState.FLY_TYPE&&!s.immortal,"The current earned-point rule still chooses Fly, even if the rendered baby was strongly Run-developed and Stamina higher");
      h.assertFalse(HeroRunMorphology052.sample(s).enabled(),"The .54 visual path cannot take over an unrelated adult primary form");assertSyncAndNbt(h,c);h.succeed();
    });
  }
}
