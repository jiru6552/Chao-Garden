package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.hub.HubContent;
import com.chaogarden.kindergarten.KindergartenContent;
import com.chaogarden.kindergarten.KindergartenDecor025;
import com.chaogarden.kindergarten.KindergartenDecor025.SchoolDoorBlock;
import com.chaogarden.kindergarten.KindergartenNetwork;
import com.chaogarden.kindergarten.KindergartenNpc;
import com.chaogarden.kindergarten.KindergartenRooms025;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.LinkedHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Native outline rays, ordinary empty-hand use and physical collision for school doors. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class SchoolDoorGameTests0251 {
  private static int drainScreens(ServerPlayer player){
    var channel=(EmbeddedChannel)player.connection.getConnection().channel();
    channel.runPendingTasks();channel.flushOutbound();channel.runPendingTasks();int screens=0;Object packet;
    while((packet=channel.readOutbound())!=null)
      if(packet instanceof ClientboundOpenScreenPacket
          ||packet instanceof ClientboundCustomPayloadPacket custom&&custom.payload() instanceof KindergartenNetwork.Open)screens++;
    return screens;
  }
  private static void assertParts(GameTestHelper h,net.minecraft.server.level.ServerLevel level,KindergartenRooms025.DoorPair pair,boolean open){
    var block=KindergartenDecor025.block(pair.id());
    var expected=SchoolDoorBlock.pairStates(block,pair.center(),pair.facing(),open);
    int count=((SchoolDoorBlock)block).isToolCabinet()?15:21;
    h.assertTrue(expected.size()==count&&SchoolDoorBlock.partCount(block)==count,"Door pair count differs from its independent21-ornate/15-cabinet ownership contract");
    expected.forEach((at,state)->{
      h.assertTrue(level.getBlockState(at).equals(state),"Door pair lost section, hinge, height, facing or synchronized opening: "+pair.id()+" "+at);
      h.assertTrue(((SchoolDoorBlock)block).complete(level,at,state),"Door slice does not recognize its complete paired assembly");
    });
  }
  private static BlockHitResult selectedHit(GameTestHelper h,net.minecraft.server.level.ServerLevel level,ServerPlayer player,BlockPos at){
    var state=level.getBlockState(at);
    boolean cabinet=((SchoolDoorBlock)state.getBlock()).isToolCabinet();
    // Front-opening ornate panels live entirely in their forward TIP/FAR_TIP
    // voxels. An inactive root is not clickable: choose a real visible panel
    // of the same leaf/height before constructing the ordinary standing ray.
    if(state.getShape(level,at).isEmpty()){
      var center=SchoolDoorBlock.pairCenter(state,at);
      for(var entry:SchoolDoorBlock.pairStates(state.getBlock(),center,state.getValue(SchoolDoorBlock.FACING),state.getValue(SchoolDoorBlock.OPEN)).entrySet()){
        var part=entry.getValue();
        if(part.getValue(SchoolDoorBlock.HINGE)==state.getValue(SchoolDoorBlock.HINGE)
            &&part.getValue(SchoolDoorBlock.SEGMENT).equals(state.getValue(SchoolDoorBlock.SEGMENT))
            &&level.getBlockState(entry.getKey()).equals(part)&&!part.getShape(level,entry.getKey()).isEmpty())
          return selectedHit(h,level,player,entry.getKey());
      }
    }
    // Hollow cabinets and stepped leaf tops both contain empty space in their
    // union bounds. Aim at actual native boxes, never an invented rectangle.
    var candidates=state.getShape(level,at).toAabbs();
    for(var local:candidates){
    var bounds=local.move(at);var target=bounds.getCenter();
    Vec3 normal=bounds.getXsize()<bounds.getZsize()?new Vec3(1,0,0):new Vec3(0,0,1);
    double floor=SchoolDoorBlock.pairCenter(state,at).getY()+(cabinet?.125:0);
    // An opened leaf can sit flush against corridor masonry. Select its reachable
    // inward face from a real standing player's eyes, never from inside that wall.
    for(int side:new int[]{1,-1}){
      Vec3 feet=new Vec3(target.x+normal.x*side*1.1,floor,target.z+normal.z*side*1.1);
      if(!level.noCollision(player,player.getBoundingBox().move(feet.subtract(player.position()))))continue;
      player.setPos(feet);player.setDeltaMovement(Vec3.ZERO);Vec3 eye=player.getEyePosition(),aim=target.subtract(eye);
      if(aim.lengthSqr()>player.blockInteractionRange()*player.blockInteractionRange())continue;
      player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,target);
      var hit=level.clip(new ClipContext(eye,target.add(aim.normalize().scale(.15)),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
      if(hit.getType()==HitResult.Type.BLOCK&&hit.getBlockPos().equals(at))return hit;
    }
    }
    h.assertTrue(false,"No unobstructed, in-range standing-player eye ray reaches the visible door slice at "+at);
    throw new IllegalStateException("Unreachable school door test fixture: "+at);
  }
  private static void use(GameTestHelper h,net.minecraft.server.level.ServerLevel level,ServerPlayer player,BlockPos at,Direction face){
    var hit=selectedHit(h,level,player,at);
    h.assertTrue(level.getBlockState(hit.getBlockPos()).useWithoutItem(level,player,hit).consumesAction(),"Ordinary empty-hand door interaction was rejected at "+at);
  }
  private static void adventureUse(GameTestHelper h,net.minecraft.server.level.ServerLevel level,ServerPlayer player,BlockPos at,Direction face){
    var hit=selectedHit(h,level,player,at);
    h.assertTrue(player.gameMode.getGameModeForPlayer()==GameType.ADVENTURE,"Realm door fixture must use normal Adventure permissions");
    h.assertTrue(player.gameMode.useItemOn(player,level,ItemStack.EMPTY,InteractionHand.MAIN_HAND,hit).consumesAction(),"Actual Adventure useItemOn/realm guard rejected the school door");
  }

  @GameTest(template="empty",batch="kindergarten_doors0251",timeoutTicks=180)
  public static void allActualDoorPartsTogglePairedLeavesAndLetAnOrdinaryPlayerWalkThrough(GameTestHelper h){
    var player=TestConnections.makeSurvivalServerPlayer(h);var level=player.server.getLevel(KindergartenContent.DIMENSION);
    h.assertTrue(level!=null&&HubContent.ensure(level),"Actual school was not built");
    player.teleportTo(level,.5,80,.5,0,0);player.hasChangedDimension();player.setNoGravity(true);player.getInventory().clearContent();
    player.setGameMode(GameType.ADVENTURE);
    var original=new LinkedHashMap<BlockPos,BlockState>();
    try{
      h.assertTrue(KindergartenRooms025.doorPairs().size()==7,"Expected six foyer pairs and one exterior pair");
      for(var pair:KindergartenRooms025.doorPairs()){
        var left=pair.leftBase();var right=pair.rightBase();
        SchoolDoorBlock.pairStates(KindergartenDecor025.block(pair.id()),pair.center(),pair.facing(),false).keySet().forEach(at->original.put(at,level.getBlockState(at)));
        var first=level.getBlockState(left);
        h.assertTrue(first.getBlock() instanceof SchoolDoorBlock,"Canonical doorway is not an interactive school door: "+pair.id());
        if(first.getValue(SchoolDoorBlock.OPEN))use(h,level,player,left,pair.facing());
        assertParts(h,level,pair,false);drainScreens(player);
        Vec3 normal=new Vec3(pair.facing().getStepX(),0,pair.facing().getStepZ());
        if(!((SchoolDoorBlock)first.getBlock()).isToolCabinet()){
          var panel=level.getBlockState(left).getShape(level,left).bounds().move(left);
          double towardsFacing=panel.getCenter().subtract(Vec3.atBottomCenterOf(pair.center())).dot(normal);
          h.assertTrue(Math.abs(towardsFacing-7./16)<1e-6,"Actual school leaf is not at the requested outside/lobby edge of its frame: "+pair.id());
        }
        Vec3 center=Vec3.atBottomCenterOf(pair.center()).add(0,((SchoolDoorBlock)first.getBlock()).isToolCabinet()?.125:0,0),start=center.subtract(normal.scale(1.1));
        player.setPos(start);player.setDeltaMovement(Vec3.ZERO);
        player.move(MoverType.PLAYER,normal.scale(2.2));
        h.assertTrue(player.position().subtract(center).dot(normal)<.2,"Closed school doors did not physically block the player: "+pair.id());
        // Exercise ServerPlayerGameMode and HubContent's real RightClickBlock
        // guard before the lower-level per-segment behavior checks.
        adventureUse(h,level,player,left,pair.facing());assertParts(h,level,pair,true);
        adventureUse(h,level,player,right.above(1),pair.facing());assertParts(h,level,pair,false);
        for(int part=0;part<6;part++){
          BlockPos at=(part<3?left:right).above(part%3);
          use(h,level,player,at,pair.facing());boolean open=part%2==0;assertParts(h,level,pair,open);
          if(open){
            player.setPos(start);player.setDeltaMovement(Vec3.ZERO);player.move(MoverType.PLAYER,normal.scale(2.2));
            h.assertTrue(player.position().subtract(center).dot(normal)>.85,"Open school doors still blocked real player movement: "+pair.id());
          }
        }
        assertParts(h,level,pair,false);
        h.assertTrue(player.containerMenu==player.inventoryMenu&&drainScreens(player)==0,"Using a door unexpectedly opened staff or container UI");
      }
    }finally{
      // Other school tests may have intentionally left a pair open; restore exact prior states.
      original.forEach((at,state)->level.setBlock(at,state,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE));
      if(HubContent.hasReturnJournal(player))HubContent.returnHome(player);player.server.getPlayerList().remove(player);
    }
    h.succeed();
  }

  @GameTest(template="empty",batch="kindergarten_doors0251",timeoutTicks=180)
  public static void realMetalCabinetKeepsItsCaseAndAllowsGroundedAdventurePassage(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);var level=p.server.getLevel(KindergartenContent.DIMENSION);
    h.assertTrue(level!=null&&HubContent.ensure(level),"Actual school was not built");
    var pair=KindergartenRooms025.doorPairs().stream().filter(q->q.id().equals("school_tool_cabinet")).findFirst().orElseThrow();
    var block=(SchoolDoorBlock)KindergartenDecor025.block(pair.id());
    var original=new LinkedHashMap<BlockPos,BlockState>();SchoolDoorBlock.pairStates(block,pair.center(),pair.facing(),false).keySet().forEach(q->original.put(q,level.getBlockState(q)));
    try{
      h.assertTrue(block.isToolCabinet(),"Hidden entrance is still an ordinary school doorway");
      for(int z:new int[]{3,7})for(int y=80;y<=83;y++)h.assertTrue(level.getBlockState(new BlockPos(-7,y,z)).is(KindergartenDecor025.block(y==80?"school_wainscot":"school_cream_wall")),"Cabinet has a conspicuous colored public surround");
      var originalState=level.getBlockState(pair.leftBase());if(originalState.getValue(SchoolDoorBlock.OPEN))use(h,level,p,pair.leftBase(),pair.facing());
      p.setGameMode(GameType.ADVENTURE);p.getInventory().clearContent();p.setNoGravity(false);p.teleportTo(level,-4.2,80,5.5,90,0);p.hasChangedDimension();p.setOnGround(true);
      for(int step=0;step<26;step++){p.move(MoverType.PLAYER,new Vec3(-.15,-.08,0));p.setOnGround(true);}
      h.assertTrue(p.getX()>-5.3,"Closed metal leaves did not block ordinary grounded movement");
      adventureUse(h,level,p,pair.leftBase().above(1),pair.facing());assertParts(h,level,pair,true);
      for(var at:java.util.List.of(pair.leftBase(),pair.leftBase().above(2),pair.center(),pair.center().above(2),pair.rightBase()))h.assertTrue(!level.getBlockState(at).getShape(level,at).isEmpty(),"Opening the cabinet erased its fixed steel case at "+at);
      p.setPos(-4.2,80,5.5);p.setDeltaMovement(Vec3.ZERO);p.setOnGround(true);
      double highest=80;
      for(int step=0;step<26;step++){p.move(MoverType.PLAYER,new Vec3(-.15,-.08,0));highest=Math.max(highest,p.getY());p.setOnGround(true);}
      h.assertTrue(p.getX()<-7.8&&highest>=80.124&&highest<80.2,"A normal grounded player cannot step over the cabinet sill into the hidden hallway: "+p.position());
      h.assertTrue(!p.noPhysics&&p.getBbHeight()>1.7&&p.containerMenu==p.inventoryMenu,"Cabinet passage bypassed normal player collision or opened a menu");
    }finally{original.forEach((q,s)->level.setBlock(q,s,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE));p.server.getPlayerList().remove(p);}
    h.succeed();
  }

  @GameTest(template="activity_test",batch="kindergarten_doors0251",timeoutTicks=100)
  public static void ordinaryBlockItemPlacementAndBreakingPreserveOneCompletePair(GameTestHelper h){
    for(int x=2;x<=12;x++)for(int z=2;z<=12;z++){
      h.setBlock(new BlockPos(x,0,z),Blocks.STONE);for(int y=1;y<=5;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
    }
    var level=h.getLevel();var player=TestConnections.makeSurvivalServerPlayer(h);player.getInventory().clearContent();player.setNoGravity(true);
    var floor=h.absolutePos(new BlockPos(6,0,6));var base=floor.above();var block=KindergartenDecor025.block("school_door_classroom");
    var stack=new ItemStack(KindergartenDecor025.item("school_door_classroom"),2);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
    player.teleportTo(level,base.getX()+1.5,base.getY(),base.getZ()+3.5,180,0);
    var hit=new BlockHitResult(Vec3.atCenterOf(floor).add(0,.5,0),Direction.UP,floor,false);
    try{
      h.assertTrue(player.gameMode.useItemOn(player,level,stack,InteractionHand.MAIN_HAND,hit).consumesAction(),"Ordinary BlockItem use could not place the school door");
      h.assertTrue(stack.getCount()==1,"Placing twenty-one native slices did not consume exactly one door item");
      var placed=level.getBlockState(base);h.assertTrue(placed.is(block),"The clicked lower door part was not placed");
      Direction face=placed.getValue(SchoolDoorBlock.FACING);var center=base.relative(face.getCounterClockWise());
      var pair=new KindergartenRooms025.DoorPair(center,"school_door_classroom",face);assertParts(h,level,pair,false);
      h.assertTrue(player.gameMode.destroyBlock(pair.rightBase().above(1)),"Survival player could not break an upper paired-door part");
      for(var at:SchoolDoorBlock.pairStates(block,pair.center(),face,false).keySet())
        h.assertTrue(!level.getBlockState(at).is(block),"Breaking one part left an orphan school-door segment");
      int drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(base).inflate(4),e->e.getItem().is(stack.getItem())).stream().mapToInt(e->e.getItem().getCount()).sum();
      h.assertTrue(drops==1,"Breaking one paired assembly duplicated or lost its one door item");
      var obstruction=pair.rightBase().above(2);level.setBlock(obstruction,Blocks.STONE.defaultBlockState(),Block.UPDATE_ALL);
      player.gameMode.useItemOn(player,level,stack,InteractionHand.MAIN_HAND,hit);
      h.assertTrue(stack.getCount()==1&&!level.getBlockState(base).is(block)&&level.getBlockState(obstruction).is(Blocks.STONE),"Blocked pair placement consumed an item, left partial parts or destroyed the obstruction");
      for(var at:SchoolDoorBlock.pairStates(block,pair.center(),face,false).keySet())
        h.assertTrue(!level.getBlockState(at).is(block),"Failed placement left an orphan paired-door segment");
      level.setBlock(obstruction,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
      for(int height=0;height<3;height++){
        var middle=pair.center().above(height);level.setBlock(middle,Blocks.STONE.defaultBlockState(),Block.UPDATE_ALL);
        player.gameMode.useItemOn(player,level,stack,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(stack.getCount()==1&&level.getBlockState(middle).is(Blocks.STONE),"Solid middle-column placement consumed a door item or destroyed its obstruction");
        for(var at:SchoolDoorBlock.pairStates(block,pair.center(),face,false).keySet())
          h.assertTrue(!level.getBlockState(at).is(block),"Solid middle-column placement admitted a partial door assembly");
        level.setBlock(middle,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
      }
      for(var entry:SchoolDoorBlock.pairStates(block,pair.center(),face,false).entrySet())if(entry.getValue().getValue(SchoolDoorBlock.SECTION).ordinal()>=SchoolDoorBlock.DoorSection.TIP.ordinal()){
        var swing=entry.getKey();level.setBlock(swing,Blocks.STONE.defaultBlockState(),Block.UPDATE_ALL);
        player.gameMode.useItemOn(player,level,stack,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(stack.getCount()==1&&level.getBlockState(swing).is(Blocks.STONE),"Blocked swing-column placement consumed a door item or destroyed masonry");
        for(var at:SchoolDoorBlock.pairStates(block,pair.center(),face,false).keySet())h.assertTrue(!level.getBlockState(at).is(block),"Blocked swing-cell placement left an orphan native door slice");
        level.setBlock(swing,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
      }
    }finally{player.server.getPlayerList().remove(player);}
    h.succeed();
  }

  private static Vec3 turn(Vec3 point,BlockPos center,Direction face){
    double x=point.x-center.getX()-.5,z=point.z-center.getZ()-.5;
    return switch(face){
      case WEST->new Vec3(center.getX()+.5-z,point.y,center.getZ()+.5+x);
      case NORTH->new Vec3(center.getX()+.5-x,point.y,center.getZ()+.5-z);
      case EAST->new Vec3(center.getX()+.5+z,point.y,center.getZ()+.5-x);
      default->point;
    };
  }
  @GameTest(template="activity_test",batch="kindergarten_doors0252",timeoutTicks=120)
  public static void nativeRaysAndClicksHitBothSidesAtCentersAndEdgesOfEveryFacingAndHeight(GameTestHelper h){
    for(int x=1;x<=14;x++)for(int z=1;z<=14;z++)for(int y=1;y<=5;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
    var level=h.getLevel();var player=TestConnections.makeSurvivalServerPlayer(h);player.setNoGravity(true);player.getInventory().clearContent();
    var center=h.absolutePos(new BlockPos(7,1,7));var block=KindergartenDecor025.block("school_door_classroom");int rays=0;
    try{
      for(var face:Direction.Plane.HORIZONTAL){
        var pair=new KindergartenRooms025.DoorPair(center,"school_door_classroom",face);
        for(boolean open:new boolean[]{false,true})for(var hinge:DoorHingeSide.values())for(int height=0;height<3;height++)for(double distance:new double[]{height==2?6:.5,10.75,21})for(int side:new int[]{-1,1}){
          SchoolDoorBlock.pairStates(block,center,face,open).forEach((at,state)->level.setBlock(at,state,Block.UPDATE_ALL));
          boolean left=hinge==DoorHingeSide.LEFT;double x,z;
          if(open){x=center.getX()+(left?-13.5:29.5)/16;z=center.getZ()+(16+distance)/16;}
          else{x=center.getX()+(left?-14.5+distance:30.5-distance)/16;z=center.getZ()+15./16;}
          Vec3 target=turn(new Vec3(x,center.getY()+height+(height==2?.125:.5),z),center,face);
          Vec3 normal=turn(new Vec3(center.getX()+.5+(open?(left?1:-1):0),center.getY(),center.getZ()+.5+(open?0:1)),center,face)
              .subtract(new Vec3(center.getX()+.5,center.getY(),center.getZ()+.5)).scale(side);
          var hit=level.clip(new ClipContext(target.add(normal.scale(1.2)),target.subtract(normal.scale(.2)),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
          var owner=BlockPos.containing(target);
          h.assertTrue(hit.getType()==HitResult.Type.BLOCK&&hit.getBlockPos().equals(owner),"Native ray missed visible "+face+" "+hinge+" open="+open+" height="+height+" distance="+distance+" side="+side+" expected="+owner+" got="+hit.getBlockPos());
          h.assertTrue(player.gameMode.useItemOn(player,level,ItemStack.EMPTY,InteractionHand.MAIN_HAND,hit).consumesAction(),"Native ray-selected door click was rejected");
          assertParts(h,level,pair,!open);rays++;
        }
        // The upper outer curved cuts are visibly empty. Both viewing sides must
        // ray through them instead of selecting the old full-height rectangle.
        for(boolean open:new boolean[]{false,true})for(var hinge:DoorHingeSide.values())for(int side:new int[]{-1,1}){
          SchoolDoorBlock.pairStates(block,center,face,open).forEach((at,state)->level.setBlock(at,state,Block.UPDATE_ALL));
          boolean left=hinge==DoorHingeSide.LEFT;
          double x=center.getX()+(open?(left?-13.5:29.5):(left?-14.:30.))/16,z=center.getZ()+(open?16.5:15)/16;
          Vec3 target=turn(new Vec3(x,center.getY()+2.5,z),center,face);
          Vec3 normal=turn(new Vec3(center.getX()+.5+(open?1:0),center.getY(),center.getZ()+.5+(open?0:1)),center,face)
              .subtract(new Vec3(center.getX()+.5,center.getY(),center.getZ()+.5)).scale(side);
          var cut=level.clip(new ClipContext(target.add(normal.scale(.35)),target.subtract(normal.scale(.35)),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
          h.assertTrue(cut.getType()==HitResult.Type.MISS,"Curved upper leaf cut retained a phantom rectangular hitbox: "+face+" "+hinge+" open="+open);
        }
        for(boolean open:new boolean[]{false,true}){
          SchoolDoorBlock.pairStates(block,center,face,open).forEach((at,state)->{
            level.setBlock(at,state,Block.UPDATE_ALL);var shape=state.getShape(level,at);var collider=state.getCollisionShape(level,at);
            h.assertTrue(shape.isEmpty()==collider.isEmpty()&&(shape.isEmpty()||near(shape.bounds(),collider.bounds())),"Door collision differs from the visible native outline");
            if(!shape.isEmpty()){
              var b=shape.bounds();h.assertTrue(b.minX>=0&&b.minY>=0&&b.minZ>=0&&b.maxX<=1&&b.maxY<=1&&b.maxZ<=1,"Door selection escapes its owning voxel");
            }
          });
          Vec3 from,to;
          if(open){from=new Vec3(center.getX()+.5,center.getY()+1.5,center.getZ()-1);to=from.add(0,0,4);}
          else{from=new Vec3(center.getX()-1.5,center.getY()+1.5,center.getZ()+1.5);to=from.add(4,0,0);}
          var miss=level.clip(new ClipContext(turn(from,center,face),turn(to,center,face),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
          h.assertTrue(miss.getType()==HitResult.Type.MISS,"Inactive door slices left a phantom selectable air panel");
        }
        // Clear before rotating the next fixture; no old swing slices may masquerade as walls.
        SchoolDoorBlock.pairStates(block,center,face,false).keySet().forEach(at->level.setBlock(at,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL));
      }
      h.assertTrue(rays==288,"Native door ray matrix did not execute all288 geometrically distinct cases");
    }finally{player.server.getPlayerList().remove(player);}
    h.succeed();
  }

  private static LinkedHashMap<BlockPos,BlockState> legacyParts(Block block,BlockPos center,Direction face,boolean open,boolean rear){
    var old=new LinkedHashMap<BlockPos,BlockState>();
    SchoolDoorBlock.pairStates(block,center,face,open).forEach((at,state)->{
      var section=state.getValue(SchoolDoorBlock.SECTION);
      if(section!=SchoolDoorBlock.DoorSection.FAR_TIP)
        old.put(rear&&section==SchoolDoorBlock.DoorSection.TIP?at.relative(face.getOpposite(),2):at,state);
    });
    return old;
  }

  @GameTest(template="activity_test",batch="kindergarten_doors0252",timeoutTicks=100)
  public static void legacySixRootPairsExpandConservativelyAndNewSwingCellsCannotDestroyMasonry(GameTestHelper h){
    for(int x=2;x<=12;x++)for(int z=2;z<=12;z++)for(int y=1;y<=5;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
    var level=h.getLevel();var player=TestConnections.makeSurvivalServerPlayer(h);player.setNoGravity(true);player.getInventory().clearContent();
    var center=h.absolutePos(new BlockPos(6,1,6));var block=KindergartenDecor025.block("school_door_classroom");var face=Direction.SOUTH;
    var parts=SchoolDoorBlock.pairStates(block,center,face,false);var left=SchoolDoorBlock.leftBase(center,face);var obstruction=left.relative(SchoolDoorBlock.swingDirection(block,face)).above(1);
    try{
      parts.forEach((at,state)->{if(state.getValue(SchoolDoorBlock.SECTION)==SchoolDoorBlock.DoorSection.LEAF)level.setBlock(at,state,Block.UPDATE_ALL);});
      level.setBlock(obstruction,Blocks.STONE.defaultBlockState(),Block.UPDATE_ALL);
      var hit=selectedHit(h,level,player,left);h.assertTrue(!level.getBlockState(left).useWithoutItem(level,player,hit).consumesAction(),"Legacy expansion overwrote an occupied swing cell");
      h.assertTrue(level.getBlockState(obstruction).is(Blocks.STONE),"Door expansion destroyed corridor masonry");
      level.setBlock(obstruction,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);use(h,level,player,left,face);
      assertParts(h,level,new KindergartenRooms025.DoorPair(center,"school_door_classroom",face),true);
      parts.keySet().forEach(at->level.setBlock(at,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL));
      // Both historical fifteen-part layouts retain their exact saved roots:
      // 0.25.7 rear tips and the older forward tips. New forward TIP/FAR_TIP
      // reservation must be completely clear before either layout is changed.
      for(Direction facing:Direction.Plane.HORIZONTAL)for(boolean open:new boolean[]{false,true})for(boolean rear:new boolean[]{true,false}){
        var current=SchoolDoorBlock.pairStates(block,center,facing,open);var old=legacyParts(block,center,facing,open,rear);
        var root=SchoolDoorBlock.leftBase(center,facing);
        var clicked=open?old.entrySet().stream().filter(e->e.getValue().getValue(SchoolDoorBlock.SECTION)==SchoolDoorBlock.DoorSection.TIP
            &&e.getValue().getValue(SchoolDoorBlock.HINGE)==DoorHingeSide.LEFT).findFirst().orElseThrow().getKey():root;
        for(var section:new SchoolDoorBlock.DoorSection[]{SchoolDoorBlock.DoorSection.TIP,SchoolDoorBlock.DoorSection.FAR_TIP}){
          var obstacle=current.entrySet().stream().filter(e->e.getValue().getValue(SchoolDoorBlock.SECTION)==section&&!old.containsKey(e.getKey())).findFirst();
          if(obstacle.isEmpty())continue; // Forward legacy TIP cells are already owned, not an exclusive new footprint.
          old.forEach((at,state)->level.setBlock(at,state,Block.UPDATE_ALL));var occupied=obstacle.get().getKey();
          level.setBlock(occupied,Blocks.STONE.defaultBlockState(),Block.UPDATE_ALL);var blocked=selectedHit(h,level,player,clicked);
          h.assertTrue(!level.getBlockState(blocked.getBlockPos()).useWithoutItem(level,player,blocked).consumesAction(),"Saved fifteen-part pair overwrote occupied new "+section+" space");
          old.forEach((at,state)->h.assertTrue(level.getBlockState(at).equals(state),"Blocked saved-pair migration changed an original part"));
          h.assertTrue(level.getBlockState(occupied).is(Blocks.STONE),"Saved-pair migration destroyed unrelated masonry");
          level.setBlock(occupied,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
          old.keySet().forEach(at->level.setBlock(at,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL));
        }
        old.forEach((at,state)->level.setBlock(at,state,Block.UPDATE_ALL));use(h,level,player,clicked,facing);
        assertParts(h,level,new KindergartenRooms025.DoorPair(center,"school_door_classroom",facing),!open);
        for(var at:old.keySet())if(!current.containsKey(at))h.assertTrue(level.getBlockState(at).isAir(),"Saved-pair migration left an orphan historical tip");
        current.keySet().forEach(at->level.setBlock(at,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL));
      }
    }finally{player.server.getPlayerList().remove(player);}
    h.succeed();
  }

  @GameTest(template="activity_test",batch="kindergarten_doors0252",timeoutTicks=100)
  public static void breakingAnyCurrentOrHistoricalNativeSliceDropsExactlyOnePairAndLeavesNoOrphans(GameTestHelper h){
    for(int x=2;x<=12;x++)for(int z=2;z<=12;z++)for(int y=1;y<=5;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
    var level=h.getLevel();var player=TestConnections.makeSurvivalServerPlayer(h);player.setNoGravity(true);player.getInventory().clearContent();
    var center=h.absolutePos(new BlockPos(6,1,6));var block=KindergartenDecor025.block("school_door_classroom");var item=KindergartenDecor025.item("school_door_classroom");
    var area=new AABB(center).inflate(5);
    try{
      for(Direction facing:Direction.Plane.HORIZONTAL)for(boolean open:new boolean[]{false,true})for(int layout=0;layout<3;layout++){
        var assembly=layout==0?SchoolDoorBlock.pairStates(block,center,facing,open):legacyParts(block,center,facing,open,layout==1);
        h.assertTrue(assembly.size()==(layout==0?21:15),"Current/historical fixture owns the wrong number of native cells");
        for(var broken:assembly.keySet()){
          level.getEntitiesOfClass(ItemEntity.class,area).forEach(ItemEntity::discard);
          assembly.forEach((at,state)->level.setBlock(at,state,Block.UPDATE_ALL));
          h.assertTrue(player.gameMode.destroyBlock(broken),"Survival destruction failed for native slice "+broken);
          for(var at:assembly.keySet())h.assertTrue(!level.getBlockState(at).is(block),"Breaking native slice "+broken+" left orphan "+at);
          int count=level.getEntitiesOfClass(ItemEntity.class,area,e->e.getItem().is(item)).stream().mapToInt(e->e.getItem().getCount()).sum();
          h.assertTrue(count==1,"Breaking native slice "+broken+" dropped "+count+" pair items instead of one");
        }
      }
    }finally{level.getEntitiesOfClass(ItemEntity.class,area).forEach(ItemEntity::discard);player.server.getPlayerList().remove(player);}
    h.succeed();
  }

  @GameTest(template="empty",batch="kindergarten_doors0251",timeoutTicks=400)
  public static void fortuneGuardStopsJumpingWithoutHidingTheTellerOrBlockingHerService(GameTestHelper h){
    var player=TestConnections.makeSurvivalServerPlayer(h);var level=player.server.getLevel(KindergartenContent.DIMENSION);
    h.assertTrue(level!=null&&HubContent.ensure(level),"Actual school was not built");
    player.teleportTo(level,19.4,80,5.5,-90,15);player.hasChangedDimension();player.setNoGravity(true);player.getInventory().clearContent();player.setGameMode(GameType.ADVENTURE);
    h.startSequence().thenWaitUntil(()->{
      KindergartenContent.ensureServices(level);
      var staff=level.getEntitiesOfClass(KindergartenNpc.class,new AABB(KindergartenContent.Role.FORTUNE.anchor).inflate(3),n->n.role()==KindergartenContent.Role.FORTUNE);
      h.assertTrue(staff.size()==1,"Actual Fortune-Teller is not loaded once yet");
    }).thenExecute(()->{
      try{
        var context=CollisionContext.of(player);
        for(int y=81;y<=84;y++)for(int z=4;z<=6;z++){
          var at=new BlockPos(21,y,z);var state=level.getBlockState(at);
          h.assertTrue(state.is(KindergartenDecor025.block("school_altar_guard")),"Actual anti-jump guard is absent at "+at);
          h.assertTrue(state.getShape(level,at,context).isEmpty()&&state.getVisualShape(level,at,context).isEmpty(),"Invisible guard blocks the visible outline/camera ray");
          h.assertTrue(state.getCollisionShape(level,at,context).bounds().equals(new AABB(0,0,0,1,1,1)),"Guard lost full physical collision");
        }
        player.setPos(19.4,80,5.5);Vec3 eye=player.getEyePosition(),target=new Vec3(22.5,80.8,5.5);
        var ray=level.clip(new ClipContext(eye,target,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
        h.assertTrue(ray.getType()==HitResult.Type.MISS,"Real eye-to-teller outline ray was blocked by "+level.getBlockState(ray.getBlockPos())+" at "+ray.getBlockPos());
        player.setPos(19.4,81,5.5);player.setDeltaMovement(Vec3.ZERO);player.move(MoverType.PLAYER,new Vec3(3.1,0,0));
        h.assertTrue(player.getX()<21,"A jumping-height player crossed the Fortune-Teller guard");
        var teller=level.getEntitiesOfClass(KindergartenNpc.class,new AABB(KindergartenContent.Role.FORTUNE.anchor).inflate(3),n->n.role()==KindergartenContent.Role.FORTUNE).getFirst();
        drainScreens(player);player.interactOn(teller,InteractionHand.MAIN_HAND);
        var channel=(EmbeddedChannel)player.connection.getConnection().channel();channel.runPendingTasks();channel.flushOutbound();channel.runPendingTasks();
        KindergartenNetwork.Open opened=null;Object out;
        while((out=channel.readOutbound())!=null)if(out instanceof ClientboundCustomPayloadPacket packet&&packet.payload() instanceof KindergartenNetwork.Open view)opened=view;
        h.assertTrue(opened!=null&&opened.npc()==teller.getId()&&opened.role().equals("FORTUNE"),"The guard prevented an ordinary authorized Fortune service interaction");
      }finally{
        if(HubContent.hasReturnJournal(player))HubContent.returnHome(player);player.server.getPlayerList().remove(player);
      }
      h.succeed();
    });
  }

  private static AABB rotateBounds(AABB south,Direction facing){
    double angle=switch(facing){case WEST->Math.PI/2;case NORTH->Math.PI;case EAST->-Math.PI/2;default->0;};
    double c=Math.cos(angle),s=Math.sin(angle),minX=Double.POSITIVE_INFINITY,minZ=minX,maxX=Double.NEGATIVE_INFINITY,maxZ=maxX;
    for(double x:new double[]{south.minX,south.maxX})for(double z:new double[]{south.minZ,south.maxZ}){
      double X=.5+(x-.5)*c-(z-.5)*s,Z=.5+(x-.5)*s+(z-.5)*c;
      minX=Math.min(minX,X);maxX=Math.max(maxX,X);minZ=Math.min(minZ,Z);maxZ=Math.max(maxZ,Z);
    }
    return new AABB(minX,south.minY,minZ,maxX,south.maxY,maxZ);
  }
  private static boolean near(AABB a,AABB b){
    return Math.abs(a.minX-b.minX)<1e-6&&Math.abs(a.minY-b.minY)<1e-6&&Math.abs(a.minZ-b.minZ)<1e-6
        &&Math.abs(a.maxX-b.maxX)<1e-6&&Math.abs(a.maxY-b.maxY)<1e-6&&Math.abs(a.maxZ-b.maxZ)<1e-6;
  }

  @GameTest(template="activity_test",batch="kindergarten_doors0251",timeoutTicks=100)
  public static void tabletopPropsDoNotLeaveInvisibleRayOrBodyObstaclesAboveTheirNativeMeshes(GameTestHelper h){
    for(int x=2;x<=12;x++)for(int z=2;z<=12;z++){
      h.setBlock(new BlockPos(x,0,z),Blocks.STONE);for(int y=1;y<=5;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
    }
    var level=h.getLevel();var player=TestConnections.makeSurvivalServerPlayer(h);player.setNoGravity(true);player.getInventory().clearContent();
    var counter=h.absolutePos(new BlockPos(6,0,6));var at=counter.above();
    level.setBlock(counter,KindergartenDecor025.state("school_red_counter",Direction.SOUTH),Block.UPDATE_ALL);
    String[] ids={"school_crystal_ball","school_globe","school_book_stack","school_open_book","school_potion_display","school_candelabra","school_quill","school_bust"};
    try{
      for(String id:ids){
        var south=KindergartenDecor025.state(id,Direction.SOUTH);level.setBlock(at,south,Block.UPDATE_ALL);
        var baseline=south.getShape(level,at).bounds();
        h.assertTrue(baseline.minY<-.3&&baseline.maxY<.025,"Small native tabletop object retained its old high floating outline: "+id);
        for(var face:Direction.Plane.HORIZONTAL){
          var state=KindergartenDecor025.state(id,face);level.setBlock(at,state,Block.UPDATE_ALL);
          var shape=state.getShape(level,at).bounds();var physical=state.getCollisionShape(level,at).bounds();
          h.assertTrue(near(shape,rotateBounds(baseline,face))&&near(shape,physical),"Small prop outline/collision did not follow its native facing transform: "+id+" "+face);
          Vec3 normal=new Vec3(face.getStepX(),0,face.getStepZ()),above=new Vec3(at.getX()+.5,at.getY()+.1,at.getZ()+.5);
          var ray=level.clip(new ClipContext(above.subtract(normal.scale(1.1)),above.add(normal.scale(1.1)),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
          h.assertTrue(ray.getType()==HitResult.Type.MISS,"A ray above the visible prop hit a phantom high outline: "+id+" "+face);
          player.setPos(above.subtract(normal.scale(1.1)));player.setDeltaMovement(Vec3.ZERO);player.move(MoverType.PLAYER,normal.scale(2.2));
          h.assertTrue(player.position().subtract(above).dot(normal)>.85,"A body above the visible prop hit a phantom high collider: "+id+" "+face);
        }
      }
    }finally{player.server.getPlayerList().remove(player);}
    h.succeed();
  }
}
