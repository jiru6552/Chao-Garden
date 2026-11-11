package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.mojang.logging.LogUtils;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Excluded client-only reference-pose probe, run once during the release study. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class ReleaseVisualSmokeOfficialProbe {
  private static boolean done;
  private static final String[] KEYS={"baby","swim","fly","run","power","hero","hero_fly","hero_power","dark","dark_run","dark_swim","golden","baghead","dad","dragon","ender","tails","knuckles","emerald","creeper"};
  private ReleaseVisualSmokeOfficialProbe(){}
  @SubscribeEvent public static void tick(ClientTickEvent.Post event){
    if(done||!Boolean.getBoolean("chao_garden.audit022Study")||Minecraft.getInstance().level==null)return;
    done=true;int count=inspect();
    LogUtils.getLogger().info("CHAO_OFFICIAL_POSE_PROBE_COMPLETE: {} finite source-rig samples; 20 rigs; idle arm signs and loop endpoints verified",count);
  }
  static void near(float a,float b,String why){if(Math.abs(a-b)>.0001f)throw new IllegalStateException("Official pose contract: "+why);}
  static float[] values(ChaoMeshAsset.Pose p){return new float[]{p.x,p.y,p.z,p.rx,p.ry,p.rz,p.sx,p.sy,p.sz};}
  public static int inspect(){
    var rig=new OfficialChaoRig();int samples=0;
    for(String key:KEYS){
      for(int emotion=0;emotion<4;emotion++)for(int ticks=0;ticks<=120;ticks+=5){
        rig.animate(key,ticks,0,ticks%10==0?0:1,emotion,ticks/20f,0,0);
        rig.applySpecial(key.equals("ender"),key.equals("creeper")?.4f:0,ticks);
        if(key.equals("ender"))near(rig.pose("held_block").sx,1,"carried block restored after locomotion");
        if(!rig.finite())throw new IllegalStateException("Non-finite imported pose "+key);
        samples++;
      }
      rig.animate(key,0,0,0,0,0,0,0);
      if(rig.pose("arm_left")!=null){near(rig.pose("arm_left").rz,50*(float)Math.PI/180,"left idle arm lowers");near(rig.pose("arm_left").y,.3f,"left idle arm offset");}
      if(rig.pose("arm_right")!=null)near(rig.pose("arm_right").rz,-40*(float)Math.PI/180,"right idle arm lowers");
      var initial=new java.util.HashMap<String,float[]>();rig.poses().forEach((name,p)->initial.put(name,values(p)));
      rig.animate(key,27.5f,0,0,0,0,0,0);
      rig.poses().forEach((name,p)->{float[] before=initial.get(name),after=values(p);for(int i=0;i<before.length;i++)near(before[i],after[i],"idle loop endpoint "+name);});
    }
    return samples;
  }
}
