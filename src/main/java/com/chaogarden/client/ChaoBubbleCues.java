package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.*;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;

/** Consumes each server cue once at the actual world-rendered ring, never in UI previews. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class ChaoBubbleCues {
  private record Pending(ToyCueNetwork.Bubble cue,long received){}
  private record Anchor(ChaoEntity chao,Vec3 point,Vec3 direction){}
  private static ClientLevel level;
  private static boolean worldPass;
  private static final Map<UUID,Anchor> anchors=new HashMap<>();
  private static final LinkedHashMap<UUID,Pending> pending=new LinkedHashMap<>();
  private static final LinkedHashMap<UUID,ToyCueNetwork.Bubble> seen=new LinkedHashMap<>();
  static int emitted;static double maxEmissionError;
  private static void level(){
    var current=Minecraft.getInstance().level;
    if(level!=current){level=current;anchors.clear();pending.clear();seen.clear();worldPass=false;}
  }
  public static void receive(ToyCueNetwork.Bubble cue){
    level();if(level==null||!BubblePlayRules.puffDue(cue.elapsed()))return;
    var previous=seen.get(cue.chao());
    if(previous!=null&&(cue.until()<previous.until()||cue.until()==previous.until()&&cue.elapsed()<=previous.elapsed()))return;
    seen.put(cue.chao(),cue);pending.put(cue.chao(),new Pending(cue,level.getGameTime()));
    // Bound memory even on very long sessions with many unloaded Chao.
    while(seen.size()>1024)seen.remove(seen.keySet().iterator().next());
    while(pending.size()>256)pending.remove(pending.keySet().iterator().next());
  }
  static void ring(ChaoEntity chao,PoseStack stack){
    if(!worldPass||level!=chao.level())return;
    // LevelRenderer starts entity rendering with an identity PoseStack and
    // EntityRenderDispatcher translates it relative to the current camera.
    var p=stack.last().pose().transformPosition(new Vector3f(0,.33f,0));
    var n=stack.last().pose().transformDirection(new Vector3f(0,0,1)).normalize();
    var camera=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
    var direction=new Vec3(n.x,n.y,n.z);var forward=Vec3.directionFromRotation(0,chao.getYRot());
    if(direction.dot(forward)<0)direction=direction.scale(-1);
    anchors.put(chao.getUUID(),new Anchor(chao,camera.add(p.x,p.y,p.z),direction));
  }
  @SubscribeEvent public static void stage(RenderLevelStageEvent event){
    level();if(level==null)return;
    if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_SKY){worldPass=true;anchors.clear();}
    if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES)return;
    worldPass=false;
    var it=pending.entrySet().iterator();
    while(it.hasNext()){
      var item=it.next();var p=item.getValue();var a=anchors.get(item.getKey());
      if(level.getGameTime()-p.received()>3){it.remove();continue;}
      if(a==null)continue; // An off-screen cue expires; never bursts on re-entry.
      if(a.chao.toyPlayingUntil()!=p.cue.until()||!ChaoBubbleAnimation.active(a.chao)){it.remove();continue;}
      var velocity=a.direction.scale(.035).add(0,.012,0);
      level.addParticle(ChaoToyParticles.SOAP_BUBBLE.get(),a.point.x,a.point.y,a.point.z,velocity.x,velocity.y,velocity.z);
      emitted++;it.remove();
    }
  }
}
