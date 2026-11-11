package com.chaogarden.recovery;

import com.chaogarden.ChaoGarden;
import com.chaogarden.activities.*;
import com.chaogarden.companion.CompanionContent;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoCarrierItem;
import com.mojang.logging.LogUtils;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;

/** Damage-only recovery journal. Pending snapshots own the identity; delivered receipts do not store a second Chao. */
@EventBusSubscriber(modid=ChaoGarden.MODID)
public final class ChaoRecovery extends SavedData {
  public static final String TICKET="ChaoRecoveryTicket";
  private static final int PENDING=0, ITEM=1, WORLD=2, REVIEW=3;
  private static final class Record {
    UUID id,ticket,owner;
    CompoundTag entity;
    GardenRegistry.Place origin;
    int stage=PENDING;
    String notice="";
    boolean notified;
  }
  private final Map<UUID,Record> records=new LinkedHashMap<>();
  private final Set<UUID> publishing=new HashSet<>();
  public static ChaoRecovery get(MinecraftServer server) {
    return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(ChaoRecovery::new,ChaoRecovery::load),"chao_damage_recovery");
  }
  private static UUID ticket(CompoundTag entity) {
    var tag=entity.getCompound("NeoForgeData");
    return tag.hasUUID(TICKET)?tag.getUUID(TICKET):null;
  }
  public static boolean itemAllowed(MinecraftServer server,ItemStack stack) {
    var data=get(server);var r=data.records.get(ChaoCarrierItem.storedUUID(stack));
    // An actual recovered item found after an ambiguous storage error is the delivery receipt.
    if(r!=null&&r.stage==REVIEW&&r.ticket.equals(ticket(ChaoCarrierItem.entityData(stack))))
      data.delivered(r,ITEM,"as the recovered item you retrieved");
    return r==null || r.stage==ITEM && r.ticket.equals(ticket(ChaoCarrierItem.entityData(stack)));
  }
  public static void captured(ChaoEntity chao) {
    if(!(chao.level() instanceof ServerLevel level))return;
    var data=get(level.getServer());var r=data.records.get(chao.getUUID());
    if(r!=null && r.stage==WORLD){r.stage=ITEM;data.setDirty();}
    GardenRegistry.get(level.getServer()).rememberHome(chao);
  }
  public static void released(ChaoEntity chao) {
    if(!(chao.level() instanceof ServerLevel level))return;
    var data=get(level.getServer());var r=data.records.get(chao.getUUID());
    if(r!=null && r.stage==ITEM){r.stage=WORLD;data.setDirty();}
  }
  public static void lifecycleFinished(ChaoEntity chao) {
    if(chao.level() instanceof ServerLevel level){var data=get(level.getServer());
      var r=data.records.get(chao.getUUID());if(r!=null&&r.stage==WORLD){data.records.remove(r.id);data.setDirty();}
    }
  }
  public int pendingCount() { return (int)records.values().stream().filter(r->r.stage==PENDING||r.stage==REVIEW).count(); }

  @SubscribeEvent(priority=EventPriority.LOWEST)
  public static void death(LivingDeathEvent event) {
    if(event.isCanceled() || !(event.getEntity() instanceof ChaoEntity chao)
        || !(chao.level() instanceof ServerLevel level) || GardenRegistry.managed(chao)
        || ActivityManager.isBusy(chao) || GardenRegistry.constructing(chao) || !chao.mayRecoverFromDamage())return;
    var data=get(level.getServer());var old=data.records.get(chao.getUUID());
    event.setCanceled(true);
    if(old!=null && (old.stage!=WORLD || !old.ticket.equals(chao.getPersistentData().hasUUID(TICKET)?chao.getPersistentData().getUUID(TICKET):null))) {
      chao.discard();return; // Repeated death callback or an obsolete saved body cannot publish another copy.
    }
    GardenRegistry.get(level.getServer()).rememberHome(chao);
    com.chaogarden.toys.ToyContent.endPlaying(chao,false);
    var r=new Record();r.id=chao.getUUID();r.ticket=UUID.randomUUID();r.owner=chao.owner;
    r.origin=GardenRegistry.Place.at(level,chao.blockPosition());
    var carrier=new ItemStack(ChaoGarden.item("chao_carrier"));ChaoCarrierItem.snapshot(carrier,chao);
    r.entity=ChaoCarrierItem.entityData(carrier);
    r.entity.putFloat("Health",chao.getMaxHealth());r.entity.putShort("DeathTime",(short)0);
    r.entity.putShort("HurtTime",(short)0);r.entity.putShort("Fire",(short)0);r.entity.putFloat("FallDistance",0);
    var motion=new ListTag();for(int i=0;i<3;i++)motion.add(DoubleTag.valueOf(0));r.entity.put("Motion",motion);
    r.entity.getCompound("NeoForgeData").putUUID(TICKET,r.ticket);
    // getCompound returns a detached empty tag if no persistent data existed.
    var persistent=r.entity.getCompound("NeoForgeData");persistent.putUUID(TICKET,r.ticket);r.entity.put("NeoForgeData",persistent);
    data.records.put(r.id,r);data.setDirty();
    chao.discard(); // Cancelled death skips vanilla loot/equipment; the complete snapshot owns every stack.
  }

  @SubscribeEvent(priority=EventPriority.HIGHEST)
  public static void stale(EntityJoinLevelEvent event) {
    if(!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ChaoEntity chao))return;
    var data=get(level.getServer());var r=data.records.get(chao.getUUID());if(r==null)return;
    UUID bodyTicket=chao.getPersistentData().hasUUID(TICKET)?chao.getPersistentData().getUUID(TICKET):null;
    boolean match=r.ticket.equals(bodyTicket);
    boolean allowed=match&&(r.stage==WORLD || data.publishing.contains(r.id)
        || r.stage==ITEM&&!event.loadedFromDisk());
    if(!allowed)event.setCanceled(true);
  }

  @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
    if(event.getServer().overworld().getGameTime()%20==0)get(event.getServer()).retry(event.getServer());
  }
  public void retry(MinecraftServer server) {
    // Copies protect against reentrant event listeners causing another damage death.
    for(var r:new ArrayList<>(records.values())) {
      if(r.stage==PENDING && publishing.add(r.id)) {
        try { deliver(server,r); }
        catch(RuntimeException failure) {
          r.stage=REVIEW;r.notice="Recovery is safely held for review after a storage error. Please contact the server operator.";
          setDirty();LogUtils.getLogger().error("Chao recovery {} held; ambiguous external write must not be retried automatically",r.id,failure);
        } finally {publishing.remove(r.id);}
      }
      if(!r.notice.isEmpty()&&!r.notified&&r.owner!=null){
        var owner=server.getPlayerList().getPlayer(r.owner);
        if(owner!=null){owner.sendSystemMessage(Component.literal(r.notice));r.notified=true;setDirty();}
      }
    }
  }
  private void delivered(Record r,int stage,String destination) {
    r.stage=stage;r.entity=null;r.notice="Your Chao recovered "+destination+".";r.notified=false;setDirty();
  }
  private void deliver(MinecraftServer server,Record r) {
    if(CompanionContent.findEntity(server,r.id)!=null)return;
    var gardens=GardenRegistry.get(server);var mark=gardens.markedHome(r.id,r.owner);
    if(mark!=null){
      var garden=gardens.gardens.get(mark.garden());var area=gardens.areas.get(mark.garden());
      var level=mark.garden().level(server);
      var previous=gardens.entries.get(r.id);
      boolean space=previous!=null&&previous.garden.equals(mark.garden())||gardens.firstSlot(mark.garden())>=0;
      if(garden!=null&&garden.owner().equals(r.owner)&&area!=null&&level!=null&&space) {
        var chao=ChaoGarden.CHAO.get().create(level);
        if(chao!=null) {
          chao.load(r.entity.copy());
          search: for(int radius=0;radius<=4;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++)for(int dy:new int[]{0,1,-1,2}) {
            var at=area.home.offset(dx,dy,dz);
            if(Math.max(Math.abs(dx),Math.abs(dz))!=radius||!level.getWorldBorder().isWithinBounds(at)||!area.contains(level,at.getBottomCenter()))continue;
            level.getChunkAt(at);
            chao.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
            if(!level.getFluidState(at).isEmpty()||!level.getFluidState(at.below()).isEmpty()
                ||level.getBlockState(at.below()).getCollisionShape(level,at.below()).isEmpty()||!level.noCollision(chao)
                ||!chao.isAlive())continue;
            if(level.addFreshEntity(chao)){
              gardens.placed(chao);gardens.update(chao);
              delivered(r,WORLD,"at "+garden.name()+" ("+mark.garden().dimension()+" "+at.toShortString()+")");return;
            }
            break search; // A vetoed garden spawn is not retried at dozens of positions this tick.
          }
        }
      }
    }
    var owner=r.owner==null?null:server.getPlayerList().getPlayer(r.owner);
    if(r.owner!=null&&owner==null)return; // Offline: retain the only snapshot until its owner can receive it.
    var stack=new ItemStack(ChaoGarden.item("chao_carrier"));ChaoCarrierItem.storeEntity(stack,r.entity);
    if(r.entity.contains("CustomName",8)){
      try{stack.set(DataComponents.CUSTOM_NAME,Component.Serializer.fromJson(r.entity.getString("CustomName"),server.registryAccess()));}
      catch(RuntimeException ignored) { /* The full original name remains in the entity snapshot. */ }
    }
    CompanionContent.token(stack,true);
    if(owner!=null) {
      for(int i=0;i<36;i++)if(owner.getInventory().getItem(i).isEmpty()){
        owner.getInventory().setItem(i,stack);owner.getInventory().setChanged();owner.containerMenu.broadcastChanges();
        delivered(r,ITEM,"into your inventory");return;
      }
      // Public item capability only. Respect slot filters, locks and component-sensitive storage.
      for(int i=0;i<owner.getInventory().getContainerSize();i++){
        var storage=owner.getInventory().getItem(i);if(storage.isEmpty())continue;
        var handler=storage.getCapability(Capabilities.ItemHandler.ITEM);
        if(handler!=null&&insert(handler,stack)){
          owner.getInventory().setChanged();owner.containerMenu.broadcastChanges();delivered(r,ITEM,"into your carried storage");return;
        }
      }
      for(var accessory:com.chaogarden.companion.AccessoriesBridge.storageAccessories(owner)){
        var handler=accessory.stack().getCapability(Capabilities.ItemHandler.ITEM);
        if(handler!=null&&insert(handler,stack)){
          accessory.changed().run();delivered(r,ITEM,"into your equipped storage");return;
        }
      }
    }
    ServerLevel level=owner==null?r.origin.level(server):owner.serverLevel();if(level==null)return;
    Vec3 at=owner==null?r.origin.block().getBottomCenter():owner.position();
    if(at.y<level.getMinBuildHeight()||at.y>level.getMaxBuildHeight()||!level.getWorldBorder().isWithinBounds(BlockPos.containing(at)))return;
    level.getChunkAt(BlockPos.containing(at));
    var dropped=new ItemEntity(level,at.x,at.y+.5,at.z,stack);dropped.setUUID(r.ticket);
    dropped.setExtendedLifetime();dropped.setDefaultPickUpDelay();
    if(owner!=null)dropped.setTarget(owner.getUUID());
    boolean added=level.addFreshEntity(dropped);
    // Entity replacement listeners can cancel the original after publishing their own ItemEntity.
    boolean replacement=level.getEntitiesOfClass(ItemEntity.class,dropped.getBoundingBox().inflate(8),item->
        item.isAlive()&&r.id.equals(ChaoCarrierItem.storedUUID(item.getItem()))
            &&r.ticket.equals(ticket(ChaoCarrierItem.entityData(item.getItem())))).size()>0;
    if(added||replacement)delivered(r,ITEM,"as an item at "+level.dimension().location()+" "+BlockPos.containing(at).toShortString());
  }
  /** Simulation and real insertion both receive private copies; a rejection never mutates the journal. */
  public static boolean insert(IItemHandler handler,ItemStack stack) {
    for(int slot=0;slot<handler.getSlots();slot++) {
      if(!handler.insertItem(slot,stack.copy(),true).isEmpty())continue;
      ItemStack remainder;
      try {remainder=handler.insertItem(slot,stack.copy(),false);}
      catch(RuntimeException error){if(contains(handler,stack))return true;throw error;}
      if(remainder.isEmpty()){
        if(contains(handler,stack))return true;
        throw new IllegalStateException("Storage consumed recovery without exposing the stored item (possibly a void filter)");
      }
      if(contains(handler,stack))return true; // A contradictory rejection cannot cause a second insertion.
      if(!ItemStack.matches(remainder,stack))throw new IllegalStateException("Storage returned a changed recovery item");
    }
    return false;
  }
  private static boolean contains(IItemHandler handler,ItemStack stack) {
    for(int i=0;i<handler.getSlots();i++)if(ItemStack.isSameItemSameComponents(handler.getStackInSlot(i),stack))return true;
    return false;
  }
  @Override public CompoundTag save(CompoundTag root,HolderLookup.Provider lookup) {
    var list=new ListTag();for(var r:records.values()){
      var n=new CompoundTag();n.putUUID("Id",r.id);n.putUUID("Ticket",r.ticket);
      if(r.owner!=null)n.putUUID("Owner",r.owner);if(r.entity!=null)n.put("Entity",r.entity.copy());
      n.putString("Dimension",r.origin.dimension());n.putLong("Pos",r.origin.pos());n.putInt("Stage",r.stage);
      n.putString("Notice",r.notice);n.putBoolean("Notified",r.notified);list.add(n);
    }root.put("Records",list);return root;
  }
  public static ChaoRecovery load(CompoundTag root,HolderLookup.Provider lookup) {
    var data=new ChaoRecovery();for(var tag:root.getList("Records",10)){
      var n=(CompoundTag)tag;if(!n.hasUUID("Id")||!n.hasUUID("Ticket"))continue;
      var r=new Record();r.id=n.getUUID("Id");r.ticket=n.getUUID("Ticket");r.owner=n.hasUUID("Owner")?n.getUUID("Owner"):null;
      r.entity=n.contains("Entity",10)?n.getCompound("Entity").copy():null;r.stage=n.getInt("Stage");
      if(r.stage<0||r.stage>3||((r.stage==PENDING||r.stage==REVIEW)&&r.entity==null))continue;
      r.origin=new GardenRegistry.Place(n.getString("Dimension"),n.getLong("Pos"));r.notice=n.getString("Notice");r.notified=n.getBoolean("Notified");data.records.put(r.id,r);
    }return data;
  }
}
