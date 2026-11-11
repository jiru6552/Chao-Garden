package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.care.DoctorContent;
import com.chaogarden.hub.HubContent;
import com.chaogarden.kindergarten.KindergartenContent;
import com.chaogarden.kindergarten.KindergartenDecor025;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Built-school support and orientation checks against the actual packaged native meshes. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class ClinicRefinementGameTests0254 {
  private static JsonObject asset(String path){
    try(var in=ClinicRefinementGameTests0254.class.getResourceAsStream("/assets/chao_garden/"+path+".json")){
      if(in==null)throw new IllegalStateException("Missing packaged native asset: "+path);
      return JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
    }catch(java.io.IOException e){throw new IllegalStateException(e);}
  }
  private static AABB box(JsonObject e){
    var a=e.getAsJsonArray("from");var b=e.getAsJsonArray("to");
    return new AABB(a.get(0).getAsDouble()/16,a.get(1).getAsDouble()/16,a.get(2).getAsDouble()/16,
        b.get(0).getAsDouble()/16,b.get(1).getAsDouble()/16,b.get(2).getAsDouble()/16);
  }
  private static AABB turn(AABB b,int degrees){
    for(int i=0;i<degrees/90;i++)b=new AABB(1-b.maxZ,b.minY,b.minX,1-b.minZ,b.maxY,b.maxX);
    return b;
  }
  private static AABB mesh(String id,int degrees){
    AABB result=null;
    for(var value:asset("models/block/"+id).getAsJsonArray("elements")){
      var b=turn(box(value.getAsJsonObject()),degrees);result=result==null?b:result.minmax(b);
    }
    return result;
  }

  @GameTest(template="empty",batch="clinic0254",timeoutTicks=120)
  public static void completeClinicPortraitMeshesHaveSolidBackingAcrossTheirEntireWidth(GameTestHelper h){
    var player=TestConnections.makeSurvivalServerPlayer(h);var level=player.server.getLevel(KindergartenContent.DIMENSION);
    try{
      h.assertTrue(level!=null&&HubContent.ensure(level),"Actual school was not built");
      for(var mount:new Object[][]{{"school_art_chao",new BlockPos(14,82,-18)},{"school_art_family",new BlockPos(14,82,-16)}}){
        String id=(String)mount[0];BlockPos at=(BlockPos)mount[1];var state=level.getBlockState(at);
        h.assertTrue(state.is(KindergartenDecor025.block(id))&&state.getValue(BlockStateProperties.HORIZONTAL_FACING)==Direction.EAST,"Clinic frame is absent or perpendicular to its backing");
        AABB visible=mesh(id,270).move(at);
        h.assertTrue(Math.abs(visible.minX-14)<.000001,"Frame has an air gap before the actual west wall");
        for(double y=visible.minY+.01;y<visible.maxY;y+=.05)for(double z=visible.minZ+.01;z<visible.maxZ;z+=.05){
          var backing=BlockPos.containing(visible.minX-.001,y,z);
          h.assertTrue(level.getBlockState(backing).isCollisionShapeFullBlock(level,backing),"Part of the actual portrait overhangs the open corridor corner at "+backing);
        }
      }
      h.assertTrue(!level.getBlockState(new BlockPos(14,82,-13)).is(KindergartenDecor025.block("school_art_family")),"The unsupported corridor-elbow frame was retained");
    }finally{player.server.getPlayerList().remove(player);}
    h.succeed();
  }

  @GameTest(template="empty",batch="clinic0254",timeoutTicks=120)
  public static void clinicBedPillowsFaceTheWallAndReversedMeshesKeepTheirPhysicalFootprint(GameTestHelper h){
    var player=TestConnections.makeSurvivalServerPlayer(h);var level=player.server.getLevel(KindergartenContent.DIMENSION);
    try{
      h.assertTrue(level!=null&&HubContent.ensure(level),"Actual school was not built");
      AABB pillow=null,quilt=null;
      for(var value:asset("models/block/school_white_exam_bed").getAsJsonArray("elements")){
        var e=value.getAsJsonObject();var b=box(e);var up=e.getAsJsonObject("faces").getAsJsonObject("up");if(up==null)continue;
        if(up.get("texture").getAsString().equals("#quilt"))quilt=turn(b,90);
        if(up.get("texture").getAsString().equals("#white")&&Math.abs(b.maxY-11/16.)<.000001)pillow=turn(b,90);
      }
      h.assertTrue(pillow!=null&&quilt!=null&&pillow.getCenter().x>quilt.getCenter().x,"The packaged bed's raised pillow still faces the room/foot end");
      for(int z:new int[]{-18,-15}){
        var at=new BlockPos(23,80,z);var state=level.getBlockState(at);
        h.assertTrue(state.is(KindergartenDecor025.block("school_white_exam_bed"))&&state.getValue(BlockStateProperties.HORIZONTAL_FACING)==Direction.WEST,"Clinic bed did not reverse and reanchor");
        var actual=state.getCollisionShape(level,at).bounds().move(at);var drawn=mesh("school_white_exam_bed",90).move(at);
        var expected=new AABB(22,80,z,24,80+15/16.,z+1);
        h.assertTrue(actual.equals(expected)&&drawn.equals(expected),"Turning the bed moved its real collision/mesh into the wall or the aisle");
        h.assertTrue(pillow.move(at).maxX<25,"Pillow penetrates the east wall");
      }
    }finally{player.server.getPlayerList().remove(player);}
    h.succeed();
  }

  @GameTest(template="empty",batch="clinic0254",timeoutTicks=120)
  public static void clinicDiagnosticScreenFacesTheDoctorAndPatients(GameTestHelper h){
    var player=TestConnections.makeSurvivalServerPlayer(h);var level=player.server.getLevel(KindergartenContent.DIMENSION);
    try{
      h.assertTrue(level!=null&&HubContent.ensure(level),"Actual school was not built");
      var at=new BlockPos(18,80,-17);var state=level.getBlockState(at);
      h.assertTrue(state.is(DoctorContent.DOCTOR_STATION.get())&&state.getValue(BlockStateProperties.HORIZONTAL_FACING)==Direction.SOUTH,"Clinic machine still faces away from the doctor's chair");
      var variant=asset("blockstates/doctor_station").getAsJsonObject("variants").getAsJsonObject("facing=south");
      int degrees=variant.get("y").getAsInt();AABB screen=null;
      for(var value:asset("models/block/doctor_station").getAsJsonArray("elements")){
        var e=value.getAsJsonObject();var north=e.getAsJsonObject("faces").getAsJsonObject("north");
        if(north!=null&&north.get("texture").getAsString().equals("#screen"))screen=turn(box(e),degrees).move(at);
      }
      h.assertTrue(screen!=null&&screen.minZ>at.getZ()+.5&&degrees==180,"Packaged machine screen does not rotate toward the south-facing chair/patient side");
      h.assertTrue(level.getBlockState(new BlockPos(18,80,-15)).is(KindergartenDecor025.block("school_pink_chair")),"Doctor's actual seat is no longer in front of the diagnostic screen");
    }finally{player.server.getPlayerList().remove(player);}
    h.succeed();
  }

  @GameTest(template="empty",batch="clinic0254",timeoutTicks=120)
  public static void rightCurtainMeetsTheClinicWallAndBothRightTasselsRemainVisibleOnTheLeft(GameTestHelper h){
    var player=TestConnections.makeSurvivalServerPlayer(h);var level=player.server.getLevel(KindergartenContent.DIMENSION);
    try{
      h.assertTrue(level!=null&&HubContent.ensure(level),"Actual school was not built");
      for(int y=80;y<=84;y++){
        var at=new BlockPos(24,y,-11);var state=level.getBlockState(at);
        h.assertTrue(state.is(KindergartenDecor025.block(y==81?"school_green_room_curtain_tie":"school_green_room_curtain"))&&state.getValue(BlockStateProperties.HORIZONTAL_FACING)==Direction.EAST,"Right Clinic curtain was not moved to its wall-facing anchor");
        AABB b=state.getShape(level,at).bounds().move(at);var wall=new BlockPos(24,y,-9);
        h.assertTrue(Math.abs(b.maxZ-wall.getZ())<.000001&&level.getBlockState(wall).isCollisionShapeFullBlock(level,wall),"Actual curtain leaves an air gap before the south wall");
        h.assertTrue(!level.getBlockState(new BlockPos(24,y,-14)).is(KindergartenDecor025.block("school_green_room_curtain")),"Former floating side panel was retained");
      }
      var state=asset("blockstates/school_green_room_curtain_tie").getAsJsonObject("variants").getAsJsonObject("facing=east");
      String id=state.get("model").getAsString().replace("chao_garden:block/","");int degrees=state.get("y").getAsInt();AABB tassel=null;
      for(var value:asset("models/block/"+id).getAsJsonArray("elements")){
        var e=value.getAsJsonObject();var b=box(e);
        if(Math.abs(b.minY-.25)<.000001&&Math.abs(b.maxY-7/16.)<.000001)tassel=turn(b,degrees);
      }
      h.assertTrue(tassel!=null&&tassel.maxZ<.5&&tassel.maxX<=5.5/16.+.000001,"Right curtain's real dangling gold end is behind the fabric or on the right");
      for(var at:new BlockPos[]{new BlockPos(24,81,-11),new BlockPos(4,81,-18)})h.assertTrue(level.getBlockState(at).getValue(BlockStateProperties.HORIZONTAL_FACING)==Direction.EAST,"Clinic/Principal right tie did not select its mirrored native variant");
      for(var at:new BlockPos[]{new BlockPos(24,81,-19),new BlockPos(4,81,-24)})h.assertTrue(level.getBlockState(at).getValue(BlockStateProperties.HORIZONTAL_FACING)==Direction.WEST,"Left curtain was accidentally mirrored with the right one");
    }finally{player.server.getPlayerList().remove(player);}
    h.succeed();
  }
}
