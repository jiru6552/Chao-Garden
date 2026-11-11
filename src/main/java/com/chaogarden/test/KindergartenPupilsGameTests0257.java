package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.activities.GardenRegistry;
import com.chaogarden.care.ChaoCareServices;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.hub.HubContent;
import com.chaogarden.item.ChaoCarrierItem;
import com.chaogarden.kindergarten.KindergartenContent;
import com.chaogarden.kindergarten.KindergartenPupil0257;
import com.chaogarden.kindergarten.KindergartenPupils0257;
import com.chaogarden.toys.ChaoToyBlock;
import com.chaogarden.toys.ToyContent;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Actual school-owned bodies, native toy play and safe separation from keeper data. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class KindergartenPupilsGameTests0257 {
  private static List<KindergartenPupil0257> pupils(ServerLevel level){return level.getEntitiesOfClass(KindergartenPupil0257.class,new AABB(-30,76,-28,30,90,20));}
  private static ServerPlayer visit(GameTestHelper h){
    var keeper=TestConnections.makeSurvivalServerPlayer(h);var level=keeper.server.getLevel(KindergartenContent.DIMENSION);
    h.assertTrue(level!=null&&HubContent.ensure(level),"School scene unavailable");
    keeper.teleportTo(level,.5,80,.5,0,0);keeper.hasChangedDimension();return keeper;
  }
  private static void ready(GameTestHelper h,ServerPlayer keeper){
    var level=keeper.serverLevel();KindergartenContent.ensureServices(level);
    // Existing saved bodies can publish before a newly arriving keeper's chunk
    // tickets reach entity-ticking status. Mutation fixtures need that admission
    // precondition before expecting an immediate recovery sweep.
    for(var c:new int[][]{{-2,-1},{-1,0},{0,0}})h.assertTrue(
        level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(c[0],c[1]))
            &&level.isPositionEntityTicking(new BlockPos(c[0]*16+8,80,c[1]*16+8)),
        "Waiting for all pupil home chunks to publish and tick: "+c[0]+","+c[1]);
    var list=pupils(level);h.assertTrue(list.size()==4&&list.stream().allMatch(p->level.getEntity(p.getId())==p),"Waiting for the four published ambient school pupils");
    h.assertTrue(list.stream().allMatch(p->KindergartenPupils0257.withinClassroom(p)&&KindergartenPupils0257.withinHome(p)),
        "An ambient baby is in the foyer, corridor or outside its classroom pocket");
  }
  private static String cohortState(ServerLevel level){
    var state=new StringBuilder(" time=").append(level.getGameTime()).append(" chunks=");
    for(var c:new int[][]{{-2,-1},{-1,0},{0,0}})state.append(c[0]).append(',').append(c[1]).append(':')
        .append(level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(c[0],c[1]))).append('/')
        .append(level.isPositionEntityTicking(new BlockPos(c[0]*16+8,80,c[1]*16+8))).append(';');
    state.append(" pupils=");for(var p:pupils(level))state.append(p.schoolSlot()).append(':').append(p.getUUID())
        .append('@').append(p.position()).append(" removed=").append(p.isRemoved()).append(';');
    return state.toString();
  }
  @GameTest(template="empty",batch="school0257_cohort",timeoutTicks=500)
  public static void schoolCohortRetainsFourSavedIdentitiesAndRepairsDuplicatesWithoutTouchingOwnedChao(GameTestHelper h){
    var keeper=visit(h);var level=keeper.serverLevel();h.runAtTickTime(499,()->keeper.server.getPlayerList().remove(keeper));
    var recovering=new java.util.UUID[1];
    h.startSequence().thenWaitUntil(()->ready(h,keeper)).thenExecute(()->{
      try{
        var before=pupils(level);Set<java.util.UUID> ids=before.stream().map(ChaoEntity::getUUID).collect(Collectors.toSet());
        h.assertTrue(ids.size()==4&&before.stream().allMatch(p->p.isBaby()&&p.shouldBeSaved()&&p.owner==null),"Ambient pupils are not independent persistent babies");
        // Existing0.25.7 saves have two canonical bodies in the foyer. Migrate
        // those very bodies with their state intact, rather than minting four
        // replacement babies at the new classroom coordinates.
        for(int slot=0;slot<2;slot++){
          final int index=slot;var legacy=before.stream().filter(p->p.schoolSlot()==index).findFirst().orElseThrow();
          var id=legacy.getUUID();String payload=legacy.state().encode();legacy.getPersistentData().putString("Pupil0258Migration","retained "+slot);
          legacy.moveTo(slot==0?-2.5:3.5,80,2.5,0,0);KindergartenPupils0257.ensure(level);
          h.assertTrue(level.getEntity(id)==legacy&&KindergartenPupils0257.withinClassroom(legacy)&&KindergartenPupils0257.withinHome(legacy)
              &&legacy.state().encode().equals(payload)&&legacy.getPersistentData().getString("Pupil0258Migration").equals("retained "+slot),
              "Legacy foyer baby was replaced, rerolled or left outside the classroom");
        }
        h.assertTrue(level.getBlockState(new BlockPos(-3,80,3)).isAir()&&level.getBlockState(new BlockPos(3,80,3)).isAir(),
            "Old authored foyer toys survived the classroom-only room update");
        var retained=before.getFirst();retained.getPersistentData().putString("Pupil0257Fixture","keep original body");
        var saved=new CompoundTag();retained.saveWithoutId(saved);var restored=KindergartenContent.PUPIL.get().create(level);restored.load(saved);
        h.assertTrue(restored.schoolSlot()==retained.schoolSlot()&&restored.getUUID().equals(retained.getUUID())&&restored.isBaby(),"Pupil identity/slot did not survive native NBT save/load");
        var clone=KindergartenContent.PUPIL.get().create(level);clone.schoolSlot(retained.schoolSlot());clone.moveTo(retained.getX(),80,retained.getZ(),0,0);
        boolean accepted=level.addFreshEntity(clone),listed=false;for(var actor:level.getAllEntities())if(actor==clone)listed=true;
        KindergartenPupils0257.ensure(level);
        String duplicateState="clone="+clone.getUUID()
            +" accepted="+accepted+" listedBefore="+listed+" removed="+clone.isRemoved()
            +" published="+(level.getEntity(clone.getId())==clone)+cohortState(level);
        org.slf4j.LoggerFactory.getLogger(KindergartenPupilsGameTests0257.class).info("CHAO_PUPIL0257_DUPLICATE {}",duplicateState);
        h.assertTrue(clone.isRemoved()&&pupils(level).size()==4,"A duplicate school slot survives recovery: "+duplicateState);
        h.assertTrue(level.getEntity(retained.getUUID())==retained&&retained.getPersistentData().getString("Pupil0257Fixture").equals("keep original body"),"Recovery replaced a saved pupil UUID or payload");
        retained.moveTo(keeper.getX()+12,80,keeper.getZ(),0,0);KindergartenPupils0257.ensure(level);
        h.assertTrue(KindergartenPupils0257.withinHome(retained)&&KindergartenPupils0257.withinClassroom(retained)&&level.noCollision(retained),"Escaped pupil did not recover to a clear bounded classroom position");
        retained.moveTo(.5,80,.5,0,0);KindergartenPupils0257.tick(retained);
        h.assertTrue(KindergartenPupils0257.withinClassroom(retained)&&KindergartenPupils0257.withinHome(retained),
            "The next ambient AI tick did not rehome a displaced lobby baby immediately");
        h.assertTrue(ids.equals(pupils(level).stream().map(ChaoEntity::getUUID).collect(Collectors.toSet())),"Reentry/recovery changed the bounded cohort identities");
        recovering[0]=retained.getUUID();
        var at=net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(1,2,1)));
        h.assertTrue(retained.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(h.getLevel(),at,net.minecraft.world.phys.Vec3.ZERO,0,0,
            net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING))!=null,"Unable to move the controlled recovery fixture to another realm");
      }catch(RuntimeException|AssertionError failure){keeper.server.getPlayerList().remove(keeper);throw failure;}
    }).thenWaitUntil(()->{
      KindergartenPupils0257.ensure(level);var restored=level.getEntity(recovering[0]);
      h.assertTrue(restored instanceof KindergartenPupil0257 child&&KindergartenPupils0257.withinHome(child)
          &&child.getPersistentData().getString("Pupil0257Fixture").equals("keep original body")&&pupils(level).size()==4,
          "Waiting for same-UUID cross-realm pupil recovery, without duplicating its saved body");
    }).thenExecute(()->keeper.server.getPlayerList().remove(keeper)).thenSucceed();
  }
  @GameTest(template="empty",batch="school0257_lessons",timeoutTicks=500)
  public static void pupilsUseCurrentLessonAnimationsAndRealToySessionsWithoutAgingOrLearningRewards(GameTestHelper h){
    var keeper=visit(h);var level=keeper.serverLevel();h.runAtTickTime(499,()->keeper.server.getPlayerList().remove(keeper));
    var snapshots=new java.util.HashMap<java.util.UUID,String>();
    var watching=new java.util.concurrent.atomic.AtomicBoolean();
    h.onEachTick(()->{if(watching.get())h.assertTrue(pupils(level).stream().allMatch(KindergartenPupils0257::withinClassroom),
        "A live wandering, toy-playing or rehearsing ambient baby left the classroom");});
    h.startSequence().thenWaitUntil(()->ready(h,keeper)).thenExecute(()->{
      // Other school tests deliberately displace these durable bodies. Start
      // this live-AI fixture from a fresh idle schedule, not their random wander
      // destination/cooldown. Native navigation still starts the toy session.
      for(var pupil:pupils(level))if(pupil.schoolSlot()<2){
        ToyContent.endPlaying(pupil,false);var saved=new CompoundTag();pupil.saveWithoutId(saved);pupil.load(saved);
        var at=KindergartenPupils0257.home(pupil.schoolSlot());pupil.moveTo(at.x,at.y,at.z,0,0);
        pupil.getNavigation().stop();pupil.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);pupil.clearBehavior();
      }
    }).thenWaitUntil(()->{
      var current=ChaoCareServices.currentLesson(level.getGameTime());
      var seated=pupils(level).stream().filter(p->p.schoolSlot()>=2).toList();
      h.assertTrue(seated.size()==2&&seated.stream().allMatch(p->p.behaviorDetail().equals(current)&&p.getBehavior()==KindergartenPupils0257.lessonBehavior(current)),
          "Waiting for actual classroom AI to perform the teacher's current lesson");
      h.assertTrue(pupils(level).stream().anyMatch(p->p.schoolSlot()<2&&p.toyPlayingTicks()>0&&ToyContent.hasPlayerAt(level,KindergartenPupils0257.toy(p.schoolSlot()))),
          "Waiting for an ambient pupil's live AI to begin native toy play");
    }).thenExecute(()->{
      pupils(level).forEach(p->snapshots.put(p.getUUID(),p.state().encode()));
      watching.set(true);
    }).thenIdle(80).thenExecute(()->{
      try{
        h.assertTrue(pupils(level).stream().allMatch(p->snapshots.get(p.getUUID()).equals(p.state().encode())),"Ambient play/lessons changed care, aging or earned stats over actual loaded ticks");
        var child=pupils(level).stream().filter(p->p.schoolSlot()==2).findFirst().orElseThrow();
        for(var lesson:ChaoCareServices.LESSONS){KindergartenPupils0257.rehearse(child,lesson.id());
          h.assertTrue(child.getBehavior()==KindergartenPupils0257.lessonBehavior(lesson.id())&&child.behaviorDetail().equals(lesson.id()),"Current class has no corresponding synchronized pupil pose: "+lesson.id());
          h.assertTrue(!ChaoCareServices.hasLesson(child)&&ChaoCareServices.lessonLevel(child.state(),lesson.id())==0,"Ambient rehearsal consumed a keeper boarding slot or granted a lesson reward");
        }
        KindergartenPupils0257.tick(child);var current=ChaoCareServices.currentLesson(level.getGameTime());
        h.assertTrue(child.behaviorDetail().equals(current)&&child.getBehavior()==KindergartenPupils0257.lessonBehavior(current),"Ambient classroom does not follow the teacher's actual current lesson");
        int age=child.state().ageTicks;String state=child.state().encode();child.aiStep();
        h.assertTrue(child.state().ageTicks==age&&child.state().encode().equals(state),"School scenery advances care, aging or earned stats");
        var free=pupils(level).stream().filter(p->p.schoolSlot()==0).findFirst().orElseThrow();
        var stand=KindergartenPupils0257.home(0);free.moveTo(stand.x,80,stand.z,0,0);
        h.assertTrue(ToyContent.beginSchoolPlay(free,KindergartenPupils0257.toy(0),BlockPos.containing(stand)),"Pupil cannot start actual authored toy play");
        var toy=level.getBlockState(KindergartenPupils0257.toy(0));
        h.assertTrue(free.toyPlayingKind().equals("chao_ball")&&ToyContent.hasPlayerAt(level,KindergartenPupils0257.toy(0))&&toy.getValue(ChaoToyBlock.PLAYING),"Pupil play is disconnected from the actual toy block's animation");
        h.assertTrue(free.getMainHandItem().isEmpty(),"School toy play copied a physical toy into an item");ToyContent.endPlaying(free,false);
      }finally{watching.set(false);keeper.server.getPlayerList().remove(keeper);}
    }).thenSucceed();
  }
  @GameTest(template="empty",batch="school0257_ownership",timeoutTicks=500)
  public static void ambientPupilsCannotBeClaimedCapturedOrEnrolledAndOwnedBabiesRemainUsable(GameTestHelper h){
    var keeper=visit(h);var level=keeper.serverLevel();h.runAtTickTime(499,()->keeper.server.getPlayerList().remove(keeper));
    h.startSequence().thenWaitUntil(()->ready(h,keeper)).thenExecute(()->{
      ChaoEntity own=null;GardenRegistry data=GardenRegistry.get(keeper.server);
      try{
        var pupil=pupils(level).getFirst();var id=pupil.getUUID();var carrier=new ItemStack(ChaoGarden.item("chao_carrier"));
        h.assertTrue(!pupil.canCare(keeper)&&!ChaoCarrierItem.capture(carrier,pupil,keeper)&&!ChaoCarrierItem.filled(carrier)&&pupil.isAlive(),"Ambient pupil can be claimed or carried away");
        var interaction=new PlayerInteractEvent.EntityInteract(keeper,InteractionHand.MAIN_HAND,pupil);KindergartenContent.protectedInteraction(interaction);
        h.assertTrue(interaction.isCanceled()&&pupil.owner==null,"Generic entity interaction bypasses the school pupil guard");
        var garden=new GardenRegistry.Garden(GardenRegistry.Place.at(level,new BlockPos(2,80,7)),keeper.getUUID(),"Pupil boundary fixture");
        pupil.owner=keeper.getUUID();h.assertTrue(!data.bind(keeper,pupil,garden)&&!data.entries.containsKey(id),"Bell/transporter ownership spoof admitted a school pupil");pupil.owner=null;
        h.assertTrue(!pupil.mayBreed()&&!pupil.canBeLeashed()&&!pupil.hurt(level.damageSources().generic(),50),"A protected pupil accepts breeding, leads or damage");
        own=ChaoGarden.CHAO.get().create(level);own.owner=keeper.getUUID();own.moveTo(-1.5,80,5.5,0,0);level.addFreshEntity(own);
        h.assertTrue(own.isBaby()&&own.canCare(keeper)&&data.bind(keeper,own,garden),"Ambient protections blocked an ordinary owned baby");
        h.assertTrue(pupil.getUUID().equals(id)&&pupils(level).size()==4,"Ownership checks replaced or removed school scenery");
      }finally{if(own!=null){data.entries.remove(own.getUUID());data.setDirty();own.discard();}for(var p:pupils(level))p.owner=null;keeper.server.getPlayerList().remove(keeper);}
    }).thenSucceed();
  }
  private KindergartenPupilsGameTests0257(){}
}
