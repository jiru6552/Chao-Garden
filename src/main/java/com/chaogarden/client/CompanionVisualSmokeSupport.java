package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.companion.AccessoriesBridge;
import com.chaogarden.companion.CompanionContent;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoCarrierItem;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Opt-in study support using the real slot API; excluded from the installable JAR. */
public final class CompanionVisualSmokeSupport {
  private CompanionVisualSmokeSupport(){}
  private static Object container(ServerPlayer player){
    if(!AccessoriesBridge.available())throw new IllegalStateException("Actual Accessories is required for this study");
    try{
      var type=Class.forName("io.wispforest.accessories.api.AccessoriesCapability");
      var capability=type.getMethod("get",LivingEntity.class).invoke(null,player);
      var containers=(Map<?,?>)type.getMethod("getContainers").invoke(capability);var container=containers.get("chao_companion");
      if(container==null)throw new IllegalStateException("Actual Chao companion slot missing");return container;
    }catch(ReflectiveOperationException e){throw new IllegalStateException("Cannot inspect actual Accessories slot",e);}
  }
  private static Container inventory(Object container){
    try{return (Container)Class.forName("io.wispforest.accessories.api.AccessoriesContainer").getMethod("getAccessories").invoke(container);}
    catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
  }
  private static void changed(Object container){
    try{Class.forName("io.wispforest.accessories.api.AccessoriesContainer").getMethod("markChanged",boolean.class).invoke(container,false);}
    catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
  }
  public static void clear(ServerPlayer player){
    var container=container(player);var inventory=inventory(container);
    for(int slot=0;slot<inventory.getContainerSize();slot++){
      var current=inventory.getItem(slot);if(!current.isEmpty())CompanionContent.unequip(player,current);
      inventory.setItem(slot,ItemStack.EMPTY);
    }
    changed(container);
  }
  public static ItemStack equip(ServerPlayer player,ChaoState state){
    clear(player);var chao=ChaoGarden.CHAO.get().create(player.serverLevel());chao.owner=player.getUUID();chao.setState(state);
    var carrier=new ItemStack(ChaoGarden.item("chao_carrier"));
    if(!ChaoCarrierItem.capture(carrier,chao,player)||!CompanionContent.canEquip(carrier,player))throw new IllegalStateException("Actual companion admission rejected the study fixture");
    var container=container(player);inventory(container).setItem(0,carrier);changed(container);return carrier;
  }
  public static ChaoEntity body(ServerPlayer player){
    for(var stack:AccessoriesBridge.equipped(player)){
      var entity=CompanionContent.findEntity(player.server,ChaoCarrierItem.storedUUID(stack));
      if(entity instanceof ChaoEntity chao&&CompanionContent.isManaged(chao))return chao;
    }
    return null;
  }
  public static ItemStack held(ServerPlayer player){
    return AccessoriesBridge.equipped(player).stream().findFirst().orElse(ItemStack.EMPTY);
  }
}
