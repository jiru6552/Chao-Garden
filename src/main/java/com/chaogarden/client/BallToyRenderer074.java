package com.chaogarden.client;

import com.chaogarden.toys.*;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Matrix4f;

/** Continuous presentation of the committed real block, never a duplicate item/entity. */
@EventBusSubscriber(modid="chao_garden",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class BallToyRenderer074 implements BlockEntityRenderer<BallToyBlockEntity074> {
  private static final Map<BakedModel,float[][]> VERTICES=new IdentityHashMap<>();
  public BallToyRenderer074(BlockEntityRendererProvider.Context context){}
  @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(ToyContent.BALL_ENTITY.get(),BallToyRenderer074::new);}
  private static BlockState idle(BallToyBlockEntity074 ball){return ball.getBlockState().setValue(ChaoToyBlock.PLAYING,false).setValue(ChaoToyBlock.FRAME,0).setValue(ChaoBallBlock074.ROLLING,false);}
  private static BakedModel model(BallToyBlockEntity074 ball){return Minecraft.getInstance().getBlockRenderer().getBlockModel(idle(ball));}
  private static float[][] vertices(BakedModel model,BlockState state){
    if(VERTICES.size()>32)VERTICES.clear(); // Model reloads replace their identities.
    return VERTICES.computeIfAbsent(model,key->{
      var points=new ArrayList<float[]>();
      for(int side=-1;side<6;side++)for(var quad:key.getQuads(state,side<0?null:Direction.values()[side],RandomSource.create(74))){
        int[] data=quad.getVertices();int stride=data.length/4;
        for(int vertex=0;vertex<4;vertex++)points.add(new float[]{Float.intBitsToFloat(data[vertex*stride]),Float.intBitsToFloat(data[vertex*stride+1]),Float.intBitsToFloat(data[vertex*stride+2])});
      }
      return points.toArray(float[][]::new);
    });
  }
  public static double supportHeight(BallToyBlockEntity074 ball,float partial){
    double angle=Math.PI*2*ball.progress(partial),cos=Math.cos(angle),sin=Math.sin(angle),minimum=Double.POSITIVE_INFINITY;
    var direction=ball.rollDirection();double ax=direction.getStepZ(),az=-direction.getStepX();
    for(float[] v:vertices(model(ball),idle(ball))){double x=v[0]-.5,y=v[1]-BallPlayRules074.RADIUS,z=v[2]-.5;minimum=Math.min(minimum,y*cos+(az*x-ax*z)*sin);}
    return Double.isFinite(minimum)?-minimum:BallPlayRules074.RADIUS;
  }
  /** Exact matrix consumed by render; block-local units make numerical client checks possible. */
  public static Matrix4f modelTransform(BallToyBlockEntity074 ball,float partial){
    var offset=ball.offset(partial);var direction=ball.rollDirection();float radius=(float)BallPlayRules074.RADIUS;
    return new Matrix4f().translate((float)offset.x,(float)(supportHeight(ball,partial)-radius),(float)offset.z)
        .translate(.5f,radius,.5f).rotate((float)(Math.PI*2*ball.progress(partial)),direction.getStepZ(),0,-direction.getStepX()).translate(-.5f,-radius,-.5f);
  }
  public static Vec3 visualCenter(BallToyBlockEntity074 ball,float partial){return Vec3.atLowerCornerOf(ball.getBlockPos()).add(.5,supportHeight(ball,partial),.5).add(ball.offset(partial));}
  @Override public AABB getRenderBoundingBox(BallToyBlockEntity074 ball){return new AABB(ball.getBlockPos()).inflate(1,.3,1);}
  @Override public void render(BallToyBlockEntity074 ball,float partial,PoseStack stack,MultiBufferSource buffers,int light,int overlay){
    var state=ball.getBlockState();if(!state.getValue(ChaoToyBlock.PLAYING)&&!state.getValue(ChaoBallBlock074.ROLLING))return;
    stack.pushPose();try{stack.mulPose(modelTransform(ball,partial));Minecraft.getInstance().getBlockRenderer().renderSingleBlock(idle(ball),stack,buffers,light,overlay);}finally{stack.popPose();}
  }
}
