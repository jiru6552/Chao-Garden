package com.chaogarden.toys;

import com.chaogarden.activities.ActivityManager;
import com.chaogarden.companion.CompanionContent;
import com.chaogarden.companion.CarryPreview;
import com.chaogarden.core.ChaoPreferences;
import com.chaogarden.core.LocomotionRules;
import com.chaogarden.entity.ChaoEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/** Bounded, interruptible physical approaches for both appearances of the same float.
 * Selection never moves or claims a prop. Only the actual active goal may claim,
 * push at local contact, swim a connected route, or mount within the entry arc.
 * No Chao care/stat, item, NoAI or gravity data is changed by this goal. */
public final class FloatToyGoal080 extends Goal {
  private static final double SEARCH=8,CONTACT=.14,ARRIVED=.11;
  private static final int MAX_CANDIDATES=4,MAX_APPROACH_TICKS=1200;
  private final ChaoEntity chao;
  private FloatToyEntity080 vessel;
  private FloatToyRoutes080.Plan launch;
  private Entry entry;
  private int launchIndex,swimIndex,walkIndex,age,stalled;
  private long nextCheck,nextLegacy,nextNavigation;
  private boolean failed,rescue,shoreDeparted;
  private Vec3 lastActor,lastVessel,walkTarget;
  private List<Vec3> turnWalk=List.of();
  private BlockPos legacy;
  private Vec3 legacyStand;

  /** Dry is the supported approach footpoint; wet is entered physically, never teleported.
   * Null dry means the actor is already swimming; empty swim permits local dry boarding. */
  private record Entry(Vec3 dry,Vec3 wet,List<Vec3> swim){
    private Entry {swim=List.copyOf(swim);}
  }

  public FloatToyGoal080(ChaoEntity chao){this.chao=chao;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP));}
  private ServerLevel level(){return (ServerLevel)chao.level();}
  private boolean distress(){return chao.isInWater()&&!LocomotionRules.canSwim(chao.state());}
  private boolean eligible(){
    if(!(chao.level() instanceof ServerLevel)||!chao.isAlive()||chao.state().dead||chao.isNoAi()
        ||chao.isCocooned()||chao.isEating()||chao.isHandlingItem()||chao.isBeingPetted()
        ||chao.hurtTime>0||chao.isVehicle()||ActivityManager.isBusy(chao)||CompanionContent.isManaged(chao)
        ||ToyPreview.active(chao)||CarryPreview.active(chao)
        ||chao.getPersistentData().getBoolean("ChaoToyDismountWait")
        ||chao.state().intTrait("content.construction")>0)return false;
    if(chao.isPassenger())return chao.getVehicle() instanceof FloatToyEntity080;
    if(chao.isResting()||chao.state().sleeping||chao.toyPlayingTicks()>0)return false;
    if(chao.isBehaving()&&chao.getBehavior()!=ChaoEntity.Behavior.IDLE)return false;
    return distress()||!chao.isStaying();
  }
  @Override public boolean canUse(){
    if(!eligible())return false;
    if(chao.getVehicle() instanceof FloatToyEntity080 riding){vessel=riding;launch=null;entry=null;legacy=null;return true;}
    long now=level().getGameTime();if(now<nextCheck)return false;
    rescue=distress();nextCheck=now+(rescue?20:80+chao.getRandom().nextInt(81));
    if(!rescue){
      if(chao.state().hunger<35||chao.state().energy<30||chao.state().isIll()
          ||!chao.getHeldChaoItem().isEmpty()||!chao.getMainHandItem().isEmpty())return false;
      int interest=ChaoPreferences.playChanceDenominator(chao.state());
      if(interest<=0||chao.getRandom().nextInt(interest)!=0)return false;
      // Untrained Chao may launch a grounded float, but do not deliberately enter open water.
      if(chao.isInWater()&&!LocomotionRules.canSwim(chao.state()))return false;
    }
    vessel=null;launch=null;entry=null;legacy=null;legacyStand=null;
    var candidates=new ArrayList<>(FloatToyContent080.nearby(chao,SEARCH));
    candidates.sort(Comparator.comparingDouble(chao::distanceToSqr));
    int count=0;
    for(var candidate:candidates){
      if(count++>=MAX_CANDIDATES)break;
      if(!available(candidate))continue;
      if(select(candidate))return true;
    }
    // Old saves keep their old block/item IDs. A sparse, loaded local scan may
    // nominate a legacy block; land sources convert only at the actual approach.
    if(now>=nextLegacy){nextLegacy=now+200+Math.floorMod(chao.getUUID().hashCode(),61);vessel=null;return selectLegacy();}
    return false;
  }
  private boolean available(FloatToyEntity080 f){
    return f.isAlive()&&!f.isRemoved()&&f.committed()&&f.rider()==null
        &&(f.claimant().isEmpty()||f.claimedBy(chao))&&Math.abs(f.getY()-chao.getY())<=4;
  }
  private boolean select(FloatToyEntity080 candidate){
    vessel=candidate;launch=null;entry=null;
    if(candidate.afloat()){
      var approach=waterEntry(candidate);
      if(approach==null)return false;
      vessel=candidate;entry=approach;launch=null;return true;
    }
    if(rescue||chao.isInWater())return false;
    var route=FloatToyRoutes080.launchRoute(level(),candidate,chao);
    if(route==null||route.points().size()<2)return false;
    var delta=route.points().get(1).subtract(candidate.position());
    var pusher=pushFeet(candidate,delta);
    if(pusher==null||approachWalk(candidate,pusher).isEmpty())return false;
    vessel=candidate;launch=route;entry=null;return true;
  }
  private boolean selectLegacy(){
    var origin=chao.blockPosition();var nearest=new ArrayList<BlockPos>();
    for(var at:BlockPos.betweenClosed(origin.offset(-6,-2,-6),origin.offset(6,2,6)))
      if(FloatToyContent080.legacyAvailable(level(),at,chao))nearest.add(at.immutable());
    nearest.sort(Comparator.comparingDouble(p->chao.distanceToSqr(Vec3.atBottomCenterOf(p))));
    int tested=0;
    for(var at:nearest){
      if(tested++>=MAX_CANDIDATES)break;
      var state=level().getBlockState(at);var toy=(ChaoToyBlock)state.getBlock();
      var point=Vec3.atBottomCenterOf(at).add(0,state.getValue(ChaoToyBlock.WATERLOGGED)?.8:0,0);
      if(state.getValue(ChaoToyBlock.WATERLOGGED)){
        var probe=FloatToyContent080.newCandidate(level(),toy.toy,
            new net.minecraft.world.item.ItemStack(com.chaogarden.ChaoGarden.item(toy.toy)),point,
            state.getValue(ChaoToyBlock.FACING).toYRot());
        if(probe==null)continue;var approach=waterEntry(probe,at);probe.discardUncommitted();
        if(approach!=null){legacy=at;legacyStand=null;vessel=null;return true;}
      }else if(!rescue&&!chao.isInWater()&&nearbyWater(at)){
        // The old source is still a real colliding block. Do not pretend it is
        // absent during route proof. Approach a real dry neighbor first; only
        // then migrate that exact item to a stationary physical float and plan
        // its full push route. A failed plan leaves one safe, recoverable toy.
        for(var d:Direction.Plane.HORIZONTAL){
          var stand=FloatToyRoutes080.dryFeetAt(level(),chao,point.x+d.getStepX(),point.z+d.getStepZ(),point.y);
          if(stand!=null&&reachable(stand)){legacy=at;legacyStand=stand;vessel=null;return true;}
        }
      }
    }
    return false;
  }
  private boolean nearbyWater(BlockPos center){
    for(int dx=-6;dx<=6;dx++)for(int dz=-6;dz<=6;dz++){
      var probe=Vec3.atBottomCenterOf(center.offset(dx,0,dz));
      var surface=FloatToyRoutes080.surfaceAt(level(),probe);
      if(surface!=null&&Math.abs(surface-center.getY())<=.6)return true;
    }
    return false;
  }
  private boolean convertLegacy(){
    var converted=FloatToyContent080.convertLegacy(level(),legacy,chao);legacy=null;legacyStand=null;
    if(converted==null||!select(converted)||!converted.claim(chao)){failed=true;return false;}
    launchIndex=1;swimIndex=1;turnWalk=List.of();walkTarget=null;
    lastActor=chao.position();lastVessel=converted.position();return true;
  }
  @Override public void start(){
    failed=false;shoreDeparted=false;age=stalled=0;launchIndex=1;swimIndex=1;walkIndex=0;
    turnWalk=List.of();walkTarget=null;nextNavigation=0;
    if(legacy!=null&&legacyStand==null&&!convertLegacy())return;
    if(legacy!=null){chao.getNavigation().stop();lastActor=chao.position();return;}
    if(vessel==null||!vessel.claim(chao)){failed=true;return;}
    if(chao.isBehaving())chao.clearBehavior();
    chao.getNavigation().stop();lastActor=chao.position();lastVessel=vessel.position();
  }
  @Override public boolean canContinueToUse(){
    if(!failed&&eligible()&&legacy!=null)return age<MAX_APPROACH_TICKS
        &&FloatToyContent080.legacyAvailable(level(),legacy,chao);
    return !failed&&eligible()&&vessel!=null&&!vessel.isRemoved()&&vessel.level()==chao.level()
        &&(vessel.rider()==chao||vessel.claimedBy(chao))
        &&(vessel.rider()==chao||age<MAX_APPROACH_TICKS&&chao.distanceToSqr(vessel)<256);
  }
  @Override public boolean requiresUpdateEveryTick(){return true;}
  @Override public void tick(){
    if(failed)return;
    age++;
    if(legacy!=null){
      if(horizontal(chao.position(),legacyStand)>.14){if(!walk(legacyStand))failed=true;return;}
      if(!convertLegacy())return;
    }
    if(vessel==null){failed=true;return;}
    if(vessel.rider()==chao){chao.getNavigation().stop();return;}
    if(!vessel.claim(chao)){failed=true;return;}
    chao.getLookControl().setLookAt(vessel,30,30);
    if(lastActor==null||chao.position().distanceToSqr(lastActor)>.0016||vessel.position().distanceToSqr(lastVessel)>.0016){
      lastActor=chao.position();lastVessel=vessel.position();stalled=0;
    }else if(++stalled>160){failed=true;return;}
    if(launch!=null){tickLaunch();return;}
    if(!vessel.afloat()){failed=true;return;}
    tickEntry();
  }
  private void tickLaunch(){
    // Finish the entire authored launch, even once its center first touches water.
    // Otherwise a half-grounded prop could mount before clearing the bank.
    while(launchIndex<launch.points().size()&&horizontal(vessel.position(),launch.points().get(launchIndex))<.008){
      launchIndex++;turnWalk=List.of();walkTarget=null;
    }
    if(launchIndex>=launch.points().size()){
      vessel.stopPush();launch=null;
      if(!vessel.afloat()){failed=true;return;}
      setEntry(waterEntry(vessel));if(entry==null)failed=true;return;
    }
    var to=launch.points().get(launchIndex);var delta=to.subtract(vessel.position());
    var desired=pushFeet(vessel,delta);if(desired==null){vessel.stopPush();failed=true;return;}
    double distance=horizontal(chao.position(),desired);
    if(distance<=CONTACT&&Math.abs(chao.getY()-desired.y)<.20
        &&FloatToyRoutes080.safeFeet(level(),chao,chao.position())){
      // A real nearby pusher is required on every tick, not merely at goal start.
      chao.getNavigation().stop();turnWalk=List.of();walkTarget=null;
      chao.getMoveControl().setWantedPosition(desired.x,desired.y,desired.z,.65);
      vessel.pushing(chao,to);return;
    }
    vessel.pauseLaunch(chao);
    if(turnWalk.isEmpty()||walkTarget==null||walkTarget.distanceToSqr(desired)>.0121){
      turnWalk=approachWalk(vessel,desired);walkIndex=0;walkTarget=desired;
      if(turnWalk.isEmpty()){failed=true;return;}
    }
    if(!walk(turnWalk.get(walkIndex))){failed=true;return;}
    if(horizontal(chao.position(),turnWalk.get(walkIndex))<ARRIVED&&walkIndex+1<turnWalk.size())walkIndex++;
  }
  private Vec3 pushFeet(FloatToyEntity080 f,Vec3 delta){
    if(delta.horizontalDistanceSqr()<.000001)return null;
    var direction=new Vec3(delta.x,0,delta.z).normalize();
    // Rectangular collision boxes touch at a larger center distance along a
    // diagonal. Keep the same small physical edge gap while centering an
    // off-grid placed prop; a fixed radial .81 would overlap both box corners.
    double distance=FloatToyRoutes080.PUSH_DISTANCE/Math.max(Math.abs(direction.x),Math.abs(direction.z));
    return FloatToyRoutes080.dryFeetAt(level(),chao,f.getX()-direction.x*distance,
        f.getZ()-direction.z*distance,f.getY());
  }
  private void tickEntry(){
    if(entry==null){setEntry(waterEntry(vessel));if(entry==null){failed=true;return;}}
    if(chao.distanceToSqr(vessel)<=1.21&&vessel.board(chao)){chao.getNavigation().stop();return;}
    if(!chao.isInWater()){
      if(entry.dry()==null){failed=true;return;}
      if(entry.wet()==null){
        // Direct dry boarding still approaches its original precise shore point;
        // it never borrows the competent swimmer's committed departure state.
        if(horizontal(chao.position(),entry.dry())>.16){if(!walk(entry.dry()))failed=true;return;}
        failed=true;return;
      }
      if(!LocomotionRules.canSwim(chao.state())){failed=true;return;}
      if(!shoreDeparted){
        // Native ground navigation can finish within half the actor width.
        // Commit only after proving the complete real short shore departure.
        double arrival=Math.max(.16,chao.getBbWidth()*.5+.025);
        if(horizontal(chao.position(),entry.dry())>arrival){if(!walk(entry.dry()))failed=true;return;}
        if(!FloatToyRoutes080.swimStepClear(level(),chao,chao.position(),entry.wet())){failed=true;return;}
        shoreDeparted=true;
      }
      // Once committed, continue forward instead of repeatedly walking back to
      // the dry anchor as soon as its arrival radius is left. External movement
      // cannot turn this local entry into a distant, unproved approach.
      if(chao.position().distanceToSqr(entry.wet())>2.25){failed=true;return;}
      if(horizontal(chao.position(),entry.wet())>.06){paddle(entry.wet(),.045);return;}
      if(entry.swim().size()<2){failed=true;return;}
      var submerged=entry.swim().get(1);
      // The footprint is now wholly inside the validated wet cell. Descend to
      // its already-proved submerged route point; holding bank height forever
      // would leave the actor above the water flag and unable to start swimming.
      if(horizontal(submerged,entry.wet())>.06||submerged.y>=entry.wet().y){failed=true;return;}
      paddle(new Vec3(entry.wet().x,submerged.y,entry.wet().z),.045);return;
    }
    chao.getNavigation().stop();
    if(entry.swim().isEmpty()){setEntry(waterEntry(vessel));if(entry==null){failed=true;return;}}
    while(swimIndex<entry.swim().size()&&horizontal(chao.position(),entry.swim().get(swimIndex))<.12
        &&Math.abs(chao.getY()-entry.swim().get(swimIndex).y)<.18)swimIndex++;
    if(swimIndex>=entry.swim().size()){
      // A moving/obstructed target is replanned locally, never snapped into a seat.
      setEntry(waterEntry(vessel));if(entry==null)failed=true;return;
    }
    var to=entry.swim().get(swimIndex);
    var delta=to.subtract(chao.position());
    double speed=LocomotionRules.canSwim(chao.state())?.05:.025;
    paddle(to,speed);
  }
  private void setEntry(Entry value){entry=value;swimIndex=1;shoreDeparted=false;}
  private void paddle(Vec3 to,double speed){
    chao.getNavigation().stop();var delta=to.subtract(chao.position());
    var step=delta.horizontalDistanceSqr()<.000001?Vec3.ZERO:new Vec3(delta.x,0,delta.z).normalize().scale(Math.min(speed,delta.horizontalDistance()));
    double rise=chao.getY()<to.y-.08?.055:Math.max(-.025,Math.min(.025,delta.y*.2));
    var next=chao.position().add(step.x,rise,step.z);
    if(!FloatToyRoutes080.actorStepClear(level(),chao,chao.position(),next)){failed=true;return;}
    chao.setDeltaMovement(step.x,rise,step.z);chao.getLookControl().setLookAt(to.x,to.y+.3,to.z,30,30);
  }
  private Entry waterEntry(FloatToyEntity080 f){return waterEntry(f,null);}
  private Entry waterEntry(FloatToyEntity080 f,BlockPos consumedLegacy){
    if(chao.isInWater()){
      var directions=new ArrayList<Direction>();for(var d:Direction.Plane.HORIZONTAL)directions.add(d);
      directions.sort(Comparator.comparingDouble(d->chao.distanceToSqr(f.position().add(d.getStepX()*.95,0,d.getStepZ()*.95))));
      for(var d:directions){
        var target=f.position().add(d.getStepX()*.95,0,d.getStepZ()*.95);
        var route=FloatToyRoutes080.swimRoute(level(),chao,target);
        if(!route.isEmpty())return new Entry(null,null,route);
      }
      return null;
    }
    // Prefer direct shore boarding. It has no deliberate exposure to water.
    for(var d:Direction.Plane.HORIZONTAL){
      var dry=FloatToyRoutes080.dryFeetAt(level(),chao,f.getX()+d.getStepX()*.95,f.getZ()+d.getStepZ()*.95,f.getY()+.05);
      if(dry!=null&&dry.distanceToSqr(f.position())<=1.21&&reachable(dry)
          &&(consumedLegacy==null?FloatToyRoutes080.exitClear(level(),chao,dry,f.boardPoint(chao),f)
              :FloatToyRoutes080.legacyEntryClear(level(),chao,dry,f.boardPoint(chao),f,consumedLegacy)))return new Entry(dry,null,List.of());
    }
    if(!LocomotionRules.canSwim(chao.state()))return null;
    return deepWaterEntry(f);
  }
  private Entry deepWaterEntry(FloatToyEntity080 f){
    // Small loaded shore search. Proof includes an actual dry approach, a clear
    // physical water entry, and a connected wet route to the real vessel.
    var candidates=new ArrayList<Vec3[]>();var center=chao.blockPosition();
    for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++){
      var probe=new Vec3(center.getX()+dx+.5,chao.getY(),center.getZ()+dz+.5);
      Double top=FloatToyRoutes080.surfaceAt(level(),probe);if(top==null||Math.abs(top-chao.getY())>.6)continue;
      for(var d:Direction.Plane.HORIZONTAL){
        var dry=FloatToyRoutes080.dryFeetAt(level(),chao,probe.x+d.getStepX(),probe.z+d.getStepZ(),top);
        if(dry==null)continue;
        // Even source water can sit below the adjacent collision top. Keep the
        // departure level until the complete actor footprint clears the bank;
        // the connected swim route then descends inside its own wet cell.
        var wet=new Vec3(probe.x,Math.max(dry.y,top+.02),probe.z);
        if(FloatToyRoutes080.swimStepClear(level(),chao,dry,wet))candidates.add(new Vec3[]{dry,wet});
      }
    }
    candidates.sort(Comparator.comparingDouble(pair->chao.distanceToSqr(pair[0])));int attempts=0;
    for(var pair:candidates){
      if(attempts++>=8)break;if(!reachable(pair[0]))continue;
      for(var d:Direction.Plane.HORIZONTAL){
        var target=f.position().add(d.getStepX()*.95,0,d.getStepZ()*.95);
        var route=FloatToyRoutes080.swimRouteFrom(level(),chao,pair[1],target);
        if(!route.isEmpty())return new Entry(pair[0],pair[1],route);
      }
    }
    return null;
  }
  private boolean reachable(Vec3 at){var path=chao.getNavigation().createPath(BlockPos.containing(at),0);return path!=null&&path.canReach()&&pathAvoidsVessel(path);}
  private boolean pathAvoidsVessel(Path path){
    if(vessel==null)return true;
    Vec3 from=chao.position();
    for(int i=0;i<path.getNodeCount();i++){
      var node=path.getNode(i);var to=new Vec3(node.x+.5,node.y,node.z+.5);
      int steps=Math.max(1,(int)Math.ceil(from.distanceTo(to)/.10));if(steps>160)return false;
      for(int step=0;step<=steps;step++){
        var at=from.lerp(to,step/(double)steps);
        if(chao.getBoundingBox().move(at.subtract(chao.position())).intersects(vessel.getBoundingBox().inflate(.01)))return false;
      }
      from=to;
    }
    return true;
  }
  private boolean walk(Vec3 at){
    if(!FloatToyRoutes080.safeFeet(level(),chao,at))return false;
    if(horizontal(chao.position(),at)<1.25&&walkClear(chao.position(),at)){
      if(!walkClear(chao.position(),at))return false;
      chao.getNavigation().stop();chao.getMoveControl().setWantedPosition(at.x,at.y,at.z,.65);return true;
    }
    if(level().getGameTime()>=nextNavigation||chao.getNavigation().isDone()){
      nextNavigation=level().getGameTime()+12;
      var path=chao.getNavigation().createPath(BlockPos.containing(at),0);
      if(path==null||!path.canReach()||!pathAvoidsVessel(path))return false;
      chao.getNavigation().moveTo(path,.85);
    }
    return true;
  }
  /** Walk around the actual prop for a change of pushing direction. Ground paths
   * alone ignore entity obstacles, so near turns are explicit supported arcs. */
  private List<Vec3> approachWalk(FloatToyEntity080 f,Vec3 desired){
    if(walkClear(chao.position(),desired))return List.of(desired);
    double radius=Math.max(1.1,f.getBbWidth()*.5+chao.getBbWidth()*.5+.28);
    var ring=new ArrayList<Vec3>();
    for(int i=0;i<8;i++){
      double a=i*Math.PI/4;
      ring.add(FloatToyRoutes080.dryFeetAt(level(),chao,f.getX()+Math.cos(a)*radius,f.getZ()+Math.sin(a)*radius,f.getY()));
    }
    List<Vec3> best=List.of();double bestLength=Double.MAX_VALUE;
    for(int start=0;start<8;start++){
      var begin=ring.get(start);if(begin==null||!reachable(begin))continue;
      // A remote native path is permitted only to the outer ring, never to a
      // pusher point through the vessel. Close approaches prove every segment.
      if(chao.distanceToSqr(f)<4&&!walkClear(chao.position(),begin))continue;
      for(int sign:new int[]{-1,1}){
        var path=new ArrayList<Vec3>();path.add(begin);double length=chao.position().distanceTo(begin);
        for(int step=0;step<8;step++){
          int index=Math.floorMod(start+step*sign,8);var here=ring.get(index);if(here==null)break;
          if(step>0){var previous=path.getLast();if(!walkClear(previous,here))break;length+=previous.distanceTo(here);path.add(here);}
          if(walkClear(here,desired)&&length+here.distanceTo(desired)<bestLength){
            var proposal=new ArrayList<>(path);proposal.add(desired);best=List.copyOf(proposal);bestLength=length+here.distanceTo(desired);
          }
        }
      }
    }
    return best;
  }
  private boolean walkClear(Vec3 from,Vec3 to){
    if(Math.abs(from.y-to.y)>.03)return false;
    int steps=Math.max(1,(int)Math.ceil(from.distanceTo(to)/.08));if(steps>160)return false;
    for(int i=0;i<=steps;i++)if(!FloatToyRoutes080.safeFeet(level(),chao,from.lerp(to,i/(double)steps)))return false;
    return true;
  }
  /** Read-only native fixture diagnostics; never calls canUse or consumes selection RNG. */
  public String diagnostic080(){
    return "eligible="+eligible()+",rescue="+rescue+",failed="+failed+",age="+age+",stall="+stalled
        +",nextCheck="+nextCheck+",launch="+(launch==null?"none":launchIndex+"/"+launch.points())
        +",shoreDeparted="+shoreDeparted+",entry="+(entry==null?"none":"dry:"+entry.dry()+" wet:"+entry.wet()+" swim:"+swimIndex+"/"+entry.swim())
        +",walk="+walkIndex+"/"+turnWalk+",legacy="+legacy;
  }
  private static double horizontal(Vec3 a,Vec3 b){return a.subtract(b).horizontalDistance();}
  @Override public void stop(){
    chao.getNavigation().stop();
    if(vessel!=null&&!vessel.isRemoved()){
      vessel.stopPush();if(vessel.rider()==chao)vessel.requestLand();else vessel.release(chao);
    }
    // Stop only movement this controller owns; no artificial flags require restoring.
    if(chao.isInWater()||chao.getVehicle() instanceof FloatToyEntity080)chao.setDeltaMovement(0,chao.getDeltaMovement().y,0);
    vessel=null;launch=null;entry=null;shoreDeparted=false;legacy=null;legacyStand=null;turnWalk=List.of();walkTarget=null;
    nextCheck=chao.level().getGameTime()+40+chao.getRandom().nextInt(61);
  }
}
