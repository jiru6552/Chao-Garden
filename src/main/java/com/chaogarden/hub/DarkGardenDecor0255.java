package com.chaogarden.hub;

import com.chaogarden.ChaoGarden;
import com.mojang.serialization.MapCodec;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Original native materials for the Battle-style pale shore and graveyard garden. */
public final class DarkGardenDecor0255 {
  private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,ChaoGarden.MODID);
  private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,ChaoGarden.MODID);
  private static final Map<String,DeferredHolder<Block,? extends Block>> BLOCK_MAP=new LinkedHashMap<>();
  private static final Map<String,DeferredHolder<Item,BlockItem>> ITEM_MAP=new LinkedHashMap<>();
  public static final List<String> IDS=List.of("dark_garden_soil","dark_garden_rock","dark_garden_deadwood",
      "dark_garden_gravestone","dark_garden_fence","dark_garden_effigy");
  static {
    for(String id:IDS){
      var block=BLOCKS.register(id,()->switch(id){
        case "dark_garden_soil"->new Block(properties().sound(SoundType.SAND));
        case "dark_garden_deadwood"->new Deadwood(properties().sound(SoundType.WOOD).noOcclusion());
        case "dark_garden_gravestone"->new Gravestone(properties().noOcclusion());
        case "dark_garden_fence"->new FenceBlock(properties().sound(SoundType.METAL).noOcclusion());
        case "dark_garden_effigy"->new Effigy(properties());
        default->new Block(properties());
      });
      BLOCK_MAP.put(id,block);ITEM_MAP.put(id,ITEMS.register(id,()->new BlockItem(block.get(),new Item.Properties())));
    }
  }
  private static BlockBehaviour.Properties properties(){return BlockBehaviour.Properties.of().strength(2).sound(SoundType.STONE);}
  private DarkGardenDecor0255(){}
  public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);}
  public static Block block(String id){var value=BLOCK_MAP.get(id);if(value==null)throw new IllegalArgumentException(id);return value.get();}
  public static Item item(String id){return ITEM_MAP.get(id).get();}
  public static List<String> ids(){return IDS;}

  /** A thin physical bare trunk, not an invisible full-cube obstruction. */
  public static class Deadwood extends RotatedPillarBlock {
    public static final MapCodec<Deadwood> CODEC=simpleCodec(Deadwood::new);
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty BRANCHED=
        net.minecraft.world.level.block.state.properties.BooleanProperty.create("branched");
    private static final VoxelShape JOINT=Shapes.or(box(6,0,6,10,16,10),box(0,6,6,16,10,10),box(6,6,0,10,10,16));
    public Deadwood(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(BRANCHED,false));}
    @Override public MapCodec<? extends RotatedPillarBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(BRANCHED);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
      if(state.getValue(BRANCHED))return JOINT;
      return switch(state.getValue(AXIS)){case X->box(0,6,6,16,10,10);case Z->box(6,6,0,10,10,16);default->box(6,0,6,10,16,10);};
    }
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return getShape(state,level,pos,context);}
  }

  /** Rounded one-block gravestone with matched native silhouette. */
  public static final class Gravestone extends HorizontalDirectionalBlock {
    public static final MapCodec<Gravestone> CODEC=simpleCodec(Gravestone::new);
    private static final VoxelShape NORTH=stone(false),EAST=stone(true);
    public Gravestone(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.SOUTH));}
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return rotate(s,m.getRotation(s.getValue(FACING)));}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return s.getValue(FACING).getAxis()==Direction.Axis.X?EAST:NORTH;}
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return getShape(s,l,p,c);}
    private static VoxelShape stone(boolean turn){
      VoxelShape shape=Shapes.empty();
      for(int[] row:new int[][]{{1,15,0,3},{3,13,3,12},{4,12,12,14},{5,11,14,15},{6,10,15,16}}){
        var box=turn?Block.box(5,row[2],row[0],11,row[3],row[1]):Block.box(row[0],row[2],5,row[1],row[3],11);
        shape=Shapes.joinUnoptimized(shape,box,BooleanOp.OR);
      }
      return shape.optimize();
    }
  }

  /** One registry ID holds ordinary black rock and the nine face tiles on the distant crag. */
  public static final class Effigy extends HorizontalDirectionalBlock {
    public static final MapCodec<Effigy> CODEC=simpleCodec(Effigy::new);
    public static final IntegerProperty TILE=IntegerProperty.create("tile",0,9);
    public Effigy(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.SOUTH).setValue(TILE,0));}
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,TILE);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return rotate(s,m.getRotation(s.getValue(FACING)));}
  }
}
