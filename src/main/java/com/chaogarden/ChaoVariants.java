package com.chaogarden;

import com.chaogarden.core.ChaoState;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.function.Predicate;
import net.neoforged.fml.ModList;

/** Data-driven appearance integrations. No optional mod classes are linked. */
public final class ChaoVariants {
  public static final String NORMAL = "normal";

  public record Definition(String id, String name, List<String> mods, boolean morphedHead) {
    public Definition(String id, String name, List<String> mods) {
      this(id, name, mods, false);
    }

    public Definition {
      if (id == null || !id.matches("[a-z][a-z0-9_]{0,47}") || id.equals(NORMAL))
        throw new IllegalArgumentException("Invalid Chao variant id: " + id);
      if (name == null || name.isBlank()) throw new IllegalArgumentException("Missing variant name");
      mods = List.copyOf(mods);
      for (String mod : mods)
        if (!mod.matches("[a-z][a-z0-9_]{1,63}"))
          throw new IllegalArgumentException("Invalid integration mod id: " + mod);
    }
  }

  private static final Map<String, Definition> DEFINITIONS = load();
  private static final Set<String> RETIRED = loadRetired();

  private ChaoVariants() {}

  private static Map<String, Definition> load() {
    String path = "/assets/chao_garden/chao_variants.json";
    var stream = ChaoVariants.class.getResourceAsStream(path);
    if (stream == null) throw new IllegalStateException("Missing Chao variant catalog " + path);
    try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
      JsonElement source = JsonParser.parseReader(reader);
      JsonArray entries = source.isJsonArray() ? source.getAsJsonArray()
          : source.getAsJsonObject().getAsJsonArray("variants");
      if (entries == null) throw new IllegalStateException("Chao variant catalog has no variants");
      Map<String, Definition> definitions = new LinkedHashMap<>();
      for (JsonElement entry : entries) {
        JsonObject value = entry.getAsJsonObject();
        List<String> mods = new ArrayList<>();
        for (JsonElement mod : value.getAsJsonArray("mods")) mods.add(mod.getAsString());
        Definition definition = new Definition(value.get("id").getAsString(),
            value.get("name").getAsString(), mods,
            value.has("head_morph") && value.get("head_morph").getAsBoolean());
        if (definitions.putIfAbsent(definition.id(), definition) != null)
          throw new IllegalStateException("Duplicate Chao variant " + definition.id());
      }
      return java.util.Collections.unmodifiableMap(definitions);
    } catch (IOException | RuntimeException error) {
      throw new IllegalStateException("Cannot load Chao variant catalog", error);
    }
  }

  private static Set<String> loadRetired() {
    String path = "/assets/chao_garden/retired_chao_variants.json";
    var stream = ChaoVariants.class.getResourceAsStream(path);
    if (stream == null) throw new IllegalStateException("Missing retired Chao catalog " + path);
    try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
      Set<String> ids = new LinkedHashSet<>();
      for (JsonElement value : JsonParser.parseReader(reader).getAsJsonArray()) {
        String id = value.getAsString();
        if (!id.matches("[a-z][a-z0-9_]{0,47}") || id.equals(NORMAL)
            || DEFINITIONS.containsKey(id) || !ids.add(id))
          throw new IllegalStateException("Invalid retired Chao theme: " + id);
      }
      return java.util.Collections.unmodifiableSet(ids);
    } catch (IOException | RuntimeException error) {
      throw new IllegalStateException("Cannot load retired Chao catalog", error);
    }
  }

  /** Keep serialized blood_magic identities while exposing the current mod name. */
  public static String canonicalId(String id) {
    return "neovitae".equals(id) ? "blood_magic" : id;
  }

  public static String commandId(String id) {
    return "blood_magic".equals(canonicalId(id)) ? "neovitae" : id;
  }

  public static List<String> retiredIds() {
    return List.copyOf(RETIRED);
  }

  public static boolean retired(String id) {
    return RETIRED.contains(id);
  }

  /** A deliberately discontinued form becomes ordinary without losing care or genes. */
  public static ChaoState normalizeRetired(ChaoState state) {
    if (retired(state.variant)) state.variant = NORMAL;
    return state;
  }

  public static List<Definition> all() {
    return List.copyOf(DEFINITIONS.values());
  }

  public static Optional<Definition> find(String id) {
    return Optional.ofNullable(DEFINITIONS.get(canonicalId(id)));
  }

  public static boolean hasMorphedHead(String id) {
    return find(id).map(Definition::morphedHead).orElse(false);
  }

  /** Alternate IDs are aliases: loading any one of them unlocks the theme. */
  public static boolean available(Definition definition, Predicate<String> loadedMod) {
    return !retired(definition.id())
        && (definition.mods().isEmpty() || definition.mods().stream().anyMatch(loadedMod));
  }

  public static boolean available(String id, Predicate<String> loadedMod) {
    if (NORMAL.equals(id)) return true;
    Definition definition = DEFINITIONS.get(canonicalId(id));
    return definition != null && available(definition, loadedMod);
  }

  public static boolean available(String id) {
    return available(id, mod -> ModList.get().isLoaded(mod));
  }

  public static boolean available(Definition definition) {
    return available(definition, mod -> ModList.get().isLoaded(mod));
  }

  /** Existing appearance availability is separate from new acquisition during this phase. */
  public static boolean obtainable(String id) {
    String canonical=canonicalId(id);
    return NORMAL.equals(canonical)||"nether".equals(canonical)||"end".equals(canonical);
  }
  public static boolean obtainable(Definition definition) {return obtainable(definition.id());}
  public static ChaoState normalizeNewCreature(ChaoState state) {
    if(!obtainable(state.variant))state.variant=NORMAL;
    return state;
  }

  /** A removed integration becomes dormant without erasing the saved heritage. */
  public static String activeId(ChaoState state) {
    return activeId(state, mod -> ModList.get().isLoaded(mod));
  }

  public static String activeId(ChaoState state, Predicate<String> loadedMod) {
    return available(state.variant, loadedMod) ? canonicalId(state.variant) : NORMAL;
  }

  public static String name(String id) {
    return find(id).map(Definition::name).orElse("Normal");
  }

  public static String itemId(String id) {
    return canonicalId(id) + "_chao_egg";
  }

  public static String lockedMessage(String id) {
    if(find(id).map(definition -> !definition.mods().isEmpty()).orElse(false))
      return "New addon Chao are temporarily unavailable. This saved egg is preserved for a future update.";
    if (retired(id)) return "This Chao theme has been removed. Existing Chao and eggs use their ordinary form.";
    return find(id).map(definition -> definition.name()
            + " requires its matching Minecraft mod. This egg remains saved until that mod is loaded.")
        .orElse("This Chao appearance is unavailable. Its saved heritage has been preserved.");
  }
}
