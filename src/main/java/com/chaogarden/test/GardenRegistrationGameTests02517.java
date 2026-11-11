package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.activities.*;
import com.chaogarden.fruit.*;
import com.chaogarden.item.ChaoCarrierItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class GardenRegistrationGameTests02517 {
  private static BlockPos enclosure(GameTestHelper h,boolean tree){
    var l=h.getLevel();
    for(var p:BlockPos.betweenClosed(2,0,2,25,10,25))l.setBlock(h.absolutePos(p),p.getY()==0?Blocks.GRASS_BLOCK.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
    for(int x=3;x<=24;x++)for(int z=3;z<=24;z++)if(x==3||x==24||z==3||z==24)l.setBlock(h.absolutePos(new BlockPos(x,1,z)),Blocks.STONE.defaultBlockState(),3);
    var root=h.absolutePos(new BlockPos(8,1,8));l.setBlock(root,ActivityContent.GARDEN.get().defaultBlockState(),3);
    if(tree)h.assertTrue(FruitContent.growTree(l,h.absolutePos(new BlockPos(16,1,16)),"garden_fruit"),"Tree fixture failed");
    return root;
  }
  private static void fails(GameTestHelper h,Runnable r,String why){boolean denied=false;try{r.run();}catch(IllegalArgumentException expected){denied=true;}h.assertTrue(denied,why);}
  @GameTest(template="activity_test",timeoutTicks=80)
  public static void activationNeedsNoEnclosureOrFood(GameTestHelper h){
    var level=h.getLevel();var root=enclosure(h,false);
    var area=GardenArea.inspect(level,root);h.assertTrue(area.chunkSquare,"Treeless garden uses nine chunks");
    var gate=h.absolutePos(new BlockPos(3,1,10));level.setBlock(gate,Blocks.AIR.defaultBlockState(),3);
    h.assertTrue(GardenArea.inspect(level,root).contains(level,root.east(3).getCenter()),"Open wall must not prevent activation");
    h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=80)
  public static void sixCarriedSlotsPreserveItemsRejectSpoofsAndPersist(GameTestHelper h){
    var level=h.getLevel();var root=enclosure(h,true);var keeper=TestConnections.makeSurvivalServerPlayer(h);keeper.setPos(root.getX()+2,root.getY(),root.getZ()+2);
    var data=GardenRegistry.get(keeper.server);var garden=data.register(keeper,root);var place=garden.place();
    try{
      h.assertTrue(data.activate(keeper,place),"Valid garden activation failed");
      for(int i=0;i<7;i++){
        var chao=ChaoGarden.CHAO.get().create(level);chao.owner=keeper.getUUID();var stack=new ItemStack(ChaoGarden.item("chao_carrier"));ChaoCarrierItem.snapshot(stack,chao);
        keeper.getInventory().setItem(i,stack);var before=ChaoCarrierItem.data(stack).copy();
        boolean ok=data.registerCarried(keeper,place,i,i%6,chao.getUUID());h.assertTrue(ok==(i<6),"Six-slot capacity/occupied slot check failed");
        h.assertTrue(stack.getCount()==1&&before.equals(ChaoCarrierItem.data(stack)),"Registration changed a real Chao item");
      }
      var id=ChaoCarrierItem.storedUUID(keeper.getInventory().getItem(0));
      h.assertTrue(!data.registerCarried(keeper,place,0,0,java.util.UUID.randomUUID()),"Stale drag UUID was trusted");
      h.assertTrue(!data.registerCarried(keeper,place,-1,0,id)&&!data.registerCarried(keeper,place,0,6,id),"Out-of-range packet index accepted");
      var restored=GardenRegistry.load(data.save(new CompoundTag(),level.registryAccess()),level.registryAccess());
      h.assertTrue(restored.areas.containsKey(place)&&restored.entries.get(id).slot==0&&restored.entries.values().stream().filter(e->e.garden.equals(place)).count()==6,"Saved registration lost its area, slots or UUID");
      ActivityManager.handleAction(keeper,root,"toggle",id);h.assertTrue(!data.entries.containsKey(id)&&ChaoCarrierItem.storedUUID(keeper.getInventory().getItem(0)).equals(id),"Unregister changed/lost the Chao item");
      var foreign=ChaoCarrierItem.entityData(keeper.getInventory().getItem(0));foreign.putUUID("Keeper",java.util.UUID.randomUUID());ChaoCarrierItem.storeEntity(keeper.getInventory().getItem(0),foreign);
      h.assertTrue(!data.registerCarried(keeper,place,0,0,id),"Another keeper's Chao was registered");
    }finally{data.removeGarden(place);keeper.server.getPlayerList().remove(keeper);}
    h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=80)
  public static void residentStartsOnEntryAndStopsAtSavedBoundaryUntilUnregistered(GameTestHelper h){
    var l=h.getLevel();var root=enclosure(h,true);var keeper=TestConnections.makeSurvivalServerPlayer(h);keeper.setPos(root.getX()+2,root.getY(),root.getZ()+2);
    var data=GardenRegistry.get(keeper.server);var garden=data.register(keeper,root);var c=ChaoGarden.CHAO.get().create(l);c.owner=keeper.getUUID();c.setNoAi(true);c.setNoGravity(true);c.moveTo(root.getX()+2,root.getY(),root.getZ()+2,0,0);l.addFreshEntity(c);
    try{
      data.activate(keeper,garden.place());
      var legacy=new java.util.HashSet<Long>();for(int x=4;x<24;x++)for(int z=4;z<24;z++)legacy.add(h.absolutePos(new BlockPos(x,1,z)).asLong());
      data.areas.put(garden.place(),new GardenArea(false,legacy,root.east(2)));
      h.assertTrue(data.bind(keeper,c,garden),"Resident fixture bind failed");
      var e=data.entries.get(c.getUUID());var outside=h.absolutePos(new BlockPos(1,1,1));c.setPos(outside.getX(),outside.getY(),outside.getZ());data.contain(c);
      h.assertTrue(!e.entered&&c.blockPosition().equals(outside),"Unreleased registration forcibly recalled an outside Chao");
      var inside=root.east(3);c.setPos(inside.getX()+.5,inside.getY(),inside.getZ()+.5);c.setOnGround(true);data.contain(c);
      h.assertTrue(e.entered,"Chao did not settle on entering its garden");
      c.setPos(outside.getX(),outside.getY(),outside.getZ());data.contain(c);
      h.assertTrue(data.areas.get(garden.place()).contains(l,c.position()),"Resident escaped the saved garden");
      ActivityManager.handleAction(keeper,root,"toggle",c.getUUID());c.setPos(outside.getX(),outside.getY(),outside.getZ());data.contain(c);
      h.assertTrue(c.blockPosition().equals(outside),"Unregistered Chao remained confined");
    }finally{c.discard();data.removeGarden(garden.place());keeper.server.getPlayerList().remove(keeper);}
    h.succeed();
  }
  @GameTest(template="empty",timeoutTicks=40)
  public static void authoredGardensBypassInspectionAndCoverWholeGarden(GameTestHelper h){
    var realm=h.getLevel().getServer().getLevel(com.chaogarden.hub.HubContent.GARDEN);
    h.assertTrue(realm!=null,"Missing garden dimension");
    var area=GardenArea.inspect(realm,new BlockPos(0,80,0));
    h.assertTrue(area.realm,"Authored garden demanded a tree or fence");h.succeed();
  }
  @GameTest(template="empty",timeoutTicks=40)
  public static void gardenFruitTreesNeverWitherAfterSaveLoad(GameTestHelper h){
    var l=h.getLevel();for(String fruit:GardenArea.FOOD){
      var d=new GardenTreeData();var root=h.absolutePos(new BlockPos(2,2,2));d.planted(root,fruit);d.grown(root);
      var tag=d.save(new CompoundTag(),l.registryAccess());tag.getList("Trees",10).getCompound(0).putInt("Age",GardenTreeData.ORDINARY_LIFE+1200);
      var restored=GardenTreeData.load(tag,l.registryAccess());h.assertTrue(!restored.withered(root),"Permanent food source became mortal: "+fruit);
    }h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=80)
  public static void oneTatamiItemPlacesFortyNineTilesAndBlockedPlacementIsAtomic(GameTestHelper h){
    var l=h.getLevel();var center=h.absolutePos(new BlockPos(12,1,12));
    for(var p:BlockPos.betweenClosed(center.offset(-4,-1,-4),center.offset(4,2,4)))l.setBlock(p,p.getY()==center.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
    var player=TestConnections.makeSurvivalServerPlayer(h);player.setPos(center.getX()+5,center.getY(),center.getZ()+5);
    var stack=new ItemStack(ChaoGarden.item("karate_mat"),2);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
    var hit=new net.minecraft.world.phys.BlockHitResult(center.below().getCenter(),Direction.UP,center.below(),false);
    var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
    try {
      l.setBlock(center.offset(3,0,3),Blocks.DIAMOND_BLOCK.defaultBlockState(),3);
      h.assertTrue(!stack.getItem().useOn(context).consumesAction()&&stack.getCount()==2&&l.getBlockState(center).isAir(),"Blocked carpet placement changed blocks/items");
      l.removeBlock(center.offset(3,0,3),false);
      h.assertTrue(stack.getItem().useOn(context).consumesAction()&&stack.getCount()==1,"One carpet did not consume exactly one item");
      h.assertTrue(ActivityManager.mat(l,center).size()==49,"One carpet did not lay a complete seven-square arena");
      var corner=center.offset(-3,0,-3);player.gameMode.destroyBlock(corner);
      h.assertTrue(l.getBlockState(center).isAir()&&l.getBlockState(center.offset(3,0,3)).isAir(),"Breaking an edge left orphan carpet tiles");
      var drops=l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(center).inflate(5),e->e.getItem().is(ChaoGarden.item("karate_mat")));
      h.assertTrue(drops.stream().mapToInt(e->e.getItem().getCount()).sum()==1,"Breaking the assembled carpet duplicated/lost its item");
    }finally{player.server.getPlayerList().remove(player);}
    h.succeed();
  }
}
