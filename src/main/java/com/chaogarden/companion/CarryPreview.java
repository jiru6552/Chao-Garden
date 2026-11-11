package com.chaogarden.companion;

import com.chaogarden.ChaoGarden;
import com.chaogarden.entity.ChaoEntity;
import java.util.*;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Operator preview of the real attachment path. No inventory, stat or player movement changes. */
@EventBusSubscriber(modid=ChaoGarden.MODID)
public final class CarryPreview {
  private record Session(UUID keeper,long start,boolean loop) {}
  private static final Map<ChaoEntity,Session> sessions=new WeakHashMap<>();
  public static boolean active(ChaoEntity chao){return sessions.containsKey(chao);}
  public static void stop(ChaoEntity chao){sessions.remove(chao);}
  public static int position(ChaoEntity chao,ServerPlayer keeper,int actual) {
    var s=sessions.get(chao);if(s==null)return actual;
    long elapsed=(chao.level().getGameTime()-s.start)%160;
    return CarryRules.position(chao.state(),elapsed>=50&&elapsed<100,elapsed>=100&&elapsed<130);
  }
  @SubscribeEvent public static void tick(ServerTickEvent.Pre e) {
    sessions.entrySet().removeIf(entry->{
      var chao=entry.getKey();var s=entry.getValue();var owner=e.getServer().getPlayerList().getPlayer(s.keeper);
      return chao.isRemoved()||!chao.isAlive()||owner==null||owner.level()!=chao.level()||!CompanionContent.isManaged(chao)
          ||owner.isFallFlying()||owner.getAbilities().flying||owner.isInWater()||chao.hurtTime>0||chao.isHandlingItem()||!s.loop&&chao.level().getGameTime()-s.start>=160;
    });
  }
  @SubscribeEvent public static void left(EntityLeaveLevelEvent e){if(e.getEntity() instanceof ChaoEntity c)stop(c);}
  @SubscribeEvent public static void damage(LivingIncomingDamageEvent e){
    if(e.getEntity() instanceof ChaoEntity c)stop(c);
    if(e.getEntity() instanceof ServerPlayer p)sessions.entrySet().removeIf(entry->entry.getValue().keeper.equals(p.getUUID()));
  }
  @SubscribeEvent public static void interact(PlayerInteractEvent.EntityInteract e){if(e.getTarget() instanceof ChaoEntity c)stop(c);}
  @SubscribeEvent public static void commands(RegisterCommandsEvent e) {
    var root=Commands.literal("testcarry").requires(s->s.hasPermission(2));
    for(String mode:List.of("once","loop","stop"))root.then(Commands.literal(mode).executes(ctx->{
      var player=ctx.getSource().getPlayerOrException();
      ChaoEntity body=null;
      for(var stack:AccessoriesBridge.equipped(player)){
        var entity=CompanionContent.findEntity(player.server,com.chaogarden.item.ChaoCarrierItem.storedUUID(stack));
        if(entity instanceof ChaoEntity c&&CompanionContent.isManaged(c))body=c;
      }
      if(body==null){ctx.getSource().sendFailure(Component.literal("Equip a living Chao in your Chao accessory slot first."));return 0;}
      if(mode.equals("stop"))stop(body);
      else {
        if(player.isFallFlying()||player.getAbilities().flying||player.isInWater()){ctx.getSource().sendFailure(Component.literal("Start this preview on dry ground, outside flight."));return 0;}
        if(body.isHandlingItem()||body.hurtTime>0){ctx.getSource().sendFailure(Component.literal("Let the current interaction finish first."));return 0;}
        sessions.put(body,new Session(player.getUUID(),body.level().getGameTime(),mode.equals("loop")));
      }
      ctx.getSource().sendSuccess(()->Component.literal("Carrying preview: "+mode+". Ground → flight → swim → ground, using this Chao's real ability unlocks. /chao testcarry stop ends playback."),false);return 1;
    }));
    e.getDispatcher().register(Commands.literal("chao").then(root));
  }
}
