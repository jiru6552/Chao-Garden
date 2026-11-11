package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Independent resource-vertex/contact checks, excluded from the installable JAR. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class AccessoryVisualSmokePoseProbe {
  private static final String[] PRIMARY={"baby","swim","fly","run","power","hero","hero_swim","hero_fly","hero_run","hero_power","dark","dark_swim","dark_fly","dark_run","dark_power"};
  private static final String[] SPECIAL={"baby","amy","omochao","chaos_light","chaos_angel","chaos_devil"};
  private static final String[] ITEMS={"baseball_cap_accessory","press_hat_accessory","headphones_accessory","round_glasses_accessory","aviators_accessory","pacifier_accessory","bowtie_accessory","cheese_bowtie_accessory","blue_hoodie_accessory","pink_hoodie_accessory","hoodie_sleeve_left","hoodie_sleeve_right","hoodie_hood","medal_race","medal_karate","eggshell_hat"};
  private static final Map<String,JsonObject> RESOURCES=new HashMap<>();
  private static final Map<String,ChaoMeshAsset> DETAIL_GEOMETRY=new HashMap<>();
  private static boolean done;
  private static float ageBlend=1;
  private AccessoryVisualSmokePoseProbe() {}
  private static void require(boolean good,String message){if(!good)throw new IllegalStateException("Chao accessory contact: "+message);}
  private static float number(JsonArray a,int n){return a.get(n).getAsFloat();}
  private static JsonObject resource(String path){return RESOURCES.computeIfAbsent(path,id->{
    try(var reader=Minecraft.getInstance().getResourceManager().openAsReader(ResourceLocation.fromNamespaceAndPath("chao_garden","models/entity/"+id+".json"))){return JsonParser.parseReader(reader).getAsJsonObject();}
    catch(Exception e){throw new IllegalStateException(id,e);}
  });}
  @SubscribeEvent public static void tick(ClientTickEvent.Post event){
    if(done||!Boolean.getBoolean("chao_garden.audit022Study")||Minecraft.getInstance().level==null)return;
    done=true;int count=inspect();
    LogUtils.getLogger().info("CHAO_ACCESSORY_021_PROBE_COMPLETE: {} fits; three independent age strengths; actual resource vertices, 15 primary rigs x 9 endpoints, child/Amy/Omochao/three Chaos rigs, five moving poses, eye/crown/torso/upper-arm contacts, full rendered ancestor chains, closed-cap crest suppression and visible core/eye preservation verified",count);
  }

  /** Parse the resource independently: this deliberately does not call mesh bounds code. */
  private static Map<String,JsonObject> bones(JsonObject data){
    var out=new LinkedHashMap<String,JsonObject>();
    for(var e:data.getAsJsonArray("bones")){var b=e.getAsJsonObject();out.put(b.get("name").getAsString(),b);}return out;
  }
  private static Matrix4f chain(Map<String,JsonObject> bones,String name,String stop,Map<String,ChaoMeshAsset.Pose> poses,boolean pixels){
    var path=new ArrayDeque<JsonObject>();
    for(String key=name;key!=null&&!key.equals(stop);){
      var b=bones.get(key);require(b!=null,"missing ancestor "+key);path.push(b);
      key=!b.has("parent")||b.get("parent").isJsonNull()?null:b.get("parent").getAsString();
      if(stop!=null&&key==null)throw new IllegalStateException("unrelated attachment bone");
    }
    Matrix4f m=new Matrix4f();float unit=pixels?1:1/16f;
    while(!path.isEmpty()){
      var b=path.pop();var p=poses.get(b.get("name").getAsString());if(p==null)p=new ChaoMeshAsset.Pose();
      var v=b.getAsJsonArray("pivot");var r=b.getAsJsonArray("rotation");float radians=(float)Math.PI/180;
      m.translate((number(v,0)+p.x)*unit,(number(v,1)+p.y)*unit,(number(v,2)+p.z)*unit)
          .rotateZ(number(r,2)*radians+p.rz).rotateY(number(r,1)*radians+p.ry).rotateX(number(r,0)*radians+p.rx).scale(p.sx,p.sy,p.sz);
    }return m;
  }
  private static float[] meshBounds(JsonObject mesh,Matrix4f m,float sf,float rp){
    float[] out={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
    var base=mesh.getAsJsonArray("vertices");Vector3f point=new Vector3f();
    for(int i=0;i<base.size();i++){
      float[] p=new float[3];
      if(!mesh.has("morph_vertices"))for(int axis=0;axis<3;axis++)p[axis]=number(base.get(i).getAsJsonArray(),axis);
      else for(int y=0;y<3;y++)for(int x=0;x<3;x++){
        float w=Math.max(0,1-Math.abs(sf-(y-1)))*Math.max(0,1-Math.abs(rp-(x-1)));
        var v=mesh.getAsJsonArray("morph_vertices").get(y*3+x).getAsJsonArray().get(i).getAsJsonArray();
        for(int axis=0;axis<3;axis++)p[axis]+=w*number(v,axis);
      }
      if(mesh.has("morph_vertices"))for(int axis=0;axis<3;axis++) {
        float young=number(base.get(i).getAsJsonArray(),axis);p[axis]=young+(p[axis]-young)*ageBlend;
      }
      m.transformPosition(p[0],p[1],p[2],point);
      for(int axis=0;axis<3;axis++){float v=point.get(axis);out[axis]=Math.min(out[axis],v);out[axis+3]=Math.max(out[axis+3],v);}
    }return out;
  }
  private static float[] join(float[] a,float[] b){if(a==null)return b.clone();for(int axis=0;axis<3;axis++){a[axis]=Math.min(a[axis],b[axis]);a[axis+3]=Math.max(a[axis+3],b[axis+3]);}return a;}
  private static float[] core(Map<String,JsonObject> all,String anchor,Map<String,ChaoMeshAsset.Pose> poses,float sf,float rp){
    float[] best=null;float volume=-1;
    for(var entry:all.entrySet()){
      if(!entry.getKey().startsWith(anchor+"__cube_")||anchor.startsWith("arm_")&&!entry.getKey().equals(anchor+"__cube_0"))continue;
      float[] bounds=null;var b=entry.getValue();if(!b.has("meshes"))continue;
      Matrix4f m=chain(all,entry.getKey(),anchor,poses,true);
      for(var e:b.getAsJsonArray("meshes"))bounds=join(bounds,meshBounds(e.getAsJsonObject(),m,sf,rp));
      if(bounds==null)continue;float v=(bounds[3]-bounds[0])*(bounds[4]-bounds[1])*(bounds[5]-bounds[2]);
      if(v>volume){best=bounds;volume=v;}
    }require(best!=null,"no independent skin core "+anchor);return best;
  }
  private static float[] fittedPart(String item,int part,ChaoAccessoryFit.Fit fit){
    var b=resource("cwe_accessories/"+item).getAsJsonArray("bones").get(0).getAsJsonObject();
    var mesh=b.getAsJsonArray("meshes").get(part).getAsJsonObject();
    Matrix4f m=new Matrix4f().translate(fit.x(),fit.y(),fit.z()).scale(fit.sx(),fit.sy(),fit.sz());
    return meshBounds(mesh,m,0,0);
  }
  private static float[] envelope(JsonObject data,Map<String,JsonObject> all,String anchor,Map<String,ChaoMeshAsset.Pose> poses,float sf,float rp){
    float[] out=core(all,anchor,poses,sf,rp);
    if(data.has("attachment_extra_bones")&&data.getAsJsonObject("attachment_extra_bones").has(anchor))
      for(var name:data.getAsJsonObject("attachment_extra_bones").getAsJsonArray(anchor)){
        String key=name.getAsString();var b=all.get(key);var m=chain(all,key,anchor,poses,true);
        for(var mesh:b.getAsJsonArray("meshes"))out=join(out,meshBounds(mesh.getAsJsonObject(),m,sf,rp));
      }
    return out;
  }
  private static float[] independentEyes(Map<String,JsonObject> all,Map<String,ChaoMeshAsset.Pose> poses,float sf,float rp){
    var centres=new ArrayList<float[]>();float front=Float.POSITIVE_INFINITY;
    for(var entry:all.entrySet()){
      var bone=entry.getValue();if(!bone.has("meshes"))continue;
      Matrix4f m=null;float[] eye=null;
      for(var element:bone.getAsJsonArray("meshes")){
        var mesh=element.getAsJsonObject();
        boolean lens=mesh.get("material").getAsString().equals("lens");
        boolean special=entry.getKey().startsWith("chaos_eye_")||entry.getKey().startsWith("character_robot_eye_");
        if(!lens&&!special)continue;
        if(m==null)m=chain(all,entry.getKey(),"skull",poses,true);
        float[] bounds=meshBounds(mesh,m,sf,rp);front=Math.min(front,bounds[2]);
        // Robot sockets locate the eye centre; the asymmetric spiral is its iris.
        if(lens||special&&(!entry.getKey().startsWith("character_robot_eye_")||mesh.get("name").getAsString().equals("eye_socket")))eye=join(eye,bounds);
      }if(eye!=null)centres.add(eye);
    }
    require(centres.size()==2,"expected exactly two rendered eyes, found "+centres.size());
    float x=0,y=0;for(var eye:centres){x+=(eye[0]+eye[3])*.5f;y+=(eye[1]+eye[4])*.5f;}x/=2;y/=2;
    float half=0;for(var eye:centres)half+=Math.abs((eye[0]+eye[3])*.5f-x);
    return new float[]{x,y,front,half/2};
  }
  private static void near(float a,float b,float tolerance,String why){require(Math.abs(a-b)<=tolerance,why+" "+a+" versus "+b);}
  private static void contacts(String item,ChaoAccessoryFit.Fit fit,float[] h,float[] eyes,float[] b,float[] arm){
    float[] part=fittedPart(item,0,fit);
    if(item.contains("glasses")||item.contains("aviators")){
      var other=fittedPart(item,1,fit);
      near((part[1]+part[4])*.5f,eyes[1],.02f,"glasses left eye height");
      near((other[1]+other[4])*.5f,eyes[1],.02f,"glasses right eye height");
      near(((part[0]+part[3])+(other[0]+other[3]))*.25f,eyes[0],.02f,"glasses eye midpoint");
      near((other[0]+other[3]-part[0]-part[3])*.25f,eyes[3],.02f,"glasses eye spacing");
      require(part[5]<eyes[2]-.03f,"glass lens hidden behind iris plane");
    }else if(item.contains("cap_")||item.contains("press_hat")){
      near(part[4],h[1],.4f,"cap brim missed crown");require(part[0]<=h[0]&&part[3]>=h[3],"cap too narrow");
    }else if(item.contains("headphones"))near(part[1],h[1],.15f,"headphone band missed crown");
    else if(item.contains("pacifier")){
      near((part[1]+part[4])*.5f,eyes[1]+1.5f,.02f,"pacifier missed mouth height");require(part[5]<eyes[2]-.03f,"pacifier inside face");
    }else if(item.contains("bowtie")){
      var band=fittedPart(item,1,fit);require(band[1]>=b[1]&&band[4]<b[1]+(b[4]-b[1])*.4f,"bowtie missed upper torso");require(band[5]<b[2],"bowtie inside chest");
    }else if(item.equals("hoodie_hood")){
      for(int axis=0;axis<3;axis++)require(part[axis]<h[axis]&&part[axis+3]>h[axis+3],"hood buried in skull");
    }else if(item.endsWith("hoodie_accessory")||item.startsWith("hoodie_sleeve_")){
      float[] skin=item.startsWith("hoodie_sleeve_")?arm:b;
      for(int axis=0;axis<3;axis++)require(part[axis]<skin[axis]&&part[axis+3]>skin[axis+3],"cloth buried in skin axis "+axis);
    }else if(item.startsWith("medal_")){
      require(part[5]<b[2]-.20f,"medal behind torso/hoodie");require(part[1]>=b[1]&&part[4]<b[4],"medal ribbon outside chest");
    }else if(item.equals("eggshell_hat"))near(part[4],h[1]+.35f,.03f,"eggshell rim missed crown");
  }

  /** Exercise the actual hide operation, independently classify donor/authored meshes. */
  private static void closedCap(JsonObject data,Map<String,JsonObject> all,OfficialChaoRig rig,float sf,float rp){
    var roots=new HashSet<String>();
    var donor=resource("official/chao_"+rig.sourceGeometryKey());
    for(var e:donor.getAsJsonArray("head_decorations"))roots.add(e.getAsString());
    for(String name:all.keySet())
      if(name.startsWith("evolution_")||name.startsWith("evolution020_")||name.equals("monster_crown")
          ||name.equals("character_hair")||name.equals("character_propeller")||name.startsWith("chaos_crown"))roots.add(name);
    String coreName=null;float coreVolume=-1;
    for(var entry:all.entrySet())if(entry.getKey().startsWith("skull__cube_")){
      float[] bounds=null;var bone=entry.getValue();if(!bone.has("meshes"))continue;
      var matrix=chain(all,entry.getKey(),"skull",rig.poses(),true);
      for(var e:bone.getAsJsonArray("meshes"))bounds=join(bounds,meshBounds(e.getAsJsonObject(),matrix,sf,rp));
      if(bounds==null)continue;float v=(bounds[3]-bounds[0])*(bounds[4]-bounds[1])*(bounds[5]-bounds[2]);
      if(v>coreVolume){coreVolume=v;coreName=entry.getKey();}
    }
    rig.hideHeadDecorations();
    require(coreName!=null&&rig.poses().get(coreName).visible,rig.geometryKey()+" closed cap hid main skull core "+coreName);
    int eyes=0;
    for(var entry:all.entrySet()){
      var bone=entry.getValue();if(!bone.has("meshes")||bone.getAsJsonArray("meshes").isEmpty())continue;
      String name=entry.getKey();boolean crest=false,eye=name.startsWith("character_robot_eye_")||name.startsWith("chaos_eye_");
      for(var e:bone.getAsJsonArray("meshes"))if(e.getAsJsonObject().get("material").getAsString().equals("lens"))eye=true;
      for(String at=name;at!=null;){
        if(roots.contains(at)){crest=true;break;}
        var b=all.get(at);at=!b.has("parent")||b.get("parent").isJsonNull()?null:b.get("parent").getAsString();
      }
      if(crest)require(!rig.poses().get(name).visible,rig.geometryKey()+" closed cap left donor/authored crest visible "+name);
      if(eye){eyes++;require(rig.poses().get(name).visible,rig.geometryKey()+" closed cap hid rendered eye "+name);}
    }
    require(eyes==2,rig.geometryKey()+" closed cap expected two retained eyes, found "+eyes);
  }

  public static int inspect(){
    require(CweAccessoryLayer.anchor(EquipmentSlot.LEGS).equals("body"),"neckwear follows moving head instead of collar");
    require(ChaoAccessoryFit.coversHead("baseball_cap_accessory")&&ChaoAccessoryFit.coversHead("press_hat_accessory")
        &&!ChaoAccessoryFit.coversHead("headphones_accessory"),"closed caps must hide intersecting crests; headphones preserve open crown");
    var rig=new OfficialChaoRig();var state=new ChaoState();state.adult=true;int count=0;
    var keys=new ArrayList<String>();Collections.addAll(keys,PRIMARY);Collections.addAll(keys,SPECIAL);
    for(int k=0;k<keys.size();k++){
      String key=keys.get(k);boolean primary=k<PRIMARY.length;
      for(int sf=primary?-1:0;sf<=(primary?1:0);sf++)for(int rp=primary?-1:0;rp<=(primary?1:0);rp++)
        for(float strength:new float[]{0,.6f,1.2f})for(var mode:List.of(OfficialChaoRig.Motion.WALK,OfficialChaoRig.Motion.CRAWL,OfficialChaoRig.Motion.RUN,OfficialChaoRig.Motion.SIT,OfficialChaoRig.Motion.HELD)){
          UUID id=new UUID(2000+k,mode.ordinal());
          state.adult=primary;state.setEvolutionStrength(strength);state.swimFly=sf;state.runPower=rp;ageBlend=state.gradualVisualStrength()*(primary?1:.14f);
          rig.prepareDevelopment(key,state);rig.animateGameplay(key,id,mode,120,1.3f,.04f,0,0,31,-18,false);rig.development(state);
          var data=resource("gameplay/chao_"+key);var all=bones(data);var poses=rig.poses();
          float[] h=envelope(data,all,"skull",poses,sf,rp),b=envelope(data,all,"body",poses,sf,rp),eyes=rig.attachmentEyes();
          require(eyes!=null,key+" has no eye landmarks");
          float[] drawnEyes=independentEyes(all,poses,sf,rp);
          for(int axis=0;axis<4;axis++)near(eyes[axis],drawnEyes[axis],axis==2?.06f:.001f,key+" eye landmark versus rendered eye geometry");
          for(String anchor:List.of("skull","body","arm_left","arm_right")){
            var independent=envelope(data,all,anchor,poses,sf,rp);var actual=rig.attachmentBounds(anchor);
            for(int axis=0;axis<6;axis++)near(actual[axis],independent[axis],.001f,key+" runtime skin bounds "+anchor);
          }
          for(String item:ITEMS){
            String anchor=item.startsWith("hoodie_sleeve_")?(item.endsWith("left")?"arm_left":"arm_right"):
                item.contains("bowtie")||item.endsWith("hoodie_accessory")||item.startsWith("medal_")?"body":"skull";
            float[] arm=anchor.startsWith("arm_")?core(all,anchor,poses,sf,rp):null;
            var fit=ChaoAccessoryFit.accessory(item,rig.attachmentBounds("skull"),eyes,rig.attachmentBounds("body"),arm);
            contacts(item,fit,h,eyes,b,arm);
            if(item.contains("glasses")||item.contains("aviators")){
              var glass=resource("cwe_accessories/"+item);var geometry=DETAIL_GEOMETRY.computeIfAbsent(item,idKey->ChaoMeshAsset.load("cwe_accessories/"+idKey));var detail=geometry.createPoses();
              ChaoAccessoryFit.parts(item,h,fit,detail);var parts=bones(glass);
              for(String side:List.of("left","right")){
                String temple="temple_"+side;var mesh=parts.get(temple).getAsJsonArray("meshes").get(0).getAsJsonObject();
                var bounds=meshBounds(mesh,chain(parts,temple,null,detail,true),0,0);
                float sideContact=fit.x()+(bounds[0]+bounds[3])*.5f*fit.sx();
                near(sideContact,side.equals("left")?h[0]-.1f:h[3]+.1f,.002f,key+" spectacles temple-to-skull contact");
                require(detail.get("hinge_"+side).sx>0,"spectacle hinge inverted: "+key+" "+sf+"/"+rp+" "+mode+" "+side+" scale="+detail.get("hinge_"+side).sx+" head="+Arrays.toString(h)+" eyes="+Arrays.toString(eyes)+" fit="+fit);
              }
            }
            // Use the real PoseStack path, including ancestor translations,
            // rotations/scales. A second JSON walk supplies the independent path.
            PoseStack stack=new PoseStack();rig.transformBone(anchor,stack);fit.apply(stack);
            Matrix4f expected=chain(all,anchor,null,poses,false);
            var mesh=resource("cwe_accessories/"+item).getAsJsonArray("bones").get(0).getAsJsonObject().getAsJsonArray("meshes").get(0).getAsJsonObject();
            for(var e:mesh.getAsJsonArray("vertices")){
              var v=e.getAsJsonArray();float[] p={number(v,0),number(v,1),number(v,2)},f=fit.point(p);
              var actualPoint=stack.last().pose().transformPosition(p[0]/16,p[1]/16,p[2]/16,new Vector3f());
              var independent=expected.transformPosition(f[0]/16,f[1]/16,f[2]/16,new Vector3f());
              require(actualPoint.distance(independent)<.0001f,key+" worn "+item+" ancestor transform drift");
            }count++;
          }
          var reference=core(bones(resource("official/chao_"+rig.sourceGeometryKey())),"skull",Map.of(),0,0);
          var legacy=ChaoAccessoryFit.legacyHead(h,rig.sourceGeometryKey());
          for(int corner=0;corner<2;corner++)for(int axis=0;axis<3;axis++){
            float[] p={reference[corner*3],reference[corner*3+1],reference[corner*3+2]};
            near(legacy.point(p)[axis],h[corner*3+axis],.001f,"legacy hat must follow current skin, not source alias");
          }
          closedCap(data,all,rig,sf,rp);
        }
    }return count;
  }
}
