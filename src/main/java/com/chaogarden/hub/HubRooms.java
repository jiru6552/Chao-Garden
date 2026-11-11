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
    if(level.dimension().equals(HubContent.HUB)){hub().apply(level,HUB_BOUNDS);return;}
    if(level.dimension().equals(HubContent.KINDERGARTEN)){KindergartenRooms025.ensure(level);return;}
    if(level.dimension().equals(HubContent.GARDEN)){GardenRooms025.ensure(level);return;}
    if(level.dimension().equals(HubContent.HERO_GARDEN)){HeroGardenRooms0253.ensure(level);return;}
    if(level.dimension().equals(HubContent.DARK_GARDEN)){DarkGardenRooms0255.ensure(level);return;}
    throw new IllegalArgumentException("Chao scenery builds only in its own dimensions");
  }
  private static ScenePlan025 hub(){var p=new ScenePlan025();
    // Collision is genuine glass footing with no occluding native faces and no
    // opaque block foundation. The dedicated scene entity renders the translucent
    // surface, thin radial grid, gold rings and celestial/reflected underside.
    for(int x=-25;x<=25;x++)for(int z=-25;z<=25;z++){
      double radius=Math.hypot(x,z);if(radius<=25&&!LobbyApproachGeometry0255.floorOpening(x,z))p.put(x,79,z,LobbyDecor025.GLASS_FLOOR.get());
      if(radius>=24.4&&radius<=25.1){boolean school=Math.abs(x)<=2&&z<0,garden=Math.abs(x)<=2&&z>0;
        if(!school&&!garden)p.box(x,80,z,x,84,z,Blocks.BARRIER);
      }
    }
    p.put(0,78,0,LobbyDecor025.SCENE_ANCHOR.get());
    for(int x:new int[]{-3,3})for(int z:new int[]{-3,3})p.box(x,80,z,x,84,z,Blocks.BARRIER);
    // Curved 3-wide half-height treads follow the circular side walls. The same
    // finite plan supplies thin floors, stair guards and the joined upper rim.
    for(var cell:LobbyApproachGeometry0255.nativeCells().keySet())
      p.put(cell.x(),cell.y(),cell.z(),LobbyApproachDecor0255.FLOOR_STAIRS.get());
    // Portal columns flank the opening; no barrier crosses the walking mouth.
    for(boolean hero:new boolean[]{true,false}){
      var gate=hero?HERO_DOOR:DARK_DOOR;
      for(int side:new int[]{-2,2})p.box(gate.getX()+side,gate.getY(),gate.getZ(),gate.getX()+side,gate.getY()+3,gate.getZ(),Blocks.BARRIER);
    }
    PortalScenes0251.place(p,HERO_DOOR,HERO_FACING,PortalScenes0251.HERO);
    PortalScenes0251.place(p,DARK_DOOR,DARK_FACING,PortalScenes0251.DARK);
    // Collisions follow the two real entrances and leave their centers open.
    for(int x:new int[]{-4,4}){p.box(x,80,-23,x,84,-22,Blocks.BARRIER);p.box(x,80,22,x,84,23,Blocks.BARRIER);}
    PortalScenes0251.place(p,PortalScenes0251.HUB_SCHOOL,Direction.SOUTH,PortalScenes0251.SCHOOL);
    PortalScenes0251.place(p,PortalScenes0251.HUB_GARDEN,Direction.NORTH,PortalScenes0251.GARDEN);
    return p;
  }
}
