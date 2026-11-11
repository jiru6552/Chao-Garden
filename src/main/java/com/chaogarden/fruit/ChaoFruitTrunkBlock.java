package com.chaogarden.fruit;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
/** Same slim trunk and shake-to-harvest interaction as the authored garden palms. */
public final class ChaoFruitTrunkBlock extends RotatedPillarBlock {
  public ChaoFruitTrunkBlock(Properties p){super(p);}
  @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return s.getValue(AXIS)==Direction.Axis.Y?Block.box(4,0,4,12,16,12):super.getShape(s,l,p,c);}
  @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
    return harvestAbove(l,p);
  }
  static InteractionResult harvestAbove(Level l,BlockPos p){
    if(l.isClientSide)return InteractionResult.SUCCESS;
    for(int y=0;y<=12;y++){var at=p.above(y);var crown=l.getBlockState(at);
      if(crown.getBlock() instanceof ChaoTreeCrownBlock tree){
        boolean ripe=false;for(var pod:FruitContent.podPositions(l,at,tree.fruit)){var ps=l.getBlockState(pod);if(ps.getBlock() instanceof HangingFruitBlock f)ripe|=f.harvest(l,pod,ps);}
        l.playSound(null,p,net.minecraft.sounds.SoundEvents.AZALEA_LEAVES_STEP,net.minecraft.sounds.SoundSource.BLOCKS,.8f,ripe?.8f:1.2f);return InteractionResult.SUCCESS;
      }
      if(!FruitContent.isTrunk(crown))break;
    }return InteractionResult.PASS;
  }
}
