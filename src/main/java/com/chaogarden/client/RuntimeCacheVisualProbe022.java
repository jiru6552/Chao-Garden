package com.chaogarden.client;

import com.chaogarden.ChaoGarden;
import com.chaogarden.core.ChaoState;
import com.chaogarden.entity.ChaoEntity;
import com.chaogarden.item.ChaoCarrierItem;
import com.mojang.logging.LogUtils;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Actual client cache contracts. Development fixture, excluded from the JAR. */
@EventBusSubscriber(modid=ChaoGarden.MODID,value=Dist.CLIENT)
public final class RuntimeCacheVisualProbe022 {
  private static boolean done;
  private RuntimeCacheVisualProbe022() {}
  @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
    if(done||!Boolean.getBoolean("chao_garden.audit022Study")||Minecraft.getInstance().level==null)return;
    done=true;
    LogUtils.getLogger().info("CHAO_RUNTIME_CACHE_022_PROBE_COMPLETE: {} actual render-state checks; bounded512 LRU, eviction and reload-clear verified",inspect());
    LogUtils.getLogger().info("CHAO_HELD_CACHE_022_PROBE_COMPLETE: {} actual snapshot/entity/pose checks; immutable component reuse, structural change detection, bounded64 LRUs and clear verified",inspectHeld());
  }
  private static void require(boolean condition,String reason) {if(!condition)throw new IllegalStateException("Runtime cache audit: "+reason);}
  private static ResourceLocation id(int i) {return ResourceLocation.fromNamespaceAndPath(ChaoGarden.MODID,"dynamic/cache_audit/slot_"+i);}
  public static int inspect() {
    int checks=0;ChaoShine.clearSurfaceCache();
    try {
      RenderType first=ChaoShine.surface(id(0));
      for(int i=1;i<4096;i++){ChaoShine.surface(id(i));require(ChaoShine.cachedSurfaceCount()<=512,"unbounded render-state working set");checks++;}
      require(ChaoShine.cachedSurfaceCount()==512,"LRU did not fill");require(ChaoShine.surface(id(0))!=first,"oldest was not evicted");checks+=2;
      ChaoShine.clearSurfaceCache();RenderType retained=ChaoShine.surface(id(0)),oldest=ChaoShine.surface(id(1));
      for(int i=2;i<512;i++)ChaoShine.surface(id(i));
      require(ChaoShine.surface(id(0))==retained,"repeat source not reused");ChaoShine.surface(id(512));
      require(ChaoShine.surface(id(0))==retained,"recently accessed state evicted");require(ChaoShine.surface(id(1))!=oldest,"unaccessed state retained");checks+=3;
      ChaoShine.clearSurfaceCache();require(ChaoShine.cachedSurfaceCount()==0,"clear retained states");require(ChaoShine.surface(id(0))!=retained,"clear reused stale render state");checks+=2;
      return checks;
    } finally {ChaoShine.clearSurfaceCache();}
  }
  private static ItemStack held(ChaoEntity body) {var stack=new ItemStack(ChaoGarden.item("chao_carrier"));ChaoCarrierItem.snapshot(stack,body);return stack;}
  private static int inspectHeld() {
    var mc=Minecraft.getInstance();int checks=0;HeldChaoRenderer.clearCache();
    try {
      ChaoEntity source=ChaoGarden.CHAO.get().create(mc.level);require(source!=null,"no source entity");source.setUUID(new UUID(22,1));
      var state=new ChaoState();state.happiness=32;source.setState(state);
      ItemStack stack=held(source);ChaoEntity cached=HeldChaoRenderer.preparedEntity(stack);
      require(cached!=null&&cached!=source,"no isolated held entity");checks++;
      for(int i=0;i<120;i++){require(HeldChaoRenderer.preparedEntity(stack)==cached,"steady frame replaced entity");checks++;}
      var stats=HeldChaoRenderer.cacheStats();require(stats.copies()==1&&stats.loads()==1&&stats.comparisons()==0,"steady frame copied/reloaded NBT");checks++;
      ItemStack equal=held(source);require(HeldChaoRenderer.preparedEntity(equal)==cached,"equal component replaced body");checks++;
      stats=HeldChaoRenderer.cacheStats();require(stats.copies()==2&&stats.loads()==1&&stats.comparisons()==1,"equal component reloaded body");checks++;
      for(int i=0;i<120;i++){require(HeldChaoRenderer.preparedEntity(equal)==cached&&HeldChaoRenderer.cacheStats().comparisons()==1,"equal component repeated structural comparison");checks++;}
      stats=HeldChaoRenderer.cacheStats();require(stats.copies()==2&&stats.loads()==1,"equal steady frames copied/reloaded NBT");checks++;
      CompoundTag outer=ChaoCarrierItem.data(equal);outer.putString("UnrelatedItemMetadata","retained");ChaoCarrierItem.setData(equal,outer);
      require(HeldChaoRenderer.preparedEntity(equal)==cached&&HeldChaoRenderer.cacheStats().loads()==1,"outer metadata reset animation");checks++;
      source.state().happiness=73;ItemStack changed=held(source);
      require(HeldChaoRenderer.preparedEntity(changed)==cached&&cached.state().happiness==73,"changed component not refreshed in place");checks++;
      require(HeldChaoRenderer.cacheStats().loads()==2,"changed component loaded more than once");checks++;
      require(cached.tickCount==(int)(mc.level.getGameTime()&Integer.MAX_VALUE),"held animation clock frozen");checks++;
      var renderer=mc.getEntityRenderDispatcher().getRenderer(cached);require(renderer instanceof ChaoRenderer,"actual Chao renderer not resolved");checks++;
      var model=((ChaoRenderer)renderer).getModel();
      for(int i=0;i<12;i++){model.setupAnim(cached,0,0,cached.tickCount+i*.1f,0,0);require(model.officialPosesFinite()&&model.officialMotion()==OfficialChaoRig.Motion.HELD,"cached body lost held pose");checks++;}
      for(int i=0;i<500;i++) {
        source.setUUID(new UUID(22,i+2));source.state().happiness=i%100;
        var prepared=HeldChaoRenderer.preparedEntity(held(source));stats=HeldChaoRenderer.cacheStats();
        require(prepared!=null&&stats.entities()<=64&&stats.snapshots()<=64,"new revision failed or cache exceeded64 entries");checks++;
      }
      stats=HeldChaoRenderer.cacheStats();require(stats.entities()==64&&stats.snapshots()==64,"working set not bounded at64");checks++;
      HeldChaoRenderer.clearCache();stats=HeldChaoRenderer.cacheStats();require(stats.entities()==0&&stats.snapshots()==0,"world-clear retained components/entities");checks++;
      return checks;
    } finally {HeldChaoRenderer.clearCache();}
  }
}
