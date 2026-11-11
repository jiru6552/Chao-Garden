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

/** Tiny skull-local eyelid strokes fitted to the actual native eye landmarks. */
final class ChaoExpressionLayer025 extends RenderLayer<ChaoEntity,ChaoModel> {
  private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath(
      "chao_garden","textures/entity/chao/expression_stroke025.png");
  ChaoExpressionLayer025(RenderLayerParent<ChaoEntity,ChaoModel> parent){super(parent);}

  @Override public void render(PoseStack stack,MultiBufferSource buffers,int light,ChaoEntity chao,
      float swing,float amount,float partial,float age,float yaw,float pitch) {
    var model=getParentModel();int expression=model.eyeExpression();
    if(expression==0||chao.officialComparison()||!model.officialActive()||chao.isInvisible())return;
    float[] eyes=model.accessoryEyes();if(eyes==null||eyes[3]<=0)return;
    stack.pushPose();model.transformAccessoryAnchor("skull",stack);
    var vertices=buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
    float width=Math.min(1,eyes[3]*.48f),z=eyes[2]-.025f;
    for(int side:new int[]{-1,1}) {
      float x=eyes[0]+side*eyes[3],y=eyes[1];
      if(expression==2) {
        // > <, the distinctive squeezed-eye water distress in the supplied clip.
        float tip=x-side*width*.10f,outer=x+side*width*.85f;
        stroke(stack,vertices,light,outer,y-width*.55f,tip,y,z,.20f);
        stroke(stack,vertices,light,tip,y,outer,y+width*.55f,z,.20f);
      } else if(expression==1) {
        stroke(stack,vertices,light,x-width*.85f,y+width*.20f,x-width*.40f,y-width*.30f,z,.19f);
        stroke(stack,vertices,light,x-width*.40f,y-width*.30f,x+width*.40f,y-width*.30f,z,.19f);
        stroke(stack,vertices,light,x+width*.40f,y-width*.30f,x+width*.85f,y+width*.20f,z,.19f);
      } else {
        // A quiet sleep arc; a blink uses a smaller nearly horizontal line.
        float bend=expression==3?width*.18f:width*.04f;
        stroke(stack,vertices,light,x-width*.83f,y-bend*.5f,x,y+bend,z,.18f);
        stroke(stack,vertices,light,x,y+bend,x+width*.83f,y-bend*.5f,z,.18f);
      }
    }
    stack.popPose();
    if(expression==3&&chao.isResting()) {
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

  private static void stroke(PoseStack stack,VertexConsumer vertices,int light,
      float x1,float y1,float x2,float y2,float z,float thickness) {
    stroke(stack,vertices,light,x1,y1,x2,y2,z,thickness,0xff17232b);
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
