package com.chaogarden.client;

import com.chaogarden.companion.CompanionNetwork;
import com.chaogarden.companion.AccessoriesBridge;
import com.chaogarden.item.ChaoCarrierItem;
import java.util.UUID;
import com.chaogarden.companion.EquippedKnockback080;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** A physical use press, not vanilla's four-tick held-use repeat. No UI text. */
@EventBusSubscriber(modid="chao_garden",value=Dist.CLIENT)
public final class EquippedAbilityInput080 {
  private static boolean latched,freshUse;
  private static int sequence;
  private EquippedAbilityInput080() {}
  @SubscribeEvent(priority=EventPriority.LOWEST)
  public static void use(InputEvent.InteractionKeyMappingTriggered event){
    if(!event.isUseItem()||event.getHand()!=InteractionHand.MAIN_HAND)return;
    freshUse=!latched;latched=true;
    // Do not cancel the event or swing: ordinary block/entity/off-hand use continues.
  }
  @SubscribeEvent public static void empty(PlayerInteractEvent.RightClickEmpty event){
    if(event.getHand()!=InteractionHand.MAIN_HAND)return;
    var mc=Minecraft.getInstance();
    boolean fresh=freshUse||!latched;freshUse=false;latched=true;
    if(!fresh||mc.player==null||event.getEntity()!=mc.player||mc.screen!=null
        ||!mc.player.isShiftKeyDown()||!mc.player.getMainHandItem().isEmpty()||!mc.player.getOffhandItem().isEmpty()
        ||mc.hitResult==null||mc.hitResult.getType()!=HitResult.Type.MISS)return;
    if(sequence==Integer.MAX_VALUE)return; // Session-length bound; reset only on disconnect.
    UUID expected=null;
    for(var stack:AccessoriesBridge.equipped(mc.player))if(ChaoCarrierItem.filled(stack)){
      UUID stored=ChaoCarrierItem.storedUUID(stack);if(stored==null||expected!=null)return;expected=stored;
    }
    if(expected!=null)CompanionNetwork.requestAbilityCycle080(++sequence,mc.player.level().dimension().location(),expected);
  }
  @SubscribeEvent public static void tick(ClientTickEvent.Post event){
    var mc=Minecraft.getInstance();freshUse=false;
    if(mc.player==null||mc.screen!=null||!mc.options.keyUse.isDown())latched=false;
    if(mc.player!=null)EquippedKnockback080.clientTick(mc.player);
  }
  @SubscribeEvent public static void leave(ClientPlayerNetworkEvent.LoggingOut event){latched=freshUse=false;sequence=0;EquippedKnockback080.clearClient();}
}
