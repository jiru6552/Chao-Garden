package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.GardenConfig;
import com.chaogarden.care.ChaoCareNetwork;
import com.chaogarden.core.AnalyzerStatRules;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.DriveRules;
import com.chaogarden.core.HeroRunMorphology052;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoCarrierItem;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Server-hosted policy, actual item/Analyzer commits and older-save boundaries.
 * Geometry, visuals and audio require the separate real-client study. */
@GameTestHolder("chao_morph52") @PrefixGameTestTemplate(false)
public final class HeroRunMorphologyGameTests052 {
  private static final BlockPos AT=new BlockPos(3,2,3);
  private static final String TEMPLATE="chao_morph52:activity_test";
  private HeroRunMorphologyGameTests052() {}

  @GameTestGenerator public static Collection<TestFunction> cases() {
    var tests=new ArrayList<TestFunction>();
    for(int stat=0;stat<4;stat++)for(int alignment=-1;alignment<=1;alignment++) {
      int ability=stat,sign=alignment;
      add(tests,"first_"+ChaoState.STATS[stat].toLowerCase()+"_alignment_"+(alignment+1),40,
          h->firstEvolution(h,ability,sign));
    }
    for(int earlier=0;earlier<4;earlier++)for(int later=0;later<4;later++)if(earlier!=later) {
      int old=earlier,recent=later;
      add(tests,"tied_"+earlier+"_then_"+later,40,h->tieEvolution(h,old,recent));
    }
    for(boolean adult:new boolean[]{false,true})for(int quarter=0;quarter<=4;quarter++) {
      int step=quarter;
      add(tests,"stage_"+(adult?"adult":"baby")+"_"+(25*quarter),40,h->stage(h,adult,step));
    }
    for(int alignment=-1;alignment<=1;alignment++)for(boolean complete:new boolean[]{false,true}) {
      int sign=alignment;
      add(tests,"legacy_chaos_admission_"+(alignment+1)+"_"+complete,40,h->chaosAdmission(h,sign,complete));
    }
    for(boolean adult:new boolean[]{false,true})for(String item:List.of("run_drive","solid_run_drive"))
      add(tests,"held_"+item+"_"+(adult?"adult":"baby"),200,h->heldDrive(h,item,adult));
    for(String tier:List.of("super_solid","super_chaotic","unstable_solid_low","unstable_solid_high","unstable_chaotic_low","perfect_stamina"))
      add(tests,"combined_tier_"+tier,40,h->combinedTier(h,tier));
    add(tests,"untouched_and_partial_history",40,HeroRunMorphologyGameTests052::zeroAndPartialHistory);
    add(tests,"balanced_transaction_retains_prior_tie",40,HeroRunMorphologyGameTests052::balancedTransactions);
    add(tests,"negative_combined_and_reverse_preserve_biology",40,HeroRunMorphologyGameTests052::reversibleSpecialization);
    add(tests,"creative_analyzer_authority_sync_and_reload",40,HeroRunMorphologyGameTests052::analyzer);
    add(tests,"first_identity_and_existing_chaos_reload",40,HeroRunMorphologyGameTests052::lockedIdentity);
    add(tests,"carrier_release_keeps_maturity_and_attachments",100,HeroRunMorphologyGameTests052::carrier);
    add(tests,"schema5_migration_and_reincarnation",40,HeroRunMorphologyGameTests052::migrationAndReincarnation);
    var old=legacyStates();
    for(int index=0;index<old.size();index++) {
      int fixture=index;
      add(tests,"actual_saved_schema6_"+index,40,h->actualLegacy(h,fixture));
    }
    return tests;
  }
  private static void add(List<TestFunction> tests,String name,int timeout,java.util.function.Consumer<GameTestHelper> body) {
    tests.add(new TestFunction("morph052_"+(tests.size()/8),name,TEMPLATE,timeout,0,true,body));
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
    cleanup(h,()->{if(p.server.getPlayerList().getPlayer(p.getUUID())==p)p.server.getPlayerList().remove(p);});
    return p;
  }
  private static ChaoEntity actor(GameTestHelper h,ChaoState state,ServerPlayer owner) {
    for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)h.setBlock(AT.offset(x,-1,z),Blocks.STONE);
    var c=ChaoGarden.CHAO.get().create(h.getLevel());h.assertTrue(c!=null,"Actual Chao creates");
    c.setState(state);c.owner=owner==null?UUID.randomUUID():owner.getUUID();
    c.setNoAi(true);c.setNoGravity(true);c.moveTo(Vec3.atBottomCenterOf(h.absolutePos(AT)));
    h.getLevel().addFreshEntity(c);cleanup(h,c::discard);
    if(owner!=null)owner.teleportTo(c.getX()+1,c.getY(),c.getZ());
    return c;
  }
  private static ChaoState ready() {
    var s=new ChaoState();s.align=.8f;s.energy=100;s.hunger=80;s.happiness=50;
    s.ageTicks=GardenConfig.CHILD_TICKS.get()-1;return s;
  }
  private static ChaoState adult() {
    var s=new ChaoState();s.adult=true;s.primaryType=ChaoState.RUN_TYPE;s.align=.8f;
    s.ageTicks=1234;s.adultAgeTicks=400;s.setEvolutionStrength(1.2f);
    s.setTrait("morph52.foreign","keep this earned record");return s;
  }
  private static void near(GameTestHelper h,float actual,double expected,String why) {
    h.assertTrue(Math.abs(actual-expected)<.00001,why+": "+actual+" vs "+expected);
  }
  private static void firstEvolution(GameTestHelper h,int stat,int alignment) {
    var s=ready();s.align=.8f*alignment;
    Arrays.fill(s.points,100);s.points[stat]=500;s.points[ChaoState.STAMINA]=9999;
    // Conflicting level bars prove that the selector uses earned points.
    s.levels[(stat+1)%4]=99;
    var c=actor(h,s,null);String lineage=s.lineageId();
    h.runAtTickTime(4,()->{
      var actual=c.state();
      h.assertTrue(actual.adult&&!actual.immortal&&actual.primaryType==stat+1,
          "Highest of four earned abilities must win despite much higher Stamina and another level bar");
      near(h,actual.align,.8*alignment,"First alignment stays locked");
      h.assertTrue(actual.cocoonTicks>0&&actual.adultAgeTicks==0&&actual.evolutionStrength()==0,
          "Real first cocoon must start at young adult maturity");
      h.assertTrue(actual.lineageId().equals(lineage),"First evolution cannot replace identity");
      if(stat==ChaoState.RUN&&alignment==1) {
        var sample=HeroRunMorphology052.sample(actual);
        h.assertTrue(sample.enabled()&&sample.adult(),"Hero Run adult must enter the focused path");
        near(h,sample.adultGrowth(),0,"Freshly evolved adult must start at first-evolution Hero Run shape");
      }
      h.succeed();
    });
  }
  private static void tieEvolution(GameTestHelper h,int earlier,int later) {
    var s=ready();s.train(earlier,1,new Random(1));s.train(later,1,new Random(2));
    int outsider=0;while(outsider==earlier||outsider==later)outsider++;
    s.train(outsider,1,new Random(3));s.train(earlier,-1,new Random(4));s.train(earlier,0,new Random(5));
    Arrays.fill(s.points,1);s.points[earlier]=s.points[later]=500;s.points[ChaoState.STAMINA]=9999;
    var c=actor(h,ChaoState.decode(s.encode()),null);
    h.runAtTickTime(4,()->{
      h.assertTrue(c.state().primaryType==later+1,
          "Latest raised among tied highest abilities must win; latest outsider, loss and no-op cannot steal the tie");
      h.succeed();
    });
  }
  private static void stage(GameTestHelper h,boolean adult,int quarter) {
    var s=adult?adult():new ChaoState();float expected=quarter*.25f;
    if(!adult){s.align=quarter*.125f;s.setEvolutionStrength(.5f);}
    s.train(ChaoState.RUN,(adult?240:120)*quarter,new Random(52));
    int age=s.ageTicks,adultAge=s.adultAgeTicks;float maturity=s.evolutionStrength();String before=s.encode();
    var c=actor(h,s,null);var sample=HeroRunMorphology052.sample(c.state());
    h.assertTrue(sample.enabled()&&sample.adult()==adult,"The ordinary focused sample must be enabled");
    if(adult){near(h,sample.specialization(),expected,"Adult independent 0/25/50/75/100 training target");near(h,sample.adultGrowth(),expected,"Adult full-maturity target");}
    else {near(h,sample.babyHero(),expected,"Baby alignment stage");near(h,sample.babyRun(),expected,"Baby Run stage");}
    h.assertTrue(s.ageTicks==age&&s.adultAgeTicks==adultAge&&s.evolutionStrength()==maturity&&s.encode().equals(before),
        "Reading morphology cannot age a Chao, create a lock or mutate saved fields");
    assertSyncAndNbt(h,c);h.succeed();
  }
  private static void chaosAdmission(GameTestHelper h,int alignment,boolean complete) {
    var s=ready();s.align=alignment;s.reincarnations=2;s.happiness=90;
    s.animalMask=complete?0x7fffffff:0x7ffffffe;s.points[ChaoState.RUN]=500;s.points[ChaoState.STAMINA]=900;
    var c=actor(h,s,null);
    h.runAtTickTime(4,()->{
      h.assertTrue(c.state().adult&&c.state().immortal==complete,"Exact old rare Chaos gate must remain reachable, with every DNA required");
      h.assertTrue(c.state().primaryType==(complete?ChaoState.NORMAL:ChaoState.RUN_TYPE),
          "Legacy rare Chaos exception keeps Normal; ordinary incomplete candidate follows four abilities");
      h.assertTrue(c.state().animalMask==(complete?0x7fffffff:0x7ffffffe),"Admission cannot lose acquired DNA");
      assertSyncAndNbt(h,c);h.succeed();
    });
  }
  private static void zeroAndPartialHistory(GameTestHelper h) {
    var s=new ChaoState();s.points[ChaoState.STAMINA]=9999;
    h.assertTrue(s.prospectivePrimaryType()==ChaoState.NORMAL,"Untouched four abilities retain Normal compatibility fallback");
    s.train(ChaoState.RUN,1,new Random(1));
    h.assertTrue(s.points[ChaoState.RUN]==0&&s.prospectivePrimaryType()==ChaoState.RUN_TYPE,
        "A real partial bar counts as latest positive training even before earned points increase");
    s.train(ChaoState.POWER,-1,new Random(2));s.train(ChaoState.FLY,0,new Random(3));
    h.assertTrue(s.lastTrainedStat()==ChaoState.RUN&&s.prospectivePrimaryType()==ChaoState.RUN_TYPE,"Loss and no-op are not latest raises");
    var c=actor(h,s,null);assertSyncAndNbt(h,c);h.succeed();
  }
  private static void heldDrive(GameTestHelper h,String id,boolean adult) {
    var p=keeper(h);var s=adult?adult():new ChaoState();s.align=.8f;s.energy=100;s.hunger=80;
    var live=new ChaoEntity[]{actor(h,s,p)};UUID uuid=live[0].getUUID();
    var offered=new ItemStack(ChaoGarden.item(id));p.setItemInHand(InteractionHand.MAIN_HAND,offered);
    live[0].mobInteract(p,InteractionHand.MAIN_HAND);
    h.assertTrue(offered.isEmpty()&&s.driveCount==0,"Actual held drive transfers once before its benefit");
    h.runAtTickTime(12,()->{
      var saved=new CompoundTag();live[0].saveWithoutId(saved);live[0].discard();
      var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(saved);h.getLevel().addFreshEntity(restored);live[0]=restored;cleanup(h,restored::discard);
    });
    h.runAtTickTime(150,()->{
      var c=live[0];var actual=c.state();int expected=id.equals("run_drive")?24:100;
      h.assertTrue(c.getUUID().equals(uuid)&&actual.driveCount==1&&c.getMainHandItem().isEmpty(),
          "Pending-item entity reload must retain one UUID and exactly one completed drive");
      h.assertTrue(actual.levels[ChaoState.RUN]*100+actual.progress[ChaoState.RUN]==expected,"Actual drive must grant its documented training amount once");
      var sample=HeroRunMorphology052.sample(actual);
      near(h,adult?sample.specialization():sample.babyRun(),adult?(id.equals("run_drive")?.025:5.0/48):(id.equals("run_drive")?.05:5.0/24),
          "Ordinary and combined Run drives must feed the same morphology target");
      h.assertTrue(actual.lockedAlignment()==.8f&&(!adult||actual.primaryType==ChaoState.RUN_TYPE),"Item care cannot unlock first identity");
      assertSyncAndNbt(h,c);h.succeed();
    });
  }
  private static void combinedTier(GameTestHelper h,String tier) {
    var s=adult();var c=actor(h,s,null);double expected;
    if(tier.equals("super_solid")) {
      h.assertTrue(DriveRules.apply(s,DriveRules.Tier.SUPER_SOLID,ChaoState.RUN,new Random(52)),"Super Solid applies");
      h.assertTrue(s.levels[ChaoState.RUN]==10&&s.progress[ChaoState.RUN]==0,"Super Solid raises ten levels");expected=1;
    }else if(tier.equals("super_chaotic")) {
      Arrays.fill(s.levels,10);Arrays.fill(s.points,200);
      // Max shuffle choices preserve the documented assignment order, and
      // therefore independently force Run +5 levels / Power -2 levels.
      var order=new Random(52){@Override public int nextInt(int bound){return bound-1;}};
      h.assertTrue(DriveRules.apply(s,DriveRules.Tier.SUPER_CHAOTIC,0,order),"Super Chaotic applies");
      h.assertTrue(s.levels[ChaoState.RUN]==15&&s.levels[ChaoState.POWER]==8
          &&s.progress[ChaoState.SWIM]==24&&s.levels[ChaoState.FLY]==11,
          "One documented +24/+100/+500/-200 assignment must affect four distinct abilities");expected=35.0/48;
    }else if(tier.startsWith("unstable_solid")) {
      s.train(ChaoState.RUN,960,new Random(52));boolean high=tier.endsWith("high");
      var roll=new Random(52){@Override public int nextInt(int bound){return bound==100?(high?0:99):0;}};
      h.assertTrue(DriveRules.apply(s,DriveRules.Tier.UNSTABLE_SOLID,ChaoState.RUN,roll),"Unstable Solid applies");
      h.assertTrue(s.levels[ChaoState.RUN]==(high?99:0)&&s.progress[ChaoState.RUN]==0,"Forced Unstable Solid documented endpoint");expected=high?1:0;
    }else if(tier.equals("unstable_chaotic_low")) {
      s.train(ChaoState.RUN,960,new Random(52));
      var roll=new Random(52){@Override public int nextInt(int bound){return switch(bound){case 4->ChaoState.RUN;case 1000->0;case 21->10;case 201->100;case 100->99;default->0;};}};
      h.assertTrue(DriveRules.apply(s,DriveRules.Tier.UNSTABLE_CHAOTIC,0,roll),"Unstable Chaotic applies");
      h.assertTrue(s.levels[ChaoState.RUN]==0&&s.progress[ChaoState.RUN]==0,"Forced Unstable Chaotic loss removes Run training");expected=0;
    }else {
      s.train(ChaoState.RUN,960,new Random(52));s.levels[ChaoState.SWIM]=s.levels[ChaoState.FLY]=99;
      s.levels[ChaoState.STAMINA]=0;s.levels[ChaoState.POWER]=99;
      // Only Stamina is eligible after making Run maxed as well. This is an
      // independent five-stat item rule, not an ordinary first-type selector.
      s.levels[ChaoState.RUN]=99;s.progress[ChaoState.RUN]=0;float before=HeroRunMorphology052.sample(s).specialization();
      h.assertTrue(DriveRules.apply(s,DriveRules.Tier.PERFECT,0,new Random(52)),"Perfect selecting Stamina applies");
      h.assertTrue(s.levels[ChaoState.STAMINA]==99,"Perfect must retain its documented Stamina eligibility");
      near(h,HeroRunMorphology052.sample(s).specialization(),before,"Stamina-only training cannot alter Run/Power appearance");expected=0;
    }
    near(h,HeroRunMorphology052.sample(s).specialization(),expected,"Combined tier has the same current-stat morphology inputs");
    h.assertTrue(s.ageTicks==1234&&s.adultAgeTicks==400&&s.evolutionStrength()==1.2f
        &&s.lockedPrimaryType()==ChaoState.RUN_TYPE&&s.lockedAlignment()==.8f,
        "No combined tier may rewrite biological age or the fixed first identity");
    assertSyncAndNbt(h,c);h.succeed();
  }
  private static void balancedTransactions(GameTestHelper h) {
    var s=new ChaoState();s.align=.8f;s.train(ChaoState.FLY,1,new Random(1));s.train(ChaoState.RUN,1,new Random(2));
    s.levels[ChaoState.FLY]=s.levels[ChaoState.RUN]=0;s.progress[ChaoState.FLY]=s.progress[ChaoState.RUN]=0;
    Arrays.fill(s.points,100);s.points[ChaoState.STAMINA]=9999;
    h.assertTrue(DriveRules.apply(s,DriveRules.Tier.CHAOTIC,0,new Random(52)),"Actual balanced combined transaction applies");
    h.assertTrue(s.lastTrainedStat()==ChaoState.RUN&&s.prospectivePrimaryType()==ChaoState.RUN_TYPE,
        "One simultaneous all-stat raise must retain prior Run-before-Fly tie history instead of loop-order Power bias");
    for(int stat=0;stat<4;stat++)h.assertTrue(s.levels[stat]==0&&s.progress[stat]==24,"Balanced transaction raises each ability once");
    near(h,HeroRunMorphology052.sample(s).babyRun(),0,"Equal Run and Power must cancel in this prototype");
    var virgin=new ChaoState();virgin.align=.8f;DriveRules.apply(virgin,DriveRules.Tier.CHAOTIC,0,new Random(52));
    h.assertTrue(virgin.lastTrainedStat()==ChaoState.SWIM&&virgin.prospectivePrimaryType()==ChaoState.SWIM_TYPE,
        "Previously unseen simultaneous tied raises use the documented canonical ability order");
    var c=actor(h,s,null);assertSyncAndNbt(h,c);h.succeed();
  }
  private static void reversibleSpecialization(GameTestHelper h) {
    var s=adult();s.train(ChaoState.RUN,960,new Random(1));
    int age=s.ageTicks,adultAge=s.adultAgeTicks;float maturity=s.evolutionStrength();
    near(h,HeroRunMorphology052.sample(s).specialization(),1,"Forty base-drive equivalents set the full training target");
    DriveRules.setTotal(s,ChaoState.RUN,480,new Random(2));
    near(h,HeroRunMorphology052.sample(s).specialization(),.5,"Combined-drive loss halves current Run specialization");
    s.train(ChaoState.POWER,240,new Random(3));
    near(h,HeroRunMorphology052.sample(s).specialization(),.25,"Power reversal offsets Run specialization");
    DriveRules.setTotal(s,ChaoState.RUN,0,new Random(4));
    near(h,HeroRunMorphology052.sample(s).adultGrowth(),0,"Removing Run training returns to first Hero Run form");
    h.assertTrue(s.ageTicks==age&&s.adultAgeTicks==adultAge&&s.evolutionStrength()==maturity,
        "Stat loss and reversal cannot make the biological adult younger");
    h.assertTrue(s.lockedPrimaryType()==ChaoState.RUN_TYPE&&s.lockedAlignment()==.8f,"The first Hero Run identity remains");
    var c=actor(h,s,null);assertSyncAndNbt(h,c);h.succeed();
  }
  private static void analyzer(GameTestHelper h) {
    var p=keeper(h);var s=adult();s.setEvolutionStrength(.6f);var c=actor(h,s,p);
    var request=new ChaoCareNetwork.StatEdit(c.getUUID().toString(),ChaoState.RUN,AnalyzerStatRules.LEVEL,10);
    String before=s.encode();h.assertFalse(ChaoCareNetwork.editStats(p,request),"Survival cannot forge Creative training edits");
    h.assertTrue(before.equals(s.encode()),"Denied edit must not change any Chao state");p.setGameMode(GameType.CREATIVE);
    int age=s.ageTicks,adultAge=s.adultAgeTicks;float maturity=s.evolutionStrength();
    h.assertTrue(ChaoCareNetwork.editStats(p,request)&&s.levels[ChaoState.RUN]==10,"Actual Creative Analyzer raises Run ten levels");
    near(h,HeroRunMorphology052.sample(s).specialization(),1,"Analyzer increase updates the target without a drive-only hook");
    near(h,HeroRunMorphology052.sample(s).adultGrowth(),.5,"Independent half biological maturity gates visual adult progression");
    var down=new ChaoCareNetwork.StatEdit(c.getUUID().toString(),ChaoState.RUN,AnalyzerStatRules.LEVEL,-10);
    h.assertTrue(ChaoCareNetwork.editStats(p,down)&&s.levels[ChaoState.RUN]==0,"Actual Creative Analyzer can reverse the gain");
    near(h,HeroRunMorphology052.sample(s).adultGrowth(),0,"Analyzer decrease visibly removes secondary specialization");
    h.assertTrue(s.ageTicks==age&&s.adultAgeTicks==adultAge&&s.evolutionStrength()==maturity&&s.align==.8f&&s.primaryType==ChaoState.RUN_TYPE,
        "Creative stats cannot alter age or first alignment/type");
    assertSyncAndNbt(h,c);h.succeed();
  }
  private static void lockedIdentity(GameTestHelper h) {
    var s=adult();var c=actor(h,s,null);s.pet(-1);s.feed(ChaoState.RUN,0,-1);
    s.align=-1;s.primaryType=ChaoState.POWER_TYPE;c.sync();
    h.assertTrue(s.align==.8f&&s.primaryType==ChaoState.RUN_TYPE,"Sync repairs stale public identity edits to its saved first Hero Run identity");
    assertSyncAndNbt(h,c);
    for(int alignment=-1;alignment<=1;alignment++) {
      var chaos=adult();chaos.immortal=true;chaos.primaryType=ChaoState.NORMAL;chaos.align=alignment;
      var old=ChaoState.decode(chaos.encode());var saved=ChaoState.decode(old.encode());
      h.assertTrue(saved.immortal&&saved.primaryType==ChaoState.NORMAL&&saved.align==alignment,
          "Already existing Chaos identities cannot be reselected by new ordinary rules");
      h.assertFalse(HeroRunMorphology052.sample(saved).enabled(),"Chaos cannot enter ordinary Hero Run morphology");
    }
    h.succeed();
  }
  private static void carrier(GameTestHelper h) {
    var p=keeper(h);var s=adult();s.train(ChaoState.RUN,480,new Random(3));s.setEvolutionStrength(.6f);
    s.animalParts[ChaoState.ARMS]=6;s.animalParts[ChaoState.FEET]=6;s.animalPartsMask=1<<6;s.animalMask=1<<6;
    var c=actor(h,s,p);c.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.CARVED_PUMPKIN));
    UUID uuid=c.getUUID();String encoded=s.encode();var stack=new ItemStack(ChaoGarden.item("chao_carrier"));
    h.assertTrue(ChaoCarrierItem.capture(stack,c,p)&&c.isRemoved(),"Real carrier capture owns this exact evolving Chao");
    var roundtrip=ItemStack.parse(h.getLevel().registryAccess(),stack.save(h.getLevel().registryAccess())).orElseThrow();
    h.assertTrue(ChaoCarrierItem.storedUUID(roundtrip).equals(uuid)&&ChaoCarrierItem.state(roundtrip).encode().equals(encoded),
        "Actual inventory NBT keeps UUID, genetics, age, parts and full state exactly");
    h.runAtTickTime(25,()->{
      h.assertTrue(ChaoCarrierItem.state(roundtrip).encode().equals(encoded),"Stored carrier must not age while unloaded");
      p.setItemInHand(InteractionHand.MAIN_HAND,roundtrip);var floor=h.absolutePos(AT.below());
      var hit=new BlockHitResult(Vec3.atCenterOf(floor),Direction.UP,floor,false);
      h.assertTrue(roundtrip.getItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,hit)).consumesAction()&&roundtrip.isEmpty(),
          "Actual release consumes the carrier exactly once");
      var restored=(ChaoEntity)h.getLevel().getEntity(uuid);h.assertTrue(restored!=null,"Actual release keeps the existing UUID");cleanup(h,restored::discard);
      h.assertTrue(restored.state().encode().equals(encoded)&&restored.getItemBySlot(EquipmentSlot.HEAD).is(Items.CARVED_PUMPKIN),
          "Release cannot lose morphology state, animal hands/feet or a real vanilla hat");
      near(h,HeroRunMorphology052.sample(restored.state()).adultGrowth(),.25,"Released biological half-mature, half-trained form remains quarter specialized");
      h.succeed();
    });
  }
  private static void migrationAndReincarnation(GameTestHelper h) {
    var s=adult();s.train(ChaoState.RUN,960,new Random(7));s.setTrait("earned.prize",7);
    String[] old=s.encode().split(";",25);old[0]="5";old[21]=String.join(",",Arrays.copyOf(old[21].split(","),21));
    var legacy=ChaoState.decode(String.join(";",old));String lineage=legacy.lineageId();int[] genes=legacy.gradeA.clone();
    h.assertTrue(legacy.adult&&legacy.primaryType==ChaoState.RUN_TYPE&&legacy.align==.8f&&legacy.intTrait("earned.prize")==7,
        "Schema5 migration must preserve the actual adult first identity and unrelated earned data");
    var c=actor(h,legacy,null);assertSyncAndNbt(h,c);
    legacy.reincarnate();
    h.assertTrue(!legacy.adult&&!legacy.immortal&&legacy.ageTicks==0&&legacy.adultAgeTicks==0&&legacy.evolutionStrength()==0
        &&legacy.lastTrainedStat()==-1&&legacy.primaryType==ChaoState.NORMAL&&legacy.reincarnations==1,
        "Reincarnation resets biological maturity, adult identity and latest-training history for the next life");
    h.assertTrue(legacy.lineageId().equals(lineage)&&Arrays.equals(legacy.gradeA,genes)&&legacy.intTrait("earned.prize")==7,
        "A new life cannot discard inherited genes, lineage or prior earned records");
    h.assertFalse(HeroRunMorphology052.sample(legacy).adult(),"New-life morphology cannot remain an adult specialization");
    c.sync();assertSyncAndNbt(h,c);h.succeed();
  }
  private static void assertSyncAndNbt(GameTestHelper h,ChaoEntity c) {
    c.sync();String encoded=c.state().encode();var updates=c.getEntityData().getNonDefaultValues();
    h.assertTrue(updates!=null&&updates.stream().anyMatch(v->v.value() instanceof String value&&value.equals(encoded)),
        "Actual entity-data synchronization must contain the authoritative complete saved state");
    var observer=ChaoGarden.CHAO.get().create(h.getLevel());observer.getEntityData().assignValues(updates);
    h.assertTrue(observer.getEntityData().getNonDefaultValues().stream().anyMatch(v->v.value() instanceof String value&&value.equals(encoded)),
        "An observer entity receives exactly the same morphology inputs");
    var tag=new CompoundTag();c.saveWithoutId(tag);var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(tag);
    h.assertTrue(restored.state().encode().equals(encoded)&&restored.getUUID().equals(c.getUUID())&&restored.owner.equals(c.owner),
        "Actual entity NBT roundtrip must preserve full state, UUID and owner");
    near(h,HeroRunMorphology052.sample(restored.state()).adultGrowth(),HeroRunMorphology052.sample(c.state()).adultGrowth(),
        "NBT copy must produce identical rendered specialization inputs");
    observer.discard();restored.discard();
  }
  private static List<Legacy> legacyStates() {
    try(var stream=HeroRunMorphologyGameTests052.class.getResourceAsStream("/data/chao_morph52/legacy_states.json")) {
      if(stream==null)throw new IllegalStateException("Missing real older-save fixtures");
      var json=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8));var out=new ArrayList<Legacy>();
      for(var entry:json.getAsJsonObject().getAsJsonArray("states")) {var value=entry.getAsJsonObject();out.add(new Legacy(value.get("source").getAsString(),value.get("sha256").getAsString(),value.get("encoded").getAsString()));}
      return out;
    }catch(Exception failure){throw new IllegalStateException("Cannot read actual older save snapshots",failure);}
  }
  private record Legacy(String source,String sha256,String encoded) {}
  private static void actualLegacy(GameTestHelper h,int index) {
    var old=legacyStates().get(index);
    try {h.assertTrue(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(old.encoded().getBytes(StandardCharsets.UTF_8))).equals(old.sha256()),
        "Original saved-state fixture bytes must match their extracted source hash");}
    catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    var state=ChaoState.decode(old.encoded());
    h.assertTrue(state.encode().equals(old.encoded()),"This actual schema6 older-save record must roundtrip byte-for-byte: "+old.source());
    var c=actor(h,state,null);assertSyncAndNbt(h,c);h.succeed();
  }
}
