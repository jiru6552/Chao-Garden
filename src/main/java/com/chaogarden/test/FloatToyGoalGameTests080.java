package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoIllness;
import com.chaogarden.core.ChaoPreferences;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.FloatToyContent080;
import com.chaogarden.toys.FloatToyEntity080;
import com.chaogarden.toys.FloatToyGoal080;
import com.chaogarden.toys.FloatToyRoutes080;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real server GoalSelector, physics, passenger ownership and safe shore movement.
 * Numeric native tests do not claim rendered pose, sound or remote-client verification. */
@GameTestHolder("chao_float80") @PrefixGameTestTemplate(false)
public final class FloatToyGoalGameTests080 {
  private static final String TEMPLATE="chao_float80:activity_test";
  private static final List<String> KINDS=List.of("chao_rubber_duck","chao_lifesaver");
  private FloatToyGoalGameTests080(){}
  @GameTestGenerator public static Collection<TestFunction> cases(){
    var out=new ArrayList<TestFunction>();
    for(boolean swimmer:new boolean[]{false,true})add(out,"float_natural_saved_waterlogged_ring_"+swimmer,1800,h->LegacyFloatToyFixtures080.ring(h,swimmer));
    for(String kind:KINDS)for(boolean adult:new boolean[]{false,true})for(int depth:new int[]{1,4})
      add(out,"float_rescue_"+kind+"_"+adult+"_depth"+depth,1600,h->rescue(h,kind,adult,depth));
    for(String kind:KINDS)for(Direction to:Direction.Plane.HORIZONTAL)
      add(out,"float_land_launch_"+kind+"_"+to,2200,h->launch(h,kind,to));
    for(String kind:KINDS){
      add(out,"float_off_grid_land_centering_"+kind,2200,h->offGrid(h,kind));
      add(out,"float_land_turn_around_obstacle_"+kind,2600,h->turn(h,kind));
      add(out,"float_two_chao_one_owner_"+kind,1200,h->oneOwner(h,kind));
      add(out,"float_unsafe_shore_retry_"+kind,1400,h->shoreRetry(h,kind));
      add(out,"float_voluntary_deep_water_entry_"+kind,2200,h->voluntary(h,kind));
      add(out,"float_obstructed_water_no_remote_mount_"+kind,500,h->blockedWater(h,kind));
      add(out,"float_no_water_stays_grounded_"+kind,500,h->noWater(h,kind));
      add(out,"float_push_stay_interrupt_releases_"+kind,1600,h->interrupt(h,kind));
    }
    return out;
  }
  private static void add(List<TestFunction> out,String name,int timeout,java.util.function.Consumer<GameTestHelper> body){
    out.add(new TestFunction("float_goal080_"+(out.size()/4),name,TEMPLATE,timeout,0,true,body));
  }
  private static void cleanup(GameTestHelper h,Runnable task){h.testInfo.addListener(new GameTestListener(){
    public void testStructureLoaded(GameTestInfo i){}
    public void testPassed(GameTestInfo i,GameTestRunner r){task.run();}
    public void testFailed(GameTestInfo i,GameTestRunner r){task.run();}
    public void testAddedForRerun(GameTestInfo a,GameTestInfo b,GameTestRunner r){task.run();}
  });}
  /** Pool 4..14 inclusive with a two-block supported escape strip on every side.
   * The same surface/shore height is used for shallow and deep water. */
  private static BlockPos terrain(GameTestHelper h,int depth,boolean water){
    for(int x=1;x<=18;x++)for(int z=1;z<=18;z++){
      for(int y=0;y<=depth;y++)h.setBlock(new BlockPos(x,y,z),Blocks.STONE);
      for(int y=depth+1;y<=depth+5;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
      if(water&&x>=4&&x<=14&&z>=4&&z<=14)
        for(int y=1;y<=depth;y++)h.setBlock(new BlockPos(x,y,z),Blocks.WATER);
    }
    return h.absolutePos(new BlockPos(9,depth+1,9));
  }
  private static Vec3 point(BlockPos center,double x,double y,double z){return Vec3.atBottomCenterOf(center).add(x,y,z);}
  private static ChaoEntity actor(GameTestHelper h,Vec3 at,boolean adult,boolean swim,float alignment){
    var c=ChaoGarden.CHAO.get().create(h.getLevel());var s=new ChaoState();
    s.adult=adult;s.align=alignment;s.primaryType=adult?ChaoState.RUN_TYPE:ChaoState.NORMAL;
    s.hunger=90;s.energy=100;s.happiness=20;s.boredom=100;s.personality=ChaoPreferences.EASILY_BORED;
    s.points[ChaoState.RUN]=800;s.points[ChaoState.SWIM]=swim?150:0;
    s.expressedColor=adult?4:0;s.shiny=adult;s.setTrait("float080.keep","existing save history");
    s.setBaselineFace076(3,2);for(int i=0;i<ChaoIllness.COUNT;i++)s.setIllness(i,100);
    if(adult)s.setEvolutionStrength(1.2f);
    c.setState(ChaoState.decode(s.encode()));c.setInvulnerable(true);c.getRandom().setSeed(18);c.moveTo(at);
    c.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ChaoGarden.item("red_wool_beanie")));
    c.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(ChaoGarden.item("aviators_accessory")));
    h.getLevel().addFreshEntity(c);c.goalSelector.removeAllGoals(g->true);c.targetSelector.removeAllGoals(g->true);
    c.goalSelector.addGoal(0,new FloatToyGoal080(c));
    cleanup(h,c::discard);return c;
  }
  private static FloatToyEntity080 vessel(GameTestHelper h,String kind,Vec3 at){
    var f=FloatToyContent080.newCandidate(h.getLevel(),kind,new ItemStack(ChaoGarden.item(kind)),at,0);
    h.assertTrue(f!=null&&h.getLevel().addFreshEntity(f),"Physical float publishes once");f.commit();
    cleanup(h,()->{if(f.rider()!=null)f.rider().discard();f.discard();
      for(var item:h.getLevel().getEntitiesOfClass(ItemEntity.class,f.getBoundingBox().inflate(2),e->e.getItem().is(ChaoGarden.item(kind))))item.discard();});return f;
  }
  private static void intact(GameTestHelper h,ChaoEntity c,boolean adult,float alignment){
    h.assertTrue(c.isAlive()&&!c.state().dead&&c.isBaby()!=adult,"Interaction preserves life stage");
    h.assertTrue(!adult||c.state().lockedPrimaryType()==ChaoState.RUN_TYPE&&Math.abs(c.state().lockedAlignment()-alignment)<.001,
        "Adult first type and alignment remain locked");
    h.assertTrue(c.state().shiny==adult&&c.state().expressedColor==(adult?4:0)&&c.state().trait("float080.keep").equals("existing save history"),
        "Existing finish, color and saved history remain intact");
    h.assertTrue(c.getItemBySlot(EquipmentSlot.HEAD).is(ChaoGarden.item("red_wool_beanie"))
        &&c.getItemBySlot(EquipmentSlot.OFFHAND).is(ChaoGarden.item("aviators_accessory")),"Real accessories stay equipped");
    h.assertTrue(!c.isNoAi()&&!c.isNoGravity(),"No temporary AI/gravity flag leaks");
  }
  private static void continuous(GameTestHelper h,ChaoEntity c,FloatToyEntity080 f){
    var beforeC=new AtomicReference<>(c.position());var beforeF=new AtomicReference<>(f.position());UUID id=f.getUUID();
    h.onEachTick(()->{
      if(h.testInfo.isDone())return;
      if(h.getTick()%160==0){
        var route=!f.afloat()&&!c.isPassenger()?FloatToyRoutes080.launchRoute(h.getLevel(),f,c):null;
        com.mojang.logging.LogUtils.getLogger().info("FLOAT080_GOAL tick={} source={} route={} {}",h.getTick(),f.kind(),route==null?"none":route.points(),diagnostic(c,f));
      }
      h.assertTrue(f.getUUID().equals(id)&&!f.storedItem().isEmpty()&&f.storedItem().getCount()==1,"One persistent real toy payload and UUID");
      h.assertTrue(c.position().distanceTo(beforeC.get())<.7,"Actor moves continuously, no remote mount or teleport");
      h.assertTrue(f.position().distanceTo(beforeF.get())<.35,"Float moves physically at a bounded speed");
      beforeC.set(c.position());beforeF.set(f.position());
      h.assertTrue(f.getPassengers().size()<=1,"One physical seat at most");
      if(f.rider()!=null)h.assertTrue(f.claimedBy(f.rider()),"Actual passenger owns claim");
      if((f.phase()==FloatToyEntity080.PUSH||f.phase()==FloatToyEntity080.LAUNCH_REPOSITION)&&f.claimedBy(c)&&!c.isPassenger())
        h.assertTrue(c.maxUpStep()==0,"Only the actual claimed launch/push actor cannot climb its low float");
      h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,f.getBoundingBox().inflate(5),
          e->e.getItem().is(ChaoGarden.item(f.kind()))).isEmpty(),"A riding/pushing float does not duplicate a dropped toy");
    });
  }
  private static String diagnostic(ChaoEntity c,FloatToyEntity080 f){
    var goals=c.goalSelector.getAvailableGoals().stream().filter(g->g.getGoal() instanceof FloatToyGoal080)
        .map(g->"running="+g.isRunning()+","+((FloatToyGoal080)g.getGoal()).diagnostic080()).toList();
    var nav=c.getNavigation().getPath();
    return "actor="+c.position()+",v="+c.getDeltaMovement()+",water="+c.isInWater()+",ground="+c.onGround()
        +",float="+f.position()+",v="+f.getDeltaMovement()+",phase="+f.phase()+",progress="+f.transition()
        +",claim="+f.claimant()+",rider="+(f.rider()==null?"none":f.rider().getUUID())
        +",nav="+(nav==null?"none":nav.getNextNodeIndex()+"/"+nav.getNodeCount()+"/"+nav.canReach())
        +",wanted="+c.getMoveControl().getWantedX()+","+c.getMoveControl().getWantedY()+","+c.getMoveControl().getWantedZ()
        +",goals="+goals;
  }
  private static String failureDiagnostic(ChaoEntity c,FloatToyEntity080 f){
    var details=diagnostic(c,f);
    // Native GameTest failure books have a bounded page size. Keep the full
    // route in FLOAT080_GOAL logs, not in an oversized assertion book page.
    return details.length()<=700?details:details.substring(0,697)+"...";
  }
  private static void rescue(GameTestHelper h,String kind,boolean adult,int depth){
    var center=terrain(h,depth,true);var f=vessel(h,kind,point(center,1,-.05,0));
    float alignment=adult?(kind.equals("chao_rubber_duck")?1:-1):0;
    var c=actor(h,point(center,-3,depth==4?-3.8:-.35,0),adult,false,alignment);continuous(h,c,f);
    var boarded=new AtomicBoolean();
    h.onEachTick(()->{if(c.getVehicle()==f)boarded.set(true);});
    h.succeedWhen(()->{
      h.assertTrue(boarded.get(),"Untrained distressed Chao actually reached and boarded nearby float");
      h.assertTrue(!c.isPassenger()&&FloatToyRoutes080.safeFeet(h.getLevel(),c,c.position()),"Rescued Chao lands on supported dry footprint");
      intact(h,c,adult,alignment);
    });
  }
  private static void launch(GameTestHelper h,String kind,Direction to){
    var center=terrain(h,1,true);
    // Bank adjacent to the pool; Chao starts on the outside and must contact
    // the actual prop, push its whole footprint into water, then board locally.
    Vec3 origin=point(center,to.getStepX()*-6,0,to.getStepZ()*-6);
    var f=vessel(h,kind,origin);var c=actor(h,origin.add(-to.getStepX(),0,-to.getStepZ()),true,false,0);
    continuous(h,c,f);var pushed=new AtomicBoolean();var boarded=new AtomicBoolean();
    h.onEachTick(()->{if(f.phase()==FloatToyEntity080.PUSH)pushed.set(true);if(c.getVehicle()==f)boarded.set(true);});
    h.succeedWhen(()->{
      h.assertTrue(pushed.get()&&boarded.get(),"Ground toy physically launched and then ridden: "+failureDiagnostic(c,f));
      h.assertTrue(f.afloat()&&horizontal(f.position(),origin)>.9,"Whole float moved from original land position");
      intact(h,c,true,0);
    });
  }
  private static void offGrid(GameTestHelper h,String kind){
    var center=terrain(h,1,true);var origin=point(center,-6.3,0,.2);
    var f=vessel(h,kind,origin);var c=actor(h,origin.add(-1.3,0,0),true,false,0);continuous(h,c,f);
    h.succeedWhen(()->{
      h.assertTrue(c.getVehicle()==f&&f.afloat(),"Off-grid placement physically centers around actual box corners, launches and boards: "+failureDiagnostic(c,f));intact(h,c,true,0);
    });
  }
  private static void turn(GameTestHelper h,String kind){
    var center=terrain(h,1,true);
    for(int z=8;z<=10;z++)for(int y=2;y<=4;y++)h.setBlock(new BlockPos(3,y,z),Blocks.STONE);
    var origin=point(center,-7,0,0);var f=vessel(h,kind,origin);
    var c=actor(h,origin.add(-1,0,0),true,false,0);continuous(h,c,f);
    var northSouth=new AtomicBoolean();var eastWest=new AtomicBoolean();var repositioned=new AtomicBoolean();float originalStep=c.maxUpStep();var previous=new AtomicReference<>(f.position());
    h.onEachTick(()->{if(h.testInfo.isDone())return;
      if(f.phase()==FloatToyEntity080.LAUNCH_REPOSITION){repositioned.set(true);h.assertTrue(c.maxUpStep()==0,"Turn walking remains scoped to the actual grounded launch");}
      var delta=f.position().subtract(previous.get());
      if(Math.abs(delta.x)>.01)eastWest.set(true);if(Math.abs(delta.z)>.01)northSouth.set(true);previous.set(f.position());
    });
    h.succeedWhen(()->{
      h.assertTrue(c.getVehicle()==f&&northSouth.get()&&eastWest.get()&&repositioned.get(),"Blocked straight launch walks around and physically pushes two directions: "+failureDiagnostic(c,f));
      h.assertTrue(Math.abs(c.maxUpStep()-originalStep)<.00001,"Completed launch/water entry restores the ordinary step height");intact(h,c,true,0);
    });
  }
  private static void oneOwner(GameTestHelper h,String kind){
    var center=terrain(h,1,true);var f=vessel(h,kind,point(center,0,-.05,0));
    var a=actor(h,point(center,-2,-.35,0),false,false,0);var b=actor(h,point(center,2,-.35,0),true,false,-1);
    continuous(h,a,f);var first=new AtomicReference<UUID>();var firstTick=new AtomicLong(-1);
    h.onEachTick(()->{
      h.assertTrue(!(a.getVehicle()==f&&b.getVehicle()==f),"Competing Chao cannot share or steal one seat");
      if(f.rider()!=null){first.compareAndSet(null,f.rider().getUUID());firstTick.compareAndSet(-1,h.getTick());h.assertTrue(f.claimant().orElseThrow().equals(f.rider().getUUID()),"Server claimant matches exactly one passenger");}
    });
    h.succeedWhen(()->{h.assertTrue(first.get()!=null&&f.rider()!=null&&h.getTick()-firstTick.get()>=40,"A nearby competing rescue succeeds and keeps sole occupancy");intact(h,a,false,0);intact(h,b,true,-1);});
  }
  private static void shoreRetry(GameTestHelper h,String kind){
    var center=terrain(h,1,true);int top=2;
    for(int n=3;n<=15;n++)for(int y=top;y<=top+2;y++){
      h.setBlock(new BlockPos(3,y,n),Blocks.STONE);h.setBlock(new BlockPos(15,y,n),Blocks.STONE);
      h.setBlock(new BlockPos(n,y,3),Blocks.STONE);h.setBlock(new BlockPos(n,y,15),Blocks.STONE);
    }
    var f=vessel(h,kind,point(center,0,-.05,0));var c=actor(h,point(center,-.95,-.35,0),true,true,1);
    c.goalSelector.removeAllGoals(g->true);
    h.runAfterDelay(4,()->{h.assertTrue(f.claim(c)&&f.board(c),"Real nearby boarding succeeds");f.requestLand();});
    continuous(h,c,f);
    h.runAfterDelay(180,()->{
      h.assertTrue(c.getVehicle()==f,"Unsafe shores keep rider aboard instead of forced water dismount");
      for(int z=8;z<=10;z++)for(int y=top;y<=top+2;y++)h.setBlock(new BlockPos(15,y,z),Blocks.AIR);
    });
    h.succeedWhen(()->{
      h.assertTrue(h.getTick()>180&&!c.isPassenger()&&FloatToyRoutes080.safeFeet(h.getLevel(),c,c.position()),"Newly opened safe shore is retried and reached");intact(h,c,true,1);
    });
  }
  private static void voluntary(GameTestHelper h,String kind){
    var center=terrain(h,4,true);var f=vessel(h,kind,point(center,0,-.05,0));
    var c=actor(h,point(center,-6,0,0),true,true,1);continuous(h,c,f);
    var dry=c.position();var departed=new AtomicBoolean();var wet=new AtomicBoolean();var furthest=new AtomicReference<Double>(c.getX());
    h.onEachTick(()->{if(h.testInfo.isDone())return;
      if(c.isInWater())wet.set(true);
      if(!c.isInWater()&&c.getX()>dry.x+.31)departed.set(true);
      if(departed.get()&&!wet.get()){
        h.assertTrue(c.getX()>=furthest.get()-.08,"Committed shore departure does not reverse toward an already-reached dry anchor");
        furthest.set(Math.max(furthest.get(),c.getX()));
      }
    });
    h.succeedWhen(()->{h.assertTrue(departed.get()&&wet.get()&&c.getVehicle()==f,"Competent swimmer continues beyond dry arrival, enters real water and boards: "+failureDiagnostic(c,f));intact(h,c,true,1);});
  }
  private static void blockedWater(GameTestHelper h,String kind){
    var center=terrain(h,1,true);
    for(int z=4;z<=14;z++)for(int y=1;y<=4;y++)h.setBlock(new BlockPos(9,y,z),Blocks.STONE);
    var f=vessel(h,kind,point(center,2,-.05,0));var c=actor(h,point(center,-2,-.35,0),false,false,0);
    continuous(h,c,f);h.runAfterDelay(300,()->{
      h.assertTrue(!c.isPassenger()&&f.rider()==null&&f.claimant().isEmpty(),"Disconnected water never remote mounts or retains a false claim");intact(h,c,false,0);h.succeed();
    });
  }
  private static void noWater(GameTestHelper h,String kind){
    var center=terrain(h,1,false);var f=vessel(h,kind,point(center,0,0,0));var origin=f.position();
    var c=actor(h,point(center,1,0,0),true,true,-1);continuous(h,c,f);
    h.runAfterDelay(300,()->{
      h.assertTrue(!c.isPassenger()&&f.rider()==null&&f.claimant().isEmpty()&&horizontal(f.position(),origin)<.02,
          "No reachable water leaves toy safely grounded and unclaimed");intact(h,c,true,-1);h.succeed();
    });
  }
  private static void interrupt(GameTestHelper h,String kind){
    var center=terrain(h,1,true);var origin=point(center,-7,0,0);var f=vessel(h,kind,origin);
    var c=actor(h,origin.add(-1,0,0),true,false,0);continuous(h,c,f);float originalStep=c.maxUpStep();
    var interrupted=new AtomicBoolean();var stopped=new AtomicReference<Vec3>();
    h.onEachTick(()->{
      if(!interrupted.get()&&f.phase()==FloatToyEntity080.PUSH){c.setStaying(true);interrupted.set(true);}
      else if(interrupted.get()&&stopped.get()==null&&f.phase()!=FloatToyEntity080.PUSH)stopped.set(f.position());
    });
    h.succeedWhen(()->{
      h.assertTrue(interrupted.get()&&stopped.get()!=null&&f.claimant().isEmpty()&&!c.isPassenger(),"Stay interruption stops physical push and releases claim: "+failureDiagnostic(c,f));
      h.assertTrue(horizontal(f.position(),stopped.get())<.04,"Interrupted float stops rather than rolling unattended");
      h.assertTrue(Math.abs(c.maxUpStep()-originalStep)<.00001,"Interruption and lost claim restore the original step height without saved modifiers");intact(h,c,true,0);
    });
  }
  private static double horizontal(Vec3 a,Vec3 b){return a.subtract(b).horizontalDistance();}
}
