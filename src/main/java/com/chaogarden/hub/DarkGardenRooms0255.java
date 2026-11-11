package com.chaogarden.hub;

import com.chaogarden.activities.ActivityBlock;
import com.chaogarden.activities.ActivityContent;
import com.chaogarden.activities.ActivityManager;
import com.chaogarden.activities.GardenRegistry;
import com.chaogarden.activities.GardenTransporterBlock;
import com.chaogarden.entity.ChaoEggEntity;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.fruit.FruitContent;
import com.chaogarden.fruit.HangingFruitBlock;
import com.chaogarden.fruit.ChaoSaplingBlock;
import com.chaogarden.fruit.ChaoTreeCrownBlock;
import com.chaogarden.toys.ChaoToyBlock;
import java.util.ArrayList;
import java.util.HashSet;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** First playable Battle-inspired Dark Garden: pale shore, red pool and distant carved crag. */
@EventBusSubscriber(modid="chao_garden")
public final class DarkGardenRooms0255 {
  public static final AABB BOUNDS=new AABB(-28,60,-46,29,256,25);
  public static final AABB ACTIVE_BOUNDS=new AABB(-26,66,-22,27,256,23);
  public static final Vec3 SPAWN=new Vec3(-18.5,80.1,8.5);
  public static final float SPAWN_YAW=-125;
  public static final BlockPos EXIT_PORTAL=new BlockPos(-22,80,8);
  public static final Direction EXIT_FACING=Direction.EAST;
  public static final int EXIT_STYLE=PortalScenes0251.TUNNEL;
  public static final BlockPos TRANSPORTER=new BlockPos(-15,80,12);
  public static final int POOL_LEVEL=79;
  public static final BlockPos WATERFALL=new BlockPos(10,88,-16);
  public static final BlockPos DISTANT_MASK=new BlockPos(0,106,-33);
  public static final int[][] FRUIT_TREES={{-11,5,5},{16,10,4},{-12,-8,4}};
  public static final int FRUIT_POD_Y=82,FRUIT_BRANCH_Y=83;
  public static final int[][] GRAVES={{-4,-12},{15,-10},{18,0},{14,10},{-17,-2}};
  private static final Set<BlockPos> FIXED_PODS=new HashSet<>();
  static{for(var tree:FRUIT_TREES)for(Direction d:Direction.Plane.HORIZONTAL)FIXED_PODS.add(new BlockPos(tree[0],FRUIT_POD_Y,tree[1]).relative(d));}
  private DarkGardenRooms0255(){}
  private static Block rock(){return DarkGardenDecor0255.block("dark_garden_rock");}
  private static Block soil(){return DarkGardenDecor0255.block("dark_garden_soil");}
  public static boolean insideField(int x,int z){return x*x/(24.0*24)+z*z/(20.0*20)<=1
      ||(x>=-23&&x<=-18&&z>=6&&z<=10);}
  public static double poolDistance(int x,int z){return Math.pow((x-6)/10.0,2)+Math.pow((z+3)/9.0,2);}
  public static boolean poolCell(int x,int z){
    return insideField(x,z)&&((x>=9&&x<=11&&z>=-16&&z<=-10)
        ||poolDistance(x,z)<1+.08*Math.sin(x*.8)+.06*Math.cos(z*1.1));
  }
  public static int surface(int x,int z){
    if(poolCell(x,z))return 77;
    double q=x*x/(24.0*24)+z*z/(20.0*20);
    if(q>.82)return 87+(int)Math.round(2*Math.sin(x*.63)+2*Math.cos(z*.51))+Math.min(7,(int)((q-.82)*37));
    return 79;
  }
  public static boolean breakable(BlockPos pos,BlockState state){return GardenRooms025.placeable(state.getBlock())
      &&!(GardenTransporterBlock.footprint(TRANSPORTER).contains(pos)&&state.is(ActivityContent.GARDEN.get()))&&!FIXED_PODS.contains(pos);}
  public static ScenePlan025 plan(){
    var p=new ScenePlan025();
    for(int x=-24;x<=24;x++)for(int z=-20;z<=20;z++)if(insideField(x,z)){
      int top=surface(x,z);boolean pool=poolCell(x,z);
      for(int y=68;y<=top;y++)p.put(x,y,z,y==top&&!pool&&top==79?soil():rock());
      if(pool)for(int y=top+1;y<=POOL_LEVEL;y++)p.put(x,y,z,Blocks.WATER);
    }
    // The cliff fall and feeding channel are real water; the red biome tint is
    // shared with the pool in this initial native-fluid pass.
    p.box(8,80,-17,12,91,-17,rock());
    p.box(8,80,-16,8,90,-16,rock());p.box(12,80,-16,12,90,-16,rock());
    p.box(9,79,-16,11,89,-16,Blocks.WATER);p.box(9,90,-17,11,90,-16,rock());
    // A two-deep plain rock tunnel provides the separate west return route.
    for(int x=-22;x<=-18;x++)for(int z=7;z<=9;z++){
      p.put(x,79,z,soil());for(int y=80;y<=83;y++)p.put(x,y,z,Blocks.AIR);
      if(x<=-21)p.put(x,84,z,rock());
    }
    p.box(-23,80,6,-23,85,10,rock());
    PortalScenes0251.place(p,EXIT_PORTAL,EXIT_FACING,EXIT_STYLE);
    for(int x=-19;x<=-12;x++)for(int z=7;z<=10;z++){
      p.put(x,79,z,soil());for(int y=80;y<=83;y++)p.put(x,y,z,Blocks.AIR);
    }
    p.box(TRANSPORTER.getX(),79,TRANSPORTER.getZ(),TRANSPORTER.getX()+1,79,TRANSPORTER.getZ()+1,soil());
    GardenTransporterBlock.putScene(p,TRANSPORTER,Direction.NORTH);
    for(var tree:FRUIT_TREES)bareFruitTree(p,tree[0],tree[1],tree[2]);
    for(var grave:GRAVES){
      int y=surface(grave[0],grave[1])+1;
      p.put(grave[0],y,grave[1],DarkGardenDecor0255.block("dark_garden_gravestone").defaultBlockState()
          .setValue(DarkGardenDecor0255.Gravestone.FACING,grave[0]>0?Direction.WEST:Direction.EAST));
    }
    // Low, engraved grave-seat on the far bank: it is scenery, not a second portal.
    p.box(-4,80,-14,0,80,-13,rock());
    p.put(-2,81,-14,DarkGardenDecor0255.block("dark_garden_gravestone"));
    crestFence(p,-14,-16,5);crestFence(p,17,11,4);
    distantCrag(p);
    // Physical containment remains invisible above the irregular native cliffs.
    for(int x=-26;x<=26;x++)for(int z=-22;z<=22;z++){
      boolean inside=insideField(x,z),rim=false;
      if(!inside)for(int dx=-2;dx<=2&&!rim;dx++)for(int dz=-2;dz<=2;dz++)if(insideField(x+dx,z+dz)){rim=true;break;}
      if(rim)for(int y=67;y<=255;y++)p.put(x,y,z,Blocks.BARRIER);
      if(inside||rim)p.put(x,255,z,Blocks.BARRIER);
    }
    return p;
  }
  private static void bareFruitTree(ScenePlan025 p,int x,int z,int height){
    p.put(x,79,z,soil());
    var wood=DarkGardenDecor0255.block("dark_garden_deadwood").defaultBlockState();
    p.box(x,80,z,x,80+height,z,wood);
    var joint=wood.setValue(DarkGardenDecor0255.Deadwood.BRANCHED,true);
    p.put(x,83+height/2,z,joint);p.put(x+1,83+height/2,z,joint);p.put(x+1,84+height/2,z,wood);
    p.put(x,82+height/2,z,joint);p.put(x-1,82+height/2,z,joint);p.put(x-1,83+height/2,z,wood);
    // Extend the rooted stem and raise every branch and fruit by one block.
    // The native fruit stem still meets the underside of its visible arm.
    p.put(x,FRUIT_BRANCH_Y,z,joint);
    for(Direction d:Direction.Plane.HORIZONTAL){
      var pod=new BlockPos(x,FRUIT_POD_Y,z).relative(d);
      p.put(pod.getX(),FRUIT_BRANCH_Y,pod.getZ(),wood.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS,d.getAxis()));
      p.put(pod.getX(),FRUIT_POD_Y,pod.getZ(),FruitContent.podForFruit("dark_garden_fruit").defaultBlockState().setValue(HangingFruitBlock.FACING,d.getOpposite()).setValue(HangingFruitBlock.AGE,3));
    }
  }
  private static void crestFence(ScenePlan025 p,int x0,int z,int length){
    int y=0;for(int x=x0;x<x0+length;x++)y=Math.max(y,surface(x,z)+1);
    for(int x=x0;x<x0+length;x++){
      p.box(x,80,z,x,y-1,z,rock());
      var fence=DarkGardenDecor0255.block("dark_garden_fence").defaultBlockState()
          .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WEST,x>x0)
          .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.EAST,x<x0+length-1);
      p.put(x,y,z,fence);
    }
  }
  private static void distantCrag(ScenePlan025 p){
    var black=DarkGardenDecor0255.block("dark_garden_effigy").defaultBlockState();
    for(int y=63;y<=113;y++){
      int radius=Math.max(1,6-(int)Math.floor((y-82)/6.0));radius=Math.min(7,radius);
      for(int x=-radius;x<=radius;x++)for(int z=-39;z<=-33;z++)
        if(Math.abs(x)+Math.abs(z+36)<=radius+2)p.put(x,y,z,black);
    }
    for(int side:new int[]{-1,1})for(int y=108;y<=119;y++){
      int x=side*(3+(y-108)/5);p.put(x,y,-35,black);if(y<116)p.put(x,y,-36,black);
      if(y>108&&((y-108)%5)==0)p.put(x,y-1,-35,black);
    }
    for(int row=0;row<3;row++)for(int col=0;col<3;col++)p.put(col-1,107-row,-32,
        black.setValue(DarkGardenDecor0255.Effigy.TILE,1+row*3+col).setValue(DarkGardenDecor0255.Effigy.FACING,Direction.SOUTH));
    // Three sharply stepped neighbouring peaks create a remote crag silhouette.
    for(int[] peak:new int[][]{{-9,-36,102},{9,-37,108},{5,-41,98}})for(int y=65;y<=peak[2];y++){
      int r=Math.max(0,(peak[2]-y)/10);for(int x=peak[0]-r;x<=peak[0]+r;x++)for(int z=peak[1]-r;z<=peak[1]+r;z++)
        if(Math.abs(x-peak[0])+Math.abs(z-peak[1])<=r)p.put(x,y,z,black);
    }
  }

  private static boolean eligible(Entity e){return e instanceof ServerPlayer||e instanceof ChaoEntity||e instanceof ChaoEggEntity||e instanceof ItemEntity;}
  private static boolean busy(Entity e){return e instanceof ChaoEntity c&&(GardenRegistry.managed(c)||ActivityManager.isBusy(c));}
  private static final class PlannedWorld implements BlockGetter {
    final ServerLevel source;final Map<BlockPos,BlockState> blocks;
    PlannedWorld(ServerLevel source,Map<BlockPos,BlockState> blocks){this.source=source;this.blocks=blocks;}
    @Override public BlockEntity getBlockEntity(BlockPos at){return null;}
    @Override public BlockState getBlockState(BlockPos at){return blocks.getOrDefault(at,Blocks.AIR.defaultBlockState());}
    @Override public FluidState getFluidState(BlockPos at){return getBlockState(at).getFluidState();}
    @Override public int getHeight(){return source.getHeight();}
    @Override public int getMinBuildHeight(){return source.getMinBuildHeight();}
  }
  private static boolean clear(BlockGetter world,Entity e,Vec3 at){
    var box=e.getBoundingBox().move(at.subtract(e.position())).deflate(1.e-6);var scan=box.inflate(1);
    for(var p:BlockPos.betweenClosed(BlockPos.containing(scan.minX,scan.minY,scan.minZ),BlockPos.containing(scan.maxX,scan.maxY,scan.maxZ)))
      for(var part:world.getBlockState(p).getCollisionShape(world,p,CollisionContext.of(e)).toAabbs())if(box.intersects(part.move(p)))return false;
    return true;
  }
  private static Vec3 safeLawn(BlockGetter world,Entity e,List<AABB> reserved){
    Vec3 best=null;double bestDistance=Double.POSITIVE_INFINITY;
    for(int x=-21;x<=21;x++)for(int z=-17;z<=17;z++){
      if(!insideField(x,z))continue;var floor=new BlockPos(x,surface(x,z),z);var at=new Vec3(x+.5,floor.getY()+1,z+.5);
      if(!world.getBlockState(floor).is(DarkGardenDecor0255.block("dark_garden_soil"))||!world.getFluidState(floor.above()).isEmpty())continue;
      var box=e.getBoundingBox().move(at.subtract(e.position())).inflate(.02);
      if(reserved.stream().anyMatch(box::intersects))continue;
      if(world instanceof ServerLevel live&&!live.getEntities(e,box,other->eligible(other)&&!other.isRemoved()).isEmpty())continue;
      double distance=at.distanceToSqr(e.position());if(distance>=bestDistance||!clear(world,e,at))continue;
      best=at;bestDistance=distance;
    }
    return best;
  }
  /** Stage only the exact former authored pods; a rejected update writes nothing. */
  private static Set<BlockPos> stageRaisedFruit(ServerLevel level,ScenePlan025 plan){
    var moved=new HashSet<BlockPos>();var fruit=FruitContent.podForFruit("dark_garden_fruit");
    var deadwood=DarkGardenDecor0255.block("dark_garden_deadwood");
    for(var tree:FRUIT_TREES)for(Direction side:Direction.Plane.HORIZONTAL){
      var oldAt=new BlockPos(tree[0],81,tree[1]).relative(side);var old=level.getBlockState(oldAt);
      if(!old.is(fruit))continue;
      var target=oldAt.above();var arm=level.getBlockState(target);
      var joint=level.getBlockState(new BlockPos(tree[0],82,tree[1]));
      if(level.getBlockEntity(oldAt)!=null||level.getBlockEntity(target)!=null
          ||!arm.is(deadwood)||arm.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS)!=side.getAxis()
          ||arm.getValue(DarkGardenDecor0255.Deadwood.BRANCHED)||!joint.is(deadwood)
          ||!joint.getValue(DarkGardenDecor0255.Deadwood.BRANCHED)||old.getValue(HangingFruitBlock.FACING)!=side.getOpposite())
        throw new IllegalStateException("Dark fruit height update awaits the former authored branch at "+oldAt);
      plan.blocks.put(target,old);plan.blocks.put(oldAt,Blocks.AIR.defaultBlockState());moved.add(oldAt);
    }
    return moved;
  }
  /** Preflight purchased care props and actors before any terrain write. */
  public static void ensure(ServerLevel level){
    if(!level.dimension().equals(HubContent.DARK_GARDEN))throw new IllegalArgumentException("Dark garden has its own dimension");
    var p=plan();var preserve=new HashSet<BlockPos>();
    var raisedPods=stageRaisedFruit(level,p);
    // Only the authored old single-cell terminal is eligible for expansion.
    // Reject an owned neighbor or any block entity before touching terrain,
    // retaining the root coordinate and therefore every roster/Bell key.
    if(!GardenTransporterBlock.canMergeScene(level,TRANSPORTER,(at,old)->
        old.equals(p.blocks.get(at))||old.isAir()||old.is(Blocks.WATER)
            ||old.is(DarkGardenDecor0255.block("dark_garden_soil"))||old.is(DarkGardenDecor0255.block("dark_garden_rock"))))
      throw new IllegalStateException("Dark transporter update awaits its complete two-by-two footprint at "+TRANSPORTER);
    for(var at:BlockPos.betweenClosed((int)BOUNDS.minX,(int)BOUNDS.minY,(int)BOUNDS.minZ,(int)BOUNDS.maxX-1,(int)BOUNDS.maxY-1,(int)BOUNDS.maxZ-1)){
      var old=level.getBlockState(at);var next=p.blocks.getOrDefault(at,Blocks.AIR.defaultBlockState());if(old.equals(next))continue;
      if(raisedPods.contains(at))continue;
      if(at.equals(TRANSPORTER)&&GardenTransporterBlock.compatibleState(old,next))continue;
      // Harvested native fruit keeps its present growth stage when the finite
      // scene is prepared again; the authored bare tree is still the same provider.
      if(FIXED_PODS.contains(at)&&old.getBlock()==next.getBlock()&&old.getBlock() instanceof HangingFruitBlock){
        preserve.add(at.immutable());p.blocks.put(at.immutable(),old);continue;
      }
      if(GardenRooms025.placeable(old.getBlock())){
        if(!next.isAir()&&!next.is(Blocks.WATER))throw new IllegalStateException("Dark update awaits care prop clearance at "+at);
        if(old.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)
            &&old.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)
            &&(!next.is(Blocks.WATER)||!next.getFluidState().isSource()))throw new IllegalStateException("Dark update would dry an owned water toy at "+at);
        preserve.add(at.immutable());p.blocks.put(at.immutable(),old);
      }else if(level.getBlockEntity(at)!=null&&!(level.getBlockEntity(at) instanceof LobbyDecor025.SceneEntity))
        throw new IllegalStateException("Dark update awaits unsupported block entity at "+at);
    }
    var planned=new PlannedWorld(level,p.blocks);
    for(var at:preserve){
      var old=p.blocks.get(at);var below=planned.getBlockState(at.below());var block=old.getBlock();
      boolean wet=old.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)&&old.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED);
      boolean activityRoot=block instanceof ActivityBlock&&(!(block instanceof GardenTransporterBlock)
          ||GardenTransporterBlock.legacy(old)||old.getValue(GardenTransporterBlock.PART)<4);
      boolean root=activityRoot||block instanceof ChaoToyBlock||block instanceof ChaoSaplingBlock
          ||(block==FruitContent.TRUNK.get()&&old.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS)==Direction.Axis.Y&&!preserve.contains(at.below()))
          ||(block instanceof ChaoTreeCrownBlock&&!preserve.contains(at.below()));
      if(root&&!wet&&!preserve.contains(at.below())&&!below.isFaceSturdy(planned,at.below(),Direction.UP))throw new IllegalStateException("Dark update would leave a care device unsupported at "+at);
    }
    var actors=level.getEntitiesOfClass(Entity.class,BOUNDS,DarkGardenRooms0255::eligible);
    var reserved=new ArrayList<AABB>();var displaced=new ArrayList<Entity>();var moves=new LinkedHashMap<Entity,Vec3>();
    for(var e:actors){
      var feet=e.blockPosition();
      boolean newlyWet=(!planned.getFluidState(feet).isEmpty()&&level.getFluidState(feet).isEmpty())
          ||(!planned.getFluidState(feet.below()).isEmpty()&&level.getFluidState(feet.below()).isEmpty());
      if(insideField(e.blockPosition().getX(),e.blockPosition().getZ())&&clear(planned,e,e.position())&&!newlyWet)reserved.add(e.getBoundingBox().inflate(.02));
      else{if(busy(e))throw new IllegalStateException("Dark update awaits active activity for "+e.getUUID());displaced.add(e);}
    }
    for(var e:displaced){var at=safeLawn(planned,e,reserved);if(at==null)throw new IllegalStateException("No clear Dark Garden landing for "+e.getUUID());moves.put(e,at);reserved.add(e.getBoundingBox().move(at.subtract(e.position())).inflate(.02));}
    p.apply(level,BOUNDS,(at,state)->preserve.contains(at));
    for(var move:moves.entrySet())if(!moveBody(level,move.getKey(),move.getValue()))throw new IllegalStateException("Dark body did not reach its preflighted landing");
  }
  private static boolean moveBody(ServerLevel level,Entity e,Vec3 at){
    if(e instanceof ServerPlayer player)player.teleportTo(level,at.x,at.y,at.z,Set.of(),player.getYRot(),player.getXRot());else e.teleportTo(at.x,at.y,at.z);
    if(e.isRemoved()||e.level()!=level||e.position().distanceToSqr(at)>1.e-8)return false;
    e.setDeltaMovement(Vec3.ZERO);e.fallDistance=0;if(e instanceof ChaoEntity c)GardenRegistry.get(level.getServer()).update(c);return true;
  }
  public static boolean rescueBody(ServerLevel level,Entity e){
    if(e.level()!=level||!level.dimension().equals(HubContent.DARK_GARDEN)||!eligible(e)||e.isRemoved()||busy(e))return false;
    var at=safeLawn(level,e,List.of());return at!=null&&moveBody(level,e,at);
  }
  /** Saved entity sections may arrive after terrain preparation; rescue them without replacing identity. */
  @SubscribeEvent public static void rescueLatePublication(EntityTickEvent.Post event){
    var e=event.getEntity();if(e.isRemoved()||e.tickCount%10!=0||!(e.level() instanceof ServerLevel level)
        ||!level.dimension().equals(HubContent.DARK_GARDEN)||!eligible(e)||busy(e))return;
    boolean unsafe=!insideField(e.blockPosition().getX(),e.blockPosition().getZ())||e.getY()<69;
    var box=e.getBoundingBox().deflate(1.e-6);
    for(var at:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ))){
      var state=level.getBlockState(at);var block=state.getBlock();
      if(!(state.is(DarkGardenDecor0255.block("dark_garden_rock"))||state.is(DarkGardenDecor0255.block("dark_garden_soil"))
          ||block==DarkGardenDecor0255.block("dark_garden_deadwood")||block==DarkGardenDecor0255.block("dark_garden_gravestone")
          ||block==DarkGardenDecor0255.block("dark_garden_fence")||block==DarkGardenDecor0255.block("dark_garden_effigy")))continue;
      for(var part:state.getCollisionShape(level,at,CollisionContext.of(e)).toAabbs())if(box.intersects(part.move(at)))unsafe=true;
    }
    if(unsafe)rescueBody(level,e);
  }
}
