package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.fruit.*;
import com.chaogarden.hub.HubContent;
import com.chaogarden.market.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class GardeningGameTests02512 {
  @GameTest(template="empty",timeoutTicks=40)
  public static void permanentGardenPalmsAndDarkBranchesCanBeWateredWithoutBecomingMortal(GameTestHelper h) {
    var level=h.getLevel();
    for(boolean dark:new boolean[]{false,true}) {
      BlockPos at=soil(h);var palm=com.chaogarden.hub.LobbyDecor025.PALM_TRUNK.get();
      var wood=com.chaogarden.hub.DarkGardenDecor0255.block("dark_garden_deadwood");
      for(int y=0;y<3;y++)level.setBlock(at.above(y),(dark?wood:palm).defaultBlockState(),3);
      level.setBlock(at.above(3),dark?wood.defaultBlockState().setValue(com.chaogarden.hub.DarkGardenDecor0255.Deadwood.BRANCHED,true)
          :com.chaogarden.hub.LobbyDecor025.PALM_CROWN.get().defaultBlockState(),3);
      String fruit=dark?"dark_garden_fruit":"garden_fruit";
      for(Direction side:Direction.Plane.HORIZONTAL) {
        level.setBlock(at.above(3).relative(side),dark?wood.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS,side.getAxis())
            :com.chaogarden.hub.LobbyDecor025.PALM_SUPPORT.get().defaultBlockState(),3);
        level.setBlock(at.above(2).relative(side),FruitContent.podForFruit(fruit).defaultBlockState().setValue(HangingFruitBlock.FACING,side.getOpposite()),3);
      }
      h.assertTrue(at.equals(GardenTools.root(level,at.above(1)))&&GardenTools.water(level,at.above(1)),"Authored tree cannot be watered");
      for(Direction side:Direction.Plane.HORIZONTAL)h.assertTrue(level.getBlockState(at.above(2).relative(side)).getValue(HangingFruitBlock.AGE)==3,"Authored tree left unripe fruit");
      h.assertTrue(!GardenTreeData.get(level).withered(at),"Watering made a permanent garden tree mortal");
    }
    h.succeed();
  }
  @GameTest(template="empty",timeoutTicks=40)
  public static void plantedTreeWithersDuringNearbyPlayAndDoesNotEraseForeignBlocks(GameTestHelper h) {
    var level=h.getLevel();BlockPos at=soil(h);
    h.assertTrue(FruitContent.growTree(level,at,"tasty_fruit"),"Aging fixture tree failed");
    var data=new GardenTreeData();data.planted(at,"tasty_fruit");data.grown(at);
    var tag=data.save(new CompoundTag(),level.registryAccess());tag.getList("Trees",10).getCompound(0).putInt("Age",GardenTreeData.ORDINARY_LIFE-20);
    data=GardenTreeData.load(tag,level.registryAccess());
    var player=TestConnections.makeSurvivalServerPlayer(h);player.teleportTo(at.getX()+1,at.getY(),at.getZ()+1);
    try {
      var foreign=FruitContent.crownPosition(at).offset(1,1,0);level.setBlock(foreign,Blocks.DIAMOND_BLOCK.defaultBlockState(),3);
      var playerLeaves=FruitContent.crownPosition(at).offset(-1,1,0);level.setBlock(playerLeaves,FruitContent.LEAVES.get().defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT,true),3);
      data.advance(level,20);
      h.assertTrue(level.getBlockState(playerLeaves).is(FruitContent.LEAVES.get()),"Palm withering erased player leaves");
      h.assertTrue(data.withered(at)&&level.getBlockState(at).is(FruitContent.TRUNK.get()),"Mature tree did not enter bare-trunk stage");
      for(BlockPos pod:FruitContent.podPositions(FruitContent.crownPosition(at),false))h.assertTrue(level.getBlockState(pod).isAir(),"Withered tree retained fruit");
      h.assertTrue(level.getBlockState(foreign).is(Blocks.DIAMOND_BLOCK),"Withering erased a foreign build");
      for(int i=0;i<60;i++)data.advance(level,20);
      h.assertTrue(data.count()==0&&level.getBlockState(at).isAir()&&level.getBlockState(foreign).is(Blocks.DIAMOND_BLOCK),"Expired tree did not release its slot safely");
    }finally{level.getServer().getPlayerList().remove(player);}
    h.succeed();
  }
  @GameTest(template="empty",timeoutTicks=210)
  public static void actualGardeningGoalPlantsWithoutFreezeGoalInterruptingTheDig(GameTestHelper h) {
    BlockPos at=soil(h);h.getLevel().setDayTime(6000);
    var p=h.makeMockPlayer(GameType.SURVIVAL);p.getAbilities().instabuild=false;
    var c=chao(h,p,at.east());c.state().setTrait("reward.shovel",1);c.state().points[ChaoState.RUN]=50;c.setNoAi(false);c.setNoGravity(false);
    var stack=new ItemStack(ChaoGarden.item("orange_fruit_seed"),2);p.setItemInHand(InteractionHand.MAIN_HAND,stack);c.mobInteract(p,InteractionHand.MAIN_HAND);
    h.runAtTickTime(180,()->{
      int planted=0;
      for(BlockPos pos:BlockPos.betweenClosed(at.offset(-5,-1,-5),at.offset(5,1,5)))
        if(h.getLevel().getBlockState(pos).is(FruitContent.saplingForFruit("orange_fruit")))planted++;
      h.assertTrue(planted==1&&stack.getCount()==1&&c.getMainHandItem().isEmpty(),"Actual AI did not complete exactly one seed planting");
      c.discard();h.succeed();
    });
  }
  @GameTest(template="empty",timeoutTicks=140)
  public static void idleChaoUsesItsLearnedWateringCanOnAnActualSeedling(GameTestHelper h) {
    BlockPos at=soil(h);h.getLevel().setDayTime(6000);
    h.getLevel().setBlock(at,FruitContent.saplingForFruit("hyper_swim_fruit").defaultBlockState(),3);
    GardenTreeData.get(h.getLevel()).planted(at,"hyper_swim_fruit");
    var p=h.makeMockPlayer(GameType.SURVIVAL);var c=chao(h,p,at.east());c.state().setTrait("reward.watering_can",1);c.setNoAi(false);c.setNoGravity(false);
    h.runAtTickTime(100,()->{
      var state=h.getLevel().getBlockState(at);
      h.assertTrue(state.is(FruitContent.TRUNK.get())||state.is(FruitContent.saplingForFruit("hyper_swim_fruit"))&&state.getValue(ChaoSaplingBlock.STAGE)==1,
          "Idle Chao did not water the seedling through its gardening goal");
      h.assertTrue(c.getMainHandItem().isEmpty(),"Learned watering created a real duplicate tool");c.discard();h.succeed();
    });
  }
  private static BlockPos soil(GameTestHelper h) {
    var at=h.absolutePos(new BlockPos(2,2,2));
    for(var p:BlockPos.betweenClosed(at.offset(-3,0,-3),at.offset(3,8,3)))h.getLevel().setBlock(p,Blocks.AIR.defaultBlockState(),3);
    for(var p:BlockPos.betweenClosed(at.offset(-3,-1,-3),at.offset(3,-1,3)))h.getLevel().setBlock(p,Blocks.DIRT.defaultBlockState(),3);
    return at;
  }
  private static ChaoEntity chao(GameTestHelper h,net.minecraft.world.entity.player.Player p,BlockPos at) {
    var c=ChaoGarden.CHAO.get().create(h.getLevel());var s=new ChaoState();s.hunger=80;s.energy=80;
    c.setState(s);c.owner=p.getUUID();c.setNoAi(true);c.setNoGravity(true);c.moveTo(Vec3.atBottomCenterOf(at));h.getLevel().addFreshEntity(c);return c;
  }
  @GameTest(template="empty",timeoutTicks=200)
  public static void toolsTeachOnceThroughRealOffersAndPersist(GameTestHelper h) {
    var p=h.makeMockPlayer(GameType.SURVIVAL);p.getAbilities().instabuild=false;
    var a=chao(h,p,h.absolutePos(new BlockPos(1,3,1)));var b=chao(h,p,h.absolutePos(new BlockPos(4,3,1)));
    String[] tools={"chao_shovel","chao_watering_can"};ChaoEntity[] children={a,b};
    for(int i=0;i<2;i++) {
      var gift=new ItemStack(ChaoGarden.item(tools[i]),2);p.setItemInHand(InteractionHand.MAIN_HAND,gift);children[i].mobInteract(p,InteractionHand.MAIN_HAND);
      h.assertTrue(gift.getCount()==1&&children[i].getMainHandItem().getCount()==1,"Tool offer must transfer exactly one real item");
      h.assertTrue(children[i].state().intTrait(GardenTools.ability(gift))==0,"Ability must not apply before acceptance");
    }
    h.runAtTickTime(150,()->{
      for(int i=0;i<2;i++) {
        var c=children[i];var tool=new ItemStack(ChaoGarden.item(tools[i]));String trait=GardenTools.ability(tool);
        h.assertTrue(c.state().intTrait(trait)==1&&c.getMainHandItem().isEmpty(),"Tool acceptance did not teach and consume once");
        var tag=new CompoundTag();c.saveWithoutId(tag);var loaded=ChaoGarden.CHAO.get().create(h.getLevel());loaded.load(tag);
        h.assertTrue(loaded.state().intTrait(trait)==1&&!GardenTools.canLearn(loaded,tool),"Saved ability lost or duplicate tool can be consumed");
        c.discard();
      }
      h.succeed();
    });
  }
  @GameTest(template="empty",timeoutTicks=60)
  public static void realSeedPlacementWateringAndFailedPlacementPreserveItems(GameTestHelper h) {
    BlockPos at=soil(h);var level=h.getLevel();level.setDayTime(6000);
    var p=h.makeMockPlayer(GameType.SURVIVAL);p.getAbilities().instabuild=false;p.getAbilities().mayBuild=true;
    var seeds=new ItemStack(ChaoGarden.item("round_fruit_seed"),3);p.setItemInHand(InteractionHand.MAIN_HAND,seeds);
    var use=new UseOnContext(level,p,InteractionHand.MAIN_HAND,seeds,new BlockHitResult(Vec3.atCenterOf(at.below()),Direction.UP,at.below(),false));
    h.assertTrue(seeds.getItem().useOn(use).consumesAction()&&seeds.getCount()==2,"Player planted seed count mismatch");
    h.assertTrue(level.getBlockState(at).is(FruitContent.saplingForFruit("round_fruit")),"Seed did not produce actual matching seedling");
    h.assertTrue(GardenTools.water(level,at)&&level.getBlockState(at).getValue(ChaoSaplingBlock.STAGE)==1,"First watering must advance one seed stage");
    h.assertTrue(!GardenTools.water(level,at),"Repeated same-tick watering bypassed cooldown");
    level.setBlock(at.above(),Blocks.DIAMOND_BLOCK.defaultBlockState(),3);
    h.assertTrue(!FruitContent.growTree(level,at,"round_fruit")&&level.getBlockState(at.above()).is(Blocks.DIAMOND_BLOCK),"Growth overwrote an obstruction");
    level.removeBlock(at.above(),false);h.assertTrue(FruitContent.growTree(level,at,"round_fruit"),"Clear seedling failed to grow");
    var saved=GardenTreeData.get(level).save(new CompoundTag(),level.registryAccess());
    var loaded=GardenTreeData.load(saved,level.registryAccess());
    h.assertTrue(loaded.save(new CompoundTag(),level.registryAccess()).equals(saved),"Planted tree data failed exact round trip");
    for(BlockPos pod:FruitContent.podPositions(FruitContent.crownPosition(at),false))
      h.assertTrue(level.getBlockState(pod).getValue(HangingFruitBlock.AGE)==0,"New orchard unexpectedly spawned ripe duplicate harvest");
    h.succeed();
  }
  @GameTest(template="empty",timeoutTicks=100)
  public static void shovelThresholdAndSeedCommitAreCheckedAtTheActualPlantingMoment(GameTestHelper h) {
    BlockPos at=soil(h);h.getLevel().setDayTime(6000);
    var p=h.makeMockPlayer(GameType.SURVIVAL);p.getAbilities().instabuild=false;
    var c=chao(h,p,at.east());c.state().setTrait("reward.shovel",1);c.state().points[ChaoState.RUN]=49;
    h.assertTrue(!GardenTools.canPlant(c,"square_fruit"),"49 Run points bypassed walk requirement");
    c.state().points[ChaoState.RUN]=50;h.assertTrue(GardenTools.canPlant(c,"square_fruit"),"50 Run points plus shovel should permit planting");
    var gift=new ItemStack(ChaoGarden.item("square_fruit_seed"),2);p.setItemInHand(InteractionHand.MAIN_HAND,gift);c.mobInteract(p,InteractionHand.MAIN_HAND);
    h.runAtTickTime(45,()->{
      h.assertTrue(c.getItemAction()==ChaoEntity.ItemAction.GARDEN,"Qualified seed offer did not enter gardening");
      var saved=new CompoundTag();c.saveWithoutId(saved);var restored=ChaoGarden.CHAO.get().create(h.getLevel());restored.load(saved);
      h.assertTrue(restored.getItemAction()==ChaoEntity.ItemAction.THINK&&restored.getMainHandItem().getCount()==1,"Reload must retain real seed and reconsider navigation");
      c.beginPlanting();h.getLevel().setBlock(at,Blocks.GOLD_BLOCK.defaultBlockState(),3);
      h.assertTrue(!c.finishPlanting(at)&&c.getMainHandItem().getCount()==1,"Blocked commit lost seed or overwrote build");
      h.getLevel().removeBlock(at,false);
      h.assertTrue(c.finishPlanting(at)&&c.getMainHandItem().isEmpty()&&gift.getCount()==1,"Valid planting did not consume exactly the held seed");
      h.assertTrue(!c.finishPlanting(at)&&h.getLevel().getBlockState(at).is(FruitContent.saplingForFruit("square_fruit")),"Commit replay or wrong seedling");
      c.discard();h.succeed();
    });
  }
  @GameTest(template="empty",timeoutTicks=40)
  public static void fourTreeLimitSavedAgingWateringAndPermanentTreesAreDistinct(GameTestHelper h) {
    var level=h.getLevel();var data=new GardenTreeData();
    for(int i=0;i<4;i++)data.planted(new BlockPos(20000+i*10,80,20000),"strong_fruit");
    var garden=level.getServer().getLevel(HubContent.GARDEN);
    h.assertTrue(!data.canPlant(garden,new BlockPos(0,81,0)),"Garden allowed a fifth planted tree");
    h.assertTrue(data.canPlant(level,new BlockPos(0,81,0)),"Overworld should not share one global four-tree cap");
    var tag=data.save(new CompoundTag(),level.registryAccess());var row=tag.getList("Trees",10).getCompound(0);
    row.putBoolean("Grown",true);row.putInt("Age",GardenTreeData.STRONG_LIFE-100);
    var restored=GardenTreeData.load(tag,level.registryAccess());var root=BlockPos.of(row.getLong("Root"));
    h.assertTrue(restored.watered(root,1000)&&!restored.watered(root,1001),"Watering life extension or per-tree cooldown failed");
    var watered=restored.save(new CompoundTag(),level.registryAccess()).getList("Trees",10).getCompound(0);
    h.assertTrue(watered.getInt("Age")==GardenTreeData.STRONG_LIFE-300,"Watering must grant only the bounded ten-second extension");
    row.putInt("Age",GardenTreeData.STRONG_LIFE);restored=GardenTreeData.load(tag,level.registryAccess());
    h.assertTrue(restored.withered(root)&&!restored.mayWater(root,99999),"Dead tree was revivable");
    BlockPos at=soil(h);h.assertTrue(FruitContent.growTree(level,at,"tasty_fruit"),"Permanent tree fixture failed");
    h.assertTrue(!GardenTreeData.get(level).withered(at),"Untracked existing scenery became mortal");
    h.assertTrue(GardenTools.water(level,at),"Mature tree watering failed");
    for(BlockPos pod:FruitContent.podPositions(FruitContent.crownPosition(at),false))
      h.assertTrue(level.getBlockState(pod).getValue(HangingFruitBlock.AGE)==3,"Mature watering did not ripen every real fruit");
    h.succeed();
  }
}
