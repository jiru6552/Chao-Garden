package com.chaogarden.activities;

import com.chaogarden.entity.ChaoEntity;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/** UUID ledger, never a second serialized Chao inventory. Missing bodies are never recreated. */
public final class GardenRegistry extends SavedData {
  public record Place(String dimension, long pos) {
    public static Place at(ServerLevel level, BlockPos pos) { return new Place(level.dimension().location().toString(), pos.asLong()); }
    public BlockPos block() { return BlockPos.of(pos); }
    public ServerLevel level(MinecraftServer server) {
      ResourceLocation id = ResourceLocation.tryParse(dimension);
      return id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
    }
    CompoundTag tag() { var n = new CompoundTag(); n.putString("Dimension", dimension); n.putLong("Pos", pos); return n; }
    static Place load(CompoundTag tag) { return new Place(tag.getString("Dimension"), tag.getLong("Pos")); }
    static Place readLink(CompoundTag tag) {
      if(!tag.contains("Dimension",8)||!tag.contains("Pos",4))return null;
      var place=load(tag);return validPlace(place)?place:null;
    }
  }
  public record Garden(Place place, UUID owner, String name) {}
  private record Relocation(Place target,UUID owner) {}
  private static final int MAX_RELOCATIONS=4096,MAX_RELOCATION_DEPTH=64;
  private final Map<Place,Relocation> relocations=new LinkedHashMap<>();
  public static final class Entry {
    public UUID id, owner;
    public Place garden, last;
    public String name = "Chao", stats = "";
    public long seen;
    public int slot = -1;
    public String appearance = "";
    public boolean entered, automatic;
    public long inside;
    Entry(UUID id, UUID owner, Place garden, Place last) { this.id=id; this.owner=owner; this.garden=garden; this.last=last; }
  }
  public final Map<Place, GardenArea> areas = new LinkedHashMap<>();
  // A keeper can dismiss a resident even while its chunk is unloaded.
  public record Home(UUID owner, Place garden) {}
  private final Map<UUID,Home> homes=new HashMap<>();
  public Home markedHome(UUID id,UUID owner) {
    var entry=entries.get(id);
    var home=entry!=null?new Home(entry.owner,entry.garden):homes.get(id);
    return home!=null&&home.owner.equals(owner)?new Home(owner,resolvePlace(home.garden)):null;
  }
  public void rememberHome(ChaoEntity chao) {
    var entry=entries.get(chao.getUUID());
    if(entry!=null&&entry.owner.equals(chao.owner)){
      homes.put(chao.getUUID(),new Home(entry.owner,entry.garden));setDirty();
    }
  }
  private final Map<UUID,Place> dismissed = new HashMap<>();
  public final Map<Place, String> messages = new HashMap<>();
  public final Map<Place, Garden> gardens = new LinkedHashMap<>();
  public final Map<UUID, Entry> entries = new LinkedHashMap<>();
  public final Map<Place, UUID> nodeOwners = new LinkedHashMap<>();
  public final Map<Place, Place> links = new LinkedHashMap<>();
  public final Map<UUID, CompoundTag> recoveries = new LinkedHashMap<>();

  public static GardenRegistry get(MinecraftServer server) {
    return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(GardenRegistry::new, GardenRegistry::load), "chao_gardens");
  }
  public Garden register(ServerPlayer player, BlockPos pos) {
    var place = Place.at(player.serverLevel(), GardenTransporterBlock.root(player.serverLevel(),pos));
    Garden existing = gardens.get(place);
    if (existing != null) {
      if(com.chaogarden.hub.HubContent.isGardenDimension(player.level())&&!areas.containsKey(place)){areas.put(place,GardenArea.inspect(player.serverLevel(),place.block()));setDirty();}
      return existing;
    }
    long count = gardens.values().stream().filter(g -> g.owner.equals(player.getUUID())).count();
    Garden garden = new Garden(place, player.getUUID(), "Garden " + (count+1));
    if(com.chaogarden.hub.HubContent.isGardenDimension(player.level()))areas.put(place,GardenArea.inspect(player.serverLevel(),place.block()));
    relocations.remove(place);gardens.put(place, garden); setDirty(); return garden;
  }
  private static boolean validPlace(Place place){
    return place!=null&&place.dimension!=null&&place.dimension.length()<=256&&ResourceLocation.tryParse(place.dimension)!=null
        &&Math.abs((long)place.block().getX())<=29_999_984L&&Math.abs((long)place.block().getZ())<=29_999_984L;
  }
  /** Resolve only at a saved-link read boundary, never while removing a real block. */
  public Place resolvePlace(Place original){
    if(!validPlace(original))return original;
    Place current=original;UUID owner=null;var seen=new HashSet<Place>();
    for(int depth=0;depth<MAX_RELOCATION_DEPTH;depth++){
      if(!seen.add(current))return original;
      var garden=gardens.get(current);
      if(garden!=null)return owner==null||garden.owner.equals(owner)?current:original;
      var move=relocations.get(current);
      if(move==null||!validPlace(move.target)||!current.dimension.equals(move.target.dimension))return original;
      if(owner==null)owner=move.owner;else if(!owner.equals(move.owner))return original;
      current=move.target;
    }
    return original;
  }
  public boolean canRelocateGarden(Place from,Place target){
    return validPlace(from)&&validPlace(target)&&from.dimension.equals(target.dimension)&&!from.equals(target)
        &&gardens.containsKey(from)&&!gardens.containsKey(target)&&!nodeOwners.containsKey(target)
        &&!links.containsKey(target)&&!links.containsValue(target)&&!relocations.containsKey(target)
        &&(relocations.size()<MAX_RELOCATIONS||relocations.containsKey(from));
  }
  /** Keep the original garden/entry owners and identities; destination claims are never merged. */
  public boolean relocateGarden(Place from,Place target){
    if(!canRelocateGarden(from,target))return false;
    dismissed.replaceAll((id,place)->place.equals(from)?target:place);
    homes.replaceAll((id,home)->home.garden.equals(from)?new Home(home.owner,target):home);
    var area=areas.remove(from);if(area!=null)areas.put(target,area);
    var old=gardens.remove(from);gardens.put(target,new Garden(target,old.owner,old.name));
    // Last is a physical observation used to load an unpublished body, not a
    // provider link. Only update(chao) after a real move may change it.
    for(var entry:entries.values())if(entry.garden.equals(from))entry.garden=target;
    var nodeOwner=nodeOwners.remove(from);if(nodeOwner!=null)nodeOwners.put(target,nodeOwner);
    var next=links.remove(from);if(next!=null)links.put(target,next.equals(from)?target:next);
    links.replaceAll((source,dest)->dest.equals(from)?target:dest);
    // Flatten earlier relocations during each move, so ordinary upgrades do not
    // accumulate chains. A malformed saved chain still has a strict read bound.
    var invalid=new ArrayList<Place>();
    relocations.replaceAll((source,move)->{
      if(!move.target.equals(from))return move;
      if(!move.owner.equals(old.owner)){invalid.add(source);return move;}
      return new Relocation(target,move.owner);
    });
    invalid.forEach(relocations::remove);relocations.put(from,new Relocation(target,old.owner));
    setDirty();return true;
  }
  public boolean bind(ServerPlayer player, ChaoEntity chao, Garden garden) {
    if (!garden.owner.equals(player.getUUID()) || !player.getUUID().equals(chao.owner)
        || ActivityManager.isBusy(chao) || managed(chao) || constructing(chao)) return false;
    long count = entries.values().stream().filter(e -> e.garden.equals(garden.place) && !e.id.equals(chao.getUUID())).count();
    if (count >= 6) return false;
    var entry = new Entry(chao.getUUID(), chao.owner, garden.place,
        Place.at((ServerLevel) chao.level(), chao.blockPosition()));
    entry.slot=firstSlot(garden.place);entry.appearance=chao.state().encode();
    entries.put(entry.id, entry); update(chao); setDirty(); return true;
  }
  public int firstSlot(Place place) {
    for(int i=0;i<6;i++){int slot=i;if(entries.values().stream().noneMatch(e->e.garden.equals(place)&&e.slot==slot))return i;}
    return -1;
  }
  public boolean activate(ServerPlayer player, Place place) {
    var garden=gardens.get(place);if(garden==null||!garden.owner.equals(player.getUUID()))return false;
    try { var area=GardenArea.inspect(player.serverLevel(),place.block());areas.put(place,area);
      messages.put(place,area.realm?"Active: this garden covers the whole garden area.":"Active: the device’s chunk and its eight neighboring chunks.");setDirty();return true;
    } catch(IllegalArgumentException error){messages.put(place,error.getMessage());return false;}
  }
  /** Server reads the real inventory slot. A registration never moves, consumes or clones its item. */
  public boolean registerCarried(ServerPlayer player, Place place, int inventorySlot, int targetSlot, UUID expected) {
    var garden=gardens.get(place);
    if(garden==null||!garden.owner.equals(player.getUUID())||!areas.containsKey(place)||inventorySlot<0||inventorySlot>=36||targetSlot<0||targetSlot>=6)return false;
    var stack=player.getInventory().getItem(inventorySlot);
    if(!(stack.getItem() instanceof com.chaogarden.item.ChaoCarrierItem)||!com.chaogarden.item.ChaoCarrierItem.filled(stack))return false;
    var tag=com.chaogarden.item.ChaoCarrierItem.entityData(stack);UUID id=com.chaogarden.item.ChaoCarrierItem.storedUUID(stack);
    if(id==null||!id.equals(expected)||!tag.hasUUID("Keeper")||!tag.getUUID("Keeper").equals(player.getUUID())
        ||com.chaogarden.companion.CompanionContent.itemIsSummoned(stack)||find(player.server,id)!=null)return false;
    var old=entries.get(id);
    if(old!=null&&!old.owner.equals(player.getUUID()))return false;
    if(entries.values().stream().anyMatch(e->e.garden.equals(place)&&e.slot==targetSlot&&!e.id.equals(id)))return false;
    if(entries.values().stream().filter(e->e.garden.equals(place)&&!e.id.equals(id)).count()>=6)return false;
    var state=com.chaogarden.item.ChaoCarrierItem.state(stack);if(state.dead||state.intTrait("content.construction")>0)return false;
    var entry=new Entry(id,player.getUUID(),place,Place.at(player.serverLevel(),player.blockPosition()));
    entry.slot=targetSlot;entry.name=stack.getHoverName().getString();entry.appearance=state.encode();
    dismissed.remove(id);entries.put(id,entry);homes.put(id,new Home(entry.owner,entry.garden));setDirty();return true;
  }
  /** Discover only actual, published bodies. Existing manual reservations always take precedence. */
  public void detect(ChaoEntity chao, boolean transported) {
    if (!(chao.level() instanceof ServerLevel level) || chao.owner == null || !chao.isAlive()
        || chao.state().dead || constructing(chao) || managed(chao) || ActivityManager.isBusy(chao)) return;
    UUID id=chao.getUUID();
    Entry existing=entries.get(id);
    String dimension=level.dimension().location().toString();
    if (existing!=null) {
      if (!existing.automatic) return;
      var home=areas.get(existing.garden);
      if (existing.owner.equals(chao.owner) && home!=null && existing.garden.dimension.equals(dimension)
          && (!transported || home.contains(level,chao.position()))) return;
      entries.remove(id);setDirty();
    }
    var ignored=dismissed.get(id);
    if (ignored!=null) {
      var area=areas.get(ignored);
      if (dimension.equals(ignored.dimension) && area!=null && area.contains(level,chao.position())) return;
      dismissed.remove(id);setDirty();
    }
    Place match=null;
    for (var pair:areas.entrySet()) {
      var place=pair.getKey();var garden=gardens.get(place);
      if (!place.dimension.equals(dimension) || garden==null || !garden.owner.equals(chao.owner)
          || !pair.getValue().contains(level,chao.position())) continue;
      // Overlapping gardens already have a manual chooser. Do not pick an owner or home for the keeper.
      if (match!=null) return;
      match=place;
    }
    if (match==null) return;
    int slot=firstSlot(match);
    if (slot<0) return;
    Entry entry=new Entry(id,chao.owner,match,Place.at(level,chao.blockPosition()));
    entry.automatic=true;entry.entered=true;entry.slot=slot;
    entries.put(id,entry);update(chao);
  }

  public void dismiss(UUID id, Place garden) {
    entries.remove(id);homes.remove(id);dismissed.put(id,garden);setDirty();
  }

  public void placed(ChaoEntity chao) {
    if(dismissed.remove(chao.getUUID())!=null)setDirty();
    detect(chao,true);
  }

  public void departed(ChaoEntity chao, ServerLevel level) {
    UUID id=chao.getUUID();var reason=chao.getRemovalReason();
    if(chao.state().dead || reason==Entity.RemovalReason.KILLED){
      entries.remove(id);homes.remove(id);dismissed.remove(id);recoveries.remove(id);setDirty();
    } else if(reason==Entity.RemovalReason.DISCARDED || reason==Entity.RemovalReason.CHANGED_DIMENSION){
      var e=entries.get(id);
      if(e!=null && e.automatic && e.garden.dimension.equals(level.dimension().location().toString())
          && !ActivityManager.isBusy(chao) && !managed(chao)){entries.remove(id);setDirty();}
    }
  }

  public boolean activeResident(ChaoEntity chao) {
    var entry=entries.get(chao.getUUID());return entry!=null&&entry.entered&&areas.containsKey(entry.garden);
  }
  public void contain(ChaoEntity chao) {
    if(!(chao.level() instanceof ServerLevel level)||managed(chao)||ActivityManager.isBusy(chao)||constructing(chao))return;
    var entry=entries.get(chao.getUUID());if(entry==null||!entry.owner.equals(chao.owner))return;
    var area=areas.get(entry.garden);if(area==null)return;
    if(!entry.garden.dimension.equals(level.dimension().location().toString())){if(entry.entered)ActivityManager.callHome(chao,entry.garden);return;}
    if(area.contains(level,chao.position())) {
      if(!entry.entered){entry.entered=true;setDirty();}
      // Keep only collision-free resting positions for a safe boundary correction.
      if(chao.onGround()&&level.noCollision(chao))entry.inside=chao.blockPosition().asLong();
    } else if(entry.entered) {
      BlockPos safe=BlockPos.of(entry.inside);
      if(entry.inside==0||!area.contains(level,safe.getCenter())||!level.getBlockState(safe).getCollisionShape(level,safe).isEmpty())safe=area.home;
      if(level.hasChunkAt(safe)){
        chao.getNavigation().stop();chao.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        chao.teleportTo(safe.getX()+.5,safe.getY(),safe.getZ()+.5);
      }
    }
  }
  public void update(ChaoEntity chao) {
    Entry entry = entries.get(chao.getUUID());
    if (entry == null || chao.owner == null || managed(chao)) return;
    if (!entry.owner.equals(chao.owner)) { entries.remove(chao.getUUID()); setDirty(); return; }
    rememberHome(chao);
    entry.last = Place.at((ServerLevel)chao.level(), chao.blockPosition());
    entry.name = chao.getName().getString();entry.appearance=chao.state().encode();
    var s = chao.state();
    entry.stats = "Swim " + s.levels[0] + "  Fly " + s.levels[1] + "  Run " + s.levels[2]
        + "  Power " + s.levels[3] + "  Stamina " + s.levels[4];
    entry.seen = chao.level().getGameTime(); setDirty();
  }
  public static boolean constructing(ChaoEntity chao) { return chao.state().intTrait("content.construction")>0; }
  /** Bodies controlled by another lifecycle remain registered but cannot be moved into gardens or contests. */
  public static boolean managed(ChaoEntity chao) { return chao.getPersistentData().contains("ChaoCompanion")
      ||chao instanceof com.chaogarden.kindergarten.KindergartenPupil0257
      ||com.chaogarden.kindergarten.KindergartenContent.isBoarded(chao); }
  public static ChaoEntity find(MinecraftServer server, UUID id) {
    for (ServerLevel level : server.getAllLevels()) {
      Entity entity = level.getEntity(id);
      if (entity instanceof ChaoEntity chao && chao.isAlive()) return chao;
    }
    return null;
  }
  public void removeGarden(Place place) {
    // In particular, removal of an obsolete physical provider must NOT resolve
    // its old alias and erase the garden that was just migrated elsewhere.
    dismissed.values().removeIf(place::equals);areas.remove(place);messages.remove(place);var removed=gardens.remove(place);entries.values().removeIf(e -> e.garden.equals(place));
    if(removed!=null)relocations.entrySet().removeIf(e->e.getValue().target.equals(place));
    setDirty();
  }
  @Override public CompoundTag save(CompoundTag root, HolderLookup.Provider lookup) {
    var gs = new ListTag();
    for (Garden garden : gardens.values()) {
      var n = garden.place.tag(); n.putUUID("Owner", garden.owner); n.putString("Name", garden.name); if(areas.containsKey(garden.place))n.put("Area",areas.get(garden.place).save());gs.add(n);
    }
    root.put("Gardens", gs);
    var moved=new ListTag();
    for(var pair:relocations.entrySet()){
      var target=resolvePlace(pair.getKey());if(target.equals(pair.getKey()))continue;
      var n=pair.getKey().tag();n.put("Target",target.tag());n.putUUID("Owner",pair.getValue().owner);moved.add(n);
    }
    root.put("Relocations",moved);
    var es = new ListTag();
    for (Entry entry : entries.values()) {
      var n = new CompoundTag(); n.putUUID("Id", entry.id); n.putUUID("Owner", entry.owner);
      n.put("Garden", entry.garden.tag()); n.put("Last", entry.last.tag()); n.putString("Name", entry.name);
      n.putInt("Slot",entry.slot);n.putString("Appearance",entry.appearance);n.putBoolean("Entered",entry.entered);n.putBoolean("Automatic",entry.automatic);n.putLong("Inside",entry.inside);
      n.putString("Stats", entry.stats); n.putLong("Seen", entry.seen); es.add(n);
    }
    root.put("Entries", es);
    var exclusions=new ListTag();
    dismissed.forEach((id,place)->{var n=place.tag();n.putUUID("Id",id);exclusions.add(n);});
    root.put("Dismissed",exclusions);
    var marked=new ListTag();homes.forEach((id,home)->{
      var n=home.garden.tag();n.putUUID("Id",id);n.putUUID("Owner",home.owner);marked.add(n);
    });root.put("MarkedHomes",marked);
    var ns = new ListTag();
    for (var pair : nodeOwners.entrySet()) {
      var n = pair.getKey().tag(); n.putUUID("Owner", pair.getValue());
      if (links.containsKey(pair.getKey())) n.put("Next", links.get(pair.getKey()).tag()); ns.add(n);
    }
    root.put("Nodes", ns);
    var rs = new ListTag();
    for (var pair : recoveries.entrySet()) { var n = pair.getValue().copy(); n.putUUID("Id", pair.getKey()); rs.add(n); }
    root.put("Recovery", rs); return root;
  }
  public static GardenRegistry load(CompoundTag root, HolderLookup.Provider lookup) {
    var data = new GardenRegistry();
    for (var item : root.getList("Gardens", 10)) { var n = (CompoundTag)item;
      if (n.hasUUID("Owner")) { var p = Place.load(n); data.gardens.put(p, new Garden(p,n.getUUID("Owner"),n.getString("Name")));if(n.contains("Area",10)){var a=GardenArea.load(n.getCompound("Area"));if(a!=null)data.areas.put(p,a);} }
    }
    int moves=0;
    for(var item:root.getList("Relocations",10)){
      if(moves++>=MAX_RELOCATIONS)break;
      var n=(CompoundTag)item;if(!n.hasUUID("Owner")||!n.contains("Target",10)||!n.contains("Dimension",8)||!n.contains("Pos",4))continue;
      var tag=n.getCompound("Target");if(!tag.contains("Dimension",8)||!tag.contains("Pos",4))continue;
      var source=Place.load(n);var target=Place.load(tag);
      if(validPlace(source)&&validPlace(target)&&source.dimension.equals(target.dimension)&&!source.equals(target)&&!data.gardens.containsKey(source))
        data.relocations.putIfAbsent(source,new Relocation(target,n.getUUID("Owner")));
    }
    for(var source:new ArrayList<>(data.relocations.keySet())){
      var target=data.resolvePlace(source);var move=data.relocations.get(source);
      if(target.equals(source))data.relocations.remove(source);else data.relocations.put(source,new Relocation(target,move.owner));
    }
    for (var item : root.getList("Entries", 10)) { var n = (CompoundTag)item;
      if (!n.hasUUID("Id") || !n.hasUUID("Owner")) continue;
      var entry = new Entry(n.getUUID("Id"),n.getUUID("Owner"),Place.load(n.getCompound("Garden")),Place.load(n.getCompound("Last")));
      entry.slot=n.contains("Slot")?n.getInt("Slot"):data.firstSlot(entry.garden);entry.appearance=n.getString("Appearance");entry.entered=n.getBoolean("Entered");entry.automatic=n.getBoolean("Automatic");entry.inside=n.getLong("Inside");
      entry.name=n.getString("Name"); entry.stats=n.getString("Stats"); entry.seen=n.getLong("Seen");
      if(entry.slot<0||entry.slot>=6||data.entries.values().stream().anyMatch(e->e.garden.equals(entry.garden)&&e.slot==entry.slot))entry.slot=data.firstSlot(entry.garden);
      if (data.gardens.containsKey(entry.garden)&&entry.slot>=0) data.entries.put(entry.id,entry);
    }
    for(var entry:data.entries.values())data.homes.put(entry.id,new Home(entry.owner,entry.garden));
    for(var item:root.getList("MarkedHomes",10)){var n=(CompoundTag)item;var place=Place.readLink(n);
      if(n.hasUUID("Id")&&n.hasUUID("Owner")&&place!=null)data.homes.putIfAbsent(n.getUUID("Id"),new Home(n.getUUID("Owner"),place));
    }
    for(var item:root.getList("Dismissed",10)){var n=(CompoundTag)item;var place=Place.readLink(n);
      if(n.hasUUID("Id") && data.gardens.containsKey(place))data.dismissed.put(n.getUUID("Id"),place);
    }
    for (var item : root.getList("Nodes",10)) { var n=(CompoundTag)item;
      if(n.hasUUID("Owner")) {var p=Place.load(n);data.nodeOwners.put(p,n.getUUID("Owner"));if(n.contains("Next"))data.links.put(p,Place.load(n.getCompound("Next")));}
    }
    for(var item:root.getList("Recovery",10)){var n=(CompoundTag)item;if(n.hasUUID("Id"))data.recoveries.put(n.getUUID("Id"),n.copy());}
    return data;
  }
}
