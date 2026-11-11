package com.chaogarden.core;

public final class WardrobeChoiceTest {
  private static int checks;
  private static void check(boolean result,String message){checks++;if(!result)throw new AssertionError(message);}
  public static int run(){
    checks=0;var state=new ChaoState();state.energy=50;
    for(int energy=0;energy<=100;energy+=10){state.energy=energy;int previous=-1;
      for(int happy=-100;happy<=100;happy++){
        state.happiness=happy;int p=WardrobeChoice.percent(state),yes=0;
        check(p>=15&&p<=90,"Awake wardrobe chance bounded15..90");
        check(p>=previous,"Happiness cannot reduce willingness");previous=p;
        for(int roll=0;roll<100;roll++)if(WardrobeChoice.accepts(state,roll))yes++;
        check(yes==p,"Every percentage has exactly its stated successful rolls");
      }
    }
    state.happiness=0;state.energy=50;int ordinary=WardrobeChoice.percent(state);
    ChaoEmotion.set(state,ChaoEmotion.JOY,200);check(WardrobeChoice.percent(state)>ordinary,"Joy improves willingness");
    ChaoEmotion.set(state,ChaoEmotion.JOY,0);ChaoEmotion.set(state,ChaoEmotion.ANGER,200);check(WardrobeChoice.percent(state)<ordinary,"Anger lowers willingness");
    ChaoEmotion.set(state,ChaoEmotion.CRY,200);ChaoEmotion.set(state,ChaoEmotion.FEAR,200);check(WardrobeChoice.percent(state)>=15,"Distress still has a bounded low chance");
    for(int extreme:new int[]{Integer.MIN_VALUE,-1000,1000,Integer.MAX_VALUE}){
      state.happiness=extreme;state.energy=extreme;check(WardrobeChoice.percent(state)>=15&&WardrobeChoice.percent(state)<=90,"Corrupt legacy meters cannot overflow chance");
    }
    check(!WardrobeChoice.accepts(state,-1)&&!WardrobeChoice.accepts(state,100),"Invalid rolls never accept");
    state.sleeping=true;check(WardrobeChoice.percent(state)==0,"Sleeping Chao do not choose outfits");
    state.sleeping=false;state.cocoonTicks=1;check(WardrobeChoice.percent(state)==0,"Cocooned Chao do not choose outfits");
    state.cocoonTicks=0;state.dead=true;check(WardrobeChoice.percent(state)==0,"Dead Chao cannot accept");
    return checks;
  }
}
