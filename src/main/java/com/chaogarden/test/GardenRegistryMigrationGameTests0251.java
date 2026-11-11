package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.activities.ActivityContent;
import com.chaogarden.activities.GardenBellItem;
import com.chaogarden.activities.GardenRegistry;
import com.chaogarden.activities.GardenRegistry.Place;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.hub.GardenRooms025;
import com.chaogarden.hub.HubContent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Actual providers, keepers, UUID bodies, Bell items and SavedData migration boundaries. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class GardenRegistryMigrationGameTests0251 {
  private static final BlockPos OLD=new BlockPos(-33,82,15);
  private static final int FLAGS=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE;
  private GardenRegistryMigrationGameTests0251(){}

  /** Cleanup touches only this fixture's owners, bodies, changed provider cells and new tickets. */
  private static final class Fixture implements AutoCloseable {
    final GameTestHelper h;final ServerLevel level;final GardenRegistry data;
    final List<ServerPlayer> players=new ArrayList<>();final List<ChaoEntity> bodies=new ArrayList<>();
    final Map<BlockPos,BlockState> props=new LinkedHashMap<>();final List<ChunkPos> forced=new ArrayList<>();boolean closed;
    Fixture(GameTestHelper h,ServerLevel level){this.h=h;this.level=level;this.data=GardenRegistry.get(level.getServer());}
    ServerPlayer keeper(){var p=TestConnections.makeSurvivalServerPlayer(h);p.setNoGravity(true);players.add(p);return p;}
    void force(BlockPos at){var chunk=new ChunkPos(at);if(!level.getForcedChunks().contains(chunk.toLong())){level.setChunkForced(chunk.x,chunk.z,true);forced.add(chunk);}level.getChunkAt(at);}
    void remember(BlockPos at){props.putIfAbsent(at.immutable(),level.getBlockState(at));}
    void provider(BlockPos at){remember(at);level.setBlock(at,ActivityContent.GARDEN.get().defaultBlockState(),FLAGS);}
    ChaoEntity child(ServerPlayer p,double x,double y,double z,int seed){
      var c=ChaoGarden.CHAO.get().create(level);c.owner=p.getUUID();c.setNoAi(true);c.setNoGravity(true);
      c.setCustomName(Component.literal("Migration "+seed));c.setState(ChaoState.egg(seed%12,seed%2==0,true,0,new Random(seed)));
      c.moveTo(x,y,z,0,0);force(c.blockPosition());h.assertTrue(level.addFreshEntity(c),"Real owned migration body could not be added");bodies.add(c);return c;
    }
    void ready(){for(var c:bodies)h.assertTrue(level.getEntity(c.getUUID())==c&&level.isPositionEntityTicking(c.blockPosition()),"Migration fixture awaits genuine UUID publication and entity-ticking chunk");}
    @Override public void close(){
      if(closed)return;closed=true;bodies.forEach(ChaoEntity::discard);
      var owners=players.stream().map(ServerPlayer::getUUID).collect(java.util.stream.Collectors.toSet());
      data.entries.values().removeIf(e->owners.contains(e.owner));
      for(var place:new ArrayList<>(data.gardens.keySet()))if(owners.contains(data.gardens.get(place).owner()))data.removeGarden(place);
      data.setDirty();
      props.forEach((at,state)->level.setBlock(at,state,FLAGS));
      for(var chunk:forced)level.setChunkForced(chunk.x,chunk.z,false);
      for(var p:players){if(HubContent.hasReturnJournal(p))HubContent.returnHome(p);p.server.getPlayerList().remove(p);}
    }
  }
  private static ServerLevel garden(GameTestHelper h){var l=h.getLevel().getServer().getLevel(HubContent.GARDEN);h.assertTrue(l!=null&&HubContent.ensure(l),"Actual garden dimension unavailable");return l;}
  private static CompoundTag place(Place p){var n=new CompoundTag();n.putString("Dimension",p.dimension());n.putLong("Pos",p.pos());return n;}
  private static CompoundTag relocation(Place from,Place to,UUID owner){var n=place(from);n.put("Target",place(to));n.putUUID("Owner",owner);return n;}
  private static GardenRegistry restored(GardenRegistry d,ServerPlayer p){return GardenRegistry.load(d.save(new CompoundTag(),p.registryAccess()),p.registryAccess());}
  private static ItemStack persisted(ItemStack stack,ServerPlayer p){return ItemStack.parse(p.registryAccess(),stack.save(p.registryAccess())).orElseThrow();}
  private static ItemStack link(GameTestHelper h,ServerPlayer p,BlockPos provider){
    var bell=new ItemStack(TestConnections.legacyGardenBell());var before=p.position();
    p.setGameMode(GameType.ADVENTURE);p.setPos(provider.getX()+2.5,provider.getY(),provider.getZ()+.5);
    p.setItemInHand(InteractionHand.MAIN_HAND,bell);InteractionResult result;
    try{result=p.gameMode.useItemOn(p,p.serverLevel(),bell,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(provider),Direction.UP,provider,false));}
    finally{p.setPos(before);}
    h.assertTrue(result.consumesAction()&&bell.getItem() instanceof GardenBellItem,"Ordinary Adventure gameMode.useItemOn failed to link the genuine Garden Bell to its actual provider");
    h.assertTrue(p.gameMode.getGameModeForPlayer()==GameType.ADVENTURE&&!p.mayBuild(),"Garden Bell use leaked Adventure build permission or changed game mode");return bell;
  }
  private static InteractionResult assign(ItemStack bell,ServerPlayer p,ChaoEntity child){
    // Vanilla Player returns PASS after a non-consuming item FAIL; denial is
    // proved by no action/assignment/move, rather than inventing a FAIL result.
    p.setItemInHand(InteractionHand.MAIN_HAND,bell);return p.interactOn(child,InteractionHand.MAIN_HAND);
  }
  private record EntrySnapshot(UUID id,UUID owner,Place garden,Place last,String name,String stats,long seen){
    static EntrySnapshot capture(GardenRegistry.Entry e){return new EntrySnapshot(e.id,e.owner,e.garden,e.last,e.name,e.stats,e.seen);}
  }
  private static void sameEntry(GameTestHelper h,EntrySnapshot before,GardenRegistry.Entry after,Place garden,Place last){
    h.assertTrue(after!=null&&after.id.equals(before.id())&&after.owner.equals(before.owner())&&after.garden.equals(garden)&&after.last.equals(last)
        &&after.name.equals(before.name())&&after.stats.equals(before.stats())&&after.seen==before.seen(),"Migration/SavedData changed keeper, UUID, name, stats, observation time or physical Last");
  }

  // Different batches deliberately serialize the two real canonical legacy-site fixtures.
  @GameTest(template="empty",batch="garden0251_registry_link",timeoutTicks=260)
  public static void registeredBodyAndOfflineBellKeepIdentityAndPhysicalLastAcrossRelocationAndSave(GameTestHelper h){
    var f=new Fixture(h,garden(h));h.runAtTickTime(259,f::close);
    try{
      var p=f.keeper();p.teleportTo(f.level,10.5,GardenRooms025.surface(10,5)+1,5.5,0,0);p.hasChangedDimension();
      var from=Place.at(f.level,OLD);var target=Place.at(f.level,GardenRooms025.TRANSPORTER);
      h.assertTrue(!f.data.gardens.containsKey(from)&&!f.data.gardens.containsKey(target),"Migration fixture refuses to overwrite an existing keeper registration");
      f.force(OLD);f.force(GardenRooms025.TRANSPORTER);f.provider(OLD);f.remember(GardenRooms025.TRANSPORTER);
      var original=f.data.register(p,OLD);var bell=link(h,p,OLD);var rawBell=bell.save(p.registryAccess()).copy();
      var child=f.child(p,10.5,GardenRooms025.surface(10,5)+1,5.5,351);
      h.startSequence().thenWaitUntil(f::ready).thenExecute(()->{try{
        // Observe the real old-site body before metadata migration. Last is not a link.
        child.setPos(Vec3.atBottomCenterOf(OLD));var physical=child.position();
        h.assertTrue(f.data.bind(p,child,original),"Real owned Chao could not bind to old registered provider");
        var entry=f.data.entries.get(child.getUUID());var snapshot=EntrySnapshot.capture(entry);var last=entry.last;var state=child.state().encode();var id=child.getUUID();
        h.assertTrue(last.equals(from),"Regression fixture must record physical Last at the legacy provider");
        f.level.setBlock(GardenRooms025.TRANSPORTER,ActivityContent.GARDEN.get().defaultBlockState(),FLAGS);
        h.assertTrue(f.level.getBlockState(GardenRooms025.TRANSPORTER).is(ActivityContent.GARDEN.get())&&f.data.relocateGarden(from,target),"Staged actual destination provider could not commit relocation");
        h.assertTrue(f.data.entries.get(id)==entry&&entry.garden.equals(target)&&entry.last.equals(last)&&child.position().equals(physical),"Metadata relocation rewrote physical Last, moved the body or replaced its entry");
        var moved=f.data.gardens.get(target);
        h.assertTrue(moved.owner().equals(p.getUUID())&&moved.name().equals(original.name())&&f.data.resolvePlace(from).equals(target),"Relocation lost garden name/owner or old-link alias");
        sameEntry(h,snapshot,entry,target,last);var reload=restored(f.data,p);sameEntry(h,snapshot,reload.entries.get(id),target,last);
        h.assertTrue(reload.gardens.get(target).owner().equals(original.owner())&&reload.gardens.get(target).name().equals(original.name())&&reload.resolvePlace(from).equals(target),"SavedData lost original garden or persistent relocation alias");
        // Exercise ActivityBlock.onRemove, not just the ledger method.
        f.level.setBlock(OLD,Blocks.AIR.defaultBlockState(),FLAGS);
        h.assertTrue(f.data.gardens.get(target)==moved&&f.data.entries.get(id)==entry&&f.data.resolvePlace(from).equals(target),"Obsolete physical provider removal followed its alias and erased the new garden");
        var offline=ItemStack.parse(p.registryAccess(),rawBell).orElseThrow();
        h.assertTrue(assign(offline,p,child).consumesAction(),"Authentic offline Bell with old saved GardenLink failed at the live interaction boundary");
        h.assertTrue(f.data.entries.get(id).garden.equals(target)&&id.equals(child.getUUID())&&p.getUUID().equals(child.owner)&&state.equals(child.state().encode()),"Old Bell rebound to a wrong garden or changed body UUID/owner/state");
        h.assertTrue(ItemStack.isSameItemSameComponents(offline,bell),"Using the old Bell rewrote/lost its saved item data");
        f.data.update(child);h.assertTrue(f.data.entries.get(id).last.equals(Place.at(f.level,child.blockPosition())),"Physical Last did not update after the genuine Bell teleport and real body observation");
        f.close();h.succeed();
      }catch(RuntimeException failure){f.close();throw failure;}});
    }catch(RuntimeException failure){f.close();throw failure;}
  }

  @GameTest(template="empty",batch="garden0251_registry_conflict",timeoutTicks=300)
  public static void claimedPreferredProviderSelectsARealAlternateWithoutMergingKeepersOrItems(GameTestHelper h){
    var f=new Fixture(h,garden(h));h.runAtTickTime(299,f::close);
    try{
      var a=f.keeper();var b=f.keeper();a.teleportTo(f.level,10.5,GardenRooms025.surface(10,5)+1,5.5,0,0);b.teleportTo(f.level,12.5,GardenRooms025.surface(12,5)+1,5.5,0,0);a.hasChangedDimension();b.hasChangedDimension();
      var from=Place.at(f.level,OLD);var occupied=Place.at(f.level,GardenRooms025.TRANSPORTER);
      h.assertTrue(!f.data.gardens.containsKey(from)&&!f.data.gardens.containsKey(occupied),"Conflict fixture refuses to replace another existing keeper");
      f.provider(OLD);f.remember(GardenRooms025.TRANSPORTER);f.force(OLD);f.force(GardenRooms025.TRANSPORTER);
      // Capture only possible provider cells; cleanup restores just the one actually changed.
      var candidates=new LinkedHashMap<BlockPos,BlockState>();
      for(int dx=-12;dx<=12;dx++)for(int dz=-12;dz<=12;dz++){int x=GardenRooms025.TRANSPORTER.getX()+dx,z=GardenRooms025.TRANSPORTER.getZ()+dz;var at=new BlockPos(x,GardenRooms025.surface(x,z)+1,z);candidates.put(at,f.level.getBlockState(at));}
      var ga=f.data.register(a,OLD);var gb=f.data.register(b,GardenRooms025.TRANSPORTER);
      var ba=link(h,a,OLD);var bb=link(h,b,GardenRooms025.TRANSPORTER);a.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);b.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);a.getInventory().setItem(4,ba);b.getInventory().setItem(4,bb);
      var ca=f.child(a,10.5,GardenRooms025.surface(10,5)+1,5.5,352);var cb=f.child(b,12.5,GardenRooms025.surface(12,5)+1,5.5,353);
      h.startSequence().thenWaitUntil(f::ready).thenExecute(()->{try{
        h.assertTrue(f.data.bind(a,ca,ga)&&f.data.bind(b,cb,gb),"Two genuine owned Chao could not bind to their independent gardens");
        var liveA=f.data.entries.get(ca.getUUID());var liveB=f.data.entries.get(cb.getUUID());var ea=EntrySnapshot.capture(liveA);var eb=EntrySnapshot.capture(liveB);var la=ea.last();var lb=eb.last();
        String sa=ca.state().encode(),sb=cb.state().encode();var itemA=persisted(ba,a);var itemB=persisted(bb,b);
        h.assertTrue(!f.data.canRelocateGarden(from,occupied)&&!f.data.relocateGarden(from,occupied),"Relocation accepted another keeper's occupied destination");
        GardenRooms025.ensure(f.level);var alternate=f.data.resolvePlace(from);
        h.assertTrue(!alternate.equals(from)&&!alternate.equals(occupied)&&alternate.dimension().equals(from.dimension())&&Math.abs(alternate.block().getX()-GardenRooms025.TRANSPORTER.getX())<=20&&Math.abs(alternate.block().getZ()-GardenRooms025.TRANSPORTER.getZ())<=24,"Production ensure did not choose a bounded distinct alternate provider");
        f.props.putIfAbsent(alternate.block(),candidates.get(alternate.block()));
        h.assertTrue(f.level.getBlockState(alternate.block()).is(ActivityContent.GARDEN.get())&&f.level.getBlockState(alternate.block().below()).is(com.chaogarden.hub.LobbyDecor025.TURF.get()),"Alternate registration has no actual clear supported provider");
        h.assertTrue(f.data.gardens.get(occupied)==gb&&f.data.gardens.get(alternate).owner().equals(a.getUUID())&&f.data.gardens.get(alternate).name().equals(ga.name()),"Production relocation merged/overwrote keeper identity or garden name");
        sameEntry(h,ea,f.data.entries.get(ca.getUUID()),alternate,la);sameEntry(h,eb,f.data.entries.get(cb.getUUID()),occupied,lb);
        h.assertTrue(f.data.entries.get(ca.getUUID())==liveA&&f.data.entries.get(cb.getUUID())==liveB&&f.level.getEntity(ca.getUUID())==ca&&f.level.getEntity(cb.getUUID())==cb&&a.getUUID().equals(ca.owner)&&b.getUUID().equals(cb.owner)&&sa.equals(ca.state().encode())&&sb.equals(cb.state().encode()),"Garden migration replaced or changed either real body/state");
        h.assertTrue(ItemStack.isSameItemSameComponents(itemA,a.getInventory().getItem(4))&&ItemStack.isSameItemSameComponents(itemB,b.getInventory().getItem(4)),"Production ensure changed either keeper's actual linked inventory item");
        var saved=f.data.save(new CompoundTag(),a.registryAccess());GardenRooms025.ensure(f.level);
        h.assertTrue(saved.equals(f.data.save(new CompoundTag(),a.registryAccess()))&&f.level.getBlockState(alternate.block()).is(ActivityContent.GARDEN.get()),"Second production ensure was not idempotent for metadata, aliases and the alternate provider");
        a.getInventory().setItem(4,ItemStack.EMPTY);b.getInventory().setItem(4,ItemStack.EMPTY);
        h.assertTrue(assign(itemA,a,ca).consumesAction()&&assign(itemB,b,cb).consumesAction(),"Persisted old/new Bell items did not assign to their respective real providers");
        b.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        h.assertTrue(!assign(itemB,a,ca).consumesAction()&&f.data.entries.get(ca.getUUID()).garden.equals(alternate)&&f.data.entries.get(cb.getUUID()).garden.equals(occupied),"Another keeper's Bell merged or stole a Chao registration");
        f.close();h.succeed();
      }catch(RuntimeException failure){f.close();throw failure;}});
    }catch(RuntimeException failure){f.close();throw failure;}
  }

  @GameTest(template="empty",batch="garden0251_registry_malformed",timeoutTicks=180)
  public static void malformedLinksAndBoundedSavedAliasesFailClosedWithoutClaimingOriginOrAnotherOwner(GameTestHelper h){
    var f=new Fixture(h,h.getLevel());h.runAtTickTime(179,f::close);
    try{
      var a=f.keeper();var b=f.keeper();var zero=Place.at(f.level,BlockPos.ZERO);var other=Place.at(f.level,h.absolutePos(new BlockPos(15,2,15)));
      h.assertTrue(!f.data.gardens.containsKey(zero)&&!f.data.gardens.containsKey(other),"Malformed-link fixture refuses to overwrite an existing registration");
      var ga=f.data.register(a,BlockPos.ZERO);var gb=f.data.register(b,other.block());
      var child=f.child(a,h.absolutePos(new BlockPos(2,2,2)).getX()+.5,h.absolutePos(new BlockPos(2,2,2)).getY(),h.absolutePos(new BlockPos(2,2,2)).getZ()+.5,354);
      h.startSequence().thenWaitUntil(f::ready).thenExecute(()->{try{
        var position=child.position();var state=child.state().encode();var malformed=new ArrayList<CompoundTag>();
        var missingPos=place(zero);missingPos.remove("Pos");malformed.add(missingPos);
        var wrongPos=place(zero);wrongPos.putString("Pos","0");malformed.add(wrongPos);
        var missingDim=place(zero);missingDim.remove("Dimension");malformed.add(missingDim);
        var wrongDim=place(zero);wrongDim.putInt("Dimension",0);malformed.add(wrongDim);
        var invalidDim=place(zero);invalidDim.putString("Dimension","Bad Dimension!");malformed.add(invalidDim);
        var oversizedDim=place(zero);oversizedDim.putString("Dimension","a".repeat(257));malformed.add(oversizedDim);
        var outOfBounds=place(new Place(zero.dimension(),new BlockPos(33_000_000,80,0).asLong()));malformed.add(outOfBounds);
        for(var bad:malformed){var bell=new ItemStack(TestConnections.legacyGardenBell());CustomData.update(DataComponents.CUSTOM_DATA,bell,n->n.put("GardenLink",bad));
          h.assertTrue(!assign(bell,a,child).consumesAction(),"Malformed typed GardenLink was accepted: "+bad);}
        var wrongContainer=new ItemStack(TestConnections.legacyGardenBell());CustomData.update(DataComponents.CUSTOM_DATA,wrongContainer,n->n.putString("GardenLink","not a compound"));
        h.assertTrue(!assign(wrongContainer,a,child).consumesAction()&&f.data.entries.get(child.getUUID())==null&&position.equals(child.position())&&state.equals(child.state().encode())&&f.data.gardens.get(zero)==ga&&f.data.gardens.get(other)==gb,"Malformed Bell defaulted to origin, changed a body or mutated another keeper");
        var root=f.data.save(new CompoundTag(),a.registryAccess());
        // Remove origin only in a detached saved copy. This independently catches
        // a missing/wrong source Pos defaulting to ZERO; a live origin garden
        // would otherwise mask that defect through physical-source precedence.
        var noOrigin=root.copy();var foreignOnly=new ListTag();
        for(var tag:root.getList("Gardens",10))if(((CompoundTag)tag).hasUUID("Owner")&&((CompoundTag)tag).getUUID("Owner").equals(b.getUUID()))foreignOnly.add(tag.copy());
        noOrigin.put("Gardens",foreignOnly);
        for(boolean wrongType:new boolean[]{false,true}){
          var badSource=relocation(new Place(zero.dimension(),new BlockPos(9999,80,0).asLong()),other,b.getUUID());
          if(wrongType)badSource.putString("Pos","0");else badSource.remove("Pos");
          var input=new ListTag();input.add(badSource);noOrigin.put("Relocations",input);
          h.assertTrue(GardenRegistry.load(noOrigin,a.registryAccess()).resolvePlace(zero).equals(zero),"Malformed saved source Pos redirected unclaimed origin ZERO to another keeper's garden");
        }
        var badMoves=new ListTag();var rejected=new ArrayList<Place>();int cursor=10000;
        // Each malformed saved relocation has its own source so rejection is independently observable.
        var source=new Place(zero.dimension(),new BlockPos(cursor++,80,0).asLong());var n=relocation(source,zero,a.getUUID());n.remove("Pos");badMoves.add(n);
        source=new Place(zero.dimension(),new BlockPos(cursor++,80,0).asLong());rejected.add(source);n=relocation(source,zero,a.getUUID());n.getCompound("Target").remove("Pos");badMoves.add(n);
        source=new Place(zero.dimension(),new BlockPos(cursor++,80,0).asLong());rejected.add(source);n=relocation(source,zero,a.getUUID());n.putString("Pos","wrong type");badMoves.add(n);
        source=new Place(zero.dimension(),new BlockPos(cursor++,80,0).asLong());rejected.add(source);n=relocation(source,zero,a.getUUID());n.getCompound("Target").putString("Dimension","Bad Dimension!");badMoves.add(n);
        source=new Place(zero.dimension(),new BlockPos(cursor++,80,0).asLong());rejected.add(source);badMoves.add(relocation(source,new Place("minecraft:the_nether",zero.pos()),a.getUUID()));
        source=new Place(zero.dimension(),new BlockPos(cursor++,80,0).asLong());rejected.add(source);badMoves.add(relocation(source,other,a.getUUID()));
        var cycleA=new Place(zero.dimension(),new BlockPos(cursor++,80,0).asLong());var cycleB=new Place(zero.dimension(),new BlockPos(cursor++,80,0).asLong());
        rejected.add(cycleA);rejected.add(cycleB);badMoves.add(relocation(cycleA,cycleB,a.getUUID()));badMoves.add(relocation(cycleB,cycleA,a.getUUID()));
        var mixedA=new Place(zero.dimension(),new BlockPos(cursor++,80,0).asLong());var mixedB=new Place(zero.dimension(),new BlockPos(cursor++,80,0).asLong());
        rejected.add(mixedA);badMoves.add(relocation(mixedA,mixedB,a.getUUID()));badMoves.add(relocation(mixedB,other,b.getUUID()));
        var duplicate=new Place(zero.dimension(),new BlockPos(cursor++,80,0).asLong());badMoves.add(relocation(duplicate,zero,a.getUUID()));badMoves.add(relocation(duplicate,other,b.getUUID()));
        // An alias cannot replace a physically registered source, regardless of its owner.
        badMoves.add(relocation(zero,other,b.getUUID()));
        var chain=new ArrayList<Place>();for(int i=0;i<66;i++)chain.add(new Place(zero.dimension(),new BlockPos(cursor++,80,0).asLong()));
        for(int i=0;i<chain.size();i++)badMoves.add(relocation(chain.get(i),i+1<chain.size()?chain.get(i+1):zero,a.getUUID()));rejected.add(chain.getFirst());
        root.put("Relocations",badMoves);var loaded=GardenRegistry.load(root,a.registryAccess());
        for(var bad:rejected)h.assertTrue(loaded.resolvePlace(bad).equals(bad),"Malformed/cyclic/mixed-owner/over-depth alias claimed a different garden: "+bad);
        h.assertTrue(loaded.resolvePlace(duplicate).equals(zero)&&loaded.resolvePlace(zero).equals(zero)&&loaded.gardens.get(other).owner().equals(b.getUUID()),"Duplicate alias stole the first valid target or overrode a physical/foreign garden");
        // Independent huge input: only the first 4096 well-formed records may be admitted.
        var many=new ListTag();Place ignored=null;
        for(int i=0;i<4097;i++){var key=new Place(zero.dimension(),new BlockPos(20000+i,80,0).asLong());many.add(relocation(key,zero,a.getUUID()));ignored=key;}
        root.put("Relocations",many);var bounded=GardenRegistry.load(root,a.registryAccess());
        h.assertTrue(bounded.resolvePlace(ignored).equals(ignored)&&bounded.save(new CompoundTag(),a.registryAccess()).getList("Relocations",10).size()==4096,"Oversized saved aliases escaped the 4096 record admission bound");
        h.assertTrue(f.data.entries.get(child.getUUID())==null&&f.data.gardens.get(zero)==ga&&f.data.gardens.get(other)==gb&&position.equals(child.position()),"Detached SavedData validation changed live registration/body state");
        f.close();h.succeed();
      }catch(RuntimeException failure){f.close();throw failure;}});
    }catch(RuntimeException failure){f.close();throw failure;}
  }
}
