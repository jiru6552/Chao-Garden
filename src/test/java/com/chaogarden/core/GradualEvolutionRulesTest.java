package com.chaogarden.core;

import java.util.Arrays;
import java.util.Random;

/** Observable lifecycle, training history and save boundaries for gradual evolution. */
public final class GradualEvolutionRulesTest {
  private static int checks;
  private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
  private static void near(float actual,float expected,String why){check(Math.abs(actual-expected)<.00001f,why+": "+actual+" versus "+expected);}
  public static int run(){checks=0;firstTypeAndHistory();ageAndFood();fixedPrimaryMutableSecondary();migrationAndPersistence();optionalExtension();return checks;}

  private static void firstTypeAndHistory(){
    check(new ChaoState().prospectivePrimaryType()==ChaoState.NORMAL,"An untouched child has no invented specialism");
    for(int stat=0;stat<5;stat++)for(float alignment:new float[]{-1,0,1}){
      var s=new ChaoState();s.points[stat]=100;s.align=alignment;s.ageTicks=99;
      check(s.tick(100,1000,new Random(1))==ChaoState.Transition.EVOLVED,"A child reaches its actual first cocoon");
      check(s.primaryType==(stat==4?0:stat+1),"Highest trained ability or Stamina selects the requested primary");
      check(s.align==alignment&&s.adult&&s.evolutionStrength()==0,"First evolution locks alignment and restarts adult development");
    }
    var partial=new ChaoState();partial.train(ChaoState.FLY,1,new Random(1));
    check(partial.points[1]==0&&partial.prospectivePrimaryType()==ChaoState.FLY_TYPE,"A real positive partial bar is a tie-breaking increase even before its first level");
    partial.train(ChaoState.RUN,1,new Random(2));partial.train(ChaoState.SWIM,-1,new Random(3));
    check(partial.lastTrainedStat()==ChaoState.RUN&&partial.prospectivePrimaryType()==ChaoState.RUN_TYPE,"Negative training cannot steal the latest positive increase");
    var tie=new ChaoState();tie.train(0,1);tie.train(1,1);tie.train(2,1);
    tie.points[0]=tie.points[1]=100;tie.points[2]=3;
    tie=ChaoState.decode(tie.encode());
    check(tie.lastTrainedStat()==2&&tie.prospectivePrimaryType()==ChaoState.FLY_TYPE,"Most recent AMONG tied greatest stats wins, even when the latest overall stat is not greatest");
    tie.train(0,1);check(tie.prospectivePrimaryType()==ChaoState.SWIM_TYPE,"Changing which tied stat was last raised changes the prospective primary");
    tie.points[4]=101;check(tie.prospectivePrimaryType()==ChaoState.NORMAL,"Highest Stamina keeps Normal and legitimate Chaos evolution obtainable");
  }

  private static void ageAndFood(){
    var child=new ChaoState();child.swimFly=1;
    for(int i=0;i<50;i++)child.tick(100,10000,new Random(i));
    near(child.evolutionStrength(),.5f,"Child strength follows loaded age independently of saturated directions");
    child.onFruitEaten();near(child.evolutionStrength(),.52f,"One four-bite fruit adds child magnitude once");
    check(child.swimFly==1&&child.ageTicks==50,"Fruit maturity does not fake extra loaded ticks or alter the chosen direction");
    var fed=new ChaoState();for(int i=0;i<50;i++)fed.onFruitEaten();
    check(fed.tick(10000,10000,new Random(1))==ChaoState.Transition.EVOLVED,"Sufficient food advances first evolution without waiting for its old age-only threshold");
    near(fed.evolutionStrength(),0,"Adult starts with no fully aged secondary deformation");
    fed.cocoonTicks=0;for(int i=0;i<100;i++)fed.tick(100,10000,new Random(i));
    near(fed.evolutionStrength(),.25f,"Adult magnitude ages at a quarter of the childhood rate");
    fed.onFruitEaten();near(fed.evolutionStrength(),.255f,"One whole adult fruit adds four source bites at a quarter childhood strength");
    float strength=fed.evolutionStrength();fed.cocoonTicks=20;for(int i=0;i<20;i++)fed.tick(100,10000,new Random(i));
    near(fed.evolutionStrength(),strength,"Cocoon time does not become extra adult development");
    fed.setEvolutionStrength(100);near(fed.evolutionStrength(),1.2f,"Adult maturity is bounded to the ordinary source cap");
    fed.setEvolutionStrength(Float.NaN);near(fed.evolutionStrength(),0,"Invalid fixture magnitude is repaired without poisoning the state");
    fed.reincarnate();check(!fed.adult&&fed.evolutionStrength()==0&&fed.lastTrainedStat()==-1,"A new life resets maturity and training recency");
  }

  private static void fixedPrimaryMutableSecondary(){
    var s=new ChaoState();s.align=.8f;s.points[1]=100;s.ageTicks=99;s.tick(100,10000,new Random(1));s.cocoonTicks=0;
    for(int i=0;i<40;i++)s.giveDrive(ChaoState.FLY);
    check(Arrays.equals(s.secondaryTypes(),new int[]{ChaoState.FLY_TYPE}),"Adult can specialize in its fixed first type");
    for(int i=0;i<80;i++)s.giveDrive(ChaoState.SWIM);
    check(s.primaryType==ChaoState.FLY_TYPE&&Arrays.equals(s.secondaryTypes(),new int[]{ChaoState.SWIM_TYPE}),"Fly primary may be retrained to Swim secondary; the first category stays fixed");
    for(int i=0;i<40;i++)s.giveDrive(ChaoState.POWER);
    check(Arrays.equals(s.secondaryTypes(),new int[]{ChaoState.SWIM_TYPE,ChaoState.POWER_TYPE}),"Valid cross-axis secondary combination is expressed");
    int samples=0;
    for(float sf:new float[]{-1,0,1})for(float rp:new float[]{-1,0,1}){
      s.swimFly=sf;s.runPower=rp;s.tick(100,10000,new Random(samples++));int[] t=s.secondaryTypes();
      boolean swim=false,fly=false,run=false,power=false;for(int v:t){swim|=v==1;fly|=v==2;run|=v==3;power|=v==4;}
      check(!(swim&&fly)&&!(run&&power)&&t.length<=2,"Opposing secondary types never coexist at any corner or axis center");
      check(s.primaryType==ChaoState.FLY_TYPE&&s.align==.8f&&s.cocoonTicks==0,"Secondary drift does not unlock alignment, change the primary, or create another cocoon");
    }
    s.align=-1;s.primaryType=ChaoState.RUN_TYPE;s=snapshot(s);
    check(s.align==.8f&&s.primaryType==ChaoState.FLY_TYPE,"The persisted first-evolution lock survives direct stale/public-field rewrites");
  }

  private static ChaoState snapshot(ChaoState state){return ChaoState.decode(state.encode());}
  private static void migrationAndPersistence(){
    var s=new ChaoState();s.adult=true;s.primaryType=2;s.align=-.7f;s.adultAgeTicks=80;s.swimFly=1;s.runPower=-1;
    s.setEvolutionStrength(.6f);s.train(3,5);s.setTrait("reward.wrench",1);String identity=s.lineageId();
    String encoded=s.encode();check(encoded.startsWith("6;"),"New bounded state has an explicit schema version");
    var saved=snapshot(s);near(saved.evolutionStrength(),.6f,"Age strength survives exact state serialization");
    check(saved.encode().equals(encoded)&&saved.lastTrainedStat()==3&&saved.lineageId().equals(identity)&&saved.intTrait("reward.wrench")==1,"Clock, training history, stable identity and earned rewards round-trip together");
    String[] old=encoded.split(";",25);old[0]="5";old[21]=String.join(",",Arrays.copyOf(old[21].split(","),21));old[13]="-0x1.666666p-1,0x1.8p1,-0x1.4p1";
    var legacy=ChaoState.decode(String.join(";",old));
    check(legacy.adult&&legacy.primaryType==2&&legacy.lineageId().equals(identity)&&legacy.intTrait("reward.wrench")==1,"Version5 migration preserves actual adult type, identity and content");
    check(legacy.swimFly==1&&legacy.runPower==-1&&legacy.optionalMonsterSwimFly()==0,"Legacy exaggerated axes are restrained, without silently recreating optional overdrive");
    legacy.tick(100,10000,new Random(1));near(legacy.evolutionStrength(),81f/400,"Legacy maturity derives from its saved adult clock once");
    legacy=snapshot(legacy);legacy.tick(100,10000,new Random(2));near(legacy.evolutionStrength(),82f/400,"Reload never adds an already observed age a second time");
    s=new ChaoState();s.adult=true;for(int i=0;i<256;i++)s.setTrait("record.v"+i,i);s.setEvolutionStrength(.4f);s.train(0,1);
    s=snapshot(s);check(s.traitEntries().size()==256&&s.lastTrainedStat()==0,"Saturated feature maps cannot discard the separate training clock or history");near(s.evolutionStrength(),.4f,"Maturity persists independently of trait capacity");
  }

  private static void optionalExtension(){
    var s=new ChaoState();s.adult=true;s.primaryType=2;s.points[1]=3000;s.swimFly=1;s.giveDrive(1,true);
    check(s.swimFly==1&&s.optionalMonsterSwimFly()>1&&s.monsterStage()==2,"Explicit optional overdrive has separate storage and never stretches the ordinary axis");
    s=snapshot(s);check(s.swimFly==1&&s.optionalMonsterSwimFly()>1&&s.monsterStage()==2,"Separate optional progression survives new-schema persistence");
    s.giveDrive(1);check(s.swimFly==1&&s.monsterStage()==0,"Ordinary feeding never enables the optional extreme model");
  }
}
