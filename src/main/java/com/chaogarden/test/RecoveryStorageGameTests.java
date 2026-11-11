package com.chaogarden.test;
import com.chaogarden.ChaoGarden;
import com.chaogarden.item.ChaoCarrierItem;
import com.chaogarden.recovery.ChaoRecovery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("chao_storage_tests") @PrefixGameTestTemplate(false)
public final class RecoveryStorageGameTests {
 static void storage(GameTestHelper h,String id,boolean full){
   var p=TestConnections.makeSurvivalServerPlayer(h);for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));
   var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));h.assertTrue(item!=Items.AIR,"Real storage mod missing "+id);
   var bag=new ItemStack(item);p.setItemInHand(InteractionHand.MAIN_HAND,bag);item.use(h.getLevel(),p,InteractionHand.MAIN_HAND);p.closeContainer();
   var handler=bag.getCapability(Capabilities.ItemHandler.ITEM);h.assertTrue(handler!=null&&handler.getSlots()>0,"Real opened bag exposes no slots");
   if(full)for(int i=0;i<handler.getSlots();i++)h.assertTrue(handler.insertItem(i,new ItemStack(Items.STONE,handler.getSlotLimit(i)),false).isEmpty(),"Could not fill actual storage");
   var c=ChaoGarden.CHAO.get().create(h.getLevel());c.owner=p.getUUID();c.state().adult=true;c.state().bond=53;c.state().ageTicks=500;
   c.moveTo(h.absolutePos(new BlockPos(2,2,2)).getBottomCenter());h.getLevel().addFreshEntity(c);c.hurt(c.damageSources().genericKill(),10000);
   ChaoRecovery.get(p.server).retry(p.server);ChaoRecovery.get(p.server).retry(p.server);
   int found=0;for(int i=0;i<handler.getSlots();i++)if(c.getUUID().equals(ChaoCarrierItem.storedUUID(handler.getStackInSlot(i)))){
     found++;h.assertTrue(ChaoCarrierItem.state(handler.getStackInSlot(i)).bond==53&&ChaoCarrierItem.state(handler.getStackInSlot(i)).ageTicks==500,"Storage changed Chao state");
   }
   var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(10),e->c.getUUID().equals(ChaoCarrierItem.storedUUID(e.getItem())));
   h.assertTrue(found==(full?0:1)&&drops.size()==(full?1:0),"Storage route count wrong: "+id+" stored="+found+" dropped="+drops.size());
   var reload=ItemStack.parseOptional(p.registryAccess(),(CompoundTag)bag.save(p.registryAccess()));var restored=reload.getCapability(Capabilities.ItemHandler.ITEM);
   int persisted=0;for(int i=0;i<restored.getSlots();i++)if(c.getUUID().equals(ChaoCarrierItem.storedUUID(restored.getStackInSlot(i))))persisted++;
   h.assertTrue(persisted==found,"Reloaded bag lost reference to Chao");drops.forEach(ItemEntity::discard);p.server.getPlayerList().remove(p);h.succeed();
 }
 @GameTest(template="empty") public static void dankAccepts(GameTestHelper h){storage(h,"dankstorage:dank_1",false);}
 @GameTest(template="empty") public static void dankFullDrops(GameTestHelper h){storage(h,"dankstorage:dank_1",true);}
 @GameTest(template="empty") public static void backpackAccepts(GameTestHelper h){storage(h,"sophisticatedbackpacks:backpack",false);}
 @GameTest(template="empty") public static void backpackFullDrops(GameTestHelper h){storage(h,"sophisticatedbackpacks:backpack",true);}
}
