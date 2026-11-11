package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.toys.*;
import io.netty.buffer.Unpooled;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.network.bundle.PacketAndPayloadAcceptor;

/** Real tracker pairing packets and their wire codec, with two independent observer replicas.
 * These deterministic server tests complement, rather than stand in for, the native client run. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class ToyMultiplayerGameTests02521 {
  @GameTest(template="empty",timeoutTicks=10)
  public static void emptySessionNeverAnimatesOnALaggingClient(GameTestHelper h) {
    for(int lag=0;lag<=40;lag++){
      h.assertTrue(ToyInteractionRules.remainingTicks("",1000,1000-lag)==0,"empty metadata cannot revive cancelled play");
      h.assertTrue(ToyInteractionRules.remainingTicks(RattlePlayRules.KIND,0,1000-lag)==0,"zero deadline is inactive");
      h.assertTrue(ToyInteractionRules.remainingTicks(RattlePlayRules.KIND,1160,1000-lag)==160,"slightly early client holds the first pose");
    }
    h.assertTrue(ToyInteractionRules.remainingTicks(RattlePlayRules.KIND,1160,1040)==120,"late join uses remaining phase");
    h.assertTrue(ToyInteractionRules.remainingTicks(RattlePlayRules.KIND,1160,1161)==0,"expired session stays ended");h.succeed();
  }
  @GameTest(template="activity_test",timeoutTicks=1800) public static void babyBreak(GameTestHelper h){RattleGameTests02520.interrupt(h,"break",false);}
  @GameTest(template="activity_test",timeoutTicks=1800) public static void babyDisplacement(GameTestHelper h){RattleGameTests02520.interrupt(h,"move",false);}
  @GameTest(template="activity_test",timeoutTicks=1800) public static void babyReload(GameTestHelper h){RattleGameTests02520.interrupt(h,"reload",false);}
  @GameTest(template="activity_test",timeoutTicks=1800) public static void babyStay(GameTestHelper h){RattleGameTests02520.interrupt(h,"stay",false);}
  @GameTest(template="activity_test",timeoutTicks=1800) public static void adultStay(GameTestHelper h){RattleGameTests02520.interrupt(h,"stay",true);}
  @GameTest(template="activity_test",timeoutTicks=1800) public static void babyRescueBoat(GameTestHelper h){RattleGameTests02520.interrupt(h,"ride",false);}
  @GameTest(template="activity_test",timeoutTicks=1800) public static void adultRescueBoat(GameTestHelper h){RattleGameTests02520.interrupt(h,"ride",true);}
  @GameTest(template="activity_test",timeoutTicks=1800) public static void babyHeldItem(GameTestHelper h){RattleGameTests02520.interrupt(h,"item",false);}
  @GameTest(template="activity_test",timeoutTicks=1800) public static void adultHeldItem(GameTestHelper h){RattleGameTests02520.interrupt(h,"item",true);}

  private static void pairing(GameTestHelper h,ChaoEntity source,ServerPlayer viewer,ChaoEntity replica) {
    var tracker=new ServerEntity(h.getLevel(),source,1,true,p->{});
    int[] metadata={0};
    tracker.sendPairingData(viewer,new PacketAndPayloadAcceptor<>(p->{
      if(p instanceof ClientboundSetEntityDataPacket packet) {
        var buf=new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess());
        try {
          ClientboundSetEntityDataPacket.STREAM_CODEC.encode(buf,packet);
          var decoded=ClientboundSetEntityDataPacket.STREAM_CODEC.decode(buf);
          h.assertTrue(decoded.id()==source.getId()&&!buf.isReadable(),"whole real metadata packet round trips");
          replica.getEntityData().assignValues(decoded.packedItems());metadata[0]++;
        } finally {buf.release();}
      }
    }));
    h.assertTrue(metadata[0]==1,"joining observer receives entity metadata");
    h.assertTrue(source.toyPlayingKind().equals(replica.toyPlayingKind())&&source.toyPlayingTicks()==replica.toyPlayingTicks(),
        "observer starts at the current server phase, never restarts an animation");
  }
  private static void observers(GameTestHelper h,boolean adult,boolean cancel) {
    var pos=RattleGameTests02520.floor(h);
    var c=RattleGameTests02520.actor(h,pos,adult,ChaoState.NORMAL,false);
    var viewer=TestConnections.makeServerPlayer(h);viewer.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
    var a=ChaoGarden.CHAO.get().create(h.getLevel());var b=ChaoGarden.CHAO.get().create(h.getLevel());
    var start=new AtomicLong(-1);var joined=new boolean[3];
    h.onEachTick(()->{
      if(ToyContent.isPlaying(c))start.compareAndSet(-1,c.getPersistentData().getLong("ChaoToyUntil")-160);
      if(start.get()<0)return;long elapsed=h.getLevel().getGameTime()-start.get();
      if(elapsed>=20&&!joined[0]){pairing(h,c,viewer,a);joined[0]=true;}
      if(elapsed>=60&&!joined[1]){pairing(h,c,viewer,b);joined[1]=true;}
      if(elapsed>=80&&!joined[2]){pairing(h,c,viewer,a);joined[2]=true;if(cancel)h.getLevel().destroyBlock(pos,true);}
      if(elapsed>22&&elapsed<80&&joined[1])h.assertTrue(a.toyPlayingTicks()==b.toyPlayingTicks(),"different join times share one advancing phase");
    });
    h.succeedWhen(()->{
      h.assertTrue(start.get()>=0&&h.getLevel().getGameTime()>start.get()+(cancel?100:175),"observe final state");
      h.assertTrue(joined[0]&&joined[1]&&joined[2],"early, late and rejoining observers tested");
      pairing(h,c,viewer,a);pairing(h,c,viewer,b);
      h.assertTrue(a.toyPlayingKind().isEmpty()&&b.toyPlayingKind().isEmpty(),"both observers clear presentation after end/cancel");
      h.assertTrue(c.state().happiness==(cancel?20:adult?24:27),"observers cannot duplicate reward");
      viewer.server.getPlayerList().remove(viewer);
    });
  }
  @GameTest(template="activity_test",timeoutTicks=1800,batch="toy_observers") public static void babyLateJoinAndRejoin(GameTestHelper h){observers(h,false,false);}
  @GameTest(template="activity_test",timeoutTicks=1800,batch="toy_observers") public static void adultLateJoinAndRejoin(GameTestHelper h){observers(h,true,false);}
  @GameTest(template="activity_test",timeoutTicks=1800,batch="toy_observers") public static void babyObserverCancellation(GameTestHelper h){observers(h,false,true);}
  @GameTest(template="activity_test",timeoutTicks=1800,batch="toy_observers") public static void adultObserverCancellation(GameTestHelper h){observers(h,true,true);}

  @GameTest(template="activity_test",timeoutTicks=1800)
  public static void babyAndAdultCannotOwnSameRattle(GameTestHelper h) {
    var pos=RattleGameTests02520.floor(h);
    var a=RattleGameTests02520.actor(h,pos,false,ChaoState.NORMAL,false);
    var b=RattleGameTests02520.actor(h,pos,true,ChaoState.RUN_TYPE,false);b.setPos(pos.getX()+2.5,pos.getY(),pos.getZ()+.5);
    var started=new AtomicLong(-1);var winner=new ChaoEntity[1];
    h.onEachTick(()->{
      h.assertTrue(!(ToyContent.isPlaying(a)&&ToyContent.isPlaying(b)),"one placed source has only one owner");
      if(winner[0]==null&&(ToyContent.isPlaying(a)||ToyContent.isPlaying(b))) {
        winner[0]=ToyContent.isPlaying(a)?a:b;started.set(h.getLevel().getGameTime());
      }
    });
    h.succeedWhen(()->{
      h.assertTrue(started.get()>=0&&h.getLevel().getGameTime()>started.get()+175,"first owner completes");
      var loser=winner[0]==a?b:a;
      h.assertTrue(winner[0].state().happiness==(winner[0].state().adult?24:27)&&loser.state().happiness==20,"only completed owner receives reward");
      h.assertTrue(h.getLevel().getBlockState(pos).is(ToyContent.block(RattlePlayRules.KIND)),"one source survives contention");
    });
  }
}
