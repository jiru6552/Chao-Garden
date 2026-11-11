package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.hub.HeroCorniceBlock0254;
import com.chaogarden.hub.HeroGardenDecor0253;
import com.chaogarden.hub.HeroGardenRooms0253;
import com.chaogarden.hub.HubContent;
import com.chaogarden.kindergarten.KindergartenContent;
import com.chaogarden.kindergarten.KindergartenDecor025;
import com.chaogarden.kindergarten.KindergartenIonicColumn0254;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.DoubleStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.ArrayVoxelShape;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Actual school ceiling joins, packaged Ionic meshes, and placed Hero cornice orientation. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class ClassicalModelsGameTests0254 {
  private static JsonObject asset(String path){
    try(var in=ClassicalModelsGameTests0254.class.getResourceAsStream("/assets/chao_garden/"+path+".json")){
      if(in==null)throw new IllegalStateException("Missing packaged native asset: "+path);
      return JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
    }catch(java.io.IOException failure){throw new IllegalStateException(failure);}
  }
  private static final class ResourceMesh extends ArrayVoxelShape {
    ResourceMesh(DiscreteVoxelShape grid,double[] x,double[] y,double[] z){super(grid,x,y,z);}
  }
  private static VoxelShape mesh(String id,int turn){
    var boxes=new ArrayList<AABB>();
    for(var value:asset("models/block/"+id).getAsJsonArray("elements")){
      var e=value.getAsJsonObject();var a=e.getAsJsonArray("from");var b=e.getAsJsonArray("to");
      AABB box=new AABB(a.get(0).getAsDouble()/16,a.get(1).getAsDouble()/16,a.get(2).getAsDouble()/16,b.get(0).getAsDouble()/16,b.get(1).getAsDouble()/16,b.get(2).getAsDouble()/16);
      for(int i=0;i<turn;i++)box=new AABB(1-box.maxZ,box.minY,box.minX,1-box.minZ,box.maxY,box.maxX);
      boxes.add(box);
    }
    // Independent resource oracle: fill exact JSON element cells directly,
    // rather than repeating the production balanced-union construction.
    double[] xs=boxes.stream().flatMapToDouble(b->DoubleStream.of(b.minX,b.maxX)).distinct().sorted().toArray();
    double[] ys=boxes.stream().flatMapToDouble(b->DoubleStream.of(b.minY,b.maxY)).distinct().sorted().toArray();
    double[] zs=boxes.stream().flatMapToDouble(b->DoubleStream.of(b.minZ,b.maxZ)).distinct().sorted().toArray();
    var grid=new BitSetDiscreteVoxelShape(xs.length-1,ys.length-1,zs.length-1);
    for(var box:boxes){
      int x0=Arrays.binarySearch(xs,box.minX),x1=Arrays.binarySearch(xs,box.maxX);
      int y0=Arrays.binarySearch(ys,box.minY),y1=Arrays.binarySearch(ys,box.maxY);
      int z0=Arrays.binarySearch(zs,box.minZ),z1=Arrays.binarySearch(zs,box.maxZ);
      for(int x=x0;x<x1;x++)for(int y=y0;y<y1;y++)for(int z=z0;z<z1;z++)grid.fill(x,y,z);
    }
    return new ResourceMesh(grid,xs,ys,zs);
  }
  @GameTest(template="empty",batch="classical0254",timeoutTicks=120)
  public static void everyAuthoredSchoolColumnHasOneMoldedBaseSlenderShaftAndIonicCapitalMeetingItsCeiling(GameTestHelper h){
    var level=h.getLevel().getServer().getLevel(KindergartenContent.DIMENSION);
    h.assertTrue(level!=null&&HubContent.ensure(level),"Actual school scene was not built");
    int total=0;
    for(int[] footprint:new int[][]{{-5,-6},{5,-6},{5,8},{-5,8},{-24,19},{-15,19},{-6,19},{6,19},{15,19},{24,19}}){
      String id=footprint[1]==19?"school_twisted_column":"school_hall_column";
      for(int y=80;y<=84;y++){
        var at=new BlockPos(footprint[0],y,footprint[1]);var state=level.getBlockState(at);
        var part=y==80?KindergartenIonicColumn0254.Part.BASE:y==84?KindergartenIonicColumn0254.Part.CAPITAL:KindergartenIonicColumn0254.Part.SHAFT;
        h.assertTrue(state.is(KindergartenDecor025.block(id))&&state.getValue(KindergartenIonicColumn0254.PART)==part,"School Ionic stack has the wrong structural member at "+at);
        var bounds=state.getShape(level,at).bounds();h.assertTrue(bounds.minY==0&&bounds.maxY==1,"Ionic column has a vertical gap at "+at);
        if(y==84){
          h.assertTrue(state.getShape(level,at).toAabbs().stream().anyMatch(b->b.contains(.5,.999,.5)),"Reduced Ionic abacus lost central ceiling contact");
          h.assertTrue(level.getBlockState(at.above()).isFaceSturdy(level,at.above(),Direction.DOWN),"Ionic capital floats below its ceiling");
        }
      }
      total++;
    }
    h.assertTrue(total==10,"Actual Kindergarten pillar inventory changed");h.succeed();
  }
  @GameTest(template="empty",batch="classical0254",timeoutTicks=120)
  public static void ionicPickingAndCollisionFollowEveryPackagedMemberMeshAcrossAllFourFacings(GameTestHelper h){
    var level=h.getLevel();var at=h.absolutePos(BlockPos.ZERO);
    for(String id:new String[]{"school_hall_column","school_twisted_column"})for(var part:KindergartenIonicColumn0254.Part.values())for(Direction face:Direction.Plane.HORIZONTAL){
      var state=KindergartenDecor025.state(id,face).setValue(KindergartenIonicColumn0254.PART,part);
      var variant=asset("blockstates/"+id).getAsJsonObject("variants").getAsJsonObject("facing="+face.getSerializedName()+",part="+part.getSerializedName());
      String model=variant.get("model").getAsString().substring("chao_garden:block/".length());int turns=variant.has("y")?variant.get("y").getAsInt()/90:0;
      var drawn=mesh(model,turns);var outline=state.getShape(level,at);var physical=state.getCollisionShape(level,at);
      h.assertTrue(!Shapes.joinIsNotEmpty(drawn,outline,BooleanOp.NOT_SAME)&&!Shapes.joinIsNotEmpty(drawn,physical,BooleanOp.NOT_SAME),"Ionic scroll/flute mesh differs from native picking or collision: "+id+" "+face+" "+part);
      if(part==KindergartenIonicColumn0254.Part.SHAFT){
        var bounds=outline.bounds();
        h.assertTrue(bounds.minX>=.25&&bounds.minZ>=.25&&bounds.maxX<=.75&&bounds.maxZ<=.75,
            "Ionic flute ribs overhang the supporting base neck");
      }
      // Every member carries the central vertical load continuously, including
      // the narrow neck hidden behind the newly exposed scroll crowns.
      for(int y=0;y<256;y++){
        final double py=(y+.5)/256;
        h.assertTrue(outline.toAabbs().stream().anyMatch(b->b.contains(.5,py,.5)),
            "Ionic molding floats across a vertical gap in its central support");
      }
      if(part==KindergartenIonicColumn0254.Part.CAPITAL){
        // Independent silhouette probes: former outer abacus and clipping
        // lower plate are air, while each intact upper scroll arc is solid.
        for(double[] point:new double[][]{{.02,.95,.5},{3.0/16,13.2/16,2.7/16},{3.0/16,13.2/16,13.3/16}}){
          double x=point[0],z=point[2];
          for(int i=0;i<turns;i++){double next=1-z;z=x;x=next;}
          final double px=x,pz=z;
          h.assertTrue(outline.toAabbs().stream().noneMatch(b->b.contains(px,point[1],pz)),
              "Ionic abacus/lower plate still projects across the volute silhouette");
        }
        for(double z:new double[]{3.1/16,12.9/16}){
          double x=3.0/16;
          for(int i=0;i<turns;i++){double next=1-z;z=x;x=next;}
          final double px=x,pz=z;
          h.assertTrue(outline.toAabbs().stream().anyMatch(b->b.contains(px,13.1/16,pz)),
              "Ionic volute crown was removed instead of clearing its upper plate");
        }
      }
    }
    h.succeed();
  }
  @GameTest(template="empty",batch="classical0254",timeoutTicks=120)
  public static void bothActualHeroCorniceRingsTurnStraightSoffitsAndFourRealCornerBlocks(GameTestHelper h){
    var level=h.getLevel().getServer().getLevel(HubContent.HERO_GARDEN);
    h.assertTrue(level!=null&&HubContent.ensure(level),"Actual Hero garden was not built");
    int corners=0,total=0;
    for(int[] ring:new int[][]{{-12,6,-4,14},{-25,-9,-22,1}}){
      int x0=ring[0],z0=ring[1],x1=ring[2],z1=ring[3];
      for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++){
        if(x!=x0&&x!=x1&&z!=z0&&z!=z1)continue;
        boolean corner=(x==x0||x==x1)&&(z==z0||z==z1);var at=new BlockPos(x,89,z);var state=level.getBlockState(at);
        h.assertTrue(state.is(HeroGardenDecor0253.block("hero_marble_trim"))&&state.getValue(HeroCorniceBlock0254.CORNER)==corner,"Actual Hero cornice has no real corner member at "+at);
        var face=state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        if(corner){
          Direction first=face,second=face.getCounterClockWise();
          h.assertTrue((first==Direction.NORTH||second==Direction.NORTH)==(z==z0)&&(first==Direction.EAST||second==Direction.EAST)==(x==x1)
              &&(first==Direction.SOUTH||second==Direction.SOUTH)==(z==z1)&&(first==Direction.WEST||second==Direction.WEST)==(x==x0),"Hero corner texture faces its inner roof instead of its two outer sides at "+at);
          corners++;
        }else h.assertTrue(face==(z==z0?Direction.NORTH:x==x1?Direction.EAST:z==z1?Direction.SOUTH:Direction.WEST),"Hero straight soffit pattern crosses its run at "+at);
        h.assertTrue(state.isCollisionShapeFullBlock(level,at),"Decorative Hero soffit lost the solid masonry roof");total++;
      }
    }
    h.assertTrue(corners==8&&total==58,"Hero cornice rings lost perimeter members or exactly four corners each: "+total+" / "+corners);h.succeed();
  }
  private ClassicalModelsGameTests0254(){}
}
