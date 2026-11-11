package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.hub.HubContent;
import com.chaogarden.kindergarten.KindergartenContent;
import com.chaogarden.kindergarten.KindergartenDecor025;
import com.chaogarden.kindergarten.KindergartenIonicColumn0254;
import com.chaogarden.kindergarten.KindergartenNpc;
import com.chaogarden.kindergarten.KindergartenRooms025;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Actual room layout, packaged purple tie and retained bulletin-board service. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class KindergartenLayoutGameTests0255 {
  private static JsonObject asset(String name){
    try(var stream=KindergartenLayoutGameTests0255.class.getResourceAsStream("/assets/chao_garden/"+name+".json")){
      if(stream==null)throw new IllegalStateException("Missing packaged school asset: "+name);
      return JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
    }catch(java.io.IOException failure){throw new IllegalStateException(failure);}
  }

  @GameTest(template="empty",batch="school0255_layout",timeoutTicks=120)
  public static void noticePanelIsRightOfTheUnmarkedCupboardAndItsOldCornerIsAnIonicStack(GameTestHelper h){
    var level=h.getLevel().getServer().getLevel(KindergartenContent.DIMENSION);
    h.assertTrue(level!=null&&HubContent.ensure(level),"Actual school was not built");
    var panel=KindergartenRooms025.BOARD_PANEL;
    h.assertTrue(panel.equals(new BlockPos(-6,82,0))&&panel.getZ()<3,"Bulletin board did not move to the north/right of the west-wall cupboard");
    var state=level.getBlockState(panel);
    h.assertTrue(state.is(KindergartenDecor025.block("school_noticeboard"))&&state.getValue(BlockStateProperties.HORIZONTAL_FACING)==Direction.EAST,"Notice panel does not face its public foyer");
    var backing=panel.west();h.assertTrue(level.getBlockState(backing).isCollisionShapeFullBlock(level,backing),"Notice panel floats away from its full masonry backing");
    for(int y=80;y<=84;y++){
      var at=new BlockPos(-5,y,8);var column=level.getBlockState(at);
      var part=y==80?KindergartenIonicColumn0254.Part.BASE:y==84?KindergartenIonicColumn0254.Part.CAPITAL:KindergartenIonicColumn0254.Part.SHAFT;
      h.assertTrue(column.is(KindergartenDecor025.block("school_hall_column"))&&column.getValue(KindergartenIonicColumn0254.PART)==part,"Former bulletin corner lacks its complete Ionic member at "+at);
      h.assertTrue(column.getShape(level,at).bounds().minY==0&&column.getShape(level,at).bounds().maxY==1,"Corner column has a vertical gap");
    }
    var ceiling=new BlockPos(-5,85,8);h.assertTrue(level.getBlockState(ceiling).isFaceSturdy(level,ceiling,Direction.DOWN),"New Ionic capital does not meet its ceiling");
    var art=new BlockPos(3,82,-7);h.assertTrue(level.getBlockState(art).is(KindergartenDecor025.block("school_art_friends")),"The displaced friends picture was discarded rather than rehung");
    for(int x=2;x<=4;x++)for(int y=82;y<=83;y++){
      var wall=new BlockPos(x,y,-8);h.assertTrue(level.getBlockState(wall).isCollisionShapeFullBlock(level,wall),"Rehung friends frame overhangs a door or unsupported wall edge at "+wall);
    }
    h.succeed();
  }

  @GameTest(template="empty",batch="school0255_layout",timeoutTicks=120)
  public static void fortuneRightCurtainTurns180AndItsRealGoldEndRemainsRoomFacingOnTheLeft(GameTestHelper h){
    var level=h.getLevel().getServer().getLevel(KindergartenContent.DIMENSION);
    h.assertTrue(level!=null&&HubContent.ensure(level),"Actual Fortune-Teller room was not built");
    for(int y=80;y<=84;y++){
      String id=y==81?"school_purple_room_curtain_tie":"school_purple_room_curtain";
      var right=level.getBlockState(new BlockPos(21,y,8));var left=level.getBlockState(new BlockPos(21,y,2));
      h.assertTrue(right.is(KindergartenDecor025.block(id))&&right.getValue(BlockStateProperties.HORIZONTAL_FACING)==Direction.EAST,"Fortune right curtain has not turned180degrees");
      h.assertTrue(left.is(KindergartenDecor025.block(id))&&left.getValue(BlockStateProperties.HORIZONTAL_FACING)==Direction.WEST,"Fortune left curtain was altered with the right one");
    }
    var binding=asset("blockstates/school_purple_room_curtain_tie").getAsJsonObject("variants").getAsJsonObject("facing=east");
    h.assertTrue(binding.get("model").getAsString().equals("chao_garden:block/school_purple_room_curtain_tie_east")&&binding.get("y").getAsInt()==270,"Native EAST facing does not select the physical reflected purple tie");
    AABB tassel=null;
    for(var value:asset("models/block/school_purple_room_curtain_tie_east").getAsJsonArray("elements")){
      var element=value.getAsJsonObject();var a=element.getAsJsonArray("from");var b=element.getAsJsonArray("to");
      if(!element.getAsJsonObject("faces").getAsJsonObject("south").get("texture").getAsString().equals("#gold")||b.get(1).getAsDouble()>7)continue;
      // Exact native270-degree model turn: X becomes Z, with the expected sign.
      tassel=new AABB(a.get(2).getAsDouble()/16,a.get(1).getAsDouble()/16,1-b.get(0).getAsDouble()/16,
          b.get(2).getAsDouble()/16,b.get(1).getAsDouble()/16,1-a.get(0).getAsDouble()/16);
    }
    h.assertTrue(tassel!=null&&tassel.maxX<6/16.&&tassel.maxZ<.5,"Turned Fortune tassel is buried behind its cloth or remains on the right");
    h.succeed();
  }

  @GameTest(template="empty",batch="school0255_board",timeoutTicks=500)
  public static void movingThePanelRetainsTheOldInvisibleResidentAndOnlyTheNewPanelOpensItsService(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);var level=p.server.getLevel(KindergartenContent.DIMENSION);
    h.assertTrue(level!=null&&HubContent.ensure(level),"Actual school was not built");
    p.teleportTo(level,-4.5,80,.5,90,0);p.hasChangedDimension();p.setNoGravity(true);
    h.runAtTickTime(499,()->p.server.getPlayerList().remove(p));
    h.startSequence().thenWaitUntil(()->{
      KindergartenContent.ensureServices(level);
      var actors=level.getEntitiesOfClass(KindergartenNpc.class,new AABB(KindergartenContent.Role.BOARD.anchor).inflate(4),n->n.role()==KindergartenContent.Role.BOARD);
      h.assertTrue(actors.size()==1&&level.getEntity(actors.getFirst().getId())==actors.getFirst(),"Waiting for the actual board resident publication");
    }).thenExecute(()->{
      try{
        var board=level.getEntitiesOfClass(KindergartenNpc.class,new AABB(KindergartenContent.Role.BOARD.anchor).inflate(4),n->n.role()==KindergartenContent.Role.BOARD).getFirst();
        var id=board.getUUID();board.getPersistentData().putString("Board0255Fixture","retain existing actor");
        board.moveTo(-6.5,80,8.5,0,0);KindergartenContent.ensureServices(level);
        h.assertTrue(level.getEntity(id)==board&&board.position().equals(new Vec3(-6.5,80,.5))&&board.getPersistentData().getString("Board0255Fixture").equals("retain existing actor"),"Old corner resident was replaced or not reanchored to the new notice panel");
        h.assertTrue(board.isInvisible()&&!board.isCustomNameVisible()&&KindergartenContent.describe(p,board)!=null,"New board service is exposed as a visible NPC or unauthorized at its real panel");
        var panel=KindergartenRooms025.BOARD_PANEL;
        var use=new PlayerInteractEvent.RightClickBlock(p,InteractionHand.MAIN_HAND,panel,new BlockHitResult(panel.getCenter(),Direction.EAST,panel,false));
        KindergartenContent.board(use);h.assertTrue(use.isCanceled()&&use.getCancellationResult().consumesAction(),"Actual notice panel no longer opens the native board service");
        var old=new BlockPos(-5,82,8);
        var oldUse=new PlayerInteractEvent.RightClickBlock(p,InteractionHand.MAIN_HAND,old,new BlockHitResult(old.getCenter(),Direction.EAST,old,false));
        KindergartenContent.board(oldUse);h.assertTrue(!oldUse.isCanceled(),"Former board corner still acts as a hidden bulletin button through its new pillar");
        board.getPersistentData().remove("Board0255Fixture");
      }finally{p.server.getPlayerList().remove(p);}
    }).thenSucceed();
  }

  private static AABB toyModelBounds(String name){
    double[] lo={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY};
    double[] hi={Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
    for(var value:asset("models/block/"+name).getAsJsonArray("elements")){
      var e=value.getAsJsonObject();var a=e.getAsJsonArray("from");var b=e.getAsJsonArray("to");
      if(e.has("rotation"))throw new IllegalStateException("Review rotated play-city mesh bounds: "+name);
      for(int i=0;i<3;i++){lo[i]=Math.min(lo[i],a.get(i).getAsDouble()/16);hi[i]=Math.max(hi[i],b.get(i).getAsDouble()/16);}
    }
    return new AABB(lo[0],lo[1],lo[2],hi[0],hi[1],hi[2]);
  }
  private static AABB turnedToy(AABB p,Direction face){return switch(face){
    case NORTH->new AABB(1-p.maxX,p.minY,1-p.maxZ,1-p.minX,p.maxY,1-p.minZ);
    case WEST->new AABB(1-p.maxZ,p.minY,p.minX,1-p.minZ,p.maxY,p.maxX);
    case EAST->new AABB(p.minZ,p.minY,1-p.maxX,p.maxZ,p.maxY,1-p.minX);
    default->p;
  };}
  private static boolean sameBounds(AABB a,AABB b){
    return Math.abs(a.minX-b.minX)<1e-7&&Math.abs(a.minY-b.minY)<1e-7&&Math.abs(a.minZ-b.minZ)<1e-7
        &&Math.abs(a.maxX-b.maxX)<1e-7&&Math.abs(a.maxY-b.maxY)<1e-7&&Math.abs(a.maxZ-b.maxZ)<1e-7;
  }
  @GameTest(template="empty",batch="school0258_city",timeoutTicks=120)
  public static void paintedPlayCityHasNativeRoofBoundsAndSeparateClassroomFootprints(GameTestHelper h){
    var level=h.getLevel().getServer().getLevel(KindergartenContent.DIMENSION);
    h.assertTrue(level!=null&&HubContent.ensure(level),"Actual classroom was not built");
    String[] names={"school_toy_house_red","school_toy_house_blue","school_toy_house_yellow","school_toy_tower","school_toy_tree","school_toy_ball"};
    for(var name:names)for(var face:Direction.Plane.HORIZONTAL){
      var model=turnedToy(toyModelBounds(name),face);var state=KindergartenDecor025.state(name,face);
      h.assertTrue(sameBounds(model,state.getShape(level,BlockPos.ZERO).bounds()),"Toy outline misses its actual roof/canopy: "+name+" "+face);
      h.assertTrue(sameBounds(model,state.getCollisionShape(level,BlockPos.ZERO).bounds()),"Toy collision misses its actual roof/canopy: "+name+" "+face);
    }
    int[][] sites={{-23,-21},{-20,-21},{-16,-21},{-22,-19},{-17,-19},{-24,-17},{-14,-17}};
    String[] placed={names[0],names[1],names[2],names[3],names[3],names[4],names[4]};
    var props=new java.util.ArrayList<AABB>();
    var deskAt=new BlockPos(-18,80,-17);var desk=level.getBlockState(deskAt).getCollisionShape(level,deskAt).bounds().move(deskAt);
    for(int i=0;i<sites.length;i++){
      var at=new BlockPos(sites[i][0],80,sites[i][1]);var state=level.getBlockState(at);
      h.assertTrue(state.is(KindergartenDecor025.block(placed[i]))&&state.getValue(BlockStateProperties.HORIZONTAL_FACING)==Direction.SOUTH,"Authored play-city piece moved or was overwritten: "+at);
      var physical=toyModelBounds(placed[i]).move(at);
      h.assertTrue(physical.minX>-24&&physical.maxX<-13&&physical.minZ>=-21&&physical.maxZ<=-15.5&&physical.minY==80&&physical.maxY<85,"City piece enters wall, ceiling or reserved play floor: "+at);
      h.assertTrue(!physical.intersects(desk),"City piece clips the real lesson desk: "+at);
      for(var other:props)h.assertTrue(!physical.inflate(.015625).intersects(other.inflate(.015625)),"Painted city pieces phase into each other");
      props.add(physical);
    }
    for(int slot=0;slot<2;slot++){
      var at=com.chaogarden.kindergarten.KindergartenPupils0257.toy(slot);
      h.assertTrue(level.getBlockState(at).is(com.chaogarden.toys.ToyContent.block(slot==0?"chao_ball":"chao_rattle")),"Classroom scenery overwrote an actual pupil toy");
    }
    h.assertTrue(level.getBlockState(new BlockPos(-15,80,-20)).isAir()&&level.getBlockState(new BlockPos(-15,81,-20)).isAir(),"Intersecting concrete placeholder remains beside the city houses");
    h.succeed();
  }

  private KindergartenLayoutGameTests0255(){}
}
