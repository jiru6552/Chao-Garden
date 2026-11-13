package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.AnalyzerStatRules;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.DriveRules;
import com.chaogarden.core.HeroRunMorphology052;
import com.chaogarden.core.NeutralFlyMorphology061;
import com.chaogarden.core.NeutralMorphology056;
import com.chaogarden.care.ChaoCareNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
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

/** Authoritative saved Neutral/Fly identity and actual training routes.
 * This suite proves server policy/transport, not visual quality or elapsed life timing. */
@GameTestHolder("chao_fly61") @PrefixGameTestTemplate(false)
public final class NeutralFlyMorphologyGameTests061 {
  private static final String TEMPLATE="chao_morph54:activity_test";
  private static final BlockPos AT=new BlockPos(3,2,3);
  private NeutralFlyMorphologyGameTests061() {}

  @GameTestGenerator public static Collection<TestFunction> cases(){
    var tests=new ArrayList<TestFunction>();
    for(int rp=-1;rp<=1;rp++)for(int sf=0;sf<=1;sf++)for(int age=0;age<=2;age++){
      int r=rp,f=sf,a=age;add(tests,"neutral_fly_grid_"+(rp+1)+"_"+(sf+1)+"_age_"+(age*50),h->grid(h,r,f,a));
    }
    add(tests,"natural_fly_first_evolution_then_real_drives",NeutralFlyMorphologyGameTests061::firstEvolution);
    for(int stat=0;stat<4;stat++){int selected=stat;add(tests,"earned_first_type_and_latest_ties_"+stat,h->otherFirstType(h,selected));}
    for(int stat=0;stat<4;stat++){int selected=stat;add(tests,"ordinary_combined_and_analyzer_route_"+stat,h->mutationRoutes(h,selected));}
    add(tests,"combined_balanced_gain_and_real_reversal",NeutralFlyMorphologyGameTests061::opposition);
    add(tests,"compatible_stat_order_is_not_a_new_form",NeutralFlyMorphologyGameTests061::commutative);
    add(tests,"neutral_fly_adult_identity_remains_locked",NeutralFlyMorphologyGameTests061::locked);
    add(tests,"existing_hero_run_path_is_preserved",NeutralFlyMorphologyGameTests061::hero);
    add(tests,"existing_neutral_normal_path_remains_separate",NeutralFlyMorphologyGameTests061::neutralRegression);
    add(tests,"actual_native_commands_keep_neutral_fly_identity",NeutralFlyMorphologyGameTests061::commands);
    add(tests,"actual_maturity_payload_keeps_adult_identity",NeutralFlyMorphologyGameTests061::maturityPayload);
    add(tests,"actual_analyzer_permissions_and_locked_alignment",NeutralFlyMorphologyGameTests061::analyzerPayload);
    add(tests,"named_jewel_and_shiny_inputs_do_not_change_form",NeutralFlyMorphologyGameTests061::colors);
    add(tests,"literal_existing_neutral_world_stays_neutral",h->legacy(h,false));
    add(tests,"synthetic_expanded_care21_neutral_fly_migration",h->legacy(h,true));
    return tests;
  }
  private static void add(List<TestFunction> tests,String name,java.util.function.Consumer<GameTestHelper> body){
    tests.add(new TestFunction("fly061_"+(tests.size()/8),name,TEMPLATE,40,0,true,body));
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
    var s=new ChaoState();s.adult=true;s.primaryType=ChaoState.FLY_TYPE;s.align=0;s.ageTicks=2345;s.adultAgeTicks=432;
    s.hunger=80;s.energy=100;s.happiness=70;s.setEvolutionStrength(1.2f);s.setTrait("fly61.foreign","preserve unrelated earned data");
    return ChaoState.decode(s.encode());
  }
  private static ChaoEntity actor(GameTestHelper h,ChaoState s){
    for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)h.setBlock(AT.offset(x,-1,z),Blocks.STONE);
    var c=ChaoGarden.CHAO.get().create(h.getLevel());h.assertTrue(c!=null,"Production Chao entity creates");
    c.owner=UUID.fromString("06100000-0000-0000-0000-000000000061");c.setState(s);c.setNoAi(true);c.setNoGravity(true);
    c.moveTo(Vec3.atBottomCenterOf(h.absolutePos(AT)));h.getLevel().addFreshEntity(c);cleanup(h,c::discard);return c;
  }
  private static void axis(ChaoState s,int rp,int sf){
    for(int stat=0;stat<4;stat++)DriveRules.setTotal(s,stat,0,new Random(61));
    if(rp!=0)DriveRules.setTotal(s,rp>0?ChaoState.POWER:ChaoState.RUN,Math.abs(rp),new Random(61));
    if(sf!=0)DriveRules.setTotal(s,sf>0?ChaoState.FLY:ChaoState.SWIM,Math.abs(sf),new Random(61));
  }
  private static void near(GameTestHelper h,float actual,float expected,String why){h.assertTrue(Float.isFinite(actual)&&Math.abs(actual-expected)<.00001f,why+": "+actual+" vs "+expected);}
  private static void syncAndNbt(GameTestHelper h,ChaoEntity c){
    c.sync();String encoded=c.state().encode();var input=NeutralFlyMorphology061.sample(c.state());
    var updates=c.getEntityData().getNonDefaultValues();
    h.assertTrue(updates!=null&&updates.stream().anyMatch(v->v.value() instanceof String s&&s.equals(encoded)),"Entity synchronization carries complete authoritative age/training/identity data");
    var observer=ChaoGarden.CHAO.get().create(h.getLevel());observer.getEntityData().assignValues(updates);
    h.assertTrue(observer.getEntityData().getNonDefaultValues().stream().anyMatch(v->v.value() instanceof String s&&s.equals(encoded)),"Observer receives exactly the same authoritative serialized record");
    var tag=new CompoundTag();c.saveWithoutId(tag);var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(tag);
    h.assertTrue(restored.state().encode().equals(encoded)&&restored.owner.equals(c.owner)&&restored.getUUID().equals(c.getUUID()),"Real entity NBT retains saved data, UUID and owner");
    h.assertTrue(NeutralFlyMorphology061.sample(restored.state()).equals(input),"Reload computes the same independent Neutral Fly morphology inputs");
    observer.discard();restored.discard();
  }
  private static void grid(GameTestHelper h,int rp,int sf,int age){
    var s=adult();axis(s,rp*960,sf*960);s.setEvolutionStrength(age*.6f);var c=actor(h,s);String encoded=s.encode();
    var p=NeutralFlyMorphology061.sample(s);h.assertTrue(p.enabled(),"Each of the six requested Neutral Fly combinations uses the new path");
    near(h,p.runPower(),rp,"Run/Power influence reads current opposing bar totals");near(h,p.swimFly(),sf,"Swim/Fly remains a separate compatible axis");near(h,p.maturity(),age*.5f,"Young/intermediate/mature states share independent biological age");
    var weights=NeutralFlyMorphology061.weights(p);float sum=0;for(float w:weights){h.assertTrue(Float.isFinite(w)&&w>=0&&w<=1,"Shape weights stay finite and convex");sum+=w;}
    near(h,sum,1,"The complete corner mix has unit mass");near(h,weights[0],1-age*.5f,"A trained young adult cannot jump directly to the mature form");
    near(h,weights[1+(sf+1)*3+(rp+1)],age*.5f,"Only the correct mature corner develops at a grid endpoint");
    h.assertTrue(encoded.equals(s.encode()),"Sampling the renderer inputs has no saved-state side effects");syncAndNbt(h,c);h.succeed();
  }
  private static void firstEvolution(GameTestHelper h){
    var s=new ChaoState();s.align=.125f;s.ageTicks=19;s.points[ChaoState.STAMINA]=9999;s.intelligence=4000;s.luck=4000;s.train(ChaoState.FLY,960,new Random(61));
    h.assertTrue(s.tick(20,100000,new Random(61))==ChaoState.Transition.EVOLVED,"The real trained first Fly cocoon completes");
    h.assertTrue(s.adult&&!s.immortal&&s.lockedPrimaryType()==ChaoState.FLY_TYPE&&s.lockedAlignment()==.125f,"Neutral Fly is selected naturally from earned points");
    var p=NeutralFlyMorphology061.sample(s);h.assertTrue(p.enabled(),"Natural first Fly uses the new scoped path");near(h,p.swimFly(),1,"Baby Fly training carries into adult targeting");near(h,p.maturity(),0,"First evolution starts young even when already trained");
    while(s.cocoonTicks>0)s.tick(20,100000,new Random(61));for(int i=0;i<40;i++)s.giveDrive(ChaoState.RUN);
    near(h,NeutralFlyMorphology061.sample(s).runPower(),-1,"Ordinary post-evolution Run drives develop compatible influence");h.assertTrue(s.lockedPrimaryType()==ChaoState.FLY_TYPE,"Later training retains first Fly identity");syncAndNbt(h,actor(h,s));h.succeed();
  }
  private static void otherFirstType(GameTestHelper h,int stat){
    var s=new ChaoState();s.ageTicks=19;s.align=0;s.points[stat]=100;s.levels[(stat+1)%4]=99;s.points[ChaoState.STAMINA]=9999;s.intelligence=4000;s.luck=4000;
    h.assertTrue(s.tick(20,100000,new Random(61))==ChaoState.Transition.EVOLVED&&s.lockedPrimaryType()==stat+1,"Highest earned points select first type, excluding Stamina and unrelated higher levels");
    h.assertTrue(NeutralFlyMorphology061.sample(s).enabled()==(stat==ChaoState.FLY),"Only the requested Neutral Fly-first form selects the new renderer");
    for(int earlier=0;earlier<4;earlier++)if(earlier!=stat){var tied=new ChaoState();tied.align=0;tied.train(earlier,1);tied.train(stat,1);tied.points[earlier]=tied.points[stat]=100;tied.points[ChaoState.STAMINA]=9999;tied=ChaoState.decode(tied.encode());tied.ageTicks=19;tied.tick(20,100000,new Random(61));h.assertTrue(tied.lockedPrimaryType()==stat+1,"Actual tied first evolution follows latest raised tied skill after reload");}
    syncAndNbt(h,actor(h,s));h.succeed();
  }
  private static void mutationRoutes(GameTestHelper h,int stat){
    var direct=adult();var ordinary=adult();var combined=adult();var analyzer=adult();
    direct.train(stat,240,new Random(61));for(int i=0;i<10;i++)ordinary.giveDrive(stat);
    h.assertTrue(DriveRules.apply(combined,DriveRules.Tier.SOLID,stat,new Random(61)),"Actual typed combined drive applies");DriveRules.change(combined,stat,140,new Random(61));
    AnalyzerStatRules.apply(analyzer,stat,AnalyzerStatRules.LEVEL,1);AnalyzerStatRules.apply(analyzer,stat,AnalyzerStatRules.LEVEL,1);AnalyzerStatRules.apply(analyzer,stat,AnalyzerStatRules.PROGRESS,40);
    var expected=NeutralFlyMorphology061.sample(direct);
    h.assertTrue(expected.equals(NeutralFlyMorphology061.sample(ordinary))&&expected.equals(NeutralFlyMorphology061.sample(combined))&&expected.equals(NeutralFlyMorphology061.sample(analyzer)),"Ordinary, typed combined, direct and Creative training all derive identical morphology from equal current totals");
    near(h,stat<2?expected.swimFly():expected.runPower(),stat==ChaoState.SWIM?0:stat==ChaoState.RUN?-.25f:.25f,"Correct independent axis expresses the actual training");
    int age=analyzer.ageTicks,adultAge=analyzer.adultAgeTicks;float maturity=analyzer.evolutionStrength();
    AnalyzerStatRules.apply(analyzer,stat,AnalyzerStatRules.PROGRESS,0);AnalyzerStatRules.apply(analyzer,stat,AnalyzerStatRules.LEVEL,-1);
    var reduced=NeutralFlyMorphology061.sample(analyzer);near(h,Math.abs(stat<2?reduced.swimFly():reduced.runPower()),(stat==ChaoState.SWIM?0:100f/960),"Creative level/progress decreases recede this same current-state input");
    h.assertTrue(analyzer.ageTicks==age&&analyzer.adultAgeTicks==adultAge&&analyzer.evolutionStrength()==maturity,"Stat editing does not rewrite chronological age/maturity");
    syncAndNbt(h,actor(h,analyzer));h.succeed();
  }
  private static void opposition(GameTestHelper h){
    var s=adult();axis(s,-960,960);s.setEvolutionStrength(.6f);int age=s.ageTicks,adultAge=s.adultAgeTicks;float maturity=s.evolutionStrength();
    h.assertTrue(DriveRules.apply(s,DriveRules.Tier.CHAOTIC,-1,new Random(61)),"Balanced four-stat drive applies");near(h,NeutralFlyMorphology061.sample(s).runPower(),-1,"Balanced combined drive preserves Run advantage");near(h,NeutralFlyMorphology061.sample(s).swimFly(),1,"Balanced combined drive preserves Fly advantage");
    for(int i=0;i<40;i++)s.giveDrive(ChaoState.POWER);near(h,NeutralFlyMorphology061.sample(s).runPower(),0,"Opposing Power cancels Run");near(h,NeutralFlyMorphology061.sample(s).swimFly(),1,"Independent Fly remains");
    for(int i=0;i<80;i++)s.giveDrive(ChaoState.SWIM);near(h,NeutralFlyMorphology061.sample(s).swimFly(),0,"Opposing and then excess Swim cancel later Fly without introducing Swim specialization");
    DriveRules.change(s,ChaoState.RUN,-480,new Random(61));DriveRules.change(s,ChaoState.SWIM,-1200,new Random(61));
    near(h,NeutralFlyMorphology061.sample(s).runPower(),.5f,"Run reductions expose stored opposing Power");near(h,NeutralFlyMorphology061.sample(s).swimFly(),.25f,"Swim decreases restore later Fly independently");
    h.assertTrue(s.ageTicks==age&&s.adultAgeTicks==adultAge&&s.evolutionStrength()==maturity&&s.lockedPrimaryType()==ChaoState.FLY_TYPE,"Reversal preserves both age inputs and first identity");syncAndNbt(h,actor(h,s));h.succeed();
  }
  private static void commutative(GameTestHelper h){
    for(int a:new int[]{ChaoState.RUN,ChaoState.POWER})for(int b:new int[]{ChaoState.FLY}){
      var first=adult();var reverse=adult();for(int i=0;i<40;i++)first.giveDrive(a);for(int i=0;i<40;i++)first.giveDrive(b);
      for(int i=0;i<40;i++)reverse.giveDrive(b);for(int i=0;i<40;i++)reverse.giveDrive(a);
      h.assertTrue(NeutralFlyMorphology061.sample(first).equals(NeutralFlyMorphology061.sample(reverse)),"Compatible pair order does not create a separate form");
      h.assertTrue(first.lockedPrimaryType()==ChaoState.FLY_TYPE&&reverse.lockedPrimaryType()==ChaoState.FLY_TYPE,"Later recency cannot rewrite either saved first identity");
    }h.succeed();
  }
  private static void locked(GameTestHelper h){
    var s=adult();axis(s,-960,960);var before=NeutralFlyMorphology061.sample(s);s.align=-1;s.primaryType=ChaoState.RUN_TYPE;
    h.assertTrue(NeutralFlyMorphology061.sample(s).equals(before),"Renderer reads the saved adult identity despite stale public field edits");
    s=ChaoState.decode(s.encode());h.assertTrue(s.align==0&&s.primaryType==ChaoState.FLY_TYPE,"Reload/normalization preserves original Neutral alignment and Fly first type");syncAndNbt(h,actor(h,s));h.succeed();
  }
  private static void hero(GameTestHelper h){
    var s=new ChaoState();s.adult=true;s.primaryType=ChaoState.RUN_TYPE;s.align=1;s.ageTicks=1000;s.adultAgeTicks=300;s.setEvolutionStrength(1.2f);s=ChaoState.decode(s.encode());axis(s,-960,960);
    var p=HeroRunMorphology052.sample(s);h.assertTrue(p.enabled()&&!NeutralFlyMorphology061.sample(s).enabled(),"Approved Hero Run is still selected independently from Neutral Fly adults");near(h,p.adultGrowth(),1,"Hero mature Run specialization survives compatible Fly training");
    var c=actor(h,s);syncAndNbt(h,c);var tag=new CompoundTag();c.saveWithoutId(tag);var copy=ChaoGarden.CHAO.get().create(h.getLevel());copy.load(tag);
    h.assertTrue(HeroRunMorphology052.sample(copy.state()).equals(p),"Real entity reload keeps the same approved Hero Run inputs");copy.discard();h.succeed();
  }
  private static void neutralRegression(GameTestHelper h){
    var s=new ChaoState();s.adult=true;s.primaryType=ChaoState.NORMAL;s.align=0;s.setEvolutionStrength(.6f);s=ChaoState.decode(s.encode());axis(s,-960,960);
    var p=NeutralMorphology056.sample(s);h.assertTrue(p.enabled()&&!NeutralFlyMorphology061.sample(s).enabled(),"Existing Neutral Normal path remains independently selected");near(h,p.maturity(),.5f,"Neutral maturity unchanged");near(h,p.runPower(),-1,"Neutral Run influence unchanged");near(h,p.swimFly(),1,"Neutral compatible Fly unchanged");syncAndNbt(h,actor(h,s));h.succeed();
  }
  private static ServerPlayer creative(GameTestHelper h,ChaoEntity c){
    var p=TestConnections.makeSurvivalServerPlayer(h);p.setGameMode(GameType.CREATIVE);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ChaoGarden.item("chao_analyzer")));
    c.owner=p.getUUID();p.teleportTo(c.getX()+1,c.getY(),c.getZ());
    cleanup(h,()->{if(p.server.getPlayerList().getPlayer(p.getUUID())==p)p.server.getPlayerList().remove(p);});return p;
  }
  private static int command(ServerPlayer p,ChaoEntity c,String arguments){
    try{return p.server.getCommands().getDispatcher().execute("chao \""+c.getName().getString()+"\" "+arguments,p.createCommandSourceStack().withPermission(0));}
    catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){throw new IllegalStateException(e);}
  }
  private static void commands(GameTestHelper h){
    var c=actor(h,adult());c.setCustomName(Component.literal("NeutralFly61 "+UUID.randomUUID()));var p=creative(h,c);var s=c.state();
    var decoy=actor(h,adult());decoy.setCustomName(Component.literal("OtherFly61 "+UUID.randomUUID()));decoy.moveTo(c.getX()+6,c.getY(),c.getZ());String untouched=decoy.state().encode();
    for(int alignment:new int[]{-100,100}){String before=s.encode();h.assertTrue(command(p,c,alignment+" Run 99")==0&&s.encode().equals(before),"Native command rejects conflicting locked adult alignment atomically");}
    for(String stat:List.of("Run","Fly"))h.assertTrue(command(p,c,"0 "+stat+" 10")==1,"Actual Creative command accepts locked Neutral alignment and selected stat");
    var a=NeutralFlyMorphology061.sample(s);near(h,a.runPower(),-1,"Actual command Run drives current morphology");near(h,a.swimFly(),1,"Actual compatible Fly shares same corner");
    h.assertTrue(s.lockedPrimaryType()==ChaoState.FLY_TYPE&&s.lockedAlignment()==0&&s.adult,"Command preserves Neutral Fly-first identity");
    int chronological=s.adultAgeTicks;h.assertTrue(command(p,c,"0 Run 0")==1,"Actual command reduces stat");near(h,NeutralFlyMorphology061.sample(s).runPower(),0,"Command reduction recedes Run");near(h,NeutralFlyMorphology061.sample(s).swimFly(),1,"Independent Fly survives");
    h.assertTrue(decoy.state().encode().equals(untouched),"Quoted native name selection changes only the intended Fly-first adult");
    try{
      h.assertTrue(p.server.getCommands().getDispatcher().execute("chao ~ 0 Power 10",p.createCommandSourceStack().withPermission(0))==1,"Nearest selector accepts the actual nearby Fly-first adult");
      h.assertTrue(p.server.getCommands().getDispatcher().execute("chao ~ 0 Swim 20",p.createCommandSourceStack().withPermission(0))==1,"Native excess opposing Swim command applies to current bars");
    }catch(com.mojang.brigadier.exceptions.CommandSyntaxException invalid){throw new IllegalStateException(invalid);}
    near(h,NeutralFlyMorphology061.sample(s).runPower(),1,"Nearest native edit develops Power independently");near(h,NeutralFlyMorphology061.sample(s).swimFly(),0,"Excess Swim returns only the secondary Fly axis to balanced");
    h.assertTrue(s.lockedPrimaryType()==ChaoState.FLY_TYPE&&s.typeName().equals("Fly/Power")&&decoy.state().encode().equals(untouched),"Opposing nearest edit preserves first Fly identity and the other loaded Chao");
    String before=s.encode();p.setGameMode(GameType.SURVIVAL);boolean denied=false;try{int result=p.server.getCommands().getDispatcher().execute("chao \""+c.getName().getString()+"\" 0 Power 99",p.createCommandSourceStack().withPermission(0));denied=result==0;}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){denied=true;}
    h.assertTrue(denied&&s.encode().equals(before),"Survival without admin permission cannot mutate through command dispatcher");
    h.assertTrue(s.adultAgeTicks==chronological&&s.evolutionStrength()==1.2f,"Command never changes chronology or biological growth");syncAndNbt(h,c);h.succeed();
  }
  private static void maturityPayload(GameTestHelper h){
    var c=actor(h,adult());var p=creative(h,c);var s=c.state();axis(s,-960,960);int calendar=s.ageTicks,life=s.adultAgeTicks;
    h.assertTrue(ChaoCareNetwork.editMaturity(p,new ChaoCareNetwork.MaturityEdit(c.getUUID().toString(),250)),"Real Creative Analyzer maturity handler accepts the younger growth fraction");
    var a=NeutralFlyMorphology061.sample(s);near(h,a.maturity(),.25f,"Authoritative maturity controls Neutral Fly growth");near(h,a.runPower(),-1,"Maturity does not erase Run training");near(h,a.swimFly(),1,"Maturity does not erase compatible Fly training");
    h.assertTrue(s.ageTicks==calendar&&s.adultAgeTicks==life&&s.adult&&!s.dead&&s.cocoonTicks==0&&s.lockedPrimaryType()==ChaoState.FLY_TYPE&&s.lockedAlignment()==0,"Maturity drag changes neither lifetime nor first identity and cannot invoke a cocoon");
    String before=s.encode();p.setGameMode(GameType.SURVIVAL);h.assertFalse(ChaoCareNetwork.editMaturity(p,new ChaoCareNetwork.MaturityEdit(c.getUUID().toString(),1000)),"Survival remains unable to edit Neutral Fly growth");h.assertTrue(s.encode().equals(before),"Denied maturity request is read-only");
    p.setGameMode(GameType.CREATIVE);h.assertTrue(ChaoCareNetwork.editMaturity(p,new ChaoCareNetwork.MaturityEdit(c.getUUID().toString(),1000)),"Full adult growth can be restored without new first evolution");near(h,NeutralFlyMorphology061.sample(s).maturity(),1,"Mature target restores independently");syncAndNbt(h,c);h.succeed();
  }
  private static void analyzerPayload(GameTestHelper h){
    var c=actor(h,adult());var p=creative(h,c);var s=c.state();String before=s.encode();
    for(int alignment:new int[]{-100,100})h.assertFalse(ChaoCareNetwork.editAlignment(p,new ChaoCareNetwork.AlignmentEdit(c.getUUID().toString(),alignment)),"Creative alignment remains read-only after first evolution");
    h.assertTrue(s.encode().equals(before),"Denied alignment cannot rewrite first Fly identity");
    h.assertTrue(ChaoCareNetwork.editStats(p,new ChaoCareNetwork.StatEdit(c.getUUID().toString(),ChaoState.FLY,AnalyzerStatRules.LEVEL,10)),"Actual Creative Analyzer stat payload updates current bars");near(h,NeutralFlyMorphology061.sample(s).swimFly(),1,"Analyzer payload reaches current later Fly influence");
    before=s.encode();p.setGameMode(GameType.SURVIVAL);h.assertFalse(ChaoCareNetwork.editStats(p,new ChaoCareNetwork.StatEdit(c.getUUID().toString(),ChaoState.RUN,AnalyzerStatRules.LEVEL,99)),"Survival modified client cannot use stat payload");h.assertFalse(ChaoCareNetwork.editAlignment(p,new ChaoCareNetwork.AlignmentEdit(c.getUUID().toString(),100)),"Survival modified client cannot use alignment payload");h.assertTrue(s.encode().equals(before),"Denied Analyzer edits remain read-only");syncAndNbt(h,c);h.succeed();
  }
  private static void colors(GameTestHelper h){
    var s=adult();axis(s,-960,480);var reference=NeutralFlyMorphology061.sample(s);
    for(int color=0;color<ChaoState.COLORS.length;color++)for(boolean shine:new boolean[]{false,true})for(boolean tone:new boolean[]{false,true}){
      s.expressedColor=color;s.shiny=shine;s.twoTone=tone;h.assertTrue(NeutralFlyMorphology061.sample(s).equals(reference),"Color, one/two-tone and shiny choices share the exact same shape inputs");
    }
    for(int jewel=0;jewel<ChaoState.JEWELS.length;jewel++){s.expressedJewel=jewel;h.assertTrue(NeutralFlyMorphology061.sample(s).equals(reference),"Jewel finish never changes the Neutral Fly morphology");}
    syncAndNbt(h,actor(h,s));h.succeed();
  }
  /** A genuine old Neutral fixture stays literal. The Fly migration case is
   * explicitly synthetic: supported expanded-care21 layout + explicitly substituted Neutral Fly identity.
   * It does not claim that a saved older Neutral Fly world was located. */
  private static void legacy(GameTestHelper h,boolean synthetic){
    try(var input=NeutralFlyMorphologyGameTests061.class.getResourceAsStream("/data/chao_morph52/legacy_states.json")){
      h.assertTrue(input!=null,"Retained historical world data is available");var d=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();var fixture=d.getAsJsonArray("states").get(synthetic?1:0).getAsJsonObject();
      String original=fixture.get("encoded").getAsString(),expected=synthetic?"495d2d4175ff38709f5041a3e88d5cd4dde4823c07847a72bb36ef475887692c":"bba10bc1858289535b28f115b2ba03c32c9ecf9e52822e05786c421111d8fdc0";
      h.assertTrue(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(original.getBytes(StandardCharsets.UTF_8))).equals(expected),"Original historical fixture bytes are unchanged");
      String encoded=original;
      if(synthetic){
        String[] fields=original.split(";",-1),axes=fields[13].split(",",-1),extra=fields[21].split(",",-1);
        axes[0]=Float.toHexString(0);fields[13]=String.join(",",axes);String[] values=fields[11].split(",",-1);values[13]=Integer.toString(ChaoState.FLY_TYPE);fields[11]=String.join(",",values);fields[21]=String.join(",",java.util.Arrays.copyOf(extra,21));encoded=String.join(";",fields);
        h.assertTrue(fields[21].split(",",-1).length==21,"Synthetic pre-lock Fly care layout is explicit, without first-evolution lock fields");
      }
      var s=ChaoState.decode(encoded);
      h.assertTrue(s.adult&&s.lockedPrimaryType()==(synthetic?ChaoState.FLY_TYPE:ChaoState.NORMAL),"Supported old layout retains the explicitly supplied first identity");
      if(synthetic){h.assertTrue(s.lockedAlignment()==0&&NeutralFlyMorphology061.sample(s).enabled(),"Synthetic older Neutral Fly initializes its existing alignment lock without re-evolution");}
      else{h.assertTrue(s.lockedAlignment()==0&&NeutralMorphology056.sample(s).enabled()&&!NeutralFlyMorphology061.sample(s).enabled(),"Genuine old Neutral remains Neutral and on the delivered056 path");}
      String normalized=s.encode();var sample=NeutralFlyMorphology061.sample(s);h.assertTrue(normalized.equals(s.encode())&&sample.equals(NeutralFlyMorphology061.sample(s)),"Repeated sampling changes no migrated saved state");syncAndNbt(h,actor(h,s));h.succeed();
    }catch(Exception e){throw new IllegalStateException("Cannot exercise supported saved layout",e);}
  }
}
