package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.ChaoVariants;
import com.chaogarden.core.ChaoState;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Live native materials and production fitting helpers; excluded from the mod JAR. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class VariantsVisualSmokePoseProbe {
  private static final String[] KEYS={"baby","swim","fly","run","power","hero","hero_swim","hero_fly","hero_run","hero_power","dark","dark_swim","dark_fly","dark_run","dark_power"};
  private static final String[] LAYOUTS={"baby","swim","fly","run","power","hero","hero_fly","hero_power","dark","dark_run","dark_swim"};
  private static final Map<String,JsonObject> JSON=new HashMap<>();
  private static boolean done;
  private VariantsVisualSmokePoseProbe(){}
  private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Native theme audit: "+why);}
  private static void near(float a,float b,String why){require(Math.abs(a-b)<.0005f,why+" "+a+" versus "+b);}
  private static JsonObject resource(String key){return JSON.computeIfAbsent(key,id->{
    try(var reader=Minecraft.getInstance().getResourceManager().openAsReader(ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,"models/entity/"+id+".json"))){return JsonParser.parseReader(reader).getAsJsonObject();}
    catch(Exception error){throw new IllegalStateException(id,error);}
  });}
  private static Map<String,JsonObject> bones(JsonObject data){var out=new LinkedHashMap<String,JsonObject>();for(var e:data.getAsJsonArray("bones")){var b=e.getAsJsonObject();out.put(b.get("name").getAsString(),b);}return out;}
  private static float number(JsonArray a,int i){return a.get(i).getAsFloat();}
  /** Independent Rz*Ry*Rx chain. Static rotations are optional in authored JSON. */
  private static Matrix4f chain(Map<String,JsonObject> all,String name,Map<String,ChaoMeshAsset.Pose> poses){
    var path=new ArrayDeque<JsonObject>();var seen=new HashSet<String>();
    while(name!=null){require(seen.add(name),"cyclic ancestor");var b=all.get(name);require(b!=null,"missing ancestor "+name);path.push(b);name=!b.has("parent")||b.get("parent").isJsonNull()?null:b.get("parent").getAsString();}
    var m=new Matrix4f();float r=(float)Math.PI/180;
    while(!path.isEmpty()){
      var b=path.pop();var p=poses.get(b.get("name").getAsString());if(p==null)p=new ChaoMeshAsset.Pose();
      var v=b.getAsJsonArray("pivot");require(v!=null&&v.size()==3,"missing pivot");var rotation=b.getAsJsonArray("rotation");
      float rx=rotation==null?0:number(rotation,0)*r,ry=rotation==null?0:number(rotation,1)*r,rz=rotation==null?0:number(rotation,2)*r;
      m.translate((number(v,0)+p.x)/16,(number(v,1)+p.y)/16,(number(v,2)+p.z)/16).rotateZ(rz+p.rz).rotateY(ry+p.ry).rotateX(rx+p.rx).scale(p.sx,p.sy,p.sz);
    }return m;
  }
  private static ChaoState state(String theme,int family,float age){
    var s=new ChaoState();s.adult=true;s.character="normal";s.variant=theme;
    int alignment=family/5;s.align=alignment==0?0:alignment==1?1:-1;s.primaryType=family%5;s.swimFly=s.runPower=1;s.eyeColor=3;s.setEvolutionStrength(age);return s;
  }
  private static int family(String layout){for(int i=0;i<KEYS.length;i++)if(KEYS[i].equals(layout))return i;throw new IllegalStateException(layout);}
  private static boolean expressionPanel(JsonObject bone){
    // Dark donors also have inflated 1x1 ornaments omitted from the decoration
    // list. The actual expression panels are the supplied 2x2x1 + .1 shells.
    float[] low={999,999,999},high={-999,-999,-999};
    for(var mesh:bone.getAsJsonArray("meshes"))for(var p:mesh.getAsJsonObject().getAsJsonArray("vertices"))for(int a=0;a<3;a++){
      float v=number(p.getAsJsonArray(),a);low[a]=Math.min(low[a],v);high[a]=Math.max(high[a],v);
    }
    return Math.abs(high[0]-low[0]-2.2f)<.001f&&Math.abs(high[1]-low[1]-2.2f)<.001f&&Math.abs(high[2]-low[2]-1.2f)<.001f;
  }
  private static boolean[] eyeMask(JsonObject data,int width,int height){
    var all=bones(data);Set<String> decor=new HashSet<>();for(var e:data.getAsJsonArray("head_decorations"))decor.add(e.getAsString());
    String core=null;float largest=-1;
    for(var entry:all.entrySet())if(entry.getKey().startsWith("skull__cube_"))for(var e:entry.getValue().getAsJsonArray("meshes")){
      float[] lo={999,999,999},hi={-999,-999,-999};for(var v:e.getAsJsonObject().getAsJsonArray("vertices"))for(int a=0;a<3;a++){float n=number(v.getAsJsonArray(),a);lo[a]=Math.min(lo[a],n);hi[a]=Math.max(hi[a],n);}
      float volume=(hi[0]-lo[0])*(hi[1]-lo[1])*(hi[2]-lo[2]);if(volume>largest){largest=volume;core=entry.getKey();}
    }
    float sx=width/data.get("texture_width").getAsFloat(),sy=height/data.get("texture_height").getAsFloat();boolean[] mask=new boolean[width*height];int eyeBones=0;
    for(var entry:all.entrySet())if(entry.getKey().startsWith("skull__cube_")&&!entry.getKey().equals(core)&&!decor.contains(entry.getKey())&&expressionPanel(entry.getValue())){
      eyeBones++;
      for(var mesh:entry.getValue().getAsJsonArray("meshes"))for(var face:mesh.getAsJsonObject().getAsJsonArray("faces")){
        float u0=999,v0=999,u1=-999,v1=-999;
        for(var uv:face.getAsJsonObject().getAsJsonArray("uv")){u0=Math.min(u0,number(uv.getAsJsonArray(),0));u1=Math.max(u1,number(uv.getAsJsonArray(),0));v0=Math.min(v0,number(uv.getAsJsonArray(),1));v1=Math.max(v1,number(uv.getAsJsonArray(),1));}
        for(int y=Math.max(0,(int)(v0*sy));y<Math.min(height,(int)Math.ceil(v1*sy));y++)for(int x=Math.max(0,(int)(u0*sx));x<Math.min(width,(int)Math.ceil(u1*sx));x++)mask[y*width+x]=true;
      }
    }
    require(eyeBones==2,"expected two original expression panels");return mask;
  }
  private static final class Sample {float u,v;int color;}
  private static final class Sink implements VertexConsumer {
    final List<Sample> samples=new ArrayList<>();Sample current;
    public VertexConsumer addVertex(float x,float y,float z){require(Float.isFinite(x)&&Float.isFinite(y)&&Float.isFinite(z),"non-finite rendered vertex");current=new Sample();samples.add(current);return this;}
    public VertexConsumer setColor(int r,int g,int b,int a){current.color=(a<<24)|(r<<16)|(g<<8)|b;return this;}
    public VertexConsumer setUv(float u,float v){current.u=u;current.v=v;return this;}
    public VertexConsumer setUv1(int u,int v){return this;}
    public VertexConsumer setUv2(int u,int v){return this;}
    public VertexConsumer setNormal(float x,float y,float z){return this;}
  }
  private static int materials(){
    int count=0;var rig=new OfficialChaoRig();
    for(var theme:ChaoVariants.all())for(String layout:LAYOUTS){
      var s=state(theme.id(),family(layout),1.2f);var selected=OfficialChaoRig.texture(s);
      require(selected.getPath().equals("textures/entity/chao/gameplay/variants/"+theme.id()+"/"+layout+".png"),"wrong selected native layout "+selected);
      var source=ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,"textures/entity/chao/official/"+layout+".png");
      try(var actual=OmochaoMaterial.image(selected);var original=OmochaoMaterial.image(source)){
        var image=actual.pixels();var base=original.pixels();require(image.getWidth()==base.getWidth()&&image.getHeight()==base.getHeight(),"native size mismatch");
        var protectedEyes=eyeMask(resource("official/chao_"+layout),image.getWidth(),image.getHeight());
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){
          int a=image.getPixelRGBA(x,y),b=base.getPixelRGBA(x,y);require((a>>>24)==(b>>>24),"source alpha changed");
          if(protectedEyes[y*image.getWidth()+x])require(a==b,"protected native expression recolored");
        }
        String key=OfficialChaoRig.geometryKey(s);rig.prepareDevelopment(key,s);rig.animateGameplay(key,new UUID(2200,count),OfficialChaoRig.Motion.WALK,120,2,.08f,0,0,0,0,false);rig.development(s);
        var sink=new Sink();rig.render(new PoseStack(),sink,15728880,0,-1);int irises=0;
        for(var v:sink.samples)if((v.color&0xffffff)==0xdf505c){
          int x=Math.min(image.getWidth()-1,Math.max(0,(int)(v.u*image.getWidth()))),y=Math.min(image.getHeight()-1,Math.max(0,(int)(v.v*image.getHeight())));int color=image.getPixelRGBA(x,y);
          int r=color&255,g=(color>>>8)&255,b=(color>>>16)&255;
          require((color>>>24)==255&&Math.min(r,Math.min(g,b))>205&&Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b))<22,"emitted genetic iris samples tinted skin");irises++;
        }
        require(irises>=8,"actual colored irises were not emitted");
      }catch(java.io.IOException error){throw new IllegalStateException("Native theme material "+selected,error);}
      count++;
    }return count;
  }
  private static int[] poses(){
    var rig=new OfficialChaoRig();var normal=new OfficialChaoRig();int count=0,caps=0;
    var layers=new HashMap<String,ChaoOverlayLayer.Rig>();var references=new HashMap<String,ChaoMeshAsset>();
    var entity=ChaoGarden.CHAO.get().create(Minecraft.getInstance().level);require(entity!=null,"missing live entity factory");
    var modes=List.of(OfficialChaoRig.Motion.CRAWL,OfficialChaoRig.Motion.WALK,OfficialChaoRig.Motion.SIT,OfficialChaoRig.Motion.HELD,OfficialChaoRig.Motion.GLIDE);
    for(var theme:ChaoVariants.all())for(int family=0;family<15;family++)for(float age:new float[]{0,.6f,1.2f}){
      var s=state(theme.id(),family,age);String key=KEYS[family],layout=OfficialChaoRig.sourceGeometryKey(key);require(OfficialChaoRig.geometryKey(s).equals(key),"theme forced baby/special geometry");
      entity.setState(s);entity.setItemSlot(EquipmentSlot.FEET,new ItemStack(ChaoGarden.item("baseball_cap_accessory")));require(ChaoAccessoryFit.headCovered(entity),"real closed cap fails theme suppression");entity.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);require(!ChaoAccessoryFit.headCovered(entity),"unworn cap remains active");caps++;
      var reference=references.computeIfAbsent(layout,l->ChaoMeshAsset.load("official/chao_"+l));float[] oldHead=reference.attachmentBounds("skull",reference.createPoses()),oldBody=reference.attachmentBounds("body",reference.createPoses());
      var layer=layers.computeIfAbsent(layout,l->ChaoOverlayLayer.fittedRig(ChaoMeshAsset.load("overlays/chao_"+l)));var json=bones(resource("gameplay/chao_"+key));var overlay=bones(resource("overlays/chao_"+layout));
      for(var mode:modes){
        rig.prepareDevelopment(key,s);rig.animateGameplay(key,new UUID(family,mode.ordinal()),mode,120,2,.08f,0,0,12,-4,false);rig.development(s);
        var plain=state(ChaoVariants.NORMAL,family,age);normal.prepareDevelopment(key,plain);normal.animateGameplay(key,new UUID(family,mode.ordinal()),mode,120,2,.08f,0,0,12,-4,false);normal.development(plain);
        require(rig.shapeSignature()==normal.shapeSignature(),"theme disables ordinary gradual anatomy");require(!rig.naturalHeroCoreActive(),"natural Hero swatch overrides theme palette");
        float[] head=rig.attachmentBounds("skull"),body=rig.attachmentBounds("body");
        var hf=ChaoAccessoryFit.legacyHead(head,layout);var bf=ChaoAccessoryFit.legacyBody(body,layout);var mf=ChaoAccessoryFit.legacyMarker(head,layout);
        for(int a=0;a<3;a++){
          near(hf.point(new float[]{oldHead[0],oldHead[1],oldHead[2]})[a],head[a],"head min fit");near(bf.point(new float[]{oldBody[3],oldBody[4],oldBody[5]})[a],body[a+3],"body max fit");
        }
        near(mf.sx(),1,"marker width changed");near(mf.sy(),1,"marker height changed");near(mf.sz(),1,"marker depth changed");
        layer.poses().values().forEach(ChaoMeshAsset.Pose::reset);for(var entry:rig.poses().entrySet()){var p=layer.poses().get(entry.getKey());if(p!=null)ChaoOverlayLayer.copy(entry.getValue(),p);}
        for(String suffix:List.of("head","body","marker")){
          String name="variant_"+theme.id()+"_"+suffix;var b=overlay.get(name);require(b!=null,"missing theme node "+name);var fit=suffix.equals("body")?bf:suffix.equals("marker")?mf:hf;
          var p=layer.poses().get(name);ChaoOverlayLayer.fitHead(layer,name,p,fit);
          String anchor=suffix.equals("body")?"body":"skull";Matrix4f parent=chain(json,anchor,rig.poses());var stack=new PoseStack();layer.geometry().transformBone(name,stack,layer.poses());
          var pivot=b.getAsJsonArray("pivot");var expected=new Vector3f();var actual=new Vector3f();
          for(var mesh:b.getAsJsonArray("meshes"))for(var v:mesh.getAsJsonObject().getAsJsonArray("vertices")){
            float[] point={number(v.getAsJsonArray(),0),number(v.getAsJsonArray(),1),number(v.getAsJsonArray(),2)};
            parent.transformPosition((fit.x()+fit.sx()*(number(pivot,0)+point[0]))/16,(fit.y()+fit.sy()*(number(pivot,1)+point[1]))/16,(fit.z()+fit.sz()*(number(pivot,2)+point[2]))/16,expected);
            stack.last().pose().transformPosition(point[0]/16,point[1]/16,point[2]/16,actual);require(expected.distance(actual)<.0005f,"moving ancestor lost "+key+"/"+name);
          }
          if(suffix.equals("marker"))near(number(pivot,1)+p.y,head[1]-5,"marker crown gap");
        }
        require(rig.finite(),"non-finite native themed pose");count++;
      }
    }
    var special=state("create",0,1.2f);special.immortal=true;require(OfficialChaoRig.geometryKey(special).equals("chaos_light"),"dormant theme replaces immortal crown");
    return new int[]{count,caps};
  }
  @SubscribeEvent public static void tick(ClientTickEvent.Post event){
    if(done||!Boolean.getBoolean("chao_garden.audit022Study")||Minecraft.getInstance().level==null)return;done=true;
    int materials=materials();int[] poses=poses();
    LogUtils.getLogger().info("CHAO_VARIANTS_022_POSE_PROBE_COMPLETE: {} actual fitting-helper poses; {} actual cap cases; native first-type/age geometry, head/torso affine and complete moving ancestor chains verified",poses[0],poses[1]);
    LogUtils.getLogger().info("CHAO_VARIANTS_022_MATERIAL_PROBE_COMPLETE: {} actual native theme atlases; source eyes/alpha, emitted neutral iris samples and selected donor layouts verified",materials);
  }
}
