package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.activities.ActivityBlock;
import com.chaogarden.activities.ActivityContent;
import com.chaogarden.activities.ActivityManager;
import com.chaogarden.activities.GardenRegistry;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Integration checks exercise the real server lobby, navigation, rewards and return journal. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class ActivityGameTests {
  private static final BlockPos GARDEN_A = new BlockPos(3, 1, 3);
  private static final BlockPos GARDEN_B = new BlockPos(8, 1, 3);
  private static final BlockPos START = new BlockPos(3, 1, 8);
  private static final BlockPos FLAG = new BlockPos(3, 1, 14);
  private static final BlockPos FINISH = new BlockPos(3, 1, 20);
  private static final BlockPos MAT = new BlockPos(12, 1, 12);

  @GameTest(template="activity_test",timeoutTicks=40)
  public static void movedCourseDraftClearsOnlyStaleCoordinatesAndNeverConsumesAFlag(GameTestHelper h){
    floor(h);var level=h.getLevel();var p=TestConnections.makeSurvivalServerPlayer(h);
    var stack=new net.minecraft.world.item.ItemStack(ChaoGarden.item("race_marker"),3);
    var from=h.absolutePos(new BlockPos(2,1,2));var clicked=h.absolutePos(new BlockPos(4,0,4));
    try{
    var link=new CompoundTag();link.putString("Dimension",level.dimension().location().toString());link.putLong("Pos",from.asLong());
    net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,stack,t->{t.put("RaceLink",link);t.putString("RetainedTestData","preserved");});
    p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
    var hit=new net.minecraft.world.phys.BlockHitResult(clicked.getCenter().add(0,.5,0),Direction.UP,clicked,false);
    var context=new net.minecraft.world.item.context.UseOnContext(p,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
    h.assertTrue(stack.getItem().useOn(context)==net.minecraft.world.InteractionResult.FAIL,"Stale moved-course draft did not reject placement");
    var data=stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
    h.assertTrue(!data.contains("RaceLink")&&data.getString("RetainedTestData").equals("preserved"),"Stale draft cleanup erased unrelated item data or retained old coordinates");
    h.assertTrue(stack.getCount()==3&&level.getBlockState(clicked.above()).isAir(),"Stale draft consumed/placed a flag");
    level.setBlock(from,ActivityContent.START.get().defaultBlockState(),3);
    level.setBlock(from.east(),ActivityContent.START.get().defaultBlockState(),3);
    h.assertTrue(from.equals(ActivityManager.selectLink(p,from)),"Valid draft fixture did not claim a real two-section starting line");
    p.setPos(clicked.getX()+.5,clicked.getY()+1,clicked.getZ()+2.5);
    net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,stack,t->t.put("RaceLink",link.copy()));
    h.assertTrue(stack.getItem().useOn(context).consumesAction(),"A valid owned draft could not place its next flag");
    h.assertTrue(stack.getCount()==2&&level.getBlockState(clicked.above()).is(ActivityContent.FLAG.get()),"Valid marker placement failed to consume exactly one flag");
    var next=stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
    h.assertTrue(next.getCompound("RaceLink").getLong("Pos")==clicked.above().asLong()&&next.getString("RetainedTestData").equals("preserved"),"A valid physical course did not advance its draft while preserving other item data");
    h.assertTrue(GardenRegistry.Place.at(level,clicked.above()).equals(GardenRegistry.get(p.server).links.get(GardenRegistry.Place.at(level,from))),"Valid marker placement did not create the real course link");
    }finally{
      level.removeBlock(from,false);level.removeBlock(from.east(),false);level.removeBlock(clicked.above(),false);
      p.server.getPlayerList().remove(p);
    }
    h.succeed();
  }

  @GameTest(template = "activity_test", timeoutTicks = 40)
  public static void structuresRejectBentRowsAndIncompleteMats(GameTestHelper helper) {
    floor(helper);
    BlockPos first = new BlockPos(2, 1, 3);
    for (int length = 1; length <= 8; length++) {
      helper.setBlock(first.offset(length - 1, 0, 0), ActivityContent.START.get());
      helper.assertTrue(
          ActivityManager.line(
                      helper.getLevel(), helper.absolutePos(first), ActivityBlock.Kind.START)
                  .size()
              == length,
          "A straight starting row lost a section at length " + length);
    }
    helper.setBlock(first.offset(8, 0, 0), ActivityContent.START.get());
    helper.assertTrue(
        ActivityManager.line(helper.getLevel(), helper.absolutePos(first), ActivityBlock.Kind.START)
            .isEmpty(),
        "A nine-section starting line exceeded the eight-slot capacity");
    helper.setBlock(first.offset(8, 0, 0), Blocks.AIR);
    helper.setBlock(first.south(), ActivityContent.START.get());
    helper.assertTrue(
        ActivityManager.line(helper.getLevel(), helper.absolutePos(first), ActivityBlock.Kind.START)
            .isEmpty(),
        "A bent starting line was accepted");
    helper.setBlock(first.south(), Blocks.AIR);
    helper.setBlock(
        first.east(),
        ActivityContent.START
            .get()
            .defaultBlockState()
            .setValue(ActivityBlock.AXIS, Direction.Axis.Z));
    helper.assertTrue(
        ActivityManager.line(helper.getLevel(), helper.absolutePos(first), ActivityBlock.Kind.START)
            .isEmpty(),
        "A mismatched-axis starting section was accepted");

    makeMat(helper);
    helper.assertTrue(
        ActivityManager.mat(helper.getLevel(), helper.absolutePos(MAT.offset(3, 0, 4))).size()
            == 49,
        "An interior mat tile did not find the complete seven-by-seven arena");
    helper.setBlock(MAT.offset(3, 0, 3), Blocks.AIR);
    helper.assertTrue(
        ActivityManager.mat(helper.getLevel(), helper.absolutePos(MAT)).isEmpty(),
        "A mat with a hole was accepted");
    helper.setBlock(MAT.offset(3, 0, 3), ActivityContent.MAT.get());
    helper.setBlock(MAT.offset(7, 0, 0), ActivityContent.MAT.get());
    helper.assertTrue(
        ActivityManager.mat(helper.getLevel(), helper.absolutePos(MAT)).isEmpty(),
        "A mat with an extra connected tile was accepted");
    helper.succeed();
  }

  @GameTest(template = "activity_test", timeoutTicks = 500)
  public static void sharedRaceUsesOwnedBodiesAndReturnsBothKeepersChao(GameTestHelper helper) {
    Fixture f = race(helper);
    ActivityManager.open(f.host, f.start);
    // If this unauthorized enlist succeeded, the following legitimate toggle would remove it.
    ActivityManager.handleAction(f.guest, f.start, "toggle", f.first.getUUID());
    ActivityManager.handleAction(f.host, f.start, "toggle", f.first.getUUID());
    ActivityManager.handleAction(f.guest, f.start, "toggle", f.second.getUUID());
    ActivityManager.handleAction(f.guest, f.start, "start", null);
    helper.assertTrue(
        !ActivityManager.isBusy(f.first) && !ActivityManager.isBusy(f.second),
        "A guest started another keeper's race");
    ActivityManager.handleAction(f.host, f.start, "start", null);
    helper.assertTrue(
        ActivityManager.isBusy(f.first) && ActivityManager.isBusy(f.second),
        "The two keepers could not enter their own Chao in one shared lobby");
    helper
        .startSequence()
        .thenWaitUntil(
            () -> {
              helper.assertTrue(
                  f.first.getPersistentData().getInt("RaceExperience") > 0
                      && f.second.getPersistentData().getInt("RaceExperience") > 0,
                  "Both Chao must physically finish the linked course and receive race experience");
              assertReturned(helper, f);
              assertFirstPrize(helper,f,"race","chao_rattle");
            })
        .thenExecute(() -> cleanup(f))
        .thenSucceed();
  }

  @GameTest(template="activity_test",timeoutTicks=500)
  public static void freshEntityCounterContinuesLifetimeWinsWithoutReplayingPrizes(GameTestHelper helper){
    Fixture f=race(helper);
    for(var chao:List.of(f.first,f.second)){
      // Exactly the state/NBT split after a next-life egg: saved milestones,
      // fresh entity counter. The completed rebirth/hatch path is tested too.
      com.chaogarden.activities.ActivityRewards.recordVictory(chao.state(),"race",10);
      chao.setState(ChaoState.decode(chao.state().encode()));
      chao.getPersistentData().remove("RaceExperienceWins");
    }
    ActivityManager.open(f.host,f.start);
    ActivityManager.handleAction(f.host,f.start,"toggle",f.first.getUUID());
    ActivityManager.handleAction(f.guest,f.start,"toggle",f.second.getUUID());
    ActivityManager.handleAction(f.host,f.start,"start",null);
    helper.startSequence().thenWaitUntil(()->{
      helper.assertTrue(f.first.getPersistentData().getInt("RaceExperience")>0
          && f.second.getPersistentData().getInt("RaceExperience")>0,"Both real racers finish");
      assertReturned(helper,f);
      int sum=f.first.state().intTrait("contest.race.wins")+f.second.state().intTrait("contest.race.wins");
      helper.assertTrue(sum==21,"A fresh entity counter must continue lifetime wins from10 to11");
      var winner=f.first.getPersistentData().getInt("RaceExperienceWins")>0?f.first:f.second;
      helper.assertTrue(winner.getPersistentData().getInt("RaceExperienceWins")==11,"Legacy and saved lifetime journals must reconcile");
      helper.assertTrue(f.host.getInventory().countItem(ChaoGarden.item("chao_rattle"))
          +f.guest.getInventory().countItem(ChaoGarden.item("chao_rattle"))==0,"Earlier milestone prizes must not replay after rebirth");
    }).thenExecute(()->cleanup(f)).thenSucceed();
  }

  @GameTest(template = "activity_test", timeoutTicks = 500)
  public static void karateAwardsTrainingWithoutRealDamageOrPersistentArenaFlags(
      GameTestHelper helper) {
    floor(helper);
    Fixture f = keepers(helper, helper.absolutePos(MAT));
    makeMat(helper);
    f.host.teleportTo(
        helper.getLevel(), f.start.getX() - 1, f.start.getY(), f.start.getZ() + 2, 0, 0);
    f.guest.teleportTo(
        helper.getLevel(), f.start.getX() - 2, f.start.getY(), f.start.getZ() + 2, 0, 0);
    float firstHealth = f.first.getHealth(), secondHealth = f.second.getHealth();
    ActivityManager.open(f.host, f.start);
    ActivityManager.handleAction(f.host, f.start, "toggle", f.first.getUUID());
    ActivityManager.handleAction(f.guest, f.start, "toggle", f.second.getUUID());
    ActivityManager.handleAction(f.host, f.start, "start", null);
    helper.assertTrue(ActivityManager.isBusy(f.first), "Karate entrants were not reserved");
    helper
        .startSequence()
        .thenWaitUntil(
            () -> {
              helper.assertTrue(
                  f.first.getPersistentData().getInt("KarateExperience") > 0
                      && f.second.getPersistentData().getInt("KarateExperience") > 0,
                  "Both karate entrants must receive training from a completed nonlethal bout");
              assertReturned(helper, f);
              assertFirstPrize(helper,f,"karate","chao_bubble_wand");
              helper.assertTrue(
                  f.first.getHealth() == firstHealth && f.second.getHealth() == secondHealth,
                  "Karate virtual damage changed the Chao's real health");
            })
        .thenExecute(() -> cleanup(f))
        .thenSucceed();
  }

  @GameTest(template = "activity_test", timeoutTicks = 180)
  public static void courseGuardsAndBrokenArenaRollbackKeepOneBodyPerUuid(GameTestHelper helper) {
    Fixture f = race(helper);
    var data = GardenRegistry.get(helper.getLevel().getServer());
    helper.assertTrue(
        !ActivityManager.canConnect(f.host, f.start, f.start.offset(33, 0, 0), true),
        "A race-marker link exceeded two chunks");
    helper.assertTrue(
        !ActivityManager.connect(f.guest, f.start, helper.absolutePos(FLAG)),
        "Another keeper rewired the course");
    BlockPos extra = helper.absolutePos(new BlockPos(6, 1, 14));
    helper.getLevel().setBlock(extra, ActivityContent.FLAG.get().defaultBlockState(), 3);
    helper.assertTrue(
        ActivityManager.selectLink(f.host, extra) != null
            && ActivityManager.connect(f.host, extra, helper.absolutePos(FLAG)),
        "Could not prepare the cycle-guard fixture");
    helper.assertTrue(
        !ActivityManager.connect(f.host, helper.absolutePos(FLAG), extra),
        "Race flags accepted a cycle");
    ActivityManager.open(f.host, f.start);
    ActivityManager.handleAction(f.host, f.start, "toggle", f.first.getUUID());
    ActivityManager.handleAction(f.guest, f.start, "toggle", f.second.getUUID());
    ActivityManager.handleAction(f.host, f.start, "start", null);
    helper
        .startSequence()
        .thenWaitUntil(
            () ->
                helper.assertTrue(
                    data.recoveries.containsKey(f.first.getUUID())
                        && data.recoveries.containsKey(f.second.getUUID()),
                    "The server must persist return journals before teleporting entrants"))
        .thenExecute(
            () -> {
              CompoundTag saved = data.save(new CompoundTag(), helper.getLevel().registryAccess());
              GardenRegistry restored =
                  GardenRegistry.load(saved, helper.getLevel().registryAccess());
              helper.assertTrue(
                  restored.entries.get(f.first.getUUID()).owner.equals(f.host.getUUID())
                      && restored.entries.get(f.second.getUUID()).owner.equals(f.guest.getUUID())
                      && restored.links.equals(data.links)
                      && restored.recoveries.keySet().equals(data.recoveries.keySet()),
                  "Garden ownership, course links or pending return journals failed to round-trip");
              // Removing a real arena section invokes the production cancellation/return path.
              helper.setBlock(START.east(), Blocks.AIR);
            })
        .thenWaitUntil(
            () -> {
              assertReturned(helper, f);
              helper.assertTrue(
                  f.first.getPersistentData().getInt("RaceExperience") == 0
                      && f.second.getPersistentData().getInt("RaceExperience") == 0,
                  "An interrupted race granted a completion reward");
            })
        .thenExecute(() -> cleanup(f))
        .thenSucceed();
  }

  @GameTest(template="activity_test",timeoutTicks=80)
  public static void karateKnockoutCannotCounterattackAndLifetimeXpCannotWrap(GameTestHelper helper) {
    floor(helper);Fixture f=keepers(helper,helper.absolutePos(MAT));makeMat(helper);
    try {
      f.host.teleportTo(helper.getLevel(),f.start.getX()-1,f.start.getY(),f.start.getZ()+2,0,0);
      f.guest.teleportTo(helper.getLevel(),f.start.getX()-2,f.start.getY(),f.start.getZ()+2,0,0);
      for(var body:List.of(f.first,f.second)) {
        java.util.Arrays.fill(body.state().points,0);body.state().intelligence=0;
        body.getPersistentData().putInt("KarateExperience",Integer.MAX_VALUE-2);
      }
      float firstHealth=f.first.getHealth(),secondHealth=f.second.getHealth();
      ActivityManager.open(f.host,f.start);
      ActivityManager.handleAction(f.host,f.start,"toggle",f.first.getUUID());
      ActivityManager.handleAction(f.guest,f.start,"toggle",f.second.getUUID());
      ActivityManager.handleAction(f.host,f.start,"start",null);
      helper.assertTrue(ActivityManager.isBusy(f.first)&&ActivityManager.isBusy(f.second),"Real owned karate entrants were not reserved");
      var activeField=ActivityManager.class.getDeclaredField("ACTIVE");activeField.setAccessible(true);
      var active=(java.util.Map<?,?>)activeField.get(null);Object contest=active.get(f.first.getUUID());
      helper.assertTrue(contest!=null&&contest==active.get(f.second.getUUID()),"Entrants must share one real contest");
      // Public start reserves entrants; begin performs the real saved home
      // transaction and arena teleports before any controlled final-bout setup.
      var begin=ActivityManager.class.getDeclaredMethod("begin",contest.getClass());begin.setAccessible(true);begin.invoke(null,contest);
      var journal=GardenRegistry.get(helper.getLevel().getServer());
      helper.assertTrue(journal.recoveries.containsKey(f.first.getUUID())&&journal.recoveries.containsKey(f.second.getUUID()),"Actual contest begin did not commit both home journals");
      var runners=(java.util.Map<?,?>)activityField(contest,"runners");
      activityFieldSet(contest,"left",f.first.getUUID());activityFieldSet(contest,"right",f.second.getUUID());
      activityFieldSet(contest,"boutAt",helper.getLevel().getGameTime());
      ((java.util.ArrayDeque<?>)activityField(contest,"bracket")).clear();
      for(Object runner:runners.values()) {
        activityFieldSet(runner,"hp",1d);activityFieldSet(runner,"zeal",100d);activityFieldSet(runner,"attackAt",0L);
      }
      Vec3 middle=Vec3.atBottomCenterOf(f.start).add(3,.12,3);
      f.first.teleportTo(middle.x-.35,middle.y,middle.z);f.second.teleportTo(middle.x+.35,middle.y,middle.z);
      var tick=ActivityManager.class.getDeclaredMethod("tickKarate",contest.getClass());tick.setAccessible(true);tick.invoke(null,contest);
      helper.assertTrue(f.first.state().intTrait("contest.karate.wins")==1&&f.second.state().intTrait("contest.karate.wins")==0,"A knocked-out second fighter counterattacked and reversed the first fighter's win");
      helper.assertTrue(f.first.getPersistentData().getInt("KarateExperience")==Integer.MAX_VALUE&&f.second.getPersistentData().getInt("KarateExperience")==Integer.MAX_VALUE,"Real winner/loser XP wrapped instead of saturating");
      helper.assertTrue(f.first.getHealth()==firstHealth&&f.second.getHealth()==secondHealth,"Virtual knockout changed real creature health");
      assertReturned(helper,f);helper.succeed();
    } catch(ReflectiveOperationException failure) {throw new AssertionError("Could not configure the real final-bout regression",failure);}
    finally {cleanup(f);}
  }
  private static Object activityField(Object instance,String name) throws ReflectiveOperationException {
    var field=instance.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(instance);
  }
  private static void activityFieldSet(Object instance,String name,Object value) throws ReflectiveOperationException {
    var field=instance.getClass().getDeclaredField(name);field.setAccessible(true);field.set(instance,value);
  }

  private record Fixture(
      ServerPlayer host,
      ServerPlayer guest,
      ChaoEntity first,
      ChaoEntity second,
      Vec3 firstHome,
      Vec3 secondHome,
      BlockPos start) {}

  private static Fixture race(GameTestHelper helper) {
    floor(helper);
    for (BlockPos pos : List.of(START, START.east()))
      helper.setBlock(pos, ActivityContent.START.get());
    helper.setBlock(FLAG, ActivityContent.FLAG.get());
    for (BlockPos pos : List.of(FINISH, FINISH.east()))
      helper.setBlock(pos, ActivityContent.FINISH.get());
    Fixture f = keepers(helper, helper.absolutePos(START));
    helper.assertTrue(
        ActivityManager.selectLink(f.host, f.start) != null
            && ActivityManager.connect(f.host, f.start, helper.absolutePos(FLAG))
            && ActivityManager.selectLink(f.host, helper.absolutePos(FLAG)) != null
            && ActivityManager.connect(
                f.host, helper.absolutePos(FLAG), helper.absolutePos(FINISH)),
        "Could not build the linked race course");
    return f;
  }

  private static Fixture keepers(GameTestHelper helper, BlockPos control) {
    ServerPlayer host = TestConnections.makeServerPlayer(helper);
    ServerPlayer guest = TestConnections.makeServerPlayer(helper);
    host.teleportTo(
        helper.getLevel(), control.getX() + 2, control.getY(), control.getZ() + 1, 0, 0);
    guest.teleportTo(
        helper.getLevel(), control.getX() + 3, control.getY(), control.getZ() + 1, 0, 0);
    helper.setBlock(GARDEN_A, ActivityContent.GARDEN.get());
    helper.setBlock(GARDEN_B, ActivityContent.GARDEN.get());
    var data = GardenRegistry.get(helper.getLevel().getServer());
    var firstGarden = data.register(host, helper.absolutePos(GARDEN_A));
    var secondGarden = data.register(guest, helper.absolutePos(GARDEN_B));
    ChaoEntity first = chao(helper, host, GARDEN_A.offset(0, 0, 2), "Activity host Chao");
    ChaoEntity second = chao(helper, guest, GARDEN_B.offset(0, 0, 2), "Activity guest Chao");
    helper.assertTrue(
        data.bind(host, first, firstGarden) && data.bind(guest, second, secondGarden),
        "Each keeper must be able to assign their own Chao");
    helper.assertTrue(
        !data.bind(guest, first, secondGarden), "A keeper reassigned someone else's Chao");
    return new Fixture(host, guest, first, second, first.position(), second.position(), control);
  }

  private static ChaoEntity chao(
      GameTestHelper helper, ServerPlayer owner, BlockPos relative, String name) {
    ChaoEntity chao = ChaoGarden.CHAO.get().create(helper.getLevel());
    ChaoState state = ChaoState.egg(0, false, true, 0, new Random(name.hashCode()));
    state.adult = true;
    state.happiness = 80;
    state.hunger = state.energy = 100;
    state.points[ChaoState.RUN] = state.points[ChaoState.POWER] = 2200;
    chao.setState(state);
    chao.owner = owner.getUUID();
    chao.setCustomName(Component.literal(name));
    chao.setNoAi(true);
    chao.setInvulnerable(false);
    chao.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.45);
    Vec3 point = Vec3.atBottomCenterOf(helper.absolutePos(relative));
    chao.moveTo(point.x, point.y, point.z, 0, 0);
    helper.getLevel().addFreshEntity(chao);
    return chao;
  }

  private static void floor(GameTestHelper helper) {
    for (int x = 0; x < 24; x++)
      for (int z = 0; z < 24; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
  }

  private static void makeMat(GameTestHelper helper) {
    for (int x = 0; x < 7; x++)
      for (int z = 0; z < 7; z++) helper.setBlock(MAT.offset(x, 0, z), ActivityContent.MAT.get());
  }

  private static void assertReturned(GameTestHelper helper, Fixture f) {
    var data = GardenRegistry.get(helper.getLevel().getServer());
    for (ChaoEntity chao : List.of(f.first, f.second)) {
      Vec3 home = chao == f.first ? f.firstHome : f.secondHome;
      helper.assertTrue(
          !ActivityManager.isBusy(chao), "The finished activity kept a Chao reserved");
      helper.assertTrue(
          GardenRegistry.find(helper.getLevel().getServer(), chao.getUUID()) == chao,
          "An activity replaced the original UUID body");
      helper.assertTrue(
          chao.distanceToSqr(home) < 9, "A participant was not returned to its original garden");
      helper.assertTrue(
          chao.isNoAi() && !chao.isInvulnerable() && !chao.isStaying(),
          "Arena AI, invulnerability or stay flags were not restored");
      helper.assertTrue(
          !data.recoveries.containsKey(chao.getUUID())
              && !chao.getPersistentData().contains("ChaoActivityHome"),
          "A successful return left a recovery journal behind");
      var bodies =
          helper
              .getLevel()
              .getEntitiesOfClass(
                  ChaoEntity.class,
                  new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(40),
                  e -> e.getUUID().equals(chao.getUUID()));
      helper.assertTrue(bodies.size() == 1, "The activity produced duplicate UUID bodies");
    }
  }

  private static void assertFirstPrize(GameTestHelper helper,Fixture f,String mode,String item){
    int victories=f.first.state().intTrait("contest."+mode+".wins")+f.second.state().intTrait("contest."+mode+".wins");
    helper.assertTrue(victories==1,"One completed contest must record exactly one winner");
    int medals=com.chaogarden.activities.ActivityRewards.medals(f.first.state()).size()
        +com.chaogarden.activities.ActivityRewards.medals(f.second.state()).size();
    helper.assertTrue(medals==1,"First contest must award exactly one earned medal");
    int prizes=f.host.getInventory().countItem(ChaoGarden.item(item))+f.guest.getInventory().countItem(ChaoGarden.item(item));
    helper.assertTrue(prizes==1,"First contest must deliver exactly one physical prize to its keeper");
  }

  private static void cleanup(Fixture f) {
    var server = f.host.server;
    var data = GardenRegistry.get(server);
    for (ChaoEntity chao : List.of(f.first, f.second)) {
      data.entries.remove(chao.getUUID());
      chao.discard();
    }
    data.gardens
        .values()
        .removeIf(g -> g.owner().equals(f.host.getUUID()) || g.owner().equals(f.guest.getUUID()));
    data.setDirty();
    server.getPlayerList().remove(f.host);
    server.getPlayerList().remove(f.guest);
  }

  private ActivityGameTests() {}
}
