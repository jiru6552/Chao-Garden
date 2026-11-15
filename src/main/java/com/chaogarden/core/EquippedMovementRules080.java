package com.chaogarden.core;

import java.util.ArrayDeque;

/** Bounded history of native server velocities; never a client-supplied force. */
public final class EquippedMovementRules080 {
  public static final int LIFETIME_TICKS=20,HISTORY_TICKS=8;
  private EquippedMovementRules080() {}
  public static int nativeBurst(int pending){return pending>=1&&pending<=5?pending:1;}
  public static double expectedSquared(double ordinary,double trustedSquared,int burst){
    if(!Double.isFinite(ordinary)||ordinary<0||!Double.isFinite(trustedSquared)||trustedSquared<0)return ordinary;
    int count=nativeBurst(burst);double bound=trustedSquared*count*count;
    return Double.isFinite(bound)?Math.max(ordinary,bound):ordinary;
  }
  public static final class History {
    private record Sample(long tick,double squared){}
    private final ArrayDeque<Sample> samples=new ArrayDeque<>();
    private long authorizedAt,expires;private double authorizedCapSquared;
    public History(long tick,double squared){authorize(tick,squared);}
    public void authorize(long tick,double squared){
      authorizedAt=tick;expires=tick+LIFETIME_TICKS;
      if(Double.isFinite(squared)&&squared>=0)authorizedCapSquared=Math.max(authorizedCapSquared,squared);
      sample(tick,squared);
    }
    public boolean active(long tick){return tick>=authorizedAt&&tick<expires;}
    public void sample(long tick,double squared){
      prune(tick);if(!active(tick)||!Double.isFinite(squared)||squared<0)return;
      // Ordinary movement after the hit cannot expand its trusted envelope.
      // Only another completed, server-authored authorized hit raises this cap.
      squared=Math.min(squared,authorizedCapSquared);
      var last=samples.peekLast();
      if(last!=null&&last.tick()==tick){samples.removeLast();squared=Math.max(squared,last.squared());}
      samples.addLast(new Sample(tick,squared));
      while(samples.size()>HISTORY_TICKS)samples.removeFirst();
    }
    private void prune(long tick){while(!samples.isEmpty()&&tick-samples.peekFirst().tick()>=HISTORY_TICKS)samples.removeFirst();}
    public double maximum(long tick){prune(tick);if(!active(tick))return 0;double result=0;for(var sample:samples)result=Math.max(result,sample.squared());return result;}
    public int size(){return samples.size();}
  }
}
