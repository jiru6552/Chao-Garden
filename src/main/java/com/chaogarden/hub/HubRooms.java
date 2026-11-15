package com.chaogarden.hub;

import com.chaogarden.kindergarten.KindergartenRooms025;
import com.chaogarden.core.LobbyApproachGeometry0255;
import com.chaogarden.core.GardenGateGeometry0256;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** Authored lobby collision/architecture and bounded routing to the playable spaces. */
public final class HubRooms {
  public static final int REVISION=38,FLOOR_Y=79;
  public static final BlockPos HERO_DOOR=new BlockPos(18,89,-12),DARK_DOOR=new BlockPos(-18,71,12);
  public static final Direction HERO_FACING=Direction.SOUTH,DARK_FACING=Direction.NORTH;
  public static final Vec3 HERO_RETURN_LANDING=new Vec3(18.5,89.1,-9.5),DARK_RETURN_LANDING=new Vec3(-17.5,71.1,10.5);
  public static final int MIN_VISITOR_Y=68;
  public static final BlockPos HUB_CENTER=new BlockPos(0,80,0),KINDERGARTEN_ENTRY=new BlockPos(0,80,40),KINDERGARTEN_DOOR=new BlockPos(0,80,-22),KINDERGARTEN_RETURN=new BlockPos(0,80,49);
  public static final BlockPos CLASSROOM=new BlockPos(-18,80,-15),CLASSROOM_STATION=new BlockPos(-18,80,-17),PRINCIPAL=new BlockPos(0,80,-21),DOCTOR=new BlockPos(18,80,-15),DOCTOR_STATION=new BlockPos(18,80,-17),FORTUNE=new BlockPos(22,80,5),MARKET=new BlockPos(-22,80,5),BOARD=new BlockPos(-7,80,0),BOARD_PANEL=new BlockPos(-6,82,0);
  public static final AABB HUB_BOUNDS=new AABB(-27,56,-27,28,108,28),KINDERGARTEN_BOUNDS=new AABB(-37,76,-34,38,103,52);
  private HubRooms(){}
  /** Shared gate transform for both fire sprites and real client particles. */
  public static Vec3 gateWorldPoint(BlockPos anchor,Direction facing,GardenGateGeometry0256.Point point){
    double x=point.x(),z=point.z();
    switch(facing){
      case NORTH->{x=-point.x();z=-point.z();}
      case EAST->{x=point.z();z=-point.x();}
      case WEST->{x=-point.z();z=point.x();}
      default->{}
    }
    return new Vec3(anchor.getX()+.5+x,anchor.getY()+point.y(),anchor.getZ()+.5+z);
  }
  public static AABB bounds(boolean kindergarten){return kindergarten?KINDERGARTEN_BOUNDS:HUB_BOUNDS;}
  public static void ensure(ServerLevel level){
    if(level.dimension().equals(HubContent.HUB)){plan081(GardenUnlocks081.isUnlocked(level.getServer(),true),GardenUnlocks081.isUnlocked(level.getServer(),false)).apply(level,HUB_BOUNDS);return;}
    if(level.dimension().equals(HubContent.KINDERGARTEN)){KindergartenRooms025.ensure(level);return;}
    if(level.dimension().equals(HubContent.GARDEN)){GardenRooms025.ensure(level);return;}
    if(level.dimension().equals(HubContent.HERO_GARDEN)){HeroGardenRooms0253.ensure(level);return;}
    if(level.dimension().equals(HubContent.DARK_GARDEN)){DarkGardenRooms0255.ensure(level);return;}
    throw new IllegalArgumentException("Chao scenery builds only in its own dimensions");
  }
  public static final BlockPos SCENE_ANCHOR081=new BlockPos(0,78,0);
  public record Refresh081(boolean applied,int changed,java.util.List<BlockPos> conflicts) {
    public Refresh081 { conflicts=java.util.List.copyOf(conflicts); }
  }
  private static final java.util.Set<ServerLevel> REFRESHING081=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
  public static boolean accessInstalled081(ServerLevel level,boolean hero){
    var anchor=level.getBlockState(SCENE_ANCHOR081);
    if(!anchor.is(LobbyDecor025.SCENE_ANCHOR.get())||!anchor.getValue(LobbyDecor025.SceneAnchor.GARDEN_LAYOUT81)
        ||(anchor.getValue(LobbyDecor025.SceneAnchor.GARDEN_ACCESS81)&(hero?1:2))==0)return false;
    return level.getBlockState(hero?HERO_DOOR:DARK_DOOR).equals(PortalScenes0251.state(hero?HERO_FACING:DARK_FACING,hero?PortalScenes0251.HERO:PortalScenes0251.DARK));
  }
  /** Only the finite gate footprint is changed. A keeper's edit or occupied
   * support aborts the whole transaction before either floor or portal changes. */
  public static Refresh081 refreshUnlocks081(ServerLevel level){
    if(!level.dimension().equals(HubContent.HUB))return new Refresh081(false,0,java.util.List.of(SCENE_ANCHOR081));
    int revision=HubWorldData.get(level.getServer()).revisions.getOrDefault(level.dimension().location().toString(),0);
    if(revision>0&&revision<REVISION)return new Refresh081(false,0,java.util.List.of(SCENE_ANCHOR081));
    return refreshPlan081(level,BlockPos.ZERO,(GardenUnlocks081.isUnlocked(level.getServer(),true)?1:0)|(GardenUnlocks081.isUnlocked(level.getServer(),false)?2:0));
  }
  private static Refresh081 refreshPlan081(ServerLevel level,BlockPos origin,int requested){
    var anchor=SCENE_ANCHOR081.offset(origin);
    if(!REFRESHING081.add(level))return new Refresh081(false,0,java.util.List.of(anchor));
    try{
      level.getChunkAt(anchor);var state=level.getBlockState(anchor);
      if(!state.is(LobbyDecor025.SCENE_ANCHOR.get())||state.getValue(LobbyDecor025.SceneAnchor.PORTAL))
        return new Refresh081(false,0,java.util.List.of(anchor));
      int nextMask=requested&3;
      if(state.getValue(LobbyDecor025.SceneAnchor.GARDEN_LAYOUT81)&&state.getValue(LobbyDecor025.SceneAnchor.GARDEN_ACCESS81)==nextMask){
        var missing=new java.util.ArrayList<BlockPos>();
        for(boolean hero:new boolean[]{true,false})if((nextMask&(hero?1:2))!=0){
          var gate=(hero?HERO_DOOR:DARK_DOOR).offset(origin);level.getChunkAt(gate);
          if(!level.getBlockState(gate).equals(PortalScenes0251.state(hero?HERO_FACING:DARK_FACING,hero?PortalScenes0251.HERO:PortalScenes0251.DARK)))missing.add(gate);
        }
        return new Refresh081(missing.isEmpty(),0,missing);
      }
      int oldMask=LobbyDecor025.sceneAccess081(state);
      var oldPlan=plan081((oldMask&1)!=0,(oldMask&2)!=0);
      if(!state.getValue(LobbyDecor025.SceneAnchor.GARDEN_LAYOUT81))oldPlan.blocks.put(SCENE_ANCHOR081,LobbyDecor025.SCENE_ANCHOR.get().defaultBlockState());
      var nextPlan=plan081((nextMask&1)!=0,(nextMask&2)!=0);
      var keys=new java.util.LinkedHashSet<BlockPos>(oldPlan.blocks.keySet());keys.addAll(nextPlan.blocks.keySet());
      var before=new java.util.LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
      var after=new java.util.LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
      var conflicts=new java.util.ArrayList<BlockPos>();
      for(var local:keys){
        var expected=oldPlan.blocks.getOrDefault(local,Blocks.AIR.defaultBlockState());var next=nextPlan.blocks.getOrDefault(local,Blocks.AIR.defaultBlockState());
        if(expected.equals(next))continue;
        var at=local.offset(origin);level.getChunkAt(at);var actual=level.getBlockState(at);
        if(!actual.equals(expected)&&!actual.equals(next)){conflicts.add(at);continue;}
        if(actual.equals(next))continue;
        var entity=level.getBlockEntity(at);
        if(entity!=null&&!entity.saveWithoutMetadata(level.registryAccess()).isEmpty()){conflicts.add(at);continue;}
        before.put(at,actual);after.put(at,next);
      }
      if(!conflicts.isEmpty())return new Refresh081(false,0,conflicts);
      var actors=level.getEntities((net.minecraft.world.entity.Entity)null,HUB_BOUNDS.move(origin),e->!e.isRemoved()&&!e.isSpectator());
      for(var at:after.keySet())if(!at.equals(anchor)&&actors.stream().anyMatch(e->e.getBoundingBox().intersects(new AABB(at).inflate(.01,.15,.01))))conflicts.add(at);
      if(!conflicts.isEmpty())return new Refresh081(false,0,conflicts);
      // Publish the scene mask last, after all native cells are complete.
      var ordered=new java.util.ArrayList<BlockPos>(after.keySet());ordered.remove(anchor);if(after.containsKey(anchor))ordered.add(anchor);
      var written=new java.util.ArrayList<BlockPos>();
      try{
        for(var at:ordered){
          var next=after.get(at);written.add(at);
          if(!level.setBlock(at,next,net.minecraft.world.level.block.Block.UPDATE_CLIENTS|net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE)&&!level.getBlockState(at).equals(next))throw new IllegalStateException("Garden access cell rejected: "+at);
        }
      }catch(RuntimeException error){
        for(int n=written.size()-1;n>=0;n--){var at=written.get(n);level.setBlock(at,before.get(at),net.minecraft.world.level.block.Block.UPDATE_CLIENTS|net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE);}
        com.mojang.logging.LogUtils.getLogger().error("Guarded Chao lobby access transaction rolled back",error);
        return new Refresh081(false,0,java.util.List.of(anchor));
      }
      return new Refresh081(true,written.size(),java.util.List.of());
    }finally{REFRESHING081.remove(level);}
  }
  public static ScenePlan025 plan081(boolean heroOpen,boolean darkOpen){var p=new ScenePlan025();
    // Collision is genuine glass footing with no occluding native faces and no
    // opaque block foundation. The dedicated scene entity renders the translucent
    // surface, thin radial grid, gold rings and celestial/reflected underside.
    for(int x=-25;x<=25;x++)for(int z=-25;z<=25;z++){
      double radius=Math.hypot(x,z);if(radius<=25&&(!darkOpen||!LobbyApproachGeometry0255.floorOpening(x,z)))p.put(x,79,z,LobbyDecor025.GLASS_FLOOR.get());
      if(radius>=24.4&&radius<=25.1){boolean school=Math.abs(x)<=2&&z<0,garden=Math.abs(x)<=2&&z>0;
        if(!school&&!garden)p.box(x,80,z,x,84,z,Blocks.BARRIER);
      }
    }
    p.put(0,78,0,LobbyDecor025.SCENE_ANCHOR.get().defaultBlockState()
        .setValue(LobbyDecor025.SceneAnchor.GARDEN_LAYOUT81,true)
        .setValue(LobbyDecor025.SceneAnchor.GARDEN_ACCESS81,(heroOpen?1:0)|(darkOpen?2:0)));
    for(int x:new int[]{-3,3})for(int z:new int[]{-3,3})p.box(x,80,z,x,84,z,Blocks.BARRIER);
    // Curved 3-wide half-height treads follow the circular side walls. The same
    // finite plan supplies thin floors, stair guards and the joined upper rim.
    for(var cell:LobbyApproachGeometry0255.nativeCells().keySet())
      if(cell.x()>0?heroOpen:darkOpen)p.put(cell.x(),cell.y(),cell.z(),LobbyApproachDecor0255.FLOOR_STAIRS.get());
    // Portal columns flank the opening; no barrier crosses the walking mouth.
    for(boolean hero:new boolean[]{true,false}){
      if(hero?!heroOpen:!darkOpen)continue;
      var gate=hero?HERO_DOOR:DARK_DOOR;
      for(int side:new int[]{-2,2})p.box(gate.getX()+side,gate.getY(),gate.getZ(),gate.getX()+side,gate.getY()+3,gate.getZ(),Blocks.BARRIER);
    }
    if(heroOpen)PortalScenes0251.place(p,HERO_DOOR,HERO_FACING,PortalScenes0251.HERO);
    if(darkOpen)PortalScenes0251.place(p,DARK_DOOR,DARK_FACING,PortalScenes0251.DARK);
    // Collisions follow the two real entrances and leave their centers open.
    for(int x:new int[]{-4,4}){p.box(x,80,-23,x,84,-22,Blocks.BARRIER);p.box(x,80,22,x,84,23,Blocks.BARRIER);}
    PortalScenes0251.place(p,PortalScenes0251.HUB_SCHOOL,Direction.SOUTH,PortalScenes0251.SCHOOL);
    PortalScenes0251.place(p,PortalScenes0251.HUB_GARDEN,Direction.NORTH,PortalScenes0251.GARDEN);
    return p;
  }
}
