package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.hub.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class GardenBankGameTests02511 {
  @GameTest(template="empty",batch="garden02511_bank",timeoutTicks=160)
  public static void landscapedBankKeepsWaterAndSafelyMovesBodiesFromOldLawn(GameTestHelper h){
    var level=h.getLevel().getServer().getLevel(HubContent.GARDEN);
    h.assertTrue(level!=null&&HubContent.ensure(level),"Neutral Garden unavailable");
    int terraces=0;
    for(int x=-34;x<=-26;x++)for(int z=2;z<=7;z++){
      if(!GardenRooms025.waterfallBank(x,z))continue;
      int y=GardenRooms025.surface(x,z);var top=new BlockPos(x,y,z);
      h.assertTrue(y>=80&&y<=83&&level.getBlockState(top).is(LobbyDecor025.TURF.get()),"Bank is too tall, bare, or not grass-topped");
      if(y>80)h.assertTrue(level.getBlockState(top.below()).is(NeutralGardenDecor02510.CASCADE_ROCK.get()),"Terrace lacks solid pale support");
      h.assertTrue(!GardenRooms025.pondCell(x,z),"Bank enters receiving pool");terraces++;
    }
    h.assertTrue(terraces>=35&&GardenRooms025.WATERFALL_PLANTS.length==18,"Bare side was not filled with a bounded bank and supported planting");
    // Recreate occupied lawn cells before this terrain update.
    for(int x:new int[]{-31,-29}){
      level.setBlock(new BlockPos(x,79,5),LobbyDecor025.TURF.get().defaultBlockState(),2);
      for(int y=80;y<=84;y++)level.setBlock(new BlockPos(x,y,5),Blocks.AIR.defaultBlockState(),2);
    }
    var owner=java.util.UUID.randomUUID();var child=ChaoGarden.CHAO.get().create(level);var state=new ChaoState();state.happiness=63;child.setCustomName(net.minecraft.network.chat.Component.literal("Bank resident"));
    child.owner=owner;child.setState(state);child.setNoAi(true);child.setNoGravity(true);child.moveTo(-30.5,80,5.5);
    var item=new ItemEntity(level,-28.5,80,5.5,new ItemStack(Items.DIAMOND,7));item.setNoGravity(true);item.setPickUpDelay(32767);
    var chunk=new ChunkPos(child.blockPosition());boolean forced=level.getForcedChunks().contains(chunk.toLong());if(!forced)level.setChunkForced(chunk.x,chunk.z,true);
    h.assertTrue(level.addFreshEntity(child)&&level.addFreshEntity(item),"Could not publish existing garden bodies");
    Runnable cleanup=()->{child.discard();item.discard();if(!forced)level.setChunkForced(chunk.x,chunk.z,false);};h.runAtTickTime(159,cleanup);
    h.startSequence().thenWaitUntil(()->h.assertTrue(level.getEntity(child.getUUID())==child&&level.getEntity(item.getUUID())==item,"Awaiting real body publication"))
      .thenExecute(()->{
        GardenRooms025.ensure(level);
        h.assertTrue(child.owner.equals(owner)&&child.getCustomName().getString().equals("Bank resident")&&child.state().happiness==63&&!child.isRemoved(),"Landscaping changed Chao identity or care");
        h.assertTrue(!item.isRemoved()&&item.getItem().is(Items.DIAMOND)&&item.getItem().getCount()==7,"Landscaping lost existing items");
        for(var body:java.util.List.of(child,item))h.assertTrue(level.noCollision(body)&&level.getBlockState(body.blockPosition().below()).is(LobbyDecor025.TURF.get()),"Existing body was buried or moved to water");
        cleanup.run();
      }).thenSucceed();
  }
}
