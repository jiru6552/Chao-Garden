package com.chaogarden.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import org.joml.Vector3f;

/** Fit clothing to skin landmarks in joint-local pixels, before the sampled joint pose. */
final class ChaoAccessoryFit {
  record Fit(float x,float y,float z,float sx,float sy,float sz) {
    void apply(PoseStack stack) {
      stack.translate(x/16f,y/16f,z/16f);
      stack.scale(sx,sy,sz);
    }
    void apply(ChaoMeshAsset.Pose pose) {
      pose.x=x;pose.y=y;pose.z=z;pose.sx=sx;pose.sy=sy;pose.sz=sz;
    }
    /** Prepend to a root attachment whose authored pivot is zero, preserving child growth. */
    void prepend(ChaoMeshAsset.Pose pose) {
      pose.x=x+sx*pose.x;pose.y=y+sy*pose.y;pose.z=z+sz*pose.z;
      pose.sx*=sx;pose.sy*=sy;pose.sz*=sz;
    }
    float[] point(float[] p) {return new float[]{x+p[0]*sx,y+p[1]*sy,z+p[2]*sz};}
  }
  private static final Fit IDENTITY=new Fit(0,0,0,1,1,1);
  private static final float[] HEAD={-3.5f,-6,-3,3.5f,0,3};
  private static final float[] BODY={-2,-3.5f,-2,2,.5f,2};
  private static final float[] ARM_LEFT={0,0,-.5f,2,1,.5f};
  private static final float[] ARM_RIGHT={-2,0,-.5f,0,1,.5f};
  private static final float[] COVERED_HEAD={-3.75f,-6.30f,-3.30f,3.75f,.20f,3.30f};
  private static final float[] HOODED_HEAD={-3.86f,-6.38f,-3.53f,3.86f,.15f,3.38f};
  private static final float[] CLOTHED_BODY={-2.14f,-3.60f,-2.20f,2.14f,.60f,2.14f};
  private static final Map<String,float[]> REFERENCE_HEADS=new HashMap<>();
  private static final Map<String,float[]> REFERENCE_BODIES=new HashMap<>();
  static final float FACE_CLEARANCE=.18f,TEMPLE_CLEARANCE=.08f;

  /** Landmarks are measured on one coherent source assembly, in rendered local pixels.
   * Source triangles stay together; this transform never fits lens fragments separately. */
  record Source(String kind,float[] eyes,float frontBack,List<Temple> temples) {
    static Source read(JsonObject data) {
      if(!data.has("accessory_fit"))return null;
      var fit=data.getAsJsonObject("accessory_fit");
      if(fit.has("kind")&&!fit.get("kind").getAsString().equals("spectacles")) {
        String kind=fit.get("kind").getAsString();
        return new Source(kind,kind.equals("mouth")?values(fit.getAsJsonArray("mouth")):null,0,List.of());
      }
      if(!fit.has("eyes")||!fit.has("front_back"))return null;
      var temples=new ArrayList<Temple>();
      for(String side:List.of("left","right")) {
        var info=fit.getAsJsonObject(side);if(info==null)continue;
        String bone=info.has("bone")?info.get("bone").getAsString():"temple_"+side;
        String hinge=info.has("hinge")?info.get("hinge").getAsString():"hinge_"+side;
        String strip=info.get("strip").getAsString();float[] join=values(info.getAsJsonArray("join"));
        float[] inner=values(info.getAsJsonArray("inner"));
        var vertices=new ArrayList<Vector3f>();
        for(var element:data.getAsJsonArray("bones")) {
          var part=element.getAsJsonObject();if(!part.get("name").getAsString().equals(bone))continue;
          float[] pivot=values(part.getAsJsonArray("pivot"));
          for(int a=0;a<3;a++)if(Math.abs(pivot[a]-join[a])>.0001f)
            throw new IllegalArgumentException("Source temple must start at its measured rear join: "+bone);
          for(var meshElement:part.getAsJsonArray("meshes")) {
            var mesh=meshElement.getAsJsonObject();
            for(var vertex:mesh.getAsJsonArray("vertices")){float[] p=values(vertex.getAsJsonArray());vertices.add(new Vector3f(p[0],p[1],p[2]));}
          }
        }
        if(vertices.isEmpty())throw new IllegalArgumentException("Missing source temple geometry: "+bone);
        temples.add(new Temple(bone,hinge,strip,side.equals("left")?-1:1,inner,join,vertices));
      }
      return new Source("spectacles",values(fit.getAsJsonArray("eyes")),fit.get("front_back").getAsFloat(),temples);
    }
  }
  private static float[] values(JsonArray a){float[] v=new float[a.size()];for(int n=0;n<v.length;n++)v[n]=a.get(n).getAsFloat();return v;}

  /** A short source connector stays in front of the worn surface, then the
   * complete rear temple follows its side. Its positive affine transform keeps
   * both cut cross-sections joined, without widening or reflecting the head. */
  record Temple(String bone,String hinge,String strip,int side,float[] inner,float[] join,List<Vector3f> vertices) {
    void apply(float[] h,Fit fit,Map<String,ChaoMeshAsset.Pose> poses) {
      var arm=poses.get(bone);var root=poses.get(hinge);var connector=poses.get(strip);
      if(arm==null||root==null||connector==null)throw new IllegalArgumentException("Missing source spectacle arm: "+bone);
      float inside=side<0?Float.NEGATIVE_INFINITY:Float.POSITIVE_INFINITY;
      for(var p:vertices)inside=side<0?Math.max(inside,p.x):Math.min(inside,p.x);
      float targetX=((side<0?h[0]:h[3])-fit.x+side*TEMPLE_CLEARANCE)/fit.sx-inside;
      float targetZ=(h[2]-.04f-fit.z)/fit.sz;
      float length=join[2]-inner[2],shear=(targetX-inner[0])/length,depth=(targetZ-inner[2])/length;
      if(!(length>0&&depth>0))throw new IllegalArgumentException("Spectacle connector must remain in front of the face: "+bone);
      // [[1,shear],[0,depth]]=U*positiveScale*V. These are the
      // renderer's ordinary two-joint Y rotations and X/Z scales.
      double a=1+shear*shear,d=depth*depth,b=shear*depth;
      double u=.5*Math.atan2(2*b,a-d),c=Math.cos(u),s=Math.sin(u);
      double sigma=Math.sqrt((a+d+Math.sqrt((a-d)*(a-d)+4*b*b))*.5);
      root.ry=(float)-u;root.sx=(float)sigma;root.sz=(float)(depth/sigma);
      connector.ry=(float)Math.atan2(shear*c+depth*s,c);
      arm.x=targetX-join[0];arm.z=targetZ-join[2];
    }
  }
  private ChaoAccessoryFit() {}
  static boolean coversHead(String id){return id.equals("baseball_cap_accessory")||id.equals("press_hat_accessory");}
  static boolean hoodVisible(com.chaogarden.entity.ChaoEntity entity) {
    var chest=entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
    return chest.getItem() instanceof com.chaogarden.cwe.CweGiftItem garment
        &&garment.gift.id().endsWith("hoodie_accessory")&&entity.getChaoHat().isBlank()
        &&entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).isEmpty();
  }
  static boolean headCovered(com.chaogarden.entity.ChaoEntity entity) {
    var slot=entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET);
    return !entity.getChaoHat().isBlank()||hoodVisible(entity)
        ||slot.getItem() instanceof com.chaogarden.cwe.CweGiftItem cap&&coversHead(cap.gift.id())
        ||entity.getBehavior()==com.chaogarden.entity.ChaoEntity.Behavior.BIRTHDAY
          &&entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty()&&slot.isEmpty();
  }

  static Fit accessory(String id,String anchor,ChaoModel model) {
    return accessory(id,model.accessoryBounds("skull"),model.accessoryEyes(),
        model.accessoryBounds("body"),model.accessoryBounds(anchor));
  }

  static Fit accessory(String id,String anchor,ChaoModel model,Source source) {
    return accessory(id,anchor,model,source,model.accessoryBounds("skull"));
  }

  static Fit accessory(String id,String anchor,ChaoModel model,Source source,float[] headSurface) {
    return accessory(id,anchor,model,source,headSurface,model.accessoryBounds("body"));
  }

  static Fit accessory(String id,String anchor,ChaoModel model,Source source,float[] headSurface,float[] bodySurface) {
    float[] mouth=model.toyMouth();
    if(mouth!=null&&model.resolvedFace076()!=null&&!model.resolvedFace076().nativeExcluded()) {
      mouth=mouth.clone();mouth[1]+=ChaoExpressionLayer025.MOUTH_DROP076;
    }
    return accessory(id,id.equals("hoodie_hood")?model.accessoryBounds("skull"):headSurface,model.accessoryEyes(),
        id.endsWith("hoodie_accessory")||id.equals("hoodie_hood_folded")?model.accessoryBounds("body"):bodySurface,
        model.accessoryBounds(anchor),mouth,source);
  }

  static Fit accessory(String id,float[] actualHead,float[] actualEyes,float[] actualBody,float[] arm,float[] mouth,Source source) {
    float[] h=actualHead==null?HEAD:actualHead;
    float[] eyes=actualEyes==null?new float[]{(h[0]+h[3])*.5f,h[1]+(h[4]-h[1])*2/3,h[2]-.06f,(h[3]-h[0])*2/7}:actualEyes;
    if(source!=null) {
      if(source.kind.equals("head"))return between(h,HEAD);
      if(source.kind.equals("body"))return between(actualBody==null?BODY:actualBody,BODY);
      if(source.kind.equals("arm_left"))return between(arm==null?ARM_LEFT:arm,ARM_LEFT);
      if(source.kind.equals("arm_right"))return between(arm==null?ARM_RIGHT:arm,ARM_RIGHT);
      if(source.kind.equals("mouth")&&mouth!=null) {
        float scale=(h[3]-h[0])/7;
        return new Fit(mouth[0]-source.eyes[0]*scale,mouth[1]-source.eyes[1]*scale,
            Math.min(h[2],mouth[2])+.08f-source.eyes[2]*scale,scale,scale,scale);
      }
    }
    if(source!=null&&source.eyes!=null&&(id.contains("glasses")||id.contains("aviators"))) {
      float scale=eyes[3]/source.eyes[3];
      return new Fit(eyes[0]-source.eyes[0]*scale,eyes[1]-source.eyes[1]*scale,
          Math.min(h[2],eyes[2])-FACE_CLEARANCE-source.frontBack*scale,scale,scale,scale);
    }
    var fit=accessory(id,actualHead,actualEyes,actualBody,arm);
    if(id.contains("pacifier")&&mouth!=null)
      return new Fit(mouth[0],mouth[1]-1.9f,Math.min(h[2],mouth[2])-FACE_CLEARANCE+3.05f*fit.sz,fit.sx,fit.sy,fit.sz);
    return fit;
  }

  static Fit accessory(String id,float[] actualHead,float[] actualEyes,float[] actualBody,float[] arm) {
    float[] h=actualHead==null?HEAD:actualHead,b=actualBody==null?BODY:actualBody;
    float hw=h[3]-h[0],hh=h[4]-h[1],hd=h[5]-h[2];
    float hx=(h[0]+h[3])*.5f,hz=(h[2]+h[5])*.5f;
    float bw=b[3]-b[0],bh=b[4]-b[1],bd=b[5]-b[2];
    float bx=(b[0]+b[3])*.5f,bz=(b[2]+b[5])*.5f;
    float[] eyes=actualEyes==null?new float[]{hx,h[1]+hh*2/3,h[2]-.06f,hw*2/7}:actualEyes;
    if(id.contains("glasses")||id.contains("aviators")) {
      // The source forehead joint is at the bottom of the skull. Glasses align
      // to the visible irises, rather than to the joint's numerical origin.
      float sx=eyes[3]/1.55f,sy=1.25f,sz=hd/6;
      return new Fit(eyes[0]+.05f*sx,eyes[1]-.125f*sy,eyes[2]-.08f+3.03f*sz,sx,sy,sz);
    }
    if(id.contains("pacifier")) {
      float sx=hw/7,sz=hd/6;
      return new Fit(eyes[0],eyes[1]+1.5f-1.9f,eyes[2]-.10f+3.05f*sz,sx,1,sz);
    }
    if(id.contains("headphones")) {
      float sx=hw/7,sy=hh/6,sz=hd/6;
      return new Fit(hx,h[1]-.08f+3.1f*sy,hz,sx,sy,sz);
    }
    if(id.equals("eggshell_hat")) {
      float sx=hw/6.2f,sy=hh/6,sz=hd/5.6f;
      return new Fit(hx,h[1]+.35f+2.2f*sy,hz,sx,sy,sz);
    }
    if(id.contains("cap_")||id.contains("press_hat")) {
      float sx=(hw+.10f)/6.5f,sy=hh/6,sz=(hd+.10f)/5.9f;
      return new Fit(hx,h[1]+.25f+3.2f*sy,hz-.05f*sz,sx,sy,sz);
    }
    if(id.contains("bowtie")) {
      float sx=bw*1.1f/6,sy=bh/4;
      return new Fit(bx,b[1]+.55f-3.15f*sy,b[2]-.05f+2.05f,sx,sy,1);
    }
    if(id.equals("hoodie_hood")) {
      return between(new float[]{h[0]-.4f,h[1]-.4f,h[2]-.4f,h[3]+.4f,h[4]+.12f,h[5]+.4f},HEAD);
    }
    if(id.equals("hoodie_hood_folded"))return between(b,BODY);
    if(id.startsWith("hoodie_sleeve_")) {
      return between(arm==null?(id.endsWith("left")?ARM_LEFT:ARM_RIGHT):arm,
          id.endsWith("left")?ARM_LEFT:ARM_RIGHT);
    }
    if(id.endsWith("hoodie_accessory")) {
      float sx=(bw+.26f)/4,sy=(bh+.2f)/3.9f,sz=(bd+.26f)/3.2f;
      return new Fit(bx,b[1]-.10f+.7f*sy,bz,sx,sy,sz);
    }
    if(id.startsWith("medal_")) {
      float sx=bw/4,sy=bh/4;
      return new Fit(bx,b[1]+.6f*bh-1.125f*sy,b[2]-.25f+1.9f,sx,sy,1);
    }
    return IDENTITY;
  }

  static Fit legacyHead(float[] actual,String sourceKey) {
    if(actual==null)return IDENTITY;
    String key=sourceKey.startsWith("chao_")?sourceKey:"chao_"+sourceKey;
    float[] reference=REFERENCE_HEADS.computeIfAbsent(key,id->{
      var mesh=ChaoMeshAsset.load("official/"+id);
      var bounds=mesh.attachmentBounds("skull",mesh.createPoses());
      if(bounds==null)throw new IllegalStateException("Missing accessory reference skull "+id);
      return bounds;
    });
    return between(actual,reference);
  }

  static Fit canonicalHead(float[] actual){return actual==null?IDENTITY:between(actual,HEAD);}
  static float[] coveredHead(float[] actual){return headEnvelope(actual,COVERED_HEAD);}
  static float[] hoodedHead(float[] actual){return headEnvelope(actual,HOODED_HEAD);}
  static float[] hoodedHead(float[] actual,float[] authored){return headEnvelope(actual,authored);}
  static float[] clothedBody(float[] actual){
    return clothedBody(actual,CLOTHED_BODY);
  }
  static float[] clothedBody(float[] actual,float[] authored){
    var fit=actual==null?IDENTITY:between(actual,BODY);var min=fit.point(authored);var max=fit.point(new float[]{authored[3],authored[4],authored[5]});
    return new float[]{min[0],min[1],min[2],max[0],max[1],max[2]};
  }
  private static float[] headEnvelope(float[] actual,float[] envelope) {
    var fit=canonicalHead(actual);var min=fit.point(envelope);var max=fit.point(new float[]{envelope[3],envelope[4],envelope[5]});
    return new float[]{min[0],min[1],min[2],max[0],max[1],max[2]};
  }

  static Fit legacyBody(float[] actual,String sourceKey) {
    if(actual==null)return IDENTITY;
    String key=sourceKey.startsWith("chao_")?sourceKey:"chao_"+sourceKey;
    float[] reference=REFERENCE_BODIES.computeIfAbsent(key,id->{
      var mesh=ChaoMeshAsset.load("official/"+id);
      var bounds=mesh.attachmentBounds("body",mesh.createPoses());
      if(bounds==null)throw new IllegalStateException("Missing theme torso "+id);
      return bounds;
    });
    return between(actual,reference);
  }

  /** Keep the authored marker size and five-pixel crown gap as the skull grows. */
  static Fit legacyMarker(float[] actual,String sourceKey) {
    if(actual==null)return IDENTITY;
    legacyHead(actual,sourceKey);
    String key=sourceKey.startsWith("chao_")?sourceKey:"chao_"+sourceKey;
    float[] reference=REFERENCE_HEADS.get(key);
    return new Fit((actual[0]+actual[3]-reference[0]-reference[3])*.5f,
        actual[1]-reference[1],(actual[2]+actual[5]-reference[2]-reference[5])*.5f,1,1,1);
  }

  static Fit between(float[] actual,float[] reference) {
    float sx=(actual[3]-actual[0])/(reference[3]-reference[0]);
    float sy=(actual[4]-actual[1])/(reference[4]-reference[1]);
    float sz=(actual[5]-actual[2])/(reference[5]-reference[2]);
    return new Fit(actual[0]-reference[0]*sx,actual[1]-reference[1]*sy,
        actual[2]-reference[2]*sz,sx,sy,sz);
  }

  /** Eye spacing and skull width vary independently, so spectacles need hinges. */
  static void parts(String id,float[] actualHead,Fit fit,Map<String,ChaoMeshAsset.Pose> poses) {
    poses.values().forEach(ChaoMeshAsset.Pose::reset);
    if(!(id.contains("glasses")||id.contains("aviators")))return;
    float[] h=actualHead==null?HEAD:actualHead;
    float left=(h[0]-.10f-fit.x)/fit.sx,right=(h[3]+.10f-fit.x)/fit.sx;
    var l=poses.get("temple_left");if(l!=null)l.x=left+2.95f;
    var r=poses.get("temple_right");if(r!=null)r.x=right-2.85f;
    var lh=poses.get("hinge_left");if(lh!=null)lh.sx=(-2.45f-left)/.35f;
    var rh=poses.get("hinge_right");if(rh!=null)rh.sx=(right-2.35f)/.35f;
  }

  static void parts(String id,float[] actualHead,Fit fit,Map<String,ChaoMeshAsset.Pose> poses,Source source) {
    if(source==null){parts(id,actualHead,fit,poses);return;}
    poses.values().forEach(ChaoMeshAsset.Pose::reset);
    float[] h=actualHead==null?HEAD:actualHead;
    for(var temple:source.temples)temple.apply(h,fit,poses);
  }
}
