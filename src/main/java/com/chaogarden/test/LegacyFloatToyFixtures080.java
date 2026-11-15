package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoPreferences;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Compatibility fixtures for the historical public test names. They retain
 * real legacy block inputs and conservation/care checks while exercising the
 * replacement physical float; no fixed bob-frame, NoGravity or remote anchor. */
final class LegacyFloatToyFixtures080 {
  private LegacyFloatToyFixtures080(){}
  private record Fixture(ChaoEntity c,BlockPos source,String kind,boolean wet,
      AtomicReference<FloatToyEntity080> vessel,AtomicBoolean rode,AtomicLong rideAt){}
  private static void cleanup(GameTestHelper h,Runnable work){h.testInfo.addListener(new GameTestListener(){
    public void testStructureLoaded(GameTestInfo i){}public void testPassed(GameTestInfo i,GameTestRunner r){work.run();}
    public void testFailed(GameTestInfo i,GameTestRunner r){work.run();}public void testAddedForRerun(GameTestInfo a,GameTestInfo b,GameTestRunner r){work.run();}
  });}
  private static Fixture fixture(GameTestHelper h,String kind,boolean wet,boolean swimmer,boolean roof){
    for(int x=1;x<=20;x++)for(int z=1;z<=20;z++){
      h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
      for(int y=1;y<=5;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
    }
    var source=h.absolutePos(new BlockPos(9,wet?0:1,9));
    if(wet)h.getLevel().setBlock(source.below(),Blocks.STONE.defaultBlockState(),3);
    else for(int x=10;x<=14;x++)for(int z=7;z<=11;z++)h.setBlock(new BlockPos(x,0,z),Blocks.WATER);
    h.getLevel().setBlock(source,ToyContent.block(kind).defaultBlockState().setValue(ChaoToyBlock.WATERLOGGED,wet),3);
    if(roof)h.getLevel().setBlock(source.above(),Blocks.STONE.defaultBlockState(),3);
    var c=ChaoGarden.CHAO.get().create(h.getLevel());var state=new ChaoState();state.adult=true;
    state.points[ChaoState.RUN]=667;state.points[ChaoState.SWIM]=swimmer?667:0;
    state.happiness=20;state.hunger=90;state.energy=100;state.boredom=100;state.personality=ChaoPreferences.EASILY_BORED;
    state.setTrait("legacy.float080","retain existing care/genetics history");c.setState(ChaoState.decode(state.encode()));c.setInvulnerable(true);c.getRandom().setSeed(18);
    var start=wet?source.north(2).above():source.west(2);c.moveTo(Vec3.atBottomCenterOf(start));c.setOnGround(true);h.getLevel().addFreshEntity(c);
    // Original fixtures removed the new replacement goal. Preserve both real
    // flotation controllers so arbitration is tested rather than bypassed.
    c.goalSelector.removeAllGoals(g->!(g instanceof FloatGoal)&&!(g instanceof FloatToyGoal080));c.targetSelector.removeAllGoals(g->true);
    var f=new Fixture(c,source,kind,wet,new AtomicReference<>(),new AtomicBoolean(),new AtomicLong(-1));
    var last=new AtomicReference<>(c.position());
    cleanup(h,()->{c.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);var boat=f.vessel().get();if(boat!=null)boat.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);});
    h.onEachTick(()->{
      if(h.testInfo.isDone())return;
      if(h.getTick()%200==0)com.mojang.logging.LogUtils.getLogger().info("LEGACY_FLOAT080 tick={} {}",h.getTick(),diagnostic(h,f));
      h.assertTrue(c.position().distanceTo(last.get())<.7,"Legacy actor moves physically without remote anchoring");last.set(c.position());
      var floats=h.getLevel().getEntitiesOfClass(FloatToyEntity080.class,new AABB(source).inflate(8),e->e.committed()&&e.kind().equals(kind));
      h.assertTrue(floats.size()<=1,"One source becomes at most one real float");
      if(!floats.isEmpty()){
        var boat=floats.getFirst();if(f.vessel().get()==null)f.vessel().set(boat);
        h.assertTrue(f.vessel().get()==boat&&boat.storedItem().getCount()==1&&boat.storedItem().is(ChaoGarden.item(kind)),"Same float identity holds sole recoverable payload");
        h.assertTrue(!h.getLevel().getBlockState(source).is(ToyContent.block(kind)),"Conversion removes exactly the legacy source block");
        if(c.getVehicle()==boat){f.rode().set(true);if(boat.phase()==FloatToyEntity080.RIDE)f.rideAt().compareAndSet(-1,h.getTick());
          h.assertTrue(boat.rider()==c&&boat.claimedBy(c)&&boat.getPassengers().size()==1,"Exactly one actor owns actual passenger seat");
          h.assertTrue(c.getEyeY()>boat.getY()+.05,"Physical aid holds eyes above its water line");
          var goals=c.goalSelector.getAvailableGoals().stream().filter(g->g.getGoal() instanceof FloatGoal).toList();
          h.assertTrue(!goals.isEmpty()&&goals.stream().noneMatch(g->g.isRunning()||g.getGoal().canUse()),"Ordinary FloatGoal cannot compete with real supported passenger");
        }
      }
      h.assertTrue(!c.isNoGravity()&&!c.isNoAi()&&c.getHeldChaoItem().isEmpty()&&c.getMainHandItem().isEmpty(),"Controller creates no gravity/AI leak or inventory prop");
      h.assertTrue(c.state().trait("legacy.float080").equals("retain existing care/genetics history")&&c.state().adult,"Existing identity and unrelated save data survive");
      h.assertTrue(drops(h,source,kind)==0,"Migration and riding never publish a duplicate dropped item");
    });
    return f;
  }
  private static String diagnostic(GameTestHelper h,Fixture f){
    var c=f.c();var state=h.getLevel().getBlockState(f.source());
    var points=new java.util.ArrayList<String>();
    if(state.getBlock() instanceof ChaoToyBlock&&state.getValue(ChaoToyBlock.WATERLOGGED)){
      var point=Vec3.atBottomCenterOf(f.source()).add(0,.8,0);
      var probe=FloatToyContent080.newCandidate(h.getLevel(),f.kind(),new ItemStack(ChaoGarden.item(f.kind())),point,0);
      if(probe!=null){
        for(var direction:Direction.Plane.HORIZONTAL){
          var dry=FloatToyRoutes080.dryFeetAt(h.getLevel(),c,point.x+direction.getStepX()*.95,point.z+direction.getStepZ()*.95,point.y+.05);
          points.add(direction+"="+dry+",arcBefore="+(dry!=null&&FloatToyRoutes080.exitClear(h.getLevel(),c,dry,probe.boardPoint(c),probe))
              +",arcAfterConsume="+(dry!=null&&FloatToyRoutes080.legacyEntryClear(h.getLevel(),c,dry,probe.boardPoint(c),probe,f.source())));
        }
        points.add("legacySeat="+probe.boardPoint(c));probe.discardUncommitted();
      }
    }
    var goals=c.goalSelector.getAvailableGoals().stream().filter(g->g.getGoal() instanceof FloatToyGoal080)
        .map(g->"running="+g.isRunning()+","+((FloatToyGoal080)g.getGoal()).diagnostic080()).toList();
    return "actor="+c.position()+",water="+c.isInWater()+",ground="+c.onGround()+",source="+state
        +",shape="+state.getCollisionShape(h.getLevel(),f.source()).toAabbs()+",surface="+FloatToyRoutes080.surfaceAt(h.getLevel(),Vec3.atBottomCenterOf(f.source()))
        +",legacyAvailable="+FloatToyContent080.legacyAvailable(h.getLevel(),f.source(),c)+",vessel="+(f.vessel().get()==null?"none":f.vessel().get().position())
        +",entryProbes="+points+",goals="+goals;
  }
  private static int drops(GameTestHelper h,BlockPos source,String kind){return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(source).inflate(8),e->e.getItem().is(ChaoGarden.item(kind))).stream().mapToInt(e->e.getItem().getCount()).sum();}
  private static void landed(GameTestHelper h,Fixture f,int happiness){
    var boat=f.vessel().get();h.assertTrue(f.rode().get()&&boat!=null,"Real legacy source migrated and was actually ridden");
    h.assertTrue(!f.c().isPassenger()&&FloatToyRoutes080.safeFeet(h.getLevel(),f.c(),f.c().position()),"Physical dismount reaches supported dry footprint");
    h.assertTrue(!f.c().isNoGravity()&&!f.c().isNoAi()&&f.c().toyTarget()==null&&f.c().toyPlayingTicks()==0,"Temporary ownership/flags clear after physical exit");
    h.assertTrue(f.c().state().happiness==happiness,"Natural completion pays exactly once; interruption cannot reward");
    h.assertTrue(boat.committed()&&!boat.isRemoved()&&boat.rider()==null&&boat.claimant().isEmpty()&&boat.storedItem().getCount()==1&&drops(h,f.source(),f.kind())==0,"One physical recoverable toy remains after exit");
  }
  static void ring(GameTestHelper h,boolean swimmer){var f=fixture(h,"chao_lifesaver",true,swimmer,false);h.succeedWhen(()->{
    landed(h,f,24);h.assertTrue(h.getLevel().getFluidState(f.source()).is(net.minecraft.tags.FluidTags.WATER),"Legacy conversion retains its original water");
  });}
  static void duck(GameTestHelper h){var f=fixture(h,"chao_rubber_duck",false,false,false);h.succeedWhen(()->{
    landed(h,f,24);h.assertTrue(f.vessel().get().afloat(),"Ground duck physically reaches water before natural ride");
  });}
  static void drain(GameTestHelper h,String kind){
    var f=fixture(h,kind,true,false,false);var drained=new AtomicBoolean();var drainAt=new AtomicLong(-1);
    h.onEachTick(()->{
      if(h.testInfo.isDone()||drained.get()||f.rideAt().get()<0||h.getTick()-f.rideAt().get()<40)return;
      drained.set(true);drainAt.set(h.getTick());
      h.assertTrue(f.c().state().happiness==20,"No premature reward before actual160-tick natural completion");
      // The old waterlogged source has already become a real entity. Remove
      // real support water; do not manually request land and mask its reaction.
      h.getLevel().setBlock(f.source(),Blocks.AIR.defaultBlockState(),3);
    });
    h.succeedWhen(()->{
      h.assertTrue(drained.get()&&h.getTick()-drainAt.get()>=20,"Drain permits the full physical entry/exit arc");landed(h,f,20);
      h.assertTrue(!h.getLevel().getFluidState(f.source()).is(net.minecraft.tags.FluidTags.WATER),"Drain changes actual fluid rather than an obsolete block state");
    });
  }
  static void lowRoof(GameTestHelper h){
    var f=fixture(h,"chao_lifesaver",true,false,true);
    h.runAfterDelay(450,()->{
      h.assertTrue(!f.rode().get()&&!f.c().isPassenger()&&f.c().state().happiness==20,"Real low headroom prevents unsupported local entry or reward");
      h.assertTrue(f.vessel().get()==null&&h.getLevel().getBlockState(f.source()).is(ToyContent.block(f.kind()))&&drops(h,f.source(),f.kind())==0,"Rejected migration keeps exact real legacy source, no copies");h.succeed();
    });
  }
  static void newRoof(GameTestHelper h){
    var f=fixture(h,"chao_lifesaver",true,false,false);var changed=new AtomicBoolean();var reopened=new AtomicBoolean();var changedAt=new AtomicLong(-1);
    h.onEachTick(()->{
      if(h.testInfo.isDone()||changed.get()||f.rideAt().get()<0||h.getTick()-f.rideAt().get()<40)return;
      changed.set(true);changedAt.set(h.getTick());
      // Block future shore headroom while leaving the current actor/float
      // clear. Forcing a solid block through the actor would not be an honest
      // no-clipping test and cannot require an unvalidated teleport to escape.
      for(var d:Direction.Plane.HORIZONTAL)h.getLevel().setBlock(f.source().relative(d).above(2),Blocks.STONE.defaultBlockState(),3);
      f.vessel().get().requestLand();
      h.runAfterDelay(40,()->{
        h.assertTrue(f.c().getVehicle()==f.vessel().get()&&h.getLevel().noCollision(f.c(),f.c().getBoundingBox())&&f.c().state().happiness==20,"New unsafe shore headroom retains a clear buoyant rider, no interruption reward");
        for(var d:Direction.Plane.HORIZONTAL)h.getLevel().setBlock(f.source().relative(d).above(2),Blocks.AIR.defaultBlockState(),3);reopened.set(true);
      });
    });
    h.succeedWhen(()->{h.assertTrue(changed.get()&&reopened.get()&&h.getTick()>changedAt.get()+40,"Observe blocked landing and reopened safe route");landed(h,f,20);});
  }
  static void placed(GameTestHelper h,Player keeper,BlockPos ground,String kind){
    var at=ground.above();var boats=h.getLevel().getEntitiesOfClass(FloatToyEntity080.class,new AABB(at).inflate(.1),e->e.committed()&&e.kind().equals(kind));
    h.assertTrue(boats.size()==1&&!h.getLevel().getBlockState(at).is(ToyContent.block(kind)),"Existing item ID places one physical boat-style toy");var boat=boats.getFirst();
    h.assertTrue(boat.storedItem().getCount()==1&&ItemStack.isSameItemSameComponents(boat.storedItem(),keeper.getMainHandItem())&&boat.getYRot()==keeper.getYRot(),"Exact source item/components and intended yaw retained");
    h.assertTrue(keeper.getMainHandItem().getCount()==1&&boat.rider()==null&&boat.claimant().isEmpty(),"Placement consumes exactly one item without forcing play");
    boat.remove(Entity.RemovalReason.KILLED);h.assertTrue(boat.isRemoved()&&drops(h,at,kind)==1,"Breaking returns exactly one physical toy");
    boat.remove(Entity.RemovalReason.KILLED);h.assertTrue(drops(h,at,kind)==1,"Repeated physical removal cannot duplicate its payload");
  }
}
