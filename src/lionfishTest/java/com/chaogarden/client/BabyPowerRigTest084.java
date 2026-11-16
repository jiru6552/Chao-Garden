package com.chaogarden.client;

import com.chaogarden.core.BabyPowerMorphology084;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.CreativeChaoEditRules056;
import com.chaogarden.core.HeroRunMorphology052;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.joml.Vector3f;

/** Emitted fixed-head geometry, actual shared rig and pure pixel policy checks. */
public final class BabyPowerRigTest084 {
  private static final Path ROOT=Path.of("src/main/resources/assets/chao_garden");
  private static final String MODEL="gameplay/chao_hero_run_proto055";
  private static int checks,frames,motionFrames;
  private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
  private static void near(float a,float b,float epsilon,String why){check(Float.isFinite(a)&&Float.isFinite(b)&&Math.abs(a-b)<=epsilon,why+": "+a+" vs "+b);}
  private static JsonObject json(String relative)throws Exception{return JsonParser.parseString(Files.readString(ROOT.resolve(relative))).getAsJsonObject();}
  private static ChaoMeshAsset asset(String key,boolean power){
    try{var mesh=ChaoMeshAsset.parse(json("models/entity/"+key+".json"));if(power&&key.equals(MODEL))mesh.babyPowerTargets084(json("baby_power084.json"));mesh.lens(0xff101820);mesh.expression(0);return mesh;}
    catch(Exception error){throw new IllegalStateException(key,error);}
  }
  private static ChaoState state(float strength,float signedPower,float alignment){
    var s=new ChaoState();s.align=alignment;s.ageTicks=1234;s.setEvolutionStrength(strength);
    int total=Math.round(Math.abs(signedPower)*480),stat=signedPower<0?ChaoState.RUN:ChaoState.POWER;
    s.levels[stat]=total/100;s.progress[stat]=total%100;s.points[stat]=total;return s;
  }
  private static void sample(ChaoMeshAsset mesh,ChaoState s){
    mesh.heroRunDevelopment(HeroRunMorphology052.sample(s),s.swimFly,s.runPower);
    mesh.babyPowerDevelopment084(BabyPowerMorphology084.sample(s).growth(),s.lockedAlignment());
  }
  private static final class Capture implements VertexConsumer {
    final List<Vector3f> points=new ArrayList<>(),normals=new ArrayList<>();final List<Float> uv=new ArrayList<>();
    public VertexConsumer addVertex(float x,float y,float z){check(Float.isFinite(x)&&Float.isFinite(y)&&Float.isFinite(z),"Nonfinite emitted position");points.add(new Vector3f(x*16,y*16,z*16));return this;}
    public VertexConsumer setColor(int r,int g,int b,int a){return this;}
    public VertexConsumer setUv(float u,float v){check(Float.isFinite(u)&&Float.isFinite(v),"Nonfinite UV");uv.add(u);uv.add(v);return this;}
    public VertexConsumer setUv1(int u,int v){return this;}public VertexConsumer setUv2(int u,int v){return this;}
    public VertexConsumer setNormal(float x,float y,float z){check(Float.isFinite(x)&&Float.isFinite(y)&&Float.isFinite(z),"Nonfinite normal");normals.add(new Vector3f(x,y,z));return this;}
  }
  private static Capture capture(ChaoMeshAsset mesh,Map<String,ChaoMeshAsset.Pose> poses){var c=new Capture();mesh.render(new PoseStack(),c,0xf000f0,0,0xffffffff,poses,true);check(c.points.size()==c.normals.size()&&c.uv.size()==c.points.size()*2,"Missing emitted attributes");return c;}
  private static Capture solid(ChaoMeshAsset mesh,String bone){var p=mesh.createPoses();p.values().forEach(v->v.visible=false);p.get(bone).visible=true;return capture(mesh,p);}
  private static void equal(Capture a,Capture b,String why){check(a.points.size()==b.points.size()&&a.uv.equals(b.uv),why+" topology/UV");for(int i=0;i<a.points.size();i++)check(a.points.get(i).equals(b.points.get(i)),why+" position "+i);}
  private static Vector3f point(Capture c,int i){return new Vector3f(c.points.get(i));}
  private static Vector3f[] edges(Capture c,int start){var front=new Vector3f();var back=new Vector3f();for(int i=0;i<4;i++){front.add(c.points.get(start+i));back.add(c.points.get(start+4+i));}return new Vector3f[]{point(c,start+1).sub(c.points.get(start)),point(c,start+2).sub(c.points.get(start+1)),back.sub(front).div(4)};}
  private static boolean intersects(Capture a,int ai,Capture b,int bi){
    var axes=new ArrayList<Vector3f>();for(int face=0;face<6;face++){axes.add(new Vector3f(a.normals.get(ai+face*4)));axes.add(new Vector3f(b.normals.get(bi+face*4)));}
    for(var x:edges(a,ai))for(var y:edges(b,bi))axes.add(new Vector3f(x).cross(y));
    for(var axis:axes){if(axis.lengthSquared()<1e-12f)continue;axis.normalize();float al=99999,ah=-99999,bl=99999,bh=-99999;
      for(int i=0;i<24;i++){float av=a.points.get(ai+i).dot(axis),bv=b.points.get(bi+i).dot(axis);al=Math.min(al,av);ah=Math.max(ah,av);bl=Math.min(bl,bv);bh=Math.max(bh,bv);}
      if(Math.min(ah,bh)-Math.max(al,bl)<.000001f)return false;
    }return true;
  }
  private static void cuboids(Capture c){
    check(c.points.size()==16*24,"Approved separate feature topology changed");
    for(int start=0;start<c.points.size();start+=24){
      Vector3f center=new Vector3f();for(int i=0;i<24;i++)center.add(c.points.get(start+i));center.div(24);float volume=0;
      for(int i=start;i<start+24;i+=4){var a=point(c,i).sub(center);var b=point(c,i+1).sub(center);var q=point(c,i+2).sub(center);var d=point(c,i+3).sub(center);volume+=a.dot(new Vector3f(b).cross(q))/6+a.dot(new Vector3f(q).cross(d))/6;
        var ab=point(c,i+1).sub(c.points.get(i));var bc=point(c,i+2).sub(c.points.get(i+1));check(ab.lengthSquared()>1e-7&&bc.lengthSquared()>1e-7,"Collapsed cuboid edge");
        check(Math.abs(ab.dot(bc))<.00004f*Math.max(1,ab.length()*bc.length()),"Power blend sheared a cuboid");
      }check(volume>.00000005f,"Inverted or collapsed cuboid");
    }
  }
  private static void geometry()throws Exception{
    var original=asset(MODEL,false);var mesh=asset(MODEL,true);
    for(float alignment:new float[]{-1,-.5f,-.25f,0,.25f,.5f,1})for(float age:new float[]{0,.25f,.5f,.75f,.99f,1})for(int t=-4;t<=4;t++){
      float power=t/4f;var s=state(age,power,alignment);String saved=s.encode();sample(original,s);sample(mesh,s);
      for(String bone:List.of("skull__cube_0","skull__cube_2","skull__cube_3","body__cube_0","arm_left__cube_1","arm_right__cube_1","foot_left__cube_0","foot_right__cube_0"))equal(solid(original,bone),solid(mesh,bone),"Original head/eyes/body/hands/feet changed: "+bone);
      if(power<=0||age==0)equal(capture(original,original.createPoses()),capture(mesh,mesh.createPoses()),"Zero Power/newborn or approved Run route changed");
      else check(original.shapeSignature()!=mesh.shapeSignature(),"Positive Power and age did not produce visible separate features");
      var skull=solid(mesh,"skull__cube_0");var features=solid(mesh,"gradual_head_021");var orb=solid(mesh,"emotion_dot__cube_0");var left=solid(mesh,"skull__cube_2");var right=solid(mesh,"skull__cube_3");cuboids(features);
      boolean[] reached=new boolean[16];int connected=0;
      for(int i=0;i<16;i++){
        check(!intersects(features,i*24,left,0)&&!intersects(features,i*24,right,0),"Feature clips an unchanged eye");check(!intersects(features,i*24,orb,0),"Feature clips the original baby sphere");
        if(intersects(features,i*24,skull,0)){reached[i]=true;connected++;}
      }
      boolean changed=true;while(changed){changed=false;for(int i=0;i<16;i++)if(!reached[i])for(int j=0;j<16;j++)if(reached[j]&&intersects(features,i*24,features,j*24)){reached[i]=true;connected++;changed=true;break;}}
      check(connected==16,"Separate Power features detached from head at alignment="+alignment+" age="+age+" training="+power);
      if(age==1&&power==1){
        var legacy=solid(mesh,"blocky_legacy_tuft_055");
        for(var point:legacy.points)for(int axis=0;axis<3;axis++){
          float low=Float.POSITIVE_INFINITY,high=Float.NEGATIVE_INFINITY;
          for(var head:skull.points){low=Math.min(low,head.get(axis));high=Math.max(high,head.get(axis));}
          check(point.get(axis)>=low-.00001f&&point.get(axis)<=high+.00001f,"Original decoration remains exposed through new Power features");
        }
      }
      check(saved.equals(s.encode()),"Rendering changed saved state");frames++;
    }
    for(float alignment:new float[]{-1,0,1})for(int type=0;type<=4;type++){
      var s=state(1,1,alignment);s.adult=true;s.primaryType=type;s.setEvolutionStrength(1.2f);s=ChaoState.decode(s.encode());sample(original,s);sample(mesh,s);
      equal(capture(original,original.createPoses()),capture(mesh,mesh.createPoses()),"Adult original path changed");
    }
  }
  private static void edits(){
    var s=state(.8f,0,0);var mesh=asset(MODEL,true);UUID id=new UUID(84,840084);
    var first=HeroRunAppearance052.prepare(id,s,0);mesh.heroRunDevelopment(first,0,0);mesh.babyPowerDevelopment084(HeroRunAppearance052.babyPowerGrowth084(s),HeroRunAppearance052.signedAlignment(s));var before=capture(mesh,mesh.createPoses());
    check(CreativeChaoEditRules056.apply(s,0,ChaoState.POWER,99),"Actual stat editor changes Power");String saved=s.encode();
    var edit=HeroRunAppearance052.prepare(id,s,1);mesh.heroRunDevelopment(edit,0,0);mesh.babyPowerDevelopment084(HeroRunAppearance052.babyPowerGrowth084(s),HeroRunAppearance052.signedAlignment(s));equal(before,capture(mesh,mesh.createPoses()),"First edit frame popped");
    for(int tick=2;tick<=21;tick++){var p=HeroRunAppearance052.prepare(id,s,tick);mesh.heroRunDevelopment(p,0,0);mesh.babyPowerDevelopment084(HeroRunAppearance052.babyPowerGrowth084(s),HeroRunAppearance052.signedAlignment(s));var now=capture(mesh,mesh.createPoses());for(int i=0;i<before.points.size();i++)check(before.points.get(i).distance(now.points.get(i))<1,"Large Power edit jumped a model pixel in one tick");before=now;}
    near(HeroRunAppearance052.babyPowerGrowth084(s),.8f,.00001f,"Presentation never reached real age-conditioned Power target");check(saved.equals(s.encode()),"Presentation interpolation edits save");
    CreativeChaoEditRules056.apply(s,0,ChaoState.POWER,0);var p=HeroRunAppearance052.prepare(id,s,22);mesh.heroRunDevelopment(p,0,0);mesh.babyPowerDevelopment084(HeroRunAppearance052.babyPowerGrowth084(s),0);equal(before,capture(mesh,mesh.createPoses()),"Reversal popped");
    for(int tick=23;tick<=42;tick++)HeroRunAppearance052.prepare(id,s,tick);near(HeroRunAppearance052.babyPowerGrowth084(s),0,.000001f,"Power decrease retained stale features");
  }
  private static void motion()throws Exception{
    var clips=json("animations/official/chao.animation.json").getAsJsonObject("animations");var fresh=new OfficialChaoRig(clips,key->asset(key,true));var old=new OfficialChaoRig(clips,key->asset(key,false));int serial=0;
    for(float alignment:new float[]{-1,0,1})for(float age:new float[]{0,.5f,.99f})for(var motion:List.of(OfficialChaoRig.Motion.CRAWL,OfficialChaoRig.Motion.WALK,OfficialChaoRig.Motion.SIT,OfficialChaoRig.Motion.HELD))for(int tick:new int[]{0,10,20}){
      var s=state(age,1,alignment);UUID id=new UUID(84,++serial);HeroRunAppearance052.prepare(id,s,tick);String saved=s.encode();
      for(var rig:List.of(old,fresh)){rig.prepareDevelopment("baby",s);rig.animateGameplay("baby",id,motion,tick,1,.05f,0,0,0,0,false);rig.development(s);check(rig.finite(),"Production Lionfish pose became nonfinite");}
      for(String joint:List.of("skull","body","arm_left","arm_right","foot_left","foot_right","wing_left","wing_right","tail")){var a=new PoseStack();var b=new PoseStack();old.transformBone(joint,a);fresh.transformBone(joint,b);check(a.last().pose().equals(b.last().pose()),"Shared equipment/animal/toy anchor changed: "+joint);}
      for(boolean left:new boolean[]{false,true}){float[] a=old.toyPalm(left),b=fresh.toyPalm(left);check(a!=null&&b!=null,"Toy grip disappeared");for(int i=0;i<a.length;i++)near(a[i],b[i],.000001f,"Compact toy palm changed");}
      check(saved.equals(s.encode()),"Animated rig changed saves");motionFrames++;
    }
  }
  private static void paint(){
    int body=HeroRunRegionPaint055.rgb(30,200,220),accent=HeroRunRegionPaint055.rgb(230,210,80);
    for(boolean adult:new boolean[]{false,true})for(boolean natural:new boolean[]{false,true})for(boolean two:new boolean[]{false,true})for(float alignment:new float[]{-1,0,1}){
      var input=new HeroRunRegionPaint055.Input(natural,two,adult,Math.max(0,alignment),Math.max(0,-alignment),0,.5f,0,body,accent,accent,accent,0xffeeeeee,accent,0xff333333,accent);
      var c=BabyPowerRegionPaint084.colors(input,HeroRunRegionPaint055.colors(input));
      for(int flags:new int[]{0,HeroRunRegionPaint055.SKIN,HeroRunRegionPaint055.ACCENT|HeroRunRegionPaint055.SKIN,HeroRunRegionPaint055.WING,HeroRunRegionPaint055.MARKER,HeroRunRegionPaint055.PROTECTED})for(int alpha:new int[]{0,63,128,255}){
        int original=alpha<<24|0x6599bb;
        check(BabyPowerRegionPaint084.pixel(input,c,0,12,5,original,flags,255)==original,"Zero Power changes original pixel");
        int changed=BabyPowerRegionPaint084.pixel(input,c,1,12,5,original,flags,255);
        check(changed>>>24==alpha,"Power paint changes source alpha");
        if(adult||alpha==0||flags==HeroRunRegionPaint055.PROTECTED)check(changed==original,"Adult/transparent/facial source pixel changed");
        if(!natural&&(flags&HeroRunRegionPaint055.SKIN)!=0)check(changed==original,"Inherited body hue replaced by natural Power pink");
      }
    }
  }
  public static void main(String[] args)throws Exception{
    geometry();edits();motion();paint();String report="Baby Power084: "+checks+" checks; "+frames+" emitted age/signed-training/alignment cases and "+motionFrames+" actual Lionfish pose cases; closed connected cuboids, fixed head/eyes/body/limbs, orb clearance, zero-Power and adult identity, edit/reversal continuity, exact joint/toy anchors and protected pixel alpha passed. Numerical checks, not visual/shader/multiplayer acceptance.\n";
    Files.createDirectories(Path.of("build/reports/lionfish"));Files.writeString(Path.of("build/reports/lionfish/baby-power084-results.txt"),report);System.out.print(report);
  }
}
