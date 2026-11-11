package com.chaogarden.activities;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** One item lays a seven-square tatami carpet; the center owns its single recoverable item. */
public final class KarateMatBlock extends ActivityBlock {
  public static final IntegerProperty TILE=IntegerProperty.create("tile",0,49);
  public static final int LEGACY=49,CENTER=24;
  public static final MapCodec<KarateMatBlock> CODEC=simpleCodec(KarateMatBlock::new);
  private static final ThreadLocal<Boolean> REMOVING=ThreadLocal.withInitial(()->false);
  public KarateMatBlock(Properties p){super(Kind.MAT,p);registerDefaultState(defaultBlockState().setValue(TILE,LEGACY));}
  @Override protected MapCodec<? extends KarateMatBlock> codec(){return CODEC;}
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> b){super.createBlockStateDefinition(b);b.add(TILE);}
  public static BlockPos offset(int tile){return new BlockPos(tile%7-3,0,tile/7-3);}
  public static BlockPos center(BlockPos pos,BlockState state){return state.getValue(TILE)==LEGACY?pos:pos.subtract(offset(state.getValue(TILE)));}
  @Override public BlockState getStateForPlacement(BlockPlaceContext context){
    var level=context.getLevel();var center=context.getClickedPos();
    for(int i=0;i<49;i++){
      var pos=center.offset(offset(i));
      if(!level.hasChunkAt(pos)||!level.isInWorldBounds(pos)||!level.getWorldBorder().isWithinBounds(pos)
          ||!level.getBlockState(pos).canBeReplaced(context)||level.getBlockEntity(pos)!=null
          ||!level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP)
          ||context.getPlayer()!=null&&!context.getPlayer().mayUseItemAt(pos,Direction.UP,context.getItemInHand()))return null;
    }
    return defaultBlockState().setValue(TILE,CENTER);
  }
  @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.LivingEntity placer,net.minecraft.world.item.ItemStack stack){
    if(state.getValue(TILE)!=CENTER)return;
    for(int i=0;i<49;i++)if(i!=CENTER)level.setBlock(pos.offset(offset(i)),state.setValue(TILE,i),3);
  }
  @Override protected BlockState updateShape(BlockState state,Direction d,BlockState neighbor,net.minecraft.world.level.LevelAccessor level,BlockPos pos,BlockPos other){
    if(state.getValue(TILE)!=LEGACY)level.scheduleTick(pos,this,1);return state;
  }
  @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random){
    if(state.getValue(TILE)==LEGACY)return;
    var root=center(pos,state);var first=level.getBlockState(root);
    if(!first.is(this)||first.getValue(TILE)!=CENTER){level.removeBlock(pos,false);return;}
    for(int i=0;i<49;i++){
      var at=root.offset(offset(i));if(!level.hasChunkAt(at))return;var tile=level.getBlockState(at);
      if(!tile.is(this)||tile.getValue(TILE)!=i||!level.getBlockState(at.below()).isFaceSturdy(level,at.below(),Direction.UP)){level.destroyBlock(root,true);return;}
    }
  }
  @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.player.Player player){
    int tile=state.getValue(TILE);
    if(!level.isClientSide&&tile!=CENTER&&tile!=LEGACY){var root=center(pos,state);var first=level.getBlockState(root);
      if(first.is(this)&&first.getValue(TILE)==CENTER){if(!player.isCreative())dropResources(first,level,root,null,player,player.getMainHandItem());level.removeBlock(root,false);}
    }
    return super.playerWillDestroy(level,pos,state,player);
  }
  @Override protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,
      net.minecraft.world.level.storage.loot.LootParams.Builder params){
    int tile=state.getValue(TILE);
    if(tile==CENTER||tile==LEGACY)return super.getDrops(state,params);
    var origin=params.getParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN);
    var root=center(BlockPos.containing(origin),state);var first=params.getLevel().getBlockState(root);
    if(!first.is(this)||first.getValue(TILE)!=CENTER)return java.util.List.of();
    // Non-player destruction asks for loot before onRemove clears the assembly.
    // Use the center's existing loot table with the original explosion/tool
    // context. No-drop removal and creative harvesting never enter this path.
    return super.getDrops(first,params);
  }
  @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
    if(!state.is(next.getBlock())&&!level.isClientSide&&state.getValue(TILE)!=LEGACY&&!REMOVING.get()){
      REMOVING.set(true);try{
        var root=center(pos,state);
        // Removing any tile clears only its own assembly. Loot is handled before removal.
        for(int i=0;i<49;i++){var at=root.offset(offset(i));var sibling=level.getBlockState(at);if(!at.equals(pos)&&sibling.is(this)&&sibling.getValue(TILE)==i)level.setBlock(at,Blocks.AIR.defaultBlockState(),35);}
      }finally{REMOVING.remove();}
    }
    super.onRemove(state,level,pos,next,moving);
  }
  @Override protected BlockState rotate(BlockState state,net.minecraft.world.level.block.Rotation rotation){
    int t=state.getValue(TILE);if(t==LEGACY)return state;
    var v=offset(t).rotate(rotation);return state.setValue(TILE,(v.getZ()+3)*7+v.getX()+3);
  }
  @Override protected BlockState mirror(BlockState state,net.minecraft.world.level.block.Mirror mirror){
    int t=state.getValue(TILE);if(t==LEGACY)return state;var p=offset(t);int x=p.getX(),z=p.getZ();
    if(mirror==net.minecraft.world.level.block.Mirror.LEFT_RIGHT)z=-z;if(mirror==net.minecraft.world.level.block.Mirror.FRONT_BACK)x=-x;
    return state.setValue(TILE,(z+3)*7+x+3);
  }
}
