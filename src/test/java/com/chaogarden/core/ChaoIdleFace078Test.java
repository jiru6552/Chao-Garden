package com.chaogarden.core;

import java.util.Random;
import com.chaogarden.core.ChaoFaceRules076.*;

/** Temporary idle affect must yield to real needs and restore inherited faces exactly. */
public final class ChaoIdleFace078Test {
  private static int checks;
  private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
  private static ChaoState state(boolean adult,float align,int eye,int mouth) {
    var s=ChaoState.random(new Random(78));s.adult=adult;s.align=align;
    s.sleeping=false;s.dead=false;s.cocoonTicks=0;s.energy=100;s.hunger=80;
    java.util.Arrays.fill(s.emotions,0);java.util.Arrays.fill(s.illnessValues,0);
    s.setBaselineFace076(eye,mouth);return s;
  }
  public static int run() {
    checks=0;
    for(boolean adult:new boolean[]{false,true})for(float align:new float[]{-1,0,1})
      for(int eye=0;eye<5;eye++)for(int mouth=0;mouth<5;mouth++)for(String idle:ChaoIdleRules078.IDS) {
        var s=state(adult,align,eye,mouth);String before=s.encode();
        var quiet=Signals.quiet();var base=ChaoFaceRules076.resolve(s,quiet);
        var during=ChaoFaceRules076.resolve(s,quiet,idle,30);
        check(during.style()==base.style(),"idle changed alignment/lens style");
        check(during.baselineEyes()==eye&&during.baselineMouth()==mouth,"idle rerolled inherited face");
        check(ChaoFaceRules076.resolve(s,quiet,idle,0).equals(base),"entry face changed before gesture");
        check(ChaoFaceRules076.resolve(s,quiet,idle,ChaoIdleRules078.duration(idle)).equals(base),"idle face stuck on exit");
        check(ChaoFaceRules076.resolve(s,quiet,"",0).equals(base),"interrupted face failed to restore");
        check(before.equals(s.encode()),"idle face mutated state");
        for(var signal:new Signals[]{
            new Signals(false,true,false,false,false,Symptom.NONE,false,false,false,null,-1,false),
            new Signals(false,false,true,false,false,Symptom.NONE,false,false,false,null,-1,false),
            new Signals(false,false,false,true,false,Symptom.NONE,false,false,false,null,-1,false),
            new Signals(false,false,false,false,false,Symptom.SCRATCH,true,false,false,null,-1,false),
            new Signals(false,false,false,false,false,Symptom.NONE,true,false,false,null,-1,false),
            new Signals(false,false,false,false,false,Symptom.NONE,false,true,false,null,-1,false),
            new Signals(false,false,false,false,false,Symptom.NONE,false,false,true,null,-1,false)})
          check(ChaoFaceRules076.resolve(s,signal,idle,30).equals(ChaoFaceRules076.resolve(s,signal)),"idle beat higher-priority expression");
      }
    var s=state(true,1,0,3);
    check(ChaoFaceRules076.resolve(s,Signals.quiet(),"tired",30).mouth()==Mouth.OPEN,"yawn missing");
    check(ChaoFaceRules076.resolve(s,Signals.quiet(),"chuckle",26).eyes()==Eyes.HAPPY_CLOSED,"laugh eyes missing");
    check(ChaoFaceRules076.resolve(s,Signals.quiet(),"think",30).mouth()==Mouth.FLAT,"thinking face missing");
    for(int iris:new int[]{9,10,11}) {
      s.eyeColor=iris;var style=ChaoFaceRules076.resolve(s,Signals.quiet()).style();
      for(String idle:ChaoIdleRules078.IDS)check(ChaoFaceRules076.resolve(s,Signals.quiet(),idle,30).style()==style,"special lens changed");
    }
    s.immortal=true;
    for(String idle:ChaoIdleRules078.IDS)check(ChaoFaceRules076.resolve(s,Signals.quiet(),idle,30).nativeExcluded(),"Chaos face replaced");
    s.immortal=false;s.character="tails";
    for(String idle:ChaoIdleRules078.IDS)check(ChaoFaceRules076.resolve(s,Signals.quiet(),idle,30).nativeExcluded(),"character face replaced");
    return checks;
  }
}
