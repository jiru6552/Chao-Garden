package com.chaogarden.kindergarten;

import com.chaogarden.care.DoctorContent;
import com.chaogarden.hub.PortalScenes0251;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

/** Compact, physically connected school authored against the complete supplied native recording. */
public final class KindergartenRooms025 {
  public static final AABB BOUNDS=new AABB(-37,76,-34,38,103,52);
  public static final BlockPos ENTRY=new BlockPos(0,80,40),RETURN=new BlockPos(0,80,49);
  public static final BlockPos BOARD_PANEL=new BlockPos(-6,82,0),BOARD_CORNER=new BlockPos(-5,80,8);
  public record DoorPair(BlockPos center,String id,Direction facing){
    public BlockPos leftBase(){return KindergartenDecor025.SchoolDoorBlock.leftBase(center,facing);}
    public BlockPos rightBase(){return KindergartenDecor025.SchoolDoorBlock.rightBase(center,facing);}
  }
  public static List<DoorPair> doorPairs(){return List.of(
      new DoorPair(new BlockPos(-7,80,-4),"school_door_classroom",Direction.EAST),new DoorPair(new BlockPos(7,80,-4),"school_door_health",Direction.WEST),
      new DoorPair(new BlockPos(-6,80,5),"school_tool_cabinet",Direction.EAST),new DoorPair(new BlockPos(7,80,5),"school_door_fortune",Direction.WEST),
      new DoorPair(new BlockPos(0,80,-8),"school_door_principal",Direction.SOUTH),new DoorPair(new BlockPos(0,80,10),"school_door_exit",Direction.NORTH),
      new DoorPair(new BlockPos(0,80,18),"school_door_exit",Direction.SOUTH));}
  private KindergartenRooms025(){}
  private static final class Plan {
    final Map<BlockPos,BlockState> blocks=new LinkedHashMap<>();
    void put(int x,int y,int z,Block b){var state=b.defaultBlockState();if(state.hasProperty(LeavesBlock.PERSISTENT))state=state.setValue(LeavesBlock.PERSISTENT,true);put(x,y,z,state);}
    void put(int x,int y,int z,BlockState s){blocks.put(new BlockPos(x,y,z),s);}
    void decor(int x,int y,int z,String id){decor(x,y,z,id,Direction.SOUTH);}
    void decor(int x,int y,int z,String id,Direction face){put(x,y,z,KindergartenDecor025.state(id,face));}
    void surface(int x,int y,int z,String id){if(KindergartenHallThemes0252.contains(id))put(x,y,z,KindergartenHallThemes0252.block(id));else decor(x,y,z,id);}
    void atlas(int x,int y,int z,String id,int tile,Direction face){put(x,y,z,KindergartenDecor025.atlas(id,tile,face));}
    void box(int x0,int y0,int z0,int x1,int y1,int z1,Block b){for(int x=x0;x<=x1;x++)for(int y=y0;y<=y1;y++)for(int z=z0;z<=z1;z++)put(x,y,z,b);}
    void fill(int x0,int y0,int z0,int x1,int y1,int z1,String id){for(int x=x0;x<=x1;x++)for(int y=y0;y<=y1;y++)for(int z=z0;z<=z1;z++)decor(x,y,z,id);}
    void air(int x0,int y0,int z0,int x1,int y1,int z1){box(x0,y0,z0,x1,y1,z1,Blocks.AIR);}
    void barrierAir(int x,int y,int z){BlockPos q=new BlockPos(x,y,z);if(blocks.getOrDefault(q,Blocks.AIR.defaultBlockState()).isAir())put(x,y,z,Blocks.BARRIER);}
  }
  public static void ensure(ServerLevel level){
    if(!KindergartenContent.inKindergarten(level))throw new IllegalArgumentException("Kindergarten scenery is dimension scoped");
    for(int x=-3;x<=2;x++)for(int z=-3;z<=3;z++)level.getChunk(x,z);
    Plan p=plan();BlockPos.MutableBlockPos c=new BlockPos.MutableBlockPos();
    for(int y=76;y<103;y++)for(int x=-37;x<38;x++)for(int z=-34;z<52;z++){
      c.set(x,y,z);var desired=p.blocks.getOrDefault(c,Blocks.AIR.defaultBlockState());
      if(!level.getBlockState(c).equals(desired))level.setBlock(c,desired,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
    }
    KindergartenHallDisplays0252.ensure(level);
  }
  /** Bounded, idempotent aperture repair. Never runs the scenery reset for this upgrade. */
  public static void repairDormerWindows(ServerLevel level){
    Plan desired=plan();
    for(int center:new int[]{-18,18}){
      BlockPos anchor=new BlockPos(center-1,88,14);
      // Older authored builds placed the complete frame in front of the wall.
      for(int oldY:new int[]{87,88})for(int z:new int[]{15,16}){
        BlockPos old=new BlockPos(center-1,oldY,z);
        if(!level.getBlockState(old).is(KindergartenDecor025.block("school_round_window")))continue;
        var target=level.getBlockState(anchor);
        if(!target.isAir()&&!target.is(KindergartenDecor025.block("school_clapboard"))&&!target.is(KindergartenDecor025.block("school_round_window")))continue;
        level.setBlock(old,desired.blocks.getOrDefault(old,Blocks.AIR.defaultBlockState()),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
        level.setBlock(anchor,KindergartenDecor025.state("school_round_window",Direction.SOUTH),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
        for(int x=center-1;x<=center;x++){
          BlockPos sill=new BlockPos(x,oldY-1,z);if(level.getBlockState(sill).is(Blocks.GREEN_CONCRETE))level.setBlock(sill,desired.blocks.getOrDefault(sill,Blocks.AIR.defaultBlockState()),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
        }
      }
      if(!level.getBlockState(anchor).is(KindergartenDecor025.block("school_round_window")))continue;
      for(var at:BlockPos.betweenClosed(center-1,88,12,center,89,14))
        if(level.getBlockState(at).is(KindergartenDecor025.block("school_clapboard")))level.setBlock(at,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
    }
  }
  private static boolean foyer(int x,int z){return Math.abs(x)<=7&&z>=-8&&z<=10&&Math.abs(x)+Math.max(0,Math.max(-z-5,z-7))<=7;}
  private static Plan plan(){
    Plan p=new Plan();p.box(-36,77,-33,36,78,51,Blocks.DIRT);p.box(-36,79,-33,36,79,51,Blocks.GRASS_BLOCK);
    for(int x=-3;x<=3;x++)for(int z=19;z<=49;z++)p.decor(x,79,z,"school_yellow_brick");
    for(int x:new int[]{-5,5}){p.box(x,80,21,x,80,45,Blocks.OAK_LEAVES);p.box(x+(x<0?-1:1),80,21,x+(x<0?-1:1),80,45,Blocks.OAK_LEAVES);for(int z:new int[]{23,30,37,44})p.box(x-1,81,z,x+1,81,z+1,Blocks.OAK_LEAVES);}
    for(int x:new int[]{-34,34})p.box(x,80,-31,x,83,47,Blocks.OAK_LEAVES);p.box(-34,80,-32,34,83,-32,Blocks.OAK_LEAVES);
    // Invisible fences rise above the hedges: the school approach is public, the building sides are scenery.
    for(int x:new int[]{-5,5})p.box(x,82,20,x,102,48,Blocks.BARRIER);
    p.box(-34,84,-32,34,102,-32,Blocks.BARRIER);for(int x:new int[]{-34,34})p.box(x,84,-31,x,102,48,Blocks.BARRIER);
    // Closed blue school shell. The roof is a solid enclosed hip roof with a central cross-gable.
    p.fill(-28,79,-27,28,79,18,"school_stone_tile");p.fill(-28,85,-27,28,85,18,"school_cream_wall");
    for(int x=-28;x<=28;x++)for(int z=-27;z<=18;z++){
      if(Math.abs(x)==28||z==-27||z==18)p.fill(x,80,z,x,85,z,"school_clapboard");
      int rise=Math.max(0,Math.min(7,Math.min(Math.min((18-z)/3,(z+27)/3),(28-Math.abs(x))/2)));
      int roof=86+rise;if(Math.abs(x)<=7&&z>=-3)roof=Math.max(roof,93-Math.abs(x));
      p.fill(x,86,z,x,roof-1,z,"school_clapboard");p.decor(x,roof,z,"school_red_shingle");
    }
    room(p,-25,-22,-13,-8,"school_classroom_floor","school_cloud_mural");
    room(p,13,-21,25,-9,"school_clinic_tile","school_cream_wall");
    room(p,16,-8,24,-1,"school_clinic_tile","school_cream_wall");
    room(p,-5,-25,5,-16,"school_principal_rug","school_cream_wall");
    room(p,18,0,26,10,"school_principal_rug","school_fortune_stone");
    room(p,-25,1,-17,9,"school_stone_tile","school_fortune_stone");
    // Small connecting passages retain all stable resident/student coordinates without an oversized hall.
    corridor(p,-14,-5,-6,-3,Hall.CLASSROOM);corridor(p,-15,-11,-12,-3,Hall.CLASSROOM);
    corridor(p,6,-5,14,-3,Hall.CLINIC);corridor(p,12,-12,15,-3,Hall.CLINIC);
    corridor(p,-1,-17,1,-7,Hall.PRINCIPAL);corridor(p,6,4,19,6,Hall.FORTUNE);corridor(p,-18,4,-6,6,Hall.MARKET);corridor(p,-1,9,1,18,Hall.ENTRANCE);
    for(int[] r:new int[][]{{-14,-5,-6,-3},{-15,-11,-12,-3},{6,-5,14,-3},{12,-12,15,-3},{-1,-17,1,-7},{6,4,19,6},{-18,4,-6,6},{-1,9,1,18}})p.air(r[0],80,r[1],r[2],84,r[3]);
    // Architectural walls are the boundary of the UNION, never the capped ends of a corridor inside a room.
    for(int[] r:new int[][]{{-25,-22,-13,-8},{13,-21,25,-9},{16,-8,24,-1},{-5,-25,5,-16},{18,0,26,10},{-25,1,-17,9}})p.air(r[0]+1,80,r[1]+1,r[2]-1,84,r[3]-1);
    // A side waiting alcove opens into the clinic approach without narrowing its walking route.
    p.air(15,80,-6,17,83,-4);
    for(int x=-7;x<=7;x++)for(int z=-8;z<=10;z++)if(foyer(x,z)){
      p.decor(x,79,z,"school_stone_tile");p.decor(x,85,z,"school_cream_wall");
      boolean edge=!foyer(x-1,z)||!foyer(x+1,z)||!foyer(x,z-1)||!foyer(x,z+1);
      if(!edge)p.air(x,80,z,x,84,z);
      if(edge)for(int y=80;y<85;y++)p.decor(x,y,z,y==80?"school_wainscot":y==84?"school_frieze":"school_cream_wall");
    }
    for(int[] light:new int[][]{{0,-3},{0,4},{-4,0},{4,0}})p.put(light[0],85,light[1],Blocks.SEA_LANTERN);
    // One composed 9×13 rug with a woven border and central floral medallion.
    for(int x=-4;x<=4;x++)for(int z=-5;z<=7;z++)p.atlas(x,79,z,"school_lobby_composite",x+4+(z+5)*9,Direction.SOUTH);
    hallFurnishings(p);
    KindergartenPupils0257.authorToys(p.blocks);
    xDoor(p,-7,-4,"classroom",Direction.EAST);xDoor(p,7,-4,"health",Direction.WEST);
    toolCabinet(p);xDoor(p,7,5,"fortune",Direction.WEST);
    zDoor(p,0,-8,"principal",Direction.SOUTH);zDoor(p,0,10,"exit",Direction.NORTH);
    // Architectural corner piers and properly mounted child paintings.
    for(int[] corner:new int[][]{{-5,-6},{5,-6},{5,8},{-5,8}})ionicColumn(p,corner[0],corner[1],"school_hall_column");
    p.decor(3,82,-7,"school_art_friends",Direction.SOUTH);p.decor(6,82,0,"school_art_family",Direction.WEST);
    // Facing the west-wall utility cupboard, right is north. The notice board
    // sits on continuous masonry there, leaving its former corner to a full Ionic column.
    p.decor(BOARD_PANEL.getX(),BOARD_PANEL.getY(),BOARD_PANEL.getZ(),"school_noticeboard",Direction.EAST);p.decor(6,82,7,"school_art_chao_small",Direction.WEST);
    facade(p);classroom(p);clinic(p);principal(p);fortune(p);market(p);
    // A compact, animated return aperture replaces the empty oversized yard arch.
    p.put(RETURN.getX(),RETURN.getY(),RETURN.getZ(),PortalScenes0251.state(Direction.NORTH,PortalScenes0251.RETURN));
    for(int x:new int[]{-9,9,-18,18})for(int z:new int[]{25,34,42})p.put(x,80,z,z%2==0?Blocks.POTTED_POPPY:Blocks.POTTED_DANDELION);
    // A bounded invisible approach volume closes the demonstrated porch bypass,
    // the return-end walk-around, and flight above the hedges/over the front gable.
    // AIR-only placement preserves all authored leaves, columns, portal frames and roofs.
    for(int x:new int[]{-5,5})for(int z=18;z<=51;z++)for(int y=80;y<103;y++)p.barrierAir(x,y,z);
    for(int x=-5;x<=5;x++)for(int y=80;y<103;y++)p.barrierAir(x,y,51);
    for(int x=-5;x<=5;x++)for(int z=18;z<=51;z++)p.barrierAir(x,102,z);
    for(int x=-5;x<=5;x++)for(int y=86;y<103;y++)p.barrierAir(x,y,18);
    // The ring band belongs on vertical walls. Neutral caps prevent the old
    // cube-all motif from rotating across exposed return/ceiling corner faces.
    p.blocks.replaceAll((at,state)->state.is(KindergartenDecor025.block("school_frieze"))?KindergartenHallThemes0252.block("school_hall_foyer_frieze").defaultBlockState():state);
    return p;
  }
  private static void room(Plan p,int x0,int z0,int x1,int z1,String floor,String wall){
    p.fill(x0,79,z0,x1,79,z1,floor);p.fill(x0,85,z0,x1,85,z1,"school_cream_wall");
    for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++){
      if(x==x0||x==x1||z==z0||z==z1)for(int y=80;y<85;y++){
        if(wall.equals("school_cloud_mural")){
          Direction face=z==z0?Direction.SOUTH:z==z1?Direction.NORTH:x==x0?Direction.EAST:Direction.WEST;
          int u=switch(face){case SOUTH->x-x0;case NORTH->x1-x;case EAST->z1-z;default->z-z0;};
          p.atlas(x,y,z,"school_mountain_composite",Math.floorMod(u,8)+(84-y)*8,face);
        }
        else p.decor(x,y,z,y==80?(floor.equals("school_clinic_tile")?"school_clinic_wainscot":"school_wainscot"):y==84?"school_frieze":wall);
      }
      if(wall.equals("school_cloud_mural"))p.atlas(x,85,z,"school_cloud_composite",Math.floorMod(x-x0,4)+Math.floorMod(z-z0,4)*4,Direction.SOUTH);
      else if(Math.floorMod(x,4)==0&&Math.floorMod(z,4)==0)p.put(x,85,z,Blocks.SEA_LANTERN);
    }
  }
  private enum Hall {
    CLASSROOM("school_classroom_floor","school_hall_classroom_dado","school_cloud_mural","school_hall_classroom_letters","school_cloud_composite",null),
    CLINIC("school_clinic_tile","school_clinic_wainscot","school_hall_clinic_wall","school_hall_clinic_frieze","school_hall_clinic_ceiling","school_hall_clinic_light"),
    PRINCIPAL("school_hall_principal_parquet","school_hall_principal_panel","school_hall_principal_wall","school_hall_principal_frieze","school_hall_principal_ceiling","school_hall_principal_light"),
    FORTUNE("school_hall_fortune_carpet","school_hall_fortune_dado","school_hall_fortune_wall","school_hall_fortune_frieze","school_hall_fortune_ceiling",null),
    MARKET("school_hall_market_flagstone","school_clinic_wainscot","school_hall_market_wall","school_hall_market_frieze","school_hall_market_ceiling",null),
    ENTRANCE("school_stone_tile","school_wainscot","school_cream_wall","school_frieze","school_cream_wall","sea_lantern");
    final String floor,dado,wall,frieze,ceiling,light;
    Hall(String floor,String dado,String wall,String frieze,String ceiling,String light){this.floor=floor;this.dado=dado;this.wall=wall;this.frieze=frieze;this.ceiling=ceiling;this.light=light;}
  }
  private static void corridor(Plan p,int x0,int z0,int x1,int z1,Hall theme){
    for(int x=x0-1;x<=x1+1;x++)for(int z=z0-1;z<=z1+1;z++){
      p.surface(x,79,z,theme.floor);
      if(theme==Hall.CLASSROOM)p.atlas(x,85,z,"school_cloud_composite",Math.floorMod(x,4)+Math.floorMod(z,4)*4,Direction.SOUTH);
      else p.surface(x,85,z,theme.ceiling);
      if(x<x0||x>x1||z<z0||z>z1)for(int y=80;y<85;y++){
        if(theme==Hall.CLASSROOM&&y>=81&&y<=83){
          Direction face=z<z0?Direction.SOUTH:z>z1?Direction.NORTH:x<x0?Direction.EAST:Direction.WEST;
          int u=face.getAxis()==Direction.Axis.Z?(face==Direction.SOUTH?x:-x):(face==Direction.EAST?-z:z);
          p.atlas(x,y,z,"school_mountain_composite",Math.floorMod(u,8)+(84-y)*8,face);
        }else p.surface(x,y,z,y==80?theme.dado:y==84?theme.frieze:theme.wall);
      }
      else p.air(x,80,z,x,84,z);
    }
    // The Fortune passage uses sparse blue-flame lanterns, so the old bright white ceiling grid is gone.
    if(theme.light!=null)for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++)if(Math.floorMod(x+z,5)==0){
      if(theme==Hall.ENTRANCE)p.put(x,85,z,Blocks.SEA_LANTERN);else p.surface(x,85,z,theme.light);
    }
  }
  private static void hallFurnishings(Plan p){
    // Everything is wall-inset or overhead: each corridor keeps its complete three-wide walking floor.
    for(int x:new int[]{11,16})p.put(x,84,5,Blocks.SOUL_LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING,true));
    for(int x:new int[]{-12,-16})p.put(x,84,5,Blocks.SOUL_LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING,true));
    for(int x:new int[]{-2,2})for(int y=81;y<=82;y++)p.put(x,y,-12,Blocks.BOOKSHELF);
    for(int z:new int[]{-10,-14})for(int x:new int[]{-2,2})p.surface(x,82,z,x<0?"school_hall_principal_certificate":"school_hall_principal_award");
    // These cells lie on flat walls of the complete corridor union. The bent
    // route itself contains(-13,-6), so mounting a cube there would suspend it in air.
    p.surface(-10,82,-6,"school_hall_classroom_student_work");p.surface(-9,82,-6,"school_hall_classroom_class_photo");
    p.surface(-16,82,-6,"school_hall_classroom_student_work");
    // The cupboard is unmarked. Only after opening it do the dim utility hall and stock shelves appear.
    for(int x:new int[]{-10,-13,-16})for(int z:new int[]{3,7})p.surface(x,80,z,"school_hall_market_shelf");
  }
  private static void placeDoor(Plan p,int x,int z,String role,Direction face){
    BlockPos center=new BlockPos(x,80,z);Block b=KindergartenDecor025.block("school_door_"+role);
    KindergartenDecor025.SchoolDoorBlock.pairStates(b,center,face,false).forEach((q,state)->p.put(q.getX(),q.getY(),q.getZ(),state));
  }
  private static void xDoor(Plan p,int x,int z,String role,Direction face){
    p.air(x-1,80,z-1,x+1,82,z+1);
    p.decor(x+(face==Direction.EAST?1:-1),81,z,"school_arch_"+role,face);p.decor(x+(face==Direction.EAST?1:-1),83,z,"school_label_"+role,face);
    placeDoor(p,x,z,role,face);
  }
  private static void toolCabinet(Plan p){
    // A freestanding-depth metal utility case stands against ordinary foyer masonry.
    // Closed leaves conceal the whole opening; there is no public sign, colored wall or room arch.
    for(int z=3;z<=7;z++)for(int y=80;y<=83;y++){
      if(z==3||z==7||y==83)p.decor(-7,y,z,y==80?"school_wainscot":"school_cream_wall");
    }
    p.air(-7,80,4,-5,82,6);
    KindergartenDecor025.SchoolDoorBlock.pairStates(KindergartenDecor025.block("school_tool_cabinet"),new BlockPos(-6,80,5),Direction.EAST,false)
        .forEach((q,state)->p.put(q.getX(),q.getY(),q.getZ(),state));
  }
  private static void zDoor(Plan p,int x,int z,String role,Direction face){
    p.air(x-1,80,z-1,x+1,82,z+1);p.decor(x,81,z+(face==Direction.SOUTH?1:-1),"school_arch_"+role,face);p.decor(x,83,z+(face==Direction.SOUTH?1:-1),"school_label_"+role,face);placeDoor(p,x,z,role,face);
  }
  private static void ionicColumn(Plan p,int x,int z,String id){
    for(int y=80;y<=84;y++)p.put(x,y,z,KindergartenDecor025.state(id,Direction.SOUTH).setValue(KindergartenIonicColumn0254.PART,
        y==80?KindergartenIonicColumn0254.Part.BASE:y==84?KindergartenIonicColumn0254.Part.CAPITAL:KindergartenIonicColumn0254.Part.SHAFT));
  }
  private static void facade(Plan p){
    // One low blue school body under a real red sloping roof: four oval windows and six columns.
    for(int x:new int[]{-24,-15,-6,6,15,24})ionicColumn(p,x,19,"school_twisted_column");
    p.fill(-28,85,19,28,85,19,"school_cream_wall");
    for(int x:new int[]{-21,-12,11,20}){p.air(x,81,18,x+1,82,18);p.decor(x,81,18,"school_round_window");p.box(x-1,80,20,x+2,80,20,Blocks.BRICKS);p.put(x,81,20,Blocks.POTTED_POPPY);p.put(x+1,81,20,Blocks.POTTED_DANDELION);}
    p.air(-2,80,19,2,85,20);
    // Real cream masonry closes the entire volume above the three-high doorway.
    // The previous single lintel left an open porch slot from low, oblique front views.
    // Back-plane infill meets the arch and original porch lintel without projecting
    // in front of the centered title panels or hiding their lettering from below.
    p.fill(-2,83,18,2,85,18,"school_cream_wall");p.fill(-2,84,19,2,84,19,"school_cream_wall");
    p.air(-1,80,17,1,82,19);p.decor(0,81,19,"school_arch_exit");
    p.decor(-2,85,19,"school_facade_title_left");p.decor(0,85,19,"school_facade_title_middle");p.decor(2,85,19,"school_facade_title_right");p.decor(0,88,19,"school_emblem");
    placeDoor(p,0,18,"exit",Direction.SOUTH);
    for(int center:new int[]{-18,18}){
      for(int dx=-2;dx<=2;dx++){int top=91-Math.abs(dx);p.fill(center+dx,87,12,center+dx,top-1,14,"school_clapboard");p.fill(center+dx,top,11,center+dx,top,14,"school_red_shingle");}
      // Seat the full-depth casement inside the gable's front wall. Its two-by-two
      // aperture and sill share the masonry plane instead of projecting over the roof.
      p.air(center-1,88,12,center,89,14);
      p.decor(center-1,88,14,"school_round_window");
      p.box(center-1,87,14,center,87,14,Blocks.GREEN_CONCRETE);
    }
    for(int x:new int[]{-22,22}){p.box(x,89,-10,x+1,93,-9,Blocks.STONE_BRICKS);p.box(x-1,94,-11,x+2,94,-8,Blocks.SMOOTH_SANDSTONE);}
  }
  private static void classroom(Plan p){
    // Separate one-voxel painted city pieces keep each roof, tower and canopy
    // clear of its neighbours. The south half remains an actual class/play area.
    p.decor(-23,80,-21,"school_toy_house_red");p.decor(-20,80,-21,"school_toy_house_blue");p.decor(-16,80,-21,"school_toy_house_yellow");
    p.decor(-22,80,-19,"school_toy_tower");p.decor(-17,80,-19,"school_toy_tower");
    p.decor(-24,80,-17,"school_toy_tree");p.decor(-14,80,-17,"school_toy_tree");
    p.put(-18,80,-17,DoctorContent.LESSON_DESK.get());
    // Native ball/rattle play blocks are supplied by KindergartenPupils0257;
    // there is no second decorative ball or intersecting concrete placeholder.
    p.decor(-23,83,-21,"school_art_chao");p.decor(-19,83,-21,"school_art_friends");
  }
  private static void clinic(Plan p){
    // Registration and patient seating occupy a dedicated side alcove off the mint hallway.
    for(int x:new int[]{18,20,22})p.decor(x,80,-7,"school_green_chair");
    p.decor(22,80,-3,"school_green_counter",Direction.WEST);p.decor(22,80,-4,"school_green_counter",Direction.WEST);
    p.decor(22,81,-3,"school_open_book");p.surface(24,82,-3,"school_hall_clinic_registration");p.surface(24,82,-6,"school_hall_clinic_wellness");
    p.put(18,80,-2,Blocks.POTTED_FERN);p.surface(20,85,-4,"school_hall_clinic_light");
    p.decor(21,80,-20,"school_medicine_cabinet");p.put(18,80,-17,DoctorContent.DOCTOR_STATION.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH));
    p.decor(16,80,-15,"school_green_counter");p.decor(16,81,-15,"school_potion_display");p.decor(18,80,-15,"school_pink_chair");
    // Reanchor the reversed two-block beds so their pillow ends face the east wall,
    // while the foot ends and the original x22..24 floor footprint remain unchanged.
    p.decor(23,80,-15,"school_white_exam_bed",Direction.WEST);p.decor(23,80,-18,"school_white_exam_bed",Direction.WEST);p.decor(20,80,-12,"school_pink_stool");
    for(int z:new int[]{-18,-13}){p.air(25,82,z,25,83,z+1);p.decor(25,82,z,"school_round_window",Direction.WEST);}
    // The south panel ends exactly at z-9, the inside face of the south wall.
    // EAST selects the real mirrored tie mesh; its gold tassel still faces the room.
    for(int z:new int[]{-19,-11})for(int y=80;y<=84;y++)p.decor(24,y,z,y==81?"school_green_room_curtain_tie":"school_green_room_curtain",z==-11?Direction.EAST:Direction.WEST);
    // Keep the entire 1.5-block-wide portrait above continuous west-wall backing,
    // clear of the open corridor elbow beginning at z-12.
    p.decor(14,82,-18,"school_art_chao",Direction.EAST);p.decor(14,82,-16,"school_art_family",Direction.EAST);
  }
  private static void principal(Plan p){
    p.box(1,80,-24,3,82,-24,Blocks.BOOKSHELF);p.decor(-3,80,-23,"school_red_counter");p.decor(-3,81,-23,"school_bust");
    p.decor(0,80,-20,"school_principal_desk");p.decor(0,80,-21,"school_green_chair");
    p.decor(-3,82,-24,"school_art_family");p.decor(-1,83,-24,"school_art_chao");p.decor(1,83,-24,"school_art_friends");
    p.surface(-5,82,-20,"school_hall_principal_certificate");p.surface(-5,82,-23,"school_hall_principal_award");
    p.air(5,82,-22,5,83,-21);p.decor(5,82,-22,"school_round_window",Direction.WEST);
    for(int z:new int[]{-24,-18})for(int y=80;y<=84;y++)p.decor(4,y,z,y==81?"school_green_room_curtain_tie":"school_green_room_curtain",z==-18?Direction.EAST:Direction.WEST);
  }
  private static void fortune(Plan p){
    // The right panel is turned180degrees. Its dedicated mirrored tie keeps
    // the dangling gold end on the left and toward the room, not behind the cloth.
    for(int z:new int[]{2,8})for(int y=80;y<=84;y++)p.decor(21,y,z,y==81?"school_purple_room_curtain_tie":"school_purple_room_curtain",z==8?Direction.EAST:Direction.WEST);
    // A thin invisible ceiling-to-altar screen blocks jumping behind the teller, while ray interaction remains clear.
    p.fill(21,81,4,21,84,6,"school_altar_guard");
    p.decor(21,80,5,"school_fortune_altar",Direction.WEST);
    p.decor(22,80,5,"school_fortune_chair",Direction.WEST);p.decor(24,80,8,"school_red_counter");p.decor(24,81,8,"school_book_stack");p.decor(25,82,5,"school_art_chao",Direction.WEST);p.decor(25,82,2,"school_art_family",Direction.WEST);
  }
  private static void market(Plan p){
    p.box(-25,85,1,-17,85,9,Blocks.DEEPSLATE_BRICKS);p.box(-25,80,1,-17,84,1,Blocks.DEEPSLATE_BRICKS);p.box(-25,80,9,-17,84,9,Blocks.DEEPSLATE_BRICKS);p.box(-25,80,1,-25,84,9,Blocks.DEEPSLATE_BRICKS);p.box(-24,79,2,-17,79,8,Blocks.RED_WOOL);
    for(int z=4;z<=6;z++)p.decor(-20,80,z,"school_red_counter",Direction.EAST);p.decor(-20,81,4,"school_potion_display");p.decor(-20,81,6,"school_book_stack");p.decor(-23,80,8,"school_toy_ball");
    p.box(-25,80,2,-25,83,8,Blocks.DARK_OAK_PLANKS);
    for(int z:new int[]{3,7}){p.surface(-24,81,z,"school_hall_market_wall_shelf");p.decor(-24,82,z,"school_potion_display");}
    for(int z:new int[]{4,6}){p.surface(-24,82,z,"school_hall_market_wall_shelf");p.decor(-24,83,z,"school_book_stack");}
    // Hanging playthings and pennants, rather than an empty collection of shelves.
    for(int z:new int[]{3,5,7}){p.box(-22,83,z,-22,84,z,Blocks.CHAIN);p.decor(-22,82,z,z==5?"school_toy_ball":"school_toy_tower");}
    p.put(-18,83,2,Blocks.LANTERN);p.put(-24,83,8,Blocks.LANTERN);
  }
}
