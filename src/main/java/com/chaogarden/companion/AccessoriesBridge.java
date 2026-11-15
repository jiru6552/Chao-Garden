package com.chaogarden.companion;

import com.chaogarden.ChaoGarden;
import com.mojang.logging.LogUtils;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * Optional boundary against the real Accessories 1.21.1 API. The published
 * beta.54 binary and 1.21.x source were checked; there are no optional-class
 * descriptors in ordinary Chao classes, so a standalone server can load them.
 */
public final class AccessoriesBridge {
  private static Class<?> slotType, capabilityType, containerType, entryType;
  // Resolve API metadata once, while the optional mod is present. Values such
  // as capabilities, real slot stacks and containers are still read live.
  private static Method slotEntity, slotStack, slotContainer, capabilityGet,
      capabilityEquipped, capabilityContainers, containerAccessories,
      containerChanged, entryStack;
  private static boolean ready;
  private static final String API = "io.wispforest.accessories.api.";

  private AccessoriesBridge() {}

  public static boolean available() { return ModList.get().isLoaded("accessories"); }

  public static void register() {
    if (!available()) return;
    try {
      Class<?> accessoryType = Class.forName(API + "Accessory");
      Class<?> accessories = Class.forName(API + "AccessoriesAPI");
      slotType = Class.forName(API + "slot.SlotReference");
      capabilityType = Class.forName(API + "AccessoriesCapability");
      containerType = Class.forName(API + "AccessoriesContainer");
      entryType = Class.forName(API + "slot.SlotEntryReference");
      slotEntity = slotType.getMethod("entity");
      slotStack = slotType.getMethod("getStack");
      slotContainer = slotType.getMethod("slotContainer");
      capabilityGet = capabilityType.getMethod("get", LivingEntity.class);
      capabilityEquipped = capabilityType.getMethod("getEquipped", Item.class);
      capabilityContainers = capabilityType.getMethod("getContainers");
      containerAccessories = containerType.getMethod("getAccessories");
      containerChanged = containerType.getMethod("markChanged", boolean.class);
      entryStack = entryType.getMethod("stack");
      Object implementation = Proxy.newProxyInstance(accessoryType.getClassLoader(),
          new Class<?>[] {accessoryType}, AccessoriesBridge::callback);
      accessories.getMethod("registerAccessory", Item.class, accessoryType)
          .invoke(null, ChaoGarden.item("chao_carrier"), implementation);
      ready = true;
      LogUtils.getLogger().info("Chao companion registered with the Accessories API");
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException("Cannot register the Chao companion with this Accessories API", exception);
    }
  }

  private static Object callback(Object proxy, Method method, Object[] supplied) throws Throwable {
    Object[] args = supplied == null ? new Object[0] : supplied;
    if (method.getDeclaringClass() == Object.class) return switch (method.getName()) {
      case "toString" -> "Chao Garden companion accessory";
      case "hashCode" -> System.identityHashCode(proxy);
      case "equals" -> proxy == args[0];
      default -> null;
    };
    ItemStack stack = args.length > 0 && args[0] instanceof ItemStack s ? s : ItemStack.EMPTY;
    LivingEntity entity = args.length > 1 && slotType.isInstance(args[1])
        ? (LivingEntity) slotEntity.invoke(args[1]) : null;
    return switch (method.getName()) {
      case "canEquip" -> entity instanceof Player player && CompanionContent.canEquip(stack, player);
      case "canEquipFromUse" -> !stack.isEmpty() && CompanionContent.isEquippable(stack);
      case "canUnequip" -> {
        if (entity instanceof ServerPlayer player) CompanionContent.beforeUnequip(player, stack);
        yield true;
      }
      case "tick" -> {
        if (entity instanceof ServerPlayer player) CompanionContent.accessoryTick(player, stack, args[1]);
        yield null;
      }
      case "onUnequip" -> {
        Object current = slotStack.invoke(args[1]);
        // Accessories may report a component refresh as equipment changing.
        // Updating the saved Chao in its existing slot must not recall it.
        boolean sameChao = current instanceof ItemStack held
            && CompanionContent.token(stack, false) != null
            && CompanionContent.token(stack, false).equals(CompanionContent.token(held, false));
        if (!sameChao && entity instanceof ServerPlayer player) CompanionContent.unequip(player, stack);
        yield null;
      }
      case "getDropRule" -> enumConstant(method.getReturnType(), "KEEP");
      default -> method.isDefault() ? InvocationHandler.invokeDefault(proxy, method, args) : null;
    };
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static Object enumConstant(Class<?> type, String name) {
    return Enum.valueOf((Class<? extends Enum>) type, name);
  }

  /** The callback receives the real inventory stack; tell Accessories to sync it after changes. */
  public static void changed(Object reference) {
    if (!ready || reference == null) return;
    try {
      Object container = slotContainer.invoke(reference);
      if (container != null) containerChanged.invoke(container, false);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException("Cannot sync a Chao companion accessory", exception);
    }
  }

  public static List<ItemStack> equipped(Player player) {
    List<ItemStack> out = new ArrayList<>();
    if (!ready) return out;
    try {
      Object capability = capabilityGet.invoke(null, player);
      if (capability == null) return out;
      var entries = (List<?>) capabilityEquipped.invoke(capability, ChaoGarden.item("chao_carrier"));
      for (Object entry : entries) out.add((ItemStack) entryStack.invoke(entry));
      return out;
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException("Cannot inspect equipped Chao companions", exception);
    }
  }

  /** Optional storage accessories expose their own public item capability. */
  public record StoredAccessory(ItemStack stack, Runnable changed) {}
  public static List<StoredAccessory> storageAccessories(Player player) {
    List<StoredAccessory> out=new ArrayList<>();if(!ready)return out;
    try {
      Object cap=capabilityGet.invoke(null,player);if(cap==null)return out;
      var containers=(java.util.Map<?,?>)capabilityContainers.invoke(cap);
      for(Object container:containers.values()){
        var inventory=(net.minecraft.world.Container)containerAccessories.invoke(container);
        for(int i=0;i<inventory.getContainerSize();i++){
          var stack=inventory.getItem(i);if(stack.isEmpty())continue;
          out.add(new StoredAccessory(stack,()->{
            try{containerChanged.invoke(container,false);}
            catch(ReflectiveOperationException error){throw new IllegalStateException("Cannot sync recovered accessory storage",error);}
          }));
        }
      }return out;
    }catch(ReflectiveOperationException error){throw new IllegalStateException("Cannot inspect accessory storage",error);}
  }

  /** Prevent the default body-mounted item renderer: this accessory summons its own living body. */
  public static void disableDefaultRenderer() {
    if (!available()) return;
    try {
      Class.forName(API + "client.AccessoriesRendererRegistry")
          .getMethod("registerNoRenderer", Item.class).invoke(null, ChaoGarden.item("chao_carrier"));
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException("Cannot configure Chao companion accessory rendering", exception);
    }
  }
}
