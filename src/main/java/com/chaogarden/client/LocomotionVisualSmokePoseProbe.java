package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.mojang.logging.LogUtils;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Development pose contracts, excluded from the installable JAR. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class LocomotionVisualSmokePoseProbe {
  private static final String[] KEYS={"baby","swim","fly","run","power","hero","hero_swim","hero_fly","hero_run","hero_power","dark","dark_swim","dark_fly","dark_run","dark_power","amy","omochao","tails","knuckles","chaos_light","chaos_angel","chaos_devil"};
  private static boolean done;
  private LocomotionVisualSmokePoseProbe() {}

  @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
    if(done||!Boolean.getBoolean("chao_garden.audit022Study")||Minecraft.getInstance().level==null)return;
    done=true;
    int source=ReleaseVisualSmokeOfficialProbe.inspect();
    int samples=inspect();
    int forms=inspectForms019();
    LogUtils.getLogger().info("CHAO_GRADUAL_021_PROBE_COMPLETE: {} samples; 15 identical young bases across axes and 135 distinct aged shapes; age0/0.6/1.2; 15 primary skeletons; every synced behavior finite",forms);
    LogUtils.getLogger().info("CHAO_LOCOMOTION_POSE_PROBE_COMPLETE: {} gameplay poses; {} raw imported poses; 22 active gameplay rigs and20 exact reference rigs; quiet idle, visible contact/recovery crawling, gait fitting, gesture duty and transitions verified",samples,source);
  }

  private static void require(boolean condition,String message) {
    if(!condition)throw new IllegalStateException("Chao locomotion pose: "+message);
  }

  public static int inspect() {
    int samples=0;
    var rig=new OfficialChaoRig();
    UUID stride=new UUID(18,18);
    rig.animateGameplay("baby",stride,OfficialChaoRig.Motion.WALK,120,0,.04f,0,0,0,0,false);
    float firstFoot=rig.pose("foot_left").rx;
    rig.animateGameplay("baby",stride,OfficialChaoRig.Motion.WALK,120,.2f,.04f,0,0,0,0,false);
    require(Math.abs(rig.pose("foot_left").rx-firstFoot)>.15f,"walking stride stretched into a nearly static clip");
    samples+=2;
    for(int i=0;i<KEYS.length;i++)for(var mode:OfficialChaoRig.Motion.values()) {
      UUID id=new UUID(i+1,mode.ordinal()+1);
      for(int age=0;age<=160;age+=4) {
        rig.animateGameplay(KEYS[i],id,mode,age,age*.35f,age%16==0?0:.35f,
            0,0,0,0,true);
        require(rig.finite(),KEYS[i]+" / "+mode+" non-finite sample");
        require(rig.idleGestureWeight()>=0 && rig.idleGestureWeight()<=1,"gesture bounds");
        if(mode==OfficialChaoRig.Motion.CRAWL) {
          require(rig.pose("body")==null || rig.pose("body").rx>.9f,"crawl body must lean toward the ground");
          require(rig.pose("skull")==null || rig.pose("skull").y>.2f,"crawl head must be lower");
        }
        samples++;
      }
    }
    for(int i=0;i<32;i++) {
      long seed=i*0x9e3779b97f4a7c15L;
      int active=0;
      for(int age=0;age<4000;age++) {
        float weight=OfficialChaoRig.gestureWeight(age,seed);
        require(Float.isFinite(weight) && weight>=0 && weight<=1,"gesture sample range");
        if(weight>0)active++;
      }
      require(active>0 && active<480,"sway must occupy less than 12 percent of idle time");
    }
    int phased=0;
    for(int i=0;i<64;i++)if(OfficialChaoRig.gestureWeight(60,i*0x9e3779b97f4a7c15L)>0)phased++;
    require(phased>0 && phased<64,"UUID phases must not synchronize every Chao");
    UUID quiet=new UUID(90,42);
    for(int age=0;age<=200;age+=5) {
      rig.animateGameplay("baby",quiet,OfficialChaoRig.Motion.WALK,age,3,0,0,0,0,0,false);
      require(Math.abs(rig.pose("body").ry)<.0001f,"stationary torso kept swinging");
      require(Math.abs(rig.pose("arm_left").ry)<.0001f,"stationary arm kept swinging");
      require(Math.abs(rig.pose("skull").rz)<.0001f,"stationary head kept swaying");
      samples++;
    }
    // Exercise the small limb amount observed at an actual newborn's server
    // movement speed, rather than the much larger synthetic upright gait input.
    // A full short stride must show a lifted recovery and an opposite contact.
    for(int rigIndex=0;rigIndex<KEYS.length;rigIndex++) {
      UUID crawling=new UUID(200+rigIndex,17);
      float minReach=Float.POSITIVE_INFINITY,maxReach=Float.NEGATIVE_INFINITY;
      float maxLift=0;
      for(int frame=0;frame<=48;frame++) {
        float cycle=frame/48f*(float)(Math.PI*2);
        rig.animateGameplay(KEYS[rigIndex],crawling,OfficialChaoRig.Motion.CRAWL,
            120,cycle/7.5f,.022f,0,0,0,0,false);
        var left=rig.pose("arm_left");var right=rig.pose("arm_right");
        if(left!=null && right!=null) {
          float sin=(float)Math.sin(1.10),cos=(float)Math.cos(1.10);
          float reach=sin*left.y+cos*(left.z-.7f);
          float leftLift=-(cos*left.y-sin*(left.z-.7f));
          float rightLift=-(cos*right.y-sin*(right.z-.7f));
          minReach=Math.min(minReach,reach);maxReach=Math.max(maxReach,reach);
          maxLift=Math.max(maxLift,leftLift);
          // Bob correction adds at most 0.09 to both values. A planted dwell
          // must remain while the opposite hand makes its lifted recovery.
          require(Math.min(leftLift,rightLift)<.10f,"both crawling hands left support");
        }
        require(rig.finite(),"low-speed crawl non-finite");
        samples++;
      }
      if(Float.isFinite(minReach)) {
        require(maxReach-minReach>2f,"newborn hand stroke too small to see");
        require(maxLift>.8f,"newborn hand never lifts for recovery");
      }
      for(int frame=0;frame<=8;frame++) {
        rig.animateGameplay(KEYS[rigIndex],crawling,OfficialChaoRig.Motion.CRAWL,
            120,frame,0,0,0,0,0,false);
        var hand=rig.pose("arm_left");
        if(hand!=null)require(Math.abs(hand.y)<.0001f && Math.abs(hand.z-.7f)<.0001f,
            "stopped crawl kept paddling its hands");
        samples++;
      }
    }
    UUID changing=new UUID(91,43);
    rig.animateGameplay("baby",changing,OfficialChaoRig.Motion.CRAWL,0,0,0,0,0,0,0,false);
    float lowered=rig.pose("skull").y;
    rig.animateGameplay("baby",changing,OfficialChaoRig.Motion.WALK,1,0,0,0,0,0,0,false);
    require(Math.abs(rig.pose("skull").y-lowered)<.001f,"unlock transition snapped on first frame");
    rig.animateGameplay("baby",changing,OfficialChaoRig.Motion.WALK,9,0,0,0,0,0,0,false);
    require(Math.abs(rig.pose("skull").y)<.05f,"unlock transition did not settle");
    samples+=3;
    UUID interacting=new UUID(92,44);
    rig.animateGameplay("baby",interacting,OfficialChaoRig.Motion.WALK,100,0,0,0,0,0,0,false);
    float restingArm=rig.pose("arm_left").rx;
    rig.animateGameplay("baby",interacting,OfficialChaoRig.Motion.PONDER,101,0,0,0,0,0,0,false,false);
    rig.pose("arm_left").rx=-1.2f;
    rig.finishGameplayFrame(interacting,0,101);
    require(Math.abs(rig.pose("arm_left").rx-restingArm)<.0001f,"held-item addition bypassed entry blend");
    rig.animateGameplay("baby",interacting,OfficialChaoRig.Motion.PONDER,109,0,0,0,0,0,0,false,false);
    rig.pose("arm_left").rx=-1.2f;
    rig.finishGameplayFrame(interacting,0,109);
    require(Math.abs(rig.pose("arm_left").rx+1.2f)<.0001f,"held-item blend did not reach completed pose");
    float holdingArm=rig.pose("arm_left").rx;
    rig.animateGameplay("baby",interacting,OfficialChaoRig.Motion.WALK,110,0,0,0,0,0,0,false);
    require(Math.abs(rig.pose("arm_left").rx-holdingArm)<.0001f,"held-item exit snapped");
    rig.animateGameplay("baby",interacting,OfficialChaoRig.Motion.SIT,120,0,0,3,.4f,0,0,false);
    rig.animateGameplay("baby",interacting,OfficialChaoRig.Motion.SIT,128,0,0,3,.8f,0,0,false);
    float happyArm=rig.pose("arm_left").rx;
    rig.animateGameplay("baby",interacting,OfficialChaoRig.Motion.WALK,129,0,0,0,0,0,0,false);
    require(Math.abs(rig.pose("arm_left").rx-happyArm)<.0001f,"post-petting pose snapped");
    rig.animate("baby",120,0,0,3,.4f,0,0);
    float integerHappy=rig.pose("arm_left").ry;
    rig.animate("baby",120.5f,0,0,3,.425f,0,0);
    require(Math.abs(rig.pose("arm_left").ry-integerHappy)>.005f,"happy clip lost fractional render sampling");
    return samples+9;
  }
  /** New checks are additive; the published native/motion contract stays intact. */
  public static int inspectForms019() {
    String[] primary={"baby","swim","fly","run","power","hero","hero_swim","hero_fly","hero_run","hero_power","dark","dark_swim","dark_fly","dark_run","dark_power"};
    var signatures=new java.util.HashSet<Long>();int count=0;
    var rig=new OfficialChaoRig();var state=new com.chaogarden.core.ChaoState();state.adult=true;state.setEvolutionStrength(1.2f);
    var youngByKey=new java.util.HashMap<String,Long>();
    for(int key=0;key<primary.length;key++)for(int sf=-1;sf<=1;sf++)for(int rp=-1;rp<=1;rp++) {
      var id=new UUID(1900+key,100+sf*3+rp);
      rig.animateGameplay(primary[key],id,OfficialChaoRig.Motion.WALK,120,0,0,0,0,0,0,false);
      state.swimFly=sf;state.runPower=rp;rig.development(state);
      require(signatures.add(rig.shapeSignature()),"duplicate adult morphology "+primary[key]+" "+sf+"/"+rp);
      require(rig.finite(),"new morphology pose non-finite");count++;
      long endpoint=rig.shapeSignature();
      state.setEvolutionStrength(0);rig.development(state);long young=rig.shapeSignature();
      var previous=youngByKey.putIfAbsent(primary[key],young);
      require(previous==null||previous==young,"young base depends on adult secondary axes");count++;
      state.setEvolutionStrength(.6f);rig.development(state);long middle=rig.shapeSignature();
      require(middle!=young&&middle!=endpoint,"age midpoint did not interpolate young and fully aged geometry");count++;
      state.setEvolutionStrength(1.2f);rig.development(state);
      require(rig.shapeSignature()==endpoint,"age reversal did not restore exact endpoint");count++;
      state.swimFly=sf*3;state.runPower=rp*3;rig.development(state);
      require(rig.shapeSignature()==endpoint,"ordinary inputs beyond +/-1 must saturate without Monster");count++;
      state.swimFly=-sf;state.runPower=-rp;rig.development(state);
      state.swimFly=sf;state.runPower=rp;rig.development(state);
      require(rig.shapeSignature()==endpoint,"development failed to reverse exactly");count++;
    }
    require(signatures.size()==135,"adult morphology matrix incomplete");
    for(String key:primary)for(var behavior:com.chaogarden.entity.ChaoEntity.Behavior.values()) {
      var id=new UUID(1919,key.hashCode()+behavior.ordinal());
      for(int time=0;time<=60;time+=5) {
        rig.animateGameplay(key,id,ChaoBehaviorPoses.seated(behavior)?OfficialChaoRig.Motion.SIT:OfficialChaoRig.Motion.WALK,
            time,0,0,0,0,0,0,false,false);
        int face=ChaoBehaviorPoses.apply(rig,behavior,"trumpet",time/60f,time);
        require(face>=0&&face<=6,"expression contract");
        require(rig.finite(),key+" / "+behavior+" non-finite gesture");count++;
      }
    }
    for(String character:new String[]{"amy","omochao","tails","knuckles"}) {
      rig.animateGameplay(character,new UUID(1920,character.hashCode()),OfficialChaoRig.Motion.WALK,120,2,.1f,0,0,0,0,false);
      require(rig.finite(),character+" non-finite native fit");count++;
    }
    for(int type=0;type<5;type++)for(int stage=1;stage<=2;stage++) {
      var monster=new com.chaogarden.core.ChaoState();monster.adult=true;monster.primaryType=type;
      monster.refreshMonsterEvolution(true,false);
      int stat=type==0?com.chaogarden.core.ChaoState.STAMINA:type-1;monster.points[stat]=stage==1?2200:type==0?4000:3200;
      float amount=stage==1?1.5f:3;
      monster.setEvolutionStrength(1.2f);
      if(type>0)monster.influence(type-1,amount,true);
      rig.animateGameplay(primary[type],new UUID(1921,type*3+stage),OfficialChaoRig.Motion.WALK,120,0,0,0,0,0,0,false);
      rig.development(monster);
      require(monster.monsterStage()==stage,"monster fixture stage");
      require(rig.pose("skull").sx>1&&rig.finite(),"monster head amplification");
      require(type!=0||rig.pose("monster_crown").visible,"normal monster crown missing");
      require(monster.monsterAlpha()>0&&monster.monsterAlpha()<=1,"monster opacity bounds");count++;
    }
    return count;
  }

}
