package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real entity/stack publication, persistence, ownership and native production playback.
 * Natural path/physics fixtures are separate; these do not assert visual or sound approval. */
@GameTestHolder("chao_float80") @PrefixGameTestTemplate(false)
public final class FloatToyGameTests080 {
  private static final String TEMPLATE="chao_float80:activity_test";
  private record Fixture(ChaoEntity c,ServerPlayer p,FloatToyEntity080 f,BlockPos bank){}
  private FloatToyGameTests080(){}
  @GameTestGenerator public static Collection<TestFunction> cases(){
    var out=new ArrayList<TestFunction>();
    for(String kind:List.of("chao_rubber_duck","chao_lifesaver")){
      add(out,"float_placement_cancel_reentry_"+kind,60,h->placement(h,kind));
      add(out,"float_legacy_cancel_commit_"+kind,60,h->legacy(h,kind));
      add(out,"float_drop_cancel_reentry_"+kind,100,h->drop(h,kind));
      add(out,"float_payload_passenger_reload_"+kind,100,h->reload(h,kind));
      add(out,"float_pending_break_recursive_reload_"+kind,280,h->pendingReload(h,kind));
      add(out,"float_occupied_break_retains_until_safe_"+kind,220,h->occupiedBreak(h,kind));
      add(out,"float_landing_removed_mid_exit_"+kind,450,h->landingRemoved(h,kind));
      add(out,"float_cancelled_board_restores_owner_"+kind,60,h->cancelBoard(h,kind));
      add(out,"float_drained_pool_safe_exit_"+kind,200,h->drained(h,kind));
      add(out,"float_entry_obstructed_returns_safely_"+kind,140,h->blockedEntry(h,kind));
      add(out,"float_full_inventory_pickup_retains_one_"+kind,80,h->pickup(h,kind));
      add(out,"float_missing_claimant_reload_lease_"+kind,140,h->lease(h,kind));
      for(String mode:List.of("pickup","hurt","remove"))add(out,"float_loop_removal_"+kind+"_"+mode,280,h->loopRemoval(h,kind,mode));
      for(boolean adult:new boolean[]{false,true})for(boolean loop:new boolean[]{false,true})
        add(out,"float_preview_"+kind+"_"+adult+"_"+loop,380,h->preview(h,kind,adult,loop));
    }
    for(String mode:List.of("stop","damage","pet","logout","range","reload","unload","original_noai"))
      add(out,"float_preview_interrupt_"+mode,120,h->interrupt(h,mode));
    add(out,"float_native_permissions_and_variants",60,FloatToyGameTests080::permissions);
    add(out,"float_nonlethal_water_without_toy_preserved",100,FloatToyGameTests080::ordinaryWater);
    add(out,"float_complete_support_union_rejects_internal_holes",60,FloatToyGameTests080::support);
    for(boolean adult:new boolean[]{false,true})add(out,"float_settled_ring_native_bank_preview_"+adult,340,h->settledRing(h,adult));
    add(out,"float_natural_reward_paid_once",260,h->reward(h,false,false));
    add(out,"float_natural_reward_reload_paid_once",320,h->reward(h,true,false));
    add(out,"float_early_interruption_never_rewards_wait",260,h->reward(h,false,true));
    return out;
  }
  private static void add(List<TestFunction> out,String name,int ticks,Consumer<GameTestHelper> test){out.add(new TestFunction("float080_transactions_"+(out.size()/5),name,TEMPLATE,ticks,0,true,test));}
  private static void cleanup(GameTestHelper h,Runnable task){h.testInfo.addListener(new GameTestListener(){
    public void testStructureLoaded(GameTestInfo i){}public void testPassed(GameTestInfo i,GameTestRunner r){task.run();}
    public void testFailed(GameTestInfo i,GameTestRunner r){task.run();}public void testAddedForRerun(GameTestInfo a,GameTestInfo b,GameTestRunner r){task.run();}
  });}
  private static void floor(GameTestHelper h){
    for(int x=1;x<=18;x++)for(int z=1;z<=18;z++){
      h.setBlock(new BlockPos(x,0,z),Blocks.STONE);h.setBlock(new BlockPos(x,1,z),Blocks.STONE);
      for(int y=2;y<=6;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
    }
    for(int x=8;x<=14;x++)for(int z=4;z<=14;z++)h.setBlock(new BlockPos(x,1,z),Blocks.WATER);
  }
  private static ChaoEntity actor(GameTestHelper h,Vec3 at,boolean adult){
    var c=ChaoGarden.CHAO.get().create(h.getLevel());var s=new ChaoState();s.adult=adult;s.align=adult?-1:0;
    s.primaryType=adult?ChaoState.RUN_TYPE:ChaoState.NORMAL;s.hunger=90;s.energy=95;s.happiness=50;
    c.setState(ChaoState.decode(s.encode()));c.moveTo(at);c.setOnGround(true);c.setInvulnerable(true);
    h.getLevel().addFreshEntity(c);c.goalSelector.removeAllGoals(g->true);c.targetSelector.removeAllGoals(g->true);
    c.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ChaoGarden.item("red_wool_beanie")));
    c.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(ChaoGarden.item("aviators_accessory")));
    cleanup(h,()->{ToyPreview.stop(c);c.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);});return c;
  }
  private static FloatToyEntity080 vessel(GameTestHelper h,String kind,Vec3 at){
    var f=FloatToyContent080.newCandidate(h.getLevel(),kind,new ItemStack(ChaoGarden.item(kind)),at,0);
    h.assertTrue(f!=null&&h.getLevel().addFreshEntity(f),"Real float publishes");f.commit();
    cleanup(h,()->f.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK));return f;
  }
  private static Fixture fixture(GameTestHelper h,String kind,boolean adult){
    floor(h);var bank=h.absolutePos(new BlockPos(7,2,8));var c=actor(h,Vec3.atBottomCenterOf(bank),adult);
    var f=vessel(h,kind,Vec3.atBottomCenterOf(bank.east()).add(0,-.05,0));
    var p=TestConnections.makeServerPlayer(h);p.teleportTo(c.getX()-2,c.getY(),c.getZ());c.owner=p.getUUID();
    cleanup(h,()->{if(p.server.getPlayerList().getPlayer(p.getUUID())!=null)p.server.getPlayerList().remove(p);});return new Fixture(c,p,f,bank);
  }
  private static int command(ServerPlayer p,String text,int permission){try{return p.server.getCommands().getDispatcher().execute(text,p.createCommandSourceStack().withPermission(permission));}catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){return -1;}}
  private static int command(ServerPlayer p,String text){return command(p,text,2);}
  private static String stateDiff(String a,String b){int i=0;while(i<a.length()&&i<b.length()&&a.charAt(i)==b.charAt(i))i++;return a.equals(b)?"same":"at"+i+" expected="+a.substring(Math.max(0,i-24),Math.min(a.length(),i+80))+" actual="+b.substring(Math.max(0,i-24),Math.min(b.length(),i+80));}
  private static int drops(GameTestHelper h,FloatToyEntity080 f){return h.getLevel().getEntitiesOfClass(ItemEntity.class,f.getBoundingBox().inflate(3),i->i.getItem().is(ChaoGarden.item(f.kind()))).stream().mapToInt(i->i.getItem().getCount()).sum();}
  private static void placement(GameTestHelper h,String kind){
    floor(h);var p=TestConnections.makeSurvivalServerPlayer(h);var at=h.absolutePos(new BlockPos(4,2,4));p.teleportTo(at.getX()+.5,at.getY(),at.getZ()+1.5);
    cleanup(h,()->p.server.getPlayerList().remove(p));var stack=new ItemStack(ChaoGarden.item(kind),3);stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("retained float"));p.setItemInHand(InteractionHand.MAIN_HAND,stack);
    var second=TestConnections.makeSurvivalServerPlayer(h);second.teleportTo(at.getX()+.5,at.getY(),at.getZ()-2.5);var secondStack=new ItemStack(ChaoGarden.item(kind),2);second.setItemInHand(InteractionHand.MAIN_HAND,secondStack);cleanup(h,()->second.server.getPlayerList().remove(second));
    var attempts=new AtomicInteger();Consumer<EntityJoinLevelEvent> cancel=e->{if(e.getLevel()==h.getLevel()&&e.getEntity() instanceof FloatToyEntity080 f&&f.kind().equals(kind)&&f.distanceToSqr(Vec3.atBottomCenterOf(at))<1){
      attempts.incrementAndGet();h.assertTrue(!f.committed()&&!f.isPickable(),"Candidate cannot be collected during publication");
      h.assertTrue(!FloatToyContent080.place(h.getLevel(),p,InteractionHand.MAIN_HAND,stack,kind,Vec3.atBottomCenterOf(at),at.below(),Direction.UP).consumesAction(),"Reentrant placement rejected");
      h.assertTrue(!FloatToyContent080.place(h.getLevel(),second,InteractionHand.MAIN_HAND,secondStack,kind,Vec3.atBottomCenterOf(at),at.below(),Direction.UP).consumesAction()&&secondStack.getCount()==2,"A second player cannot overlap a pending spatial publication");e.setCanceled(true);
    }};
    NeoForge.EVENT_BUS.addListener(cancel);try{h.assertTrue(!FloatToyContent080.place(h.getLevel(),p,InteractionHand.MAIN_HAND,stack,kind,Vec3.atBottomCenterOf(at),at.below(),Direction.UP).consumesAction(),"Canceled publication fails");}finally{NeoForge.EVENT_BUS.unregister(cancel);}
    h.assertTrue(attempts.get()==1&&stack.getCount()==3&&stack.getHoverName().getString().equals("retained float"),"Exact source stack/components refunded once");
    h.assertTrue(FloatToyContent080.place(h.getLevel(),p,InteractionHand.MAIN_HAND,stack,kind,Vec3.atBottomCenterOf(at),at.below(),Direction.UP).consumesAction()&&stack.getCount()==2,"Retry consumes precisely one source");
    var floats=h.getLevel().getEntitiesOfClass(FloatToyEntity080.class,new AABB(at).inflate(1),FloatToyEntity080::committed);h.assertTrue(floats.size()==1,"One physical payload only");h.assertTrue(Math.abs(floats.getFirst().getBbHeight()-(kind.equals("chao_lifesaver")?.20:.68))<.0001,"New placement refreshes the appearance-specific collision height");cleanup(h,()->floats.forEach(f->f.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK)));h.succeed();
  }
  private static void legacy(GameTestHelper h,String kind){
    floor(h);var at=h.absolutePos(new BlockPos(4,2,4));var c=actor(h,Vec3.atBottomCenterOf(at.east(2)),false);
    var old=ToyContent.block(kind).defaultBlockState().setValue(ChaoToyBlock.FACING,Direction.WEST);h.getLevel().setBlock(at,old,3);
    Consumer<EntityJoinLevelEvent> cancel=e->{if(e.getLevel()==h.getLevel()&&e.getEntity() instanceof FloatToyEntity080)e.setCanceled(true);};
    NeoForge.EVENT_BUS.addListener(cancel);try{h.assertTrue(FloatToyContent080.convertLegacy(h.getLevel(),at,c)==null&&h.getLevel().getBlockState(at).equals(old),"Rejected migration preserves exact old block/state");}finally{NeoForge.EVENT_BUS.unregister(cancel);}
    var f=FloatToyContent080.convertLegacy(h.getLevel(),at,c);h.assertTrue(f!=null&&f.committed()&&h.getLevel().getBlockState(at).isAir()&&f.storedItem().getCount()==1,"Same legacy item becomes exactly one real float");
    h.assertTrue(Math.abs(f.getBbHeight()-(kind.equals("chao_lifesaver")?.20:.68))<.0001,"Legacy conversion uses the correct ring/duck collision height");
    cleanup(h,()->f.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK));h.assertTrue(FloatToyContent080.convertLegacy(h.getLevel(),at,c)==null,"No duplicate conversion");h.succeed();
  }
  private static void drop(GameTestHelper h,String kind){
    var f=fixture(h,kind,false);var attempts=new AtomicInteger();
    Consumer<EntityJoinLevelEvent> cancel=e->{if(e.getLevel()==h.getLevel()&&e.getEntity() instanceof ItemEntity item&&item.getItem().is(ChaoGarden.item(kind))){attempts.incrementAndGet();f.f.discard();e.setCanceled(true);}};
    NeoForge.EVENT_BUS.addListener(cancel);try{f.f.remove(Entity.RemovalReason.KILLED);}finally{NeoForge.EVENT_BUS.unregister(cancel);}
    h.assertTrue(attempts.get()==1&&!f.f.isRemoved()&&f.f.storedItem().getCount()==1&&drops(h,f.f)==0,"Reentrant remove plus canceled drop retains live sole source");
    h.assertTrue(!f.f.claim(f.c),"Pending break refuses a new claimant");f.f.remove(Entity.RemovalReason.KILLED);
    h.assertTrue(f.f.isRemoved()&&f.f.storedItem().isEmpty()&&drops(h,f.f)==1,"Retry drops exactly one");f.f.remove(Entity.RemovalReason.KILLED);h.assertTrue(drops(h,f.f)==1,"Repeated removal cannot duplicate");h.succeed();
  }
  private static void reload(GameTestHelper h,String kind){
    var f=fixture(h,kind,true);h.assertTrue(f.f.board(f.c),"Local real mount begins");
    h.runAfterDelay(22,()->{
      var id=f.f.getUUID();var cId=f.c.getUUID();var state=f.c.state().encode();var tag=new CompoundTag();h.assertTrue(f.f.save(tag),"Real passenger tree saves");
      f.f.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);f.c.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
      var restored=EntityType.loadEntityRecursive(tag,h.getLevel(),e->e);h.assertTrue(restored instanceof FloatToyEntity080,"Registered type reloads");
      var boat=(FloatToyEntity080)restored;h.getLevel().addFreshEntityWithPassengers(boat);cleanup(h,()->{if(boat.rider()!=null)boat.rider().remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);boat.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);});
      h.assertTrue(Math.abs(boat.getBbHeight()-(kind.equals("chao_lifesaver")?.20:.68))<.0001,"Reload refreshes appearance-specific collision height");
      h.assertTrue(boat.getUUID().equals(id)&&boat.storedItem().getCount()==1&&boat.kind().equals(kind)&&boat.rider()!=null&&boat.rider().getUUID().equals(cId),"Toy/rider identities, appearance and one payload persist");
      h.assertTrue(boat.claimedBy(boat.rider())&&boat.rider().state().encode().equals(state)&&boat.rider().getItemBySlot(EquipmentSlot.HEAD).is(ChaoGarden.item("red_wool_beanie")),"Claim, care, first-type and equipment persist");h.succeed();
    });
  }
  private static void pendingReload(GameTestHelper h,String kind){
    var original=fixture(h,kind,true);h.assertTrue(original.f.board(original.c),"Actual local board begins");
    var restored=new java.util.concurrent.atomic.AtomicReference<FloatToyEntity080>();
    var restoredActor=new java.util.concurrent.atomic.AtomicReference<ChaoEntity>();
    h.runAfterDelay(24,()->{
      for(int x=1;x<=18;x++)for(int z=1;z<=18;z++)h.setBlock(new BlockPos(x,1,z),Blocks.WATER);
      original.f.hurt(h.getLevel().damageSources().generic(),1);
      var cId=original.c.getUUID();String state=original.c.state().encode();var saved=new CompoundTag();original.f.save(saved);
      h.assertTrue(saved.getBoolean("RemovalWanted080")&&saved.getList("Passengers",net.minecraft.nbt.Tag.TAG_COMPOUND).size()==1,"Real pending break saves its occupied passenger tree");
      original.c.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);original.f.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
      var rival=actor(h,Vec3.atBottomCenterOf(original.bank.west(3)),false);rival.setNoAi(true);
      var loaded=EntityType.loadEntityRecursive(saved,h.getLevel(),e->{
        if(e instanceof FloatToyEntity080 boat){
          h.assertTrue(!boat.claim(rival),"Unpublished restore window refuses new claimants");
          var mid=new CompoundTag();boat.saveWithoutId(mid);h.assertTrue(mid.getBoolean("RemovalWanted080"),"A save during reconstruction retains pending removal intent");
        }return e;
      });
      h.assertTrue(loaded instanceof FloatToyEntity080&&((FloatToyEntity080)loaded).rider()!=null,"Vanilla recursive forced attachment retains the pending float's saved Chao");
      var boat=(FloatToyEntity080)loaded;var rider=boat.rider();restored.set(boat);restoredActor.set(rider);
      h.assertTrue(rider.getUUID().equals(cId)&&rider.state().encode().equals(state)&&rider.getItemBySlot(EquipmentSlot.HEAD).is(ChaoGarden.item("red_wool_beanie"))
          &&rider.getItemBySlot(EquipmentSlot.OFFHAND).is(ChaoGarden.item("aviators_accessory")),"Saved identity, care and real gear are retained");
      h.getLevel().addFreshEntityWithPassengers(boat);
      cleanup(h,()->{rider.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);boat.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);});
      h.assertTrue(!boat.claim(rival)&&!rival.startRiding(boat,true),"Published pending float retains strict occupancy/claim authorization");
      h.runAfterDelay(40,()->{
        h.assertTrue(boat.rider()==rider&&!boat.isRemoved()&&boat.storedItem().getCount()==1&&drops(h,boat)==0,"Reloaded pending break waits safely without losing its Chao or duplicating its toy");
        h.getLevel().setBlock(original.bank.below(),Blocks.STONE.defaultBlockState(),3);h.getLevel().setBlock(original.bank.west().below(),Blocks.STONE.defaultBlockState(),3);
      });
    });
    h.succeedWhen(()->{
      var boat=restored.get();var rider=restoredActor.get();
      h.assertTrue(boat!=null&&rider!=null&&boat.isRemoved()&&!rider.isPassenger()&&FloatToyRoutes080.safeFeet(h.getLevel(),rider,rider.position())&&drops(h,boat)==1,"Pending reload completes safe egress before exactly one delayed toy drop");
      h.assertTrue(rider.isAlive()&&rider.state().lockedPrimaryType()==ChaoState.RUN_TYPE,"Delayed removal preserves the saved Chao's life and locked first type");
    });
  }
  private static void occupiedBreak(GameTestHelper h,String kind){
    var f=fixture(h,kind,true);h.assertTrue(f.f.board(f.c),"Local board");f.f.previewHeld(true);
    // Remove every nearby bank so a pending pickup cannot force an unsafe eject.
    h.runAfterDelay(20,()->{
      for(int x=1;x<=18;x++)for(int z=1;z<=18;z++)h.setBlock(new BlockPos(x,1,z),Blocks.WATER);
      f.f.hurt(h.getLevel().damageSources().generic(),1);f.c.stopRiding();
      h.assertTrue(f.f.rider()==f.c&&!f.f.isRemoved()&&drops(h,f.f)==0,"Occupied break waits safely; ordinary dismount cannot eject into water");
      var other=actor(h,f.c.position().add(2,0,0),false);h.assertTrue(!f.f.claim(other)&&!other.startRiding(f.f,true),"Pending break prevents second owner/forced boarding");
      h.runAfterDelay(20,()->{h.assertTrue(f.f.rider()==f.c&&!f.f.isRemoved(),"No route retains buoyant passenger");
        h.getLevel().setBlock(f.bank.below(),Blocks.STONE.defaultBlockState(),3);h.getLevel().setBlock(f.bank.west().below(),Blocks.STONE.defaultBlockState(),3);f.f.previewHeld(false);f.f.requestLand();
      });
    });
    h.succeedWhen(()->h.assertTrue(f.f.isRemoved()&&!f.c.isPassenger()&&FloatToyRoutes080.safeFeet(h.getLevel(),f.c,f.c.position())&&drops(h,f.f)==1,"Landing is safe before exactly-one delayed break drop"));
  }
  private static void landingRemoved(GameTestHelper h,String kind){
    var f=fixture(h,kind,true);h.assertTrue(f.f.board(f.c),"Local board begins");f.f.requestLand();
    var removed=new java.util.concurrent.atomic.AtomicBoolean();var returned=new java.util.concurrent.atomic.AtomicBoolean();var previous=new java.util.concurrent.atomic.AtomicReference<>(f.c.position());
    h.onEachTick(()->{
      h.assertTrue(f.c.position().distanceTo(previous.get())<.35,"Dynamic exit never snaps the actor back to its seat");previous.set(f.c.position());
      if(!removed.get()&&f.f.phase()==FloatToyEntity080.EXIT&&f.f.transition()>.3f&&f.f.transition()<.8f){
        removed.set(true);h.getLevel().setBlock(f.bank.below(),Blocks.AIR.defaultBlockState(),3);h.getLevel().setBlock(f.bank.west().below(),Blocks.AIR.defaultBlockState(),3);
        h.runAfterDelay(45,()->{h.assertTrue(f.c.getVehicle()==f.f,"Vanished support cannot commit an unsafe exit");returned.set(true);
          h.getLevel().setBlock(f.bank.below(),Blocks.STONE.defaultBlockState(),3);h.getLevel().setBlock(f.bank.west().below(),Blocks.STONE.defaultBlockState(),3);f.f.requestLand();});
      }
    });
    h.succeedWhen(()->h.assertTrue(removed.get()&&returned.get()&&!f.c.isPassenger()&&FloatToyRoutes080.safeFeet(h.getLevel(),f.c,f.c.position()),"Restored bank eventually permits a fresh safe egress"));
  }
  private static void cancelBoard(GameTestHelper h,String kind){
    var f=fixture(h,kind,false);Consumer<net.neoforged.neoforge.event.entity.EntityMountEvent> callback=e->{if(e.isMounting()&&e.getEntityMounting()==f.c&&e.getEntityBeingMounted()==f.f)e.setCanceled(true);};
    NeoForge.EVENT_BUS.addListener(callback);try{h.assertTrue(!f.f.board(f.c)&&!f.c.isPassenger()&&f.f.rider()==null&&f.f.claimant().isEmpty(),"Canceled real mount restores occupancy/claim");}finally{NeoForge.EVENT_BUS.unregister(callback);}
    h.assertTrue(f.f.board(f.c)&&f.f.rider()==f.c,"Normal retry boards once");h.succeed();
  }
  private static void pickup(GameTestHelper h,String kind){
    var f=fixture(h,kind,false);for(int i=0;i<36;i++)f.p.getInventory().setItem(i,new ItemStack(Items.STONE,64));
    f.p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);f.p.setShiftKeyDown(true);
    h.assertTrue(!f.f.interact(f.p,InteractionHand.OFF_HAND).consumesAction()&&!f.f.isRemoved()&&f.f.storedItem().getCount()==1,"A full inventory cannot consume the physical toy");
    f.p.getInventory().setItem(10,ItemStack.EMPTY);
    h.assertTrue(f.f.interact(f.p,InteractionHand.OFF_HAND).consumesAction()&&f.f.isRemoved()&&f.f.storedItem().isEmpty(),"Pickup transfers exactly one payload before source removal");
    int count=0;for(int i=0;i<36;i++)if(f.p.getInventory().getItem(i).is(ChaoGarden.item(kind)))count+=f.p.getInventory().getItem(i).getCount();
    h.assertTrue(count==1&&drops(h,f.f)==0,"One inventory toy, no world duplicate");h.succeed();
  }
  private static void lease(GameTestHelper h,String kind){
    var f=fixture(h,kind,false);h.assertTrue(f.f.claim(f.c),"Approaching owner reserves float");var tag=new CompoundTag();f.f.save(tag);
    f.c.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);f.f.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
    var restored=(FloatToyEntity080)EntityType.loadEntityRecursive(tag,h.getLevel(),e->e);h.assertTrue(restored!=null&&h.getLevel().addFreshEntity(restored),"Claimed float reloads without missing owner");
    cleanup(h,()->restored.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK));
    h.runAfterDelay(86,()->{h.assertTrue(restored.claimant().isEmpty()&&restored.storedItem().getCount()==1,"Missing/different-dimension owner cannot retain a permanent claim");h.succeed();});
  }
  private static void blockedEntry(GameTestHelper h,String kind){
    var f=fixture(h,kind,false);h.assertTrue(f.f.board(f.c),"Actual local entry starts");
    h.runAfterDelay(1,()->h.getLevel().setBlock(f.bank.east(),Blocks.STONE.defaultBlockState(),3));
    var previous=new java.util.concurrent.atomic.AtomicReference<>(f.c.position());
    h.onEachTick(()->{h.assertTrue(f.c.position().distanceTo(previous.get())<.35,"Blocked entry reverses gradually without seat snap");previous.set(f.c.position());});
    h.succeedWhen(()->h.assertTrue(!f.c.isPassenger()&&f.f.phase()!=FloatToyEntity080.BOARD&&FloatToyRoutes080.safeFeet(h.getLevel(),f.c,f.c.position())&&!f.f.isRemoved()&&f.f.storedItem().getCount()==1,"Obstructed entry returns to original dry support without dropping toy"));
  }
  private static void drained(GameTestHelper h,String kind){
    var f=fixture(h,kind,true);h.assertTrue(f.f.board(f.c),"Real mount begins");f.f.previewHeld(true);
    h.runAfterDelay(22,()->{for(int x=8;x<=14;x++)for(int z=4;z<=14;z++)h.setBlock(new BlockPos(x,1,z),Blocks.AIR);f.f.previewHeld(false);});
    h.succeedWhen(()->h.assertTrue(!f.c.isPassenger()&&f.f.phase()!=FloatToyEntity080.BOARD&&FloatToyRoutes080.safeFeet(h.getLevel(),f.c,f.c.position()),"Drained solid pool offers direct safe exit without a water BFS"));
  }
  private static void preview(GameTestHelper h,String kind,boolean adult,boolean loop){
    var f=fixture(h,kind,adult);var before=f.c.state().encode();String alias=kind.substring(5);var position=f.f.position();
    h.assertTrue(command(f.p,"chao testtoy "+alias+" "+(loop?"loop":"once"))==1,"Native float once/loop accepted");
    var previous=new java.util.concurrent.atomic.AtomicReference<>(f.f.position());
    h.onEachTick(()->{if(h.testInfo.isDone())return;var now=f.f.position();
      h.assertTrue(Math.abs(now.y-previous.get().y)<=.081,"Preview retains bounded real buoyancy, without position snapping");
      if(ToyPreview.active(f.c))h.assertTrue(now.subtract(position).horizontalDistance()<.001,"Preview holds the actual hull horizontally");previous.set(now);
    });
    h.runAfterDelay(40,()->h.assertTrue(ToyPreview.active(f.c)&&f.f.rider()==f.c&&f.f.phase()==FloatToyEntity080.RIDE
        &&f.f.position().subtract(position).horizontalDistance()<.001
        &&FloatToyRoutes080.surfaceAt(h.getLevel(),f.f.position())!=null
        &&Math.abs(f.f.getY()-(FloatToyRoutes080.surfaceAt(h.getLevel(),f.f.position())-FloatToyRoutes080.DRAFT))<.002
        &&f.c.state().encode().equals(before),"Production local entry/vehicle seat with frozen care and no item/teleport: active="+ToyPreview.active(f.c)+" rider="+(f.f.rider()==f.c)+" phase="+f.f.phase()+" progress="+f.f.transition()+" boatdelta="+f.f.position().subtract(position)+" care="+stateDiff(before,f.c.state().encode())+" unavailable="+ToyPreview.unavailable(f.c)));
    h.runAfterDelay(loop?330:160,()->{
      if(loop)h.assertTrue(command(f.p,"chao testtoy "+alias+" stop")==1,"Native stop releases loop");
      else if(ToyPreview.active(f.c))ToyPreview.tick(f.c);
      h.assertTrue(!ToyPreview.active(f.c)&&f.c.toyPlayingTicks()==0&&!f.c.isNoAi()&&!f.c.getPersistentData().contains("ChaoToyPreviewRestoreNoAi"),"Preview flags and tracked expression cleared; safe landing may finish afterward");
      h.assertTrue(f.c.state().encode().equals(before)&&!f.f.isRemoved()&&f.f.storedItem().getCount()==1,"Preview changes no saved care or inventory");h.succeed();
    });
  }
  private static void interrupt(GameTestHelper h,String mode){
    var f=fixture(h,"chao_lifesaver",true);boolean original=mode.equals("original_noai");f.c.setNoAi(original);
    h.assertTrue(command(f.p,"chao testtoy lifesaver loop")==1,"Native float preview starts");
    h.runAfterDelay(28,()->{
      switch(mode){
        case "stop","original_noai"->command(f.p,"chao testtoy stop");
        case "damage"->{f.c.setInvulnerable(false);f.c.hurt(h.getLevel().damageSources().generic(),1);}
        case "pet"->f.c.mobInteract(f.p,InteractionHand.MAIN_HAND);
        case "logout"->f.p.server.getPlayerList().remove(f.p);
        case "range"->f.p.teleportTo(f.c.getX()+70,f.c.getY(),f.c.getZ());
        case "reload"->{var saved=new CompoundTag();f.c.saveWithoutId(saved);h.assertTrue(!saved.getBoolean("NoAI")&&!saved.contains("ToyPlayingKind"),"Temporary flags cannot persist");ToyPreview.stop(f.c);}
        case "unload"->f.c.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
      }
      h.runAfterDelay(2,()->{h.assertTrue(!ToyPreview.active(f.c)&&f.c.toyPlayingTicks()==0&&f.c.isNoAi()==original,"Interruption restores exact original AI/expression flags");
        h.assertTrue(f.f.isRemoved()||f.f.storedItem().getCount()==1,"Interruption retains sole real toy");h.succeed();});
    });
  }
  private static void permissions(GameTestHelper h){var f=fixture(h,"chao_lifesaver",false);
    h.assertTrue(command(f.p,"chao testtoy lifesaver once",0)<=0&&!ToyPreview.active(f.c),"Nonoperator cannot drive preview");
    for(String id:List.of("lifesaver","rubber_duck")){var node=f.p.server.getCommands().getDispatcher().getRoot().getChild("chao").getChild("testtoy").getChild(id);h.assertTrue(node!=null&&node.getChild("once")!=null&&node.getChild("loop")!=null&&node.getChild("stop")!=null,"Native controls for both existing item IDs");}
    h.succeed();
  }
  private static void loopRemoval(GameTestHelper h,String kind,String mode){
    var f=fixture(h,kind,true);boolean noAi=mode.equals("pickup");f.c.setNoAi(noAi);
    h.assertTrue(command(f.p,"chao testtoy "+kind.substring(5)+" loop")==1,"Native actual-vessel loop starts before removal interaction");
    var interrupted=new java.util.concurrent.atomic.AtomicBoolean();
    h.runAfterDelay(40,()->{
      h.assertTrue(ToyPreview.active(f.c)&&f.f.rider()==f.c&&f.f.phase()==FloatToyEntity080.RIDE,"Actual loop reached production ride");
      for(int x=1;x<=18;x++)for(int z=1;z<=18;z++)h.setBlock(new BlockPos(x,1,z),Blocks.WATER);
      switch(mode){
        case "pickup"->{f.p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);f.p.setShiftKeyDown(true);h.assertTrue(f.f.interact(f.p,InteractionHand.MAIN_HAND).consumesAction(),"Actual occupied shift-pickup handled");}
        case "hurt"->h.assertTrue(f.f.hurt(h.getLevel().damageSources().generic(),1),"Actual occupied break handled");
        case "remove"->f.f.remove(Entity.RemovalReason.KILLED);
      }
      interrupted.set(true);
      h.assertTrue(!ToyPreview.active(f.c)&&f.c.toyPlayingTicks()==0&&f.c.isNoAi()==noAi&&!f.c.getPersistentData().contains("ChaoToyPreviewRestoreNoAi"),"Removal immediately clears loop/expression and restores original AI without an explicit preview stop");
      h.assertTrue(!f.f.isRemoved()&&f.f.rider()==f.c&&f.f.storedItem().getCount()==1&&drops(h,f.f)==0,"Unsafe shore still defers destructive release and item publication");
      h.runAfterDelay(40,()->{
        h.assertTrue(!ToyPreview.active(f.c)&&f.f.rider()==f.c&&drops(h,f.f)==0,"Stopped preview never re-enables its hold while the real rider waits safely");
        h.getLevel().setBlock(f.bank.below(),Blocks.STONE.defaultBlockState(),3);h.getLevel().setBlock(f.bank.west().below(),Blocks.STONE.defaultBlockState(),3);
      });
    });
    h.succeedWhen(()->{
      h.assertTrue(interrupted.get()&&f.f.isRemoved()&&!f.c.isPassenger()&&!ToyPreview.active(f.c)&&f.c.isNoAi()==noAi
          &&FloatToyRoutes080.safeFeet(h.getLevel(),f.c,f.c.position())&&drops(h,f.f)==1,"Interrupted native loop proceeds through safe physical egress and exactly one delayed item drop");
      f.f.remove(Entity.RemovalReason.KILLED);h.assertTrue(drops(h,f.f)==1&&f.c.isAlive(),"Repeated removal cannot duplicate a toy or lose the rider");
    });
  }
  private static void settledRing(GameTestHelper h,boolean adult){
    var f=fixture(h,"chao_lifesaver",adult);f.c.setNoAi(adult);
    var began=new java.util.concurrent.atomic.AtomicBoolean();var rode=new java.util.concurrent.atomic.AtomicBoolean();var care=new java.util.concurrent.atomic.AtomicReference<String>();
    h.runAfterDelay(70,()->{
      Double top=FloatToyRoutes080.surfaceAt(h.getLevel(),f.f.position());
      h.assertTrue(top!=null&&Math.abs(f.f.getY()-(top-FloatToyRoutes080.DRAFT))<.001,"Actual source-water buoyancy settles before boarding; no manual float Y relocation");
      var at=f.c.position();h.getLevel().setBlock(f.bank.above(),Blocks.STONE.defaultBlockState(),3);
      h.assertTrue(command(f.p,"chao testtoy lifesaver once")<=0&&!ToyPreview.active(f.c)&&f.f.claimant().isEmpty()&&f.c.position().equals(at),"A ceiling still rejects the real entry and releases failed-preview ownership without moving Chao");
      h.getLevel().setBlock(f.bank.above(),Blocks.AIR.defaultBlockState(),3);care.set(f.c.state().encode());
      h.assertTrue(command(f.p,"chao testtoy lifesaver once")==1,"Native operator preview boards from the bank at the actual settled ring height");began.set(true);
    });
    h.onEachTick(()->{if(h.testInfo.isDone()||!began.get())return;
      h.assertTrue(!h.getLevel().getBlockCollisions(f.c,f.c.getBoundingBox().deflate(.00001)).iterator().hasNext(),"Full actual baby/adult body clears the bank and ceiling throughout local entry/exit");
      if(ToyPreview.active(f.c))h.assertTrue(f.c.state().encode().equals(care.get()),"Settled entry uses the same reward-free production preview");
      if(f.f.phase()==FloatToyEntity080.RIDE&&f.f.rider()==f.c)rode.set(true);
    });
    h.succeedWhen(()->h.assertTrue(began.get()&&rode.get()&&!ToyPreview.active(f.c)&&!f.c.isPassenger()&&f.c.isNoAi()==adult
        &&FloatToyRoutes080.safeFeet(h.getLevel(),f.c,f.c.position())&&f.f.storedItem().getCount()==1,"Once preview completes real settled boarding, connected ride and safe bank egress with exact original AI and one toy"));
  }
  private static void reward(GameTestHelper h,boolean reload,boolean interrupt){
    var fixture=fixture(h,"chao_lifesaver",true);var boat=new java.util.concurrent.atomic.AtomicReference<>(fixture.f);var actor=new java.util.concurrent.atomic.AtomicReference<>(fixture.c);
    h.assertTrue(fixture.f.board(fixture.c),"Dry voluntary board uses natural ride; only an actual distressed water rescue seeks immediate land");
    if(interrupt)h.runAfterDelay(70,()->{boat.get().requestLand();for(int x=1;x<=18;x++)for(int z=1;z<=18;z++)h.setBlock(new BlockPos(x,1,z),Blocks.WATER);});
    if(reload)h.runAfterDelay(100,()->{
      var saved=new CompoundTag();boat.get().save(saved);actor.get().remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);boat.get().remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
      var restored=(FloatToyEntity080)EntityType.loadEntityRecursive(saved,h.getLevel(),e->e);h.getLevel().addFreshEntityWithPassengers(restored);boat.set(restored);actor.set(restored.rider());
      cleanup(h,()->{if(restored.rider()!=null)restored.rider().remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);restored.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);});
    });
    h.runAfterDelay(210,()->{
      var saved=new CompoundTag();boat.get().saveWithoutId(saved);
      h.assertTrue(saved.getBoolean("RewardPaid080")==!interrupt,"Exactly one completed ride benefit; interrupted safe-wait never counts as play");
      h.assertTrue(actor.get().state().happiness==(interrupt?50:54),"Existing natural happiness reward is retained once, including through reload");
      h.assertTrue(actor.get().state().boredom<=(interrupt?100:65),"Existing boredom benefit retained without repeated rewards");
      int happy=actor.get().state().happiness;h.runAfterDelay(20,()->{h.assertTrue(actor.get().state().happiness==happy,"No duplicate benefit on later wait/ride ticks");h.succeed();});
    });
  }
  private static void support(GameTestHelper h){
    try{
      var method=FloatToyRoutes080.class.getDeclaredMethod("coveredXZ",double.class,double.class,double.class,double.class,List.class);method.setAccessible(true);
      var full=List.of(new AABB(0,0,0,1,1,1));
      var tiled=List.of(new AABB(0,0,0,.37,1,1),new AABB(.37,0,0,1,1,.41),new AABB(.37,0,.41,1,1,1));
      var thinHole=List.of(new AABB(0,0,0,.123,1,1),new AABB(.124,0,0,1,1,1));
      var outside=List.of(new AABB(0,0,0,1,1,.999));
      h.assertTrue((boolean)method.invoke(null,0d,0d,1d,1d,full)&&(boolean)method.invoke(null,0d,0d,1d,1d,tiled),"Whole and tiled solid support accepted");
      h.assertTrue(!(boolean)method.invoke(null,0d,0d,1d,1d,thinHole)&&!(boolean)method.invoke(null,0d,0d,1d,1d,outside),"Thin unsupported interior and edge strip reject despite supported corners/center samples");
      h.succeed();
    }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
  }
  private static void ordinaryWater(GameTestHelper h){floor(h);var c=actor(h,Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(11,1,9))),false);float health=c.getHealth();
    c.setAirSupply(1);h.runAfterDelay(60,()->{h.assertTrue(!c.isPassenger()&&c.getHealth()==health&&c.getAirSupply()==c.getMaxAirSupply(),"Existing non-swimmer distress remains nonlethal without toys");h.succeed();});
  }
}
