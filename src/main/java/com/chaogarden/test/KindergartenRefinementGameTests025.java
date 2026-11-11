package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.hub.HubContent;
import com.chaogarden.item.ChaoCarrierItem;
import com.chaogarden.kindergarten.KindergartenContent;
import com.chaogarden.kindergarten.KindergartenNetwork;
import com.chaogarden.kindergarten.KindergartenNpc;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Actual school-session heartbeat selection and placed campus scenery regressions. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class KindergartenRefinementGameTests025 {
  private static ServerPlayer player(GameTestHelper h){
    var p=TestConnections.makeSurvivalServerPlayer(h);p.getInventory().clearContent();
    var level=p.server.getLevel(KindergartenContent.DIMENSION);
    h.assertTrue(level!=null&&HubContent.ensure(level),"Actual Kindergarten dimension was not built");
    p.teleportTo(level,21,80,5.5,90,0);p.hasChangedDimension();p.setNoGravity(true);drain(p);return p;
  }
  private static KindergartenNetwork.Open drain(ServerPlayer p){
    var channel=(EmbeddedChannel)p.connection.getConnection().channel();channel.runPendingTasks();channel.flushOutbound();channel.runPendingTasks();
    KindergartenNetwork.Open result=null;Object out;
    while((out=channel.readOutbound())!=null)if(out instanceof ClientboundCustomPayloadPacket packet&&packet.payload() instanceof KindergartenNetwork.Open view)result=view;
    return result;
  }
  private static ItemStack child(GameTestHelper h,ServerPlayer p,int slot,String name){
    var c=ChaoGarden.CHAO.get().create(p.serverLevel());h.assertTrue(c!=null,"Cannot create owned carried Chao");c.owner=p.getUUID();c.setCustomName(Component.literal(name));
    var item=new ItemStack(ChaoGarden.item("chao_carrier"));h.assertTrue(ChaoCarrierItem.capture(item,c,p),"Cannot capture owned Chao");p.getInventory().setItem(slot,item);return item;
  }
  private static String key(ItemStack item,int slot){return "i:"+slot+":"+ChaoCarrierItem.storedUUID(item);}
  private static void action(ServerPlayer p,KindergartenNetwork.Open open,String selection,String command,String value){
    KindergartenContent.handle(p,new KindergartenNetwork.Action(open.npc(),open.nonce(),selection,command,value));
  }

  @GameTest(template="empty",timeoutTicks=120)
  public static void heartbeatDoesNotSwallowOwnedChaoSelectionOrItsIndependentWrite(GameTestHelper h){
    var p=player(h);
    h.startSequence().thenWaitUntil(()->{
      KindergartenContent.ensureServices(p.serverLevel());
      var matches=p.serverLevel().getEntitiesOfClass(KindergartenNpc.class,new AABB(KindergartenContent.Role.FORTUNE.anchor).inflate(4),n->n.role()==KindergartenContent.Role.FORTUNE);
      h.assertTrue(matches.size()==1,"Fortune resident is not loaded once yet");
    }).thenExecute(()->{
      var npc=p.serverLevel().getEntitiesOfClass(KindergartenNpc.class,new AABB(KindergartenContent.Role.FORTUNE.anchor).inflate(4),n->n.role()==KindergartenContent.Role.FORTUNE).getFirst();
      var first=child(h,p,0,"First child");var second=child(h,p,1,"Second child");
      UUID firstId=ChaoCarrierItem.storedUUID(first),secondId=ChaoCarrierItem.storedUUID(second);
      String a=key(first,0),b=key(second,1);
      KindergartenContent.open(p,npc,a);var opened=drain(p);h.assertTrue(opened!=null&&opened.selected().equals(a),"Initial service did not select the first owned Chao");
      long tick=p.level().getGameTime();
      action(p,opened,a,"refresh","");action(p,opened,b,"select","");var selected=drain(p);
      h.assertTrue(p.level().getGameTime()==tick,"Heartbeat/selection fixture was not same-tick");
      h.assertTrue(selected!=null&&selected.selected().equals(b)&&selected.name().equals("Second child"),"The heartbeat swallowed the player's second-Chao selection");
      action(p,selected,b,"name","Chosen child");var renamed=drain(p);
      action(p,selected,b,"name","Repeated write");
      h.assertTrue(renamed!=null&&renamed.selected().equals(b)&&second.getHoverName().getString().equals("Chosen child"),"Selection heartbeat blocked the independent first write or allowed a duplicate same-tick write");
      h.assertTrue(first.getHoverName().getString().equals("First child")&&firstId.equals(ChaoCarrierItem.storedUUID(first))&&secondId.equals(ChaoCarrierItem.storedUUID(second))&&first.getCount()==1&&second.getCount()==1,"Selection or writing changed the other Chao, identity or carrier count");
      p.server.getPlayerList().remove(p);h.succeed();
    });
  }

  @GameTest(template="empty",timeoutTicks=80)
  public static void realCampusHedgesSurviveVanillaRandomTicksWithoutAttachedLogs(GameTestHelper h){
    var p=player(h);var level=p.serverLevel();
    // These are authored entry-walk and perimeter hedges, not detached replacement leaves.
    for(var at:java.util.List.of(new BlockPos(5,80,30),new BlockPos(-5,80,37),new BlockPos(34,82,12))){
      var placed=level.getBlockState(at);
      h.assertTrue(placed.is(Blocks.OAK_LEAVES)&&placed.getValue(LeavesBlock.PERSISTENT),"Placed campus hedge is absent or nonpersistent at "+at);
      h.assertTrue(placed.getValue(LeavesBlock.DISTANCE)==7,"Campus hedge fixture unexpectedly depends on a nearby vanilla log");
      for(int i=0;i<32;i++)level.getBlockState(at).randomTick(level,at,level.random);
      h.assertTrue(level.getBlockState(at).is(Blocks.OAK_LEAVES)&&level.getBlockState(at).getValue(LeavesBlock.PERSISTENT),"Vanilla random ticks decayed an actual campus hedge at "+at);
    }
    p.server.getPlayerList().remove(p);h.succeed();
  }
}
