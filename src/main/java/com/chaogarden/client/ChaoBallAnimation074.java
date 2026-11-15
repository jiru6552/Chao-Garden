package com.chaogarden.client;

import com.chaogarden.core.ChaoState;
import com.chaogarden.core.LocomotionRules;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.BallPlayRules074;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

/** Rigid close-contact gestures on the ordinary rig; no head or limb scaling.
 * Crawlers gently nudge with their low face while their hands remain supports.
 * Walkers tip one toe into the ball. Both are authored Minecraft adaptations. */
final class ChaoBallAnimation074 {
  private static float[][] geometry;
  private static final float EPSILON=.00002f;
  private record ToeFit(long shape,int distance,float[][] boxes,int[] corners,float angle) {}
  private static ToeFit lastToe;
  private static final java.util.Map<ChaoEntity,Float> RECENT=new java.util.WeakHashMap<>();
  static void resetGeometry(){geometry=null;lastToe=null;RECENT.clear();}
  private ChaoBallAnimation074() {}

  static boolean active(ChaoEntity c) {
    return c.getType()==com.chaogarden.ChaoGarden.CHAO.get()&&BallPlayRules074.KIND.equals(c.toyPlayingKind())&&c.toyPlayingTicks()>0
        &&c.isAlive()&&c.hurtTime<=0&&!c.isCocooned()&&!c.isCompanionGripping()
        &&!com.chaogarden.companion.CompanionContent.isManaged(c)
        &&!c.officialComparison()&&!c.isResting()&&!c.isEating()&&!c.isBeingPetted()
        &&!c.isHandlingItem()&&!c.isPassenger()&&c.getHeldChaoItem().isEmpty()
        &&c.getMainHandItem().isEmpty()&&c.getBehavior()==ChaoEntity.Behavior.NONE;
  }
  static boolean crawler(ChaoState state){return LocomotionRules.groundGait(state)==LocomotionRules.GroundGait.CRAWL;}
  static boolean nudge(ChaoState state){return BallPlayRules074.nudge(state);}
  static float scale(ChaoState state){return state.immortal?.86f:state.adult?.80f:.64f;}
  static float pressure(float t) {
    if(!Float.isFinite(t)||t<=0||t>=BallPlayRules074.ACTION_TICKS)return 0;
    float f=t<=BallPlayRules074.CONTACT_TICK?t/BallPlayRules074.CONTACT_TICK:
        (BallPlayRules074.ACTION_TICKS-t)/(BallPlayRules074.ACTION_TICKS-BallPlayRules074.CONTACT_TICK);
    f=Math.clamp(f,0,1);return f*f*(3-2*f);
  }
  static boolean happy(ChaoEntity c,float partial){float t=c.toyKickElapsed()+partial;return active(c)&&c.toyKickElapsed()>=0&&t>=10&&t<14;}
  /** Padding of the actual fitted pumpkin/skull and the lower eggshell face. */
  static float hatPadding(String hat){return (float)BallPlayRules074.headPaddingPixels(hat);}
  static double contactDistance(ChaoState state,String hat){return BallPlayRules074.contactDistance(state,hat);}
  static float gazeYaw(boolean active,ChaoState state,float yaw){return active?0:state.immortal?Math.clamp(yaw,-12,12):yaw;}
  static float gazePitch(boolean active,float pitch){return active?0:pitch;}
  static boolean pose(ChaoEntity c,OfficialChaoRig rig,float partial,float age){
    if(active(c)){
      RECENT.put(c,age);pose(rig,c.state(),c.getChaoHat(),c.toyKickElapsed()<0?-1:c.toyKickElapsed()+partial,boxes());return true;
    }
    var last=RECENT.get(c);
    if(last!=null){
      if(age>=last&&age-last<8&&!c.isPassenger()&&!c.isCompanionGripping()&&!com.chaogarden.companion.CompanionContent.isManaged(c)&&!c.isInWater()){
        clearFootFloor(rig,c.state());return true;
      }
      RECENT.remove(c);
    }
    return false;
  }
  /** Only lift a penetrating sole; raised stride feet retain their normal lift. */
  static void clearFootFloor(OfficialChaoRig rig,ChaoState state){
    for(String foot:ChaoSittingPose.FEET)if(rig.pose(foot)!=null){
      float lowest=Float.POSITIVE_INFINITY;for(var p:part(rig,foot,scale(state)))lowest=Math.min(lowest,p.y);
      if(lowest<0){
        if(rig.sampledMesh().partBounds(foot,rig.poses())!=null)ChaoSittingPose.ground(rig.sampledMesh(),rig.poses(),foot);
        else {
          // Same rigid sole correction as ChaoSittingPose.ground; that helper
          // intentionally ignores a hidden skin mesh, so use its base envelope.
          var body=new PoseStack();rig.transformBone("body",body);
          float vertical=body.last().pose().transformDirection(new Vector3f(0,1,0)).y;
          if(Math.abs(vertical)>.2f)rig.pose(foot).y+=(lowest*16/scale(state)-.025f)/vertical;
        }
      }
    }
  }
  record Fit(boolean solved,float rigidAmount,float remainingSeparation){}

  /** Called after the ordinary incoming pose blend, as with the existing fitted toy grips. */
  static Fit pose(OfficialChaoRig rig,ChaoState state,String hat,float phase,float[][] boxes) {
    clearFootFloor(rig,state);float w=pressure(phase);if(w<=0)return new Fit(true,0,Float.POSITIVE_INFINITY);
    float size=scale(state),distance=(float)contactDistance(state,hat);
    if(nudge(state)) {
      var skull=rig.pose("skull");if(skull==null)return new Fit(false,0,Float.POSITIVE_INFINITY);
      var points=head(rig,state,hat);
      float shift=firstTranslation(points,boxes,distance);
      float maximum=size/16; // At most one rigid model pixel; never stretch the neck or replace its box.
      boolean solved=Float.isFinite(shift)&&shift>=-EPSILON&&shift<=maximum;
      float apply=Float.isFinite(shift)?Math.clamp(shift-EPSILON,0,maximum):0;
      skull.z-=apply/size*16*w;
      return new Fit(solved,apply/size*16,contactSeparation(head(rig,state,hat),boxes,distance));
    }
    String foot=rig.pose("foot_right")!=null?"foot_right":"feet_bone_right";
    var ankle=rig.pose(foot);if(ankle==null)return new Fit(false,0,Float.POSITIVE_INFINITY);
    float original=ankle.rx;
    if(intersects(part(rig,foot,size),boxes,distance))return new Fit(false,0,contactSeparation(part(rig,foot,size),boxes,distance));
    var before=part(rig,foot,size);int[] corners=new int[before.length*3];
    for(int i=0;i<before.length;i++){corners[3*i]=Float.floatToIntBits(before[i].x);corners[3*i+1]=Float.floatToIntBits(before[i].y);corners[3*i+2]=Float.floatToIntBits(before[i].z);}
    long shape=rig.shapeSignature();int distanceBits=Float.floatToIntBits(distance);
    if(lastToe!=null&&lastToe.shape==shape&&lastToe.distance==distanceBits&&lastToe.boxes==boxes&&java.util.Arrays.equals(lastToe.corners,corners)){
      ankle.rx=original-lastToe.angle*w;return new Fit(true,lastToe.angle,contactSeparation(part(rig,foot,size),boxes,distance));
    }
    float lower=0,upper=Float.NaN;
    for(int i=1;i<=16;i++) {
      float angle=i*.8f/16;ankle.rx=original-angle;
      if(intersects(part(rig,foot,size),boxes,distance)){upper=angle;break;}lower=angle;
    }
    if(!Float.isFinite(upper)){ankle.rx=original;return new Fit(false,0,Float.POSITIVE_INFINITY);}
    for(int i=0;i<18;i++){float mid=(lower+upper)*.5f;ankle.rx=original-mid;if(intersects(part(rig,foot,size),boxes,distance))upper=mid;else lower=mid;}
    lastToe=new ToeFit(shape,distanceBits,boxes,corners,lower);
    ankle.rx=original-lower*w;
    return new Fit(true,lower,contactSeparation(part(rig,foot,size),boxes,distance));
  }

  /** Actual joint transforms converted into actor-local world blocks: X, up-Y, forward-Z. */
  static Vector3f[] part(OfficialChaoRig rig,String bone,float size) {
    var bounds=rig.sampledMesh().partBounds(bone,rig.poses());
    // Animal feet hide the skin mesh but still inherit this same ankle.
    // Preserve its rigid gesture using the unhidden base attachment envelope;
    // this does not claim exact contact with every replacement animal mesh.
    if(bounds==null&&ChaoSittingPose.FEET.contains(bone))bounds=rig.attachmentBounds(bone);
    if(bounds==null)return new Vector3f[0];return transformed(rig,bone,bounds,size);
  }
  static Vector3f[] head(OfficialChaoRig rig,ChaoState state,String hat) {
    float[] h=rig.attachmentBounds("skull");
    if(h==null)return new Vector3f[0];
    if(hat.equals("eggshell_hat")) {
      float cx=(h[0]+h[3])*.5f,cz=(h[2]+h[5])*.5f;
      float rx=(h[3]-h[0])*.6f+.55f,rz=(h[5]-h[2])*.6f+.55f;
      return transformed(rig,"skull",new float[]{cx-rx,h[1]-1.45f,cz-rz,cx+rx,h[4]+.45f,cz+rz},scale(state));
    }
    if(hatPadding(hat)>0)return transformed(rig,"skull",VanillaHatRenderer050.fittedBounds(h),scale(state));
    if(rig.pose("skull__cube_0")!=null)return part(rig,"skull__cube_0",scale(state));
    return transformed(rig,"skull",h,scale(state));
  }
  private static Vector3f[] transformed(OfficialChaoRig rig,String bone,float[] bounds,float size) {
    var frame=new PoseStack();rig.transformBone(bone,frame);var result=new Vector3f[8];
    for(int n=0;n<8;n++) {
      var v=frame.last().pose().transformPosition(new Vector3f(bounds[(n&1)==0?0:3],bounds[(n&2)==0?1:4],bounds[(n&4)==0?2:5]).div(16));
      result[n]=new Vector3f(v.x*size,(1.5f-v.y)*size,-v.z*size);
    }
    return result;
  }
  private static List<Vector3f> axes(Vector3f[] part) {
    var list=new ArrayList<Vector3f>();if(part.length!=8)return list;
    var edges=new Vector3f[]{new Vector3f(part[1]).sub(part[0]),new Vector3f(part[2]).sub(part[0]),new Vector3f(part[4]).sub(part[0])};
    list.add(new Vector3f(1,0,0));list.add(new Vector3f(0,1,0));list.add(new Vector3f(0,0,1));
    for(int i=0;i<3;i++)list.add(new Vector3f(edges[i]).cross(edges[(i+1)%3]));
    for(var edge:edges)for(int a=0;a<3;a++)list.add(new Vector3f(edge).cross(new Vector3f(a==0?1:0,a==1?1:0,a==2?1:0)));
    list.removeIf(a->a.lengthSquared()<1e-12f);for(var axis:list)axis.normalize();
    for(int i=list.size()-1;i>=0;i--)for(int j=0;j<i;j++)if(Math.abs(list.get(i).dot(list.get(j)))>.9999999f){list.remove(i);break;}return list;
  }
  private static float[] interval(Vector3f[] points,Vector3f axis) {
    float lo=Float.POSITIVE_INFINITY,hi=Float.NEGATIVE_INFINITY;
    for(var p:points){float v=p.dot(axis);lo=Math.min(lo,v);hi=Math.max(hi,v);}return new float[]{lo,hi};
  }
  private static float[] interval(float[] b,float distance,Vector3f a) {
    float cx=(b[0]+b[3])*.5f,cy=(b[1]+b[4])*.5f,cz=distance+(b[2]+b[5])*.5f;
    float at=cx*a.x+cy*a.y+cz*a.z,extent=(b[3]-b[0])*.5f*Math.abs(a.x)+(b[4]-b[1])*.5f*Math.abs(a.y)+(b[5]-b[2])*.5f*Math.abs(a.z);
    return new float[]{at-extent,at+extent};
  }
  /** Separating-axis distance. A zero first-contact value proves convex surface contact. */
  static float contactSeparation(Vector3f[] part,float[][] boxes,float distance) {
    if(part.length!=8)return Float.POSITIVE_INFINITY;var axes=axes(part);float result=Float.POSITIVE_INFINITY;
    for(var box:boxes){float separation=Float.NEGATIVE_INFINITY;for(var axis:axes){var p=interval(part,axis);var b=interval(box,distance,axis);separation=Math.max(separation,Math.max(b[0]-p[1],p[0]-b[1]));}result=Math.min(result,separation);}return result;
  }
  private static boolean intersects(Vector3f[] part,float[][] boxes,float distance){
    if(part.length!=8)return false;float[] b={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
    for(var p:part){b[0]=Math.min(b[0],p.x);b[1]=Math.min(b[1],p.y);b[2]=Math.min(b[2],p.z);b[3]=Math.max(b[3],p.x);b[4]=Math.max(b[4],p.y);b[5]=Math.max(b[5],p.z);}var axes=axes(part);
    for(var box:boxes){if(b[0]>box[3]||b[3]<box[0]||b[1]>box[4]||b[4]<box[1]||b[2]>distance+box[5]||b[5]<distance+box[2])continue;boolean overlap=true;
      for(var a:axes){var p=interval(part,a);var q=interval(box,distance,a);if(p[1]<q[0]||p[0]>q[1]){overlap=false;break;}}if(overlap)return true;
    }return false;
  }
  /** Exact swept convex SAT along forward-Z; the first nonnegative contact with any voxel. */
  private static float firstTranslation(Vector3f[] part,float[][] boxes,float distance) {
    if(part.length!=8)return Float.POSITIVE_INFINITY;var axes=axes(part);float earliest=Float.POSITIVE_INFINITY;
    for(var box:boxes){float enter=Float.NEGATIVE_INFINITY,leave=Float.POSITIVE_INFINITY;boolean possible=true;
      for(var axis:axes){var p=interval(part,axis);var b=interval(box,distance,axis);float speed=axis.z;
        if(Math.abs(speed)<1e-7f){if(p[1]<b[0]||p[0]>b[1]){possible=false;break;}continue;}
        float a=(b[0]-p[1])/speed,c=(b[1]-p[0])/speed;enter=Math.max(enter,Math.min(a,c));leave=Math.min(leave,Math.max(a,c));
        if(enter>leave){possible=false;break;}}
      if(possible&&leave>=Math.max(0,enter))earliest=Math.min(earliest,enter);
    }return earliest;
  }
  static float[][] parseGeometry(JsonObject model) {
    var result=new ArrayList<float[]>();
    for(var item:model.getAsJsonArray("elements")){var e=item.getAsJsonObject();var from=e.getAsJsonArray("from");var to=e.getAsJsonArray("to");result.add(new float[]{(from.get(0).getAsFloat()-8)/16,from.get(1).getAsFloat()/16,(from.get(2).getAsFloat()-8)/16,(to.get(0).getAsFloat()-8)/16,to.get(1).getAsFloat()/16,(to.get(2).getAsFloat()-8)/16});}
    return result.toArray(float[][]::new);
  }
  static float[][] boxes() {
    if(geometry!=null)return geometry;
    var id=ResourceLocation.fromNamespaceAndPath("chao_garden","models/block/chao_ball_idle.json");
    try {
      var mc=Minecraft.getInstance();
      if(mc!=null){try(var reader=mc.getResourceManager().openAsReader(id)){return geometry=parseGeometry(JsonParser.parseReader(reader).getAsJsonObject());}}
      try(var input=ChaoBallAnimation074.class.getResourceAsStream("/assets/chao_garden/models/block/chao_ball_idle.json")){
        if(input==null)throw new IllegalStateException("Missing ball geometry");
        try(var reader=new InputStreamReader(input,StandardCharsets.UTF_8)){return geometry=parseGeometry(JsonParser.parseReader(reader).getAsJsonObject());}
      }
    }catch(Exception e){throw new IllegalStateException("Cannot read ball contact geometry",e);}
  }
}
