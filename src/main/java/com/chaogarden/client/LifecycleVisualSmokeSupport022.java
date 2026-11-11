package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.GardenConfig;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.GradualEvolutionRules;
import com.chaogarden.entity.ChaoEggEntity;
import com.chaogarden.entity.ChaoEntity;
import java.util.Arrays;
import java.util.Random;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Real lifecycle transitions for the opt-in gallery; excluded from the installed JAR. */
public final class LifecycleVisualSmokeSupport022 {
  private static final AABB AREA=new AABB(-3,62,-3,4,69,4);
  private LifecycleVisualSmokeSupport022() {}
  private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Lifecycle preview: "+why);}
  private static void freeze(ChaoEntity c){c.setNoAi(true);c.setNoGravity(true);c.noPhysics=true;c.setOnGround(true);c.setDeltaMovement(Vec3.ZERO);}
  private static ChaoEntity hatch(ServerLevel world,ChaoEggEntity egg){
    String lineage=egg.state().lineageId();egg.setIncubation(Math.max(0,GardenConfig.HATCH_TICKS.get()-1));egg.tick();
    var bodies=world.getEntitiesOfClass(ChaoEntity.class,AREA,c->c.state().lineageId().equals(lineage));
    require(egg.isRemoved()&&bodies.size()==1,"one physical egg must hatch once");
    var c=bodies.getFirst();freeze(c);return c;
  }
  private static void firstCocoon(ChaoEntity c){
    c.state().ageTicks=GradualEvolutionRules.durationTicks(GardenConfig.CHILD_TICKS.get())-1;
    c.state().setEvolutionStrength(0);c.aiStep();
    require(c.state().adult&&c.state().primaryType==ChaoState.FLY_TYPE&&c.state().cocoonTicks==200,"actual first Fly cocoon");
  }
  private static void finishFirstCocoon(ChaoEntity c){
    for(int i=0;i<200;i++)c.aiStep();
    require(c.state().adult&&c.state().cocoonTicks==0&&c.state().adultAgeTicks==0,"first cocoon pauses the adult clock");
    freeze(c);
  }
  private static void mature(ChaoEntity c){for(int i=0;i<240;i++)c.state().onFruitEaten();require(c.state().evolutionStrength()>1.199f,"full-adult maturity");}

  /** Only incubation/age and whole-fruit maturity clocks are accelerated. */
  public static Entity buildStage(MinecraftServer server,String requested){
    var world=server.overworld();var keeper=server.getPlayerList().getPlayers().getFirst();
    String stage=requested.startsWith("lifecycle-")?requested.substring(10):requested;
    if(stage.startsWith("grade-audit-")){
      int grade=Integer.parseInt(stage.substring(12));require(grade>=0&&grade<=5,"E-S grade fixture");
      var c=ChaoGarden.CHAO.get().create(world);var s=new ChaoState();s.adult=true;
      Arrays.fill(s.grades,grade);Arrays.fill(s.gradeA,grade);Arrays.fill(s.gradeB,grade);
      for(int stat=0;stat<5;stat++)s.train(stat,100,new Random(22022+stat));
      c.setState(s);c.owner=keeper.getUUID();c.setCustomName(Component.literal("Grade "+ChaoState.gradeName(grade)));
      c.moveTo(.5,64,.5,180,0);freeze(c);require(world.addFreshEntity(c),"grade actor insertion");c.sync();return c;
    }
    boolean stats=stage.startsWith("stats-");var s=ChaoState.egg(0,false,true,0,new Random(22022));
    s.happiness=90;s.bond=90;s.hunger=80;s.energy=100;s.points[ChaoState.FLY]=100;
    if(stats){
      s.points=new int[]{1255,2509,1699,21,1009};s.levels=new int[]{99,90,99,1,70};s.progress=new int[]{97,99,36,12,76};
      s.grades=new int[]{5,4,3,5,3};s.gradeA=s.grades.clone();s.gradeB=s.grades.clone();
    }
    var egg=ChaoGarden.CHAO_EGG.get().create(world);egg.setState(s);egg.owner=keeper.getUUID();egg.chaoName=Component.literal("Mochi");
    egg.setNoGravity(true);egg.moveTo(.5,64,.5,180,0);require(world.addFreshEntity(egg),"egg insertion");
    if(stage.equals("egg"))return egg;
    var c=hatch(world,egg);if(stage.equals("baby")){c.sync();return c;}
    firstCocoon(c);if(stage.equals("cocoon")||stage.equals("evolution-cocoon")){c.sync();return c;}
    finishFirstCocoon(c);if(stage.equals("young")||stage.equals("young-adult")){c.sync();return c;}
    mature(c);if(stage.equals("adult")||stage.equals("fully-adult")||stage.equals("stats-before")){c.sync();return c;}
    boolean death=stage.equals("death")||stage.equals("death-cocoon");c.state().happiness=death?0:90;
    c.state().adultAgeTicks=GradualEvolutionRules.durationTicks(GardenConfig.ADULT_TICKS.get())-1;
    boolean natural=GardenConfig.NATURAL_DEATH.get();GardenConfig.NATURAL_DEATH.set(true);
    try{c.aiStep();}finally{GardenConfig.NATURAL_DEATH.set(natural);}
    require(c.state().cocoonTicks==200&&(death?c.isDeathCocoon():c.isRebirthCocoon()),"actual terminal cocoon");
    if(death||stage.equals("rebirth")||stage.equals("rebirth-cocoon")){c.sync();return c;}
    var identity=c.getUUID();for(int i=0;i<200;i++)c.aiStep();
    var eggs=world.getEntitiesOfClass(ChaoEggEntity.class,AREA,e->e.state().lineageId().equals(s.lineageId()));
    require(c.isRemoved()&&eggs.size()==1,"actual body-to-egg transaction");var reborn=eggs.getFirst();reborn.setNoGravity(true);
    if(stage.equals("reborn-egg"))return reborn;
    require(stage.equals("stats-after"),"known stage "+stage);var next=hatch(world,reborn);
    require(next.getUUID().equals(identity)&&Arrays.equals(next.state().points,new int[]{125,250,169,2,100})
        &&Arrays.equals(next.state().levels,new int[]{1,1,1,1,1})&&Arrays.equals(next.state().progress,new int[5]),"retained next-life statistics");
    next.sync();return next;
  }
}
