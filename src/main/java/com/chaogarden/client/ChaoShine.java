package com.chaogarden.client;

import com.chaogarden.core.ChaoState;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

/** Continuous silver reflections with the source coat's alpha silhouette. */
@EventBusSubscriber(modid="chao_garden", bus=EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public final class ChaoShine extends RenderType {
  private ChaoShine(String name, VertexFormat format, VertexFormat.Mode mode, int size,
                    boolean crumbling, boolean sort, Runnable setup, Runnable clear) {
    super(name, format, mode, size, crumbling, sort, setup, clear);
  }

  private static final ResourceLocation REFLECTION = ResourceLocation.fromNamespaceAndPath(
      "chao_garden", "textures/misc/chao_reflection.png");
  private static final int SURFACE_LIMIT = 512;
  private static final Map<ResourceLocation, RenderType> SURFACES = new LinkedHashMap<>(64,.75f,true) {
    @Override protected boolean removeEldestEntry(Map.Entry<ResourceLocation, RenderType> eldest) {
      return size()>SURFACE_LIMIT;
    }
  };
  /** RenderType objects do not own the textures bound by their state. */
  public static void clearSurfaceCache() { SURFACES.clear(); }
  static int cachedSurfaceCount() { return SURFACES.size(); }
  private static ShaderInstance shader;

  @SubscribeEvent public static void shaders(RegisterShadersEvent event) throws IOException {
    clearSurfaceCache();
    event.registerShader(new ShaderInstance(event.getResourceProvider(),
        ResourceLocation.fromNamespaceAndPath("chao_garden", "chao_reflection"),
        DefaultVertexFormat.POSITION_TEX), loaded -> shader=loaded);
  }
  // The shader projects one continuous field from view-space geometry. Atlas
  // UVs are reserved for the original texture's alpha mask; they never drive
  // the reflection, so egg longitude panels and Chao UV islands cannot reset it.
  private static final TexturingStateShard REFLECTION_TEXTURING = new TexturingStateShard(
      "chao_reflection_texturing", () -> setupGlintTexturing(3f),
      com.mojang.blaze3d.systems.RenderSystem::resetTextureMatrix);

  /**
   * Source-alpha masking keeps the original eye and wing silhouettes intact,
   * including overlapping or coplanar faces where equal depth alone cannot.
   * Vanilla's foil texture transform supplies continuous motion and respects
   * the player's glint-speed and glint-strength accessibility settings.
   */
  public static RenderType surface(ResourceLocation source) {
    return SURFACES.computeIfAbsent(source, texture -> create("chao_garden_silver_reflection",
      DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 1536, false, false,
      CompositeState.builder()
          .setShaderState(new ShaderStateShard(() -> shader))
          .setTextureState(MultiTextureStateShard.builder()
              .add(texture, false, false).add(REFLECTION, true, false).build())
          .setTransparencyState(GLINT_TRANSPARENCY)
          .setDepthTestState(EQUAL_DEPTH_TEST)
          .setWriteMaskState(COLOR_WRITE)
          .setCullState(NO_CULL)
          .setTexturingState(REFLECTION_TEXTURING)
          .createCompositeState(false)));
  }

  public static boolean visible(ChaoState state) {
    return state.isShiny() || state.jewel() > 0;
  }
}
