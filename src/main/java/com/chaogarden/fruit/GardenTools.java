package com.chaogarden.fruit;

import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.core.ChaoState;
import com.chaogarden.hub.HubContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** Tool ownership is a learned Chao ability, separate from the placed shovel toy. */
public final class GardenTools {
  private GardenTools() {}
  public static String ability(ItemStack stack) {
    var id=BuiltInRegistries.ITEM.getKey(stack.getItem());
    if(!id.getNamespace().equals("chao_garden"))return "";
    return switch(id.getPath()) {
      case "chao_shovel" -> "reward.shovel";
      case "chao_watering_can" -> "reward.watering_can";
      default -> "";
    };
  }
  public static boolean isTool(ItemStack stack) { return !ability(stack).isEmpty(); }
  public static boolean canLearn(ChaoEntity chao,ItemStack stack) {
    String trait=ability(stack);
    return !trait.isEmpty()&&chao.state().intTrait(trait)==0&&!chao.state().sleeping&&!chao.state().dead
        &&ChaoState.decode(chao.state().encode()).setTrait(trait,1);
  }
  public static void learn(ChaoEntity chao,ItemStack stack) {
    String trait=ability(stack);if(!trait.isEmpty()){chao.state().setTrait(trait,1);chao.sync();}
  }
  public static boolean canPlant(ChaoEntity chao,String fruit) {
    return !FruitCatalog.isMushroom(fruit)&&chao.state().intTrait("reward.shovel")>0
        &&chao.state().points[ChaoState.RUN]>=50&&!chao.isStaying()
        &&!chao.isInWater()&&!chao.state().sleeping&&!chao.state().dead;
  }
  public static boolean plantingSpot(ServerLevel level,BlockPos pos,String fruit) {
    if(HubContent.isHubDimension(level.dimension())&&(!HubContent.isGardenDimension(level)
        ||!HubContent.insideGarden(level.dimension(),pos.getX(),pos.getZ())))return false;
    return !FruitCatalog.isMushroom(fruit)&&level.getWorldBorder().isWithinBounds(pos)
        &&level.getBlockState(pos).isAir()&&level.getMaxLocalRawBrightness(pos)>=9
        &&FruitContent.canPlantTree(level,pos,fruit)&&GardenTreeData.get(level).canPlant(level,pos);
  }
  /** Revalidate immediately before the real seed transaction. */
  public static boolean plant(ServerLevel level,BlockPos pos,String fruit) {
    if(!plantingSpot(level,pos,fruit))return false;
    if(!level.setBlock(pos,FruitContent.saplingForFruit(fruit).defaultBlockState(),3))return false;
    GardenTreeData.get(level).planted(pos,fruit);return true;
  }
  public static BlockPos root(ServerLevel level,BlockPos clicked) {
    var state=level.getBlockState(clicked);
    if(state.getBlock() instanceof ChaoSaplingBlock)return clicked;
    if(state.getBlock() instanceof ChaoTreeCrownBlock)return clicked.below(state.getValue(ChaoTreeCrownBlock.EXPANDED)?4:3);
    if(FruitContent.isTrunk(state)) {
      BlockPos at=clicked;
      for(int i=0;i<7&&FruitContent.isTrunk(level.getBlockState(at.below()));i++)at=at.below();
      for(int h:new int[]{4,3})if(level.getBlockState(at.above(h)).getBlock() instanceof ChaoTreeCrownBlock)return at;
    }
    var palm=com.chaogarden.hub.LobbyDecor025.PALM_TRUNK.get();
    var deadwood=com.chaogarden.hub.DarkGardenDecor0255.block("dark_garden_deadwood");
    if(state.is(palm)||state.is(com.chaogarden.hub.LobbyDecor025.PALM_CROWN.get())||state.is(deadwood)) {
      var trunk=state.is(deadwood)?deadwood:palm;BlockPos at=clicked;
      for(int i=0;i<12&&level.getBlockState(at.below()).is(trunk);i++)at=at.below();
      if(!permanentPods(level,at).isEmpty())return at;
    }
    return null;
  }
  private static java.util.List<BlockPos> permanentPods(ServerLevel level,BlockPos root) {
    var out=new java.util.ArrayList<BlockPos>();
    for(int y=1;y<=10;y++)for(var side:net.minecraft.core.Direction.Plane.HORIZONTAL) {
      BlockPos pos=root.above(y).relative(side);var pod=level.getBlockState(pos);
      if(pod.getBlock() instanceof HangingFruitBlock&&pod.getValue(HangingFruitBlock.FACING)==side.getOpposite()
          &&pod.canSurvive(level,pos))out.add(pos);
    }
    return out;
  }
  /** One watering grows a seed stage, or ripens all actual fruit on one tree. */
  public static boolean water(ServerLevel level,BlockPos clicked) {
    BlockPos root=root(level,clicked);if(root==null)return false;
    var data=GardenTreeData.get(level);
    if(!data.mayWater(root,level.getGameTime()))return false;
    var state=level.getBlockState(root);boolean changed=false;
    if(state.getBlock() instanceof ChaoSaplingBlock seed)changed=seed.advance(level,root,state);
    else if(!FruitContent.isTrunk(state)) {
      for(BlockPos at:permanentPods(level,root)) {
        var pod=level.getBlockState(at);
        if(pod.getValue(HangingFruitBlock.AGE)<3){level.setBlock(at,pod.setValue(HangingFruitBlock.AGE,3),3);changed=true;}
      }
    } else for(int h:new int[]{4,3}) {
      BlockPos crown=root.above(h);var top=level.getBlockState(crown);
      if(!(top.getBlock() instanceof ChaoTreeCrownBlock tree))continue;
      FruitContent.replenishPods(level,crown,tree.fruit);
      for(BlockPos at:FruitContent.podPositions(level,crown,tree.fruit)) {
        var pod=level.getBlockState(at);
        if(pod.is(FruitContent.podForFruit(tree.fruit))&&pod.getValue(HangingFruitBlock.AGE)<3) {
          level.setBlock(at,pod.setValue(HangingFruitBlock.AGE,3),3);changed=true;
        }
      }
      break;
    }
    if(data.watered(root,level.getGameTime()))changed=true;
    if(changed)level.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH,
        root.getX()+.5,root.getY()+.6,root.getZ()+.5,12,.35,.25,.35,.1);
    return changed;
  }
}
