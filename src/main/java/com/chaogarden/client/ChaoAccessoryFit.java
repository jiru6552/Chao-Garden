package com.chaogarden.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;

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
  private static final Map<String,float[]> REFERENCE_HEADS=new HashMap<>();
  private static final Map<String,float[]> REFERENCE_BODIES=new HashMap<>();
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
      return new Fit(hx,h[1]-.08f+3.4f*sy,hz,sx,sy,sz);
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
}
