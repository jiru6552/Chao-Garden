package com.chaogarden.hub;

import com.chaogarden.ChaoGarden;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Native scenery pieces; no synthetic inventories, loot or player privileges. */
public final class LobbyDecor025 {
  public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(ChaoGarden.MODID);
  public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE,ChaoGarden.MODID);
  public static final DeferredBlock<GlassFloor> GLASS_FLOOR=BLOCKS.register("lobby_glass_floor",()->new GlassFloor(properties(MapColor.COLOR_BLUE).sound(SoundType.GLASS)));
  public static final DeferredBlock<SceneAnchor> SCENE_ANCHOR=BLOCKS.register("lobby_scene_anchor",()->new SceneAnchor(properties(MapColor.NONE).noCollission()));
  public static final DeferredBlock<Block> TURF=BLOCKS.register("garden_turf",()->new Block(terrainProperties(MapColor.GRASS).sound(SoundType.GRASS)));
  public static final DeferredBlock<Block> CLIFF=BLOCKS.register("garden_limestone",()->new Block(terrainProperties(MapColor.TERRACOTTA_WHITE).sound(SoundType.STONE)));
  public static final DeferredBlock<Block> SAND=BLOCKS.register("garden_sand",()->new Block(terrainProperties(MapColor.SAND).sound(SoundType.SAND)));
  public static final DeferredBlock<PalmTrunk> PALM_TRUNK=BLOCKS.register("garden_palm_trunk",()->new PalmTrunk(properties(MapColor.WOOD).sound(SoundType.WOOD)));
  public static final DeferredBlock<SceneAnchor> PALM_CROWN=BLOCKS.register("garden_palm_crown",()->new SceneAnchor(properties(MapColor.PLANT).noCollission().sound(SoundType.GRASS)));
  public static final DeferredBlock<GlassFloor> PALM_SUPPORT=BLOCKS.register("garden_palm_fruit_support",()->new GlassFloor(properties(MapColor.PLANT).noCollission()));
  public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<SceneEntity>> SCENE=ENTITIES.register("lobby_scene",()->BlockEntityType.Builder.of(SceneEntity::new,SCENE_ANCHOR.get(),PALM_CROWN.get()).build(null));
  private LobbyDecor025(){}
  private static BlockBehaviour.Properties terrainProperties(MapColor color){return BlockBehaviour.Properties.of().mapColor(color).strength(-1,3600000).noLootTable();}
  private static BlockBehaviour.Properties properties(MapColor color){return BlockBehaviour.Properties.of().mapColor(color).strength(-1,3600000).noOcclusion().noLootTable().isViewBlocking((s,l,p)->false).isSuffocating((s,l,p)->false);}
  public static void register(IEventBus bus){BLOCKS.register(bus);ENTITIES.register(bus);}
  public static final class GlassFloor extends Block {
    public GlassFloor(Properties p){super(p);}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
  }
  public static final class SceneAnchor extends BaseEntityBlock {
    public static final BooleanProperty PORTAL=BooleanProperty.create("portal");
    public static final DirectionProperty FACING=net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty STYLE=IntegerProperty.create("portal_style",0,5);
    private static final MapCodec<SceneAnchor> CODEC=simpleCodec(SceneAnchor::new);
    public SceneAnchor(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(PORTAL,false).setValue(FACING,Direction.SOUTH).setValue(STYLE,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(PORTAL,FACING,STYLE);}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new SceneEntity(pos,state);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.empty();}
  }
  public static final class SceneEntity extends BlockEntity {
    public SceneEntity(BlockPos p,BlockState s){super(SCENE.get(),p,s);}
    public AABB sceneBounds(){return getBlockState().is(PALM_CROWN.get())?new AABB(worldPosition).inflate(6,4,6):getBlockState().getValue(SceneAnchor.PORTAL)?new AABB(worldPosition).inflate(5,8,5):new AABB(worldPosition).inflate(140);}
  }
  public static final class PalmTrunk extends Block {
    public PalmTrunk(Properties p){super(p);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(4,0,4,12,16,12);}
    @Override protected boolean isPathfindable(BlockState state,net.minecraft.world.level.pathfinder.PathComputationType type){return false;}

    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
      if(level.isClientSide)return InteractionResult.SUCCESS;
      BlockPos crown=null;for(int y=0;y<=12;y++){var next=pos.above(y);if(level.getBlockState(next).is(PALM_CROWN.get())){crown=next;break;}if(y>0&&!level.getBlockState(next).is(this))break;}
      boolean ripe=false;
      if(crown!=null)for(Direction d:Direction.Plane.HORIZONTAL){var at=crown.below().relative(d);var pod=level.getBlockState(at);if(pod.getBlock() instanceof com.chaogarden.fruit.HangingFruitBlock fruit)ripe|=fruit.harvest(level,at,pod);}
      level.playSound(null,pos,SoundEvents.AZALEA_LEAVES_STEP,SoundSource.BLOCKS,.8f,ripe?.8f:1.2f);
      return InteractionResult.SUCCESS;
    }
  }
}
