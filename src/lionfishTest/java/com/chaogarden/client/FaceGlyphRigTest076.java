package com.chaogarden.client;

import com.chaogarden.core.ChaoFaceRules076;
import com.chaogarden.core.ChaoFaceRules076.Eyes;
import com.chaogarden.core.ChaoFaceRules076.Mouth;
import com.chaogarden.core.ChaoFaceRules076.Resolved;
import com.chaogarden.core.ChaoFaceRules076.Signals;
import com.chaogarden.core.ChaoFaceRules076.Source;
import com.chaogarden.core.ChaoFaceRules076.Style;
import com.chaogarden.core.ChaoState;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Actual production glyph emission and registered-rig attachments; no client or image rendering. */
public final class FaceGlyphRigTest076 {
  private static final Path ROOT=Path.of("src/main/resources/assets/chao_garden/models/entity");
  private static final int INK=0xff17232b,WHITE=0xfff8fbff;
  private static final int[] PIGMENTS={INK,0xff4a9df1,0xff48bd67,0xffdf505c,0xffffd852,
      0xff9b68d9,0xffee8ab5,0xffef9b47,0xff48d5d2};
  private static final Map<String,ChaoMeshAsset> ASSETS=new HashMap<>();
  private static final Map<ChaoMeshAsset,JsonObject> DATA=new IdentityHashMap<>();
  private static JsonObject clips;
  private static long checks,quads,attachmentCopies;
  private static int faceCases,anatomyCases,nativeCases;
  private static float maxAnchorError;

  private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
  private static void near(float actual,float expected,float epsilon,String why){check(Float.isFinite(actual)&&Math.abs(actual-expected)<=epsilon,why+" actual="+actual+" expected="+expected);}
  private static ChaoMeshAsset load(String key){
    return ASSETS.computeIfAbsent(key,k->{try{
      var data=JsonParser.parseString(Files.readString(ROOT.resolve(k+".json"))).getAsJsonObject();
      var mesh=ChaoMeshAsset.parse(data);DATA.put(mesh,data);return mesh;
    }catch(Exception e){throw new IllegalStateException(k,e);}});
  }
  private static Matrix4f at(OfficialChaoRig rig,String bone){var stack=new PoseStack();rig.transformBone(bone,stack);return new Matrix4f(stack.last().pose());}
  private static OfficialChaoRig rig(ChaoState state,int serial){
    var rig=new OfficialChaoRig(clips,FaceGlyphRigTest076::load);String key=OfficialChaoRig.geometryKey(state);
    rig.prepareDevelopment(key,state);
    rig.animateGameplay(key,new UUID(760076,serial),OfficialChaoRig.Motion.WALK,120,0,0,0,0,17,-9,false,false);
    rig.development(state);check(rig.finite(),"Registered rig must remain finite "+key);return rig;
  }
  private static ChaoState state(boolean adult,int family,int first,float maturity){
    var state=new ChaoState();state.adult=adult;state.align=family;state.primaryType=first;
    state.levels[ChaoState.RUN]=12;state.levels[ChaoState.POWER]=12;
    state.levels[ChaoState.SWIM]=9;state.progress[ChaoState.SWIM]=60;
    state.setEvolutionStrength(maturity*1.2f);state.setBaselineFace076(0,0);
    return ChaoState.decode(state.encode());
  }
  private static Resolved face(Eyes eyes,Mouth mouth,Style style,int iris){
    return new Resolved(0,0,eyes,mouth,style,Source.BASELINE,Source.BASELINE,false,iris);
  }
  private static final class Vertex {
    final Vector3f point;Vector3f normal;int color;float u,v;boolean colored,uv;
    Vertex(float x,float y,float z){point=new Vector3f(x*16,y*16,z*16);}
  }
  private static final class Capture implements VertexConsumer {
    final List<Vertex> vertices=new ArrayList<>();
    private Vertex last(){return vertices.get(vertices.size()-1);}
    public VertexConsumer addVertex(float x,float y,float z){vertices.add(new Vertex(x,y,z));return this;}
    public VertexConsumer setColor(int r,int g,int b,int a){var vertex=last();vertex.color=a<<24|r<<16|g<<8|b;vertex.colored=true;return this;}
    public VertexConsumer setUv(float u,float v){var vertex=last();vertex.u=u;vertex.v=v;vertex.uv=true;return this;}
    public VertexConsumer setUv1(int u,int v){return this;}
    public VertexConsumer setUv2(int u,int v){return this;}
    public VertexConsumer setNormal(float x,float y,float z){last().normal=new Vector3f(x,y,z);return this;}
    int count(){return vertices.size();}
  }
  private static Capture emit(OfficialChaoRig rig,Resolved face,float[] eyes,float[] mouth,float alpha){
    var stack=new PoseStack();rig.transformBone("skull",stack);var capture=new Capture();
    var matrix=new Matrix4f(stack.last().pose());
    ChaoExpressionLayer025.emitFace076(stack,capture,0x00f000f0,face,eyes,mouth,alpha);
    check(matrix.equals(stack.last().pose()),"Face emission changed the caller's skull transform");return capture;
  }
  private static void quads(Capture capture,OfficialChaoRig rig,Resolved face,float[] eyes,float[] mouth,int alpha,String label){
    check(capture.count()>0&&capture.count()%4==0,"Complete visible face quads "+label);
    var inverse=at(rig,"skull").invert();
    var core=rig.sampledMesh().partBounds("skull__cube_0",rig.poses());
    check(core!=null,"Registered base head exists for face-depth clearance "+label);
    var coreToSkull=new Matrix4f(inverse).mul(at(rig,"skull__cube_0"));float skullFront=Float.POSITIVE_INFINITY;
    for(int corner=0;corner<8;corner++){
      var point=new Vector3f(core[(corner&1)==0?0:3],core[(corner&2)==0?1:4],core[(corner&4)==0?2:5]).div(16);
      coreToSkull.transformPosition(point);skullFront=Math.min(skullFront,point.z*16);
    }
    var left=ChaoFaceGlyphs076.eye(face,-1);var right=ChaoFaceGlyphs076.eye(face,1);
    var lips=mouth==null?List.<ChaoFaceGlyphs076.Pixel>of():ChaoFaceGlyphs076.mouth(face.mouth());
    var pixels=new ArrayList<ChaoFaceGlyphs076.Pixel>();pixels.addAll(left);pixels.addAll(right);pixels.addAll(lips);
    check(capture.count()==pixels.size()*4,"Each authored pixel emits exactly one quad "+label);
    int eyeVertices=4*(left.size()+right.size());float[] actualDepth=new float[pixels.size()];
    float[] mouthMinY={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY},mouthMaxY={Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
    for(int i=0;i<capture.count();i+=4){
      boolean eyeQuad=i<eyeVertices;String channel=eyeQuad?"Eye":"Mouth";
      check(eyeQuad||mouth!=null,"A mouth quad requires its own authored landmark "+label);
      float plane=eyeQuad?eyes[2]:mouth[2];var pixel=pixels.get(i/4);
      check(pixel.depth()==0||pixel.depth()==.002f,"Only the literal foreground/backing depth planes are permitted "+label);
      var a=capture.vertices.get(i);var b=capture.vertices.get(i+1);var c=capture.vertices.get(i+2);var d=capture.vertices.get(i+3);
      for(var vertex:List.of(a,b,c,d)){
        check(vertex.point.isFinite()&&vertex.normal!=null&&vertex.normal.isFinite(),"Finite emitted position and normal "+label);
        check(vertex.colored&&vertex.color>>>24==alpha&&vertex.uv&&Float.isFinite(vertex.u)&&Float.isFinite(vertex.v),"Complete opaque/alpha/UV attributes "+label);
        check((vertex.color&0x00ffffff)==(pixel.color()&0x00ffffff),"Actual emission preserves the required glyph pigment "+label);
        near(vertex.normal.length(),1,.0001f,"Unit glyph normal "+label);
        var local=new Vector3f(vertex.point).div(16);inverse.transformPosition(local);local.mul(16);
        if(!eyeQuad){int layer=pixel.depth()==0?0:1;mouthMinY[layer]=Math.min(mouthMinY[layer],local.y);mouthMaxY[layer]=Math.max(mouthMaxY[layer],local.y);}
        check(Math.abs(local.x-eyes[0])<=eyes[3]+1.1f&&Math.abs(local.y-eyes[1])<4,"Face stays inside the original skull-front envelope "+label);
        // Mouth6 sits behind the raised eye panels, while both remain in front of the base head.
        near(local.z,plane-(eyeQuad?.025f:.02f)+pixel.depth(),.00005f,channel+" quad follows its explicit foreground/backing plane "+label);
        if(vertex==a)actualDepth[i/4]=local.z;
        float clearance=skullFront-local.z;
        check(clearance>=.015f&&clearance<=.25f,channel+" quad clears the actual base head without floating "+label
            +" z="+local.z+" eyePlane="+eyes[2]+" mouthPlane="+(mouth==null?"none":mouth[2])+" skullFront="+skullFront+" clearance="+clearance);
      }
      var ab=new Vector3f(b.point).sub(a.point);var ad=new Vector3f(d.point).sub(a.point);var cross=ab.cross(ad,new Vector3f());
      check(cross.lengthSquared()>1e-9f&&cross.lengthSquared()<1,"Nondegenerate bounded pixel quad "+label);
      check(cross.dot(a.normal)<0,"Consistent front-facing glyph winding without inversion "+label);
      near(new Vector3f(c.point).sub(b.point).distance(new Vector3f(d.point).sub(a.point)),0,.00005f,"Rectangular pixel corners "+label);quads++;
    }
    contrastSeparation(left,actualDepth,0,"left eye/"+label);
    contrastSeparation(right,actualDepth,left.size(),"right eye/"+label);
    contrastSeparation(lips,actualDepth,left.size()+right.size(),"mouth/"+label);
    for(int layer=0;layer<2;layer++)if(Float.isFinite(mouthMinY[layer])){
      float glyphMin=Float.POSITIVE_INFINITY,glyphMax=Float.NEGATIVE_INFINITY;
      for(var pixel:lips)if((pixel.depth()==0?0:1)==layer){glyphMin=Math.min(glyphMin,pixel.y());glyphMax=Math.max(glyphMax,pixel.y()+pixel.height());}
      float renderedOrigin=(mouthMinY[layer]+mouthMaxY[layer]-glyphMin-glyphMax)*.5f;
      near(renderedOrigin,mouth[1]+.25f,.00005f,"Both rendered mouth layers sit0.25pixels below the unchanged mouth6 anchor "+label+" layer="+layer);
    }
  }
  /** White backing must cover the ink and sit strictly behind it in the actual emitted geometry. */
  private static void contrastSeparation(List<ChaoFaceGlyphs076.Pixel> pixels,float[] actualDepth,int offset,String label){
    if(pixels.stream().noneMatch(p->p.depth()==.002f))return;
    for(int i=0;i<pixels.size();i++){
      var ink=pixels.get(i);if(ink.color()!=INK)continue;
      check(ink.depth()==0,"Dark strokes occupy the foreground plane "+label);
      boolean overlap=false;
      for(int j=0;j<pixels.size();j++){
        var backing=pixels.get(j);if(backing.depth()!=.002f)continue;
        check(backing.color()==WHITE,"Contrast backing remains light "+label);
        if(Math.min(ink.x()+ink.width(),backing.x()+backing.width())-Math.max(ink.x(),backing.x())<=.00001f
            ||Math.min(ink.y()+ink.height(),backing.y()+backing.height())-Math.max(ink.y(),backing.y())<=.00001f)continue;
        overlap=true;float separation=actualDepth[offset+j]-actualDepth[offset+i];
        check(separation>0,"Overlapping light backing cannot be coplanar with or in front of dark ink "+label);
        near(separation,.002f,.00005f,"Literal0.002-pixel raster-fight prevention gap "+label);
      }
      check(overlap,"Every dark stroke has contrast backing "+label);
      for(float[] sample:new float[][]{{.01f,.01f},{ink.width()-.01f,.01f},{.01f,ink.height()-.01f},
          {ink.width()-.01f,ink.height()-.01f},{ink.width()/2,ink.height()/2}}){
        float x=ink.x()+sample[0],y=ink.y()+sample[1];boolean covered=false;
        for(var backing:pixels)if(backing.depth()==.002f&&backing.color()==WHITE
            &&x>=backing.x()&&x<=backing.x()+backing.width()&&y>=backing.y()&&y<=backing.y()+backing.height()){covered=true;break;}
        check(covered,"Light backing covers the interior and corners of the dark stroke "+label);
      }
    }
  }
  private static Set<Integer> colors(List<ChaoFaceGlyphs076.Pixel> pixels){var result=new HashSet<Integer>();for(var pixel:pixels)result.add(pixel.color());return result;}
  private static void bounded(List<ChaoFaceGlyphs076.Pixel> pixels,boolean eye,String label){
    for(var pixel:pixels){
      check(Float.isFinite(pixel.x())&&Float.isFinite(pixel.y())&&Float.isFinite(pixel.width())&&Float.isFinite(pixel.height())
          &&(pixel.depth()==0||pixel.depth()==.002f),"Finite cached glyph on exactly the two permitted planes "+label);
      check(pixel.width()>0&&pixel.width()<=.25f&&pixel.height()>0&&pixel.height()<=.25f&&pixel.color()>>>24==255,"Opaque positive pixel rectangle "+label);
      float x=eye?1.01f:.90f,y=eye?1.06f:.50f;
      check(pixel.x()>=-x&&pixel.x()+pixel.width()<=x&&pixel.y()>=-y&&pixel.y()+pixel.height()<=y,"Bounded glyph silhouette "+label);
    }
  }
  private static void palette(Resolved face,List<ChaoFaceGlyphs076.Pixel> pixels,String label){
    var colors=colors(pixels);check(!pixels.isEmpty(),"Every baseline eye remains visible "+label);
    if(face.eyes()==Eyes.HAPPY_CLOSED){check(colors.equals(Set.of(INK,WHITE)),"Closed eyes keep white backing and dark strokes without an iris "+label);return;}
    if(face.irisColor()==11){check(colors.equals(Set.of(0xff48d979)),"Chaos lens11 remains a solid green eye "+label);return;}
    if(face.eyes()==Eyes.CHAOS&&face.irisColor()<9){
      int pigment=face.irisColor()==0?0xff48d979:PIGMENTS[face.irisColor()];
      check(colors.equals(Set.of(pigment)),"Chaos baseline preserves a solid inherited pigment "+label);return;
    }
    boolean hero=face.style()==Style.HERO||face.style()==Style.HERO_LENS;
    boolean dark=face.style()==Style.DARK||face.style()==Style.DARK_LENS;
    check(colors.contains(hero?0xff101820:dark?0xff35afea:WHITE),"Neutral white / Hero black / Dark blue sclera cannot invert "+label);
    int iris=face.irisColor();int pupil=iris>=1&&iris<=8?PIGMENTS[iris]:hero||dark?WHITE:INK;
    check(colors.contains(pupil),"An open eye retains its contrasting pupil and explicit iris colour "+label);
    check(colors.size()>=2,"An ordinary open eye cannot become blind/monochrome "+label);
  }
  private static void baselineGrid(){
    for(int family=-1;family<=1;family++){
      var state=state(true,family,ChaoState.NORMAL,.5f);var rig=rig(state,100+family);var eyes=rig.attachmentEyes();var mouth=rig.attachmentMouth();
      check(eyes!=null&&eyes[3]>0&&mouth!=null,"Actual family eye and toy-mouth landmarks exist");
      var eyeBefore=eyes.clone();var mouthBefore=mouth.clone();var skull=at(rig,"skull");
      for(int eye=0;eye<5;eye++)for(int lips=0;lips<5;lips++)for(int iris=0;iris<=11;iris++){
        state.setBaselineFace076(eye,lips);state.eyeColor=iris;String saved=state.encode();
        var face=ChaoFaceRules076.resolve(state,Signals.quiet());String label="family="+family+" eye="+eye+" mouth="+lips+" iris="+iris;
        check(!face.nativeExcluded(),"Ordinary family uses the new single face "+label);
        if(iris==9)check(face.style()==Style.HERO_LENS,"Lens9 explicitly selects the Hero family");
        if(iris==10)check(face.style()==Style.DARK_LENS,"Lens10 explicitly selects the Dark family");
        if(iris==11)check(face.style()==Style.CHAOS_LENS,"Lens11 explicitly selects the Chaos family");
        for(int side:new int[]{-1,1}){var pixels=ChaoFaceGlyphs076.eye(face,side);bounded(pixels,true,label);palette(face,pixels,label);}
        var lipsPixels=ChaoFaceGlyphs076.mouth(face.mouth());bounded(lipsPixels,false,label);
        check((lips==0)==lipsPixels.isEmpty(),"No-mouth baseline emits exactly zero mouth quads "+label);
        var capture=emit(rig,face,eyes,mouth,1);quads(capture,rig,face,eyes,mouth,255,label);
        int expected=4*(ChaoFaceGlyphs076.eye(face,-1).size()+ChaoFaceGlyphs076.eye(face,1).size()+lipsPixels.size());
        check(capture.count()==expected,"Exactly one pair of eyes and one selected mouth emitted "+label);
        check(saved.equals(state.encode()),"Glyph resolution/emission cannot write care, genetics or baseline "+label);faceCases++;
      }
      check(Arrays.equals(eyeBefore,eyes)&&Arrays.equals(mouthBefore,mouth)&&skull.equals(at(rig,"skull")),"Face choices cannot move landmarks or reshape the head");
      var alphaFace=face(Eyes.NORMAL,Mouth.SMILE,Style.NEUTRAL,0);
      var alpha=emit(rig,alphaFace,eyes,mouth,.5f);quads(alpha,rig,alphaFace,eyes,mouth,128,"half-alpha");
    }
    check(faceCases==900,"Bounded complete5eyes×5mouths×3families×12irises cohort");
  }
  private static float inkY(List<ChaoFaceGlyphs076.Pixel> pixels,boolean middle){
    float sum=0;int count=0;for(var p:pixels)if(p.color()==INK){float x=p.x()+p.width()/2;
      if(middle?Math.abs(x)<.31f:Math.abs(x)>.50f){sum+=p.y()+p.height()/2;count++;}}
    check(count>0,"Silhouette has both middle and outer dark strokes");return sum/count;
  }
  private static float squeezeX(List<ChaoFaceGlyphs076.Pixel> pixels,boolean middle){
    float sum=0;int count=0;for(var p:pixels)if(p.color()==INK){float y=p.y()+p.height()/2;
      if(middle?Math.abs(y)<.11f:Math.abs(y)>.49f){sum+=p.x()+p.width()/2;count++;}}
    check(count>0,"Distress silhouette has an inner point and outer arms");return sum/count;
  }
  private static void semanticSilhouettes(){
    for(Style style:List.of(Style.NEUTRAL,Style.HERO,Style.DARK,Style.HERO_LENS,Style.DARK_LENS,Style.CHAOS_LENS)){
      for(Eyes eye:List.of(Eyes.HAPPY_CLOSED,Eyes.SLEEP,Eyes.BLINK,Eyes.DISTRESS)){
        var reference=ChaoFaceGlyphs076.eye(face(eye,Mouth.NONE,Style.NEUTRAL,0),-1);
        for(int iris:new int[]{0,8,9,10,11}){
          var actual=ChaoFaceGlyphs076.eye(face(eye,Mouth.NONE,style,iris),-1);
          check(actual.equals(reference)&&colors(actual).equals(Set.of(INK,WHITE)),"Closed/sleep/distress marks stay legible and never turn into pupils for "+style+"/"+iris);
        }
      }
      var happy=ChaoFaceGlyphs076.eye(face(Eyes.HAPPY_CLOSED,Mouth.NONE,style,0),-1);
      var sleep=ChaoFaceGlyphs076.eye(face(Eyes.SLEEP,Mouth.NONE,style,0),-1);
      check(inkY(happy,true)<inkY(happy,false),"Happy arc rises at its centre; it cannot invert into a sad lid");
      check(inkY(sleep,true)>inkY(sleep,false),"Sleeping lid settles downward at its centre");
      var left=ChaoFaceGlyphs076.eye(face(Eyes.DISTRESS,Mouth.NONE,style,0),-1);
      var right=ChaoFaceGlyphs076.eye(face(Eyes.DISTRESS,Mouth.NONE,style,0),1);
      check(squeezeX(left,true)>squeezeX(left,false)&&squeezeX(right,true)<squeezeX(right,false),"Distress must face inward as >< rather than inverted <>");
      check(ChaoFaceGlyphs076.eye(face(Eyes.HALF_CLOSED,Mouth.NONE,style,0),-1).size()<ChaoFaceGlyphs076.eye(face(Eyes.NORMAL,Mouth.NONE,style,0),-1).size(),"Half-closed silhouette actually narrows its opening");
    }
    var smile=ChaoFaceGlyphs076.mouth(Mouth.SMILE);var frown=ChaoFaceGlyphs076.mouth(Mouth.FROWN);
    check(inkY(smile,true)>inkY(smile,false)&&inkY(frown,true)<inkY(frown,false),"Smile/frown corners have opposite readable curvature");
    check(ChaoFaceGlyphs076.mouth(Mouth.NONE).isEmpty(),"None has no hidden mouth backing");
    check(ChaoFaceGlyphs076.mouth(Mouth.GRIN).stream().anyMatch(p->p.color()==WHITE&&p.width()<.2f),"Grin retains authored white teeth, separate from its larger contrast backing");
    check(!ChaoFaceGlyphs076.mouth(Mouth.WAVY).equals(smile)&&!ChaoFaceGlyphs076.mouth(Mouth.WAVY).equals(frown),"Wavy is a separate silhouette");
    for(Mouth mouth:Mouth.values())bounded(ChaoFaceGlyphs076.mouth(mouth),false,"mouth/"+mouth);
    var ordinary=ChaoFaceGlyphs076.eye(face(Eyes.NORMAL,Mouth.NONE,Style.NEUTRAL,0),-1);
    for(int invalid:new int[]{Integer.MIN_VALUE,-1,12,Integer.MAX_VALUE})
      check(ChaoFaceGlyphs076.eye(face(Eyes.NORMAL,Mouth.NONE,Style.NEUTRAL,invalid),-1).equals(ordinary),"Invalid/retired iris IDs fall back to the ordinary eye");
  }
  private static Set<String> materialBones(OfficialChaoRig rig,String prefix){
    var names=new HashSet<String>();for(var element:DATA.get(rig.sampledMesh()).getAsJsonArray("bones")){
      var bone=element.getAsJsonObject();for(var mesh:bone.getAsJsonArray("meshes"))
        if(mesh.getAsJsonObject().get("material").getAsString().startsWith(prefix))names.add(bone.get("name").getAsString());
    }return names;
  }
  private static Capture visibleGroup(OfficialChaoRig rig,Set<String> names){
    var before=new HashMap<String,Boolean>();rig.poses().forEach((name,pose)->before.put(name,pose.visible));
    try{rig.poses().forEach((name,pose)->{if(!names.contains(name))pose.visible=false;});var capture=new Capture();rig.render(new PoseStack(),capture,0x00f000f0,0,0xffffffff);return capture;}
    finally{rig.poses().forEach((name,pose)->pose.visible=before.get(name));}
  }
  private static Map<String,float[]> dimensions(OfficialChaoRig rig){
    var result=new HashMap<String,float[]>();rig.poses().forEach((name,p)->result.put(name,new float[]{p.x,p.y,p.z,p.rx,p.ry,p.rz,p.sx,p.sy,p.sz,p.reach,p.proportionY}));return result;
  }
  private static void attachmentCopies(OfficialChaoRig rig,String label){
    for(String folder:List.of("hats/","overlays/")){
      var target=load(folder+"chao_"+rig.sourceGeometryKey());var poses=target.createPoses();
      for(var entry:poses.entrySet())if(rig.pose(entry.getKey())!=null)rig.copyAttachmentPoseTo(entry.getKey(),target,entry.getValue());
      for(String joint:List.of("skull","arm_left","arm_right","foot_left"))if(rig.pose(joint)!=null&&poses.containsKey(joint)){
        var stack=new PoseStack();target.transformBone(joint,stack,poses);var difference=new Matrix4f(at(rig,joint)).invert().mul(stack.last().pose());
        float error=new Vector3f(difference.m30(),difference.m31(),difference.m32()).length()*16;maxAnchorError=Math.max(maxAnchorError,error);
        check(difference.equals(new Matrix4f(),.00005f),"Hat/animal attachment joint remains bound to the same pose "+label+"/"+folder+joint);attachmentCopies++;
      }
    }
  }
  private static void anatomy(ChaoState state,String label,int serial){
    var rig=rig(state,serial);var before=dimensions(rig);var eyes=rig.attachmentEyes();var mouth=rig.attachmentMouth();
    var head=rig.sampledMesh().partBounds("skull__cube_0",rig.poses());
    check(head!=null,"Original registered head core exists "+label);
    for(int axis=0;axis<3;axis++)near(head[axis+3]-head[axis],axis==0?7:6,.00005f,"Fixed original7×6×6 head box "+label);
    check(eyes!=null&&eyes[3]>0&&mouth!=null,"Valid actual face landmarks "+label);
    var mouthBox=rig.sampledMesh().partBounds("expression_mouth_6",rig.poses());
    check(mouthBox!=null,"The original open-mouth6 instrument landmark remains authored "+label);
    var relative=at(rig,"skull").invert().mul(at(rig,"expression_mouth_6"));var low=new Vector3f(Float.POSITIVE_INFINITY);var high=new Vector3f(Float.NEGATIVE_INFINITY);
    for(int corner=0;corner<8;corner++){var point=new Vector3f(mouthBox[(corner&1)==0?0:3],mouthBox[(corner&2)==0?1:4],mouthBox[(corner&4)==0?2:5]).div(16);relative.transformPosition(point);point.mul(16);low.min(point);high.max(point);}
    near(mouth[0],(low.x+high.x)/2,.0001f,"Toy mouth x stays on actual expression6 "+label);
    near(mouth[1],(low.y+high.y)/2,.0001f,"Toy mouth y stays on actual expression6 "+label);
    near(mouth[2],low.z,.0001f,"Toy mouth depth stays on actual expression6 "+label);
    var donorEyes=materialBones(rig,"lens");var donorMouth=materialBones(rig,"expression_");
    check(!donorEyes.isEmpty()&&donorEyes.size()==2,"Exactly two authored native eye panels "+label);
    rig.sampledMesh().lens(0xff17232b);rig.eyesVisible(true);check(visibleGroup(rig,donorEyes).count()>0,"Native eye panels exist before replacement "+label);
    rig.eyesVisible(false);rig.expression(0);
    check(visibleGroup(rig,donorEyes).count()==0&&visibleGroup(rig,donorMouth).count()==0,"No native open eyes or authored second mouth remain under replacement "+label);
    for(Eyes eye:Eyes.values()){
      var result=face(eye,Mouth.OPEN,state.adult&&state.align>0?Style.HERO:state.adult&&state.align<0?Style.DARK:Style.NEUTRAL,0);
      quads(emit(rig,result,eyes,mouth,1),rig,result,eyes,mouth,255,label+"/"+eye);
    }
    var after=dimensions(rig);
    for(var entry:before.entrySet())check(Arrays.equals(entry.getValue(),after.get(entry.getKey())),"Expressions cannot alter body/head dimensions or joint scales "+label+"/"+entry.getKey());
    check(Arrays.equals(head,rig.sampledMesh().partBounds("skull__cube_0",rig.poses()))&&Arrays.equals(eyes,rig.attachmentEyes())&&Arrays.equals(mouth,rig.attachmentMouth()),"Expression replacement preserves fixed head and all face attachment anchors "+label);
    attachmentCopies(rig,label);anatomyCases++;
  }
  private static void nativeBypass(){
    var states=new ArrayList<ChaoState>();for(int family=-1;family<=1;family++){var state=state(true,family,0,.5f);state.immortal=true;states.add(state);}
    for(String character:List.of("omochao","tails","knuckles","amy")){var state=state(true,0,0,.5f);state.character=character;states.add(ChaoState.decode(state.encode()));}
    int serial=2000;for(var state:states){
      var rig=rig(state,serial++);var before=dimensions(rig);var resolved=ChaoFaceRules076.resolve(state,Signals.quiet());
      check(resolved.nativeExcluded()&&resolved.style()==Style.NATIVE,"Chaos/robot/character anatomy stays native "+state.character+"/"+state.align);
      check(emit(rig,resolved,rig.attachmentEyes(),rig.attachmentMouth(),1).count()==0,"Native face bypass emits zero replacement glyphs");
      var after=dimensions(rig);
      for(var entry:before.entrySet())check(Arrays.equals(entry.getValue(),after.get(entry.getKey())),"Bypass cannot move native anatomy");nativeCases++;
    }
    var state=state(false,0,0,0);var rig=rig(state,3000);var normal=face(Eyes.NORMAL,Mouth.SMILE,Style.NEUTRAL,0);var eyes=rig.attachmentEyes();var mouth=rig.attachmentMouth();
    for(float[] invalid:new float[][]{null,new float[0],new float[]{0,0,0,0},new float[]{0,0,Float.NaN,2},new float[]{0,0,0,-1}})
      check(emit(rig,normal,invalid,mouth,1).count()==0,"Invalid eye landmark cannot emit corrupt geometry");
    check(emit(rig,normal,eyes,mouth,Float.NaN).count()==0,"Nonfinite alpha cannot emit geometry");
    check(emit(rig,null,eyes,mouth,1).count()==0,"An unresolved face cannot emit geometry");
    check(emit(rig,ChaoFaceRules076.resolve(state,Signals.nativeFace()),eyes,mouth,1).count()==0,"An explicit native/comparison signal bypasses the replacement");
    var eyesOnly=emit(rig,normal,eyes,null,1);check(eyesOnly.count()>0,"Missing mouth landmark preserves the valid eyes");
  }
  public static void main(String[] args)throws Exception{
    near(ChaoExpressionLayer025.MOUTH_DROP076,.25f,0,"Requested mouth-only visual drop stays exactly one quarter model pixel");
    clips=JsonParser.parseString(Files.readString(ROOT.getParent().getParent().resolve("animations/official/chao.animation.json"))).getAsJsonObject().getAsJsonObject("animations");
    semanticSilhouettes();baselineGrid();int serial=1000;
    for(int family=-1;family<=1;family++)for(int first=0;first<=4;first++)for(float maturity:new float[]{0,.5f,1})anatomy(state(true,family,first,maturity),"adult/"+family+"/"+first+"/"+maturity,serial++);
    for(int family=-1;family<=1;family++)for(float maturity:new float[]{0,1})anatomy(state(false,family,0,maturity),"baby/"+family+"/"+maturity,serial++);
    nativeBypass();
    System.out.println("Face glyph rig076: "+checks+" checks; "+faceCases+" complete baseline/family/iris cases; "+anatomyCases+" actual baby/adult/morph head cases; "+nativeCases+" native Chaos/robot/character bypasses; "+quads+" emitted pixel quads; "+attachmentCopies+" actual hat/animal anchor copies; maxAnchorErrorPixels="+maxAnchorError+". Shared production emitter, fixed head and mouth6 anchors, closed silhouettes, family/lens palettes and native-panel suppression checked numerically; actual client visual/shader/multiplayer review remains separate.");
  }
}
