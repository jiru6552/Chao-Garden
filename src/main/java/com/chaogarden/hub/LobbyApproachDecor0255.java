package com.chaogarden.hub;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.LobbyApproachGeometry0255;
import com.mojang.serialization.MapCodec;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Internal finite collision cells for the curved celestial treads and connected rails. */
public final class LobbyApproachDecor0255 {
  public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(ChaoGarden.MODID);
  public static final DeferredBlock<FloorStairs> FLOOR_STAIRS=BLOCKS.register("lobby_floor_stairs",()->new FloorStairs(
      BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(-1,3600000)
          .noOcclusion().noLootTable().dynamicShape().sound(SoundType.GLASS).isViewBlocking((s,l,p)->false).isSuffocating((s,l,p)->false)));
  private LobbyApproachDecor0255(){}
  public static void register(IEventBus bus){BLOCKS.register(bus);}
  public static final class FloorStairs extends Block {
    public static final MapCodec<FloorStairs> CODEC=simpleCodec(FloorStairs::new);
    private static final Map<BlockPos,VoxelShape> SHAPES=new java.util.concurrent.ConcurrentHashMap<>();
    public FloorStairs(Properties properties){super(properties);}
    @Override protected MapCodec<? extends Block> codec(){return CODEC;}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return shape(pos);}
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return shape(pos);}
    private static VoxelShape shape(BlockPos pos){return SHAPES.computeIfAbsent(pos.immutable(),p->{
      var shapes=new java.util.ArrayList<VoxelShape>();for(var b:LobbyApproachGeometry0255.nativeBoxes(p.getX(),p.getY(),p.getZ()))shapes.add(Shapes.box(b.x0(),b.y0(),b.z0(),b.x1(),b.y1(),b.z1()));
      if(shapes.isEmpty())return Shapes.empty();
      while(shapes.size()>1){var next=new java.util.ArrayList<VoxelShape>();for(int i=0;i<shapes.size();i+=2)next.add(i+1<shapes.size()?Shapes.or(shapes.get(i),shapes.get(i+1)):shapes.get(i));shapes=next;}
      return shapes.getFirst().optimize();
    });}
  }
}
