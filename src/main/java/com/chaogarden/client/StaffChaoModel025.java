package com.chaogarden.client;

import com.chaogarden.kindergarten.KindergartenContent;
import com.chaogarden.kindergarten.KindergartenNpc;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Fixed adult professions on the imported neutral skeleton, with one authored eye pair. */
public final class StaffChaoModel025 extends HierarchicalModel<KindergartenNpc> {
  public static final ModelLayerLocation LAYER=new ModelLayerLocation(
      ResourceLocation.fromNamespaceAndPath("chao_garden","kindergarten_staff025"),"main");
  private record Frame(ChaoMeshAsset mesh,Map<String,ChaoMeshAsset.Pose> poses){}
  private final ModelPart root;
  private final OfficialChaoRig sourceMotion=new OfficialChaoRig();
  private final Map<KindergartenContent.Role,Frame> frames=new EnumMap<>(KindergartenContent.Role.class);
  private Frame current;
  private KindergartenContent.Role currentRole;
  private UUID currentActor,renderedFortuneActor;
  private float currentAge,renderedFortuneAge,renderedFortuneLeft,renderedFortuneRight;
  private boolean renderedFortunePreserved;
  public record FortuneEyelids(UUID entityUuid,float renderAge,float leftScale,float rightScale,boolean preserved){}

  public StaffChaoModel025(ModelPart root){this.root=root;}
  public static LayerDefinition createLayer(){return LayerDefinition.create(new MeshDefinition(),256,128);}
  @Override public ModelPart root(){return root;}
  private Frame frame(KindergartenContent.Role role){
    return frames.computeIfAbsent(role,key->{
      String id=switch(key){case DOCTOR->"doctor";case PRINCIPAL->"principal";case FORTUNE->"fortune";default->"teacher";};
      var mesh=ChaoMeshAsset.load("staff025/"+id);return new Frame(mesh,mesh.createPoses());
    });
  }
  private static void copy(ChaoMeshAsset.Pose source,ChaoMeshAsset.Pose target){
    target.x=source.x;target.y=source.y;target.z=source.z;
    target.rx=source.rx;target.ry=source.ry;target.rz=source.rz;
    target.sx=source.sx;target.sy=source.sy;target.sz=source.sz;
    target.visible=source.visible;target.proportionY=source.proportionY;
  }
  @Override public void setupAnim(KindergartenNpc staff,float swing,float amount,float age,float yaw,float pitch){
    var role=staff.role();currentRole=role;currentActor=staff.getUUID();currentAge=age;
    current=frame(role);current.poses.values().forEach(ChaoMeshAsset.Pose::reset);
    // The original neutral geometry is named "baby" by its author, but also
    // supplies normal adults. Staff always use an adult upright presentation;
    // zero ability points never select the newborn crawling locomotion path.
    var posture=role==KindergartenContent.Role.TEACHER||role==KindergartenContent.Role.BOARD
        ?OfficialChaoRig.Motion.WALK:OfficialChaoRig.Motion.SIT;
    sourceMotion.animateGameplay("baby",staff.getUUID(),posture,
        age,0,0,0,0,Mth.clamp(yaw,-35,35),Mth.clamp(pitch,-12,12),false);
    sourceMotion.poses().forEach((name,pose)->{var target=current.poses.get(name);if(target!=null)copy(pose,target);});
    long seed=staff.getUUID().getLeastSignificantBits();
    if(role==KindergartenContent.Role.TEACHER||role==KindergartenContent.Role.PRINCIPAL||role==KindergartenContent.Role.FORTUNE){
      for(String side:new String[]{"left","right"}){
        var arm=current.poses.get("arm_"+side);if(arm==null)continue;
        float sign=side.equals("left")?1:-1;
        arm.x=-sign*.55f;arm.y=.15f;arm.z=-.4f;arm.rx=0;arm.ry=sign*(role==KindergartenContent.Role.TEACHER?1f:.8f);arm.rz=sign*.65f;
      }
    }
    var head=current.poses.get("skull");
    if(head!=null)head.rx+=Mth.sin(age*.035f+(seed&31))*.009f;
    serviceGesture(staff,Mth.clamp(age-staff.tickCount,0,1));
    // Apply the profession's own lids last so a service gesture cannot replace
    // the Fortune-Teller's permanently closed face with a blink or blank face.
    float eyelid=StaffFaceMotion0251.eyelidScale(role==KindergartenContent.Role.FORTUNE,age,seed);
    for(String side:new String[]{"left","right"}){
      var eye=current.poses.get("staff_eye_"+side);if(eye==null)continue;eye.sy=eyelid;
      if(role==KindergartenContent.Role.FORTUNE){eye.sx=1;eye.sz=1;eye.visible=true;}
    }
  }
  private void serviceGesture(KindergartenNpc staff,float partial){
    int gesture=staff.serviceGesture();if(gesture==0)return;
    float elapsed=staff.serviceGestureAge(partial),t=elapsed/48f;
    if(t<=0||t>=1)return;
    // A soft, finite acknowledgement belongs to the service interaction. It
    // never restarts the baby's perpetual idle sway or moves staff off-station.
    float envelope=Mth.sin(t*Mth.PI),nod=Mth.sin(t*Mth.TWO_PI)*envelope;
    var head=current.poses.get("skull");
    if(head!=null){head.rx+=nod*(gesture==4?.10f:.055f);head.rz+=envelope*(gesture==2?-.045f:0);}
    for(String side:new String[]{"left","right"}){
      var arm=current.poses.get("arm_"+side);if(arm==null)continue;
      float sign=side.equals("left")?1:-1;
      if(gesture==1&&side.equals("left")){arm.ry+=.24f*envelope;arm.rz-=.16f*envelope;}
      else if(gesture==2){arm.ry+=sign*.18f*envelope;arm.rx-=.09f*envelope;}
      else if(gesture==3){arm.rx-=.14f*envelope;arm.ry+=sign*.10f*nod;}
      else if(gesture==4)arm.rz-=sign*.045f*envelope;
    }
  }
  @Override public void renderToBuffer(PoseStack pose,VertexConsumer vertices,int light,int overlay,int color){
    if(current!=null){
      current.mesh.render(pose,vertices,light,overlay,color,current.poses);
      if(currentRole==KindergartenContent.Role.FORTUNE){
        renderedFortuneActor=currentActor;renderedFortuneAge=currentAge;
        renderedFortuneLeft=currentEyeVerticalScale(true);renderedFortuneRight=currentEyeVerticalScale(false);
        renderedFortunePreserved=authoredFortuneLidsPreserved();
      }
    }
  }
  /** Small read-only probe used by the excluded actual-client study. */
  public boolean uprightAdultPose(){
    if(current==null)return false;var body=current.poses.get("body");var head=current.poses.get("skull");
    // The native seated pose leans its adult torso back by only 0.08 radians.
    return body!=null&&head!=null&&Math.abs(body.rx)<.12f&&Math.abs(head.z)<.25f;
  }
  /** Actual rendered custom-eyelid scale; observation never changes a pose. */
  public float currentEyeVerticalScale(boolean left){
    if(current==null)return Float.NaN;var eye=current.poses.get("staff_eye_"+(left?"left":"right"));
    return eye==null?Float.NaN:eye.sy;
  }
  public boolean authoredFortuneLidsPreserved(){
    if(current==null||currentRole!=KindergartenContent.Role.FORTUNE)return false;
    for(String side:new String[]{"left","right"}){
      var eye=current.poses.get("staff_eye_"+side);
      if(eye==null||!eye.visible||eye.sx!=1||eye.sy!=1||eye.sz!=1)return false;
    }
    return true;
  }
  /** Last Fortune mesh actually submitted, bound to actor and advancing render age. */
  public FortuneEyelids lastRenderedFortuneEyelids(){
    return renderedFortuneActor==null?null:new FortuneEyelids(renderedFortuneActor,renderedFortuneAge,renderedFortuneLeft,renderedFortuneRight,renderedFortunePreserved);
  }
}
