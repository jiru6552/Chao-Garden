package com.chaogarden.toys;

import com.chaogarden.entity.ChaoEntity;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Conservative, bounded plans for the physical float. Never loads a chunk or moves an actor.
 * Plans are suggestions only: the controller must revalidate every actual movement and exit.
 * No shore is safer than returning a water/rider position as a nominal dismount destination. */
public final class FloatToyRoutes080 {
  public static final int RADIUS=12,MAX_NODES=512;
  public static final double DRAFT=.05,MAX_STEP=.60,PUSH_DISTANCE=.81;
  private static final double EPS=.0001,HEADROOM=.30;
  private FloatToyRoutes080(){}

  public record Node(Vec3 pos,boolean water){}
  public record Landing(Vec3 waterDock,Vec3 feet,Vec3 escape){}
  public record Plan(List<Vec3> points,Landing landing){
    public Plan {points=List.copyOf(points);}
  }
  private record SearchNode(int x,int z,Vec3 pos,SearchNode previous){}

  /** Top surface of the connected water column near this point, not a fixed full-block height.
   * A solid roof/floor is never searched through to discover an unrelated pool. */
  public static Double surfaceAt(ServerLevel level,Vec3 near){
    int x=BlockPos.containing(near).getX(),z=BlockPos.containing(near).getZ();
    int top=Math.min(level.getMaxBuildHeight()-2,(int)Math.floor(near.y)+1);
    BlockPos water=null;
    for(int y=top;y>=Math.max(level.getMinBuildHeight(),top-16);y--){
      var at=new BlockPos(x,y,z);if(!level.hasChunkAt(at))return null;
      var fluid=level.getFluidState(at);
      if(fluid.is(FluidTags.WATER)){water=at;break;}
      if(!fluid.isEmpty()||!level.getBlockState(at).getCollisionShape(level,at).isEmpty())return null;
    }
    if(water==null)return null;
    for(int count=0;count<16;count++){
      var above=water.above();if(above.getY()>=level.getMaxBuildHeight()||!level.hasChunkAt(above))return null;
      var fluid=level.getFluidState(above);
      if(!fluid.is(FluidTags.WATER)){
        if(!fluid.isEmpty())return null;
        return water.getY()+(double)level.getFluidState(water).getHeight(level,water);
      }
      water=above;
    }
    return null;
  }

  /** A loaded, solid, non-hazardous complete footprint and an empty dry actor/head envelope. */
  public static boolean safeFeet(ServerLevel level,ChaoEntity actor,Vec3 feet){return safeFeetIgnoring(level,actor,feet,null);}
  /** Planning alone may ignore the stationary source prop while checking its
   * explicitly translated future envelope separately. Actual contact never does. */
  private static boolean safeFeetIgnoring(ServerLevel level,ChaoEntity actor,Vec3 feet,Entity ignored){
    var body=actorBox(actor,feet,true);
    return clear(level,actor,body,ignored,actor)&&!liquid(level,body)
        &&supported(level,actor,feet,actor.getBbWidth()*.5+.005);
  }

  /** Finds a physical collision top at exact X/Z. Supports ordinary blocks and substantial slabs.
   * It never invents Y=block+1 or accepts an unsupported corner/one-pixel decoration. */
  public static Vec3 dryFeetAt(ServerLevel level,ChaoEntity actor,double x,double z,double referenceY){
    return floorAt(level,actor,x,z,referenceY,actor.getBbWidth()*.5+.005,true);
  }

  public static Vec3 pushPoint(ServerLevel level,Entity vessel,ChaoEntity actor,Direction movement){
    if(movement.getAxis().isVertical())return null;
    return dryFeetAt(level,actor,vessel.getX()-movement.getStepX()*PUSH_DISTANCE,
        vessel.getZ()-movement.getStepZ()*PUSH_DISTANCE,vessel.getY());
  }

  /** Full sampled actual float/body movement; other floats/Chao/players count as obstacles.
   * An unmounted actor is ignored as the pusher, not imagined in a passenger seat. */
  public static boolean swept(ServerLevel level,Entity vessel,ChaoEntity rider,Vec3 from,Vec3 to){
    int steps=Math.max(1,(int)Math.ceil(from.distanceTo(to)/.08));
    if(steps>64)return false;
    Vec3 offset=rider!=null&&rider.getVehicle()==vessel?rider.position().subtract(vessel.position()):null;
    for(int i=0;i<=steps;i++){
      Vec3 at=from.lerp(to,i/(double)steps);
      AABB body=vessel.getBoundingBox().move(at.subtract(vessel.position())).deflate(.00001);
      if(!clear(level,vessel,body,vessel,rider))return false;
      if(offset!=null&&!clear(level,rider,actorBox(rider,at.add(offset),true),vessel,rider))return false;
    }
    return true;
  }

  /** A gentle short hop, shared by planning and the real entry/exit controller. */
  public static Vec3 exitPoint(Vec3 from,Vec3 to,float progress){
    double t=Math.max(0,Math.min(1,progress)),smooth=t*t*(3-2*t);
    // A little lift clears the shoreline edge; the endpoint is exactly the validated dry footpoint.
    double hop=Math.sin(Math.PI*t)*(.20+Math.abs(to.y-from.y)*.35);
    return from.lerp(to,smooth).add(0,hop,0);
  }
  public static boolean exitClear(ServerLevel level,ChaoEntity actor,Vec3 from,Vec3 to,Entity ignored){
    return exitClearAfterConsumption(level,actor,from,to,ignored,null);
  }
  /** Read-only preflight for a saved waterlogged toy. Ignore exactly the block
   * that the checked migration transaction will consume, never its neighbors,
   * roof, actors or other floats. Normal entry is revalidated after conversion. */
  public static boolean legacyEntryClear(ServerLevel level,ChaoEntity actor,Vec3 from,Vec3 to,Entity ignored,BlockPos source){
    if(source==null||!level.hasChunkAt(source))return false;
    var old=level.getBlockState(source);
    if(!(old.getBlock() instanceof ChaoToyBlock toy)||!FloatToyContent080.kind(toy.toy)
        ||!old.getValue(ChaoToyBlock.WATERLOGGED)
        ||ignored instanceof FloatToyEntity080 f&&!toy.toy.equals(f.kind()))return false;
    return exitClearAfterConsumption(level,actor,from,to,ignored,source);
  }
  private static boolean exitClearAfterConsumption(ServerLevel level,ChaoEntity actor,Vec3 from,Vec3 to,Entity ignored,BlockPos consumed){
    if(from.distanceToSqr(to)>2.25||Math.abs(to.y-from.y)>MAX_STEP+.25)return false;
    for(int i=0;i<=24;i++){
      var at=exitPoint(from,to,i/24f);
      if(!clear(level,actor,actorBox(actor,at,true),ignored,actor,consumed))return false;
    }
    return true;
  }

  /** One actual actor step, without restarting or reparameterizing the full entry/exit arc. */
  public static boolean sweptRider(ServerLevel level,Entity ignored,ChaoEntity actor,Vec3 from,Vec3 to){
    int steps=Math.max(1,(int)Math.ceil(from.distanceTo(to)/.04));
    if(steps>64)return false;
    for(int i=0;i<=steps;i++)
      if(!clear(level,actor,actorBox(actor,from.lerp(to,i/(double)steps),true),ignored,actor))return false;
    return true;
  }

  /** Current position followed by water-cell centers, ending at a revalidated dry landing. */
  public static Plan waterRoute(ServerLevel level,Entity vessel,ChaoEntity rider){
    if(rider==null)return null;
    Double surface=surfaceAt(level,vessel.position());if(surface==null)return null;
    int ox=BlockPos.containing(vessel.position()).getX(),oz=BlockPos.containing(vessel.position()).getZ();
    Vec3 start=new Vec3(ox+.5,surface-DRAFT,oz+.5);
    if(!swept(level,vessel,rider,vessel.position(),start))return null;
    var root=new SearchNode(ox,oz,start,null);var queue=new ArrayDeque<SearchNode>();queue.add(root);
    var seen=new HashSet<Long>();seen.add(key(ox,oz));int examined=0;
    while(!queue.isEmpty()&&examined++<MAX_NODES){
      var n=queue.removeFirst();var landing=landingAt(level,vessel,rider,n.pos);
      if(landing!=null)return new Plan(points(vessel.position(),n),landing);
      for(var direction:Direction.Plane.HORIZONTAL){
        int x=n.x+direction.getStepX(),z=n.z+direction.getStepZ();long key=key(x,z);
        if(Math.abs(x-ox)+Math.abs(z-oz)>RADIUS||seen.contains(key))continue;
        var probe=new Vec3(x+.5,n.pos.y+DRAFT,z+.5);Double nextSurface=surfaceAt(level,probe);
        if(nextSurface==null||Math.abs(nextSurface-(n.pos.y+DRAFT))>MAX_STEP)continue;
        var next=new Vec3(x+.5,nextSurface-DRAFT,z+.5);
        if(swept(level,vessel,rider,n.pos,next)){seen.add(key);queue.addLast(new SearchNode(x,z,next,n));}
      }
    }
    return null;
  }

  /** Nearby dry egress also works after the water is drained or a ridden float reaches land. */
  public static Landing directLanding(ServerLevel level,Entity vessel,ChaoEntity rider){
    return rider==null?null:landingAt(level,vessel,rider,vessel.position());
  }

  /** Revalidate the entire dry escape corridor, not only its two endpoints.
   * Call again before leaving the float and before committing a finished exit. */
  public static boolean landingSafe(ServerLevel level,ChaoEntity rider,Landing landing,Entity ignored){
    return landing!=null&&safeFeet(level,rider,landing.feet())&&safeFeet(level,rider,landing.escape())
        &&walkingCorridor(level,rider,landing.feet(),landing.escape(),ignored);
  }

  /** Plans dry supported pushes to a usable water cell. The final pusher remains on dry support.
   * Every turn still needs the controller's nearby walk-around/pusher-position check. */
  public static Plan launchRoute(ServerLevel level,Entity vessel,ChaoEntity actor){
    if(actor==null)return null;
    int ox=BlockPos.containing(vessel.position()).getX(),oz=BlockPos.containing(vessel.position()).getZ();
    var feet=floorAt(level,vessel,ox+.5,oz+.5,vessel.getY(),vessel.getBbWidth()*.5,false);
    if(feet==null||!swept(level,vessel,actor,vessel.position(),feet))return null;
    var root=new SearchNode(ox,oz,feet,null);var queue=new ArrayDeque<SearchNode>();queue.add(root);
    Set<Long> seen=new HashSet<>();seen.add(key(ox,oz));int examined=0;
    while(!queue.isEmpty()&&examined++<MAX_NODES){
      var n=queue.removeFirst();
      for(var direction:Direction.Plane.HORIZONTAL){
        int x=n.x+direction.getStepX(),z=n.z+direction.getStepZ();long key=key(x,z);
        if(Math.abs(x-ox)+Math.abs(z-oz)>RADIUS||seen.contains(key))continue;
        Vec3 next=null;boolean water=false;
        Double surface=surfaceAt(level,new Vec3(x+.5,n.pos.y,z+.5));
        if(surface!=null&&Math.abs((surface-DRAFT)-n.pos.y)<=MAX_STEP){
          // Keep the prop above its current bank until the complete footprint
          // has left it; its physical buoyancy/gravity settles it afterward.
          next=new Vec3(x+.5,Math.max(n.pos.y,surface-DRAFT),z+.5);water=true;
        }
        if(next==null)next=floorAt(level,vessel,x+.5,z+.5,n.pos.y,vessel.getBbWidth()*.5,false);
        // This pushing controller has no climbing step. Do not plan a diagonal
        // cut through the riser of a slab/block which physics cannot perform.
        if(next==null||!water&&Math.abs(next.y-n.pos.y)>.025||Math.abs(next.y-n.pos.y)>MAX_STEP||!swept(level,vessel,actor,n.pos,next)
            ||!pushEdge(level,vessel,actor,n.pos,next,direction))continue;
        seen.add(key);var entry=new SearchNode(x,z,next,n);
        if(water)return new Plan(points(vessel.position(),entry),null);
        queue.addLast(entry);
      }
    }
    return null;
  }

  /** A short physical surface route for an actor already in water. Ground navigation
   * cannot reliably describe this case, especially for an untrained distressed Chao.
   * The caller still supplies bounded real velocity and revalidates each small step.
   * An empty list means no safe route; it never means jump/teleport to the target. */
  public static List<Vec3> swimRoute(ServerLevel level,ChaoEntity actor,Vec3 target){
    return swimRouteFrom(level,actor,actor.position(),target);
  }

  /** Pure preflight from an explicit wet entry point; never repositions the actor. */
  public static List<Vec3> swimRouteFrom(ServerLevel level,ChaoEntity actor,Vec3 start,Vec3 target){
    Double surface=surfaceAt(level,start);if(surface==null)return List.of();
    int ox=BlockPos.containing(start).getX(),oz=BlockPos.containing(start).getZ();
    int tx=BlockPos.containing(target).getX(),tz=BlockPos.containing(target).getZ();
    if(Math.abs(tx-ox)+Math.abs(tz-oz)>RADIUS)return List.of();
    var rootPos=new Vec3(ox+.5,surface-.35,oz+.5);
    if(!swimClear(level,actor,start,rootPos))return List.of();
    var root=new SearchNode(ox,oz,rootPos,null);var queue=new ArrayDeque<SearchNode>();queue.add(root);
    var seen=new HashSet<Long>();seen.add(key(ox,oz));int examined=0;
    while(!queue.isEmpty()&&examined++<MAX_NODES){
      var n=queue.removeFirst();
      if(n.x==tx&&n.z==tz){
        Double endSurface=surfaceAt(level,new Vec3(target.x,n.pos.y+.35,target.z));
        if(endSurface==null)return List.of();
        var last=new Vec3(target.x,endSurface-.35,target.z);
        if(!swimClear(level,actor,n.pos,last))return List.of();
        var route=points(start,n);
        if(route.getLast().distanceToSqr(last)>.0001)route.add(last);
        return List.copyOf(route);
      }
      for(var direction:Direction.Plane.HORIZONTAL){
        int x=n.x+direction.getStepX(),z=n.z+direction.getStepZ();long key=key(x,z);
        if(Math.abs(x-ox)+Math.abs(z-oz)>RADIUS||seen.contains(key))continue;
        Double nextSurface=surfaceAt(level,new Vec3(x+.5,n.pos.y+.35,z+.5));
        if(nextSurface==null||Math.abs(nextSurface-(n.pos.y+.35))>MAX_STEP)continue;
        var next=new Vec3(x+.5,nextSurface-.35,z+.5);
        if(swimClear(level,actor,n.pos,next)){seen.add(key);queue.addLast(new SearchNode(x,z,next,n));}
      }
    }
    return List.of();
  }

  /** Revalidate a real physical step, including connected water under the full step. */
  public static boolean swimStepClear(ServerLevel level,ChaoEntity actor,Vec3 from,Vec3 to){
    if(from.distanceToSqr(to)>2.25)return false;
    if(surfaceAt(level,to)==null)return false;
    return swimClear(level,actor,from,to);
  }

  /** Full collision envelope for one bounded authored shore-entry or swim step. */
  public static boolean actorStepClear(ServerLevel level,ChaoEntity actor,Vec3 from,Vec3 to){
    return from.distanceToSqr(to)<=2.25&&swimClear(level,actor,from,to);
  }

  private static boolean swimClear(ServerLevel level,ChaoEntity actor,Vec3 from,Vec3 to){
    int steps=Math.max(1,(int)Math.ceil(from.distanceTo(to)/.10));if(steps>256)return false;
    for(int i=0;i<=steps;i++)
      if(!clear(level,actor,actorBox(actor,from.lerp(to,i/(double)steps),true),null,actor))return false;
    return true;
  }

  private static boolean pushEdge(ServerLevel level,Entity vessel,ChaoEntity actor,Vec3 from,Vec3 to,Direction direction){
    Vec3 a=floorAt(level,actor,from.x-direction.getStepX()*PUSH_DISTANCE,
        from.z-direction.getStepZ()*PUSH_DISTANCE,from.y,actor.getBbWidth()*.5+.005,true,vessel);
    Vec3 b=floorAt(level,actor,to.x-direction.getStepX()*PUSH_DISTANCE,
        to.z-direction.getStepZ()*PUSH_DISTANCE,from.y,actor.getBbWidth()*.5+.005,true,vessel);
    if(a==null||b==null||Math.abs(a.y-b.y)>MAX_STEP)return false;
    int steps=Math.max(1,(int)Math.ceil(a.distanceTo(b)/.10));
    for(int i=0;i<=steps;i++){
      Vec3 at=a.lerp(b,i/(double)steps);
      if(!safeFeetIgnoring(level,actor,at,vessel))return false;
      // Do not let a nominal pusher position intersect the moving physical prop.
      Vec3 prop=from.lerp(to,i/(double)steps);
      if(actorBox(actor,at,false).intersects(vessel.getBoundingBox().move(prop.subtract(vessel.position())).deflate(.001)))return false;
    }
    return true;
  }

  private static Landing landingAt(ServerLevel level,Entity vessel,ChaoEntity rider,Vec3 dock){
    Vec3 seatOffset=rider.getVehicle()==vessel?rider.position().subtract(vessel.position()):new Vec3(0,.28,0);
    Vec3 from=dock.add(seatOffset);double waterTop=dock.y+DRAFT;
    for(var direction:Direction.Plane.HORIZONTAL){
      double x=Math.floor(dock.x)+.5+direction.getStepX(),z=Math.floor(dock.z)+.5+direction.getStepZ();
      var feet=dryFeetAt(level,rider,x,z,waterTop);
      if(feet==null||Math.abs(feet.y-waterTop)>MAX_STEP||!exitClear(level,rider,from,feet,vessel))continue;
      var escape=dryFeetAt(level,rider,x+direction.getStepX(),z+direction.getStepZ(),feet.y);
      if(escape==null||Math.abs(escape.y-feet.y)>MAX_STEP||!walkingCorridor(level,rider,feet,escape,vessel))continue;
      return new Landing(dock,feet,escape);
    }
    return null;
  }
  private static boolean walkingCorridor(ServerLevel level,ChaoEntity actor,Vec3 a,Vec3 b,Entity ignored){
    int steps=Math.max(1,(int)Math.ceil(a.distanceTo(b)/.10));
    for(int i=0;i<=steps;i++){
      var at=a.lerp(b,i/(double)steps);
      if(!clear(level,actor,actorBox(actor,at,true),ignored,actor)||liquid(level,actorBox(actor,at,false))
          ||!supported(level,actor,at,actor.getBbWidth()*.5+.005))return false;
    }
    return true;
  }

  private static Vec3 floorAt(ServerLevel level,Entity entity,double x,double z,double referenceY,double radius,boolean actor){return floorAt(level,entity,x,z,referenceY,radius,actor,null);}
  private static Vec3 floorAt(ServerLevel level,Entity entity,double x,double z,double referenceY,double radius,boolean actor,Entity ignored){
    int bx=(int)Math.floor(x),bz=(int)Math.floor(z),base=(int)Math.floor(referenceY);var candidates=new ArrayList<Double>();
    for(int y=base-2;y<=base+1;y++){
      var at=new BlockPos(bx,y,bz);if(!level.hasChunkAt(at)||hazard(level.getBlockState(at)))continue;
      for(var box:level.getBlockState(at).getCollisionShape(level,at,CollisionContext.of(entity)).toAabbs()){
        if(box.maxY-box.minY<.05)continue;double top=y+box.maxY;
        if(Math.abs(top-referenceY)<=MAX_STEP+EPS)candidates.add(top);
      }
    }
    candidates.sort((a,b)->Double.compare(Math.abs(a-referenceY),Math.abs(b-referenceY)));
    for(double y:candidates){
      var feet=new Vec3(x,y,z);AABB body=entity.getBoundingBox().move(feet.subtract(entity.position()));
      if(actor)body=body.expandTowards(0,HEADROOM,0);
      if(!supported(level,entity,feet,radius)||!clear(level,entity,body,ignored,entity)||liquid(level,body))continue;
      return feet;
    }
    return null;
  }

  private static boolean supported(ServerLevel level,Entity context,Vec3 feet,double radius){
    double r=Math.max(.01,radius),x0=feet.x-r,x1=feet.x+r,z0=feet.z-r,z1=feet.z+r;
    var rectangles=new ArrayList<AABB>();
    // The one-cell rim also catches ordinary overhanging collision shapes.
    // Neighbor blocks outside that rim are never read or loaded.
    int minX=(int)Math.floor(x0)-1,maxX=(int)Math.floor(x1)+1;
    int minZ=(int)Math.floor(z0)-1,maxZ=(int)Math.floor(z1)+1;
    int minY=Math.max(level.getMinBuildHeight(),(int)Math.floor(feet.y)-2);
    int maxY=Math.min(level.getMaxBuildHeight()-1,(int)Math.floor(feet.y));
    for(var at:BlockPos.betweenClosed(new BlockPos(minX,minY,minZ),new BlockPos(maxX,maxY,maxZ))){
      if(!level.hasChunkAt(at))return false;
      var state=level.getBlockState(at);
      for(var b:state.getCollisionShape(level,at,CollisionContext.of(context)).toAabbs()){
        if(b.maxY-b.minY<.05||Math.abs(at.getY()+b.maxY-feet.y)>=.025)continue;
        double left=Math.max(x0,at.getX()+b.minX),right=Math.min(x1,at.getX()+b.maxX);
        double back=Math.max(z0,at.getZ()+b.minZ),front=Math.min(z1,at.getZ()+b.maxZ);
        if(right<=left||front<=back)continue;
        if(hazard(state))return false;
        rectangles.add(new AABB(left,feet.y-.01,back,right,feet.y,front));
        // Extremely intricate modded shapes are conservatively rejected rather
        // than making a route probe consume unbounded CPU time.
        if(rectangles.size()>256)return false;
      }
    }
    return coveredXZ(x0,z0,x1,z1,rectangles);
  }

  /** Rectangular-union coverage, with only a 1e-7-block rounding tolerance.
   * Sampling corners/center cannot prove support across holes in non-convex shapes. */
  static boolean coveredXZ(double x0,double z0,double x1,double z1,List<AABB> rectangles){
    final double rounding=1.0e-7;
    if(!(x1>x0&&z1>z0)||rectangles.isEmpty())return false;
    var cuts=new ArrayList<Double>();cuts.add(x0);cuts.add(x1);
    for(var b:rectangles){
      cuts.add(Math.max(x0,Math.min(x1,b.minX)));cuts.add(Math.max(x0,Math.min(x1,b.maxX)));
    }
    Collections.sort(cuts);
    for(int i=0;i<cuts.size()-1;i++){
      double left=cuts.get(i),right=cuts.get(i+1);if(right<=left)continue;
      double middle=(left+right)*.5;var intervals=new ArrayList<AABB>();
      for(var b:rectangles)if(b.minX<=middle&&b.maxX>=middle&&b.maxZ>z0&&b.minZ<z1)intervals.add(b);
      intervals.sort((a,b)->Double.compare(a.minZ,b.minZ));double covered=z0;
      for(var b:intervals){
        if(b.maxZ<=covered)continue;
        if(b.minZ>covered+rounding)return false;
        covered=Math.max(covered,b.maxZ);if(covered>=z1-rounding)break;
      }
      if(covered<z1-rounding)return false;
    }
    return true;
  }
  private static AABB actorBox(ChaoEntity actor,Vec3 feet,boolean headroom){
    var result=actor.getBoundingBox().move(feet.subtract(actor.position())).deflate(.00001);
    return headroom?result.expandTowards(0,HEADROOM,0):result;
  }
  private static boolean clear(ServerLevel level,Entity context,AABB body,Entity ignored,Entity ignoredActor){
    return clear(level,context,body,ignored,ignoredActor,null);
  }
  private static boolean clear(ServerLevel level,Entity context,AABB body,Entity ignored,Entity ignoredActor,BlockPos consumed){
    if(!loaded(level,body))return false;
    for(var at:BlockPos.betweenClosed(BlockPos.containing(body.minX,body.minY,body.minZ),
        BlockPos.containing(body.maxX-EPS,body.maxY-EPS,body.maxZ-EPS))){
      if(consumed!=null&&at.equals(consumed))continue;
      var state=level.getBlockState(at);if(hazard(state))return false;
      for(var shape:state.getCollisionShape(level,at,CollisionContext.of(context)).toAabbs())
        if(shape.move(at).intersects(body))return false;
    }
    return level.getEntities(context,body,e->e!=ignored&&e!=ignoredActor&&!e.isSpectator()&&e.isAlive()
        &&(e instanceof LivingEntity||e.isPickable())&&!e.isPassengerOfSameVehicle(context)).isEmpty();
  }
  private static boolean liquid(ServerLevel level,AABB body){
    return !loaded(level,body)||level.containsAnyLiquid(body.deflate(.0001));
  }
  private static boolean loaded(ServerLevel level,AABB body){
    if(body.minY<level.getMinBuildHeight()||body.maxY>level.getMaxBuildHeight())return false;
    if(!level.getWorldBorder().isWithinBounds(body))return false;
    // Collision getters can consult a neighboring state. Test each affected
    // chunk once, not every block/Y in the same already-loaded chunk.
    int x0=Math.floorDiv((int)Math.floor(body.minX-1),16),x1=Math.floorDiv((int)Math.floor(body.maxX+1),16);
    int z0=Math.floorDiv((int)Math.floor(body.minZ-1),16),z1=Math.floorDiv((int)Math.floor(body.maxZ+1),16);
    if(x1-x0>2||z1-z0>2)return false;
    int y=(int)Math.floor(body.minY);
    for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++)
      if(!level.hasChunkAt(new BlockPos(x*16,y,z*16)))return false;
    return true;
  }
  private static boolean hazard(BlockState state){
    return state.is(Blocks.FIRE)||state.is(Blocks.SOUL_FIRE)||state.is(Blocks.LAVA)||state.is(Blocks.MAGMA_BLOCK)
        ||state.is(Blocks.CACTUS)||state.is(Blocks.SWEET_BERRY_BUSH)||state.is(Blocks.WITHER_ROSE)
        ||state.is(Blocks.CAMPFIRE)||state.is(Blocks.SOUL_CAMPFIRE)||state.is(Blocks.POWDER_SNOW);
  }
  private static long key(int x,int z){return BlockPos.asLong(x,0,z);}
  private static List<Vec3> points(Vec3 actual,SearchNode end){
    var result=new ArrayList<Vec3>();for(var n=end;n!=null;n=n.previous)result.add(n.pos);
    Collections.reverse(result);if(result.isEmpty()||actual.distanceToSqr(result.getFirst())>.0001)result.addFirst(actual);
    return result;
  }
}
