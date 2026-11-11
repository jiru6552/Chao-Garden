package com.chaogarden.test;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEggEntity;
import com.chaogarden.entity.ChaoEntity;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real join cancellation and synchronous callback reentry around breeding publication. */
@GameTestHolder(ChaoGarden.MODID)
@PrefixGameTestTemplate(false)
public final class BreedingPublicationGameTests02518 {
  private static ChaoEntity parent(GameTestHelper h,BlockPos at,UUID owner){
    var child=ChaoGarden.CHAO.get().create(h.getLevel());var state=new ChaoState();state.adult=true;
    child.setState(state);child.owner=owner;child.setNoAi(true);child.setNoGravity(true);
    child.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);h.assertTrue(h.getLevel().addFreshEntity(child),"Parent fixture publication failed");
    child.setInLove(null);return child;
  }
  private static int cooldown(ChaoEntity child){var tag=new CompoundTag();child.saveWithoutId(tag);return tag.getInt("BreedingCooldown");}
  private static int hearts(io.netty.channel.embedded.EmbeddedChannel channel){
    channel.runPendingTasks();channel.flushOutbound();channel.runPendingTasks();int count=0;Object message;
    while((message=channel.readOutbound())!=null){
      if(message instanceof net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket packet
          &&packet.getParticle().getType()==ParticleTypes.HEART)count+=packet.getCount();
      io.netty.util.ReferenceCountUtil.release(message);
    }
    return count;
  }
  @GameTest(template="empty",timeoutTicks=60)
  public static void canceledEggPreservesParentsAndRetryCommitsExactlyOnceDespiteReentry(GameTestHelper h){
    var level=h.getLevel();var at=h.absolutePos(new BlockPos(2,2,2));var bounds=new AABB(at).inflate(4);
    var keeper=TestConnections.makeSurvivalServerPlayer(h);keeper.teleportTo(at.getX()+.5,at.getY(),at.getZ()+2.5);
    var first=parent(h,at,keeper.getUUID());var second=parent(h,at.east(),keeper.getUUID());var third=parent(h,at.west(),keeper.getUUID());
    var channel=(io.netty.channel.embedded.EmbeddedChannel)keeper.connection.getConnection().channel();hearts(channel);
    var eggAttempts=new java.util.concurrent.atomic.AtomicInteger();var xpAttempts=new java.util.concurrent.atomic.AtomicInteger();
    Runnable reenter=()->{
      h.assertTrue(!first.canMate(second)&&!second.canMate(first)&&!first.canMate(third)&&!third.canMate(second),"Publication did not reserve both parents");
      first.spawnChildFromBreeding(level,second);second.spawnChildFromBreeding(level,first);
      first.spawnChildFromBreeding(level,third);third.spawnChildFromBreeding(level,second);
    };
    java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> callback=event->{
      if(event.getEntity() instanceof ChaoEggEntity egg&&keeper.getUUID().equals(egg.owner)){
        int attempt=eggAttempts.incrementAndGet();reenter.run();if(attempt==1)event.setCanceled(true);
      }else if(event.getEntity() instanceof ExperienceOrb orb&&bounds.contains(orb.position())){
        xpAttempts.incrementAndGet();reenter.run();
      }
    };
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(callback);
    try{
      h.assertTrue(first.canMate(second),"Parents were not eligible before publication");
      first.spawnChildFromBreeding(level,second);
      h.assertTrue(eggAttempts.get()==1&&level.getEntitiesOfClass(ChaoEggEntity.class,bounds,e->keeper.getUUID().equals(e.owner)).isEmpty(),"Canceled/reentrant publication left an egg");
      h.assertTrue(first.isInLove()&&second.isInLove()&&first.canMate(second)&&cooldown(first)==0&&cooldown(second)==0,"Failed publication consumed love/cooldown or retained a reservation");
      h.assertTrue(xpAttempts.get()==0&&hearts(channel)==0,"Rejected breeding emitted XP or completion hearts");
      first.spawnChildFromBreeding(level,second);
      h.assertTrue(eggAttempts.get()==2&&level.getEntitiesOfClass(ChaoEggEntity.class,bounds,e->keeper.getUUID().equals(e.owner)).size()==1,"Retry did not publish exactly one egg");
      h.assertTrue(!first.isInLove()&&!second.isInLove()&&cooldown(first)==6000&&cooldown(second)==6000,"Successful publication did not commit both parents once");
      h.assertTrue(xpAttempts.get()==1&&level.getEntitiesOfClass(ExperienceOrb.class,bounds).size()==1&&hearts(channel)==12,"Successful breeding did not emit exactly one reward/heart burst");
      first.spawnChildFromBreeding(level,second);second.spawnChildFromBreeding(level,first);third.spawnChildFromBreeding(level,first);
      h.assertTrue(eggAttempts.get()==2&&xpAttempts.get()==1&&hearts(channel)==0,"Repeated callbacks duplicated offspring or rewards");
      h.assertTrue(third.isInLove()&&cooldown(third)==0,"Reentrant attempt consumed the uninvolved parent's eligibility");
    }finally{
      net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(callback);
      level.getEntitiesOfClass(ChaoEggEntity.class,bounds,e->keeper.getUUID().equals(e.owner)).forEach(ChaoEggEntity::discard);
      level.getEntitiesOfClass(ExperienceOrb.class,bounds).forEach(ExperienceOrb::discard);
      first.discard();second.discard();third.discard();keeper.server.getPlayerList().remove(keeper);
    }
    h.succeed();
  }
}
