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

/** A first playable SA2/Battle Hero Garden: pool-left, open lawn-right, pink cloud horizon. */
@EventBusSubscriber(modid="chao_garden")
public final class HeroGardenRooms0253 {
  public static final AABB BOUNDS=new AABB(-32,66,-45,33,256,30);
  public static final AABB ACTIVE_BOUNDS=new AABB(-30,66,-28,31,256,28);
  public static final Vec3 SPAWN=new Vec3(18.5,80.1,8.5);
  public static final float SPAWN_YAW=115;
  public static final BlockPos EXIT_PORTAL=new BlockPos(24,80,8);
  public static final Direction EXIT_FACING=Direction.WEST;
  public static final BlockPos TRANSPORTER=new BlockPos(0,80,-20);
  public static final BlockPos FOUNTAIN_EMBLEM=new BlockPos(-23,86,-4);
  public static final BlockPos PAVILION_CENTER=new BlockPos(-8,82,10);
  public static final BlockPos BELL_ISLAND=new BlockPos(-9,78,-38);
  public static final int POOL_LEVEL=79;
  public static final int[][] PALMS={{4,6,4},{12,-4,4},{-1,-13,3},{14,15,4}};
  private static final Set<BlockPos> FIXED_PODS=new HashSet<>();
  static {for(var tree:PALMS)for(Direction d:Direction.Plane.HORIZONTAL)FIXED_PODS.add(new BlockPos(tree[0],surface(tree[0],tree[1])+tree[2],tree[1]).relative(d));}
  private HeroGardenRooms0253(){}

  public static boolean insideField(int x,int z){return Math.pow(x/28.0,4)+Math.pow(z/25.0,4)<=1;}
  public static double poolDistance(int x,int z){return Math.pow((x+15)/8.0,2)+Math.pow((z+3)/11.0,2);}
  /** Final open water surface; three ellipse cells sit under the pavilion's solid shore plinth. */
  public static boolean poolCell(int x,int z){return poolDistance(x,z)<1&&!(x>=-12&&x<=-4&&z>=6&&z<=14);}
  public static boolean poolBorder(int x,int z){
    if(poolDistance(x,z)<1)return false;
    if(poolDistance(x,z)<1.20)return true;
    // A continuous circle can skip a cardinal neighbour after voxelisation.
    // Include every land cell touching the original water outline.
    for(Direction side:Direction.Plane.HORIZONTAL)
      if(poolDistance(x+side.getStepX(),z+side.getStepZ())<1)return true;
    return false;
  }
  public static int surface(int x,int z){
    int height=79;
    // Source grass includes low sloped shoulders; two walkable terraces soften
    // the cliff foot without changing the pool, pavilion or gate approach.
    if(x>=19&&x<=21&&(z<=-6||z>=16))height=79+Math.min(2,x-18);
    // Two separate rock shoulders frame an OPEN northern cloud gap.
    int west=-23+(int)Math.round(.7*Math.sin(z*.27));
    if(x<=west){height=Math.max(height,88+(int)Math.round(2*Math.sin(z*.19)+1.5*Math.cos(z*.41))+Math.min(5,west-x));}
    int east=22+(int)Math.round(.8*Math.cos(z*.24));
    if(x>=east){height=Math.max(height,88+(int)Math.round(2*Math.cos(z*.18)+1.7*Math.sin(z*.43))+Math.min(5,x-east));}
    if(z<=-18&&Math.abs(x)>12)height=Math.max(height,86+Math.min(6,(-z-18)/2)+(int)Math.round(Math.abs(x)/9.0));
    return Math.min(97,height);
  }
  public static boolean breakable(BlockPos pos,BlockState state){return GardenRooms025.placeable(state.getBlock())
      &&!(GardenTransporterBlock.footprint(TRANSPORTER).contains(pos)&&state.is(ActivityContent.GARDEN.get()))&&!FIXED_PODS.contains(pos);}

  public static ScenePlan025 plan(){
    var p=new ScenePlan025();var marble=HeroGardenDecor0253.block("hero_marble");
    for(int x=-28;x<=28;x++)for(int z=-25;z<=25;z++){
      if(!insideField(x,z))continue;
      int top=surface(x,z);double lake=poolDistance(x,z);boolean pool=lake<1,curb=poolBorder(x,z);
      if(pool)top=77;else if(curb)top=79;
      for(int y=68;y<=top;y++)p.put(x,y,z,y==top?(pool||curb?marble:top>81?LobbyDecor025.CLIFF.get():LobbyDecor025.TURF.get()):LobbyDecor025.CLIFF.get());
      if(pool)for(int y=top+1;y<=POOL_LEVEL;y++)p.put(x,y,z,Blocks.WATER);
      if(top>81&&!pool&&!curb)p.put(x,top,z,LobbyDecor025.TURF.get());
    }
    fountain(p);pavilion(p);bellIsland(p);fallenColumn(p);
    // The garden exit is a separate east-wall arch, never the fountain's temple.
    for(int x=19;x<=24;x++)for(int z=7;z<=9;z++){
      p.put(x,79,z,marble);for(int y=80;y<=84;y++)p.put(x,y,z,Blocks.AIR);
    }
    p.box(25,80,6,25,85,10,LobbyDecor025.CLIFF.get());
    PortalScenes0251.place(p,EXIT_PORTAL,EXIT_FACING,PortalScenes0251.RETURN);
    // Broad grass landing and unbroken five-metre approach stay clear of decor.
    p.box(17,79,6,20,79,10,LobbyDecor025.TURF.get());
    for(int x=16;x<=21;x++)for(int z=6;z<=10;z++)for(int y=80;y<=83;y++)p.put(x,y,z,Blocks.AIR);
    p.box(TRANSPORTER.getX(),79,TRANSPORTER.getZ(),TRANSPORTER.getX()+1,79,TRANSPORTER.getZ()+1,LobbyDecor025.TURF.get());
    GardenTransporterBlock.putScene(p,TRANSPORTER,Direction.SOUTH);
    for(var tree:PALMS)palm(p,tree[0],tree[1],tree[2]);
    int[][] flowers={{7,13},{11,9},{7,-11},{-3,-16},{16,-10},{17,1},{3,19},{-15,17}};
    for(int i=0;i<flowers.length;i++)for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)if((dx+dz+i)%2==0){
      int x=flowers[i][0]+dx,z=flowers[i][1]+dz;var floor=new BlockPos(x,surface(x,z),z);var at=floor.above();
      if(p.blocks.getOrDefault(floor,Blocks.AIR.defaultBlockState()).is(LobbyDecor025.TURF.get())&&p.blocks.getOrDefault(at,Blocks.AIR.defaultBlockState()).isAir())
        p.put(at.getX(),at.getY(),at.getZ(),i%2==0?Blocks.WHITE_TULIP:Blocks.PINK_TULIP);
    }
    for(int x=-31;x<=31;x++)for(int z=-28;z<=28;z++){
      boolean inside=insideField(x,z),rim=false;
      if(!inside)for(int dx=-2;dx<=2&&!rim;dx++)for(int dz=-2;dz<=2;dz++)if(insideField(x+dx,z+dz)){rim=true;break;}
      if(rim)for(int y=67;y<=255;y++)p.put(x,y,z,Blocks.BARRIER);
      if(inside||rim)p.put(x,255,z,Blocks.BARRIER);
    }
    return p;
  }
  private static void fountain(ScenePlan025 p){
    var marble=HeroGardenDecor0253.block("hero_marble");
    p.box(-25,79,-9,-23,80,1,marble);
    p.box(-24,81,-7,-24,88,-1,marble);
    for(int z:new int[]{-8,0}){
      p.box(-24,80,z,-23,81,z,marble);
      p.box(-23,82,z,-23,87,z,Blocks.QUARTZ_PILLAR);
      p.box(-24,88,z,-22,88,z,marble);
    }
    corniceRing(p,-25,-9,-22,1,89);p.box(-24,89,-8,-23,89,0,marble);
    // The reference fountain has a shallow lintel rather than a tall stepped pediment.
    p.box(-24,90,-8,-23,90,0,marble);
    p.put(FOUNTAIN_EMBLEM.getX(),FOUNTAIN_EMBLEM.getY(),FOUNTAIN_EMBLEM.getZ(),HeroGardenDecor0253.block("hero_fountain_emblem"));
    p.box(-23,82,-7,-21,82,-1,marble);
    // A genuine broad water curtain falls east from the left-wall fountain into its filled pool.
    for(int z=-6;z<=-2;z++)for(int y=79;y<=83;y++)p.put(-21,y,z,Blocks.WATER);
    for(int z=-6;z<=-2;z++)p.put(-22,83,z,Blocks.WATER);
  }
  private static void pavilion(ScenePlan025 p){
    var marble=HeroGardenDecor0253.block("hero_marble");
    p.box(-12,79,6,-4,81,14,marble);p.box(-11,82,7,-5,82,13,marble);
    // The entry steps meet the broad lawn; all four columns meet the entablature.
    p.box(-10,80,15,-6,80,16,marble);p.box(-10,81,14,-6,81,15,marble);
    var stair=Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING,Direction.NORTH);
    for(int x=-10;x<=-6;x++){p.put(x,80,16,stair);p.put(x,81,15,stair);p.put(x,82,14,stair);}
    for(int x:new int[]{-11,-5})for(int z:new int[]{7,13}){
      p.put(x,82,z,marble);
      var column=HeroGardenDecor0253.block("hero_column").defaultBlockState();
      p.put(x,83,z,column.setValue(HeroColumn0259.PART,HeroColumn0259.Part.BASE));
      p.box(x,84,z,x,87,z,column);
      p.put(x,88,z,column.setValue(HeroColumn0259.PART,HeroColumn0259.Part.CAPITAL));
    }
    corniceRing(p,-12,6,-4,14,89);
    p.box(-11,89,7,-5,89,13,marble);
    // Two shallow, clipped-corner tiers retain the pavilion roof silhouette.
    for(int tier=0;tier<2;tier++)for(int x=-12+tier;x<=-4-tier;x++)for(int z=6+tier;z<=14-tier;z++){
      if((x==-12+tier||x==-4-tier)&&(z==6+tier||z==14-tier))continue;
      p.put(x,90+tier,z,marble);
    }
  }
  private static void corniceRing(ScenePlan025 p,int x0,int z0,int x1,int z1,int y){
    var trim=HeroGardenDecor0253.block("hero_marble_trim").defaultBlockState();
    for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++){
      if(x!=x0&&x!=x1&&z!=z0&&z!=z1)continue;
      boolean corner=(x==x0||x==x1)&&(z==z0||z==z1);
      Direction facing=corner?(x==x0?(z==z0?Direction.NORTH:Direction.WEST):(z==z0?Direction.EAST:Direction.SOUTH)):
          z==z0?Direction.NORTH:x==x1?Direction.EAST:z==z1?Direction.SOUTH:Direction.WEST;
      p.put(x,y,z,trim.setValue(HeroCorniceBlock0254.FACING,facing).setValue(HeroCorniceBlock0254.CORNER,corner));
    }
  }
  private static void bellIsland(ScenePlan025 p){
    var marble=HeroGardenDecor0253.block("hero_marble");
    for(int x=-13;x<=-5;x++)for(int z=-42;z<=-34;z++){
      double distance=Math.pow((x+9)/4.5,2)+Math.pow((z+38)/4.5,2);if(distance>1)continue;
      int bottom=71+(int)Math.round(distance*5);
      for(int y=bottom;y<78;y++)p.put(x,y,z,LobbyDecor025.CLIFF.get());p.put(x,78,z,LobbyDecor025.TURF.get());
    }
    p.box(-11,79,-40,-7,79,-36,marble);
    for(int x:new int[]{-10,-8})for(int z:new int[]{-39,-37})p.box(x,80,z,x,84,z,Blocks.QUARTZ_PILLAR);
    p.box(-11,85,-40,-7,85,-36,marble);
    // A hanging native bell attaches directly to the pavilion's stone lintel.
    p.put(-9,84,-38,Blocks.BELL.defaultBlockState().setValue(net.minecraft.world.level.block.BellBlock.ATTACHMENT,
        net.minecraft.world.level.block.state.properties.BellAttachType.CEILING));
    p.box(-10,86,-39,-8,86,-37,marble);p.put(-9,87,-38,marble);
  }
  private static void fallenColumn(ScenePlan025 p){
    var axis=Blocks.QUARTZ_PILLAR.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS,Direction.Axis.X);
    for(int x=-8;x<=-4;x++)p.put(x,80,-19,axis);
    p.box(-9,80,-20,-9,81,-18,HeroGardenDecor0253.block("hero_marble"));
  }
  private static void palm(ScenePlan025 p,int x,int z,int height){
    int base=surface(x,z)+1,crown=base+height;
    if(!p.blocks.getOrDefault(new BlockPos(x,base-1,z),Blocks.AIR.defaultBlockState()).is(LobbyDecor025.TURF.get()))throw new IllegalStateException("Hero palm has no dry lawn support");
    for(int y=base;y<crown;y++)p.put(x,y,z,LobbyDecor025.PALM_TRUNK.get());p.put(x,crown,z,LobbyDecor025.PALM_CROWN.get());
    for(Direction d:Direction.Plane.HORIZONTAL){var support=new BlockPos(x,crown,z).relative(d);p.put(support.getX(),support.getY(),support.getZ(),LobbyDecor025.PALM_SUPPORT.get());
      var pod=support.below();p.put(pod.getX(),pod.getY(),pod.getZ(),FruitContent.podForFruit("garden_fruit").defaultBlockState().setValue(HangingFruitBlock.FACING,d.getOpposite()).setValue(HangingFruitBlock.AGE,3));}
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
    for(int x=-2;x<=20;x++)for(int z=-17;z<=20;z++){
      if(!insideField(x,z))continue;var floor=new BlockPos(x,surface(x,z),z);var at=new Vec3(x+.5,floor.getY()+1,z+.5);
      if(!world.getBlockState(floor).is(LobbyDecor025.TURF.get())||!world.getFluidState(floor.above()).isEmpty())continue;
      var box=e.getBoundingBox().move(at.subtract(e.position())).inflate(.02);
      if(reserved.stream().anyMatch(box::intersects))continue;
      if(world instanceof ServerLevel live&&!live.getEntities(e,box,other->eligible(other)&&!other.isRemoved()).isEmpty())continue;
      double distance=at.distanceToSqr(e.position());if(distance>=bestDistance||!clear(world,e,at))continue;
      best=at;bestDistance=distance;
    }
    return best;
  }
  /** Preflight purchased care props and actors before any terrain write. */
  public static void ensure(ServerLevel level){
    if(!level.dimension().equals(HubContent.HERO_GARDEN))throw new IllegalArgumentException("Hero garden has its own dimension");
    var p=plan();var preserve=new HashSet<BlockPos>();
    // Only the authored old single-cell terminal is eligible for expansion.
    // Reject an owned neighbor or any block entity before touching terrain,
    // retaining the root coordinate and therefore every roster/Bell key.
    if(!GardenTransporterBlock.canMergeScene(level,TRANSPORTER,(at,old)->
        old.equals(p.blocks.get(at))||old.isAir()||old.is(Blocks.WATER)
            ||old.is(LobbyDecor025.TURF.get())||old.is(LobbyDecor025.CLIFF.get())))
      throw new IllegalStateException("Hero transporter update awaits its complete two-by-two footprint at "+TRANSPORTER);
    for(var at:BlockPos.betweenClosed((int)BOUNDS.minX,(int)BOUNDS.minY,(int)BOUNDS.minZ,(int)BOUNDS.maxX-1,(int)BOUNDS.maxY-1,(int)BOUNDS.maxZ-1)){
      var old=level.getBlockState(at);var next=p.blocks.getOrDefault(at,Blocks.AIR.defaultBlockState());if(old.equals(next))continue;
      if(at.equals(TRANSPORTER)&&GardenTransporterBlock.compatibleState(old,next))continue;
      // Harvested native fruit keeps its present growth stage when the finite
      // scene is prepared again; the authored palm is still the same provider.
      if(FIXED_PODS.contains(at)&&old.getBlock()==next.getBlock()&&old.getBlock() instanceof HangingFruitBlock){
        preserve.add(at.immutable());p.blocks.put(at.immutable(),old);continue;
      }
      if(GardenRooms025.placeable(old.getBlock())){
        if(!next.isAir()&&!next.is(Blocks.WATER))throw new IllegalStateException("Hero update awaits care prop clearance at "+at);
        if(old.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)
            &&old.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)
            &&(!next.is(Blocks.WATER)||!next.getFluidState().isSource()))throw new IllegalStateException("Hero update would dry an owned water toy at "+at);
        preserve.add(at.immutable());p.blocks.put(at.immutable(),old);
      }else if(level.getBlockEntity(at)!=null&&!(level.getBlockEntity(at) instanceof LobbyDecor025.SceneEntity))
        throw new IllegalStateException("Hero update awaits unsupported block entity at "+at);
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
      if(root&&!wet&&!preserve.contains(at.below())&&!below.isFaceSturdy(planned,at.below(),Direction.UP))throw new IllegalStateException("Hero update would leave a care device unsupported at "+at);
    }
    var actors=level.getEntitiesOfClass(Entity.class,BOUNDS,HeroGardenRooms0253::eligible);
    var reserved=new ArrayList<AABB>();var displaced=new ArrayList<Entity>();var moves=new LinkedHashMap<Entity,Vec3>();
    for(var e:actors){
      var feet=e.blockPosition();
      boolean newlyWet=(!planned.getFluidState(feet).isEmpty()&&level.getFluidState(feet).isEmpty())
          ||(!planned.getFluidState(feet.below()).isEmpty()&&level.getFluidState(feet.below()).isEmpty());
      if(insideField(e.blockPosition().getX(),e.blockPosition().getZ())&&clear(planned,e,e.position())&&!newlyWet)reserved.add(e.getBoundingBox().inflate(.02));
      else{if(busy(e))throw new IllegalStateException("Hero update awaits active activity for "+e.getUUID());displaced.add(e);}
    }
    for(var e:displaced){var at=safeLawn(planned,e,reserved);if(at==null)throw new IllegalStateException("No clear Hero Garden landing for "+e.getUUID());moves.put(e,at);reserved.add(e.getBoundingBox().move(at.subtract(e.position())).inflate(.02));}
    p.apply(level,BOUNDS,(at,state)->preserve.contains(at));
    for(var move:moves.entrySet())if(!moveBody(level,move.getKey(),move.getValue()))throw new IllegalStateException("Hero body did not reach its preflighted landing");
  }
  private static boolean moveBody(ServerLevel level,Entity e,Vec3 at){
    if(e instanceof ServerPlayer player)player.teleportTo(level,at.x,at.y,at.z,Set.of(),player.getYRot(),player.getXRot());else e.teleportTo(at.x,at.y,at.z);
    if(e.isRemoved()||e.level()!=level||e.position().distanceToSqr(at)>1.e-8)return false;
    e.setDeltaMovement(Vec3.ZERO);e.fallDistance=0;if(e instanceof ChaoEntity c)GardenRegistry.get(level.getServer()).update(c);return true;
  }
  public static boolean rescueBody(ServerLevel level,Entity e){
    if(e.level()!=level||!level.dimension().equals(HubContent.HERO_GARDEN)||!eligible(e)||e.isRemoved()||busy(e))return false;
    var at=safeLawn(level,e,List.of());return at!=null&&moveBody(level,e,at);
  }
  /** Saved entity sections may arrive after terrain preparation; rescue them without replacing identity. */
  @SubscribeEvent public static void rescueLatePublication(EntityTickEvent.Post event){
    var e=event.getEntity();if(e.isRemoved()||e.tickCount%10!=0||!(e.level() instanceof ServerLevel level)
        ||!level.dimension().equals(HubContent.HERO_GARDEN)||!eligible(e)||busy(e))return;
    boolean unsafe=!insideField(e.blockPosition().getX(),e.blockPosition().getZ())||e.getY()<69;
    var box=e.getBoundingBox().deflate(1.e-6);
    for(var at:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ))){
      var state=level.getBlockState(at);var block=state.getBlock();
      if(!(state.is(LobbyDecor025.CLIFF.get())||state.is(LobbyDecor025.TURF.get())||state.is(Blocks.QUARTZ_PILLAR)
          ||block==HeroGardenDecor0253.block("hero_marble")||block==HeroGardenDecor0253.block("hero_marble_trim")||block==HeroGardenDecor0253.block("hero_fountain_emblem")))continue;
      for(var part:state.getCollisionShape(level,at,CollisionContext.of(e)).toAabbs())if(box.intersects(part.move(at)))unsafe=true;
    }
    if(unsafe)rescueBody(level,e);
  }
}
