package com.chaogarden.client;

import com.chaogarden.entity.ChaoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

/** Pixel face glyphs fitted to the actual native skull landmarks. */
final class ChaoExpressionLayer025 extends RenderLayer<ChaoEntity,ChaoModel> {
  static final float MOUTH_DROP076=.25f;
  private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath(
      "chao_garden","textures/entity/chao/expression_stroke025.png");
  ChaoExpressionLayer025(RenderLayerParent<ChaoEntity,ChaoModel> parent){super(parent);}

  @Override public void render(PoseStack stack,MultiBufferSource buffers,int light,ChaoEntity chao,
      float swing,float amount,float partial,float age,float yaw,float pitch) {
    var model=getParentModel();var face=model.resolvedFace076();
    if(face==null||face.nativeExcluded()||chao.officialComparison()||!model.officialActive()||chao.isInvisible())return;
    float[] eyes=model.accessoryEyes();if(eyes==null||eyes[3]<=0)return;
    stack.pushPose();model.transformAccessoryAnchor("skull",stack);
    float alpha=chao.state().monsterAlpha();
    var vertices=buffers.getBuffer(alpha<1?RenderType.entityTranslucent(WHITE):RenderType.entityCutoutNoCull(WHITE));
    emitFace076(stack,vertices,light,face,eyes,model.toyMouth(),alpha);
    stack.popPose();
    if(face.eyes()==com.chaogarden.core.ChaoFaceRules076.Eyes.SLEEP) {
      // SA2's sleeping indicator is a golden spiral, not an alert/question orb.
      stack.pushPose();model.transformAccessoryAnchor("emotion_dots",stack);
      var skull=model.officialPoses().get("skull");
      if(skull!=null)stack.mulPose(new org.joml.Quaternionf().rotationZYX(skull.rz,skull.ry,skull.rx).invert());
      float bob=(float)Math.sin(age*.055f)*.10f;
      for(int i=0;i<36;i++) {
        float a=i/36f,b=(i+1)/36f;
        float angleA=a*(float)Math.PI*4.25f,angleB=b*(float)Math.PI*4.25f;
        float ra=.10f+a*1.05f,rb=.10f+b*1.05f;
        stroke(stack,vertices,0x00f000f0,(float)Math.cos(angleA)*ra,-1+bob+(float)Math.sin(angleA)*ra,
            (float)Math.cos(angleB)*rb,-1+bob+(float)Math.sin(angleB)*rb,-.52f,.17f,0xffffd564);
      }
      stack.popPose();
    }
  }

  /** Shared production emitter for numerical checks using the actual rig's skull-local anchors. */
  static void emitFace076(PoseStack stack,VertexConsumer vertices,int light,
      com.chaogarden.core.ChaoFaceRules076.Resolved face,float[] eyes,float[] mouth,float alpha) {
    if(face==null||face.nativeExcluded()||!finiteAnchor(eyes,4)||eyes[3]<=0||!Float.isFinite(alpha))return;
    alpha=Math.max(0,Math.min(1,alpha));
    var glyphPose=stack.last();var glyphNormal=glyphPose.transformNormal(new Vector3f(0,0,-1),new Vector3f());
    var glyphVertex=new Vector3f();
    float scale=Math.min(1,eyes[3]*.5f),z=eyes[2]-.025f;
    for(int side:new int[]{-1,1}) {
      float x=eyes[0]+side*eyes[3];
      for(var pixel:ChaoFaceGlyphs076.eye(face,side))
        pixel(glyphPose,glyphNormal,glyphVertex,vertices,light,x+pixel.x()*scale,eyes[1]+pixel.y()*scale,z+pixel.depth(),
            pixel.width()*scale,pixel.height()*scale,fade(pixel.color(),alpha));
    }
    if(finiteAnchor(mouth,3))for(var pixel:ChaoFaceGlyphs076.mouth(face.mouth()))
      pixel(glyphPose,glyphNormal,glyphVertex,vertices,light,mouth[0]+pixel.x(),mouth[1]+MOUTH_DROP076+pixel.y(),mouth[2]-.02f+pixel.depth(),
          pixel.width(),pixel.height(),fade(pixel.color(),alpha));
  }

  private static boolean finiteAnchor(float[] anchor,int count) {
    if(anchor==null||anchor.length<count)return false;
    for(int i=0;i<count;i++)if(!Float.isFinite(anchor[i]))return false;
    return true;
  }

  private static int fade(int color,float alpha) {
    return color&0x00ffffff|Math.round((color>>>24)*alpha)<<24;
  }

  private static void pixel(PoseStack.Pose pose,Vector3f normal,Vector3f vertex,VertexConsumer vertices,int light,
      float x,float y,float z,float width,float height,int color) {
    pixelVertex(pose,normal,vertex,vertices,light,x,y,z,0,0,color);
    pixelVertex(pose,normal,vertex,vertices,light,x+width,y,z,1,0,color);
    pixelVertex(pose,normal,vertex,vertices,light,x+width,y+height,z,1,1,color);
    pixelVertex(pose,normal,vertex,vertices,light,x,y+height,z,0,1,color);
  }

  private static void pixelVertex(PoseStack.Pose pose,Vector3f normal,Vector3f vertex,VertexConsumer vertices,
      int light,float x,float y,float z,float u,float v,int color) {
    pose.pose().transformPosition(x/16f,y/16f,z/16f,vertex);
    vertices.addVertex(vertex.x,vertex.y,vertex.z,color,u,v,OverlayTexture.NO_OVERLAY,light,normal.x,normal.y,normal.z);
  }
  private static void stroke(PoseStack stack,VertexConsumer vertices,int light,
      float x1,float y1,float x2,float y2,float z,float thickness,int color) {
    float length=(float)Math.hypot(x2-x1,y2-y1);if(length<.0001f)return;
    float nx=-(y2-y1)*thickness/(length*2),ny=(x2-x1)*thickness/(length*2);
    float[][] points={{x1+nx,y1+ny,0,0},{x2+nx,y2+ny,1,0},
        {x2-nx,y2-ny,1,1},{x1-nx,y1-ny,0,1}};
    var pose=stack.last();var normal=pose.transformNormal(new Vector3f(0,0,-1),new Vector3f());
    for(float[] p:points) {
      var v=pose.pose().transformPosition(p[0]/16f,p[1]/16f,z/16f,new Vector3f());
      vertices.addVertex(v.x,v.y,v.z,color,p[2],p[3],OverlayTexture.NO_OVERLAY,light,
          normal.x,normal.y,normal.z);
    }
  }
}
