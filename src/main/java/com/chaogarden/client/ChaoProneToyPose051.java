package com.chaogarden.client;

import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Belly-supported floor play, shared by the real car and crayon performances. */
final class ChaoProneToyPose051 {
  static final float FLOOR_Y=1.501f;
  static final float BODY_ANGLE=1.48f,HEAD_ANGLE=.60f;
  private static final float BELLY_GAP=.008f,FOOT_GAP=.002f,HEAD_GAP=.003f;
  private static final float[] BODY={-2,-3.5f,-2,2,.5f,2};
  private static final float[] HEAD={-3.5f,-6,-3,3.5f,0,3};
  private record FootRig(ChaoMeshAsset mesh,Map<String,ChaoMeshAsset.Pose> poses){}
  private static final Map<String,FootRig> FEET=new HashMap<>();
  private ChaoProneToyPose051(){}

  static boolean supports(String kind){return kind.equals("chao_toy_car")||kind.equals("chao_crayons");}

  /** Use the geometry that is actually visible, including glass and animal soles. */
  static Function<String,float[]> runtimeBounds(ChaoEntity c,ChaoModel model){
    return name->{
      if(name.equals("hand_left"))return model.toyHandBounds(c.state(),true);
      if(name.equals("hand_right"))return model.toyHandBounds(c.state(),false);
      if(ChaoSittingPose.FEET.contains(name))return model.officialPoses().containsKey(name)?footBounds(c,model,name):null;
      if(name.equals("skull")&&VanillaHatRenderer050.supports(c.getChaoHat()))
        return VanillaHatRenderer050.fittedBounds(model.accessoryBounds(name));
      return model.accessoryBounds(name);
    };
  }
  private static float[] footBounds(ChaoEntity c,ChaoModel model,String name){
    int animal=c.state().animalParts[ChaoState.FEET];
    boolean replacement=c.state().allowsAnimalParts()&&animal>=0
        &&(ChaoState.animalSlots(animal)&(1<<ChaoState.FEET))!=0;
    boolean glass=c.state().immortal&&c.state().character.equals("normal");
    if(!replacement&&!glass)return model.accessoryBounds(name);
    String path=replacement?(animal<21?"overlays/":"cwe_parts/")+"chao_"+model.officialGeometryKey()
        :"gameplay/chao_"+model.gameplayFormKey()+"_glass";
    var rig=FEET.computeIfAbsent(path,key->{var mesh=ChaoMeshAsset.load(key);return new FootRig(mesh,mesh.createPoses());});
    rig.poses.forEach((key,p)->{
      p.reset();var from=model.officialPoses().get(key);if(from!=null)ChaoOverlayLayer.copy(from,p);
      if(key.startsWith("animal_")){
        p.visible=replacement&&key.startsWith("animal_"+ChaoState.FEET+"_"+animal+"_");
        if(p.visible&&!c.state().adult&&animal<21)p.sx=p.sy=p.sz=.92f;
      }
      // These empty semantic joints still render their replacement children
      // when the source skin foot is hidden. partBounds needs the joint enabled
      // to measure those actually visible children.
      if(ChaoSittingPose.FEET.contains(key))p.visible=true;
    });
    if(name.equals("feet_bone_right")){
      var alias=rig.poses.get("foot_right");var from=model.officialPoses().get(name);
      if(alias!=null&&from!=null)ChaoOverlayLayer.copy(from,alias);
    }
    return rig.mesh.partBounds(name.equals("feet_bone_right")?"foot_right":name,rig.poses);
  }

  static void apply(ChaoEntity c,ChaoModel model,float weight){
    apply(c,model,weight,HEAD_ANGLE);
  }
  static void apply(ChaoEntity c,ChaoModel model,float weight,float headAngle){
    apply(c,model,weight,headAngle,0);
  }
  static void apply(ChaoEntity c,ChaoModel model,float weight,float headAngle,float headLiftPixels){
    apply(model.officialPoses(),model::transformAccessoryAnchor,runtimeBounds(c,model),weight,headAngle,headLiftPixels);
  }

  /** Pure production path used by source-rig checks; all distances are model units. */
  static void apply(Map<String,ChaoMeshAsset.Pose> poses,BiConsumer<String,PoseStack> anchor,
                    Function<String,float[]> bounds,float weight){
    apply(poses,anchor,bounds,weight,HEAD_ANGLE);
  }
  static void apply(Map<String,ChaoMeshAsset.Pose> poses,BiConsumer<String,PoseStack> anchor,
                    Function<String,float[]> bounds,float weight,float headAngle){
    apply(poses,anchor,bounds,weight,headAngle,0);
  }
  static void apply(Map<String,ChaoMeshAsset.Pose> poses,BiConsumer<String,PoseStack> anchor,
                    Function<String,float[]> bounds,float weight,float headAngle,float headLiftPixels){
    float w=Math.clamp(weight,0,1);if(w<=0)return;
    var body=poses.get("body");var head=poses.get("skull");if(body==null||head==null)return;
    var torso=bounds.apply("body");if(torso==null)torso=BODY;
    var skull=bounds.apply("skull");if(skull==null)skull=HEAD;
    // Most imported skulls are root siblings, while Tails and Knuckles inherit
    // the torso. Probe the actual live hierarchy instead of guessing from age
    // or appearance names; both pure rig and runtime paths use these anchors.
    float oldBodyRx=body.rx;
    var before=matrix(anchor,"skull");body.rx+=.03125f;
    boolean bodyHead=!before.equals(matrix(anchor,"skull"),.000001f);
    body.rx=oldBodyRx;
    String headParent=bodyHead?"body":"root";
    float oldHeadAngle=head.rx+(bodyHead?oldBodyRx:0);
    body.rx=lerp(body.rx,BODY_ANGLE,w);body.ry*=1-w;body.rz*=1-w;
    var low=worldBounds(anchor,torso,"body");
    shift(poses,anchor,"body",new Vector3f(0,(FLOOR_Y-BELLY_GAP-low[4])*w,0));

    // Reconnect the neck after leaning the belly. Most source skulls are torso
    // siblings; the actual-parent correction also handles character donors.
    var torsoAt=matrix(anchor,"body");
    var neck=torsoAt.transformPosition(new Vector3f((torso[0]+torso[3])/32,torso[1]/16,0));
    neck.y-=.047f;neck.z-=.018f;
    var current=matrix(anchor,"skull").transformPosition(new Vector3f());
    shift(poses,anchor,"skull",headParent,neck.sub(current).mul(w));
    head.rx=lerp(oldHeadAngle,headAngle,w)-(bodyHead?body.rx:0);head.ry*=1-w;head.rz*=1-w;

    // A large evolved rear crown must clear the back without moving the belly
    // off the floor. The minimal lift is found against both oriented volumes.
    if(w>.001f){
      float gap=separation(anchor,torso,skull);
      if(gap<HEAD_GAP*w){
        float originalX=head.x,originalY=head.y,originalZ=head.z,lo=0,hi=.25f;
        shift(poses,anchor,"skull",headParent,new Vector3f(0,-hi,0));
        if(separation(anchor,torso,skull)>=HEAD_GAP*w){
          for(int i=0;i<12;i++){
            float mid=(lo+hi)*.5f;head.x=originalX;head.y=originalY;head.z=originalZ;
            shift(poses,anchor,"skull",headParent,new Vector3f(0,-mid,0));
            if(separation(anchor,torso,skull)>=HEAD_GAP*w)hi=mid;else lo=mid;
          }
        }
        head.x=originalX;head.y=originalY;head.z=originalZ;shift(poses,anchor,"skull",headParent,new Vector3f(0,-hi,0));
      }
    }
    var headBox=worldBounds(anchor,skull,"skull");
    if(headBox[4]>FLOOR_Y-HEAD_GAP)
      shift(poses,anchor,"skull",headParent,new Vector3f(0,FLOOR_Y-HEAD_GAP-headBox[4],0));
    // Per-activity clearance is additional to the minimum neck/back fit, so a
    // lifted chin cannot be absorbed by reducing the collision solver's lift.
    if(headLiftPixels>0)shift(poses,anchor,"skull",headParent,new Vector3f(0,-headLiftPixels*w/16,0));

    for(String name:ChaoSittingPose.FEET){
      var foot=poses.get(name);var sole=bounds.apply(name);if(foot==null||sole==null)continue;
      boolean left=name.contains("left");
      // Parent Rx(a) · Ry(pi) · Rx(a) leaves the visible foot level and turns
      // its toes rearward. They do not hang through the floor as the torso tilts.
      foot.rx=lerp(foot.rx,BODY_ANGLE,w);foot.ry=lerp(foot.ry,(float)Math.PI,w);foot.rz*=1-w;
      float spread=Math.max(1.4f,(left?sole[3]:-sole[0])-.60f);
      foot.x+=(left?spread:-spread)*w;
      var box=worldBounds(anchor,sole,name);
      float move=(FLOOR_Y-FOOT_GAP-box[4])*w;
      // Hard lower bound is continuous at contact during entry/exit.
      move=Math.min(move,FLOOR_Y-FOOT_GAP-box[4]);
      shift(poses,anchor,name,new Vector3f(0,move,0));
    }
  }

  private static float lerp(float a,float b,float w){return a+(b-a)*w;}
  private static Matrix4f matrix(BiConsumer<String,PoseStack> anchor,String bone){
    var at=new PoseStack();anchor.accept(bone,at);return new Matrix4f(at.last().pose());
  }
  private static void shift(Map<String,ChaoMeshAsset.Pose> poses,BiConsumer<String,PoseStack> anchor,
                            String bone,Vector3f delta){
    String parent=bone.equals("body")||bone.equals("skull")?"root":"body";
    shift(poses,anchor,bone,parent,delta);
  }
  private static void shift(Map<String,ChaoMeshAsset.Pose> poses,BiConsumer<String,PoseStack> anchor,
                            String bone,String parent,Vector3f delta){
    var local=matrix(anchor,parent).invert().transformDirection(delta).mul(16);
    var p=poses.get(bone);p.x+=local.x;p.y+=local.y;p.z+=local.z;
  }
  static boolean clearExitFloor(ChaoEntity c,ChaoModel model){
    if(c.isInWater()||c.isCompanionAttached()||c.isChaoGliding()||c.isPassenger()||!floorSupported(c))return false;
    return clearExitFloor(model.officialPoses(),model::transformAccessoryAnchor,runtimeBounds(c,model));
  }
  /** Client movement packets can briefly report onGround=false for a stationary
   * grounded actor. Require real block support immediately below its feet rather
   * than that transient flag; actual flight and unsupported ledges stay exempt. */
  static boolean floorSupported(ChaoEntity c){
    var box=c.getBoundingBox();
    var support=new net.minecraft.world.phys.AABB(box.minX+.01,box.minY-.03,box.minZ+.01,
        box.maxX-.01,box.minY+.001,box.maxZ-.01);
    for(var shape:c.level().getBlockCollisions(c,support))for(var part:shape.toAabbs()){
      // Examine the actual top face, not merely intersection with a wall or
      // an embedded block. Individual boxes retain lower stair/slab support.
      if(part.maxY>=support.minY&&part.maxY<=support.maxY
          &&part.maxX>support.minX&&part.minX<support.maxX
          &&part.maxZ>support.minZ&&part.minZ<support.maxZ)return true;
    }
    return false;
  }
  /** During the existing outgoing blend, keep every joint connected and lift
   * only enough for its lowest real surface to clear the actor's floor plane. */
  static boolean clearExitFloor(Map<String,ChaoMeshAsset.Pose> poses,
                                BiConsumer<String,PoseStack> anchor,Function<String,float[]> bounds){
    float bottom=Float.NEGATIVE_INFINITY;
    for(String bone:java.util.List.of("body","skull","foot_left","foot_right","feet_bone_right",
                                     "foot_left2","foot_right2","hand_left","hand_right")){
      if(!bone.startsWith("hand_")&&!poses.containsKey(bone))continue;
      var box=worldBounds(anchor,bounds,bone);if(box!=null)bottom=Math.max(bottom,box[4]);
    }
    if(bottom<=FLOOR_Y-.001f)return false;
    var root=poses.get("root");if(root==null)return false;
    root.y+=(FLOOR_Y-.001f-bottom)*16;return true;
  }
  static float[] worldBounds(BiConsumer<String,PoseStack> anchor,Function<String,float[]> bounds,String bone){
    var b=bounds.apply(bone);return b==null?null:worldBounds(anchor,b,bone);
  }
  static float[] worldBounds(BiConsumer<String,PoseStack> anchor,float[] bounds,String bone){
    String joint=bone.equals("hand_left")?"arm_left":bone.equals("hand_right")?"arm_right":bone;
    var corners=corners(matrix(anchor,joint),bounds);
    var low=new Vector3f(Float.POSITIVE_INFINITY);var high=new Vector3f(Float.NEGATIVE_INFINITY);
    for(var p:corners){low.min(p);high.max(p);}return new float[]{low.x,low.y,low.z,high.x,high.y,high.z};
  }
  private static Vector3f[] corners(Matrix4f matrix,float[] b){
    var result=new Vector3f[8];
    for(int i=0;i<8;i++)result[i]=matrix.transformPosition(new Vector3f(b[(i&1)==0?0:3],b[(i&2)==0?1:4],b[(i&4)==0?2:5]).div(16));
    return result;
  }
  /** Signed oriented-bounds separation in model units; positive means no overlap. */
  static float separation(BiConsumer<String,PoseStack> anchor,Function<String,float[]> bounds){
    var body=bounds.apply("body");var head=bounds.apply("skull");
    return separation(anchor,body==null?BODY:body,head==null?HEAD:head);
  }
  private static float separation(BiConsumer<String,PoseStack> anchor,float[] body,float[] head){
    var a=corners(matrix(anchor,"body"),body);var b=corners(matrix(anchor,"skull"),head);
    var ax=axes(a);var bx=axes(b);float gap=Float.NEGATIVE_INFINITY;
    for(var v:ax)gap=Math.max(gap,gap(a,b,v));for(var v:bx)gap=Math.max(gap,gap(a,b,v));
    for(var x:ax)for(var y:bx)gap=Math.max(gap,gap(a,b,new Vector3f(x).cross(y)));
    return gap;
  }
  private static Vector3f[] axes(Vector3f[] p){return new Vector3f[]{new Vector3f(p[1]).sub(p[0]),new Vector3f(p[2]).sub(p[0]),new Vector3f(p[4]).sub(p[0])};}
  private static float gap(Vector3f[] a,Vector3f[] b,Vector3f axis){
    if(axis.lengthSquared()<1e-12f)return Float.NEGATIVE_INFINITY;axis=new Vector3f(axis).normalize();
    float al=Float.POSITIVE_INFINITY,ah=Float.NEGATIVE_INFINITY,bl=al,bh=ah;
    for(var p:a){float t=p.dot(axis);al=Math.min(al,t);ah=Math.max(ah,t);}
    for(var p:b){float t=p.dot(axis);bl=Math.min(bl,t);bh=Math.max(bh,t);}
    return Math.max(bl-ah,al-bh);
  }
}
