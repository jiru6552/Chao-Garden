package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.companion.AccessoriesBridge;
import com.chaogarden.entity.ChaoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** The inventory item is a live, blinking Chao model in either hand. */
@EventBusSubscriber(modid = ChaoGarden.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HeldChaoRenderer extends BlockEntityWithoutLevelRenderer {
  private static HeldChaoRenderer instance;
  private static long firstPersonRenders;
  private static final ResourceLocation EMPTY = ResourceLocation.fromNamespaceAndPath(
      ChaoGarden.MODID, "textures/item/held_chao.png");
  private static final int CACHE_LIMIT=64;
  private final Map<UUID, Entry> cache = lru();
  private final Map<DataKey, Snapshot> snapshots = lru();
  private ClientLevel cacheLevel;
  private long copies,loads,comparisons;

  private static <K,V> Map<K,V> lru() {
    return new LinkedHashMap<K,V>(16,.75f,true) {
      @Override protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {return size()>CACHE_LIMIT;}
    };
  }
  /** CustomData owns an immutable tag snapshot. Identity lookup avoids per-frame NBT comparison. */
  private record DataKey(CustomData data) {
    @Override public boolean equals(Object other) {return other instanceof DataKey key&&key.data==data;}
    @Override public int hashCode() {return System.identityHashCode(data);}
  }
  private record Snapshot(CompoundTag entity,UUID id) {}
  private static final class Entry {ChaoEntity chao;CompoundTag entityTag;}
  public record CacheStats(long copies,long loads,long comparisons,int snapshots,int entities) {}

  private HeldChaoRenderer() {
    super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
  }
  public static void clearCache() {
    if(instance!=null){instance.cache.clear();instance.snapshots.clear();instance.cacheLevel=null;instance.copies=instance.loads=instance.comparisons=0;}
  }
  static CacheStats cacheStats() {
    return instance==null?new CacheStats(0,0,0,0,0):new CacheStats(instance.copies,instance.loads,instance.comparisons,instance.snapshots.size(),instance.cache.size());
  }
  static ChaoEntity preparedEntity(ItemStack stack) {
    if(instance==null)instance=new HeldChaoRenderer();
    return instance.prepare(stack);
  }
  private ChaoEntity prepare(ItemStack stack) {
    var level=Minecraft.getInstance().level;if(level==null)return null;
    if(cacheLevel!=level){cache.clear();snapshots.clear();cacheLevel=level;}
    CustomData component=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY);
    if(component==CustomData.EMPTY)return null;
    Snapshot snapshot=snapshots.computeIfAbsent(new DataKey(component),ignored->{
      CompoundTag root=component.copyTag();copies++;
      if(!root.contains("ChaoEntity"))return new Snapshot(null,null);
      CompoundTag entity=root.getCompound("ChaoEntity");
      UUID id=entity.hasUUID("UUID")?entity.getUUID("UUID"):new UUID(0,entity.hashCode());
      return new Snapshot(entity,id);
    });
    if(snapshot.entity==null)return null;
    Entry entry=cache.computeIfAbsent(snapshot.id,ignored->new Entry());
    if(entry.chao==null){entry.chao=ChaoGarden.CHAO.get().create(level);entry.entityTag=null;}
    if(entry.chao==null)return null;
    if(entry.entityTag!=snapshot.entity){
      boolean changed=entry.entityTag==null;
      if(!changed){comparisons++;changed=!entry.entityTag.equals(snapshot.entity);}
      if(changed){entry.chao.load(snapshot.entity);loads++;}
      // Rebind even equal content: subsequent frames of this component use an
      // identity check instead of repeatedly walking its complete NBT tree.
      entry.entityTag=snapshot.entity;
    }
    ChaoEntity chao=entry.chao;
    chao.tickCount=(int)(level.getGameTime()&Integer.MAX_VALUE);chao.setNoAi(true);chao.setPos(0,0,0);
    chao.yBodyRot=chao.yBodyRotO=chao.yHeadRot=chao.yHeadRotO=0;
    chao.setYRot(0);chao.yRotO=0;chao.setXRot(0);chao.xRotO=0;
    chao.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    return chao;
  }

  /** Allows the excluded development gallery to verify that the actual hand path rendered. */
  public static long firstPersonRenders() { return firstPersonRenders; }

  @SubscribeEvent
  public static void extensions(RegisterClientExtensionsEvent event) {
    event.registerItem(new IClientItemExtensions() {
      @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if (instance == null) instance = new HeldChaoRenderer();
        return instance;
      }
    }, ChaoGarden.item("chao_carrier"));
  }

  @SubscribeEvent
  public static void setup(FMLClientSetupEvent event) {
    event.enqueueWork(AccessoriesBridge::disableDefaultRenderer);
  }

  @Override
  public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
      MultiBufferSource buffers, int light, int overlay) {
    Minecraft minecraft = Minecraft.getInstance();
    ChaoEntity chao=preparedEntity(stack);
    if(chao==null){empty(pose,buffers,light,overlay);return;}
    pose.pushPose();
    pose.translate(.5, .02, .5);
    if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
        || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
      // The entity has its origin at its feet, unlike a centered item sprite.
      // Lift the complete creature into the hand view and mirror its small
      // inward offset so the head, feet and floating marker fit in either hand.
      pose.translate(context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? -.06 : .06, .56, .12);
      pose.scale(.55f, .55f, .55f);
    }
    float partial = minecraft.getTimer().getGameTimeDeltaPartialTick(true);
    boolean names = ChaoRenderer.suppressNames;
    ChaoRenderer.suppressNames = true;
    var dispatcher = minecraft.getEntityRenderDispatcher();
    dispatcher.setRenderShadow(false);
    try {
      dispatcher.render(chao, 0, 0, 0, 180, partial, pose, buffers, light);
      if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
          || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) firstPersonRenders++;
    } finally {
      dispatcher.setRenderShadow(true); ChaoRenderer.suppressNames = names; pose.popPose();
    }
  }

  private static void empty(PoseStack stack, MultiBufferSource buffers, int light, int overlay) {
    VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(EMPTY));
    var pose = stack.last();
    float[][] positions = {{0,0,0,1},{1,0,1,1},{1,1,1,0},{0,1,0,0}};
    for (float[] point : positions) {
      org.joml.Vector3f position = pose.pose().transformPosition(point[0], point[1], .5f, new org.joml.Vector3f());
      org.joml.Vector3f normal = pose.transformNormal(new org.joml.Vector3f(0,0,1), new org.joml.Vector3f());
      vertices.addVertex(position.x, position.y, position.z, -1, point[2], point[3], overlay, light,
          normal.x, normal.y, normal.z);
    }
  }
}
