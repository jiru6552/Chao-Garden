package com.chaogarden.hub;

import com.chaogarden.ChaoGarden;
import com.chaogarden.activities.GardenRegistry;
import com.chaogarden.companion.AccessoriesBridge;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoCarrierItem;
import com.mojang.logging.LogUtils;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Acquisition evidence is read from real server state. It never recreates a
 * creature, writes an inventory, loads an offline player, or forces a source chunk. */
@EventBusSubscriber(modid=ChaoGarden.MODID)
public final class GardenUnlocks081 {
  private static final int STACK_BUDGET=512, NESTING_LIMIT=4, PENDING_LIMIT=4096;
  private record ChunkWork(ServerLevel level,int x,int z){}
  private static final ArrayDeque<ChunkWork> CHUNKS=new ArrayDeque<>();
  private static final Set<ChunkWork> QUEUED=new HashSet<>();
  private static final WeakHashMap<MinecraftServer,Long> NEXT_RETRY=new WeakHashMap<>();
  private static final WeakHashMap<Object,Integer> ROOT_OFFSETS=new WeakHashMap<>();
  private static final WeakHashMap<LevelChunk,Integer> CHUNK_OFFSETS=new WeakHashMap<>();
  private static final WeakHashMap<MinecraftServer,RecoveryScan> RECOVERY_SCANS=new WeakHashMap<>();
  private static final class RecoveryScan {
    final java.util.List<String> states=new java.util.ArrayList<>();int next;
    RecoveryScan(net.minecraft.nbt.CompoundTag snapshot){
      for(var raw:snapshot.getList("Records",10)){
        var row=(net.minecraft.nbt.CompoundTag)raw;var entity=row.getCompound("Entity");
        if(row.hasUUID("Owner")&&entity.hasUUID("UUID")&&!entity.getString("Chao").isEmpty())states.add(entity.getString("Chao"));
      }
    }
    boolean consume(HubWorldData data){
      boolean changed=false;int end=Math.min(states.size(),next+128);
      while(next<end&&!data.allGardensUnlocked())changed|=recordHistorical(data,ChaoState.decode(states.get(next++)));
      if(data.allGardensUnlocked()||next==states.size()){states.clear();next=0;}return changed;
    }
  }
  private GardenUnlocks081(){}

  public static boolean isUnlocked(MinecraftServer server,boolean hero){return HubWorldData.get(server).gardenUnlocked(hero);}
  public static boolean canUsePortal(MinecraftServer server,boolean hero){
    if(!isUnlocked(server,hero))return false;
    var level=server.getLevel(HubContent.HUB);
    return level!=null&&HubRooms.accessInstalled081(level,hero);
  }
  /** Child alignment is already visible in the mod; an adult always uses its locked identity. */
  public static int qualifyingAlignment(ChaoState state){
    if(state==null||state.dead||state.intTrait("content.construction")>0)return 0;
    float alignment=state.adult?state.lockedAlignment():state.align;
    return !Float.isFinite(alignment)?0:alignment>=.5f?1:alignment<=-.5f?-1:0;
  }
  public static boolean record(HubWorldData data,ChaoState state){
    int alignment=qualifyingAlignment(state);
    return alignment!=0&&data.unlockGarden(alignment>0);
  }
  /** Used only after validating retained ownership and creature identity. A
   * terminal snapshot still proves its owner's earlier acquisition; reading it
   * never revives or republishes the creature. Ordinary live admission above
   * deliberately retains its dead-state exclusion. */
  private static boolean recordHistorical(HubWorldData data,ChaoState state){
    if(state==null||state.intTrait("content.construction")>0)return false;
    float alignment=state.adult?state.lockedAlignment():state.align;
    if(!Float.isFinite(alignment)||alignment>-.5f&&alignment<.5f)return false;
    return data.unlockGarden(alignment>=.5f);
  }
  /** Called after successful care/editor transactions. Unpublished previews and
   * unowned creatures do not prove that a player has obtained a Chao. */
  public static void observe(ChaoEntity chao){
    if(!(chao.level() instanceof ServerLevel level)||chao.owner==null||chao.isRemoved()||!chao.isAlive())return;
    try{
      var data=HubWorldData.get(level.getServer());
      if(data.allGardensUnlocked()||level.getEntity(chao.getUUID())!=chao)return;
      if(record(data,chao.state()))refreshIfPresent(level.getServer());
    }catch(RuntimeException failure){
      // Scenery failure must never split a capture into a filled item plus a live body.
      LogUtils.getLogger().error("Chao garden acquisition observation deferred safely",failure);
    }
  }
  /** Logical unlock commits first. A blocked construction remains pending and
   * actual portal entry stays denied until the guarded scene installation succeeds. */
  public static boolean unlock(MinecraftServer server,boolean hero){
    boolean changed=HubWorldData.get(server).unlockGarden(hero);refreshIfPresent(server);return changed;
  }
  private static void refreshIfPresent(MinecraftServer server){
    var level=server.getLevel(HubContent.HUB);var data=HubWorldData.get(server);
    if(level==null||data.revisions.getOrDefault(HubContent.HUB.location().toString(),0)==0
        ||level.getChunkSource().getChunkNow(0,0)==null)return;
    try{HubRooms.refreshUnlocks081(level);}
    catch(RuntimeException failure){LogUtils.getLogger().error("Chao garden access remains pending; existing lobby blocks were preserved",failure);}
  }
  private static final class Scan {
    final HubWorldData data;int remaining=STACK_BUDGET;boolean changed;
    Scan(HubWorldData data){this.data=data;}
    void state(ChaoState state){changed|=record(data,state);}
    void direct(ItemStack stack){
      if(!stack.isEmpty()&&stack.getItem() instanceof ChaoCarrierItem&&ChaoCarrierItem.filled(stack)
          &&ChaoCarrierItem.storedUUID(stack)!=null&&!ChaoCarrierItem.entityData(stack).getString("Chao").isEmpty()){
        var appearance=ChaoCarrierItem.state(stack);
        if(!appearance.dead)state(appearance);
        else if(ChaoCarrierItem.entityData(stack).hasUUID("Keeper"))changed|=recordHistorical(data,appearance);
      }
    }
    void item(ItemStack stack,int depth){
      if(remaining--<=0||depth>NESTING_LIMIT||stack.isEmpty()||data.allGardensUnlocked())return;
      direct(stack);
      if(depth==NESTING_LIMIT)return;
      var contents=stack.get(DataComponents.CONTAINER);
      if(contents!=null){var items=contents.stream().iterator();while(remaining>0&&items.hasNext())item(items.next(),depth+1);}
      var bundle=stack.get(DataComponents.BUNDLE_CONTENTS);
      if(bundle!=null)for(var nested:bundle.items()){if(remaining<=0)break;item(nested,depth+1);}
      var handler=stack.getCapability(Capabilities.ItemHandler.ITEM);
      if(handler!=null)for(int slot=0;slot<handler.getSlots()&&remaining>0;slot++)item(handler.getStackInSlot(slot),depth+1);
    }
    void roots(java.util.List<ItemStack> roots,Object owner){
      // A huge backpack cannot hide later ordinary inventory/ender/chest slots.
      for(var stack:roots){direct(stack);if(data.allGardensUnlocked())return;}
      if(roots.isEmpty())return;
      int first=Math.floorMod(ROOT_OFFSETS.getOrDefault(owner,0),roots.size());
      ROOT_OFFSETS.put(owner,(first+1)%roots.size());
      for(int count=0;count<roots.size()&&remaining>0;count++)item(roots.get((first+count)%roots.size()),0);
    }
    void container(Container container){
      var roots=new java.util.ArrayList<ItemStack>();
      for(int slot=0;slot<Math.min(4096,container.getContainerSize());slot++)roots.add(container.getItem(slot));
      roots(roots,container);
    }
  }
  /** Read-only bounded component/capability traversal, also used by migration tests. */
  public static boolean observeStored(HubWorldData data,ItemStack stack){
    var scan=new Scan(data);scan.item(stack,0);return scan.changed;
  }
  public static boolean observeStoredContainer(HubWorldData data,Container container){
    if(container instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity random&&random.getLootTable()!=null)return false;
    var scan=new Scan(data);scan.container(container);return scan.changed;
  }
  /** Existing pending recovery snapshots are historical acquisition evidence;
   * observation never delivers, consumes or edits the recovery journal. */
  public static boolean observeRecoverySnapshot(HubWorldData data,net.minecraft.nbt.CompoundTag snapshot){return new RecoveryScan(snapshot).consume(data);}
  public static void scanPlayer(ServerPlayer player){
    var data=HubWorldData.get(player.server);if(data.allGardensUnlocked())return;
    try{
      var scan=new Scan(data);var roots=new java.util.ArrayList<ItemStack>();
      for(int slot=0;slot<player.getInventory().getContainerSize();slot++)roots.add(player.getInventory().getItem(slot));
      for(int slot=0;slot<player.getEnderChestInventory().getContainerSize();slot++)roots.add(player.getEnderChestInventory().getItem(slot));
      roots.addAll(AccessoriesBridge.equipped(player));
      for(var accessory:AccessoriesBridge.storageAccessories(player))roots.add(accessory.stack());
      for(var slot:player.containerMenu.slots){if(roots.size()>=4096)break;roots.add(slot.getItem());}
      roots.add(player.containerMenu.getCarried());scan.roots(roots,player);
      if(scan.changed)refreshIfPresent(player.server);
    }catch(RuntimeException failure){LogUtils.getLogger().warn("Could not inspect one accessible Chao storage; no items were changed",failure);}
  }
  private static void scanChunk(ChunkWork work){
    var level=work.level();var data=HubWorldData.get(level.getServer());if(data.allGardensUnlocked())return;
    var chunk=level.getChunkSource().getChunkNow(work.x(),work.z());if(chunk==null)return;
    try{
      var blocks=new java.util.ArrayList<>(chunk.getBlockEntities().values());
      int first=blocks.isEmpty()?0:Math.floorMod(CHUNK_OFFSETS.getOrDefault(chunk,0),blocks.size());boolean changed=false;
      for(int count=0;count<Math.min(16,blocks.size());count++){
        var block=blocks.get((first+count)%blocks.size());
        // Loot-bearing chests have no concrete inventory yet. Do not roll loot merely to migrate unlocks.
        if(block instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity random&&random.getLootTable()!=null)continue;
        if(block instanceof Container container){var scan=new Scan(data);scan.container(container);changed|=scan.changed;}
      }
      if(!blocks.isEmpty())CHUNK_OFFSETS.put(chunk,(first+Math.min(16,blocks.size()))%blocks.size());
      if(blocks.size()>16&&!data.allGardensUnlocked())enqueue(level,chunk);
      if(changed)refreshIfPresent(level.getServer());
    }catch(RuntimeException failure){LogUtils.getLogger().warn("Could not inspect one loaded Chao container; source storage is unchanged",failure);}
  }
  /** Current loaded bodies and existing owned registry snapshots are enough to
   * recover many old acquisitions without loading their old chunks. */
  public static void rescan(MinecraftServer server){
    var data=HubWorldData.get(server);if(!data.allGardensUnlocked()){
      // Snapshot the existing recovery journal once per server/admin rescan,
      // retaining only immutable encoded states; inspect at most128 per retry.
      RECOVERY_SCANS.computeIfAbsent(server,s->new RecoveryScan(com.chaogarden.recovery.ChaoRecovery.get(s).save(new net.minecraft.nbt.CompoundTag(),s.registryAccess()))).consume(data);
      for(var entry:GardenRegistry.get(server).entries.values())if(entry.owner!=null&&entry.id!=null&&!entry.appearance.isEmpty())recordHistorical(data,ChaoState.decode(entry.appearance));
      for(var level:server.getAllLevels())for(var entity:level.getAllEntities()){
        if(data.allGardensUnlocked())break;
        if(entity instanceof ChaoEntity chao)observe(chao);
        else if(entity instanceof ItemEntity item&&ChaoCarrierItem.entityData(item.getItem()).hasUUID("Keeper"))observeStored(data,item.getItem());
      }
      for(var player:server.getPlayerList().getPlayers()){
        scanPlayer(player);
        var at=player.chunkPosition();
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
          var chunk=player.serverLevel().getChunkSource().getChunkNow(at.x+x,at.z+z);
          if(chunk!=null)enqueue(player.serverLevel(),chunk);
        }
      }
    }
    refreshIfPresent(server);
  }
  private static void enqueue(ServerLevel level,LevelChunk chunk){
    var work=new ChunkWork(level,chunk.getPos().x,chunk.getPos().z);
    if(QUEUED.size()<PENDING_LIMIT&&QUEUED.add(work))CHUNKS.addLast(work);
  }
  @SubscribeEvent public static void chunkLoad(ChunkEvent.Load event){
    if(event.getLevel() instanceof ServerLevel level&&event.getChunk() instanceof LevelChunk chunk)enqueue(level,chunk);
  }
  @SubscribeEvent public static void started(ServerStartedEvent event){rescan(event.getServer());}
  @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){if(event.getEntity() instanceof ServerPlayer player)scanPlayer(player);}
  @SubscribeEvent public static void entityTick(EntityTickEvent.Post event){
    if(event.getEntity() instanceof ChaoEntity chao&&chao.tickCount%20==0)observe(chao);
    else if(event.getEntity() instanceof ServerPlayer player&&player.tickCount%20==0)scanPlayer(player);
  }
  @SubscribeEvent public static void tick(ServerTickEvent.Post event){
    var server=event.getServer();
    for(int i=0;i<2&&!CHUNKS.isEmpty();i++){var work=CHUNKS.removeFirst();QUEUED.remove(work);if(work.level().getServer()==server)scanChunk(work);}
    long now=server.overworld().getGameTime();
    if(now>=NEXT_RETRY.getOrDefault(server,0L)){NEXT_RETRY.put(server,now+200);rescan(server);}
  }
  @SubscribeEvent public static void stop(ServerStoppingEvent event){CHUNKS.removeIf(w->w.level().getServer()==event.getServer());QUEUED.removeIf(w->w.level().getServer()==event.getServer());NEXT_RETRY.remove(event.getServer());RECOVERY_SCANS.remove(event.getServer());ROOT_OFFSETS.clear();CHUNK_OFFSETS.clear();}

  @SubscribeEvent public static void commands(RegisterCommandsEvent event){
    var root=Commands.literal("gardens").requires(source->source.hasPermission(2));
    root.then(Commands.literal("status").executes(c->status(c.getSource())));
    root.then(Commands.literal("rescan").executes(c->{RECOVERY_SCANS.remove(c.getSource().getServer());rescan(c.getSource().getServer());return status(c.getSource());}));
    var unlock=Commands.literal("unlock");
    unlock.then(Commands.literal("hero").executes(c->{unlock(c.getSource().getServer(),true);return status(c.getSource());}));
    unlock.then(Commands.literal("dark").executes(c->{unlock(c.getSource().getServer(),false);return status(c.getSource());}));
    unlock.then(Commands.literal("all").executes(c->{unlock(c.getSource().getServer(),true);unlock(c.getSource().getServer(),false);return status(c.getSource());}));
    root.then(unlock);event.getDispatcher().register(Commands.literal("chao").then(root));
  }
  private static int status(CommandSourceStack source){
    var server=source.getServer();var data=HubWorldData.get(server);var hub=server.getLevel(HubContent.HUB);
    source.sendSuccess(()->Component.literal("Server gardens: Hero "+(data.heroGardenUnlocked()?"unlocked":"locked")+", Dark "+(data.darkGardenUnlocked()?"unlocked":"locked")+". Unlocks are permanent."),false);
    if(hub==null||data.revisions.getOrDefault(HubContent.HUB.location().toString(),0)==0){source.sendSuccess(()->Component.literal("Lobby access is uninitialized. A first visit builds only an empty footprint; unknown existing blocks are preserved and require backed-up restoration of a compatible lobby and its metadata."),false);return 1;}
    try{
      var result=HubRooms.refreshUnlocks081(hub);
      source.sendSuccess(()->Component.literal(result.applied()?"Lobby access matches the saved unlocks.":"Lobby update is pending. Existing changes were preserved; clear actors from the affected path or restore edited cells to the authored layout, then /chao gardens rescan. Conflicts: "+result.conflicts()),false);
      if(data.revisions.getOrDefault(HubContent.HUB.location().toString(),0)<38)source.sendSuccess(()->Component.literal("This lobby predates the supported layout or has an unknown revision. Back up the world and restore a known authored layout before migration; rescan does not rebuild it."),false);
      return 1;
    }catch(RuntimeException failure){
      source.sendFailure(Component.literal("Unlocks are saved, but the lobby update could not be inspected safely. Existing blocks were preserved; see the server log."));
      LogUtils.getLogger().error("Could not inspect Chao garden access",failure);return 0;
    }
  }
}
