package com.chaogarden.test;

import com.chaogarden.*;
import com.chaogarden.fruit.*;
import com.chaogarden.toys.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("chao_refine") @PrefixGameTestTemplate(false)
public final class RefineGameTests02537 {
  @GameTest(template="empty") public static void journalInventoryGridAndExactTooltip(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);
    try{p.getInventory().clearContent();var m=p.inventoryMenu;
      m.getSlot(1).setByPlayer(new ItemStack(Items.BOOK,3));m.getSlot(4).setByPlayer(key(1));
      m.clicked(0,0,ClickType.QUICK_MOVE,p);
      h.assertTrue(p.getInventory().countItem(ChaoGarden.item("garden_guide"))==3,"2x2 survival inventory shift crafts all journals");keyCheck(h,m.getSlot(4).getItem(),1);
      var tips=new java.util.ArrayList<Component>();ChaoGarden.item("chao_key").appendHoverText(key(1),Item.TooltipContext.of(h.getLevel()),tips,TooltipFlag.NORMAL);
      h.assertTrue(tips.stream().anyMatch(t->t.getString().equals("Combine with a book to learn more about caring for a Chao!")),"exact requested key tooltip");
      m.removed(p);h.assertTrue(p.getInventory().countItem(ChaoGarden.item("chao_key"))==1,"2x2 close preserves key");
    }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();
  }
  private static CraftingMenu menu(GameTestHelper h,ServerPlayer p){
    p.getInventory().clearContent();var at=h.absolutePos(new BlockPos(1,1,1));h.getLevel().setBlock(at,Blocks.CRAFTING_TABLE.defaultBlockState(),3);
    return new CraftingMenu(37,p.getInventory(),ContainerLevelAccess.create(h.getLevel(),at));
  }
  private static ItemStack key(int count){var s=new ItemStack(ChaoGarden.item("chao_key"),count);s.set(DataComponents.CUSTOM_NAME,Component.literal("Reusable keeper key"));return s;}
  private static void keyCheck(GameTestHelper h,ItemStack s,int count){h.assertTrue(s.is(ChaoGarden.item("chao_key"))&&s.getCount()==count&&s.getHoverName().getString().equals("Reusable keeper key"),"key count and components retained");}
  @GameTest(template="empty") public static void journalRepeatedNormalCrafts(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);var m=menu(h,p);
    try{m.getSlot(1).setByPlayer(new ItemStack(Items.BOOK,4));m.getSlot(2).setByPlayer(key(1));
      for(int n=0;n<4;n++){h.assertTrue(m.getSlot(0).getItem().is(ChaoGarden.item("garden_guide")),"journal result");m.clicked(0,0,ClickType.PICKUP,p);h.assertTrue(m.getCarried().is(ChaoGarden.item("garden_guide")),"normal click produced journal");p.getInventory().add(m.getCarried());m.setCarried(ItemStack.EMPTY);keyCheck(h,m.getSlot(2).getItem(),1);}
      h.assertTrue(m.getSlot(1).getItem().isEmpty()&&p.getInventory().countItem(ChaoGarden.item("garden_guide"))==4,"only four books consumed");m.removed(p);h.assertTrue(p.getInventory().countItem(ChaoGarden.item("chao_key"))==1,"closing returns one key");
    }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();
  }
  @GameTest(template="empty") public static void journalShiftClickStackedKeys(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);var m=menu(h,p);
    try{m.getSlot(1).setByPlayer(new ItemStack(Items.BOOK,8));m.getSlot(9).setByPlayer(key(3));m.clicked(0,0,ClickType.QUICK_MOVE,p);
      h.assertTrue(p.getInventory().countItem(ChaoGarden.item("garden_guide"))==8&&m.getSlot(1).getItem().isEmpty(),"shift click crafts all eight journals");keyCheck(h,m.getSlot(9).getItem(),3);m.removed(p);h.assertTrue(p.getInventory().countItem(ChaoGarden.item("chao_key"))==3,"no duplicated or lost stacked keys");
    }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();
  }
  @GameTest(template="empty") public static void journalFullInventoryAndInvalidIngredients(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);var m=menu(h,p);
    try{for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));m.getSlot(1).setByPlayer(new ItemStack(Items.BOOK,2));m.getSlot(2).setByPlayer(key(1));m.clicked(0,0,ClickType.QUICK_MOVE,p);keyCheck(h,m.getSlot(2).getItem(),1);h.assertTrue(m.getSlot(1).getItem().getCount()==2,"blocked quick move consumes nothing");
      m.getSlot(3).setByPlayer(new ItemStack(Items.WHEAT_SEEDS));h.assertTrue(m.getSlot(0).getItem().isEmpty(),"extra ingredient rejected");m.getSlot(2).setByPlayer(ItemStack.EMPTY);h.assertTrue(m.getSlot(0).getItem().isEmpty(),"old seed recipe removed");
    }finally{m.removed(p);h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();
  }
  @GameTest(template="empty") public static void teleporterStillConsumesKey(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);var m=menu(h,p);
    try{for(int i=0;i<9;i++)m.getSlot(i+1).setByPlayer(i==4?key(1):new ItemStack(i%3==1?Items.LIGHT_WEIGHTED_PRESSURE_PLATE:Items.GLASS_PANE));m.clicked(0,0,ClickType.QUICK_MOVE,p);
      h.assertTrue(p.getInventory().countItem(ChaoGarden.item("chao_teleporter"))==1&&m.getSlot(5).getItem().isEmpty(),"teleporter consumes key normally");
    }finally{m.removed(p);h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();
  }
  @GameTest(template="empty") public static void bothLogsYieldExactlyFourPlanks(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);
    try{for(boolean dark:new boolean[]{false,true}){var m=menu(h,p);var log=ChaoGarden.item(dark?"dark_chao_fruit_log":"chao_fruit_log");var planks=dark?Items.DARK_OAK_PLANKS:Items.OAK_PLANKS;
      m.getSlot(5).setByPlayer(new ItemStack(log,16));h.assertTrue(m.getSlot(0).getItem().is(planks)&&m.getSlot(0).getItem().getCount()==4,"single-log output");m.clicked(0,0,ClickType.QUICK_MOVE,p);h.assertTrue(p.getInventory().countItem(planks)==64&&m.getSlot(5).getItem().isEmpty(),"bulk crafting conserves 16 logs to 64 planks");m.removed(p);
      var mixed=CraftingInput.of(2,1,java.util.List.of(new ItemStack(log),new ItemStack(Items.DIRT)));h.assertTrue(h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,mixed,h.getLevel()).isEmpty(),"extra ingredient rejected");}
    }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();
  }
  private static BlockPos ground(GameTestHelper h){var r=h.absolutePos(new BlockPos(5,2,5));for(var at:BlockPos.betweenClosed(r.offset(-3,0,-3),r.offset(3,7,3)))h.getLevel().setBlock(at,Blocks.AIR.defaultBlockState(),3);h.getLevel().setBlock(r.below(),Blocks.DIRT.defaultBlockState(),3);return r;}
  @GameTest(template="activity_test",batch="refine_trees") public static void darkSeedGrowthDropsHarvestAndLedgerReload(GameTestHelper h){
    var l=h.getLevel();var root=ground(h);String fruit="dark_garden_fruit";var seed=FruitContent.saplingForFruit(fruit);l.setBlock(root,seed.defaultBlockState(),3);GardenTreeData.get(l).planted(root,fruit);
    seed.performBonemeal(l,l.random,root,l.getBlockState(root));h.assertTrue(l.getBlockState(root).getValue(ChaoSaplingBlock.STAGE)==1,"first bonemeal advances seed");seed.performBonemeal(l,l.random,root,l.getBlockState(root));
    var crown=FruitContent.crownPosition(root);h.assertTrue(l.getBlockState(crown).getValue(ChaoTreeCrownBlock.BARE),"bare Dark tree crown");
    for(var e:FruitContent.darkWoodPlan(root).entrySet()){h.assertTrue(l.getBlockState(e.getKey()).equals(e.getValue()),"native Dark tree joint and axis "+e.getKey());var drop=Block.getDrops(e.getValue(),l,e.getKey(),null);h.assertTrue(drop.size()==1&&drop.getFirst().is(ChaoGarden.item("dark_chao_fruit_log"))&&drop.getFirst().getCount()==1,"each log drops one Dark Chao Fruit Log");}
    h.assertTrue(GardenTools.water(l,root),"Dark trunk accepts existing watering tool");
    for(var at:FruitContent.podPositions(l,crown,fruit)){var s=l.getBlockState(at);h.assertTrue(s.canSurvive(l,at)&&s.getValue(HangingFruitBlock.AGE)==3,"ripe native supported pod");h.assertTrue(FruitContent.podForFruit(fruit).harvest(l,at,s),"harvest succeeds once");h.assertTrue(!FruitContent.podForFruit(fruit).harvest(l,at,l.getBlockState(at)),"no double harvest");}
    FruitContent.treeForFruit(fruit).performBonemeal(l,l.random,crown,l.getBlockState(crown));for(var at:FruitContent.podPositions(l,crown,fruit))h.assertTrue(l.getBlockState(at).getValue(HangingFruitBlock.AGE)==1,"regrowth after harvest");
    var saved=GardenTreeData.get(l).save(new CompoundTag(),l.registryAccess());var reloaded=GardenTreeData.load(saved,l.registryAccess());h.assertTrue(reloaded.save(new CompoundTag(),l.registryAccess()).equals(saved),"planting and watering ledger round trip");h.assertTrue(!reloaded.mayWater(root,l.getGameTime()),"watering cooldown retained");h.succeed();
  }
  @GameTest(template="activity_test",batch="refine_migrate") public static void existingDarkPalmMigrationPreservesFruitAndPlayerBlocks(GameTestHelper h){
    var l=h.getLevel();var root=ground(h);var crown=root.above(4);String fruit="dark_garden_fruit";
    for(int y=0;y<4;y++)l.setBlock(root.above(y),FruitContent.TRUNK.get().defaultBlockState(),3);
    l.setBlock(crown,FruitContent.treeForFruit(fruit).defaultBlockState().setValue(ChaoTreeCrownBlock.EXPANDED,true).setValue(ChaoTreeCrownBlock.GARDEN,true),3);
    for(var d:Direction.Plane.HORIZONTAL){l.setBlock(crown.relative(d),FruitContent.PALM_SUPPORT.get().defaultBlockState(),3);l.setBlock(crown.below().relative(d),FruitContent.podForFruit(fruit).defaultBlockState().setValue(HangingFruitBlock.FACING,d.getOpposite()).setValue(HangingFruitBlock.AGE,d.ordinal()%4),3);}
    GardenTreeData.get(l).planted(root,fruit);GardenTreeData.get(l).grown(root);GardenTreeData.get(l).watered(root,123);var before=GardenTreeData.get(l).save(new CompoundTag(),l.registryAccess());
    var protectedAt=crown.above(2).east();l.setBlock(protectedAt,Blocks.DIAMOND_BLOCK.defaultBlockState(),3);h.assertTrue(!FruitContent.migratePlantedPalm(l,root,fruit)&&l.getBlockState(root).is(FruitContent.TRUNK.get()),"obstructed migration makes no partial changes");l.removeBlock(protectedAt,false);
    h.assertTrue(FruitContent.migratePlantedPalm(l,root,fruit),"old Dark palm converted");h.assertTrue(before.equals(GardenTreeData.get(l).save(new CompoundTag(),l.registryAccess())),"saved ledger retained");
    for(var d:Direction.Plane.HORIZONTAL){var at=crown.below().relative(d);h.assertTrue(l.getBlockState(at).is(FruitContent.podForFruit(fruit))&&l.getBlockState(at).getValue(HangingFruitBlock.AGE)==d.ordinal()%4&&l.getBlockState(at).canSurvive(l,at),"fruit identity age support preserved");}
    h.assertTrue(!FruitContent.migratePlantedPalm(l,root,fruit),"migration idempotent");h.succeed();
  }
  @GameTest(template="activity_test",batch="refine_blocked") public static void darkGrowthPreservesObstructionsAndNormalPalm(GameTestHelper h){
    var l=h.getLevel();var root=ground(h);var seed=FruitContent.saplingForFruit("dark_garden_fruit");l.setBlock(root,seed.defaultBlockState(),3);var at=root.above(6).east();l.setBlock(at,Blocks.GOLD_BLOCK.defaultBlockState(),3);h.assertTrue(!FruitContent.growTree(l,root,"dark_garden_fruit")&&l.getBlockState(root).is(seed)&&l.getBlockState(at).is(Blocks.GOLD_BLOCK),"failed growth preserves seed and build");
    l.removeBlock(at,false);l.setBlock(root,FruitContent.saplingForFruit("garden_fruit").defaultBlockState(),3);h.assertTrue(FruitContent.growTree(l,root,"garden_fruit"),"normal tree still grows");h.assertTrue(l.getBlockState(root).is(FruitContent.TRUNK.get())&&!l.getBlockState(root.above(4)).getValue(ChaoTreeCrownBlock.BARE),"normal palm and log unchanged");h.succeed();
  }
}
