package com.chaogarden.hub;

import com.chaogarden.activities.ActivityBlock;
import com.chaogarden.activities.ActivityContent;
import com.chaogarden.activities.ActivityManager;
import com.chaogarden.activities.ActivityRules;
import com.chaogarden.activities.GardenRegistry;
import com.chaogarden.activities.GardenTransporterBlock;
import com.chaogarden.entity.ChaoEggEntity;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.fruit.ChaoSaplingBlock;
import com.chaogarden.fruit.ChaoTreeCrownBlock;
import com.chaogarden.fruit.FruitContent;
import com.chaogarden.fruit.HangingFruitBlock;
import com.chaogarden.toys.ChaoToyBlock;
import java.util.HashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** SA2/Battle's first garden: cave wall, western cascade, eastern ridge and an open sky edge. */
@EventBusSubscriber(modid="chao_garden")
public final class GardenRooms025 {
  // The invisible outer safety wall reaches the dimension's build ceiling. This
  // prevents flying over a short fence and falling out of the floating garden.
  public static final AABB BOUNDS=new AABB(-53,66,-51,54,256,48);
  public static final AABB ACTIVE_BOUNDS=new AABB(-40,66,-23,26,256,20);
  public static final BlockPos EXIT_PORTAL=new BlockPos(5,80,-18);
  public static final BlockPos TRANSPORTER=new BlockPos(17,81,9);
  private static final BlockPos PREVIOUS_TRANSPORTER=new BlockPos(34,82,15);
  private static final BlockPos LEGACY_TRANSPORTER=new BlockPos(-33,82,15);
  public static final int POND_LEVEL=79;
  public static final int[][] PALMS={{-3,0,4},{-5,10,4},{5,10,5},{11,-3,4},{15,13,4},{-8,5,4},{14,-10,4},{17,0,4}};
  public static final int POND_CENTER_X=-20,POND_CENTER_Z=-8;
  public static final Direction WATERFALL_FLOW=Direction.EAST;
  public static final AABB WATERFALL_BOUNDS=new AABB(-40,79,-13,-24,95,1);
  /** X coordinates: the cascade now faces east from the adjoining western wall. */
  public static final int[] WATERFALL_FRONTS={-26,-29,-32,-35},WATERFALL_LEVELS={82,85,88,91};
  private static final Set<BlockPos> FIXED_PODS=new HashSet<>(),LEGACY_FIXED=new HashSet<>();
  static{
    for(var p:PALMS)addPods(FIXED_PODS,p[0],p[1],surface(p[0],p[1])+p[2]+1);
    int[][] old={{-14,1,6},{-4,18,6},{-35,7,7},{-30,26,6},{8,21,7},{34,13,6},{7,-18,5},{17,-24,5},{34,-27,4},{24,-33,4},{28,-39,4}};
    for(var p:old)addPods(LEGACY_FIXED,p[0],p[1],legacySurface(p[0],p[1])+p[2]+1);
    int[][] previous={{-9,-2,6},{-5,19,6},{12,17,7},{24,3,6},{32,25,6},{-4,-19,5},{27,-24,6},{38,8,6}};
    for(var p:previous)addPods(LEGACY_FIXED,p[0],p[1],previousSurface(p[0],p[1])+p[2]+1);
    // The 0.25.2 pond-side native palm moves to a dry shore as its water feature
    // turns. Only actual native trunk/crown/support geometry qualifies as old
    // scenery; an owned ordinary fruit tree at these coordinates remains care.
    addPods(LEGACY_FIXED,-1,-8,84);
    LEGACY_FIXED.add(LEGACY_TRANSPORTER);LEGACY_FIXED.add(PREVIOUS_TRANSPORTER);
  }
  private GardenRooms025(){}
  private static void addPods(Set<BlockPos> output,int x,int z,int crown){for(Direction d:Direction.Plane.HORIZONTAL)output.add(new BlockPos(x,crown-1,z).relative(d));}
  public static boolean placeable(Block block){return block instanceof ActivityBlock||block instanceof ChaoToyBlock||block instanceof ChaoSaplingBlock||block instanceof ChaoTreeCrownBlock||block instanceof HangingFruitBlock||block==FruitContent.TRUNK.get()||block==FruitContent.DARK_TRUNK.get()||block==FruitContent.LEAVES.get()||block==FruitContent.PALM_SUPPORT.get();}
  public static boolean breakable(BlockPos pos,BlockState state){return placeable(state.getBlock())&&!(GardenTransporterBlock.footprint(TRANSPORTER).contains(pos)&&state.is(ActivityContent.GARDEN.get()))&&!FIXED_PODS.contains(pos);}
  /** Keep the old east edge; extend only the western half fifteen blocks for the moved waterfall. */
  public static boolean insideField(int x,int z){return Math.pow(x/(x<0?39.0:24.0),4)+Math.pow((z+2)/20.0,4)<=1;}
  private static int westEdge(int z){for(int x=-39;x<=0;x++)if(insideField(x,z))return x;return 0;}
  private static int eastEdge(int z){for(int x=24;x>=0;x--)if(insideField(x,z))return x;return 0;}
  public static double pondDistance(int x,int z){return Math.pow((x-POND_CENTER_X)/10.0,2)+Math.pow((z-POND_CENTER_Z)/7.0,2);}
  public static boolean pondCell(int x,int z){return pondDistance(x,z)<1&&x>=-25;}
  private static boolean waterfallMass(int x,int z){return x>=-40&&x<=-26&&z>=-13&&z<=1;}
  /** A low curved bank fills the bare lawn beside the cascade, clear of its stream. */
  public static boolean waterfallBank(int x,int z){
    return z>=2&&z<=8&&x>westEdge(z)+3&&Math.pow((x+31.0)/6.0,2)+Math.pow((z-2.0)/6.0,2)<1;
  }
  private static boolean previousWaterfallMass(int x,int z){return x>=-47&&x<=-15&&z>=-43&&z<=-17;}
  public static int surface(int x,int z){
    int height=79;
    // The source gneut03 terrain rises into the eastern shoulder before the cliff.
    // Keep the existing palms, care lawn, transporter and pool outside this bank.
    if(x>=17&&x<=19&&z>=-13&&z<=-5)height=79+Math.min(2,x-16);
    // A broad central lawn runs continuously to the compact cave. The departure
    // machine occupies a low outer hummock rather than a separate huge plateau.
    double ridge=Math.pow((x-17)/6.0,2)+Math.pow((z-9)/7.0,2);
    if(ridge<1)height=80;
    if(z>11&&x>-9&&x<11&&((x+3)*(x+3)+(z-17)*(z-17))<65)height=Math.max(height,80);
    int face=-15-(int)Math.round(.8*Math.sin(x*.34)+.65*Math.cos(x*.57));
    if(x>=2&&x<=8)face=-17;
    if(z<=face){
      int crag=89+(int)Math.round(1.5*Math.cos(x*.29)+1.2*Math.sin(x*.61));
      if(z<face-3)crag+=Math.min(2,(face-z-3)/2);
      height=Math.max(height,crag);
    }
    // Both side mountains now meet the northern mass and run to the southern
    // shore. The southern horizon is the only open side; the two-cell invisible
    // safety ring still follows the edge beyond these real rock columns.
    if(insideField(x,z)&&z<18){
      boolean west=x<=westEdge(z)+3,east=x>=eastEdge(z)-3;
      if(west||east){
        int crag=89+(int)Math.round(1.4*Math.cos(z*.31)+1.2*Math.sin(z*.47));
        height=Math.max(height,crag);
      }
    }
    // A solid northern shoulder joins every terrace directly to the main
    // mountain instead of leaving the reported skylight seam behind it.
    if(x<=-24&&z<=-9)height=Math.max(height,91+(int)Math.round(1.5*Math.sin(x*.23)));
    if(waterfallMass(x,z)&&z>=-8){
      int step=Math.max(0,Math.min(3,(-x-26)/3));
      int shoulders=Math.abs(z+6)<=4?0:1;
      height=82+step*3+shoulders;
    }
    if(waterfallBank(x,z)){
      double distance=Math.pow((x+31.0)/6.0,2)+Math.pow((z-2.0)/6.0,2);
      height=Math.max(height,79+(int)Math.ceil(4*(1-distance)));
    }
    if(x>=-6&&x<=6&&z>=0&&z<=10)return 79;
    return Math.min(95,height);
  }
  private static int previousSurface(int x,int z){
    // Keep ordinary care and activity placements on a continuous level lawn.
    int height=79;
    if(z>19)height+=(int)Math.floor(Math.max(0,1.7*Math.sin((z-17)*.11)));
    double ridge=Math.pow((x-34)/13.0,2)+Math.pow((z-15)/15.0,2);
    if(ridge<1)height=Math.max(height,79+(int)Math.floor(2.8*(1-ridge)));
    int face=-33-(int)Math.round(1.5*Math.sin(x*.14)+Math.cos(x*.31));
    if(x>=8&&x<=16)face=-33;
    if(z<=face){
      int crag=93+(int)Math.round(3*Math.cos(x*.11)+2*Math.sin(x*.29));
      if(z<face-5)crag+=Math.min(3,(face-z-5)/3);
      height=Math.max(height,crag);
    }
    // One irregular cliff arm ends beside the transporter. The remainder of the
    // front/west horizon is deliberately open, rather than another wall.
    int eastFace=43+(int)Math.round(2*Math.sin(z*.16));
    if(x>=eastFace&&z<=18){
      int crag=92+(int)Math.round(3*Math.cos(z*.14));
      if(z>7)crag-=Math.min(9,z-7);
      height=Math.max(height,crag);
    }
    if(previousWaterfallMass(x,z)){
      int depth=Math.max(0,Math.min(3,(-z-17)/6));
      int width=Math.max(0,Math.min(3,(16-Math.abs(x+31))/4));
      height=Math.max(height,82+4*Math.min(depth,width));
    }
    if(x>=-6&&x<=6&&z>=0&&z<=11)return 79;
    return Math.min(101,height);
  }
  private static int legacySurface(int x,int z){
    if(x>=-6&&x<=6&&z>=0&&z<=11)return 79;
    if(x>=-30&&x<=-23&&z>=-39&&z<=-21)return 79;
    int h=79+(int)Math.round(Math.max(0,Math.sin(x*.07+z*.04))*2+Math.max(0,Math.sin(z*.09-1))*1.4);
    if(z<-28){int tier=(-z-27)/4;h=Math.max(h,85+tier*4+(int)Math.round(2*Math.sin(x*.19)));}
    if(x<-43)h=Math.max(h,88+(-x-43)*2);if(x>43)h=Math.max(h,88+(x-43)*2);
    if(x>=13&&x<=38&&z<=-21&&z>=-46){h=Math.max(h,82);for(int i=1;i<=5;i++)if(x>=13+i&&x<=38-i&&z<=-21-i*4)h=Math.max(h,82+i*5);}
    if(Math.pow((x+33)/7.0,2)+Math.pow((z-15)/7.0,2)<1)h=Math.max(h,81);
    return Math.min(112,h);
  }
  public static void ensure(ServerLevel level){
    if(!level.dimension().equals(HubContent.GARDEN))throw new IllegalArgumentException("Neutral garden only builds in its own dimension");
    var p=plan();var migration=prepareCareAssemblies(level,p);
    var planned=new PlannedWorld(level,p);var bodies=level.getEntitiesOfClass(Entity.class,BOUNDS,GardenRooms025::eligibleBody);
    var landings=new LinkedHashMap<Entity,Vec3>();var reserved=new ArrayList<AABB>();var displaced=new ArrayList<Entity>();
    // Freeze all surviving body positions before assigning any new landing.
    // Provider staging, callbacks and terrain changes happen only after this
    // complete capacity/collision preflight succeeds.
    for(var entity:bodies){
      var feet=entity.blockPosition();
      // A formerly dry lawn can become the receiving pool one block below a
      // standing actor's feet. Its body has no immediate water/collision there,
      // but keeping that position would drop it into newly authored water.
      // Existing swimmers and actors already over water retain their position.
      boolean wasWet=!level.getFluidState(feet).isEmpty()||!level.getFluidState(feet.below()).isEmpty();
      boolean becomesWet=!planned.getFluidState(feet).isEmpty()||!planned.getFluidState(feet.below()).isEmpty();
      boolean newlyWet=becomesWet&&!wasWet;
      boolean safe=insideField(entity.blockPosition().getX(),entity.blockPosition().getZ())&&clear(planned,entity,entity.position())&&!newlyWet;
      if(safe)reserved.add(entity.getBoundingBox().inflate(.02));
      else{
        if(protectedActivity(entity))throw new IllegalStateException("Garden update awaits active activity/companion in removed terrain: "+entity.getUUID());
        displaced.add(entity);
      }
    }
    boolean changesCourse=migration.moves.keySet().stream().anyMatch(at->migration.props.get(at.block()).getBlock() instanceof ActivityBlock block&&block.kind!=ActivityBlock.Kind.GARDEN);
    if(changesCourse&&bodies.stream().anyMatch(e->e instanceof ChaoEntity chao&&ActivityManager.isBusy(chao)))throw new IllegalStateException("Garden update awaits active contest before moving its course");
    for(var entity:displaced){
      var safe=safeLawn(planned,entity,reserved);if(safe==null)throw new IllegalStateException("No clear garden upgrade landing for "+entity.getUUID());
      landings.put(entity,safe);reserved.add(entity.getBoundingBox().move(safe.subtract(entity.position())).inflate(.02));
    }
    commitCareAssemblies(level,migration);
    p.apply(level,BOUNDS);
    for(var move:landings.entrySet()){
      if(!moveBody(level,move.getKey(),move.getValue()))throw new IllegalStateException("Garden body relocation did not reach its preflighted position: "+move.getKey().getUUID());
    }
  }
  private static boolean eligibleBody(Entity entity){return entity instanceof ChaoEntity||entity instanceof ChaoEggEntity||entity instanceof ItemEntity||entity instanceof ServerPlayer;}
  private static boolean protectedActivity(Entity entity){return entity instanceof ChaoEntity chao&&(GardenRegistry.managed(chao)||ActivityManager.isBusy(chao));}
  private static final class PlannedWorld implements BlockGetter{
    final ServerLevel source;final ScenePlan025 plan;
    PlannedWorld(ServerLevel source,ScenePlan025 plan){this.source=source;this.plan=plan;}
    @Override public BlockEntity getBlockEntity(BlockPos at){return null;}
    @Override public BlockState getBlockState(BlockPos at){return plan.blocks.getOrDefault(at,Blocks.AIR.defaultBlockState());}
    @Override public FluidState getFluidState(BlockPos at){return getBlockState(at).getFluidState();}
    @Override public int getHeight(){return source.getHeight();}
    @Override public int getMinBuildHeight(){return source.getMinBuildHeight();}
  }
  private static boolean clear(BlockGetter world,Entity entity,Vec3 at){
    var box=entity.getBoundingBox().move(at.subtract(entity.position())).deflate(1.e-6);var scan=box.inflate(1);
    for(var pos:BlockPos.betweenClosed(BlockPos.containing(scan.minX,scan.minY,scan.minZ),BlockPos.containing(scan.maxX,scan.maxY,scan.maxZ)))
      for(var part:world.getBlockState(pos).getCollisionShape(world,pos,CollisionContext.of(entity)).toAabbs())if(box.intersects(part.move(pos)))return false;
    return true;
  }
  private static boolean dryPlacement(ScenePlan025 plan,Map<BlockPos,BlockState> props,BlockPos offset,Set<BlockPos> occupied){
    for(var e:props.entrySet()){
      var at=e.getKey().offset(offset);var proposed=plan.blocks.getOrDefault(at,Blocks.AIR.defaultBlockState());
      if(!insideField(at.getX(),at.getZ())||occupied.contains(at))return false;
      boolean wet=e.getValue().hasProperty(BlockStateProperties.WATERLOGGED)&&e.getValue().getValue(BlockStateProperties.WATERLOGGED);
      if(wet){if(!proposed.getFluidState().isSource()||!proposed.is(Blocks.WATER))return false;}
      else if(!proposed.isAir()&&!(e.getValue().is(ActivityContent.GARDEN.get())&&GardenTransporterBlock.compatibleState(e.getValue(),proposed)))return false;
      // Floating water toys keep their exact waterlogged state in actual source
      // water; dry bottom cells require turf. All upper tree geometry stays intact.
      var block=e.getValue().getBlock();boolean root=block instanceof ActivityBlock||block instanceof ChaoToyBlock||block instanceof ChaoSaplingBlock
          ||(block==FruitContent.TRUNK.get()&&e.getValue().getValue(BlockStateProperties.AXIS)==Direction.Axis.Y&&!props.containsKey(e.getKey().below()))
          ||(block instanceof ChaoTreeCrownBlock&&!props.containsKey(e.getKey().below()));
      if(!wet&&root&&!props.containsKey(e.getKey().below())&&!plan.blocks.getOrDefault(at.below(),Blocks.AIR.defaultBlockState()).is(LobbyDecor025.TURF.get()))return false;
    }
    return true;
  }
  private static List<Map<BlockPos,BlockState>> assemblies(Map<BlockPos,BlockState> props,GardenRegistry data,ServerLevel level){
    var edges=new LinkedHashMap<BlockPos,List<BlockPos>>();
    data.links.forEach((from,to)->{if(from.dimension().equals(level.dimension().location().toString())&&to.dimension().equals(from.dimension())&&props.containsKey(from.block())&&props.containsKey(to.block())){edges.computeIfAbsent(from.block(),key->new ArrayList<>()).add(to.block());edges.computeIfAbsent(to.block(),key->new ArrayList<>()).add(from.block());}});
    var left=new LinkedHashMap<>(props);var groups=new ArrayList<Map<BlockPos,BlockState>>();
    while(!left.isEmpty()){
      var group=new LinkedHashMap<BlockPos,BlockState>();var todo=new ArrayDeque<BlockPos>();todo.add(left.keySet().iterator().next());
      while(!todo.isEmpty()){
        var at=todo.removeFirst();var state=left.remove(at);if(state==null)continue;group.put(at,state);
        for(var direction:Direction.values())if(left.containsKey(at.relative(direction)))todo.add(at.relative(direction));
        for(var linked:edges.getOrDefault(at,List.of()))if(left.containsKey(linked))todo.add(linked);
      }
      groups.add(group);
    }
    groups.sort(Comparator.<Map<BlockPos,BlockState>>comparingInt(Map::size).reversed());return groups;
  }
  /** Plan every care assembly before changing any block; never leave owned props in removed space. */
  private static boolean authoredPod(ServerLevel level,BlockPos at,BlockState state){
    if(!(FIXED_PODS.contains(at)||LEGACY_FIXED.contains(at))||!state.is(FruitContent.podForFruit("garden_fruit")))return false;
    Direction toward=state.getValue(HangingFruitBlock.FACING);
    return level.getBlockState(at.above()).is(LobbyDecor025.PALM_SUPPORT.get())&&level.getBlockState(at.relative(toward)).is(LobbyDecor025.PALM_TRUNK.get())
        &&level.getBlockState(at.relative(toward).above()).is(LobbyDecor025.PALM_CROWN.get());
  }
  private record CareMigration(Map<GardenRegistry.Place,GardenRegistry.Place> moves,Map<BlockPos,BlockState> props){}
  private static CareMigration prepareCareAssemblies(ServerLevel level,ScenePlan025 plan){
    var data=GardenRegistry.get(level.getServer());var props=new LinkedHashMap<BlockPos,BlockState>();
    for(var cursor:BlockPos.betweenClosed((int)BOUNDS.minX,(int)BOUNDS.minY,(int)BOUNDS.minZ,(int)BOUNDS.maxX-1,255,(int)BOUNDS.maxZ-1)){
      var at=cursor.immutable();var state=level.getBlockState(at);var place=GardenRegistry.Place.at(level,at);
      var next=plan.blocks.getOrDefault(at,Blocks.AIR.defaultBlockState());
      if(level.getBlockEntity(at)!=null&&!(level.getBlockEntity(at) instanceof com.chaogarden.fruit.FruitCrownEntity)&&!state.equals(next)&&!(level.getBlockEntity(at) instanceof LobbyDecor025.SceneEntity&&(state.is(LobbyDecor025.SCENE_ANCHOR.get())||state.is(LobbyDecor025.PALM_CROWN.get()))))throw new IllegalStateException("Unknown block entity requires a lossless upgrade: "+at);
      if(!placeable(state.getBlock())||authoredPod(level,at,state))continue;
      // Every authored part is scenery. A legacy root may become part0 at
      // the same registry key; conflicting purchases in its seven new cells
      // still enter the ordinary lossless relocation preflight below.
      if(GardenTransporterBlock.footprint(TRANSPORTER).contains(at)&&state.is(ActivityContent.GARDEN.get())&&GardenTransporterBlock.compatibleState(state,next))continue;
      if((at.equals(LEGACY_TRANSPORTER)||at.equals(PREVIOUS_TRANSPORTER))&&state.is(ActivityContent.GARDEN.get())&&!data.gardens.containsKey(place))continue;
      if(level.getBlockEntity(at)!=null&&!(level.getBlockEntity(at) instanceof com.chaogarden.fruit.FruitCrownEntity))throw new IllegalStateException("Unknown care block entity requires a lossless upgrade: "+at);
      props.put(at,state);
    }
    var groups=assemblies(props,data,level);var occupied=new HashSet<BlockPos>();var offsets=new LinkedHashMap<Map<BlockPos,BlockState>,BlockPos>();
    // Retain valid existing placements first, then fit the largest displaced
    // tree/line assemblies before smaller toys. This is a bounded deterministic
    // relocation, not item rewards or silently dropped course pieces.
    for(var group:groups)if(dryPlacement(plan,group,BlockPos.ZERO,occupied)){
      offsets.put(group,BlockPos.ZERO);occupied.addAll(group.keySet());
    }
    for(var group:groups)if(!offsets.containsKey(group)){
      int minY=group.keySet().stream().mapToInt(BlockPos::getY).min().orElseThrow();
      var first=group.keySet().stream().min(Comparator.<BlockPos>comparingInt(BlockPos::getY).thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ)).orElseThrow();
      boolean registered=group.keySet().stream().anyMatch(at->data.gardens.containsKey(GardenRegistry.Place.at(level,at)));
      BlockPos best=null;double distance=Double.POSITIVE_INFINITY;
      if(group.size()==1&&group.values().iterator().next().is(ActivityContent.GARDEN.get())&&data.gardens.containsKey(GardenRegistry.Place.at(level,first))){
        var preferred=TRANSPORTER.subtract(first);
        if(!props.containsKey(TRANSPORTER)&&dryPlacement(plan,group,preferred,occupied)
            &&data.canRelocateGarden(GardenRegistry.Place.at(level,first),GardenRegistry.Place.at(level,TRANSPORTER))){best=preferred;distance=-1;}
      }
      for(int x=-34;x<=19;x++)for(int z=-14;z<=14;z++){
        boolean wetGroup=group.values().stream().anyMatch(state->state.hasProperty(BlockStateProperties.WATERLOGGED)&&state.getValue(BlockStateProperties.WATERLOGGED));
        var offset=new BlockPos(x-first.getX(),(wetGroup?POND_LEVEL:surface(x,z)+1)-minY,z-first.getZ());
        double d=registered?first.offset(offset).distSqr(TRANSPORTER):offset.distSqr(BlockPos.ZERO);if(d>=distance||!dryPlacement(plan,group,offset,occupied))continue;
        if(group.keySet().stream().map(at->at.offset(offset)).anyMatch(props::containsKey))continue;
        // A registered provider may not merge with another garden/node/alias.
        boolean allowed=true;for(var e:group.entrySet())if(data.gardens.containsKey(GardenRegistry.Place.at(level,e.getKey()))
            &&!data.canRelocateGarden(GardenRegistry.Place.at(level,e.getKey()),GardenRegistry.Place.at(level,e.getKey().offset(offset)))){allowed=false;break;}
        if(allowed){best=offset;distance=d;}
      }
      if(best==null)throw new IllegalStateException("No lossless compact garden location for care assembly at "+first);
      offsets.put(group,best);for(var at:group.keySet())occupied.add(at.offset(best));
    }
    var moves=new LinkedHashMap<GardenRegistry.Place,GardenRegistry.Place>();
    for(var pair:offsets.entrySet())for(var e:pair.getKey().entrySet()){
      var from=e.getKey();var target=from.offset(pair.getValue());var state=e.getValue();var next=plan.blocks.getOrDefault(target,Blocks.AIR.defaultBlockState());
      // Only a canonical assembled root can replace a legacy1-cell provider.
      // Elsewhere its exact saved legacy state remains a1-cell purchased prop.
      plan.blocks.put(target,state.is(ActivityContent.GARDEN.get())&&GardenTransporterBlock.compatibleState(state,next)?next:state);
      if(!target.equals(from))moves.put(GardenRegistry.Place.at(level,from),GardenRegistry.Place.at(level,target));
    }
    long providerMoves=moves.keySet().stream().filter(data.gardens::containsKey).count();
    // Every existing alias is valid on save. Reserve capacity for the whole
    // transaction before the first provider or registration is touched.
    int aliases=data.save(new net.minecraft.nbt.CompoundTag(),level.registryAccess()).getList("Relocations",10).size();
    if(aliases+providerMoves>4096)throw new IllegalStateException("Insufficient lossless garden alias capacity for compact upgrade");
    for(var edge:data.links.entrySet())if(moves.containsKey(edge.getKey())||moves.containsKey(edge.getValue())){
      var from=moves.getOrDefault(edge.getKey(),edge.getKey());var to=moves.getOrDefault(edge.getValue(),edge.getValue());
      if(!from.dimension().equals(to.dimension())||!ActivityRules.linkLength(to.block().getX()-from.block().getX(),to.block().getY()-from.block().getY(),to.block().getZ()-from.block().getZ()))throw new IllegalStateException("No lossless compact course route for "+edge.getKey());
    }
    return new CareMigration(moves,props);
  }
  private static void commitCareAssemblies(ServerLevel level,CareMigration migration){
    var data=GardenRegistry.get(level.getServer());var moves=migration.moves;var props=migration.props;
    // Physically stage providers first. Old removal callbacks see already-moved
    // registration keys, while saved Garden Bells resolve their retained alias.
    for(var pair:moves.entrySet())if(data.gardens.containsKey(pair.getKey())){
      var state=props.get(pair.getKey().block());level.setBlock(pair.getValue().block(),state,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
      if(!data.relocateGarden(pair.getKey(),pair.getValue()))throw new IllegalStateException("Could not relocate staged garden provider "+pair.getKey());
    }
    // Preserve completed course ownership and graph before old blocks fire their
    // onRemove hooks; old physical positions must no longer name live nodes.
    var owners=new LinkedHashMap<>(data.nodeOwners);var links=new LinkedHashMap<>(data.links);
    data.nodeOwners.clear();owners.forEach((at,owner)->data.nodeOwners.put(moves.getOrDefault(at,at),owner));
    data.links.clear();links.forEach((from,to)->data.links.put(moves.getOrDefault(from,from),moves.getOrDefault(to,to)));data.setDirty();
    // Remove all old fruit first, then crowns, then the remaining moved care
    // blocks. A crown's onRemove cannot find/drop either old or newly placed pods.
    for(int phase=0;phase<3;phase++)for(var old:moves.keySet()){
      var state=props.get(old.block());int kind=state.getBlock() instanceof HangingFruitBlock?0:state.getBlock() instanceof ChaoTreeCrownBlock?1:2;
      if(kind==phase)level.setBlock(old.block(),Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
    }
  }
  private static boolean authoredTerrainCollision(ServerLevel level,Entity entity){
    var box=entity.getBoundingBox().deflate(1.0e-6);
    for(var pos:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ))){
      var state=level.getBlockState(pos);
      if((state.is(LobbyDecor025.CLIFF.get())||state.is(LobbyDecor025.TURF.get())||state.is(LobbyDecor025.SAND.get())||state.is(NeutralGardenDecor02510.CASCADE_ROCK.get()))&&box.intersects(new AABB(pos)))return true;
    }
    return false;
  }
  /** Entity sections may publish after the bounded terrain rewrite completes. */
  @SubscribeEvent public static void rescueLatePublication(EntityTickEvent.Post event){
    var entity=event.getEntity();
    if(entity.isRemoved()||entity.tickCount%10!=0||!(entity.level() instanceof ServerLevel level)||!level.dimension().equals(HubContent.GARDEN)
        ||!eligibleBody(entity)||protectedActivity(entity))return;
    if(insideField(entity.blockPosition().getX(),entity.blockPosition().getZ())&&!authoredTerrainCollision(level,entity))return;
    rescueBody(level,entity);
  }
  /** Shared with the global out-of-bounds service; never stack bodies at a fixed spawn. */
  public static boolean rescueBody(ServerLevel level,Entity entity){
    if(entity.level()!=level||!level.dimension().equals(HubContent.GARDEN)||!eligibleBody(entity)||entity.isRemoved()||protectedActivity(entity))return false;
    var safe=safeLawn(level,entity);return safe!=null&&moveBody(level,entity,safe);
  }
  private static boolean moveBody(ServerLevel level,Entity entity,Vec3 at){
    if(entity instanceof ServerPlayer player)player.teleportTo(level,at.x,at.y,at.z,java.util.Set.of(),player.getYRot(),player.getXRot());
    else entity.teleportTo(at.x,at.y,at.z);
    if(entity.isRemoved()||entity.level()!=level||entity.position().distanceToSqr(at)>1.e-8)return false;
    entity.setDeltaMovement(Vec3.ZERO);entity.fallDistance=0;
    if(entity instanceof ChaoEntity chao)GardenRegistry.get(level.getServer()).update(chao);return true;
  }
  private static Vec3 safeLawn(ServerLevel level,Entity entity){
    return safeLawn((BlockGetter)level,entity,java.util.List.of());
  }
  private static Vec3 safeLawn(BlockGetter level,Entity entity,java.util.List<AABB> reserved){
    Vec3 best=null;double distance=Double.POSITIVE_INFINITY;
    for(int x=-7;x<=18;x++)for(int z=-11;z<=14;z++){
      int y=surface(x,z)+1;var pos=new BlockPos(x,y,z);var floor=pos.below();
      if(!insideField(x,z)||!level.getBlockState(floor).is(LobbyDecor025.TURF.get())||!level.getFluidState(pos).isEmpty())continue;
      var candidate=new Vec3(x+.5,y,z+.5);var box=entity.getBoundingBox().move(candidate.subtract(entity.position()));
      double d=entity.position().distanceToSqr(candidate);if(d>=distance)continue;
      var clearance=box.inflate(.02);
      // Vanilla entity collision is asymmetric: an egg can block a keeper,
      // while that keeper does not block the egg. Explicitly reserve body space
      // rather than allowing a later egg to invalidate an earlier landing.
      if(reserved.stream().anyMatch(clearance::intersects))continue;
      if(level instanceof ServerLevel live&&!live.getEntities(entity,clearance,other->!other.isRemoved()&&eligibleBody(other)).isEmpty())continue;
      if(clear(level,entity,candidate)){best=candidate;distance=d;}
    }
    return best;
  }
  public static ScenePlan025 plan(){var p=new ScenePlan025();
    for(int x=-39;x<=24;x++)for(int z=-22;z<=18;z++){
      if(!insideField(x,z))continue;
      int top=surface(x,z);boolean lake=pondCell(x,z),shore=pondDistance(x,z)<1.14&&x>=-25;
      if(lake)top=pondDistance(x,z)>.60?78:77;else if(shore)top=79;
      boolean rock=top>82||waterfallMass(x,z);
      var stone=waterfallMass(x,z)||waterfallBank(x,z)?NeutralGardenDecor02510.CASCADE_ROCK.get():LobbyDecor025.CLIFF.get();
      for(int y=68;y<=top;y++)p.put(x,y,z,y==top?(lake||shore?LobbyDecor025.SAND.get():rock?stone:LobbyDecor025.TURF.get()):y>=80?stone:LobbyDecor025.CLIFF.get());
      if(lake)for(int y=top+1;y<=POND_LEVEL;y++)p.put(x,y,z,Blocks.WATER);
      // Thin crest grass belongs on the jagged main cliff, not every waterfall step.
      if(rock&&!waterfallMass(x,z))p.put(x,top,z,LobbyDecor025.TURF.get());
    }
    waterfall(p);
    waterfallPlants(p);
    // Keep the established arrival/care walk on level grass. The rotated
    // pond's one-cell sand rim reaches this otherwise dry eastern approach.
    for(int z=-12;z<=-6;z++)p.put(5,79,z,LobbyDecor025.TURF.get());
    // Only the two mountain cells behind the mouth form the throat. Three
    // blocks of clearance span its width; the centre alone rises one higher.
    for(int x=4;x<=6;x++)for(int z=-18;z<=-17;z++){
      p.put(x,79,z,LobbyDecor025.TURF.get());
      for(int y=80;y<=82;y++)p.put(x,y,z,Blocks.AIR);
      p.put(x,83,z,x==5?Blocks.AIR:LobbyDecor025.CLIFF.get());
      p.put(x,84,z,LobbyDecor025.CLIFF.get());
    }
    // A plain 3x4 animated portal sits behind the rock silhouette. No quartz
    // rim, threshold or model frame is rendered for this garden tunnel.
    PortalScenes0251.place(p,EXIT_PORTAL,Direction.SOUTH,PortalScenes0251.TUNNEL);
    p.box(4,80,-19,6,84,-19,LobbyDecor025.CLIFF.get());
    p.box(16,80,8,18,80,10,LobbyDecor025.TURF.get());
    GardenTransporterBlock.putScene(p,TRANSPORTER,Direction.NORTH);
    for(var tree:PALMS)palm(p,tree[0],tree[1],tree[2]);
    int[][] flowers={{1,-12},{8,-12},{12,-9},{-4,-11},{-7,-4},{-8,7},{1,12},{9,3},{12,12},{17,3},{17,12},{-13,12},{-20,9}};
    for(int i=0;i<flowers.length;i++)for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)if((dx+dz+i)%2==0){
      int x=flowers[i][0]+dx,z=flowers[i][1]+dz,y=surface(x,z)+1;var floor=new BlockPos(x,y-1,z);var at=floor.above();
      if(y<=82&&p.blocks.getOrDefault(floor,Blocks.AIR.defaultBlockState()).is(LobbyDecor025.TURF.get())&&p.blocks.getOrDefault(at,Blocks.AIR.defaultBlockState()).isAir())
        p.put(x,y,z,i%3==0?Blocks.BLUE_ORCHID:i%3==1?Blocks.ALLIUM:Blocks.PINK_TULIP);
    }
    // Invisible physical limits follow the floating lawn edge. A two-cell ring
    // and high cap contain babies, dropped items and elytra flight alike.
    for(int x=-41;x<=26;x++)for(int z=-24;z<=20;z++){
      boolean inside=insideField(x,z),rim=false;
      if(!inside)for(int dx=-2;dx<=2&&!rim;dx++)for(int dz=-2;dz<=2;dz++)if(insideField(x+dx,z+dz)){rim=true;break;}
      if(rim)for(int y=67;y<=255;y++)p.put(x,y,z,Blocks.BARRIER);
      if(inside||rim)p.put(x,255,z,Blocks.BARRIER);
    }
    return p;
  }
  private static void waterfall(ScenePlan025 p){
    // Keep the quarter-turned feature's exact source-water geometry, moved
    // another fifteen blocks west. It still descends east into its receiving
    // pool, perpendicular to the unchanged north-wall exit tunnel.
    for(int step=0;step<4;step++){
      int front=WATERFALL_FRONTS[step],top=WATERFALL_LEVELS[step],lower=step==0?POND_LEVEL:top-3;
      for(int x=front-2;x<=front;x++){
        for(int z=-7;z<=-4;z++){
          for(int y=68;y<top;y++)p.put(x,y,z,y>=80?NeutralGardenDecor02510.CASCADE_ROCK.get():LobbyDecor025.CLIFF.get());
          p.put(x,top,z,Blocks.WATER);
        }
        for(int side:new int[]{-8,-3})for(int y=68;y<=top;y++)p.put(x,y,side,y>=80?NeutralGardenDecor02510.CASCADE_ROCK.get():LobbyDecor025.CLIFF.get());
      }
      for(int z=-7;z<=-4;z++)for(int y=lower;y<=top;y++)p.put(front,y,z,Blocks.WATER);
    }
    p.box(-38,68,-8,-38,79,-3,LobbyDecor025.CLIFF.get());
    p.box(-38,80,-8,-38,92,-3,NeutralGardenDecor02510.CASCADE_ROCK.get());
  }
  public static final int[][] WATERFALL_PLANTS={{-27,83,-3},{-30,86,-8},{-33,89,-3},{-36,92,-8},
      {-27,84,1},{-24,80,3},{-17,80,0},{-10,80,-11},{-31,surface(-31,3)+1,3},
      {-33,surface(-33,2)+1,2},{-29,surface(-29,2)+1,2},{-34,surface(-34,4)+1,4},
      {-28,surface(-28,4)+1,4},{-32,surface(-32,5)+1,5},{-30,surface(-30,6)+1,6},
      {-33,surface(-33,7)+1,7},{-27,surface(-27,6)+1,6},{-29,surface(-29,7)+1,7}};
  private static void waterfallPlants(ScenePlan025 p){
    for(var xyz:WATERFALL_PLANTS){
      var at=new BlockPos(xyz[0],xyz[1],xyz[2]);var below=p.blocks.getOrDefault(at.below(),Blocks.AIR.defaultBlockState());
      if(!(below.is(LobbyDecor025.TURF.get())||below.is(LobbyDecor025.SAND.get())||below.is(NeutralGardenDecor02510.CASCADE_ROCK.get()))
          ||!p.blocks.getOrDefault(at,Blocks.AIR.defaultBlockState()).isAir())
        throw new IllegalStateException("Neutral broadleaf lacks a dry terrace: "+at);
      p.put(at.getX(),at.getY(),at.getZ(),NeutralGardenDecor02510.BROADLEAF.get());
    }
  }
  private static void palm(ScenePlan025 p,int x,int z,int height){
    int base=surface(x,z)+1,crown=base+height;
    var ground=new BlockPos(x,base-1,z);
    if(!p.blocks.getOrDefault(ground,Blocks.AIR.defaultBlockState()).is(LobbyDecor025.TURF.get())||WATERFALL_BOUNDS.inflate(3,0,3).contains(x+.5,base,z+.5))throw new IllegalStateException("Palm lacks dry lawn clearance: "+ground);
    for(int y=base;y<crown;y++)p.put(x,y,z,LobbyDecor025.PALM_TRUNK.get());p.put(x,crown,z,LobbyDecor025.PALM_CROWN.get());
    for(Direction side:Direction.Plane.HORIZONTAL){var support=new BlockPos(x,crown,z).relative(side);p.put(support.getX(),support.getY(),support.getZ(),LobbyDecor025.PALM_SUPPORT.get());
      var pod=support.below();p.put(pod.getX(),pod.getY(),pod.getZ(),FruitContent.podForFruit("garden_fruit").defaultBlockState().setValue(HangingFruitBlock.FACING,side.getOpposite()).setValue(HangingFruitBlock.AGE,3));}
  }
}
