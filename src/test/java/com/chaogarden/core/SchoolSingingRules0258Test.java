package com.chaogarden.core;

/** Auditory cadence: solo call-and-response, bounded repetition and true rests. */
public final class SchoolSingingRules0258Test {
  private static int checks;
  private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
  public static void main(String[] args){checks=0;
    int[] plays=new int[2],notes=new int[2];
    for(long now=0;now<1200;now++){
      int a=SchoolSingingRules0258.phraseAt(2,now),b=SchoolSingingRules0258.phraseAt(3,now);
      check(!(a>=0&&b>=0),"Singers begin together and double the voice volume");
      if(a>=0){check(a==0,"First pupil uses the wrong phrase");plays[0]++;}
      if(b>=0){check(b==1,"Second pupil uses the wrong response");plays[1]++;}
      for(int slot=2;slot<=3;slot++)if(SchoolSingingRules0258.noteAt(slot,now))notes[slot-2]++;
      boolean aAudible=SchoolSingingRules0258.localTick(2,now)<56;
      boolean bAudible=SchoolSingingRules0258.localTick(3,now)<56;
      check(!(aAudible&&bAudible),"Authored2.8s phrases overlap instead of leaving a rest");
    }
    check(plays[0]==10&&plays[1]==10,"A pupil floods sounds or misses a complete cycle");
    check(notes[0]==60&&notes[1]==60,"Notes do not follow six sung syllables per phrase");
    for(int slot:new int[]{-1,0,1,4,100})for(long time:new long[]{0,8,60,120,Long.MIN_VALUE,Long.MAX_VALUE})
      check(SchoolSingingRules0258.phraseAt(slot,time)==-1&&!SchoolSingingRules0258.noteAt(slot,time),"A non-singer emits classroom vocals");
    for(int slot=2;slot<=3;slot++)for(long time:new long[]{-121,-120,-1,0,1,119,120,121,Long.MIN_VALUE,Long.MAX_VALUE})
      check(SchoolSingingRules0258.localTick(slot,time)>=0&&SchoolSingingRules0258.localTick(slot,time)<120,"Clock edge escaped a bounded musical cycle");
    System.out.println("School singing rules0258: "+checks+" checks passed");
  }
}
