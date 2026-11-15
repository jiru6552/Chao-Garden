package com.chaogarden.client;

import com.chaogarden.ChaoVariants;
import com.chaogarden.core.ChaoGaitClock;
import com.chaogarden.core.ChaoState;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Shared runtime for the imported Bedrock cuboids and their authored clips. */
public final class OfficialChaoRig {
  public static final List<String> REFERENCE_MODELS = List.of("baby","swim","fly","run","power",
      "hero","hero_fly","hero_power","dark","dark_run","dark_swim","golden","baghead",
      "dad","dragon","ender","tails","knuckles","emerald","creeper");
  private record Frame(ChaoMeshAsset mesh, Map<String, ChaoMeshAsset.Pose> poses) {}
  private final Map<String, Frame> frames = new HashMap<>();
  private final JsonObject clips;
  private final Map<JsonElement,NavigableMap<Float,JsonElement>> channelPoints = new IdentityHashMap<>();
  private Frame current;
  private String key = "baby";
  private int sampledFrames;
  private boolean referenceRendering;
  private boolean adultPose, broadWalkHead;
  private float powerSwimWeight;
  private boolean prototypeSelection;
  private boolean neutralSelection056,heroNormalSelection057,neutralSwimSelection059,darkNormalSelection060,neutralFlySelection061,neutralRunSelection062,neutralPowerSelection063,heroSwimSelection064,heroFlySelection065,heroRunSelection066,heroPowerSelection067,darkSwimSelection068,darkFlySelection069,darkRunSelection070,darkPowerSelection071;
  private String prototypeOwnerKey;
  public enum Motion { CRAWL, WALK, RUN, DASH, FLAIL, PADDLE, SWIM, GLIDE, GRIP, FALL, HELD, SIT, REST,
    PONDER, INSPECT, EAT, TRAIN, REJECT, DELIGHT, PLAY, PET }
  private Motion motion = Motion.WALK;
  private float idleGestureWeight;
  // Entity UUIDs stay strongly held while that entity is loaded. Weak keys avoid
  // retaining departed creatures, without evicting every stride in a busy garden.
  private final Map<UUID, ChaoGaitClock> gaitClocks = new WeakHashMap<>();
  private final LinkedHashMap<UUID, Transition> transitions = new LinkedHashMap<>(128, .75f, true);
  private static final class Transition {
    Motion mode;
    int emotion;
    String geometry;
    float age, started;
    Map<String, float[]> last, from;
    boolean lastProne,fromProne,crawlWingTransition068,crawlWingTransition069,floorWingTransition069;
  }


  private final java.util.function.Function<String,ChaoMeshAsset> assets;
  public OfficialChaoRig() {this(loadClips(),ChaoMeshAsset::load);}
  OfficialChaoRig(JsonObject clips,java.util.function.Function<String,ChaoMeshAsset> assets){
    this.clips=clips;this.assets=assets;if(clips.size()!=8)throw new IllegalStateException("Expected eight imported Chao clips");select("baby");
  }
  private static JsonObject loadClips(){
    var id=ResourceLocation.fromNamespaceAndPath("chao_garden","animations/official/chao.animation.json");
    try(var reader=Minecraft.getInstance().getResourceManager().openAsReader(id)){
      return JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("animations");
    }catch(IOException|RuntimeException error){throw new IllegalStateException("Cannot load imported Chao animations",error);}
  }

  private void select(String model) { select(model,false); }

  private void select(String model,boolean gameplay) {
    key = model;
    boolean prototype=gameplay&&prototypeSelection&&model.equals(prototypeOwnerKey);
    String prototypeModel=darkPowerSelection071?"dark_power_proto071":darkRunSelection070?"dark_run_proto070":darkFlySelection069?"dark_fly_proto069":darkSwimSelection068?"dark_swim_proto068":heroPowerSelection067?"hero_power_proto067":heroRunSelection066?"hero_run_proto066":heroFlySelection065?"hero_fly_proto065":heroSwimSelection064?"hero_swim_proto064":neutralPowerSelection063?"neutral_power_proto063":neutralRunSelection062?"neutral_run_proto062":neutralFlySelection061?"neutral_fly_proto061":darkNormalSelection060?"dark_normal_proto060":neutralSwimSelection059?"neutral_swim_proto059":heroNormalSelection057?"hero_normal_proto057":neutralSelection056?"neutral_proto056":"hero_run_proto055";
    String cache=prototype?"gameplay:"+prototypeModel:(gameplay?"gameplay:":"source:")+model;
    current = frames.computeIfAbsent(cache, ignored -> {
      var mesh = assets.apply(prototype?"gameplay/chao_"+prototypeModel:(gameplay?"gameplay/":"official/")+"chao_"+model);
      return new Frame(mesh, mesh.createPoses());
    });
  }

  /** Selection preserves our genetic/care state; source variants are only visual rigs. */
  public static String geometryKey(ChaoState state) {
    if(state.character!=null && Set.of("tails","amy","knuckles","omochao").contains(state.character))return state.character;
    if(state.immortal&&"normal".equals(state.character))return state.align>=.5f?"chaos_angel":state.align<=-.5f?"chaos_devil":"chaos_light";
    if (!state.adult) return "baby";
    int type = state.primaryType;
    boolean hero = state.align >= .5f, dark = state.align <= -.5f;
    if (hero) return switch (type) { case 1 -> "hero_swim"; case 2 -> "hero_fly"; case 3 -> "hero_run"; case 4 -> "hero_power"; default -> "hero"; };
    if (dark) return switch (type) { case 1 -> "dark_swim"; case 2 -> "dark_fly"; case 3 -> "dark_run"; case 4 -> "dark_power"; default -> "dark"; };
    return switch (type) { case 1 -> "swim"; case 2 -> "fly"; case 3 -> "run"; case 4 -> "power"; default -> "baby"; };
  }

  public static ResourceLocation texture(ChaoState state) {
    if(DarkPowerAppearance071.sample(state).enabled())return ChaoPaletteMaterial.texture(DarkPowerCoatMaterial071.texture(state),state,"baby");
    if(DarkRunAppearance070.sample(state).enabled())return ChaoPaletteMaterial.texture(DarkRunCoatMaterial070.texture(state),state,"baby");
    if(DarkFlyAppearance069.sample(state).enabled())return ChaoPaletteMaterial.texture(DarkFlyCoatMaterial069.texture(state),state,"baby");
    if(DarkSwimAppearance068.sample(state).enabled())return ChaoPaletteMaterial.texture(DarkSwimCoatMaterial068.texture(state),state,"baby");
    if(HeroPowerAppearance067.sample(state).enabled())return ChaoPaletteMaterial.texture(HeroPowerCoatMaterial067.texture(state),state,"baby");
    if(HeroRunAppearance066.sample(state).enabled())return ChaoPaletteMaterial.texture(HeroRunCoatMaterial066.texture(state),state,"baby");
    if(HeroFlyAppearance065.sample(state).enabled())return ChaoPaletteMaterial.texture(HeroFlyCoatMaterial065.texture(state),state,"baby");
    if(HeroSwimAppearance064.sample(state).enabled())return ChaoPaletteMaterial.texture(HeroSwimCoatMaterial064.texture(state),state,"baby");
    if(NeutralPowerAppearance063.sample(state).enabled())return ChaoPaletteMaterial.texture(NeutralPowerCoatMaterial063.texture(state),state,"baby");
    if(NeutralRunAppearance062.sample(state).enabled())return ChaoPaletteMaterial.texture(NeutralRunCoatMaterial062.texture(state),state,"baby");
    if(NeutralFlyAppearance061.sample(state).enabled())return ChaoPaletteMaterial.texture(NeutralFlyCoatMaterial061.texture(state),state,"baby");
    if(DarkNormalAppearance060.sample(state).enabled())return ChaoPaletteMaterial.texture(DarkNormalCoatMaterial060.texture(state),state,"baby");
    if(NeutralSwimAppearance059.sample(state).enabled())return ChaoPaletteMaterial.texture(NeutralSwimCoatMaterial059.texture(state),state,"baby");
    if(HeroNormalAppearance057.sample(state).enabled())return ChaoPaletteMaterial.texture(HeroNormalCoatMaterial057.texture(state),state,"baby");
    if(NeutralAppearance056.sample(state).enabled())return ChaoPaletteMaterial.texture(NeutralCoatMaterial056.texture(state),state,"baby");
    if(HeroRunAppearance052.sample(state).enabled())return ChaoPaletteMaterial.texture(HeroRunCoatMaterial055.texture(state),state,"baby");
    String selected=geometryKey(state),geometry=sourceGeometryKey(selected),old=ChaoRenderer.textureKey(state),path;
    if(selected.equals("omochao"))return OmochaoMaterial.texture(state);
    if(selected.startsWith("chaos_"))
      return ChaoPaletteMaterial.texture(ResourceLocation.fromNamespaceAndPath("chao_garden",
          "textures/entity/chao/gameplay/chaos/"+old+".png"),state,"chaos");
    if(selected.equals("amy"))
      return ChaoPaletteMaterial.texture(ResourceLocation.fromNamespaceAndPath("chao_garden",
          "textures/entity/chao/gameplay/character_"+selected+".png"),state,geometry);
    if(selected.equals("tails")||selected.equals("knuckles"))
      return ChaoPaletteMaterial.texture(ResourceLocation.fromNamespaceAndPath("chao_garden",
          "textures/entity/chao/official/"+selected+".png"),state,geometry);
    if(!ChaoVariants.NORMAL.equals(ChaoVariants.activeId(state)))
      return ChaoPaletteMaterial.texture(ResourceLocation.fromNamespaceAndPath("chao_garden",
          "textures/entity/chao/gameplay/variants/"+ChaoVariants.activeId(state)+"/"+geometry+".png"),state,geometry);
    int effectiveType = state.primaryType;
    if (state.immortal || !ChaoVariants.NORMAL.equals(ChaoVariants.activeId(state))) path = old;
    else if (state.baseColor() == 0 && state.jewel() == 0 && !state.isShiny()
        && !(state.adult && (state.align>=.5f && (effectiveType==1 || effectiveType==3)
          || state.align<=-.5f && (effectiveType==2 || effectiveType==4)))) path = geometry;
    else path = "coats/" + geometry + "/" + old;
    var raw=ResourceLocation.fromNamespaceAndPath("chao_garden","textures/entity/chao/official/"+path+".png");
    return ChaoPaletteMaterial.texture(GradualCoatMaterial.texture(raw,state,geometry),state,geometry);
  }

  public static String referenceGeometry(ChaoState state, String requested) {
    return REFERENCE_MODELS.contains(requested) ? requested : sourceGeometryKey(geometryKey(state));
  }

  /** Exact original PNG; no palette, extra symbols, animal parts or theme overlay. */
  public static ResourceLocation referenceTexture(ChaoState state, String requested) {
    return ResourceLocation.fromNamespaceAndPath("chao_garden",
        "textures/entity/chao/official/"+referenceGeometry(state,requested)+".png");
  }

  /** UV/attachment skeleton alias; gameplay counterparts retain these semantic anchors. */
  public static String sourceGeometryKey(String selected) {
    return switch(selected) {
      case "hero_swim" -> "swim";case "hero_run" -> "run";
      case "dark_fly" -> "fly";case "dark_power" -> "power";
      case "amy","omochao","chaos_light","chaos_angel","chaos_devil" -> "baby";
      default -> selected;
    };
  }
  public String sourceGeometryKey() {
    // These scoped gameplay paths retain the original baby UV and attachment
    // family while their saved/logical alignment and first type remain locked.
    return current.mesh.heroRunPrototype()||current.mesh.neutralPrototype056()||current.mesh.heroNormalPrototype057()||current.mesh.neutralSwimPrototype059()||current.mesh.darkNormalPrototype060()||current.mesh.neutralFlyPrototype061()||current.mesh.neutralRunPrototype062()||current.mesh.neutralPowerPrototype063()||current.mesh.heroSwimPrototype064()||current.mesh.heroFlyPrototype065()||current.mesh.heroRunPrototype066()||current.mesh.heroPowerPrototype067()||current.mesh.darkSwimPrototype068()||darkFanPosture070()
        ?"baby":sourceGeometryKey(key);
  }
  public String geometryKey() { return key; }
  long shapeSignature(){return current.mesh.shapeSignature();}
  boolean naturalHeroCoreActive(){return current.mesh.naturalHeroCoreActive();}
  void expression(int value){current.mesh.expression(value);}
  void eyesVisible(boolean visible){current.mesh.eyesVisible(current.poses,visible);}
  Map<String, ChaoMeshAsset.Pose> poses() { return current.poses; }
  /** Actual sampled surface; a logical family can select a shared morph asset. */
  ChaoMeshAsset sampledMesh() { return current.mesh; }
  /** Visible terminal volume in arm-local pixels, before procedural reach. */
  float[] baseToyHandBounds(boolean left) {
    var arm=pose(left?"arm_left":"arm_right");
    if(arm==null)return null;
    float reach=arm.reach;arm.reach=0;
    try{return current.mesh.visibleHandBounds(current.poses,left);}
    finally{arm.reach=reach;}
  }
  ChaoMeshAsset.Pose pose(String name) { return current.poses.get(name); }
  public int sampledFrames() { return sampledFrames; }

  public void animate(String model, float ageTicks, float limbSwing, float movement,
                      int emotion, float emotionSeconds, float yawDegrees, float pitchDegrees) {
    select(model);
    referenceRendering=true;
    current.poses.values().forEach(ChaoMeshAsset.Pose::reset);
    float time = ageTicks / 20f;
    float walking = Mth.clamp(movement * 4f, 0, 1);
    if(emotion==0) {
      apply("animation.chao.idle", time, 1 - walking, 0, 0);
      apply("animation.chao.walk", time, walking, 0, 0);
    }
    String clip = switch (emotion) { case 1 -> "sad"; case 2 -> "alert"; case 3 -> "happy"; default -> "no_emotion"; };
    apply("animation.chao." + clip, emotion == 0 ? time : Math.max(0, emotionSeconds), 1, 0, 0);
    // Mutually exclusive modelled symbols: unused authoring glyphs are never
    // rendered together when a clip omits their explicit scale channel.
    String marker = emotion == 3 ? "emotion_heart" : emotion == 2 ? "emotion_exclamation" : emotion == 0 ? "emotion_dot" : "";
    for (String symbol : List.of("emotion_dot", "emotion_heart", "emotion_exclamation", "emotion_question", "emotion_dizzy"))
      if (!symbol.equals(marker)) hidePart(symbol);
    var skull = pose("skull");
    if (skull != null) {
      skull.ry += Mth.clamp(yawDegrees * Mth.DEG_TO_RAD, -.65f, .65f);
      skull.rx += Mth.clamp(pitchDegrees * Mth.DEG_TO_RAD, -.25f, .25f);
    }
    sampledFrames++;
  }

  public Motion motion() { return motion; }
  public float idleGestureWeight() { return idleGestureWeight; }

  /** Legacy inspection timing; gameplay no longer samples this imported sweep. */
  public static float gestureWeight(float ageTicks, long seed) {
    int period = 260 + (int) Math.floorMod(seed, 140L);
    float phase = (ageTicks + Math.floorMod(seed >>> 17, (long) period)) % period;
    if (phase < 0) phase += period;
    if (phase >= 27.5f) return 0;
    float fade = Math.min(phase / 5f, (27.5f - phase) / 5f);
    float value = Mth.clamp(fade, 0, 1);
    return value * value * (3 - 2 * value);
  }

  /**
   * Gameplay controller. The raw animate method above remains the exact source-clip
   * sampler used by the imported-animation inspection; ordinary creatures use this
   * quieter controller and stat-dependent locomotion instead.
   */
  public void animateGameplay(String model, UUID id, Motion requested, float ageTicks,
      float limbSwing, float movement, int emotion, float emotionSeconds,
      float yawDegrees, float pitchDegrees, boolean allowGesture) {
    animateGameplay(model,id,requested,ageTicks,limbSwing,movement,emotion,emotionSeconds,
        yawDegrees,pitchDegrees,allowGesture,true);
  }

  /** Defer the blend when the model will add held-item, gene or activity poses. */
  void animateGameplay(String model, UUID id, Motion requested, float ageTicks,
      float limbSwing, float movement, int emotion, float emotionSeconds,
      float yawDegrees, float pitchDegrees, boolean allowGesture, boolean finishTransition) {
    select(model,true);
    referenceRendering=false;
    motion = requested;
    current.poses.values().forEach(ChaoMeshAsset.Pose::reset);
    var monsterCrown=pose("monster_crown");if(monsterCrown!=null)monsterCrown.visible=false;
    long seed = id.getLeastSignificantBits() ^ id.getMostSignificantBits();
    float activity = Mth.clamp(movement * 22f, 0, 1);
    boolean uprightGait = requested == Motion.WALK || requested == Motion.RUN || requested == Motion.DASH;
    float walk = uprightGait ? activity : 0;
    // Keep the source resting silhouette, but do not replay its broad arm/
    // torso sweep during gameplay: it reads as repetitive belly scratching.
    // Server-selected078 episodes now own expressive idle movement. Raw
    // animate() above remains the exact imported/reference clip sampler.
    idleGestureWeight = 0;
    if(emotion==0) {
      apply("animation.chao.idle", 0, 1 - walk, 0, 0);
    }
    // Vanilla limbSwing integrates distance, not clip seconds. Feeding it directly
    // to the imported clip made a high-Run Chao flicker through many full strides.
    var gaitClock = gaitClocks.computeIfAbsent(id, ignored -> new ChaoGaitClock());
    float gaitTime = gaitClock.sample(ageTicks, limbSwing, movement, uprightGait && emotion == 0);
    if(emotion==0)apply("animation.chao.walk", gaitTime, walk, 0, 0);
    float breath = Mth.sin(ageTicks * .055f + (seed & 1023) * .01f);
    var body = pose("body");
    var skull = pose("skull");
    if (requested == Motion.RUN || requested == Motion.DASH) {
      // Keep the authored feet and their ground contact, but steady the broad
      // walking shimmy as the faster gait leans into its direction of travel.
      float steady = 1 - walk * (requested == Motion.DASH ? .60f : .38f);
      if (body != null) {
        body.x *= steady; body.y *= steady; body.ry *= steady; body.rz *= steady;
      }
      if (skull != null) {
        skull.x *= steady; skull.y *= steady; skull.z *= steady; skull.rz *= steady;
      }
    }
    // Broad Dark Power, Chaos, Tails and developed Swim heads can meet walking palms;
    // that inherited overlap also carries into the first toy-entry frames.
    if((broadWalkHead||powerSwimWeight>0)&&uprightGait&&skull!=null){
      float anatomy=broadWalkHead?1:powerSwimWeight;
      float steady=1-.9f*walk*anatomy;
      skull.x*=steady;skull.rz*=steady;skull.y-=.35f*walk*anatomy;
      if(body!=null){body.x*=steady;body.y*=steady;body.rz*=steady;}
      // Keep these scoped full-head shoulders on the original torso sides.
      // A small downward walking offset puts swinging palms below the full
      // skull's look arc while their upper roots remain within the torso.
      // This preserves the original gaze range and avoids an outward root gap.
      if(current.mesh.darkNormalPrototype060()||current.mesh.neutralFlyPrototype061()||current.mesh.neutralRunPrototype062()||current.mesh.neutralPowerPrototype063()||current.mesh.heroSwimPrototype064()||current.mesh.heroFlyPrototype065()||current.mesh.heroRunPrototype066()||current.mesh.heroPowerPrototype067()||current.mesh.darkSwimPrototype068()||darkFanPosture070())for(boolean left:new boolean[]{false,true}){var arm=pose(left?"arm_left":"arm_right");if(arm!=null)arm.y+=.60f*walk;}
      if(powerSwimWeight>0&&!current.mesh.darkNormalPrototype060()&&!current.mesh.neutralFlyPrototype061()&&!current.mesh.neutralRunPrototype062()&&!current.mesh.neutralPowerPrototype063()&&!current.mesh.heroSwimPrototype064()&&!current.mesh.heroFlyPrototype065()&&!current.mesh.heroRunPrototype066()&&!current.mesh.heroPowerPrototype067()&&!current.mesh.darkSwimPrototype068()&&!darkFanPosture070())for(boolean left:new boolean[]{false,true}){var arm=pose(left?"arm_left":"arm_right");if(arm!=null)arm.x+=(left?.75f:-.75f)*walk*powerSwimWeight;}
    }
    if (body != null) { body.y += breath * .035f; body.sy += breath * .004f; }
    if (skull != null) { skull.y += breath * .02f; skull.rx += breath * .006f; }
    switch (requested) {
      // A newborn covers only a few hundredths of a block per tick. Upright
      // gait weighting and stride length made its previous tiny strokes almost
      // disappear. Keep the crawl short and readable at that actual speed.
      case CRAWL -> crawl(limbSwing * 7.5f, Mth.clamp(movement * 45f, 0, 1));
      case RUN, DASH -> {
        float lean = requested == Motion.DASH ? .24f : .12f;
        if (body != null) body.rx += lean * activity;
        if (skull != null) { skull.z -= lean * 2 * activity; skull.y += lean * activity; }
      }
      case FLAIL, PADDLE, SWIM -> swim(requested, ageTicks);
      case GLIDE -> glide(ageTicks);
      case GRIP -> grip();
      case FALL -> {
        rotate("arm_left", -.1f, .1f, -.2f);
        rotate("arm_right", -.1f, -.1f, .2f);
        rotate("skull", -.10f, 0, 0);
      }
      case SIT -> sit(false);
      case REST -> {
        sit(true);
        // The sleeping child at 06:27-06:29 curls low with its face down,
        // rather than remaining in the same upright sitting silhouette.
        if(key.equals("baby")) {
          if(body!=null){body.y-=1.10f;body.rx=.75f;body.rz=.16f;}
          if(skull!=null){skull.y+=1.25f;skull.z-=.8f;skull.rx+=.34f;skull.rz+=.10f;}
          rotate("arm_left",-.15f,.65f,.50f);rotate("arm_right",-.15f,-.65f,-.50f);
        }
      }
      case PET -> {
        sit(false);
        // SA2's long pet sequence keeps the feet planted and the arms low.
        // The imported celebration remains available to DELIGHT/comparison.
        float content=Mth.sin(ageTicks*.15f);
        rotate("skull",-.09f+content*.025f,0,content*.035f);
        for(String name:List.of("arm_left","arm_right")) {
          var arm=pose(name);if(arm==null)continue;
          float side=name.endsWith("left")?1:-1;
          arm.y=.5f;arm.rx=-.06f;arm.rz=side*.95f;
        }
      }
      case HELD -> held(ageTicks);
      case PONDER, INSPECT, EAT, TRAIN, REJECT, DELIGHT -> sit(false);
      case PLAY -> sit(false);
      default -> { }
    }
    String reaction = requested==Motion.PET ? "no_emotion" : switch (emotion) { case 1 -> "sad"; case 2 -> "alert"; case 3 -> "happy"; default -> "no_emotion"; };
    apply("animation.chao." + reaction, emotion == 0 ? ageTicks / 20f : Math.max(0, emotionSeconds), 1, 0, 0);
    String marker = emotion == 3 ? "emotion_heart" : emotion == 2 ? "emotion_exclamation" : emotion == 0 ? "emotion_dot" : "";
    for (String symbol : List.of("emotion_dot", "emotion_heart", "emotion_exclamation", "emotion_question", "emotion_dizzy"))
      if (!symbol.equals(marker)) hidePart(symbol);
    if (skull != null) {
      skull.ry += Mth.clamp(yawDegrees * Mth.DEG_TO_RAD, -.65f, .65f);
      skull.rx += Mth.clamp(pitchDegrees * Mth.DEG_TO_RAD, -.25f, .25f);
    }
    if(switch(requested){case SIT,PET,PONDER,INSPECT,EAT,TRAIN,REJECT,DELIGHT,PLAY->true;default->false;})
      ChaoSittingPose.apply(current.mesh,current.poses,adultPose);
    heroFlyWingPose065(requested);
    heroRunWingPose066(requested);
    heroPowerWingPose067(requested);
    darkSwimWingPose068(requested);
    darkSwimTailPose068(requested);
    darkFlyWingPose069(requested);
    darkFlyTailPose069(requested);
    if(finishTransition)transition(id,requested,emotion,ageTicks);
    sampledFrames++;
  }

  /** Fold this family's broad fans with the existing posture transition.
   * The horizontal torso pitches in water/flight can otherwise lift the wing
   * roots and rays through the unchanged sibling skull. Joint definitions and
   * the authored surface stay fixed; posed roots remain inside the torso, and
   * animal wings inherit these same animated joint transforms.
   */
  private void heroFlyWingPose065(Motion requested) {
    if(!current.mesh.heroFlyPrototype065())return;
    boolean prone=switch(requested){case CRAWL,PADDLE,SWIM,GLIDE->true;default->false;};
    float lower=prone?2.5f:1.3f;
    float fold=switch(requested){
      case CRAWL,SWIM->1.2f;
      case PADDLE->.65f;
      case GLIDE->.4f;
      default->.3f;
    };
    for(String name:List.of("wing_left","wing_right")) {
      var wing=pose(name);if(wing==null)continue;
      wing.y+=lower;wing.rz+=(name.endsWith("left")?-1:1)*fold;
    }
  }

  /** Small Run-first wings share the original joints and existing transition.
   * The upright and prone offsets are scoped to066; all older profiles retain
   * their exact poses. Replacement animal wings inherit the posed anchors.
   */
  private void heroRunWingPose066(Motion requested) {
    if(!current.mesh.heroRunPrototype066())return;
    boolean prone=switch(requested){case CRAWL,PADDLE,SWIM,GLIDE->true;default->false;};
    float lower=prone?2.5f:1.0f;
    float fold=switch(requested){
      case CRAWL,SWIM->1.2f;
      case PADDLE->.65f;
      case GLIDE->.4f;
      default->.25f;
    };
    for(String name:List.of("wing_left","wing_right")) {
      var wing=pose(name);if(wing==null)continue;
      wing.y+=lower;wing.rz+=(name.endsWith("left")?-1:1)*fold;
    }
  }

  /** Power-first leaf/fan wings retain their original attachment joints.
   * Scoped posture folding also moves replacement animal wings through the
   * existing transition clock; older family poses remain unchanged.
   */
  private void heroPowerWingPose067(Motion requested) {
    if(!current.mesh.heroPowerPrototype067())return;
    boolean prone=switch(requested){case CRAWL,PADDLE,SWIM,GLIDE->true;default->false;};
    float lower=prone?2.5f:1.3f;
    float fold=switch(requested){
      case CRAWL,SWIM->1.2f;
      case PADDLE->.65f;
      case GLIDE->.4f;
      default->.3f;
    };
    for(String name:List.of("wing_left","wing_right")) {
      var wing=pose(name);if(wing==null)continue;
      wing.y+=lower;wing.rz+=(name.endsWith("left")?-1:1)*fold;
    }
  }

  /** Keep this family's wings clear of the fixed skull and moving feet.
   * CRAWL folds a compact fan against the torso; other poses retain their full
   * authored span. Replacement animal wings inherit these same posed joints.
   */
  private void darkSwimWingPose068(Motion requested) {
    if(!current.mesh.darkSwimPrototype068())return;
    if(requested==Motion.CRAWL) {
      for(String name:List.of("wing_left","wing_right")) {
        var wing=pose(name);if(wing==null)continue;
        wing.y+=1.3f;wing.z-=.8f;wing.rx+=Mth.HALF_PI-1.1f;
        wing.ry+=(name.endsWith("left")?-35:35)*Mth.DEG_TO_RAD;
        wing.sx*=.7f;wing.sy*=.7f;wing.sz*=.7f;
      }
      return;
    }
    boolean prone=switch(requested){case PADDLE,SWIM,GLIDE->true;default->false;};
    float lower=prone?2.25f:requested==Motion.DASH?1.6f:1.4f;
    float fold=switch(requested){case SWIM->1.05f;case PADDLE->.65f;case GLIDE->.4f;default->.3f;};
    for(String name:List.of("wing_left","wing_right")) {
      var wing=pose(name);if(wing==null)continue;
      wing.y+=lower;wing.rz+=(name.endsWith("left")?-1:1)*fold;
    }
  }

  /** Forward torso pitch otherwise sweeps this family's new upturned tail into
   * its rear hood. Fold only the two stem meshes about a point inside the
   * retained tail base; that base, its torso contact and animal joint stay exact.
   */
  private void darkSwimTailPose068(Motion requested) {
    if(!current.mesh.darkSwimPrototype068())return;
    boolean prone=switch(requested){case CRAWL,PADDLE,SWIM,GLIDE->true;default->false;};
    if(!prone&&requested!=Motion.DASH)return;
    var body=pose("body");var stem=pose("tail__cube_0");
    if(body==null||stem==null)return;
    float extra=requested==Motion.CRAWL?.70f:prone?.45f:0;
    stem.rx-=Math.max(0,body.rx)+extra;
    darkSwimTailAnchor068();
  }

  /** Re-evaluate the shared anchor after the production posture blend. */
  private void darkSwimTailAnchor068() {
    if(!current.mesh.darkSwimPrototype068())return;
    var stem=pose("tail__cube_0");if(stem==null)return;
    float c=Mth.cos(stem.rx),s=Mth.sin(stem.rx);
    float y=-.15f,z=1.75f;
    stem.y=y-(c*y-s*z);stem.z=z-(s*y+c*z);
  }

  /** The scoped Dark fan rigs share the same existing posture history. */
  private boolean darkFanPosture070() {
    return current.mesh.darkFlyPrototype069()||current.mesh.darkRunPrototype070()||current.mesh.darkPowerPrototype071();
  }

  /** Articulate the The scoped Dark fans and new tail stems on preserved joints.
   * Body, limbs and tail keep the existing eight-tick production clock;
   * crawl-entry/exit fans alone settle over fourteen ticks on that same clock. */
  private void darkFlyWingPose069(Motion requested) {
    if(!darkFanPosture070())return;
    if(requested==Motion.CRAWL) {
      for(String name:List.of("wing_left","wing_right")) {
        var wing=pose(name);if(wing==null)continue;
        wing.y+=1.3f;wing.z-=.8f;wing.rx+=Mth.HALF_PI-1.1f;
        wing.ry+=(name.endsWith("left")?-35:35)*Mth.DEG_TO_RAD;
        wing.sx*=.45f;wing.sy*=.45f;wing.sz*=.45f;
      }
      return;
    }
    boolean prone=switch(requested){case PADDLE,SWIM,GLIDE->true;default->false;};
    // The unchanged square head sweeps a rear corner toward the raised
    // shoulder fans when it looks up and sideways. Follow those same sampled
    // joint angles continuously; ordinary forward gaze retains the high fan.
    var head=pose("skull");
    float gaze=head==null?0:3*Math.max(0,-head.rx)+.8f*Math.abs(head.ry);
    float upright=.70f+gaze;
    if(requested==Motion.DASH)upright=Math.max(upright,1.6f);
    float lower=prone?2.25f:upright;
    float fold=switch(requested){case SWIM->1.05f;case PADDLE->.65f;case GLIDE->.4f;default->.10f;};
    for(String name:List.of("wing_left","wing_right")) {
      var wing=pose(name);if(wing==null)continue;
      wing.y+=lower;wing.rz+=(name.endsWith("left")?-1:1)*fold;
      if(!prone)wing.ry+=(name.endsWith("left")?1:-1)*gaze*.10f;
      // Compact fans naturally in these poses before another body turn.
      float motionFold=switch(requested){case DASH->.55f;case GLIDE,SIT,REST->.7f;default->1;};
      wing.sx*=motionFold;wing.sy*=motionFold;wing.sz*=motionFold;
    }
  }

  /** Per-render scratch only; no saved field, clock or cross-entity cache. */
  static final class FloorToyWingFrame069 {
    final boolean activeFloor;
    final float[] rawBody;
    final float[][] rawWings;
    float[] sourceBody;
    float[][] sourceWings;
    boolean recovering;
    FloorToyWingFrame069(boolean activeFloor,float[] body,float[][] wings) {
      this.activeFloor=activeFloor;this.rawBody=body;this.rawWings=wings;
    }
  }

  private static float[] floorWingPose069(ChaoMeshAsset.Pose p) {
    return new float[]{p.x,p.y,p.z,p.rx,p.ry,p.rz,p.sx,p.sy,p.sz};
  }

  /** Capture the ordinary target before its production transition. */
  FloorToyWingFrame069 floorToyWingBasis069(UUID id,boolean activeFloor) {
    if(!darkFanPosture070())return null;
    var history=transitions.get(id);
    if(!activeFloor&&(history==null||!history.lastProne&&!history.fromProne))return null;
    var body=pose("body");var left=pose("wing_left");var right=pose("wing_right");
    if(body==null||left==null||right==null)return null;
    return new FloorToyWingFrame069(activeFloor,floorWingPose069(body),
        new float[][]{floorWingPose069(left),floorWingPose069(right)});
  }

  /** Capture once after finish/exit-floor support and before the late toy pose. */
  void floorToyWingSource069(UUID id,FloorToyWingFrame069 frame) {
    if(frame==null)return;
    frame.sourceBody=floorWingPose069(pose("body"));
    frame.sourceWings=new float[][]{floorWingPose069(pose("wing_left")),floorWingPose069(pose("wing_right"))};
    frame.recovering=recoveringProne(id);
  }

  /** The actual car/crayon body turn occurs after finishGameplayFrame. Fit only
   * these fans after that whole pose and arm fit, then remember the final pose.
   * Targets are absolute and the captured source never changes, so applying
   * this hook twice to one frame cannot multiply its fold on a cancelled exit. */
  boolean floorToyWings069(FloorToyWingFrame069 frame,float presence) {
    if(frame==null||frame.sourceBody==null||frame.sourceWings==null
        ||!darkFanPosture070()||!frame.activeFloor)return false;
    var body=pose("body");if(body==null)return false;
    float visible=Mth.clamp(presence,0,1);
    float denominator=ChaoProneToyPose051.BODY_ANGLE-frame.rawBody[3];
    float amount=Math.abs(denominator)<.0001f?visible
        :Mth.clamp((body.rx-frame.rawBody[3])/denominator,0,1);
    amount=Math.max(amount,visible);
    float weight=frame.activeFloor?visible:amount;
    if(weight<=0)return false;
    var rawBodyQ=new org.joml.Quaternionf().rotationZYX(frame.rawBody[5],frame.rawBody[4],frame.rawBody[3]);
    var sourceBodyQ=new org.joml.Quaternionf().rotationZYX(frame.sourceBody[5],frame.sourceBody[4],frame.sourceBody[3]);
    var inverseBodyQ=new org.joml.Quaternionf().rotationZYX(body.rz,body.ry,body.rx).invert();
    int index=0;
    for(String name:List.of("wing_left","wing_right")) {
      var wing=pose(name);var raw=frame.rawWings[index];var source=frame.sourceWings[index];
      float baseYaw=(index==0?35:-35)*Mth.DEG_TO_RAD;
      var rawWorld=new org.joml.Quaternionf(rawBodyQ).mul(new org.joml.Quaternionf().rotationZYX(raw[5],baseYaw+raw[4],raw[3]));
      var sourceWorld=new org.joml.Quaternionf(sourceBodyQ).mul(new org.joml.Quaternionf().rotationZYX(source[5],baseYaw+source[4],source[3]));
      var euler=new org.joml.Quaternionf(inverseBodyQ).mul(sourceWorld.slerp(rawWorld,weight)).getEulerAnglesZYX(new org.joml.Vector3f());
      float scaleWeight=frame.activeFloor?Mth.clamp(visible*3,0,1):weight;
      wing.x=Mth.lerp(scaleWeight,source[0],raw[0]+(index==0?.48f:-.48f));
      wing.y=Mth.lerp(weight,source[1],raw[1]+1.1f*amount);
      // Stow slightly up the spine to clear both entry and natural-outro feet.
      // The root stays on the rear torso face; no head/body pose is changed.
      wing.y-=.35f*scaleWeight;
      wing.z=Mth.lerp(weight,source[2],raw[2]);
      wing.rx=euler.x;wing.ry=euler.y-baseYaw;wing.rz=euler.z;
      float scale=.25f;
      wing.sx=Mth.lerp(scaleWeight,source[6],raw[6]*scale);
      wing.sy=Mth.lerp(scaleWeight,source[7],raw[7]*scale);
      wing.sz=Mth.lerp(scaleWeight,source[8],raw[8]*scale);
      index++;
    }
    return true;
  }

  private void darkFlyTailPose069(Motion requested) {
    if(!darkFanPosture070())return;
    boolean prone=switch(requested){case CRAWL,PADDLE,SWIM,GLIDE->true;default->false;};
    if(!prone&&requested!=Motion.DASH)return;
    var body=pose("body");var stem=pose("tail__cube_0");
    if(body==null||stem==null)return;
    float extra=requested==Motion.CRAWL?.70f:prone?.45f:0;
    stem.rx-=Math.max(0,body.rx)+extra;
    darkFlyTailAnchor069();
  }

  private void darkFlyTailAnchor069() {
    if(!darkFanPosture070())return;
    var stem=pose("tail__cube_0");if(stem==null)return;
    float c=Mth.cos(stem.rx),s=Mth.sin(stem.rx);
    float y=-.15f,z=1.75f;
    stem.y=y-(c*y-s*z);stem.z=z-(s*y+c*z);
  }

  private void rotate(String name, float x, float y, float z) {
    var p = pose(name); if (p != null) { p.rx += x; p.ry += y; p.rz += z; }
  }

  /** Arms reach around the keeper's sides; inherited head anatomy stays intact. */
  private void grip() {
    var body = pose("body"); if (body != null) body.rx = 0;
    var head = pose("skull");
    if (head != null) { head.rx = -.04f; head.ry = 0; head.rz = 0; }
    for (String name : List.of("arm_left", "arm_right")) {
      var arm = pose(name); if (arm == null) continue;
      float side = name.endsWith("left") ? 1 : -1;
      arm.x = side * .45f; arm.y = .15f; arm.z = -1.8f;
      arm.rx = 0; arm.ry = side * .85f; arm.rz = side * .06f;
    }
    for (String name : List.of("foot_left", "foot_right", "foot_left2", "foot_right2", "feet_bone_right")) {
      var foot = pose(name); if (foot == null) continue;
      foot.y = -.25f; foot.z = -.7f; foot.rx = -.25f;
    }
    rotate("wing_left", 0, -.55f, 0); rotate("wing_right", 0, .55f, 0);
  }

  /** The comparison renderer keeps its exact mesh, UVs and source clips. */
  void applyCompanionGrip() { motion = Motion.GRIP; grip(); }

  /** Blend the existing seated pose into the existing grip, including replacement hands. */
  void applyCompanionCarry(float back) {
    if(back<=0)return;
    var before=new HashMap<String,float[]>();snapshot(current.poses,before);
    grip();
    current.poses.forEach((name,p)->{
      var a=before.get(name);if(a==null)return;
      p.x=Mth.lerp(back,a[0],p.x);p.y=Mth.lerp(back,a[1],p.y);p.z=Mth.lerp(back,a[2],p.z);
      p.rx=Mth.lerp(back,a[3],p.rx);p.ry=Mth.lerp(back,a[4],p.ry);p.rz=Mth.lerp(back,a[5],p.rz);
    });
  }


  private record CrawlStep(float reach, float lift) {}

  /** Sixty percent planted push, then a lifted recovery to the next contact. */
  private static CrawlStep crawlStep(float cycle) {
    float phase = cycle / Mth.TWO_PI;
    phase -= Mth.floor(phase);
    if (phase < .6f) return new CrawlStep(Mth.lerp(phase / .6f, -1.05f, 1.05f), 0);
    float recovery = (phase - .6f) / .4f;
    float ease = recovery * recovery * (3 - 2 * recovery);
    return new CrawlStep(Mth.lerp(ease, 1.05f, -1.05f),
        Mth.sin(recovery * Mth.PI) * .85f);
  }

  private void crawl(float cycle, float movement) {
    var body = pose("body");
    var head = pose("skull");
    final float pitch = 1.10f;
    float rock = Mth.sin(cycle) * movement;
    float bob = (1 - Mth.cos(cycle * 2)) * .045f * movement;
    if (body != null) {
      body.y += .5f + bob; body.rx = pitch;
      body.x += rock * .12f;
      body.ry += rock * .025f;
      body.rz += rock * .045f;
    }
    if (head != null) {
      // The source skull is the torso's sibling. Its low, upright face leads
      // the hands while the shoulders and tucked rear feet do the crawling.
      // Keep 0.7 pixel more shoulder clearance than the old static crawl: the
      // recovery hand now actually lifts toward the underside of the head.
      float lower = key.equals("hero") ? .3f : .7f;
      head.y += lower - bob * .35f;
      head.x += rock * .025f;
      head.z -= 3.4f; head.rx += .025f;
    }
    float sinPitch = Mth.sin(pitch), cosPitch = Mth.cos(pitch);
    for (String name : List.of("arm_left", "arm_right")) {
      var p = pose(name); if (p == null) continue;
      float side = name.endsWith("left") ? 1 : -1;
      CrawlStep step = crawlStep(cycle + (side < 0 ? Mth.PI : 0));
      float reach = step.reach * movement;
      float lift = step.lift * movement;
      // Express the visible hand arc in ground coordinates, then undo the
      // torso's pitch. A planted hand sweeps backward as the body moves ahead;
      // the other hand lifts and reaches instead of both sliding together.
      float vertical = -lift - bob;
      p.x = side * .9f;
      p.y = cosPitch * vertical + sinPitch * reach;
      p.z = .7f - sinPitch * vertical + cosPitch * reach;
      p.rx = 0; p.ry = side * pitch; p.rz = side * (1.4f - lift * .09f);
    }
    for (String name : List.of("foot_left", "foot_right", "foot_left2", "foot_right2", "feet_bone_right")) {
      var p = pose(name); if (p == null) continue;
      float side = name.contains("left") ? 1 : -1;
      // Each rear knee pushes with the opposite forelimb, then tucks forward.
      CrawlStep step = crawlStep(cycle + (side > 0 ? Mth.PI : 0));
      float reach = step.reach * .7f * movement;
      float lift = step.lift * .6f * movement;
      float depth = current.mesh.forwardReach(name);
      float bodyY = current.mesh.pivot("root", 1) + current.mesh.pivot("body", 1) + .5f;
      float ankle = current.mesh.pivot(name, 1);
      float rear = (bodyY + ankle * cosPitch + depth - 24f) / sinPitch;
      float vertical = -lift - bob;
      p.y = cosPitch * vertical + sinPitch * reach;
      p.z = rear - current.mesh.pivot(name, 2) - sinPitch * vertical + cosPitch * reach;
      p.rx = Mth.HALF_PI - pitch - lift * .22f; p.ry = 0; p.rz = 0;
    }
  }

  private void sit(boolean sleeping) {
    var body = pose("body"); var skull = pose("skull");
    float lower = sleeping ? 1.2f : .8f;
    if(powerSwimWeight>0)for(boolean left:new boolean[]{false,true}){var arm=pose(left?"arm_left":"arm_right");if(arm!=null)arm.rz+=(left?.22f:-.22f)*powerSwimWeight;}
    if (body != null) { body.y += lower; body.rx -= .08f; }
    if (skull != null) { skull.y += lower; skull.rx += sleeping ? .15f : .025f; if(sleeping)skull.rz += .18f; }
    for (String name : List.of("foot_left", "foot_right", "foot_left2", "foot_right2", "feet_bone_right")) {
      var p = pose(name); if (p != null && sleeping) { p.y -= .8f; p.z -= .5f; p.rx -= .65f; }
    }
  }

  /** Suspended, slightly tucked feet and low outstretched hands, as at 05:27. */
  private void held(float age) {
    float swing=Mth.sin(age*.11f),kick=Mth.sin(age*.17f);
    rotate("body",.035f,0,swing*.025f);
    rotate("skull",.055f,0,-swing*.018f);
    for(String name:List.of("arm_left","arm_right")) {
      var p=pose(name);if(p==null)continue;
      float side=name.endsWith("left")?1:-1;
      p.y=.45f;p.z-=.15f;p.rx=-.12f+side*swing*.025f;
      p.ry=side*.25f;p.rz=side*.80f;
    }
    for(String name:List.of("foot_left","foot_right","foot_left2","foot_right2","feet_bone_right")) {
      var p=pose(name);if(p==null)continue;
      float side=name.contains("left")?1:-1;
      p.y-=.25f;p.z-=.55f;p.rx=-.58f+side*kick*.06f;
    }
  }

  private void swim(Motion mode, float age) {
    float phase = age * (mode == Motion.FLAIL ? .48f : mode == Motion.SWIM ? .21f : .26f);
    var body = pose("body"); var head = pose("skull");
    float lean = mode == Motion.FLAIL ? -.09f : mode == Motion.SWIM ? .75f : .45f;
    if (body != null) {body.rx += lean;if(mode==Motion.FLAIL)body.rz+=Mth.sin(phase)*.035f;}
    if (head != null) { head.y += Math.max(0, lean); head.z -= Math.max(0, lean) * 1.8f; head.rx += mode == Motion.FLAIL ? -.08f : .05f; }
    for (String name : List.of("arm_left", "arm_right")) {
      var p = pose(name); if (p == null) continue;
      float side = name.endsWith("left") ? 1 : -1;
      if (mode == Motion.FLAIL) {
        // Small frantic alternating scoops while the body remains upright;
        // this is deliberately distinct from the trained breaststroke.
        p.rx = -.20f+Mth.sin(phase + (side < 0 ? Mth.PI : 0)) * .48f;
        p.ry = side*.25f;
        p.y=.35f;p.rz = side*(.55f+Mth.cos(phase)*.13f);
      } else if (mode == Motion.PADDLE) {
        p.rx += Mth.sin(phase + (side < 0 ? Mth.PI : 0)) * .42f;
        p.ry += side * .2f;
      } else {
        p.ry += side * (.45f + Mth.sin(phase) * .5f);
        p.rx += .2f + Mth.cos(phase) * .25f;
        p.rz -= side * .15f;
      }
    }
    for (String name : List.of("foot_left", "foot_right", "foot_left2", "foot_right2", "feet_bone_right")) {
      var p = pose(name); if (p != null) {
        p.rx += Mth.cos(phase + (name.contains("left") ? 0 : Mth.PI)) * (mode == Motion.FLAIL ? .62f : .35f);
        if(mode==Motion.FLAIL){p.rx-=.25f;p.z-=.32f;}
      }
    }
  }

  private void glide(float age) {
    var body = pose("body"); var head = pose("skull");
    if (body != null) body.rx += .65f;
    if (head != null) { head.y += .6f; head.z -= 1.2f; }
    for (String name : List.of("arm_left", "arm_right")) {
      var p = pose(name); if (p == null) continue;
      float side = name.endsWith("left") ? 1 : -1;
      p.rz = side * .28f; p.ry = side * .2f; p.rx = -.08f;
    }
    rotate("wing_left", 0, -.7f + Mth.sin(age * .12f) * .06f, 0);
    rotate("wing_right", 0, .7f - Mth.sin(age * .12f) * .06f, 0);
    rotate("foot_left", -.3f, 0, 0); rotate("foot_right", -.3f, 0, 0);
  }

  private void snapshot(Map<String, ChaoMeshAsset.Pose> poses, Map<String, float[]> target) {
    if(!current.mesh.darkSwimPrototype068()&&!darkFanPosture070())target.remove("tail__cube_0");
    poses.forEach((name,p) -> {
      if (name.contains("__cube_")&&!((current.mesh.darkSwimPrototype068()||darkFanPosture070())&&name.equals("tail__cube_0"))) return;
      float[] values = target.computeIfAbsent(name, key -> new float[10]);
      values[0]=p.x;values[1]=p.y;values[2]=p.z;values[3]=p.rx;values[4]=p.ry;
      values[5]=p.rz;values[6]=p.sx;values[7]=p.sy;values[8]=p.sz;values[9]=p.reach;
    });
  }

  private void transition(UUID id, Motion mode, int emotion, float age) {
    Transition state = transitions.get(id);
    // A culled Chao can change activities while it is offscreen. Replaying
    // an old pose on re-entry would attach a current prop to stale, lowered arms.
    if (state == null || age < state.age - 1 || age - state.age > 8) {
      state = new Transition(); state.mode = mode; state.emotion=emotion; state.geometry = key;
      transitions.put(id, state);
    } else if (state.mode != mode || state.emotion != emotion || !key.equals(state.geometry)) {
      state.crawlWingTransition068=current.mesh.darkSwimPrototype068()
          &&(state.mode!=mode&&(state.mode==Motion.CRAWL||mode==Motion.CRAWL)
             ||state.crawlWingTransition068&&state.from!=null);
      state.floorWingTransition069=darkFanPosture070()
          &&(state.lastProne||state.fromProne||state.floorWingTransition069&&state.from!=null);
      state.crawlWingTransition069=darkFanPosture070()
          &&(state.mode!=mode&&(state.mode==Motion.CRAWL||mode==Motion.CRAWL)
             ||state.lastProne||state.fromProne&&state.from!=null
             ||state.crawlWingTransition069&&state.from!=null);
      state.fromProne=state.lastProne||(state.fromProne&&state.from!=null);
      state.lastProne=false;
      state.from = state.last; state.last = null; state.started = age; state.mode = mode; state.emotion=emotion; state.geometry = key;
    }
    if (state.from != null) {
      float blend = Mth.clamp((age - state.started) / 8f, 0, 1);
      blend = blend * blend * (3 - 2 * blend);
      final float weight = blend;
      // Capture the true body-times-wing orientation before the ordinary
      // pose blend. Euler-angle sums cannot compensate for parent rotation.
      org.joml.Quaternionf[] wingWorld069=null;
      float[][] wingPositions069=null;
      if(state.crawlWingTransition069&&darkFanPosture070()) {
        var body069=pose("body");var left069=pose("wing_left");var right069=pose("wing_right");
        if(body069!=null&&left069!=null&&right069!=null){
          wingPositions069=new float[][]{{left069.x,left069.y,left069.z},{right069.x,right069.y,right069.z}};
          var bodyQ=new org.joml.Quaternionf().rotationZYX(body069.rz,body069.ry,body069.rx);
          wingWorld069=new org.joml.Quaternionf[]{new org.joml.Quaternionf(bodyQ).mul(new org.joml.Quaternionf().rotationZYX(left069.rz,35*Mth.DEG_TO_RAD+left069.ry,left069.rx)),new org.joml.Quaternionf(bodyQ).mul(new org.joml.Quaternionf().rotationZYX(right069.rz,-35*Mth.DEG_TO_RAD+right069.ry,right069.rx))};
        }
      }
      state.from.forEach((name, prior) -> {
        if(name.equals("tail__cube_0")&&!current.mesh.darkSwimPrototype068()&&!darkFanPosture070())return;
        var p = pose(name); if (p == null) return;
        p.x=Mth.lerp(weight,prior[0],p.x);p.y=Mth.lerp(weight,prior[1],p.y);p.z=Mth.lerp(weight,prior[2],p.z);
        p.rx=Mth.lerp(weight,prior[3],p.rx);p.ry=Mth.lerp(weight,prior[4],p.ry);p.rz=Mth.lerp(weight,prior[5],p.rz);
        p.sx=Mth.lerp(weight,prior[6],p.sx);p.sy=Mth.lerp(weight,prior[7],p.sy);p.sz=Mth.lerp(weight,prior[8],p.sz);p.reach=Mth.lerp(weight,prior[9],p.reach);
      });
      if(state.crawlWingTransition068&&current.mesh.darkSwimPrototype068()) {
        // The existing eight-tick weight briefly tucks the fan while the
        // torso turns, then reopens it exactly at the target pose.
        float fold=1-3*blend*(1-blend);
        for(String name:List.of("wing_left","wing_right")) {
          var wing=pose(name);if(wing==null)continue;
          wing.sx*=fold;wing.sy*=fold;wing.sz*=fold;
        }
      }
      if(state.crawlWingTransition069&&darkFanPosture070()) {
        float elapsed=age-state.started;
        // Slerp only these fans in world orientation, then remove the current
        // eight-tick torso rotation. This keeps a broad fan from sweeping
        // through the fixed skull or feet while the torso changes posture.
        if(wingWorld069!=null&&state.from.containsKey("body")) {
          float t=Mth.clamp(elapsed/14f,0,1);t=t*t*(3-2*t);
          var body069=pose("body");var priorBody=state.from.get("body");int index=0;
          var fromBodyQ=new org.joml.Quaternionf().rotationZYX(priorBody[5],priorBody[4],priorBody[3]);
          var inverseBodyQ=new org.joml.Quaternionf().rotationZYX(body069.rz,body069.ry,body069.rx).invert();
          for(String name:List.of("wing_left","wing_right")) {
            var wing=pose(name);var prior=state.from.get(name);float baseYaw=(index==0?35:-35)*Mth.DEG_TO_RAD;
            if(wing!=null&&prior!=null){var fromQ=new org.joml.Quaternionf(fromBodyQ).mul(new org.joml.Quaternionf().rotationZYX(prior[5],baseYaw+prior[4],prior[3]));var worldQ=fromQ.slerp(wingWorld069[index],t);var euler=new org.joml.Quaternionf(inverseBodyQ).mul(worldQ).getEulerAnglesZYX(new org.joml.Vector3f());wing.rx=euler.x;wing.ry=euler.y-baseYaw;wing.rz=euler.z;}index++;
          }
        }
        // Keep a floor-origin root stowed through the first half of the
        // existing torso turn, then release it over4..8. Its local motion
        // counters the parent's late downward floor-support recovery.
        if(state.floorWingTransition069&&wingPositions069!=null) {
          float reopen=Mth.clamp((elapsed-4)/4f,0,1);reopen=reopen*reopen*(3-2*reopen);
          int index=0;
          for(String name:List.of("wing_left","wing_right")) {
            var wing=pose(name);var prior=state.from.get(name);var target=wingPositions069[index++];
            if(wing==null||prior==null)continue;
            wing.x=Mth.lerp(reopen,prior[0],target[0]);
            wing.y=Mth.lerp(reopen,prior[1],target[1]);
            wing.z=Mth.lerp(reopen,prior[2],target[2]);
          }
        }
        // Contract, hold during the torso turn, then reopen smoothly. No
        // extra saved timer or biological-age state is introduced.
        float fold;
        if(elapsed<=6.25f){float t=Mth.clamp(elapsed/6.25f,0,1);t=t*t*(3-2*t);fold=Mth.lerp(t,1,.25f);}
        else if(elapsed<=8)fold=.25f;
        else {float t=Mth.clamp((elapsed-8)/6f,0,1);t=t*t*(3-2*t);fold=Mth.lerp(t,.25f,1);}
        for(String name:List.of("wing_left","wing_right")) {
          var wing=pose(name);if(wing==null)continue;
          wing.sx*=fold;wing.sy*=fold;wing.sz*=fold;
        }
      }
      if (blend >= 1){state.fromProne=false;state.crawlWingTransition068=false;if(!(state.crawlWingTransition069&&darkFanPosture070()&&age-state.started<14f)){state.from=null;state.crawlWingTransition069=false;state.floorWingTransition069=false;}}
    }
    darkSwimTailAnchor068();
    darkFlyTailAnchor069();
    state.age = age;
    if (state.last == null) state.last = new HashMap<>();
    snapshot(current.poses, state.last);
    if (transitions.size() > 96) transitions.remove(transitions.keySet().iterator().next());
  }

  /** Blend the complete outgoing/target poses, including interaction additions. */
  void finishGameplayFrame(UUID id, int emotion, float ageTicks) {
    if(switch(motion){case SIT,PET,PONDER,INSPECT,EAT,TRAIN,REJECT,DELIGHT,PLAY->true;default->false;})
      for(String foot:ChaoSittingPose.FEET)ChaoSittingPose.ground(current.mesh,current.poses,foot);
    transition(id,motion,emotion,ageTicks);
  }

  /** Keep the final constraint-fitted pose for interruption recovery. */
  void rememberGameplayFrame(UUID id) {
    var state=transitions.get(id);if(state==null)return;
    if(state.last==null)state.last=new HashMap<>();
    snapshot(current.poses,state.last);
  }
  /** Only the actual completed car/crayon pose marks an outgoing floor-play blend. */
  void rememberProneFrame(UUID id,boolean prone){var state=transitions.get(id);if(state!=null)state.lastProne=prone;}
  boolean recoveringProne(UUID id){var state=transitions.get(id);return state!=null&&state.from!=null&&state.fromProne;}

  void transformBone(String name, PoseStack stack) { current.mesh.transformBone(name,stack,current.poses); }
  void copyAttachmentPoseTo(String name,ChaoMeshAsset target,ChaoMeshAsset.Pose to) {
    var from=current.poses.get(name);if(from!=null)current.mesh.copyJointPoseTo(name,target,from,to);
  }

  float[] rattlePalm() { return toyPalm(false); }
  float[] toyPalm(boolean left) { return current.mesh.toyPalm(current.poses,left); }
  float[] attachmentBounds(String name) { return current.mesh.attachmentBounds(name,current.poses); }
  float[] attachmentMouth() { return current.mesh.attachmentMouth(current.poses); }
  float[] attachmentEyes() { return current.mesh.attachmentEyes(current.poses); }

  void symbol(String selected) {
    for(String name:List.of("emotion_dot","emotion_heart","emotion_exclamation","emotion_question","emotion_dizzy")) {
      var p=pose(name);if(p==null)continue;
      current.mesh.setSubtreeVisible(name,current.poses,name.equals(selected));
      if(name.equals(selected))p.sx=p.sy=p.sz=1;
    }
  }

  /** Set this entity's geometry before gait fitting samples its current toe reach. */
  void prepareDevelopment(String model,ChaoState state) {
    neutralSelection056=NeutralAppearance056.sample(state).enabled();
    heroNormalSelection057=HeroNormalAppearance057.sample(state).enabled();
    neutralSwimSelection059=NeutralSwimAppearance059.sample(state).enabled();
    darkNormalSelection060=DarkNormalAppearance060.sample(state).enabled();
    neutralFlySelection061=NeutralFlyAppearance061.sample(state).enabled();
    neutralRunSelection062=NeutralRunAppearance062.sample(state).enabled();
    neutralPowerSelection063=NeutralPowerAppearance063.sample(state).enabled();
    heroSwimSelection064=HeroSwimAppearance064.sample(state).enabled();
    heroFlySelection065=HeroFlyAppearance065.sample(state).enabled();
    heroRunSelection066=HeroRunAppearance066.sample(state).enabled();
    heroPowerSelection067=HeroPowerAppearance067.sample(state).enabled();
    darkSwimSelection068=DarkSwimAppearance068.sample(state).enabled();
    darkFlySelection069=DarkFlyAppearance069.sample(state).enabled();
    darkRunSelection070=DarkRunAppearance070.sample(state).enabled();
    darkPowerSelection071=DarkPowerAppearance071.sample(state).enabled();
    prototypeSelection=darkPowerSelection071||darkRunSelection070||darkFlySelection069||darkSwimSelection068||heroPowerSelection067||heroRunSelection066||heroFlySelection065||heroSwimSelection064||neutralPowerSelection063||neutralRunSelection062||neutralFlySelection061||darkNormalSelection060||neutralSwimSelection059||heroNormalSelection057||neutralSelection056||HeroRunAppearance052.sample(state).enabled();prototypeOwnerKey=model;
    select(model,true);meshDevelopment(state);
  }

  private void meshDevelopment(ChaoState state) {
    adultPose=state.adult;
    var neutralSwim=NeutralSwimAppearance059.sample(state);
    var darkNormal=DarkNormalAppearance060.sample(state);
    var neutralFly=NeutralFlyAppearance061.sample(state);
    var neutralRun=NeutralRunAppearance062.sample(state);
    var neutralPower=NeutralPowerAppearance063.sample(state);
    var heroSwim=HeroSwimAppearance064.sample(state);
    var heroFly=HeroFlyAppearance065.sample(state);
    var heroRun=HeroRunAppearance066.sample(state);
    var heroPower=HeroPowerAppearance067.sample(state);
    var darkSwim=DarkSwimAppearance068.sample(state);
    var darkFly=DarkFlyAppearance069.sample(state);
    var darkRun=DarkRunAppearance070.sample(state);
    var darkPower=DarkPowerAppearance071.sample(state);
    // Scoped full-head profiles follow their actual current-bar Power anatomy,
    // including reductions; legacy paths keep their existing gait input.
    powerSwimWeight=darkPower.enabled()?Math.max(0,darkPower.runPower())*darkPower.maturity()
        :darkRun.enabled()?Math.max(0,darkRun.runPower())*darkRun.maturity()
        :darkFly.enabled()?Math.max(0,darkFly.runPower())*darkFly.maturity()
        :darkSwim.enabled()?Math.max(0,darkSwim.runPower())*darkSwim.maturity()
        :heroPower.enabled()?Math.max(0,heroPower.runPower())*heroPower.maturity()
        :heroRun.enabled()?Math.max(0,heroRun.runPower())*heroRun.maturity()
        :heroFly.enabled()?Math.max(0,heroFly.runPower())*heroFly.maturity()
        :heroSwim.enabled()?Math.max(0,heroSwim.runPower())*heroSwim.maturity()
        :neutralPower.enabled()?Math.max(0,neutralPower.runPower())*neutralPower.maturity()
        :neutralRun.enabled()?Math.max(0,neutralRun.runPower())*neutralRun.maturity()
        :neutralFly.enabled()?Math.max(0,neutralFly.runPower())*neutralFly.maturity()
        :darkNormal.enabled()?Math.max(0,darkNormal.runPower())*darkNormal.maturity()
        :neutralSwim.enabled()?Math.max(0,neutralSwim.runPower())*neutralSwim.maturity()
        :state.adult&&"normal".equals(state.character)&&state.primaryType==ChaoState.SWIM_TYPE?Mth.clamp(state.runPower,0,1)*state.gradualVisualStrength():0;
    // These scoped first-type profiles retain the full original head even at zero Power.
    // Keep its walking palms clear independently of reversible specialization.
    broadWalkHead=darkPower.enabled()||darkRun.enabled()||darkFly.enabled()||darkSwim.enabled()||heroPower.enabled()||heroRun.enabled()||heroFly.enabled()||heroSwim.enabled()||neutralPower.enabled()||neutralRun.enabled()||neutralFly.enabled()||darkNormal.enabled()||neutralSwim.enabled()||state.immortal||"tails".equals(state.character)||state.adult&&"normal".equals(state.character)&&state.align<=-.5f&&state.primaryType==ChaoState.POWER_TYPE;
    boolean develop=state.adult && !state.immortal
        && (state.character==null||state.character.equals("normal"));
    boolean child=!state.adult && !state.immortal && (state.character==null||state.character.equals("normal"));
    current.mesh.naturalHeroCore(!heroSwim.enabled()&&develop&&ChaoVariants.NORMAL.equals(ChaoVariants.activeId(state))&&state.align>=.5f&&state.primaryType==ChaoState.SWIM_TYPE&&state.baseColor()==0&&state.jewel()==0);
    float growth=state.gradualVisualStrength();
    current.mesh.development(develop||child?state.swimFly:0,develop||child?state.runPower:0,
        develop?growth:child?growth*.14f:0);
    current.mesh.heroRunDevelopment(current.mesh.heroRunPrototype()?HeroRunAppearance052.sample(state):null,state.swimFly,state.runPower);
    current.mesh.heroRunTwoTone(state.baseColor()==0&&state.jewel()==0&&state.isTwoTone());
    if(current.mesh.neutralPrototype056())current.mesh.neutralDevelopment(NeutralAppearance056.sample(state));
    if(current.mesh.heroNormalPrototype057())current.mesh.heroNormalDevelopment(HeroNormalAppearance057.sample(state));
    if(current.mesh.neutralSwimPrototype059())current.mesh.neutralSwimDevelopment(neutralSwim);
    if(current.mesh.darkNormalPrototype060())current.mesh.darkNormalDevelopment(darkNormal);
    if(current.mesh.neutralFlyPrototype061())current.mesh.neutralFlyDevelopment(neutralFly);
    if(current.mesh.neutralRunPrototype062())current.mesh.neutralRunDevelopment(neutralRun);
    if(current.mesh.neutralPowerPrototype063())current.mesh.neutralPowerDevelopment(neutralPower);
    if(current.mesh.heroSwimPrototype064())current.mesh.heroSwimDevelopment(heroSwim);
    if(current.mesh.heroFlyPrototype065())current.mesh.heroFlyDevelopment(heroFly);
    if(current.mesh.heroRunPrototype066())current.mesh.heroRunDevelopment066(heroRun);
    if(current.mesh.heroPowerPrototype067())current.mesh.heroPowerDevelopment067(heroPower);
    if(current.mesh.darkSwimPrototype068())current.mesh.darkSwimDevelopment068(darkSwim);
    if(current.mesh.darkFlyPrototype069())current.mesh.darkFlyDevelopment069(darkFly);
    if(current.mesh.darkRunPrototype070())current.mesh.darkRunDevelopment070(darkRun);
    if(current.mesh.darkPowerPrototype071())current.mesh.darkPowerDevelopment071(darkPower);
  }

  /** Reversible secondary anatomy is blended independently from biological age. */
  void development(ChaoState state) {
    meshDevelopment(state);
    var marker=pose("emotion_dots");
    if(marker!=null) {
      float offset=current.mesh.proportionMarkerOffset();
      marker.y+=offset-marker.proportionY;
      marker.proportionY=offset;
    }
    int[] colors={0,0xff4a9df1,0xff48bd67,0xffdf505c,0xffffd852,0xff9b68d9,
        0xffee8ab5,0xffef9b47,0xff48d5d2,0xff8edfff,0xffd75d55,0xff61e58b,0xffffd852};
    current.mesh.lens(colors[Math.max(0,Math.min(colors.length-1,state.eyeColor))]);
    int stage=state.monsterStage();float boost=stage==0?0:Mth.clamp(Math.max(stage*.35f,state.monsterInfluence()-1),0,2);
    var crown=pose("monster_crown");if(crown!=null)crown.visible=stage>0&&state.monsterType()==ChaoState.NORMAL;
    if(boost>0) {
      // Authored amplification beyond ordinary development. Semantic anchors
      // scale with their attachments; comparison sampling never enters here.
      var skull=pose("skull");if(skull!=null&&!current.mesh.fixedHeadBox055()){skull.sx*=1+boost*.05f;skull.sy*=1+boost*.03f;skull.sz*=1+boost*.07f;}
      var body=pose("body");if(body!=null){body.sx*=1+boost*.08f;body.sy*=1+boost*.05f;body.y-=boost*.125f;}
      for(var entry:current.poses.entrySet())if(entry.getKey().startsWith("evolution_ridge_")||entry.getKey().startsWith("evolution020_")) {
        var ridge=entry.getValue();ridge.sx*=1+boost*.12f;ridge.sy*=1+boost*.15f;ridge.sz*=1+boost*.25f;
      }
      if(crown!=null){crown.sy=1+boost*.2f;crown.sz=1+boost*.2f;}
    }
  }

  /** Public special clips retained for matching source rigs and development inspection. */
  public void applySpecial(boolean carrying, float swelling, float ageTicks) {
    if (key.equals("ender") && carrying) {
      apply("animation.chao_ender.carry", ageTicks / 20f, 1, 1, swelling);
      // Idle/walk use variable.carry to hide this mesh. Carry is supplied here
      // after locomotion sampling, so restore its authored unit scale as well.
      var block = pose("held_block");
      if (block != null) block.sx = block.sy = block.sz = 1;
    }
    if (key.equals("creeper")) apply("animation.chao_creeper.swell", ageTicks / 20f, 1, 0, swelling);
  }

  public void hideCube(String bone, int index) {
    var p = pose(bone + "__cube_" + index); if (p != null) p.visible = false;
  }
  void hidePart(String name) { current.mesh.setSubtreeVisible(name, current.poses, false); }
  void hideHeadDecorations() { current.mesh.hideHeadDecorations(current.poses); }

  public void render(PoseStack stack, VertexConsumer vertices, int light, int overlay, int color) {
    current.mesh.render(stack, vertices, light, overlay, color, current.poses,!referenceRendering);
  }

  public boolean finite() {
    for (var p : current.poses.values())
      for (float v : new float[]{p.x,p.y,p.z,p.rx,p.ry,p.rz,p.sx,p.sy,p.sz,p.reach})
        if (!Float.isFinite(v)) return false;
    return true;
  }

  private void apply(String name, float seconds, float weight, float carry, float swelling) {
    if (weight <= 0) return;
    JsonObject clip = clips.getAsJsonObject(name);
    float time = seconds;
    if (clip.has("animation_length")) {
      float length = clip.get("animation_length").getAsFloat();
      boolean loop = clip.has("loop") && clip.get("loop").isJsonPrimitive()
          && clip.get("loop").getAsJsonPrimitive().isBoolean() && clip.get("loop").getAsBoolean();
      time = loop ? seconds % length : Math.min(seconds, length);
    }
    for (var entry : clip.getAsJsonObject("bones").entrySet()) {
      var p = pose(entry.getKey()); if (p == null) continue;
      JsonObject channels = entry.getValue().getAsJsonObject();
      for (var channel : channels.entrySet()) {
        String property = channel.getKey();
        for (int i=0; i<3; i++) {
          float sign = property.equals("rotation") ? (i == 0 ? 1 : -1) : (i == 2 ? 1 : -1);
          float currentValue = switch (property) {
            case "rotation" -> (i==0?p.rx:i==1?p.ry:p.rz) / Mth.DEG_TO_RAD * sign;
            case "position" -> (i==0?p.x:i==1?p.y:p.z) * sign;
            case "scale" -> i==0?p.sx:i==1?p.sy:p.sz;
            default -> 0;
          };
          float v = sample(channel.getValue(), time, i, currentValue, carry, swelling);
          if (property.equals("rotation")) {
            v *= sign * Mth.DEG_TO_RAD * weight;
            if(i==0)p.rx+=v;else if(i==1)p.ry+=v;else p.rz+=v;
          } else if (property.equals("position")) {
            v *= sign * weight;
            if(i==0)p.x+=v;else if(i==1)p.y+=v;else p.z+=v;
          } else if (property.equals("scale")) {
            v=(v-1)*weight;
            if(i==0)p.sx+=v;else if(i==1)p.sy+=v;else p.sz+=v;
          }
        }
      }
    }
  }

  private float sample(JsonElement data, float time, int component,
                              float prior, float carry, float swelling) {
    if (!data.isJsonObject()) return value(data, component, time, prior, carry, swelling);
    var points = channelPoints.computeIfAbsent(data, source -> {
      var sorted = new TreeMap<Float,JsonElement>();
      for (var p : source.getAsJsonObject().entrySet()) sorted.put(Float.parseFloat(p.getKey()),p.getValue());
      return sorted;
    });
    var a = points.floorEntry(time); if(a==null)a=points.firstEntry();
    var b = points.ceilingEntry(time); if(b==null)b=points.lastEntry();
    float av=value(a.getValue(),component,time,prior,carry,swelling);
    float bv=value(b.getValue(),component,time,prior,carry,swelling);
    return a.getKey().equals(b.getKey()) ? av : Mth.lerp((time-a.getKey())/(b.getKey()-a.getKey()),av,bv);
  }

  private static float value(JsonElement raw, int component, float time,
                             float prior, float carry, float swelling) {
    if(raw.isJsonArray())raw=raw.getAsJsonArray().get(component);
    if(raw.getAsJsonPrimitive().isNumber())return raw.getAsFloat();
    return new Expression(raw.getAsString(),time,prior,carry,swelling).parse();
  }

  /** Small deterministic Molang subset used by the eight supplied source clips. */
  private static final class Expression {
    final String source; final float time, prior, carry, swelling; int at;
    Expression(String s,float t,float p,float c,float swell){source=s.toLowerCase(Locale.ROOT).replace(" ","");time=t;prior=p;carry=c;swelling=swell;}
    float parse(){float n=sum();if(at!=source.length())throw new IllegalArgumentException("Unsupported Chao animation expression");return n;}
    boolean eat(char c){if(at<source.length()&&source.charAt(at)==c){at++;return true;}return false;}
    float sum(){float n=product();while(at<source.length()){if(eat('+'))n+=product();else if(eat('-'))n-=product();else break;}return n;}
    float product(){float n=atom();while(at<source.length()){if(eat('*'))n*=atom();else if(eat('/'))n/=atom();else break;}return n;}
    float atom(){if(eat('-'))return -atom();if(eat('+'))return atom();if(eat('(')){float n=sum();if(!eat(')'))throw new IllegalArgumentException("Animation parenthesis");return n;}
      int start=at;
      if(at<source.length()&&(Character.isDigit(source.charAt(at))||source.charAt(at)=='.')){while(at<source.length()&&(Character.isDigit(source.charAt(at))||source.charAt(at)=='.'))at++;return Float.parseFloat(source.substring(start,at));}
      while(at<source.length()&&(Character.isLetterOrDigit(source.charAt(at))||source.charAt(at)=='.'||source.charAt(at)=='_'))at++;
      if(start==at)throw new IllegalArgumentException("Animation expression token");
      String id=source.substring(start,at);
      if(eat('(')){float n=sum();if(!eat(')'))throw new IllegalArgumentException("Animation function");return switch(id){case "math.sin"->Mth.sin(n*Mth.DEG_TO_RAD);case "math.cos"->Mth.cos(n*Mth.DEG_TO_RAD);default->throw new IllegalArgumentException("Unsupported Chao animation function");};}
      return switch(id){case "query.anim_time"->time;case "this"->prior;case "variable.carry"->carry;case "variable.swelling_scale1"->(float)((Math.pow(Mth.clamp(swelling,0,1),4)*.4+1)*(Math.sin(swelling*5730*Mth.DEG_TO_RAD)*swelling*.01+1));case "variable.swelling_scale2"->(float)((Math.pow(Mth.clamp(swelling,0,1),4)*.1+1)/(Math.sin(swelling*5730*Mth.DEG_TO_RAD)*swelling*.01+1));default->throw new IllegalArgumentException("Unsupported Chao animation variable");};
    }
  }
}
