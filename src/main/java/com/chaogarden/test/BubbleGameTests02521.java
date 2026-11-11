package com.chaogarden.test;

import com.chaogarden.core.ChaoState;
import com.chaogarden.toys.*;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.RenderShape;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("chao_garden") @PrefixGameTestTemplate(false)
public final class BubbleGameTests02521 {
  private static void play(GameTestHelper h,boolean adult,boolean cancel) {
    var pos=RattleGameTests02520.floor(h);h.getLevel().setBlock(pos,ToyContent.block(BubblePlayRules.KIND).defaultBlockState(),3);
    var c=RattleGameTests02520.actor(h,pos,adult,ChaoState.NORMAL,false);
    var viewer=TestConnections.makeServerPlayer(h);viewer.setGameMode(GameType.SPECTATOR);
    viewer.teleportTo(pos.getX()+4,pos.getY()+1,pos.getZ()+4);
    var channel=(EmbeddedChannel)viewer.connection.getConnection().channel();
    var start=new AtomicLong(-1);var cancelled=new AtomicLong(-1);var puffs=new ArrayList<Long>();int[] atCancel={0};
    h.onEachTick(()->{
      if(ToyContent.isPlaying(c))start.compareAndSet(-1,c.getPersistentData().getLong("ChaoToyUntil")-160);
      Object packet;
      while((packet=channel.readOutbound())!=null)if(packet instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket custom
          &&custom.payload() instanceof ToyCueNetwork.Bubble p && p.chao().equals(c.getUUID())) {
        h.assertTrue(p.until()==c.toyPlayingUntil(),"cue belongs to this exact session");
        h.assertTrue(BubblePlayRules.puffDue(p.elapsed()),"cue is one authored blow tick");
        int elapsed=(int)(h.getLevel().getGameTime()-start.get());
        h.assertTrue(BubblePlayRules.puffDue(elapsed)||BubblePlayRules.puffDue(elapsed-1),"particle packet follows blow timeline");
        puffs.add(h.getLevel().getGameTime());
      }
      if(ToyContent.isPlaying(c))h.assertTrue(h.getLevel().getBlockState(pos).getRenderShape()==RenderShape.INVISIBLE
          &&c.getHeldChaoItem().isEmpty(),"one real source and a presentation-only hand prop");
      if(cancel&&cancelled.get()<0&&start.get()>=0&&h.getLevel().getGameTime()-start.get()>=60) {
        cancelled.set(h.getLevel().getGameTime());atCancel[0]=puffs.size();c.setStaying(true);
      }
    });
    h.succeedWhen(()->{
      h.assertTrue(start.get()>=0&&h.getLevel().getGameTime()>start.get()+(cancel?90:175),"observe completed/cancelled session");
      h.assertTrue(!ToyContent.isPlaying(c)&&h.getLevel().getBlockState(pos).getRenderShape()==RenderShape.MODEL,"placed wand restored");
      h.assertTrue(c.state().happiness==(cancel?20:24),"one completion reward, none for interruption");
      h.assertTrue(puffs.size()==(cancel?atCancel[0]:12)&&puffs.size()>0,"exact puffs; no packets after cancellation: "+puffs);
      viewer.server.getPlayerList().remove(viewer);
    });
  }
  @GameTest(template="activity_test",timeoutTicks=1800,batch="bubble_packets") public static void babyBlowsTwelveAirBubbles(GameTestHelper h){play(h,false,false);}
  @GameTest(template="activity_test",timeoutTicks=1800,batch="bubble_packets") public static void adultBlowsTwelveAirBubbles(GameTestHelper h){play(h,true,false);}
  @GameTest(template="activity_test",timeoutTicks=1800,batch="bubble_packets") public static void babyInterruptedBlowStopsPackets(GameTestHelper h){play(h,false,true);}
  @GameTest(template="activity_test",timeoutTicks=1800,batch="bubble_packets") public static void adultInterruptedBlowStopsPackets(GameTestHelper h){play(h,true,true);}
}
