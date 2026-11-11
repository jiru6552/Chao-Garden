package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.SchoolDoorGeometry0255;
import com.chaogarden.kindergarten.KindergartenDecor025;
import com.chaogarden.kindergarten.KindergartenDecor025.SchoolDoorBlock;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real native rays distinguish the smooth circle from both rectangles and the old seven-step silhouette. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class SchoolDoorCurveGameTests0255 {
  private static com.google.gson.JsonObject resource(String path){
    try(var in=SchoolDoorCurveGameTests0255.class.getResourceAsStream("/assets/chao_garden/"+path+".json")){
      if(in==null)throw new IllegalStateException("Missing packaged curved-door asset: "+path);
      return JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
    }catch(java.io.IOException failure){throw new IllegalStateException(failure);}
  }
  private static VoxelShape union(java.util.List<VoxelShape> pieces,int a,int b){
    if(a==b)return Shapes.empty();if(b-a==1)return pieces.get(a);int m=(a+b)/2;
    return Shapes.joinUnoptimized(union(pieces,a,m),union(pieces,m,b),BooleanOp.OR);
  }
  @GameTest(template="empty",batch="kindergarten_curves0255",timeoutTicks=120)
  public static void allSixPackagedCurvedDoorFamiliesMatchActualNativeSelectionAndCollision(GameTestHelper h){
    int variants=0;
    for(String role:new String[]{"principal","classroom","health","fortune","exit","market"}){
      String id="school_door_"+role;var block=KindergartenDecor025.block(id);
      var arch=resource("models/block/school_arch_"+role);
      h.assertTrue(arch.get("chao_door_profile").getAsString().equals("curve0255")&&arch.get("curve_sections").getAsInt()==96,"Packaged school frame retained the coarse staircase: "+role);
      for(var hinge:DoorHingeSide.values())for(boolean open:new boolean[]{false,true})for(var section:SchoolDoorBlock.DoorSection.values())for(int segment=0;segment<3;segment++){
        String name=id+"_"+hinge.getSerializedName()+"_"+(open?"open":"closed")+"_"+section.getSerializedName()+"_"+segment;
        var model=resource("models/block/"+name);h.assertTrue(model.get("chao_door_profile").getAsString().equals("curve0255"),"Actual state model is not the new fitted curve: "+name);
        var members=new java.util.ArrayList<VoxelShape>();
        for(var value:model.getAsJsonArray("elements")){
          var e=value.getAsJsonObject();var a=e.getAsJsonArray("from");var b=e.getAsJsonArray("to");
          members.add(Shapes.box(a.get(0).getAsDouble()/16,a.get(1).getAsDouble()/16,a.get(2).getAsDouble()/16,b.get(0).getAsDouble()/16,b.get(1).getAsDouble()/16,b.get(2).getAsDouble()/16));
        }
        var drawn=union(members,0,members.size());var state=SchoolDoorBlock.part(block,Direction.SOUTH,hinge,segment,open,section);var at=h.absolutePos(BlockPos.ZERO);
        h.assertTrue(!Shapes.joinIsNotEmpty(drawn,state.getShape(h.getLevel(),at),BooleanOp.NOT_SAME)&&!Shapes.joinIsNotEmpty(drawn,state.getCollisionShape(h.getLevel(),at),BooleanOp.NOT_SAME),"A curved panel has phantom or missing native geometry: "+name);
        variants++;
      }
    }
    h.assertTrue(variants==288,"Curved finite state/model coverage changed");h.succeed();
  }
  private static Vec3 turn(Vec3 value,BlockPos center,Direction face){
    double x=value.x-center.getX()-.5,z=value.z-center.getZ()-.5;
    return switch(face){case NORTH->new Vec3(center.getX()+.5-x,value.y,center.getZ()+.5-z);case WEST->new Vec3(center.getX()+.5-z,value.y,center.getZ()+.5+x);case EAST->new Vec3(center.getX()+.5+z,value.y,center.getZ()+.5-x);default->value;};
  }
  @GameTest(template="activity_test",batch="kindergarten_curves0255",timeoutTicks=120)
  public static void fineCircularCrownPicksBelowAndMissesAboveItsCurveAcrossEveryFacingAndSwing(GameTestHelper h){
    for(int x=1;x<=14;x++)for(int z=1;z<=14;z++)for(int y=1;y<=5;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
    var level=h.getLevel();var center=h.absolutePos(new BlockPos(7,1,7));var block=KindergartenDecor025.block("school_door_exit");
    var player=TestConnections.makeSurvivalServerPlayer(h);player.setNoGravity(true);int rays=0;
    try{
      for(Direction face:Direction.Plane.HORIZONTAL){
        for(boolean open:new boolean[]{false,true}){
          SchoolDoorBlock.pairStates(block,center,face,open).forEach((at,state)->level.setBlock(at,state,Block.UPDATE_ALL));
          for(boolean left:new boolean[]{true,false})for(double distance:new double[]{1,5,10.5,18.5,21.5})for(boolean below:new boolean[]{true,false})for(int side:new int[]{-1,1}){
            // This independent circle equation does not call the generated native
            // height table; the old coarse silhouette fails these tight probes.
            double gx=left?-14.5+distance:30.5-distance;
            double crown=23.99+Math.sqrt(22.5*22.5-(gx-8)*(gx-8));double y=crown+(below?-.9:.2);
            double x=center.getX()+(open?(left?-13.5:29.5):gx)/16,z=center.getZ()+(open?16+distance:15)/16;
            Vec3 target=turn(new Vec3(x,center.getY()+y/16,z),center,face);
            Vec3 normal=turn(new Vec3(center.getX()+.5+(open?1:0),center.getY(),center.getZ()+.5+(open?0:1)),center,face).subtract(new Vec3(center.getX()+.5,center.getY(),center.getZ()+.5)).scale(side*.3);
            var hit=level.clip(new ClipContext(target.add(normal),target.subtract(normal),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
            h.assertTrue((hit.getType()==HitResult.Type.BLOCK)==below,"Native curved crown picked the wrong side of its mathematical contour: "+face+" open="+open+" left="+left+" distance="+distance+" below="+below+" hit="+hit.getType());rays++;
          }
        }
        SchoolDoorBlock.pairStates(block,center,face,false).forEach((at,state)->level.setBlock(at,state,Block.UPDATE_ALL));
        // At both jamb seats a ray a thousandth of a model pixel inboard must
        // hit the fitted closed leaf, including the previously visible slit.
        for(boolean left:new boolean[]{true,false})for(int side:new int[]{-1,1}){
          var target=turn(new Vec3(center.getX()+(left?-14.499:30.499)/16,center.getY()+.5,center.getZ()+15./16),center,face);
          var normal=turn(new Vec3(center.getX()+.5,center.getY(),center.getZ()+1.5),center,face).subtract(new Vec3(center.getX()+.5,center.getY(),center.getZ()+.5)).scale(side*.3);
          var hit=level.clip(new ClipContext(target.add(normal),target.subtract(normal),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
          h.assertTrue(hit.getType()==HitResult.Type.BLOCK,"Closed curved leaf leaves a selectable gap at its frame jamb: "+face+" left="+left+" side="+side);rays++;
        }
        SchoolDoorBlock.pairStates(block,center,face,false).keySet().forEach(at->level.setBlock(at,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL));
      }
    }finally{player.server.getPlayerList().remove(player);}
    h.assertTrue(rays==336,"Curved edge ray coverage changed");h.succeed();
  }
  private SchoolDoorCurveGameTests0255(){}
}
