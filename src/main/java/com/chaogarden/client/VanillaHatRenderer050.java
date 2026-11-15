package com.chaogarden.client;

import com.chaogarden.market.ChaoHats;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

/** Native vanilla surfaces around the animated skull, with real iris ports.
 * Only the Chao's worn adapter changes; vanilla inventory/block models are retained. */
final class VanillaHatRenderer050 {
  private static final ResourceLocation SKULL=ResourceLocation.withDefaultNamespace("textures/entity/skeleton/skeleton.png");
  private final Vector3f point=new Vector3f(),normal=new Vector3f();
  private record UV(float u0,float v0,float u1,float v1) {}
  static boolean supports(String kind){return kind.equals(ChaoHats.PUMPKIN)||kind.equals(ChaoHats.SKULL);}
  static float[] fittedBounds(float[] head) {
    return ChaoAccessoryFit.coveredHead(head);
  }
  private static UV sprite(String name) {
    var texture=Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
        .apply(ResourceLocation.withDefaultNamespace("block/"+name));
    return new UV(texture.getU0(),texture.getV0(),texture.getU1(),texture.getV1());
  }
  void render(ChaoModel model,String kind,PoseStack stack,MultiBufferSource buffers,int light) {
    float[] h=model.accessoryBounds("skull");
    if(h==null)h=new float[]{-3.5f,-6,-3,3.5f,0,3};
    float[] b=fittedBounds(h);var ports=ChaoHeadCover079.eyePorts(h,model.accessoryEyes());
    boolean skull=kind.equals(ChaoHats.SKULL);
    var vertices=buffers.getBuffer(RenderType.entityCutoutNoCull(skull?SKULL:TextureAtlas.LOCATION_BLOCKS));
    UV front=skull?new UV(8/64f,8/32f,16/64f,16/32f):sprite("carved_pumpkin");
    UV left=skull?new UV(0,8/32f,8/64f,16/32f):sprite("pumpkin_side");
    UV right=skull?new UV(16/64f,8/32f,24/64f,16/32f):left;
    UV back=skull?new UV(24/64f,8/32f,32/64f,16/32f):left;
    UV top=skull?new UV(8/64f,0,16/64f,8/32f):sprite("pumpkin_top");
    stack.pushPose();
    try {
      model.transformAccessoryAnchor("skull",stack);
      float x=b[0],y=b[1],z=b[2],X=b[3],Y=b[4],Z=b[5];
      // Five front panels leave two actual openings around the full native irises.
      front(stack,vertices,light,b,front,ports,!skull,x,X,y,ports.top());
      front(stack,vertices,light,b,front,ports,!skull,x,X,ports.bottom(),Y);
      front(stack,vertices,light,b,front,ports,!skull,x,ports.left0(),ports.top(),ports.bottom());
      front(stack,vertices,light,b,front,ports,!skull,ports.left1(),ports.right0(),ports.top(),ports.bottom());
      front(stack,vertices,light,b,front,ports,!skull,ports.right1(),X,ports.top(),ports.bottom());
      quad(stack,vertices,light,new float[][]{{X,y,Z},{X,Y,Z},{x,Y,Z},{x,y,Z}},back,0,0,1,1,0,0,1);
      quad(stack,vertices,light,new float[][]{{x,y,Z},{x,Y,Z},{x,Y,z},{x,y,z}},left,0,0,1,1,-1,0,0);
      quad(stack,vertices,light,new float[][]{{X,y,z},{X,Y,z},{X,Y,Z},{X,y,Z}},right,0,0,1,1,1,0,0);
      quad(stack,vertices,light,new float[][]{{x,y,Z},{x,y,z},{X,y,z},{X,y,Z}},top,0,0,1,1,0,-1,0);
      // The neck stays open, as on the custom full-head shells.
    } finally {stack.popPose();}
  }
  private void front(PoseStack stack,VertexConsumer out,int light,float[] b,UV uv,ChaoHeadCover079.Ports ports,boolean carved,float x,float X,float y,float Y) {
    if(X<=x||Y<=y)return;
    float[] range=frontUv(b,ports,carved,x,X,y,Y);
    quad(stack,out,light,new float[][]{{x,y,b[2]},{x,Y,b[2]},{X,Y,b[2]},{X,y,b[2]}},uv,range[0],range[1],range[2],range[3],0,0,-1);
  }
  /** Native pumpkin eyes occupy rows 2..7, mouth rows 8..15. Put the
   * painted eyes wholly inside the real ports instead of above the irises. */
  static float[] frontUv(float[] b,ChaoHeadCover079.Ports ports,boolean carved,float x,float X,float y,float Y) {
    if(!carved)return new float[]{(x-b[0])/(b[3]-b[0]),(y-b[1])/(b[4]-b[1]),
        (X-b[0])/(b[3]-b[0]),(Y-b[1])/(b[4]-b[1])};
    return new float[]{pumpkinU(x,b,ports),pumpkinV(y,b,ports),pumpkinU(X,b,ports),pumpkinV(Y,b,ports)};
  }
  private static float map(float value,float low,float high,float from,float to) {
    return from+(value-low)/(high-low)*(to-from);
  }
  private static float pumpkinU(float x,float[] b,ChaoHeadCover079.Ports ports) {
    if(x<=ports.left0())return map(x,b[0],ports.left0(),0,3/16f);
    if(x<=ports.left1())return map(x,ports.left0(),ports.left1(),3/16f,7/16f);
    if(x<=ports.right0())return map(x,ports.left1(),ports.right0(),7/16f,9/16f);
    if(x<=ports.right1())return map(x,ports.right0(),ports.right1(),9/16f,14/16f);
    return map(x,ports.right1(),b[3],14/16f,1);
  }
  private static float pumpkinV(float y,float[] b,ChaoHeadCover079.Ports ports) {
    if(y<=ports.top())return map(y,b[1],ports.top(),0,2/16f);
    if(y<=ports.bottom())return map(y,ports.top(),ports.bottom(),2/16f,8/16f);
    return map(y,ports.bottom(),b[4],8/16f,1);
  }
  private void quad(PoseStack stack,VertexConsumer out,int light,float[][] points,UV uv,
      float u0,float v0,float u1,float v1,float nx,float ny,float nz) {
    float[][] coords={{u0,v0},{u0,v1},{u1,v1},{u1,v0}};
    normal.set(nx,ny,nz);stack.last().transformNormal(normal,normal);normal.normalize();
    for(int i=0;i<4;i++) {
      float[] p=points[i];stack.last().pose().transformPosition(p[0]/16,p[1]/16,p[2]/16,point);
      float u=uv.u0()+(uv.u1()-uv.u0())*coords[i][0],v=uv.v0()+(uv.v1()-uv.v0())*coords[i][1];
      out.addVertex(point.x,point.y,point.z,-1,u,v,OverlayTexture.NO_OVERLAY,light,normal.x,normal.y,normal.z);
    }
  }
}
