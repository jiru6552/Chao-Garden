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
public final class BellGameTests02525 {
  private static void play(GameTestHelper h,boolean adult,boolean cancel) {
    var pos=RattleGameTests02520.floor(h);h.getLevel().setBlock(pos,ToyContent.block(BellPlayRules.KIND).defaultBlockState(),3);
    var c=RattleGameTests02520.actor(h,pos,adult,ChaoState.NORMAL,false);c.state().setTraitInt("learned.bell",1);c.sync();
    var viewer=TestConnections.makeServerPlayer(h);viewer.setGameMode(GameType.SPECTATOR);
    viewer.teleportTo(pos.getX()+4,pos.getY()+1,pos.getZ()+4);
    var channel=(EmbeddedChannel)viewer.connection.getConnection().channel();
    var start=new AtomicLong(-1);var cancelled=new AtomicLong(-1);var cues=new ArrayList<Long>();int[] atCancel={0};
    h.onEachTick(()->{
      if(ToyContent.isPlaying(c))start.compareAndSet(-1,c.getPersistentData().getLong("ChaoToyUntil")-160);
      Object packet;
      while((packet=channel.readOutbound())!=null)if(packet instanceof net.minecraft.network.protocol.game.ClientboundSoundPacket sound
          &&sound.getSound().value().getLocation().equals(com.chaogarden.ChaoSounds.HAND_BELL.getId())) {
        int elapsed=(int)(h.getLevel().getGameTime()-start.get());
        h.assertTrue(BellPlayRules.soundDue(elapsed)||BellPlayRules.soundDue(elapsed-1),"server cue follows alternating shake extrema");
        cues.add(h.getLevel().getGameTime());
      }
      if(ToyContent.isPlaying(c))h.assertTrue(h.getLevel().getBlockState(pos).getRenderShape()==RenderShape.INVISIBLE
          &&c.getHeldChaoItem().isEmpty(),"one real source and a presentation-only hand prop");
      if(cancel&&cancelled.get()<0&&start.get()>=0&&h.getLevel().getGameTime()-start.get()>=60) {
        cancelled.set(h.getLevel().getGameTime());// Vanilla sound packets can reach the embedded channel one tick after dispatch.
        // Include the legitimate cue dispatched on this cancellation tick; the
        // direct event lifecycle tests separately forbid new events after stop.
        for(int tick=0;tick<=h.getLevel().getGameTime()-start.get();tick++)if(BellPlayRules.soundDue(tick))atCancel[0]++;
        c.setStaying(true);
      }
    });
    h.succeedWhen(()->{
      h.assertTrue(start.get()>=0&&h.getLevel().getGameTime()>start.get()+(cancel?90:175),"observe completed/cancelled session");
      h.assertTrue(!ToyContent.isPlaying(c)&&h.getLevel().getBlockState(pos).getRenderShape()==RenderShape.MODEL,"placed bell restored");
      h.assertTrue(c.state().happiness==(cancel?20:24),"one completion reward, none for interruption");
      h.assertTrue(cues.size()==(cancel?atCancel[0]:8)&&cues.size()>0,"exact cues; no packets after cancellation: "+cues);
      viewer.server.getPlayerList().remove(viewer);
    });
  }
  @GameTest(template="activity_test",timeoutTicks=1800,batch="bell_packets") public static void babyShakesEightTimes(GameTestHelper h){play(h,false,false);}
  @GameTest(template="activity_test",timeoutTicks=1800,batch="bell_packets") public static void adultShakesEightTimes(GameTestHelper h){play(h,true,false);}
  @GameTest(template="activity_test",timeoutTicks=1800,batch="bell_packets") public static void babyInterruptedShakeStopsPackets(GameTestHelper h){play(h,false,true);}
  @GameTest(template="activity_test",timeoutTicks=1800,batch="bell_packets") public static void adultInterruptedShakeStopsPackets(GameTestHelper h){play(h,true,true);}
}
