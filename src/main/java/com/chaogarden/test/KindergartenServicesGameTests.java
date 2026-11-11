package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.care.ChaoCareServices;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.hub.HubContent;
import com.chaogarden.item.ChaoCarrierItem;
import com.chaogarden.kindergarten.*;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real room residents, held-item transactions and the durable shared classroom. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class KindergartenServicesGameTests {
  private static final java.util.Map<GameTestHelper,java.util.Set<ServerPlayer>> OWNED=new java.util.WeakHashMap<>();
  private static final java.util.Set<KindergartenContent.Role> ALL_ROLES=java.util.EnumSet.allOf(KindergartenContent.Role.class);
  private static ServerPlayer player(GameTestHelper h,KindergartenContent.Role role){return player(h,role,500);}
  private static ServerPlayer player(GameTestHelper h,KindergartenContent.Role role,int overallBudget){
    var p=TestConnections.makeSurvivalServerPlayer(h);p.getInventory().clearContent();
    if(!OWNED.containsKey(h)){
      OWNED.put(h,new java.util.HashSet<>());
      h.runAtTickTime(overallBudget-1,()->{if(OWNED.containsKey(h)){com.mojang.logging.LogUtils.getLogger().warn("CHAO_KIND_FIXTURE_DEADLINE tick={} players={}",h.getTick(),OWNED.get(h).size());cleanup(h);}});
    }
    OWNED.get(h).add(p);
    try{var level=p.server.getLevel(KindergartenContent.DIMENSION);h.assertTrue(level!=null&&HubContent.ensure(level),"Kindergarten is not registered/built");
      visit(p,role);p.setNoGravity(true);return p;
    }catch(RuntimeException failure){cleanup(h);throw failure;}
  }
  private static void visit(ServerPlayer p,KindergartenContent.Role role){var at=role.anchor;p.teleportTo(p.server.getLevel(KindergartenContent.DIMENSION),at.getX()+.5,80,at.getZ()+2.5,0,0);p.hasChangedDimension();}
  private static ItemStack carried(ServerPlayer p,int slot,String name){
    var chao=ChaoGarden.CHAO.get().create(p.serverLevel());chao.owner=p.getUUID();var state=new ChaoState();state.hunger=83;state.energy=100;state.happiness=84;chao.setState(state);chao.setCustomName(Component.literal(name));
    var item=new ItemStack(ChaoGarden.item("chao_carrier"));if(!ChaoCarrierItem.capture(item,chao,p))throw new IllegalStateException("Fixture cannot capture");p.getInventory().setItem(slot,item);return item;
  }
  private static String key(ItemStack item,int slot){return "i:"+slot+":"+ChaoCarrierItem.storedUUID(item);}
  private static ChaoEntity body(ServerPlayer p,UUID id){return (ChaoEntity)p.server.getLevel(KindergartenContent.DIMENSION).getEntity(id);}
  private static void finish(ServerPlayer p){if(HubContent.hasReturnJournal(p))HubContent.returnHome(p);p.server.getPlayerList().remove(p);OWNED.values().forEach(set->set.remove(p));OWNED.entrySet().removeIf(e->e.getValue().isEmpty());}
  private static void cleanup(GameTestHelper h){
    var players=OWNED.remove(h);if(players==null)return;
    var ids=players.stream().map(ServerPlayer::getUUID).collect(java.util.stream.Collectors.toSet());var server=h.getLevel().getServer();
    var roster=KindergartenRoster.get(server);var school=server.getLevel(KindergartenContent.DIMENSION);
    for(var student:java.util.List.copyOf(roster.students.values()))if(ids.contains(student.keeper)){
      if(school!=null&&school.getEntity(student.id) instanceof ChaoEntity body)body.discard();roster.students.remove(student.id);roster.setDirty();
    }
    for(var p:java.util.List.copyOf(players))finish(p);
  }
  private static void checked(GameTestHelper h,Runnable task){try{task.run();}catch(RuntimeException failure){cleanup(h);throw failure;}}
  private static int count(ServerPlayer p,net.minecraft.world.item.Item item){int n=0;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(item))n+=p.getInventory().getItem(i).getCount();return n;}

  private static String residentStatus(ServerPlayer p,java.util.Set<KindergartenContent.Role> roles,boolean market){
    var level=p.server.getLevel(KindergartenContent.DIMENSION);if(level==null)return "Kindergarten dimension absent";
    var result=new StringBuilder("dimension="+level.dimension().location()+" playerDimension="+p.level().dimension().location()+" players="+level.players().size()+" gameTime="+level.getGameTime());
    for(var role:roles){var actors=level.getEntitiesOfClass(KindergartenNpc.class,new AABB(role.anchor).inflate(4),n->n.role()==role);
      result.append(" ").append(role).append('=').append(actors.size()).append(" loaded=").append(level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(role.anchor))).append(" ticking=").append(level.isPositionEntityTicking(role.anchor));}
    if(market)result.append(" market=").append(level.getEntitiesOfClass(com.chaogarden.market.BlackMarketTrader.class,new AABB(new net.minecraft.core.BlockPos(-22,80,5)).inflate(4),t->t.resident()).size());
    return result.toString();
  }
  /** Accelerated GameTest CPU ticks are not wall seconds; each asynchronous phase has its own explicit budget. */
  private static void await(GameTestHelper h,ServerPlayer p,String phase,int tickBudget,java.util.function.BooleanSupplier condition,Runnable task){
    long start=h.getTick(),wall=System.nanoTime();boolean[] complete={false};
    com.mojang.logging.LogUtils.getLogger().info("CHAO_KIND_PHASE_START phase={} tick={} budget={} player={}",phase,start,tickBudget,p.getUUID());
    h.runAtTickTime(start+tickBudget,()->{if(!complete[0]){String state=residentStatus(p,ALL_ROLES,true);cleanup(h);h.fail("Kindergarten phase "+phase+" exceeded "+tickBudget+" CPU ticks; wallMs="+(System.nanoTime()-wall)/1000000+" "+state);}});
    h.startSequence().thenWaitUntil(()->{
      boolean ready=condition.getAsBoolean();
      if(!ready&&(System.nanoTime()-wall)>java.util.concurrent.TimeUnit.SECONDS.toNanos(12)){
        String state=residentStatus(p,ALL_ROLES,true);cleanup(h);throw new IllegalStateException("Kindergarten phase "+phase+" exceeded12s wall I/O budget at tick "+h.getTick()+" "+state);
      }
      h.assertTrue(ready,"Waiting for Kindergarten phase "+phase+" ticks="+(h.getTick()-start)+" wallMs="+(System.nanoTime()-wall)/1000000+" "+residentStatus(p,ALL_ROLES,true));
    }).thenExecute(()->{
      complete[0]=true;com.mojang.logging.LogUtils.getLogger().info("CHAO_KIND_PHASE_READY phase={} tick={} elapsedTicks={} wallMs={} {}",phase,h.getTick(),h.getTick()-start,(System.nanoTime()-wall)/1000000,residentStatus(p,ALL_ROLES,true));checked(h,task);
    });
  }
  private static void ready(GameTestHelper h,ServerPlayer p,String phase,java.util.Set<KindergartenContent.Role> roles,boolean market,Runnable task){
    await(h,p,phase,400,()->{
      var level=p.serverLevel();KindergartenContent.ensureServices(level);
      for(var role:roles){var actors=level.getEntitiesOfClass(KindergartenNpc.class,new AABB(role.anchor).inflate(4),n->n.role()==role);
        if(actors.size()!=1||level.getEntity(actors.getFirst().getId())!=actors.getFirst())return false;}
      if(market){
        // Published actors can be visible one tick before their service chunks
        // become entity-ticking. Production deliberately defers merchant repair
        // until the whole campus is loaded+ticking, so this fixture must await
        // that same precondition before asserting synchronous resident recovery.
        for(var role:ALL_ROLES)if(!level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(role.anchor))||!level.isPositionEntityTicking(role.anchor))return false;
        var at=KindergartenContent.MARKET_ANCHOR;
        if(!level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(at))||!level.isPositionEntityTicking(at))return false;
        var actors=level.getEntitiesOfClass(com.chaogarden.market.BlackMarketTrader.class,new AABB(at).inflate(4),t->t.resident());
        if(actors.size()!=1||level.getEntity(actors.getFirst().getId())!=actors.getFirst())return false;
      }
      return true;
    },task);
  }

  @GameTest(template="empty",timeoutTicks=500)
  public static void realDoctorAndFortuneMutateTheSameHeldChaoAndRejectStrangers(GameTestHelper h){
    var p=player(h,KindergartenContent.Role.DOCTOR);
    ready(h,p,"doctor-and-fortune",java.util.EnumSet.of(KindergartenContent.Role.DOCTOR,KindergartenContent.Role.FORTUNE),false,()->{var item=carried(p,0,"My Chao");var id=ChaoCarrierItem.storedUUID(item);
    var entity=ChaoCarrierItem.entityData(item);var state=ChaoCarrierItem.state(item);for(int i=0;i<6;i++)state.setIllness(i,-100);entity.putString("Chao",state.encode());ChaoCarrierItem.storeEntity(item,entity);
    var stranger=player(h,KindergartenContent.Role.DOCTOR);stranger.getInventory().setItem(0,item.copy());
    h.assertTrue(!KindergartenContent.treat(stranger,key(item,0)),"Doctor treated another keeper's carried Chao");
    var npc=p.serverLevel().getEntitiesOfClass(KindergartenNpc.class,new AABB(KindergartenContent.Role.DOCTOR.anchor).inflate(4),n->n.role()==KindergartenContent.Role.DOCTOR).getFirst();
    npc.mobInteract(p,net.minecraft.world.InteractionHand.MAIN_HAND);
    KindergartenNetwork.Open opened=null;Object outbound;var channel=(io.netty.channel.embedded.EmbeddedChannel)p.connection.getConnection().channel();
    channel.runPendingTasks();channel.flushOutbound();channel.runPendingTasks();var observed=new java.util.ArrayList<String>();
    while((outbound=channel.readOutbound())!=null){
      if(observed.size()<40)observed.add(outbound.getClass().getSimpleName());
      if(outbound instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket packet&&packet.payload() instanceof KindergartenNetwork.Open view)opened=view;
    }
    h.assertTrue(opened!=null,"No real service payload: authorized="+(KindergartenContent.describe(p,npc)!=null)+" dimension="+p.level().dimension().location()+" position="+p.position()+" alive="+p.isAlive()+" mode="+p.gameMode.getGameModeForPlayer()+" inventoryMenu="+(p.containerMenu==p.inventoryMenu)+" cursor="+p.inventoryMenu.getCarried()+" npc="+npc.position()+" distance="+p.distanceToSqr(npc)+" outbound="+observed);
    KindergartenContent.handle(p,new KindergartenNetwork.Action(npc.getId(),UUID.randomUUID().toString(),key(item,0),"cure",""));
    KindergartenContent.handle(p,new KindergartenNetwork.Action(Integer.MAX_VALUE,opened.nonce(),key(item,0),"cure",""));
    h.assertTrue(ChaoCarrierItem.state(item).isIll(),"Forged nonce/NPC request mutated a carried Chao");
    KindergartenContent.handle(p,new KindergartenNetwork.Action(npc.getId(),opened.nonce(),key(item,0),"refresh",""));
    KindergartenContent.handle(p,new KindergartenNetwork.Action(npc.getId(),opened.nonce(),key(item,0),"cure",""));
    h.assertTrue(!ChaoCarrierItem.state(item).isIll(),"Actual authorized service action did not treat the real carried Chao");
    h.assertTrue(!ChaoCarrierItem.state(item).isIll()&&id.equals(ChaoCarrierItem.storedUUID(item))&&item.getCount()==1,"Doctor lost identity, duplicated carrier or failed six-illness treatment");
    h.assertTrue(!KindergartenContent.treat(p,key(item,0)),"Healthy Chao treatment should be a no-op");
    visit(p,KindergartenContent.Role.FORTUNE);h.assertTrue(KindergartenContent.rename(p,key(item,0),"  Melody§\nStar  "),"Fortune house cannot name held Chao");
    h.assertTrue(item.getHoverName().getString().equals("MelodyStar")&&id.equals(ChaoCarrierItem.storedUUID(item)),"Name sanitization or same-Chao commit failed");
    var restored=ChaoGarden.CHAO.get().create(p.serverLevel());restored.load(ChaoCarrierItem.entityData(item));h.assertTrue(restored.getName().getString().equals("MelodyStar"),"Held name did not persist in entity data");
    p.teleportTo(0,80,0);h.assertTrue(!KindergartenContent.rename(p,key(item,0),"Remote"),"Fortune accepted out-of-range mutation");
    finish(stranger);finish(p);h.succeed();
  
    });
  }

  private static String classroomStatus(ServerPlayer p,ServerPlayer other,java.util.List<UUID> ids,String offered){
    var school=p.server.getLevel(KindergartenContent.DIMENSION);var roster=KindergartenRoster.get(p.server);
    var text=new StringBuilder("keeperDimensions="+p.level().dimension().location()+","+other.level().dimension().location()+" class="+offered);
    for(var id:ids){var c=body(p,id);var stored=roster.students.get(id);text.append(" [").append(id);
      if(c==null)text.append(" body=null");else text.append(" entityId=").append(c.getId()).append(" tickCount=").append(c.tickCount).append(" active=").append(c.state().trait("lesson.active")).append(" remaining=").append(c.state().traitInt("lesson.remaining",0)).append(" paused=").append(c.state().traitInt("lesson.paused",0)).append(" learned=").append(ChaoCareServices.lessonLevel(c.state(),offered)).append(" ticking=").append(school.isPositionEntityTicking(c.blockPosition()));
      text.append(" rosterRemaining=").append(stored==null?"missing":ChaoState.decode(stored.entity.getString("Chao")).traitInt("lesson.remaining",0)).append(']');
    }return text.toString();
  }

  @GameTest(template="empty",timeoutTicks=1400)
  public static void sharedClassroomPersistsIdentityCompletesAwayAndPaysRewardsOnce(GameTestHelper h){
    var p=player(h,KindergartenContent.Role.TEACHER,1400);
    ready(h,p,"classroom",java.util.EnumSet.of(KindergartenContent.Role.TEACHER),false,()->{var other=player(h,KindergartenContent.Role.TEACHER,1400);var roster=KindergartenRoster.get(p.server);
    h.assertTrue(roster.students.isEmpty(),"Classroom test needs its own initial roster");
    // Failed entity publication must release the provisional seat and retain the carrier.
    var rejected=carried(p,7,"Try again");var rejectedId=ChaoCarrierItem.storedUUID(rejected);var before=rejected.copy();
    java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> rejectStudent=event->{if(event.getEntity().getUUID().equals(rejectedId))event.setCanceled(true);};
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(rejectStudent);
    try{h.assertTrue(!KindergartenContent.enroll(p,key(rejected,7)),"Canceled student publication unexpectedly enrolled");}
    finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(rejectStudent);}
    h.assertTrue(roster.students.isEmpty()&&body(p,rejectedId)==null&&ItemStack.isSameItemSameComponents(before,rejected)&&rejected.getCount()==1,"Failed enrollment lost carrier, leaked a body or reserved a seat");
    p.getInventory().setItem(7,ItemStack.EMPTY);
    var ids=new java.util.ArrayList<UUID>();String offered=ChaoCareServices.currentLesson(p.level().getGameTime());
    for(int i=0;i<4;i++){
      var owner=i%2==0?p:other;var item=carried(owner,i,"Student "+i);var id=ChaoCarrierItem.storedUUID(item);ids.add(id);
      h.assertTrue(KindergartenContent.enroll(owner,key(item,i))&&item.isEmpty(),"Teacher failed one real held drop-off");
      var child=body(p,id);h.assertTrue(child!=null&&KindergartenContent.isBoarded(child)&&!child.shouldBeSaved(),"Classroom did not keep a single protected projection");
      h.assertTrue(!ChaoCarrierItem.capture(new ItemStack(ChaoGarden.item("chao_carrier")),child,owner),"Active student could be captured early");
      h.assertTrue(!child.canBeLeashed(),"Boarded student inherited generic Mob leash attachment");
      h.assertTrue(!child.canCare(owner)&&!child.hurt(child.damageSources().genericKill(),100),"Student remained mutable/damageable during class");
      h.assertTrue(!KindergartenContent.retrieve(owner,id),"Unfinished student could withdraw");
      if(i==0){int remaining=child.state().traitInt("lesson.remaining",0);child.state().setIllness(0,-100);
        ChaoCareServices.tickLearning(child);h.assertTrue(child.state().traitInt("lesson.remaining",0)==remaining&&child.state().traitInt("lesson.paused",0)==1,"Ill boarded student must pause");
        child.state().cureAllIllnesses();ChaoCareServices.tickLearning(child);h.assertTrue(child.state().traitInt("lesson.remaining",0)==remaining-1,"Treatment resumes the same daycare lesson");}
      child.state().setTraitInt("lesson.remaining",10);
      roster.students.get(id).entity.putString("Chao",child.state().encode());
    }
    var extra=carried(p,8,"Waiting");h.assertTrue(!KindergartenContent.enroll(p,key(extra,8))&&!extra.isEmpty(),"Shared capacity overflow consumed a fifth Chao");
    h.assertTrue(!KindergartenContent.retrieve(other,ids.getFirst()),"Another keeper could take a student");
    var serialized=roster.save(new CompoundTag(),p.registryAccess());var loaded=KindergartenRoster.load(serialized,p.registryAccess());
    h.assertTrue(loaded.students.size()==4&&loaded.freeSeat()==-1,"Four-student roster did not survive save/load");
    var first=body(p,ids.getFirst());var firstSnapshot=loaded.students.get(ids.getFirst()).entity;
    h.assertTrue(firstSnapshot.getUUID("UUID").equals(ids.getFirst())&&!firstSnapshot.getBoolean("NoGravity"),"Saved student identity or original physics was corrupted");
    first.discard(); // Reconstitute the unsaved classroom projection from the actual durable roster.
    p.teleportTo(h.getLevel(),0,100,0,0,0);other.teleportTo(h.getLevel(),0,100,0,0,0);
    long awayStart=h.getTick();boolean[] completedAway={false},publishedAway={false};
    h.runAtTickTime(awayStart+45,()->{if(!completedAway[0])com.mojang.logging.LogUtils.getLogger().info("CHAO_CLASSROOM_OLD45_PENDING tick={} {}",h.getTick(),classroomStatus(p,other,ids,offered));});
    h.runAtTickTime(awayStart+399,()->{if(!publishedAway[0])com.mojang.logging.LogUtils.getLogger().error("CHAO_CLASSROOM_PUBLICATION_DEADLINE tick={} {}",h.getTick(),classroomStatus(p,other,ids,offered));});
    await(h,p,"classroom-away-projections-published",400,()->{
      h.assertTrue(!KindergartenContent.inKindergarten(p.level())&&!KindergartenContent.inKindergarten(other.level()),"A keeper returned during away publication");
      boolean published=ids.stream().allMatch(id->{var c=body(p,id);return c!=null&&c.isAlive()&&KindergartenContent.isBoarded(c)&&p.server.getLevel(KindergartenContent.DIMENSION).getEntity(c.getId())==c;});
      if(published)com.mojang.logging.LogUtils.getLogger().info("CHAO_CLASSROOM_PUBLISHED tick={} {}",h.getTick(),classroomStatus(p,other,ids,offered));
      return published;
    },()->{
    publishedAway[0]=true;
    h.runAtTickTime(h.getTick()+399,()->{if(!completedAway[0])com.mojang.logging.LogUtils.getLogger().error("CHAO_CLASSROOM_COMPLETION_DEADLINE tick={} {}",h.getTick(),classroomStatus(p,other,ids,offered));});
    await(h,p,"classroom-actual-away-completion",400,()->{
      h.assertTrue(!KindergartenContent.inKindergarten(p.level())&&!KindergartenContent.inKindergarten(other.level()),"A keeper returned before actual lesson completion");
      return ids.stream().allMatch(id->{var c=body(p,id);return c!=null&&!ChaoCareServices.hasLesson(c)&&ChaoCareServices.lessonLevel(c.state(),offered)==1;});
    },()->{
      completedAway[0]=true;com.mojang.logging.LogUtils.getLogger().info("CHAO_CLASSROOM_COMPLETED_AWAY tick={} elapsed={} {}",h.getTick(),h.getTick()-awayStart,classroomStatus(p,other,ids,offered));
      for(UUID id:ids){var student=body(p,id);h.assertTrue(student!=null&&!ChaoCareServices.hasLesson(student)&&ChaoCareServices.lessonLevel(student.state(),offered)==1,"Classroom did not finish while both keepers were away: "+classroomStatus(p,other,ids,offered));}
      visit(p,KindergartenContent.Role.TEACHER);visit(other,KindergartenContent.Role.TEACHER);
      var student=body(p,ids.getFirst());student.state().setTraitInt("lesson.reward.bell",1);
      for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
      h.assertTrue(!KindergartenContent.retrieve(p,ids.getFirst())&&roster.students.containsKey(ids.getFirst()),"Full-inventory pickup lost the student");
      p.getInventory().setItem(0,ItemStack.EMPTY);
      var reentered=new java.util.concurrent.atomic.AtomicBoolean();
      java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> rejectReward=event->{
        if(event.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity drop&&drop.getItem().is(ChaoGarden.item("chao_bell"))){
          reentered.set(true);var reserved=p.getInventory().getItem(0);
          reserved.getItem().inventoryTick(reserved,p.level(),p,0,false);
          h.assertTrue(ChaoCarrierItem.filled(reserved)&&!KindergartenContent.retrieve(p,ids.getFirst()),"Reward callback exposed empty reservation or replayed retrieval");
          event.setCanceled(true);
        }
      };
      net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(rejectReward);
      try{h.assertTrue(KindergartenContent.retrieve(p,ids.getFirst()),"Ready student could not be picked up after inventory space opened");}
      finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(rejectReward);}
      h.assertTrue(reentered.get(),"One-free-slot fixture never exercised canceled reward drop callback");
      var result=p.getInventory().getItem(0);h.assertTrue(ChaoCarrierItem.filled(result)&&ids.getFirst().equals(ChaoCarrierItem.storedUUID(result)),"Retrieval changed UUID or yielded an empty carrier");
      h.assertTrue(count(p,ChaoGarden.item("chao_bell"))==0&&ChaoCarrierItem.state(result).traitInt("lesson.reward.bell",0)==2,"Canceled reward callback replayed or uncommitted the physical award");
      h.assertTrue(!KindergartenContent.retrieve(p,ids.getFirst())&&count(p,ChaoGarden.item("chao_bell"))==0,"Repeated pickup duplicated student/reward");
      p.getInventory().clearContent();
      for(int i=1;i<4;i++)h.assertTrue(KindergartenContent.retrieve(i%2==0?p:other,ids.get(i)),"Remaining owner could not collect student");
      h.assertTrue(roster.students.isEmpty(),"Pickup left an orphan roster entry");finish(other);finish(p);h.succeed();
    });
    });
  
    });
  }

  @GameTest(template="empty",timeoutTicks=500)
  public static void arbitraryLongNamesRemainBoundedInTheRealServicePayload(GameTestHelper h){
    var p=player(h,KindergartenContent.Role.DOCTOR);
    ready(h,p,"long-name-doctor",java.util.EnumSet.of(KindergartenContent.Role.DOCTOR),false,()->{String name="🌟".repeat(3000);
    for(int slot=0;slot<36;slot++)carried(p,slot,name);
    var npc=p.serverLevel().getEntitiesOfClass(KindergartenNpc.class,new AABB(KindergartenContent.Role.DOCTOR.anchor).inflate(4),n->n.role()==KindergartenContent.Role.DOCTOR).getFirst();
    var packet=KindergartenContent.describe(p,npc);h.assertTrue(packet!=null&&packet.name().length()<=96&&packet.options().length()<7900,"Service name/option display bounds failed");
    var bytes=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),p.registryAccess());
    try{KindergartenNetwork.Open.CODEC.encode(bytes,packet);var decoded=KindergartenNetwork.Open.CODEC.decode(bytes);h.assertTrue(decoded.options().equals(packet.options()),"Actual packet codec lost bounded long-name choices");}finally{bytes.release();}
    h.assertTrue(p.getInventory().getItem(0).getHoverName().getString().equals(name),"Display truncation changed the actual saved Chao name");finish(p);h.succeed();
  
    });
  }

  @GameTest(template="empty",batch="kindergarten_resident_lifecycle",timeoutTicks=1600)
  public static void permanentRolesAreDistinctProtectedAndEnsureDoesNotDuplicateThem(GameTestHelper h){
    var p=player(h,KindergartenContent.Role.PRINCIPAL,1600);
    ready(h,p,"lifecycle-initial-all-residents",ALL_ROLES,true,()->{var level=p.serverLevel();
    var duplicateTeacher=KindergartenContent.NPC.get().create(level);duplicateTeacher.role(KindergartenContent.Role.TEACHER);duplicateTeacher.moveTo(-17.5,80,-14.5,0,0);level.addFreshEntity(duplicateTeacher);
    KindergartenContent.ensureServices(level);KindergartenContent.ensureServices(level);
    for(var role:KindergartenContent.Role.values()){
      var npcs=level.getEntitiesOfClass(KindergartenNpc.class,new AABB(role.anchor).inflate(4),n->n.role()==role);
      h.assertTrue(npcs.size()==1,"Repeated room entry duplicated/missed resident "+role);var npc=npcs.getFirst();
      h.assertTrue(!npc.canCare(p)&&!npc.hurt(npc.damageSources().genericKill(),100),"Service resident could be changed/killed");
      p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.LEAD));p.interactOn(npc,net.minecraft.world.InteractionHand.MAIN_HAND);
      h.assertTrue(!npc.isLeashed()&&p.getMainHandItem().getCount()==1,"Generic staff interaction consumed a lead or attached it");
      var tag=new ItemStack(Items.NAME_TAG);tag.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,Component.literal("Griefed"));p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,tag);String before=npc.getName().getString();p.interactOn(npc,net.minecraft.world.InteractionHand.MAIN_HAND);
      h.assertTrue(npc.getName().getString().equals(before)&&tag.getCount()==1,"Generic name-tag path bypassed protected resident service interaction");
      h.assertTrue(!npc.canBeLeashed(),"Protected staff inherited generic Mob leash handling");npc.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);npc.push(1,0,1);
      h.assertTrue(npc.getDeltaMovement().lengthSqr()==0,"Protected staff accepted an external push");
      if(role==KindergartenContent.Role.PRINCIPAL)h.assertTrue(npc.getY()==80.5,"Principal was not raised onto the actual half-block stool");
      h.assertTrue(!ChaoCarrierItem.capture(new ItemStack(ChaoGarden.item("chao_carrier")),npc,p),"Staff could become a carried Chao");
    }
    var marketAt=new net.minecraft.core.BlockPos(-22,80,5);
    var trader=level.getEntitiesOfClass(com.chaogarden.market.BlackMarketTrader.class,new AABB(marketAt).inflate(4)).getFirst();
    // This tests persistence before restock, not the randomized restock timer.
    // Earlier batches can leave an already-expired resident offer list behind;
    // its next AI tick is then allowed to replace the purchased product.
    trader.getOffers();var stockFixture=new CompoundTag();trader.saveWithoutId(stockFixture);
    long previousRestock=stockFixture.getLong("MarketRestockAt");
    stockFixture.putLong("MarketRestockAt",level.getGameTime()+2400);trader.load(stockFixture);
    h.assertTrue(trader.restockTicksRemaining()==2400,"Resident persistence fixture must remain before restock");
    com.mojang.logging.LogUtils.getLogger().info("CHAO_KIND_STOCK_FIXTURE now={} previousRestock={} fixedRemaining={}",level.getGameTime(),previousRestock,trader.restockTicksRemaining());
    var keeperPosition=p.position();p.setPos(trader.getX()+.2,trader.getY(),trader.getZ());
    trader.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    for(int collision=0;collision<40;collision++){p.push(trader);trader.push(p);}
    h.assertTrue(!trader.isPushable()&&trader.getDeltaMovement().lengthSqr()==0,"Vanilla player collisions could move the resident outside its stock anchor");
    var visitor=com.chaogarden.market.MarketContent.TRADER.get().create(level);visitor.moveTo(trader.position());visitor.setNoAi(true);visitor.setInvulnerable(true);
    visitor.push(p);h.assertTrue(visitor.isPushable()&&visitor.getDeltaMovement().horizontalDistanceSqr()>0,"Resident collision protection changed ordinary wandering merchants");
    h.assertTrue(!trader.canBeLeashed()&&!visitor.canBeLeashed(),"Inherited merchant leash protection changed");visitor.discard();
    p.setPos(keeperPosition);p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    var offer=trader.getOffers().stream().filter(o->o.getResult().getItem() instanceof com.chaogarden.genes.GeneItem).findFirst().orElseThrow();offer.increaseUses();
    var retainedStock=trader.getOffers();var retainedUuid=trader.getUUID();
    // Reproduce the missing old resident: beyond the former search radius,
    // below the room, and carrying a saved invisible state.
    trader.moveTo(-14.5,73,5.5,-90,0);trader.setNoGravity(false);trader.setInvisible(true);
    trader.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.INVISIBILITY,1200));
    trader.setDeltaMovement(1,-1,1);KindergartenContent.ensureServices(level);KindergartenContent.ensureServices(level);
    h.assertTrue(level.getEntity(retainedUuid)==trader,"A drifted resident was replaced: expected="+retainedUuid+" actual="+level.getEntity(retainedUuid));
    h.assertTrue(trader.position().equals(new net.minecraft.world.phys.Vec3(-21.5,80,5.5)),"A drifted resident was left outside its shop: position="+trader.position()+" "+residentStatus(p,ALL_ROLES,true));
    h.assertTrue(!trader.isInvisible()&&!trader.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY)&&trader.isNoGravity()&&trader.getDeltaMovement().lengthSqr()==0,"Old hidden/falling merchant state was not repaired");
    h.assertTrue(!trader.isCustomNameVisible(),"Resident name leaked the hidden shop through the cabinet wall");
    h.assertTrue(trader.getOffers()==retainedStock&&offer.getUses()==1,"Resident recovery rerolled or reset purchased stock");
    var product=offer.getResult().getItem();var purchased=new CompoundTag();trader.saveWithoutId(purchased);offer.resetUses();
    var usedDuplicate=com.chaogarden.market.MarketContent.TRADER.get().create(level);usedDuplicate.load(purchased);usedDuplicate.setUUID(UUID.randomUUID());level.addFreshEntity(usedDuplicate);
    var merchantId=usedDuplicate.getUUID();KindergartenContent.ensureServices(level);
    var reconciled=level.getEntitiesOfClass(com.chaogarden.market.BlackMarketTrader.class,new AABB(marketAt).inflate(4));
    h.assertTrue(reconciled.size()==1&&reconciled.getFirst()==usedDuplicate,"Dedup discarded demonstrably purchased stock in favor of an unused stable actor");
    var saved=new CompoundTag();usedDuplicate.saveWithoutId(saved);usedDuplicate.discard();
    var restored=com.chaogarden.market.MarketContent.TRADER.get().create(level);restored.load(saved);h.assertTrue(level.addFreshEntity(restored),"Saved resident could not reload in its actual room");
    h.assertTrue(!restored.isPushable(),"Saved resident lost its collision protection after reload");
    await(h,p,"lifecycle-restored-actor-publication",100,()->level.getEntity(restored.getId())==restored,()->{
    p.teleportTo(h.getLevel(),0,100,0,0,0);
    await(h,p,"lifecycle-room-empty-ticket-release",400,()->{
      if(!level.players().isEmpty())return false;
      var forced=level.getDataStorage().computeIfAbsent(net.minecraft.world.level.ForcedChunksSavedData.factory(),"chunks").save(new CompoundTag(),level.registryAccess());
      return !forced.toString().contains("chao_garden:kindergarten_residents");
    },()->{
      h.assertTrue(level.players().isEmpty(),"Resident lifecycle batch did not isolate its visitor");
      visit(p,KindergartenContent.Role.PRINCIPAL);KindergartenContent.ensureServices(level);KindergartenContent.ensureServices(level);
      ready(h,p,"lifecycle-reentry-all-residents",ALL_ROLES,true,()->{
        var merchants=level.getEntitiesOfClass(com.chaogarden.market.BlackMarketTrader.class,new AABB(marketAt).inflate(4));
        h.assertTrue(merchants.size()==1&&merchants.getFirst().getUUID().equals(merchantId),"Reentry resident count="+merchants.size()+" actual="+merchants.stream().map(t->t.getUUID().toString()).toList()+" expected="+merchantId+" loaded="+level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(marketAt))+" ticking="+level.isPositionEntityTicking(marketAt));
        var retainedOffer=merchants.getFirst().getOffers().stream().filter(o->o.getResult().is(product)).findFirst();
        h.assertTrue(retainedOffer.isPresent()&&retainedOffer.get().getUses()==1,"Reentry rerolled finite DNA purchases before restock; remaining="+merchants.getFirst().restockTicksRemaining());
        finish(p);h.succeed();
      });
    });
    });
  
    });
  }
  // This test requires two seats in the one shared classroom. Do not race the
  // separate four-student capacity fixture in the default concurrent batch.
  @GameTest(template="activity_test",timeoutTicks=1400,batch="kindergarten_registered_gardens")
  public static void registeredGardenStudentsStayInClassAcrossReloadAndRetrieveWithRewards(GameTestHelper h){
    var p=player(h,KindergartenContent.Role.TEACHER,1400);
    ready(h,p,"registered-garden-daycare",java.util.EnumSet.of(KindergartenContent.Role.TEACHER),false,()->{
      var world=h.getLevel();var data=com.chaogarden.activities.GardenRegistry.get(p.server);
      var roster=KindergartenRoster.get(p.server);var ids=new java.util.ArrayList<UUID>();
      var places=new java.util.ArrayList<com.chaogarden.activities.GardenRegistry.Place>();
      var mat=h.absolutePos(new net.minecraft.core.BlockPos(14,1,16));
      p.teleportTo(world,mat.getX()+.5,mat.getY(),mat.getZ()+.5,0,0);p.hasChangedDimension();
      for(var at:net.minecraft.core.BlockPos.betweenClosed(mat.offset(-12,-1,-14),mat.offset(7,2,7)))
        world.setBlock(at,at.getY()==mat.getY()-1?net.minecraft.world.level.block.Blocks.STONE.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
      for(int x=0;x<7;x++)for(int z=0;z<7;z++)world.setBlock(mat.offset(x,0,z),com.chaogarden.activities.ActivityContent.MAT.get().defaultBlockState(),3);
      for(int i=0;i<2;i++){
        var root=h.absolutePos(new net.minecraft.core.BlockPos(4+i*8,1,4));var home=root.east(2);
        world.setBlock(root,com.chaogarden.activities.ActivityContent.GARDEN.get().defaultBlockState(),3);
        var garden=data.register(p,root);places.add(garden.place());
        // One active enclosure and one older non-area registry exercise both recall paths.
        if(i==0)data.areas.put(garden.place(),new com.chaogarden.activities.GardenArea(false,java.util.List.of(home.asLong()),home));
        var child=ChaoGarden.CHAO.get().create(world);var state=new ChaoState();state.hunger=83;state.energy=100;state.happiness=84;
        child.setState(state);child.owner=p.getUUID();child.setNoAi(true);child.setNoGravity(true);child.moveTo(home.getX()+.5,home.getY(),home.getZ()+.5,0,0);
        h.assertTrue(world.addFreshEntity(child)&&data.bind(p,child,garden),"Registered garden fixture failed");
        ids.add(child.getUUID());data.contain(child);
        if(i==0)h.assertTrue(data.entries.get(child.getUUID()).entered,"Enclosure resident never entered its garden");
        p.teleportTo(home.getX()+.5,home.getY(),home.getZ()+1.5);
        var item=new ItemStack(ChaoGarden.item("chao_carrier"));h.assertTrue(ChaoCarrierItem.capture(item,child,p),"Registered resident could not be carried to class");
        p.getInventory().setItem(i,item);
      }
      visit(p,KindergartenContent.Role.TEACHER);
      for(int i=0;i<2;i++){
        var item=p.getInventory().getItem(i);h.assertTrue(KindergartenContent.enroll(p,key(item,i))&&item.isEmpty(),"Registered carrier could not enroll");
        var child=body(p,ids.get(i));h.assertTrue(child!=null&&com.chaogarden.activities.GardenRegistry.managed(child),"Daycare did not protect the registered body");
        // Keep the real lesson desk/enrollment transaction; select a known first
        // instrument and shorten its duration so this regression also checks rewards.
        child.state().setTrait("lesson.active","bell");child.state().setTraitInt("lesson.remaining",120);
        roster.students.get(child.getUUID()).entity.putString("Chao",child.state().encode());
        data.contain(child);com.chaogarden.activities.ActivityManager.callHome(child,places.get(i));
        h.assertTrue(body(p,ids.get(i))==child&&KindergartenContent.isBoarded(child),"Garden containment or direct recall stole a boarded student");
      }
      // A remote garden-activity request must not take either enrolled student.
      p.teleportTo(world,mat.getX()+.5,mat.getY(),mat.getZ()+.5,0,0);p.hasChangedDimension();
      com.chaogarden.activities.ActivityManager.open(p,mat);
      for(var id:ids)com.chaogarden.activities.ActivityManager.handleAction(p,mat,"toggle",id);
      com.chaogarden.activities.ActivityManager.handleAction(p,mat,"start",null);
      for(var id:ids)h.assertTrue(!com.chaogarden.activities.ActivityManager.isBusy(body(p,id)),"Remote contest accepted a boarded student");
      com.chaogarden.activities.ActivityManager.handleAction(p,mat,"cancel",null);
      var savedGardens=com.chaogarden.activities.GardenRegistry.load(data.save(new CompoundTag(),p.registryAccess()),p.registryAccess());
      var savedRoster=KindergartenRoster.load(roster.save(new CompoundTag(),p.registryAccess()),p.registryAccess());
      for(var id:ids){
        data.entries.put(id,savedGardens.entries.get(id));roster.students.put(id,savedRoster.students.get(id));
      }
      h.assertTrue(data.entries.get(ids.getFirst()).entered,"Garden entered state did not survive save/load");
      for(var place:places)if(savedGardens.areas.containsKey(place))data.areas.put(place,savedGardens.areas.get(place));
      data.setDirty();roster.setDirty();body(p,ids.getFirst()).discard();
      // Rebuild the first projection from the restored durable roster while the keeper is away.
      await(h,p,"registered-student-republished",400,()->body(p,ids.getFirst())!=null&&KindergartenContent.isBoarded(body(p,ids.getFirst())),()->{
        h.runAfterDelay(80,()->checked(h,()->{
          for(int i=0;i<2;i++){
            var child=body(p,ids.get(i));h.assertTrue(child!=null&&KindergartenContent.isBoarded(child),"Native classroom ticks recalled a registered student");
            h.assertTrue(data.entries.get(ids.get(i)).garden.equals(places.get(i)),"Daycare lost the original garden registration");
            h.assertTrue(ChaoCareServices.hasLesson(child)||ChaoCareServices.lessonLevel(child.state(),"bell")==1,"Garden recall canceled the lesson");
          }
          await(h,p,"registered-students-learn-away",400,()->ids.stream().allMatch(id->{var c=body(p,id);return c!=null&&!ChaoCareServices.hasLesson(c)&&ChaoCareServices.lessonLevel(c.state(),"bell")==1;}),()->{
            visit(p,KindergartenContent.Role.TEACHER);
            for(var id:ids)h.assertTrue(KindergartenContent.retrieve(p,id),"Completed registered student could not be retrieved");
            h.assertTrue(count(p,ChaoGarden.item("chao_bell"))==2,"Retrieval did not pay exactly one first instrument per student");
            for(int i=0;i<2;i++){
              final UUID id=ids.get(i);ItemStack carrier=ItemStack.EMPTY;
              for(int slot=0;slot<36;slot++){var candidate=p.getInventory().getItem(slot);if(id.equals(ChaoCarrierItem.storedUUID(candidate)))carrier=candidate;}
              h.assertTrue(ChaoCarrierItem.filled(carrier)&&ChaoCareServices.lessonLevel(ChaoCarrierItem.state(carrier),"bell")==1,"Retrieval lost UUID or learned reward");
              h.assertTrue(ChaoCarrierItem.state(carrier).traitInt("lesson.reward.bell",0)==2&&!KindergartenContent.retrieve(p,id),"Reward or retrieval can replay");
              h.assertTrue(!roster.students.containsKey(id)&&data.entries.get(id).garden.equals(places.get(i)),"Retrieval lost registration or retained a roster entry");
              // Once school releases control, ordinary cross-dimension recall still works.
              var released=ChaoGarden.CHAO.get().create(p.serverLevel());released.load(ChaoCarrierItem.entityData(carrier));carrier.setCount(0);
              h.assertTrue(!com.chaogarden.activities.GardenRegistry.managed(released)&&p.serverLevel().addFreshEntity(released),"Retrieved carrier retained classroom management");
              com.chaogarden.activities.ActivityManager.callHome(released,places.get(i));
              var home=com.chaogarden.activities.GardenRegistry.find(p.server,id);
              h.assertTrue(home!=null&&home.level()==world,"Ordinary recall stopped working after daycare retrieval");home.discard();
            }
            for(var place:places)data.removeGarden(place);
            for(int x=0;x<7;x++)for(int z=0;z<7;z++)world.removeBlock(mat.offset(x,0,z),false);
            finish(p);h.succeed();
          });
        }));
      });
    });
  }
}
