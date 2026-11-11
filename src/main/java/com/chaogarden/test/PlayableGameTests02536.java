package com.chaogarden.test;
import com.chaogarden.*;
import com.chaogarden.market.*;
import com.chaogarden.fruit.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("chao_garden") @PrefixGameTestTemplate(false)
public final class PlayableGameTests02536 {
 @GameTest(template="empty",timeoutTicks=60) public static void exactKeyRecipeAndPermanentWildStock(GameTestHelper h){
  var l=h.getLevel();var key=ChaoGarden.item("chao_key");var items=new java.util.ArrayList<ItemStack>();
  for(int i=0;i<9;i++)items.add(new ItemStack(i==4?key:i%3==1?Items.LIGHT_WEIGHTED_PRESSURE_PLATE:Items.GLASS_PANE));
  var recipe=l.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,CraftingInput.of(3,3,items),l).orElseThrow();
  h.assertTrue(recipe.value().getResultItem(l.registryAccess()).is(ChaoGarden.item("chao_teleporter")),"exact full-pane recipe");
  for(int i=0;i<9;i++){var bad=new java.util.ArrayList<>(items);bad.set(i,ItemStack.EMPTY);h.assertTrue(l.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,CraftingInput.of(3,3,bad),l).isEmpty(),"Every position is required: "+i);}
  var t=MarketContent.TRADER.get().create(l);t.beginVisit(l.getGameTime());var p=TestConnections.makeServerPlayer(h);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
  try{
   for(int pass=0;pass<3;pass++){
    var offers=t.getOffers();int index=java.util.stream.IntStream.range(0,offers.size()).filter(i->offers.get(i).getResult().is(key)).findFirst().orElseThrow();var offer=offers.get(index);
    h.assertTrue(!t.resident()&&offers.stream().filter(o->o.getResult().is(key)).count()==1&&offer.getCostA().is(Items.EMERALD)&&offer.getCostA().getCount()==4,"wild permanent key costs four emeralds");
    t.setTradingPlayer(p);var menu=new net.minecraft.world.inventory.MerchantMenu(36,p.getInventory(),t);p.getInventory().clearContent();p.getInventory().setItem(9,new ItemStack(Items.EMERALD,4));menu.setSelectionHint(index);menu.tryMoveItems(index);menu.quickMoveStack(p,2);menu.removed(p);
    h.assertTrue(p.getInventory().countItem(key)==1&&p.getInventory().countItem(Items.EMERALD)==0&&!offer.isOutOfStock()&&offer.getUses()==0,"survival payment + permanent reuse");
    var saved=new CompoundTag();t.saveWithoutId(saved);if(pass==0)saved.putLong("MarketRestockAt",Math.max(1,l.getGameTime()-1));if(pass==1)saved.putInt("MarketStockVersion",25);
    t=MarketContent.TRADER.get().create(l);t.load(saved);t.aiStep();
   }
  }finally{l.getServer().getPlayerList().remove(p);t.discard();}h.succeed();
 }
 @GameTest(template="empty",timeoutTicks=60) public static void oldPlantedTreeMigratesWithoutDuplicatingFruitOrResettingAge(GameTestHelper h){
  var l=h.getLevel();var root=h.absolutePos(new BlockPos(4,2,4));var crown=root.above(4);String fruit="round_fruit";
  for(var at:BlockPos.betweenClosed(root.offset(-3,0,-3),root.offset(3,7,3)))l.setBlock(at,Blocks.AIR.defaultBlockState(),3);
  l.setBlock(root.below(),Blocks.DIRT.defaultBlockState(),3);for(int y=0;y<4;y++)l.setBlock(root.above(y),FruitContent.TRUNK.get().defaultBlockState(),3);
  l.setBlock(crown,FruitContent.treeForFruit(fruit).defaultBlockState().setValue(ChaoTreeCrownBlock.EXPANDED,true),3);
  for(var d:Direction.Plane.HORIZONTAL){l.setBlock(crown.relative(d),FruitContent.TRUNK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS,d.getAxis()),3);l.setBlock(crown.relative(d,2),FruitContent.LEAVES.get().defaultBlockState().setValue(LeavesBlock.DISTANCE,1),3);l.setBlock(crown.below().relative(d,2),FruitContent.podForFruit(fruit).defaultBlockState().setValue(HangingFruitBlock.FACING,d.getOpposite()).setValue(HangingFruitBlock.AGE,d.ordinal()%4),3);}
  var playerLeaf=crown.above(2);l.setBlock(playerLeaf,FruitContent.LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT,true),3);
  GardenTreeData.get(l).planted(root,fruit);GardenTreeData.get(l).grown(root);GardenTreeData.get(l).watered(root,99);var before=GardenTreeData.get(l).save(new CompoundTag(),l.registryAccess());
  h.assertTrue(FruitContent.migratePlantedPalm(l,root,fruit),"legacy canopy converted: "+l.getBlockState(crown)+" "+java.util.stream.Stream.of(Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST).map(d->d+" arm="+l.getBlockState(crown.relative(d))+" inner="+l.getBlockState(crown.below().relative(d))).toList());
  h.assertTrue(before.equals(GardenTreeData.get(l).save(new CompoundTag(),l.registryAccess())),"life and watering ledger unchanged");
  for(var d:Direction.Plane.HORIZONTAL){var p=crown.below().relative(d);h.assertTrue(l.getBlockState(p).getValue(HangingFruitBlock.AGE)==d.ordinal()%4&&l.getBlockState(p).canSurvive(l,p),"fruit age/support preserved");h.assertTrue(l.getBlockState(crown.below().relative(d,2)).isAir(),"no old duplicate pod");}
  h.assertTrue(l.getBlockState(playerLeaf).getValue(LeavesBlock.PERSISTENT),"player foliage preserved");
  h.assertTrue(l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(root).inflate(5)).isEmpty(),"no harvest minted by migration");
  h.assertTrue(!FruitContent.migratePlantedPalm(l,root,fruit),"idempotent conversion");h.succeed();
 }
 @GameTest(template="empty",timeoutTicks=100,batch="playable_windows") public static void windowRepairIsBoundedIdempotentAndPreservesPlayerBlocks(GameTestHelper h){
  var l=h.getLevel().getServer().getLevel(com.chaogarden.hub.HubContent.KINDERGARTEN);h.assertTrue(com.chaogarden.hub.HubContent.ensure(l),"school exists");
  var old=new java.util.LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();for(var at:BlockPos.betweenClosed(16,87,11,21,90,17))old.put(at.immutable(),l.getBlockState(at));
  try{
   var anchor=new BlockPos(17,88,14);var outside=new BlockPos(17,87,15);var protectedBlock=new BlockPos(17,88,12);
   l.setBlock(anchor,com.chaogarden.kindergarten.KindergartenDecor025.block("school_clapboard").defaultBlockState(),2);l.setBlock(outside,com.chaogarden.kindergarten.KindergartenDecor025.state("school_round_window",Direction.SOUTH),2);
   for(var at:BlockPos.betweenClosed(17,88,12,18,89,13))l.setBlock(at,com.chaogarden.kindergarten.KindergartenDecor025.block("school_clapboard").defaultBlockState(),2);
   l.setBlock(protectedBlock,Blocks.DIAMOND_BLOCK.defaultBlockState(),2);var sentinel=new BlockPos(20,88,12);l.setBlock(sentinel,Blocks.GOLD_BLOCK.defaultBlockState(),2);
   com.chaogarden.kindergarten.KindergartenRooms025.repairDormerWindows(l);
   h.assertTrue(l.getBlockState(anchor).is(com.chaogarden.kindergarten.KindergartenDecor025.block("school_round_window"))&&!l.getBlockState(outside).is(com.chaogarden.kindergarten.KindergartenDecor025.block("school_round_window")),"old protruding anchor seated inside wall");
   h.assertTrue(l.getBlockState(new BlockPos(18,89,13)).isAir(),"interior aperture unblocked");h.assertTrue(l.getBlockState(protectedBlock).is(Blocks.DIAMOND_BLOCK)&&l.getBlockState(sentinel).is(Blocks.GOLD_BLOCK),"player block inside and outside repair footprint retained");
   var repaired=new java.util.LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();old.keySet().forEach(at->repaired.put(at,l.getBlockState(at)));com.chaogarden.kindergarten.KindergartenRooms025.repairDormerWindows(l);h.assertTrue(repaired.entrySet().stream().allMatch(e->l.getBlockState(e.getKey()).equals(e.getValue())),"repair is idempotent");
  }finally{old.forEach((at,state)->l.setBlock(at,state,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE));}h.succeed();
 }
}
