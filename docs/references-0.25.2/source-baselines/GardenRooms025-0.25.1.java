package com.chaogarden.hub;

import com.chaogarden.activities.ActivityBlock;
import com.chaogarden.activities.ActivityContent;
import com.chaogarden.activities.ActivityManager;
import com.chaogarden.activities.GardenRegistry;
import com.chaogarden.entity.ChaoEggEntity;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.fruit.ChaoSaplingBlock;
import com.chaogarden.fruit.ChaoTreeCrownBlock;
import com.chaogarden.fruit.FruitContent;
import com.chaogarden.fruit.HangingFruitBlock;
import com.chaogarden.toys.ChaoToyBlock;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
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
  public static final BlockPos EXIT_PORTAL=new BlockPos(12,80,-36);
  public static final BlockPos TRANSPORTER=new BlockPos(34,82,15);
  private static final BlockPos LEGACY_TRANSPORTER=new BlockPos(-33,82,15);
  public static final int POND_LEVEL=79;
  public static final int[][] PALMS={{-9,-2,6},{-5,19,6},{12,17,7},{24,3,6},{32,25,6},{-4,-19,5},{27,-24,6},{38,8,6}};
  public static final AABB WATERFALL_BOUNDS=new AABB(-38,79,-42,-25,100,-15);
  private static final Set<BlockPos> FIXED_PODS=new HashSet<>(),LEGACY_FIXED=new HashSet<>();
  static{
    for(var p:PALMS)addPods(FIXED_PODS,p[0],p[1],surface(p[0],p[1])+p[2]+1);
    int[][] old={{-14,1,6},{-4,18,6},{-35,7,7},{-30,26,6},{8,21,7},{34,13,6},{7,-18,5},{17,-24,5},{34,-27,4},{24,-33,4},{28,-39,4}};
    for(var p:old)addPods(LEGACY_FIXED,p[0],p[1],legacySurface(p[0],p[1])+p[2]+1);
    LEGACY_FIXED.add(LEGACY_TRANSPORTER);
  }
  private GardenRooms025(){}
  private static void addPods(Set<BlockPos> output,int x,int z,int crown){for(Direction d:Direction.Plane.HORIZONTAL)output.add(new BlockPos(x,crown-1,z).relative(d));}
  public static boolean placeable(Block block){return block instanceof ActivityBlock||block instanceof ChaoToyBlock||block instanceof ChaoSaplingBlock||block instanceof ChaoTreeCrownBlock||block instanceof HangingFruitBlock||block==FruitContent.TRUNK.get()||block==FruitContent.LEAVES.get();}
  public static boolean breakable(BlockPos pos,BlockState state){return placeable(state.getBlock())&&!(pos.equals(TRANSPORTER)&&state.is(ActivityContent.GARDEN.get()))&&!FIXED_PODS.contains(pos);}
  public static boolean insideField(int x,int z){return Math.pow(x/50.0,4)+Math.pow((z+3)/45.0,4)<=1;}
  public static double pondDistance(int x,int z){return Math.pow((x+28)/13.0,2)+Math.pow((z+2)/19.0,2);}
  public static boolean pondCell(int x,int z){return pondDistance(x,z)<1&&z>=-16;}
  private static boolean waterfallMass(int x,int z){return x>=-47&&x<=-15&&z>=-43&&z<=-17;}
  public static int surface(int x,int z){
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
    if(waterfallMass(x,z)){
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
    var p=plan();
    migrateRegisteredTransporter(level,p);
    var wetBefore=new HashSet<java.util.UUID>();
    var reserved=new java.util.ArrayList<AABB>();
    for(var entity:level.getEntitiesOfClass(Entity.class,BOUNDS,GardenRooms025::eligibleBody))
      if(!level.getFluidState(entity.blockPosition()).isEmpty())wetBefore.add(entity.getUUID());
    p.apply(level,BOUNDS,(pos,state)->breakable(pos,state)&&!LEGACY_FIXED.contains(pos));
    // A scenery upgrade must not entomb an existing keeper/Chao/egg/item when
    // the old eastern pond and mountains move. Preserve the same entity/state;
    // relocate only a body actually obstructed or newly immersed by this plan.
    for(var entity:level.getEntitiesOfClass(Entity.class,BOUNDS,GardenRooms025::eligibleBody)){
      boolean newlyWet=!level.getFluidState(entity.blockPosition()).isEmpty()&&!wetBefore.contains(entity.getUUID());
      if(level.noCollision(entity)&&!newlyWet&&insideField(entity.blockPosition().getX(),entity.blockPosition().getZ()))continue;
      var safe=safeLawn(level,entity,reserved);if(safe==null)throw new IllegalStateException("No clear garden upgrade landing for "+entity.getUUID());
      entity.teleportTo(safe.x,safe.y,safe.z);entity.setDeltaMovement(Vec3.ZERO);entity.fallDistance=0;
      reserved.add(entity.getBoundingBox().inflate(.02));
      if(entity instanceof ChaoEntity chao)GardenRegistry.get(level.getServer()).update(chao);
    }
  }
  private static boolean eligibleBody(Entity entity){
    return (entity instanceof ChaoEntity||entity instanceof ChaoEggEntity||entity instanceof ItemEntity||entity instanceof ServerPlayer)
        &&!(entity instanceof ChaoEntity chao&&(GardenRegistry.managed(chao)||ActivityManager.isBusy(chao)));
  }
  private static boolean providerAvailable(ServerLevel level,ScenePlan025 plan,GardenRegistry data,GardenRegistry.Place old,BlockPos at){
    var place=GardenRegistry.Place.at(level,at);if(!data.canRelocateGarden(old,place))return false;
    var floor=plan.blocks.getOrDefault(at.below(),Blocks.AIR.defaultBlockState());
    var proposed=plan.blocks.getOrDefault(at,Blocks.AIR.defaultBlockState());
    var current=level.getBlockState(at);
    return floor.is(LobbyDecor025.TURF.get())&&(proposed.isAir()||proposed.is(ActivityContent.GARDEN.get()))
        &&(!placeable(current.getBlock())||current.is(ActivityContent.GARDEN.get()));
  }
  /** Stage a real provider before old onRemove can erase its membership ledger. */
  private static void migrateRegisteredTransporter(ServerLevel level,ScenePlan025 plan){
    var data=GardenRegistry.get(level.getServer());var old=GardenRegistry.Place.at(level,LEGACY_TRANSPORTER);
    if(!data.gardens.containsKey(old))return;
    BlockPos target=providerAvailable(level,plan,data,old,TRANSPORTER)?TRANSPORTER:null;
    for(int radius=3;target==null&&radius<=12;radius++)for(int dx=-radius;dx<=radius&&target==null;dx++)for(int dz=-radius;dz<=radius;dz++){
      if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
      int x=TRANSPORTER.getX()+dx,z=TRANSPORTER.getZ()+dz;
      var at=new BlockPos(x,surface(x,z)+1,z);
      if(insideField(x,z)&&providerAvailable(level,plan,data,old,at)){target=at;break;}
    }
    if(target!=null){
      // The source block's removal callback must see already-relocated entries.
      // The destination is nevertheless installed first, so a later plan failure
      // leaves a real provider for the still-preserved garden registration.
      var planned=plan.blocks.get(target);var previous=level.getBlockState(target);
      plan.put(target.getX(),target.getY(),target.getZ(),ActivityContent.GARDEN.get());
      level.setBlock(target,ActivityContent.GARDEN.get().defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
      if(level.getBlockState(target).is(ActivityContent.GARDEN.get())&&data.relocateGarden(old,GardenRegistry.Place.at(level,target)))return;
      var place=GardenRegistry.Place.at(level,target);
      if(!data.gardens.containsKey(place)&&!data.nodeOwners.containsKey(place)&&!data.links.containsKey(place)&&!data.links.containsValue(place)){
        level.setBlock(target,previous,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
        if(planned==null)plan.blocks.remove(target);else plan.blocks.put(target,planned);
      }
    }
    // No free claim/alias capacity: keep the exact old registration and physical
    // provider rather than deleting or merging another keeper's garden.
    plan.put(LEGACY_TRANSPORTER.getX(),LEGACY_TRANSPORTER.getY(),LEGACY_TRANSPORTER.getZ(),ActivityContent.GARDEN.get());
    for(int x=-34;x<=-32;x++)for(int z=14;z<=16;z++)for(int y=76;y<=81;y++)
      plan.put(x,y,z,y==81?LobbyDecor025.TURF.get():LobbyDecor025.CLIFF.get());
  }
  private static boolean authoredTerrainCollision(ServerLevel level,Entity entity){
    var box=entity.getBoundingBox().deflate(1.0e-6);
    for(var pos:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ))){
      var state=level.getBlockState(pos);
      if((state.is(LobbyDecor025.CLIFF.get())||state.is(LobbyDecor025.TURF.get())||state.is(LobbyDecor025.SAND.get()))&&box.intersects(new AABB(pos)))return true;
    }
    return false;
  }
  /** Entity sections may publish after the bounded terrain rewrite completes. */
  @SubscribeEvent public static void rescueLatePublication(EntityTickEvent.Post event){
    var entity=event.getEntity();
    if(entity.isRemoved()||entity.tickCount%10!=0||!(entity.level() instanceof ServerLevel level)||!level.dimension().equals(HubContent.GARDEN)
        ||!eligibleBody(entity))return;
    if(!authoredTerrainCollision(level,entity))return;
    var safe=safeLawn(level,entity);if(safe==null)return;
    entity.teleportTo(safe.x,safe.y,safe.z);entity.setDeltaMovement(Vec3.ZERO);entity.fallDistance=0;
    if(entity instanceof ChaoEntity chao)GardenRegistry.get(level.getServer()).update(chao);
  }
  private static Vec3 safeLawn(ServerLevel level,Entity entity){
    return safeLawn(level,entity,java.util.List.of());
  }
  private static Vec3 safeLawn(ServerLevel level,Entity entity,java.util.List<AABB> reserved){
    Vec3 best=null;double distance=Double.POSITIVE_INFINITY;
    for(int x=-8;x<=20;x++)for(int z=-10;z<=22;z++){
      int y=surface(x,z)+1;var pos=new BlockPos(x,y,z);var floor=pos.below();
      if(!level.getBlockState(floor).is(LobbyDecor025.TURF.get())||!level.getFluidState(pos).isEmpty())continue;
      var candidate=new Vec3(x+.5,y,z+.5);var box=entity.getBoundingBox().move(candidate.subtract(entity.position()));
      double d=entity.position().distanceToSqr(candidate);if(d>=distance)continue;
      var clearance=box.inflate(.02);
      // Vanilla entity collision is asymmetric: an egg can block a keeper,
      // while that keeper does not block the egg. Explicitly reserve body space
      // rather than allowing a later egg to invalidate an earlier landing.
      if(reserved.stream().anyMatch(clearance::intersects)||!level.getEntities(entity,clearance,other->!other.isRemoved()
          &&(other instanceof ServerPlayer||other instanceof ChaoEntity||other instanceof ChaoEggEntity||other instanceof ItemEntity)).isEmpty())continue;
      if(level.noCollision(entity,box)){best=candidate;distance=d;}
    }
    return best;
  }
  public static ScenePlan025 plan(){var p=new ScenePlan025();
    for(int x=-51;x<=51;x++)for(int z=-49;z<=45;z++){
      if(!insideField(x,z))continue;
      int top=surface(x,z);boolean lake=pondCell(x,z),shore=pondDistance(x,z)<1.12&&z>=-16;
      if(lake)top=pondDistance(x,z)>.67?77:76;else if(shore)top=79;
      boolean rock=top>84||waterfallMass(x,z);
      for(int y=68;y<=top;y++)p.put(x,y,z,y==top?(lake||shore?LobbyDecor025.SAND.get():rock?LobbyDecor025.CLIFF.get():LobbyDecor025.TURF.get()):LobbyDecor025.CLIFF.get());
      if(lake)for(int y=top+1;y<=POND_LEVEL;y++)p.put(x,y,z,Blocks.WATER);
      // Thin crest grass belongs on the jagged main cliff, not every waterfall step.
      if(rock&&!waterfallMass(x,z))p.put(x,top,z,LobbyDecor025.TURF.get());
    }
    waterfall(p);
    // A compact throat sits inside a continuous mountain. Its roof is the actual
    // cliff mass: no box awning, external shelf, hollow upper notch or wall sign.
    for(int x=11;x<=13;x++)for(int z=-36;z<=-30;z++){
      p.put(x,79,z,LobbyDecor025.TURF.get());
      for(int y=80;y<=83;y++)p.put(x,y,z,Blocks.AIR);
    }
    // The frame has a proper quartz threshold and an animated end-portal surface.
    PortalScenes0251.place(p,EXIT_PORTAL,Direction.SOUTH,PortalScenes0251.RETURN);
    p.box(11,80,-37,13,84,-37,LobbyDecor025.CLIFF.get());
    p.box(32,81,13,36,81,17,LobbyDecor025.TURF.get());
    p.put(TRANSPORTER.getX(),TRANSPORTER.getY(),TRANSPORTER.getZ(),ActivityContent.GARDEN.get());
    for(var tree:PALMS)palm(p,tree[0],tree[1],tree[2]);
    int[][] flowers={{7,-28},{17,-29},{22,-25},{-4,-22},{-12,-11},{-9,9},{1,17},{16,4},{23,22},{37,4},{35,25},{-15,21},{-38,20}};
    for(int i=0;i<flowers.length;i++)for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)if((dx+dz+i)%2==0){
      int x=flowers[i][0]+dx,z=flowers[i][1]+dz,y=surface(x,z)+1;var floor=new BlockPos(x,y-1,z);var at=floor.above();
      if(p.blocks.getOrDefault(floor,Blocks.AIR.defaultBlockState()).is(LobbyDecor025.TURF.get())&&p.blocks.getOrDefault(at,Blocks.AIR.defaultBlockState()).isAir())
        p.put(x,y,z,i%3==0?Blocks.BLUE_ORCHID:i%3==1?Blocks.ALLIUM:Blocks.PINK_TULIP);
    }
    // Invisible physical limits follow the floating lawn edge. A two-cell ring
    // and high cap contain babies, dropped items and elytra flight alike.
    for(int x=-52;x<=52;x++)for(int z=-50;z<=46;z++){
      boolean inside=insideField(x,z),rim=false;
      if(!inside)for(int dx=-2;dx<=2&&!rim;dx++)for(int dz=-2;dz<=2;dz++)if(insideField(x+dx,z+dz)){rim=true;break;}
      if(rim)for(int y=67;y<=255;y++)p.put(x,y,z,Blocks.BARRIER);
      if(inside||rim)p.put(x,255,z,Blocks.BARRIER);
    }
    return p;
  }
  private static void waterfall(ScenePlan025 p){
    // Four broad shelves; every receiving basin is completely filled with source
    // water. The former cave behind the lowest curtain is solid limestone.
    for(int step=0;step<4;step++){
      int front=-17-step*6,top=83+step*4,lower=step==0?POND_LEVEL:top-4;
      for(int z=front-5;z<=front;z++){
        for(int x=-34;x<=-30;x++){
          for(int y=68;y<top;y++)p.put(x,y,z,LobbyDecor025.CLIFF.get());
          p.put(x,top,z,Blocks.WATER);
        }
        for(int side:new int[]{-35,-29})for(int y=68;y<=top;y++)p.put(side,y,z,LobbyDecor025.CLIFF.get());
      }
      for(int x=-34;x<=-30;x++)for(int y=lower;y<=top;y++)p.put(x,y,front,Blocks.WATER);
    }
    // One deep rear wall closes the top supply; water cannot escape behind it.
    p.box(-35,68,-41,-29,96,-41,LobbyDecor025.CLIFF.get());
  }
  private static void palm(ScenePlan025 p,int x,int z,int height){
    int base=surface(x,z)+1,crown=base+height;
    var ground=new BlockPos(x,base-1,z);
    if(!p.blocks.getOrDefault(ground,Blocks.AIR.defaultBlockState()).is(LobbyDecor025.TURF.get())||WATERFALL_BOUNDS.inflate(7,0,7).contains(x+.5,base,z+.5))throw new IllegalStateException("Palm lacks dry lawn clearance: "+ground);
    for(int y=base;y<crown;y++)p.put(x,y,z,LobbyDecor025.PALM_TRUNK.get());p.put(x,crown,z,LobbyDecor025.PALM_CROWN.get());
    for(Direction side:Direction.Plane.HORIZONTAL){var support=new BlockPos(x,crown,z).relative(side);p.put(support.getX(),support.getY(),support.getZ(),LobbyDecor025.PALM_SUPPORT.get());
      var pod=support.below();p.put(pod.getX(),pod.getY(),pod.getZ(),FruitContent.podForFruit("garden_fruit").defaultBlockState().setValue(HangingFruitBlock.FACING,side.getOpposite()).setValue(HangingFruitBlock.AGE,3));}
  }
}
