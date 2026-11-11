package com.chaogarden.test;
import com.chaogarden.*;
import com.chaogarden.core.*;
import com.chaogarden.genes.*;
import com.chaogarden.item.*;
import com.chaogarden.market.*;
import com.chaogarden.companion.*;
import com.chaogarden.entity.ChaoEntity;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Container;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
@net.neoforged.fml.common.EventBusSubscriber(modid="chao_garden")
@GameTestHolder("chao_drives43") @PrefixGameTestTemplate(false)
public final class DriveGameTests043 {
 static Vec3 cancelAt;
 @net.neoforged.bus.api.SubscribeEvent public static void cancelSpawn(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent e){
  if(cancelAt!=null&&e.getEntity() instanceof ItemEntity item&&EnemyDriveDrops.drive(item.getItem())&&item.position().distanceToSqr(cancelAt)<.01)e.setCanceled(true);
 }
 @GameTest(template="activity_test",timeoutTicks=80)
 public static void dragonOnceCancelledDropsAndCombinedGear(GameTestHelper h)throws Exception{
  var p=TestConnections.makeSurvivalServerPlayer(h);equip(h,p,true);var l=h.getLevel();var at=Vec3.atCenterOf(h.absolutePos(new BlockPos(4,2,4)));
  var dragon=EntityType.ENDER_DRAGON.create(l);dragon.moveTo(at);dragon.setHealth(0);
  var event=new net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent(dragon,p,100);
  cancelAt=at;try{EnemyDriveDrops.dragonDeath(event);}finally{cancelAt=null;}
  h.assertTrue(dragon.getPersistentData().getBoolean("ChaoDragonDriveCommitted"),"cancelled spawn commits dragon roll");
  var n=new CompoundTag();dragon.saveWithoutId(n);var copy=EntityType.ENDER_DRAGON.create(l);copy.load(n);
  for(int i=0;i<20;i++)EnemyDriveDrops.dragonDeath(new net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent(copy,p,100));
  h.assertTrue(l.getEntitiesOfClass(ItemEntity.class,new AABB(BlockPos.containing(at)).inflate(1),e->EnemyDriveDrops.drive(e.getItem())).isEmpty(),"cancelled dragon reward not retried after save");
  var zombie=EntityType.ZOMBIE.create(l);zombie.moveTo(at);zombie.hurt(zombie.damageSources().playerAttack(p),1);
  var loot=new ArrayList<ItemEntity>();var crafted=new ItemEntity(l,at.x,at.y,at.z,item("perfect_drive"));loot.add(crafted);
  loot.add(new ItemEntity(l,at.x,at.y,at.z,new ItemStack(ChaoGarden.item("swim_drive"),8)));
  var drops=new net.neoforged.neoforge.event.entity.living.LivingDropsEvent(zombie,zombie.damageSources().playerAttack(p),loot,true);
  net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(drops);
  h.assertTrue(loot.contains(crafted)&&loot.size()==2&&loot.stream().filter(i->EnemyDriveDrops.drive(i.getItem())).mapToInt(i->i.getItem().getCount()).sum()==1,"crafted gear retained and old base pool normalized");
  var cancelled=new net.neoforged.neoforge.event.entity.living.LivingDropsEvent(zombie,zombie.damageSources().playerAttack(p),loot,true);cancelled.setCanceled(true);
  var before=List.copyOf(loot);net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(cancelled);h.assertTrue(loot.equals(before),"cancelled loot untouched");
  boolean enabled=l.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT);
  try{l.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(false,l.getServer());loot.clear();EnemyDriveDrops.drops(drops);h.assertTrue(loot.isEmpty(),"doMobLoot respected");}
  finally{l.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(enabled,l.getServer());}
  slot(p).setItem(0,ItemStack.EMPTY);p.server.getPlayerList().remove(p);h.succeed();
 }
 static List<String> ids(){return ChaoGarden.CONTENT.entrySet().stream().filter(e->e.getValue().get() instanceof CombinedDriveItem).map(Map.Entry::getKey).toList();}
 static ItemStack item(String id){return new ItemStack(ChaoGarden.item(id));}
 static ChaoEntity chao(GameTestHelper h,ServerPlayer p){
  var c=ChaoGarden.CHAO.get().create(h.getLevel());var at=h.absolutePos(new BlockPos(3,2,3));h.getLevel().setBlock(at.below(),Blocks.STONE.defaultBlockState(),3);
  c.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5);c.owner=p.getUUID();c.setNoAi(true);c.setNoGravity(true);
  c.state().energy=100;c.state().happiness=10;Arrays.fill(c.state().levels,26);Arrays.fill(c.state().progress,43);Arrays.fill(c.state().points,650);
  c.setCustomName(net.minecraft.network.chat.Component.literal("Existing keeper's Chao"));h.getLevel().addFreshEntity(c);c.sync();return c;
 }
 static Container slot(ServerPlayer p)throws Exception{
  Class<?> cap=Class.forName("io.wispforest.accessories.api.AccessoriesCapability"),ct=Class.forName("io.wispforest.accessories.api.AccessoriesContainer");
  Object capability=cap.getMethod("get",LivingEntity.class).invoke(null,p);
  Object container=((Map<?,?>)cap.getMethod("getContainers").invoke(capability)).get("chao_companion");
  return (Container)ct.getMethod("getAccessories").invoke(container);
 }
 static ItemStack equip(GameTestHelper h,ServerPlayer p,boolean adult)throws Exception{
  var c=chao(h,p);c.state().adult=adult;var stack=item("chao_carrier");h.assertTrue(ChaoCarrierItem.capture(stack,c,p),"capture existing Chao");slot(p).setItem(0,stack);return stack;
 }
 static void checkRecipe(GameTestHelper h,List<ItemStack> items,int width,int height,String expected){
  var input=CraftingInput.of(width,height,items);var found=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,h.getLevel());
  if(expected==null){h.assertTrue(found.isEmpty(),"Invalid mixed recipe accepted");return;}
  h.assertTrue(found.isPresent(),"Recipe missing: "+expected);
  var out=found.get().value().assemble(input,h.getLevel().registryAccess());
  h.assertTrue(out.is(ChaoGarden.item(expected))&&out.getCount()==1,"Wrong recipe output "+expected);
  h.assertTrue(found.get().value().getRemainingItems(input).stream().allMatch(ItemStack::isEmpty),"Drive ingredients must be consumed");
 }
 @GameTest(template="activity_test",timeoutTicks=80)
 public static void recipesAndTooltip(GameTestHelper h){
  h.assertTrue(ids().size()==16,"All combined types registered");
  for(String stat:List.of("swim","fly","run","power")){
   String previous=stat+"_drive";
   for(String tier:List.of("solid","super_solid","unstable_solid")){
    int n=tier.equals("solid")?4:9;String next=tier+"_"+stat+"_drive";
    var inputs=new ArrayList<ItemStack>();for(int i=0;i<n;i++)inputs.add(item(previous));
    checkRecipe(h,inputs,n==4?2:3,n==4?2:3,next);
    inputs.set(0,item(previous.replace(stat,stat.equals("swim")?"fly":"swim")));checkRecipe(h,inputs,n==4?2:3,n==4?2:3,null);
    previous=next;
   }
  }
  checkRecipe(h,TrainingSupplyRules.DRIVES.stream().map(DriveGameTests043::item).toList(),2,2,"chaotic_drive");
  for(String tier:List.of("super_chaotic","unstable_chaotic")){
   String previous=tier.equals("super_chaotic")?"chaotic_drive":"super_chaotic_drive";
   var input=new ArrayList<ItemStack>();for(int i=0;i<9;i++)input.add(item(previous));checkRecipe(h,input,3,3,tier+"_drive");
   input.set(8,item("swim_drive"));checkRecipe(h,input,3,3,null);
  }
  for(boolean mixed:new boolean[]{false,true}){
   var in=new ArrayList<ItemStack>();String[] types={"swim","fly","run","power"};int solid=0;
   for(int i=0;i<9;i++)in.add(i==4?new ItemStack(Items.NETHER_STAR):i%2==1?item("unstable_chaotic_drive"):item("unstable_solid_"+types[mixed?solid++:0]+"_drive"));
   checkRecipe(h,in,3,3,"perfect_drive");in.set(4,item("chaotic_drive"));checkRecipe(h,in,3,3,null);
  }
  for(String id:ids()){
   var tips=new ArrayList<net.minecraft.network.chat.Component>();item(id).getItem().appendHoverText(item(id),Item.TooltipContext.of(h.getLevel()),tips,TooltipFlag.NORMAL);
   if(id.equals("unstable_chaotic_drive"))h.assertTrue(tips.size()==1&&tips.getFirst().getString().equals("ARE YOU SURE YOU WANT TO USE THIS???")&&tips.getFirst().getStyle().isItalic(),"Mysterious tooltip exact and italic");
   else h.assertTrue(!tips.isEmpty(),"Missing benefits tooltip "+id);
  }h.succeed();
 }
 static void use(GameTestHelper h,String id){
  var p=TestConnections.makeSurvivalServerPlayer(h);var observer=TestConnections.makeSurvivalServerPlayer(h);var live=new ChaoEntity[]{chao(h,p)};var uuid=live[0].getUUID();
  p.teleportTo(live[0].getX()+1,live[0].getY(),live[0].getZ());observer.teleportTo(p.getX(),p.getY(),p.getZ()+1);
  if(id.equals("perfect_drive")){Arrays.fill(live[0].state().levels,99);Arrays.fill(live[0].state().progress,0);live[0].state().levels[4]=26;}
  var offered=item(id);p.setItemInHand(InteractionHand.MAIN_HAND,offered);live[0].mobInteract(p,InteractionHand.MAIN_HAND);
  h.assertTrue(offered.isEmpty()&&live[0].state().driveCount==0,"transfer once before effects");
  h.runAtTickTime(12,()->{var n=new CompoundTag();live[0].saveWithoutId(n);live[0].discard();var copy=ChaoGarden.CHAO.get().create(h.getLevel());copy.load(n);h.getLevel().addFreshEntity(copy);live[0]=copy;});
  h.runAtTickTime(150,()->{
   var c=live[0];h.assertTrue(c.getUUID().equals(uuid)&&c.state().driveCount==1&&c.getMainHandItem().isEmpty(),"one effect after pending-item reload "+id);
   if(id.equals("solid_swim_drive"))h.assertTrue(c.state().levels[0]==27&&c.state().progress[0]==43,"real full level preserves progress");
   if(id.equals("perfect_drive"))h.assertTrue(c.state().levels[4]==99,"Perfect can select Stamina");
   c.sync();var data=c.getEntityData().getNonDefaultValues();var remote=ChaoGarden.CHAO.get().create(h.getLevel());remote.getEntityData().assignValues(data);
   h.assertTrue(remote.getEntityData().getNonDefaultValues().stream().anyMatch(v->v.value() instanceof String encoded&&encoded.equals(c.state().encode())),"Observer receives identical serialized Analyzer state");
   var n=new CompoundTag();c.saveWithoutId(n);var saved=ChaoGarden.CHAO.get().create(h.getLevel());saved.load(n);
   h.assertTrue(saved.state().encode().equals(c.state().encode())&&saved.getUUID().equals(uuid)&&saved.owner.equals(p.getUUID()),"identity and stats persist");
   c.discard();p.server.getPlayerList().remove(p);observer.server.getPlayerList().remove(observer);h.succeed();
  });
 }
 @GameTest(template="activity_test",timeoutTicks=220)
 public static void maxedPerfectReturned(GameTestHelper h){
  var p=TestConnections.makeSurvivalServerPlayer(h);var c=chao(h,p);Arrays.fill(c.state().levels,99);Arrays.fill(c.state().progress,0);var offered=item("perfect_drive");
  p.setItemInHand(InteractionHand.MAIN_HAND,offered);c.mobInteract(p,InteractionHand.MAIN_HAND);
  h.runAtTickTime(150,()->{h.assertTrue(c.state().driveCount==0,"maxed Perfect must not be used");
   int count=h.getLevel().getEntitiesOfClass(ItemEntity.class,c.getBoundingBox().inflate(8),e->e.getItem().is(ChaoGarden.item("perfect_drive"))).stream().mapToInt(e->e.getItem().getCount()).sum();
   h.assertTrue(count==1||p.getInventory().countItem(ChaoGarden.item("perfect_drive"))==1,"Perfect returned intact exactly once");
   c.discard();p.server.getPlayerList().remove(p);h.succeed();});
 }
 @GameTest(template="activity_test",timeoutTicks=80)
 public static void stockRefreshAndExistingSave(GameTestHelper h){
  for(int seed=0;seed<200;seed++)for(var listing:MarketStock.select(seed,List.of(),List.of(),false))
   h.assertFalse(ids().contains(listing.itemId()),"combined drive in catalog");
  var trader=MarketContent.TRADER.get().create(h.getLevel());trader.setResident(true);var n=new CompoundTag();trader.getOffers();trader.saveWithoutId(n);
  n.putLong("MarketRestockAt",1);var restored=MarketContent.TRADER.get().create(h.getLevel());restored.load(n);restored.aiStep();
  for(var offer:restored.getOffers())h.assertFalse(offer.getResult().getItem() instanceof CombinedDriveItem,"combined stock after refresh/reload");
  h.assertTrue(restored.getOffers().stream().filter(o->EnemyDriveDrops.drive(o.getResult())).count()==4,"four base drives retained");
  h.succeed();
 }
 @GameTest(template="activity_test",timeoutTicks=100)
 public static void dedicatedDropsAttributionAndEquipment(GameTestHelper h)throws Exception{
  h.assertTrue(AccessoriesBridge.available(),"Requires actual Accessories mod");
  var p=TestConnections.makeSurvivalServerPlayer(h);var other=TestConnections.makeSurvivalServerPlayer(h);var at=Vec3.atCenterOf(h.absolutePos(new BlockPos(5,2,5)));
  var random=net.minecraft.util.RandomSource.create(43);int normal=0;for(int i=0;i<10000;i++)if(EnemyDriveDrops.shouldDrop(p,random))normal++;
  h.assertTrue(normal>420&&normal<580,"normal drop rate near 5%, actual="+normal);
  for(boolean adult:new boolean[]{false,true}){
   var carrier=equip(h,p,adult);h.assertTrue(EnemyDriveDrops.equipped(p),"baby/adult real accessory detected");
   for(int i=0;i<1000;i++)h.assertTrue(EnemyDriveDrops.shouldDrop(p,random),"equipped guarantees reward");
   var mob=EntityType.ZOMBIE.create(h.getLevel());mob.moveTo(at);mob.setNoAi(true);mob.setNoGravity(true);h.getLevel().addFreshEntity(mob);
   mob.hurt(mob.damageSources().playerAttack(p),1);h.assertTrue(EnemyDriveDrops.credited(mob)==p,"player last-hit attribution");
   mob.invulnerableTime=0;mob.hurt(mob.damageSources().playerAttack(other),1);h.assertTrue(EnemyDriveDrops.credited(mob)==other&&!EnemyDriveDrops.equipped(other),"other player's equipped status not borrowed");
   mob.invulnerableTime=0;mob.hurt(mob.damageSources().playerAttack(p),Float.MAX_VALUE);
   int count=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(BlockPos.containing(at)).inflate(1),e->EnemyDriveDrops.drive(e.getItem())).stream().mapToInt(e->e.getItem().getCount()).sum();
   h.assertTrue(count==1,"real equipped hostile kill drops exactly one base drive");
   h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(BlockPos.containing(at)).inflate(2)).forEach(ItemEntity::discard);mob.discard();
   slot(p).setItem(0,ItemStack.EMPTY);p.getInventory().add(carrier);h.assertFalse(EnemyDriveDrops.equipped(p),"inventory-only Chao gives no guarantee");
  }
  var pet=chao(h,p);var victim=EntityType.ZOMBIE.create(h.getLevel());victim.moveTo(at);h.getLevel().addFreshEntity(victim);victim.hurt(victim.damageSources().mobAttack(pet),1);
  h.assertTrue(EnemyDriveDrops.credited(victim)==p,"Chao attack resolves owner");victim.discard();pet.discard();
  p.server.getPlayerList().remove(p);other.server.getPlayerList().remove(other);h.succeed();
 }
 @GameTest(template="activity_test",timeoutTicks=220,batch="drive43_0") public static void use_solid_swim_drive(GameTestHelper h){use(h,"solid_swim_drive");}
 @GameTest(template="activity_test",timeoutTicks=220,batch="drive43_1") public static void use_solid_fly_drive(GameTestHelper h){use(h,"solid_fly_drive");}
 @GameTest(template="activity_test",timeoutTicks=220,batch="drive43_2") public static void use_solid_run_drive(GameTestHelper h){use(h,"solid_run_drive");}
 @GameTest(template="activity_test",timeoutTicks=220,batch="drive43_3") public static void use_solid_power_drive(GameTestHelper h){use(h,"solid_power_drive");}
 @GameTest(template="activity_test",timeoutTicks=220,batch="drive43_4") public static void use_super_solid_swim_drive(GameTestHelper h){use(h,"super_solid_swim_drive");}
 @GameTest(template="activity_test",timeoutTicks=220,batch="drive43_5") public static void use_super_solid_fly_drive(GameTestHelper h){use(h,"super_solid_fly_drive");}
 @GameTest(template="activity_test",timeoutTicks=220,batch="drive43_6") public static void use_super_solid_run_drive(GameTestHelper h){use(h,"super_solid_run_drive");}
 @GameTest(template="activity_test",timeoutTicks=220,batch="drive43_7") public static void use_super_solid_power_drive(GameTestHelper h){use(h,"super_solid_power_drive");}
 @GameTest(template="activity_test",timeoutTicks=220,batch="drive43_8") public static void use_unstable_solid_swim_drive(GameTestHelper h){use(h,"unstable_solid_swim_drive");}
 @GameTest(template="activity_test",timeoutTicks=220,batch="drive43_9") public static void use_unstable_solid_fly_drive(GameTestHelper h){use(h,"unstable_solid_fly_drive");}
 @GameTest(template="activity_test",timeoutTicks=220,batch="drive43_10") public static void use_unstable_solid_run_drive(GameTestHelper h){use(h,"unstable_solid_run_drive");}
 @GameTest(template="activity_test",timeoutTicks=220,batch="drive43_11") public static void use_unstable_solid_power_drive(GameTestHelper h){use(h,"unstable_solid_power_drive");}
 @GameTest(template="activity_test",timeoutTicks=220,batch="drive43_12") public static void use_chaotic_drive(GameTestHelper h){use(h,"chaotic_drive");}
 @GameTest(template="activity_test",timeoutTicks=220,batch="drive43_13") public static void use_super_chaotic_drive(GameTestHelper h){use(h,"super_chaotic_drive");}
 @GameTest(template="activity_test",timeoutTicks=220,batch="drive43_14") public static void use_unstable_chaotic_drive(GameTestHelper h){use(h,"unstable_chaotic_drive");}
 @GameTest(template="activity_test",timeoutTicks=220,batch="drive43_15") public static void use_perfect_drive(GameTestHelper h){use(h,"perfect_drive");}
 @GameTest(template="activity_test",timeoutTicks=260,batch="equipped43")
 public static void equippedDriveUpdatesCarrier(GameTestHelper h)throws Exception{
  var p=TestConnections.makeSurvivalServerPlayer(h);var stack=equip(h,p,true);var id=ChaoCarrierItem.storedUUID(stack);
  h.runAtTickTime(30,()->{var c=(ChaoEntity)h.getLevel().getEntity(id);h.assertTrue(c!=null,"equipped body exists");c.state().energy=100;
   p.setItemInHand(InteractionHand.MAIN_HAND,item("solid_power_drive"));c.mobInteract(p,InteractionHand.MAIN_HAND);});
  h.runAtTickTime(200,()->{var c=(ChaoEntity)h.getLevel().getEntity(id);var s=ChaoCarrierItem.state(stack);
   h.assertTrue(c!=null&&c.state().driveCount==1&&s.driveCount==1&&s.levels[3]==27&&s.progress[3]==43,"one equipped use updates authoritative carrier and body");
   try{slot(p).setItem(0,ItemStack.EMPTY);}catch(Exception ex){throw new RuntimeException(ex);}
   p.server.getPlayerList().remove(p);h.succeed();});
 }
 @GameTest(template="activity_test",timeoutTicks=100) public static void baseDropRegression(GameTestHelper h){TrainingSupplyGameTests.actualEnemyKillsRollRareBaseDriveIncludingLootingAndHeldDrops(h);}
 @GameTest(template="activity_test",timeoutTicks=100) public static void dragonDeathRegression(GameTestHelper h){TrainingSupplyGameTests.dragonDeathSequenceAwardsOneDriveAcrossAllExperienceBurstsAndReload(h);}

}
