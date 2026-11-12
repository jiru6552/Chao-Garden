package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.AnalyzerStatRules;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.DriveRules;
import com.chaogarden.core.HeroRunMorphology052;
import com.chaogarden.core.NeutralMorphology056;
import com.chaogarden.entity.ChaoEntity;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Authoritative saved Neutral/Normal identity and actual training routes.
 * This suite proves server policy/transport, not visual quality or elapsed life timing. */
@GameTestHolder("chao_neutral56") @PrefixGameTestTemplate(false)
public final class NeutralMorphologyGameTests056 {
  private static final String TEMPLATE="chao_morph54:activity_test";
  private static final BlockPos AT=new BlockPos(3,2,3);
  private NeutralMorphologyGameTests056() {}

  @GameTestGenerator public static Collection<TestFunction> cases(){
    var tests=new ArrayList<TestFunction>();
    for(int rp=-1;rp<=1;rp++)for(int sf=-1;sf<=1;sf++)for(int age=0;age<=2;age++){
      int r=rp,f=sf,a=age;add(tests,"neutral_grid_"+(rp+1)+"_"+(sf+1)+"_age_"+(age*50),h->grid(h,r,f,a));
    }
    add(tests,"natural_untouched_first_evolution_then_real_drives",NeutralMorphologyGameTests056::firstEvolution);
    for(int stat=0;stat<4;stat++){int selected=stat;add(tests,"trained_first_type_remains_outside_"+stat,h->otherFirstType(h,selected));}
    for(int stat=0;stat<4;stat++){int selected=stat;add(tests,"ordinary_combined_and_analyzer_route_"+stat,h->mutationRoutes(h,selected));}
    add(tests,"combined_balanced_gain_and_real_reversal",NeutralMorphologyGameTests056::opposition);
    add(tests,"compatible_stat_order_is_not_a_new_form",NeutralMorphologyGameTests056::commutative);
    add(tests,"neutral_adult_identity_remains_locked",NeutralMorphologyGameTests056::locked);
    add(tests,"existing_hero_run_path_is_preserved",NeutralMorphologyGameTests056::hero);
    for(int index:new int[]{0,1}){int fixture=index;add(tests,"literal_existing_world_neutral_"+index,h->legacy(h,fixture));}
    return tests;
  }
  private static void add(List<TestFunction> tests,String name,java.util.function.Consumer<GameTestHelper> body){
    tests.add(new TestFunction("neutral056_"+(tests.size()/8),name,TEMPLATE,40,0,true,body));
  }
  private static void cleanup(GameTestHelper h,Runnable action){
    h.testInfo.addListener(new GameTestListener(){
      public void testStructureLoaded(GameTestInfo i){}
      public void testPassed(GameTestInfo i,GameTestRunner r){action.run();}
      public void testFailed(GameTestInfo i,GameTestRunner r){action.run();}
      public void testAddedForRerun(GameTestInfo a,GameTestInfo b,GameTestRunner r){action.run();}
    });
  }
  private static ChaoState adult(){
    var s=new ChaoState();s.adult=true;s.primaryType=ChaoState.NORMAL;s.align=.125f;s.ageTicks=2345;s.adultAgeTicks=432;
    s.hunger=80;s.energy=100;s.happiness=70;s.setEvolutionStrength(1.2f);s.setTrait("neutral56.foreign","preserve unrelated earned data");
    return ChaoState.decode(s.encode());
  }
  private static ChaoEntity actor(GameTestHelper h,ChaoState s){
    for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)h.setBlock(AT.offset(x,-1,z),Blocks.STONE);
    var c=ChaoGarden.CHAO.get().create(h.getLevel());h.assertTrue(c!=null,"Production Chao entity creates");
    c.owner=UUID.fromString("05600000-0000-0000-0000-000000000056");c.setState(s);c.setNoAi(true);c.setNoGravity(true);
    c.moveTo(Vec3.atBottomCenterOf(h.absolutePos(AT)));h.getLevel().addFreshEntity(c);cleanup(h,c::discard);return c;
  }
  private static void axis(ChaoState s,int rp,int sf){
    for(int stat=0;stat<4;stat++)DriveRules.setTotal(s,stat,0,new Random(56));
    if(rp!=0)DriveRules.setTotal(s,rp>0?ChaoState.POWER:ChaoState.RUN,Math.abs(rp),new Random(56));
    if(sf!=0)DriveRules.setTotal(s,sf>0?ChaoState.FLY:ChaoState.SWIM,Math.abs(sf),new Random(56));
  }
  private static void near(GameTestHelper h,float actual,float expected,String why){h.assertTrue(Float.isFinite(actual)&&Math.abs(actual-expected)<.00001f,why+": "+actual+" vs "+expected);}
  private static void syncAndNbt(GameTestHelper h,ChaoEntity c){
    c.sync();String encoded=c.state().encode();var input=NeutralMorphology056.sample(c.state());
    var updates=c.getEntityData().getNonDefaultValues();
    h.assertTrue(updates!=null&&updates.stream().anyMatch(v->v.value() instanceof String s&&s.equals(encoded)),"Entity synchronization carries complete authoritative age/training/identity data");
    var observer=ChaoGarden.CHAO.get().create(h.getLevel());observer.getEntityData().assignValues(updates);
    h.assertTrue(observer.getEntityData().getNonDefaultValues().stream().anyMatch(v->v.value() instanceof String s&&s.equals(encoded)),"Observer receives exactly the same authoritative serialized record");
    var tag=new CompoundTag();c.saveWithoutId(tag);var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(tag);
    h.assertTrue(restored.state().encode().equals(encoded)&&restored.owner.equals(c.owner)&&restored.getUUID().equals(c.getUUID()),"Real entity NBT retains saved data, UUID and owner");
    h.assertTrue(NeutralMorphology056.sample(restored.state()).equals(input),"Reload computes the same independent Neutral morphology inputs");
    observer.discard();restored.discard();
  }
  private static void grid(GameTestHelper h,int rp,int sf,int age){
    var s=adult();axis(s,rp*960,sf*960);s.setEvolutionStrength(age*.6f);var c=actor(h,s);String encoded=s.encode();
    var p=NeutralMorphology056.sample(s);h.assertTrue(p.enabled(),"Each of the nine requested Neutral combinations uses the new path");
    near(h,p.runPower(),rp,"Run/Power influence reads current opposing bar totals");near(h,p.swimFly(),sf,"Swim/Fly remains a separate compatible axis");near(h,p.maturity(),age*.5f,"Young/intermediate/mature states share independent biological age");
    var weights=NeutralMorphology056.weights(p);float sum=0;for(float w:weights){h.assertTrue(Float.isFinite(w)&&w>=0&&w<=1,"Shape weights stay finite and convex");sum+=w;}
    near(h,sum,1,"The complete corner mix has unit mass");near(h,weights[0],1-age*.5f,"A trained young adult cannot jump directly to the mature form");
    near(h,weights[1+(sf+1)*3+(rp+1)],age*.5f,"Only the correct mature corner develops at a grid endpoint");
    h.assertTrue(encoded.equals(s.encode()),"Sampling the renderer inputs has no saved-state side effects");syncAndNbt(h,c);h.succeed();
  }
  private static void firstEvolution(GameTestHelper h){
    var s=new ChaoState();s.align=.125f;s.ageTicks=19;s.points[ChaoState.STAMINA]=9999;
    h.assertTrue(s.tick(20,100000,new Random(56))==ChaoState.Transition.EVOLVED,"The real untouched first cocoon completes");
    h.assertTrue(s.adult&&!s.immortal&&s.lockedPrimaryType()==ChaoState.NORMAL&&s.lockedAlignment()==.125f,"Neutral/Normal is naturally available without changing four-stat selection rules");
    for(int i=0;i<40;i++)s.giveDrive(ChaoState.RUN);
    var p=NeutralMorphology056.sample(s);h.assertTrue(p.enabled(),"Post-evolution training keeps the locked Normal first type");near(h,p.runPower(),-1,"Forty actual drives span the declared later Run influence");near(h,p.maturity(),0,"First evolution starts young even with later training");
    var c=actor(h,s);syncAndNbt(h,c);h.succeed();
  }
  private static void otherFirstType(GameTestHelper h,int stat){
    var s=new ChaoState();s.ageTicks=19;s.points[stat]=100;s.levels[(stat+1)%4]=99;s.points[ChaoState.STAMINA]=9999;
    h.assertTrue(s.tick(20,100000,new Random(56))==ChaoState.Transition.EVOLVED&&s.lockedPrimaryType()==stat+1,"First type still uses highest earned points, excluding Stamina and unrelated higher levels");
    h.assertFalse(NeutralMorphology056.sample(s).enabled(),"A trained non-Normal first form retains its existing renderer");
    var tied=new ChaoState();int earlier=(stat+1)%4;tied.train(earlier,1);tied.train(stat,1);tied.points[earlier]=tied.points[stat]=100;tied.ageTicks=19;
    tied.tick(20,100000,new Random(56));h.assertTrue(tied.lockedPrimaryType()==stat+1,"An actual first-evolution tie uses the latest raised tied skill");
    syncAndNbt(h,actor(h,s));h.succeed();
  }
  private static void mutationRoutes(GameTestHelper h,int stat){
    var direct=adult();var ordinary=adult();var combined=adult();var analyzer=adult();
    direct.train(stat,240,new Random(56));for(int i=0;i<10;i++)ordinary.giveDrive(stat);
    h.assertTrue(DriveRules.apply(combined,DriveRules.Tier.SOLID,stat,new Random(56)),"Actual typed combined drive applies");DriveRules.change(combined,stat,140,new Random(56));
    AnalyzerStatRules.apply(analyzer,stat,AnalyzerStatRules.LEVEL,1);AnalyzerStatRules.apply(analyzer,stat,AnalyzerStatRules.LEVEL,1);AnalyzerStatRules.apply(analyzer,stat,AnalyzerStatRules.PROGRESS,40);
    var expected=NeutralMorphology056.sample(direct);
    h.assertTrue(expected.equals(NeutralMorphology056.sample(ordinary))&&expected.equals(NeutralMorphology056.sample(combined))&&expected.equals(NeutralMorphology056.sample(analyzer)),"Ordinary, typed combined, direct and Creative training all derive identical morphology from equal current totals");
    near(h,stat<2?expected.swimFly():expected.runPower(),stat==ChaoState.FLY||stat==ChaoState.POWER?.25f:-.25f,"Correct independent axis expresses the actual training");
    int age=analyzer.ageTicks,adultAge=analyzer.adultAgeTicks;float maturity=analyzer.evolutionStrength();
    AnalyzerStatRules.apply(analyzer,stat,AnalyzerStatRules.PROGRESS,0);AnalyzerStatRules.apply(analyzer,stat,AnalyzerStatRules.LEVEL,-1);
    var reduced=NeutralMorphology056.sample(analyzer);near(h,Math.abs(stat<2?reduced.swimFly():reduced.runPower()),100f/960,"Creative level/progress decreases recede this same current-state input");
    h.assertTrue(analyzer.ageTicks==age&&analyzer.adultAgeTicks==adultAge&&analyzer.evolutionStrength()==maturity,"Stat editing does not rewrite chronological age/maturity");
    syncAndNbt(h,actor(h,analyzer));h.succeed();
  }
  private static void opposition(GameTestHelper h){
    var s=adult();axis(s,-960,960);s.setEvolutionStrength(.6f);int age=s.ageTicks,adultAge=s.adultAgeTicks;float maturity=s.evolutionStrength();
    h.assertTrue(DriveRules.apply(s,DriveRules.Tier.CHAOTIC,-1,new Random(56)),"Actual balanced four-stat drive applies");
    near(h,NeutralMorphology056.sample(s).runPower(),-1,"Balanced combined drive preserves Run advantage");near(h,NeutralMorphology056.sample(s).swimFly(),1,"Balanced combined drive preserves Fly advantage");
    for(int i=0;i<40;i++)s.giveDrive(ChaoState.POWER);
    near(h,NeutralMorphology056.sample(s).runPower(),0,"Opposing training cancels Run");near(h,NeutralMorphology056.sample(s).swimFly(),1,"Compatible Fly is unaffected");
    for(int i=0;i<40;i++)s.giveDrive(ChaoState.SWIM);
    near(h,NeutralMorphology056.sample(s).swimFly(),0,"Opposing Swim cancels Fly");
    DriveRules.change(s,ChaoState.RUN,-480,new Random(56));DriveRules.change(s,ChaoState.FLY,-240,new Random(56));
    near(h,NeutralMorphology056.sample(s).runPower(),.5f,"Direct reductions can expose the opposing stored Power");near(h,NeutralMorphology056.sample(s).swimFly(),-.25f,"Swim influence develops independently after a Fly decrease");
    h.assertTrue(s.ageTicks==age&&s.adultAgeTicks==adultAge&&s.evolutionStrength()==maturity&&s.lockedPrimaryType()==ChaoState.NORMAL,"Reversal preserves identity and both biological inputs");syncAndNbt(h,actor(h,s));h.succeed();
  }
  private static void commutative(GameTestHelper h){
    for(int a:new int[]{ChaoState.RUN,ChaoState.POWER})for(int b:new int[]{ChaoState.FLY,ChaoState.SWIM}){
      var first=adult();var reverse=adult();for(int i=0;i<40;i++)first.giveDrive(a);for(int i=0;i<40;i++)first.giveDrive(b);
      for(int i=0;i<40;i++)reverse.giveDrive(b);for(int i=0;i<40;i++)reverse.giveDrive(a);
      h.assertTrue(NeutralMorphology056.sample(first).equals(NeutralMorphology056.sample(reverse)),"Compatible pair order does not create a separate form");
      h.assertTrue(first.lockedPrimaryType()==ChaoState.NORMAL&&reverse.lockedPrimaryType()==ChaoState.NORMAL,"Later recency cannot rewrite either saved first identity");
    }h.succeed();
  }
  private static void locked(GameTestHelper h){
    var s=adult();axis(s,-960,960);var before=NeutralMorphology056.sample(s);s.align=1;s.primaryType=ChaoState.RUN_TYPE;
    h.assertTrue(NeutralMorphology056.sample(s).equals(before),"Renderer reads the saved adult identity despite stale public field edits");
    s=ChaoState.decode(s.encode());h.assertTrue(s.align==.125f&&s.primaryType==ChaoState.NORMAL,"Reload/normalization preserves original Neutral alignment and Normal first type");syncAndNbt(h,actor(h,s));h.succeed();
  }
  private static void hero(GameTestHelper h){
    var s=new ChaoState();s.adult=true;s.primaryType=ChaoState.RUN_TYPE;s.align=1;s.ageTicks=1000;s.adultAgeTicks=300;s.setEvolutionStrength(1.2f);s=ChaoState.decode(s.encode());axis(s,-960,960);
    var p=HeroRunMorphology052.sample(s);h.assertTrue(p.enabled()&&!NeutralMorphology056.sample(s).enabled(),"Approved Hero Run is still selected independently from Neutral adults");near(h,p.adultGrowth(),1,"Hero mature Run specialization survives compatible Fly training");
    var c=actor(h,s);syncAndNbt(h,c);var tag=new CompoundTag();c.saveWithoutId(tag);var copy=ChaoGarden.CHAO.get().create(h.getLevel());copy.load(tag);
    h.assertTrue(HeroRunMorphology052.sample(copy.state()).equals(p),"Real entity reload keeps the same approved Hero Run inputs");copy.discard();h.succeed();
  }
  private static void legacy(GameTestHelper h,int index){
    try(var input=NeutralMorphologyGameTests056.class.getResourceAsStream("/data/chao_morph52/legacy_states.json")){
      h.assertTrue(input!=null,"Literal historical world records remain available");var d=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();var fixture=d.getAsJsonArray("states").get(index).getAsJsonObject();
      String encoded=fixture.get("encoded").getAsString();String expected=index==0?"bba10bc1858289535b28f115b2ba03c32c9ecf9e52822e05786c421111d8fdc0":"495d2d4175ff38709f5041a3e88d5cd4dde4823c07847a72bb36ef475887692c";
      h.assertTrue(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(encoded.getBytes(StandardCharsets.UTF_8))).equals(expected),"The test uses unchanged literal pre-prototype world bytes");
      var s=ChaoState.decode(encoded);h.assertTrue(s.adult&&s.lockedPrimaryType()==ChaoState.NORMAL&&NeutralMorphology056.sample(s).enabled(),"An existing saved Neutral/Normal adult joins the new path without re-evolution");
      String normalized=s.encode();var sample=NeutralMorphology056.sample(s);h.assertTrue(normalized.equals(s.encode())&&sample.equals(NeutralMorphology056.sample(s)),"Repeated morphology sampling changes no saved record or identity");
      syncAndNbt(h,actor(h,s));h.succeed();
    }catch(Exception e){throw new IllegalStateException("Cannot load unchanged historical Neutral fixture",e);}
  }
}
