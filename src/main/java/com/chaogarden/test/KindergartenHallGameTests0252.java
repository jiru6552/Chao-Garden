package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.hub.HubContent;
import com.chaogarden.kindergarten.KindergartenContent;
import com.chaogarden.kindergarten.KindergartenDecor025;
import com.chaogarden.kindergarten.KindergartenHallDisplays0252;
import com.chaogarden.kindergarten.KindergartenHallThemes0252;
import com.chaogarden.market.MarketStock;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Actual built-school low-angle rays, waiting-room movement and durable native stock showcases. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class KindergartenHallGameTests0252 {
  private static void walk(GameTestHelper h,ServerPlayer player,Vec3 target){
    Vec3 start=player.position(),delta=target.subtract(start);int steps=Math.max(1,(int)Math.ceil(delta.length()/.15));
    for(int i=1;i<=steps;i++){
      Vec3 next=start.add(delta.scale(i/(double)steps));player.move(MoverType.SELF,next.subtract(player.position()));
      h.assertTrue(player.position().distanceToSqr(next)<.0001,"New hall scenery blocked a full-size Adventure body at "+next+" actual="+player.position());
    }
  }
  @GameTest(template="empty",batch="kindergarten_halls0252",timeoutTicks=120)
  public static void fortuneSigilsOccupyCompleteNativeFacesOnBothWallsAndTheirEnds(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);var level=p.server.getLevel(KindergartenContent.DIMENSION);
    try{
      h.assertTrue(level!=null&&HubContent.ensure(level),"Actual school was not built");
      var wall=KindergartenHallThemes0252.block("school_hall_fortune_wall");
      for(int x=8;x<=18;x++)for(int z:new int[]{3,7})for(int y=81;y<=83;y++){
        var at=new BlockPos(x,y,z);var state=level.getBlockState(at);
        h.assertTrue(state.is(wall)&&state.isCollisionShapeFullBlock(level,at),"Fortune circle is mapped to a partial/scaled wall or cut off by a corner at "+at);
        Vec3 from=new Vec3(x+.5,y+.5,z==3?4.5:6.5),to=at.getCenter();
        var hit=level.clip(new ClipContext(from,to,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,p));
        h.assertTrue(hit.getType()==HitResult.Type.BLOCK&&hit.getBlockPos().equals(at)&&hit.getDirection()==(z==3?net.minecraft.core.Direction.SOUTH:net.minecraft.core.Direction.NORTH),"Actual Fortune wall exposes the wrong face or an interrupted motif at "+at);
      }
    }finally{p.server.getPlayerList().remove(p);}
    h.succeed();
  }

  @GameTest(template="empty",batch="kindergarten_halls0252",timeoutTicks=120)
  public static void lowFrontSightlinesHitSolidCreamInfillAndTheDoorOpeningStillAdmitsThePlayer(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);var level=p.server.getLevel(KindergartenContent.DIMENSION);
    try{
      h.assertTrue(level!=null&&HubContent.ensure(level),"Actual school was not built");
      for(int x=-2;x<=2;x++)for(int y=83;y<=85;y++)h.assertTrue(level.getBlockState(new BlockPos(x,y,18)).is(KindergartenDecor025.block("school_cream_wall")),"Entrance infill is open above the inner arch at "+x+","+y);
      Vec3[][] rays={{new Vec3(.5,81.62,22.8),new Vec3(.5,84.6,17)},
          {new Vec3(2.2,81.62,22.8),new Vec3(-1,84.6,17)}, {new Vec3(-1.8,81.62,22.8),new Vec3(1,84.6,17)}};
      for(var ray:rays){
        var hit=level.clip(new ClipContext(ray[0],ray[1],ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,p));
        h.assertTrue(hit.getType()==HitResult.Type.BLOCK&&level.getBlockState(hit.getBlockPos()).is(KindergartenDecor025.block("school_cream_wall"))&&hit.getBlockPos().getZ()>=18,"The user's low front-under-lintel angle still sees inside the school: "+hit);
      }
      var pair=com.chaogarden.kindergarten.KindergartenRooms025.doorPairs().getLast();var leaf=pair.leftBase();
      var state=level.getBlockState(leaf);boolean initiallyOpen=state.getValue(KindergartenDecor025.SchoolDoorBlock.OPEN);
      if(!initiallyOpen)state.useWithoutItem(level,p,new BlockHitResult(leaf.getCenter(),pair.facing(),leaf,false));
      p.setGameMode(GameType.ADVENTURE);p.setNoGravity(true);p.teleportTo(level,.5,80.01,22.5,180,0);p.hasChangedDimension();
      h.assertTrue(!p.noPhysics&&p.getBbHeight()>1.7,"Doorway movement must use a normal full-size colliding player");
      walk(h,p,new Vec3(.5,80.01,16.5));walk(h,p,new Vec3(.5,80.01,22.5));
      if(!initiallyOpen)level.getBlockState(leaf).useWithoutItem(level,p,new BlockHitResult(leaf.getCenter(),pair.facing(),leaf,false));
    }finally{p.server.getPlayerList().remove(p);}
    h.succeed();
  }
  @GameTest(template="empty",batch="kindergarten_halls0252",timeoutTicks=120)
  public static void destinationFinishesKeepTheirCeilingsAndClinicWaitingAlcoveWalkable(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);var level=p.server.getLevel(KindergartenContent.DIMENSION);
    try{
      h.assertTrue(level!=null&&HubContent.ensure(level),"Actual school was not built");
      h.assertTrue(level.getBlockState(new BlockPos(12,79,5)).is(KindergartenHallThemes0252.block("school_hall_fortune_carpet")),"Fortune hall lost its dark carpet");
      h.assertTrue(level.getBlockState(new BlockPos(-12,79,5)).is(KindergartenHallThemes0252.block("school_hall_market_flagstone")),"Hidden storage passage lost its utility floor");
      h.assertTrue(level.getBlockState(new BlockPos(0,79,-12)).is(KindergartenHallThemes0252.block("school_hall_principal_parquet")),"Scholarly hall lost its parquet");
      for(int x=9;x<=17;x++)for(int z=4;z<=6;z++)h.assertTrue(level.getBlockState(new BlockPos(x,85,z)).is(KindergartenHallThemes0252.block("school_hall_fortune_ceiling")),"Fortune ceiling retains an abrupt white light/tile or a hole");
      h.assertTrue(level.getBlockState(new BlockPos(11,84,5)).is(Blocks.SOUL_LANTERN),"Fortune passage lost its subdued lantern lighting");
      h.assertTrue(level.getBlockState(new BlockPos(-2,82,-14)).is(KindergartenHallThemes0252.block("school_hall_principal_certificate")),"Scholarly approach gallery is absent");
      int gallery=0;var work=KindergartenHallThemes0252.block("school_hall_classroom_student_work");var photo=KindergartenHallThemes0252.block("school_hall_classroom_class_photo");
      for(var at:BlockPos.betweenClosed(new BlockPos(-17,81,-12),new BlockPos(-8,83,-2))){
        var state=level.getBlockState(at);if(!state.is(work)&&!state.is(photo))continue;gallery++;
        // Test actual supporting architecture, independently of the authored coordinates.
        // A flat wall-inset picture must join its wall above/below and along one full
        // horizontal tangent. The former floating class-photo cube fails both axes.
        java.util.function.Predicate<BlockPos> solid=q->level.getBlockState(q).isCollisionShapeFullBlock(level,q);
        boolean tangent=solid.test(at.west())&&solid.test(at.east())||solid.test(at.north())&&solid.test(at.south());
        h.assertTrue(solid.test(at.above())&&solid.test(at.below())&&tangent,"Student gallery projects into the corridor or lacks a continuous flat wall at "+at);
      }
      h.assertTrue(gallery==3,"Student work/class photos are missing from the actual approach walls");
      h.assertTrue(level.getBlockState(new BlockPos(-13,82,-6)).isAir(),"The bent public route still contains the former floating photo obstruction");
      p.setGameMode(GameType.ADVENTURE);p.setNoGravity(true);p.teleportTo(level,14.5,80.01,-5.5,-90,0);p.hasChangedDimension();
      walk(h,p,new Vec3(18.5,80.01,-5.5));walk(h,p,new Vec3(20.5,80.01,-5.5));walk(h,p,new Vec3(20.5,80.01,-3.5));walk(h,p,new Vec3(18.5,80.01,-5.5));walk(h,p,new Vec3(14.5,80.01,-5.5));
      for(int x:new int[]{18,20,22})h.assertTrue(level.getBlockState(new BlockPos(x,80,-7)).is(KindergartenDecor025.block("school_green_chair")),"Clinic waiting seats are absent");
      h.assertTrue(level.getBlockState(new BlockPos(24,82,-3)).is(KindergartenHallThemes0252.block("school_hall_clinic_registration")),"Registration desk is not identified inside the waiting alcove");
      h.assertTrue(!level.getBlockState(new BlockPos(-6,83,5)).is(KindergartenDecor025.block("school_label_market"))&&level.getBlockState(new BlockPos(-6,80,3)).getBlock()!=Blocks.CAULDRON,"The unmarked cupboard still advertises the hidden shop");
      int stockedShelves=0;
      for(var at:BlockPos.betweenClosed(new BlockPos(-24,81,2),new BlockPos(-24,83,8))){
        var prop=level.getBlockState(at);if(!prop.is(KindergartenDecor025.block("school_potion_display"))&&!prop.is(KindergartenDecor025.block("school_book_stack")))continue;stockedShelves++;
        var below=at.below();var support=level.getBlockState(below);
        h.assertTrue(support.is(KindergartenHallThemes0252.block("school_hall_market_wall_shelf")),"Shop wall goods have no real supporting shelf at "+at);
        var board=support.getCollisionShape(level,below).bounds().move(below);var goods=prop.getShape(level,at).bounds().move(at);
        h.assertTrue(Math.abs(board.maxY-goods.minY)<.000001&&board.minX<=goods.minX&&board.maxX>=goods.maxX&&board.minZ<=goods.minZ&&board.maxZ>=goods.maxZ,"Shop goods float above or are buried inside their visible support at "+at);
        var wall=below.west();var wallShape=level.getBlockState(wall).getCollisionShape(level,wall);
        h.assertTrue(!wallShape.isEmpty()&&Math.abs(wallShape.bounds().maxX+wall.getX()-board.minX)<.000001,"Shop shelf/brackets do not meet the actual back wall at "+below);
      }
      h.assertTrue(stockedShelves==4,"Some back-wall goods disappeared instead of acquiring real supports");
      for(int z:new int[]{3,5,7}){
        var lower=new BlockPos(-22,83,z);var upper=lower.above();var ceiling=upper.above();
        h.assertTrue(level.getBlockState(lower).is(Blocks.CHAIN)&&level.getBlockState(upper).is(Blocks.CHAIN),"Hanging shop chain has an air gap before its ceiling");
        var chain=level.getBlockState(upper).getShape(level,upper).bounds().move(upper);var cap=level.getBlockState(ceiling).getCollisionShape(level,ceiling);
        h.assertTrue(!cap.isEmpty()&&Math.abs(chain.maxY-cap.bounds().minY-ceiling.getY())<.000001,"Hanging shop chain does not physically reach its ceiling");
      }
    }finally{p.server.getPlayerList().remove(p);}
    h.succeed();
  }
  @GameTest(template="empty",batch="kindergarten_halls0252",timeoutTicks=500)
  public static void insetStockShelvesUseRealCatalogItemModelsAndDoNotDuplicateOnReentry(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);var level=p.server.getLevel(KindergartenContent.DIMENSION);
    boolean[] finished={false};Runnable cleanup=()->{if(!finished[0]){finished[0]=true;if(HubContent.hasReturnJournal(p))HubContent.returnHome(p);p.server.getPlayerList().remove(p);}};
    try{
      h.assertTrue(level!=null&&HubContent.ensure(level),"Actual school was not built");
      // A real campus visitor acquires resident tickets and makes the saved shelf
      // chunk visible/ticking. An empty prewarmed room deliberately creates none.
      p.setGameMode(GameType.ADVENTURE);p.setNoGravity(true);p.teleportTo(level,-12.5,80.01,5.5,90,0);p.hasChangedDimension();
    }catch(RuntimeException failure){cleanup.run();throw failure;}
    long began=h.getTick(),wall=System.nanoTime();
    h.runAtTickTime(began+450,()->{if(!finished[0]){cleanup.run();h.fail("School stock showcases were not published before their450tick fixture deadline");}});
    h.startSequence().thenWaitUntil(()->{
      try{KindergartenContent.ensureServices(level);KindergartenHallDisplays0252.ensure(level);}
      catch(RuntimeException failure){cleanup.run();throw failure;}
      if(System.nanoTime()-wall>java.util.concurrent.TimeUnit.SECONDS.toNanos(12)){cleanup.run();h.fail("School stock showcase publication exceeded12wall seconds");}
      h.assertTrue(KindergartenHallDisplays0252.showcases().stream().allMatch(g->level.getEntity(g.uuid()) instanceof Display.ItemDisplay),"Persisted chunk displays are not published yet");
    }).thenExecute(()->{
      try{
        var identities=new java.util.HashMap<java.util.UUID,Integer>();
        for(var goods:KindergartenHallDisplays0252.showcases()){
          var entity=level.getEntity(goods.uuid());identities.put(goods.uuid(),entity.getId());
          CompoundTag saved=new CompoundTag();entity.saveWithoutId(saved);var actual=ItemStack.parseOptional(level.registryAccess(),saved.getCompound("item"));
          h.assertTrue(MarketStock.catalog().containsKey(goods.itemId())&&actual.is(ChaoGarden.item(goods.itemId()))&&actual.getCount()==1,"Shelf fixture is not one real possible catalog item: "+goods.itemId());
          h.assertTrue(entity.getTags().contains("chao_school_stock_showcase")&&entity.isNoGravity()&&entity.position().distanceToSqr(goods.x(),goods.y(),goods.z())<.000001,"Shelf fixture floated away from its supported inset shelf");
        }
        for(int i=0;i<8;i++)KindergartenHallDisplays0252.ensure(level);
        for(var goods:KindergartenHallDisplays0252.showcases())h.assertTrue(level.getEntity(goods.uuid()).getId()==identities.get(goods.uuid()),"Reentering the cupboard duplicated/replaced an actual stock showcase identity");
        h.assertTrue(p.getInventory().isEmpty(),"Looking at stock shelves gave the player actual goods");
      }finally{cleanup.run();}
      h.succeed();
    });
  }
}
