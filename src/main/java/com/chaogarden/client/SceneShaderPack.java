package com.chaogarden.client;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.neoforged.fml.ModList;

/** The public Iris API is optional and only resolved on the physical client. */
final class SceneShaderPack {
  private record Queries(MethodHandle active, MethodHandle shadows) {}
  private static final Queries QUERIES = queries();

  private static Queries queries() {
    if (!ModList.get().isLoaded("iris")) return null;
    try {
      var api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
      var instance = api.getMethod("getInstance").invoke(null);
      var flag = MethodType.methodType(boolean.class);
      var lookup = MethodHandles.publicLookup();
      return new Queries(lookup.findVirtual(api, "isShaderPackInUse", flag).bindTo(instance),
          lookup.findVirtual(api, "isRenderingShadowPass", flag).bindTo(instance));
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Unable to resolve the Iris v0 render-pass API", e);
    }
  }

  static boolean active() { return QUERIES != null && read(QUERIES.active); }
  static boolean shadows() { return QUERIES != null && read(QUERIES.shadows); }
  private static boolean read(MethodHandle query) {
    try { return (boolean) query.invokeExact(); }
    catch (Throwable failure) { throw new IllegalStateException("Unable to query the Iris render pass", failure); }
  }
}
