package com.chaogarden.client;

import com.chaogarden.ChaoVariants;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.joml.Vector3f;

/** A broken upper shell surrounds the whole skull, with two real eye openings.
 * Shell UVs sample the top half of the same continuous placed-egg atlas. */
final class EggshellMask {
  private final IdentityHashMap<CustomData,ChaoState> coats=new IdentityHashMap<>();
  private final ArrayDeque<CustomData> order=new ArrayDeque<>();
  private final Vector3f normal=new Vector3f(),point=new Vector3f();
  private record Shape(float x0,float y0,float z0,float x1,float y1,float z1,float ex,float ey,float ez,float eh) {}
  private final Map<Shape,float[]> geometry=new LinkedHashMap<>(16,.75f,true) {
    @Override protected boolean removeEldestEntry(Map.Entry<Shape,float[]> entry){return size()>64;}
  };

  private ChaoState coat(ItemStack hat,ChaoEntity wearer) {
    CustomData component=hat.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY);
    ChaoState stored=coats.get(component);
    if(stored!=null)return stored;
    String encoded=component.copyTag().getString("EggShellCoat");
    if(encoded.isEmpty())return wearer.originEggCoat();
    stored=ChaoState.decode(encoded);
    if(order.size()>=64)coats.remove(order.removeFirst());
    order.addLast(component);coats.put(component,stored);return stored;
  }

  void render(ChaoModel model,ChaoEntity wearer,ItemStack hat,PoseStack stack,
      MultiBufferSource buffers,int light) {
    ChaoState coat=coat(hat,wearer);
    ResourceLocation texture=ResourceLocation.fromNamespaceAndPath("chao_garden",
        "textures/entity/eggs/"+ChaoEggRenderer.textureKey(coat)+".png");
    boolean portal="end".equals(ChaoVariants.activeId(coat));
    stack.pushPose();
    try {
      model.transformAccessoryAnchor("skull",stack);
      float[] head=model.accessoryBounds("skull"),eyes=model.accessoryEyes();
      if(head==null)head=new float[]{-3.5f,-6,-3,3.5f,0,3};
      if(eyes==null)eyes=new float[]{(head[0]+head[3])*.5f,head[1]+(head[4]-head[1])*2/3,head[2]-.06f,1.55f};
      draw(stack,buffers.getBuffer(portal?RenderType.endPortal():RenderType.entityCutoutNoCull(texture)),light,head,eyes);
      if(!portal&&ChaoShine.visible(coat))draw(stack,buffers.getBuffer(ChaoShine.surface(texture)),light,head,eyes);
    } finally {stack.popPose();}
  }

  private void draw(PoseStack stack,VertexConsumer vertices,int light,float[] h,float[] eyes) {
    Shape key=new Shape(h[0],h[1],h[2],h[3],h[4],h[5],eyes[0],eyes[1],eyes[2],eyes[3]);
    float[] data=geometry.computeIfAbsent(key,unused->build(h,eyes));
    for(int i=0;i<data.length;i+=8) {
      stack.last().pose().transformPosition(data[i]/16,data[i+1]/16,data[i+2]/16,point);
      normal.set(data[i+5],data[i+6],data[i+7]);stack.last().transformNormal(normal,normal);normal.normalize();
      vertices.addVertex(point.x,point.y,point.z,-1,data[i+3],data[i+4],OverlayTexture.NO_OVERLAY,
          light,normal.x,normal.y,normal.z);
    }
  }

  float[] inspectGeometry(float[] h,float[] eyes) {
    Shape key=new Shape(h[0],h[1],h[2],h[3],h[4],h[5],eyes[0],eyes[1],eyes[2],eyes[3]);
    return geometry.computeIfAbsent(key,unused->build(h,eyes));
  }

  private float[] build(float[] h,float[] eyes) {
    var vertices=new ArrayList<Float>();
    float width=h[3]-h[0],height=h[4]-h[1],depth=h[5]-h[2];
    float centerX=(h[0]+h[3])*.5f,centerZ=(h[2]+h[5])*.5f;
    float rx=width*.6f+.55f,rz=depth*.6f+.55f,top=h[1]-1.4f,rim=h[4]+.3f;
    float eyeHalfWidth=Math.max(.9f,width*.17f),eyeHalfHeight=Math.max(1.0f,height*.225f);
    float eyeTop=Math.max(h[1]+.2f,eyes[1]-eyeHalfHeight);
    float eyeBottom=Math.min(h[4]-.15f,eyes[1]+eyeHalfHeight);
    float[] ys={top,h[1]-1.2f,h[1]-.6f,h[1]-.12f,eyeTop,eyeBottom,rim};
    float[] scales={.20f,.58f,.86f,1,1,1,1};
    var angles=new ArrayList<Float>();
    for(int i=0;i<=64;i++)angles.add((float)(-Math.PI+Math.PI*2*i/64));
    // Exact boundaries prevent the mesh cells from covering the native irises.
    for(float side:new float[]{-1,1})for(float edge:new float[]{-1,1}) {
      float x=eyes[0]+side*eyes[3]+edge*eyeHalfWidth-centerX;
      float ratio=Math.max(-.98f,Math.min(.98f,x/rx));
      angles.add((float)Math.asin(Math.copySign(ratio*ratio,ratio)));
    }
    angles.sort(Comparator.naturalOrder());
    for(int i=0;i<angles.size()-1;i++) {
      float a=angles.get(i),b=angles.get(i+1);if(b-a<.00001f)continue;
      float[] apex={centerX,top-.05f,centerZ};
      quad(vertices,apex,position(a,top,scales[0],centerX,centerZ,rx,rz),
          position(b,top,scales[0],centerX,centerZ,rx,rz),apex,a,b,top,rim);
    }
    for(int row=0;row<ys.length-1;row++)for(int i=0;i<angles.size()-1;i++) {
      float a=angles.get(i),b=angles.get(i+1);if(b-a<.00001f)continue;
      float mid=(a+b)*.5f,x=centerX+shapeSin(mid)*rx;
      boolean eye=row==4&&Math.cos(mid)>0
          && (Math.abs(x-(eyes[0]-eyes[3]))<eyeHalfWidth||Math.abs(x-(eyes[0]+eyes[3]))<eyeHalfWidth);
      if(eye)continue;
      float[] p0=position(a,ys[row],scales[row],centerX,centerZ,rx,rz);
      float[] p1=position(a,ys[row+1]+(row==5?jag(a):0),scales[row+1],centerX,centerZ,rx,rz);
      float[] p2=position(b,ys[row+1]+(row==5?jag(b):0),scales[row+1],centerX,centerZ,rx,rz);
      float[] p3=position(b,ys[row],scales[row],centerX,centerZ,rx,rz);
      quad(vertices,p0,p1,p2,p3,a,b,top,rim);
    }
    // Open neck: a narrow broken lip supplies thickness without a flat cap.
    for(int i=0;i<angles.size()-1;i++) {
      float a=angles.get(i),b=angles.get(i+1);if(b-a<.00001f)continue;
      var p0=position(a,rim+jag(a),1,centerX,centerZ,rx,rz);
      var p1=position(b,rim+jag(b),1,centerX,centerZ,rx,rz);
      var p2=position(b,rim+jag(b),1,centerX,centerZ,rx-.18f,rz-.18f);
      var p3=position(a,rim+jag(a),1,centerX,centerZ,rx-.18f,rz-.18f);
      quad(vertices,p0,p1,p2,p3,a,b,top,rim);
    }
    float[] packed=new float[vertices.size()];for(int i=0;i<packed.length;i++)packed[i]=vertices.get(i);return packed;
  }
  private static float jag(float angle){return (((int)Math.floor((angle+Math.PI)*16/(Math.PI*2)))&1)==0?-.24f:.15f;}
  private static float shapeSin(float a){float s=(float)Math.sin(a);return Math.copySign((float)Math.sqrt(Math.abs(s)),s);}
  private static float shapeCos(float a){float c=(float)Math.cos(a);return Math.copySign((float)Math.sqrt(Math.abs(c)),c);}
  private static float[] position(float angle,float y,float scale,float cx,float cz,float rx,float rz){
    return new float[]{cx+shapeSin(angle)*rx*scale,y,cz-shapeCos(angle)*rz*scale};
  }
  private void quad(ArrayList<Float> out,float[] a,float[] b,float[] c,float[] d,
      float thetaA,float thetaB,float top,float rim) {
    normal.set(b[0]-a[0],b[1]-a[1],b[2]-a[2]).cross(c[0]-a[0],c[1]-a[1],c[2]-a[2]);
    if(normal.lengthSquared()<.0000001f)return;
    normal.normalize();
    float u0=(float)(.5+(thetaA+Math.PI)/(Math.PI*2)*63),u1=(float)(.5+(thetaB+Math.PI)/(Math.PI*2)*63);
    emit(out,a,u0,top,rim);emit(out,b,u0,top,rim);emit(out,c,u1,top,rim);emit(out,d,u1,top,rim);
  }
  private void emit(ArrayList<Float> out,float[] v,float u,float top,float rim){
    float textureV=.5f+Math.max(0,Math.min(1,(v[1]-top)/(rim-top)))*31;
    for(float value:new float[]{v[0],v[1],v[2],u/64,textureV/64,normal.x,normal.y,normal.z})out.add(value);
  }
}
