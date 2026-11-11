package com.chaogarden.cwe;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.core.OmochaoRules;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.companion.CompanionContent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** One physical output replaces an ended garden robot; companion defeat remains a temporary recall. */
@EventBusSubscriber(modid=ChaoGarden.MODID)
public final class OmochaoLifecycle {
  private static final java.util.Set<java.util.UUID> IN_FLIGHT=java.util.concurrent.ConcurrentHashMap.newKeySet();
  private OmochaoLifecycle(){}

  /** Failed item insertion keeps the robot and all real equipment for a later loaded tick. */
  public static boolean resumePending(ChaoEntity chao){
    if(!OmochaoRules.isOmochao(chao.state())||chao.level().isClientSide||CompanionContent.isManaged(chao))return false;
    int pending=chao.state().robotPendingOutcome();if(pending==0)pending=chao.state().intTrait("robot.expired");
    if(chao.state().robotOutputCommitted()&&pending==0)pending=1;
    if(pending==0)return false;
    finish(chao,pending==2?ChaoState.Transition.REINCARNATED:ChaoState.Transition.DIED);return true;
  }

  public static boolean finish(ChaoEntity chao,ChaoState.Transition transition){
    if(!(chao.level() instanceof ServerLevel level)||!OmochaoRules.isOmochao(chao.state())||CompanionContent.isManaged(chao)
        ||transition!=ChaoState.Transition.DIED&&transition!=ChaoState.Transition.REINCARNATED)return false;
    if(chao.isRemoved())return true;
    if(!IN_FLIGHT.add(chao.getUUID()))return false;
    try {
    var state=chao.state();
    int pending=state.robotPendingOutcome();if(pending==0)pending=state.intTrait("robot.expired");
    boolean heart=pending>0?pending==2:transition==ChaoState.Transition.REINCARNATED;
    if(state.robotOutputCommitted()||state.intTrait("robot.output_committed")>0)return cleanupCommitted(chao,level,heart);
    state.dead=true;state.cocoonTicks=0;state.setRobotPendingOutcome(heart?2:1);state.setTrait("robot.expired",heart?2:1);
    var output=new ItemEntity(level,chao.getX(),chao.getY()+.15,chao.getZ(),CweRobotPartItem.recovered(state,chao.getUUID(),heart));
    output.setDefaultPickUpDelay();
    if(!level.addFreshEntity(output)){chao.sync();return false;}
    // Both actor and output share a chunk. In-flight protection also covers
    // callbacks during item insertion; then mark before equipment/discard.
    state.setRobotOutputCommitted(true);state.setTrait("robot.output_committed",1);
    return cleanupCommitted(chao,level,heart);
    } finally {IN_FLIGHT.remove(chao.getUUID());}
  }

  private static boolean cleanupCommitted(ChaoEntity chao,ServerLevel level,boolean heart){
    var state=chao.state();
    if(!chao.releaseLifecycleItems()){chao.sync();return false;}
    // A mechanical successor has a new UUID. Only committed terminal robots
    // leave the garden ledger; ordinary carried/reborn Chao keep their slots.
    var registry=com.chaogarden.activities.GardenRegistry.get(level.getServer());
    boolean removedEntry=registry.entries.remove(chao.getUUID())!=null;
    boolean removedRecovery=registry.recoveries.remove(chao.getUUID())!=null;
    if(removedEntry||removedRecovery)registry.setDirty();
    String actor=chao.getUUID().toString(),token=state.trait("content.builder_token");
    for(var world:level.getServer().getAllLevels())for(var entity:world.getAllEntities())
      if(entity instanceof ChaoEntity builder&&builder!=chao
          &&actor.equals(builder.state().trait("content.omochao_uuid"))
          &&token.equals(builder.state().trait("content.builder_token"))){
        builder.state().setTrait("content.omochao_uuid","");builder.state().setTrait("content.omochao_parts","");
        builder.state().setTrait("content.builder_token","");builder.sync();
      }
    level.sendParticles(heart?ParticleTypes.HEART:ParticleTypes.POOF,chao.getX(),chao.getY()+.35,chao.getZ(),12,.2,.25,.2,.02);
    com.chaogarden.recovery.ChaoRecovery.lifecycleFinished(chao);
    chao.discard();
    return true;
  }

  /** Actual garden combat death yields a component, never a care heart. */
  @SubscribeEvent(priority=EventPriority.LOWEST)
  public static void death(LivingDeathEvent event){
    if(!(event.getEntity() instanceof ChaoEntity chao)||chao.level().isClientSide
        ||!OmochaoRules.isOmochao(chao.state())||CompanionContent.isManaged(chao)
        ||chao.state().intTrait("content.construction")>0)return;
    event.setCanceled(true);
    if(!finish(chao,ChaoState.Transition.DIED))chao.setHealth(1);
  }
}
